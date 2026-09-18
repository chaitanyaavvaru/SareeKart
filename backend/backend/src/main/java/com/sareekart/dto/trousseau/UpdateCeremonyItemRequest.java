package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCeremonyItemRequest {

    @Size(max = 1000, message = "Notes must be under 1000 characters")
    private String notes;

    @Pattern(
        regexp = "SHORTLISTED|SELECTED|IN_CART|PURCHASED|ARCHIVED",
        message = "Status must be SHORTLISTED, SELECTED, IN_CART, PURCHASED, or ARCHIVED"
    )
    private String status;
}
