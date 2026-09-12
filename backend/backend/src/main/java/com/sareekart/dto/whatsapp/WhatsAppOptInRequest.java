package com.sareekart.dto.whatsapp;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppOptInRequest {

    @NotNull(message = "Opt-in status is required")
    private Boolean optIn;
}
