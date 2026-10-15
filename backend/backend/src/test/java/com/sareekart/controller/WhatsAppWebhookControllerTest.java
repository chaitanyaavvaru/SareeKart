package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.whatsapp.WhatsAppWebhookDto;
import com.sareekart.security.WhatsAppWebhookSignatureValidator;
import com.sareekart.service.WhatsAppWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock
    private WhatsAppWebhookService webhookService;

    @Mock
    private WhatsAppWebhookSignatureValidator signatureValidator;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "verifyToken", "sareekart-verify-token");
    }

    @Test
    @DisplayName("GET verifyWebhook returns 200 and challenge when token matches, without leaking token in logs")
    void testVerifyWebhook_Success() {
        Logger logger = (Logger) LoggerFactory.getLogger(WhatsAppWebhookController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            ResponseEntity<String> response = controller.verifyWebhook("subscribe", "sareekart-verify-token", "challenge_abc_123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("challenge_abc_123");

            for (ILoggingEvent event : listAppender.list) {
                String msg = event.getFormattedMessage();
                assertFalse(msg.contains("sareekart-verify-token"), "Verify token must NOT appear in log: " + msg);
            }
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("GET verifyWebhook returns 403 when token fails to match, without leaking token in logs")
    void testVerifyWebhook_InvalidToken() {
        Logger logger = (Logger) LoggerFactory.getLogger(WhatsAppWebhookController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            ResponseEntity<String> response = controller.verifyWebhook("subscribe", "bad-token", "challenge_abc_123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            for (ILoggingEvent event : listAppender.list) {
                String msg = event.getFormattedMessage();
                assertFalse(msg.contains("bad-token"), "Untrusted token must NOT appear in log: " + msg);
            }
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("POST receiveWebhook accepts valid HMAC-SHA256 signature and returns 200 OK")
    void testReceiveWebhook_ValidSignature() {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=valid_hash";

        when(signatureValidator.isValid(payload, signature)).thenReturn(true);

        ResponseEntity<Void> response = controller.receiveWebhook(signature, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, times(1)).processWebhook(any(WhatsAppWebhookDto.class));
    }

    @Test
    @DisplayName("POST receiveWebhook rejects invalid signature with 401 Unauthorized")
    void testReceiveWebhook_InvalidSignature() {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=tampered_signature";

        when(signatureValidator.isValid(payload, signature)).thenReturn(false);

        ResponseEntity<Void> response = controller.receiveWebhook(signature, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(webhookService, never()).processWebhook(any());
    }

    @Test
    @DisplayName("POST receiveWebhook handles signed null/empty payload gracefully with 200 OK")
    void testReceiveWebhook_EmptyPayload() {
        when(signatureValidator.isValid(any(byte[].class), eq("sha256=hash"))).thenReturn(true);

        ResponseEntity<Void> response = controller.receiveWebhook("sha256=hash", new byte[0]);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, never()).processWebhook(any());
    }

    @Test
    @DisplayName("POST receiveWebhook rejects empty payload when signature is missing or invalid")
    void testReceiveWebhook_EmptyPayload_InvalidSignature() {
        when(signatureValidator.isValid(any(byte[].class), isNull())).thenReturn(false);

        ResponseEntity<Void> response = controller.receiveWebhook(null, new byte[0]);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(webhookService, never()).processWebhook(any());
    }
}
