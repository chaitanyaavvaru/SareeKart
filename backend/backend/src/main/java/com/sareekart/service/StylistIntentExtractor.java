package com.sareekart.service;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.StylistChatRequest;

/**
 * Phase 9: Extracts structured shopping and styling intent from natural-language queries.
 */
public interface StylistIntentExtractor {

    /**
     * Parses the customer message and session context into structured criteria.
     *
     * @param message Natural language query from patron
     * @param request Contextual request parameters (chips, history, reference product)
     * @return Discovered StylistIntent with price bounds, occasion, fabric, color, and query type
     */
    StylistIntent extractIntent(String message, StylistChatRequest request);
}
