package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppOptInRequest;
import com.sareekart.entity.User;
import com.sareekart.service.WhatsAppNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whatsapp")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationController {

    private final WhatsAppNotificationService whatsAppNotificationService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<WhatsAppNotificationResponse>>> getOrderNotifications(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        log.info("User {} requesting WhatsApp dispatch timeline for order #{}", user != null ? user.getEmail() : "anonymous", orderId);
        List<WhatsAppNotificationResponse> logs = whatsAppNotificationService.getOrderNotifications(orderId, user);
        return ResponseEntity.ok(ApiResponse.success("Order WhatsApp timeline retrieved successfully", logs));
    }

    @PutMapping("/preference")
    public ResponseEntity<ApiResponse<Boolean>> updateOptInPreference(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WhatsAppOptInRequest request) {
        log.info("User {} updating WhatsApp opt-in preference to {}", user != null ? user.getEmail() : "anonymous", request.getOptIn());
        boolean updated = whatsAppNotificationService.updateOptIn(user, request.getOptIn());
        return ResponseEntity.ok(ApiResponse.success("WhatsApp preference updated successfully", updated));
    }
}
