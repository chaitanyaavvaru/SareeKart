package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.customer.BehavioralOverviewResponse;
import com.sareekart.dto.response.customer.CustomerAffinityResponse;
import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.dto.response.customer.SearchQueryTelemetryDto;
import com.sareekart.service.CustomerBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/customer-behavior")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
@Tag(name = "Admin Customer Behavior", description = "Behavioral telemetry, event conversion funnel, search intelligence, and customer affinities")
public class AdminCustomerBehaviorController {

    private final CustomerBehaviorService customerBehaviorService;

    @GetMapping("/overview")
    @Operation(summary = "Get behavioral telemetry overview, true 4-stage funnel, and trending items")
    public ResponseEntity<ApiResponse<BehavioralOverviewResponse>> getOverview(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        BehavioralOverviewResponse response = customerBehaviorService.getBehavioralOverview(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{userId}")
    @Operation(summary = "Get deterministic customer affinity profile and purchase intent score")
    public ResponseEntity<ApiResponse<CustomerAffinityResponse>> getCustomerAffinity(@PathVariable Long userId) {
        CustomerAffinityResponse response = customerBehaviorService.getCustomerAffinityProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{userId}/journey")
    @Operation(summary = "Get chronological customer event timeline")
    public ResponseEntity<ApiResponse<List<CustomerEventResponse>>> getCustomerJourney(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {

        List<CustomerEventResponse> responses = customerBehaviorService.getCustomerJourney(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/searches")
    @Operation(summary = "Get trending and zero-result search telemetry")
    public ResponseEntity<ApiResponse<List<SearchQueryTelemetryDto>>> getSearchTelemetry(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean zeroResultsOnly) {

        List<SearchQueryTelemetryDto> responses = customerBehaviorService.getSearchTelemetry(range, startDate, endDate, zeroResultsOnly);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
