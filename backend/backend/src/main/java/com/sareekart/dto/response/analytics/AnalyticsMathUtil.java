package com.sareekart.dto.response.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AnalyticsMathUtil {

    private AnalyticsMathUtil() {}

    public static double calculatePercentageDelta(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) return 0.0;
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : -100.0;
        }
        if (current == null) current = BigDecimal.ZERO;
        BigDecimal delta = current.subtract(prior);
        BigDecimal pct = delta.multiply(BigDecimal.valueOf(100.0))
                .divide(prior.abs(), 4, RoundingMode.HALF_UP);
        return pct.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculatePercentageDelta(long current, long prior) {
        return calculatePercentageDelta(BigDecimal.valueOf(current), BigDecimal.valueOf(prior));
    }

    public static double calculateRatioPercentage(long numerator, long denominator) {
        if (denominator <= 0) return 0.0;
        return BigDecimal.valueOf(numerator * 100.0)
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double calculateRatioPercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        if (numerator == null) return 0.0;
        return numerator.multiply(BigDecimal.valueOf(100.0))
                .divide(denominator, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
