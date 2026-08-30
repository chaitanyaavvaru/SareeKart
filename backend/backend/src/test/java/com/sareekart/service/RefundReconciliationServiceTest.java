package com.sareekart.service;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RazorpayProperties;
import com.sareekart.config.RefundReconciliationProperties;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Refund;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.entity.enums.RefundStatus;
import com.sareekart.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reconciliation decision-table units: gateway-state → local-transition
 * mapping, backoff bookkeeping, and anomaly triggers. Persistence mocked.
 */
@ExtendWith(MockitoExtension.class)
class RefundReconciliationServiceTest {

    private static final long LOCAL_PAISE = 120000L; // ₹1200.00

    @Mock private RefundReconciliationProperties properties;
    @Mock private RefundRepository refundRepository;
    @Mock private RazorpayGateway gateway;
    @Mock private ObjectProvider<RazorpayGateway> gatewayProvider;
    @Mock private RefundService refundService;
    @Mock private AnomalyRecorder anomalyRecorder;
    @Mock private ObjectProvider<RefundReconciliationService> self;

    private RefundReconciliationService service;

    @BeforeEach
    void setUp() {
        properties = new RefundReconciliationProperties();
        properties.setEnabled(true);
        when(gatewayProvider.getIfAvailable()).thenReturn(gateway);
        lenient().when(self.getObject()).thenAnswer(inv -> null); // tests call reconcileOne directly

        service = new RefundReconciliationService(properties, refundRepository,
            gatewayProvider, refundService, anomalyRecorder,
            new ObjectProvider<>() {
                @Override public RefundReconciliationService getObject() { return service; }
                @Override public RefundReconciliationService getIfAvailable() { return service; }
            });
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Refund pending(String providerRefundId) {
        User u = new User(); u.setId(7L); u.setEmail("c@s.com");
        Order o = new Order(); o.setId(11L); o.setOrderNumber("ORD-R");
        o.setUser(u); o.setStatus(OrderStatus.PROCESSING);
        o.setPaymentStatus(PaymentStatus.PAID);

        Payment pay = new Payment();
        pay.setId(5L); pay.setOrder(o); pay.setAmount(new BigDecimal("1200.00"));
        pay.setStatus(PaymentStatus.PAID); pay.setMethod(PaymentMethod.RAZORPAY);
        pay.setProviderPaymentId("pay_z1");

        Refund r = new Refund();
        r.setId(9L); r.setPayment(pay); r.setOrder(o);
        r.setAmount(new BigDecimal("1200.00"));
        r.setStatus(RefundStatus.PENDING);
        r.setProviderRefundId(providerRefundId);
        r.setAttemptCount(0);
        return r;
    }

    private void lockFixture(Refund r) {
        lenient().when(refundRepository.findById(r.getId())).thenReturn(Optional.of(r));
        lenient().when(refundRepository.lockPayment(r.getPayment().getId()))
            .thenReturn(Optional.of(r.getPayment()));
        lenient().when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RazorpayRefundResponse remote(String status, long amountPaise) {
        return RazorpayRefundResponse.builder()
            .id("rf_remote").status(status).amount(amountPaise).build();
    }

    // ── outcomes ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("gateway processed → local SUCCESS + WEBHOOK_MISSING anomaly + aggregate recompute")
    void reconcilesToSuccess() {
        Refund r = pending("rf_ok");
        lockFixture(r);
        when(gateway.fetchRefund("rf_ok"))
            .thenReturn(Optional.of(remote("processed", LOCAL_PAISE)));

        var outcome = service.reconcileOne(r.getId());

        assertThat(outcome.reconciledToSuccess()).isTrue();
        assertThat(r.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        verify(refundService).recomputeAggregates(r.getPayment());
        verify(anomalyRecorder).record(
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalyCode.WEBHOOK_MISSING),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("gateway failed → local FAILED (financial-only, no restock trigger)")
    void reconcilesToFailed() {
        Refund r = pending("rf_fail");
        lockFixture(r);
        when(gateway.fetchRefund("rf_fail")).thenReturn(Optional.of(remote("failed", LOCAL_PAISE)));

        var outcome = service.reconcileOne(r.getId());

        assertThat(outcome.reconciledToFailed()).isTrue();
        assertThat(r.getStatus()).isEqualTo(RefundStatus.FAILED);
        verify(refundService, never()).recomputeAggregates(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("gateway still pending → stays PENDING with backoff metadata")
    void stillPendingBumpsBackoff() {
        Refund r = pending("rf_slow");
        lockFixture(r);
        when(gateway.fetchRefund("rf_slow")).thenReturn(Optional.of(remote("pending", LOCAL_PAISE)));

        var outcome = service.reconcileOne(r.getId());

        assertThat(outcome.stillPending()).isTrue();
        assertThat(r.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(r.getAttemptCount()).isEqualTo(1);
        assertThat(r.getLastAttemptAt()).isNotNull();
        assertThat(r.getNextRetryAt()).isAfter(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("gateway does not know the id → UNKNOWN_REFUND CRITICAL anomaly, parked")
    void unknownRefundAnomaly() {
        Refund r = pending("rf_ghost");
        lockFixture(r);
        when(gateway.fetchRefund("rf_ghost")).thenReturn(Optional.empty());

        var outcome = service.reconcileOne(r.getId());
        assertThat(outcome.skippedNotPending())
            .as("unknown gateway id parks the refund")
            .isTrue();
        assertThat(r.getNextRetryAt()).isNotNull(); // parked 24h
        verify(anomalyRecorder).record(
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalyCode.UNKNOWN_REFUND),
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalySeverity.CRITICAL),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("gateway amount ≠ local row → GATEWAY_LOCAL_STATE_MISMATCH, money state untouched")
    void mismatchNeverMutatesMoney() {
        Refund r = pending("rf_bad");
        lockFixture(r);
        when(gateway.fetchRefund("rf_bad")).thenReturn(Optional.of(remote("processed", 999L)));

        service.reconcileOne(r.getId());

        assertThat(r.getStatus()).as("money state must not mutate on mismatch")
            .isEqualTo(RefundStatus.PENDING);
        verify(anomalyRecorder).record(
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalyCode.GATEWAY_LOCAL_STATE_MISMATCH),
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalySeverity.CRITICAL),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(refundService, never()).recomputeAggregates(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("network error → temporary failure: attempt bumped, bounded backoff applied")
    void temporaryFailureRetries() {
        Refund r = pending("rf_net");
        lockFixture(r);
        when(gateway.fetchRefund("rf_net")).thenThrow(new IllegalStateException("connect timeout"));

        var outcome = service.reconcileOne(r.getId());

        assertThat(outcome.stillPending()).isTrue();
        assertThat(r.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(r.getAttemptCount()).isEqualTo(1);
        assertThat(r.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("already-terminal refunds are skipped without gateway calls")
    void terminalSkipped() {
        Refund done = pending("rf_done");
        done.setStatus(RefundStatus.SUCCESS);
        lockFixture(done);

        var outcome = service.reconcileOne(done.getId());

        assertThat(outcome.skippedNotPending()).isTrue();
        verify(gateway, never()).fetchRefund(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("max attempts reached while still pending parks row + REFUND_STUCK_PENDING")
    void maxAttemptsRaisesStuckPending() {
        Refund r = pending("rf_stuck");
        r.setAttemptCount(properties.getMaxAttempts() - 1); // this poll crosses the line
        lockFixture(r);
        when(gateway.fetchRefund("rf_stuck")).thenReturn(Optional.of(remote("pending", LOCAL_PAISE)));

        var outcome = service.reconcileOne(r.getId());

        assertThat(outcome.stillPending())
            .as("parked refunds leave the still-pending bucket")
            .isFalse();
        assertThat(r.getAttemptCount()).isEqualTo(properties.getMaxAttempts());
        verify(anomalyRecorder).record(
            org.mockito.ArgumentMatchers.eq(com.sareekart.entity.enums.AnomalyCode.REFUND_STUCK_PENDING),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    // matcher helper
    private static <T> T mEq(T value) { return org.mockito.ArgumentMatchers.eq(value); }

}
