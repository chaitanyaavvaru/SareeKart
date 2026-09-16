package com.sareekart.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Validates the Meta WhatsApp Webhook HMAC-SHA256 signature (X-Hub-Signature-256).
 */
@Component
@Slf4j
public class WhatsAppWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    @Value("${whatsapp.webhook.app-secret:sareekart-meta-secret-2026}")
    private String appSecret;

    /**
     * Validates that the received X-Hub-Signature-256 header matches HMAC-SHA256(payload, appSecret).
     *
     * @param payloadBytes Raw request body bytes
     * @param signatureHeader Received X-Hub-Signature-256 header (e.g. "sha256=...")
     * @return true if signature matches or if dev mode fallback applies, false if signature is forged
     */
    public boolean isValid(byte[] payloadBytes, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            // Allow dev/test requests where signature header is absent if secret is default test key
            if ("sareekart-meta-secret-2026".equals(appSecret) || appSecret == null || appSecret.isBlank()) {
                log.debug("Signature header missing, allowing in development mode");
                return true;
            }
            log.warn("Missing or invalid X-Hub-Signature-256 header format: {}", signatureHeader);
            return false;
        }

        if (payloadBytes == null) {
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
                log.warn("Meta Webhook signature mismatch! Calculated: {}, Received: {}", calculatedHash, receivedHash);
            }

            return matches;
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 signature for webhook", e);
            return false;
        }
    }
}
