package com.sareekart.service;

import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.request.WishlistSyncRequest;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.entity.Wishlist;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.WishlistRepository;
import com.sareekart.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
                .id(20L)
                .name("Tussar Silk Saree")
                .price(new BigDecimal("8500"))
                .stockQuantity(5)
                .active(true)
                .build();
    }

    @Test
    void getWishlist_returnsMappedProducts() {
        Wishlist entry = Wishlist.builder().id(1L).userId(5L).productId(20L).build();
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(entry));
        when(productRepository.findAllById(List.of(20L))).thenReturn(List.of(activeProduct));
        when(productMapper.toResponse(activeProduct)).thenReturn(ProductResponse.builder().id(20L).name("Tussar Silk Saree").build());

        List<ProductResponse> res = wishlistService.getWishlist(5L);

        assertEquals(1, res.size());
        assertEquals(20L, res.get(0).getId());
    }

    @Test
    void getWishlistCount_returnsCount() {
        when(wishlistRepository.countByUserId(5L)).thenReturn(3L);

        long count = wishlistService.getWishlistCount(5L);

        assertEquals(3L, count);
    }

    @Test
    void addToWishlist_savesWhenNotPresent() {
        when(productRepository.existsById(20L)).thenReturn(true);
        when(wishlistRepository.findByUserIdAndProductId(5L, 20L)).thenReturn(Optional.empty());

        wishlistService.addToWishlist(5L, 20L);

        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void addToWishlist_idempotentWhenAlreadyPresent() {
        Wishlist existing = Wishlist.builder().id(1L).userId(5L).productId(20L).build();
        when(productRepository.existsById(20L)).thenReturn(true);
        when(wishlistRepository.findByUserIdAndProductId(5L, 20L)).thenReturn(Optional.of(existing));

        wishlistService.addToWishlist(5L, 20L);

        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void addToWishlist_throwsNotFoundForInvalidProduct() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.addToWishlist(5L, 999L));
    }

    @Test
    void removeFromWishlist_deletesIfExists() {
        Wishlist existing = Wishlist.builder().id(1L).userId(5L).productId(20L).build();
        when(wishlistRepository.findByUserIdAndProductId(5L, 20L)).thenReturn(Optional.of(existing));

        wishlistService.removeFromWishlist(5L, 20L);

        verify(wishlistRepository).delete(existing);
    }

    @Test
    void moveWishlistToCart_success_addsToCartAndDeletesFromWishlist() {
        Wishlist existing = Wishlist.builder().id(1L).userId(5L).productId(20L).build();
        when(productRepository.findById(20L)).thenReturn(Optional.of(activeProduct));
        when(wishlistRepository.findByUserIdAndProductId(5L, 20L)).thenReturn(Optional.of(existing));
        when(cartService.addItemToCart(eq(5L), any(CartItemRequest.class))).thenReturn(CartResponse.builder().id(3L).build());

        CartResponse res = wishlistService.moveWishlistToCart(5L, 20L);

        assertNotNull(res);
        verify(cartService).addItemToCart(eq(5L), argThat(req -> req.getProductId().equals(20L) && req.getQuantity() == 1));
        verify(wishlistRepository).delete(existing);
    }

    @Test
    void moveWishlistToCart_rejectsWhenOutOfStock() {
        activeProduct.setStockQuantity(0);
        when(productRepository.findById(20L)).thenReturn(Optional.of(activeProduct));

        assertThrows(BadRequestException.class, () -> wishlistService.moveWishlistToCart(5L, 20L));
        verify(cartService, never()).addItemToCart(any(), any());
        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    void moveWishlistToCart_rejectsWhenInactive() {
        activeProduct.setActive(false);
        when(productRepository.findById(20L)).thenReturn(Optional.of(activeProduct));

        assertThrows(BadRequestException.class, () -> wishlistService.moveWishlistToCart(5L, 20L));
        verify(cartService, never()).addItemToCart(any(), any());
    }

    @Test
    void syncGuestWishlist_mergesMissingProducts() {
        when(productRepository.existsById(20L)).thenReturn(true);
        when(wishlistRepository.findByUserIdAndProductId(5L, 20L)).thenReturn(Optional.empty());

        WishlistSyncRequest syncReq = WishlistSyncRequest.builder()
                .productIds(List.of(20L))
                .build();

        wishlistService.syncGuestWishlist(5L, syncReq);

        verify(wishlistRepository).save(any(Wishlist.class));
    }
}
