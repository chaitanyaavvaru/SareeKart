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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Full-stack sweeper lifecycle against the REAL application + MySQL.
 * Staleness engineered via created_at rewind — no wall-clock waits.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.sweeper.enabled=true",
        "app.sweeper.stale-after-minutes=30",
        "app.sweeper.interval-ms=3600000", // scheduler silent; we call runOnce() directly
        "app.razorpay.enabled=true",
        "app.razorpay.key-id=rzp_test_sweep",
        "app.razorpay.key-secret=sweep-it-secret",
        "app.razorpay.webhook-secret=sweep-it-whsec"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderSweeperIntegrationTest {

    private static final String KEY_SECRET = "sweep-it-secret";
    private static final String WEBHOOK_SECRET = "sweep-it-whsec";
    private static final String RUN = String.valueOf(System.nanoTime() % 1_000_000_000L);

    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private RazorpayGateway razorpayGateway;

    private static String customerToken, adminToken;

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        ResponseEntity<Map> res = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", email, "password", password), json(null)), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Map<?, ?>) res.getBody().get("data")).get("token").toString();
    }

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
    private Map<String, Object> dataOf(ResponseEntity<Map> res) {
        assertThat(res.getStatusCode().value())
            .as(() -> String.valueOf(res.getBody())).isEqualTo(200);
        return (Map<String, Object>) res.getBody().get("data");
    }

    /** Product + optional coupon + RAZORPAY/COD order; optionally rewind clock. */
    private long placeOrder(String tag, boolean makeStale, String couponCode, String method) {
        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of(
                "name", "Sweep Saree " + tag,
                "categoryId", 1, "basePrice", 1000,
                "sku", "SK-SWEEP-" + tag,
                "variants", List.of(Map.of("size", "Free", "color", "Onyx", "stockQuantity", 25))
            ), adminToken), Map.class);
        long pid = ((Number) dataOf(product).get("id")).longValue();

        rest.exchange("/cart", HttpMethod.DELETE, new HttpEntity<>(json(customerToken)), Map.class);
        rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", pid, "quantity", 1), customerToken), Map.class);

        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingAddress", Map.of("fullName", "SW", "phone", "9876543210",
            "streetAddress", "1 Sweep St", "city", "Hyderabad",
            "state", "Telangana", "pincode", "500001"));
        payload.put("paymentMethod", method);
        if (couponCode != null) payload.put("couponCode", couponCode);

        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(payload, customerToken), Map.class);
        long orderId = ((Number) dataOf(order).get("id")).longValue();

        if (makeStale) {
            jdbc.update("UPDATE orders SET created_at = DATE_SUB(NOW(), INTERVAL 45 MINUTE) WHERE id = ?", orderId);
        }
        return orderId;
    }

    private void seedCoupon(String code) {
        ResponseEntity<Map> res = rest.exchange("/admin/coupons", HttpMethod.POST,
            body(Map.of("code", code, "discountType", "FIXED_AMOUNT",
                    "discountValue", new BigDecimal("10"),
                    "minimumOrderAmount", BigDecimal.ZERO),
                adminToken), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    private Map<String, Object> sweepViaApi() {
        ResponseEntity<Map> res = rest.exchange("/admin/sweeper/run", HttpMethod.POST,
            new HttpEntity<>(json(adminToken)), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return dataOf(res);
    }

    private Map<String, Object> orderRow(long orderId) {
        return jdbc.queryForMap(
            "SELECT status, payment_status, cancel_reason, cancelled_at FROM orders WHERE id = ?", orderId);
    }

    private int couponHolds(String code) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id = r.coupon_id WHERE c.code = ?",
            Integer.class, code);
        return n == null ? 0 : n;
    }

    private int paymentHistoryHits(long orderId, String marker) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE order_id = ? AND status = 'FAILED' "
                + "AND status_transition_history LIKE ?",
            Integer.class, orderId, "%" + marker + "%");
        return n == null ? 0 : n;
    }

    /** Mints gateway order for an existing SareeKart order; returns rzp order id. */
    private String mintRazorpayOrder(long orderId) {
        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id("order_swp_" + orderId).amount(inv.getArgument(1, Long.class)).build());
        ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + orderId,
            HttpMethod.POST, new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(co.getStatusCode().value()).isEqualTo(200);
        return (String) dataOf(co).get("razorpayOrderId");
    }

    // ── flow ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void setup() {
        customerToken = login("customer@sareekart.com", "customer123");
        adminToken = login("admin@sareekart.com", "admin123");
        seedCoupon("SWPGONE-" + RUN); // hold must vanish after sweep
        seedCoupon("SWPHOLD-" + RUN); // hold must survive on fresh order
    }

    @Test
    @Order(2)
    @DisplayName("stale PENDING online order → CANCELLED/FAILED, reason EXPIRED, coupon released")
    void staleOrderExpired() {
        long oid = placeOrder("STALE-" + RUN, true, "SWPGONE-" + RUN, "RAZORPAY");

        Map<String, Object> summary = sweepViaApi();
        assertThat(((Number) summary.get("expired")).intValue()).isGreaterThanOrEqualTo(1);

        Map<String, Object> row = orderRow(oid);
        assertThat(row.get("status")).isEqualTo("CANCELLED");
        assertThat(row.get("payment_status")).isEqualTo("FAILED");
        assertThat(row.get("cancel_reason")).isEqualTo("EXPIRED");
        assertThat(row.get("cancelled_at")).isNotNull();
        assertThat(paymentHistoryHits(oid, "SWEEPER:EXPIRED")).isEqualTo(1);
        assertThat(couponHolds("swpgone-" + RUN)).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("fresh PENDING orders are untouched, coupon hold preserved")
    void freshOrderUntouched() {
        long oid = placeOrder("FRESH-" + RUN, false, "SWPHOLD-" + RUN, "RAZORPAY");

        sweepViaApi();

        Map<String, Object> row = orderRow(oid);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("payment_status")).isEqualTo("PENDING");
        assertThat(couponHolds("swphold-" + RUN)).isEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("PAID orders are never swept")
    void paidOrderNeverSwept() {
        long oid = placeOrder("PAID-" + RUN, true, null, "RAZORPAY");
        String rzpOrderId = mintRazorpayOrder(oid);
        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + "pay_ok_" + oid, KEY_SECRET);
        ResponseEntity<Map> verify = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of("orderId", String.valueOf(oid),
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", "pay_ok_" + oid,
                    "razorpaySignature", sig), customerToken), Map.class);
        assertThat(verify.getStatusCode().value()).isEqualTo(200);
        int stockAfterPay = variantStock(pidOf(oid));

        sweepViaApi();

        Map<String, Object> row = orderRow(oid);
        assertThat(row.get("status")).isEqualTo("PROCESSING");
        assertThat(row.get("payment_status")).isEqualTo("PAID");
        assertThat(variantStock(pidOf(oid))).isEqualTo(stockAfterPay);
    }

    @Test
    @Order(5)
    @DisplayName("late verification of a swept order → 400, order stays CANCELLED")
    void lateVerifyRejectedAfterExpiry() {
        long oid = placeOrder("LATE-" + RUN, true, null, "RAZORPAY");
        String rzpOrderId = mintRazorpayOrder(oid);

        sweepViaApi(); // expire first

        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + "pay_late", KEY_SECRET);
        ResponseEntity<Map> verify = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of("orderId", String.valueOf(oid),
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", "pay_late",
                    "razorpaySignature", sig), customerToken), Map.class);
        assertThat(verify.getStatusCode().value()).isEqualTo(400);
        assertThat(orderRow(oid).get("status")).isEqualTo("CANCELLED");
        assertThat(orderRow(oid).get("payment_status")).isEqualTo("FAILED"); // not resurrected
    }

    @Test
    @Order(6)
    @DisplayName("captured webhook for a swept order accepted but does NOT resurrect")
    void capturedWebhookAfterExpiryIgnored() {
        long oid = placeOrder("WHLATE-" + RUN, true, null, "RAZORPAY");
        String rzpOrderId = mintRazorpayOrder(oid);

        sweepViaApi();

        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{"
            + "\"id\":\"pay_whlate\",\"order_id\":\"" + rzpOrderId + "\",\"amount\":115000}}}}";
        HttpHeaders h = json(null);
        h.set("X-Razorpay-Signature", SignatureUtil.hmacSha256Hex(payload, WEBHOOK_SECRET));
        h.set("X-Razorpay-Event-Id", "evt_whlate_" + RUN);
        ResponseEntity<Map> wh = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, h), Map.class);
        assertThat(wh.getStatusCode().value()).isEqualTo(200);
        assertThat(orderRow(oid).get("status")).isEqualTo("CANCELLED");
        assertThat(orderRow(oid).get("payment_status")).isEqualTo("FAILED");
    }

    @Test
    @Order(7)
    @DisplayName("stale COD orders are never candidates")
    void codNeverSwept() {
        long oid = placeOrder("COD-" + RUN, true, null, "COD");
        sweepViaApi();
        assertThat(orderRow(oid).get("status")).isEqualTo("PENDING");
    }

    @Test
    @Order(8)
    @DisplayName("second sweep pass is a no-op for already-expired orders")
    void doubleSweepIdempotent() {
        long oid = placeOrder("IDEM-" + RUN, true, null, "RAZORPAY");
        int expiredFirst = ((Number) sweepViaApi().get("expired")).intValue();
        int expiredSecond = ((Number) sweepViaApi().get("expired")).intValue();
        assertThat(expiredSecond <= expiredFirst).isTrue();
        assertThat(orderRow(oid).get("status")).isEqualTo("CANCELLED");
        assertThat(paymentHistoryHits(oid, "SWEEPER:EXPIRED"))
            .as("history appended exactly once").isEqualTo(1);
    }

    @Test
    @Order(9)
    @DisplayName("ops endpoint is admin-only")
    void sweeperEndpointAuthz() {
        ResponseEntity<Map> anon = rest.exchange("/admin/sweeper/run", HttpMethod.POST,
            new HttpEntity<>(json(null)), Map.class);
        assertThat(anon.getStatusCode().value()).isIn(401, 403);
        ResponseEntity<Map> cust = rest.exchange("/admin/sweeper/run", HttpMethod.POST,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(cust.getStatusCode().value()).isEqualTo(403);
        ResponseEntity<Map> admin = rest.exchange("/admin/sweeper/run", HttpMethod.POST,
            new HttpEntity<>(json(adminToken)), Map.class);
        assertThat(admin.getStatusCode().value()).isEqualTo(200);
    }

    // ── utils ────────────────────────────────────────────────────────────────

    private long pidOf(long orderId) {
        Long pid = jdbc.queryForObject(
            "SELECT product_id FROM order_items WHERE order_id = ? LIMIT 1", Long.class, orderId);
        return pid == null ? 1L : pid;
    }

    @SuppressWarnings("unchecked")
    private int variantStock(long pid) {
        ResponseEntity<Map> p = rest.getForEntity("/products/" + pid, Map.class);
        List<Map<?, ?>> variants = (List<Map<?, ?>>) ((Map<?, ?>) p.getBody().get("data")).get("variants");
        return ((Number) variants.get(0).get("stockQuantity")).intValue();
    }
}