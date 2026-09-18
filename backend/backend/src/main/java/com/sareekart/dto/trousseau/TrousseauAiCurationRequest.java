package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauAiCurationRequest {

    private String ceremonyType;

    private String colorTheme;

    private BigDecimal maxBudget;

    private String preferredFabric;

    @Size(max = 500, message = "User preferences note must not exceed 500 characters")
    private String userPreferences;

    @Builder.Default
    private Integer limit = 6;
}
