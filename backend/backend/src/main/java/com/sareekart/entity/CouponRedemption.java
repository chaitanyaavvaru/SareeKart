package com.sareekart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Reservation ledger for coupon usage.
 *
 * Lifecycle: inserted at ORDER PLACEMENT (holds quota atomically via the
 * UNIQUE(order_id) constraint), flagged redeemed=true when the order's
 * payment commits (PAID / COD creation), deleted when an uncommitted order
 * fails payment or is cancelled. Limits count every existing row — a held
 * reservation blocks other checkouts from overselling the same quota.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "coupon_redemptions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_coupon_redemptions_order", columnNames = "order_id")
})
public class CouponRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    @ToString.Exclude
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @ToString.Exclude
    private Order order;

    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;

    /** True once the order committed (payment PAID or COD placement). */
    @Column(name = "redeemed", nullable = false)
    private Boolean redeemed = false;
}