package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCeremonyRequest {

    @NotBlank(message = "Ceremony type is required")
    @Size(max = 50, message = "Ceremony type must be under 50 characters")
    @Pattern(
        regexp = "ENGAGEMENT|HALDI|MEHENDI|SANGEET|MUHURTHAM|RECEPTION|OTHER",
        message = "Ceremony type must be ENGAGEMENT, HALDI, MEHENDI, SANGEET, MUHURTHAM, RECEPTION, or OTHER"
    )
    private String ceremonyType;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be under 100 characters")
    private String title;

    @Size(max = 100, message = "Color theme must be under 100 characters")
    private String colorTheme;

    @DecimalMin(value = "0.0", message = "Target budget must be non-negative")
    private BigDecimal targetBudget;

    @Builder.Default
    private Integer displayOrder = 0;
}
