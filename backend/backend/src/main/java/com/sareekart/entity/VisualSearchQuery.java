package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visual_search_queries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualSearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "extracted_primary_color", nullable = false, length = 50)
    private String extractedPrimaryColor;

    @Column(name = "extracted_secondary_color", length = 50)
    private String extractedSecondaryColor;

    @Column(name = "extracted_weave_type", length = 100)
    private String extractedWeaveType;

    @Column(name = "top_matched_product_id")
    private Long topMatchedProductId;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal confidenceScore = new BigDecimal("95.00");

    @Column(name = "execution_time_ms", nullable = false)
    @Builder.Default
    private Integer executionTimeMs = 15;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
