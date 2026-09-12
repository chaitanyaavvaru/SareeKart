package com.sareekart.dto.response.analytics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Getter
@Builder
public class DateRangeWindow {
    private final LocalDateTime currentStart;
    private final LocalDateTime currentEnd;
    private final LocalDateTime priorStart;
    private final LocalDateTime priorEnd;
    private final long daysInWindow;

    public static DateRangeWindow calculate(AnalyticsDateRange range, LocalDate customStart, LocalDate customEnd) {
        LocalDate today = LocalDate.now();
        LocalDateTime currentStart;
        LocalDateTime currentEnd = today.atTime(LocalTime.MAX);
        LocalDateTime priorStart;
        LocalDateTime priorEnd;
        long days;

        switch (range) {
            case TODAY -> {
                currentStart = today.atStartOfDay();
                days = 1;
                priorStart = today.minusDays(1).atStartOfDay();
                priorEnd = today.minusDays(1).atTime(LocalTime.MAX);
            }
            case _7D -> {
                currentStart = today.minusDays(6).atStartOfDay();
                days = 7;
                priorStart = currentStart.minusDays(7);
                priorEnd = currentStart.minusNanos(1);
            }
            case _30D -> {
                currentStart = today.minusDays(29).atStartOfDay();
                days = 30;
                priorStart = currentStart.minusDays(30);
                priorEnd = currentStart.minusNanos(1);
            }
            case _90D -> {
                currentStart = today.minusDays(89).atStartOfDay();
                days = 90;
                priorStart = currentStart.minusDays(90);
                priorEnd = currentStart.minusNanos(1);
            }
            case YTD -> {
                currentStart = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                days = ChronoUnit.DAYS.between(currentStart.toLocalDate(), today) + 1;
                priorStart = currentStart.minusYears(1);
                priorEnd = currentEnd.minusYears(1);
            }
            case ALL -> {
                currentStart = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
                days = ChronoUnit.DAYS.between(currentStart.toLocalDate(), today) + 1;
                priorStart = currentStart;
                priorEnd = currentStart;
            }
            case CUSTOM -> {
                if (customStart == null) customStart = today.minusDays(29);
                if (customEnd == null) customEnd = today;
                if (customStart.isAfter(customEnd)) {
                    throw new IllegalArgumentException("Start date cannot be after end date");
                }
                currentStart = customStart.atStartOfDay();
                currentEnd = customEnd.atTime(LocalTime.MAX);
                days = ChronoUnit.DAYS.between(customStart, customEnd) + 1;
                priorStart = currentStart.minusDays(days);
                priorEnd = currentStart.minusNanos(1);
            }
            default -> throw new IllegalStateException("Unexpected range: " + range);
        }

        return DateRangeWindow.builder()
                .currentStart(currentStart)
                .currentEnd(currentEnd)
                .priorStart(priorStart)
                .priorEnd(priorEnd)
                .daysInWindow(days)
                .build();
    }
}
