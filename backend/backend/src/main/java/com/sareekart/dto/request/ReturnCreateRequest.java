package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCreateRequest {

    @NotNull(message = "Order ID is mandatory")
    private Long orderId;

    @NotBlank(message = "Return type is mandatory (RETURN or EXCHANGE)")
    private String type; // RETURN, EXCHANGE

    @NotBlank(message = "Reason is mandatory")
    private String reason; // COLOR_MISMATCH, ZARI_DEFECT, FABRIC_FEEL, INCORRECT_ITEM, SIZE_MISMATCH, OTHER

    private String comments;

    @NotBlank(message = "Refund mode is mandatory (ORIGINAL_PAYMENT, STORE_CREDIT, EXCHANGE_DRAPE)")
    private String refundMode;

    private String exchangeSku; // Mandatory if type == "EXCHANGE"

    private BigDecimal refundAmount; // Optional: defaults to order total if omitted

    @Size(max = 3, message = "Maximum 3 defect photos allowed")
    private List<String> images; // List of static URL strings, e.g. ["/uploads/return-photos/defect-uuid.jpg"]
}
