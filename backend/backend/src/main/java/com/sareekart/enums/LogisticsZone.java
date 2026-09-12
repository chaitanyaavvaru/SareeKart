package com.sareekart.enums;

public enum LogisticsZone {
    METRO("Metro Capital Circle", 1, 2),
    TIER_1("Tier-1 Commercial Hub", 2, 3),
    TIER_2("Tier-2 Regional City", 3, 4),
    REGIONAL("Regional / Rural District", 4, 5),
    REMOTE("Remote & Mountain Corridor", 5, 7);

    private final String displayName;
    private final int minDays;
    private final int maxDays;

    LogisticsZone(String displayName, int minDays, int maxDays) {
        this.displayName = displayName;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }
}
