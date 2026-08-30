package com.sareekart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "SareeKart Backend",
            "version", "0.0.1-SNAPSHOT"
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        return ResponseEntity.ok(Map.of(
            "status", "READY",
            "timestamp", LocalDateTime.now()
        ));
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", LocalDateTime.now()
        ));
    }
}