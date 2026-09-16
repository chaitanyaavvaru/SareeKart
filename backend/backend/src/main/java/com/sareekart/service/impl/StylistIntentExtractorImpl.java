package com.sareekart.service.impl;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.service.StylistIntentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-speed pattern-based heuristic extractor for SareeKart styling intent.
 */
@Service
@Slf4j
public class StylistIntentExtractorImpl implements StylistIntentExtractor {

    // Regex patterns for budget detection
    private static final Pattern BETWEEN_BUDGET_PATTERN = Pattern.compile(
            "(?:between|from)?\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)(k)?\\s*(?:and|to|-)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)(k)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern UNDER_BUDGET_PATTERN = Pattern.compile(
            "(?:under|below|less than|within|budget of|max|upto)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d+)*)(k)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ABOVE_BUDGET_PATTERN = Pattern.compile(
            "(?:above|more than|exceeding|minimum|min)\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d+)*)(k)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXACT_PRICE_PATTERN = Pattern.compile(
            "(?:rs\\.?|inr|₹)\\s*(\\d+(?:,\\d+)*)(k)?",
            Pattern.CASE_INSENSITIVE);

    @Override
    public StylistIntent extractIntent(String message, StylistChatRequest request) {
        String raw = (message != null ? message : "").toLowerCase();
        List<String> keywords = new ArrayList<>();

        // 1. Detect Query Type
        StylistIntent.QueryType queryType = determineQueryType(raw, request);

        // 2. Extract Budget Range
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        Matcher betweenMatcher = BETWEEN_BUDGET_PATTERN.matcher(raw);
        if (betweenMatcher.find()) {
            minPrice = parseNumberWithK(betweenMatcher.group(1), betweenMatcher.group(2) != null);
            maxPrice = parseNumberWithK(betweenMatcher.group(3), betweenMatcher.group(4) != null);
        } else {
            Matcher underMatcher = UNDER_BUDGET_PATTERN.matcher(raw);
            if (underMatcher.find()) {
                maxPrice = parseNumberWithK(underMatcher.group(1).replace(",", ""), underMatcher.group(2) != null);
            } else {
                Matcher exactMatcher = EXACT_PRICE_PATTERN.matcher(raw);
                if (exactMatcher.find()) {
                    maxPrice = parseNumberWithK(exactMatcher.group(1).replace(",", ""), exactMatcher.group(2) != null);
                }
            }

            Matcher aboveMatcher = ABOVE_BUDGET_PATTERN.matcher(raw);
            if (aboveMatcher.find()) {
                minPrice = parseNumberWithK(aboveMatcher.group(1).replace(",", ""), aboveMatcher.group(2) != null);
            }
        }

        // Check fallback from request chip if not in text
        if (maxPrice == null && minPrice == null && request != null && request.getBudgetRange() != null) {
            String bRange = request.getBudgetRange().toUpperCase();
            if ("UNDER_15K".equals(bRange)) {
                maxPrice = BigDecimal.valueOf(15000);
            } else if ("15K_TO_30K".equals(bRange)) {
                minPrice = BigDecimal.valueOf(15000);
                maxPrice = BigDecimal.valueOf(30000);
            } else if ("LUXURY_30K_PLUS".equals(bRange)) {
                minPrice = BigDecimal.valueOf(30000);
            }
        }

        // 3. Extract Occasion
        String occasion = determineOccasion(raw);
        if (occasion == null && request != null && request.getOccasion() != null && !request.getOccasion().isBlank()) {
            occasion = request.getOccasion();
        }
        if (occasion != null) {
            keywords.add(occasion);
        }

        // 4. Extract Fabric / Weave
        String fabric = determineFabric(raw);
        if (fabric == null && request != null && request.getPreferredWeave() != null 
                && !"ANY".equalsIgnoreCase(request.getPreferredWeave())) {
            fabric = request.getPreferredWeave();
        }
        if (fabric != null) {
            keywords.add(fabric);
        }

        // 5. Extract Color & Color Family
        String color = determineColor(raw);
        String colorFamily = determineColorFamily(color != null ? color : raw);
        if (color != null) {
            keywords.add(color);
        }

        // 6. Extract Skin Undertone
        String skinUndertone = determineSkinUndertone(raw);
        if (skinUndertone == null && request != null && request.getSkinUndertone() != null) {
            skinUndertone = request.getSkinUndertone();
        }

        // 7. Reference Product ID
        Long refId = request != null ? request.getReferenceProductId() : null;

        log.debug("Extracted StylistIntent: queryType={}, occasion={}, fabric={}, color={}, min={}, max={}",
                queryType, occasion, fabric, color, minPrice, maxPrice);

        return StylistIntent.builder()
                .queryType(queryType)
                .occasion(occasion)
                .preferredFabric(fabric)
                .preferredColor(color)
                .colorFamily(colorFamily)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .skinUndertone(skinUndertone)
                .referenceProductId(refId)
                .stylingKeywords(keywords)
                .build();
    }

