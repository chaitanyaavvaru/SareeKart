package com.sareekart.service.impl;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.ConsultationQuizRequest;
import com.sareekart.dto.request.DrapeStyleRequest;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.dto.response.AiStylistTelemetryResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.dto.response.StylistChatResponse;
import com.sareekart.entity.AiStyleConsultation;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.AiStyleConsultationRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.AiStylistService;
import com.sareekart.service.StylistGroundingService;
import com.sareekart.service.StylistIntentExtractor;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiStylistServiceImpl implements AiStylistService {

    private final AiStyleConsultationRepository consultationRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final StylistIntentExtractor stylistIntentExtractor;
    private final StylistGroundingService stylistGroundingService;

    @Autowired(required = false)
    private ChatClient chatClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
    private static final Pattern SAREE_TAG_PATTERN = Pattern.compile("\\[SAREE-(\\d+)\\]");

    private final ExecutorService stylistExecutor = Executors.newFixedThreadPool(4);

    @PreDestroy
    public void cleanup() {
        try {
            stylistExecutor.shutdown();
            if (!stylistExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                stylistExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            stylistExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public DrapeStyleResponse generateDrapeStyling(DrapeStyleRequest request, User user) {
        log.info("Generating AI luxury drape styling for saree: {}", request.getSareeName());

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId()).orElse(null);
        }

        String fabric = request.getFabric() != null && !request.getFabric().isBlank() 
                ? request.getFabric() 
                : (product != null && product.getFabric() != null ? product.getFabric() : "Pure Mulberry Silk");

        String primaryColor = request.getPrimaryColor() != null && !request.getPrimaryColor().isBlank()
                ? request.getPrimaryColor()
                : (product != null && product.getColor() != null ? product.getColor() : "Maroon");

        String occasion = request.getOccasion() != null && !request.getOccasion().isBlank()
                ? request.getOccasion()
                : "Bridal / Wedding Festivities";

        // Generate 3 curated styling ensembles
        List<DrapeStyleResponse.EnsembleLook> looks = buildCuratedLooks(fabric, primaryColor, occasion, request.getZariType());

        DrapeStyleResponse.EnsembleLook primaryLook = looks.get(0);

        // Record consultation session in database
        AiStyleConsultation consultation = AiStyleConsultation.builder()
                .user(user)
                .product(product)
                .sareeName(request.getSareeName())
                .fabric(fabric)
                .primaryColor(primaryColor)
                .occasion(occasion)
                .chosenLookTitle(primaryLook.getTitle())
                .contrastColor(primaryLook.getBlouse().getContrastColor())
                .blouseStyle(primaryLook.getBlouse().getBlouseStyle())
                .jewelryRecommendation(primaryLook.getJewelry().getCategory())
                .convertedToTailoring(false)
                .build();

        AiStyleConsultation saved = consultationRepository.save(consultation);

        return DrapeStyleResponse.builder()
                .consultationId(saved.getId())
                .sareeId(product != null ? product.getId() : request.getProductId())
                .sareeName(request.getSareeName())
                .fabric(fabric)
                .primaryColor(primaryColor)
                .occasion(occasion)
                .curatedLooks(looks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> consultStyleQuiz(ConsultationQuizRequest request) {
        log.info("Executing AI style consultation quiz for occasion: {}, undertone: {}", 
                request.getOccasion(), request.getSkinUndertone());

        List<Product> activeProducts = productRepository.findByActiveTrue(PageRequest.of(0, 50)).getContent();
        if (activeProducts.isEmpty()) {
            return Collections.emptyList();
        }

        String preferredWeave = request.getPreferredWeave() != null ? request.getPreferredWeave().toUpperCase() : "ANY";

        List<Product> matched = activeProducts.stream()
                .filter(p -> {
                    boolean weaveMatches = preferredWeave.equals("ANY") ||
                            (p.getFabric() != null && p.getFabric().toUpperCase().contains(preferredWeave)) ||
                            (p.getName() != null && p.getName().toUpperCase().contains(preferredWeave));
                    return weaveMatches;
                })
                .filter(p -> {
                    if (request.getBudgetRange() == null || request.getBudgetRange().isBlank() || request.getBudgetRange().equals("ANY")) {
                        return true;
                    }
                    BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                    if (request.getBudgetRange().equalsIgnoreCase("UNDER_15K")) {
                        return price.compareTo(BigDecimal.valueOf(15000)) <= 0;
                    } else if (request.getBudgetRange().equalsIgnoreCase("15K_TO_30K")) {
                        return price.compareTo(BigDecimal.valueOf(15000)) > 0 && price.compareTo(BigDecimal.valueOf(30000)) <= 0;
                    } else if (request.getBudgetRange().equalsIgnoreCase("LUXURY_30K_PLUS")) {
                        return price.compareTo(BigDecimal.valueOf(30000)) > 0;
                    }
                    return true;
                })
                .limit(6)
                .collect(Collectors.toList());

        // Fallback if strict filter yields zero
        if (matched.isEmpty()) {
            matched = activeProducts.stream().limit(4).collect(Collectors.toList());
        }

        return matched.stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public StylistChatResponse chatWithStylist(StylistChatRequest request, User user) {
        log.info("Patron initiated AI Stylist conversation: query='{}', session='{}'", 
                request.getMessage(), request.getSessionId());

        // Step 1: Extract structured shopping and styling intent
        StylistIntent intent = stylistIntentExtractor.extractIntent(request.getMessage(), request);

        // Step 2: Gate 1 Authoritative MySQL Grounding (Pre-retrieval candidate pool)
        List<ScoredProductResponse> candidates = stylistGroundingService.retrieveGroundedCandidates(
                intent,
                request.getSessionId(),
                user != null ? user.getId() : null,
                6
        );

        // Determine primary product context
        Product primaryProduct = null;
        if (request.getReferenceProductId() != null) {
            primaryProduct = productRepository.findByIdAndActiveTrue(request.getReferenceProductId()).orElse(null);
        }
        if (primaryProduct == null && !candidates.isEmpty()) {
            primaryProduct = productRepository.findById(candidates.get(0).getProduct().getId()).orElse(null);
        }

        String fabric = intent.getPreferredFabric() != null 
                ? intent.getPreferredFabric() 
                : (primaryProduct != null && primaryProduct.getFabric() != null ? primaryProduct.getFabric() : "Pure Mulberry Silk");

        String primaryColor = intent.getPreferredColor() != null 
                ? intent.getPreferredColor() 
                : (primaryProduct != null && primaryProduct.getColor() != null ? primaryProduct.getColor() : "Maroon");

        String occasion = intent.getOccasion() != null 
                ? intent.getOccasion() 
                : "Festive & Wedding Celebration";

        // Build deterministic curated ensembles
        List<DrapeStyleResponse.EnsembleLook> looks = buildCuratedLooks(fabric, primaryColor, occasion, "Pure Gold Zari");
        DrapeStyleResponse.EnsembleLook primaryLook = looks.get(0);

        String replyText = null;
        boolean fallbackUsed = false;

        // Step 3: LLM Styling Reasoning with 1,500ms SLA Timeout
        if (chatClient != null && !candidates.isEmpty()) {
            try {
                Future<String> future = stylistExecutor.submit(() -> callChatClient(request.getMessage(), candidates, primaryLook, occasion));
                replyText = future.get(1500, TimeUnit.MILLISECONDS);

                // Gate 2: Post-Generation Hallucination Guard
                replyText = validateAndSanitizeLlmResponse(replyText, candidates);
            } catch (TimeoutException te) {
                log.warn("AI Stylist LLM SLA timeout exceeded (>1,500ms); falling back to curated looks");
                fallbackUsed = true;
            } catch (Exception e) {
                log.warn("AI Stylist LLM unavailable ({}); falling back to curated looks", e.getMessage());
                fallbackUsed = true;
            }
        } else {
            fallbackUsed = true;
        }

        // Step 4: Deterministic Fallback if LLM timed out or failed
        if (fallbackUsed || replyText == null || replyText.isBlank()) {
            replyText = buildFallbackReply(intent, primaryLook, candidates, primaryProduct, occasion);
            fallbackUsed = true;
        }

        // Step 5: Save consultation record for Bespoke Tailoring Studio continuity
        AiStyleConsultation consultation = AiStyleConsultation.builder()
                .user(user)
                .product(primaryProduct)
                .sareeName(primaryProduct != null ? primaryProduct.getName() : "Artisanal Silk Saree")
                .fabric(fabric)
                .primaryColor(primaryColor)
                .occasion(occasion)
                .chosenLookTitle(primaryLook.getTitle())
                .contrastColor(primaryLook.getBlouse().getContrastColor())
                .blouseStyle(primaryLook.getBlouse().getBlouseStyle())
                .jewelryRecommendation(primaryLook.getJewelry().getCategory())
                .convertedToTailoring(false)
                .build();

        AiStyleConsultation saved = consultationRepository.save(consultation);

        String detectedBudget = intent.getMaxPrice() != null ? "Under ₹" + intent.getMaxPrice().toPlainString() : null;

        return StylistChatResponse.builder()
                .reply(replyText)
                .recommendedSarees(candidates)
                .primaryLook(primaryLook)
                .consultationId(saved.getId())
                .fallbackUsed(fallbackUsed)
                .detectedOccasion(intent.getOccasion())
                .detectedFabric(intent.getPreferredFabric())
                .detectedBudget(detectedBudget)
                .build();
    }

    private String callChatClient(String userMessage, List<ScoredProductResponse> candidates, DrapeStyleResponse.EnsembleLook look, String occasion) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the master AI Luxury Saree Stylist and Drape Concierge for SareeKart, an artisanal Indian haute couture atelier.\n");
        sb.append("Your duty is to advise the patron with cultural grace, handloom sophistication, and impeccable color harmony.\n\n");
        sb.append("MANDATORY GROUNDING RULES:\n");
        sb.append("1. You must ONLY recommend sarees from the VERIFIED CANDIDATE LIST below. Cite each chosen saree as [SAREE-{id}].\n");
        sb.append("2. NEVER invent a saree ID, fabric, or price not in the list.\n");
        sb.append("3. Incorporate the contrast blouse coordinates: Color '").append(look.getBlouse().getContrastColor())
                .append("', Fabric '").append(look.getBlouse().getFabric())
                .append("', Neckline '").append(look.getBlouse().getFrontNeck()).append("'.\n");
        sb.append("4. Recommend jewelry: '").append(look.getJewelry().getCategory()).append("'.\n");
        sb.append("5. Mention the drape technique: '").append(look.getDrapingTechnique()).append("'.\n\n");
        sb.append("VERIFIED CANDIDATE LIST:\n");

        for (ScoredProductResponse c : candidates) {
            ProductResponse p = c.getProduct();
            sb.append("- [SAREE-").append(p.getId()).append("]: \"").append(p.getName())
                    .append("\", Fabric: ").append(p.getFabric())
                    .append(", Color: ").append(p.getColor())
                    .append(", Price: ₹").append(p.getPrice()).append("\n");
        }

        return chatClient.prompt()
                .system(sb.toString())
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Gate 2 Post-Processing: Validates that all [SAREE-id] tags in LLM output match the verified candidates whitelist.
     * Strips any hallucinated product IDs.
     */
    private String validateAndSanitizeLlmResponse(String rawResponse, List<ScoredProductResponse> candidates) {
        if (rawResponse == null) return "";
        Set<Long> allowedIds = candidates.stream()
                .map(c -> c.getProduct().getId())
                .collect(Collectors.toSet());

        Matcher matcher = SAREE_TAG_PATTERN.matcher(rawResponse);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            try {
                Long citedId = Long.parseLong(matcher.group(1));
                if (allowedIds.contains(citedId)) {
                    matcher.appendReplacement(sb, matcher.group(0)); // Keep valid
                } else {
                    // Hallucinated ID detected! Strip citation tag to preserve truthfulness
                    log.warn("Dual-Gate Guard detected hallucinated product citation [SAREE-{}]; removing from text", citedId);
                    matcher.appendReplacement(sb, "");
                }
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String buildFallbackReply(StylistIntent intent, DrapeStyleResponse.EnsembleLook look, 
                                      List<ScoredProductResponse> candidates, Product primaryProduct, String occasion) {
        StringBuilder sb = new StringBuilder();
        String sareeTitle = primaryProduct != null ? primaryProduct.getName() : "Artisanal Heritage Saree";
        String sareeTag = primaryProduct != null ? " [SAREE-" + primaryProduct.getId() + "]" : "";

        sb.append("Namaste! For your **").append(occasion).append("**, our atelier concierge recommends pairing the **")
                .append(sareeTitle).append("**").append(sareeTag).append(" with an exquisite **")
                .append(look.getBlouse().getContrastColor()).append("** blouse crafted from ")
                .append(look.getBlouse().getFabric()).append(".\n\n");

        sb.append("### ✨ Haute Couture Stylist Ensemble\n");
        sb.append("• **Contrast Blouse**: ").append(look.getBlouse().getContrastColor())
                .append(" (").append(look.getBlouse().getFrontNeck()).append(" neckline, ")
                .append(look.getBlouse().getSleeve()).append(" sleeve) with ").append(look.getBlouse().getRecommendedWork()).append("\n");
        sb.append("• **Jewelry Coordination**: ").append(look.getJewelry().getCategory()).append(" (").append(look.getJewelry().getNecklace()).append(")\n");
        sb.append("• **Draping Technique**: ").append(look.getDrapingTechnique()).append("\n");
        sb.append("• **Styling Rationale**: ").append(look.getStylingRationale()).append("\n\n");

        if (candidates.size() > 1) {
            sb.append("We have also curated ").append(candidates.size()).append(" authentic in-stock handloom masterpieces from our catalog matching your preferences below. You can customize the blouse directly in our **Bespoke Tailoring Studio**!");
        } else {
            sb.append("You can customize this blouse directly with our master karigars in the **Bespoke Tailoring Studio** below!");
        }

        return sb.toString();
    }

    @Override
    public void recordTailoringConversion(Long consultationId) {
        if (consultationId == null) return;
        consultationRepository.findById(consultationId).ifPresent(c -> {
            c.setConvertedToTailoring(true);
            consultationRepository.save(c);
            log.info("Attributed tailoring conversion to AI Consultation ID: {}", consultationId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AiStylistTelemetryResponse getTelemetry() {
        long total = consultationRepository.count();
        long converted = consultationRepository.countByConvertedToTailoringTrue();
        double conversionRate = total > 0 ? ((double) converted / total) * 100.0 : 0.0;

        List<Map<String, Object>> occasionList = new ArrayList<>();
        for (Object[] row : consultationRepository.findOccasionFrequencies()) {
            occasionList.add(Map.of("occasion", row[0] != null ? row[0] : "General", "count", row[1]));
        }

        List<Map<String, Object>> styledSareesList = new ArrayList<>();
        for (Object[] row : consultationRepository.findTopStyledSarees()) {
            styledSareesList.add(Map.of("sareeName", row[0] != null ? row[0] : "Heritage Saree", "count", row[1]));
        }

        List<AiStyleConsultation> recent = consultationRepository.findTop10ByOrderByCreatedAtDesc();
        List<AiStylistTelemetryResponse.ConsultationSummary> recentSummaries = recent.stream()
                .map(c -> AiStylistTelemetryResponse.ConsultationSummary.builder()
                        .id(c.getId())
                        .sareeName(c.getSareeName())
                        .occasion(c.getOccasion())
                        .chosenLookTitle(c.getChosenLookTitle())
                        .contrastColor(c.getContrastColor())
                        .blouseStyle(c.getBlouseStyle())
                        .convertedToTailoring(Boolean.TRUE.equals(c.getConvertedToTailoring()))
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMATTER) : "Recently")
                        .build())
                .collect(Collectors.toList());

        return AiStylistTelemetryResponse.builder()
                .totalConsultations(total)
                .convertedToTailoringCount(converted)
                .tailoringConversionRatePercent(Math.round(conversionRate * 10.0) / 10.0)
                .topOccasions(occasionList)
                .topStyledSarees(styledSareesList)
                .recentConsultations(recentSummaries)
                .build();
    }

    private List<DrapeStyleResponse.EnsembleLook> buildCuratedLooks(String fabric, String color, String occasion, String zariType) {
        String c = color.toLowerCase();
        boolean isRedMaroon = c.contains("red") || c.contains("maroon") || c.contains("crimson") || c.contains("vermilion");
        boolean isPinkMagenta = c.contains("pink") || c.contains("magenta") || c.contains("rani") || c.contains("rose");
        boolean isGreen = c.contains("green") || c.contains("emerald") || c.contains("olive") || c.contains("mint");
        boolean isBlueTeal = c.contains("blue") || c.contains("navy") || c.contains("teal") || c.contains("indigo");
        boolean isYellowGold = c.contains("yellow") || c.contains("mustard") || c.contains("gold") || c.contains("ochre");

        List<DrapeStyleResponse.EnsembleLook> looks = new ArrayList<>();

        // 1. Traditional Regal Heritage Look
        String heritageContrast;
        String heritageHex;
        String heritageWork;
        if (isRedMaroon) {
            heritageContrast = "Peacock Emerald Green";
            heritageHex = "#0E5B4B";
            heritageWork = "Heavy Korvai Zari Border with Hand-Zardosi floral jaal";
        } else if (isPinkMagenta) {
            heritageContrast = "Temple Mustard Gold";
            heritageHex = "#D4AF37";
            heritageWork = "Brocade weave with authentic Ganda Berunda motifs";
        } else if (isGreen) {
            heritageContrast = "Royal Vermilion Red";
            heritageHex = "#9E1B1E";
            heritageWork = "Intricate bullion knot embroidery with vintage gold thread";
        } else if (isBlueTeal) {
            heritageContrast = "Sunset Tangerine Orange";
            heritageHex = "#D9531E";
            heritageWork = "Cutwork zardosi border with antique coin dori piping";
        } else if (isYellowGold) {
            heritageContrast = "Deep Royal Violet";
            heritageHex = "#4A154B";
            heritageWork = "Zari booti work with traditional Korvai temple spires";
        } else {
            heritageContrast = "Imperial Ruby Red";
            heritageHex = "#800020";
            heritageWork = "Hand-embroidered Aari work with pure metallic zari";
        }

        looks.add(DrapeStyleResponse.EnsembleLook.builder()
                .id("look-heritage")
                .title("Royal Heritage Grandeur")
                .subtitle("Time-Honored Korvai & Temple Aristocracy")
                .description("A majestic, high-contrast tribute to South Indian royal court drapes. Pairs the saree with an opulent contrast blouse and antique Nakshi jewelry.")
                .blouse(DrapeStyleResponse.BlouseRecommendation.builder()
                        .fabric("Pure Raw Silk Brocade")
                        .contrastColor(heritageContrast)
                        .colorHex(heritageHex)
                        .frontNeck("Sweetheart")
                        .backNeck("Deep U with Dori")
                        .sleeve("Elbow Length (Traditional)")
                        .blouseStyle("designer")
                        .recommendedWork(heritageWork)
                        .build())
                .jewelry(DrapeStyleResponse.JewelryRecommendation.builder()
                        .category("Temple Nakshi Antique Gold")
                        .necklace("22k handcrafted Nakshi collar with Goddess Lakshmi pendant and uncut rubies")
                        .earrings("Karanphool Jhumkas with freshwater pearl cluster drops")
                        .bangles("Pachisi Antique Kada with embossed lion-head finials")
                        .build())
                .accents(DrapeStyleResponse.AccentRecommendation.builder()
                        .hairFlorals("Classic polished low chignon adorned with double Madurai jasmine gajra")
                        .footwear("Heirloom gold metallic Kolhapuri slip-ons with cushioned arch")
                        .potliBag("Velvet drawstring potli with pearl-tasseled drawstrings")
                        .build())
                .drapingTechnique("Traditional Nivi Drape with 6-inch architectural pallu pleats pinned gracefully over the shoulder.")
                .stylingRationale("Classical Indian color theory dictates that rich warm silks find their highest regal balance when grounded by a contrasting secondary jewel tone.")
                .build());

        // 2. Contemporary Minimalist Chic Look
        looks.add(DrapeStyleResponse.EnsembleLook.builder()
                .id("look-minimalist")
                .title("Contemporary Minimalist Chic")
                .subtitle("Monochrome Sheer & Understated Modernity")
                .description("Sophisticated, clean-lined styling for gala evenings and cocktail sangeets. Celebrates the drape's fluid movement with subtle tonal contrast.")
                .blouse(DrapeStyleResponse.BlouseRecommendation.builder()
                        .fabric("Tissue Organza with Butter Crepe Lining")
                        .contrastColor("Champagne Ivory & Burnished Gold")
                        .colorHex("#E6DEC9")
                        .frontNeck("Boat Neck")
                        .backNeck("Keyhole")
                        .sleeve("Sleeveless")
                        .blouseStyle("tailored")
                        .recommendedWork("Delicate sequin piping along neckline and armholes")
                        .build())
                .jewelry(DrapeStyleResponse.JewelryRecommendation.builder()
                        .category("Uncut Polki & Basra Pearls")
                        .necklace("Delicate single-strand Polki choker with baroque pearl drops")
                        .earrings("Contemporary geometric Polki studs")
                        .bangles("Sleek diamond-cut gold cuffs")
                        .build())
                .accents(DrapeStyleResponse.AccentRecommendation.builder()
                        .hairFlorals("Textured low ponytail with single white orchid pinned at the nape")
                        .footwear("Pointed satin stilettos with micro-crystal strap")
                        .potliBag("Structured mother-of-pearl acrylic minaudière")
                        .build())
                .drapingTechnique("Floating Pallu drape cascading naturally over the forearm without tight pinning, showcasing the lustrous weave fluidity.")
                .stylingRationale("Subtle champagne and tonal tones allow the intricate handloom craftsmanship of the main drape to remain the uncontested focal centerpiece.")
                .build());

        // 3. Festive Fusion Drama Look
        looks.add(DrapeStyleResponse.EnsembleLook.builder()
                .id("look-festive")
                .title("Festive Fusion Drama")
                .subtitle("Celebratory Jewel-Tone Play & Statement Silhouettes")
                .description("Vibrant, head-turning energy tailored for celebratory wedding receptions and festive Diwali evenings.")
                .blouse(DrapeStyleResponse.BlouseRecommendation.builder()
                        .fabric("Banarasi Chanderi Silk")
                        .contrastColor("Vibrant Electric Plum")
                        .colorHex("#6B2D5C")
                        .frontNeck("Deep U")
                        .backNeck("Square Back")
                        .sleeve("3/4th Sleeve")
                        .blouseStyle("designer")
                        .recommendedWork("Scalloped border embroidery with silver-gold multi-metal zari")
                        .build())
                .jewelry(DrapeStyleResponse.JewelryRecommendation.builder()
                        .category("Guttapusalu & Victorian Silver Foil")
                        .necklace("Multi-tiered Guttapusalu necklace fringed with hundreds of seed pearls")
                        .earrings("Statement Chandbali drops with suspended micro-pearls")
                        .bangles("Filigree Jadau bangles with emerald accents")
                        .build())
                .accents(DrapeStyleResponse.AccentRecommendation.builder()
                        .hairFlorals("Voluminous side-swept Dutch braid intertwined with baby's breath and mini jasmine buds")
                        .footwear("Embroidered zardosi block-heel sandals")
                        .potliBag("Sequined silk box clutch with detachable antique gold chain")
                        .build())
                .drapingTechnique("Bengali-inspired front pallu crossover drape or belted dhoti drape with embellished kamarbandh.")
                .stylingRationale("High saturation contrast amplifies festive joy while multi-gem guttapusalu reflects ambient evening candle and chandelier lighting.")
                .build());

        return looks;
    }
}
