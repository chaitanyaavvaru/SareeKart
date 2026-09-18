package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCeremonyItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Size(max = 1000, message = "Notes must be under 1000 characters")
    private String notes;

    @Builder.Default
    private Boolean isAiRecommended = false;
}
