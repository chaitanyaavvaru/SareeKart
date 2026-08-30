package com.sareekart.service;

import com.sareekart.config.SweeperProperties;
import com.sareekart.entity.Order;
import com.sareekart.entity.Payment;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expires abandoned online checkouts.
 *
 * SAFETY MODEL
 * ────────────
 * Eligible:  method=RAZORPAY ∧ status=PENDING ∧ paymentStatus=PENDING ∧
 *            createdAt < now − staleAfterMinutes. COD and every committed /
 *            paid state are excluded at the query AND re-checked under lock.
 *
 * Race with late verify/webhook: both paths acquire the SAME pessimistic
 * lock on the order's payment row (Phase 6 mechanism), so expiry and capture
 * are fully serialized — a payment can never be applied to an expired order,
 * and an in-flight capture finishes before the sweeper observes the row.
 *
 * Idempotency: each candidate is expired inside its own transaction; the
 * mutations are conditional on still-PENDING state, so repeated passes (or
 * parallel instances) are no-ops after the first success. A crash rolls the
 * whole per-order transaction back.
 *
 * Inventory note: unpaid online orders never commit inventory (commit happens
 * only at PAID), so there is nothing to restore by construction. The
 * defensive isCommitted guard below keeps that invariant explicit.
 */
@Slf4j
@Service
public class OrderSweeperService {

    static final String CANCEL_REASON_EXPIRED = "EXPIRED";

    private final SweeperProperties properties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    /** Lazy self-proxy so per-order transactions apply despite internal calls. */
    private final org.springframework.beans.factory.ObjectProvider<OrderSweeperService> self;


    public OrderSweeperService(SweeperProperties properties,
                               OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               InventoryService inventoryService,
                               CouponService couponService,
                               org.springframework.beans.factory.ObjectProvider<OrderSweeperService> self) {
        this.properties = properties;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.self = self;
    }

    public record SweepSummary(int examined, int expired) {}

    /** One scheduler/ops pass. Never throws upward — failures are logged. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SweepSummary runOnce() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(properties.getStaleAfterMinutes());
        List<Long> candidateIds = orderRepository.findStaleUnpaidOnlineOrders(
                cutoff, PageRequest.of(0, properties.getBatchSize()))
            .stream().map(Order::getId).toList();

        int expired = 0;
        for (Long orderId : candidateIds) {
            try {
                // MUST go through the proxy: direct this.expireOne() would skip
                // @Transactional and die on the pessimistic-lock query.
                if (self.getObject().expireOne(orderId)) expired++;
            } catch (Exception e) {
                log.error("Sweeper failed to expire orderId {}: {}", orderId, e.getMessage());
            }
        }
        if (!candidateIds.isEmpty()) {
            log.info("Sweeper pass: examined={} expired={}", candidateIds.size(), expired);
        }
        return new SweepSummary(candidateIds.size(), expired);
    }

    /**
     * Expires a single order atomically. Returns false when it was no longer
     * eligible (paid / already cancelled / concurrently captured).
     */
    @Transactional
    public boolean expireOne(Long orderId) {
        // Serialize against verify/webhook via the shared payment-row lock.
        List<Payment> locked = paymentRepository.findByOrderIdAndStatusForUpdate(
            orderId, PaymentStatus.PENDING);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        boolean stillEligible = order.getStatus() == OrderStatus.PENDING
            && order.getPaymentStatus() == PaymentStatus.PENDING
            && !locked.isEmpty();
        if (!stillEligible) {
            return false; // lost a race or already handled — idempotent skip
        }

        Payment payment = locked.get(0);
        LocalDateTime now = LocalDateTime.now();

        // Defensive invariant: unpaid online orders are never inventory-committed.
        if (inventoryService.isCommitted(order)) {
            log.error("Sweeper invariant violation: order {} committed but PENDING — skipping expiry",
                order.getOrderNumber());
            return false;
        }

        payment.setStatus(PaymentStatus.FAILED);
        appendExpiryHistory(payment);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setCancelledAt(now);
        order.setCancelReason(CANCEL_REASON_EXPIRED);
        orderRepository.save(order);

        // Release the coupon hold; idempotent and guarded internally.
        couponService.releaseIfReserved(orderId);

        log.info("Expired stale online order {} (reason=EXPIRED)", order.getOrderNumber());
        return true;
    }

    private void appendExpiryHistory(Payment payment) {
        try {
            String entry = "{\"from\":\"" + payment.getStatus() + "\",\"to\":\"FAILED\","
                + "\"by\":\"SWEEPER:EXPIRED\",\"at\":\"" + LocalDateTime.now() + "\"}";
            String existing = payment.getStatusTransitionHistory();
            payment.setStatusTransitionHistory(existing == null || existing.isBlank()
                ? "[" + entry + "]"
                : existing.substring(0, existing.length() - 1) + "," + entry + "]");
        } catch (Exception ignored) {
            // history is best-effort audit only
        }
    }
}