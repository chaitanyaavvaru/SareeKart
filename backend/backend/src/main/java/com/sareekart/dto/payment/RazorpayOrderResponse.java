package com.sareekart.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Razorpay Order API response.
 * Maps to https://razorpay.com/docs/api/orders/
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayOrderResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("entity")
    private String entity;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("amount_paid")
    private Long amountPaid;

    @JsonProperty("amount_due")
    private Long amountDue;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("receipt")
    private String receipt;

    @JsonProperty("offer_id")
    private String offerId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("attempts")
    private Integer attempts;

    @JsonProperty("notes")
    private Map<String, String> notes;

    @JsonProperty("created_at")
    private Long createdAt;
}