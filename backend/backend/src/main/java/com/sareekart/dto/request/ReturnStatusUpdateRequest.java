package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatusUpdateRequest {

    @NotBlank(message = "Target status is mandatory (APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED)")
    private String status;

    private String reverseCourier; // Mandatory if status == "PICKUP_SCHEDULED"

    private String reverseTrackingNumber; // Mandatory if status == "PICKUP_SCHEDULED"

    private String adminNotes; // Mandatory if status == "REJECTED", optional otherwise

    private BigDecimal refundAmount; // Optional: allows staff to adjust approved refund amount
}
