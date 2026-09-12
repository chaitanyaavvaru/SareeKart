package com.sareekart.service;

import com.sareekart.config.CartConstants;
import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.request.CartMergeRequest;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.entity.Cart;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.CartMapper;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(7L).email("shopper@example.com").build();
        product = Product.builder()
                .id(11L)
                .name("Kanchipuram Brocade Saree")
                .price(new BigDecimal("15000"))
                .stockQuantity(15)
                .active(true)
                .build();
        cart = Cart.builder().id(3L).user(user).items(new ArrayList<>()).build();
        lenient().when(cartRepository.findByUserId(7L)).thenReturn(Optional.of(cart));
        lenient().when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addItemToCart_success_newItem() {
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        CartResponse res = cartService.addItemToCart(7L, CartItemRequest.builder().productId(11L).quantity(2).build());

        assertNotNull(res);
        assertEquals(1, cart.getItems().size());
        assertEquals(11L, cart.getItems().get(0).getProduct().getId());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_success_incrementsExistingItem() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        cartService.addItemToCart(7L, CartItemRequest.builder().productId(11L).quantity(3).build());

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_rejectsQuantityExceedingMaxCap() {
        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(
                7L, CartItemRequest.builder().productId(11L).quantity(CartConstants.MAX_QUANTITY_PER_SKU + 1).build()));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemToCart_rejectsQuantityExceedingStock() {
        product.setStockQuantity(2);
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(
                7L, CartItemRequest.builder().productId(11L).quantity(3).build()));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_success() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        cartService.updateItemQuantity(7L, 11L, 4);

        assertEquals(4, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateItemQuantity_zeroQuantityRemovesItem() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        cartService.updateItemQuantity(7L, 11L, 0);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateItemQuantity_rejectsNegativeQuantity() {
        assertThrows(BadRequestException.class, () -> cartService.updateItemQuantity(7L, 11L, -1));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_rejectsExceedingMaxCap() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());

        assertThrows(BadRequestException.class, () -> cartService.updateItemQuantity(7L, 11L, CartConstants.MAX_QUANTITY_PER_SKU + 1));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItemFromCart_success() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        cartService.removeItemFromCart(7L, 11L);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_success() {
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());

        cartService.clearCart(7L);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void mergeGuestCart_mergesItemsWithCappedSummation() {
        // Customer already has 2 units of Product 11
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(2).build());

        // Second product available for guest
        Product product2 = Product.builder()
                .id(12L)
                .name("Banarasi Georgette")
                .price(new BigDecimal("12000"))
                .stockQuantity(8)
                .active(true)
                .build();

        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(productRepository.findById(12L)).thenReturn(Optional.of(product2));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        // Guest has 3 units of Product 11, and 2 units of Product 12
        CartMergeRequest mergeRequest = CartMergeRequest.builder()
                .items(List.of(
                        CartItemRequest.builder().productId(11L).quantity(3).build(),
                        CartItemRequest.builder().productId(12L).quantity(2).build()
                ))
                .build();

        cartService.mergeGuestCart(7L, mergeRequest);

        assertEquals(2, cart.getItems().size());
        // Product 11: 2 existing + 3 guest = 5 units (below stock of 15 and below cap of 10)
        CartItem item1 = cart.getItems().stream().filter(i -> i.getProduct().getId().equals(11L)).findFirst().orElseThrow();
        assertEquals(5, item1.getQuantity());

        // Product 12: new item with 2 units
        CartItem item2 = cart.getItems().stream().filter(i -> i.getProduct().getId().equals(12L)).findFirst().orElseThrow();
        assertEquals(2, item2.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void mergeGuestCart_capsCombinedQuantityAtStock() {
        // Available stock is only 4
        product.setStockQuantity(4);
        cart.getItems().add(CartItem.builder().id(101L).cart(cart).product(product).quantity(3).build());

        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        // Guest wants 3 more (total would be 6, but stock is 4)
        CartMergeRequest mergeRequest = CartMergeRequest.builder()
                .items(List.of(CartItemRequest.builder().productId(11L).quantity(3).build()))
                .build();

        cartService.mergeGuestCart(7L, mergeRequest);

        CartItem item = cart.getItems().get(0);
        assertEquals(4, item.getQuantity(), "Quantity must be capped at available stock of 4");
    }

    @Test
    void mergeGuestCart_skipsInactiveOrOutOfStockItems() {
        Product outOfStockProduct = Product.builder().id(99L).stockQuantity(0).active(true).build();
        when(productRepository.findById(99L)).thenReturn(Optional.of(outOfStockProduct));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().id(3L).build());

        CartMergeRequest mergeRequest = CartMergeRequest.builder()
                .items(List.of(CartItemRequest.builder().productId(99L).quantity(2).build()))
                .build();

        cartService.mergeGuestCart(7L, mergeRequest);

        assertTrue(cart.getItems().isEmpty());
    }
}
