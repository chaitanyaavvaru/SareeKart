package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTrousseauBoardRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be under 150 characters")
    private String title;

    @FutureOrPresent(message = "Wedding date cannot be in the past")
    private LocalDate weddingDate;

    @Size(max = 2000, message = "Notes must be under 2000 characters")
    private String notes;

    private Boolean isPublicVoting;

    @Pattern(regexp = "ACTIVE|ARCHIVED|COMPLETED", message = "Status must be ACTIVE, ARCHIVED, or COMPLETED")
    private String status;
}
