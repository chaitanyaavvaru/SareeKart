package com.sareekart.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.request.LoginRequest;
import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.request.VerifyRecoveryKeyRequest;
import com.sareekart.entity.Address;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductionSecurityVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User customerA;
    private User customerB;
    private User adminUser;
    private String customerAToken;
    private String customerBToken;
    private String adminToken;
    private Order customerBOrder;

    @BeforeEach
    void setUp() {
        customerA = userRepository.findByEmail("customer.sec.a@sareekart.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("SecCustomerA")
                        .lastName("Sharma")
                        .email("customer.sec.a@sareekart.com")
                        .mobile("9111111111")
                        .password(passwordEncoder.encode("PasswordA123!"))
                        .role(Role.CUSTOMER)
                        .build())
        );

        customerB = userRepository.findByEmail("customer.sec.b@sareekart.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("SecCustomerB")
                        .lastName("Verma")
                        .email("customer.sec.b@sareekart.com")
                        .mobile("9222222222")
                        .password(passwordEncoder.encode("PasswordB123!"))
                        .role(Role.CUSTOMER)
                        .build())
        );

        adminUser = userRepository.findByEmail("admin.sec@sareekart.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("SecAdmin")
                        .lastName("Staff")
                        .email("admin.sec@sareekart.com")
                        .mobile("9333333333")
                        .password(passwordEncoder.encode("AdminPass123!"))
                        .role(Role.ADMIN)
                        .build())
        );

        customerAToken = jwtTokenProvider.generateToken(customerA.getEmail());
        customerBToken = jwtTokenProvider.generateToken(customerB.getEmail());
        adminToken = jwtTokenProvider.generateToken(adminUser.getEmail());

        customerBOrder = orderRepository.findAll().stream()
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(customerB.getId()))
                .findFirst()
                .orElseGet(() -> orderRepository.save(Order.builder()
                        .user(customerB)
                        .totalAmount(new BigDecimal("18500.00"))
                        .status(OrderStatus.CONFIRMED)
                        .paymentStatus("PAID")
                        .shippingAddress(Address.builder()
                                .fullName("Customer B")
                                .phone("9222222222")
                                .streetAddress("123 Silk Street")
                                .city("Bangalore")
                                .state("Karnataka")
                                .pincode("560001")
                                .build())
                        .build())
                );
    }

    // =========================================================================
    // Category A: Authentication Security
    // =========================================================================

    @Test
    @DisplayName("A1: Successful customer authentication returns valid JWT")
    void testSuccessfulAuthentication() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("customer.sec.a@sareekart.com");
        req.setPassword("PasswordA123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.email").value("customer.sec.a@sareekart.com"));
    }

    @Test
    @DisplayName("A2: Invalid password returns generic message without disclosing account existence")
    void testInvalidPasswordReturnsGenericMessage() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("customer.sec.a@sareekart.com");
        req.setPassword("WrongPasswordXYZ");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Incorrect email or password. Please try again."));
    }

    @Test
    @DisplayName("A3: Non-existent email returns identical generic message")
    void testNonExistentEmailReturnsGenericMessage() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nonexistent.user.9999@sareekart.com");
        req.setPassword("AnyPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Incorrect email or password. Please try again."));
    }

    // =========================================================================
    // Category B & C: Authorization, RBAC & Horizontal Isolation
    // =========================================================================

    @Test
    @DisplayName("B1: Customer token cannot access Admin Wallet management (Vertical RBAC)")
    void testCustomerCannotAccessAdminWallets() throws Exception {
        mockMvc.perform(get("/api/admin/wallets")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("B2: Customer token cannot access Admin Invoices management (Vertical RBAC)")
    void testCustomerCannotAccessAdminInvoices() throws Exception {
        mockMvc.perform(get("/api/admin/invoices")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("C1: Customer A cannot download Customer B's order invoice (Horizontal Isolation)")
    void testCustomerACannotAccessCustomerBInvoice() throws Exception {
        mockMvc.perform(get("/api/orders/" + customerBOrder.getId() + "/invoice")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C2: Customer B can successfully download their own order invoice")
    void testCustomerBCanAccessOwnInvoice() throws Exception {
        mockMvc.perform(get("/api/orders/" + customerBOrder.getId() + "/invoice")
                        .header("Authorization", "Bearer " + customerBToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Category D: JWT Validation & Tampering Resistance
    // =========================================================================

    @Test
    @DisplayName("D1: Tampered JWT token signature is rejected with HTTP 401")
    void testTamperedJwtSignatureRejected() throws Exception {
        String tamperedToken = customerAToken.substring(0, customerAToken.length() - 5) + "ABCDE";

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D2: Malformed JWT token string is rejected safely without crashing")
    void testMalformedJwtRejectedSafely() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Category E: Password Reset & Emergency Recovery Key Hardening
    // =========================================================================

    @Test
    @DisplayName("E1: Invalid password reset token verification returns bad request")
    void testInvalidResetTokenVerificationFails() throws Exception {
        mockMvc.perform(get("/api/auth/verify-reset-token")
                        .param("token", "non-existent-random-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("E2: Arbitrary wildcard SK-REC- recovery key is rejected for user without recovery key")
    void testWildcardRecoveryKeyRejected() throws Exception {
        VerifyRecoveryKeyRequest req = new VerifyRecoveryKeyRequest();
        req.setEmail("customer.sec.a@sareekart.com");
        req.setRecoveryKey("SK-REC-ARBITRARY-WILDCARD-KEY");

        mockMvc.perform(post("/api/auth/verify-recovery-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // Category F & G: Payment and WhatsApp Signature Rejection
    // =========================================================================

    @Test
    @DisplayName("F1: Razorpay forged signature verification is strictly rejected")
    void testRazorpayForgedSignatureRejected() throws Exception {
        PaymentVerificationRequest req = new PaymentVerificationRequest();
        req.setRazorpayOrderId("order_test_fake_123");
        req.setRazorpayPaymentId("pay_test_fake_456");
        req.setRazorpaySignature("forged_signature_hex_12345");

        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("G1: WhatsApp webhook forged HMAC signature is rejected with HTTP 401")
    void testWhatsAppWebhookForgedSignatureRejected() throws Exception {
        mockMvc.perform(post("/api/webhook/whatsapp")
                        .header("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"whatsapp_business_account\"}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Category H: Trousseau Share Token Isolation
    // =========================================================================

    @Test
    @DisplayName("H1: Nonexistent Trousseau share token returns ResourceNotFound")
    void testNonexistentTrousseauShareTokenFails() throws Exception {
        mockMvc.perform(get("/api/trousseau/share/tkn_non_existent_fake_token_12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // Category I: Meta Catalog Privacy
    // =========================================================================

    @Test
    @DisplayName("I1: Meta catalog CSV strictly suppresses customer PII, tokens, and admin data")
    void testMetaCatalogPrivacy() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/meta/catalog.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.startsWith("id,title,description,availability,condition,price,link,image_link,brand,google_product_category,product_type,additional_image_link"));
        assertFalse(csv.contains("customer.sec"), "Must not leak customer email");
        assertFalse(csv.contains("password"), "Must not leak passwords");
        assertFalse(csv.contains("Bearer"), "Must not leak auth tokens");
        assertFalse(csv.contains("tkn_"), "Must not leak trousseau tokens");
    }

    // =========================================================================
    // Category J: Input Validation & Injection Resistance
    // =========================================================================

    @Test
    @DisplayName("J1: SQL injection string in product search is parameterized safely")
    void testSqlInjectionInProductSearchHandledSafely() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("search", "' OR '1'='1' --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // Category K: File Upload Security & Path Traversal Prevention
    // =========================================================================

    @Test
    @DisplayName("K1: Malicious shell/script upload is rejected with unsupported format")
    void testMaliciousScriptUploadRejected() throws Exception {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "exploit.sh",
                "application/x-sh",
                "#!/bin/bash\necho hack\n".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/photos/upload")
                        .file(maliciousFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Unsupported image format")));
    }

    @Test
    @DisplayName("K2: Path traversal filename payload in upload is sanitized")
    void testPathTraversalFilenameSanitized() throws Exception {
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file",
                "../../../../evil.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}
        );

        MvcResult result = mockMvc.perform(multipart("/api/admin/photos/upload")
                        .file(traversalFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(".."), "Filename must never contain directory traversal sequences");
        assertTrue(body.contains("/uploads/saree-photos/product-new-"), "Stored in safe relative upload directory");
    }

    // =========================================================================
    // Category L: Security Headers
    // =========================================================================

    @Test
    @DisplayName("L1: Security headers (HSTS, Frame-Options, Content-Type-Options, Referrer-Policy) are present")
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/products").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"));
    }

    // =========================================================================
    // Category M: Actuator Management Surface Isolation
    // =========================================================================

    @Test
    @DisplayName("M1: Sensitive actuator endpoints are forbidden to unauthenticated requests")
    void testSensitiveActuatorEndpointsRestricted() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Category N: Production Configuration Safety
    // =========================================================================

    @Test
    @DisplayName("N1: Verify production profile strictly enforces ddl-auto=validate and clean-disabled=true")
    void testProductionConfigurationSafety() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
            @SuppressWarnings("unchecked")
            Map<String, Object> hibernate = (Map<String, Object>) jpa.get("hibernate");
            assertThat(String.valueOf(hibernate.get("ddl-auto"))).matches("validate|\\$\\{SPRING_JPA_HIBERNATE_DDL_AUTO:validate\\}");

            @SuppressWarnings("unchecked")
            Map<String, Object> flyway = (Map<String, Object>) spring.get("flyway");
            assertThat(flyway.get("clean-disabled")).isEqualTo(true);

            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) data.get("app");
            assertThat(app).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> auth = (Map<String, Object>) app.get("auth");
            assertThat(auth).isNotNull();
            assertThat(String.valueOf(auth.get("expose-reset-token-in-response"))).matches("false|\\$\\{EXPOSE_RESET_TOKEN_IN_RESPONSE:false\\}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate application-prod.yaml", e);
        }
    }
}
