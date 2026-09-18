package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sareekart.dto.trousseau.CastVoteRequest;
import com.sareekart.dto.trousseau.SharedTrousseauViewResponse;
import com.sareekart.dto.trousseau.TrousseauVoteResponse;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.service.TrousseauRateLimiter;
import com.sareekart.service.TrousseauService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrousseauShareControllerSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TrousseauService trousseauService;

    private TrousseauRateLimiter rateLimiter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rateLimiter = new TrousseauRateLimiter();
        rateLimiter.reset();

        mockMvc = MockMvcBuilders.standaloneSetup(new TrousseauShareController(trousseauService, rateLimiter))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 1: GET /api/trousseau/share/{token} returns 200 OK without customer PII")
    void testGetSharedBoardSuccess() throws Exception {
        SharedTrousseauViewResponse response = SharedTrousseauViewResponse.builder()
                .shareToken("tkn_sample_valid_token_123")
                .title("Kavya's Wedding Trousseau")
                .brideOrOwnerName("Kavya")
                .weddingDate(LocalDate.now().plusMonths(3))
                .status("ACTIVE")
                .isPublicVoting(true)
                .totalCeremonies(1)
                .totalBudget(new BigDecimal("150000.00"))
                .ceremonies(Collections.emptyList())
                .build();

        when(trousseauService.getSharedBoardByToken("tkn_sample_valid_token_123")).thenReturn(response);

        mockMvc.perform(get("/api/trousseau/share/tkn_sample_valid_token_123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareToken").value("tkn_sample_valid_token_123"))
                .andExpect(jsonPath("$.data.title").value("Kavya's Wedding Trousseau"))
                .andExpect(jsonPath("$.data.brideOrOwnerName").value("Kavya"))
                // PII protection: userId, email, phone must not be present in public DTO
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 2: GET /api/trousseau/share/{token} with invalid token returns 404")
    void testGetSharedBoardNotFound() throws Exception {
        when(trousseauService.getSharedBoardByToken("tkn_invalid_unknown"))
                .thenThrow(new ResourceNotFoundException("Invalid or expired trousseau share link"));

        mockMvc.perform(get("/api/trousseau/share/tkn_invalid_unknown")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired trousseau share link"));
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 3: POST /api/trousseau/share/{token}/items/{itemId}/vote records vote and masks phone")
    void testCastVoteSuccess() throws Exception {
        CastVoteRequest req = CastVoteRequest.builder()
                .voterName("Aunt Meena")
                .voterPhone("+919876543210")
                .reaction("LOVE")
                .note("Perfect bridal red!")
                .build();

        TrousseauVoteResponse res = TrousseauVoteResponse.builder()
                .id(1L)
                .itemId(10L)
                .voterName("Aunt Meena")
                .voterPhoneMasked("+91****3210")
                .reaction("LOVE")
                .note("Perfect bridal red!")
                .createdAt(LocalDateTime.now())
                .build();

        when(trousseauService.castVote(eq("tkn_valid"), eq(10L), any(CastVoteRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/share/tkn_valid/items/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.voterName").value("Aunt Meena"))
                .andExpect(jsonPath("$.data.reaction").value("LOVE"))
                .andExpect(jsonPath("$.data.voterPhoneMasked").value("+91****3210"));
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 4: POST vote with missing mandatory fields fails with 400 Bad Request")
    void testCastVoteValidationFailure() throws Exception {
        CastVoteRequest req = CastVoteRequest.builder()
                .voterName("") // blank voterName
                .reaction(null) // null reaction
                .build();

        mockMvc.perform(post("/api/trousseau/share/tkn_valid/items/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(trousseauService, never()).castVote(any(), any(), any());
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 5: Sliding-window rate limiter returns 429 Too Many Requests after 15 requests/min")
    void testVoteRateLimitingEnforcement() throws Exception {
        CastVoteRequest req = CastVoteRequest.builder()
                .voterName("Fast Voter")
                .reaction("LOVE")
                .build();

        TrousseauVoteResponse res = TrousseauVoteResponse.builder()
                .id(1L)
                .itemId(10L)
                .voterName("Fast Voter")
                .reaction("LOVE")
                .build();

        when(trousseauService.castVote(eq("tkn_rate_limit"), eq(10L), any(CastVoteRequest.class))).thenReturn(res);

        String clientIp = "192.168.1.100";

        // First 15 requests from same IP should succeed (200 OK)
        for (int i = 1; i <= 15; i++) {
            mockMvc.perform(post("/api/trousseau/share/tkn_rate_limit/items/10/vote")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        // 16th request from same IP must be rejected with 429 Too Many Requests
        mockMvc.perform(post("/api/trousseau/share/tkn_rate_limit/items/10/vote")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Maximum 15 votes per minute allowed."));

        // Verify service was called exactly 15 times, not 16
        verify(trousseauService, times(15)).castVote(eq("tkn_rate_limit"), eq(10L), any(CastVoteRequest.class));
    }

    @Test
    @DisplayName("Stage 5 Public Controller Test 6: GET /api/trousseau/share/{token}/items/{itemId}/votes returns votes list")
    void testGetItemVotesSuccess() throws Exception {
        TrousseauVoteResponse v1 = TrousseauVoteResponse.builder()
                .id(1L)
                .itemId(10L)
                .voterName("Aunt Meena")
                .reaction("LOVE")
                .build();

        when(trousseauService.getItemVotes("tkn_valid", 10L)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/trousseau/share/tkn_valid/items/10/votes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].voterName").value("Aunt Meena"))
                .andExpect(jsonPath("$.data[0].reaction").value("LOVE"));
    }
}
