package com.sareekart.client;

import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.dto.payment.RazorpayRefundResponse;

/**
 * Outbound port to the Razorpay Orders API.
 *
 * Interface exists so PaymentService is unit-testable without network I/O;
 * {@link RazorpayClient} is the production adapter. Amounts are in rupees
 * (BigDecimal, 2dp); conversion to paise happens at the adapter boundary.
 */
public interface RazorpayGateway {

    /**
     * Creates a Razorpay order.
     *
     * @param receiptOrderId SareeKart order id — sent as `receipt` for
     *                       reconciliation and Razorpay-side dedup
     * @param amountRupees   authoritative total from the stored SareeKart order
     * @param currency       ISO-4217 code, e.g. INR
     */
    RazorpayOrderResponse createOrder(String receiptOrderId, long amountRupees, String currency);

    /**
     * Creates a refund against a captured gateway payment.
     *
     * @param razorpayPaymentId gateway payment identifier (from our stored
     *                          providerPaymentId — never client-supplied)
     * @param amountPaise       refund amount in paise; validated server-side
     */
    RazorpayRefundResponse createRefund(String razorpayPaymentId, long amountPaise);

    /**
     * Authoritative refund lookup for reconciliation.
     * empty()  → gateway does not know this id (terminal mismatch signal)
     * throws   → temporary network/5xx failure (safe to retry later)
     */
    java.util.Optional<RazorpayRefundResponse> fetchRefund(String providerRefundId);
}