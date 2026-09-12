package com.sareekart.entity;

import com.sareekart.enums.RefundMode;
import com.sareekart.enums.ReturnReason;
import com.sareekart.enums.ReturnStatus;
import com.sareekart.enums.ReturnType;
import com.sareekart.util.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "return_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_return_requests_order", columnNames = {"order_id"})
    },
    indexes = {
        @Index(name = "idx_return_requests_user", columnList = "user_id"),
        @Index(name = "idx_return_requests_order", columnList = "order_id"),
        @Index(name = "idx_return_requests_status", columnList = "status"),
        @Index(name = "idx_return_requests_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnType type = ReturnType.RETURN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnReason reason;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    @Convert(converter = StringListConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_mode", length = 50)
    private RefundMode refundMode;

    @Column(name = "exchange_sku", length = 100)
    private String exchangeSku;

    @Column(name = "reverse_courier", length = 100)
    private String reverseCourier;

    @Column(name = "reverse_tracking_number", length = 100)
    private String reverseTrackingNumber;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Convenience accessors
    public Long getOrderId() {
        return order != null ? order.getId() : null;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    // Overloaded setters for flexibility
    public void setStatus(String statusStr) {
        if (statusStr == null) {
            this.status = null;
        } else {
            try {
                this.status = ReturnStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (Exception e) {
                this.status = ReturnStatus.PENDING;
            }
        }
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public void setType(String typeStr) {
        if (typeStr == null) {
            this.type = ReturnType.RETURN;
        } else {
            try {
                this.type = ReturnType.valueOf(typeStr.trim().toUpperCase());
            } catch (Exception e) {
                this.type = ReturnType.RETURN;
            }
        }
    }

    public void setType(ReturnType type) {
        this.type = type;
    }

    public void setReason(String reasonStr) {
        if (reasonStr == null) {
            this.reason = null;
        } else {
            try {
                this.reason = ReturnReason.valueOf(reasonStr.trim().toUpperCase());
            } catch (Exception e) {
                this.reason = ReturnReason.OTHER;
            }
        }
    }

    public void setReason(ReturnReason reason) {
        this.reason = reason;
    }

    public void setRefundMode(String refundModeStr) {
        if (refundModeStr == null) {
            this.refundMode = null;
        } else {
            try {
                this.refundMode = RefundMode.valueOf(refundModeStr.trim().toUpperCase());
            } catch (Exception e) {
                this.refundMode = RefundMode.ORIGINAL_PAYMENT;
            }
        }
    }

    public void setRefundMode(RefundMode refundMode) {
        this.refundMode = refundMode;
    }

    // Defensive lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReturnStatus.PENDING;
        }
        if (this.type == null) {
            this.type = ReturnType.RETURN;
        }
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Custom Builder methods to support String arguments
    public static class ReturnRequestBuilder {
        public ReturnRequestBuilder status(String statusStr) {
            if (statusStr != null) {
                try {
                    this.status$value = ReturnStatus.valueOf(statusStr.trim().toUpperCase());
                    this.status$set = true;
                } catch (Exception e) {
                    this.status$value = ReturnStatus.PENDING;
                    this.status$set = true;
                }
            }
            return this;
        }

        public ReturnRequestBuilder status(ReturnStatus status) {
            this.status$value = status;
            this.status$set = true;
            return this;
        }

        public ReturnRequestBuilder type(String typeStr) {
            if (typeStr != null) {
                try {
                    this.type$value = ReturnType.valueOf(typeStr.trim().toUpperCase());
                    this.type$set = true;
                } catch (Exception e) {
                    this.type$value = ReturnType.RETURN;
                    this.type$set = true;
                }
            }
            return this;
        }

        public ReturnRequestBuilder type(ReturnType type) {
            this.type$value = type;
            this.type$set = true;
            return this;
        }

        public ReturnRequestBuilder reason(String reasonStr) {
            if (reasonStr != null) {
                try {
                    this.reason = ReturnReason.valueOf(reasonStr.trim().toUpperCase());
                } catch (Exception e) {
                    this.reason = ReturnReason.OTHER;
                }
            }
            return this;
        }

        public ReturnRequestBuilder reason(ReturnReason reason) {
            this.reason = reason;
            return this;
        }

        public ReturnRequestBuilder refundMode(String refundModeStr) {
            if (refundModeStr != null) {
                try {
                    this.refundMode = RefundMode.valueOf(refundModeStr.trim().toUpperCase());
                } catch (Exception e) {
                    this.refundMode = RefundMode.ORIGINAL_PAYMENT;
                }
            }
            return this;
        }

        public ReturnRequestBuilder refundMode(RefundMode refundMode) {
            this.refundMode = refundMode;
            return this;
        }
    }
}
