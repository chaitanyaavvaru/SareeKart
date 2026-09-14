package com.sareekart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventBatchRequest {

    @NotEmpty(message = "Events list cannot be empty")
    @Size(max = 50, message = "Batch size cannot exceed 50 events")
    private List<@Valid CustomerEventRequest> events;
}
