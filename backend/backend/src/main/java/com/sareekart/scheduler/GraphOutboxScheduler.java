package com.sareekart.scheduler;

import com.sareekart.entity.GraphSyncFailure;
import com.sareekart.repository.GraphSyncFailureRepository;
import com.sareekart.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 7: Dead-Letter Queue & Catalog Reconciliation Scheduler.
 * 
 * Retries failed graph updates and periodically reconciles catalog metadata
 * between MySQL and Neo4j.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GraphOutboxScheduler {

    private final GraphService graphService;
    private final GraphSyncFailureRepository failureRepository;

    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    @Transactional
    public void retryFailedGraphSyncs() {
        if (!graphService.isAvailable()) {
            return;
        }

        List<GraphSyncFailure> pending = failureRepository.findByResolvedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Retrying {} failed graph sync operations...", pending.size());
        for (GraphSyncFailure failure : pending) {
            if (failure.getRetryCount() >= 5) {
                // Exhausted retries, keep recorded for manual inspection
                continue;
            }
            try {
                String type = failure.getEventType();
                if ("PRODUCT_SYNC".equalsIgnoreCase(type) && failure.getEventId() != null) {
                    graphService.syncProduct(failure.getEventId());
                    failure.setResolvedAt(LocalDateTime.now());
                } else if ("PRODUCT_DEACTIVATE".equalsIgnoreCase(type) && failure.getEventId() != null) {
                    graphService.deactivateProduct(failure.getEventId());
                    failure.setResolvedAt(LocalDateTime.now());
                } else {
                    // Generic resolved
                    failure.setResolvedAt(LocalDateTime.now());
                }
                failureRepository.save(failure);
            } catch (Exception e) {
                failure.setRetryCount(failure.getRetryCount() + 1);
                failure.setErrorMessage(e.getMessage());
                failureRepository.save(failure);
            }
        }
    }

    /**
     * Daily Catalog Parity Reconciliation (Runs at 02:00 UTC).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void reconcileCatalogParity() {
        if (graphService.isAvailable()) {
            log.info("Running nightly Neo4j catalog parity reconciliation...");
            graphService.seedCatalog();
        }
    }
}
