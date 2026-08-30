package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RazorpayProperties;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.dto.payment.PaymentOrderResponse;
import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.dto.payment.VerifyPaymentRequest;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Product;
import com.sareekart.entity.ProductVariant;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ServiceUnavailableException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.PaymentRepository;
import com.sareekart.repository.WebhookEventRepository;
import com.sareekart.security.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * Unit tests for the Razorpay payment lifecycle. Repositories and the
 * gateway port are mocked; signature checks run for real against a fixed
 * test secret.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String KEY_SECRET = "unit-test-key-secret";
    private static final String WEBHOOK_SECRET = "unit-test-whsec";

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CouponService couponService;
    @Mock private RefundService refundService;
    @Mock private WebhookLedgerService webhookLedger;
    @Mock private RazorpayGateway razorpayGateway;

    private RazorpayProperties props;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        props = new RazorpayProperties();
        props.setEnabled(true);
        props.setKeyId("rzp_test_key");
        props.setKeySecret(KEY_SECRET);
        props.setWebhookSecret(WEBHOOK_SECRET);

        lenient().when(webhookLedger.recordIfFirst(anyString(), anyString(), anyString()))
            .thenAnswer(inv -> {
                com.sareekart.entity.WebhookEvent e = new com.sareekart.entity.WebhookEvent();
                e.setEventId(inv.getArgument(0));
                e.setEventType(inv.getArgument(1));
                e.setPayload(inv.getArgument(2));
                return e;
            });

        service = new PaymentService(paymentRepository, orderRepository,
            webhookEventRepository, inventoryService, couponService, refundService, webhookLedger, props,
            new ObjectMapper(), providerOf(razorpayGateway));
    }

    /** Minimal ObjectProvider wrapper so the unit test mirrors Spring's optional wiring. */
    private static org.springframework.beans.factory.ObjectProvider<RazorpayGateway> providerOf(RazorpayGateway g) {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public RazorpayGateway getIfAvailable() { return g; }
            @Override public RazorpayGateway getObject() { return g; }
            @Override public RazorpayGateway getObject(Object... args) { return g; }
            @Override public RazorpayGateway getIfUnique() { return g; }
        };
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("T");
        u.setLastName("U");
        return u;
    }

    private Order paidPendingOrder(Long orderId, String ownerEmail) {
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-1");
        order.setUser(user(ownerEmail));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.RAZORPAY);
        order.setTotalAmount(new BigDecimal("23098.95"));

        Product product = new Product();
        product.setId(10L);
        ProductVariant variant = new ProductVariant();
        variant.setId(99L);
        variant.setProduct(product);

        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(2);
        order.setItems(List.of(item));

        Payment payment = new Payment();
        payment.setId(500L);
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.RAZORPAY);
        payment.setProviderOrderId("order_rzp_123");
        order.setPayments(new java.util.ArrayList<>(List.of(payment)));

        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        return order;
    }

    // ── create-order ─────────────────────────────────────────────────────────

    @Nested
    class CreateOrder {

        @Test
        @DisplayName("amount comes from the stored order — never from any client input")
        void amountIsServerAuthority() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            order.getPayments().get(0).setProviderOrderId(null); // force the fresh-create branch
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(order.getPayments().get(0)));
            when(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
                .thenReturn(RazorpayOrderResponse.builder().id("order_rzp_new").build());

            PaymentOrderResponse resp = service.createPaymentOrder("c@sareekart.com", 1L);

            // 23098.95 * 100 = 2309895 paise — derived from stored total
            assertThat(resp.getAmount()).isEqualTo(2309895L);
            verify(razorpayGateway).createOrder("1", 2309895L, "INR");
        }

        @Test
        @DisplayName("repeated create reuses the same Razorpay order id (no double gateway orders)")
        void idempotentReuse() {
            Order order = paidPendingOrder(1L, "c@sareekart.com"); // already has providerOrderId=order_rzp_123
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(order.getPayments().get(0)));

            PaymentOrderResponse resp = service.createPaymentOrder("c@sareekart.com", 1L);

            assertThat(resp.getRazorpayOrderId()).isEqualTo("order_rzp_123");
            verify(razorpayGateway, never()).createOrder(anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("another user's order is rejected")
        void wrongUserRejected() {
            Order order = paidPendingOrder(1L, "owner@sareekart.com");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.createPaymentOrder("attacker@sareekart.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");

            verify(razorpayGateway, never()).createOrder(anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("disabled mode returns explicit 503-mapped exception")
        void disabledMode() {
            props.setEnabled(false);

            assertThatThrownBy(() -> service.createPaymentOrder("c@sareekart.com", 1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cash on Delivery");
        }
    }

    // ── verify ───────────────────────────────────────────────────────────────

    @Nested
    class Verify {

        private VerifyPaymentRequest signedRequest(String rzpOrderId, String rzpPaymentId) {
            String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);
            return VerifyPaymentRequest.builder()
                .orderId("1").razorpayOrderId(rzpOrderId)
                .razorpayPaymentId(rzpPaymentId).razorpaySignature(sig)
                .build();
        }

        @Test
        @DisplayName("valid signature marks PAID, enters PROCESSING, commits inventory once")
        void happyPath() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            when(inventoryService.isCommitted(order)).thenReturn(false);
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(order.getPayments().get(0)));

            OrderResponse resp = service.verifyPayment(
                "c@sareekart.com", signedRequest("order_rzp_123", "pay_ok"));

            assertThat(resp.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(resp.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            verify(inventoryService).commitForOrder(order); // exactly once
            assertThat(order.getPayments().get(0).getProviderPaymentId()).isEqualTo("pay_ok");
            assertThat(order.getPayments().get(0).getStatusTransitionHistory())
                .contains("PENDING").contains("PAID").contains("VERIFY");
        }

        @Test
        @DisplayName("duplicate verification is idempotent — inventory NOT decremented twice")
        void duplicateVerifyIdempotent() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            Payment payment = order.getPayments().get(0);
            payment.setStatus(PaymentStatus.PAID); // first call already succeeded
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.PROCESSING);

            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(payment));
            // Already-PAID short-circuits BEFORE any inventory decision — assert no interaction at all.
            lenient().when(inventoryService.isCommitted(order)).thenReturn(true);

            service.verifyPayment("c@sareekart.com", signedRequest("order_rzp_123", "pay_again"));

            verify(inventoryService, never()).commitForOrder(any()); // no second decrement
        }

        @Test
        @DisplayName("invalid/tampered signature is rejected and marks payment FAILED while pending")
        void invalidSignatureRejected() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            Payment payment = order.getPayments().get(0);
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(payment));

            VerifyPaymentRequest forged = VerifyPaymentRequest.builder()
                .orderId("1").razorpayOrderId("order_rzp_123")
                .razorpayPaymentId("pay_x").razorpaySignature("deadbeef")
                .build();

            assertThatThrownBy(() -> service.verifyPayment("c@sareekart.com", forged))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("signature");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(inventoryService, never()).commitForOrder(any());
        }

        @Test
        @DisplayName("wrong user cannot verify someone else's payment")
        void wrongUserRejected() {
            Order order = paidPendingOrder(1L, "owner@sareekart.com");
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(order.getPayments().get(0)));

            assertThatThrownBy(() ->
                service.verifyPayment("intruder@sareekart.com",
                    signedRequest("order_rzp_123", "pay_ok")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("already-paid order short-circuits before signature work")
        void alreadyPaidShortCircuit() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            Payment payment = order.getPayments().get(0);
            payment.setStatus(PaymentStatus.PAID);
            order.setPaymentStatus(PaymentStatus.PAID);
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(payment));

            OrderResponse resp = service.verifyPayment("c@sareekart.com",
                VerifyPaymentRequest.builder() // garbage signature must not matter
                    .orderId("1").razorpayOrderId("order_rzp_123")
                    .razorpayPaymentId("whatever").razorpaySignature("bogus")
                    .build());

            assertThat(resp.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getStatus()).isNotEqualTo(PaymentStatus.FAILED); // not downgraded
        }
    }

    // ── webhooks ─────────────────────────────────────────────────────────────

    @Nested
    class Webhooks {

        @Test
        @DisplayName("payment.captured webhook flips pending payment to PAID exactly once")
        void capturedWebhook() throws Exception {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            when(inventoryService.isCommitted(order)).thenReturn(false);
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(order.getPayments().get(0)));

            long paise = order.getTotalAmount().movePointRight(2).longValueExact();
            String body = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{"
                + "\"id\":\"pay_wh1\",\"order_id\":\"order_rzp_123\",\"amount\":" + paise + "}}}}";
            String sig = SignatureUtil.hmacSha256Hex(body, WEBHOOK_SECRET);

            service.handleWebhook(body, sig, "evt_1");

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            verify(inventoryService, times(1)).commitForOrder(order);
        }

        @Test
        @DisplayName("replayed webhook (duplicate event id) is skipped entirely")
        void replayedWebhookDeduped() {
            String body = "{\"event\":\"payment.captured\"}";
            String sig = SignatureUtil.hmacSha256Hex(body, WEBHOOK_SECRET);
            when(webhookLedger.recordIfFirst(anyString(), anyString(), anyString())).thenReturn(null); // duplicate

            service.handleWebhook(body, sig, "evt_dup");

            verify(paymentRepository, never()).findByProviderOrderIdForUpdate(anyString());
        }

        @Test
        @DisplayName("webhook with bad signature is rejected outright")
        void badWebhookSignature() {
            String body = "{\"event\":\"payment.captured\"}";

            assertThatThrownBy(() -> service.handleWebhook(body, "badsig", "evt_x"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("webhook signature");

            verify(webhookEventRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("captured webhook with mismatched amount refuses to mark PAID")
        void amountMismatchGuard() {
            Order order = paidPendingOrder(1L, "c@sareekart.com");
            when(paymentRepository.findByProviderOrderIdForUpdate("order_rzp_123"))
                .thenReturn(Optional.of(order.getPayments().get(0)));
            String body = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{"
                + "\"id\":\"pay_wh2\",\"order_id\":\"order_rzp_123\",\"amount\":100}}}}"; // ₹1 vs real total
            String sig = SignatureUtil.hmacSha256Hex(body, WEBHOOK_SECRET);

            service.handleWebhook(body, sig, "evt_2");

            Payment payment = order.getPayments().get(0);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(inventoryService, never()).commitForOrder(any());
        }
    }
}