package com.sareekart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-fast contract for gateway configuration.
 */
class RazorpayPropertiesValidationTest {

    private RazorpayProperties props(boolean enabled, String keyId, String keySecret, String whsec) {
        RazorpayProperties p = new RazorpayProperties();
        p.setEnabled(enabled);
        p.setKeyId(keyId);
        p.setKeySecret(keySecret);
        p.setWebhookSecret(whsec);
        return p;
    }

    @Test
    @DisplayName("enabled + missing keyId fails fast at boot")
    void missingKeyIdFails() {
        assertThatThrownBy(() -> props(true, "", "sec", "wh").validate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RAZORPAY_KEY_ID");
    }

    @Test
    @DisplayName("enabled + missing key secret fails fast")
    void missingKeySecretFails() {
        assertThatThrownBy(() -> props(true, "id", "", "wh").validate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("KEY_SECRET");
    }

    @Test
    @DisplayName("enabled + missing webhook secret fails fast")
    void missingWebhookSecretFails() {
        assertThatThrownBy(() -> props(true, "id", "sec", "").validate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("WEBHOOK_SECRET");
    }

    @Test
    @DisplayName("enabled + all credentials present boots cleanly")
    void completeConfigPasses() {
        assertThatCode(() -> props(true, "id", "sec", "wh").validate())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("COD-only mode (disabled) boots without any credentials")
    void disabledModeNeedsNothing() {
        assertThatCode(() -> props(false, "", "", "").validate())
            .doesNotThrowAnyException();
    }
}