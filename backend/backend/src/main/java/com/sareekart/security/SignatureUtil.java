package com.sareekart.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

/**
 * Razorpay signature verification — pure static utility, no Spring lifecycle.
 *
 * Kept free of injected secrets deliberately: webhook/payment secrets are
 * passed per call from validated configuration, so the application can boot
 * in COD-only mode (Razorpay disabled) without any gateway credentials.
 *
 * Comparisons use MessageDigest.isEqual (constant-time) to prevent timing
 * attacks, per Razorpay's own verification guidance.
 */
public final class SignatureUtil {

    private SignatureUtil() {
    }

    /**
     * Verifies a checkout payment signature.
     * HMAC-SHA256 of {@code razorpayOrderId|razorpayPaymentId} keyed with the
     * Key Secret, hex-encoded lowercase — as returned to the browser handler.
     */
    public static boolean verifyPaymentSignature(String razorpayOrderId,
                                                 String razorpayPaymentId,
                                                 String razorpaySignature,
                                                 String keySecret) {
        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId)
                || isBlank(razorpaySignature) || isBlank(keySecret)) {
            return false;
        }
        String expected = hmacSha256Hex(razorpayOrderId + "|" + razorpayPaymentId, keySecret);
        return constantTimeEquals(expected, razorpaySignature);
    }

    /**
     * Verifies a webhook signature: HMAC-SHA256 of the RAW request body keyed
     * with the Webhook Secret, hex-encoded lowercase (X-Razorpay-Signature).
     */
    public static boolean verifyWebhookSignature(String rawPayload,
                                                 String signature,
                                                 String webhookSecret) {
        if (isBlank(rawPayload) || isBlank(signature) || isBlank(webhookSecret)) {
            return false;
        }
        String expected = hmacSha256Hex(rawPayload, webhookSecret);
        return constantTimeEquals(expected, signature);
    }

    /** Hex-encoded lowercase HMAC-SHA256. */
    public static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** Constant-time string comparison; null/length-safe. */
    private static boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) return false;
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            provided.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}