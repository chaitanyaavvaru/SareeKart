package com.sareekart.payment;

import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.sareekart.dto.request.AddressRequest;
import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.*;
import com.sareekart.service.LogisticsService;
import com.sareekart.service.OrderNotificationService;
import com.sareekart.service.WalletService;
import com.sareekart.service.impl.OrderServiceImpl;
import com.sareekart.service.impl.PaymentServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 14 · Stage 3: Production Payments & Checkout Verification Test Suite
 * 
 * Verifies the complete 14-step checkout sequence, sandbox cryptographic verification,
 * failure/cancellation flows, stock restoration, webhook idempotency, and security constraints.
 */
@ExtendWith(MockitoExtension.class)
public class PaymentProductionVerificationTest {

    // --- Repositories & Dependencies ---
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderNotificationService notificationService;

    @Mock
    private WalletService walletService;

    @Mock
    private LogisticsService logisticsService;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // Sandbox Test Credentials (Prefixed with rzp_test_, strictly sandbox-only)
    private static final String SANDBOX_KEY_ID = "rzp_test_51ProdCheck123";
    private static final String SANDBOX_KEY_SECRET = "sandbox_secret_998877665544";
    private static final String SANDBOX_WEBHOOK_SECRET = "webhook_secret_1122334455";

    private User customer;
    private Product saree;
    private Cart cart;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya.sharma@example.com")
                .role(Role.CUSTOMER)
                .build();

        saree = Product.builder()
                .id(101L)
                .name("Kanchipuram Temple Border Silk Saree")
                .price(new BigDecimal("18500.00"))
                .stockQuantity(10)
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(501L)
                .product(saree)
                .quantity(2)
                .build();

        List<CartItem> items = new ArrayList<>();
        items.add(cartItem);

        cart = Cart.builder()
                .id(201L)
                .user(customer)
                .items(items)
                .build();

        order = Order.builder()
                .id(1001L)
                .user(customer)
                .status(OrderStatus.PENDING)
                .paymentStatus("PENDING")
                .paymentMethod("RAZORPAY")
                .totalAmount(new BigDecimal("37000.00"))
                .items(new ArrayList<>())
                .build();

        // Configure PaymentServiceImpl sandbox properties
        paymentService.setKeyId(SANDBOX_KEY_ID);
        paymentService.setKeySecret(SANDBOX_KEY_SECRET);
        paymentService.setWebhookSecret(SANDBOX_WEBHOOK_SECRET);
        paymentService.setRazorpayClient(razorpayClient);
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Step 3: Complete 14-Step Checkout & Sandbox Payment Confirmation Flow")
    void test1_Complete14StepCheckoutFlow() throws Exception {
        // 1-7: Order Creation via Cart Checkout
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(101L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });

        AddressRequest address = AddressRequest.builder()
                .fullName("Priya Sharma")
                .phone("9876543210")
                .streetAddress("12th Main Road, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .shippingAddress(address)
                .paymentMethod("RAZORPAY")
                .build();

        // Execute Order Placement
        OrderResponse createdOrder = orderService.createOrder(1L, orderRequest);

        assertNotNull(createdOrder);
        assertEquals(1001L, createdOrder.getId());
        assertEquals("PENDING", createdOrder.getStatus());
        assertEquals("PENDING", createdOrder.getPaymentStatus());
        verify(productRepository).decrementStockIfAvailable(101L, 2);
        assertTrue(cart.getItems().isEmpty(), "Cart must be emptied upon order creation");
        verify(cartRepository).save(cart);

        // 8: Payment Order Creation (Razorpay Order Intent)
        razorpayClient.orders = orderClient;
        JSONObject razorpayOrderJson = new JSONObject();
        razorpayOrderJson.put("id", "order_rzp_mock_1001");
        com.razorpay.Order mockRzpOrder = new com.razorpay.Order(razorpayOrderJson);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);

        PaymentOrderResponse paymentOrderResponse = paymentService.createRazorpayOrder(1001L, 1L);

