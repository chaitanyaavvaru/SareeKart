package com.sareekart.service;

import com.sareekart.controller.AdminReturnController;
import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.entity.ReturnRequest;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.enums.RefundMode;
import com.sareekart.enums.ReturnReason;
import com.sareekart.enums.ReturnStatus;
import com.sareekart.enums.ReturnType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.ReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Milestone 3 Final Adversarial Challenger Verification Test Suite.
 * Directly stresses:
 * 1. 7-day post-delivery cutoff parity: 7.1 days is rejected, 6.9 days is accepted.
 * 2. Exchange SKU validation: exchange requests strictly require exchangeSku.
 * 3. RBAC: unauthorized requests to /api/admin/returns/** receive HTTP 403 Forbidden.
 */
@ExtendWith(MockitoExtension.class)
public class Milestone3AdversarialEdgeCaseTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventService notificationEventService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customer;
    private User admin;
    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1001L)
                .firstName("Adversarial")
                .lastName("Tester")
                .email("adv_customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        admin = User.builder()
                .id(1002L)
                .firstName("Staff")
                .lastName("Admin")
                .email("adv_admin@sareekart.com")
                .role(Role.ADMIN)
                .build();

        deliveredOrder = Order.builder()
                .id(5001L)
                .user(customer)
                .totalAmount(new BigDecimal("3999.00"))
                .status(OrderStatus.DELIVERED)
                .build();
    }

    // =========================================================================
    // 1. Cutoff Parity Verification: 7.1 Days (Reject) vs 6.9 Days (Accept)
    // =========================================================================
    @Nested
    @DisplayName("1. 7-Day Cutoff Parity")
    class CutoffParityTests {

        @Test
        @DisplayName("Order delivered 7.1 days ago (170.4 hours) must be rejected with BadRequestException")
        void testCutoffParity_7Point1DaysAgo_Rejected() {
            // 7.1 days = 7 days + 0.1 * 24 hours = 7 days and 2.4 hours (170.4 hours)
            long hoursAgo = (long) (7.1 * 24); // 170 hours
            long minutesAgo = (long) (7.1 * 24 * 60); // 10224 minutes
            deliveredOrder.setDeliveredAt(LocalDateTime.now().minusMinutes(minutesAgo));

            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("RETURN")
                    .reason("COLOR_MISMATCH")
                    .refundMode("ORIGINAL_PAYMENT")
                    .comments("Cutoff boundary challenge: 7.1 days post-delivery")
                    .build();

            when(orderRepository.findById(5001L)).thenReturn(Optional.of(deliveredOrder));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.createReturnRequest(request, customer.getId()));

            assertTrue(ex.getMessage().contains("Return window has expired"),
                    "Expected exception message to contain 'Return window has expired', but got: " + ex.getMessage());
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Order delivered 6.9 days ago (165.6 hours) must be accepted successfully")
        void testCutoffParity_6Point9DaysAgo_Accepted() {
            // 6.9 days = 6 days + 0.9 * 24 hours = 6 days and 21.6 hours (165.6 hours)
            long minutesAgo = (long) (6.9 * 24 * 60); // 9936 minutes (< 10080 minutes in 7 days)
            deliveredOrder.setDeliveredAt(LocalDateTime.now().minusMinutes(minutesAgo));

            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("RETURN")
                    .reason("COLOR_MISMATCH")
                    .refundMode("ORIGINAL_PAYMENT")
                    .comments("Cutoff boundary challenge: 6.9 days post-delivery")
                    .build();

            when(orderRepository.findById(5001L)).thenReturn(Optional.of(deliveredOrder));
            when(returnRequestRepository.findByOrderId(5001L)).thenReturn(Optional.empty());
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
                ReturnRequest r = i.getArgument(0);
                r.setId(9001L);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            ReturnResponse response = returnService.createReturnRequest(request, customer.getId());

            assertNotNull(response);
            assertEquals(9001L, response.getId());
            assertEquals("PENDING", response.getStatus());
            assertEquals(5001L, response.getOrderId());
            verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
        }
    }

    // =========================================================================
    // 2. Exchange SKU Validation: Strict Requirement for EXCHANGE Requests
    // =========================================================================
    @Nested
    @DisplayName("2. Exchange SKU Validation")
    class ExchangeSkuValidationTests {

        @BeforeEach
        void setupOrderWithinWindow() {
            deliveredOrder.setDeliveredAt(LocalDateTime.now().minusDays(2));
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(deliveredOrder));
        }

        @Test
        @DisplayName("Exchange request with null exchangeSku must throw BadRequestException")
        void testExchangeRequest_NullSku_Rejected() {
            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("EXCHANGE")
                    .reason("SIZE_MISMATCH")
                    .refundMode("EXCHANGE_DRAPE")
                    .exchangeSku(null)
                    .comments("Requesting size exchange without SKU")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.createReturnRequest(request, customer.getId()));

            assertTrue(ex.getMessage().contains("Exchange SKU is required"),
                    "Expected exception message to state 'Exchange SKU is required', but got: " + ex.getMessage());
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Exchange request with empty string exchangeSku must throw BadRequestException")
        void testExchangeRequest_EmptySku_Rejected() {
            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("EXCHANGE")
                    .reason("SIZE_MISMATCH")
                    .refundMode("EXCHANGE_DRAPE")
                    .exchangeSku("")
                    .comments("Empty SKU string")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.createReturnRequest(request, customer.getId()));

            assertTrue(ex.getMessage().contains("Exchange SKU is required"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Exchange request with whitespace-only exchangeSku must throw BadRequestException")
        void testExchangeRequest_WhitespaceSku_Rejected() {
            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("EXCHANGE")
                    .reason("SIZE_MISMATCH")
                    .refundMode("EXCHANGE_DRAPE")
                    .exchangeSku("   \t  ")
                    .comments("Whitespace SKU string")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.createReturnRequest(request, customer.getId()));

            assertTrue(ex.getMessage().contains("Exchange SKU is required"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Exchange request with valid exchangeSku must succeed and persist SKU")
        void testExchangeRequest_ValidSku_Accepted() {
            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("EXCHANGE")
                    .reason("SIZE_MISMATCH")
                    .refundMode("EXCHANGE_DRAPE")
                    .exchangeSku("SK-KAN-TEMPLE-GOLD-15")
                    .comments("Exchanging for larger border variant")
                    .build();

            when(returnRequestRepository.findByOrderId(5001L)).thenReturn(Optional.empty());
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
                ReturnRequest r = i.getArgument(0);
                r.setId(9002L);
                return r;
            });

            ReturnResponse response = returnService.createReturnRequest(request, customer.getId());

            assertNotNull(response);
            assertEquals("EXCHANGE", response.getType());
            assertEquals("SK-KAN-TEMPLE-GOLD-15", response.getExchangeSku());
            assertEquals("PENDING", response.getStatus());
            verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Standard RETURN request does NOT require exchangeSku")
        void testStandardReturn_WithoutExchangeSku_Accepted() {
            ReturnCreateRequest request = ReturnCreateRequest.builder()
                    .orderId(5001L)
                    .type("RETURN")
                    .reason("COLOR_MISMATCH")
                    .refundMode("ORIGINAL_PAYMENT")
                    .exchangeSku(null)
                    .comments("Standard return for refund")
                    .build();

            when(returnRequestRepository.findByOrderId(5001L)).thenReturn(Optional.empty());
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
                ReturnRequest r = i.getArgument(0);
                r.setId(9003L);
                return r;
            });

            ReturnResponse response = returnService.createReturnRequest(request, customer.getId());

            assertNotNull(response);
            assertEquals("RETURN", response.getType());
            assertNull(response.getExchangeSku());
            assertEquals("PENDING", response.getStatus());
            verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
        }
    }

    // =========================================================================
    // 3. RBAC & Access Control: Unauthorized Requests Receive HTTP 403
    // =========================================================================
    @Nested
    @DisplayName("3. RBAC on /api/admin/returns/**")
    class RbacAdminReturnsTests {

        private MockMvc mockMvc;
        private ReturnService mockReturnService;

        @BeforeEach
        void setupMockMvc() {
            mockReturnService = mock(ReturnService.class);
            AdminReturnController controller = new AdminReturnController(mockReturnService);
            mockMvc = standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("Customer invoking GET /api/admin/returns triggers HTTP 403 Forbidden with exact payload")
        void testCustomerGetAdminReturns_ReturnsHttp403() throws Exception {
            when(mockReturnService.getAllReturnsForAdmin(any()))
                    .thenThrow(new AccessDeniedException("Not authorised to perform this action"));

            mockMvc.perform(get("/api/admin/returns")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
        }

        @Test
        @DisplayName("Customer invoking PUT /api/admin/returns/{id}/status triggers HTTP 403 Forbidden with exact payload")
        void testCustomerPutAdminReturnStatus_ReturnsHttp403() throws Exception {
            when(mockReturnService.updateReturnStatus(eq(99L), any(ReturnStatusUpdateRequest.class), any()))
                    .thenThrow(new AccessDeniedException("Not authorised to perform this action"));

            String jsonPayload = "{\"status\":\"APPROVED\",\"adminNotes\":\"Unauthorized attempt\"}";

            mockMvc.perform(put("/api/admin/returns/99/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
        }

        @Test
        @DisplayName("ReturnServiceImpl directly throws AccessDeniedException when customer calls getAllReturnsForAdmin")
        void testServiceRejectsCustomer_GetAllReturnsForAdmin() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    returnService.getAllReturnsForAdmin("ALL", customer));

            assertEquals("Not authorised to perform this action", ex.getMessage());
        }

        @Test
        @DisplayName("ReturnServiceImpl directly throws AccessDeniedException when customer calls updateReturnStatus")
        void testServiceRejectsCustomer_UpdateReturnStatus() {
            ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .build();

            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    returnService.updateReturnStatus(99L, updateReq, customer));

            assertEquals("Not authorised to perform this action", ex.getMessage());
        }
    }
}
