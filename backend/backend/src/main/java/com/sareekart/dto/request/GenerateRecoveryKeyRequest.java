package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRecoveryKeyRequest {

    @NotBlank(message = "Current password is required to generate a recovery key")
    private String currentPassword;
}
