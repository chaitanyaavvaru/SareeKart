package com.sareekart.service;

import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.PaymentConfigurationException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.PaymentServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static final String TEST_KEY_ID = "rzp_test_1234567890abcdef";
    private static final String TEST_KEY_SECRET = "test_secret_key_abcdef123456";
    private static final String TEST_WEBHOOK_SECRET = "test_webhook_secret_987654321";

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("priya.sharma@example.com")
                .build();

        testOrder = Order.builder()
                .id(100L)
                .user(testUser)
                .totalAmount(new BigDecimal("12500.00"))
                .status(OrderStatus.PENDING)
                .paymentStatus("PENDING")
                .paymentMethod("RAZORPAY")
                .razorpayOrderId("order_test_100")
                .build();

        paymentService.setKeyId(TEST_KEY_ID);
        paymentService.setKeySecret(TEST_KEY_SECRET);
        paymentService.setWebhookSecret(TEST_WEBHOOK_SECRET);
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
    @DisplayName("Payment Order Creation: Succeeds for authorized pending order")
    void createRazorpayOrder_Success() throws Exception {
        // Arrange
        razorpayClient.orders = orderClient;
        JSONObject razorpayOrderJson = new JSONObject();
        razorpayOrderJson.put("id", "order_rzp_mock_123");
        com.razorpay.Order mockRzpOrder = new com.razorpay.Order(razorpayOrderJson);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentOrderResponse response = paymentService.createRazorpayOrder(100L, 1L);

        // Assert
        assertNotNull(response);
        assertEquals("order_rzp_mock_123", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("12500.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(TEST_KEY_ID, response.getKeyId());
        assertEquals("order_rzp_mock_123", testOrder.getRazorpayOrderId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Payment Order Creation: Rejects unauthorized user attempting to pay for another user's order")
    void createRazorpayOrder_UnauthorizedUser_ThrowsBadRequest() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.createRazorpayOrder(100L, 999L));

        assertTrue(ex.getMessage().contains("not authorized to pay"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment Order Creation: Rejects already paid or confirmed order")
    void createRazorpayOrder_NonPendingOrder_ThrowsBadRequest() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.createRazorpayOrder(100L, 1L));

        assertTrue(ex.getMessage().contains("already paid or cancelled"));
    }

    @Test
    @DisplayName("Payment Order Creation: Fails fast if Razorpay credentials are missing")
    void createRazorpayOrder_Unconfigured_ThrowsPaymentConfigurationException() {
        paymentService.setKeyId(null);
        paymentService.setRazorpayClient(null);

        assertThrows(PaymentConfigurationException.class, () ->
                paymentService.createRazorpayOrder(100L, 1L));
    }

    @Test
    @DisplayName("Payment Verification: Valid cryptographic HMAC-SHA256 signature confirms order")
    void verifyPaymentSignature_ValidSignature_ConfirmsOrder() {
        // Arrange
        String razorpayOrderId = "order_test_100";
        String razorpayPaymentId = "pay_test_987654";
        String validSignature = computeHmacSha256(razorpayOrderId + "|" + razorpayPaymentId, TEST_KEY_SECRET);

        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(razorpayPaymentId)
                .razorpaySignature(validSignature)
                .build();

        OrderResponse mockResponse = OrderResponse.builder()
                .id(100L)
                .status("CONFIRMED")
                .paymentStatus("COMPLETED")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mockResponse);

        // Act
        OrderResponse result = paymentService.verifyPaymentSignature(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("COMPLETED", result.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        assertEquals("COMPLETED", testOrder.getPaymentStatus());
        assertEquals(razorpayPaymentId, testOrder.getRazorpayPaymentId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Payment Verification: Forged or invalid signature is rejected")
    void verifyPaymentSignature_ForgedSignature_ThrowsBadRequest() {
        // Arrange
        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId("order_test_100")
                .razorpayPaymentId("pay_test_987654")
                .razorpaySignature("forged_tampered_signature_hex_value")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(request, 1L));

        assertTrue(ex.getMessage().contains("signature verification failed"));
        assertEquals(OrderStatus.PENDING, testOrder.getStatus());
        assertNotEquals("COMPLETED", testOrder.getPaymentStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment Verification: Cross-order mismatch protection prevents applying signature to different order")
    void verifyPaymentSignature_CrossOrderMismatch_ThrowsBadRequest() {
        // Arrange: valid signature for order_other_999, but request orderId is 100 which has order_test_100
        String foreignOrderId = "order_other_999";
        String razorpayPaymentId = "pay_test_987654";
        String validForeignSignature = computeHmacSha256(foreignOrderId + "|" + razorpayPaymentId, TEST_KEY_SECRET);

        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId(foreignOrderId)
                .razorpayPaymentId(razorpayPaymentId)
                .razorpaySignature(validForeignSignature)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(request, 1L));

        assertTrue(ex.getMessage().contains("Razorpay order ID mismatch"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment Verification: Cancelled order rejects verification")
    void verifyPaymentSignature_CancelledOrder_ThrowsBadRequest() {
        testOrder.setStatus(OrderStatus.CANCELLED);
        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId("order_test_100")
                .razorpayPaymentId("pay_test_987654")
                .razorpaySignature("any_sig")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(request, 1L));

        assertTrue(ex.getMessage().contains("already been cancelled"));
    }

    @Test
    @DisplayName("Payment Verification: Idempotent replay on already confirmed order returns cleanly")
    void verifyPaymentSignature_IdempotentReplay_ReturnsExistingOrder() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        testOrder.setPaymentStatus("COMPLETED");

        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId("order_test_100")
                .razorpayPaymentId("pay_test_987654")
                .razorpaySignature("any_sig")
                .build();

        OrderResponse mockResponse = OrderResponse.builder()
                .id(100L)
                .status("CONFIRMED")
                .paymentStatus("COMPLETED")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(mockResponse);

        OrderResponse result = paymentService.verifyPaymentSignature(request, 1L);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment Verification: Unauthorized user cannot verify another user's payment")
    void verifyPaymentSignature_UnauthorizedUser_ThrowsBadRequest() {
        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId("order_test_100")
                .razorpayPaymentId("pay_test_987654")
                .razorpaySignature("sig")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.verifyPaymentSignature(request, 999L));

        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("Webhook: Valid 'order.paid' event transitions order to CONFIRMED / COMPLETED")
    void processWebhook_OrderPaid_ConfirmsOrder() {
        // Arrange
        String payload = "{" +
                "\"entity\":\"event\"," +
                "\"event\":\"order.paid\"," +
                "\"payload\":{" +
                "  \"payment\":{\"entity\":{\"id\":\"pay_webhook_999\",\"order_id\":\"order_test_100\"}}," +
                "  \"order\":{\"entity\":{\"id\":\"order_test_100\"}}" +
                "}" +
                "}";

        String signature = computeHmacSha256(payload, TEST_WEBHOOK_SECRET);

        when(orderRepository.findByRazorpayOrderId("order_test_100")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean handled = paymentService.processWebhook(payload, signature);

        // Assert
        assertTrue(handled);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        assertEquals("COMPLETED", testOrder.getPaymentStatus());
        assertEquals("pay_webhook_999", testOrder.getRazorpayPaymentId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Webhook: Valid 'payment.captured' event transitions order to CONFIRMED / COMPLETED")
    void processWebhook_PaymentCaptured_ConfirmsOrder() {
        // Arrange
        String payload = "{" +
                "\"entity\":\"event\"," +
                "\"event\":\"payment.captured\"," +
                "\"payload\":{" +
                "  \"payment\":{\"entity\":{\"id\":\"pay_webhook_captured_111\",\"order_id\":\"order_test_100\"}}" +
                "}" +
                "}";

        String signature = computeHmacSha256(payload, TEST_WEBHOOK_SECRET);

        when(orderRepository.findByRazorpayOrderId("order_test_100")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean handled = paymentService.processWebhook(payload, signature);

        // Assert
        assertTrue(handled);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        assertEquals("COMPLETED", testOrder.getPaymentStatus());
        assertEquals("pay_webhook_captured_111", testOrder.getRazorpayPaymentId());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Webhook: Duplicate replay for already confirmed order returns true without re-mutating")
    void processWebhook_DuplicateReplay_IdempotentSuccess() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        testOrder.setPaymentStatus("COMPLETED");

        String payload = "{" +
                "\"entity\":\"event\"," +
                "\"event\":\"order.paid\"," +
                "\"payload\":{" +
                "  \"payment\":{\"entity\":{\"id\":\"pay_webhook_dup\",\"order_id\":\"order_test_100\"}}" +
                "}" +
                "}";

        String signature = computeHmacSha256(payload, TEST_WEBHOOK_SECRET);

        when(orderRepository.findByRazorpayOrderId("order_test_100")).thenReturn(Optional.of(testOrder));

        boolean handled = paymentService.processWebhook(payload, signature);

        assertTrue(handled);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Webhook: 'payment.failed' event updates payment status to FAILED")
    void processWebhook_PaymentFailed_UpdatesStatus() {
        String payload = "{" +
                "\"entity\":\"event\"," +
                "\"event\":\"payment.failed\"," +
                "\"payload\":{" +
                "  \"payment\":{\"entity\":{\"id\":\"pay_fail_001\",\"order_id\":\"order_test_100\"}}" +
                "}" +
                "}";

        String signature = computeHmacSha256(payload, TEST_WEBHOOK_SECRET);

        when(orderRepository.findByRazorpayOrderId("order_test_100")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = paymentService.processWebhook(payload, signature);

        assertTrue(handled);
        assertEquals("FAILED", testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Webhook: Forged signature throws BadRequestException")
    void processWebhook_ForgedSignature_ThrowsBadRequest() {
        String payload = "{\"event\":\"order.paid\"}";
        String forgedSignature = "invalid_tampered_signature_value";

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.processWebhook(payload, forgedSignature));

        assertTrue(ex.getMessage().contains("Invalid webhook signature"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Webhook: Missing signature header throws BadRequestException")
    void processWebhook_MissingSignature_ThrowsBadRequest() {
        String payload = "{\"event\":\"order.paid\"}";

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.processWebhook(payload, null));

        assertTrue(ex.getMessage().contains("Missing X-Razorpay-Signature"));
    }

    @Test
    @DisplayName("Webhook: Missing webhook secret configuration throws PaymentConfigurationException")
    void processWebhook_UnconfiguredSecret_ThrowsConfigurationException() {
        paymentService.setWebhookSecret("");

        PaymentConfigurationException ex = assertThrows(PaymentConfigurationException.class, () ->
                paymentService.processWebhook("{}", "sig"));

        assertTrue(ex.getMessage().contains("webhook secret is not configured"));
    }
}
