package com.sareekart.enums;

import java.math.BigDecimal;

public enum PatronTier {
    SILVER("Silver Patron", 1.0, BigDecimal.ZERO),
    GOLD("Gold Connoisseur", 1.25, BigDecimal.valueOf(25000)),
    ROYAL_PATRON("Royal Silk Club", 1.5, BigDecimal.valueOf(100000));

    private final String displayName;
    private final double pointsMultiplier;
    private final BigDecimal minimumSpend;

    PatronTier(String displayName, double pointsMultiplier, BigDecimal minimumSpend) {
        this.displayName = displayName;
        this.pointsMultiplier = pointsMultiplier;
        this.minimumSpend = minimumSpend;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPointsMultiplier() {
        return pointsMultiplier;
    }

    public BigDecimal getMinimumSpend() {
        return minimumSpend;
    }

    public static PatronTier calculateTier(BigDecimal lifetimeSpent) {
        if (lifetimeSpent == null) return SILVER;
        if (lifetimeSpent.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            return ROYAL_PATRON;
        } else if (lifetimeSpent.compareTo(BigDecimal.valueOf(25000)) >= 0) {
            return GOLD;
        }
        return SILVER;
    }
}
