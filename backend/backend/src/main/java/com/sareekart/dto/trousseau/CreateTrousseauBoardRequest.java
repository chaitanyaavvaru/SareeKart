package com.sareekart.dto.trousseau;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTrousseauBoardRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be under 150 characters")
    private String title;

    @FutureOrPresent(message = "Wedding date cannot be in the past")
    private LocalDate weddingDate;

    @Size(max = 2000, message = "Notes must be under 2000 characters")
    private String notes;

    @Builder.Default
    private Boolean isPublicVoting = true;

    @Valid
    @Builder.Default
    private List<CreateCeremonyRequest> ceremonies = new ArrayList<>();
}
