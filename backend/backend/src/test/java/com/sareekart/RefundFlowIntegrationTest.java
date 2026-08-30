package com.sareekart;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.dto.payment.RazorpayRefundResponse;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Full-stack refund lifecycle over REAL HTTP + MySQL.
 * Only the outbound Razorpay gateway is mocked; signatures are real.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.razorpay.enabled=true",
        "app.razorpay.key-id=rzp_test_refund_it",
        "app.razorpay.key-secret=refund-it-secret",
        "app.razorpay.webhook-secret=refund-it-whsec"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RefundFlowIntegrationTest {

    private static final String KEY_SECRET = "refund-it-secret";
    private static final String WEBHOOK_SECRET = "refund-it-whsec";
    private static final String RUN = String.valueOf(System.nanoTime() % 1_000_000_000L);

    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private RazorpayGateway razorpayGateway;

    private static String customerToken, adminToken;

    // ── shared helpers ───────────────────────────────────────────────────────

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

    /** Paid PROCESSING order; returns {orderId, rzpOrderId} after real verify. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createPaidOrder(String tag) {
        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of(
                "name", "Refund Saree " + tag,
                "categoryId", 1, "basePrice", 1000,
                "sku", "SK-REF-" + tag,
                "variants", List.of(Map.of("size", "Free", "color", "Pearl", "stockQuantity", 10))
            ), adminToken), Map.class);
        long pid = ((Number) dataOf(product).get("id")).longValue();

        rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", pid, "quantity", 1), customerToken), Map.class);

        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(Map.of(
                "shippingAddress", Map.of("fullName", "RF", "phone", "9876543210",
                    "streetAddress", "1 Refund Rd", "city", "Hyderabad",
                    "state", "Telangana", "pincode", "500001"),
                "paymentMethod", "RAZORPAY"), customerToken), Map.class);
        long orderId = ((Number) dataOf(order).get("id")).longValue();

        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id("order_rfr_" + orderId).amount(inv.getArgument(1, Long.class)).build());
        ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + orderId,
            HttpMethod.POST, new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(co.getStatusCode().value()).isEqualTo(200);
        String rzpOrderId = (String) dataOf(co).get("razorpayOrderId");
        String rzpPaymentId = "pay_rfr_" + orderId;

        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);
        ResponseEntity<Map> verify = rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of("orderId", String.valueOf(orderId),
                    "razorpayOrderId", rzpOrderId,
                    "razorpayPaymentId", rzpPaymentId,
                    "razorpaySignature", sig), customerToken), Map.class);
        assertThat(verify.getStatusCode().value()).isEqualTo(200);

        Map<String, Object> out = new java.util.HashMap<>();
        out.put("orderId", orderId);
        out.put("productId", pid);
        out.put("rzpPaymentId", rzpPaymentId);
        out.put("rzpOrderId", rzpOrderId);
        return out;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> initiateRefund(long orderId, String amount) {
        Map<String, Object> payload = new java.util.HashMap<>();
        if (amount != null) payload.put("amount", new BigDecimal(amount));
        payload.put("reason", "IT refund");
        return rest.exchange("/admin/orders/" + orderId + "/refund", HttpMethod.POST,
            body(payload, adminToken), Map.class);
    }

    private int variantStock(long pid) {
        ResponseEntity<Map> p = rest.getForEntity("/products/" + pid, Map.class);
        List<Map<?, ?>> variants =
            (List<Map<?, ?>>) ((Map<?, ?>) p.getBody().get("data")).get("variants");
        return ((Number) variants.get(0).get("stockQuantity")).intValue();
    }

    private void sendWebhook(String payload, String eventId) {
        HttpHeaders h = json(null);
        h.set("X-Razorpay-Signature",
            SignatureUtil.hmacSha256Hex(payload, WEBHOOK_SECRET));
        h.set("X-Razorpay-Event-Id", eventId);
        ResponseEntity<Map> res = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, h), Map.class);
        assertThat(res.getStatusCode().value())
            .as(() -> String.valueOf(res.getBody())).isEqualTo(200);
    }

    // ── flow ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void setup() {
        customerToken = login("customer@sareekart.com", "customer123");
        adminToken = login("admin@sareekart.com", "admin123");
    }

    @Test
    @Order(2)
    @DisplayName("FULL refund: gateway accepted → REFUNDED, auto-restock once, coupon untouched")
    void fullRefundHappyPath() {
        Map<String, Object> ctx = createPaidOrder("FULL" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        long pid = ((Number) ctx.get("productId")).longValue();
        int stockPaid = variantStock(pid);           // 9 (committed on PAID)

        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_full_" + orderId).status("processed")
                .amount(inv.getArgument(1, Long.class)).build());

        // Captured total for ₹1000 subtotal: 1050 (free ship ≥5000? no — 1000<5000 ⇒ +150 ship, +5% tax)
        // subtotal 1000 · tax 50 · shipping 150 · total 1200.00
        ResponseEntity<Map> rf = initiateRefund(orderId, null); // FULL
        System.out.println("REFUND-DEBUG status=" + rf.getStatusCode()
            + " body=" + rf.getBody()
            + " headers=" + rf.getHeaders());
        assertThat(rf.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> refundData = dataOf(rf);
        assertThat(new BigDecimal(refundData.get("amount").toString())).isEqualByComparingTo("1200.00");
        assertThat(refundData.get("status")).isEqualTo("SUCCESS");

        ResponseEntity<Map> orderRes = rest.exchange("/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(orderRes.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> order = (Map<?, ?>) orderRes.getBody().get("data");
        assertThat(order.get("paymentStatus")).isEqualTo("REFUNDED");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("refunds");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).get("status")).isEqualTo("SUCCESS");

        assertThat(variantStock(pid)).isEqualTo(stockPaid + 1); // 10 restored exactly once
        Integer restocked = jdbc.queryForObject(
            "SELECT inventory_restocked FROM orders WHERE id = ?", Integer.class, orderId);
        assertThat(restocked).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("PARTIAL refunds: PARTIALLY_REFUNDED without restock, then full → restock once")
    void partialThenFull() {
        Map<String, Object> ctx = createPaidOrder("PART" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        long pid = ((Number) ctx.get("productId")).longValue();
        int stockPaid = variantStock(pid);
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_part_" + System.nanoTime()).status("processed")
                .amount(inv.getArgument(1, Long.class)).build());

        // captured total 1200 → partial 300
        ResponseEntity<Map> r1 = initiateRefund(orderId, "300");
        assertThat(r1.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> res_o1 = rest.exchange("/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(res_o1.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> o1 = (Map<?, ?>) res_o1.getBody().get("data");
        assertThat(o1.get("paymentStatus")).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(variantStock(pid)).isEqualTo(stockPaid); // no restock yet

        // remaining 900
        ResponseEntity<Map> r2 = initiateRefund(orderId, "900");
        assertThat(r2.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> res_o2 = rest.exchange("/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(res_o2.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> o2 = (Map<?, ?>) res_o2.getBody().get("data");
        assertThat(o2.get("paymentStatus")).isEqualTo("REFUNDED");
        assertThat(variantStock(pid)).isEqualTo(stockPaid + 1); // exactly once
    }

    @Test
    @Order(4)
    @DisplayName("over-refund rejected with remaining balance surfaced")
    void overRefundRejected() {
        Map<String, Object> ctx = createPaidOrder("OVER" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        ResponseEntity<Map> r = initiateRefund(orderId, "999999");
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(r.getBody().get("message"))).contains("refundable balance of ₹1200");
    }

    @Test
    @Order(5)
    @DisplayName("unpaid (PENDING) order cannot be refunded")
    void unpaidRejected() {
        long pidSeed = -1;
        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of("name", "Unpaid " + RUN, "categoryId", 1, "basePrice", 500,
                        "sku", "SK-UNPAID-" + RUN,
                        "variants", List.of(Map.of("size","F","color","c","stockQuantity",3))),
                  adminToken), Map.class);
        long pid = ((Number) dataOf(product).get("id")).longValue();
        rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", pid, "quantity", 1), customerToken), Map.class);
        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(Map.of("shippingAddress", Map.of("fullName","U","phone","9","streetAddress","s",
                       "city","H","state","T","pincode","500001"),
                    "paymentMethod", "RAZORPAY"), customerToken), Map.class);
        long oid = ((Number) dataOf(order).get("id")).longValue();

        ResponseEntity<Map> r = initiateRefund(oid, null);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(r.getBody().get("message"))).contains("No captured payment");
    }

    @Test
    @Order(6)
    @DisplayName("fully-refunded order rejects further refunds")
    void fullyRefundedRejectsMore() {
        Map<String, Object> ctx = createPaidOrder("AGAIN" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_again_" + orderId).status("processed").build());
        initiateRefund(orderId, null);
        ResponseEntity<Map> second = initiateRefund(orderId, "1");
        assertThat(second.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(second.getBody().get("message"))).contains("fully refunded");
    }

    @Test
    @Order(7)
    @DisplayName("duplicate admin request while PENDING is blocked")
    void duplicatePendingBlocked() throws Exception {
        Map<String, Object> ctx = createPaidOrder("DUP" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        // Gateway returns async-pending style refund
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willReturn(RazorpayRefundResponse.builder()
                .id("rf_dup_" + orderId).status("pending").build());
        assertThat(initiateRefund(orderId, null).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> again = initiateRefund(orderId, null);
        assertThat(again.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(again.getBody().get("message"))).contains("already in progress");

        // Confirm via webhook → SUCCESS
        String payload = "{\"event\":\"refund.processed\",\"payload\":{\"refund\":{\"entity\":{"
            + "\"id\":\"rf_dup_" + orderId + "\",\"payment_id\":\"" + ctx.get("rzpPaymentId")
            + "\",\"amount\":120000}},\"payment\":{\"entity\":{\"id\":\"" + ctx.get("rzpPaymentId") + "\"}}}}";
        sendWebhook(payload, "evt_dup_ok_" + orderId);
        Map<?, ?> row = jdbc.queryForMap(
            "SELECT status FROM refunds WHERE provider_refund_id = ?", "rf_dup_" + orderId);
        assertThat(row.get("status")).isEqualTo("SUCCESS");
    }

    @Test
    @Order(8)
    @DisplayName("invalid webhook signature rejected with 400")
    void invalidWebhookSignature() {
        String payload = "{\"event\":\"refund.processed\",\"payload\":{}}";
        HttpHeaders h = json(null);
        h.set("X-Razorpay-Signature", "forged");
        ResponseEntity<Map> res = rest.exchange("/payments/webhook", HttpMethod.POST,
            new HttpEntity<>(payload, h), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(9)
    @DisplayName("dashboard-initiated refund reconstructed from webhook; replay idempotent")
    void dashboardRefundReconstructionAndReplay() {
        Map<String, Object> ctx = createPaidOrder("DASH" + RUN);
        long orderId = ((Number) ctx.get("orderId")).longValue();
        long pid = ((Number) ctx.get("productId")).longValue();
        int stockBefore = variantStock(pid);

        String refundId = "rf_dash_" + orderId;
        String payload = "{\"event\":\"refund.processed\",\"payload\":{"
            + "\"refund\":{\"entity\":{\"id\":\"" + refundId + "\","
            + "\"payment_id\":\"" + ctx.get("rzpPaymentId") + "\",\"amount\":120000}},"
            + "\"payment\":{\"entity\":{\"id\":\"" + ctx.get("rzpPaymentId") + "\"}}}}";

        sendWebhook(payload, "evt_dash_1_" + orderId);
        sendWebhook(payload, "evt_dash_2_" + orderId); // duplicate delivery, different event id

        ResponseEntity<Map> orderRes = rest.exchange("/orders/" + orderId, HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(orderRes.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> order = (Map<?, ?>) orderRes.getBody().get("data");
        assertThat(order.get("paymentStatus")).isEqualTo("REFUNDED"); // full 1200 reconstructed
        List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("refunds");
        assertThat(lines).hasSize(1); // single trail despite replay
        assertThat(variantStock(pid)).isEqualTo(stockBefore + 1); // restocked once
    }

    @Test
    @Order(10)
    @DisplayName("coupon usage invariant: refunds never alter committed usage")
    void couponUsageInvariant() {
        String code = "RFKEEP-" + RUN;
        rest.exchange("/admin/coupons", HttpMethod.POST,
            body(Map.of("code", code, "discountType", "FIXED_AMOUNT",
                        "discountValue", new BigDecimal("25"),
                        "minimumOrderAmount", BigDecimal.ZERO), adminToken), Map.class);

        // paid order WITH coupon
        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of("name", "KeepC Saree " + RUN, "categoryId", 1, "basePrice", 800,
                        "sku", "SK-KEEPC-" + RUN,
                        "variants", List.of(Map.of("size","F","color","c","stockQuantity",5))),
                  adminToken), Map.class);
        long pid = ((Number) dataOf(product).get("id")).longValue();
        rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", pid, "quantity", 1), customerToken), Map.class);
        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(Map.of("shippingAddress", Map.of("fullName","K","phone","9","streetAddress","s",
                       "city","H","state","T","pincode","500001"),
                    "paymentMethod", "RAZORPAY", "couponCode", code), customerToken), Map.class);
        long oid = ((Number) dataOf(order).get("id")).longValue();
        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id("order_keep_" + oid).amount(inv.getArgument(1, Long.class)).build());
        ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + oid,
            HttpMethod.POST, new HttpEntity<>(json(customerToken)), Map.class);
        String rzpOrderId = (String) dataOf(co).get("razorpayOrderId");
        String rzpPayId = "pay_keep_" + oid;
        String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|" + rzpPayId, KEY_SECRET);
        rest.exchange("/payments/verify", HttpMethod.POST,
            body(Map.of("orderId", String.valueOf(oid), "razorpayOrderId", rzpOrderId,
                        "razorpayPaymentId", rzpPayId, "razorpaySignature", sig),
                 customerToken), Map.class);

        Long usedAfterPay = jdbc.queryForObject(
            "SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE c.code=?",
            Long.class, code);
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_keep_" + oid).status("processed").build());
        assertThat(initiateRefund(oid, null).getStatusCode().value()).isEqualTo(200);

        Long usedAfterRefund = jdbc.queryForObject(
            "SELECT COUNT(*) FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id WHERE c.code=?",
            Long.class, code);
        assertThat(usedAfterRefund).as("committed usage unchanged by refund").isEqualTo(usedAfterPay);
        assertThat(usedAfterRefund).isEqualTo(1);
    }

    @Test
    @Order(11)
    @DisplayName("late capture after sweeper expiry triggers automatic reconciliation refund once")
    void lateCaptureReconciliation() {
        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            body(Map.of("name", "LateCap " + RUN, "categoryId", 1, "basePrice", 700,
                        "sku", "SK-LATECAP-" + RUN,
                        "variants", List.of(Map.of("size","F","color","c","stockQuantity",4))),
                  adminToken), Map.class);
        long pid = ((Number) dataOf(product).get("id")).longValue();
        rest.exchange("/cart/items", HttpMethod.POST,
            body(Map.of("productId", pid, "quantity", 1), customerToken), Map.class);
        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            body(Map.of("shippingAddress", Map.of("fullName","L","phone","9","streetAddress","s",
                       "city","H","state","T","pincode","500001"),
                    "paymentMethod", "RAZORPAY"), customerToken), Map.class);
        long oid = ((Number) dataOf(order).get("id")).longValue();

        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id("order_late_" + oid).amount(inv.getArgument(1, Long.class)).build());
        ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + oid,
            HttpMethod.POST, new HttpEntity<>(json(customerToken)), Map.class);
        String rzpOrderId = (String) dataOf(co).get("razorpayOrderId");
        String rzpPaymentId = "pay_latecap_" + oid;

        // Expire via sweeper (rewind clock first)
        jdbc.update("UPDATE orders SET created_at = DATE_SUB(NOW(), INTERVAL 45 MINUTE) WHERE id=?", oid);
        ResponseEntity<Map> sweep = rest.exchange("/admin/sweeper/run", HttpMethod.POST,
            new HttpEntity<>(json(adminToken)), Map.class);
        assertThat(sweep.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(sweep).get("expired")).isNotNull();

        // Late capture arrives
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id("rf_reconcile_" + oid).status("processed").build());
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{"
            + "\"id\":\"" + rzpPaymentId + "\",\"order_id\":\"" + rzpOrderId + "\",\"amount\":88200}}}}";
        sendWebhook(payload, "evt_latecap_" + RUN);

        Map<String, Object> recon = jdbc.queryForMap(
            "SELECT status, reason_code FROM refunds WHERE provider_refund_id = ?", "rf_reconcile_" + oid);
        assertThat(recon.get("status")).isEqualTo("SUCCESS");
        assertThat(recon.get("reason_code")).isEqualTo("PAYMENT_RECONCILIATION");

        // Order remains CANCELLED — money returned, fulfillment truth preserved
        ResponseEntity<Map> res_o = rest.exchange("/orders/" + oid, HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(res_o.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> o = (Map<?, ?>) res_o.getBody().get("data");
        assertThat(o.get("status")).isEqualTo("CANCELLED");

        // Duplicate late-capture webhook → still exactly one reconciliation row
        sendWebhook(payload, "evt_latecap_dup_" + RUN);
        Integer rows = jdbc.queryForObject(
            "SELECT COUNT(*) FROM refunds WHERE provider_refund_id = ?", Integer.class, "rf_reconcile_" + oid);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @Order(12)
    @DisplayName("authorization: customer and anonymous blocked from refund initiation/listing")
    void authorizationBoundaries() {
        ResponseEntity<Map> anon = rest.exchange("/admin/refunds", HttpMethod.GET,
            new HttpEntity<>(json(null)), Map.class);
        assertThat(anon.getStatusCode().value()).isIn(401, 403);
        ResponseEntity<Map> custInit = rest.exchange("/admin/orders/1/refund", HttpMethod.POST,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(custInit.getStatusCode().value()).isEqualTo(403);
        ResponseEntity<Map> custList = rest.exchange("/admin/refunds", HttpMethod.GET,
            new HttpEntity<>(json(customerToken)), Map.class);
        assertThat(custList.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @Order(13)
    void contextStartupCovered() {
        assertThat(rest.getForEntity("/health", Map.class).getStatusCode().value()).isEqualTo(200);
    }
}