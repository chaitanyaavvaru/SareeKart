package com.sareekart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Refund-reconciliation scheduler configuration.
 *
 * Production-safe defaults: poll every 2 minutes for refunds PENDING longer
 * than 15 minutes, 50 per pass. Retry backoff starts at 60s and doubles to a
 * 1h cap; after {@code maxAttempts} the refund is parked and a
 * REFUND_STUCK_PENDING anomaly is raised for human review.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.refund-reconciliation")
public class RefundReconciliationProperties {

    private boolean enabled = true;

    /** Refunds PENDING longer than this become candidates. */
    private int staleMinutes = 15;

    private int batchSize = 50;

    private long intervalMs = 120_000L;

    private long initialDelayMs = 30_000L;

    /** Poll attempts before parking + raising REFUND_STUCK_PENDING. */
    private int maxAttempts = 10;

    /** Backoff base in seconds: nextRetry = now + min(cap, base × 2^attempt). */
    private long backoffBaseSeconds = 60;

    /** Backoff cap in seconds (1 hour default). */
    private long backoffMaxSeconds = 3600;
}