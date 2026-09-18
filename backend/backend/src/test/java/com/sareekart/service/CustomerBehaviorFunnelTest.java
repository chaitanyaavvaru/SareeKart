package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.controller.AdminCustomerBehaviorController;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 13 Stage 5 — Full-Funnel eCommerce Analytics & Conversion Tracking Verification Suite.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CustomerBehaviorFunnelTest {

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
    private static final String TEST_SESSION = "sess_funnel_stage5_test_99";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(101L)
                .email("analyst@sareekart.com")
                .firstName("SareeKart")
                .lastName("Analyst")
                .build();
    }

    // =========================================================================
    // 1. EVENT TAXONOMY & ENUM VALIDATION
    // =========================================================================

    @Test
    @DisplayName("STAGE5-TAX-1: All 8 core eCommerce funnel event types are recognized and valid")
    void testCoreFunnelEventTypesAreValid() {
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
            assertTrue(CustomerEventType.isValid(eventType), "Event type " + eventType + " must be recognized as valid");
            assertDoesNotThrow(() -> CustomerEventType.fromString(eventType));
        }
    }

    @Test
    @DisplayName("STAGE5-TAX-2: Auxiliary engagement channel event types are recognized and valid")
    void testAuxiliaryChannelEventTypesAreValid() {
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

    @Test
    @DisplayName("STAGE5-TAX-3: Invalid event types are rejected safely")
    void testInvalidEventTypeRejected() {
        assertFalse(CustomerEventType.isValid("INVALID_TYPE_XYZ"));
        assertFalse(CustomerEventType.isValid(null));
        assertFalse(CustomerEventType.isValid("   "));
        assertThrows(IllegalArgumentException.class, () -> CustomerEventType.fromString("UNKNOWN_ACTION"));
    }

    // =========================================================================
    // 2. FULL 8-STAGE ECOMMERCE CONVERSION FUNNEL
    // =========================================================================

    @Test
    @DisplayName("STAGE5-FUNNEL-1: buildEcommerceFunnel computes all 8 stages in chronological order")
    void testFullEcommerceFunnelStages() {
        // Setup mock counts for 8 stages
        // Stage 1: Landing (1000 total, 500 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("LANDING_PAGE_VIEW"), any(), any())).thenReturn(1000L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("LANDING_PAGE_VIEW"), any(), any())).thenReturn(500L);

        // Stage 2: Product View (800 total, 400 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(800L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(400L);

        // Stage 3: Search Query (300 total, 200 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("SEARCH_QUERY"), any(), any())).thenReturn(300L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("SEARCH_QUERY"), any(), any())).thenReturn(200L);

        // Stage 4: Category Browse (500 total, 300 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CATEGORY_VIEW"), any(), any())).thenReturn(500L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CATEGORY_VIEW"), any(), any())).thenReturn(300L);

        // Stage 5: Added to Bag (250 total, 150 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(250L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(150L);

        // Stage 6: Checkout Initiated (120 total, 90 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(120L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(90L);

        // Stage 7: Payment Step (80 total, 72 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PAYMENT_ATTEMPT"), any(), any())).thenReturn(80L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PAYMENT_ATTEMPT"), any(), any())).thenReturn(72L);

        // Stage 8: Purchase Completed (60 total, 50 sessions)
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(60L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(50L);

        ConversionFunnelResponse response = customerBehaviorService.getConversionFunnel("30D", null, null);

        assertNotNull(response);
        assertEquals(8, response.getStages().size(), "Funnel must contain exactly 8 eCommerce stages");

        // Verify stage ordering
        assertEquals("LANDING_PAGE_VIEW", response.getStages().get(0).getStage());
        assertEquals("PRODUCT_VIEW", response.getStages().get(1).getStage());
        assertEquals("SEARCH_QUERY", response.getStages().get(2).getStage());
        assertEquals("CATEGORY_VIEW", response.getStages().get(3).getStage());
        assertEquals("ADD_TO_CART", response.getStages().get(4).getStage());
        assertEquals("CHECKOUT_INITIATED", response.getStages().get(5).getStage());
        assertEquals("PAYMENT_ATTEMPT", response.getStages().get(6).getStage());
        assertEquals("ORDER_COMPLETED", response.getStages().get(7).getStage());

        // Stage 1 is entry (100% conversion)
        assertEquals(100.0, response.getStages().get(0).getOverallConversionRate());
        assertEquals(100.0, response.getStages().get(0).getConversionRateFromPrevious());

        // Stage 2: 400 / 500 = 80.0%
        assertEquals(80.0, response.getStages().get(1).getOverallConversionRate());
        assertEquals(80.0, response.getStages().get(1).getConversionRateFromPrevious());

        // Overall conversion (Stage 8 sessions / Stage 1 sessions = 50 / 500 = 10.0%)
        assertEquals(10.0, response.getOverallConversionRate());

        // Detail to Cart (Stage 5 cart sessions / Stage 2 view sessions = 150 / 400 = 37.5%)
        assertEquals(37.5, response.getDetailToCartRate());

        // Cart to Checkout (Stage 6 checkout sessions / Stage 5 cart sessions = 90 / 150 = 60.0%)
        assertEquals(60.0, response.getCartToCheckoutRate());

        // Checkout to Payment (Stage 7 payment sessions / Stage 6 checkout sessions = 72 / 90 = 80.0%)
        assertEquals(80.0, response.getCheckoutToPaymentRate());

        // Payment to Order (Stage 8 order sessions / Stage 7 payment sessions = 50 / 72 = 69.4%)
        assertEquals(69.4, response.getPaymentToOrderRate());

        // Cart abandonment (1.0 - (50 / 150) = 66.7%)
        assertEquals(66.7, response.getCartAbandonmentRate());

        // Checkout abandonment (1.0 - (50 / 90) = 44.4%)
        assertEquals(44.4, response.getCheckoutAbandonmentRate());
    }

    @Test
    @DisplayName("STAGE5-FUNNEL-2: Empty telemetry returns safe zero-bounded conversion rates without exceptions")
    void testEmptyTelemetryZeroBoundedRates() {
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
    // 3. AUXILIARY CHANNEL ENGAGEMENT ATTRIBUTION
    // =========================================================================

    @Test
    @DisplayName("STAGE5-AUX-1: Auxiliary engagement channels aggregate counts accurately")
    void testAuxiliaryChannelEngagementAggregation() {
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_WISHLIST"), any(), any())).thenReturn(145L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("RECOMMENDATION_CLICK"), any(), any())).thenReturn(88L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("AI_STYLIST_ENGAGE"), any(), any())).thenReturn(42L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("WHATSAPP_COMMERCE_ENGAGE"), any(), any())).thenReturn(35L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("TROUSSEAU_ENGAGE"), any(), any())).thenReturn(27L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("SHARE_LINK_ENGAGE"), any(), any())).thenReturn(64L);

        ConversionFunnelResponse response = customerBehaviorService.getConversionFunnel("7D", null, null);

        assertNotNull(response.getChannelEngagement());
        assertEquals(145L, response.getChannelEngagement().get("wishlist"));
        assertEquals(88L, response.getChannelEngagement().get("recommendations"));
        assertEquals(42L, response.getChannelEngagement().get("aiStylist"));
        assertEquals(35L, response.getChannelEngagement().get("whatsapp"));
        assertEquals(27L, response.getChannelEngagement().get("trousseau"));
        assertEquals(64L, response.getChannelEngagement().get("shareLinks"));
    }

    // =========================================================================
    // 4. BACKWARD COMPATIBILITY
    // =========================================================================

    @Test
    @DisplayName("STAGE5-COMPAT-1: getBehavioralOverview preserves legacy 4-stage funnel for existing tests")
    void testLegacyFunnelPreservedInOverview() {
        // Setup mock counts for legacy stages
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(500L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("PRODUCT_VIEW"), any(), any())).thenReturn(250L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(100L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ADD_TO_CART"), any(), any())).thenReturn(60L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(50L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("CHECKOUT_INITIATED"), any(), any())).thenReturn(30L);
        when(customerEventRepository.countByEventTypeAndCreatedAtBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(25L);
        when(customerEventRepository.countDistinctSessionsByEventTypeBetween(eq("ORDER_COMPLETED"), any(), any())).thenReturn(20L);

        BehavioralOverviewResponse overview = customerBehaviorService.getBehavioralOverview("30D", null, null);

        assertNotNull(overview);
        assertNotNull(overview.getFunnel());
        assertEquals(4, overview.getFunnel().size(), "Legacy funnel must retain exactly 4 stages for backward compatibility");
        assertEquals("PRODUCT_VIEW", overview.getFunnel().get(0).getStage());
        assertEquals("ADD_TO_CART", overview.getFunnel().get(1).getStage());
        assertEquals("CHECKOUT_INITIATED", overview.getFunnel().get(2).getStage());
        assertEquals("ORDER_COMPLETED", overview.getFunnel().get(3).getStage());

        // Full 8-stage eCommerce funnel is also present
        assertNotNull(overview.getEcommerceFunnel());
        assertEquals(8, overview.getEcommerceFunnel().size(), "eCommerce funnel must have 8 stages");

        // Channel engagement is also present
        assertNotNull(overview.getChannelEngagement());
        assertTrue(overview.getChannelEngagement().containsKey("wishlist"));
        assertTrue(overview.getChannelEngagement().containsKey("recommendations"));
        assertTrue(overview.getChannelEngagement().containsKey("aiStylist"));
        assertTrue(overview.getChannelEngagement().containsKey("whatsapp"));
        assertTrue(overview.getChannelEngagement().containsKey("trousseau"));
        assertTrue(overview.getChannelEngagement().containsKey("shareLinks"));
    }

    // =========================================================================
    // 5. PRIVACY & PII PROTECTION
    // =========================================================================

    @Test
    @DisplayName("STAGE5-PRIVACY-1: Sensitive fields in event metadata are strictly redacted")
    void testPiiSanitizationInEventIngestion() {
        CustomerEventRequest request = CustomerEventRequest.builder()
                .clientEventId("evt_privacy_test_01")
                .sessionId(TEST_SESSION)
                .eventType("PAYMENT_ATTEMPT")
                .entityType("PAYMENT")
                .metadata(Map.of(
                        "paymentMethod", "RAZORPAY",
                        "password", "secret123",
                        "token", "jwt_token_abc",
                        "creditcard", "4111222233334444",
                        "cvv", "123",
                        "amount", 12500
                ))
                .build();

        when(customerEventRepository.existsByClientEventId("evt_privacy_test_01")).thenReturn(false);
        when(customerEventRepository.save(any(CustomerEvent.class))).thenAnswer(invocation -> {
            CustomerEvent saved = invocation.getArgument(0);
            saved.setId(999L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        CustomerEventResponse response = customerBehaviorService.recordEvent(request, null, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertNotNull(response.getMetadata());
        assertEquals("[REDACTED]", response.getMetadata().get("password"));
        assertEquals("[REDACTED]", response.getMetadata().get("token"));
        assertEquals("[REDACTED]", response.getMetadata().get("creditcard"));
        assertEquals("[REDACTED]", response.getMetadata().get("cvv"));
        assertEquals("RAZORPAY", response.getMetadata().get("paymentMethod"));
        assertEquals(12500, response.getMetadata().get("amount"));
    }

    // =========================================================================
    // 6. CONTROLLER INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("STAGE5-CTRL-1: AdminCustomerBehaviorController.getConversionFunnel returns 200 OK")
    void testControllerConversionFunnelEndpoint() {
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
