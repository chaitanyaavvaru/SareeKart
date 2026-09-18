package com.sareekart.service;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrousseauRateLimiter {

    private final Map<String, List<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();
    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 15;
    private static final long WINDOW_MS = 60_000L;

    public boolean tryAcquire(String ipAddress) {
        return tryAcquire(ipAddress, DEFAULT_MAX_REQUESTS_PER_MINUTE);
    }

    public synchronized boolean tryAcquire(String ipAddress, int maxRequests) {
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = "UNKNOWN";
        }
        long now = System.currentTimeMillis();
        List<Long> timestamps = ipRequestTimestamps.computeIfAbsent(ipAddress, k -> new LinkedList<>());
        timestamps.removeIf(t -> (now - t) > WINDOW_MS);

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.add(now);
        return true;
    }

    public void reset() {
        ipRequestTimestamps.clear();
    }
}
