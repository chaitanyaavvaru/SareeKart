package com.sareekart.service;

import com.sareekart.entity.*;
import com.sareekart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private ApprovalService approvalService;

    private User manager;
    private User owner;

    @BeforeEach
    void setUp() {
        manager = User.builder().id(101L).email("manager@sareekart.com").role(Role.MANAGER).build();
        owner = User.builder().id(100L).email("owner@sareekart.com").role(Role.OWNER).build();
    }

    @Test
    void testSubmitRequestPendingState() {
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(i -> {
            ApprovalRequest r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        ApprovalRequest req = approvalService.submitRequest(
                "PRODUCT_PRICE", "11", "PRICE_CHANGE", "10000", "12500", "Diwali festive update", manager
        );

        assertNotNull(req);
        assertEquals(ApprovalRequest.STATUS_PENDING, req.getStatus());
        assertEquals("manager@sareekart.com", req.getRequestedByEmail());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testManagerCannotApproveRequest() {
        assertThrows(SecurityException.class, () -> {
            approvalService.approveRequest(1L, manager, "Attempted self-approval");
        });
    }

    @Test
    void testOwnerApproveProductPrice() {
        ApprovalRequest pending = ApprovalRequest.builder()
                .id(1L)
                .entityType("PRODUCT_PRICE")
                .targetEntityId("11")
                .requestedValue("14000")
                .status(ApprovalRequest.STATUS_PENDING)
                .build();

        Product product = Product.builder().id(11L).name("Kanchipuram Silk").price(new BigDecimal("12000")).build();

        when(approvalRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(pending);

        ApprovalRequest result = approvalService.approveRequest(1L, owner, "Looks good");

        assertEquals(ApprovalRequest.STATUS_APPROVED, result.getStatus());
        assertEquals(new BigDecimal("14000"), product.getPrice());
        verify(productRepository, times(1)).save(product);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testPreventDoubleApproval() {
        ApprovalRequest alreadyApproved = ApprovalRequest.builder()
                .id(2L)
                .entityType("PRODUCT_PRICE")
                .status(ApprovalRequest.STATUS_APPROVED)
                .build();

        when(approvalRequestRepository.findById(2L)).thenReturn(Optional.of(alreadyApproved));

        assertThrows(IllegalStateException.class, () -> {
            approvalService.approveRequest(2L, owner, "Trying again");
        });
    }
}
