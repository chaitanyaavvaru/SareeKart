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
}
