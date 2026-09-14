package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.response.customer.*;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.entity.CustomerEventType;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.CustomerBehaviorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerBehaviorServiceTest {

    @Mock
    private CustomerEventRepository customerEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CustomerBehaviorServiceImpl customerBehaviorService;

    private User testUser;
    private final String testSessionId = "sess_test_uuid_123456";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(7L)
                .email("chaitanya@sareekart.com")
                .firstName("Chaitanya")
                .build();
    }

    @Test
    @DisplayName("1. Record PRODUCT_VIEW Event with Guest Session")
    void recordEvent_productView_guestSession() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .entityType("PRODUCT")
                .entityId(101L)
                .metadata(Map.of("name", "Kanchipuram Silk Saree", "price", 18500, "dwellTimeMs", 4200))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(1L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        CustomerEventResponse res = customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(res);
        assertEquals(CustomerEventType.PRODUCT_VIEW.name(), res.getEventType());
        assertEquals("PRODUCT", res.getEntityType());
        assertEquals(101L, res.getEntityId());
        assertNull(res.getUserId());
        assertEquals(testSessionId, res.getSessionId());
        assertNotNull(res.getClientEventId());
        assertEquals("Kanchipuram Silk Saree", res.getMetadata().get("name"));
    }

    @Test
    @DisplayName("2. Record SEARCH_QUERY Event")
    void recordEvent_searchQuery() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.SEARCH_QUERY.name())
                .entityType("SEARCH")
                .metadata(Map.of("query", "Banarasi Brocade", "resultCount", 12))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(2L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        CustomerEventResponse res = customerBehaviorService.recordEvent(req, 7L, "127.0.0.1", "Chrome");

        assertNotNull(res);
        assertEquals(CustomerEventType.SEARCH_QUERY.name(), res.getEventType());
        assertEquals("Banarasi Brocade", res.getMetadata().get("query"));
    }

    @Test
    @DisplayName("3. Record CATEGORY_VIEW Event")
    void recordEvent_categoryView() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.CATEGORY_VIEW.name())
                .entityType("CATEGORY")
                .metadata(Map.of("categorySlug", "kanchipuram-silk"))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(3L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        CustomerEventResponse res = customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Chrome");
        assertNotNull(res);
        assertEquals(CustomerEventType.CATEGORY_VIEW.name(), res.getEventType());
    }

    @Test
    @DisplayName("4. Record ADD_TO_CART and REMOVE_FROM_CART Events")
    void recordEvent_cartActions() {
        CustomerEventRequest addReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .entityType("PRODUCT")
                .entityId(101L)
                .metadata(Map.of("quantity", 1, "price", 15000))
                .build();

        CustomerEventRequest remReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.REMOVE_FROM_CART.name())
                .entityType("PRODUCT")
                .entityId(101L)
                .metadata(Map.of("quantity", 1))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(4L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        CustomerEventResponse addRes = customerBehaviorService.recordEvent(addReq, 7L, "127.0.0.1", "Safari");
        CustomerEventResponse remRes = customerBehaviorService.recordEvent(remReq, 7L, "127.0.0.1", "Safari");

        assertEquals(CustomerEventType.ADD_TO_CART.name(), addRes.getEventType());
        assertEquals(CustomerEventType.REMOVE_FROM_CART.name(), remRes.getEventType());
    }

    @Test
    @DisplayName("5. Record ADD_TO_WISHLIST and REMOVE_FROM_WISHLIST Events")
    void recordEvent_wishlistActions() {
        CustomerEventRequest addReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.ADD_TO_WISHLIST.name())
                .entityType("PRODUCT")
                .entityId(102L)
                .build();

        CustomerEventRequest remReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.REMOVE_FROM_WISHLIST.name())
                .entityType("PRODUCT")
                .entityId(102L)
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(5L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        assertEquals(CustomerEventType.ADD_TO_WISHLIST.name(), customerBehaviorService.recordEvent(addReq, 7L, "127.0.0.1", "Firefox").getEventType());
        assertEquals(CustomerEventType.REMOVE_FROM_WISHLIST.name(), customerBehaviorService.recordEvent(remReq, 7L, "127.0.0.1", "Firefox").getEventType());
    }

    @Test
    @DisplayName("6. Record CHECKOUT_INITIATED and ORDER_COMPLETED Events")
    void recordEvent_checkoutAndOrder() {
        CustomerEventRequest chkReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.CHECKOUT_INITIATED.name())
                .metadata(Map.of("itemCount", 2, "cartValue", 32000))
                .build();

        CustomerEventRequest ordReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.ORDER_COMPLETED.name())
                .entityType("ORDER")
                .entityId(55L)
                .metadata(Map.of("orderId", 55, "totalAmount", 32000, "paymentMethod", "COD"))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(6L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        assertEquals(CustomerEventType.CHECKOUT_INITIATED.name(), customerBehaviorService.recordEvent(chkReq, 7L, "127.0.0.1", "Edge").getEventType());
        assertEquals(CustomerEventType.ORDER_COMPLETED.name(), customerBehaviorService.recordEvent(ordReq, 7L, "127.0.0.1", "Edge").getEventType());
    }

    @Test
    @DisplayName("7. Record AI_STYLIST_ENGAGE and VISUAL_SEARCH_ENGAGE Events")
    void recordEvent_aiEngagements() {
        CustomerEventRequest styleReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.AI_STYLIST_ENGAGE.name())
                .metadata(Map.of("occasion", "Bridal", "colorPreference", "Ruby Red"))
                .build();

        CustomerEventRequest visReq = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.VISUAL_SEARCH_ENGAGE.name())
                .metadata(Map.of("extractedWeave", "Kanchipuram", "confidence", 0.94))
                .build();

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(7L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        assertEquals(CustomerEventType.AI_STYLIST_ENGAGE.name(), customerBehaviorService.recordEvent(styleReq, 7L, "127.0.0.1", "Mobile").getEventType());
        assertEquals(CustomerEventType.VISUAL_SEARCH_ENGAGE.name(), customerBehaviorService.recordEvent(visReq, 7L, "127.0.0.1", "Mobile").getEventType());
    }

    @Test
    @DisplayName("8. Privacy: Sensitive Metadata Keys (passwords, tokens, CVVs) Are Redacted")
    void recordEvent_redactsSensitiveMetadata() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType(CustomerEventType.CHECKOUT_INITIATED.name())
                .metadata(Map.of(
                        "cartValue", 12000,
                        "password", "Secret123!",
                        "creditCard", "4111111111111111",
                        "cvv", "123",
                        "token", "bearer_jwt_xyz"
                ))
                .build();

        ArgumentCaptor<CustomerEvent> captor = ArgumentCaptor.forClass(CustomerEvent.class);
        when(customerEventRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CustomerEventResponse res = customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Agent");

        assertNotNull(res);
        CustomerEvent captured = captor.getValue();
        assertTrue(captured.getMetadata().contains("[REDACTED]"));
        assertFalse(captured.getMetadata().contains("Secret123!"));
        assertFalse(captured.getMetadata().contains("4111111111111111"));
        assertFalse(captured.getMetadata().contains("bearer_jwt_xyz"));
    }

    @Test
    @DisplayName("9. Deduplication: Duplicate ClientEventId Returns Existing Record Idempotently")
    void recordEvent_duplicateDeliveryDeduplication() {
        String clientEventId = "evt_dedup_unique_999";
        CustomerEvent existingEvent = CustomerEvent.builder()
                .id(999L)
                .clientEventId(clientEventId)
                .sessionId(testSessionId)
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .createdAt(LocalDateTime.now())
                .build();

        when(customerEventRepository.findByClientEventId(clientEventId)).thenReturn(Optional.of(existingEvent));

        CustomerEventRequest req = CustomerEventRequest.builder()
                .clientEventId(clientEventId)
                .sessionId(testSessionId)
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .build();

        CustomerEventResponse res = customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Browser");

        assertEquals(999L, res.getId());
        verify(customerEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Validation: Unknown Event Type Is Rejected")
    void recordEvent_rejectsUnknownEventType() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId(testSessionId)
                .eventType("UNSUPPORTED_MALICIOUS_EVENT")
                .build();

        assertThrows(BadRequestException.class, () ->
                customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Browser"));
    }

    @Test
    @DisplayName("11. Validation: Missing Session ID Is Rejected")
    void recordEvent_rejectsMissingSessionId() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId("   ")
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .build();

        assertThrows(BadRequestException.class, () ->
                customerBehaviorService.recordEvent(req, null, "127.0.0.1", "Browser"));
    }

    @Test
    @DisplayName("12. Batch Ingestion: Records Multiple Events Atomically")
    void recordBatch_success() {
        List<CustomerEventRequest> batch = List.of(
                CustomerEventRequest.builder().sessionId(testSessionId).eventType(CustomerEventType.PRODUCT_VIEW.name()).build(),
                CustomerEventRequest.builder().sessionId(testSessionId).eventType(CustomerEventType.ADD_TO_CART.name()).build(),
                CustomerEventRequest.builder().sessionId(testSessionId).eventType(CustomerEventType.CHECKOUT_INITIATED.name()).build()
        );

        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent ce = inv.getArgument(0);
            ce.setId(10L);
            ce.setCreatedAt(LocalDateTime.now());
            return ce;
        });

        List<CustomerEventResponse> responses = customerBehaviorService.recordBatch(batch, 7L, "127.0.0.1", "Browser");

        assertEquals(3, responses.size());
        verify(customerEventRepository, times(3)).save(any(CustomerEvent.class));
    }

    @Test
    @DisplayName("13. Batch Size Limit: Rejects Batches Exceeding 50 Events")
    void recordBatch_rejectsExcessiveSize() {
        List<CustomerEventRequest> oversized = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            oversized.add(CustomerEventRequest.builder().sessionId(testSessionId).eventType(CustomerEventType.PRODUCT_VIEW.name()).build());
        }

        assertThrows(BadRequestException.class, () ->
                customerBehaviorService.recordBatch(oversized, 7L, "127.0.0.1", "Browser"));
    }

    @Test
    @DisplayName("14. Identity Resolution: Associates Historical Guest Events with Authenticated User")
    void identifySession_success() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(testUser));
        when(customerEventRepository.findDistinctUserIdsBySessionId(testSessionId)).thenReturn(Collections.emptyList());
        when(customerEventRepository.linkSessionToUser(testSessionId, testUser)).thenReturn(8);

        int linked = customerBehaviorService.identifySession(testSessionId, 7L);

        assertEquals(8, linked);
        verify(customerEventRepository).linkSessionToUser(testSessionId, testUser);
    }

    @Test
    @DisplayName("15. Session Ownership Protection: Rejects Reassignment If Claimed by Another User")
    void identifySession_sessionOwnershipProtection() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(testUser));
        // Session was already linked to user 99L
        when(customerEventRepository.findDistinctUserIdsBySessionId(testSessionId)).thenReturn(List.of(99L));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                customerBehaviorService.identifySession(testSessionId, 7L));

        assertTrue(ex.getMessage().contains("already been claimed by another user"));
        verify(customerEventRepository, never()).linkSessionToUser(anyString(), any());
    }

    @Test
    @DisplayName("16. Deterministic Customer Affinity Engine Scoring")
    void getCustomerAffinityProfile_deterministicScoring() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(testUser));

        CustomerEvent view1 = CustomerEvent.builder()
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .metadata("{\"fabric\":\"Kanchipuram Silk\",\"color\":\"Crimson Red\",\"price\":18000}")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        CustomerEvent view2 = CustomerEvent.builder()
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .metadata("{\"fabric\":\"Kanchipuram Silk\",\"color\":\"Royal Gold\",\"price\":22000}")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        CustomerEvent cart1 = CustomerEvent.builder()
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .metadata("{\"fabric\":\"Banarasi Brocade\",\"color\":\"Crimson Red\",\"price\":19500}")
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();
        CustomerEvent order1 = CustomerEvent.builder()
                .eventType(CustomerEventType.ORDER_COMPLETED.name())
                .metadata("{\"fabric\":\"Kanchipuram Silk\",\"color\":\"Crimson Red\",\"totalAmount\":18000}")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(customerEventRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(order1, cart1, view2, view1));

        CustomerAffinityResponse profile = customerBehaviorService.getCustomerAffinityProfile(7L);

        assertNotNull(profile);
        assertEquals(7L, profile.getUserId());
        assertEquals("Kanchipuram Silk", profile.getPreferredFabric());
        assertEquals("Crimson Red", profile.getPreferredColor());
        assertEquals("LUXURY", profile.getPriceSensitivity(), "Average price > 15,000 must map to LUXURY tier");
        assertTrue(profile.getPurchaseIntentScore() >= 50, "Recent views, cart add, and order must yield high intent score");
        assertEquals(2, profile.getTotalViews());
        assertEquals(1, profile.getTotalCartAdds());
        assertEquals(1, profile.getTotalPurchases());
        assertNotNull(profile.getScoreBreakdown());
    }

    @Test
    @DisplayName("17. Accurate 4-Stage Event-Driven Conversion Funnel")
    void getBehavioralOverview_eventDrivenFunnel() {
        when(customerEventRepository.countByCreatedAtBetween(any(), any())).thenReturn(100L);
        when(customerEventRepository.countDistinctSessionsBetween(any(), any())).thenReturn(40L);
        lenient().when(customerEventRepository.countByEventTypeAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        lenient().when(customerEventRepository.countDistinctSessionsByEventTypeBetween(anyString(), any(), any())).thenReturn(0L);

        // Stage 1: Views
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(50L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(30L);

        // Stage 2: Carts
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(20L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(15L);

        // Stage 3: Checkout Initiated
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(10L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(8L);

        // Stage 4: Order Completed
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(5L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(4L);

        when(customerEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(customerEventRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        BehavioralOverviewResponse overview = customerBehaviorService.getBehavioralOverview("30D", null, null);

        assertNotNull(overview);
        assertEquals(4, overview.getFunnel().size());

        BehavioralFunnelStageDto viewStage = overview.getFunnel().get(0);
        assertEquals("PRODUCT_VIEW", viewStage.getStage());
        assertEquals(30L, viewStage.getUniqueSessions());
        assertEquals(100.0, viewStage.getOverallConversionRate());

        BehavioralFunnelStageDto cartStage = overview.getFunnel().get(1);
        assertEquals("ADD_TO_CART", cartStage.getStage());
        assertEquals(15L, cartStage.getUniqueSessions());
        assertEquals(50.0, cartStage.getConversionRateFromPrevious(), "15 / 30 = 50%");

        BehavioralFunnelStageDto orderStage = overview.getFunnel().get(3);
        assertEquals("ORDER_COMPLETED", orderStage.getStage());
        assertEquals(4L, orderStage.getUniqueSessions());
        assertEquals(13.3, orderStage.getOverallConversionRate(), "4 / 30 = 13.3%");
    }
}
