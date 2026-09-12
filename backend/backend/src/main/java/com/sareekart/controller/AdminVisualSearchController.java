package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.visualsearch.VisualSearchTelemetryResponse;
import com.sareekart.service.VisualSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/visual-search")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AdminVisualSearchController {

    private final VisualSearchService visualSearchService;

    @GetMapping("/telemetry")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VisualSearchTelemetryResponse>> getTelemetry() {
        log.info("Admin requested AI Visual Search telemetry & query logs");
        VisualSearchTelemetryResponse telemetry = visualSearchService.getVisualSearchTelemetry();
        return ResponseEntity.ok(ApiResponse.success("AI Visual Search telemetry retrieved successfully", telemetry));
    }
}
