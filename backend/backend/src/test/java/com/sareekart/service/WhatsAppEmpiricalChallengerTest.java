package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.controller.WhatsAppWebhookController;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppWebhookDto;
import com.sareekart.entity.*;
import com.sareekart.repository.*;
import com.sareekart.security.WhatsAppWebhookSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Adversarial Empirical Challenger Suite for Phase 13 Stage 4 (WhatsApp Production Readiness).
 *
 * <p>Exhaustively stress-tests:
 * <ul>
 *   <li>HMAC-SHA256 tampering, payload byte mutations, malformed headers, timing/probe attacks</li>
 *   <li>Idempotency high-concurrency replay attacks (50 threads), sliding window, DB persistence fallback</li>
 *   <li>Regulatory opt-in/opt-out keyword permutations, case-insensitivity, whitespace variations, rapid toggling</li>
 * </ul>
 */
@SpringBootTest
@Transactional
public class WhatsAppEmpiricalChallengerTest {

    @Autowired
    private WhatsAppWebhookSignatureValidator signatureValidator;

    @Autowired
    private WhatsAppWebhookController webhookController;

    @Autowired
    private WhatsAppWebhookService webhookService;

    @Autowired
    private WhatsAppIdempotencyService idempotencyService;

    @Autowired
    private WhatsAppNotificationService notificationService;

    @Autowired
    private WhatsAppContactRepository contactRepository;

    @Autowired
    private WhatsAppMessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private WhatsAppApiClient whatsAppApiClient;

