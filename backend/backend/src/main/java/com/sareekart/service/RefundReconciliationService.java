package com.sareekart.service;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RefundReconciliationProperties;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Refund;
import com.sareekart.entity.enums.AnomalyCode;
import com.sareekart.entity.enums.AnomalySeverity;
import com.sareekart.entity.enums.RefundStatus;
import com.sareekart.repository.RefundRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reconciles refunds stuck in local PENDING against the gateway's
 * authoritative state.
 *
 * SAFETY MODEL
 * ────────────
 * Candidates: status=PENDING ∧ older than staleMinutes ∧ retry-due.
 * Per-candidate transaction acquires the shared payment-row lock first, so
 * reconciliation can never interleave with verification/webhooks/expiry —
 * identical discipline to every other money transition here.
 *
 * Outcomes per candidate:
 *   gateway processed        → SUCCESS + WEBHOOK_MISSING anomaly (INFO)
 *                              + aggregate recompute (restock exactly once)
 *   gateway failed           → FAILED (financial-only; no restock)
 *   gateway still pending    → attempts/backoff bump; max-attempts parks the
 *                              row and raises REFUND_STUCK_PENDING WARNING
 *   gateway unknown id       → UNKNOWN_REFUND CRITICAL anomaly; parked
 *   amount mismatch          → GATEWAY_LOCAL_STATE_MISMATCH CRITICAL anomaly;
 *                              money state NOT mutated (human review required)
 *   network/5xx error        → temporary: bounded-backoff retry
 *
 * Multi-instance safe: the payment-row lock serializes concurrent workers;
 * the loser observes a terminal state and counts as a no-op.
 */
@Slf4j
@Service
public class RefundReconciliationService {

    private final RefundReconciliationProperties properties;
    private final RefundRepository refundRepository;
    private final ObjectProvider<RazorpayGateway> gatewayProvider;
    private final AnomalyRecorder anomalyRecorder;
    private final RefundService refundService;
    private final ObjectProvider<RefundReconciliationService> self;

    public record ReconcileSummary(int examined, int reconciled, int stillPending,
                                   int failed, int anomalies) {}

    public record ReconcileOutcome(boolean reconciledToSuccess,
                                   boolean reconciledToFailed,
                                   boolean stillPending,
                                   boolean skippedNotPending) {}

    public RefundReconciliationService(RefundReconciliationProperties properties,
                                       RefundRepository refundRepository,
                                       ObjectProvider<RazorpayGateway> gatewayProvider,
                                       RefundService refundService,
                                       AnomalyRecorder anomalyRecorder,
                                       ObjectProvider<RefundReconciliationService> self) {
        this.properties = properties;
        this.refundRepository = refundRepository;
        this.gatewayProvider = gatewayProvider;
        this.refundService = refundService;
        this.anomalyRecorder = anomalyRecorder;
        this.self = self;
    }

    // ── pass orchestration ───────────────────────────────────────────────────

