package com.sareekart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Abandoned-checkout sweeper configuration.
 *
 * Defaults are production-safe: run every minute, expire online orders that
 * have sat in PENDING/PENDING for over 30 minutes, at most 100 per pass.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.sweeper")
public class SweeperProperties {

    /** Master switch; docker compose passes APP_SWEEPER_ENABLED explicitly. */
    private boolean enabled = true;

    /** Online orders older than this (minutes) become expiry candidates. */
    private int staleAfterMinutes = 30;

    /** Max candidates expired per scheduler pass (bounds lock hold time). */
    private int batchSize = 100;

    /** Fixed delay between passes in milliseconds. */
    private long intervalMs = 60_000L;
}