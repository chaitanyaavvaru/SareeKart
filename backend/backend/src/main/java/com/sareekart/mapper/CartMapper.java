package com.sareekart.mapper;

import com.sareekart.dto.response.CartItemResponse;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.entity.Cart;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = cart.getItems() != null
                ? cart.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : Collections.emptyList();

        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        boolean hasStockIssues = itemResponses.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getIsOutOfStock())
                               || Boolean.TRUE.equals(item.getQuantityExceedsStock())
                               || !Boolean.TRUE.equals(item.getIsActive()));

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemResponses)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .hasStockIssues(hasStockIssues)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        Product product = item.getProduct();
        BigDecimal price = (product != null && product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal itemTotalPrice = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        String image = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            image = product.getImages().get(0);
        }

        int availableStock = (product != null && product.getStockQuantity() != null) ? product.getStockQuantity() : 0;
        boolean isActive = product != null && Boolean.TRUE.equals(product.getActive());
        boolean isOutOfStock = availableStock <= 0;
        boolean quantityExceedsStock = item.getQuantity() > availableStock;

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : "Unknown Product")
                .productImage(image)
                .price(price)
                .quantity(item.getQuantity())
                .totalPrice(itemTotalPrice)
                .availableStock(availableStock)
                .isActive(isActive)
                .isOutOfStock(isOutOfStock)
                .quantityExceedsStock(quantityExceedsStock)
                .build();
    }
}
