package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationQuizRequest {

    @NotBlank(message = "Occasion is required")
    private String occasion;

    private String skinUndertone; // WARM, COOL, NEUTRAL

    private String preferredWeave; // KANCHIPURAM, BANARASI, PAITHANI, ORGANZA, CHANDERI, ANY

    private String budgetRange; // UNDER_15K, 15K_TO_30K, LUXURY_30K_PLUS
}
