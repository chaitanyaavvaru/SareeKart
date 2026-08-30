package com.sareekart.sweeper;

import com.sareekart.service.OrderSweeperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler shell around {@link OrderSweeperService}. Fixed-delay (not cron)
 * so a long pass can never overlap itself; disabled via APP_SWEEPER_ENABLED=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sweeper", name = "enabled", havingValue = "true")
public class OrderSweeperJob {

    private final OrderSweeperService sweeperService;

    @Scheduled(fixedDelayString = "${app.sweeper.interval-ms:60000}",
               initialDelayString = "${app.sweeper.initial-delay-ms:20000}")
    public void sweep() {
        try {
            sweeperService.runOnce();
        } catch (Exception e) {
            log.error("Sweeper pass aborted: {}", e.getMessage());
        }
    }
}
