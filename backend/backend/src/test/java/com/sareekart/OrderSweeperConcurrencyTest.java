package com.sareekart;

import com.sareekart.client.RazorpayGateway;
import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.dto.payment.VerifyPaymentRequest;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.security.SignatureUtil;
import com.sareekart.service.OrderSweeperService;
import com.sareekart.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Genuine interleaving proof: for each seeded stale order, the sweeper and a
 * valid signature verification race simultaneously on separate threads.
 *
 * Invariant under the shared payment-row lock — exactly one outcome wins:
 *   A) verify wins   → order PROCESSING/PAID, sweeper.expireOne == false
 *   B) sweeper wins  → order CANCELLED/FAILED, verification rejected
 * Mixed or lost transitions are failures. Runs against REAL MySQL.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.sweeper.enabled=true",
        "app.sweeper.interval-ms=3600000", // scheduler silent; direct service calls
        "app.razorpay.enabled=true",
        "app.razorpay.key-id=rzp_test_race",
        "app.razorpay.key-secret=race-it-secret",
        "app.razorpay.webhook-secret=race-it-whsec"
    })
class OrderSweeperConcurrencyTest {

    private static final String KEY_SECRET = "race-it-secret";
    private static final String CUSTOMER_EMAIL = "customer@sareekart.com";
    private static final int ORDERS = 10;

    @Autowired private TestRestTemplate rest;
    @Autowired private OrderSweeperService sweeperService;
    @Autowired private PaymentService paymentService;
    @MockitoBean private RazorpayGateway razorpayGateway;

    @Test
    @DisplayName("sweeper ∥ verify: exactly one side wins per order, never both, never neither")
    void sweeperAndVerifyRace() throws Exception {
        // ── login once ──
        HttpHeaders jsonNoAuth = new HttpHeaders();
        jsonNoAuth.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> login = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", CUSTOMER_EMAIL, "password", "customer123"), jsonNoAuth), Map.class);
        String token = ((Map<?, ?>) login.getBody().get("data")).get("token").toString();
        HttpHeaders authed = new HttpHeaders();
        authed.setContentType(MediaType.APPLICATION_JSON);
        authed.setBearerAuth(token);

        // ── admin seeds one reusable product ──
        ResponseEntity<Map> adminLogin = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", "admin@sareekart.com", "password", "admin123"), jsonNoAuth), Map.class);
        String adminToken = ((Map<?, ?>) adminLogin.getBody().get("data")).get("token").toString();
        HttpHeaders adminAuth = new HttpHeaders();
        adminAuth.setContentType(MediaType.APPLICATION_JSON);
        adminAuth.setBearerAuth(adminToken);

        ResponseEntity<Map> product = rest.exchange("/admin/products", HttpMethod.POST,
            new HttpEntity<>(Map.of(
                "name", "Race Saree " + System.nanoTime(),
                "categoryId", 1, "basePrice", 1000,
                "sku", "SK-RACE-" + System.nanoTime(),
                "variants", List.of(Map.of("size", "Free", "color", "Slate", "stockQuantity", 500))
            ), adminAuth), Map.class);
        long pid = ((Number) ((Map<?, ?>) product.getBody().get("data")).get("id")).longValue();

        given(razorpayGateway.createOrder(anyString(), anyLong(), anyString()))
            .willAnswer(inv -> RazorpayOrderResponse.builder()
                .id("order_race_" + System.nanoTime() + "_" + inv.getArgument(1))
                .amount(inv.getArgument(1, Long.class)).build());

        record Outcome(int swept, int verified) {}

        AtomicInteger verifyWins = new AtomicInteger();
        AtomicInteger sweepWins = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(ORDERS * 2);

        for (int i = 0; i < ORDERS; i++) {
            // ── seed stale order + gateway mint ──
            rest.exchange("/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", pid, "quantity", 1), authed), Map.class);
            ResponseEntity<Map> orderRes = rest.exchange("/orders", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                    "shippingAddress", Map.of("fullName", "R", "phone", "9876543210",
                        "streetAddress", "1 Race St", "city", "Hyderabad",
                        "state", "Telangana", "pincode", "500001"),
                    "paymentMethod", "RAZORPAY"), authed), Map.class);
            long orderId = ((Number) ((Map<?, ?>) orderRes.getBody().get("data")).get("id")).longValue();

            ResponseEntity<Map> co = rest.exchange("/payments/create-order/" + orderId,
                HttpMethod.POST, new HttpEntity<>(authed), Map.class);
            assertThat(co.getStatusCode().value()).isEqualTo(200);
            final String rzpOrderId = (String) ((Map<?, ?>) co.getBody().get("data")).get("razorpayOrderId");

            // Rewind so the sweeper treats this order as stale (45min > 30min cutoff).
            makeStaleViaJdbc(orderId);

            CountDownLatch start = new CountDownLatch(1);

            Future<Boolean> sweeperF = pool.submit(() -> {
                start.await();
                return sweeperService.expireOne(orderId);
            });

            Future<Boolean> verifyF = pool.submit(() -> {
                start.await();
                try {
                    String sig = SignatureUtil.hmacSha256Hex(rzpOrderId + "|pay_race_" + orderId, KEY_SECRET);
                    OrderResponse r = paymentService.verifyPayment(CUSTOMER_EMAIL,
                        VerifyPaymentRequest.builder()
                            .orderId(String.valueOf(orderId))
                            .razorpayOrderId(rzpOrderId)
                            .razorpayPaymentId("pay_race_" + orderId)
                            .razorpaySignature(sig)
                            .build());
                    return r.getStatus() == OrderStatus.PROCESSING || r.getStatus() == OrderStatus.PENDING;
                } catch (Exception rejected) {
                    return false; // expected when sweeper won
                }
            });

            start.countDown(); // release both racers simultaneously

            boolean swept, verified;
            try {
                swept = sweeperF.get(45, TimeUnit.SECONDS);
                verified = verifyF.get(45, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                System.out.println("RACE TIMEOUT dump:");
                Thread.getAllStackTraces().forEach((t, st) -> {
                    if (t.getName().contains("pool")) {
                        StackTraceElement[] frames = st;
                        if (frames.length > 0)
                            System.out.println("  " + t.getName() + " → "
                                + frames[0] + " | " + (frames.length>3 ? frames[3] : ""));
                    }
                });
                throw te;
            }

            // ── invariant ──
            System.out.println("RACE result order=" + orderId + " swept=" + swept + " verified=" + verified);
            assertThat(swept ^ verified)
                .as("order %d: exactly one side must win (swept=%s verified=%s)", orderId, swept, verified)
                .isTrue();
            if (verified) verifyWins.incrementAndGet(); else sweepWins.incrementAndGet();

            // ── terminal state matches winner ──
            ResponseEntity<Map> after = rest.exchange("/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(authed), Map.class);
            String status = (String) ((Map<?, ?>) after.getBody().get("data")).get("status");
            if (verified) {
                assertThat(status).isEqualTo("PROCESSING");
            } else {
                assertThat(status).isEqualTo("CANCELLED");
            }
        }

        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        // Both sides must occur across runs; exact split is timing-dependent.
        assertThat(verifyWins.get() + sweepWins.get()).isEqualTo(ORDERS);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private void makeStaleViaJdbc(long orderId) {
        jdbc.update("UPDATE orders SET created_at = DATE_SUB(NOW(), INTERVAL 45 MINUTE) WHERE id = ?", orderId);
    }
}
