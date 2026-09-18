package com.sareekart.service;

import com.sareekart.dto.request.AddressRequest;
import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.trousseau.ShareTrousseauWhatsAppRequest;
import com.sareekart.dto.trousseau.TrousseauWhatsAppShareResponse;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.entity.*;
import com.sareekart.entity.Conversation;
import com.sareekart.entity.ConversationStatus;
import com.sareekart.entity.MessageType;
import com.sareekart.entity.SenderType;
import com.sareekart.enums.RefundMode;
import com.sareekart.enums.ReturnReason;
import com.sareekart.enums.ReturnStatus;
import com.sareekart.enums.ReturnType;
import com.sareekart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Challenger 2 Adversarial Stress Test Suite for WhatsApp Production Readiness.
 *
 * Covers:
 * - R3: Phone Normalization edge cases, Indian mobile prefix rules, delimiter variations, invalid inputs.
 * - R3: Sensitive data leak audits (DB PK masking, no passwords, no JWTs, no credit cards).
 * - R4: Failure domain isolation under simulated Meta 429, 500, 503, timeouts, runtime exceptions during checkout & updates.
 * - R4: High-concurrency rate limiter stress testing (multi-threaded per-recipient burst and global throughput caps).
 * - R5: Bridal trousseau complete isolation from commerce threads, share token isolation, error resilience, and vote alert opt-out.
 */
@SpringBootTest
@Transactional
public class WhatsAppProductionReadinessChallengerTest {

    @Autowired
    private WhatsAppIdentityService identityService;

    @Autowired
    private WhatsAppNotificationService notificationService;

    @Autowired
    private OrderNotificationService orderNotificationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WhatsAppRateLimiter rateLimiter;

    @Autowired
    private TrousseauWhatsAppService trousseauWhatsAppService;

    @Autowired
    private TrousseauService trousseauService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private WhatsAppContactRepository contactRepository;

    @Autowired
    private WhatsAppMessageRepository messageRepository;

    @Autowired
    private TrousseauBoardRepository boardRepository;

    @MockBean
    private WhatsAppApiClient whatsAppApiClient;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        reset(whatsAppApiClient);
        rateLimiter.reset();

