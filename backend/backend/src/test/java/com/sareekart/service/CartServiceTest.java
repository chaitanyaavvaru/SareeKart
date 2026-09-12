package com.sareekart.service;

import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.entity.Cart;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.mapper.CartMapper;
import com.sareekart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        User user = User.builder().id(7L).email("generated@example.test").build();
        product = Product.builder()
                .id(11L)
                .name("Generated Saree")
                .price(new BigDecimal("1000"))
                .stockQuantity(2)
                .active(true)
                .build();
        cart = Cart.builder().id(3L).user(user).build();
        when(cartRepository.findByUserId(7L)).thenReturn(Optional.of(cart));
    }

    @Test
    void rejectsAddWhenRequestedQuantityExceedsStock() {
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> cartService.addItemToCart(
                7L, CartItemRequest.builder().productId(11L).quantity(3).build()));

        verifyNoCartSave();
    }

    @Test
    void rejectsQuantityUpdateWhenItExceedsStock() {
        cart.setItems(List.of(CartItem.builder().cart(cart).product(product).quantity(1).build()));

        assertThrows(BadRequestException.class, () -> cartService.updateItemQuantity(7L, 11L, 3));

        verifyNoCartSave();
    }

    private void verifyNoCartSave() {
        org.mockito.Mockito.verify(cartRepository, never()).save(any(Cart.class));
    }
}
