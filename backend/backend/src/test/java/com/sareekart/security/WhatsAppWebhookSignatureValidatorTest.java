package com.sareekart.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppWebhookSignatureValidatorTest {

    private WhatsAppWebhookSignatureValidator validator;
    private final String testSecret = "my-test-secret-key-12345";

    @BeforeEach
    void setUp() {
        validator = new WhatsAppWebhookSignatureValidator();
        ReflectionTestUtils.setField(validator, "appSecret", testSecret);
    }

    private String calculateHmac(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("Valid HMAC-SHA256 signature returns true")
    void testValidSignature() throws Exception {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String validHash = calculateHmac(payload, testSecret);
        String header = "sha256=" + validHash;

        boolean valid = validator.isValid(payload, header);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Forged/Tampered HMAC-SHA256 signature returns false")
    void testForgedSignature() {
        byte[] payload = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String forgedHeader = "sha256=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

        boolean valid = validator.isValid(payload, forgedHeader);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Missing sha256= prefix returns false in production secret mode")
    void testMissingPrefix() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        boolean valid = validator.isValid(payload, "invalid_prefix_hash");

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Null payload returns false")
    void testNullPayload() {
        boolean valid = validator.isValid(null, "sha256=123456");

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Missing signature header always returns false — dev-bypass is removed")
    void testMissingHeaderAlwaysRejected() {
        // Even with a non-empty secret, a missing signature header must be rejected.
        // This test confirms the dev-bypass (which previously allowed null headers with
        // the default secret) has been completely removed.
        byte[] payload = "{\"test\":true}".getBytes(StandardCharsets.UTF_8);

        boolean valid = validator.isValid(payload, null);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Old default hard-coded secret cannot bypass signature validation")
    void testDefaultHardcodedSecretCannotBypass() {
        // The old default sareekart-meta-secret-2026 must never grant special access.
        // A request with the old default key but no signature must be rejected.
        ReflectionTestUtils.setField(validator, "appSecret", "sareekart-meta-secret-2026");
        byte[] payload = "{\"test\":true}".getBytes(StandardCharsets.UTF_8);

        // Missing header → rejected
        assertThat(validator.isValid(payload, null)).isFalse();

        // Blank/malformed header → rejected
        assertThat(validator.isValid(payload, "")).isFalse();
        assertThat(validator.isValid(payload, "no-prefix")).isFalse();
    }

    @Test
    @DisplayName("Absent production secret (empty string) rejects all requests")
    void testAbsentProductionSecretRejectsAll() throws Exception {
        // When WHATSAPP_APP_SECRET env var is not set, the validator resolves to an empty string.
        // The validator must reject every request rather than silently failing open.
        ReflectionTestUtils.setField(validator, "appSecret", "");
        byte[] payload = "{\"test\":true}".getBytes(StandardCharsets.UTF_8);

        // Even a correctly formatted sha256= header is rejected when secret is absent
        String someHash = calculateHmac(payload, "some-key");
        assertThat(validator.isValid(payload, "sha256=" + someHash)).isFalse();
        assertThat(validator.isValid(payload, null)).isFalse();
    }
}
