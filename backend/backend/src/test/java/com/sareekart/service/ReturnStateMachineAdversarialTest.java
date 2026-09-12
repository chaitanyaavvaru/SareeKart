package com.sareekart.service;

import com.sareekart.controller.AdminReturnController;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Adversarial Empirical Verification Test Suite by Challenger 2.
 * Strictly stress-tests:
 * 1. Valid state progression: PENDING -> APPROVED -> PICKUP_SCHEDULED -> COMPLETED
 * 2. Illegal state transitions:
 *    - PENDING -> COMPLETED directly
 *    - COMPLETED -> PENDING
 *    - REJECTED -> APPROVED
 *    - PENDING -> PICKUP_SCHEDULED
 *    - APPROVED -> COMPLETED
 *    - REJECTED -> COMPLETED
 * 3. Required fields on transitions:
 *    - PICKUP_SCHEDULED without reverse courier or AWB
 *    - REJECTED without admin notes
 * 4. Admin RBAC:
 *    - Non-staff (CUSTOMER, null) users blocked at service and controller levels.
 */
@ExtendWith(MockitoExtension.class)
public class ReturnStateMachineAdversarialTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventService notificationEventService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customerUser;
    private User adminUser;
    private User managerUser;
    private User ownerUser;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(101L)
                .firstName("Kavita")
                .lastName("Reddy")
                .email("kavita@example.com")
                .role(Role.CUSTOMER)
                .build();

        adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .email("admin@sareekart.com")
                .role(Role.ADMIN)
                .build();

        managerUser = User.builder()
                .id(2L)
                .firstName("Manager")
                .email("manager@sareekart.com")
                .role(Role.MANAGER)
                .build();

        ownerUser = User.builder()
                .id(3L)
                .firstName("Owner")
                .email("owner@sareekart.com")
                .role(Role.OWNER)
                .build();

        sampleOrder = Order.builder()
                .id(500L)
                .user(customerUser)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("4999.00"))
                .deliveredAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    private ReturnRequest createMockReturn(ReturnStatus status) {
        return ReturnRequest.builder()
                .id(999L)
                .order(sampleOrder)
                .user(customerUser)
                .status(status)
                .type(ReturnType.RETURN)
                .reason(ReturnReason.COLOR_MISMATCH)
                .refundMode(RefundMode.ORIGINAL_PAYMENT)
                .refundAmount(new BigDecimal("4999.00"))
                .images(new ArrayList<>(List.of("/uploads/return-photos/sample.jpg")))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(5))
                .build();
    }

    // =========================================================================
    // Group 1: Valid State Transitions (PENDING -> APPROVED -> PICKUP_SCHEDULED -> COMPLETED)
    // =========================================================================
    @Nested
    @DisplayName("1. Valid State Transitions")
    class ValidStateTransitions {

        @Test
        @DisplayName("Valid Full Lifecycle: PENDING -> APPROVED -> PICKUP_SCHEDULED -> COMPLETED")
        void testSequentialValidLifecycle() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

            // Step 1: PENDING -> APPROVED
            ReturnStatusUpdateRequest approveReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .adminNotes("Approved after visual inspection of fabric color.")
                    .build();
            ReturnResponse resp1 = returnService.updateReturnStatus(999L, approveReq, adminUser);
            assertNotNull(resp1);
            assertEquals("APPROVED", resp1.getStatus());
            assertEquals(ReturnStatus.APPROVED, claim.getStatus());
            assertEquals("Approved after visual inspection of fabric color.", claim.getAdminNotes());

            // Step 2: APPROVED -> PICKUP_SCHEDULED
            ReturnStatusUpdateRequest scheduleReq = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("Blue Dart Reverse Logistics")
                    .reverseTrackingNumber("BDR-778899")
                    .adminNotes("Pickup scheduled for customer address tomorrow morning.")
                    .build();
            ReturnResponse resp2 = returnService.updateReturnStatus(999L, scheduleReq, managerUser);
            assertNotNull(resp2);
            assertEquals("PICKUP_SCHEDULED", resp2.getStatus());
            assertEquals("Blue Dart Reverse Logistics", resp2.getReverseCourier());
            assertEquals("BDR-778899", resp2.getReverseTrackingNumber());
            assertEquals(ReturnStatus.PICKUP_SCHEDULED, claim.getStatus());

            // Step 3: PICKUP_SCHEDULED -> COMPLETED
            ReturnStatusUpdateRequest completeReq = ReturnStatusUpdateRequest.builder()
                    .status("COMPLETED")
                    .refundAmount(new BigDecimal("4999.00"))
                    .adminNotes("Warehouse received and verified saree intact. Full refund processed.")
                    .build();
            ReturnResponse resp3 = returnService.updateReturnStatus(999L, completeReq, ownerUser);
            assertNotNull(resp3);
            assertEquals("COMPLETED", resp3.getStatus());
            assertEquals(ReturnStatus.COMPLETED, claim.getStatus());
            assertEquals(new BigDecimal("4999.00"), resp3.getRefundAmount());
            verify(returnRequestRepository, times(3)).save(claim);
        }

        @Test
        @DisplayName("Valid Alternative Lifecycle: PENDING -> REJECTED with notes")
        void testPendingToRejectedWithNotes() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

            ReturnStatusUpdateRequest rejectReq = ReturnStatusUpdateRequest.builder()
                    .status("REJECTED")
                    .adminNotes("Security seal broken and product used.")
                    .build();

            ReturnResponse resp = returnService.updateReturnStatus(999L, rejectReq, adminUser);
            assertNotNull(resp);
            assertEquals("REJECTED", resp.getStatus());
            assertEquals(ReturnStatus.REJECTED, claim.getStatus());
            assertEquals("Security seal broken and product used.", claim.getAdminNotes());
        }
    }

    // =========================================================================
    // Group 2: Illegal State Transitions
    // =========================================================================
    @Nested
    @DisplayName("2. Illegal State Transitions")
    class IllegalStateTransitions {

        @Test
        @DisplayName("Illegal transition: PENDING -> COMPLETED directly must throw BadRequestException")
        void testPendingToCompletedDirectlyFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("COMPLETED")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Invalid state transition from PENDING to COMPLETED") ||
                       ex.getMessage().contains("Claim must first be APPROVED"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Illegal transition: PENDING -> PICKUP_SCHEDULED directly must throw BadRequestException")
        void testPendingToPickupScheduledDirectlyFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("Delhivery")
                    .reverseTrackingNumber("DLV-111")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Invalid state transition from PENDING"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Illegal transition: COMPLETED -> PENDING must throw BadRequestException (terminal state)")
        void testCompletedToPendingFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.COMPLETED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("PENDING")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Cannot alter status of an already completed return request"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Illegal transition: COMPLETED -> APPROVED must throw BadRequestException")
        void testCompletedToApprovedFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.COMPLETED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Cannot alter status of an already completed return request"));
        }

        @Test
        @DisplayName("Illegal transition: REJECTED -> APPROVED must throw BadRequestException (terminal state)")
        void testRejectedToApprovedFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.REJECTED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .adminNotes("Attempting to un-reject")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Cannot alter status of an already rejected return request"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Illegal transition: REJECTED -> PENDING must throw BadRequestException")
        void testRejectedToPendingFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.REJECTED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("PENDING")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Cannot alter status of an already rejected return request"));
        }

        @Test
        @DisplayName("Illegal transition: REJECTED -> COMPLETED must throw BadRequestException")
        void testRejectedToCompletedFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.REJECTED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("COMPLETED")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Cannot alter status of an already rejected return request"));
        }

        @Test
        @DisplayName("Illegal transition: APPROVED -> COMPLETED directly must throw BadRequestException")
        void testApprovedToCompletedDirectlyFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.APPROVED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("COMPLETED")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Invalid state transition from APPROVED to COMPLETED"));
        }

        @Test
        @DisplayName("Illegal transition: PICKUP_SCHEDULED -> PENDING must throw BadRequestException")
        void testPickupScheduledToPendingFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PICKUP_SCHEDULED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest illegalReq = ReturnStatusUpdateRequest.builder()
                    .status("PENDING")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, illegalReq, adminUser));

            assertTrue(ex.getMessage().contains("Invalid state transition from PICKUP_SCHEDULED to PENDING"));
        }
    }

    // =========================================================================
    // Group 3: Required Fields on Transitions
    // =========================================================================
    @Nested
    @DisplayName("3. Required Fields on Transitions")
    class RequiredFieldsValidation {

        @Test
        @DisplayName("Transition to PICKUP_SCHEDULED without reverse courier must fail")
        void testPickupScheduledWithoutReverseCourierFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.APPROVED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest reqNullCourier = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier(null)
                    .reverseTrackingNumber("AWB-12345")
                    .build();

            BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqNullCourier, managerUser));
            assertTrue(ex1.getMessage().contains("Reverse courier and tracking number are required"));

            ReturnStatusUpdateRequest reqBlankCourier = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("   ")
                    .reverseTrackingNumber("AWB-12345")
                    .build();

            BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqBlankCourier, managerUser));
            assertTrue(ex2.getMessage().contains("Reverse courier and tracking number are required"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Transition to PICKUP_SCHEDULED without reverse tracking number (AWB) must fail")
        void testPickupScheduledWithoutTrackingNumberFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.APPROVED);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest reqNullTracking = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("Blue Dart Reverse Logistics")
                    .reverseTrackingNumber(null)
                    .build();

            BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqNullTracking, managerUser));
            assertTrue(ex1.getMessage().contains("Reverse courier and tracking number are required"));

            ReturnStatusUpdateRequest reqEmptyTracking = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("Blue Dart Reverse Logistics")
                    .reverseTrackingNumber("")
                    .build();

            BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqEmptyTracking, managerUser));
            assertTrue(ex2.getMessage().contains("Reverse courier and tracking number are required"));
            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Transition to REJECTED without admin notes must fail")
        void testRejectedWithoutAdminNotesFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            // Null notes
            ReturnStatusUpdateRequest reqNullNotes = ReturnStatusUpdateRequest.builder()
                    .status("REJECTED")
                    .adminNotes(null)
                    .build();

            BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqNullNotes, adminUser));
            assertTrue(ex1.getMessage().contains("Mandatory rejection reason must be provided in admin notes"));

            // Empty notes
            ReturnStatusUpdateRequest reqEmptyNotes = ReturnStatusUpdateRequest.builder()
                    .status("REJECTED")
                    .adminNotes("")
                    .build();

            BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqEmptyNotes, adminUser));
            assertTrue(ex2.getMessage().contains("Mandatory rejection reason must be provided in admin notes"));

            // Whitespace notes
            ReturnStatusUpdateRequest reqWhitespaceNotes = ReturnStatusUpdateRequest.builder()
                    .status("REJECTED")
                    .adminNotes("    \t  \n  ")
                    .build();

            BadRequestException ex3 = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, reqWhitespaceNotes, adminUser));
            assertTrue(ex3.getMessage().contains("Mandatory rejection reason must be provided in admin notes"));

            verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
        }

        @Test
        @DisplayName("Target status null or empty must fail")
        void testTargetStatusMissingFails() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));

            ReturnStatusUpdateRequest emptyStatusReq = ReturnStatusUpdateRequest.builder()
                    .status("   ")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    returnService.updateReturnStatus(999L, emptyStatusReq, adminUser));
            assertTrue(ex.getMessage().contains("Target status is mandatory"));
        }
    }

    // =========================================================================
    // Group 4: Admin RBAC & Access Controls
    // =========================================================================
    @Nested
    @DisplayName("4. Admin RBAC & Access Control Enforcement")
    class AdminRbacControls {

        @Test
        @DisplayName("Customer cannot invoke updateReturnStatus - throws AccessDeniedException")
        void testCustomerRoleCannotUpdateReturnStatus() {
            ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .build();

            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    returnService.updateReturnStatus(999L, updateReq, customerUser));

            assertEquals("Not authorised to perform this action", ex.getMessage());
            verify(returnRequestRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Unauthenticated / null user cannot invoke updateReturnStatus - throws AccessDeniedException")
        void testNullUserCannotUpdateReturnStatus() {
            ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                    .status("APPROVED")
                    .build();

            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    returnService.updateReturnStatus(999L, updateReq, null));

            assertEquals("Not authorised to perform this action", ex.getMessage());
            verify(returnRequestRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Customer cannot fetch admin return list - throws AccessDeniedException")
        void testCustomerCannotFetchAdminReturnList() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    returnService.getAllReturnsForAdmin("ALL", customerUser));

            assertEquals("Not authorised to perform this action", ex.getMessage());
            verify(returnRequestRepository, never()).findAllByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("Staff roles OWNER, MANAGER, ADMIN are permitted to invoke updateReturnStatus")
        void testStaffRolesPermitted() {
            ReturnRequest claim = createMockReturn(ReturnStatus.PENDING);
            when(returnRequestRepository.findById(999L)).thenReturn(Optional.of(claim));
            when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

            // Test ADMIN
            ReturnStatusUpdateRequest req = ReturnStatusUpdateRequest.builder().status("APPROVED").build();
            ReturnResponse r1 = returnService.updateReturnStatus(999L, req, adminUser);
            assertNotNull(r1);

            // Test MANAGER
            claim.setStatus(ReturnStatus.APPROVED);
            ReturnStatusUpdateRequest req2 = ReturnStatusUpdateRequest.builder()
                    .status("PICKUP_SCHEDULED")
                    .reverseCourier("DTDC")
                    .reverseTrackingNumber("DT-1234")
                    .build();
            ReturnResponse r2 = returnService.updateReturnStatus(999L, req2, managerUser);
            assertNotNull(r2);

            // Test OWNER
            claim.setStatus(ReturnStatus.PICKUP_SCHEDULED);
            ReturnStatusUpdateRequest req3 = ReturnStatusUpdateRequest.builder()
                    .status("COMPLETED")
                    .build();
            ReturnResponse r3 = returnService.updateReturnStatus(999L, req3, ownerUser);
            assertNotNull(r3);
        }

        @Test
        @DisplayName("Admin controller layer returns HTTP 403 Forbidden with exact payload when AccessDeniedException thrown")
        void testAdminControllerAccessDeniedEndpointIntegration() throws Exception {
            ReturnService mockService = mock(ReturnService.class);
            AdminReturnController controller = new AdminReturnController(mockService);
            MockMvc mockMvc = standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

            // When customer calls updateReturnStatus through endpoint
            when(mockService.updateReturnStatus(eq(999L), any(), any()))
                    .thenThrow(new AccessDeniedException("Not authorised to perform this action"));

            String requestJson = "{\"status\":\"APPROVED\",\"adminNotes\":\"Attempt by customer\"}";

            mockMvc.perform(put("/api/admin/returns/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
        }

        @Test
        @DisplayName("Admin controller layer returns HTTP 400 when Bean Validation fails on missing status")
        void testAdminControllerValidationFailure() throws Exception {
            ReturnService mockService = mock(ReturnService.class);
            AdminReturnController controller = new AdminReturnController(mockService);
            MockMvc mockMvc = standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

            // Request body without mandatory status field
            String requestJson = "{\"adminNotes\":\"Missing status field\"}";

            mockMvc.perform(put("/api/admin/returns/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.status").exists());
        }
    }
}
