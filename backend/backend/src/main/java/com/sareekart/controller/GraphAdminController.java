package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.repository.GraphSyncFailureRepository;
import com.sareekart.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 7: Knowledge Graph Admin Controller.
 * 
 * Provides administrative oversight, diagnostic monitoring, and on-demand
 * synchronization operations for the Neo4j Knowledge Graph.
 */
@RestController
@RequestMapping("/api/admin/graph")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Knowledge Graph", description = "Operations and telemetry for Neo4j Knowledge Graph")
public class GraphAdminController {

    private final GraphService graphService;
    private final GraphSyncFailureRepository failureRepository;

    @GetMapping("/stats")
    @Operation(summary = "Detailed graph node/edge topology and DLQ health metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>(graphService.getGraphStatistics());
        long unresolvedDlq = failureRepository.countByResolvedAtIsNull();
        stats.put("unresolvedDlqCount", unresolvedDlq);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/reseed")
    @Operation(summary = "Force idempotent catalog and historical interaction re-seed into Neo4j")
    public ResponseEntity<ApiResponse<String>> triggerReseed() {
        log.info("Admin initiated manual Neo4j catalog re-seed.");
        graphService.initializeSchemaConstraints();
        graphService.seedCatalog();
        graphService.seedHistoricalInteractions();
        return ResponseEntity.ok(ApiResponse.success("Neo4j catalog and historical interactions successfully seeded."));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Trigger catalog structural parity reconciliation")
    public ResponseEntity<ApiResponse<String>> triggerReconciliation() {
        log.info("Admin initiated manual Neo4j catalog reconciliation.");
        graphService.seedCatalog();
        return ResponseEntity.ok(ApiResponse.success("Catalog parity reconciliation completed."));
    }
}