        assertNotNull(paymentOrderResponse);
        assertEquals("order_rzp_mock_1001", paymentOrderResponse.getRazorpayOrderId());
        assertEquals(new BigDecimal("37000.00"), paymentOrderResponse.getAmount());
        assertEquals("INR", paymentOrderResponse.getCurrency());
        assertEquals(SANDBOX_KEY_ID, paymentOrderResponse.getKeyId());
        assertEquals("order_rzp_mock_1001", order.getRazorpayOrderId());

        // 9-11: Sandbox Payment & Cryptographic Signature Verification
        String razorpayPaymentId = "pay_sandbox_998811";
        String validSignature = computeHmacSha256("order_rzp_mock_1001|" + razorpayPaymentId, SANDBOX_KEY_SECRET);

        PaymentVerificationRequest verifyRequest = PaymentVerificationRequest.builder()
                .orderId(1001L)
                .razorpayOrderId("order_rzp_mock_1001")
                .razorpayPaymentId(razorpayPaymentId)
                .razorpaySignature(validSignature)
                .build();

        // 12-14: Order Confirmation & History
        OrderResponse confirmedOrder = paymentService.verifyPaymentSignature(verifyRequest, 1L);

        assertNotNull(confirmedOrder);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals("COMPLETED", order.getPaymentStatus());
        assertEquals(razorpayPaymentId, order.getRazorpayPaymentId());
        verify(orderRepository, atLeastOnce()).save(order);
    }

    @Test
    @DisplayName("Step 4: Payment Failure Webhook Transitions Payment Status to FAILED")
    void test2_PaymentFailureWebhook() throws Exception {
        order.setRazorpayOrderId("order_rzp_failed_1001");
        when(orderRepository.findByRazorpayOrderId("order_rzp_failed_1001")).thenReturn(Optional.of(order));

        JSONObject webhookJson = new JSONObject();
        webhookJson.put("event", "payment.failed");

        JSONObject payloadObj = new JSONObject();
        JSONObject paymentObj = new JSONObject();
        JSONObject entityObj = new JSONObject();
        entityObj.put("id", "pay_failed_123");
        entityObj.put("order_id", "order_rzp_failed_1001");
        paymentObj.put("entity", entityObj);
        payloadObj.put("payment", paymentObj);
        webhookJson.put("payload", payloadObj);

        String payloadStr = webhookJson.toString();
        String validWebhookSig = computeHmacSha256(payloadStr, SANDBOX_WEBHOOK_SECRET);

        boolean processed = paymentService.processWebhook(payloadStr, validWebhookSig);

        assertTrue(processed);
        assertEquals("FAILED", order.getPaymentStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Step 4: Cancelled Payment Atomically Restores Product Stock")
    void test3_CancelledPaymentRestoresStock() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(saree)
                .price(saree.getPrice())
                .quantity(2)
                .build();
        order.getItems().add(item);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.transitionOrderStatusAndPaymentStatus(
                1001L, OrderStatus.PENDING, "PENDING", OrderStatus.CANCELLED, "CANCELLED"))
                .thenReturn(1);

        OrderResponse cancelledOrder = orderService.cancelPendingOrder(1001L, 1L);

        assertNotNull(cancelledOrder);
        verify(productRepository).incrementStock(101L, 2);

        // Idempotency: second call when order is already CANCELLED does NOT restore stock twice
        order.setStatus(OrderStatus.CANCELLED);
        reset(productRepository);
        orderService.cancelPendingOrder(1001L, 1L);
        verify(productRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Step 4: Tampered or Forged Cryptographic Signature Is Strictly Rejected")
    void test4_TamperedSignatureRejected() {
        order.setRazorpayOrderId("order_rzp_mock_1001");
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        PaymentVerificationRequest tamperedRequest = PaymentVerificationRequest.builder()
                .orderId(1001L)
                .razorpayOrderId("order_rzp_mock_1001")
                .razorpayPaymentId("pay_tampered_123")
                .razorpaySignature("forged_signature_hex_value_0000000000000000000000000000000000000000")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(tamperedRequest, 1L));

        assertTrue(ex.getMessage().contains("signature verification failed"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("PENDING", order.getPaymentStatus());
    }

    @Test
    @DisplayName("Step 4: Cross-Order Signature Mismatch Protection")
    void test5_CrossOrderSignatureMismatchRejected() {
        order.setRazorpayOrderId("order_rzp_mock_1001");
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        // Signature computed for a different order ID
        String validSigForForeignOrder = computeHmacSha256("order_rzp_foreign_9999|pay_sandbox_123", SANDBOX_KEY_SECRET);

        PaymentVerificationRequest crossOrderRequest = PaymentVerificationRequest.builder()
                .orderId(1001L)
                .razorpayOrderId("order_rzp_foreign_9999")
                .razorpayPaymentId("pay_sandbox_123")
                .razorpaySignature(validSigForForeignOrder)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(crossOrderRequest, 1L));

        assertTrue(ex.getMessage().contains("Razorpay order ID mismatch"));
    }

    @Test
    @DisplayName("Step 4: Duplicate Webhook Replay Is Idempotent and Does Not Double-Confirm")
    void test6_DuplicateWebhookIdempotency() throws Exception {
        order.setRazorpayOrderId("order_rzp_mock_1001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus("COMPLETED");

        when(orderRepository.findByRazorpayOrderId("order_rzp_mock_1001")).thenReturn(Optional.of(order));

        JSONObject webhookJson = new JSONObject();
        webhookJson.put("event", "order.paid");
        JSONObject payloadObj = new JSONObject();
        JSONObject entityObj = new JSONObject();
        entityObj.put("id", "order_rzp_mock_1001");
        payloadObj.put("order", new JSONObject().put("entity", entityObj));
        webhookJson.put("payload", payloadObj);

        String payloadStr = webhookJson.toString();
        String validWebhookSig = computeHmacSha256(payloadStr, SANDBOX_WEBHOOK_SECRET);

        boolean processed = paymentService.processWebhook(payloadStr, validWebhookSig);

        assertTrue(processed);
        // Verify save was NOT called since order was already completed
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("Step 4: Repeated Verify Callback Is Idempotent")
    void test7_VerifyCallbackIdempotency() {
        order.setRazorpayOrderId("order_rzp_mock_1001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus("COMPLETED");

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(1001L)
                .razorpayOrderId("order_rzp_mock_1001")
                .razorpayPaymentId("pay_already_verified")
                .razorpaySignature("sig")
                .build();

        OrderResponse res = paymentService.verifyPaymentSignature(request, 1L);

        assertNotNull(res);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals("COMPLETED", order.getPaymentStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    @DisplayName("Step 5: Unauthorized Customer Cannot Pay For Another User's Order")
    void test8_UnauthorizedUserAccessRejected() {
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        // Customer ID 999 attempts to access Customer ID 1's order
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.createRazorpayOrder(1001L, 999L));

        assertTrue(ex.getMessage().contains("not authorized to pay"));
    }

    @Test
    @DisplayName("Step 5: Paid Orders Cannot Be Cancelled Via Pending Cancellation")
    void test9_PaidOrderCannotBeCancelledViaPending() {
        order.setPaymentStatus("COMPLETED");
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelPendingOrder(1001L, 1L));

        assertTrue(ex.getMessage().contains("Paid orders cannot be cancelled via pending cancellation"));
    }

    @Test
    @DisplayName("Step 3: Cash on Delivery (COD) Flow Bypasses Razorpay & Initializes PENDING")
    void test10_CashOnDeliveryFlow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(101L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest address = AddressRequest.builder()
                .fullName("Priya Sharma")
                .phone("9876543210")
                .streetAddress("12th Main Road, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .shippingAddress(address)
                .paymentMethod("COD")
                .build();

        OrderResponse codOrder = orderService.createOrder(1L, orderRequest);

        assertNotNull(codOrder);
        assertEquals("PENDING", codOrder.getStatus());
        assertEquals("PENDING", codOrder.getPaymentStatus());
        assertNull(codOrder.getRazorpayOrderId());
        verify(productRepository).decrementStockIfAvailable(101L, 2);
    }
}
