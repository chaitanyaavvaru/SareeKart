package com.sareekart.service;

import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.response.customer.*;

import java.time.LocalDate;
import java.util.List;

public interface CustomerBehaviorService {

    CustomerEventResponse recordEvent(CustomerEventRequest request, Long authenticatedUserId, String clientIp, String userAgent);

    List<CustomerEventResponse> recordBatch(List<CustomerEventRequest> requests, Long authenticatedUserId, String clientIp, String userAgent);

    int identifySession(String sessionId, Long authenticatedUserId);

    BehavioralOverviewResponse getBehavioralOverview(String range, LocalDate startDate, LocalDate endDate);

    CustomerAffinityResponse getCustomerAffinityProfile(Long userId);

    List<CustomerEventResponse> getCustomerJourney(Long userId, int limit);

    List<SearchQueryTelemetryDto> getSearchTelemetry(String range, LocalDate startDate, LocalDate endDate, boolean zeroResultsOnly);

    ConversionFunnelResponse getConversionFunnel(String range, LocalDate startDate, LocalDate endDate);
}

