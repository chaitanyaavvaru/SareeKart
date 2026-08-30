package com.sareekart.service;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RazorpayProperties;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import com.sareekart.dto.refund.RefundRequest;
import com.sareekart.dto.refund.RefundResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Refund;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.AnomalyCode;
import com.sareekart.entity.enums.AnomalySeverity;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.entity.enums.RefundReasonCode;
import com.sareekart.entity.enums.RefundStatus;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.exception.ServiceUnavailableException;
import com.sareekart.repository.PaymentRepository;
import com.sareekart.repository.RefundRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Refund lifecycle over captured payments.
 *
 * STATE MODEL
 * ───────────
 * refunds.status        : PENDING → SUCCESS | FAILED      (this row)
 * orders.paymentStatus  : PAID → PARTIALLY_REFUNDED → REFUNDED (aggregate)
 * orders.status         : UNCHANGED by refunds            (fulfillment truth)
 *
 * AMOUNT AUTHORITY
 * ────────────────
 * Full refund = captured − Σ(successful refunds), computed here from stored
 * rows. Partial amounts are validated against the remaining refundable
 * balance. The client can never inflate a refund.
 *
 * CONCURRENCY
 * ───────────
 * Every entry point locks the PAYMENT row first — the identical lock used by
 * verify/webhook/sweeper — so initiation, confirmation and expiry can never
 * interleave or double-apply.
 *
 * INVENTORY RULE (documented decision)
 * ────────────────────────────────────
 * Automatic restock happens EXACTLY ONCE, only when cumulative successful
 * refunds reach the full captured amount AND fulfillment has not started
 * (status not SHIPPED/DELIVERED/RETURNED). After fulfillment the refund is
 * financial-only; physical returns are a manual workflow. Guarded by the
 * durable orders.inventory_restocked flag.
 *
 * COUPON RULE (unchanged from Phase 7)
 * ────────────────────────────────────
 * Committed coupon usage is NEVER returned by a refund. This service does
 * not touch coupon tables; tests prove the invariant.
 */
