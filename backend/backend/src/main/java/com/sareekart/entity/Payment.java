package com.sareekart.entity;

import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @NotBlank
    @Size(max = 20)
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Size(max = 100)
    @Column(name = "provider_order_id", length = 100)
    private String providerOrderId;

    @Size(max = 100)
    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "raw_response", columnDefinition = "JSON")
    private String rawResponse;

    /** Audit trail of status transitions: [{"from":..,"to":..,"at":..,"by":"VERIFY"|"WEBHOOK"}] */
    @Column(name = "status_transition_history", columnDefinition = "JSON")
    private String statusTransitionHistory;
}