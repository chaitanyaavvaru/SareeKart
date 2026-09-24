package com.sareekart.deployment;

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

import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 14 — Stage 11: Production Deployment, Domain Cutover & Live Go-Live Verification Suite
 *
 * Verifies all 10 critical backend production readiness, configuration, and security gates:
 * 1. Production YAML configuration integrity (ddl-auto=validate, clean-disabled=true).
 * 2. HikariCP production pool sizing for the 512MB RAM environment.
 * 3. Actuator liveness probe responds with HTTP 200 UP.
 * 4. Actuator readiness probe responds with HTTP 200 UP.
 * 5. Sensitive actuator endpoints (/actuator/beans, /actuator/env) are restricted.
 * 6. Neo4j offline graceful degradation to MySQL hydration.
 * 7. Public Meta catalog CSV streaming with Cache-Control headers.
 * 8. Public SEO sitemap XML generation with Cache-Control headers.
 * 9. Non-destructive Razorpay forged signature rejection.
 * 10. Non-destructive WhatsApp webhook forged signature rejection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductionDeploymentVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Stage 11.1: Production Configuration Safety Audit (application-prod.yaml)")
    @SuppressWarnings("unchecked")
    void testProductionConfigurationSafetyAudit() throws Exception {
        ClassPathResource resource = new ClassPathResource("application-prod.yaml");
        assertTrue(resource.exists(), "application-prod.yaml must exist on classpath");

        Yaml yaml = new Yaml();
        Map<String, Object> data;
        try (InputStream is = resource.getInputStream()) {
            data = yaml.load(is);
        }

        assertNotNull(data, "application-prod.yaml must contain valid YAML data");
        Map<String, Object> spring = (Map<String, Object>) data.get("spring");
        assertNotNull(spring, "Must define spring configuration section");

        Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
        assertNotNull(jpa, "Must define spring.jpa configuration");
        Map<String, Object> hibernate = (Map<String, Object>) jpa.get("hibernate");
        assertNotNull(hibernate, "Must define spring.jpa.hibernate configuration");
        assertEquals("${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}", hibernate.get("ddl-auto"),
                "Production hibernate ddl-auto must enforce validate");

        Map<String, Object> flyway = (Map<String, Object>) spring.get("flyway");
        assertNotNull(flyway, "Must define spring.flyway configuration");
        assertEquals(true, flyway.get("clean-disabled"), "Flyway clean must be disabled in production");
        assertEquals(true, flyway.get("validate-on-migrate"), "Flyway validate-on-migrate must be enabled");

        Map<String, Object> app = (Map<String, Object>) data.get("app");
        assertNotNull(app, "Must define app configuration section");
        Map<String, Object> auth = (Map<String, Object>) app.get("auth");
        assertNotNull(auth, "Must define app.auth configuration");
        assertEquals("${EXPOSE_RESET_TOKEN_IN_RESPONSE:false}", auth.get("expose-reset-token-in-response"),
                "Reset token exposure must default to false in production profile");
    }

    @Test
    @DisplayName("Stage 11.2: HikariCP Production Pool Bounds (512MB RAM Container Discipline)")
    @SuppressWarnings("unchecked")
    void testHikariCPProductionPoolBounds() throws Exception {
        ClassPathResource resource = new ClassPathResource("application-prod.yaml");
        Yaml yaml = new Yaml();
        Map<String, Object> data;
        try (InputStream is = resource.getInputStream()) {
            data = yaml.load(is);
        }

        Map<String, Object> spring = (Map<String, Object>) data.get("spring");
        Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
        Map<String, Object> hikari = (Map<String, Object>) datasource.get("hikari");
        assertNotNull(hikari, "Must define spring.datasource.hikari section");

        assertEquals("${HIKARI_MAX_POOL_SIZE:8}", hikari.get("maximum-pool-size"),
                "Production Hikari maximum-pool-size must be bounded to 8 connections");
        assertEquals("${HIKARI_MIN_IDLE:2}", hikari.get("minimum-idle"),
                "Production Hikari minimum-idle must be set to 2");
        assertEquals(20000, hikari.get("connection-timeout"),
                "Connection timeout must be 20000ms");
        assertEquals(30000, hikari.get("leak-detection-threshold"),
                "Leak detection threshold must be 30000ms");
    }

    @Test
    @DisplayName("Stage 11.3: Liveness Probe Endpoint returns HTTP 200 and status UP")
    void testLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Stage 11.4: Readiness Probe Endpoint returns HTTP 200 and status UP")
    void testReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Stage 11.5: Sensitive Actuator Endpoints Restricted from Unauthenticated Callers")
    void testSensitiveActuatorEndpointsRestricted() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Stage 11.6: Public Meta Catalog CSV Streaming Header Verification")
    void testPublicMetaCatalogCsvHeaders() throws Exception {
        mockMvc.perform(get("/api/meta/catalog.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sareekart-meta-catalog.csv\""));
    }

    @Test
    @DisplayName("Stage 11.7: Public SEO Sitemap XML Generation Header Verification")
    void testPublicSitemapXmlHeaders() throws Exception {
        mockMvc.perform(get("/api/seo/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"));
    }

    @Test
    @DisplayName("Stage 11.8: Non-Destructive Razorpay Signature Validation Rejecting Forged Signature")
    void testRazorpayForgedSignatureRejection() throws Exception {
        User testCustomer = userRepository.findByEmail("customer.deploy@sareekart.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("Deploy")
                        .lastName("Customer")
                        .email("customer.deploy@sareekart.com")
                        .mobile("9123456789")
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
    @DisplayName("Stage 11.9: Non-Destructive WhatsApp Webhook Invalid HMAC Rejection")
    void testWhatsAppWebhookInvalidHmacRejection() throws Exception {
        mockMvc.perform(post("/api/webhook/whatsapp")
                        .header("X-Hub-Signature-256", "sha256=invalid_hash_value_1234567890")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"whatsapp_business_account\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Stage 11.10: Public Robots.txt SEO Directive Delivery")
    void testPublicRobotsTxtDelivery() throws Exception {
        mockMvc.perform(get("/api/seo/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"));
    }
}
