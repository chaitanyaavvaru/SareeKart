package com.sareekart.dto.cart;

import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import com.sareekart.entity.ProductImage;
import com.sareekart.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;

    public static CartResponse from(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return CartResponse.builder()
                .items(List.of())
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .build();
        }

        List<CartItemResponse> itemResponses = cartItems.stream()
            .map(CartItemResponse::from)
            .collect(Collectors.toList());

        int totalItems = cartItems.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();

        BigDecimal subtotal = cartItems.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
            .items(itemResponses)
            .totalItems(totalItems)
            .subtotal(subtotal)
            .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private Long variantId;
        private String variantSize;
        private String variantColor;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static CartItemResponse from(CartItem cartItem) {
            if (cartItem == null) return null;
            
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            
            String imageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(product.getImages().get(0).getUrl());
            }

            return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productImage(imageUrl)
                .variantId(variant != null ? variant.getId() : null)
                .variantSize(variant != null ? variant.getSize() : null)
                .variantColor(variant != null ? variant.getColor() : null)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .build();
        }
    }
}