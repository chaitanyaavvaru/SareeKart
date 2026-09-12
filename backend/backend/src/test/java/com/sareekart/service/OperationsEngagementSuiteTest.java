package com.sareekart.service;

import com.sareekart.controller.ReviewController;
import com.sareekart.controller.StockTransferController;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.*;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.NotificationRepository;
import com.sareekart.repository.OrderItemRepository;
import com.sareekart.repository.ReviewRepository;
import com.sareekart.repository.StockTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperationsEngagementSuiteTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private NotificationEventService notificationEventService;

    private StockTransferController stockTransferController;
    private ReviewController reviewController;

    private User customer;
    private User manager;
    private User owner;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(10L).firstName("Ananya").lastName("Rao").email("customer@sareekart.com").role(Role.CUSTOMER).build();
        manager = User.builder().id(20L).firstName("Rajesh").email("manager@sareekart.com").role(Role.MANAGER).build();
        owner = User.builder().id(1L).firstName("Suresh").email("owner@sareekart.com").role(Role.OWNER).build();

        stockTransferController = new StockTransferController(stockTransferRepository, inventoryItemRepository, notificationEventService);
        reviewController = new ReviewController(reviewRepository, orderItemRepository, notificationEventService);
    }

    /* -------------------------------------------------------------------------
     * R1: Event-Driven Notifications Tests
     * ------------------------------------------------------------------------- */
    @Test
    void testNotifyOrderPlacedCreatesCustomerAndStaffAlerts() {
        Order order = Order.builder()
                .id(501L)
                .user(customer)
                .totalAmount(new BigDecimal("15500"))
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification customerNotif = notificationEventService.notifyOrderPlaced(order);

        assertNotNull(customerNotif);
        assertEquals(customer.getId(), customerNotif.getUserId());
        assertEquals("ORDER_PLACED", customerNotif.getType());
        assertTrue(customerNotif.getMessage().contains("501"));
        assertFalse(customerNotif.getIsRead());

        // Saves both customer and staff notifications
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void testNotifyLowStockCreatesManagerAlert() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification notif = notificationEventService.notifyLowStock("SK-KANCHI-01", "Imperial Gold Kanchipuram", 2);

        assertNotNull(notif);
        assertEquals("MANAGER", notif.getTargetRole());
        assertEquals("LOW_STOCK", notif.getType());
        assertTrue(notif.getMessage().contains("2 units"));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    /* -------------------------------------------------------------------------
     * R2: Multi-Warehouse Stock Transfer & Maker-Checker Tests
     * ------------------------------------------------------------------------- */
    @Test
    void testManagerSubmitStockTransferPendingApproval() {
        StockTransferController.TransferRequestDto dto = new StockTransferController.TransferRequestDto();
        dto.setSku("SK-BANARASI-02");
        dto.setSourceWarehouse("WH-01 Bengaluru Central");
        dto.setTargetWarehouse("WH-02 Mumbai West");
        dto.setQuantity(10);
        dto.setReason("High festival demand in Mumbai");

        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(i -> {
            StockTransfer t = i.getArgument(0);
            t.setId(77L);
            return t;
        });

        ResponseEntity<ApiResponse<StockTransfer>> res = stockTransferController.requestTransfer(dto, manager);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        StockTransfer created = res.getBody().getData();
        assertEquals(StockTransfer.STATUS_PENDING, created.getStatus());
        assertEquals("WH-01 Bengaluru Central", created.getSourceWarehouse());
        assertEquals("WH-02 Mumbai West", created.getTargetWarehouse());
        assertEquals(10, created.getQuantity());
        verify(stockTransferRepository, times(1)).save(any(StockTransfer.class));
    }

    @Test
    void testOwnerCanApproveStockTransfer() {
        StockTransfer transfer = StockTransfer.builder()
                .id(77L)
                .sku("SK-BANARASI-02")
                .sourceWarehouse("WH-01 Bengaluru Central")
                .targetWarehouse("WH-02 Mumbai West")
                .quantity(10)
                .status(StockTransfer.STATUS_PENDING)
                .requestedByUserId(manager.getId())
                .requestedByEmail(manager.getEmail())
                .build();

        when(stockTransferRepository.findById(77L)).thenReturn(Optional.of(transfer));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));

        StockTransferController.ActionDto action = new StockTransferController.ActionDto();
        action.setNote("Approved for Mumbai showroom festive display");

        ResponseEntity<ApiResponse<StockTransfer>> res = stockTransferController.approveTransfer(77L, action, owner);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        StockTransfer approved = res.getBody().getData();
        assertEquals(StockTransfer.STATUS_APPROVED, approved.getStatus());
        assertEquals(owner.getId(), approved.getApprovedByUserId());
        assertEquals("Approved for Mumbai showroom festive display", approved.getReviewNote());
    }

    @Test
    void testManagerCannotApproveStockTransferForbidden() {
        StockTransferController.ActionDto action = new StockTransferController.ActionDto();
        action.setNote("Attempting unauthorized approval");

        ResponseEntity<ApiResponse<StockTransfer>> res = stockTransferController.approveTransfer(77L, action, manager);

        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertEquals("Not authorised to perform this action", res.getBody().getMessage());
        verify(stockTransferRepository, never()).save(any(StockTransfer.class));
    }

    /* -------------------------------------------------------------------------
     * R3: Verified Customer Reviews & Moderation Tests
     * ------------------------------------------------------------------------- */
    @Test
    void testCustomerReviewVerifiedBuyerBadgeWhenPurchased() {
        when(orderItemRepository.hasUserPurchasedProduct(customer.getId(), 101L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(99L);
            return r;
        });

        Review reviewInput = Review.builder()
                .rating(5)
                .comment("Incredible weave! Gold zari looks pure and majestic.")
                .build();

        ResponseEntity<ApiResponse<Review>> res = reviewController.addReview(101L, customer, reviewInput);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        Review saved = res.getBody().getData();
        assertNotNull(saved);
        assertTrue(saved.getVerifiedBuyer(), "Customer who bought product should receive Verified Buyer badge");
        assertEquals("APPROVED", saved.getStatus());
        assertEquals("Ananya Rao", saved.getUserName());
    }

    @Test
    void testCustomerReviewNonBuyerBadgeWhenNotPurchased() {
        when(orderItemRepository.hasUserPurchasedProduct(customer.getId(), 102L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(100L);
            return r;
        });

        Review reviewInput = Review.builder()
                .rating(4)
                .comment("Looks stunning on the website!")
                .build();

        ResponseEntity<ApiResponse<Review>> res = reviewController.addReview(102L, customer, reviewInput);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        Review saved = res.getBody().getData();
        assertNotNull(saved);
        assertFalse(saved.getVerifiedBuyer(), "Visitor who hasn't bought should not have verified buyer badge");
    }

    @Test
    void testAdminModerationUpdateStatus() {
        Review existing = Review.builder()
                .id(99L)
                .productId(101L)
                .userName("Ananya Rao")
                .rating(5)
                .comment("Incredible weave!")
                .status("APPROVED")
                .build();

        when(reviewRepository.findById(99L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        ReviewController.ReviewStatusDto dto = new ReviewController.ReviewStatusDto();
        dto.setStatus("FEATURED");

        ResponseEntity<ApiResponse<Review>> res = reviewController.updateReviewStatus(99L, dto);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("FEATURED", res.getBody().getData().getStatus());
    }
}
