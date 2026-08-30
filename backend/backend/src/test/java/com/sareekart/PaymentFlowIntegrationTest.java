package com.sareekart;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.security.SignatureUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Full-stack payment flow against the REAL application (security chain,
 * controllers, services, Flyway-managed MySQL). Only the outbound Razorpay
 * Orders API is mocked — signature verification runs for real.
 *
 * Requires SPRING_DATASOURCE_URL/USERNAME/PASSWORD pointing at a live
 * database (CI provisions one; local dev exports them before mvnw test).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.razorpay.enabled=true",
        "app.razorpay.key-id=rzp_test_integration",
        "app.razorpay.key-secret=integration-key-secret",
        "app.razorpay.webhook-secret=integration-whsec"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentFlowIntegrationTest {

    private static final String KEY_SECRET = "integration-key-secret";
    private static final String WEBHOOK_SECRET = "integration-whsec";
    private static String rzpOrderId; // unique per run — provider_order_id is UNIQUE in DB
    /** subtotal 10000 + 5% GST (500.00) + ₹150 shipping = 10650.00 */
    private static final long TOTAL_PAISE = 1050000L; // subtotal 10000 ≥ ₹5000 → FREE shipping; +5% GST = 500.00

    @Autowired private TestRestTemplate rest;
    @MockitoBean private RazorpayGateway razorpayGateway;

    // Shared across ordered tests (single class-level context)
    private static final String RUN = String.valueOf(System.nanoTime() % 1_000_000_000L);
    private static String customerToken;
    private static Long orderId;
    private static Long productId;

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        ResponseEntity<Map> res = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", email, "password", password), json(null)), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Map<?, ?>) res.getBody().get("data")).get("token").toString();
    }

    /** JSON headers, optionally bearing a bearer token. */
    private HttpHeaders json(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) h.setBearerAuth(token);
        return h;
    }

    private HttpEntity<Map<String, Object>> body(Map<String, Object> payload, String token) {
        return new HttpEntity<>(payload, json(token));
    }

    @SuppressWarnings("unchecked")
    private <T> T dataOf(ResponseEntity<Map> res) {
        return (T) res.getBody().get("data");
    }

    @SuppressWarnings("unchecked")
    private int variantStock() {
        ResponseEntity<Map> p = rest.getForEntity("/products/" + productId, Map.class);
        Map<?, ?> data = (Map<?, ?>) p.getBody().get("data");
        List<Map<?, ?>> variants = (List<Map<?, ?>>) data.get("variants");
        return ((Number) variants.get(0).get("stockQuantity")).intValue();
    }

    // ── flow ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("setup: seeded logins, admin seeds product, customer places RAZORPAY order")
    void setup() {
        customerToken = login("customer@sareekart.com", "customer123");
        String adminToken = login("admin@sareekart.com", "admin123");

        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of(
                "name", "Integration Test Saree",
                "categoryId", 1,
                "basePrice", 10000,
                "fabric", "Silk",
                "sku", "SK-INT-PAY-" + RUN,
                "variants", List.of(Map.of("size", "Free", "color", "Teal", "stockQuantity", 5))
            ), adminToken), Map.class);
        if (product.getStatusCode().value() == 200) {
            productId = ((Number) ((Map<?, ?>) dataOf(product)).get("id")).longValue();
        } else {
            // Re-run against a dirty database: SKU is UNIQUE — recover existing
            // product and replenish its stock (earlier runs may have drained it).
            ResponseEntity<Map> search = rest.getForEntity(
                "/products/search?q=" + java.net.URLEncoder.encode("Integration Test Saree", java.nio.charset.StandardCharsets.UTF_8) + "&size=1", Map.class);
            List<?> content = (List<?>) ((Map<?, ?>) search.getBody().get("data")).get("content");
            assertThat(content).isNotEmpty();
            productId = ((Number) ((Map<?, ?>) content.get(0)).get("id")).longValue();
            rest.exchange("/products/" + productId + "/stock?stock=50",
                HttpMethod.PATCH, new HttpEntity<>(json(adminToken)), Map.class);
        }

        // Deterministic cart state regardless of leftovers from earlier runs
        rest.exchange("/cart", HttpMethod.DELETE,
            new HttpEntity<>(json(customerToken)), Map.class);
        ResponseEntity<Map> cartAdd = rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", productId, "quantity", 1), customerToken), Map.class);
        assertThat(cartAdd.getStatusCode().value()).isEqualTo(200);

        rzpOrderId = "order_test_rzp_" + System.nanoTime();

        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(Map.of(
                "shippingAddress", Map.of(
                    "fullName", "Integration Tester",
                    "phone", "9876543210",
                    "streetAddress", "1 Test Lane",
                    "city", "Hyderabad",
                    "state", "Telangana",
                    "pincode", "500001"),
                "paymentMethod", "RAZORPAY"
            ), customerToken), Map.class);
        assertThat(order.getStatusCode().value()).isEqualTo(200);
        orderId = ((Number) ((Map<?, ?>) dataOf(order)).get("id")).longValue();
    }

    /** Re-stub per test: @MockitoBean resets mocks after every method. */
    private void stubGateway() {
        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id(rzpOrderId)
                .amount(inv.getArgument(1, Long.class))
                .currency(inv.getArgument(2, String.class))
                .build());
    }

    @Test
    @Order(2)
    @DisplayName("create-order: server-authoritative amount reaches the gateway in paise")
    void createPaymentOrderUsesServerAmount() {
        stubGateway();
        ResponseEntity<Map> res = rest.exchange(
            "/payments/create-order/" + orderId, HttpMethod.POST,
            new HttpEntity<>(json(customerToken)), Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> data = dataOf(res);
        assertThat(data.get("razorpayOrderId")).isEqualTo(rzpOrderId);
        assertThat(((Number) data.get("amount")).longValue())
            .as("subtotal 10000 + 5% GST, free shipping at ≥ ₹5000 — server-computed")
            .isEqualTo(TOTAL_PAISE);
        assertThat(String.valueOf(data.get("keyId"))).doesNotContain("secret");

        verify(razorpayGateway).createOrder(
            anyString(),
            org.mockito.ArgumentMatchers.eq(TOTAL_PAISE),
            org.mockito.ArgumentMatchers.eq("INR"));
    }

    @Test
    @Order(3)
    @DisplayName("authorization: anonymous → 401/403; ADMIN token rejected on CUSTOMER payment surface")
    void authorizationBoundaries() {
        ResponseEntity<Map> anon = rest.exchange(
            "/payments/create-order/" + orderId, HttpMethod.POST,
            new HttpEntity<>(json(null)), Map.class);
        assertThat(anon.getStatusCode().value()).isIn(401, 403);

        String adminToken = login("admin@sareekart.com", "admin123");
        ResponseEntity<Map> adminCall = rest.exchange(
            "/payments/create-order/" + orderId, HttpMethod.POST,
            new HttpEntity<>(json(adminToken)), Map.class);
        assertThat(adminCall.getStatusCode().value())
            .as("admins are intentionally locked out of customer-only surfaces")
            .isEqualTo(403);
    }

    @Test
    @Order(4)
    @DisplayName("verify: real signature → PAID + PROCESSING + stock decremented once")
    void verifyValidSignature() {
        int qtyBefore = variantStock();
        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + "pay_int_001", KEY_SECRET);

        ResponseEntity<Map> res = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of(
                "orderId", String.valueOf(orderId),
                "razorpayOrderId", rzpOrderId,
                "razorpayPaymentId", "pay_int_001",
                "razorpaySignature", sig), customerToken), Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> order = dataOf(res);
        assertThat(order.get("paymentStatus")).isEqualTo("PAID");
        assertThat(order.get("status")).isEqualTo("PROCESSING");
        assertThat(variantStock()).isEqualTo(qtyBefore - 1);
    }

    @Test
    @Order(5)
    @DisplayName("duplicate verify is idempotent: state stable, stock unchanged")
    void duplicateVerifyIsIdempotent() {
        int qtyAfterFirstPay = variantStock();
        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + "pay_int_999", KEY_SECRET);

        ResponseEntity<Map> res = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of(
                "orderId", String.valueOf(orderId),
                "razorpayOrderId", rzpOrderId,
                "razorpayPaymentId", "pay_int_999",
                "razorpaySignature", sig), customerToken), Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) res.getBody().get("data")).get("paymentStatus")).isEqualTo("PAID");
        assertThat(variantStock()).isEqualTo(qtyAfterFirstPay);
    }

    @Test
    @Order(6)
    @DisplayName("unknown razorpay order id → 404 (no information leak)")
    void unknownProviderOrderRejected() {
        String sig = SignatureUtil.hmacSha256Hex("order_other|pay_other", KEY_SECRET);
        ResponseEntity<Map> res = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of(
                "orderId", String.valueOf(orderId),
                "razorpayOrderId", "order_never_created",
                "razorpayPaymentId", "pay_x",
                "razorpaySignature", sig), customerToken), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(7)
    @DisplayName("webhook: bad signature 400 · valid replay no-op · duplicate event id deduped")
    void webhookSecurityReplayAndDedup() {
        int stockBefore = variantStock();

        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{"
            + "\"id\":\"pay_wh_replay\",\"order_id\":\"" + rzpOrderId + "\",\"amount\":" + TOTAL_PAISE + "}}}}";

        // 7a. Invalid signature → 400, nothing processed
        HttpHeaders bad = json(null);
        bad.set("X-Razorpay-Signature", "deadbeef");
        ResponseEntity<Map> badRes = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, bad), Map.class);
        assertThat(badRes.getStatusCode().value()).isEqualTo(400);

        // 7b. Valid signature replaying capture of an already-PAID order → 200, no stock change
        HttpHeaders good = json(null);
        good.set("X-Razorpay-Signature", SignatureUtil.hmacSha256Hex(payload, WEBHOOK_SECRET));
        good.set("X-Razorpay-Event-Id", "evt_replay_001");
        ResponseEntity<Map> okRes = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, good), Map.class);
        assertThat(okRes.getStatusCode().value()).isEqualTo(200);

        // 7c. Same event id delivered again → deduped at the ledger, still 200
        HttpHeaders dup = json(null);
        dup.set("X-Razorpay-Signature", SignatureUtil.hmacSha256Hex(payload, WEBHOOK_SECRET));
        dup.set("X-Razorpay-Event-Id", "evt_replay_001");
        ResponseEntity<Map> dupRes = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, dup), Map.class);
        assertThat(dupRes.getStatusCode().value()).isEqualTo(200);

        assertThat(variantStock()).isEqualTo(stockBefore);
    }
}