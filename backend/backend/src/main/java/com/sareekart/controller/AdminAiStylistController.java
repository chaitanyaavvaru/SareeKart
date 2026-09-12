package com.sareekart.controller;

import com.sareekart.dto.response.AiStylistTelemetryResponse;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.service.AiStylistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-stylist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
public class AdminAiStylistController {

    private final AiStylistService aiStylistService;

    @GetMapping("/telemetry")
    public ResponseEntity<ApiResponse<AiStylistTelemetryResponse>> getStylistTelemetry() {
        log.info("Staff requesting AI stylist consultation telemetry");
        AiStylistTelemetryResponse telemetry = aiStylistService.getTelemetry();
        return ResponseEntity.ok(ApiResponse.success("AI stylist telemetry retrieved successfully", telemetry));
    }
}
