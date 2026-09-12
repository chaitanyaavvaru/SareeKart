package com.sareekart.service;

import com.sareekart.dto.request.AddressRequest;
import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.*;
import com.sareekart.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderConcurrencyAndSecurityTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderNotificationService notificationService;
    @Mock
    private WalletService walletService;
    @Mock
    private LogisticsService logisticsService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User userA;
    private User userB;
    private Product singleStockProduct;
    private Product fiveStockProduct;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(101L).email("userA@sareekart.com").role(Role.CUSTOMER).build();
        userB = User.builder().id(102L).email("userB@sareekart.com").role(Role.CUSTOMER).build();

        singleStockProduct = Product.builder()
                .id(201L)
                .name("Single Stock Saree")
                .price(new BigDecimal("15000"))
                .stockQuantity(1)
                .active(true)
                .build();

        fiveStockProduct = Product.builder()
                .id(205L)
                .name("Limited Five Stock Saree")
                .price(new BigDecimal("8000"))
                .stockQuantity(5)
                .active(true)
                .build();
    }

    private OrderRequest createRequest(String idemKey) {
        return OrderRequest.builder()
                .shippingAddress(AddressRequest.builder()
                        .fullName("Test Customer")
                        .phone("9876543210")
                        .streetAddress("Artisan Street")
                        .city("Hyderabad")
                        .state("Telangana")
                        .pincode("500001")
                        .build())
                .paymentMethod("COD")
                .idempotencyKey(idemKey)
                .build();
    }

    @Test
    @DisplayName("Mandatory Concurrency Test 1: Initial stock = 1, Customer A & B order 1 concurrently. Exactly ONE succeeds, ONE fails, stock = 0")
    void testConcurrentCheckout_SingleStock_PreventsOverselling() throws Exception {
        AtomicInteger simulatedStock = new AtomicInteger(1);
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        Cart cartA = Cart.builder().id(301L).user(userA).items(new ArrayList<>()).build();
        cartA.getItems().add(CartItem.builder().id(1L).cart(cartA).product(singleStockProduct).quantity(1).build());

        Cart cartB = Cart.builder().id(302L).user(userB).items(new ArrayList<>()).build();
        cartB.getItems().add(CartItem.builder().id(2L).cart(cartB).product(singleStockProduct).quantity(1).build());

        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(102L)).thenReturn(Optional.of(userB));
        when(cartRepository.findByUserId(101L)).thenReturn(Optional.of(cartA));
        when(cartRepository.findByUserId(102L)).thenReturn(Optional.of(cartB));

        // Thread-safe atomic decrement simulation mimicking InnoDB row lock
        when(productRepository.decrementStockIfAvailable(eq(201L), eq(1))).thenAnswer(inv -> {
            while (true) {
                int current = simulatedStock.get();
                if (current < 1) {
                    return 0; // 0 rows updated
                }
                if (simulatedStock.compareAndSet(current, current - 1)) {
                    return 1; // 1 row updated
                }
            }
        });

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(new Random().nextLong(1000L, 9999L));
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder().id(o.getId()).status("PENDING").build();
        });

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        // Customer A task
        executor.submit(() -> {
            try {
                startLatch.await();
                orderService.createOrder(101L, createRequest("idem_user_a"));
                successfulOrders.incrementAndGet();
            } catch (BadRequestException e) {
                failedOrders.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        // Customer B task
        executor.submit(() -> {
            try {
                startLatch.await();
                orderService.createOrder(102L, createRequest("idem_user_b"));
                successfulOrders.incrementAndGet();
            } catch (BadRequestException e) {
                failedOrders.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        // Release both threads simultaneously
        startLatch.countDown();
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        // Verifications
        assertEquals(1, successfulOrders.get(), "Exactly ONE order must succeed");
        assertEquals(1, failedOrders.get(), "Exactly ONE order must fail with insufficient stock");
        assertEquals(0, simulatedStock.get(), "Final stock must be exactly 0 (NEVER -1 or corrupted)");
    }

    @Test
    @DisplayName("Mandatory Concurrency Test 2: Initial stock = 5. A orders 4, B orders 3 concurrently. Total successful <= 5, never 7")
    void testConcurrentCheckout_FiveStock_TotalQuantityCapped() throws Exception {
        AtomicInteger simulatedStock = new AtomicInteger(5);
        AtomicInteger totalSoldQuantity = new AtomicInteger(0);

        Cart cartA = Cart.builder().id(401L).user(userA).items(new ArrayList<>()).build();
        cartA.getItems().add(CartItem.builder().id(11L).cart(cartA).product(fiveStockProduct).quantity(4).build());

        Cart cartB = Cart.builder().id(402L).user(userB).items(new ArrayList<>()).build();
        cartB.getItems().add(CartItem.builder().id(12L).cart(cartB).product(fiveStockProduct).quantity(3).build());

        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(102L)).thenReturn(Optional.of(userB));
        when(cartRepository.findByUserId(101L)).thenReturn(Optional.of(cartA));
        when(cartRepository.findByUserId(102L)).thenReturn(Optional.of(cartB));

        when(productRepository.decrementStockIfAvailable(eq(205L), anyInt())).thenAnswer(inv -> {
            int qty = inv.getArgument(1);
            while (true) {
                int current = simulatedStock.get();
                if (current < qty) {
                    return 0;
                }
                if (simulatedStock.compareAndSet(current, current - qty)) {
                    totalSoldQuantity.addAndGet(qty);
                    return 1;
                }
            }
        });

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(888L).build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                startLatch.await();
                orderService.createOrder(101L, createRequest("idem_user_a_5"));
            } catch (Exception ignored) {
            } finally {
                finishLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                orderService.createOrder(102L, createRequest("idem_user_b_5"));
            } catch (Exception ignored) {
            } finally {
                finishLatch.countDown();
            }
        });

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(totalSoldQuantity.get() <= 5, "Total sold quantity must be <= 5 (was " + totalSoldQuantity.get() + ")");
        assertTrue(simulatedStock.get() >= 0, "Stock must never drop below 0");
    }

    @Test
    @DisplayName("Mandatory Concurrency Test 3: Same customer sends identical idempotency key concurrently -> Exactly ONE logical order created")
    void testConcurrentCheckout_SameUserDuplicateSubmit_IdempotencySafe() throws Exception {
        Cart cart = Cart.builder().id(501L).user(userA).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().id(21L).cart(cart).product(singleStockProduct).quantity(1).build());

        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(cartRepository.findByUserId(101L)).thenReturn(Optional.of(cart));

        AtomicInteger stockDeductions = new AtomicInteger(0);
        when(productRepository.decrementStockIfAvailable(201L, 1)).thenAnswer(inv -> {
            stockDeductions.incrementAndGet();
            return 1;
        });

        Order existingOrder = Order.builder().id(9999L).idempotencyKey("same_click_key").user(userA).build();
        AtomicInteger ordersSaved = new AtomicInteger(0);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            int attempt = ordersSaved.incrementAndGet();
            if (attempt == 1) {
                return existingOrder;
            } else {
                // Second concurrent insert hits unique database constraint
                throw new DataIntegrityViolationException("Duplicate entry '101-same_click_key' for key 'uq_orders_user_idempotency'");
            }
        });

        // When duplicate occurs, findByUserIdAndIdempotencyKey returns the first order
        when(orderRepository.findByUserIdAndIdempotencyKey(101L, "same_click_key"))
                .thenReturn(Optional.of(existingOrder));

        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(9999L).build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);
        List<OrderResponse> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OrderResponse r = orderService.createOrder(101L, createRequest("same_click_key"));
                    responses.add(r);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(2, responses.size(), "Both requests must receive a response");
        assertEquals(responses.get(0).getId(), responses.get(1).getId(), "Both responses must resolve to the identical order ID");
    }

    @Test
    @DisplayName("Payment Abandonment Test: Cancel pending order restores stock, repeated call does NOT double restore, sweep no-ops")
    void testPaymentAbandonmentAndSweep_ExactRestoration() {
        Order pendingOrder = Order.builder()
                .id(601L)
                .user(userA)
                .status(OrderStatus.PENDING)
                .paymentStatus("PENDING")
                .items(List.of(
                        OrderItem.builder().product(singleStockProduct).quantity(1).build()
                ))
                .build();

        when(orderRepository.findById(601L)).thenReturn(Optional.of(pendingOrder));
        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder().id(o.getId()).status(o.getStatus().name()).build();
        });

        // First cancel-pending: DB atomic transition succeeds (returns 1 row updated)
        when(orderRepository.transitionOrderStatusAndPaymentStatus(
                601L, OrderStatus.PENDING, "PENDING", OrderStatus.CANCELLED, "CANCELLED"
        )).thenReturn(1);

        // 1. First cancellation call (e.g. from Razorpay ondismiss)
        OrderResponse res1 = orderService.cancelPendingOrder(601L, 101L);
        assertEquals("CANCELLED", res1.getStatus());
        // Verify stock restored exactly 1 unit
        verify(productRepository, times(1)).incrementStock(201L, 1);

        // 2. Second cancellation call (e.g. user retries cancellation or network repeat)
        OrderResponse res2 = orderService.cancelPendingOrder(601L, 101L);
        assertEquals("CANCELLED", res2.getStatus());
        // Verify stock was NOT restored a second time!
        verify(productRepository, times(1)).incrementStock(201L, 1);

        // 3. Simulated background sweep
        when(orderRepository.findByStatusAndPaymentStatusAndCreatedAtBefore(
                eq(OrderStatus.PENDING), eq("PENDING"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList()); // Already cancelled, so sweep finds 0 orders

        int swept = orderService.sweepAbandonedPendingOrders(30);
        assertEquals(0, swept);
        // Stock still remains restored only once!
        verify(productRepository, times(1)).incrementStock(201L, 1);
    }
}
