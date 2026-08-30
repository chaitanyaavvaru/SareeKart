package com.sareekart.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for HMAC-SHA256 signature verification.
 */
class SignatureUtilTest {

    private static final String KEY_SECRET = "test-key-secret-for-unit-tests";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    private String sign(String payload, String secret) {
        return SignatureUtil.hmacSha256Hex(payload, secret);
    }

    // ── Payment signatures ───────────────────────────────────────────────────

    @Test
    @DisplayName("valid payment signature verifies")
    void validPaymentSignature() {
        String orderId = "order_NyaFbYjkk0LiIx";
        String paymentId = "pay_NyaGdSscVdCLTA";
        String goodSig = sign(orderId + "|" + paymentId, KEY_SECRET);

        assertThat(SignatureUtil.verifyPaymentSignature(orderId, paymentId, goodSig, KEY_SECRET))
            .isTrue();
    }

    @Test
    @DisplayName("tampered payment id is rejected")
    void tamperedPaymentIdRejected() {
        String orderId = "order_123";
        String goodSig = sign(orderId + "|" + "pay_real", KEY_SECRET);

        assertThat(SignatureUtil.verifyPaymentSignature(orderId, "pay_forged", goodSig, KEY_SECRET))
            .isFalse();
    }

    @Test
    @DisplayName("signature minted with a different secret is rejected")
    void wrongSecretRejected() {
        String sig = sign("order_1|pay_1", "attacker-secret");
        assertThat(SignatureUtil.verifyPaymentSignature("order_1", "pay_1", sig, KEY_SECRET))
            .isFalse();
    }

    @Test
    @DisplayName("altered order id invalidates signature")
    void alteredOrderIdRejected() {
        String goodSig = sign("order_A|pay_X", KEY_SECRET);
        assertThat(SignatureUtil.verifyPaymentSignature("order_B", "pay_X", goodSig, KEY_SECRET))
            .isFalse();
    }

    @Test
    @DisplayName("null or blank inputs are safely rejected")
    void nullSafe() {
        assertThat(SignatureUtil.verifyPaymentSignature(null, "p", "s", "k")).isFalse();
        assertThat(SignatureUtil.verifyPaymentSignature("o", "", "s", "k")).isFalse();
        assertThat(SignatureUtil.verifyPaymentSignature("o", "p", null, "k")).isFalse();
        assertThat(SignatureUtil.verifyPaymentSignature("o", "p", "s", "")).isFalse();
    }

    // ── Webhook signatures ───────────────────────────────────────────────────

    @Test
    @DisplayName("valid webhook signature over raw body verifies")
    void validWebhookSignature() {
        String body = "{\"event\":\"payment.captured\",\"payload\":{}}";
        String sig = sign(body, WEBHOOK_SECRET);

        assertThat(SignatureUtil.verifyWebhookSignature(body, sig, WEBHOOK_SECRET)).isTrue();
    }

    @Test
    @DisplayName("modified webhook body is rejected even with original signature")
    void modifiedBodyRejected() {
        String body = "{\"event\":\"payment.captured\"}";
        String sig = sign(body, WEBHOOK_SECRET);
        String tampered = "{\"event\":\"refund.processed\"}";

        assertThat(SignatureUtil.verifyWebhookSignature(tampered, sig, WEBHOOK_SECRET)).isFalse();
    }

    @Test
    @DisplayName("webhook signed with key secret instead of webhook secret fails")
    void crossSecretRejected() {
        String body = "{\"event\":\"payment.captured\"}";
        String sig = sign(body, KEY_SECRET);

        assertThat(SignatureUtil.verifyWebhookSignature(body, sig, WEBHOOK_SECRET)).isFalse();
    }
}