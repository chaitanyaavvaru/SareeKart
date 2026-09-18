package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CastVoteRequest {

    @NotBlank(message = "Voter name is required")
    @Size(max = 100, message = "Voter name must be under 100 characters")
    private String voterName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid E.164 phone number (10-15 digits)")
    private String voterPhone;

    @NotBlank(message = "Reaction is required")
    @Pattern(
        regexp = "LOVE|LIKE|PASS|NEEDS_REVIEW",
        message = "Reaction must be LOVE, LIKE, PASS, or NEEDS_REVIEW"
    )
    private String reaction;

    @Size(max = 1000, message = "Note must be under 1000 characters")
    private String note;
}
