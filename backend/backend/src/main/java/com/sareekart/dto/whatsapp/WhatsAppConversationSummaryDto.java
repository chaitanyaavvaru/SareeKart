package com.sareekart.dto.whatsapp;

import com.sareekart.entity.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppConversationSummaryDto {
    private Long id;
    private Long contactId;
    private String contactPhone;
    private String contactName;
    private Long linkedUserId;
    private String linkedUserName;
    private ConversationStatus status;
    private String tags;
    private LocalDateTime lastMessageAt;
    private String lastMessageSnippet;
    private int messageCount;
}
