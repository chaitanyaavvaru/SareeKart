package com.sareekart.controller;

import com.sareekart.dto.request.WishlistSyncRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.User;
import com.sareekart.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getWishlist(
            @AuthenticationPrincipal User user) {
        List<ProductResponse> responses = wishlistService.getWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @AuthenticationPrincipal User user) {
        long count = wishlistService.getWishlistCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user) {
        wishlistService.addToWishlist(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", null));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user) {
        wishlistService.removeFromWishlist(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", null));
    }

    @PostMapping("/move-to-cart/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> moveWishlistToCart(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user) {
        CartResponse cartResponse = wishlistService.moveWishlistToCart(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Saree moved to cart successfully", cartResponse));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> syncGuestWishlist(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WishlistSyncRequest request) {
        wishlistService.syncGuestWishlist(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Wishlist synchronized successfully", null));
    }
}
