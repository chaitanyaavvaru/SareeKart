package com.sareekart.service;

import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.entity.User;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import com.sareekart.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerNormalizesStoredUserFields() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("  Generated  ")
                .lastName("  Test  ")
                .email("  Registration@Example.TEST  ")
                .mobile(" 9999999999 ")
                .password("generated123")
                .build();

        when(userRepository.existsByEmail("registration@example.test")).thenReturn(false);
        when(passwordEncoder.encode("generated123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateToken("registration@example.test")).thenReturn("generated-token");

        AuthResponse response = userService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("Generated", savedUser.getValue().getFirstName());
        assertEquals("Test", savedUser.getValue().getLastName());
        assertEquals("registration@example.test", savedUser.getValue().getEmail());
        assertEquals("9999999999", savedUser.getValue().getMobile());
        assertEquals("generated-token", response.getToken());
    }
}
