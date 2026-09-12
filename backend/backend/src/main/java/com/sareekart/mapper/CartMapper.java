package com.sareekart.mapper;

import com.sareekart.dto.response.CartItemResponse;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.entity.Cart;
import com.sareekart.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        BigDecimal price = item.getProduct().getPrice();
        BigDecimal itemTotalPrice = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        String image = null;
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            image = item.getProduct().getImages().get(0);
        }

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(image)
                .price(price)
                .quantity(item.getQuantity())
                .totalPrice(itemTotalPrice)
                .build();
    }
}
