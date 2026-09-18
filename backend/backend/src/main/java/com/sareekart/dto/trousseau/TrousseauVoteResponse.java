package com.sareekart.dto.trousseau;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauVoteResponse {

    private Long id;
    private Long itemId;
    private String voterName;
    private String voterPhoneMasked;
    private String reaction;
    private String note;
    private LocalDateTime createdAt;
}
