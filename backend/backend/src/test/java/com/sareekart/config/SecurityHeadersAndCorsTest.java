package com.sareekart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersAndCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Security Headers: Responses include X-Content-Type-Options, X-Frame-Options DENY, and HSTS")
    void testSecurityHeadersEnforced() throws Exception {
        mockMvc.perform(get("/api/products")
                .secure(true))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("CORS Preflight: Allowed origin receives 200 OK and Access-Control-Allow-Origin header")
    void testCorsPreflightAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/products")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS Preflight: Untrusted origin is rejected without Access-Control-Allow-Origin")
    void testCorsPreflightUntrustedOrigin() throws Exception {
        mockMvc.perform(options("/api/products")
                .header("Origin", "http://malicious-attacker-site.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
