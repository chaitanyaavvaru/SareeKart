package com.sareekart.service;

import com.sareekart.entity.ReconciliationAnomaly;
import com.sareekart.entity.enums.AnomalyCode;
import com.sareekart.entity.enums.AnomalySeverity;
import com.sareekart.repository.RefundRepository;
import com.sareekart.repository.ReconciliationAnomalyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable, structured anomaly recorder for payment-operations mismatches.
 *
 * Per-refund anomalies are deduplicated while OPEN (one row per code+refund)
 * so webhook replays and repeated reconciliation passes never spam the ops
 * queue. Anomalies without a refund id (payment-level) are recorded once per
 * pass by the caller's own guard.
 *
 * Transactional semantics: joins the caller's transaction (REQUIRED).
 * Callers hold the payment-row X-lock; the anomaly row's FK checks need an
 * S-lock on that same row, so REQUIRES_NEW here self-deadlocks. Joining also
 * makes anomaly evidence atomic with the recovery it documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyRecorder {

    private final ReconciliationAnomalyRepository repository;
    private final RefundRepository refundRepository;

    @Transactional
    public void record(AnomalyCode code, AnomalySeverity severity,
                       Long orderId, Long paymentId, Long refundId,
                       String providerRefundId, String providerPaymentId,
                       String detail) {
        if (refundId != null
                && repository.countOpenByCodeAndRefund(code, refundId) > 0) {
            log.debug("Anomaly {} already open for refund {} — not duplicated", code, refundId);
            return;
        }
        ReconciliationAnomaly a = new ReconciliationAnomaly();
        a.setCode(code);
        a.setSeverity(severity);
        a.setOrderId(orderId);
        a.setPaymentId(paymentId);
        a.setProviderRefundId(providerRefundId);
        a.setProviderPaymentId(providerPaymentId);
        if (refundId != null) {
            a.setRefund(refundRepository.getReferenceById(refundId));
        }
        a.setDetail(truncate(detail));
        repository.save(a);

        String ref = providerRefundId != null ? providerRefundId
            : providerPaymentId != null ? providerPaymentId : "-";
        // High-severity financial mismatches are always loudly logged (no secrets).
        if (severity == AnomalySeverity.CRITICAL) {
            log.error("ANOMALY {} [{}] refs={} : {}", code, severity, ref, truncate(detail));
        } else {
            log.warn("ANOMALY {} [{}] refs={} : {}", code, severity, ref, truncate(detail));
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}