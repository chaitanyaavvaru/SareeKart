package com.sareekart.repository;

import com.sareekart.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    long countByUserId(Long userId);
}
