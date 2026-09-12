package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ai_style_consultations",
    indexes = {
        @Index(name = "idx_ai_style_user", columnList = "user_id"),
        @Index(name = "idx_ai_style_product", columnList = "product_id"),
        @Index(name = "idx_ai_style_occasion", columnList = "occasion"),
        @Index(name = "idx_ai_style_converted", columnList = "converted_to_tailoring"),
        @Index(name = "idx_ai_style_created", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiStyleConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "saree_name", nullable = false)
    private String sareeName;

    @Column(length = 100)
    private String fabric;

    @Column(name = "primary_color", length = 100)
    private String primaryColor;

    @Column(length = 100)
    private String occasion;

    @Column(name = "chosen_look_title")
    private String chosenLookTitle;

    @Column(name = "contrast_color", length = 100)
    private String contrastColor;

    @Column(name = "blouse_style", length = 100)
    private String blouseStyle;

    @Column(name = "jewelry_recommendation")
    private String jewelryRecommendation;

    @Column(name = "converted_to_tailoring", nullable = false)
    @Builder.Default
    private Boolean convertedToTailoring = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.convertedToTailoring == null) {
            this.convertedToTailoring = false;
        }
    }
}
