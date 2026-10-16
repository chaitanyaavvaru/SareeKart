package com.sareekart.security;

import com.sareekart.entity.RateLimitCoordinator;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.RateLimitExceededException;
import com.sareekart.repository.AuditLogRepository;
import com.sareekart.repository.PasswordResetTokenRepository;
import com.sareekart.repository.RateLimitCoordinatorRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DistributedRecoveryRateLimiterIntegrationTest {

    @Autowired
    private RateLimitCoordinatorRepository coordinatorRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RecoveryRateLimiterService serviceInstance1;
    private RecoveryRateLimiterService serviceInstance2;

    @BeforeEach
    void setUp() {
        coordinatorRepository.deleteAll();
        auditLogRepository.deleteAll();

        // Construct two separate, independent service instances sharing the same database
        serviceInstance1 = new RecoveryRateLimiterService(coordinatorRepository, auditLogRepository, transactionManager);
        serviceInstance2 = new RecoveryRateLimiterService(coordinatorRepository, auditLogRepository, transactionManager);
    }

    @Test
    @DisplayName("DISTRIBUTED-01: Two separate service instances enforce combined 5-attempt limit under concurrent load")
    void testCrossInstanceConcurrentRateLimitingBurst() throws Exception {
        String clientIp = "198.51.100.200";
        String account = "distributed-target@sareekart.com";
        int totalThreads = 12;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger badRequestCount = new AtomicInteger(0);
        AtomicInteger rateLimitExceededCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            final int threadIdx = i;
            // Alternate requests between Instance 1 and Instance 2
            final RecoveryRateLimiterService assignedInstance = (threadIdx % 2 == 0) ? serviceInstance1 : serviceInstance2;

            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    assignedInstance.executeWithRateLimit(clientIp, account, () ->
                            userService.verifyRecoveryKeyAndGenerateResetToken(account, RecoveryKeyUtil.generateRecoveryKey())
                    );
                } catch (BadRequestException ex) {
                    badRequestCount.incrementAndGet();
                } catch (RateLimitExceededException ex) {
                    rateLimitExceededCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }

        // Release all threads simultaneously across both instances
        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Verify that across both instances combined, strictly 5 attempts execute and 7 are throttled
        assertEquals(5, badRequestCount.get(), "Combined service instances must permit strictly 5 attempts before blocking");
        assertEquals(7, rateLimitExceededCount.get(), "Combined service instances must reject remaining 7 requests with HTTP 429");

        // Verify rate limit coordinator row in persistent storage reflects exactly 5 failures
        String keyIp = RecoveryRateLimiterService.computeCoordinateKey("ip", clientIp);
        RateLimitCoordinator coord = coordinatorRepository.findByCoordinateKey(keyIp).orElseThrow();
        assertEquals(5, coord.getFailedAttempts(), "Coordination row must record exactly 5 failed attempts in persistent storage");
    }

    @Test
    @DisplayName("DISTRIBUTED-02: Invalid or malformed stored verifier is permanently cleared from database on failed request")
    void testInvalidStoredVerifierPermanentlyClearedFromDatabaseOnFailure() {
        String testEmail = "legacy-purge-test@sareekart.com";
        userRepository.findByEmail(testEmail).ifPresent(u -> userRepository.delete(u));

        User user = User.builder()
                .firstName("Ananya")
                .lastName("Iyer")
                .email(testEmail)
                .password(passwordEncoder.encode("TestPassword123!"))
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .recoveryKeyHash("SK-REC-LEGACY-PLAINTEXT-KEY-TO-PURGE")
                .build();
        user = userRepository.saveAndFlush(user);

        // Verify plaintext legacy key is initially persisted
        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("SK-REC-LEGACY-PLAINTEXT-KEY-TO-PURGE", savedUser.getRecoveryKeyHash());

        // Attempt verification with an invalid candidate key
        final Long userId = user.getId();
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken(testEmail, RecoveryKeyUtil.generateRecoveryKey())
        );
        assertEquals("Invalid recovery details provided.", ex.getMessage());

        // Reload user directly from database and verify stored recovery key hash is strictly nullified
        User reloadedUser = userRepository.findById(userId).orElseThrow();
        assertNull(reloadedUser.getRecoveryKeyHash(), "Invalid legacy verifier must be permanently cleared from database");

        // Verify all unrelated user fields remain completely intact
        assertEquals("Ananya", reloadedUser.getFirstName());
        assertEquals("Iyer", reloadedUser.getLastName());
        assertEquals(testEmail, reloadedUser.getEmail());
        assertEquals(Role.CUSTOMER, reloadedUser.getRole());
        assertTrue(reloadedUser.getWhatsappOptIn());
        assertTrue(passwordEncoder.matches("TestPassword123!", reloadedUser.getPassword()));
    }

    @Test
    @DisplayName("DISTRIBUTED-03: Successful recovery key provisioning and atomic single-use consumption works end-to-end")
    void testRecoveryKeyProvisioningAndSingleUseConsumption() {
        String testEmail = "provision-consume@sareekart.com";
        userRepository.findByEmail(testEmail).ifPresent(u -> userRepository.delete(u));

        String rawPassword = "StrongPassword2026!";
        User user = User.builder()
                .firstName("Pooja")
                .lastName("Hegde")
                .email(testEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.CUSTOMER)
                .build();
        user = userRepository.saveAndFlush(user);

        // Generate and provision new recovery key
        String generatedRawKey = RecoveryKeyUtil.generateRecoveryKey();
        user.setRecoveryKeyHash(passwordEncoder.encode(RecoveryKeyUtil.canonicalize(generatedRawKey)));
        userRepository.saveAndFlush(user);

        // First use: must succeed and return password reset token
        String token = userService.verifyRecoveryKeyAndGenerateResetToken(testEmail, generatedRawKey);
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Reload user: verify recovery key hash was atomically consumed (set to null)
        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        assertNull(reloadedUser.getRecoveryKeyHash(), "Recovery key verifier must be atomically consumed upon first use");

        // Second use (replay attempt): must fail closed with BadRequestException
        assertThrows(BadRequestException.class, () ->
                userService.verifyRecoveryKeyAndGenerateResetToken(testEmail, generatedRawKey)
        );
    }
}
