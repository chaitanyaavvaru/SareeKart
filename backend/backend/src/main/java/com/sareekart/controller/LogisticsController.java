package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/pincode/{pincode}")
    public ResponseEntity<ApiResponse<PincodeLookupResponse>> checkPincode(@PathVariable String pincode) {
        log.info("Checking serviceability and delivery estimate for PIN code: {}", pincode);
        PincodeLookupResponse response = logisticsService.checkPincode(pincode);
        return ResponseEntity.ok(ApiResponse.success("PIN code serviceability retrieved", response));
    }
}
