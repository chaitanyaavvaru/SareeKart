package com.sareekart.dto.whatsapp;

import com.sareekart.enums.WhatsAppEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppDispatchSimulationRequest {

    private Long orderId;

    private Long returnRequestId;

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhone;

    private String recipientName;

    @NotNull(message = "Event type is required")
    private WhatsAppEventType eventType;

    private String courierPartner;

    private String trackingNumber;

    private String customMessage;
}
