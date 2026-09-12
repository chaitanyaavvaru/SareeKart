package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.Notification;
import com.sareekart.entity.User;
import com.sareekart.repository.NotificationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required to view notifications.", null));
        }

        List<Notification> notifications = notificationRepository.findForUserOrRole(
                user.getId(),
                user.getRole() != null ? user.getRole().name() : "CUSTOMER"
        );
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", 0L)));
        }

        long unread = notificationRepository.countUnreadForUserOrRole(
                user.getId(),
                user.getRole() != null ? user.getRole().name() : "CUSTOMER"
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", unread)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required.", null));
        }

        return notificationRepository.findById(id)
                .map(notif -> {
                    notif.setIsRead(true);
                    Notification saved = notificationRepository.save(notif);
                    return ResponseEntity.ok(ApiResponse.success("Marked as read", saved));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Notification not found", null)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required.", null));
        }

        List<Notification> notifications = notificationRepository.findForUserOrRole(
                user.getId(),
                user.getRole() != null ? user.getRole().name() : "CUSTOMER"
        );
        for (Notification n : notifications) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", "SUCCESS"));
    }

    @Data
    public static class TestEventRequest {
        private String title;
        private String message;
        private String type;
        private String linkUrl;
        private String targetRole;
    }

    @PostMapping("/test-event")
    public ResponseEntity<ApiResponse<Notification>> emitTestEvent(
            @RequestBody TestEventRequest req,
            @AuthenticationPrincipal User user) {
        Notification notif = Notification.builder()
                .userId(user != null ? user.getId() : null)
                .targetRole(req.getTargetRole())
                .title(req.getTitle() != null ? req.getTitle() : "Simulated Dispatch Event")
                .message(req.getMessage() != null ? req.getMessage() : "Telemetry pulse: Order dispatched via BlueDart with AWB-SK98273.")
                .type(req.getType() != null ? req.getType() : "ORDER_PLACED")
                .linkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "/orders")
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notif);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Event emitted", saved));
    }
}
