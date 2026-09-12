package com.sareekart.controller;

import com.sareekart.dto.request.PincodeOverrideRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.PincodeOverride;
import com.sareekart.enums.LogisticsZone;
import com.sareekart.service.LogisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
public class AdminLogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/overrides")
    public ResponseEntity<ApiResponse<List<PincodeOverride>>> getAllOverrides() {
        log.info("Staff requesting all pincode logistics overrides");
        List<PincodeOverride> overrides = logisticsService.getAllOverrides();
        return ResponseEntity.ok(ApiResponse.success("Overrides fetched successfully", overrides));
    }

    @PostMapping("/overrides")
    public ResponseEntity<ApiResponse<PincodeOverride>> saveOverride(@Valid @RequestBody PincodeOverrideRequest request) {
        log.info("Staff saving pincode logistics override for PIN {}", request.getPincode());
        PincodeOverride saved = logisticsService.saveOverride(request);
        return ResponseEntity.ok(ApiResponse.success("Pincode override saved successfully", saved));
    }

    @DeleteMapping("/overrides/{pincode}")
    public ResponseEntity<ApiResponse<Void>> deleteOverride(@PathVariable String pincode) {
        log.info("Staff deleting pincode logistics override for PIN {}", pincode);
        logisticsService.deleteOverride(pincode);
        return ResponseEntity.ok(ApiResponse.success("Pincode override deleted successfully", null));
    }

    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLogisticsZones() {
        List<Map<String, Object>> zones = Arrays.stream(LogisticsZone.values())
                .map(z -> Map.<String, Object>of(
                        "name", z.name(),
                        "displayName", z.getDisplayName(),
                        "minDays", z.getMinDays(),
                        "maxDays", z.getMaxDays()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Logistics zones fetched successfully", zones));
    }
}
