package com.sareekart.scheduler;

import com.sareekart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background recovery job for abandoned PENDING checkout orders.
 * Ensures the database remains the authoritative inventory source even if
 * a client browser closes, crashes, or drops connection during checkout.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderService orderService;

    // Run sweep every 15 minutes, sweeping orders older than 30 minutes
    @Scheduled(fixedDelay = 900000, initialDelay = 60000)
    public void sweepAbandonedOrders() {
        try {
            int swept = orderService.sweepAbandonedPendingOrders(30);
            if (swept > 0) {
                log.info("OrderCleanupScheduler: successfully cleaned up {} abandoned orders and restored stock.", swept);
            }
        } catch (Exception e) {
            log.error("OrderCleanupScheduler encountered error during sweep: {}", e.getMessage(), e);
        }
    }
}
