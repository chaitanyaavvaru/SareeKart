package com.sareekart.controller;

import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.request.EventBatchRequest;
import com.sareekart.dto.request.IdentifySessionRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.service.CustomerBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Events", description = "Non-blocking event ingestion and guest identity resolution")
public class CustomerEventController {

    private final CustomerBehaviorService customerBehaviorService;

    @PostMapping
    @Operation(summary = "Record single behavioral event (guest or authenticated)")
    public ResponseEntity<ApiResponse<CustomerEventResponse>> trackEvent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CustomerEventRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        Long userId = (user != null) ? user.getId() : null;

        try {
            CustomerEventResponse response = customerBehaviorService.recordEvent(request, userId, clientIp, userAgent);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Non-blocking failure recording telemetry event: {}", ex.getMessage());
            // Graceful fallback to guarantee business flow isolation
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch record behavioral events (buffer flush / sendBeacon)")
    public ResponseEntity<ApiResponse<List<CustomerEventResponse>>> trackEventBatch(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EventBatchRequest batchRequest,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        Long userId = (user != null) ? user.getId() : null;

        try {
            List<CustomerEventResponse> responses = customerBehaviorService.recordBatch(
                    batchRequest.getEvents(), userId, clientIp, userAgent);
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Non-blocking failure recording telemetry batch: {}", ex.getMessage());
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
    }

    @PostMapping("/identify")
    @Operation(summary = "Resolve guest session to authenticated customer upon login/register")
    public ResponseEntity<ApiResponse<Integer>> identifySession(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody IdentifySessionRequest request) {

        if (user == null) {
            throw new BadRequestException("Identity resolution requires active authenticated session");
        }

        int linkedCount = customerBehaviorService.identifySession(request.getSessionId(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Session linked successfully", linkedCount));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
