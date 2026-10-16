package com.sareekart.security;

import com.sareekart.entity.AuditLog;
import com.sareekart.exception.RateLimitExceededException;
import com.sareekart.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Durable, concurrency-safe rate limiter for emergency recovery key operations.
 *
 * <p>Tracks failed verification attempts in the existing {@code audit_logs} database table,
 * enforcing a maximum of 5 failed attempts per 15-minute sliding window across both the client source IP
 * and the target account context. When limits are exceeded, throws {@link RateLimitExceededException}
 * returning HTTP 429 with {@code Retry-After: 900}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryRateLimiterService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long WINDOW_MINUTES = 15L;
    public static final long RETRY_AFTER_SECONDS = 900L;
    public static final String FAILED_ACTION = "RECOVERY_VERIFICATION_FAILED";

    private final AuditLogRepository auditLogRepository;

    /**
     * Checks if rate limits have been breached for either the client source IP or target account.
     * Throws {@link RateLimitExceededException} if either counter reaches {@link #MAX_FAILED_ATTEMPTS}.
     *
     * @param clientIp client source IP
     * @param normalizedEmail user email under recovery
     */
    @Transactional(readOnly = true)
    public void checkRateLimit(String clientIp, String normalizedEmail) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);

        if (clientIp != null && !clientIp.isBlank() && !"UNKNOWN".equalsIgnoreCase(clientIp)) {
            long ipFailures = auditLogRepository.countByActionAndIpAddressAndCreatedAtAfter(
                    FAILED_ACTION, clientIp, cutoff);
            if (ipFailures >= MAX_FAILED_ATTEMPTS) {
                log.warn("Emergency recovery rate limit exceeded for client IP address");
                throw new RateLimitExceededException(
                        "Too many failed recovery attempts from this IP address. Please try again after 15 minutes.",
                        RETRY_AFTER_SECONDS);
            }
        }

        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            String cleanEmail = normalizedEmail.trim().toLowerCase(Locale.ROOT);
            long accountFailures = auditLogRepository.countByActionAndEntityIdAndCreatedAtAfter(
                    FAILED_ACTION, cleanEmail, cutoff);
            if (accountFailures >= MAX_FAILED_ATTEMPTS) {
                log.warn("Emergency recovery rate limit exceeded for target account");
                throw new RateLimitExceededException(
                        "Too many failed recovery attempts for this account. Please try again after 15 minutes.",
                        RETRY_AFTER_SECONDS);
            }
        }
    }

    /**
     * Durably records a failed recovery attempt within a new independent transaction,
     * ensuring failure accounting persists even when the outer business transaction rolls back.
     *
     * @param clientIp client source IP
     * @param normalizedEmail user email under recovery
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String clientIp, String normalizedEmail) {
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
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to record recovery verification failure audit log: {}", ex.getMessage());
        }
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
