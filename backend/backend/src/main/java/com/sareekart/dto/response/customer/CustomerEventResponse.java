package com.sareekart.dto.response.customer;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEventResponse {
    private Long id;
    private String clientEventId;
    private String sessionId;
    private Long userId;
    private String userEmail;
    private String eventType;
    private String entityType;
    private Long entityId;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
