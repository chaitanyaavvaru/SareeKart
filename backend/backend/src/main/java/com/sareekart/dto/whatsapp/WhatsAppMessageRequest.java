package com.sareekart.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageRequest {
    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";

    private String to;
    private String type; // "text", "image", "template", "interactive"

    private Text text;
    private Template template;
    private Image image;
    private Interactive interactive;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Text {
        private boolean preview_url;
        private String body;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Template {
        private String name;
        private Language language;
        private List<TemplateComponent> components;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateComponent {
        private String type; // "header", "body", "button"
        @JsonProperty("sub_type")
        private String subType; // "url", "quick_reply"
        private Integer index;
        private List<TemplateParameter> parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateParameter {
        private String type; // "text", "currency", "date_time", "image"
        private String text;
        private Currency currency;
        private DateTime dateTime;
        private Image image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Currency {
        @JsonProperty("fallback_value")
        private String fallbackValue;
        private String code;
        @JsonProperty("amount_1000")
        private Integer amount1000;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateTime {
        @JsonProperty("fallback_value")
        private String fallbackValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Language {
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String link;
        private String caption;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interactive {
        private String type; // "button" or "list"
        private InteractiveHeader header;
        private InteractiveBody body;
        private InteractiveFooter footer;
        private InteractiveAction action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveHeader {
        private String type; // "text" or "image"
        private String text;
        private Image image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveBody {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveFooter {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveAction {
        private String button; // Label for list button
        private List<InteractiveButton> buttons; // For button type
        private List<InteractiveSection> sections; // For list type
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveButton {
        @Builder.Default
        private String type = "reply";
        private Reply reply;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reply {
        private String id;
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveSection {
        private String title;
        private List<InteractiveRow> rows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractiveRow {
        private String id;
        private String title;
        private String description;
    }
}
