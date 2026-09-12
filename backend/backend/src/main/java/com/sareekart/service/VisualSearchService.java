package com.sareekart.service;

import com.sareekart.dto.visualsearch.VisualMatchItemResponse;
import com.sareekart.dto.visualsearch.VisualMatchResponse;
import com.sareekart.dto.visualsearch.VisualSearchRequest;
import com.sareekart.dto.visualsearch.VisualSearchTelemetryResponse;

import java.util.List;

public interface VisualSearchService {

    VisualMatchResponse matchSarees(VisualSearchRequest request, Long userId);

    List<VisualMatchItemResponse> findSimilarDrapes(Long productId, int limit);

    VisualSearchTelemetryResponse getVisualSearchTelemetry();
}
