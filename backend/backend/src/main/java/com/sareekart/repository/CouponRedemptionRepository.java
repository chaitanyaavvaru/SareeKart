package com.sareekart.repository;

import com.sareekart.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    long countByCouponId(Long couponId);

    long countByCouponIdAndUserId(Long couponId, Long userId);

    Optional<CouponRedemption> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}