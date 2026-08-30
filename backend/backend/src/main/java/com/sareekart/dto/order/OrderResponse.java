package com.sareekart.dto.order;

import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import com.sareekart.entity.Refund;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal totalAmount;
    private AddressSnapshot shippingAddress;
    private AddressSnapshot billingAddress;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String notes;
    private List<OrderItemResponse> items;
    /** Customer-visible refund lines (no gateway/internal identifiers). */
    private List<RefundLine> refunds;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        if (order == null) return null;

        List<OrderItemResponse> itemResponses = order.getItems() != null
            ? order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList())
            : List.of();

        return OrderResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
            .status(order.getStatus())
            .subtotal(order.getSubtotal())
            .shippingAmount(order.getShippingAmount())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .couponCode(order.getCouponCode())
            .totalAmount(order.getTotalAmount())
            .shippingAddress(AddressSnapshot.fromJson(order.getShippingAddressJson()))
            .billingAddress(AddressSnapshot.fromJson(order.getBillingAddressJson()))
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .notes(order.getNotes())
            .items(itemResponses)
            .refunds(order.getRefunds() == null ? List.of()
                : order.getRefunds().stream()
                    .map(RefundLine::from)
                    .collect(Collectors.toList()))
            .shippedAt(order.getShippedAt())
            .deliveredAt(order.getDeliveredAt())
            .cancelledAt(order.getCancelledAt())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }

    public static List<OrderResponse> fromList(List<Order> orders) {
        return orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        /**
         * Unit price. Exposed as "price" to match the frontend order-detail modal.
         */
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;

        public static OrderItemResponse from(OrderItem item) {
            if (item == null) return null;
            return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .price(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .build();
        }
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundLine {
        private BigDecimal amount;
        private String status;   // PENDING | SUCCESS | FAILED (refund-row status)
        private String reasonCode; // structured code; customer-safe
        private LocalDateTime createdAt;

        public static RefundLine from(Refund r) {
            if (r == null) return null;
            return RefundLine.builder()
                .amount(r.getAmount())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .reasonCode(r.getReasonCode() != null ? r.getReasonCode().name() : "OTHER") // legacy rows
                .createdAt(r.getCreatedAt())
                .build();
        }
    }
}
