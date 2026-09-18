package com.sareekart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe rate limiter for outbound WhatsApp messages.
 * Enforces per-recipient throttling and global Meta Cloud API Tier 1 throughput limits.
 */
@Component
@Slf4j
public class WhatsAppRateLimiter {

    // Per-recipient request timestamps
    private final Map<String, List<Long>> recipientTimestamps = new ConcurrentHashMap<>();

    // Global outbound request timestamps
    private final List<Long> globalTimestamps = new LinkedList<>();

    // Per-recipient limit: max 5 messages per 10 seconds
    private static final int MAX_RECIPIENT_REQUESTS = 5;
    private static final long RECIPIENT_WINDOW_MS = 10_000L;

    // Global limit: max 80 messages per second (Meta Tier 1 limit)
    private static final int MAX_GLOBAL_REQUESTS = 80;
    private static final long GLOBAL_WINDOW_MS = 1_000L;

    /**
     * Checks if an outbound message to recipientPhone is permitted under rate limits.
     *
     * @param recipientPhone Destination phone number
     * @return true if permitted and token acquired; false if throttled
     */
    public synchronized boolean tryAcquire(String recipientPhone) {
        long now = System.currentTimeMillis();

        // 1. Global throughput check
        globalTimestamps.removeIf(t -> (now - t) > GLOBAL_WINDOW_MS);
        if (globalTimestamps.size() >= MAX_GLOBAL_REQUESTS) {
            log.warn("WhatsApp global rate limit reached ({}/sec). Throttling message.", MAX_GLOBAL_REQUESTS);
            return false;
        }

        // 2. Per-recipient check
        String key = (recipientPhone != null && !recipientPhone.isBlank()) ? recipientPhone.trim() : "GLOBAL";
        List<Long> timestamps = recipientTimestamps.computeIfAbsent(key, k -> new LinkedList<>());
        timestamps.removeIf(t -> (now - t) > RECIPIENT_WINDOW_MS);

        if (timestamps.size() >= MAX_RECIPIENT_REQUESTS) {
            log.warn("WhatsApp per-recipient rate limit reached for {} ({}/10s). Throttling.", key, MAX_RECIPIENT_REQUESTS);
            return false;
        }

        // Acquire token
        globalTimestamps.add(now);
        timestamps.add(now);
        return true;
    }

    public synchronized void reset() {
        recipientTimestamps.clear();
        globalTimestamps.clear();
    }
}
