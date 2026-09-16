package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppApiClient {

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api.url:https://graph.facebook.com/v19.0}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    public void sendTextMessage(String to, String body) {
        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
                .to(to)
                .type("text")
                .text(WhatsAppMessageRequest.Text.builder()
                        .preview_url(true)
                        .body(body)
                        .build())
                .build();
        sendMessage(request);
    }

    public void sendImageMessage(String to, String imageUrl, String caption) {
        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
                .to(to)
                .type("image")
                .image(WhatsAppMessageRequest.Image.builder()
                        .link(imageUrl)
                        .caption(caption)
                        .build())
                .build();
        sendMessage(request);
    }

    /**
     * Sends Meta interactive quick-reply buttons (up to 3 buttons).
     */
    public void sendInteractiveButtonsMessage(String to, String bodyText, List<Map<String, String>> buttons) {
        List<WhatsAppMessageRequest.InteractiveButton> buttonList = new ArrayList<>();
        int count = 0;
        for (Map<String, String> btn : buttons) {
            if (count >= 3) break; // Meta limit is 3 buttons
            buttonList.add(WhatsAppMessageRequest.InteractiveButton.builder()
                    .type("reply")
                    .reply(WhatsAppMessageRequest.Reply.builder()
                            .id(btn.get("id"))
                            .title(btn.get("title"))
                            .build())
                    .build());
            count++;
        }

        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
                .to(to)
                .type("interactive")
                .interactive(WhatsAppMessageRequest.Interactive.builder()
                        .type("button")
                        .body(WhatsAppMessageRequest.InteractiveBody.builder()
                                .text(bodyText)
                                .build())
                        .action(WhatsAppMessageRequest.InteractiveAction.builder()
                                .buttons(buttonList)
                                .build())
                        .build())
                .build();

        sendMessage(request);
    }

    /**
     * Sends Meta interactive list message (sections with up to 10 rows total).
     */
    public void sendInteractiveListMessage(String to, String header, String body, String buttonLabel, 
                                          List<WhatsAppMessageRequest.InteractiveSection> sections) {
        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
                .to(to)
                .type("interactive")
                .interactive(WhatsAppMessageRequest.Interactive.builder()
                        .type("list")
                        .header(header != null ? WhatsAppMessageRequest.InteractiveHeader.builder()
                                .type("text").text(header).build() : null)
                        .body(WhatsAppMessageRequest.InteractiveBody.builder().text(body).build())
                        .action(WhatsAppMessageRequest.InteractiveAction.builder()
                                .button(buttonLabel != null ? buttonLabel : "View Options")
                                .sections(sections)
                                .build())
                        .build())
                .build();

        sendMessage(request);
    }

    private void sendMessage(WhatsAppMessageRequest request) {
        if (apiToken == null || apiToken.isEmpty() || "dummy_whatsapp_token".equals(apiToken) 
                || phoneNumberId == null || phoneNumberId.isEmpty()) {
            log.info("[SIMULATED WhatsApp Outbound] Dispatched {} message to {}: {}", 
                    request.getType(), request.getTo(), 
                    request.getText() != null ? request.getText().getBody() : 
                    (request.getInteractive() != null ? request.getInteractive().getBody().getText() : "Media/Template"));
            return;
        }

        String url = apiUrl + "/" + phoneNumberId + "/messages";

        webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Successfully sent WhatsApp message to {}: {}", request.getTo(), response))
                .doOnError(error -> log.error("Failed to send WhatsApp message to {}: {}", request.getTo(), error.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
