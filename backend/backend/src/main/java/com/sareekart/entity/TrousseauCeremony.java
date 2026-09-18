package com.sareekart.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "trousseau_ceremonies",
    indexes = {
        @Index(name = "idx_ceremony_board", columnList = "board_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TrousseauCeremony {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    private TrousseauBoard board;

    @Column(name = "ceremony_type", nullable = false, length = 50)
    private String ceremonyType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "color_theme", length = 100)
    private String colorTheme;

    @Column(name = "target_budget", precision = 10, scale = 2)
    private BigDecimal targetBudget;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "ceremony", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrousseauItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
    }

    public void addItem(TrousseauItem item) {
        items.add(item);
        item.setCeremony(this);
        if (this.board != null) {
            item.setBoard(this.board);
        }
    }

    public void removeItem(TrousseauItem item) {
        items.remove(item);
        item.setCeremony(null);
    }
}
