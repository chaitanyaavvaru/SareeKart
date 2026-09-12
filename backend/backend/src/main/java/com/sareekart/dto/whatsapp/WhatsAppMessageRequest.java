package com.sareekart.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WhatsAppMessageRequest {
    @JsonProperty("messaging_product")
    private final String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    private final String recipientType = "individual";

    private String to;
    private String type;

    private Text text;
    private Template template;
    private Image image;

    @Data
    @Builder
    public static class Text {
        private boolean preview_url;
        private String body;
    }

    @Data
    @Builder
    public static class Template {
        private String name;
        private Language language;
        // Optionally add components for variables
    }

    @Data
    @Builder
    public static class Language {
        private String code;
    }

    @Data
    @Builder
    public static class Image {
        private String link;
        private String caption;
    }
}
