package com.sareekart.security;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Utility for cryptographically secure generation, canonicalization, and validation
 * of Emergency Security Recovery Keys.
 *
 * <p>Each generated key contains at least 128 bits of cryptographic entropy derived from
 * {@link SecureRandom}, formatted into 8 hyphen-delimited groups of 4 hexadecimal characters:
 * {@code SK-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX} (32 hex characters = 128 bits).
 */
public final class RecoveryKeyUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "SK-";

    private RecoveryKeyUtil() {
        // Utility class
    }

    /**
     * Generates a new high-entropy emergency recovery key with at least 128 bits of entropy.
     *
     * @return Formatted recovery key string (e.g. SK-A1B2-C3D4-E5F6-7890-ABCD-EF01-2345-6789)
     */
    public static String generateRecoveryKey() {
        byte[] entropy = new byte[16]; // 16 bytes * 8 = 128 bits
        SECURE_RANDOM.nextBytes(entropy);

        StringBuilder hex = new StringBuilder();
        for (byte b : entropy) {
            hex.append(String.format("%02X", b));
        }

        // Format as SK-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX (8 groups of 4)
        StringBuilder formatted = new StringBuilder(KEY_PREFIX);
        for (int i = 0; i < hex.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append("-");
            }
            formatted.append(hex.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Canonicalizes an input recovery key for consistent hashing and constant-time verification.
     * Strips leading "SK-" or "SK", removes hyphens and spaces, and converts to uppercase.
     *
     * @param rawKey raw input from user
     * @return canonical normalized key string, or empty string if input is blank
     */
    public static String canonicalize(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return "";
        }
        String clean = rawKey.trim().toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace(" ", "");

        if (clean.startsWith("SK")) {
            clean = clean.substring(2);
        }
        return clean;
    }

    /**
     * Validates whether a candidate recovery key conforms to the acceptable format
     * before performing cryptographic hashing or verification.
     *
     * @param rawKey candidate key
     * @return true if valid format, false otherwise
     */
    public static boolean isValidFormat(String rawKey) {
        String canonical = canonicalize(rawKey);
        // Canonical string must be alphanumeric, between 16 and 64 characters (standard 128-bit hex is 32 chars)
        return canonical.matches("^[0-9A-F]{32}$") || canonical.matches("^[0-9A-Z]{16,64}$");
    }
}
