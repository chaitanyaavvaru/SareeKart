package com.sareekart.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context, no database.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs512-algorithm-requirements!!";
    private static final long EXPIRATION_MS = 60_000L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", EXPIRATION_MS);
        provider.init();
    }

    @Test
    @DisplayName("generated token carries subject and roles claim")
    void generateAndParse() {
        String token = provider.generateTokenFromUsername("admin@sareekart.com", "ROLE_ADMIN");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("admin@sareekart.com");

        Claims claims = io.jsonwebtoken.Jwts.parser()
            .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("tampered token is rejected")
    void rejectsTamperedToken() {
        String token = provider.generateTokenFromUsername("user@sareekart.com", "ROLE_CUSTOMER");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("garbage string is rejected without throwing")
    void rejectsGarbage() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("expired token fails validation")
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", -1_000L);
        provider.init();
        String token = provider.generateTokenFromUsername("user@sareekart.com", "ROLE_CUSTOMER");

        assertThat(provider.validateToken(token)).isFalse();
    }
}