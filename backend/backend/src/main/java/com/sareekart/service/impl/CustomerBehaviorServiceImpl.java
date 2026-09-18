package com.sareekart.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.response.analytics.AnalyticsDateRange;
import com.sareekart.dto.response.analytics.DateRangeWindow;
import com.sareekart.dto.response.customer.*;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.entity.CustomerEventType;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.CustomerBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerBehaviorServiceImpl implements CustomerBehaviorService {

    private final CustomerEventRepository customerEventRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "secret", "creditcard", "cvv", "cardnumber",
            "auth", "authorization", "pin", "cvc", "expiry", "accountnumber"
    );

    private static final int MAX_METADATA_CHARS = 10000;
    private static final int MAX_BATCH_SIZE = 50;

    @Override
    @Transactional
    public CustomerEventResponse recordEvent(CustomerEventRequest request, Long authenticatedUserId, String clientIp, String userAgent) {
        if (request == null) {
            throw new BadRequestException("Event request cannot be null");
        }

        validateRequest(request);

        // Deduplication check for retried events
        if (request.getClientEventId() != null && !request.getClientEventId().isBlank()) {
            Optional<CustomerEvent> existingOpt = customerEventRepository.findByClientEventId(request.getClientEventId().trim());
            if (existingOpt.isPresent()) {
                log.debug("Deduplicated incoming event with clientEventId: {}", request.getClientEventId());
                return mapToResponse(existingOpt.get());
            }
        }

        String clientEventId = (request.getClientEventId() != null && !request.getClientEventId().isBlank())
                ? request.getClientEventId().trim()
                : "evt_" + UUID.randomUUID().toString().replace("-", "");

        User user = null;
        if (authenticatedUserId != null) {
            user = userRepository.findById(authenticatedUserId).orElse(null);
        }

        Map<String, Object> sanitizedMeta = sanitizeMetadata(request.getMetadata());
        String metaJson = serializeMetadata(sanitizedMeta);

        CustomerEvent event = CustomerEvent.builder()
                .clientEventId(clientEventId)
                .sessionId(request.getSessionId().trim())
                .user(user)
                .eventType(request.getEventType().trim().toUpperCase())
                .entityType(request.getEntityType() != null ? request.getEntityType().trim().toUpperCase() : null)
                .entityId(request.getEntityId())
                .metadata(metaJson)
                .ipAddress(clientIp)
                .userAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                .build();

        try {
            CustomerEvent saved = customerEventRepository.save(event);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate event detected for clientEventId: {}", clientEventId);
            return customerEventRepository.findByClientEventId(clientEventId)
                    .map(this::mapToResponse)
                    .orElseGet(() -> mapToResponse(event));
        }
    }

    @Override
    @Transactional
    public List<CustomerEventResponse> recordBatch(List<CustomerEventRequest> requests, Long authenticatedUserId, String clientIp, String userAgent) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        if (requests.size() > MAX_BATCH_SIZE) {
            throw new BadRequestException("Event batch size exceeds maximum limit of " + MAX_BATCH_SIZE);
        }

        List<CustomerEventResponse> responses = new ArrayList<>(requests.size());
        for (CustomerEventRequest req : requests) {
            try {
                responses.add(recordEvent(req, authenticatedUserId, clientIp, userAgent));
            } catch (Exception ex) {
                log.warn("Failed to record individual event in batch: {}", ex.getMessage());
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public int identifySession(String sessionId, Long authenticatedUserId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("Session ID is required for identity resolution");
        }
        if (authenticatedUserId == null) {
            throw new BadRequestException("Authenticated user context is required for identity resolution");
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authenticatedUserId));

        String cleanSession = sessionId.trim();

        // Session ownership protection: Ensure this session was not already permanently linked to another user
        List<Long> linkedUserIds = customerEventRepository.findDistinctUserIdsBySessionId(cleanSession);
        for (Long existingUid : linkedUserIds) {
            if (existingUid != null && !existingUid.equals(authenticatedUserId)) {
                throw new BadRequestException("Cannot associate session: session has already been claimed by another user");
            }
        }

        int updatedCount = customerEventRepository.linkSessionToUser(cleanSession, user);
        log.info("Identity resolution completed: linked {} events from session {} to user {}", updatedCount, cleanSession, authenticatedUserId);
        return updatedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public BehavioralOverviewResponse getBehavioralOverview(String range, LocalDate startDate, LocalDate endDate) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        DateRangeWindow window = DateRangeWindow.calculate(dateRange, startDate, endDate);

        LocalDateTime start = window.getCurrentStart();
        LocalDateTime end = window.getCurrentEnd();

        long totalEvents = customerEventRepository.countByCreatedAtBetween(start, end);
        long activeSessions = customerEventRepository.countDistinctSessionsBetween(start, end);

        // Count per event type
        Map<String, Long> eventCountsByType = new LinkedHashMap<>();
        for (CustomerEventType type : CustomerEventType.values()) {
            long count = customerEventRepository.countByEventTypeAndCreatedAtBetween(type.name(), start, end);
            eventCountsByType.put(type.name(), count);
        }

        // True 4-stage event conversion funnel (preserved for backward compatibility)
        List<BehavioralFunnelStageDto> funnel = buildFunnel(start, end);

        // Full 8-stage production eCommerce funnel
        List<BehavioralFunnelStageDto> ecommerceFunnel = buildEcommerceFunnel(start, end);

        // Auxiliary Channel Engagement metrics
        Map<String, Long> channelEngagement = computeChannelEngagement(start, end);

        // Conversion and abandonment ratios
        Map<String, Double> funnelMetrics = computeFunnelMetrics(ecommerceFunnel);

        // Product engagement & search telemetry
        List<CustomerEvent> periodEvents = customerEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        List<TopTrendingProductDto> topProducts = computeTopTrendingProducts(periodEvents);
        List<SearchQueryTelemetryDto> topSearches = computeSearchTelemetry(periodEvents, false);
        List<SearchQueryTelemetryDto> zeroResultSearches = computeSearchTelemetry(periodEvents, true);

        // Recent events stream
        List<CustomerEventResponse> recentEvents = customerEventRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return BehavioralOverviewResponse.builder()
                .range(range)
                .totalEvents(totalEvents)
                .activeSessions(activeSessions)
                .eventCountsByType(eventCountsByType)
                .funnel(funnel)
                .ecommerceFunnel(ecommerceFunnel)
                .channelEngagement(channelEngagement)
                .funnelMetrics(funnelMetrics)
                .topProducts(topProducts)
                .topSearches(topSearches)
                .zeroResultSearches(zeroResultSearches)
                .recentEvents(recentEvents)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAffinityResponse getCustomerAffinityProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<CustomerEvent> events = customerEventRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Integer> fabricWeights = new LinkedHashMap<>();
        Map<String, Integer> colorWeights = new LinkedHashMap<>();
        Map<String, Integer> weaveWeights = new LinkedHashMap<>();

        long views = 0;
        long cartAdds = 0;
        long purchases = 0;
        long checkoutInitiated = 0;
        long searches = 0;
        BigDecimal totalPriceAccumulator = BigDecimal.ZERO;
        long priceDataPoints = 0;
        LocalDateTime lastActiveAt = null;

        for (CustomerEvent event : events) {
            if (lastActiveAt == null || (event.getCreatedAt() != null && event.getCreatedAt().isAfter(lastActiveAt))) {
                lastActiveAt = event.getCreatedAt();
            }

            int weight = 1;
            String type = event.getEventType();

            if (CustomerEventType.PRODUCT_VIEW.name().equals(type)) {
                views++;
                weight = 1;
            } else if (CustomerEventType.AI_STYLIST_ENGAGE.name().equals(type)) {
                weight = 2;
            } else if (CustomerEventType.ADD_TO_CART.name().equals(type)) {
                cartAdds++;
                weight = 3;
            } else if (CustomerEventType.ORDER_COMPLETED.name().equals(type)) {
                purchases++;
                weight = 5;
            } else if (CustomerEventType.CHECKOUT_INITIATED.name().equals(type)) {
                checkoutInitiated++;
                weight = 4;
            } else if (CustomerEventType.SEARCH_QUERY.name().equals(type)) {
                searches++;
            }

            Map<String, Object> meta = parseMetadata(event.getMetadata());

            // Extract Fabric
            String fabric = extractString(meta, "fabric", "fabricName");
            if (fabric != null && !fabric.isBlank()) {
                fabricWeights.merge(fabric.trim(), weight, Integer::sum);
            }

            // Extract Weave
            String weave = extractString(meta, "weave", "weaveType", "category");
            if (weave != null && !weave.isBlank()) {
                weaveWeights.merge(weave.trim(), weight, Integer::sum);
            }

            // Extract Color
            String color = extractString(meta, "color", "primaryColor");
            if (color != null && !color.isBlank()) {
                colorWeights.merge(color.trim(), weight, Integer::sum);
            }

            // Extract Price
            Number priceNum = extractNumber(meta, "price", "totalAmount", "orderValue");
            if (priceNum != null && priceNum.doubleValue() > 0) {
                totalPriceAccumulator = totalPriceAccumulator.add(BigDecimal.valueOf(priceNum.doubleValue()));
                priceDataPoints++;
            }
        }

        // Determine top picks
        String topFabric = fabricWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Pure Handloom Silk");

        String topWeave = weaveWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Kanchipuram");

        String topColor = colorWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Crimson Red");

        // Price sensitivity tier
        String priceSensitivity = "MID_MARKET";
        if (priceDataPoints > 0) {
            BigDecimal avgPrice = totalPriceAccumulator.divide(BigDecimal.valueOf(priceDataPoints), 2, RoundingMode.HALF_UP);
            if (avgPrice.compareTo(BigDecimal.valueOf(5000)) < 0) {
                priceSensitivity = "BUDGET";
            } else if (avgPrice.compareTo(BigDecimal.valueOf(15000)) > 0) {
                priceSensitivity = "LUXURY";
            }
        }

        // Deterministic Purchase Intent Score (0–100)
        int recencyScore = 0;
        if (lastActiveAt != null) {
            long daysSinceActive = ChronoUnit.DAYS.between(lastActiveAt, LocalDateTime.now());
            if (daysSinceActive <= 1) recencyScore = 25;
            else if (daysSinceActive <= 7) recencyScore = 15;
            else if (daysSinceActive <= 30) recencyScore = 5;
        }

        int viewScore = (int) Math.min(20, views * 2);
        int cartScore = (int) Math.min(30, cartAdds * 10);
        int checkoutScore = (int) Math.min(20, checkoutInitiated * 10);
        int purchaseScore = (int) Math.min(25, purchases * 25);

        int intentScore = Math.min(100, recencyScore + viewScore + cartScore + checkoutScore + purchaseScore);

        Map<String, Object> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("recencyPoints", recencyScore);
        scoreBreakdown.put("viewEngagementPoints", viewScore);
        scoreBreakdown.put("cartIntentPoints", cartScore);
        scoreBreakdown.put("checkoutPoints", checkoutScore);
        scoreBreakdown.put("purchasePoints", purchaseScore);
        scoreBreakdown.put("totalCalculatedScore", intentScore);

        return CustomerAffinityResponse.builder()
                .userId(userId)
                .preferredFabric(topFabric)
                .preferredWeave(topWeave)
                .preferredColor(topColor)
                .priceSensitivity(priceSensitivity)
                .purchaseIntentScore(intentScore)
                .totalViews(views)
                .totalCartAdds(cartAdds)
                .totalPurchases(purchases)
                .lastActiveAt(lastActiveAt)
                .fabricWeights(fabricWeights)
                .colorWeights(colorWeights)
                .scoreBreakdown(scoreBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerEventResponse> getCustomerJourney(Long userId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return customerEventRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchQueryTelemetryDto> getSearchTelemetry(String range, LocalDate startDate, LocalDate endDate, boolean zeroResultsOnly) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        DateRangeWindow window = DateRangeWindow.calculate(dateRange, startDate, endDate);

        List<CustomerEvent> searchEvents = customerEventRepository.findByEventTypeAndCreatedAtBetween(
                CustomerEventType.SEARCH_QUERY.name(), window.getCurrentStart(), window.getCurrentEnd());

        return computeSearchTelemetry(searchEvents, zeroResultsOnly);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversionFunnelResponse getConversionFunnel(String range, LocalDate startDate, LocalDate endDate) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        DateRangeWindow window = DateRangeWindow.calculate(dateRange, startDate, endDate);
        LocalDateTime start = window.getCurrentStart();
        LocalDateTime end = window.getCurrentEnd();

        long totalEvents = customerEventRepository.countByCreatedAtBetween(start, end);
        long activeSessions = customerEventRepository.countDistinctSessionsBetween(start, end);

        List<BehavioralFunnelStageDto> stages = buildEcommerceFunnel(start, end);
        Map<String, Long> channelEngagement = computeChannelEngagement(start, end);
        Map<String, Double> metrics = computeFunnelMetrics(stages);

        return ConversionFunnelResponse.builder()
                .range(range)
                .startDate(start.toLocalDate().toString())
                .endDate(end.toLocalDate().toString())
                .totalEvents(totalEvents)
                .activeSessions(activeSessions)
                .stages(stages)
                .overallConversionRate(metrics.getOrDefault("overallConversionRate", 0.0))
                .detailToCartRate(metrics.getOrDefault("detailToCartRate", 0.0))
                .cartToCheckoutRate(metrics.getOrDefault("cartToCheckoutRate", 0.0))
                .checkoutToPaymentRate(metrics.getOrDefault("checkoutToPaymentRate", 0.0))
                .paymentToOrderRate(metrics.getOrDefault("paymentToOrderRate", 0.0))
                .cartAbandonmentRate(metrics.getOrDefault("cartAbandonmentRate", 0.0))
                .checkoutAbandonmentRate(metrics.getOrDefault("checkoutAbandonmentRate", 0.0))
                .channelEngagement(channelEngagement)
                .build();
    }

    // =========================================================================
    // Helper Methods

    // =========================================================================

    private void validateRequest(CustomerEventRequest request) {
        if (request.getSessionId() == null || request.getSessionId().trim().isBlank()) {
            throw new BadRequestException("sessionId is mandatory for behavioral telemetry");
        }
        if (request.getEventType() == null || request.getEventType().trim().isBlank()) {
            throw new BadRequestException("eventType is mandatory for behavioral telemetry");
        }
        if (!CustomerEventType.isValid(request.getEventType())) {
            throw new BadRequestException("Invalid eventType: " + request.getEventType() + ". Supported types are: " + Arrays.toString(CustomerEventType.values()));
        }
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String lowerKey = key.toLowerCase().replaceAll("[^a-z]", "");
            if (SENSITIVE_KEYS.contains(lowerKey)) {
                clean.put(key, "[REDACTED]");
            } else {
                clean.put(key, entry.getValue());
            }
        }
        return clean;
    }

    private String serializeMetadata(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return null;
        try {
            String json = objectMapper.writeValueAsString(meta);
            if (json.length() > MAX_METADATA_CHARS) {
                throw new BadRequestException("Event metadata payload exceeds maximum allowed size of " + MAX_METADATA_CHARS + " bytes");
            }
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private CustomerEventResponse mapToResponse(CustomerEvent event) {
        return CustomerEventResponse.builder()
                .id(event.getId())
                .clientEventId(event.getClientEventId())
                .sessionId(event.getSessionId())
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .userEmail(event.getUser() != null ? event.getUser().getEmail() : null)
                .eventType(event.getEventType())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .metadata(parseMetadata(event.getMetadata()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private List<BehavioralFunnelStageDto> buildFunnel(LocalDateTime start, LocalDateTime end) {
        String[] stages = {
                CustomerEventType.PRODUCT_VIEW.name(),
                CustomerEventType.ADD_TO_CART.name(),
                CustomerEventType.CHECKOUT_INITIATED.name(),
                CustomerEventType.ORDER_COMPLETED.name()
        };
        String[] labels = {"1. Product Views", "2. Added to Bag", "3. Checkout Initiated", "4. Orders Completed"};

        List<BehavioralFunnelStageDto> list = new ArrayList<>();
        long topStageSessions = 0;
        long previousStageSessions = 0;

        for (int i = 0; i < stages.length; i++) {
            String stage = stages[i];
            long total = customerEventRepository.countByEventTypeAndCreatedAtBetween(stage, start, end);
            long uniqueSessions = customerEventRepository.countDistinctSessionsByEventTypeBetween(stage, start, end);

            if (i == 0) {
                topStageSessions = uniqueSessions;
                previousStageSessions = uniqueSessions;
            }

            double fromPrev = previousStageSessions > 0
                    ? Math.min(100.0, ((double) uniqueSessions / previousStageSessions) * 100.0)
                    : (uniqueSessions > 0 ? 100.0 : 0.0);

            double overall = topStageSessions > 0
                    ? Math.min(100.0, ((double) uniqueSessions / topStageSessions) * 100.0)
                    : (uniqueSessions > 0 ? 100.0 : 0.0);

            list.add(BehavioralFunnelStageDto.builder()
                    .stage(stage)
                    .label(labels[i])
                    .totalEvents(total)
                    .uniqueSessions(uniqueSessions)
                    .conversionRateFromPrevious(BigDecimal.valueOf(fromPrev).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .overallConversionRate(BigDecimal.valueOf(overall).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .build());

            previousStageSessions = uniqueSessions;
        }

        return list;
    }

    private List<BehavioralFunnelStageDto> buildEcommerceFunnel(LocalDateTime start, LocalDateTime end) {
        String[] stages = {
                CustomerEventType.LANDING_PAGE_VIEW.name(),
                CustomerEventType.PRODUCT_VIEW.name(),
                CustomerEventType.SEARCH_QUERY.name(),
                CustomerEventType.CATEGORY_VIEW.name(),
                CustomerEventType.ADD_TO_CART.name(),
                CustomerEventType.CHECKOUT_INITIATED.name(),
                CustomerEventType.PAYMENT_ATTEMPT.name(),
                CustomerEventType.ORDER_COMPLETED.name()
        };
        String[] labels = {
                "1. Landing",
                "2. Product View",
                "3. Search Discovery",
                "4. Category Browse",
                "5. Added to Bag",
                "6. Checkout Initiated",
                "7. Payment Step",
                "8. Purchase Completed"
        };

        List<BehavioralFunnelStageDto> list = new ArrayList<>();
        long topStageSessions = 0;
        long previousStageSessions = 0;

        for (int i = 0; i < stages.length; i++) {
            String stage = stages[i];
            long total = customerEventRepository.countByEventTypeAndCreatedAtBetween(stage, start, end);
            long uniqueSessions = customerEventRepository.countDistinctSessionsByEventTypeBetween(stage, start, end);

            if (i == 0) {
                topStageSessions = uniqueSessions;
                previousStageSessions = uniqueSessions;
            }

            double fromPrev = previousStageSessions > 0
                    ? Math.min(100.0, ((double) uniqueSessions / previousStageSessions) * 100.0)
                    : (uniqueSessions > 0 ? 100.0 : 0.0);

            double overall = topStageSessions > 0
                    ? Math.min(100.0, ((double) uniqueSessions / topStageSessions) * 100.0)
                    : (uniqueSessions > 0 ? 100.0 : 0.0);

            list.add(BehavioralFunnelStageDto.builder()
                    .stage(stage)
                    .label(labels[i])
                    .totalEvents(total)
                    .uniqueSessions(uniqueSessions)
                    .conversionRateFromPrevious(roundOneDecimal(fromPrev))
                    .overallConversionRate(roundOneDecimal(overall))
                    .build());

            previousStageSessions = uniqueSessions;
        }

        return list;
    }

    private Map<String, Long> computeChannelEngagement(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> engagement = new LinkedHashMap<>();
        engagement.put("wishlist", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.ADD_TO_WISHLIST.name(), start, end));
        engagement.put("recommendations", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.RECOMMENDATION_CLICK.name(), start, end));
        engagement.put("aiStylist", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.AI_STYLIST_ENGAGE.name(), start, end));
        engagement.put("whatsapp", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.WHATSAPP_COMMERCE_ENGAGE.name(), start, end));
        engagement.put("trousseau", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.TROUSSEAU_ENGAGE.name(), start, end));
        engagement.put("shareLinks", customerEventRepository.countByEventTypeAndCreatedAtBetween(CustomerEventType.SHARE_LINK_ENGAGE.name(), start, end));
        return engagement;
    }

    private Map<String, Double> computeFunnelMetrics(List<BehavioralFunnelStageDto> stages) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        if (stages == null || stages.size() < 8) {
            return metrics;
        }

        long landingSessions = stages.get(0).getUniqueSessions();
        long viewSessions = stages.get(1).getUniqueSessions();
        long cartSessions = stages.get(4).getUniqueSessions();
        long checkoutSessions = stages.get(5).getUniqueSessions();
        long paymentSessions = stages.get(6).getUniqueSessions();
        long orderSessions = stages.get(7).getUniqueSessions();

        double overallConversion = landingSessions > 0 ? ((double) orderSessions / landingSessions) * 100.0 : 0.0;
        double detailToCart = viewSessions > 0 ? ((double) cartSessions / viewSessions) * 100.0 : 0.0;
        double cartToCheckout = cartSessions > 0 ? ((double) checkoutSessions / cartSessions) * 100.0 : 0.0;
        double checkoutToPayment = checkoutSessions > 0 ? ((double) paymentSessions / checkoutSessions) * 100.0 : 0.0;
        double paymentToOrder = paymentSessions > 0 ? ((double) orderSessions / paymentSessions) * 100.0 : 0.0;
        double cartAbandonment = cartSessions > 0 ? Math.max(0.0, (1.0 - ((double) orderSessions / cartSessions)) * 100.0) : 0.0;
        double checkoutAbandonment = checkoutSessions > 0 ? Math.max(0.0, (1.0 - ((double) orderSessions / checkoutSessions)) * 100.0) : 0.0;

        metrics.put("overallConversionRate", roundOneDecimal(overallConversion));
        metrics.put("detailToCartRate", roundOneDecimal(detailToCart));
        metrics.put("cartToCheckoutRate", roundOneDecimal(cartToCheckout));
        metrics.put("checkoutToPaymentRate", roundOneDecimal(checkoutToPayment));
        metrics.put("paymentToOrderRate", roundOneDecimal(paymentToOrder));
        metrics.put("cartAbandonmentRate", roundOneDecimal(cartAbandonment));
        metrics.put("checkoutAbandonmentRate", roundOneDecimal(checkoutAbandonment));
        return metrics;
    }

    private double roundOneDecimal(double val) {
        return BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, val))).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private List<TopTrendingProductDto> computeTopTrendingProducts(List<CustomerEvent> events) {
        Map<Long, Long> viewsByProd = new HashMap<>();
        Map<Long, Long> cartAddsByProd = new HashMap<>();

        for (CustomerEvent event : events) {
            Long prodId = event.getEntityId();
            if (prodId == null) continue;

            if (CustomerEventType.PRODUCT_VIEW.name().equals(event.getEventType())) {
                viewsByProd.merge(prodId, 1L, Long::sum);
            } else if (CustomerEventType.ADD_TO_CART.name().equals(event.getEventType())) {
                cartAddsByProd.merge(prodId, 1L, Long::sum);
            }
        }

        List<TopTrendingProductDto> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : viewsByProd.entrySet()) {
            Long prodId = entry.getKey();
            long viewCount = entry.getValue();
            long cartCount = cartAddsByProd.getOrDefault(prodId, 0L);
            double rate = viewCount > 0 ? ((double) cartCount / viewCount) * 100.0 : 0.0;

            String name = "Saree #" + prodId;
            String category = "Heritage Collection";
            Optional<Product> pOpt = productRepository.findById(prodId);
            if (pOpt.isPresent()) {
                name = pOpt.get().getName();
                category = pOpt.get().getCategory() != null ? pOpt.get().getCategory().getName() : "Handloom Saree";
            }

            result.add(TopTrendingProductDto.builder()
                    .productId(prodId)
                    .productName(name)
                    .category(category)
                    .viewCount(viewCount)
                    .cartAddCount(cartCount)
                    .cartAddRate(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .build());
        }

        result.sort(Comparator.comparing(TopTrendingProductDto::getViewCount).reversed());
        return result.stream().limit(10).collect(Collectors.toList());
    }

    private List<SearchQueryTelemetryDto> computeSearchTelemetry(List<CustomerEvent> events, boolean zeroResultsOnly) {
        Map<String, long[]> queryCounts = new HashMap<>(); // [0] = totalCount, [1] = zeroCount

        for (CustomerEvent event : events) {
            if (!CustomerEventType.SEARCH_QUERY.name().equals(event.getEventType())) continue;

            Map<String, Object> meta = parseMetadata(event.getMetadata());
            String q = extractString(meta, "query", "q", "searchTerm");
            if (q == null || q.isBlank()) continue;

            String normalized = q.trim().toLowerCase();
            long[] stats = queryCounts.computeIfAbsent(normalized, k -> new long[2]);
            stats[0]++;

            Number resultCount = extractNumber(meta, "resultCount", "results");
            if (resultCount != null && resultCount.intValue() == 0) {
                stats[1]++;
            }
        }

        List<SearchQueryTelemetryDto> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : queryCounts.entrySet()) {
            long total = entry.getValue()[0];
            long zero = entry.getValue()[1];
            if (zeroResultsOnly && zero == 0) continue;

            double rate = total > 0 ? ((double) zero / total) * 100.0 : 0.0;

            result.add(SearchQueryTelemetryDto.builder()
                    .query(entry.getKey())
                    .count(total)
                    .zeroResultCount(zero)
                    .zeroResultRate(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                    .build());
        }

        result.sort(zeroResultsOnly
                ? Comparator.comparing(SearchQueryTelemetryDto::getZeroResultCount).reversed()
                : Comparator.comparing(SearchQueryTelemetryDto::getCount).reversed());

        return result.stream().limit(15).collect(Collectors.toList());
    }

    private String extractString(Map<String, Object> meta, String... keys) {
        for (String k : keys) {
            Object val = meta.get(k);
            if (val instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private Number extractNumber(Map<String, Object> meta, String... keys) {
        for (String k : keys) {
            Object val = meta.get(k);
            if (val instanceof Number n) return n;
            if (val instanceof String s) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
