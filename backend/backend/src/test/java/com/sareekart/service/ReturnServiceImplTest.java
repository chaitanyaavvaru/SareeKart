package com.sareekart.service;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.*;
import com.sareekart.enums.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.ReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Test Suite for ReturnServiceImpl.
 * Verifies 7-day post-delivery cutoff, order status gating, duplicate prevention,
 * customer data isolation, and staff moderation lifecycle state transitions.
 */
@ExtendWith(MockitoExtension.class)
public class ReturnServiceImplTest {

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

    @Mock
    private WhatsAppNotificationService whatsAppNotificationService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customerA;
    private User customerB;
    private User adminUser;
    private User managerUser;
    private User ownerUser;

    private Order deliveredOrder;
    private ReturnCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        customerA = User.builder()
                .id(10L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@example.com")
                .mobile("9876543210")
                .role(Role.CUSTOMER)
                .build();

        customerB = User.builder()
                .id(20L)
                .firstName("Rohan")
                .lastName("Verma")
                .email("rohan@example.com")
                .mobile("9123456780")
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

        deliveredOrder = Order.builder()
                .id(100L)
                .user(customerA)
                .totalAmount(new BigDecimal("2499.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(3))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();

        validCreateRequest = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("RETURN")
                .reason("COLOR_MISMATCH")
                .refundMode("ORIGINAL_PAYMENT")
                .comments("The color is noticeably darker navy than the royal blue shown on screen.")
                .images(List.of("/uploads/return-photos/return-defect1.jpg", "/uploads/return-photos/return-defect2.jpg"))
                .refundAmount(new BigDecimal("2499.00"))
                .build();
    }

    // =========================================================================
    // 1. Order Eligibility & 7-Day Window Tests
    // =========================================================================

    @Test
    void testCreateReturnRequest_DeliveredOrderWithin7Days_Success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));
        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest r = invocation.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getOrderId());
        assertEquals(customerA.getId(), response.getUserId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("COLOR_MISMATCH", response.getReason());
        assertEquals(new BigDecimal("2499.00"), response.getRefundAmount());
        assertEquals(2, response.getImages().size());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderInShippedStatus_ThrowsBadRequestException() {
        Order shippedOrder = Order.builder()
                .id(101L)
                .user(customerA)
                .status(OrderStatus.SHIPPED)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        validCreateRequest.setOrderId(101L);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(shippedOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderInPendingStatus_ThrowsBadRequestException() {
        Order pendingOrder = Order.builder()
                .id(102L)
                .user(customerA)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        validCreateRequest.setOrderId(102L);
        when(orderRepository.findById(102L)).thenReturn(Optional.of(pendingOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderDeliveredMoreThan7DaysAgo_ThrowsBadRequestException() {
        Order expiredOrder = Order.builder()
                .id(103L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(8)) // 8 days ago > 7 days cutoff
                .build();

        validCreateRequest.setOrderId(103L);
        when(orderRepository.findById(103L)).thenReturn(Optional.of(expiredOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_DeliveredBoundaryExact7Days_Success() {
        Order boundaryOrder = Order.builder()
                .id(104L)
                .user(customerA)
                .totalAmount(new BigDecimal("1800.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(6).minusHours(23).minusMinutes(50)) // Within 7 days
                .build();

        validCreateRequest.setOrderId(104L);
        when(orderRepository.findById(104L)).thenReturn(Optional.of(boundaryOrder));
        when(returnRequestRepository.findByOrderId(104L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest r = invocation.getArgument(0);
            r.setId(2L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_DeliveredBoundary7Days1Hour_Fails() {
        Order boundaryExpiredOrder = Order.builder()
                .id(105L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(7).minusHours(1)) // 7 days + 1 hr
                .build();

        validCreateRequest.setOrderId(105L);
        when(orderRepository.findById(105L)).thenReturn(Optional.of(boundaryExpiredOrder));

        assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_LegacyOrderNullDeliveredAt_UsesUpdatedAtFallback() {
        Order legacyOrder = Order.builder()
                .id(106L)
                .user(customerA)
                .totalAmount(new BigDecimal("3500.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(null) // Legacy order without deliveredAt
                .updatedAt(LocalDateTime.now().minusDays(2)) // Fallback reference
                .build();

        validCreateRequest.setOrderId(106L);
        when(orderRepository.findById(106L)).thenReturn(Optional.of(legacyOrder));
        when(returnRequestRepository.findByOrderId(106L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(3L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    // =========================================================================
    // 2. Duplicate Prevention & Customer Data Isolation
    // =========================================================================

    @Test
    void testCreateReturnRequest_DuplicateSubmission_ThrowsBadRequestException() {
        ReturnRequest existingReturn = ReturnRequest.builder()
                .id(10L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));
        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.of(existingReturn));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("already been submitted") || ex.getMessage().contains("already exists"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_UnauthorizedOrderOwnership_ThrowsException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));

        // Customer B attempts to submit return on order owned by Customer A
        assertThrows(Exception.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerB.getId()));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCustomerAccessControl_CannotAccessAnotherCustomersReturnClaim() {
        ReturnRequest customerAReturn = ReturnRequest.builder()
                .id(50L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.of(customerAReturn));

        // Customer B calls getReturnRequestByOrderId for order owned by Customer A
        assertThrows(Exception.class, () ->
                returnService.getReturnRequestByOrderId(100L, customerB.getId()));
    }

    @Test
    void testGetReturnRequestByOrderId_ReturnsNull_WhenNotFound() {
        when(returnRequestRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        ReturnResponse response = returnService.getReturnRequestByOrderId(999L, customerA.getId());
        assertNull(response);
    }

    // =========================================================================
    // 3. Staff Moderation & Lifecycle State Transitions
    // =========================================================================

    @Test
    void testStaffRole_Admin_CanApproveReturnRequest() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(50L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .adminNotes("Condition photo verified. Saree defect approved for return.")
                .build();

        when(returnRequestRepository.findById(50L)).thenReturn(Optional.of(pendingClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(50L, updateReq, adminUser);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Condition photo verified. Saree defect approved for return.", response.getAdminNotes());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_Manager_CanSchedulePickupWithCourierAndAWB() {
        ReturnRequest approvedClaim = ReturnRequest.builder()
                .id(51L)
                .order(deliveredOrder)
                .user(customerA)
                .status("APPROVED")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("PICKUP_SCHEDULED")
                .reverseCourier("Blue Dart Reverse Logistics")
                .reverseTrackingNumber("BDR-89214")
                .adminNotes("Pickup scheduled for tomorrow 10:00 AM - 2:00 PM")
                .build();

        when(returnRequestRepository.findById(51L)).thenReturn(Optional.of(approvedClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(51L, updateReq, managerUser);

        assertNotNull(response);
        assertEquals("PICKUP_SCHEDULED", response.getStatus());
        assertEquals("Blue Dart Reverse Logistics", response.getReverseCourier());
        assertEquals("BDR-89214", response.getReverseTrackingNumber());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_SchedulePickupWithoutCourierOrAWB_ThrowsBadRequestException() {
        ReturnRequest approvedClaim = ReturnRequest.builder()
                .id(51L)
                .order(deliveredOrder)
                .user(customerA)
                .status("APPROVED")
                .build();

        ReturnStatusUpdateRequest missingCourierReq = ReturnStatusUpdateRequest.builder()
                .status("PICKUP_SCHEDULED")
                .reverseCourier(null)
                .reverseTrackingNumber("BDR-89214")
                .build();

        when(returnRequestRepository.findById(51L)).thenReturn(Optional.of(approvedClaim));

        assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(51L, missingCourierReq, managerUser));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_Owner_CanCompleteReturn() {
        ReturnRequest pickupClaim = ReturnRequest.builder()
                .id(52L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PICKUP_SCHEDULED")
                .reverseCourier("Delhivery")
                .reverseTrackingNumber("DLV-123456")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("COMPLETED")
                .adminNotes("Saree inspection passed at Bengaluru hub. Full refund credited.")
                .build();

        when(returnRequestRepository.findById(52L)).thenReturn(Optional.of(pickupClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(52L, updateReq, ownerUser);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_CanRejectReturnWithMandatoryReason() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(53L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest rejectReq = ReturnStatusUpdateRequest.builder()
                .status("REJECTED")
                .adminNotes("Item shows visible alteration and missing original handloom tag.")
                .build();

        when(returnRequestRepository.findById(53L)).thenReturn(Optional.of(pendingClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(53L, rejectReq, adminUser);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals("Item shows visible alteration and missing original handloom tag.", response.getAdminNotes());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_RejectWithoutReason_ThrowsBadRequestException() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(53L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest invalidRejectReq = ReturnStatusUpdateRequest.builder()
                .status("REJECTED")
                .adminNotes("") // Blank rejection notes
                .build();

        when(returnRequestRepository.findById(53L)).thenReturn(Optional.of(pendingClaim));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(53L, invalidRejectReq, adminUser));

        assertTrue(ex.getMessage().contains("Mandatory rejection reason") || ex.getMessage().contains("admin notes"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCustomerRole_CannotUpdateReturnStatus_ThrowsAccessDeniedException() {
        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                returnService.updateReturnStatus(50L, updateReq, customerA));
    }

    @Test
    void testInvalidStateTransition_CompletedToPending_ThrowsBadRequestException() {
        ReturnRequest completedClaim = ReturnRequest.builder()
                .id(54L)
                .order(deliveredOrder)
                .user(customerA)
                .status("COMPLETED")
                .build();

        ReturnStatusUpdateRequest invalidReq = ReturnStatusUpdateRequest.builder()
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(54L)).thenReturn(Optional.of(completedClaim));

        assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(54L, invalidReq, adminUser));
    }

    @Test
    void testCreateReturnRequest_ExchangeTypeMissingSku_ThrowsBadRequestException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));

        ReturnCreateRequest exchangeReq = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("EXCHANGE")
                .reason("SIZE_MISMATCH")
                .refundMode("EXCHANGE_DRAPE")
                .exchangeSku(null) // Missing SKU
                .build();

        assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(exchangeReq, customerA.getId()));
    }

    @Test
    void testCreateReturnRequest_TooManyImages_ThrowsBadRequestException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));

        ReturnCreateRequest tooManyImagesReq = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("RETURN")
                .reason("COLOR_MISMATCH")
                .refundMode("ORIGINAL_PAYMENT")
                .images(List.of("/photo1.jpg", "/photo2.jpg", "/photo3.jpg", "/photo4.jpg"))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(tooManyImagesReq, customerA.getId()));

        assertTrue(ex.getMessage().contains("Maximum 3 defect photos allowed"));
    }

    @Test
    void testGetMyReturnRequests_ReturnsMappedList() {
        ReturnRequest req1 = ReturnRequest.builder()
                .id(1L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByUserIdOrderByCreatedAtDesc(customerA.getId()))
                .thenReturn(List.of(req1));

        List<ReturnResponse> list = returnService.getMyReturnRequests(customerA.getId());
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getId());
    }

    @Test
    void testGetAllReturnsForAdmin_FilterAll_ReturnsAll() {
        ReturnRequest req1 = ReturnRequest.builder()
                .id(1L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(req1));

        List<ReturnResponse> list = returnService.getAllReturnsForAdmin("ALL", adminUser);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void testGetAllReturnsForAdmin_FilterStatus_ReturnsFiltered() {
        ReturnRequest req1 = ReturnRequest.builder()
                .id(1L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByStatusOrderByCreatedAtDesc(ReturnStatus.PENDING))
                .thenReturn(List.of(req1));

        List<ReturnResponse> list = returnService.getAllReturnsForAdmin("PENDING", managerUser);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    // =========================================================================
    // 4. Challenger Empirical Stress Tests & Edge Case Verifications
    // =========================================================================

    @Test
    void testStress_Boundary_6Days23Hours_Eligible() {
        Order eligibleOrder = Order.builder()
                .id(201L)
                .user(customerA)
                .totalAmount(new BigDecimal("3299.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(6).minusHours(23))
                .build();

        validCreateRequest.setOrderId(201L);
        when(orderRepository.findById(201L)).thenReturn(Optional.of(eligibleOrder));
        when(returnRequestRepository.findByOrderId(201L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(101L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(201L, response.getOrderId());
    }

    @Test
    void testStress_Boundary_7Days1Minute_Expired_ThrowsBadRequestException() {
        Order expiredOrder = Order.builder()
                .id(202L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(7).minusMinutes(1))
                .build();

        validCreateRequest.setOrderId(202L);
        when(orderRepository.findById(202L)).thenReturn(Optional.of(expiredOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_Boundary_Exactly7Days_ExpiredAtCutoff() {
        Order cutoffOrder = Order.builder()
                .id(203L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(7))
                .build();

        validCreateRequest.setOrderId(203L);
        when(orderRepository.findById(203L)).thenReturn(Optional.of(cutoffOrder));

        // Since time flows during execution, LocalDateTime.now() inside method is strictly after cutoff = deliveredAt + 7 days
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
    }

    @Test
    void testStress_Boundary_JustWithin7Days_10SecondsWindow_Success() {
        Order justInTimeOrder = Order.builder()
                .id(204L)
                .user(customerA)
                .totalAmount(new BigDecimal("1999.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(7).plusSeconds(10))
                .build();

        validCreateRequest.setOrderId(204L);
        when(orderRepository.findById(204L)).thenReturn(Optional.of(justInTimeOrder));
        when(returnRequestRepository.findByOrderId(204L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(102L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void testStress_NullDeliveryTimestamps_AllNull_FallbackToNow_Eligible() {
        Order allNullTimestampsOrder = Order.builder()
                .id(205L)
                .user(customerA)
                .totalAmount(new BigDecimal("1500.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(null)
                .updatedAt(null)
                .createdAt(null)
                .build();

        validCreateRequest.setOrderId(205L);
        when(orderRepository.findById(205L)).thenReturn(Optional.of(allNullTimestampsOrder));
        when(returnRequestRepository.findByOrderId(205L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(103L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void testStress_NullDeliveredAt_ExpiredUpdatedAt_ThrowsBadRequestException() {
        Order expiredUpdatedAtOrder = Order.builder()
                .id(206L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(null)
                .updatedAt(LocalDateTime.now().minusDays(9))
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        validCreateRequest.setOrderId(206L);
        when(orderRepository.findById(206L)).thenReturn(Optional.of(expiredUpdatedAtOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
    }

    @Test
    void testStress_NullDeliveredAt_NullUpdatedAt_ExpiredCreatedAt_ThrowsBadRequestException() {
        Order expiredCreatedAtOrder = Order.builder()
                .id(207L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(null)
                .updatedAt(null)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        validCreateRequest.setOrderId(207L);
        when(orderRepository.findById(207L)).thenReturn(Optional.of(expiredCreatedAtOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
    }

    @Test
    void testStress_OrderStatus_Cancelled_ThrowsBadRequestException() {
        Order cancelledOrder = Order.builder()
                .id(208L)
                .user(customerA)
                .status(OrderStatus.CANCELLED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        validCreateRequest.setOrderId(208L);
        when(orderRepository.findById(208L)).thenReturn(Optional.of(cancelledOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        assertTrue(ex.getMessage().contains("CANCELLED"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_OrderStatus_Confirmed_ThrowsBadRequestException() {
        Order confirmedOrder = Order.builder()
                .id(209L)
                .user(customerA)
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        validCreateRequest.setOrderId(209L);
        when(orderRepository.findById(209L)).thenReturn(Optional.of(confirmedOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        assertTrue(ex.getMessage().contains("CONFIRMED"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_CrossCustomer_CustomerA_CannotViewCustomerB_ReturnClaimById() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(70L)
                .order(Order.builder().id(300L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(70L)).thenReturn(Optional.of(claimOfCustomerB));
        when(userRepository.findById(customerA.getId())).thenReturn(Optional.of(customerA));

        assertThrows(AccessDeniedException.class, () ->
                returnService.getReturnRequestById(70L, customerA.getId()));
    }

    @Test
    void testStress_CrossCustomer_CustomerA_CannotViewCustomerB_ReturnClaimById_EntityMethod() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(71L)
                .order(Order.builder().id(301L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(71L)).thenReturn(Optional.of(claimOfCustomerB));

        assertThrows(AccessDeniedException.class, () ->
                returnService.getReturnRequestById(71L, customerA));
    }

    @Test
    void testStress_CrossCustomer_CustomerA_CannotViewCustomerB_ReturnClaimByOrderId() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(72L)
                .order(Order.builder().id(302L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByOrderId(302L)).thenReturn(Optional.of(claimOfCustomerB));
        when(userRepository.findById(customerA.getId())).thenReturn(Optional.of(customerA));

        assertThrows(AccessDeniedException.class, () ->
                returnService.getReturnRequestByOrderId(302L, customerA.getId()));
    }

    @Test
    void testStress_CrossCustomer_CustomerA_CannotViewCustomerB_ReturnClaimByOrderId_EntityMethod() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(73L)
                .order(Order.builder().id(303L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByOrderId(303L)).thenReturn(Optional.of(claimOfCustomerB));

        assertThrows(AccessDeniedException.class, () ->
                returnService.getReturnRequestByOrderId(303L, customerA));
    }

    @Test
    void testStress_CrossCustomer_CustomerA_CannotModifyCustomerB_ReturnStatus() {
        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .adminNotes("Customer A attempting fraudulent approval")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                returnService.updateReturnStatus(70L, updateReq, customerA));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_CrossCustomer_CustomerB_CannotModifyCustomerA_ReturnStatus() {
        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("REJECTED")
                .adminNotes("Customer B attempting malicious rejection")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                returnService.updateReturnStatus(50L, updateReq, customerB));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_StaffAccess_AdminCanViewCustomerReturnById() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(74L)
                .order(Order.builder().id(304L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(74L)).thenReturn(Optional.of(claimOfCustomerB));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        ReturnResponse response = returnService.getReturnRequestById(74L, adminUser.getId());
        assertNotNull(response);
        assertEquals(74L, response.getId());
    }

    @Test
    void testStress_StaffAccess_ManagerCanViewCustomerReturnById_EntityMethod() {
        ReturnRequest claimOfCustomerB = ReturnRequest.builder()
                .id(75L)
                .order(Order.builder().id(305L).user(customerB).status(OrderStatus.DELIVERED).build())
                .user(customerB)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(75L)).thenReturn(Optional.of(claimOfCustomerB));

        ReturnResponse response = returnService.getReturnRequestById(75L, managerUser);
        assertNotNull(response);
        assertEquals(75L, response.getId());
    }

    @Test
    void testStress_InvalidStateTransition_PendingDirectToPickupScheduled_ThrowsBadRequest() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(80L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest invalidReq = ReturnStatusUpdateRequest.builder()
                .status("PICKUP_SCHEDULED")
                .reverseCourier("Blue Dart")
                .reverseTrackingNumber("BDR-111")
                .build();

        when(returnRequestRepository.findById(80L)).thenReturn(Optional.of(pendingClaim));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(80L, invalidReq, adminUser));

        assertTrue(ex.getMessage().contains("Invalid state transition from PENDING to PICKUP_SCHEDULED"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_InvalidStateTransition_PendingDirectToCompleted_ThrowsBadRequest() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(81L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest invalidReq = ReturnStatusUpdateRequest.builder()
                .status("COMPLETED")
                .build();

        when(returnRequestRepository.findById(81L)).thenReturn(Optional.of(pendingClaim));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(81L, invalidReq, adminUser));

        assertTrue(ex.getMessage().contains("Invalid state transition from PENDING to COMPLETED"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStress_InvalidStateTransition_RejectedCannotBeAltered() {
        ReturnRequest rejectedClaim = ReturnRequest.builder()
                .id(82L)
                .order(deliveredOrder)
                .user(customerA)
                .status("REJECTED")
                .adminNotes("Original defect tag missing")
                .build();

        ReturnStatusUpdateRequest attemptUpdate = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .build();

        when(returnRequestRepository.findById(82L)).thenReturn(Optional.of(rejectedClaim));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(82L, attemptUpdate, adminUser));

        assertTrue(ex.getMessage().contains("Cannot alter status of an already rejected return request"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }
}
