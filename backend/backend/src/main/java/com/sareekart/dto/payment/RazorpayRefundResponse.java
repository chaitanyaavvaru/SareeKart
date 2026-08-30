package com.sareekart.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Razorpay Refund API response (subset). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayRefundResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("entity")
    private String entity;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("speed_requested")
    private String speedRequested;
}
