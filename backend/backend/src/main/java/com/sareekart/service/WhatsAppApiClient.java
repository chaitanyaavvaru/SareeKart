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

    private void sendMessage(WhatsAppMessageRequest request) {
        if (apiToken == null || apiToken.isEmpty() || phoneNumberId == null || phoneNumberId.isEmpty()) {
            log.warn("WhatsApp API Token or Phone Number ID is missing. Skipping message send to {}", request.getTo());
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
