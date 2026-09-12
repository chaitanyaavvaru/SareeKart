package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {

    private Long id;
    private Long orderId;
    private Long userId;

    // Customer profile telemetry
    private String customerEmail;
    private String customerName;
    private String customerMobile;

    // Financial telemetry
    private BigDecimal orderTotalAmount;
    private BigDecimal refundAmount;
    private String refundMode;

    // Claim taxonomy & details
    private String type; // RETURN, EXCHANGE
    private String reason;
    private String comments;
    private String status; // PENDING, APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED
    private String exchangeSku;
    private List<String> images;

    // Reverse logistics tracking
    private String reverseCourier;
    private String reverseTrackingNumber;

    // Staff audit notes
    private String adminNotes;

    // Timestamps & Eligibility metrics
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime orderDeliveredAt;
    private Long daysSinceDelivery;
}
