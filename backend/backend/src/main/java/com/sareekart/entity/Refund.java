package com.sareekart.entity;

import com.sareekart.entity.enums.RefundReasonCode;
import com.sareekart.entity.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * One refund movement against a captured payment.
 *
 * Partial refunds are modelled as multiple rows whose successful amounts must
 * never exceed the captured payment amount (enforced under the payment-row
 * lock in RefundService).
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "refunds", uniqueConstraints = {
    @UniqueConstraint(name = "uk_refunds_provider_refund_id", columnNames = "provider_refund_id")
})
public class Refund extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    @ToString.Exclude
    private Payment payment;

    /** Denormalized for admin queries; kept consistent by service layer. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    /** Razorpay refund id — NULL until the gateway accepts the request. */
    @Column(name = "provider_refund_id", length = 100, unique = true)
    private String providerRefundId;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    /** Structured code; NULL on pre-Phase-10 rows reads as OTHER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 40)
    private RefundReasonCode reasonCode;

    // ── Reconciliation metadata (V13): bounded-backoff bookkeeping ──
    @Column(name = "last_attempt_at")
    private java.time.LocalDateTime lastAttemptAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /** When reconciliation may next poll the gateway; NULL ⇒ immediately due. */
    @Column(name = "next_retry_at")
    private java.time.LocalDateTime nextRetryAt;

    /** Admin identity that initiated (or SYSTEM:RECONCILE for gateway-first). */
    @Column(name = "initiated_by", nullable = false, length = 255)
    private String initiatedBy;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}