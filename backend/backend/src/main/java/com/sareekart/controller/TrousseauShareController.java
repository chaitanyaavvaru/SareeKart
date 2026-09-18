package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.trousseau.CastVoteRequest;
import com.sareekart.dto.trousseau.SharedTrousseauViewResponse;
import com.sareekart.dto.trousseau.TrousseauVoteResponse;
import com.sareekart.service.TrousseauRateLimiter;
import com.sareekart.service.TrousseauService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trousseau/share")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TrousseauShareController {

    private final TrousseauService trousseauService;
    private final TrousseauRateLimiter rateLimiter;

    @GetMapping("/{shareToken}")
    public ResponseEntity<ApiResponse<SharedTrousseauViewResponse>> getSharedBoard(
            @PathVariable String shareToken) {
        SharedTrousseauViewResponse response = trousseauService.getSharedBoardByToken(shareToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{shareToken}/items/{itemId}/vote")
    public ResponseEntity<ApiResponse<TrousseauVoteResponse>> castVote(
            @PathVariable String shareToken,
            @PathVariable Long itemId,
            @Valid @RequestBody CastVoteRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiter.tryAcquire(clientIp)) {
            log.warn("Rate limit exceeded for IP {} on trousseau vote", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Rate limit exceeded. Maximum 15 votes per minute allowed."));
        }

        TrousseauVoteResponse response = trousseauService.castVote(shareToken, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", response));
    }

    @GetMapping("/{shareToken}/items/{itemId}/votes")
    public ResponseEntity<ApiResponse<List<TrousseauVoteResponse>>> getItemVotes(
            @PathVariable String shareToken,
            @PathVariable Long itemId) {
        List<TrousseauVoteResponse> responses = trousseauService.getItemVotes(shareToken, itemId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
