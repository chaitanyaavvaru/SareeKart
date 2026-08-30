package com.sareekart.entity;

import com.sareekart.entity.enums.AnomalyCode;
import com.sareekart.entity.enums.AnomalySeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Durable operational anomaly for financial-state mismatches that must be
 * visible to admins, not just log lines. Sensitive gateway payloads are
 * never stored — only structured codes and short human detail.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "reconciliation_anomalies")
public class ReconciliationAnomaly extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 60)
    private AnomalyCode code;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AnomalySeverity severity = AnomalySeverity.WARNING;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    @ToString.Exclude
    private Refund refund;

    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "detail", nullable = false, length = 500)
    private String detail;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;
}