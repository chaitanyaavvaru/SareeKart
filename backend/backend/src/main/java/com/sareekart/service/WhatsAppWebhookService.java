package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppWebhookDto;
import com.sareekart.entity.*;
import com.sareekart.repository.ConversationRepository;
import com.sareekart.repository.WhatsAppContactRepository;
import com.sareekart.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private final WhatsAppContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppIdempotencyService idempotencyService;
    private final WhatsAppIdentityService identityService;
    private final WhatsAppAiCommerceService aiCommerceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Qualifier("whatsappTaskExecutor")
    private final Executor whatsappTaskExecutor;

    @Transactional
    public void processWebhook(WhatsAppWebhookDto payload) {
        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) return;

        for (WhatsAppWebhookDto.Entry entry : payload.getEntry()) {
            if (entry.getChanges() == null) continue;
            for (WhatsAppWebhookDto.Change change : entry.getChanges()) {
                WhatsAppWebhookDto.Value value = change.getValue();
                if (value == null) continue;
                
                // Process Messages
                if (value.getMessages() != null && !value.getMessages().isEmpty()) {
                    for (WhatsAppWebhookDto.Message msg : value.getMessages()) {
                        WhatsAppWebhookDto.Contact contactInfo = (value.getContacts() != null && !value.getContacts().isEmpty()) 
                                ? value.getContacts().get(0) : null;
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
        String wamId = msg.getId();

        // 1. Check idempotency first (atomic in-memory lock + DB check)
        if (wamId != null && !idempotencyService.tryAcquireLock(wamId)) {
            log.warn("Duplicate WhatsApp message received (wam_id: {}), dropping from processing.", wamId);
            return;
        }

        // 2. Resolve Contact via Identity Service
        String profileName = (contactInfo != null && contactInfo.getProfile() != null) ? contactInfo.getProfile().getName() : "Customer";
        WhatsAppContact contact = identityService.resolveContact(phoneNumber, profileName);

        // 3. Find or create Conversation
        Conversation conversation = conversationRepository
                .findFirstByContactAndStatusNotOrderByUpdatedAtDesc(contact, ConversationStatus.CLOSED)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .contact(contact)
                            .status(ConversationStatus.BOT_HANDLING)
                            .build();
                    return conversationRepository.save(newConv);
                });

        // 4. Prevent duplicate message inserts
        WhatsAppMessage existingMsg = messageRepository.findByWamId(wamId);
        if (existingMsg != null) {
            log.info("Message already exists in repository (wam_id: {}), skipping.", wamId);
            return;
        }

        // 5. Parse Message Content and Type
        LocalDateTime msgTime;
        try {
            msgTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(msg.getTimestamp())), ZoneId.systemDefault());
        } catch (Exception e) {
            msgTime = LocalDateTime.now();
        }

        String content = "";
        MessageType type = MessageType.TEXT;

        if ("text".equals(msg.getType()) && msg.getText() != null) {
            content = msg.getText().getBody() != null ? msg.getText().getBody() : "";
        } else if ("interactive".equals(msg.getType()) && msg.getInteractive() != null) {
            type = MessageType.INTERACTIVE;
            if (msg.getInteractive().getButtonReply() != null) {
                content = msg.getInteractive().getButtonReply().getTitle();
            } else if (msg.getInteractive().getListReply() != null) {
                content = msg.getInteractive().getListReply().getTitle();
            }
        } else if ("image".equals(msg.getType()) && msg.getImage() != null) {
            type = MessageType.IMAGE;
            content = msg.getImage().getCaption() != null ? msg.getImage().getCaption() : "[Image]";
        } else {
            content = "[Unsupported message type: " + msg.getType() + "]";
        }

        WhatsAppMessage savedMsg = WhatsAppMessage.builder()
                .conversation(conversation)
                .wamId(wamId)
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

        log.info("Saved incoming WhatsApp message from {} (convId: {}): {}", phoneNumber, conversation.getId(), content);

        // Broadcast to Admin Dashboard instantly via WebSocket
        messagingTemplate.convertAndSend("/topic/admin/inbox", savedMsg);

        // 6. Trigger AI Commerce response if BOT_HANDLING
        if (conversation.getStatus() == ConversationStatus.BOT_HANDLING && (type == MessageType.TEXT || type == MessageType.INTERACTIVE)) {
            final String finalContent = content;
            final Long convId = conversation.getId();
            final String senderPhone = contact.getPhoneNumber();
            whatsappTaskExecutor.execute(() -> {
                try {
                    aiCommerceService.processIncomingMessage(senderPhone, finalContent, convId);
                } catch (Exception e) {
                    log.error("Error processing AI commerce message for {}", senderPhone, e);
                }
            });
        } else {
            log.info("Conversation {} status is {}, suppressing automated bot response.", conversation.getId(), conversation.getStatus());
        }
    }

    private void processMessageStatus(WhatsAppWebhookDto.Status status) {
        if (status == null || status.getId() == null) return;
        WhatsAppMessage existingMsg = messageRepository.findByWamId(status.getId());
        if (existingMsg != null && status.getStatus() != null) {
            switch (status.getStatus().toLowerCase()) {
                case "sent": existingMsg.setDeliveryStatus(DeliveryStatus.SENT); break;
                case "delivered": existingMsg.setDeliveryStatus(DeliveryStatus.DELIVERED); break;
                case "read": existingMsg.setDeliveryStatus(DeliveryStatus.READ); break;
                case "failed": existingMsg.setDeliveryStatus(DeliveryStatus.FAILED); break;
            }
            messageRepository.save(existingMsg);
            messagingTemplate.convertAndSend("/topic/admin/inbox/status", existingMsg);
        }
    }
}
