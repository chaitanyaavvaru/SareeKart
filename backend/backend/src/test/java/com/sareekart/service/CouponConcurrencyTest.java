package com.sareekart.service;

import com.sareekart.entity.Coupon;
import com.sareekart.entity.Order;
import com.sareekart.entity.User;
import com.sareekart.repository.CouponRedemptionRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency proof against REAL MySQL: N simultaneous reservations of a
 * total-limit-3 coupon must persist exactly 3 redemption rows.
 *
 * The pessimistic lock on the coupon row serializes each thread's
 * check-limits-then-insert pair; the UNIQUE(order_id) constraint additionally
 * makes every individual reservation idempotent.
 *
 * Requires SPRING_DATASOURCE_* pointing at a live database.
 */
@SpringBootTest
class CouponConcurrencyTest {

    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponRedemptionRepository redemptionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate txTemplate;

    @Test
    @DisplayName("8 parallel reservations on a limit-3 coupon → exactly 3 rows")
    void parallelReservationsRespectGlobalLimit() throws Exception {
        // ── fixtures ────────────────────────────────────────────────────────
        final String code = "CONC" + System.nanoTime() % 1_000_000_000L;
        Long userId = txTemplate.execute(status -> {
            User u = new User();
            u.setEmail("conc-" + System.nanoTime() + "@sareekart.test");
            u.setPassword(passwordEncoder.encode("x"));
            u.setFirstName("Conc");
            u.setLastName("Test");
            u.setEnabled(true);
            return userRepository.save(u).getId();
        });

        Long couponId = txTemplate.execute(status -> {
            Coupon c = new Coupon();
            c.setCode(code);
            c.setDescription("concurrency IT");
            c.setDiscountType(com.sareekart.entity.enums.DiscountType.FIXED_AMOUNT);
            c.setDiscountValue(new BigDecimal("10"));
            c.setMinimumOrderAmount(BigDecimal.ZERO);
            c.setTotalUsageLimit(3);
            c.setActive(true);
            return couponRepository.save(c).getId();
        });

        // Pre-create 8 REAL orders (redemptions hold an FK to them)
        final List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final int seq = i;
            orderIds.add(txTemplate.execute(status -> {
                Order o = new Order();
                o.setOrderNumber("CONC" + (System.nanoTime() % 100000000L) + "S" + seq); // ≤20 chars
                o.setUser(userRepository.getReferenceById(userId));
                o.setStatus(com.sareekart.entity.enums.OrderStatus.PENDING);
                o.setSubtotal(new BigDecimal("500"));
                o.setShippingAmount(BigDecimal.ZERO);
                o.setTaxAmount(BigDecimal.ZERO);
                o.setDiscountAmount(BigDecimal.ZERO);
                o.setTotalAmount(new BigDecimal("500"));
                o.setShippingAddressJson("{\"fullName\":\"c\"}");
                o.setPaymentMethod(com.sareekart.entity.enums.PaymentMethod.RAZORPAY);
                o.setPaymentStatus(com.sareekart.entity.enums.PaymentStatus.PENDING);
                o.setItems(new ArrayList<>());
                return orderRepository.save(o).getId();
            }));
        }

        // Detached-but-initialized order shells (mirrors real managed inputs)
        final Map<Long, Order> shells = new java.util.HashMap<>();
        for (Long oid : orderIds) {
            Order o = txTemplate.execute(st -> {
                Order loaded = orderRepository.findById(oid).orElseThrow();
                loaded.getUser().getEmail(); // force-initialize lazy owner
                return loaded;
            });
            shells.put(oid, o);
        }

        // ── race ────────────────────────────────────────────────────────────
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger successes = new AtomicInteger();

        List<String> rejections = java.util.Collections.synchronizedList(new ArrayList<>());
        List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
        for (Long orderId : orderIds) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    // Each call runs in its own transaction via the Spring proxy.
                    couponService.reserveForOrder(shells.get(orderId), code);
                    successes.incrementAndGet();
                    return true;
                } catch (Exception rejected) {
                    rejections.add(rejected.getClass().getSimpleName() + ": " + rejected.getMessage());
                    return false;
                }
            }));
        }
        start.countDown();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        System.out.println("CONC rejections sample: " + rejections.stream().limit(3).toList());
        long persisted = redemptionRepository.countByCouponId(couponId);
        assertThat(successes.get()).as("winning threads").isEqualTo(3);
        assertThat(persisted).as("rows persisted under race").isEqualTo(3);

        // Propagate any unexpected worker errors (non-BadRequest failures)
        for (var f : futures) f.get();

        // Sanity: the seeded admin account was untouched by fixture churn.
        assertThat(userRepository.existsByEmail("admin@sareekart.com")).isTrue();
        assertThat(Set.copyOf(List.of(successes.get(), (int) persisted)).size()).isEqualTo(1);
    }
}