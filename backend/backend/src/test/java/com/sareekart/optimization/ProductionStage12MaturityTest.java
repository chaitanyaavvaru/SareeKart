package com.sareekart.optimization;

import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 14 — Stage 12: 30-Day Post-Launch Runbook, Continuous Optimization & Platform Maturity Suite
 *
 * Verifies all 10 critical operational, architectural, and continuous improvement gates:
 * 1. Database index coverage on high-frequency commerce columns.
 * 2. Standalone rolling backup retention & pruning script contract.
 * 3. Automated keep-alive workflow configuration (12-minute ping cadence).
 * 4. Operational health check probe harness completeness.
 * 5. JVM memory discipline under the 512MB RAM container constraint.
 * 6. Neo4j offline graceful fallback circuit breaker.
 * 7. Public streaming catalog CSV Cache-Control headers.
 * 8. Sensitive Actuator endpoint restrictions.
 * 9. Non-destructive Razorpay forged signature rejection.
 * 10. Non-destructive WhatsApp webhook invalid HMAC rejection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductionStage12MaturityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Stage 12.1: Database Index Coverage on High-Frequency Commerce Queries")
    void testProductionDatabaseIndexCoverage() throws Exception {
        ClassPathResource v30 = new ClassPathResource("db/migration/V30__production_performance_indexes.sql");
        assertTrue(v30.exists(), "V30 performance indexes migration must exist");
        String v30Content = new String(v30.getInputStream().readAllBytes());
        assertTrue(v30Content.contains("idx_products_active_price"), "Must index active and price for fast catalog browsing");
        assertTrue(v30Content.contains("idx_products_active_category"), "Must index active and category_id for fast category filtering");
        assertTrue(v30Content.contains("idx_inventory_product"), "Must index inventory product_id");
        assertTrue(v30Content.contains("idx_reviews_product"), "Must index reviews product_id");

        ClassPathResource v31 = new ClassPathResource("db/migration/V31__add_razorpay_order_id_index.sql");
        assertTrue(v31.exists(), "V31 Razorpay index migration must exist");
        String v31Content = new String(v31.getInputStream().readAllBytes());
        assertTrue(v31Content.contains("idx_orders_razorpay_order_id"), "Must index orders razorpay_order_id for O(1) webhook fulfillment");
    }

    @Test
    @DisplayName("Stage 12.2: Standalone Rolling Backup Retention & Pruning Script Contract")
    void testBackupPruningAndRetentionPolicy() throws Exception {
        Path scriptPath = Paths.get("../../scripts/prune_backups.sh");
        assertTrue(Files.exists(scriptPath), "scripts/prune_backups.sh must exist");
        assertTrue(Files.isExecutable(scriptPath), "scripts/prune_backups.sh must be executable");

        String scriptContent = Files.readString(scriptPath);
        assertTrue(scriptContent.contains("MAX_BACKUPS"), "Script must enforce MAX_BACKUPS retention limit");
        assertTrue(scriptContent.contains(".sha256"), "Script must manage SHA-256 checksums alongside SQL dumps");
        assertTrue(scriptContent.contains("PRUNED"), "Script must report pruned backup files");
    }

    @Test
    @DisplayName("Stage 12.3: Keep-Alive Workflow Contract (12-Minute Ping Cadence)")
    void testKeepAliveWorkflowContract() throws Exception {
        Path workflowPath = Paths.get("../../.github/workflows/keepalive.yml");
        assertTrue(Files.exists(workflowPath), ".github/workflows/keepalive.yml must exist");

        String content = Files.readString(workflowPath);
        assertTrue(content.contains("cron: '*/12 * * * *'"), "Keepalive must trigger on 12-minute cron schedule");
        assertTrue(content.contains("/actuator/health"), "Keepalive must ping actuator health endpoint");
    }

    @Test
    @DisplayName("Stage 12.4: Operational Health Check Probe Harness Completeness")
    void testOperationalHealthCheckProbeHarness() throws Exception {
        Path harnessPath = Paths.get("../../scripts/operational_health_check.sh");
        assertTrue(Files.exists(harnessPath), "scripts/operational_health_check.sh must exist");
        assertTrue(Files.isExecutable(harnessPath), "scripts/operational_health_check.sh must be executable");

        String content = Files.readString(harnessPath);
        assertTrue(content.contains("/actuator/health"), "Must probe overall Actuator health");
        assertTrue(content.contains("/actuator/health/liveness"), "Must probe Liveness probe");
        assertTrue(content.contains("/actuator/health/readiness"), "Must probe Readiness probe");
        assertTrue(content.contains("/robots.txt"), "Must probe robots.txt");
        assertTrue(content.contains("/sitemap.xml"), "Must probe sitemap.xml");
        assertTrue(content.contains("/api/meta/catalog.csv"), "Must probe Meta catalog feed");
    }

    @Test
    @DisplayName("Stage 12.5: JVM Memory Discipline Under Bounded Heap (< 512 MB)")
    void testJvmMemoryDisciplineUnderBoundedHeap() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
        System.gc();

        Runtime runtime = Runtime.getRuntime();
        long totalMemoryMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemoryMb = runtime.freeMemory() / (1024 * 1024);
        long usedMemoryMb = totalMemoryMb - freeMemoryMb;
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        System.out.printf("  Stage 12 JVM Memory Status: Used=%d MB, Total=%d MB, Max=%d MB%n",
                usedMemoryMb, totalMemoryMb, maxMemoryMb);

        assertTrue(usedMemoryMb < 512, "Used heap memory exceeds 512 MB test container budget: " + usedMemoryMb + " MB");
    }

    @Test
    @DisplayName("Stage 12.6: Neo4j Offline Graceful Fallback Circuit Breaker")
    void testNeo4jOfflineGracefulFallbackCircuitBreaker() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Stage 12.7: Public Streaming Catalog CSV Cache-Control Headers")
    void testPublicStreamingCatalogCacheControlHeaders() throws Exception {
        mockMvc.perform(get("/api/meta/catalog.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"));
    }

    @Test
    @DisplayName("Stage 12.8: Sensitive Actuator Endpoints Require Authentication")
    void testSensitiveActuatorEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Stage 12.9: Non-Destructive Razorpay Signature Validation Rejecting Forged Signature")
    void testNonDestructiveRazorpayForgedSignatureRejection() throws Exception {
        User testCustomer = userRepository.findByEmail("customer.stage12@sareekart.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("Maturity")
                        .lastName("Customer")
                        .email("customer.stage12@sareekart.com")
                        .mobile("9123456780")
                        .password("hashedpassword123")
                        .role(Role.CUSTOMER)
                        .build())
        );
        String token = jwtTokenProvider.generateToken(testCustomer.getEmail());

        String forgedPayload = "{\"razorpayOrderId\":\"order_fake_123\",\"razorpayPaymentId\":\"pay_fake_456\",\"razorpaySignature\":\"forged_sig\"}";

        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Stage 12.10: Non-Destructive WhatsApp Webhook Invalid HMAC Rejection")
    void testNonDestructiveWhatsAppWebhookInvalidHmacRejection() throws Exception {
        mockMvc.perform(post("/api/webhook/whatsapp")
                        .header("X-Hub-Signature-256", "sha256=invalid_hash_value_stage12_1234567890")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"whatsapp_business_account\"}"))
                .andExpect(status().isUnauthorized());
    }
}
