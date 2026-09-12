package com.sareekart.dto.whatsapp;

import com.sareekart.enums.WhatsAppEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppNotificationResponse {
    private Long id;
    private Long orderId;
    private Long returnRequestId;
    private Long userId;
    private String recipientName;
    private String recipientPhone;
    private WhatsAppEventType eventType;
    private String templateName;
    private String messageContent;
    private String trackingNumber;
    private String courierPartner;
    private String trackingUrl;
    private String deliveryStatus;
    private Boolean simulated;
    private String createdAtFormatted;
}