    /** One scheduler/ops pass. Failures are isolated per candidate. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReconcileSummary runOnce() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(properties.getStaleMinutes());
        List<Long> ids = refundRepository.findStalePendingForReconciliation(
                cutoff, LocalDateTime.now(), PageRequest.of(0, properties.getBatchSize()))
            .stream().map(Refund::getId).toList();

        int reconciled = 0, failed = 0, pending = 0;
        for (Long id : ids) {
            try {
                // Through the proxy so each candidate gets its own transaction.
                ReconcileOutcome o = self.getObject().reconcileOne(id);
                if (o.reconciledToSuccess()) reconciled++;
                else if (o.reconciledToFailed()) failed++;
                else if (o.stillPending()) pending++;
            } catch (Exception e) {
                log.error("Reconciliation failed for refund {}: {}", id, e.getMessage());
                pending++;
            }
        }
        if (!ids.isEmpty()) {
            log.info("Refund reconciliation pass: examined={} reconciled={} stillPending={} failed={}",
                ids.size(), reconciled, pending, failed);
        }
        return new ReconcileSummary(ids.size(), reconciled, pending, failed, 0);
    }

    // ── single-refund reconciliation ────────────────────────────────────────

    @Transactional
    public ReconcileOutcome reconcileOne(Long refundId) {
        if (enabledGateway() == null) {
            return new ReconcileOutcome(false, false, true, false);
        }

        // Fresh read, then serialize on the shared payment-row lock.
        Refund refund = refundRepository.findById(refundId).orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.PENDING) {
            return new ReconcileOutcome(false, false, false, true); // webhook/peer won already
        }
        var lockedPaymentOpt = refundRepository.lockPayment(refund.getPayment().getId());
        if (lockedPaymentOpt.isEmpty()) {
            return new ReconcileOutcome(false, false, true, false);
        }
        Payment payment = lockedPaymentOpt.get();

        Refund current = refundRepository.findById(refundId).orElse(null);
        if (current == null || current.getStatus() != RefundStatus.PENDING) {
            return new ReconcileOutcome(false, false, false, true); // lost a race
        }
        refund = current;

        if (refund.getProviderRefundId() == null) {
            // Initiation never reached the gateway; that failure mode belongs
            // to the initiation path, not reconciliation.
            return new ReconcileOutcome(false, false, true, false);
        }

        Optional<RazorpayRefundResponse> fetched;
        try {
            fetched = enabledGateway().fetchRefund(refund.getProviderRefundId());
        } catch (Exception temporary) {
            markTemporaryFailure(refund, temporary.getMessage());
            return new ReconcileOutcome(false, false, true, false);
        }

        if (fetched.isEmpty()) {
            parkWithAnomaly(refund, AnomalyCode.UNKNOWN_REFUND, AnomalySeverity.CRITICAL,
                "Gateway does not recognise locally-PENDING refund id");
            return new ReconcileOutcome(false, false, false, true);
        }

        RazorpayRefundResponse remote = fetched.get();

        // Amount authority: gateway payload must match our persisted row 1:1.
        long remotePaise = remote.getAmount() != null ? remote.getAmount() : -1;
        long localPaise = refund.getAmount().movePointRight(2).longValueExact();
        if (remotePaise >= 0 && remotePaise != localPaise) {
            parkWithAnomaly(refund, AnomalyCode.GATEWAY_LOCAL_STATE_MISMATCH, AnomalySeverity.CRITICAL,
                "Gateway refund amount " + remotePaise + "p ≠ local " + localPaise + "p");
            return new ReconcileOutcome(false, false, false, true);
        }

        String remoteStatus = remote.getStatus() != null ? remote.getStatus().toLowerCase() : "";
        switch (remoteStatus) {
            case "processed" -> {
                markSuccess(refund, payment);
                return new ReconcileOutcome(true, false, false, false);
            }
            case "failed" -> {
                markFailedAtGateway(refund);
                return new ReconcileOutcome(false, true, false, false);
            }
            default -> { // 'pending' or future states
                boolean parked = bumpAttemptAndMaybePark(refund);
                return new ReconcileOutcome(false, false, !parked, false);
            }
        }
    }

    // ── transitions ─────────────────────────────────────────────────────────

    private void markSuccess(Refund refund, Payment lockedPayment) {
        refund.setStatus(RefundStatus.SUCCESS);
        touchAttemptMetadata(refund);
        refund.setNextRetryAt(null);
        refundRepository.save(refund);

        anomalyRecorder.record(AnomalyCode.WEBHOOK_MISSING, AnomalySeverity.INFO,
            lockedPayment.getOrder() != null ? lockedPayment.getOrder().getId() : null,
            lockedPayment.getId(), refund.getId(),
            refund.getProviderRefundId(), lockedPayment.getProviderPaymentId(),
            "Local PENDING recovered via gateway polling — webhook was never received");

        // Aggregates + exactly-once restock (payment-row lock already held).
        refundService.recomputeAggregates(lockedPayment);

        log.info("Reconciled refund {} → SUCCESS for order {}", mask(refund.getProviderRefundId()),
            lockedPayment.getOrder() != null ? lockedPayment.getOrder().getOrderNumber() : "?");
    }

    private void markFailedAtGateway(Refund refund) {
        refund.setStatus(RefundStatus.FAILED);
        refund.setErrorMessage("GATEWAY_REPORTED_FAILED");
        touchAttemptMetadata(refund);
        refund.setNextRetryAt(null);
        refundRepository.save(refund);
        log.warn("Gateway reports refund {} FAILED — financial-only, no restock",
            mask(refund.getProviderRefundId()));
    }

    private void markTemporaryFailure(Refund refund, String reason) {
        touchAttemptMetadata(refund);
        refund.setNextRetryAt(backoffFrom(refund.getAttemptCount()));
        refundRepository.save(refund);
        log.warn("Temporary reconciliation failure for refund {}: {} (attempt {}, next retry {})",
            mask(refund.getProviderRefundId()), reason, refund.getAttemptCount(), refund.getNextRetryAt());

        if (refund.getAttemptCount() >= properties.getMaxAttempts()) {
            raiseStuckPending(refund, "Retry budget exhausted after repeated gateway errors");
        }
    }

    private boolean bumpAttemptAndMaybePark(Refund refund) {
        touchAttemptMetadata(refund);
        boolean parked = refund.getAttemptCount() >= properties.getMaxAttempts();
        refund.setNextRetryAt(parked
            ? LocalDateTime.now().plusHours(24) // effectively parked; anomaly raised below
            : backoffFrom(refund.getAttemptCount()));
        refundRepository.save(refund);
        if (parked) {
            raiseStuckPending(refund,
                "Still PENDING at gateway after " + refund.getAttemptCount() + " polls");
        }
        return parked;
    }

    private void parkWithAnomaly(Refund refund, AnomalyCode code, AnomalySeverity severity, String detail) {
        touchAttemptMetadata(refund);
        refund.setNextRetryAt(LocalDateTime.now().plusHours(24)); // stop hammering
        refundRepository.save(refund);
        anomalyRecorder.record(code, severity,
            refund.getOrder() != null ? refund.getOrder().getId() : null,
            refund.getPayment() != null ? refund.getPayment().getId() : null,
            refund.getId(), refund.getProviderRefundId(),
            refund.getPayment() != null ? refund.getPayment().getProviderPaymentId() : null,
            detail);
    }

    private void raiseStuckPending(Refund refund, String why) {
        anomalyRecorder.record(AnomalyCode.REFUND_STUCK_PENDING, AnomalySeverity.WARNING,
            refund.getOrder() != null ? refund.getOrder().getId() : null,
            refund.getPayment() != null ? refund.getPayment().getId() : null,
            refund.getId(), refund.getProviderRefundId(),
            refund.getPayment() != null ? refund.getPayment().getProviderPaymentId() : null,
            why);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private RazorpayGateway enabledGateway() {
        if (!properties.isEnabled()) return null;
        return gatewayProvider.getIfAvailable();
    }

    private void touchAttemptMetadata(Refund refund) {
        refund.setLastAttemptAt(LocalDateTime.now());
        refund.setAttemptCount(refund.getAttemptCount() + 1);
    }

    /** Bounded exponential backoff: min(cap, base × 2^attemptCount). */
    private LocalDateTime backoffFrom(int attemptCount) {
        long seconds = properties.getBackoffBaseSeconds();
        for (int i = 1; i < attemptCount && seconds < properties.getBackoffMaxSeconds(); i++) {
            seconds *= 2;
        }
        seconds = Math.min(seconds, properties.getBackoffMaxSeconds());
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private String mask(String v) {
        if (v == null || v.length() <= 6) return "***";
        return "***" + v.substring(v.length() - 6);
    }
}