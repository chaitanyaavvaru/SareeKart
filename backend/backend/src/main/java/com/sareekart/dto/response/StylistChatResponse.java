package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload returned to the patron from the AI Luxury Saree Stylist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistChatResponse {

    /**
     * Conversational styling prose and handloom narrative
     */
    private String reply;

    /**
     * Verified, grounded saree recommendations with live MySQL pricing and stock
     */
    private List<ScoredProductResponse> recommendedSarees;

    /**
     * Curated contrast blouse, jewelry, and draping technique recommendations
     */
    private DrapeStyleResponse.EnsembleLook primaryLook;

    /**
     * Consultation record ID for direct handoff to Bespoke Tailoring Studio
     */
    private Long consultationId;

    /**
     * True if fallback was triggered due to LLM SLA timeout or offline mode
     */
    private boolean fallbackUsed;

    /**
     * Discovered context attributes
     */
    private String detectedOccasion;
    private String detectedFabric;
    private String detectedBudget;
}
