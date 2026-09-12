package com.sareekart.dto.visualsearch;

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
public class VisualMatchResponse {
    private Map<String, String> detectedAttributes;
    private String summary;
    private List<VisualMatchItemResponse> matches;
    private Integer executionTimeMs;
}
