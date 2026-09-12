package com.sareekart.service.impl;

import com.sareekart.dto.request.ConsultationQuizRequest;
import com.sareekart.dto.request.DrapeStyleRequest;
import com.sareekart.dto.response.AiStylistTelemetryResponse;
import com.sareekart.dto.response.DrapeStyleResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.AiStyleConsultation;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.AiStyleConsultationRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.AiStylistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AiStylistServiceImpl implements AiStylistService {

    private final AiStyleConsultationRepository consultationRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

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
        String occasion = request.getOccasion() != null ? request.getOccasion().toLowerCase() : "";

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
