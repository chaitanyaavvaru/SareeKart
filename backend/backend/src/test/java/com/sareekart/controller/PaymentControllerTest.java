package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(10L)
                .email("priya.sharma@example.com")
                .role(Role.CUSTOMER)
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/create-order/{id}: Returns 200 with Razorpay order details")
    void createPaymentOrder_Success() throws Exception {
        PaymentOrderResponse response = PaymentOrderResponse.builder()
                .razorpayOrderId("order_rzp_123")
                .amount(new BigDecimal("15000.00"))
                .currency("INR")
                .keyId("rzp_test_key_123")
                .build();

        when(paymentService.createRazorpayOrder(100L, 10L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_rzp_123"))
                .andExpect(jsonPath("$.data.amount").value(15000.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.keyId").value("rzp_test_key_123"));

        verify(paymentService).createRazorpayOrder(100L, 10L);
    }

    @Test
    @DisplayName("POST /api/payments/verify: Valid verification request returns 200 and confirmed order")
    void verifyPayment_Success() throws Exception {
        PaymentVerificationRequest request = PaymentVerificationRequest.builder()
                .orderId(100L)
                .razorpayOrderId("order_rzp_123")
                .razorpayPaymentId("pay_rzp_456")
                .razorpaySignature("valid_signature_hash")
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .id(100L)
                .status("CONFIRMED")
                .paymentStatus("COMPLETED")
                .totalAmount(new BigDecimal("15000.00"))
                .build();

        when(paymentService.verifyPaymentSignature(any(PaymentVerificationRequest.class), eq(10L)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"));

        verify(paymentService).verifyPaymentSignature(any(PaymentVerificationRequest.class), eq(10L));
    }

    @Test
    @DisplayName("POST /api/payments/verify: Rejects request with missing required fields")
    void verifyPayment_MissingFields_Returns400() throws Exception {
        PaymentVerificationRequest invalidRequest = PaymentVerificationRequest.builder()
                .orderId(null) // Required field missing
                .razorpayOrderId("")
                .build();

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(paymentService, never()).verifyPaymentSignature(any(), any());
    }

    @Test
    @DisplayName("POST /api/payments/webhook: Processes webhook successfully and returns 200 OK")
    void handleWebhook_Success() throws Exception {
        String payload = "{\"event\":\"order.paid\"}";
        String signature = "test_signature_hex";

        when(paymentService.processWebhook(payload, signature)).thenReturn(true);

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("OK"));

        verify(paymentService).processWebhook(payload, signature);
    }

    @Test
    @DisplayName("POST /api/payments/webhook: Returns 400 when webhook signature is rejected")
    void handleWebhook_InvalidSignature_Returns400() throws Exception {
        String payload = "{\"event\":\"order.paid\"}";
        String invalidSignature = "tampered_signature";

        when(paymentService.processWebhook(payload, invalidSignature))
                .thenThrow(new BadRequestException("Invalid webhook signature."));

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", invalidSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature."));
    }
}
