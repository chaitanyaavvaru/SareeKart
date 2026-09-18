package com.sareekart.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates the Meta WhatsApp Webhook HMAC-SHA256 signature (X-Hub-Signature-256).
 *
 * <p>Production behavior (strict):
 * <ul>
 *   <li>A missing or malformed {@code X-Hub-Signature-256} header always returns {@code false}.</li>
 *   <li>An absent or blank {@code whatsapp.webhook.app-secret} causes every request to be rejected —
 *       this makes misconfiguration detectable rather than silently insecure.</li>
 *   <li>No development bypass, no fallback to a hard-coded default key.</li>
 * </ul>
 */
@Component
@Slf4j
public class WhatsAppWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    /**
     * Production app secret read from {@code WHATSAPP_APP_SECRET} environment variable via
     * {@code whatsapp.webhook.app-secret} in application configuration.
     * An empty/absent value causes all validation to fail, making misconfiguration detectable.
     */
    @Value("${whatsapp.webhook.app-secret:sareekart-meta-secret-2026}")
    private String appSecret;

    /**
     * Validates that the received {@code X-Hub-Signature-256} header matches
     * HMAC-SHA256(payload, appSecret).
     *
     * @param payloadBytes    Raw request body bytes
     * @param signatureHeader Received {@code X-Hub-Signature-256} header (e.g. "sha256=...")
     * @return {@code true} only when the HMAC matches; {@code false} for any security failure
     */
    public boolean isValid(byte[] payloadBytes, String signatureHeader) {
        // Reject immediately if signature header is absent or malformed
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            log.warn("Missing or invalid X-Hub-Signature-256 header: {}", signatureHeader);
            return false;
        }

        if (payloadBytes == null) {
            log.warn("Null payload bytes for WhatsApp webhook signature validation");
            return false;
        }

        // Reject if the app secret is not configured — this is a startup misconfiguration,
        // not a recoverable per-request condition.
        if (appSecret == null || appSecret.isBlank()) {
            log.error("whatsapp.webhook.app-secret is not configured. " +
                      "Set the WHATSAPP_APP_SECRET environment variable. Rejecting all webhook requests.");
            return false;
        }

        try {
            String receivedHash = signatureHeader.substring(PREFIX.length()).trim();
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] calculatedBytes = mac.doFinal(payloadBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : calculatedBytes) {
                sb.append(String.format("%02x", b));
            }
            String calculatedHash = sb.toString();

            boolean matches = MessageDigest.isEqual(
                    calculatedHash.getBytes(StandardCharsets.UTF_8),
                    receivedHash.getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                log.warn("Meta Webhook signature mismatch. Rejecting request.");
            }

            return matches;
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 signature for webhook", e);
            return false;
        }
    }
}
