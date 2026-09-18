package com.sareekart.dto.trousseau;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauCollaboratorResponse {

    private Long id;
    private Long boardId;
    private String name;
    private String phone;
    private String email;
    private String role;
    private String inviteStatus;
    private LocalDateTime createdAt;
}
