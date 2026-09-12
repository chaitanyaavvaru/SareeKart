package com.sareekart.service.impl;

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
import com.sareekart.service.CartService;
import com.sareekart.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist(Long userId) {
        List<Wishlist> entries = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = entries.stream()
                .map(Wishlist::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    @Override
    public void addToWishlist(Long userId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isEmpty()) {
            Wishlist entry = Wishlist.builder()
                    .userId(userId)
                    .productId(productId)
                    .build();
            wishlistRepository.save(entry);
        }
    }

    @Override
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistRepository::delete);
    }

    @Override
    public CartResponse moveWishlistToCart(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("This saree is no longer active: " + product.getName());
        }

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (stock <= 0) {
            throw new BadRequestException("This saree is currently out of stock: " + product.getName());
        }

        // Add 1 unit to cart
        CartItemRequest cartReq = CartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();
        CartResponse cartResponse = cartService.addItemToCart(userId, cartReq);

        // Remove from wishlist upon successful addition to cart
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistRepository::delete);

        return cartResponse;
    }

    @Override
    public void syncGuestWishlist(Long userId, WishlistSyncRequest request) {
        if (request == null || request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return;
        }

        for (Long productId : request.getProductIds()) {
            if (productId == null) {
                continue;
            }
            if (productRepository.existsById(productId)) {
                if (wishlistRepository.findByUserIdAndProductId(userId, productId).isEmpty()) {
                    Wishlist entry = Wishlist.builder()
                            .userId(userId)
                            .productId(productId)
                            .build();
                    wishlistRepository.save(entry);
                }
            }
        }
    }
}
