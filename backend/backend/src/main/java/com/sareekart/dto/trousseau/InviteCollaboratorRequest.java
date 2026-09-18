package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteCollaboratorRequest {

    @NotBlank(message = "Collaborator name is required")
    @Size(max = 100, message = "Name must be under 100 characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid E.164 phone number (10-15 digits)")
    private String phone;

    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email must be under 150 characters")
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "CO_CURATOR|VOTER|VIEWER", message = "Role must be CO_CURATOR, VOTER, or VIEWER")
    @Builder.Default
    private String role = "VOTER";
}
