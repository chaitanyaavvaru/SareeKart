package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sareekart.dto.trousseau.TrousseauAiCurationRequest;
import com.sareekart.dto.trousseau.TrousseauAiCurationResponse;
import com.sareekart.entity.User;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.service.TrousseauAiCurationService;
import com.sareekart.service.TrousseauService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrousseauAiCurationControllerSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TrousseauService trousseauService;

    @Mock
    private TrousseauAiCurationService trousseauAiCurationService;

    @Mock
    private com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService;

    private MockMvc mockMvc;

    private User brideUser;
    private User intruderUser;

    @BeforeEach
    void setUp() {
        brideUser = User.builder().id(101L).email("bride@example.com").firstName("Kavya").build();
        intruderUser = User.builder().id(202L).email("intruder@example.com").firstName("Intruder").build();

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                String authHeader = webRequest.getHeader("X-Simulate-User");
                if ("intruder".equalsIgnoreCase(authHeader)) {
                    return intruderUser;
                }
                return brideUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(new TrousseauBoardController(trousseauService, trousseauAiCurationService, trousseauWhatsAppService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    @DisplayName("Stage 6 Controller Test 1: POST /api/trousseau/{boardId}/ceremonies/{ceremonyId}/curate returns 200 OK")
    void testCurateCeremonySuccess() throws Exception {
        TrousseauAiCurationRequest req = TrousseauAiCurationRequest.builder()
                .ceremonyType("MUHURTHAM")
                .colorTheme("Crimson & Gold")
                .maxBudget(new BigDecimal("100000.00"))
                .build();

        TrousseauAiCurationResponse res = TrousseauAiCurationResponse.builder()
                .boardId(42L)
                .ceremonyId(10L)
                .ceremonyType("MUHURTHAM")
                .colorTheme("Crimson & Gold")
                .ceremonyBudget(new BigDecimal("100000.00"))
                .overallStylingNote("Sacred heritage curation")
                .recommendations(Collections.emptyList())
                .fallbackUsed(false)
                .totalCandidatesEvaluated(5)
                .build();

        when(trousseauAiCurationService.curateCeremonyEnsemble(eq(101L), eq(42L), eq(10L), any(TrousseauAiCurationRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/ceremonies/10/curate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ceremonyType").value("MUHURTHAM"))
                .andExpect(jsonPath("$.data.ceremonyBudget").value(100000.00));
    }

    @Test
    @DisplayName("Stage 6 Controller Test 2: Intruder accessing another family's curation returns 403 Forbidden")
    void testCurateCeremonyIntruderAccessDenied() throws Exception {
        TrousseauAiCurationRequest req = TrousseauAiCurationRequest.builder()
                .ceremonyType("MUHURTHAM")
                .build();

        when(trousseauAiCurationService.curateCeremonyEnsemble(eq(202L), eq(42L), eq(10L), any(TrousseauAiCurationRequest.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to access this trousseau board"));

        mockMvc.perform(post("/api/trousseau/42/ceremonies/10/curate")
                        .header("X-Simulate-User", "intruder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
    }

    @Test
    @DisplayName("Stage 6 Controller Test 3: Curating non-existent ceremony returns 404 Not Found")
    void testCurateCeremonyNotFound() throws Exception {
        TrousseauAiCurationRequest req = TrousseauAiCurationRequest.builder().build();

        when(trousseauAiCurationService.curateCeremonyEnsemble(eq(101L), eq(42L), eq(999L), any(TrousseauAiCurationRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ceremony not found: 999"));

        mockMvc.perform(post("/api/trousseau/42/ceremonies/999/curate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ceremony not found: 999"));
    }
}
