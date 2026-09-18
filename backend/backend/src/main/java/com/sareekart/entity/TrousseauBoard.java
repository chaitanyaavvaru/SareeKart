package com.sareekart.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "trousseau_boards",
    indexes = {
        @Index(name = "idx_trousseau_user", columnList = "user_id"),
        @Index(name = "idx_trousseau_token", columnList = "share_token")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TrousseauBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @Column(name = "is_public_voting", nullable = false)
    @Builder.Default
    private Boolean isPublicVoting = true;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrousseauCeremony> ceremonies = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrousseauCollaborator> collaborators = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.isPublicVoting == null) {
            this.isPublicVoting = true;
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addCeremony(TrousseauCeremony ceremony) {
        ceremonies.add(ceremony);
        ceremony.setBoard(this);
        if (ceremony.getItems() != null) {
            for (TrousseauItem item : ceremony.getItems()) {
                item.setBoard(this);
            }
        }
    }

    public void removeCeremony(TrousseauCeremony ceremony) {
        ceremonies.remove(ceremony);
        ceremony.setBoard(null);
    }

    public void addCollaborator(TrousseauCollaborator collaborator) {
        collaborators.add(collaborator);
        collaborator.setBoard(this);
    }

    public void removeCollaborator(TrousseauCollaborator collaborator) {
        collaborators.remove(collaborator);
        collaborator.setBoard(null);
    }
}
