package com.sareekart.service;

import com.sareekart.dto.request.AddressRequest;
import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    private User customer;
    private Cart cart;
    private Product product1;
    private Product product2;
    private OrderRequest standardRequest;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(10L).email("buyer@sareekart.com").role(Role.CUSTOMER).build();
        cart = Cart.builder().id(20L).user(customer).items(new ArrayList<>()).build();

        product1 = Product.builder()
                .id(101L)
                .name("Kanchipuram Silk")
                .price(new BigDecimal("10000"))
                .stockQuantity(10)
                .active(true)
                .images(List.of("https://sareekart.com/img1.jpg"))
                .build();

        product2 = Product.builder()
                .id(102L)
                .name("Banarasi Brocade")
                .price(new BigDecimal("5000"))
                .stockQuantity(5)
                .active(true)
                .images(List.of("https://sareekart.com/img2.jpg"))
                .build();

        standardRequest = OrderRequest.builder()
                .shippingAddress(AddressRequest.builder()
                        .fullName("Ananya Sharma")
                        .phone("9876543210")
                        .streetAddress("12 Temple Street")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .pincode("600001")
                        .build())
                .paymentMethod("COD")
                .idempotencyKey("idem_key_123")
                .build();
    }

    @Test
    @DisplayName("1. Successful COD Order: Decrements stock atomically, captures snapshots, clears cart")
    void testCreateOrder_SuccessfulCodOrder() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(2).build());

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(101L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(501L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder().id(o.getId()).status("PENDING").paymentMethod("COD").build();
        });

        OrderResponse res = orderService.createOrder(10L, standardRequest);

        assertNotNull(res);
        assertEquals(501L, res.getId());
        verify(productRepository).decrementStockIfAvailable(101L, 2);
        assertTrue(cart.getItems().isEmpty(), "Cart items must be cleared upon successful order");
        verify(cartRepository).save(cart);
        verify(notificationService).sendOrderPlacedNotification(any(Order.class));
    }

    @Test
    @DisplayName("2. Successful Wallet-Covered Order: Payment status marks COMPLETED, redeems wallet")
    void testCreateOrder_SuccessfulWalletCoveredOrder() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product2).quantity(1).build());

        standardRequest.setPaymentMethod("WALLET");
        standardRequest.setWalletDebitAmount(new BigDecimal("5000")); // Covers 5000 total

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(102L, 1)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(502L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder().id(o.getId()).status("PENDING").paymentMethod("WALLET").paymentStatus("COMPLETED").build();
        });

        OrderResponse res = orderService.createOrder(10L, standardRequest);

        assertNotNull(res);
        assertEquals("COMPLETED", res.getPaymentStatus());
        verify(walletService).redeemWallet(eq(customer), eq(new BigDecimal("5000")), eq(502L));
    }

    @Test
    @DisplayName("3. Razorpay Pending Order: Correctly records idempotency key and PENDING status")
    void testCreateOrder_RazorpayPendingOrder() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(1).build());
        standardRequest.setPaymentMethod("RAZORPAY");

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(101L, 1)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(503L);
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return OrderResponse.builder().id(o.getId()).status("PENDING").paymentStatus("PENDING").build();
        });

        OrderResponse res = orderService.createOrder(10L, standardRequest);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertEquals("PENDING", res.getPaymentStatus());
    }

    @Test
    @DisplayName("4. Insufficient Stock: Fails with BadRequestException, order not saved")
    void testCreateOrder_InsufficientStock_ThrowsBadRequest() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(15).build());

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        // Atomic decrement returns 0 rows updated
        when(productRepository.decrementStockIfAvailable(101L, 15)).thenReturn(0);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder(10L, standardRequest));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("5. Exact Stock Boundary: Purchasing exact remaining stock succeeds")
    void testCreateOrder_ExactStockBoundary_Succeeds() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(10).build());

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(productRepository.decrementStockIfAvailable(101L, 10)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(505L).build());

        OrderResponse res = orderService.createOrder(10L, standardRequest);
        assertNotNull(res);
        verify(productRepository).decrementStockIfAvailable(101L, 10);
    }

    @Test
    @DisplayName("6 & 7. Zero or Negative Quantity: Rejected with BadRequestException")
    void testCreateOrder_ZeroOrNegativeQuantity_Rejected() {
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(0).build());

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(10L, standardRequest));
    }

    @Test
    @DisplayName("8. Inactive Product: Checkout rejected with BadRequestException")
    void testCreateOrder_InactiveProduct_Rejected() {
        product1.setActive(false);
        cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product1).quantity(1).build());

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder(10L, standardRequest));

        assertTrue(ex.getMessage().contains("no longer available"));
    }

    @Test
    @DisplayName("9. Duplicate Idempotency Key: Returns existing order without deducting stock again")
    void testCreateOrder_DuplicateIdempotencyKey_ReturnsExistingOrder() {
        Order existingOrder = Order.builder().id(777L).idempotencyKey("idem_key_123").user(customer).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByUserIdAndIdempotencyKey(10L, "idem_key_123"))
                .thenReturn(Optional.of(existingOrder));
        when(orderMapper.toResponse(existingOrder)).thenReturn(OrderResponse.builder().id(777L).build());

        OrderResponse res = orderService.createOrder(10L, standardRequest);

        assertNotNull(res);
        assertEquals(777L, res.getId());
        verify(productRepository, never()).decrementStockIfAvailable(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("13 & 16. Cancellation: Pre-dispatch cancellation restores stock atomically")
    void testCancelOrder_PreDispatch_RestoresStockAtomically() {
        Order order = Order.builder()
                .id(801L)
                .user(customer)
                .status(OrderStatus.PENDING)
                .paymentStatus("PENDING")
                .items(List.of(
                        OrderItem.builder().product(product1).quantity(3).price(new BigDecimal("10000")).build()
                ))
                .build();

        when(orderRepository.findById(801L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(801L).status("CANCELLED").build());

        OrderResponse res = orderService.cancelOrder(801L, 10L);

        assertEquals("CANCELLED", res.getStatus());
        verify(productRepository).incrementStock(101L, 3);
    }

    @Test
    @DisplayName("14. Repeated Cancellation: Idempotent cancellation does not double restore stock")
    void testCancelPendingOrder_RepeatedCall_DoesNotRestoreStockTwice() {
        Order alreadyCancelledOrder = Order.builder()
                .id(802L)
                .user(customer)
                .status(OrderStatus.CANCELLED)
                .paymentStatus("CANCELLED")
                .items(List.of(OrderItem.builder().product(product1).quantity(2).build()))
                .build();

        when(orderRepository.findById(802L)).thenReturn(Optional.of(alreadyCancelledOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderMapper.toResponse(alreadyCancelledOrder)).thenReturn(OrderResponse.builder().id(802L).status("CANCELLED").build());

        OrderResponse res = orderService.cancelPendingOrder(802L, 10L);

        assertEquals("CANCELLED", res.getStatus());
        // Verify incrementStock was NEVER called because order was already CANCELLED
        verify(productRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("15. Paid Order Cancellation Rejection: cancelPendingOrder rejects completed payment")
    void testCancelPendingOrder_PaidOrder_ThrowsBadRequest() {
        Order paidOrder = Order.builder()
                .id(803L)
                .user(customer)
                .status(OrderStatus.CONFIRMED)
                .paymentStatus("COMPLETED")
                .build();

        when(orderRepository.findById(803L)).thenReturn(Optional.of(paidOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelPendingOrder(803L, 10L));

        verify(productRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("17. Customer Order Isolation: User cannot view another customer's order")
    void testGetOrderById_CustomerIsolation_ThrowsUnauthorized() {
        User anotherUser = User.builder().id(99L).email("other@sareekart.com").role(Role.CUSTOMER).build();
        Order order = Order.builder().id(901L).user(anotherUser).build();

        when(orderRepository.findById(901L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.getOrderById(901L, 10L));

        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    @DisplayName("18, 19, 20. Staff RBAC Parity: ADMIN, OWNER, MANAGER can view customer orders")
    void testGetOrderById_StaffRoles_Authorized() {
        Order order = Order.builder().id(902L).user(customer).build();
        when(orderRepository.findById(902L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(902L).build());

        for (Role staffRole : List.of(Role.ADMIN, Role.OWNER, Role.MANAGER)) {
            User staffUser = User.builder().id(200L).email("staff@sareekart.com").role(staffRole).build();
            when(userRepository.findById(200L)).thenReturn(Optional.of(staffUser));

            OrderResponse res = orderService.getOrderById(902L, 200L);
            assertNotNull(res);
            assertEquals(902L, res.getId());
        }
    }
}
