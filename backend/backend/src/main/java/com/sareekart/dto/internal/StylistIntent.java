package com.sareekart.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal structured intent extracted from patron conversational input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistIntent {

    public enum QueryType {
        FIND_SAREE,
        BLOUSE_PAIRING,
        SIMILAR_CHEAPER,
        SIMILAR_LUXURY,
        DRAPING_ADVICE,
        GENERAL_STYLE
    }

    @Builder.Default
    private QueryType queryType = QueryType.FIND_SAREE;

    private String occasion;
    private String preferredFabric;
    private String preferredColor;
    private String colorFamily;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String skinUndertone;
    private Long referenceProductId;
    private List<String> stylingKeywords;
}
