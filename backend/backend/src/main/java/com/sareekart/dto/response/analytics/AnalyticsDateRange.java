package com.sareekart.dto.response.analytics;

public enum AnalyticsDateRange {
    TODAY,
    _7D,
    _30D,
    _90D,
    YTD,
    ALL,
    CUSTOM;

    public static AnalyticsDateRange fromString(String value) {
        if (value == null || value.isBlank()) return _30D;
        String upper = value.trim().toUpperCase();
        return switch (upper) {
            case "TODAY" -> TODAY;
            case "7D", "LAST_7_DAYS" -> _7D;
            case "30D", "LAST_30_DAYS" -> _30D;
            case "90D", "LAST_90_DAYS" -> _90D;
            case "YTD", "YEAR_TO_DATE" -> YTD;
            case "ALL" -> ALL;
            case "CUSTOM" -> CUSTOM;
            default -> throw new IllegalArgumentException("Invalid analytics date range: " + value);
        };
    }
}
