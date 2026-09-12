package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Double discountPercent;

    @Builder.Default
    private Double discountAmount = 0.0;

    @Builder.Default
    private Double minPurchaseAmount = 0.0;

    @Builder.Default
    private Integer usageLimit = 1000;

    @Builder.Default
    private Integer timesUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime expiryDate;
}
