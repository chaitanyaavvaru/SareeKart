package com.sareekart.controller;

import com.sareekart.entity.Conversation;
import com.sareekart.entity.DeliveryStatus;
import com.sareekart.entity.MessageType;
import com.sareekart.entity.SenderType;
import com.sareekart.entity.WhatsAppMessage;
import com.sareekart.repository.ConversationRepository;
import com.sareekart.repository.WhatsAppMessageRepository;
import com.sareekart.service.WhatsAppApiClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final WhatsAppApiClient whatsAppApiClient;
    private final ConversationRepository conversationRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Data
    public static class AdminMessagePayload {
        private Long conversationId;
        private String content;
    }

    @MessageMapping("/chat.sendMessage")
    @Transactional
    public void sendAdminMessage(@Payload AdminMessagePayload payload) {
        log.info("Admin sending message to conversation {}: {}", payload.getConversationId(), payload.getContent());

        Conversation conversation = conversationRepository.findById(payload.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid conversation ID"));

        // Save to DB first
        WhatsAppMessage adminMsg = WhatsAppMessage.builder()
                .conversation(conversation)
                .wamId("admin_" + System.currentTimeMillis()) // Fake ID until Meta gives actual ID if we wanted, but not strictly needed for outbound
                .senderType(SenderType.ADMIN)
                .messageType(MessageType.TEXT)
                .content(payload.getContent())
                .deliveryStatus(DeliveryStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(adminMsg);
        
        conversation.setLastMessageAt(adminMsg.getTimestamp());
        conversationRepository.save(conversation);

        // Send via WhatsApp API
        whatsAppApiClient.sendTextMessage(conversation.getContact().getPhoneNumber(), payload.getContent());

        // Broadcast to all admins so their screens update instantly
        messagingTemplate.convertAndSend("/topic/admin/inbox", adminMsg);
    }
}