    private StylistIntent.QueryType determineQueryType(String raw, StylistChatRequest request) {
        if (raw.contains("similar but cheaper") || raw.contains("cheaper alternative") 
                || raw.contains("more affordable") || raw.contains("less expensive") 
                || (raw.contains("cheaper") && request != null && request.getReferenceProductId() != null)) {
            return StylistIntent.QueryType.SIMILAR_CHEAPER;
        }
        if (raw.contains("more luxurious") || raw.contains("heirloom alternative") 
                || raw.contains("premium version") || raw.contains("higher end")
                || raw.contains("more grand")) {
            return StylistIntent.QueryType.SIMILAR_LUXURY;
        }
        if (raw.contains("contrast blouse") || raw.contains("blouse pairing") 
                || raw.contains("what blouse") || raw.contains("blouse style") 
                || raw.contains("neckline") || raw.contains("blouse color")) {
            return StylistIntent.QueryType.BLOUSE_PAIRING;
        }
        if (raw.contains("how to drape") || raw.contains("draping style") 
                || raw.contains("pleat") || raw.contains("pallu style") 
                || raw.contains("draping technique")) {
            return StylistIntent.QueryType.DRAPING_ADVICE;
        }
        if (raw.contains("hello") || raw.contains("hi") || raw.contains("namaste") 
                || raw.contains("help me style") || raw.contains("who are you")) {
            if (!raw.contains("saree") && !raw.contains("wedding") && !raw.contains("silk")) {
                return StylistIntent.QueryType.GENERAL_STYLE;
            }
        }
        return StylistIntent.QueryType.FIND_SAREE;
    }

    private String determineOccasion(String raw) {
        if (raw.contains("wedding") || raw.contains("bridal") || raw.contains("muhurtham") 
                || raw.contains("shaadi") || raw.contains("bride")) {
            return "Wedding";
        }
        if (raw.contains("reception") || raw.contains("sangeet") || raw.contains("cocktail") 
                || raw.contains("mehendi") || raw.contains("haldi")) {
            return "Reception";
        }
        if (raw.contains("temple") || raw.contains("puja") || raw.contains("pooja") 
                || raw.contains("auspicious") || raw.contains("varalakshmi")) {
            return "Temple";
        }
        if (raw.contains("festive") || raw.contains("diwali") || raw.contains("navratri") 
                || raw.contains("pongal") || raw.contains("onam") || raw.contains("dussehra")) {
            return "Festive";
        }
        if (raw.contains("gala") || raw.contains("dinner") || raw.contains("farewell") 
                || raw.contains("formal")) {
            return "Gala";
        }
        return null;
    }

    private String determineFabric(String raw) {
        if (raw.contains("kanchipuram") || raw.contains("kanjivaram") || raw.contains("kanchi")) {
            return "Kanchipuram Silk";
        }
        if (raw.contains("banarasi") || raw.contains("banaras")) {
            return "Banarasi Silk";
        }
        if (raw.contains("chanderi")) {
            return "Chanderi Silk";
        }
        if (raw.contains("paithani")) {
            return "Paithani Silk";
        }
        if (raw.contains("organza") || raw.contains("tissue organza")) {
            return "Tissue Organza";
        }
        if (raw.contains("tussar")) {
            return "Tussar Silk";
        }
        if (raw.contains("chiffon")) {
            return "Chiffon";
        }
        if (raw.contains("georgette")) {
            return "Georgette";
        }
        if (raw.contains("cotton")) {
            return "Cotton";
        }
        if (raw.contains("silk")) {
            return "Pure Silk";
        }
        return null;
    }

    private String determineColor(String raw) {
        String[] colors = {
                "maroon", "crimson", "vermilion", "ruby", "red",
                "magenta", "rani", "rose", "blush", "pink",
                "emerald", "olive", "mint", "bottle green", "green",
                "navy", "teal", "royal blue", "sky blue", "indigo", "blue",
                "mustard", "gold", "ochre", "haldi", "yellow",
                "plum", "violet", "lavender", "lilac", "purple",
                "ivory", "cream", "off white", "beige", "white",
                "black"
        };
        for (String c : colors) {
            if (raw.contains(c)) {
                return c;
            }
        }
        return null;
    }

    private String determineColorFamily(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        if (lower.contains("red") || lower.contains("maroon") || lower.contains("crimson") || lower.contains("vermilion") || lower.contains("ruby")) {
            return "Red";
        }
        if (lower.contains("pink") || lower.contains("magenta") || lower.contains("rani") || lower.contains("rose") || lower.contains("blush")) {
            return "Pink";
        }
        if (lower.contains("green") || lower.contains("emerald") || lower.contains("olive") || lower.contains("mint")) {
            return "Green";
        }
        if (lower.contains("blue") || lower.contains("navy") || lower.contains("teal") || lower.contains("indigo")) {
            return "Blue";
        }
        if (lower.contains("yellow") || lower.contains("mustard") || lower.contains("gold") || lower.contains("ochre")) {
            return "Gold";
        }
        if (lower.contains("purple") || lower.contains("plum") || lower.contains("violet") || lower.contains("lavender")) {
            return "Purple";
        }
        if (lower.contains("white") || lower.contains("ivory") || lower.contains("cream") || lower.contains("beige")) {
            return "White";
        }
        return null;
    }

    private String determineSkinUndertone(String raw) {
        if (raw.contains("warm undertone") || raw.contains("warm tone") || raw.contains("golden undertone")) {
            return "WARM";
        }
        if (raw.contains("cool undertone") || raw.contains("cool tone") || raw.contains("pink undertone")) {
            return "COOL";
        }
        if (raw.contains("jewel") || raw.contains("neutral undertone") || raw.contains("neutral tone")) {
            return "JEWEL";
        }
        return null;
    }

    private BigDecimal parseNumberWithK(String digits, boolean hasK) {
        try {
            long val = Long.parseLong(digits);
            if (hasK || val < 100) { // e.g. "15k" or someone typed "15" meaning 15k
                val = val * 1000;
            }
            return BigDecimal.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
