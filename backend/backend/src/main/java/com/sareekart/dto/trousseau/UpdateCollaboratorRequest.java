package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCollaboratorRequest {

    @Pattern(regexp = "CO_CURATOR|VOTER|VIEWER", message = "Role must be CO_CURATOR, VOTER, or VIEWER")
    private String role;

    @Pattern(regexp = "PENDING|ACCEPTED|DECLINED", message = "Invite status must be PENDING, ACCEPTED, or DECLINED")
    private String inviteStatus;
}
