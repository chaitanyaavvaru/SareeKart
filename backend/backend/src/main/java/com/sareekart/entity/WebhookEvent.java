package com.sareekart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Deduplication ledger for Razorpay webhooks. The unique event_id makes
 * at-least-once webhook delivery idempotent at the storage layer: a repeated
 * delivery violates uk_webhook_events_event_id and is skipped.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "webhook_events", uniqueConstraints = {
    @UniqueConstraint(name = "uk_webhook_events_event_id", columnNames = "event_id")
})
public class WebhookEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @ToString.Exclude
    private Payment payment;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processed_at")
    private java.time.LocalDateTime processedAt;
}