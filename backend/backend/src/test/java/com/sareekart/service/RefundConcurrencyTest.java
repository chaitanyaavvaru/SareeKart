package com.sareekart.service;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Concurrency proof on REAL MySQL: duplicate refund.processed webhooks
 * delivered in parallel (distinct event ids, same gateway refund id) must
 * produce exactly one SUCCESS trail and — for full refunds — exactly one
 * restock, with aggregates settling at REFUNDED.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.razorpay.enabled=true",
        "app.razorpay.key-id=rzp_test_refund_race",
        "app.razorpay.key-secret=refund-race-secret",
        "app.razorpay.webhook-secret=refund-race-whsec"
    })
class RefundConcurrencyTest {

    private static final String KEY_SECRET = "refund-race-secret";
    private static final String WEBHOOK_SECRET = "refund-race-whsec";

    @Autowired private org.springframework.boot.test.web.client.TestRestTemplate rest;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean private RazorpayGateway razorpayGateway;

    @Test
    @DisplayName("6 parallel duplicate refund webhooks → one trail, one restock, REFUNDED")
    void duplicateRefundWebhooksRace() throws Exception {
        String adminToken = login("admin@sareekart.com", "admin123");
        String customerToken = login("customer@sareekart.com", "customer123");

        long pid = seedProduct(adminToken);
        long orderId = createAndPay(customerToken, pid);
        final String refundId = "rf_race_" + orderId;
        int stockAfterPay = stock(pid);

        // Pre-create the local PENDING trail via admin initiation (gateway async).
        given(razorpayGateway.createRefund(anyString(), anyLong()))
            .willAnswer(inv -> RazorpayRefundResponse.builder()
                .id(refundId).status("pending")
                .amount(inv.getArgument(1, Long.class)).build());
        var initRes = initiate(adminToken, orderId);
        assertThat(initRes.getStatusCode().value()).isEqualTo(200);

        // 6 parallel webhook deliveries of the same gateway refund id
        int deliveries = 6;
        String rzpPaymentId = jdbc.queryForObject(
            "SELECT p.provider_payment_id FROM payments p JOIN refunds r ON r.payment_id=p.id "
                + "WHERE r.provider_refund_id = ?", String.class, refundId);

        ExecutorService pool = Executors.newFixedThreadPool(deliveries);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();

        List<java.util.concurrent.Future<Integer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < deliveries; i++) {
            final String eventId = "evt_race_" + i + "_" + System.nanoTime();
            futures.add(pool.submit(() -> {
                start.await();
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                String payload = payloadFor(refundId, rzpPaymentId);
                h.set("X-Razorpay-Signature",
                    com.sareekart.security.SignatureUtil.hmacSha256Hex(payload, WEBHOOK_SECRET));
                h.set("X-Razorpay-Event-Id", eventId);
                ResponseEntity<String> res = rest.postForEntity("/payments/webhook",
                    new HttpEntity<>(payload, h), String.class);
                if (res.getStatusCode().value() == 200) accepted.incrementAndGet();
                return res.getStatusCode().value();
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        for (var f : futures) f.get();

        assertThat(accepted.get()).isEqualTo(deliveries); // all acked (idempotently)

        Integer rows = jdbc.queryForObject(
            "SELECT COUNT(*) FROM refunds WHERE provider_refund_id = ?",
            Integer.class, refundId);
        assertThat(rows).as("single refund trail").isEqualTo(1);

        String rowStatus = jdbc.queryForObject(
            "SELECT status FROM refunds WHERE provider_refund_id = ?",
            String.class, refundId);
        assertThat(rowStatus).isEqualTo("SUCCESS");

        Integer restocked = jdbc.queryForObject(
            "SELECT inventory_restocked FROM orders WHERE id = ?", Integer.class, orderId);
        assertThat(restocked).as("restock exactly once").isEqualTo(1);
        assertThat(stock(pid)).isEqualTo(stockAfterPay + 1);

        String payStatus = jdbc.queryForObject(
            "SELECT payment_status FROM orders WHERE id = ?", String.class, orderId);
        assertThat(payStatus).isEqualTo("REFUNDED");

        // Coupon ledger untouched by the entire storm
        Integer couponRows = jdbc.queryForObject(
            "SELECT COUNT(*) FROM coupon_redemptions WHERE order_id = ?", Integer.class, orderId);
        assertThat(couponRows).isZero(); // this order used no coupon; nothing appeared
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String payloadFor(String refundId, String rzpPaymentId) {
        return "{\"event\":\"refund.processed\",\"payload\":{"
            + "\"refund\":{\"entity\":{\"id\":\"" + refundId + "\",\"payment_id\":\""
            + rzpPaymentId + "\",\"amount\":120000}},"
            + "\"payment\":{\"entity\":{\"id\":\"" + rzpPaymentId + "\"}}}}";
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        ResponseEntity<Map> res = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", email, "password", password),
                jsonHeaders()), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Map<?, ?>) res.getBody().get("data")).get("token").toString();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @SuppressWarnings("unchecked")
    private long seedProduct(String adminToken) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(adminToken);
        ResponseEntity<Map> res = rest.exchange("/admin/products", HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "RaceRefund Saree", "categoryId", 1,
                    "basePrice", 1000, "sku", "SK-RF-RACE-" + System.nanoTime(),
                    "variants", List.of(Map.of("size","F","color","Gold","stockQuantity",20))),
                h), Map.class);
        return ((Number) ((Map<?, ?>) res.getBody().get("data")).get("id")).longValue();
    }

    private long createAndPay(String customerToken, long productId) throws Exception {
        HttpHeaders authed = jsonHeaders();
        authed.setBearerAuth(customerToken);

        rest.exchange("/cart/items", HttpMethod.POST,
            new HttpEntity<>(Map.of("productId", productId, "quantity", 1), authed), Map.class);
        ResponseEntity<Map> order = rest.exchange("/orders", HttpMethod.POST,
            new HttpEntity<>(Map.of(
                "shippingAddress", Map.of("fullName","R","phone","9876543210",
                    "streetAddress","1","city","Hyd","state","TS","pincode","500001"),
                "paymentMethod","RAZORPAY"), authed), Map.class);
        long orderId = ((Number) ((Map<?, ?>) order.getBody().get("data")).get("id")).longValue();

        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> com.sareekart.dto.payment.RazorpayOrderResponse.builder()
                .id("order_rfrace_" + orderId).amount(inv.getArgument(1, Long.class)).build());
        ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + orderId,
            HttpMethod.POST, new HttpEntity<>(authed), Map.class);
        String rzpOrderId = (String) ((Map<?, ?>) co.getBody().get("data")).get("razorpayOrderId");
        String rzpPaymentId = "pay_rfrace_" + orderId;

        String sig = com.sareekart.security.SignatureUtil.hmacSha256Hex(
            rzpOrderId + "|" + rzpPaymentId, KEY_SECRET);
        ResponseEntity<Map> verify = rest.exchange("/payments/verify", HttpMethod.POST,
            new HttpEntity<>(Map.of("orderId", String.valueOf(orderId),
                    "razorpayOrderId", rzpOrderId, "razorpayPaymentId", rzpPaymentId,
                    "razorpaySignature", sig), authed), Map.class);
        assertThat(verify.getStatusCode().value()).isEqualTo(200);
        return orderId;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> initiate(String adminToken, long orderId) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(adminToken);
        return rest.exchange("/admin/orders/" + orderId + "/refund", HttpMethod.POST,
            new HttpEntity<>(Map.of("reason", "race IT"), h), Map.class);
    }

    private int stock(long productId) {
        Integer s = jdbc.queryForObject(
            "SELECT v.stock_quantity FROM product_variants v WHERE v.product_id=? LIMIT 1",
            Integer.class, productId);
        return s == null ? 0 : s;
    }
}