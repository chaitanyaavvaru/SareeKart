package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class RegistrationFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new AuthController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void validRegistrationReturnsCreatedResponseWithToken() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(
                AuthResponse.builder()
                        .token("generated-test-token")
                        .id(99L)
                        .firstName("Generated")
                        .email("registration@example.test")
                        .role("CUSTOMER")
                        .build()
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("generated-test-token"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void duplicateEmailReturnsClearBadRequest() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email address already in use."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email address already in use."));
    }

    @Test
    void invalidMobileReturnsFieldValidationMessage() throws Exception {
        RegisterRequest request = validRequest();
        request.setMobile("98765abcde");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mobile number must contain only digits")));
    }

    private RegisterRequest validRequest() {
        return RegisterRequest.builder()
                .firstName("Generated")
                .lastName("Test")
                .email("registration-" + UUID.randomUUID() + "@example.test")
                .mobile("9999999999")
                .password("generated123")
                .build();
    }
}
