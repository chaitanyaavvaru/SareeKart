package com.sareekart.config;

import com.sareekart.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Phase 7: Knowledge Graph Catalog Seeder & Synchronizer.
 * 
 * Runs asynchronously upon application startup to ensure schema constraints,
 * catalog taxonomy, and historical interactions are safely projected into Neo4j
 * without slowing down application boot or blocking commerce.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class Neo4jCatalogSeeder implements ApplicationRunner {

    private final GraphService graphService;

    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting Neo4j Knowledge Graph initialization in background...");
                graphService.initializeSchemaConstraints();
                graphService.seedCatalog();
                graphService.seedHistoricalInteractions();
                log.info("Neo4j Knowledge Graph initialization successfully completed.");
            } catch (Exception e) {
                log.warn("Neo4j background seeding encountered non-blocking issue: {}. Commerce unaffected.", e.getMessage());
            }
        });
    }
}
