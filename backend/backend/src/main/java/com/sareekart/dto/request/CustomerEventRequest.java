package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEventRequest {

    @Size(max = 64, message = "clientEventId must not exceed 64 characters")
    private String clientEventId;

    @NotBlank(message = "sessionId is required")
    @Size(max = 64, message = "sessionId must not exceed 64 characters")
    private String sessionId;

    @NotBlank(message = "eventType is required")
    @Size(max = 50, message = "eventType must not exceed 50 characters")
    private String eventType;

    @Size(max = 50, message = "entityType must not exceed 50 characters")
    private String entityType;

    private Long entityId;

    private Map<String, Object> metadata;
}
