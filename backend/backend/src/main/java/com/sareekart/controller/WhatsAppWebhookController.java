package com.sareekart.controller;

import com.sareekart.dto.whatsapp.WhatsAppWebhookDto;
import com.sareekart.security.WhatsAppWebhookSignatureValidator;
import com.sareekart.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;
    private final WhatsAppWebhookSignatureValidator signatureValidator;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${whatsapp.webhook.verify-token:sareekart-verify-token}")
    private String verifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        log.info("Received WhatsApp Webhook Verification Request: mode={}, token={}, challenge={}", mode, token, challenge);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verified successfully.");
            return ResponseEntity.ok(challenge);
        }

        log.error("Webhook verification failed.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) byte[] payloadBytes) {

        byte[] rawBytes = (payloadBytes != null) ? payloadBytes : new byte[0];

        if (!signatureValidator.isValid(rawBytes, signature)) {
            log.warn("Unauthorized WhatsApp Webhook: Invalid or missing HMAC signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (rawBytes.length == 0) {
            return ResponseEntity.ok().build();
        }

        try {
            WhatsAppWebhookDto payload = objectMapper.readValue(payloadBytes, WhatsAppWebhookDto.class);
            log.debug("Received WhatsApp Webhook Payload: {}", payload);
            webhookService.processWebhook(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing WhatsApp Webhook payload", e);
            // Return 200 OK so Meta doesn't continuously retry bad payloads
            return ResponseEntity.ok().build();
        }
    }
}
