package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.Review;
import com.sareekart.entity.User;
import com.sareekart.repository.OrderItemRepository;
import com.sareekart.repository.ReviewRepository;
import com.sareekart.service.NotificationEventService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final NotificationEventService notificationEventService;

    /**
     * Get all approved reviews for a specific product.
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByProduct(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdAndStatusInOrderByCreatedAtDesc(
                productId,
                List.of("APPROVED", "FEATURED")
        );
        // Fallback: If empty, also check all for backward-compat with reviews without explicit status
        if (reviews.isEmpty()) {
            reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        }
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * Add a review to a product (requires authentication).
     */
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Review>> addReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user,
            @RequestBody Review review) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("You must be logged in to leave a review.", null));
        }

        review.setProductId(productId);
        review.setUserName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : ""));
        review.setCreatedAt(LocalDateTime.now());
        
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Rating must be between 1 and 5.", null));
        }

        // Determine if user is a verified buyer of this product
        boolean isPurchased = false;
        try {
            isPurchased = orderItemRepository.hasUserPurchasedProduct(user.getId(), productId);
        } catch (Exception e) {
            log.warn("Could not determine verified buyer status for user {} product {}: {}", user.getId(), productId, e.getMessage());
        }
        review.setVerifiedBuyer(isPurchased);
        review.setStatus("APPROVED");

        Review saved = reviewRepository.save(review);

        // Notify staff of new review submission
        try {
            notificationEventService.notifyReviewSubmitted(saved);
        } catch (Exception e) {
            log.warn("Failed to dispatch review notification: {}", e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review added successfully", saved));
    }

    /* =========================================================================
     * Admin Review Moderation Endpoints
     * ========================================================================= */

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Review>>> getAdminReviews(
            @RequestParam(required = false) String status) {
        List<Review> reviews = (status != null && !status.isEmpty())
                ? reviewRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : reviewRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @Data
    public static class ReviewStatusDto {
        private String status; // APPROVED, REJECTED, FEATURED, PENDING
    }

    @PutMapping("/admin/reviews/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Review>> updateReviewStatus(
            @PathVariable Long id,
            @RequestBody ReviewStatusDto statusDto) {
        if (statusDto.getStatus() == null || statusDto.getStatus().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Status is required (APPROVED, REJECTED, FEATURED, PENDING)", null));
        }

        return reviewRepository.findById(id)
                .map(review -> {
                    review.setStatus(statusDto.getStatus().toUpperCase());
                    Review updated = reviewRepository.save(review);
                    return ResponseEntity.ok(ApiResponse.success("Review status updated to " + review.getStatus(), updated));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Review not found with id: " + id, null)));
    }

    @DeleteMapping("/admin/reviews/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    reviewRepository.delete(review);
                    return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", "DELETED"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Review not found with id: " + id, null)));
    }
}
