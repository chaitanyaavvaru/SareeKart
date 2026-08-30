package com.sareekart.service;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RazorpayProperties;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import com.sareekart.dto.refund.RefundRequest;
import com.sareekart.entity.Order;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Refund;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.entity.enums.RefundStatus;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.RefundRepository;
import com.sareekart.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Refund math, validation gates and aggregate/restock rules.
 * Persistence + gateway mocked; BigDecimal arithmetic runs for real.
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final BigDecimal CAPTURED = new BigDecimal("1000.00");

    @Mock private RefundRepository refundRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InventoryService inventoryService;
    @Mock private RazorpayGateway gateway;
    @Mock private AnomalyRecorder anomalyRecorder;
    @Mock private ObjectProvider<RazorpayGateway> gatewayProvider;

    private RazorpayProperties props;
    private RefundService service;

    @BeforeEach
    void setUp() {
        props = enabledProps();
        lenient().when(gatewayProvider.getIfAvailable()).thenReturn(gateway);
        service = new RefundService(refundRepository, paymentRepository,
            inventoryService, props, gatewayProvider, anomalyRecorder);
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RazorpayProperties enabledProps() {
        RazorpayProperties p = new RazorpayProperties();
        p.setEnabled(true);
        p.setKeyId("k");
        p.setKeySecret("s");
        p.setWebhookSecret("w");
        return p;
    }

    // ── fixture ──────────────────────────────────────────────────────────────

    private Payment capturedPaid() {
        User u = new User();
        u.setId(7L); u.setEmail("a@s.com");

        Order order = new Order();
        order.setId(11L);
        order.setOrderNumber("ORD-1");
        order.setUser(u);
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setItems(new ArrayList<>());

        Payment p = new Payment();
        p.setId(5L);
        p.setOrder(order);
        p.setAmount(CAPTURED);
        p.setStatus(PaymentStatus.PAID);
        p.setMethod(PaymentMethod.RAZORPAY);
        p.setProviderPaymentId("pay_x1");
        order.setPayments(new ArrayList<>(List.of(p)));
        return p;
    }

    private void lockReturns(Payment p) {
        lenient().when(paymentRepository.findByOrderIdAndStatusForUpdate(11L, PaymentStatus.PAID))
            .thenReturn(List.of(p));
        lenient().when(refundRepository.lockPayment(5L)).thenReturn(Optional.of(p));
    }

    private void successfulSoFar(String amount) {
        lenient().when(refundRepository.sumAmountByPaymentIdAndStatus(5L, RefundStatus.SUCCESS))
            .thenReturn(amount != null ? new BigDecimal(amount) : BigDecimal.ZERO);
    }

    private void gatewayProcesses() {
        lenient().when(gateway.createRefund(anyString(), anyLong()))
            .thenAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_" + System.nanoTime())
                .status("processed")
                .amount(inv.getArgument(1, Long.class))
                .build());
    }

    private RefundRequest req(String amount) {
        return RefundRequest.builder()
            .amount(amount != null ? new BigDecimal(amount) : null)
            .reason("unit").build();
    }

    // ── initiation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("full refund derives remaining balance server-side (captured − refunded)")
    void fullDerivesRemaining() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar(null);
        gatewayProcesses();

        var resp = service.initiate("admin@s.com", 11L, req(null));

        assertThat(resp.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(resp.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        verify(gateway).createRefund("pay_x1", 100000L);
    }

    @Test
    @DisplayName("partial refund within remaining balance accepted")
    void partialWithinBalance() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar("300.00"); // 700 refundable
        gatewayProcesses();

        var resp = service.initiate("admin@s.com", 11L, req("200"));

        assertThat(resp.getAmount()).isEqualByComparingTo("200.00");
        verify(gateway).createRefund("pay_x1", 20000L);
    }

    @Test
    @DisplayName("partial refund exceeding remaining balance rejected with remainder surfaced")
    void partialExceedingRejected() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar("800.00"); // only 200 refundable

        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req("500")))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("200");

        verify(gateway, never()).createRefund(anyString(), anyLong());
    }

    @Test
    @DisplayName("order without a captured PAID payment rejected")
    void unpaidRejected() {
        Payment p = capturedPaid();
        p.setStatus(PaymentStatus.PENDING);
        lockReturns(p);

        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req(null)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("No captured payment");
    }

    @Test
    @DisplayName("fully refunded payment rejected")
    void fullyRefundedRejected() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar("1000.00");

        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req(null)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already been fully refunded");
    }

    @Test
    @DisplayName("outstanding PENDING refund blocks re-initiation (duplicate admin click)")
    void pendingConflictRejected() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar(null);
        Refund pending = new Refund();
        pending.setPayment(p);
        pending.setStatus(RefundStatus.PENDING);
        pending.setAmount(new BigDecimal("10"));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(11L))
            .thenReturn(List.of(pending));

        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req(null)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already in progress");

        verify(gateway, never()).createRefund(anyString(), anyLong());
    }

    @Test
    @DisplayName("gateway failure persists an auditable FAILED row then surfaces to admin")
    void gatewayFailureAudited() {
        Payment p = capturedPaid();
        lockReturns(p);
        successfulSoFar(null);
        when(gateway.createRefund(anyString(), anyLong()))
            .thenThrow(new IllegalStateException("gateway down"));

        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req(null)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("gateway down");

        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    @DisplayName("disabled payments mode → explicit 503-mapped exception, no rows written")
    void disabledModeRejected() {
        props.setEnabled(false);
        assertThatThrownBy(() -> service.initiate("admin@s.com", 11L, req(null)))
            .isInstanceOf(com.sareekart.exception.ServiceUnavailableException.class);
        verify(refundRepository, never()).save(any());
    }

    // ── aggregates & restock rule ────────────────────────────────────────────

    @Nested
    class AggregateRules {

        @Test
        @DisplayName("partial success → PARTIALLY_REFUNDED; no restock")
        void partialFlipNoRestock() {
            Payment p = capturedPaid();
            when(refundRepository.sumAmountByPaymentIdAndStatus(5L, RefundStatus.SUCCESS))
                .thenReturn(new BigDecimal("400.00"));

            service.recomputeAggregates(p);

            assertThat(p.getOrder().getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            verify(inventoryService, never()).releaseForOrder(any());
            assertThat(p.getOrder().getInventoryRestocked()).isFalse();
        }

        @Test
        @DisplayName("full success pre-fulfillment → REFUNDED + restock exactly once (flag-guarded replay)")
        void fullRestockExactlyOnce() {
            Payment p = capturedPaid();
            when(inventoryService.isCommitted(any())).thenReturn(true);
            when(refundRepository.sumAmountByPaymentIdAndStatus(5L, RefundStatus.SUCCESS))
                .thenReturn(new BigDecimal("1000.00"));

            service.recomputeAggregates(p);
            service.recomputeAggregates(p); // webhook replay

            assertThat(p.getOrder().getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(inventoryService, times(1)).releaseForOrder(any());
            assertThat(p.getOrder().getInventoryRestocked()).isTrue();
        }

        @Test
        @DisplayName("full refund AFTER fulfillment: financial-only, no auto-restock")
        void fulfilledNeverAutoRestocks() {
            Payment p = capturedPaid();
            p.getOrder().setStatus(OrderStatus.DELIVERED);
            lenient().when(inventoryService.isCommitted(any())).thenReturn(true);
            when(refundRepository.sumAmountByPaymentIdAndStatus(5L, RefundStatus.SUCCESS))
                .thenReturn(new BigDecimal("1000.00"));

            service.recomputeAggregates(p);

            assertThat(p.getOrder().getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(inventoryService, never()).releaseForOrder(any());
            assertThat(p.getOrder().getInventoryRestocked()).isFalse();
        }
    }
}