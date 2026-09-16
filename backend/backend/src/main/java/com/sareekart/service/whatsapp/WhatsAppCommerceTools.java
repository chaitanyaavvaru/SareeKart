package com.sareekart.service.whatsapp;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.dto.response.StylistChatResponse;
import com.sareekart.entity.Conversation;
import com.sareekart.entity.ConversationStatus;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.repository.ConversationRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 10: Strongly Typed Commerce Tools for WhatsApp AI Assistant.
 * All operations pass through verified backend service boundaries with strict tenant isolation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCommerceTools {

    private final StylistGroundingService stylistGroundingService;
    private final AiStylistService aiStylistService;
    private final RecommendationService recommendationService;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- Records for Tools ---
    public record CatalogSearchInput(String query, String fabric, String color, String occasion, BigDecimal maxPrice) {}
    public record CatalogSearchItem(Long id, String name, BigDecimal price, String fabric, String color, boolean inStock, String imageUrl, String webUrl) {}
    public record CatalogSearchResult(List<CatalogSearchItem> items, int totalFound) {}

    public record StylingEnsembleInput(Long sareeId, String occasion, String userMessage) {}
    public record StylingEnsembleResult(String sareeName, String contrastColor, String frontNeck, String sleeve, String blouseFabric, String jewelry, String drapeTechnique, Long consultationId, String replyText) {}

    public record StockCheckInput(Long productId) {}
    public record StockCheckResult(Long productId, String name, BigDecimal price, boolean inStock, int availableQuantity) {}

    public record CartActionInput(String action, Long productId, Integer quantity, Long userId) {}
    public record CartActionResult(boolean success, String message, int totalItems, BigDecimal totalAmount) {}

    public record OrderTrackingInput(Long orderId, String trackingNumber, Long userId, String contactPhone) {}
    public record OrderTrackingResult(boolean found, String orderStatus, String trackingNumber, String courierPartner, String estimatedDelivery, String trackingUrl) {}

    public record HumanHandoffInput(Long conversationId, String reason) {}
    public record HumanHandoffResult(boolean escalated, String message) {}

    /**
     * 1. Search Catalog Tool: Invokes Phase 9 Grounding + Phase 8 Hybrid Ranker
     */
    public CatalogSearchResult searchCatalog(CatalogSearchInput input) {
        log.info("WhatsApp Tool: searchCatalog with fabric={}, color={}, occasion={}, maxPrice={}",
                input.fabric(), input.color(), input.occasion(), input.maxPrice());

        StylistIntent intent = StylistIntent.builder()
                .queryType(StylistIntent.QueryType.FIND_SAREE)
                .preferredFabric(input.fabric())
                .preferredColor(input.color())
                .occasion(input.occasion())
                .maxPrice(input.maxPrice())
                .build();

        List<ScoredProductResponse> candidates = stylistGroundingService.retrieveGroundedCandidates(
                intent, "wa_session", null, 4
        );

        List<CatalogSearchItem> items = candidates.stream().map(c -> {
            ProductResponse p = c.getProduct();
            return new CatalogSearchItem(
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    p.getFabric(),
                    p.getColor(),
                    p.getStockQuantity() != null && p.getStockQuantity() > 0,
                    p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : "",
                    "https://sareekart.com/products/" + p.getId()
            );
        }).collect(Collectors.toList());

        return new CatalogSearchResult(items, items.size());
    }

    /**
     * 2. Styling Ensemble Tool: Invokes Phase 9 AI Stylist
     */
    public StylingEnsembleResult getStylingEnsemble(StylingEnsembleInput input, User user) {
        log.info("WhatsApp Tool: getStylingEnsemble for sareeId={}, occasion={}", input.sareeId(), input.occasion());

        StylistChatRequest request = StylistChatRequest.builder()
                .message(input.userMessage() != null ? input.userMessage() : "Style contrast blouse for this saree")
                .referenceProductId(input.sareeId())
                .occasion(input.occasion())
                .build();

        StylistChatResponse stylistResponse = aiStylistService.chatWithStylist(request, user);
        DrapeStyleResponse.EnsembleLook look = stylistResponse.getPrimaryLook();

        String sareeName = (stylistResponse.getRecommendedSarees() != null && !stylistResponse.getRecommendedSarees().isEmpty())
                ? stylistResponse.getRecommendedSarees().get(0).getProduct().getName()
                : "Artisanal Silk Saree";

        return new StylingEnsembleResult(
                sareeName,
                look != null && look.getBlouse() != null ? look.getBlouse().getContrastColor() : "Royal Vermilion Red",
                look != null && look.getBlouse() != null ? look.getBlouse().getFrontNeck() : "Sweetheart Neck",
                look != null && look.getBlouse() != null ? look.getBlouse().getSleeve() : "Elbow Length",
                look != null && look.getBlouse() != null ? look.getBlouse().getFabric() : "Raw Silk Brocade",
                look != null && look.getJewelry() != null ? look.getJewelry().getCategory() : "Temple Nakshi Antique Gold",
                look != null ? look.getDrapingTechnique() : "Traditional Nivi Drape",
                stylistResponse.getConsultationId(),
                stylistResponse.getReply()
        );
    }

    /**
     * 3. Stock Check Tool: Direct MySQL authoritative query
     */
    public StockCheckResult checkAvailability(StockCheckInput input) {
        log.info("WhatsApp Tool: checkAvailability for productId={}", input.productId());

        if (input.productId() == null) {
            return new StockCheckResult(null, "Unknown", BigDecimal.ZERO, false, 0);
        }

        Product product = productRepository.findById(input.productId()).orElse(null);
        if (product == null || !Boolean.TRUE.equals(product.getActive())) {
            return new StockCheckResult(input.productId(), "Unavailable Saree", BigDecimal.ZERO, false, 0);
        }

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        return new StockCheckResult(
                product.getId(),
                product.getName(),
                product.getPrice(),
                stock > 0,
                stock
        );
    }

    /**
     * 4. Cart Management Tool: Invokes Phase 4 CartService
     */
    public CartActionResult manageCart(CartActionInput input) {
        log.info("WhatsApp Tool: manageCart action={}, productId={}, userId={}", input.action(), input.productId(), input.userId());

        if (input.userId() == null) {
            return new CartActionResult(false, "Please link your SareeKart account to view or modify your cart.", 0, BigDecimal.ZERO);
        }

        try {
            String act = input.action() != null ? input.action().toUpperCase() : "VIEW";
            CartResponse cart;

            if ("ADD".equals(act)) {
                if (input.productId() == null) {
                    return new CartActionResult(false, "Product ID is required to add to cart.", 0, BigDecimal.ZERO);
                }
                int qty = input.quantity() != null && input.quantity() > 0 ? input.quantity() : 1;
                cart = cartService.addItemToCart(input.userId(), new CartItemRequest(input.productId(), qty));
                return new CartActionResult(true, "Item added to cart successfully!", cart.getItems() != null ? cart.getItems().size() : 0, cart.getTotalPrice());
            } else if ("REMOVE".equals(act)) {
                cart = cartService.removeItemFromCart(input.userId(), input.productId());
                return new CartActionResult(true, "Item removed from cart.", cart.getItems() != null ? cart.getItems().size() : 0, cart.getTotalPrice());
            } else {
                cart = cartService.getCart(input.userId());
                int count = cart.getItems() != null ? cart.getItems().size() : 0;
                return new CartActionResult(true, "Your cart contains " + count + " items.", count, cart.getTotalPrice());
            }
        } catch (Exception e) {
            log.error("Failed to execute cart tool", e);
            return new CartActionResult(false, e.getMessage(), 0, BigDecimal.ZERO);
        }
    }

    /**
     * 5. Order Tracking Tool: Invokes Phase 5 OrderService
     */
    public OrderTrackingResult trackOrder(OrderTrackingInput input) {
        log.info("WhatsApp Tool: trackOrder orderId={}, trackingNumber={}, userId={}", input.orderId(), input.trackingNumber(), input.userId());

        try {
            OrderResponse order = null;
            if (input.orderId() != null && input.userId() != null) {
                order = orderService.getOrderById(input.orderId(), input.userId());
            } else if (input.orderId() != null || input.trackingNumber() != null) {
                order = orderService.trackOrder(input.orderId(), input.trackingNumber(), input.contactPhone());
            } else if (input.userId() != null) {
                List<OrderResponse> orders = orderService.getOrdersForUser(input.userId());
                if (!orders.isEmpty()) {
                    order = orders.get(0); // Most recent
                }
            }

            if (order == null) {
                return new OrderTrackingResult(false, "NOT_FOUND", null, null, null, null);
            }

            return new OrderTrackingResult(
                    true,
                    order.getStatus(),
                    order.getTrackingNumber() != null ? order.getTrackingNumber() : "Pending AWB",
                    order.getCourierPartner() != null ? order.getCourierPartner() : "BlueDart Logistics",
                    order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate() : "3-5 business days",
                    "https://sareekart.com/track/" + order.getId()
            );
        } catch (Exception e) {
            log.warn("Order tracking lookup failed: {}", e.getMessage());
            return new OrderTrackingResult(false, "UNAUTHORIZED_OR_NOT_FOUND", null, null, null, null);
        }
    }

    /**
     * 6. Human Handoff Tool: Escalates conversation to staff
     */
    public HumanHandoffResult escalateToHuman(HumanHandoffInput input) {
        log.info("WhatsApp Tool: escalateToHuman conversationId={}, reason={}", input.conversationId(), input.reason());

        if (input.conversationId() == null) {
            return new HumanHandoffResult(false, "Conversation ID missing");
        }

        Optional<Conversation> convOpt = conversationRepository.findById(input.conversationId());
        if (convOpt.isPresent()) {
            Conversation conv = convOpt.get();
            conv.setStatus(ConversationStatus.OPEN);
            conv.setTags("ESCALATED_HUMAN_REQUEST");
            conversationRepository.save(conv);

            // Broadcast real-time alert to staff in Admin Dashboard
            messagingTemplate.convertAndSend("/topic/admin/inbox", Map.of(
                    "eventType", "HUMAN_ESCALATION",
                    "conversationId", conv.getId(),
                    "phoneNumber", conv.getContact() != null ? conv.getContact().getPhoneNumber() : "",
                    "reason", input.reason() != null ? input.reason() : "Customer requested human stylist"
            ));

            return new HumanHandoffResult(true, "I have connected you with our boutique team in Bengaluru. A master stylist will join shortly! 🙏");
        }

        return new HumanHandoffResult(false, "Conversation not found");
    }
}
