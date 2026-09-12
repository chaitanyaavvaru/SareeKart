package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.LookupResponse;
import com.sareekart.service.AttributeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LookupController {

    private final AttributeLookupService lookupService;

    @GetMapping("/fabrics")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getFabrics(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getAllFabrics(activeOnly)));
    }

    @GetMapping("/fabrics/{id}")
    public ResponseEntity<ApiResponse<LookupResponse>> getFabricById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getFabricById(id)));
    }

    @GetMapping("/occasions")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getOccasions(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getAllOccasions(activeOnly)));
    }

    @GetMapping("/occasions/{id}")
    public ResponseEntity<ApiResponse<LookupResponse>> getOccasionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getOccasionById(id)));
    }

    @GetMapping("/colors")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getColors(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getAllColors(activeOnly)));
    }

    @GetMapping("/colors/{id}")
    public ResponseEntity<ApiResponse<LookupResponse>> getColorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getColorById(id)));
    }
}
