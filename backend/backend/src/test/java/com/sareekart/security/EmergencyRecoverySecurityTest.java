package com.sareekart.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.controller.AuthController;
import com.sareekart.dto.request.GenerateRecoveryKeyRequest;
import com.sareekart.dto.request.VerifyRecoveryKeyRequest;
import com.sareekart.dto.response.RecoveryKeyResponse;
import com.sareekart.entity.AuditLog;
import com.sareekart.entity.PasswordResetToken;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.exception.RateLimitExceededException;
import com.sareekart.repository.AuditLogRepository;
import com.sareekart.repository.PasswordResetTokenRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.UserService;
import com.sareekart.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRecoverySecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PasswordEncoder realPasswordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    private UserServiceImpl userService;
    private RecoveryRateLimiterService rateLimiterService;
    private AuthController authController;
    private MockMvc mockMvc;

    private User testUser;
    private String testPlainPassword;
    private String testHashedPassword;

    @BeforeEach
    void setUp() {
        testPlainPassword = "SecurePassword2026!";
        testHashedPassword = realPasswordEncoder.encode(testPlainPassword);

        testUser = User.builder()
                .id(42L)
                .firstName("Radha")
                .lastName("Devi")
                .email("radha@sareekart.com")
                .password(testHashedPassword)
                .role(Role.CUSTOMER)
                .build();

        userService = new UserServiceImpl(
                userRepository,
                realPasswordEncoder,
                authenticationManager,
                tokenProvider,
                passwordResetTokenRepository,
                auditLogRepository
        );

        rateLimiterService = new RecoveryRateLimiterService(auditLogRepository);
        authController = new AuthController(userService, rateLimiterService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // SEC-01: Cryptographic Entropy & Secure Storage Verification
    // =========================================================================

    @Test
    @DisplayName("SEC-01.1: Generated recovery keys contain >= 128-bit entropy and adhere to canonical format")
    void testRecoveryKeyEntropyAndFormat() {
        Set<String> generatedKeys = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String key = RecoveryKeyUtil.generateRecoveryKey();
            assertNotNull(key);
            assertTrue(key.startsWith("SK-"), "Key must start with SK- prefix: " + key);
            // 8 groups of 4 hex chars = 32 hex chars (128 bits) + 7 hyphens + 3 prefix chars = 42 chars
            assertEquals(42, key.length(), "Key length must match 128-bit hex format (SK-XXXX-XXXX-...)");
            assertTrue(RecoveryKeyUtil.isValidFormat(key), "Key must pass format validation: " + key);
            generatedKeys.add(key);
        }
        // Verify randomness / high entropy across 100 generations
        assertEquals(100, generatedKeys.size(), "All 100 generated keys must be distinct");
    }

    @Test
    @DisplayName("SEC-01.2: Recovery key canonicalizer normalizes hyphens, casing, and prefixes cleanly")
    void testCanonicalization() {
        String baseHex = "A1B2C3D4E5F67890ABCDEF0123456789";
        String formatted = "SK-A1B2-C3D4-E5F6-7890-ABCD-EF01-2345-6789";
        String spaced = "  sk-a1b2-c3d4-e5f6-7890-abcd-ef01-2345-6789  ";
        String noPrefix = "A1B2-C3D4-E5F6-7890-ABCD-EF01-2345-6789";

        assertEquals(baseHex, RecoveryKeyUtil.canonicalize(formatted));
        assertEquals(baseHex, RecoveryKeyUtil.canonicalize(spaced));
        assertEquals(baseHex, RecoveryKeyUtil.canonicalize(noPrefix));
        assertEquals("", RecoveryKeyUtil.canonicalize(null));
        assertEquals("", RecoveryKeyUtil.canonicalize("   "));
    }

    @Test
    @DisplayName("SEC-01.3: User entity ignores recoveryKeyHash during JSON serialization (zero leakage)")
    void testUserJsonIgnoreRecoveryKeyHash() throws Exception {
        testUser.setRecoveryKeyHash(realPasswordEncoder.encode("CANONICALKEY12345678901234567890"));
        String json = objectMapper.writeValueAsString(testUser);

        assertFalse(json.contains("recoveryKeyHash"), "recoveryKeyHash must not be exposed in JSON: " + json);
        assertFalse(json.contains("recovery_key"), "recovery_key must not be exposed in JSON: " + json);
        assertFalse(json.contains("CANONICALKEY"), "Plaintext recovery key must never appear in JSON");
    }

    // =========================================================================
    // SEC-02: Elimination of Static Backdoors & Step-Up Provisioning Flow
    // =========================================================================

    @Test
    @DisplayName("SEC-02.1: Hardcoded static demo keys (SK-REC-CUST-2026, SK-REC-ADMN-2026) are strictly rejected")
    void testStaticDemoKeysStrictlyRejected() {
        // Attempt using Customer demo key
        BadRequestException custEx = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken("customer@sareekart.com", "SK-REC-CUST-2026")
        );
        assertEquals("Invalid recovery details provided.", custEx.getMessage());

        // Attempt using Admin demo key
        BadRequestException admnEx = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken("admin@sareekart.com", "SK-REC-ADMN-2026")
        );
        assertEquals("Invalid recovery details provided.", admnEx.getMessage());

        // Verify zero database row locking or token generation was attempted for backdoors
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("SEC-02.2: Provisioning recovery key requires valid step-up password authentication")
    void testProvisioningRequiresValidStepUpPassword() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getEmail(), null, testUser.getAuthorities())
        );
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Wrong password throws BadRequestException
        assertThrows(BadRequestException.class, () ->
                userService.generateAndProvisionRecoveryKey("WrongPassword999!")
        );

        // Blank password throws BadRequestException
        assertThrows(BadRequestException.class, () ->
                userService.generateAndProvisionRecoveryKey("")
        );

        // Correct password succeeds and provisions 128-bit key
        RecoveryKeyResponse response = userService.generateAndProvisionRecoveryKey(testPlainPassword);
        assertNotNull(response);
        assertNotNull(response.getRecoveryKey());
        assertTrue(RecoveryKeyUtil.isValidFormat(response.getRecoveryKey()));

        // Verifier stored in database must be a salted BCrypt hash, NOT plaintext
        assertNotNull(testUser.getRecoveryKeyHash());
        assertTrue(testUser.getRecoveryKeyHash().startsWith("$2a$") || testUser.getRecoveryKeyHash().startsWith("$2b$"),
                "Stored verifier must be BCrypt hash");
        assertNotEquals(response.getRecoveryKey(), testUser.getRecoveryKeyHash());

        // Verify audit log recorded
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        AuditLog savedLog = logCaptor.getValue();
        assertEquals("RECOVERY_KEY_GENERATED", savedLog.getAction());
        assertEquals("User", savedLog.getEntityType());
        assertEquals(testUser.getId().toString(), savedLog.getEntityId());
    }

    // =========================================================================
    // SEC-03: Single-Use Consumption, Rate Limiting & Concurrency Safety
    // =========================================================================

    @Test
    @DisplayName("SEC-03.1: Valid recovery key is atomically consumed upon first verification (Single-Use)")
    void testSingleUseConsumption() {
        String rawKey = RecoveryKeyUtil.generateRecoveryKey();
        String canonicalKey = RecoveryKeyUtil.canonicalize(rawKey);
        testUser.setRecoveryKeyHash(realPasswordEncoder.encode(canonicalKey));

        when(userRepository.findByEmailForUpdate(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(testUser)).thenReturn(Collections.emptyList());

        // First verification: SUCCESS
        String token = userService.verifyRecoveryKeyAndGenerateResetToken(testUser.getEmail(), rawKey);
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Assert atomic consumption: hash in user entity is now null
        assertNull(testUser.getRecoveryKeyHash(), "Recovery key verifier must be nullified immediately");
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));

        // Replay attempt with same key must fail immediately closed
        BadRequestException replayEx = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken(testUser.getEmail(), rawKey)
        );
        assertEquals("Invalid recovery details provided.", replayEx.getMessage());
    }

    @Test
    @DisplayName("SEC-03.2: Anti-enumeration: Identical generic error returned for unknown user, missing key, or bad key")
    void testAntiEnumerationGenericResponses() {
        // Unknown user
        when(userRepository.findByEmailForUpdate("ghost@sareekart.com")).thenReturn(Optional.empty());
        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken("ghost@sareekart.com", RecoveryKeyUtil.generateRecoveryKey())
        );
        assertEquals("Invalid recovery details provided.", ex1.getMessage());

        // Known user without recovery key provisioned
        testUser.setRecoveryKeyHash(null);
        when(userRepository.findByEmailForUpdate(testUser.getEmail())).thenReturn(Optional.of(testUser));
        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken(testUser.getEmail(), RecoveryKeyUtil.generateRecoveryKey())
        );
        assertEquals("Invalid recovery details provided.", ex2.getMessage());

        // Known user with provisioned key but incorrect key provided
        testUser.setRecoveryKeyHash(realPasswordEncoder.encode(RecoveryKeyUtil.canonicalize(RecoveryKeyUtil.generateRecoveryKey())));
        BadRequestException ex3 = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken(testUser.getEmail(), RecoveryKeyUtil.generateRecoveryKey())
        );
        assertEquals("Invalid recovery details provided.", ex3.getMessage());
    }

    @Test
    @DisplayName("SEC-03.3: Rate limiting blocks after 5 failed attempts per IP or account (HTTP 429)")
    void testRateLimiterThresholdEnforcement() {
        String clientIp = "198.51.100.25";
        String account = "victim@sareekart.com";

        // Under limit (4 failures): no exception
        when(auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(clientIp), any()))
                .thenReturn(4L);
        when(auditLogRepository.countByActionAndEntityIdAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(account), any()))
                .thenReturn(4L);

        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit(clientIp, account));

        // At/over limit by IP (5 failures): throws RateLimitExceededException
        when(auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(clientIp), any()))
                .thenReturn(5L);

        RateLimitExceededException ipEx = assertThrows(RateLimitExceededException.class, () ->
                rateLimiterService.checkRateLimit(clientIp, account)
        );
        assertEquals(900L, ipEx.getRetryAfterSeconds());
        assertTrue(ipEx.getMessage().contains("IP address"));

        // At/over limit by Account (5 failures): throws RateLimitExceededException
        when(auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(clientIp), any()))
                .thenReturn(0L);
        when(auditLogRepository.countByActionAndEntityIdAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(account), any()))
                .thenReturn(5L);

        RateLimitExceededException acctEx = assertThrows(RateLimitExceededException.class, () ->
                rateLimiterService.checkRateLimit(clientIp, account)
        );
        assertEquals(900L, acctEx.getRetryAfterSeconds());
        assertTrue(acctEx.getMessage().contains("account"));
    }

    @Test
    @DisplayName("SEC-03.4: Controller returns HTTP 429 and Retry-After: 900 when rate limit is exceeded")
    void testControllerRateLimitResponse() throws Exception {
        String clientIp = "203.0.113.195";
        String email = "radha@sareekart.com";

        when(auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(eq("RECOVERY_VERIFICATION_FAILED"), eq(clientIp), any()))
                .thenReturn(5L);

        VerifyRecoveryKeyRequest req = VerifyRecoveryKeyRequest.builder()
                .email(email)
                .recoveryKey(RecoveryKeyUtil.generateRecoveryKey())
                .build();

        mockMvc.perform(post("/api/auth/verify-recovery-key")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("SEC-03.5: Failed verification triggers durable audit log recording")
    void testFailedVerificationRecordsAuditLog() throws Exception {
        String clientIp = "192.0.2.77";
        String email = "target@sareekart.com";

        when(auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(auditLogRepository.countByActionAndEntityIdAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(userRepository.findByEmailForUpdate(email)).thenReturn(Optional.empty());

        VerifyRecoveryKeyRequest req = VerifyRecoveryKeyRequest.builder()
                .email(email)
                .recoveryKey(RecoveryKeyUtil.generateRecoveryKey())
                .build();

        mockMvc.perform(post("/api/auth/verify-recovery-key")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Verify failure was durably recorded
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        AuditLog failureLog = logCaptor.getValue();
        assertEquals("RECOVERY_VERIFICATION_FAILED", failureLog.getAction());
        assertEquals("target@sareekart.com", failureLog.getEntityId());
        assertEquals(clientIp, failureLog.getIpAddress());
    }

    @Test
    @DisplayName("SEC-03.6: Trusted proxy resolution honors X-Forwarded-For only for trusted upstreams")
    void testTrustedProxyResolution() {
        MockHttpServletRequest trustedRequest = new MockHttpServletRequest();
        trustedRequest.setRemoteAddr("127.0.0.1"); // trusted loopback
        trustedRequest.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");
        assertEquals("203.0.113.50", rateLimiterService.resolveClientIp(trustedRequest));

        MockHttpServletRequest untrustedRequest = new MockHttpServletRequest();
        untrustedRequest.setRemoteAddr("198.51.100.99"); // untrusted public IP spoofing XFF
        untrustedRequest.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("198.51.100.99", rateLimiterService.resolveClientIp(untrustedRequest));
    }

    @Test
    @DisplayName("SEC-03.7: Concurrency test: Exactly one thread succeeds when racing to consume recovery key")
    void testConcurrentRaceToConsumeRecoveryKey() throws Exception {
        String rawKey = RecoveryKeyUtil.generateRecoveryKey();
        String canonicalKey = RecoveryKeyUtil.canonicalize(rawKey);
        testUser.setRecoveryKeyHash(realPasswordEncoder.encode(canonicalKey));

        // Use ReentrantLock to simulate database-level SELECT ... FOR UPDATE pessimistic row locking
        java.util.concurrent.locks.ReentrantLock dbRowLock = new java.util.concurrent.locks.ReentrantLock();
        when(userRepository.findByEmailForUpdate(testUser.getEmail())).thenAnswer(invocation -> {
            dbRowLock.lock();
            return Optional.of(testUser);
        });
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(testUser)).thenReturn(Collections.emptyList());

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    userService.verifyRecoveryKeyAndGenerateResetToken(testUser.getEmail(), rawKey);
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                }
            }));
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 1 thread must have succeeded and all others must fail closed
        assertEquals(1, successCount.get(), "Exactly 1 thread must succeed in consuming the recovery key");
        assertEquals(threadCount - 1, failureCount.get(), "Competing threads must fail closed");
        assertNull(testUser.getRecoveryKeyHash(), "Key verifier must be nullified");
    }
}
