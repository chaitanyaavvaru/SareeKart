package com.sareekart.dto.wishlist;

import com.sareekart.dto.product.ProductResponse;
import com.sareekart.entity.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wishlist payload. Items are full product DTOs whose `id` equals the
 * product id — the storefront collects wishlisted ids via
 * `(data || []).map(w => w.id)` and renders cards directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private List<ProductResponse> items;
    private int totalItems;

    public static WishlistResponse from(List<Wishlist> wishlistItems) {
        if (wishlistItems == null || wishlistItems.isEmpty()) {
            return WishlistResponse.builder()
                .items(List.of())
                .totalItems(0)
                .build();
        }

        List<ProductResponse> items = wishlistItems.stream()
            .filter(w -> w.getProduct() != null)
            .map(w -> ProductResponse.from(w.getProduct()))
            .collect(Collectors.toList());

        return WishlistResponse.builder()
            .items(items)
            .totalItems(items.size())
            .build();
    }
}