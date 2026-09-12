package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    public static final String STATUS_PENDING = "Pending owner approval";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // PRODUCT_PRICE, INVENTORY_STOCK, COUPON_CREATE, COUPON_DELETE, EXCEL_BATCH_IMPORT

    private String targetEntityId;

    @Column(nullable = false)
    private String action; // PRICE_CHANGE, STOCK_ADJUSTMENT, COUPON_DEACTIVATE, BATCH_IMPORT

    @Column(columnDefinition = "TEXT")
    private String previousValue;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestedValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(nullable = false)
    private Long requestedByUserId;

    @Column(nullable = false)
    private String requestedByEmail;

    private Long reviewedByUserId;

    private String reviewedByEmail;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
