package com.sareekart.mapper;

import com.sareekart.dto.response.OrderItemResponse;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .walletCreditUsed(order.getWalletCreditUsed() != null ? order.getWalletCreditUsed() : BigDecimal.ZERO)
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .trackingNumber(order.getTrackingNumber())
                .courierPartner(order.getCourierPartner())
                .currentLocation(order.getCurrentLocation())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        BigDecimal price = item.getPrice();
        BigDecimal itemTotalPrice = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        String image = null;
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            image = item.getProduct().getImages().get(0);
        }

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(image)
                .quantity(item.getQuantity())
                .price(price)
                .totalPrice(itemTotalPrice)
                .build();
    }
}
