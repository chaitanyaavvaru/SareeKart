package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppSessionContext;

/**
 * Manages 30-minute sliding window conversation session states and structured customer preferences.
 */
public interface WhatsAppConversationStateManager {

    /**
     * Retrieves active session context for the phone number, or creates a new one if expired.
     */
    WhatsAppSessionContext getSession(String phoneNumber);

    /**
     * Updates and saves the session context, resetting the 30-minute idle TTL.
     */
    void updateSession(String phoneNumber, WhatsAppSessionContext context);

    /**
     * Clears session context on explicit customer reset or opt-out.
     */
    void clearSession(String phoneNumber);
}
