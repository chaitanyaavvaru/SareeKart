package com.sareekart.service;

import com.sareekart.dto.coupon.CouponAdminResponse;
import com.sareekart.dto.coupon.CouponPreviewResponse;
import com.sareekart.dto.coupon.CouponRequest;
import com.sareekart.entity.Coupon;
import com.sareekart.entity.CouponRedemption;
import com.sareekart.entity.Order;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.DiscountType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.CartItemRepository;
import com.sareekart.repository.CouponRedemptionRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coupon validation, quoting and lifecycle.
 *
 * MONEY RULES (BigDecimal only):
 *   PERCENTAGE  discount = subtotal × value/100        (HALF_UP, 2dp)
 *               then capped by maximumDiscountAmount when configured
 *   FIXED       discount = value
 *   both        clamped so 0 ≤ discount ≤ subtotal ⇒ payable can never go negative
 *
 * CONCURRENCY: reservation runs under a pessimistic lock on the coupon row,
 * so two simultaneous checkouts of the same code are serialized — limits are
 * checked against committed quota INCLUDING held reservations at that moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    // ── Customer preview ─────────────────────────────────────────────────────

    /** Quotes the caller's LIVE cart subtotal — client never supplies money values. */
    @Transactional(readOnly = true)
    public CouponPreviewResponse preview(String userEmail, String rawCode) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        BigDecimal subtotal = cartItemRepository.findByUserId(user.getId()).stream()
            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            String code = normalize(rawCode); // malformed → graceful invalid, not 400
            Coupon coupon = findValidCoupon(code, subtotal);
            BigDecimal discount = quote(coupon, subtotal);
            return CouponPreviewResponse.builder()
                .code(code).description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discount(discount).subtotal(subtotal)
                .finalAmount(subtotal.subtract(discount))
                .valid(true).message("Coupon applied")
                .build();
        } catch (BadRequestException e) {
            CouponPreviewResponse r = CouponPreviewResponse.invalid(
                rawCode != null ? rawCode.trim().toUpperCase() : "", e.getMessage());
            r.setSubtotal(subtotal);
            r.setFinalAmount(subtotal);
            return r;
        }
    }

    /** Unlocked pre-quote used for total computation; reserveForOrder() re-validates under lock. */
    @Transactional(readOnly = true)
    public BigDecimal quoteByCode(String rawCode, BigDecimal subtotal) {
        Coupon coupon = findValidCoupon(normalize(rawCode), subtotal);
        return quote(coupon, subtotal);
    }

    // ── Reservation (authoritative gate, called inside order-creation tx) ───

    /**
     * Validates against the ORDER's stored subtotal and inserts the quota-
     * holding redemption row. Idempotent per order via UNIQUE(order_id);
     * losing a concurrent insert race surfaces as an explicit rejection.
     */
    @Transactional
    public BigDecimal reserveForOrder(Order order, String rawCode) {
        String code = normalize(rawCode);

        // Lock serializes same-code checkouts; all limit checks + insert are atomic.
        Coupon coupon = couponRepository.findByCodeForUpdate(code)
            .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        validateUsable(coupon, LocalDateTime.now());
        if (order.getSubtotal().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BadRequestException(String.format(
                "Minimum order amount of ₹%s required for this coupon",
                coupon.getMinimumOrderAmount().stripTrailingZeros().toPlainString()));
        }

        long totalUsed = redemptionRepository.countByCouponId(coupon.getId());
        if (coupon.getTotalUsageLimit() != null && totalUsed >= coupon.getTotalUsageLimit()) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }
        long userUsed = redemptionRepository.countByCouponIdAndUserId(coupon.getId(), order.getUser().getId());
        if (coupon.getPerUserUsageLimit() != null && userUsed >= coupon.getPerUserUsageLimit()) {
            throw new BadRequestException("You have already used this coupon the maximum number of times");
        }

        BigDecimal discount = quote(coupon, order.getSubtotal());

        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(coupon);
        redemption.setUser(order.getUser());
        redemption.setOrder(order);
        redemption.setDiscountApplied(discount);
        redemption.setRedeemed(false);
        try {
            redemptionRepository.save(redemption);
        } catch (DataIntegrityViolationException dup) {
            throw new BadRequestException("Coupon already applied to this order");
        }

        log.info("Coupon {} reserved on order {} discount={} (totalUsed={})",
            code, order.getOrderNumber(), discount, totalUsed + 1);
        return discount;
    }

    /** Marks the reservation permanently counted. Idempotent. */
    @Transactional
    public void confirmForOrder(Long orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(r -> {
            if (!Boolean.TRUE.equals(r.getRedeemed())) {
                r.setRedeemed(true);
                redemptionRepository.save(r);
                log.info("Coupon {} redemption confirmed on orderId {}", r.getCoupon().getCode(), orderId);
            }
        });
    }

    /**
     * Releases quota. Business rule: only UNCOMMITTED orders release usage —
     * once payment committed, the coupon stays consumed even on later
     * cancellation/refund. Idempotent (no-op when nothing releasable).
     */
    @Transactional
    public boolean releaseIfReserved(Long orderId) {
        return redemptionRepository.findByOrderId(orderId)
            .filter(r -> !Boolean.TRUE.equals(r.getRedeemed()))
            .map(r -> {
                redemptionRepository.delete(r);
                redemptionRepository.flush();
                log.info("Coupon reservation released for orderId {}", orderId);
                return true;
            })
            .orElse(false);
    }

    // ── Quoting math ─────────────────────────────────────────────────────────

    private Coupon findValidCoupon(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
            .orElseThrow(() -> new BadRequestException("Invalid coupon code"));
        validateUsable(coupon, LocalDateTime.now());
        if (subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BadRequestException(String.format(
                "Minimum order amount of ₹%s required for this coupon",
                coupon.getMinimumOrderAmount().stripTrailingZeros().toPlainString()));
        }
        return coupon;
    }

    private void validateUsable(Coupon coupon, LocalDateTime now) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BadRequestException("This coupon is no longer active");
        }
        if (!coupon.isStarted(now)) {
            throw new BadRequestException("This coupon is not active yet");
        }
        if (coupon.isExpired(now)) {
            throw new BadRequestException("This coupon has expired");
        }
    }

    /** Pure pricing function — the single place discount math lives. */
    BigDecimal quote(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscountAmount() != null
                    && discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                discount = coupon.getMaximumDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        // Clamp: never exceed subtotal, never negative payable.
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Coupon code is required");
        }
        String code = raw.trim().toUpperCase();
        if (!code.matches("[A-Z0-9_-]{1,50}")) {
            throw new BadRequestException("Invalid coupon code format");
        }
        return code;
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public CouponAdminResponse create(CouponRequest req) {
        String code = normalize(req.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new BadRequestException("Coupon code already exists");
        }
        Coupon c = new Coupon();
        applyRequest(c, req, code);
        c = couponRepository.save(c);
        log.info("Created coupon {}", code);
        return CouponAdminResponse.from(c, 0L);
    }

    @Transactional
    public CouponAdminResponse update(Long id, CouponRequest req) {
        Coupon c = couponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        String code = normalize(req.getCode());
        if (!c.getCode().equals(code) && couponRepository.existsByCode(code)) {
            throw new BadRequestException("Coupon code already exists");
        }
        applyRequest(c, req, code);
        c = couponRepository.save(c);
        log.info("Updated coupon {}", code);
        return CouponAdminResponse.from(c, redemptionRepository.countByCouponId(id));
    }

    @Transactional(readOnly = true)
    public List<CouponAdminResponse> list() {
        List<Coupon> coupons = couponRepository.findAll();
        Map<Long, Long> usage = coupons.stream().collect(Collectors.toMap(
            Coupon::getId,
            c -> redemptionRepository.countByCouponId(c.getId()),
            (a, b) -> a,
            java.util.LinkedHashMap::new));
        return CouponAdminResponse.fromList(coupons, usage);
    }

    @Transactional
    public void delete(Long id) {
        Coupon c = couponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        long used = redemptionRepository.countByCouponId(id);
        if (used > 0) {
            // Soft-deactivate instead of hard delete to preserve redemption history
            c.setActive(false);
            couponRepository.save(c);
            log.info("Coupon {} soft-deactivated (had {} redemptions) instead of delete", c.getCode(), used);
            return;
        }
        couponRepository.delete(c);
        log.info("Deleted coupon {}", c.getCode());
    }

    private void applyRequest(Coupon c, CouponRequest req, String normalizedCode) {
        if (req.getDiscountType() == DiscountType.PERCENTAGE
                && req.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }
        c.setCode(normalizedCode);
        c.setDescription(req.getDescription());
        c.setDiscountType(req.getDiscountType());
        c.setDiscountValue(req.getDiscountValue());
        c.setMinimumOrderAmount(req.getMinimumOrderAmount() != null
            ? req.getMinimumOrderAmount() : BigDecimal.ZERO);
        c.setMaximumDiscountAmount(req.getMaximumDiscountAmount());
        c.setValidFrom(req.getValidFrom());
        c.setValidUntil(req.getValidUntil());
        c.setTotalUsageLimit(req.getTotalUsageLimit());
        c.setPerUserUsageLimit(req.getPerUserUsageLimit());
        c.setActive(req.getActive() != null ? req.getActive() : true);
        if (c.getValidFrom() != null && c.getValidUntil() != null
                && c.getValidUntil().isBefore(c.getValidFrom())) {
            throw new BadRequestException("validUntil must be after validFrom");
        }
    }
}