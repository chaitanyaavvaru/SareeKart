package com.sareekart.service.impl;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.dto.trousseau.TrousseauAiCurationRequest;
import com.sareekart.dto.trousseau.TrousseauAiCurationResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.TrousseauBoardRepository;
import com.sareekart.repository.TrousseauCeremonyRepository;
import com.sareekart.service.StylistGroundingService;
import com.sareekart.service.TrousseauAiCurationService;
import com.sareekart.specification.ProductSpecification;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrousseauAiCurationServiceImpl implements TrousseauAiCurationService {

    private final TrousseauBoardRepository boardRepository;
    private final TrousseauCeremonyRepository ceremonyRepository;
    private final ProductRepository productRepository;
    private final StylistGroundingService stylistGroundingService;

    @Autowired(required = false)
    private ChatClient chatClient;

    private static final Pattern SAREE_TAG_PATTERN = Pattern.compile("\\[SAREE-(\\d+)\\]");

    // Prompt injection filter pattern
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?previous\\s+instructions|disregard\\s+instructions|system\\s*prompt|system\\s*:|assistant\\s*:|\\[INST\\]|<<SYS>>|<script[^>]*>.*?</script>|javascript:|drop\\s+table|delete\\s+from|bypass\\s+rules|reveal\\s+prompt|show\\s+secret)",
            Pattern.CASE_INSENSITIVE
    );

    private final ExecutorService aiCurationExecutor = Executors.newFixedThreadPool(4);

    @PreDestroy
    public void cleanup() {
        try {
            aiCurationExecutor.shutdown();
            if (!aiCurationExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                aiCurationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiCurationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public TrousseauAiCurationResponse curateCeremonyEnsemble(
            Long userId,
            Long boardId,
            Long ceremonyId,
            TrousseauAiCurationRequest request) {

        if (request == null) {
            request = new TrousseauAiCurationRequest();
        }

        // 1. Ownership & Entity Validation
        TrousseauBoard board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau board not found: " + boardId));

        if (board.getUser() == null || !board.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this trousseau board");
        }

        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau board is archived or inactive");
        }

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        // 2. Resolve Context
        String ceremonyType = resolveCeremonyType(request.getCeremonyType(), ceremony.getCeremonyType());
        String colorTheme = request.getColorTheme() != null && !request.getColorTheme().isBlank()
                ? request.getColorTheme().trim()
                : (ceremony.getColorTheme() != null ? ceremony.getColorTheme() : getDefaultColorTheme(ceremonyType));

        BigDecimal budget = request.getMaxBudget() != null
                ? request.getMaxBudget()
                : ceremony.getTargetBudget();

        String preferredFabric = request.getPreferredFabric() != null && !request.getPreferredFabric().isBlank()
                ? request.getPreferredFabric().trim()
                : null;

        String sanitizedPreferences = sanitizePromptInput(request.getUserPreferences());

        // Exclude products already shortlisted in this ceremony
        Set<Long> existingProductIds = ceremony.getItems().stream()
                .filter(item -> item.getProduct() != null)
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        // 3. Gate 1: Pre-generation Candidate Grounding from MySQL
        List<Product> groundedCandidates = retrieveGate1Candidates(
                ceremonyType,
                colorTheme,
                preferredFabric,
                budget,
                existingProductIds,
                userId,
                request.getLimit() != null ? request.getLimit() : 6
        );

        Map<Long, Product> candidateMap = groundedCandidates.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));

        // Styling presets for this ceremony
        CeremonyStylingPreset preset = getCeremonyPreset(ceremonyType);

        boolean fallbackUsed = false;
        List<Long> selectedProductIds = new ArrayList<>();
        String overallStylingNote = preset.defaultStylingNote;

        // 4. AI Reasoning with 3,500ms SLA Timeout
        if (chatClient != null && !groundedCandidates.isEmpty()) {
            try {
                final TrousseauAiCurationRequest finalReq = request;
                Future<String> future = aiCurationExecutor.submit(() ->
                        callChatClientForCuration(ceremonyType, colorTheme, budget, sanitizedPreferences, groundedCandidates, preset)
                );

                String rawLlmResponse = future.get(3500, TimeUnit.MILLISECONDS);

                // Gate 2: Post-generation Validation against candidate whitelist
                selectedProductIds = validateAndFilterLlmCitations(rawLlmResponse, candidateMap.keySet());
                if (selectedProductIds.isEmpty()) {
                    log.warn("LLM did not produce valid candidate citations; falling back to deterministic ranking");
                    fallbackUsed = true;
                }
            } catch (TimeoutException te) {
                log.warn("AI Curation 3,500ms SLA exceeded; falling back to deterministic ensemble");
                fallbackUsed = true;
            } catch (Exception e) {
                log.warn("AI Curation LLM unavailable ({}); falling back to deterministic ensemble", e.getMessage());
                fallbackUsed = true;
            }
        } else {
            fallbackUsed = true;
        }

        // 5. Fallback selection if AI failed or produced no valid citations
        if (fallbackUsed || selectedProductIds.isEmpty()) {
            selectedProductIds = new ArrayList<>(candidateMap.keySet());
            fallbackUsed = true;
        }

        // 6. Build Catalog-Grounded Recommendations (strictly hydrated from MySQL)
        int targetLimit = request.getLimit() != null ? Math.min(request.getLimit(), 10) : 6;
        List<TrousseauAiCurationResponse.EnsembleRecommendation> ensembleRecommendations = new ArrayList<>();

        int rank = 0;
        for (Long pid : selectedProductIds) {
            Product p = candidateMap.get(pid);
            if (p == null) {
                // Secondary check: ensure unknown product never sneaks into response
                continue;
            }
            if (ensembleRecommendations.size() >= targetLimit) {
                break;
            }

            double confidence = Math.max(0.70, 0.98 - (rank * 0.05));
            String primaryImage = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0) : null;
            String fabric = p.getFabricEntity() != null ? p.getFabricEntity().getName() : p.getFabric();
            String color = p.getColorEntity() != null ? p.getColorEntity().getName() : p.getColor();

            String reasoning = String.format(
                    "Selected for %s ceremony: %s drape in authentic %s, harmonizing with %s theme.",
                    ceremonyType,
                    p.getName(),
                    fabric != null ? fabric : "handloom silk",
                    colorTheme != null ? colorTheme : "ceremonial"
            );

            ensembleRecommendations.add(TrousseauAiCurationResponse.EnsembleRecommendation.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productPrice(p.getPrice())
                    .productImageUrl(primaryImage)
                    .fabric(fabric)
                    .color(color)
                    .stockQuantity(p.getStockQuantity())
                    .matchConfidence(confidence)
                    .stylingReasoning(reasoning)
                    .contrastBlouse(preset.contrastBlouse)
                    .jewelryPairing(preset.jewelry)
                    .drapeStyle(preset.drapeStyle)
                    .build());

            rank++;
        }

        log.info("AI Curation generated {} recommendations for ceremony {} (board={}, fallbackUsed={})",
                ensembleRecommendations.size(), ceremonyId, boardId, fallbackUsed);

        return TrousseauAiCurationResponse.builder()
                .boardId(boardId)
                .ceremonyId(ceremonyId)
                .ceremonyType(ceremonyType)
                .colorTheme(colorTheme)
                .ceremonyBudget(budget)
                .overallStylingNote(overallStylingNote)
                .recommendations(ensembleRecommendations)
                .fallbackUsed(fallbackUsed)
                .totalCandidatesEvaluated(groundedCandidates.size())
                .build();
    }

    @Override
    public String sanitizePromptInput(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Remove known prompt-injection markers
        String sanitized = INJECTION_PATTERN.matcher(input).replaceAll("[FILTERED]");
        // Strip control characters, quotes that could break delimiters
        sanitized = sanitized.replace("`", "'").replace("\"", "'");
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }
        return sanitized.trim();
    }

    // =========================================================================
    // Gate 1: Pre-Generation Candidate Retrieval from MySQL
    // =========================================================================

    private List<Product> retrieveGate1Candidates(
            String ceremonyType,
            String colorTheme,
            String preferredFabric,
            BigDecimal maxBudget,
            Set<Long> excludeProductIds,
            Long userId,
            int limit) {

        int fetchLimit = Math.max(limit * 2, 12);
        List<Product> candidates = new ArrayList<>();

        // 1. Try Phase 9 StylistGroundingService with Occasion intent
        try {
            StylistIntent intent = StylistIntent.builder()
                    .occasion(mapCeremonyToOccasion(ceremonyType))
                    .preferredFabric(preferredFabric)
                    .preferredColor(extractPrimaryColor(colorTheme))
                    .colorFamily(extractPrimaryColor(colorTheme))
                    .maxPrice(maxBudget)
                    .queryType(StylistIntent.QueryType.FIND_SAREE)
                    .build();

            List<ScoredProductResponse> scored = stylistGroundingService.retrieveGroundedCandidates(intent, null, userId, fetchLimit);
            if (scored != null) {
                for (ScoredProductResponse spr : scored) {
                    if (spr.getProduct() != null && spr.getProduct().getId() != null) {
                        Long pid = spr.getProduct().getId();
                        if (!excludeProductIds.contains(pid)) {
                            productRepository.findById(pid).ifPresent(candidates::add);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("StylistGroundingService lookup encountered exception; falling back to direct specification: {}", e.getMessage());
        }

        // 2. If insufficient candidates, query MySQL via ProductSpecification with strict active & in-stock
        if (candidates.size() < limit) {
            ProductSpecification spec = new ProductSpecification(
                    null,
                    null,
                    null,
                    preferredFabric,
                    null,
                    mapCeremonyToOccasion(ceremonyType),
                    null,
                    extractPrimaryColor(colorTheme),
                    null,
                    null,
                    maxBudget,
                    true // strictly inStock
            );

            List<Product> specMatches = productRepository.findAll(spec, PageRequest.of(0, fetchLimit)).getContent();
            for (Product p : specMatches) {
                if (p.getActive() != null && p.getActive()
                        && p.getStockQuantity() != null && p.getStockQuantity() > 0
                        && !excludeProductIds.contains(p.getId())
                        && (maxBudget == null || p.getPrice().compareTo(maxBudget) <= 0)
                        && !candidates.contains(p)) {
                    candidates.add(p);
                }
            }
        }

        // 3. Fallback: Any active in-stock sarees within budget
        if (candidates.isEmpty()) {
            ProductSpecification priceSpec = new ProductSpecification(
                    null, null, null, null, null, null, null, null, null,
                    null, maxBudget, true
            );
            List<Product> budgetProducts = productRepository.findAll(priceSpec, PageRequest.of(0, fetchLimit)).getContent();
            for (Product p : budgetProducts) {
                if (p.getActive() != null && p.getActive()
                        && p.getStockQuantity() != null && p.getStockQuantity() > 0
                        && !excludeProductIds.contains(p.getId())) {
                    candidates.add(p);
                }
            }
        }

        // Final sanity check: ensure strictly active and in-stock and within budget
        return candidates.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .filter(p -> maxBudget == null || p.getPrice().compareTo(maxBudget) <= 0)
                .distinct()
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Gate 2: Post-Generation Hallucination Sanitization
    // =========================================================================

    private List<Long> validateAndFilterLlmCitations(String rawLlmResponse, Set<Long> candidateWhitelist) {
        if (rawLlmResponse == null || rawLlmResponse.isBlank()) {
            return Collections.emptyList();
        }

        List<Long> validCitations = new ArrayList<>();
        Matcher matcher = SAREE_TAG_PATTERN.matcher(rawLlmResponse);

        while (matcher.find()) {
            try {
                Long citedId = Long.parseLong(matcher.group(1));
                if (candidateWhitelist.contains(citedId)) {
                    if (!validCitations.contains(citedId)) {
                        validCitations.add(citedId);
                    }
                } else {
                    log.warn("Gate 2 detected hallucinated product ID [SAREE-{}]; rejected from ensemble", citedId);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return validCitations;
    }

    private String callChatClientForCuration(
            String ceremonyType,
            String colorTheme,
            BigDecimal budget,
            String sanitizedPreferences,
            List<Product> candidates,
            CeremonyStylingPreset preset) {

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are the master AI Luxury Bridal Trousseau Stylist for SareeKart.\n");
        systemPrompt.append("Your task is to curate the ideal bridal saree ensemble for the ceremony: ").append(ceremonyType).append(".\n\n");
        systemPrompt.append("STRICT GROUNDING CONSTRAINTS:\n");
        systemPrompt.append("1. You must ONLY recommend sarees from the VERIFIED CANDIDATE LIST below.\n");
        systemPrompt.append("2. Cite each recommended saree using the exact format [SAREE-ID] with its numeric ID (e.g. [SAREE-101]).\n");
        systemPrompt.append("3. NEVER invent product IDs, prices, fabrics, or catalog details.\n");
        systemPrompt.append("4. Contrast blouse styling: ").append(preset.contrastBlouse).append(".\n");
        systemPrompt.append("5. Jewelry recommendation: ").append(preset.jewelry).append(".\n");
        systemPrompt.append("6. Drape style: ").append(preset.drapeStyle).append(".\n\n");
        systemPrompt.append("VERIFIED CANDIDATE LIST:\n");

        for (Product p : candidates) {
            String fabric = p.getFabricEntity() != null ? p.getFabricEntity().getName() : p.getFabric();
            String color = p.getColorEntity() != null ? p.getColorEntity().getName() : p.getColor();
            systemPrompt.append("- [SAREE-").append(p.getId()).append("]: \"").append(p.getName())
                    .append("\", Fabric: ").append(fabric)
                    .append(", Color: ").append(color)
                    .append(", Price: ₹").append(p.getPrice())
                    .append(", Stock: ").append(p.getStockQuantity()).append("\n");
        }

        StringBuilder userMessage = new StringBuilder();
        userMessage.append("Please curate the best bridal sarees for ").append(ceremonyType)
                .append(" with color theme '").append(colorTheme).append("'");
        if (budget != null) {
            userMessage.append(" and target budget of ₹").append(budget);
        }
        userMessage.append(".\n");

        if (sanitizedPreferences != null && !sanitizedPreferences.isBlank()) {
            userMessage.append("Patron preference notes (untrusted user input): '")
                    .append(sanitizedPreferences).append("'.\n");
        }

        return chatClient.prompt()
                .system(systemPrompt.toString())
                .user(userMessage.toString())
                .call()
                .content();
    }

    // =========================================================================
    // Ceremony Taxonomy & Aesthetic Presets
    // =========================================================================

    private String resolveCeremonyType(String reqType, String ceremonyType) {
        if (reqType != null && !reqType.isBlank()) {
            return reqType.trim().toUpperCase();
        }
        if (ceremonyType != null && !ceremonyType.isBlank()) {
            return ceremonyType.trim().toUpperCase();
        }
        return "MUHURTHAM";
    }

    private String mapCeremonyToOccasion(String ceremonyType) {
        if (ceremonyType == null) return "Bridal / Wedding Festivities";
        switch (ceremonyType.toUpperCase()) {
            case "HALDI":
                return "Festive & Wedding Celebration";
            case "SANGEET":
                return "Party & Cocktail";
            case "ENGAGEMENT":
                return "Festive & Wedding Celebration";
            case "RECEPTION":
                return "Evening & Reception";
            case "MUHURTHAM":
            case "WEDDING":
            default:
                return "Bridal / Wedding Festivities";
        }
    }

    private String extractPrimaryColor(String colorTheme) {
        if (colorTheme == null || colorTheme.isBlank()) {
            return null;
        }
        String lower = colorTheme.toLowerCase();
        if (lower.contains("yellow") || lower.contains("haldi") || lower.contains("mustard")) return "Yellow";
        if (lower.contains("red") || lower.contains("crimson") || lower.contains("maroon")) return "Red";
        if (lower.contains("pink") || lower.contains("peach") || lower.contains("rose")) return "Pink";
        if (lower.contains("green") || lower.contains("emerald") || lower.contains("mint")) return "Green";
        if (lower.contains("blue") || lower.contains("navy") || lower.contains("royal")) return "Blue";
        if (lower.contains("gold")) return "Gold";
        if (lower.contains("purple") || lower.contains("wine")) return "Purple";
        return colorTheme;
    }

    private String getDefaultColorTheme(String ceremonyType) {
        switch (ceremonyType) {
            case "HALDI":
                return "Yellow & Gold";
            case "SANGEET":
                return "Royal Blue & Emerald";
            case "ENGAGEMENT":
                return "Pastel Peach & Rose";
            case "RECEPTION":
                return "Regal Wine & Metallic Gold";
            case "MUHURTHAM":
            default:
                return "Crimson & Temple Gold";
        }
    }

    private CeremonyStylingPreset getCeremonyPreset(String ceremonyType) {
        switch (ceremonyType) {
            case "HALDI":
                return new CeremonyStylingPreset(
                        "Radiant, sunlit yellow curation featuring lightweight drapes designed for joyful floral ceremonies.",
                        "Tangerine Orange or Hot Pink embroidered sweetheart blouse",
                        "Handcrafted floral jewelry with fresh marigolds and yellow pearls",
                        "Fluid pleated drape with secured pallu for festive ease"
                );
            case "SANGEET":
                return new CeremonyStylingPreset(
                        "Glamorous and dynamic evening curation designed for movement, dance festivities, and radiant celebration.",
                        "Mirror-work or sequined halter-neck blouse in royal jewel tones",
                        "Polki and uncut diamond statement choker with chandelier earrings",
                        "Contemporary mermaid drape or pre-pleated pallu for dance fluidity"
                );
            case "ENGAGEMENT":
                return new CeremonyStylingPreset(
                        "Romantic pastel and modern heirloom curation radiating delicate elegance and understated luxury.",
                        "Sheer net or organza blouse with tone-on-tone pearl and threadwork",
                        "Rose gold Kundan or Basra pearl multi-strand rani haar",
                        "Soft flowing open pallu with contemporary waist cincher"
                );
            case "RECEPTION":
                return new CeremonyStylingPreset(
                        "Grand haute couture evening curation featuring opulent metallic accents, rich brocades, and regal silhouettes.",
                        "High-neck velvet or metallic brocade blouse with embellished cuffs",
                        "Navratan or Polki bib necklace with emerald drop accents",
                        "Royal regal drape with extended sweeping pallu"
                );
            case "MUHURTHAM":
            default:
                return new CeremonyStylingPreset(
                        "Auspicious wedding curation celebrating sacred traditions, heavy mulberry silks, and pure gold zari weaves.",
                        "Emerald Green or Deep Purple raw silk with intricate zardozi embroidery",
                        "Antique Temple Gold choker, guttapusalu necklace, and jhumkas",
                        "Traditional South Indian Nivi drape with pleated pallu"
                );
        }
    }

    private static class CeremonyStylingPreset {
        final String defaultStylingNote;
        final String contrastBlouse;
        final String jewelry;
        final String drapeStyle;

        CeremonyStylingPreset(String defaultStylingNote, String contrastBlouse, String jewelry, String drapeStyle) {
            this.defaultStylingNote = defaultStylingNote;
            this.contrastBlouse = contrastBlouse;
            this.jewelry = jewelry;
            this.drapeStyle = drapeStyle;
        }
    }
}
