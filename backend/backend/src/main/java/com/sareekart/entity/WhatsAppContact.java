package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_contacts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Link to E-commerce user if verified

    /**
     * Whether this contact has opted in to receive WhatsApp messages from SareeKart.
     * Set to {@code false} when the contact sends a STOP/UNSUBSCRIBE/CANCEL/QUIT keyword.
     * Set back to {@code true} when the contact sends a START/SUBSCRIBE/JOIN/YES keyword.
     * Defaults to {@code true} for new contacts (explicit opt-in via chat initiation).
     * All existing contacts are migrated to {@code true} via V32 migration (safe default).
     */
    @Column(name = "opted_in", nullable = false)
    @Builder.Default
    private Boolean optedIn = true;

    @Column(name = "opt_in_updated_at")
    private LocalDateTime optInUpdatedAt;

    public boolean isOptedIn() {
        return Boolean.TRUE.equals(this.optedIn);
    }

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
