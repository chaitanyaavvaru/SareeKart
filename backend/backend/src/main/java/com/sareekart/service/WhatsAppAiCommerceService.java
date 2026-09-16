package com.sareekart.service;

/**
 * Phase 10: Conversational AI Commerce Service for WhatsApp.
 * Orchestrates intent detection, commerce tools, Phase 8/9 engines, and outbound responses.
 */
public interface WhatsAppAiCommerceService {

    /**
     * Processes an incoming WhatsApp text message and dispatches a grounded response.
     *
     * @param phoneNumber Normalized sender phone number
     * @param messageContent Text message body from customer
     * @param conversationId Active conversation identifier
     */
    void processIncomingMessage(String phoneNumber, String messageContent, Long conversationId);
}
