package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for conversational consultation with the AI Luxury Saree Stylist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * Client browsing session identifier for guest taste continuity
     */
    private String sessionId;

    /**
     * Target product ID when initiating chat from a Product Detail Page (PDP)
     */
    private Long referenceProductId;

    /**
     * Optional quick-filter chips
     */
    private String occasion;
    private String budgetRange;
    private String preferredWeave;
    private String skinUndertone;

    /**
     * Multi-turn chat history within the current session
     */
    private List<StylistChatMessage> conversationHistory;
}
