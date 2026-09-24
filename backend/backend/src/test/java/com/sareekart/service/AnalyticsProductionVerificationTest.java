package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.controller.AdminCustomerBehaviorController;
import com.sareekart.controller.CustomerEventController;
import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.customer.BehavioralFunnelStageDto;
import com.sareekart.dto.response.customer.BehavioralOverviewResponse;
import com.sareekart.dto.response.customer.ConversionFunnelResponse;
import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.entity.CustomerEventType;
import com.sareekart.entity.User;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.CustomerBehaviorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 14 — Stage 5: Production Analytics & Conversion Tracking Verification Test Suite
 *
 * Verifies all 10 core analytics scenarios:
 * 1. All 8 core eCommerce funnel event types are recognized and valid.
 * 2. All auxiliary engagement channel event types are recognized and valid.
 * 3. Event payload privacy redaction (passwords, tokens, credit cards, CVVs redacted to [REDACTED]).
 * 4. Full 8-stage eCommerce conversion funnel computation with chronological ordering and conversion rates.
 * 5. Empty telemetry safe zero-bounded conversion rates without exceptions.
 * 6. Auxiliary channel engagement counts aggregation (wishlist, recommendations, AI, WhatsApp, Trousseau).
 * 7. Legacy 4-stage funnel backward compatibility in overview.getFunnel() alongside 8-stage eCommerce funnel.
 * 8. Ingestion failure isolation: database timeout or backend exceptions return 200 OK without failing UI.
 * 9. Server context enforces authoritative authenticated user ID over client claims.
 * 10. Admin customer behavior controller returns 200 OK with ConversionFunnelResponse for authorized staff.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnalyticsProductionVerificationTest {

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
    private static final String TEST_SESSION = "sess_stage5_verification_001";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(205L)
                .email("analyst.lead@sareekart.com")
                .firstName("Lead")
                .lastName("Analyst")
                .build();
    }

    // =========================================================================
    // SCENARIO 1: All 8 Core Funnel Event Types Recognized
    // =========================================================================
    @Test
    @DisplayName("Scenario 1: All 8 core eCommerce funnel event types are recognized and valid")
    void test01_CoreFunnelEventTypesAreValid() {
        String[] coreFunnelTypes = {
                "LANDING_PAGE_VIEW",
                "PRODUCT_VIEW",
                "SEARCH_QUERY",
                "CATEGORY_VIEW",
                "ADD_TO_CART",
                "CHECKOUT_INITIATED",
                "PAYMENT_ATTEMPT",
                "ORDER_COMPLETED"
        };

        for (String eventType : coreFunnelTypes) {
            assertTrue(CustomerEventType.isValid(eventType), "Event type " + eventType + " must be recognized");
            assertDoesNotThrow(() -> CustomerEventType.fromString(eventType));
        }
    }

    // =========================================================================
    // SCENARIO 2: All Auxiliary Channel Event Types Recognized
    // =========================================================================
    @Test
    @DisplayName("Scenario 2: All auxiliary engagement channel event types are recognized and valid")
    void test02_AuxiliaryChannelEventTypesAreValid() {
        String[] auxTypes = {
                "ADD_TO_WISHLIST",
                "REMOVE_FROM_WISHLIST",
                "RECOMMENDATION_CLICK",
                "AI_STYLIST_ENGAGE",
                "VISUAL_SEARCH_ENGAGE",
                "WHATSAPP_COMMERCE_ENGAGE",
                "TROUSSEAU_ENGAGE",
                "SHARE_LINK_ENGAGE"
        };

        for (String eventType : auxTypes) {
            assertTrue(CustomerEventType.isValid(eventType), "Auxiliary event type " + eventType + " must be valid");
            assertDoesNotThrow(() -> CustomerEventType.fromString(eventType));
        }
    }

    // =========================================================================
    // SCENARIO 3: Event Payload Privacy Redaction
    // =========================================================================
    @Test
    @DisplayName("Scenario 3: Sensitive fields in event metadata are strictly redacted to [REDACTED]")
    void test03_PrivacyRedactionInEventIngestion() {
        CustomerEventRequest request = CustomerEventRequest.builder()
                .clientEventId("evt_stage5_priv_01")
                .sessionId(TEST_SESSION)
                .eventType("PAYMENT_ATTEMPT")
                .entityType("PAYMENT")
                .metadata(Map.of(
                        "paymentMethod", "RAZORPAY",
                        "password", "CustomerPassword@123",
                        "token", "jwt_bearer_token_xyz",
                        "creditcard", "4111222233334444",
                        "cvv", "999",
                        "amount", 45000
                ))
                .build();

        when(customerEventRepository.existsByClientEventId("evt_stage5_priv_01")).thenReturn(false);
        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(inv -> {
            CustomerEvent saved = inv.getArgument(0);
            saved.setId(1001L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CustomerEventResponse response = customerBehaviorService.recordEvent(request, null, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertNotNull(response.getMetadata());
        assertEquals("[REDACTED]", response.getMetadata().get("password"), "Password must be redacted");
        assertEquals("[REDACTED]", response.getMetadata().get("token"), "Token must be redacted");
        assertEquals("[REDACTED]", response.getMetadata().get("creditcard"), "Card number must be redacted");
        assertEquals("[REDACTED]", response.getMetadata().get("cvv"), "CVV must be redacted");
        assertEquals("RAZORPAY", response.getMetadata().get("paymentMethod"));
        assertEquals(45000, response.getMetadata().get("amount"));
    }

    // =========================================================================
    // SCENARIO 4: Full 8-Stage Funnel Calculation
    // =========================================================================
    @Test
    @DisplayName("Scenario 4: Full 8-stage conversion funnel computes stages and conversion rates accurately")
    void test04_FullConversionFunnelCalculation() {
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("LANDING_PAGE_VIEW"), any(), any())).thenReturn(1000L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("LANDING_PAGE_VIEW"), any(), any())).thenReturn(500L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(800L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(400L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("SEARCH_QUERY"), any(), any())).thenReturn(300L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("SEARCH_QUERY"), any(), any())).thenReturn(200L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CATEGORY_VIEW"), any(), any())).thenReturn(500L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CATEGORY_VIEW"), any(), any())).thenReturn(300L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(250L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(150L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(120L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(90L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PAYMENT_ATTEMPT"), any(), any())).thenReturn(80L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PAYMENT_ATTEMPT"), any(), any())).thenReturn(72L);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(60L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(50L);

        ConversionFunnelResponse response = customerBehaviorService.getConversionFunnel("30D", null, null);

        assertNotNull(response);
        assertEquals(8, response.getStages().size(), "Funnel must contain exactly 8 stages");
        assertEquals("LANDING_PAGE_VIEW", response.getStages().get(0).getStage());
        assertEquals("ORDER_COMPLETED", response.getStages().get(7).getStage());

        assertEquals(10.0, response.getOverallConversionRate(), "Overall conversion (50 / 500) must be 10.0%");
        assertEquals(37.5, response.getDetailToCartRate(), "Detail to cart (150 / 400) must be 37.5%");
        assertEquals(60.0, response.getCartToCheckoutRate(), "Cart to checkout (90 / 150) must be 60.0%");
        assertEquals(80.0, response.getCheckoutToPaymentRate(), "Checkout to payment (72 / 90) must be 80.0%");
        assertEquals(69.4, response.getPaymentToOrderRate(), "Payment to order (50 / 72) must be 69.4%");
        assertEquals(66.7, response.getCartAbandonmentRate(), "Cart abandonment must be 66.7%");
        assertEquals(44.4, response.getCheckoutAbandonmentRate(), "Checkout abandonment must be 44.4%");
    }

    // =========================================================================
    // SCENARIO 5: Empty Telemetry Returns Zero-Bounded Rates Safely
    // =========================================================================
    @Test
    @DisplayName("Scenario 5: Empty telemetry returns safe zero-bounded conversion rates without exceptions")
    void test05_EmptyTelemetryZeroBoundedRates() {
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(anyString(), any(), any())).thenReturn(0L);

        ConversionFunnelResponse response = customerBehaviorService.getConversionFunnel("TODAY", null, null);

        assertNotNull(response);
        assertEquals(8, response.getStages().size());
        assertEquals(0.0, response.getOverallConversionRate());
        assertEquals(0.0, response.getDetailToCartRate());
        assertEquals(0.0, response.getCartToCheckoutRate());
        assertEquals(0.0, response.getCheckoutToPaymentRate());
        assertEquals(0.0, response.getPaymentToOrderRate());
        assertEquals(0.0, response.getCartAbandonmentRate());
        assertEquals(0.0, response.getCheckoutAbandonmentRate());

        for (BehavioralFunnelStageDto stage : response.getStages()) {
            assertEquals(0L, stage.getTotalEvents());
            assertEquals(0L, stage.getUniqueSessions());
            assertEquals(0.0, stage.getConversionRateFromPrevious());
            assertEquals(0.0, stage.getOverallConversionRate());
        }
    }

    // =========================================================================
    // SCENARIO 6: Auxiliary Channel Aggregation
    // =========================================================================
    @Test
    @DisplayName("Scenario 6: Auxiliary engagement channels aggregate counts accurately")
    void test06_AuxiliaryChannelAggregation() {
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_WISHLIST"), any(), any())).thenReturn(110L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("RECOMMENDATION_CLICK"), any(), any())).thenReturn(75L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("AI_STYLIST_ENGAGE"), any(), any())).thenReturn(50L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("WHATSAPP_COMMERCE_ENGAGE"), any(), any())).thenReturn(40L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("TROUSSEAU_ENGAGE"), any(), any())).thenReturn(30L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("SHARE_LINK_ENGAGE"), any(), any())).thenReturn(60L);

        ConversionFunnelResponse response = customerBehaviorService.getConversionFunnel("7D", null, null);

        assertNotNull(response.getChannelEngagement());
        assertEquals(110L, response.getChannelEngagement().get("wishlist"));
        assertEquals(75L, response.getChannelEngagement().get("recommendations"));
        assertEquals(50L, response.getChannelEngagement().get("aiStylist"));
        assertEquals(40L, response.getChannelEngagement().get("whatsapp"));
        assertEquals(30L, response.getChannelEngagement().get("trousseau"));
        assertEquals(60L, response.getChannelEngagement().get("shareLinks"));
    }

    // =========================================================================
    // SCENARIO 7: Legacy 4-Stage Funnel Backward Compatibility
    // =========================================================================
    @Test
    @DisplayName("Scenario 7: getBehavioralOverview preserves legacy 4-stage funnel and provides 8-stage eCommerce funnel")
    void test07_LegacyFunnelBackwardCompatibility() {
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(400L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(200L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(100L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(50L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(40L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(25L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(20L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(15L);

        BehavioralOverviewResponse overview = customerBehaviorService.getBehavioralOverview("30D", null, null);

        assertNotNull(overview);
        assertNotNull(overview.getFunnel());
        assertEquals(4, overview.getFunnel().size(), "Legacy funnel must retain exactly 4 stages");
        assertEquals("PRODUCT_VIEW", overview.getFunnel().get(0).getStage());
        assertEquals("ADD_TO_CART", overview.getFunnel().get(1).getStage());
        assertEquals("CHECKOUT_INITIATED", overview.getFunnel().get(2).getStage());
        assertEquals("ORDER_COMPLETED", overview.getFunnel().get(3).getStage());

        assertNotNull(overview.getEcommerceFunnel());
        assertEquals(8, overview.getEcommerceFunnel().size(), "eCommerce funnel must retain 8 stages");
    }

    // =========================================================================
    // SCENARIO 8: Ingestion Failure Isolation
    // =========================================================================
    @Test
    @DisplayName("Scenario 8: Telemetry ingestion failure is isolated, returning 200 OK null without crashing UI")
    void test08_TelemetryFailureIsolation() {
        CustomerBehaviorService mockService = mock(CustomerBehaviorService.class);
        CustomerEventController controller = new CustomerEventController(mockService);

        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId("sess_outage_001")
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .metadata(Map.of("productId", 101))
                .build();

        when(mockService.recordEvent(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Simulated Database Timeout"));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.168.1.1");

        ResponseEntity<ApiResponse<CustomerEventResponse>> response =
                controller.trackEvent(null, req, servletRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value(), "Controller must return HTTP 200 on failure");
        assertTrue(response.getBody().isSuccess(), "Response success must be true");
        assertNull(response.getBody().getData(), "Data should be null so browser does not retry frantically");
    }

    // =========================================================================
    // SCENARIO 9: Server Context Enforces Authoritative User ID
    // =========================================================================
    @Test
    @DisplayName("Scenario 9: Server context enforces authoritative authenticated user ID over client claims")
    void test09_ServerContextEnforcesUserId() {
        CustomerBehaviorService mockService = mock(CustomerBehaviorService.class);
        CustomerEventController controller = new CustomerEventController(mockService);

        User authenticatedUser = User.builder().id(77L).email("vip@sareekart.com").build();
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId("sess_auth_claim_test")
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .build();

        when(mockService.recordEvent(eq(req), eq(77L), any(), any()))
                .thenReturn(CustomerEventResponse.builder().id(500L).userId(77L).build());

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        ResponseEntity<ApiResponse<CustomerEventResponse>> res =
                controller.trackEvent(authenticatedUser, req, servletRequest);

        assertNotNull(res);
        assertEquals(77L, res.getBody().getData().getUserId(), "User ID must match server authenticated user");
        verify(mockService).recordEvent(req, 77L, "127.0.0.1", null);
    }

    // =========================================================================
    // SCENARIO 10: Admin Controller RBAC Endpoint Verification
    // =========================================================================
    @Test
    @DisplayName("Scenario 10: AdminCustomerBehaviorController.getConversionFunnel returns 200 OK with ConversionFunnelResponse")
    void test10_AdminFunnelEndpointVerification() {
        AdminCustomerBehaviorController controller = new AdminCustomerBehaviorController(customerBehaviorService);

        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(anyString(), any(), any())).thenReturn(10L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(anyString(), any(), any())).thenReturn(5L);

        ResponseEntity<ApiResponse<ConversionFunnelResponse>> responseEntity =
                controller.getConversionFunnel("30D", null, null);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().isSuccess());
        assertNotNull(responseEntity.getBody().getData());
        assertEquals(8, responseEntity.getBody().getData().getStages().size());
    }
}
