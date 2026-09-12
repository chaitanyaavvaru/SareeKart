package com.sareekart.entity;

import com.sareekart.enums.LogisticsZone;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "pincode_overrides",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pincode_overrides_pincode", columnNames = {"pincode"})
    },
    indexes = {
        @Index(name = "idx_pincode_overrides_pin", columnList = "pincode"),
        @Index(name = "idx_pincode_overrides_zone", columnList = "zone")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PincodeOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String pincode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LogisticsZone zone = LogisticsZone.TIER_1;

    @Column(name = "is_serviceable", nullable = false)
    @Builder.Default
    private Boolean serviceable = true;

    @Column(name = "is_cod_available", nullable = false)
    @Builder.Default
    private Boolean codAvailable = true;

    @Column(name = "courier_partner", nullable = false, length = 100)
    @Builder.Default
    private String courierPartner = "Blue Dart Apex Air";

    @Column(name = "transit_days", nullable = false)
    @Builder.Default
    private Integer transitDays = 3;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.serviceable == null) this.serviceable = true;
        if (this.codAvailable == null) this.codAvailable = true;
        if (this.transitDays == null) this.transitDays = 3;
    }
}
