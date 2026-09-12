package com.sareekart.dto.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTelemetryResponse {
    private long totalDispatched;
    private long simulatedCount;
    private long liveCount;
    private long deliveredCount;
    private long failedCount;
    private double deliveryRatePercent;
    private Map<String, Long> eventBreakdown;
    private List<WhatsAppNotificationResponse> recentLogs;
}