    private String configuredSecret;
    private static final String CHALLENGER_PHONE = "+919876543000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(whatsAppApiClient);
        configuredSecret = (String) org.springframework.test.util.ReflectionTestUtils.getField(signatureValidator, "appSecret");
        if (configuredSecret == null || configuredSecret.isBlank()) {
            configuredSecret = "sareekart-meta-secret-2026";
            org.springframework.test.util.ReflectionTestUtils.setField(signatureValidator, "appSecret", configuredSecret);
        }
    }

    // =========================================================================
    // 1. HMAC-SHA256 TAMPERING & SECURITY ADVERSARIAL CHALLENGES
    // =========================================================================

    @Test
    @DisplayName("ADV-HMAC-1: Tampering a single byte of payload causes signature rejection (401)")
    void testTamperingSinglePayloadByteRejected() throws Exception {
        String originalJson = buildTextWebhookPayload(CHALLENGER_PHONE, "wam-adv-tamper-1", "Valid message content");
        byte[] originalBytes = originalJson.getBytes(StandardCharsets.UTF_8);
        String validSignature = "sha256=" + computeHmac(originalBytes, configuredSecret);

        // Sanity check: valid payload and signature succeeds
        assertTrue(signatureValidator.isValid(originalBytes, validSignature),
                "Baseline valid signature must pass");

        // Attack scenario 1: Mutate 1 character in the body
        String tamperedJson = originalJson.replace("Valid message content", "Tampered msg content");
        byte[] tamperedBytes = tamperedJson.getBytes(StandardCharsets.UTF_8);

        assertFalse(signatureValidator.isValid(tamperedBytes, validSignature),
                "Validator must reject payload with mutated content bytes");

        ResponseEntity<Void> response = webhookController.receiveWebhook(validSignature, tamperedBytes);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Webhook endpoint must return 401 UNAUTHORIZED on tampered payload bytes");
    }

    @Test
    @DisplayName("ADV-HMAC-2: Appending whitespace/newline to payload invalidates signature (401)")
    void testAppendedWhitespacePayloadRejected() throws Exception {
        String originalJson = buildTextWebhookPayload(CHALLENGER_PHONE, "wam-adv-tamper-2", "Namaste");
        byte[] originalBytes = originalJson.getBytes(StandardCharsets.UTF_8);
        String validSignature = "sha256=" + computeHmac(originalBytes, configuredSecret);

        // Attack scenario: Byte appending (trailing newline or space)
        byte[] appendedBytes = (originalJson + "\n").getBytes(StandardCharsets.UTF_8);

        assertFalse(signatureValidator.isValid(appendedBytes, validSignature),
                "Validator must reject payload with appended newline");

        ResponseEntity<Void> response = webhookController.receiveWebhook(validSignature, appendedBytes);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Webhook endpoint must return 401 UNAUTHORIZED on byte-appended payload");
    }

    @Test
    @DisplayName("ADV-HMAC-3: Missing, null, empty, or blank signature headers are all rejected with 401")
    void testMissingAndBlankHeadersRejected() {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

        String[] invalidHeaders = {
                null,
                "",
                "    ",
                "\t",
                "\n"
        };

        for (String header : invalidHeaders) {
            assertFalse(signatureValidator.isValid(payload, header),
                    "Header [" + header + "] must be rejected by validator");
            ResponseEntity<Void> response = webhookController.receiveWebhook(header, payload);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                    "Header [" + header + "] must result in 401 UNAUTHORIZED");
        }
    }

    @Test
    @DisplayName("ADV-HMAC-4: Malformed prefixes and malformed hex signatures are rejected with 401")
    void testMalformedPrefixesAndHexRejected() throws Exception {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String validHash = computeHmac(payload, configuredSecret);

        String[] malformedSignatures = {
                "sha256",                                           // Missing '='
                "sha256:",                                          // Colon instead of '='
                "sha1=" + validHash,                                // Wrong algorithm prefix
                "md5=" + validHash,                                 // Wrong algorithm prefix
                "bearer " + validHash,                              // Bearer token style
                "sha256=",                                          // Prefix with empty hash
                "sha256=   ",                                       // Prefix with whitespace
                "sha256=abcdefg!@#$%^&*()_+",                       // Non-hex characters
                "sha256=123",                                       // Too short / odd length
                "sha256=" + validHash.substring(0, 32),             // Truncated (32 chars instead of 64)
                "sha256=" + validHash.substring(0, 63),             // Truncated 1 char (63 chars)
                "sha256=" + validHash + "aa"                        // Overlong (66 chars)
        };

        for (String sig : malformedSignatures) {
            assertFalse(signatureValidator.isValid(payload, sig),
                    "Malformed signature [" + sig + "] must be rejected");
            ResponseEntity<Void> response = webhookController.receiveWebhook(sig, payload);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                    "Malformed signature [" + sig + "] must result in 401 UNAUTHORIZED");
        }
    }

    @Test
    @DisplayName("ADV-HMAC-5: Signature generated with attacker secret is rejected (401)")
    void testWrongSecretSignatureRejected() throws Exception {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String attackerSecret = "malicious-attacker-forged-secret-2026";
        String attackerSignature = "sha256=" + computeHmac(payload, attackerSecret);

        assertFalse(signatureValidator.isValid(payload, attackerSignature),
                "Signature from wrong secret must be rejected");
        ResponseEntity<Void> response = webhookController.receiveWebhook(attackerSignature, payload);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("ADV-HMAC-6: Unsigned empty payload probe is rejected (401), signed empty returns 200")
    void testUnsignedEmptyPayloadProbeRejected() throws Exception {
        // Probe attack: send empty body with null signature
        ResponseEntity<Void> unsignedResponse = webhookController.receiveWebhook(null, new byte[0]);
        assertEquals(HttpStatus.UNAUTHORIZED, unsignedResponse.getStatusCode(),
                "Unsigned empty body must be rejected with 401 UNAUTHORIZED");

        // Legitimate signed empty body (Meta heartbeat probe)
        byte[] emptyBytes = new byte[0];
        String validEmptySignature = "sha256=" + computeHmac(emptyBytes, configuredSecret);
        ResponseEntity<Void> signedResponse = webhookController.receiveWebhook(validEmptySignature, emptyBytes);
        assertEquals(HttpStatus.OK, signedResponse.getStatusCode(),
                "Signed empty body must return 200 OK");
    }

    // =========================================================================
    // 2. IDEMPOTENCY REPLAY ATTACKS & CONCURRENCY STRESS-TESTING
    // =========================================================================

    @Test
    @DisplayName("ADV-IDEM-1: High-concurrency replay attack (50 threads) permits exactly 1 delivery")
    void testHighConcurrencyReplayAttack50Threads() throws Exception {
        String attackWamId = "wam-concurrency-attack-" + UUID.randomUUID();
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Simultaneous release
                    boolean acquired = idempotencyService.tryAcquireLock(attackWamId);
                    if (acquired) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // FIRE ALL 50 THREADS CONCURRENTLY

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(),
                "Under 50 concurrent threads, EXACTLY ONE thread must acquire the idempotency lock");
        assertEquals(threadCount - 1, rejectedCount.get(),
                "Under 50 concurrent threads, exactly 49 threads must be rejected");
        assertTrue(idempotencyService.isProcessed(attackWamId),
                "isProcessed must report true after lock acquisition");
    }

    @Test
    @DisplayName("ADV-IDEM-2: Sequential duplicate wam_id delivery is dropped in webhook ingestion")
    void testSequentialDuplicateWamIdDropped() throws Exception {
        String testWamId = "wam-seq-dup-" + UUID.randomUUID();
        String json = buildTextWebhookPayload(CHALLENGER_PHONE, testWamId, "Check sarees");
        WhatsAppWebhookDto dto = parseWebhook(json);

        // First delivery: processes normally
        webhookService.processWebhook(dto);
        WhatsAppMessage firstSaved = messageRepository.findByWamId(testWamId);
        assertNotNull(firstSaved, "First delivery must be persisted");
        long countAfterFirst = messageRepository.count();

        // Second delivery of identical wam_id
        webhookService.processWebhook(dto);
        long countAfterSecond = messageRepository.count();
        assertEquals(countAfterFirst, countAfterSecond,
                "Second delivery of identical wam_id must NOT insert duplicate message");

        // Third delivery of identical wam_id
        webhookService.processWebhook(dto);
        assertEquals(countAfterFirst, messageRepository.count(),
                "Third delivery must also be dropped idempotently");
    }

    @Test
    @DisplayName("ADV-IDEM-3: Database-backed persistence fallback rejects replay even if cache is cleared")
    void testDatabaseBackedReplayProtection() {
        String persistedWamId = "wam-db-persisted-" + UUID.randomUUID();

        // Simulate a message already persisted in DB (e.g. from previous server instance or after TTL eviction)
        WhatsAppContact contact = contactRepository.findByPhoneNumber(normalizePhone(CHALLENGER_PHONE))
                .orElseGet(() -> contactRepository.save(WhatsAppContact.builder()
                        .phoneNumber(normalizePhone(CHALLENGER_PHONE))
                        .name("Challenger User")
                        .optedIn(true)
                        .build()));

        Conversation conv = conversationRepository.save(Conversation.builder()
                .contact(contact)
                .status(ConversationStatus.BOT_HANDLING)
                .build());

        messageRepository.save(WhatsAppMessage.builder()
                .conversation(conv)
                .wamId(persistedWamId)
                .senderType(SenderType.CUSTOMER)
                .messageType(MessageType.TEXT)
                .content("Existing persisted message")
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .timestamp(LocalDateTime.now())
                .build());

        // Lock is NOT in in-memory activeLockMap
        idempotencyService.releaseLock(persistedWamId);

        // Attempting to acquire lock must be REJECTED by DB check (line 46 of WhatsAppIdempotencyServiceImpl)
        assertFalse(idempotencyService.tryAcquireLock(persistedWamId),
                "Database existence must reject lock acquisition even when in-memory cache is empty");
        assertTrue(idempotencyService.isProcessed(persistedWamId),
                "isProcessed must return true for DB-persisted message");
    }

    @Test
    @DisplayName("ADV-IDEM-4: Null or blank wam_id behaves safely without NPE")
    void testNullOrBlankWamIdSafety() {
        assertTrue(idempotencyService.tryAcquireLock(null), "Null wam_id should return true without throwing NPE");
        assertTrue(idempotencyService.tryAcquireLock(""), "Empty wam_id should return true without throwing NPE");
        assertTrue(idempotencyService.tryAcquireLock("   "), "Blank wam_id should return true without throwing NPE");

        assertFalse(idempotencyService.isProcessed(null), "isProcessed(null) must return false");
        assertFalse(idempotencyService.isProcessed(""), "isProcessed('') must return false");
        assertFalse(idempotencyService.isProcessed("   "), "isProcessed('   ') must return false");

        assertDoesNotThrow(() -> idempotencyService.releaseLock(null));
        assertDoesNotThrow(() -> idempotencyService.releaseLock(""));
    }

    // =========================================================================
    // 3. REGULATORY KEYWORD PERMUTATIONS, WHITESPACE & RAPID TOGGLING
    // =========================================================================

    @Test
    @DisplayName("ADV-REG-1: All case-insensitive opt-out keywords flip optedIn to false")
    void testCaseInsensitiveOptOutKeywords() {
        String[] optOutVariations = {
                "stop", "STOP", "Stop", "sToP",
                "unsubscribe", "UNSUBSCRIBE", "UnSubscribe", "uNsUbScRiBe",
                "cancel", "CANCEL", "Cancel", "cAnCeL",
                "quit", "QUIT", "Quit",
                "end", "END", "End"
        };

        for (String keyword : optOutVariations) {
            String testPhone = "+9198765" + String.format("%05d", Math.abs(keyword.hashCode() % 100000));
            // First ensure contact is created and opted in
            WhatsAppContact contact = WhatsAppContact.builder()
                    .phoneNumber(normalizePhone(testPhone))
                    .name("User " + keyword)
                    .optedIn(true)
                    .build();
            contactRepository.save(contact);

            String payloadJson = buildTextWebhookPayload(testPhone, "wam-optout-" + keyword, keyword);
            webhookService.processWebhook(parseWebhook(payloadJson));

            WhatsAppContact updated = contactRepository.findByPhoneNumber(normalizePhone(testPhone)).orElseThrow();
            assertFalse(updated.isOptedIn(),
                    "Keyword [" + keyword + "] must set optedIn=false");
            assertNotNull(updated.getOptInUpdatedAt(),
                    "optInUpdatedAt must be set after [" + keyword + "]");
        }
    }

    @Test
    @DisplayName("ADV-REG-2: All case-insensitive opt-in keywords flip optedIn to true")
    void testCaseInsensitiveOptInKeywords() {
        String[] optInVariations = {
                "start", "START", "Start", "sTaRt",
                "unstop", "UNSTOP", "Unstop", "uNsToP",
                "join", "JOIN", "Join",
                "subscribe", "SUBSCRIBE", "Subscribe",
                "yes", "YES", "Yes"
        };

        for (String keyword : optInVariations) {
            String testPhone = "+9198764" + String.format("%05d", Math.abs(keyword.hashCode() % 100000));
            // Start in opted-out state
            WhatsAppContact contact = WhatsAppContact.builder()
                    .phoneNumber(normalizePhone(testPhone))
                    .name("User " + keyword)
                    .optedIn(false)
                    .build();
            contactRepository.save(contact);

            String payloadJson = buildTextWebhookPayload(testPhone, "wam-optin-" + keyword, keyword);
            webhookService.processWebhook(parseWebhook(payloadJson));

            WhatsAppContact updated = contactRepository.findByPhoneNumber(normalizePhone(testPhone)).orElseThrow();
            assertTrue(updated.isOptedIn(),
                    "Keyword [" + keyword + "] must set optedIn=true");
            assertNotNull(updated.getOptInUpdatedAt(),
                    "optInUpdatedAt must be set after [" + keyword + "]");
        }
    }

    @Test
    @DisplayName("ADV-REG-3: Regulatory keywords with leading/trailing whitespace, newlines, tabs are trimmed and honored")
    void testWhitespaceVariationsHonored() {
        String[] whitespaceKeywords = {
                "   STOP   ",
                "\nSTOP\n",
                "\t\tCANCEL\t\t",
                " \r\n UNSUBSCRIBE \r\n ",
                "   START   ",
                "\n\nUNSTOP\n\n",
                "\tJOIN\t"
        };

        for (String raw : whitespaceKeywords) {
            String testPhone = "+9198763" + String.format("%05d", Math.abs(raw.hashCode() % 100000));
            boolean isOptIn = raw.toUpperCase().contains("START") || raw.toUpperCase().contains("UNSTOP") || raw.toUpperCase().contains("JOIN");

            // Set opposing initial state
            WhatsAppContact contact = WhatsAppContact.builder()
                    .phoneNumber(normalizePhone(testPhone))
                    .name("User Whitespace")
                    .optedIn(!isOptIn)
                    .build();
            contactRepository.save(contact);

            String payloadJson = buildTextWebhookPayload(testPhone, "wam-ws-" + UUID.randomUUID(), raw);
            webhookService.processWebhook(parseWebhook(payloadJson));

            WhatsAppContact updated = contactRepository.findByPhoneNumber(normalizePhone(testPhone)).orElseThrow();
            assertEquals(isOptIn, updated.isOptedIn(),
                    "Whitespace variation [" + raw.replace("\n", "\\n").replace("\t", "\\t") + "] must be trimmed and recognized");
        }
    }

    @Test
    @DisplayName("ADV-REG-4: Rapid toggling (STOP -> START -> STOP -> START -> CANCEL -> UNSTOP) maintains strict consistency")
    void testRapidTogglingStateIntegrity() {
        String phone = "+919876299999";
        String normPhone = normalizePhone(phone);

        // Pre-create user and contact
        User user = userRepository.save(User.builder()
                .firstName("Rapid")
                .lastName("Toggle")
                .email("rapid.toggle." + UUID.randomUUID() + "@example.com")
                .mobile(normPhone)
                .password("Secret@123")
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .build());

        WhatsAppContact contact = contactRepository.findByPhoneNumber(normPhone)
                .orElseGet(() -> contactRepository.save(WhatsAppContact.builder()
                        .phoneNumber(normPhone)
                        .name("Rapid Toggle")
                        .user(user)
                        .optedIn(true)
                        .build()));
        contact.setUser(user);
        contact.setOptedIn(true);
        contactRepository.save(contact);

        String[] sequence = {"STOP", "START", "STOP", "START", "CANCEL", "UNSTOP", "UNSUBSCRIBE"};
        boolean expected = true;

        for (int i = 0; i < sequence.length; i++) {
            String keyword = sequence[i];
            if (keyword.equals("STOP") || keyword.equals("CANCEL") || keyword.equals("UNSUBSCRIBE")) {
                expected = false;
            } else if (keyword.equals("START") || keyword.equals("UNSTOP")) {
                expected = true;
            }

            String wamId = "wam-toggle-" + i + "-" + UUID.randomUUID();
            String json = buildTextWebhookPayload(phone, wamId, keyword);
            webhookService.processWebhook(parseWebhook(json));

            WhatsAppContact updated = contactRepository.findByPhoneNumber(normPhone).orElseThrow();
            assertEquals(expected, updated.isOptedIn(),
                    "Step #" + i + " (" + keyword + ") must result in optedIn=" + expected);

            // User record must stay in sync
            User refreshedUser = userRepository.findById(user.getId()).orElseThrow();
            assertEquals(expected, refreshedUser.getWhatsappOptIn(),
                    "Step #" + i + " (" + keyword + ") must sync user.whatsappOptIn=" + expected);
        }
    }

    @Test
    @DisplayName("ADV-REG-5: Conversational sentences containing keywords are NOT falsely intercepted as control commands")
    void testConversationalSentencesNotTreatedAsControl() {
        String phone = "+919876100001";
        String normPhone = normalizePhone(phone);

        WhatsAppContact contact = contactRepository.findByPhoneNumber(normPhone)
                .orElseGet(() -> contactRepository.save(WhatsAppContact.builder()
                        .phoneNumber(normPhone)
                        .name("Conversational User")
                        .optedIn(true)
                        .build()));
        contact.setOptedIn(true);
        contactRepository.save(contact);

        String[] conversationalMessages = {
                "Please do not stop my order delivery",
                "Can we start tomorrow?",
                "I want to cancel saree item 2",
                "Should I quit my shopping session?",
                "End of discussion thank you"
        };

        for (String sentence : conversationalMessages) {
            String wamId = "wam-conv-" + UUID.randomUUID();
            String json = buildTextWebhookPayload(phone, wamId, sentence);
            webhookService.processWebhook(parseWebhook(json));

            WhatsAppContact refreshed = contactRepository.findByPhoneNumber(normPhone).orElseThrow();
            assertTrue(refreshed.isOptedIn(),
                    "Conversational sentence [" + sentence + "] must NOT flip optedIn to false");
        }
    }

    @Test
    @DisplayName("ADV-REG-6: Outbound promotional notifications are immediately suppressed when opted out")
    void testImmediateSuppressionOfOutboundOnOptOut() {
        String phone = "+919876012345";
        String normPhone = normalizePhone(phone);

        User user = userRepository.save(User.builder()
                .firstName("Kavya")
                .lastName("Patel")
                .email("kavya." + UUID.randomUUID() + "@example.com")
                .mobile(normPhone)
                .password("Password@123")
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .build());

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .totalAmount(new BigDecimal("12000.00"))
                .status(OrderStatus.CONFIRMED)
                .trackingNumber("SK-TRK-7777")
                .build());

        // Customer sends STOP
        String stopJson = buildTextWebhookPayload(phone, "wam-suppress-stop-" + UUID.randomUUID(), "STOP");
        webhookService.processWebhook(parseWebhook(stopJson));

        // Immediately attempt outbound notification
        WhatsAppNotificationResponse response = notificationService.sendOrderPlacedNotification(order);
        assertNotNull(response);
        assertEquals("SUPPRESSED", response.getDeliveryStatus(),
                "Outbound message must be SUPPRESSED immediately after STOP");

        // Now customer sends START
        String startJson = buildTextWebhookPayload(phone, "wam-suppress-start-" + UUID.randomUUID(), "START");
        webhookService.processWebhook(parseWebhook(startJson));

        // Subsequent notification can now proceed
        WhatsAppNotificationResponse responseAfterResume = notificationService.sendOrderPlacedNotification(order);
        assertNotNull(responseAfterResume);
        assertNotEquals("SUPPRESSED", responseAfterResume.getDeliveryStatus(),
                "Outbound message must NOT be SUPPRESSED after START");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String computeHmac(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String buildTextWebhookPayload(String from, String wamId, String body) {
        String escapedBody = body.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "{"
                + "\"object\":\"whatsapp_business_account\","
                + "\"entry\":[{"
                +   "\"id\":\"1234567890\","
                +   "\"changes\":[{"
                +     "\"value\":{"
                +       "\"messaging_product\":\"whatsapp\","
                +       "\"contacts\":[{\"profile\":{\"name\":\"Challenger User\"},\"wa_id\":\"" + from.replaceAll("\\+", "") + "\"}],"
                +       "\"messages\":[{"
                +         "\"from\":\"" + from + "\","
                +         "\"id\":\"" + wamId + "\","
                +         "\"timestamp\":\"" + System.currentTimeMillis() / 1000 + "\","
                +         "\"text\":{\"body\":\"" + escapedBody + "\"},"
                +         "\"type\":\"text\""
                +       "}]"
                +     "},"
                +     "\"field\":\"messages\""
                +   "}]"
                + "}]"
                + "}";
    }

    private WhatsAppWebhookDto parseWebhook(String json) {
        try {
            return objectMapper.readValue(json, WhatsAppWebhookDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test webhook payload", e);
        }
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        } else if (digits.length() == 10) {
            return digits;
        }
        return digits;
    }
}
