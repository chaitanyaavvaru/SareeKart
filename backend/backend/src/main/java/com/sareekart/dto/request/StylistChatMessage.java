package com.sareekart.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Single conversational turn message in an AI Stylist chat session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistChatMessage {

    /**
     * Role of the speaker: "user" or "assistant"
     */
    private String role;

    /**
     * Text content of the message
     */
    private String content;

    /**
     * Timestamp of message creation
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