        testUser = userRepository.save(User.builder()
                .firstName("Challenger")
                .lastName("Patron")
                .email("challenger." + UUID.randomUUID() + "@sareekart.com")
                .mobile("+919876543210")
                .password("UltraSecret@2026!")
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Adversarial Royal Zari Kanchipuram")
                .price(new BigDecimal("42000.00"))
                .stockQuantity(100)
                .active(true)
                .fabric("Pure Mulberry Silk")
                .color("Royal Blue")
                .occasion("WEDDING")
                .build());
    }

    // =========================================================================
    // REQUIREMENT R3: ADVERSARIAL PHONE NORMALIZATION STRESS TESTS
    // =========================================================================

    @Test
    @DisplayName("CHALLENGE R3-1: Phone normalization canonical 10-digit, E.164, and Meta formatting across formats")
    void testPhoneNormalizationCanonicalFormats() {
        String[] validIndianNumbers = {
                "+919876543210",
                "919876543210",
                "09876543210",
                "9876543210",
                "+91 98765 43210",
                "+91-98765-43210",
                "+91.98765.43210",
                "   +919876543210   ",
                "+91 (98765) 43210"
        };

        for (String phone : validIndianNumbers) {
            String normalized10 = identityService.normalizePhoneNumber(phone);
            assertEquals("9876543210", normalized10, "Normalized 10-digit mismatch for: " + phone);

            String e164 = identityService.normalizeToE164(phone);
            assertEquals("+919876543210", e164, "E.164 mismatch for: " + phone);

            String metaPhone = identityService.toMetaRecipientPhone(phone);
            assertEquals("919876543210", metaPhone, "Meta recipient phone mismatch for: " + phone);

            assertTrue(identityService.isValidIndianMobile(phone), "Should be valid Indian mobile for: " + phone);
        }
    }

    @Test
    @DisplayName("CHALLENGE R3-2: Indian mobile series validity — digits 6, 7, 8, 9 valid; 0 to 5 invalid")
    void testIndianMobilePrefixSeries() {
        // Valid series: starting with 6, 7, 8, 9
        String[] validPrefixes = {"+916234567890", "+917234567890", "+918234567890", "+919234567890"};
        for (String phone : validPrefixes) {
            assertTrue(identityService.isValidIndianMobile(phone), "Series starting with [6-9] must be valid: " + phone);
        }

        // Invalid series: starting with 0, 1, 2, 3, 4, 5
        String[] invalidPrefixes = {"+910234567890", "+911234567890", "+912234567890", "+913234567890", "+914234567890", "+915234567890"};
        for (String phone : invalidPrefixes) {
            assertFalse(identityService.isValidIndianMobile(phone), "Series starting with 0-5 must be invalid: " + phone);
        }
    }

    @Test
    @DisplayName("CHALLENGE R3-3: Adversarial invalid, truncated, and international numbers rejected")
    void testAdversarialInvalidPhoneFormats() {
        assertFalse(identityService.isValidIndianMobile(null), "Null must be invalid");
        assertFalse(identityService.isValidIndianMobile(""), "Empty string must be invalid");
        assertFalse(identityService.isValidIndianMobile("   "), "Blank string must be invalid");
        assertFalse(identityService.isValidIndianMobile("abcdefghij"), "Alphabetic characters must be invalid");
        assertFalse(identityService.isValidIndianMobile("!@#$%^&*()"), "Special characters must be invalid");
        assertFalse(identityService.isValidIndianMobile("12345"), "5 digits must be invalid");
        assertFalse(identityService.isValidIndianMobile("987654321"), "9 digits must be invalid");
        assertFalse(identityService.isValidIndianMobile("987654321099"), "12 digits without 91 prefix must be invalid");
        assertFalse(identityService.isValidIndianMobile("919876543210999"), "Overly long number must be invalid");

        // International numbers: US (+1), UK (+44), UAE (+971)
        assertFalse(identityService.isValidIndianMobile("+14155552671"), "US phone number must not be valid Indian mobile");
        assertFalse(identityService.isValidIndianMobile("+447911123456"), "UK phone number must not be valid Indian mobile");
        assertFalse(identityService.isValidIndianMobile("+971501234567"), "UAE phone number must not be valid Indian mobile");
    }

    @Test
    @DisplayName("CHALLENGE FINDING: 10-digit international numbers starting with [6-9] (e.g. Singapore +65) falsely collide with Indian mobile validation")
    void testEmpiricalNonIndianCollisionObservation() {
        // Empirically demonstrate the edge-case collision where international number +6581234567 (Singapore)
        // is normalized as Indian mobile because stripped digits length is 10 and starts with 6.
        boolean singaporeTreatedAsIndian = identityService.isValidIndianMobile("+6581234567");
        String normalizedSingapore = identityService.normalizeToE164("+6581234567");
        String metaRecipient = identityService.toMetaRecipientPhone("+6581234567");

        // Empirically verifies the implementation behavior under review
        assertTrue(singaporeTreatedAsIndian, "VULNERABILITY CONFIRMED: Singapore 8-digit mobile with +65 totals 10 digits and is treated as Indian");
        assertEquals("+916581234567", normalizedSingapore, "VULNERABILITY CONFIRMED: Singapore +65 number prepended with +91");
        assertEquals("916581234567", metaRecipient, "VULNERABILITY CONFIRMED: Meta recipient phone prepended with 91");
    }

    // =========================================================================
    // REQUIREMENT R4: FAILURE DOMAIN ISOLATION UNDER SIMULATED META API OUTAGES
    // =========================================================================

    @Test
    @DisplayName("CHALLENGE R4-1: Meta API 429 Too Many Requests during Order Checkout does not abort transaction")
    void testOrderCheckoutSurvivesMeta429TooManyRequests() {
        doThrow(WebClientResponseException.create(429, "Too Many Requests", HttpHeaders.EMPTY, null, null))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        Order order = createAndSaveTestOrder();

        assertDoesNotThrow(() -> orderNotificationService.sendOrderPlacedNotification(order),
                "Order notification must not throw on Meta 429");

        Order persisted = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus(), "Order must remain CONFIRMED");
    }

    @Test
    @DisplayName("CHALLENGE R4-2: Meta API 500 Internal Server Error does not roll back order placement")
    void testOrderCheckoutSurvivesMeta500ServerError() {
        doThrow(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, null, null))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        Order order = createAndSaveTestOrder();

        assertDoesNotThrow(() -> orderNotificationService.sendOrderPlacedNotification(order),
                "Order notification must not throw on Meta 500");

        Order persisted = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus(), "Order must remain CONFIRMED");
    }

    @Test
    @DisplayName("CHALLENGE R4-3: Meta API 503 Service Unavailable does not roll back order placement")
    void testOrderCheckoutSurvivesMeta503ServiceUnavailable() {
        doThrow(WebClientResponseException.create(503, "Service Unavailable", HttpHeaders.EMPTY, null, null))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        Order order = createAndSaveTestOrder();

        assertDoesNotThrow(() -> orderNotificationService.sendOrderPlacedNotification(order),
                "Order notification must not throw on Meta 503");

        Order persisted = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus(), "Order must remain CONFIRMED");
    }

    @Test
    @DisplayName("CHALLENGE R4-4: Meta API Connection Timeout does not roll back order placement")
    void testOrderCheckoutSurvivesConnectionTimeout() {
        doThrow(new WebClientRequestException(new TimeoutException("Connection timed out after 5000ms"),
                org.springframework.http.HttpMethod.POST, URI.create("https://graph.facebook.com/v19.0"), HttpHeaders.EMPTY))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        Order order = createAndSaveTestOrder();

        assertDoesNotThrow(() -> orderNotificationService.sendOrderPlacedNotification(order),
                "Order notification must not throw on connection timeout");

        Order persisted = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus(), "Order must remain CONFIRMED");
    }

    @Test
    @DisplayName("CHALLENGE R4-5: Order status update to DELIVERED succeeds despite WhatsApp API failure")
    void testOrderStatusUpdateSurvivesWhatsAppFailure() {
        doThrow(new RuntimeException("Meta Graph API unreachable"))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        Order order = createAndSaveTestOrder();

        assertDoesNotThrow(() -> orderService.updateOrderStatus(order.getId(), "DELIVERED"),
                "Updating order status must succeed even if WhatsApp alert fails");

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.DELIVERED, updated.getStatus());
        assertEquals("COMPLETED", updated.getPaymentStatus());
        assertNotNull(updated.getDeliveredAt());
    }

    // =========================================================================
    // REQUIREMENT R4: HIGH-CONCURRENCY RATE LIMITER STRESS TESTING
    // =========================================================================

    @Test
    @DisplayName("CHALLENGE R4-6: Per-recipient rate limiter: 20 concurrent threads — exactly 5 acquire, 15 throttled")
    void testConcurrentPerRecipientRateLimiterBurst() throws Exception {
        rateLimiter.reset();
        String targetPhone = "+919876543210";
        int totalThreads = 20;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger throttleCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (rateLimiter.tryAcquire(targetPhone)) {
                        successCount.incrementAndGet();
                    } else {
                        throttleCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously for a massive burst
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent rate limit execution timed out");
        executor.shutdown();

        assertEquals(5, successCount.get(), "Per-recipient limit must permit exactly 5 requests in window");
        assertEquals(15, throttleCount.get(), "Per-recipient limit must throttle remaining 15 burst requests");

        // A second recipient must still be able to acquire independently
        assertTrue(rateLimiter.tryAcquire("+919876540099"), "Independent recipient must not be blocked by first recipient");
    }

    @Test
    @DisplayName("CHALLENGE R4-7: Global throughput cap: 100 concurrent threads across distinct numbers — exactly 80 acquire, 20 throttled")
    void testConcurrentGlobalRateLimiterCap() throws Exception {
        rateLimiter.reset();
        int totalThreads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger throttleCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final String distinctPhone = String.format("+9198%08d", i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (rateLimiter.tryAcquire(distinctPhone)) {
                        successCount.incrementAndGet();
                    } else {
                        throttleCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Simultaneous global burst
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent global rate limit execution timed out");
        executor.shutdown();

        assertEquals(80, successCount.get(), "Global cap must allow exactly 80 requests per second");
        assertEquals(20, throttleCount.get(), "Global cap must throttle exactly 20 requests when capacity is exhausted");

        // After reset, capacity is restored
        rateLimiter.reset();
        assertTrue(rateLimiter.tryAcquire("+919800000001"), "Rate limiter reset must restore capacity");
    }

    // =========================================================================
    // REQUIREMENT R3: SENSITIVE DATA LEAK & PRIMARY KEY MASKING VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("CHALLENGE R3-4: Outbound message payloads completely mask DB primary keys (SK-ORD- / SK-RET-)")
    void testOutboundMessagesMaskDatabasePrimaryKeys() {
        Order order = createAndSaveTestOrder();

        // 1. Order Placed
        WhatsAppNotificationResponse placed = notificationService.sendOrderPlacedNotification(order);
        assertNotNull(placed);
        String placedMsg = placed.getMessageContent();
        assertTrue(placedMsg.contains(String.format("SK-ORD-%06d", order.getId())),
                "Order Placed must use masked reference SK-ORD-%06d");
        assertTrue(placedMsg.contains("https://sareekart.com/orders/track?orderNumber=SK-ORD-"),
                "Tracking link must use masked reference");

        // 2. Order Shipped
        WhatsAppNotificationResponse shipped = notificationService.sendOrderShippedNotification(order);
        assertNotNull(shipped);
        assertTrue(shipped.getMessageContent().contains(String.format("SK-ORD-%06d", order.getId())),
                "Order Shipped must use masked reference SK-ORD-%06d");

        // 3. Order Out For Delivery
        WhatsAppNotificationResponse ofd = notificationService.sendOrderOutForDeliveryNotification(order);
        assertNotNull(ofd);
        assertTrue(ofd.getMessageContent().contains(String.format("SK-ORD-%06d", order.getId())),
                "Order Out For Delivery must use masked reference SK-ORD-%06d");

        // 4. Order Delivered
        WhatsAppNotificationResponse delivered = notificationService.sendOrderDeliveredNotification(order);
        assertNotNull(delivered);
        assertTrue(delivered.getMessageContent().contains(String.format("SK-ORD-%06d", order.getId())),
                "Order Delivered must use masked reference SK-ORD-%06d");

        // 5. Return Pickup
        ReturnRequest retReq = returnRequestRepository.save(ReturnRequest.builder()
                .order(order)
                .user(testUser)
                .reason(ReturnReason.ZARI_DEFECT)
                .comments("Zari thread slightly loose")
                .refundAmount(order.getTotalAmount())
                .refundMode(RefundMode.ORIGINAL_PAYMENT)
                .reverseCourier("Blue Dart Reverse")
                .reverseTrackingNumber("SK-REV-987654")
                .build());

        WhatsAppNotificationResponse retResp = notificationService.sendReturnPickupNotification(retReq);
        assertNotNull(retResp);
        assertTrue(retResp.getMessageContent().contains(String.format("SK-RET-%06d", retReq.getId())),
                "Return pickup message must use masked return reference SK-RET-%06d");
    }

    @Test
    @DisplayName("CHALLENGE R3-5: Outbound messages contain zero passwords, JWT tokens, or credit cards")
    void testOutboundMessagesContainNoPiiOrCredentials() {
        Order order = createAndSaveTestOrder();

        WhatsAppNotificationResponse resp = notificationService.sendOrderPlacedNotification(order);
        assertNotNull(resp);
        String msg = resp.getMessageContent();

        // 1. User password never leaked
        assertFalse(msg.contains("UltraSecret@2026!"), "User password must never leak in WhatsApp notification");
        assertFalse(msg.toLowerCase().contains("password"), "Message must not contain password keyword");

        // 2. JWT tokens never leaked
        assertFalse(msg.contains("Bearer "), "Message must not contain JWT Bearer header");
        assertFalse(msg.contains("eyJ"), "Message must not contain JWT base64 pattern");

        // 3. Credit cards never leaked (16 digits / Luhn test regex)
        Pattern ccPattern = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b");
        assertFalse(ccPattern.matcher(msg).find(), "Message must not contain raw credit card numbers");
    }

    // =========================================================================
    // REQUIREMENT R5: BRIDAL TROUSSEAU COMPLETE ISOLATION VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("CHALLENGE R5-1: Bridal trousseau share uses dedicated token without polluting commerce threads")
    void testBridalTrousseauShareDoesNotPolluteCommerceThreads() {
        long initialConversations = conversationRepository.count();
        long initialMessages = messageRepository.count();

        // Step 1: Create a trousseau board for bride
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Adversarial Royal Wedding Trousseau")
                .shareToken(UUID.randomUUID().toString())
                .status("ACTIVE")
                .isPublicVoting(true)
                .build());

        // Step 2: Dispatch trousseau WhatsApp invitation to a recipient phone
        String recipientPhone = "+919876549999";
        ShareTrousseauWhatsAppRequest shareRequest = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone(recipientPhone)
                .recipientName("Aunt Shobha")
                .customNote("Please review my bridal saree picks!")
                .build();

        TrousseauWhatsAppShareResponse shareResponse = trousseauWhatsAppService.sendShareInvitation(
                testUser.getId(), board.getId(), shareRequest);

        assertNotNull(shareResponse);
        assertTrue(shareResponse.isSuccess(), "Trousseau share must succeed");
        assertTrue(shareResponse.getShareUrl().contains("/trousseau/share/" + board.getShareToken()),
                "Share URL must use dedicated trousseau token path");

        // Step 3: Verify strict table isolation — ZERO rows added to conversations or whatsapp_messages
        assertEquals(initialConversations, conversationRepository.count(),
                "Trousseau share dispatch must NEVER create records in conversations table");
        assertEquals(initialMessages, messageRepository.count(),
                "Trousseau share dispatch must NEVER insert records into whatsapp_messages table");
    }

    @Test
    @DisplayName("CHALLENGE R5-2: Existing commerce conversation for recipient remains untouched by trousseau share")
    void testTrousseauShareDoesNotInterfereWithExistingCommerceConversation() {
        String recipientPhone = "+919876541122";
        String normalizedPhone = identityService.normalizePhoneNumber(recipientPhone);

        // Setup active commerce contact and conversation
        WhatsAppContact contact = contactRepository.save(WhatsAppContact.builder()
                .phoneNumber(normalizedPhone)
                .name("Commerce Patron")
                .optedIn(true)
                .build());

        Conversation commerceConv = conversationRepository.save(Conversation.builder()
                .contact(contact)
                .status(com.sareekart.entity.ConversationStatus.BOT_HANDLING)
                .tags("VIP_INQUIRY")
                .build());

        WhatsAppMessage commerceMsg = messageRepository.save(WhatsAppMessage.builder()
                .conversation(commerceConv)
                .senderType(SenderType.CUSTOMER)
                .messageType(MessageType.TEXT)
                .content("Do you have Banarasi georgette sarees?")
                .wamId("wam_commerce_test_001")
                .build());

        long totalConversationsBefore = conversationRepository.count();
        long totalMessagesBefore = messageRepository.count();

        // Send bridal trousseau share to the exact same recipient phone
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Cross-Thread Isolation Board")
                .shareToken(UUID.randomUUID().toString())
                .status("ACTIVE")
                .isPublicVoting(true)
                .build());

        ShareTrousseauWhatsAppRequest shareRequest = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone(recipientPhone)
                .recipientName("Commerce Patron")
                .customNote("Checking wedding sarees")
                .build();

        trousseauWhatsAppService.sendShareInvitation(testUser.getId(), board.getId(), shareRequest);

        // Verify conversation table count is unchanged
        assertEquals(totalConversationsBefore, conversationRepository.count(),
                "Commerce conversation table count must remain strictly unchanged");
        assertEquals(totalMessagesBefore, messageRepository.count(),
                "Commerce message table count must remain strictly unchanged");

        // Verify existing commerce conversation status and tags are completely untouched
        Conversation retrievedConv = conversationRepository.findById(commerceConv.getId()).orElseThrow();
        assertEquals(com.sareekart.entity.ConversationStatus.BOT_HANDLING, retrievedConv.getStatus(),
                "Commerce conversation status must not be modified by trousseau sharing");
        assertEquals("VIP_INQUIRY", retrievedConv.getTags(),
                "Commerce conversation tags must not be modified by trousseau sharing");
    }

    @Test
    @DisplayName("CHALLENGE R5-3: Trousseau WhatsApp failure is gracefully isolated without breaking board access")
    void testTrousseauWhatsAppFailureIsolation() {
        doThrow(new RuntimeException("Meta WhatsApp API Timeout"))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Resilience Test Board")
                .shareToken(UUID.randomUUID().toString())
                .status("ACTIVE")
                .isPublicVoting(true)
                .build());

        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919876543210")
                .recipientName("Sister")
                .build();

        // Must not throw an unhandled exception
        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendShareInvitation(
                testUser.getId(), board.getId(), request);

        assertNotNull(response);
        assertFalse(response.isSuccess(), "Response success flag should be false on API outage");
        assertTrue(response.getMessage().contains("WhatsApp service unavailable"),
                "Message must indicate graceful degradation");
        assertNotNull(response.getShareUrl(), "Share URL must still be generated");

        // Board must remain completely healthy in DB
        TrousseauBoard persisted = boardRepository.findById(board.getId()).orElseThrow();
        assertEquals("ACTIVE", persisted.getStatus());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Order createAndSaveTestOrder() {
        return orderRepository.save(Order.builder()
                .user(testUser)
                .totalAmount(new BigDecimal("42000.00"))
                .status(OrderStatus.CONFIRMED)
                .paymentMethod("ONLINE")
                .paymentStatus("PAID")
                .trackingNumber("SK-TRK-" + (100000 + (int)(Math.random() * 899999)))
                .courierPartner("Blue Dart Apex Air")
                .estimatedDeliveryDate("2-3 Business Days")
                .shippingAddress(Address.builder()
                        .fullName("Challenger Patron")
                        .phone("+919876543210")
                        .streetAddress("42 Marina Boulevard")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .pincode("600028")
                        .build())
                .build());
    }
}
