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
public class DrapeStyleRequest {

    private Long productId;

    @NotBlank(message = "Saree name is required")
    private String sareeName;

    private String fabric;

    private String primaryColor;

    private String occasion;

    private String zariType;
}
