package com.sareekart.dto.whatsapp;

import com.sareekart.dto.request.StylistChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-turn structured conversation state with 30-minute sliding window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppSessionContext {

    public WhatsAppSessionContext(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.recentTurns = new ArrayList<>();
        this.lastActivityTime = System.currentTimeMillis();
    }

    private String phoneNumber;
    private Long conversationId;

    // Structured shopping attributes
    private String activeOccasion;
    private String preferredColor;
    private String colorFamily;
    private BigDecimal maxBudget;
    private BigDecimal minBudget;
    private String preferredFabric;
    private Long lastReferencedProductId;
    private Long activeConsultationId;

    // Compact history limited to last 4 turns
    @Builder.Default
    private List<StylistChatMessage> recentTurns = new ArrayList<>();

    @Builder.Default
    private long lastActivityTime = System.currentTimeMillis();
}
