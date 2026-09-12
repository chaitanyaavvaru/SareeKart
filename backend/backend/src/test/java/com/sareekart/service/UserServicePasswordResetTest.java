package com.sareekart.service;

import com.sareekart.entity.PasswordResetToken;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.PasswordResetTokenRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import com.sareekart.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordResetTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(10L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@example.com")
                .password("oldHashedPassword")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    void requestPasswordReset_generatesTokenAndInvalidatesOld() {
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(sampleUser));
        PasswordResetToken oldToken = PasswordResetToken.builder()
                .id(1L)
                .token("old-token-123")
                .user(sampleUser)
                .used(false)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();
        List<PasswordResetToken> oldTokens = new ArrayList<>(List.of(oldToken));
        when(passwordResetTokenRepository.findAllByUserAndUsedFalse(sampleUser)).thenReturn(oldTokens);

        String generatedToken = userService.requestPasswordReset("  priya@example.com  ");

        assertNotNull(generatedToken);
        assertFalse(generatedToken.isBlank());
        assertTrue(oldToken.getUsed());
        verify(passwordResetTokenRepository).saveAll(oldTokens);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertEquals(sampleUser, tokenCaptor.getValue().getUser());
        assertEquals(generatedToken, tokenCaptor.getValue().getToken());
        assertFalse(tokenCaptor.getValue().getUsed());
        assertTrue(tokenCaptor.getValue().getExpiryDate().isAfter(LocalDateTime.now()));
    }

    @Test
    void requestPasswordReset_nonExistentUserReturnsNullWithoutThrowing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        String result = userService.requestPasswordReset("ghost@example.com");

        assertNull(result);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void verifyResetToken_returnsTrueForValidUnusedToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid-uuid")
                .user(sampleUser)
                .used(false)
                .expiryDate(LocalDateTime.now().plusMinutes(25))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("valid-uuid")).thenReturn(Optional.of(token));

        assertTrue(userService.verifyResetToken("valid-uuid"));
    }

    @Test
    void verifyResetToken_returnsFalseForExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired-uuid")
                .user(sampleUser)
                .used(false)
                .expiryDate(LocalDateTime.now().minusMinutes(5))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("expired-uuid")).thenReturn(Optional.of(token));

        assertFalse(userService.verifyResetToken("expired-uuid"));
    }

    @Test
    void resetPassword_successfullyUpdatesPasswordAndMarksTokenUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("active-token")
                .user(sampleUser)
                .used(false)
                .expiryDate(LocalDateTime.now().plusMinutes(20))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("active-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecretPass123")).thenReturn("newHashedPassword");

        userService.resetPassword("active-token", "newSecretPass123");

        assertEquals("newHashedPassword", sampleUser.getPassword());
        assertTrue(token.getUsed());
        verify(userRepository).save(sampleUser);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPassword_throwsExceptionForShortPassword() {
        assertThrows(BadRequestException.class, () ->
                userService.resetPassword("any-token", "12345")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_throwsExceptionForExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired-token")
                .user(sampleUser)
                .used(false)
                .expiryDate(LocalDateTime.now().minusMinutes(10))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("expired-token")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () ->
                userService.resetPassword("expired-token", "validPass123")
        );
        assertTrue(token.getUsed());
        verify(passwordResetTokenRepository).save(token);
        verify(userRepository, never()).save(any());
    }
}
