package com.sareekart.dto.trousseau;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauWhatsAppShareResponse {

    private boolean success;
    private String message;
    private String recipientPhone;
    private String recipientName;
    private String shareUrl;
    private String messagePreview;
    private LocalDateTime dispatchedAt;
    private boolean simulated;
}
