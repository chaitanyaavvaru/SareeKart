package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    private Long productId;

    @Column(nullable = false)
    private String productName;

    private String category;

    @Column(nullable = false)
    @Builder.Default
    private String warehouseCode = "WH-01";

    @Column(nullable = false)
    @Builder.Default
    private String warehouseName = "Bengaluru Central Fulfillment Hub";

    @Builder.Default
    private String binLocation = "A1-R1-S1";

    @Column(nullable = false)
    @Builder.Default
    private Integer onHand = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reserved = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer available = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String status = "IN_STOCK";

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void recalculateStatus() {
        this.available = Math.max(0, (this.onHand != null ? this.onHand : 0) - (this.reserved != null ? this.reserved : 0));
        if (this.available <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (this.available <= 5) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "IN_STOCK";
        }
    }
}
