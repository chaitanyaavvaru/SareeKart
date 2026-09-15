package com.sareekart.config;

import com.sareekart.service.SareeEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Phase 8: AI Recommendation & Vector Embedding Catalog Seeder.
 * 
 * Runs asynchronously upon application startup to pre-compute and register
 * dense semantic embeddings for catalog sarees without blocking startup.
 */
@Component
@Order(110)
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationSeeder implements ApplicationRunner {

    private final SareeEmbeddingService sareeEmbeddingService;

    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Initializing Phase 8 AI recommendation dense embeddings in background...");
                sareeEmbeddingService.seedCatalogEmbeddings();
                log.info("Phase 8 AI recommendation dense embeddings initialized successfully.");
            } catch (Exception e) {
                log.warn("Non-blocking issue initializing recommendation embeddings: {}. Fallbacks operational.", e.getMessage());
            }
        });
    }
}