@Slf4j
@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final RazorpayProperties razorpayProperties;
    private final ObjectProvider<RazorpayGateway> gatewayProvider;
    private final AnomalyRecorder anomalyRecorder;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         InventoryService inventoryService,
                         RazorpayProperties razorpayProperties,
                         ObjectProvider<RazorpayGateway> gatewayProvider,
                         AnomalyRecorder anomalyRecorder) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
        this.razorpayProperties = razorpayProperties;
        this.gatewayProvider = gatewayProvider;
        this.anomalyRecorder = anomalyRecorder;
    }

    // ── Admin initiation ─────────────────────────────────────────────────────

    @Transactional
    public RefundResponse initiate(String adminEmail, Long orderId, RefundRequest request) {
        RazorpayGateway gateway = requireGateway();

        List<Payment> locked = paymentRepository.findByOrderIdAndStatusForUpdate(orderId, PaymentStatus.PAID);
        Payment payment = locked.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PAID)
            .findFirst()
            .orElseThrow(() -> new BadRequestException(
                "No captured payment found for this order"));

        Order order = payment.getOrder();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled orders are not refundable through this flow");
        }

        BigDecimal captured = payment.getAmount();
        BigDecimal alreadyRefunded =
            refundRepository.sumAmountByPaymentIdAndStatus(payment.getId(), RefundStatus.SUCCESS);

        long pendingCount = refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
            .filter(r -> r.getStatus() == RefundStatus.PENDING && r.getPayment().getId().equals(payment.getId()))
            .count();
        if (pendingCount > 0) {
            throw new BadRequestException("A refund is already in progress for this order");
        }

        BigDecimal refundable = captured.subtract(alreadyRefunded);
        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This payment has already been fully refunded");
        }

        BigDecimal amount = request == null || request.getAmount() == null
            ? refundable // full refund derived server-side
            : request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }
        if (amount.compareTo(refundable) > 0) {
            throw new BadRequestException(String.format(
                "Refund exceeds remaining refundable balance of ₹%s",
                refundable.stripTrailingZeros().toPlainString()));
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setOrder(order);
        refund.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        refund.setReason(request != null ? request.getReason() : null);
        refund.setReasonCode(request != null && request.getReasonCode() != null
            ? request.getReasonCode() : RefundReasonCode.OTHER);
        refund.setInitiatedBy(adminEmail);
        refund.setStatus(RefundStatus.PENDING);

        try {
            RazorpayRefundResponse rzp = gateway.createRefund(
                payment.getProviderPaymentId(),
                amount.movePointRight(2).longValueExact());
            if (rzp == null || rzp.getId() == null) {
                throw new IllegalStateException("Gateway returned no refund id");
            }
            refund.setProviderRefundId(rzp.getId());
            // Razorpay REST refund creation is synchronous: 'processed' means done.
            refund.setStatus("processed".equalsIgnoreCase(rzp.getStatus())
                ? RefundStatus.SUCCESS : RefundStatus.PENDING);
        } catch (Exception e) {
            refund.setStatus(RefundStatus.FAILED);
            refund.setErrorMessage(truncate(e.getMessage()));
            refundRepository.save(refund);
            log.error("Refund initiation failed for order {}: {}", order.getOrderNumber(), e.getMessage());
            throw new BadRequestException("Refund failed at gateway: " + truncate(e.getMessage()));
        }

        refundRepository.save(refund);
        recomputeAggregates(payment);

        log.info("Refund {} ({}) for order {} amount={} by {}",
            mask(refund.getProviderRefundId()), refund.getStatus(),
            order.getOrderNumber(), refund.getAmount(), adminEmail);
        return RefundResponse.from(refund);
    }

    // ── Aggregate maintenance (callers hold the payment-row lock) ──────────

    /**
     * Recomputes orders.paymentStatus from persisted refund rows and applies
     * the exactly-once pre-fulfillment restock rule. MUST be called while the
     * payment-row pessimistic lock is held.
     */
    public void recomputeAggregates(Payment lockedPayment) {
        Order order = lockedPayment.getOrder();
        PaymentStatus previous = order.getPaymentStatus();

        // Capture pre-mutation state: isCommitted() keys off paymentStatus,
        // so evaluating AFTER setting REFUNDED would wrongly skip the restock
        // (same class of bug as the Phase-8 sweeper guard).
        boolean inventoryCommitted = inventoryService.isCommitted(order);

        BigDecimal successTotal =
            refundRepository.sumAmountByPaymentIdAndStatus(lockedPayment.getId(), RefundStatus.SUCCESS);
        int cmp = successTotal.compareTo(lockedPayment.getAmount());

        PaymentStatus next;
        if (cmp >= 0) next = PaymentStatus.REFUNDED;
        else if (successTotal.signum() > 0) next = PaymentStatus.PARTIALLY_REFUNDED;
        else next = previous == PaymentStatus.REFUNDED || previous == PaymentStatus.PARTIALLY_REFUNDED
            ? PaymentStatus.PAID // all refunds failed/rolled back
            : previous;

        if (next != previous) {
            order.setPaymentStatus(next);
        }

        // Exactly-once restock: full refund, before fulfillment, not yet restocked.
        if (next == PaymentStatus.REFUNDED
                && !Boolean.TRUE.equals(order.getInventoryRestocked())) {
            boolean fulfilled = order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.RETURNED;
            if (fulfilled) {
                log.info("Full refund on fulfilled order {} — manual return workflow required "
                    + "(no auto-restock)", order.getOrderNumber());
            } else if (inventoryCommitted) {
                inventoryService.releaseForOrder(order);
                order.setInventoryRestocked(true);
                log.info("Inventory auto-restocked for fully-refunded order {}",
                    order.getOrderNumber());
            }
        }

        if (next != previous) {
            log.info("Order {} paymentStatus {} → {} (refunded ₹{})",
                order.getOrderNumber(), previous, next, successTotal);
        }
    }

    // ── Webhook-driven transitions (called from PaymentService dispatch) ───

    /** refund.processed / refund.failed — idempotent, gateway-first capable. */
    @Transactional
    public void applyRefundWebhook(String eventType, String rzpRefundId,
                                   String rzpPaymentId, long amountPaise) {
        Optional<Refund> existing = refundRepository.findByProviderRefundId(rzpRefundId);

        RefundStatus target = "refund.processed".equals(eventType)
            ? RefundStatus.SUCCESS : RefundStatus.FAILED;

        if (existing.isPresent()) {
            Refund r = existing.get();
            if (r.getStatus() != RefundStatus.PENDING) {
                log.debug("Refund webhook replay ignored: {} already {}", rzpRefundId, r.getStatus());
                return; // idempotent replay
            }
            r.setStatus(target);
            if (target == RefundStatus.FAILED) {
                r.setErrorMessage("GATEWAY_EVENT:" + eventType);
            }
            refundRepository.save(r);
            // Serialize aggregates behind the payment-row lock.
            refundRepository.lockPayment(r.getPayment().getId())
                .ifPresent(this::recomputeAggregates);
            log.info("Refund {} → {} via webhook", mask(rzpRefundId), target);
            return;
        }

        // Gateway-first reconciliation (dashboard-initiated refund): recreate trail.
        if (rzpPaymentId == null) {
            log.warn("Refund webhook {} for unknown refund id {}", eventType, mask(rzpRefundId));
            return;
        }
        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(rzpPaymentId);
        if (paymentOpt.isEmpty()) {
            anomalyRecorder.record(AnomalyCode.PAYMENT_NOT_FOUND,
                AnomalySeverity.CRITICAL, null, null, null,
                rzpRefundId, rzpPaymentId,
                "refund." + eventType + " webhook references unknown provider payment");
            return;
        }
        Payment payment = refundRepository.lockPayment(paymentOpt.get().getId()).orElse(null);
        if (payment == null) return;
        Order order = payment.getOrder();

        BigDecimal incoming = BigDecimal.valueOf(amountPaise > 0 ? amountPaise : 0, 2);
        BigDecimal alreadyRefunded = refundRepository.sumAmountByPaymentIdAndStatus(
            payment.getId(), RefundStatus.SUCCESS);
        BigDecimal captured = payment.getAmount();

        // Financial invariant: cumulative refunds must never exceed the captured amount.
        // On violation: record a CRITICAL anomaly and refuse to mutate money state.
        if (alreadyRefunded.add(incoming).compareTo(captured) > 0) {
            anomalyRecorder.record(AnomalyCode.REFUND_AMOUNT_EXCEEDS_CAPTURE,
                AnomalySeverity.CRITICAL,
                order != null ? order.getId() : null, payment.getId(), null,
                rzpRefundId, rzpPaymentId,
                "Gateway-first refund ₹" + incoming + " would exceed captured ₹" + captured
                    + " (already refunded ₹" + alreadyRefunded + ") — mutation refused");
            return;
        }

        Refund reconstructed = new Refund();
        reconstructed.setPayment(payment);
        reconstructed.setOrder(order);
        reconstructed.setProviderRefundId(rzpRefundId);
        reconstructed.setAmount(incoming.max(BigDecimal.ZERO));
        reconstructed.setStatus(target);
        reconstructed.setReasonCode(RefundReasonCode.PAYMENT_RECONCILIATION);
        reconstructed.setInitiatedBy("SYSTEM:DASHBOARD");
        try {
            refundRepository.save(reconstructed);
        } catch (DataIntegrityViolationException dupRace) {
            log.info("Concurrent reconstruction of refund {} lost the race — skipping", mask(rzpRefundId));
            return;
        }
        recomputeAggregates(payment);
        log.info("Reconstructed dashboard refund {} ({}) for order {}",
            mask(rzpRefundId), target, order.getOrderNumber());
    }

    // ── Admin listing ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RefundResponse> list(com.sareekart.entity.enums.RefundStatus status) {
        List<Refund> rows = status == null
            ? refundRepository.findAll()
            : refundRepository.findByStatus(status);
        return rows.stream().map(RefundResponse::from)
            .sorted(java.util.Comparator.comparing(RefundResponse::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    // ── Late-capture reconciliation (Phase 8 follow-up) ─────────────────────

    /**
     * A gateway capture arrived for an order the sweeper already expired.
     * The order stays CANCELLED (fulfillment truth); the money is returned
     * automatically via a full gateway refund and permanently audited.
     */
    /**
     * Joins the caller's transaction deliberately: it already holds the
     * payment-row X-lock, and this method's INSERTs take FK S-locks on that
     * same row — a REQUIRES_NEW here would self-deadlock.
     * All failures are swallowed+audited so the outer webhook stays committable.
     */
    @Transactional
    public void reconcileLateCapture(Payment payment, String gatewayPaymentId, long capturedPaise) {
        RazorpayGateway gateway;
        try {
            gateway = requireGateway();
        } catch (ServiceUnavailableException unavailable) {
            log.error("RECONCILIATION REQUIRED (payments offline): captured payment {} on expired "
                + "order {} — manually refund via Razorpay dashboard",
                mask(gatewayPaymentId),
                payment.getOrder() != null ? payment.getOrder().getOrderNumber() : "?");
            return;
        }

        // Idempotency: one reconciliation refund per expired payment.
        BigDecimal already = refundRepository.sumAmountByPaymentIdAndStatus(
            payment.getId(), RefundStatus.SUCCESS);
        if (already.signum() > 0
                || refundRepository.existsByPaymentIdAndStatus(payment.getId(), RefundStatus.PENDING)) {
            log.info("Late-capture reconciliation already handled for payment {}", mask(gatewayPaymentId));
            return;
        }

        long refundPaise = capturedPaise > 0
            ? capturedPaise
            : payment.getAmount().movePointRight(2).longValueExact();

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setOrder(payment.getOrder());
        refund.setAmount(BigDecimal.valueOf(refundPaise, 2));
        refund.setReason("Late capture on expired order — automatic full refund");
        refund.setReasonCode(RefundReasonCode.PAYMENT_RECONCILIATION);
        refund.setInitiatedBy("SYSTEM:RECONCILE");
        if (payment.getOrder() != null) {
            anomalyRecorder.record(AnomalyCode.ORDER_ALREADY_CANCELLED_BUT_CAPTURED,
                AnomalySeverity.WARNING,
                payment.getOrder().getId(), payment.getId(), null, rzpRefundIdIfAny(gatewayPaymentId),
                gatewayPaymentId,
                "Gateway capture arrived after sweeper expiry — automatic full refund issued");
        }
        try {
            RazorpayRefundResponse rzp = gateway.createRefund(
                gatewayPaymentId != null ? gatewayPaymentId : payment.getProviderPaymentId(),
                refundPaise);
            refund.setProviderRefundId(rzp != null ? rzp.getId() : null);
            refund.setStatus(rzp != null && "processed".equalsIgnoreCase(rzp.getStatus())
                ? RefundStatus.SUCCESS : RefundStatus.PENDING);
            refundRepository.save(refund);
            log.error("RECONCILIATION: late capture on expired order {} auto-refunded (refund {}, {}) — "
                + "verify in Razorpay dashboard", 
                payment.getOrder() != null ? payment.getOrder().getOrderNumber() : "?",
                mask(refund.getProviderRefundId()), refund.getStatus());
        } catch (Exception e) {
            // Audit the failure but NEVER propagate: callers (webhook dispatch)
            // hold their own transaction that must stay committable.
            refund.setStatus(RefundStatus.FAILED);
            refund.setErrorMessage(truncate(e.getMessage()));
            refundRepository.save(refund);
            log.error("RECONCILIATION REQUIRED: auto-refund FAILED for expired-order capture {}: {}",
                mask(gatewayPaymentId), e.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RazorpayGateway requireGateway() {
        RazorpayGateway g = gatewayProvider.getIfAvailable();
        if (!razorpayProperties.isEnabled() || g == null) {
            throw new ServiceUnavailableException(
                "Refunds are unavailable because online payments are disabled.");
        }
        return g;
    }

    private String rzpRefundIdIfAny(String anyRef) { return anyRef; }

    private String mask(String v) {
        if (v == null || v.length() <= 6) return "***";
        return "***" + v.substring(v.length() - 6);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}