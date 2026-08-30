package com.sareekart.repository;

import com.sareekart.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

    Page<Review> findByProductIdAndApprovedTrue(Long productId, Pageable pageable);

    Optional<Review> findByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByProductIdAndApprovedTrue(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double averageRatingByProductId(@Param("productId") Long productId);
}