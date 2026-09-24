package com.sareekart.observability;

import com.sareekart.security.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 14 — Stage 8: Production Observability, Reliability & Incident Readiness Test Suite
 *
 * Verifies all 12 core operational observability and resilience scenarios:
 * 1. Liveness probe (/actuator/health/liveness returns HTTP 200 UP).
 * 2. Readiness probe (/actuator/health/readiness returns HTTP 200 UP).
 * 3. Health details suppressed for unauthenticated callers (no database password or connection string leakage).
 * 4. Automatic UUID generation for inbound requests lacking X-Request-ID.
 * 5. Preservation and echo of safe inbound X-Request-ID.
 * 6. Sanitization of malformed or unsafe X-Request-ID headers.
 * 7. Correlation ID filter duration tracking and structured HTTP completion.
 * 8. Error taxonomy and requestId propagation for NOT_FOUND exceptions.
 * 9. Error taxonomy for VALIDATION_ERROR exceptions.
 * 10. Neo4j health isolation: optional Neo4j does not degrade application readiness.
 * 11. WhatsApp webhook signature enforcement rejects invalid signatures with HTTP 401.
 * 12. Zero secret or PII leakage across health and error response bodies.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductionObservabilityVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("1. Liveness Probe: /actuator/health/liveness returns HTTP 200 OK and UP status")
    void testLivenessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("2. Readiness Probe: /actuator/health/readiness returns HTTP 200 OK and UP status")
    void testReadinessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("3. Health Security: Unauthenticated health calls never leak DB passwords or stack traces")
    void testHealthDetailsSuppressedForUnauthenticated() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("password"), "Must not expose password");
        assertFalse(body.contains("jdbc:"), "Must not expose JDBC connection string");
        assertFalse(body.contains("Exception"), "Must not expose stack traces");
        assertFalse(body.contains("sareekart2026"), "Must not expose default or configured passwords");
    }

    @Test
    @DisplayName("4. Correlation ID: Generated when inbound X-Request-ID is absent")
    void testCorrelationIdGeneratedWhenMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("5. Correlation ID: Preserved and echoed when inbound X-Request-ID is valid")
    void testCorrelationIdPreservedWhenValid() throws Exception {
        String clientRequestId = "req-client-trace-1029384756";
        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId));
    }

    @Test
    @DisplayName("6. Correlation ID: Sanitized to UUID when inbound header contains illegal characters")
    void testCorrelationIdSanitizedWhenUnsafe() throws Exception {
        String unsafeHeader = "<script>alert(1)</script>; DROP TABLE users;--";
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, unsafeHeader))
                .andExpect(status().isOk())
                .andReturn();

        String echoedHeader = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(echoedHeader);
        assertNotEquals(unsafeHeader, echoedHeader, "Unsafe header must not be reflected");
        assertTrue(echoedHeader.matches("^[a-zA-Z0-9_-]{1,64}$"), "Sanitized ID must match safe regex");
    }

    @Test
    @DisplayName("7. Structured Request Tracing: Filter executes and returns response header on public API")
    void testStructuredRequestTracingOnPublicApi() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("8. Error Taxonomy & RequestId: NOT_FOUND mapped with operational error code")
    void testErrorTaxonomyAndRequestIdInNotFound() throws Exception {
        String clientRequestId = "req-not-found-trace-01";
        mockMvc.perform(get("/api/products/999999999")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId))
                .andExpect(status().isNotFound())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value(clientRequestId));
    }

    @Test
    @DisplayName("9. Error Taxonomy & RequestId: VALIDATION_ERROR mapped on bad input")
    void testErrorTaxonomyInValidation() throws Exception {
        String clientRequestId = "req-validation-trace-02";
        // Attempting to login with empty/missing credentials triggers validation error
        mockMvc.perform(post("/api/auth/login")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.requestId").value(clientRequestId));
    }

    @Test
    @DisplayName("10. Neo4j Health Isolation: neo4j health check is disabled and does not mark system DOWN")
    void testNeo4jHealthIsolation() throws Exception {
        // Even when Neo4j is not connected in test environment, readiness must be UP
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("11. WhatsApp Incident Safety: Webhook strictly rejects invalid signature with HTTP 401")
    void testWhatsAppWebhookSignatureRejection() throws Exception {
        mockMvc.perform(post("/api/webhook/whatsapp")
                        .header("X-Hub-Signature-256", "sha256=invalidforgedsignature000000000000000000000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"whatsapp_business_account\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12. Secret Redaction: Auth failure does not leak JWT secret, tokens, or customer password")
    void testAuthFailureSecretRedaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@sareekart.com\",\"password\":\"BadSecret123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("BadSecret123!"), "Password must never appear in response body");
        assertFalse(body.contains("Bearer"), "No bearer token leaked");
        assertFalse(body.contains("JWT"), "No JWT secret leaked");
    }
}
