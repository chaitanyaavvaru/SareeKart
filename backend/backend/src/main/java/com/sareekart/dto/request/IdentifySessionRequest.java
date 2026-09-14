package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentifySessionRequest {

    @NotBlank(message = "sessionId is required")
    @Size(max = 64, message = "sessionId must not exceed 64 characters")
    private String sessionId;
}
