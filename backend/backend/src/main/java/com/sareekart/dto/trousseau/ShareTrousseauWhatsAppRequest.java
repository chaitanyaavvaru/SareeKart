package com.sareekart.dto.trousseau;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareTrousseauWhatsAppRequest {

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Recipient phone must be a valid phone number (10 to 15 digits, optional '+' prefix)")
    private String recipientPhone;

    private String recipientName;

    private Long ceremonyId;

    private String customNote;
}
