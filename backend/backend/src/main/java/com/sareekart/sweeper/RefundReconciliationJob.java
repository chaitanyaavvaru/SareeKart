package com.sareekart.sweeper;

import com.sareekart.config.RefundReconciliationProperties;
import com.sareekart.service.RefundReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refund-reconciliation scheduler. Fixed-delay so a long pass never overlaps
 * itself; disabled via APP_REFUND_RECONCILIATION_ENABLED=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.refund-reconciliation", name = "enabled", havingValue = "true")
public class RefundReconciliationJob {

    private final RefundReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${app.refund-reconciliation.interval-ms:120000}",
               initialDelayString = "${app.refund-reconciliation.initial-delay-ms:30000}")
    public void reconcile() {
        try {
            reconciliationService.runOnce();
        } catch (Exception e) {
            log.error("Refund reconciliation pass aborted: {}", e.getMessage());
        }
    }
}