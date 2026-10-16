package com.sareekart.security;

import com.sareekart.entity.AuditLog;
import com.sareekart.entity.RateLimitCoordinator;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.RateLimitExceededException;
import com.sareekart.repository.AuditLogRepository;
import com.sareekart.repository.RateLimitCoordinatorRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Distributed, concurrency-safe rate limiter for emergency recovery key operations.
 *
 * <p>Coordinates state across multiple application instances using transactional row-level
 * locks ({@code SELECT ... FOR UPDATE}) on dedicated {@link RateLimitCoordinator} records
 * in the database. Rate limit keys are pseudonymous SHA-256 hashes preserving privacy for
 * both source IP and target account dimensions.
 *
 * <p><strong>Policy:</strong> Only failed recovery verification attempts consume the 5-attempt/15-minute
 * budget. When an invalid key is provided, the failure count is atomically incremented under
 * the database row lock. If 5 failures have already accumulated within 15 minutes, subsequent
 * requests (attempt 6 onwards) are immediately rejected with HTTP 429 before key verification
 * executes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryRateLimiterService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long WINDOW_MINUTES = 15L;
    public static final long RETRY_AFTER_SECONDS = 900L;
    public static final String FAILED_ACTION = "RECOVERY_VERIFICATION_FAILED";

    private final RateLimitCoordinatorRepository coordinatorRepository;
    private final AuditLogRepository auditLogRepository;
    private final PlatformTransactionManager transactionManager;

    @FunctionalInterface
    public interface RateLimitedAction<T> {
        T execute() throws BadRequestException;
    }

    /**
     * Atomically coordinates rate limit checking, key verification execution, and failed attempt
     * recording across concurrent requests sharing either the same client IP or target account.
     *
     * <p>Acquires row-level pessimistic locks ({@code SELECT ... FOR UPDATE}) in deterministic
     * lexicographical order over privacy-preserving coordinate keys. Any competing request across
     * any application instance is serialized at the database engine level.
     *
     * @param clientIp client source IP
     * @param normalizedEmail user email under recovery
     * @param action the verification action to execute if permitted
     * @param <T> result type
     * @return result of the action
     * @throws RateLimitExceededException if either IP or account limit is reached (HTTP 429)
     * @throws BadRequestException if verification fails (failed attempt is recorded before throwing)
     */
    public <T> T executeWithRateLimit(String clientIp, String normalizedEmail, RateLimitedAction<T> action) {
        String keyIp = computeCoordinateKey("ip", clientIp);
        String keyAcct = computeCoordinateKey("acct", normalizedEmail);

        String firstKey = keyIp.compareTo(keyAcct) <= 0 ? keyIp : keyAcct;
        String secondKey = keyIp.compareTo(keyAcct) <= 0 ? keyAcct : keyIp;
        boolean distinct = !firstKey.equals(secondKey);

        ensureCoordinatorRowExists(firstKey);
        if (distinct) {
            ensureCoordinatorRowExists(secondKey);
        }

        ExecutionResult<T> result;
        if (transactionManager != null) {
            TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
            tmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            result = tmpl.execute(status -> executeInLockedTransaction(firstKey, secondKey, distinct, clientIp, normalizedEmail, action));
        } else {
            synchronized (this) {
                result = executeInLockedTransaction(firstKey, secondKey, distinct, clientIp, normalizedEmail, action);
            }
        }

        if (result != null && result.exception != null) {
            throw result.exception;
        }
        return result != null ? result.value : null;
    }

    private static class ExecutionResult<T> {
        private final T value;
        private final BadRequestException exception;

        private ExecutionResult(T value, BadRequestException exception) {
            this.value = value;
            this.exception = exception;
        }

        public static <T> ExecutionResult<T> success(T value) {
            return new ExecutionResult<>(value, null);
        }

        public static <T> ExecutionResult<T> failure(BadRequestException exception) {
            return new ExecutionResult<>(null, exception);
        }
    }

    private <T> ExecutionResult<T> executeInLockedTransaction(String firstKey, String secondKey, boolean distinct,
                                                              String clientIp, String normalizedEmail,
                                                              RateLimitedAction<T> action) {
        LocalDateTime now = LocalDateTime.now();

        RateLimitCoordinator coord1 = coordinatorRepository.findByCoordinateKeyForUpdate(firstKey)
                .orElseGet(() -> createCoordinatorRow(firstKey));
        RateLimitCoordinator coord2 = distinct
                ? coordinatorRepository.findByCoordinateKeyForUpdate(secondKey).orElseGet(() -> createCoordinatorRow(secondKey))
                : coord1;

        // Check sliding window expiration and thresholds on both dimensions
        checkCoordinatorLimit(coord1, now);
        if (distinct) {
            checkCoordinatorLimit(coord2, now);
        }

        // Both dimensions under limit: execute verification
        try {
            T val = action.execute();
            return ExecutionResult.success(val);
        } catch (BadRequestException ex) {
            // Verification failed: increment failure counts on both locked rows
            recordFailureOnCoordinator(coord1, now);
            if (distinct) {
                recordFailureOnCoordinator(coord2, now);
            }
            coordinatorRepository.saveAndFlush(coord1);
            if (distinct) {
                coordinatorRepository.saveAndFlush(coord2);
            }
            recordAuditLogEvent(clientIp, normalizedEmail, now);
            return ExecutionResult.failure(ex);
        }
    }

    private void checkCoordinatorLimit(RateLimitCoordinator coord, LocalDateTime now) {
        if (coord.getFirstFailureAt() != null) {
            long minutesSinceFirst = Duration.between(coord.getFirstFailureAt(), now).toMinutes();
            if (minutesSinceFirst >= WINDOW_MINUTES) {
                // Window expired: reset
                coord.setFailedAttempts(0);
                coord.setFirstFailureAt(null);
                coord.setLastFailureAt(null);
                coordinatorRepository.save(coord);
            } else if (coord.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                long elapsedSeconds = Duration.between(coord.getFirstFailureAt(), now).toSeconds();
                long retryAfter = Math.max(1L, RETRY_AFTER_SECONDS - elapsedSeconds);
                String dim = coord.getCoordinateKey().startsWith("ip:") ? "this IP address" : "this account";
                log.warn("Emergency recovery rate limit exceeded for {} across distributed cluster", dim);
                throw new RateLimitExceededException(
                        "Too many failed recovery attempts for " + dim + ". Please try again after 15 minutes.",
                        retryAfter
                );
            }
        }
    }

    private void recordFailureOnCoordinator(RateLimitCoordinator coord, LocalDateTime now) {
        if (coord.getFirstFailureAt() == null || Duration.between(coord.getFirstFailureAt(), now).toMinutes() >= WINDOW_MINUTES) {
            coord.setFirstFailureAt(now);
            coord.setFailedAttempts(1);
        } else {
            coord.setFailedAttempts(coord.getFailedAttempts() + 1);
        }
        coord.setLastFailureAt(now);
        coord.setUpdatedAt(now);
    }

    private void ensureCoordinatorRowExists(String key) {
        if (coordinatorRepository.findByCoordinateKey(key).isEmpty()) {
            if (transactionManager != null) {
                TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
                tmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    tmpl.executeWithoutResult(status -> createCoordinatorRowIfAbsent(key));
                } catch (DataIntegrityViolationException ignored) {
                    // Row created concurrently by another instance
                }
            } else {
                synchronized (this) {
                    try {
                        createCoordinatorRowIfAbsent(key);
                    } catch (DataIntegrityViolationException ignored) {
                    }
                }
            }
        }
    }

    private RateLimitCoordinator createCoordinatorRowIfAbsent(String key) {
        return coordinatorRepository.findByCoordinateKey(key)
                .orElseGet(() -> coordinatorRepository.save(RateLimitCoordinator.builder()
                        .coordinateKey(key)
                        .failedAttempts(0)
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private RateLimitCoordinator createCoordinatorRow(String key) {
        return coordinatorRepository.save(RateLimitCoordinator.builder()
                .coordinateKey(key)
                .failedAttempts(0)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void recordAuditLogEvent(String clientIp, String normalizedEmail, LocalDateTime now) {
        if (auditLogRepository == null) return;
        try {
            String cleanEmail = (normalizedEmail != null && !normalizedEmail.isBlank())
                    ? normalizedEmail.trim().toLowerCase(Locale.ROOT)
                    : "UNKNOWN";
            String safeIp = (clientIp != null && !clientIp.isBlank()) ? clientIp : "UNKNOWN";

            AuditLog logEntry = AuditLog.builder()
                    .action(FAILED_ACTION)
                    .entityType("User")
                    .entityId(cleanEmail)
                    .ipAddress(safeIp)
                    .details("Failed emergency recovery key verification attempt")
                    .createdAt(now)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to record recovery verification failure audit log: {}", ex.getMessage());
        }
    }

    /**
     * Checks if rate limits have been breached for either the client source IP or target account.
     * Throws {@link RateLimitExceededException} if either counter reaches {@link #MAX_FAILED_ATTEMPTS}.
     *
     * @param clientIp client source IP
     * @param normalizedEmail user email under recovery
     */
    @Transactional(readOnly = true)
    public void checkRateLimit(String clientIp, String normalizedEmail) {
        String keyIp = computeCoordinateKey("ip", clientIp);
        String keyAcct = computeCoordinateKey("acct", normalizedEmail);

        LocalDateTime now = LocalDateTime.now();
        coordinatorRepository.findByCoordinateKey(keyIp).ifPresent(coord -> checkCoordinatorLimit(coord, now));
        coordinatorRepository.findByCoordinateKey(keyAcct).ifPresent(coord -> checkCoordinatorLimit(coord, now));
    }

    /**
     * Durably records a failed recovery attempt within a new independent transaction.
     *
     * @param clientIp client source IP
     * @param normalizedEmail user email under recovery
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String clientIp, String normalizedEmail) {
        String keyIp = computeCoordinateKey("ip", clientIp);
        String keyAcct = computeCoordinateKey("acct", normalizedEmail);

        ensureCoordinatorRowExists(keyIp);
        if (!keyIp.equals(keyAcct)) {
            ensureCoordinatorRowExists(keyAcct);
        }

        String firstKey = keyIp.compareTo(keyAcct) <= 0 ? keyIp : keyAcct;
        String secondKey = keyIp.compareTo(keyAcct) <= 0 ? keyAcct : keyIp;
        boolean distinct = !firstKey.equals(secondKey);

        LocalDateTime now = LocalDateTime.now();
        coordinatorRepository.findByCoordinateKeyForUpdate(firstKey).ifPresent(coord1 -> {
            recordFailureOnCoordinator(coord1, now);
            coordinatorRepository.saveAndFlush(coord1);
        });

        if (distinct) {
            coordinatorRepository.findByCoordinateKeyForUpdate(secondKey).ifPresent(coord2 -> {
                recordFailureOnCoordinator(coord2, now);
                coordinatorRepository.saveAndFlush(coord2);
            });
        }

        recordAuditLogEvent(clientIp, normalizedEmail, now);
    }

    /**
     * Resolves the real client IP address from the request, honoring {@code X-Forwarded-For}
     * only when the immediate remote address is a trusted proxy or loopback.
     *
     * @param request HTTP servlet request
     * @return resolved IP address string
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] ips = xff.split(",");
                if (ips.length > 0 && !ips[0].trim().isEmpty()) {
                    return ips[0].trim();
                }
            }
        }
        return remoteAddr != null ? remoteAddr : "UNKNOWN";
    }

    /**
     * Computes a privacy-preserving pseudonymous coordinate key using one-way SHA-256.
     *
     * @param prefix dimension prefix ("ip" or "acct")
     * @param rawValue raw IP or email address
     * @return pseudonymous coordinate key
     */
    public static String computeCoordinateKey(String prefix, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return prefix + ":unknown";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return prefix + ":" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return prefix + ":" + Integer.toHexString(rawValue.hashCode());
        }
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.equals("127.0.0.1") ||
                ip.equals("::1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                isPrivateClassB(ip);
    }

    private boolean isPrivateClassB(String ip) {
        if (!ip.startsWith("172.")) return false;
        String[] parts = ip.split("\\.");
        if (parts.length < 2) return false;
        try {
            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
