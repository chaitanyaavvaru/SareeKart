package com.sareekart.dto.request;

import jakarta.validation.Valid;
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
public class OrderRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressRequest shippingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String couponCode;

    private java.math.BigDecimal walletDebitAmount;

    private String idempotencyKey;
}
