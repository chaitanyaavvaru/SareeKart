package com.sareekart.service;

import com.sareekart.dto.request.WishlistSyncRequest;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.dto.response.ProductResponse;

import java.util.List;

public interface WishlistService {

    List<ProductResponse> getWishlist(Long userId);

    long getWishlistCount(Long userId);

    void addToWishlist(Long userId, Long productId);

    void removeFromWishlist(Long userId, Long productId);

    CartResponse moveWishlistToCart(Long userId, Long productId);

    void syncGuestWishlist(Long userId, WishlistSyncRequest request);
}
