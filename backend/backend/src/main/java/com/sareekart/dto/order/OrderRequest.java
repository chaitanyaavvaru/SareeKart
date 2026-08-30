package com.sareekart.dto.order;

import com.sareekart.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
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

    /** Existing saved address owned by the user. Mutually exclusive with shippingAddress. */
    private Long addressId;

    /** Inline delivery details captured on the checkout form. Persisted as a new address. */
    @Valid
    private AddressInput shippingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String notes;

    /**
     * Optional promotion code. Accepted for forward-compatibility; discount
     * application lands with the Promotions module and is currently ignored.
     */
    private String couponCode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInput {
        private String fullName;
        private String phone;
        private String streetAddress;
        private String landmark;
        private String city;
        private String state;
        private String pincode;
        private String country;
    }
}