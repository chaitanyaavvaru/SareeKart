package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.visualsearch.VisualMatchItemResponse;
import com.sareekart.dto.visualsearch.VisualMatchResponse;
import com.sareekart.dto.visualsearch.VisualSearchRequest;
import com.sareekart.entity.User;
import com.sareekart.service.VisualSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/visual-search")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class VisualSearchController {

    private final VisualSearchService visualSearchService;

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<VisualMatchResponse>> matchSarees(
            @RequestBody VisualSearchRequest request,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        log.info("Visual search request received: primaryColor={}, weaveHint={}, user={}",
                request.getPrimaryColor(), request.getWeaveHint(), userId);
        VisualMatchResponse response = visualSearchService.matchSarees(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response.getSummary(), response));
    }

    @GetMapping("/similar/{productId}")
    public ResponseEntity<ApiResponse<List<VisualMatchItemResponse>>> getSimilarDrapes(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        log.info("Fetching visually similar drapes for productId={}, limit={}", productId, limit);
        List<VisualMatchItemResponse> similar = visualSearchService.findSimilarDrapes(productId, limit);
        return ResponseEntity.ok(ApiResponse.success("Visually similar drapes retrieved", similar));
    }
}
