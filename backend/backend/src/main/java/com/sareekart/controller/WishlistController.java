package com.sareekart.controller;

import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.wishlist.WishlistResponse;
import com.sareekart.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist APIs")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get user's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        WishlistResponse wishlist = wishlistService.getWishlist(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        WishlistResponse wishlist = wishlistService.addToWishlist(userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlist));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        WishlistResponse wishlist = wishlistService.removeFromWishlist(userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlist));
    }

    @GetMapping("/{productId}/check")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> checkWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        boolean inWishlist = wishlistService.isInWishlist(userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }
}