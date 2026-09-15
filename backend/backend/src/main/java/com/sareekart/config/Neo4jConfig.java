package com.sareekart.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Phase 7: Neo4j Knowledge Graph Configuration.
 * 
 * Provides a thread-safe, connection-pooled Neo4j Driver bean.
 * Non-blocking guarantee: If Neo4j is offline or unreachable, connectivity errors
 * are trapped and logged as warnings; the application starts smoothly and
 * commerce is 100% unaffected.
 */
@Configuration
public class Neo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);

    @Value("${neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${neo4j.username:neo4j}")
    private String username;

    @Value("${neo4j.password:sareekart2026}")
    private String password;

    @Value("${neo4j.enabled:true}")
    private boolean enabled;

    private Driver driver;

    @Bean
    public Driver neo4jDriver() {
        if (!enabled) {
            log.info("Neo4j Knowledge Graph integration is disabled via configuration.");
            return null;
        }

        try {
            Config config = Config.builder()
                    .withMaxConnectionPoolSize(50)
                    .withConnectionTimeout(3000, TimeUnit.MILLISECONDS)
                    .withMaxConnectionLifetime(30, TimeUnit.MINUTES)
                    .withLogging(org.neo4j.driver.Logging.slf4j())
                    .build();

            this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);

            // Verify connectivity without failing application boot
            try {
                driver.verifyConnectivity();
                log.info("Neo4j Knowledge Graph connection established successfully at {}", uri);
            } catch (Exception e) {
                log.warn("Neo4j is currently unreachable at {}: {}. SareeKart commerce will continue with deterministic fallback.", uri, e.getMessage());
            }

            return this.driver;
        } catch (Exception e) {
            log.error("Failed to initialize Neo4j driver bean: {}. Proceeding in degraded graph mode.", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (driver != null) {
            try {
                driver.close();
                log.info("Neo4j driver successfully closed.");
            } catch (Exception e) {
                log.warn("Error closing Neo4j driver: {}", e.getMessage());
            }
        }
    }
}
