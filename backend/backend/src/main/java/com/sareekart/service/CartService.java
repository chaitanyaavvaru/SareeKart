package com.sareekart.service;

import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItemToCart(Long userId, CartItemRequest request);

    CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity);

    CartResponse removeItemFromCart(Long userId, Long productId);

    void clearCart(Long userId);
}
