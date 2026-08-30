package com.sareekart.entity;

import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "orders", uniqueConstraints = {
    @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number")
})
public class Order extends BaseEntity {

    @NotBlank
    @Size(max = 20)
    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "shipping_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Pricing snapshot: code active at purchase time (see V10). */
    @Size(max = 50)
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "shipping_address_json", nullable = false, columnDefinition = "JSON")
    private String shippingAddressJson;

    @Column(name = "billing_address_json", columnDefinition = "JSON")
    private String billingAddressJson;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "shipped_at")
    private java.time.LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private java.time.LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    /** USER or EXPIRED (system sweeper) — audit distinction from V11. */
    @Size(max = 50)
    @Column(name = "cancel_reason", length = 50)
    private String cancelReason;

    /** Exactly-once marker for the automatic pre-fulfillment refund restock (V12). */
    @Column(name = "inventory_restocked", nullable = false)
    private Boolean inventoryRestocked = false;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 10)
    @ToString.Exclude
    private List<Refund> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Payment> payments = new ArrayList<>();

    public boolean canCancel() {
        return status == OrderStatus.PENDING || status == OrderStatus.PROCESSING;
    }

    public boolean canShip() {
        return status == OrderStatus.PROCESSING;
    }

    public boolean canDeliver() {
        return status == OrderStatus.SHIPPED;
    }
}