package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppWebhookDto;
import com.sareekart.entity.*;
import com.sareekart.repository.ConversationRepository;
import com.sareekart.repository.WhatsAppContactRepository;
import com.sareekart.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private final WhatsAppContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final AIChatbotService aiChatbotService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processWebhook(WhatsAppWebhookDto payload) {
        if (payload.getEntry() == null || payload.getEntry().isEmpty()) return;

        for (WhatsAppWebhookDto.Entry entry : payload.getEntry()) {
            for (WhatsAppWebhookDto.Change change : entry.getChanges()) {
                WhatsAppWebhookDto.Value value = change.getValue();
                
                // Process Messages
                if (value.getMessages() != null && !value.getMessages().isEmpty()) {
                    for (WhatsAppWebhookDto.Message msg : value.getMessages()) {
                        WhatsAppWebhookDto.Contact contactInfo = value.getContacts() != null ? value.getContacts().get(0) : null;
                        processIncomingMessage(msg, contactInfo);
                    }
                }

                // Process Status Updates (Read Receipts, Delivery, etc.)
                if (value.getStatuses() != null && !value.getStatuses().isEmpty()) {
                    for (WhatsAppWebhookDto.Status status : value.getStatuses()) {
                        processMessageStatus(status);
                    }
                }
            }
        }
    }

    private void processIncomingMessage(WhatsAppWebhookDto.Message msg, WhatsAppWebhookDto.Contact contactInfo) {
        String phoneNumber = msg.getFrom();
        
        // 1. Find or create Contact
        WhatsAppContact contact = contactRepository.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            WhatsAppContact newContact = WhatsAppContact.builder()
                    .phoneNumber(phoneNumber)
                    .name(contactInfo != null && contactInfo.getProfile() != null ? contactInfo.getProfile().getName() : "Unknown")
                    .build();
            return contactRepository.save(newContact);
        });

        // 2. Find or create Conversation
        Conversation conversation = conversationRepository
                .findFirstByContactAndStatusNotOrderByUpdatedAtDesc(contact, ConversationStatus.CLOSED)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .contact(contact)
                            .status(ConversationStatus.BOT_HANDLING)
                            .build();
                    return conversationRepository.save(newConv);
                });

        // 3. Prevent duplicate message inserts
        WhatsAppMessage existingMsg = messageRepository.findByWamId(msg.getId());
        if (existingMsg != null) return;

        // 4. Save Message
        LocalDateTime msgTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(msg.getTimestamp())), ZoneId.systemDefault());
        
        String content = "";
        MessageType type = MessageType.TEXT;
        
        if ("text".equals(msg.getType())) {
            content = msg.getText().getBody();
        } else if ("image".equals(msg.getType())) {
            type = MessageType.IMAGE;
            content = msg.getImage().getCaption();
            // TODO: Fetch media URL using media ID from Meta if needed
        } else if ("interactive".equals(msg.getType())) {
            type = MessageType.INTERACTIVE;
            if (msg.getInteractive().getButtonReply() != null) {
                content = msg.getInteractive().getButtonReply().getTitle();
            } else if (msg.getInteractive().getListReply() != null) {
                content = msg.getInteractive().getListReply().getTitle();
            }
        } else {
            content = "[Unsupported message type: " + msg.getType() + "]";
        }

        WhatsAppMessage savedMsg = WhatsAppMessage.builder()
                .conversation(conversation)
                .wamId(msg.getId())
                .senderType(SenderType.CUSTOMER)
                .messageType(type)
                .content(content)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .timestamp(msgTime)
                .build();
        
        messageRepository.save(savedMsg);

        // Update conversation last message time
        conversation.setLastMessageAt(msgTime);
        conversationRepository.save(conversation);

        log.info("Saved incoming message from {}: {}", phoneNumber, content);
        
        // Broadcast to Admin Dashboard instantly
        messagingTemplate.convertAndSend("/topic/admin/inbox", savedMsg);

        // Fire event to trigger AI response if BOT_HANDLING
        if (conversation.getStatus() == ConversationStatus.BOT_HANDLING && type == MessageType.TEXT) {
            final String finalContent = content;
            // Run asynchronously to avoid blocking the webhook thread
            new Thread(() -> aiChatbotService.handleIncomingMessage(phoneNumber, finalContent)).start();
        }
    }

    private void processMessageStatus(WhatsAppWebhookDto.Status status) {
        WhatsAppMessage existingMsg = messageRepository.findByWamId(status.getId());
        if (existingMsg != null) {
            switch (status.getStatus().toLowerCase()) {
                case "sent": existingMsg.setDeliveryStatus(DeliveryStatus.SENT); break;
                case "delivered": existingMsg.setDeliveryStatus(DeliveryStatus.DELIVERED); break;
                case "read": existingMsg.setDeliveryStatus(DeliveryStatus.READ); break;
                case "failed": existingMsg.setDeliveryStatus(DeliveryStatus.FAILED); break;
            }
            messageRepository.save(existingMsg);
            
            // TODO: Broadcast status update via WebSocket
        }
    }
}
