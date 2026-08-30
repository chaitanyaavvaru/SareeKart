package com.sareekart.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Razorpay gateway configuration.
 *
 * Fail-fast contract:
 *  - enabled=true with ANY missing credential -> IllegalStateException at boot
 *    (production must never start half-configured for money movement).
 *  - enabled=false (COD-only mode) -> no credentials required; payment
 *    endpoints return 503 with an explicit message.
 *
 * Bound from environment variables (see application.yml):
 *   APP_RAZORPAY_ENABLED / RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET /
 *   RAZORPAY_WEBHOOK_SECRET / RAZORPAY_BASE_URL
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.razorpay")
public class RazorpayProperties {

    /** Defaults to false so dev/test boots without gateway keys stay green; docker profile sets it true explicitly. */
    private boolean enabled = false;

    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";
    private String baseUrl = "https://api.razorpay.com/v1";

    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }
        if (isBlank(keyId)) {
            throw new IllegalStateException(
                "Razorpay is enabled but RAZORPAY_KEY_ID is missing. "
                    + "Set it via environment or set APP_RAZORPAY_ENABLED=false.");
        }
        if (isBlank(keySecret)) {
            throw new IllegalStateException(
                "Razorpay is enabled but RAZORPAY_KEY_SECRET is missing.");
        }
        if (isBlank(webhookSecret)) {
            throw new IllegalStateException(
                "Razorpay is enabled but RAZORPAY_WEBHOOK_SECRET is missing.");
        }
    }

    public boolean areCredentialsPresent() {
        return !isBlank(keyId) && !isBlank(keySecret);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}