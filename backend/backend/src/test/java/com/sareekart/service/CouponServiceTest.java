package com.sareekart.service;

import com.sareekart.dto.coupon.CouponPreviewResponse;
import com.sareekart.entity.Coupon;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.DiscountType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.CartItemRepository;
import com.sareekart.repository.CouponRedemptionRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pure pricing-rule unit tests. The {@link CouponService#quote} function is
 * exercised directly plus the customer preview path end-to-end through
 * mocked persistence.
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository redemptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartItemRepository cartItemRepository;

    private CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(couponRepository, redemptionRepository,
            userRepository, cartItemRepository);
    }

    private Coupon coupon(DiscountType type, String value, String min, String cap,
                          LocalDateTime from, LocalDateTime until, Boolean active) {
        Coupon c = new Coupon();
        c.setCode("TEST");
        c.setDiscountType(type);
        c.setDiscountValue(new BigDecimal(value));
        c.setMinimumOrderAmount(min != null ? new BigDecimal(min) : BigDecimal.ZERO);
        c.setMaximumDiscountAmount(cap != null ? new BigDecimal(cap) : null);
        c.setValidFrom(from);
        c.setValidUntil(until);
        c.setActive(active != null ? active : true);
        return c;
    }

    // ── pure math ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("percentage: 10% of ₹1000 = ₹100.00")
    void percentageBasic() {
        assertThat(service.quote(
            coupon(DiscountType.PERCENTAGE, "10", null, null, null, null, true),
            new BigDecimal("1000"))).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("percentage cap applies when raw discount exceeds it")
    void percentageCap() {
        assertThat(service.quote(
            coupon(DiscountType.PERCENTAGE, "50", null, "50", null, null, true),
            new BigDecimal("1000"))).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("fixed coupon larger than subtotal clamps to subtotal (payable ≥ 0)")
    void fixedClampNeverNegative() {
        assertThat(service.quote(
            coupon(DiscountType.FIXED_AMOUNT, "99999", null, null, null, null, true),
            new BigDecimal("1000"))).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("rounding: 7.5% of ₹333.33 uses HALF_UP at 2dp")
    void roundingHalfUp() {
        assertThat(service.quote(
            coupon(DiscountType.PERCENTAGE, "7.5", null, null, null, null, true),
            new BigDecimal("333.33"))).isEqualByComparingTo("25.00"); // 25.00 (24.999750 → 25.00)
    }

    // ── validation via preview ──────────────────────────────────────────────

    private void stubCart(String email, String subtotal) {
        User u = new User();
        u.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        when(cartItemRepository.findByUserId(u.getId())).thenReturn(List.of()); // subtotal zero-cart not used below
        if (subtotal != null) {
            com.sareekart.entity.CartItem line = new com.sareekart.entity.CartItem();
            com.sareekart.entity.Product prod = new com.sareekart.entity.Product();
            prod.setBasePrice(new BigDecimal(subtotal)); // no salePrice → effective = base
            line.setProduct(prod);
            line.setQuantity(1);
            when(cartItemRepository.findByUserId(u.getId())).thenReturn(List.of(line));
        }
    }

    @Test
    @DisplayName("preview: nonexistent code → invalid with message")
    void nonexistentCode() {
        stubCart("c@s.com", "1000");
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        CouponPreviewResponse r = service.preview("c@s.com", "nope");
        assertThat(r.isValid()).isFalse();
        assertThat(r.getMessage()).contains("Invalid coupon code");
        assertThat(r.getFinalAmount()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("preview: malformed code format rejected before lookup")
    void malformedCode() {
        stubCart("c@s.com", "1000");
        CouponPreviewResponse r = service.preview("c@s.com", "DROP TABLE;--");
        assertThat(r.isValid()).isFalse();
        assertThat(r.getMessage()).contains("format");
        assertThatThrownBy(() -> service.quoteByCode("bad code!", new BigDecimal("100")))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("expired coupon is rejected")
    void expiredCoupon() {
        stubCart("c@s.com", "1000");
        when(couponRepository.findByCode("OLD")).thenReturn(Optional.of(
            coupon(DiscountType.FIXED_AMOUNT, "100", null, null, null, LocalDateTime.now().minusDays(1), true)));
        assertThat(service.preview("c@s.com", "OLD").getMessage()).contains("expired");
    }

    @Test
    @DisplayName("future-dated coupon is rejected")
    void futureCoupon() {
        stubCart("c@s.com", "1000");
        when(couponRepository.findByCode("SOON")).thenReturn(Optional.of(
            coupon(DiscountType.FIXED_AMOUNT, "100", null, null, LocalDateTime.now().plusDays(1), null, true)));
        assertThat(service.preview("c@s.com", "soon").getMessage()).contains("not active yet");
    }

    @Test
    @DisplayName("inactive coupon is rejected")
    void inactiveCoupon() {
        stubCart("c@s.com", "1000");
        when(couponRepository.findByCode("DEAD")).thenReturn(Optional.of(
            coupon(DiscountType.FIXED_AMOUNT, "100", null, null, null, null, false)));
        assertThat(service.preview("c@s.com", "dead").getMessage()).contains("no longer active");
    }

    @Test
    @DisplayName("minimum order amount enforced against live subtotal")
    void minimumOrderRejected() {
        stubCart("c@s.com", "500");
        when(couponRepository.findByCode("MIN5K")).thenReturn(Optional.of(
            coupon(DiscountType.FIXED_AMOUNT, "100", "5000", null, null, null, true)));
        assertThat(service.preview("c@s.com", "min5k").getMessage()).contains("Minimum order");
    }

    @Test
    @DisplayName("quoteByCode throws for violations (order-creation pre-check)")
    void quoteByCodeThrows() {
        when(couponRepository.findByCode(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.quoteByCode("X", new BigDecimal("100")))
            .isInstanceOf(BadRequestException.class);
    }
}