package com.sareekart.dto.response.customer;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchQueryTelemetryDto {
    private String query;
    private long count;
    private long zeroResultCount;
    private double zeroResultRate;
}
