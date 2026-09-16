package com.sareekart.service.impl;

import com.sareekart.repository.WhatsAppMessageRepository;
import com.sareekart.service.WhatsAppIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-speed atomic idempotency gate for WhatsApp message events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppIdempotencyServiceImpl implements WhatsAppIdempotencyService {

    private final WhatsAppMessageRepository messageRepository;

    // In-memory sliding deduplication cache with timestamps (TTL: 15 minutes)
    private final Map<String, Long> activeLockMap = new ConcurrentHashMap<>();
    private static final long LOCK_TTL_MS = 15 * 60 * 1000L; // 15 mins

    @Override
    public boolean tryAcquireLock(String wamId) {
        if (wamId == null || wamId.isBlank()) {
            return true;
        }

        long now = System.currentTimeMillis();

        // 1. Check in-memory concurrent locks
        Long existingTime = activeLockMap.putIfAbsent(wamId, now);
        if (existingTime != null) {
            log.warn("Idempotency guard: wamId '{}' is already in-flight or processed within last 15 minutes", wamId);
            return false;
        }

        // 2. Check persistence layer
        if (messageRepository.findByWamId(wamId) != null) {
            log.warn("Idempotency guard: wamId '{}' already exists in database", wamId);
            return false;
        }

        return true;
    }

    @Override
    public void releaseLock(String wamId) {
        if (wamId != null) {
            activeLockMap.remove(wamId);
        }
    }

    @Override
    public boolean isProcessed(String wamId) {
        if (wamId == null || wamId.isBlank()) {
            return false;
        }
        return activeLockMap.containsKey(wamId) || messageRepository.findByWamId(wamId) != null;
    }

    @Scheduled(fixedRate = 300_000) // Evict expired locks every 5 minutes
    public void evictExpiredLocks() {
        long cutoff = System.currentTimeMillis() - LOCK_TTL_MS;
        activeLockMap.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }
}
