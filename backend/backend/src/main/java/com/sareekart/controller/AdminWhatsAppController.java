package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.whatsapp.WhatsAppDispatchSimulationRequest;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppTelemetryResponse;
import com.sareekart.entity.User;
import com.sareekart.service.WhatsAppNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/whatsapp")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
public class AdminWhatsAppController {

    private final WhatsAppNotificationService whatsAppNotificationService;
    private final com.sareekart.repository.ConversationRepository conversationRepository;
    private final com.sareekart.repository.WhatsAppMessageRepository messageRepository;
    private final com.sareekart.service.WhatsAppApiClient whatsAppApiClient;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<WhatsAppNotificationResponse>>> getAllLogs(
            @RequestParam(required = false, defaultValue = "ALL") String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Staff requesting WhatsApp dispatch audit logs with filter: {}", eventType);
        Pageable pageable = PageRequest.of(page, size);
        Page<WhatsAppNotificationResponse> logs = whatsAppNotificationService.getAllLogs(eventType, pageable);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp logs retrieved successfully", logs));
    }

    @GetMapping("/telemetry")
    public ResponseEntity<ApiResponse<WhatsAppTelemetryResponse>> getTelemetry() {
        log.info("Staff requesting WhatsApp dispatch telemetry");
        WhatsAppTelemetryResponse telemetry = whatsAppNotificationService.getTelemetry();
        return ResponseEntity.ok(ApiResponse.success("WhatsApp dispatch telemetry retrieved successfully", telemetry));
    }

    @PostMapping("/simulate-dispatch")
    public ResponseEntity<ApiResponse<WhatsAppNotificationResponse>> simulateDispatch(
            @Valid @RequestBody WhatsAppDispatchSimulationRequest request,
            @AuthenticationPrincipal User staff) {
        log.info("Staff {} simulating WhatsApp dispatch for event {} to {}",
                staff != null ? staff.getEmail() : "anonymous", request.getEventType(), request.getRecipientPhone());
        WhatsAppNotificationResponse response = whatsAppNotificationService.simulateManualDispatch(request, staff);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp dispatch simulated successfully", response));
    }

    @PostMapping("/resend/{logId}")
    public ResponseEntity<ApiResponse<WhatsAppNotificationResponse>> resendNotification(
            @PathVariable Long logId,
            @AuthenticationPrincipal User staff) {
        log.info("Staff {} requesting resend for WhatsApp log #{}", staff != null ? staff.getEmail() : "anonymous", logId);
        WhatsAppNotificationResponse response = whatsAppNotificationService.resendNotification(logId, staff);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp notification resent successfully", response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<java.util.List<com.sareekart.dto.whatsapp.WhatsAppConversationSummaryDto>>> getConversations(
            @RequestParam(required = false) String status) {
        log.info("Staff requesting WhatsApp conversations with status filter: {}", status);
        java.util.List<com.sareekart.entity.Conversation> conversations;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                com.sareekart.entity.ConversationStatus convStatus = com.sareekart.entity.ConversationStatus.valueOf(status.toUpperCase());
                conversations = conversationRepository.findByStatusOrderByLastMessageAtDesc(convStatus);
            } catch (IllegalArgumentException e) {
                conversations = conversationRepository.findAllByOrderByLastMessageAtDesc();
            }
        } else {
            conversations = conversationRepository.findAllByOrderByLastMessageAtDesc();
        }

        java.util.List<com.sareekart.dto.whatsapp.WhatsAppConversationSummaryDto> dtos = conversations.stream().map(c -> {
            String lastSnippet = "";
            int msgCount = 0;
            if (c.getMessages() != null && !c.getMessages().isEmpty()) {
                msgCount = c.getMessages().size();
                com.sareekart.entity.WhatsAppMessage lastMsg = c.getMessages().get(c.getMessages().size() - 1);
                lastSnippet = lastMsg.getContent();
            }
            com.sareekart.entity.WhatsAppContact contact = c.getContact();
            User user = contact != null ? contact.getUser() : null;

            return com.sareekart.dto.whatsapp.WhatsAppConversationSummaryDto.builder()
                    .id(c.getId())
                    .contactId(contact != null ? contact.getId() : null)
                    .contactPhone(contact != null ? contact.getPhoneNumber() : "")
                    .contactName(contact != null ? contact.getName() : "Customer")
                    .linkedUserId(user != null ? user.getId() : null)
                    .linkedUserName(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : null)
                    .status(c.getStatus())
                    .tags(c.getTags())
                    .lastMessageAt(c.getLastMessageAt() != null ? c.getLastMessageAt() : c.getUpdatedAt())
                    .lastMessageSnippet(lastSnippet)
                    .messageCount(msgCount)
                    .build();
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("WhatsApp conversations retrieved successfully", dtos));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<java.util.List<com.sareekart.entity.WhatsAppMessage>>> getConversationMessages(@PathVariable Long id) {
        log.info("Staff fetching messages for conversation #{}", id);
        java.util.List<com.sareekart.entity.WhatsAppMessage> messages = messageRepository.findByConversationIdOrderByTimestampAsc(id);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    @PutMapping("/conversations/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateConversationStatus(
            @PathVariable Long id,
            @RequestParam com.sareekart.entity.ConversationStatus status) {
        log.info("Staff updating conversation #{} status to {}", id, status);
        com.sareekart.entity.Conversation conv = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found with ID: " + id));
        conv.setStatus(status);
        if (status == com.sareekart.entity.ConversationStatus.BOT_HANDLING) {
            conv.setTags(null);
        }
        conversationRepository.save(conv);
        return ResponseEntity.ok(ApiResponse.success("Conversation status updated successfully", null));
    }

    @PostMapping("/conversations/{id}/reply")
    public ResponseEntity<ApiResponse<com.sareekart.entity.WhatsAppMessage>> replyToConversation(
            @PathVariable Long id,
            @Valid @RequestBody com.sareekart.dto.whatsapp.WhatsAppAdminReplyRequest replyRequest,
            @AuthenticationPrincipal User staff) {
        log.info("Staff {} sending reply to conversation #{}: {}", 
                staff != null ? staff.getEmail() : "admin", id, replyRequest.getContent());

        com.sareekart.entity.Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found with ID: " + id));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.sareekart.entity.WhatsAppMessage adminMsg = com.sareekart.entity.WhatsAppMessage.builder()
                .conversation(conversation)
                .wamId("admin_" + System.currentTimeMillis())
                .senderType(com.sareekart.entity.SenderType.ADMIN)
                .messageType(com.sareekart.entity.MessageType.TEXT)
                .content(replyRequest.getContent().trim())
                .deliveryStatus(com.sareekart.entity.DeliveryStatus.SENT)
                .timestamp(now)
                .build();

        adminMsg = messageRepository.save(adminMsg);

        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        // Send outbound text via Meta WhatsApp API
        String phone = conversation.getContact() != null ? conversation.getContact().getPhoneNumber() : "";
        if (!phone.isBlank()) {
            whatsAppApiClient.sendTextMessage(phone, replyRequest.getContent().trim());
        }

        // Broadcast to WebSocket subscribers so all staff screens update
        messagingTemplate.convertAndSend("/topic/admin/inbox", adminMsg);

        return ResponseEntity.ok(ApiResponse.success("Reply dispatched to customer successfully", adminMsg));
    }
}
