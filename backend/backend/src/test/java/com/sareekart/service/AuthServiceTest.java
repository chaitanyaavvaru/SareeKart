package com.sareekart.service;

import com.sareekart.dto.auth.LoginRequest;
import com.sareekart.dto.auth.RegisterRequest;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.RoleName;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.RoleRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for registration/login business rules — repositories mocked,
 * no database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder,
            jwtTokenProvider, authenticationManager);
    }

    private RegisterRequest sampleRegistration() {
        return RegisterRequest.builder()
            .firstName("Asha")
            .lastName("Weaver")
            .email("asha@example.com")
            .password("sareeSecret1")
            .phone("9876543210")
            .build();
    }

    @Test
    @DisplayName("register encodes password, assigns CUSTOMER role and returns token")
    void registerSuccess() {
        RegisterRequest request = sampleRegistration();
        Role customerRole = new Role(RoleName.ROLE_CUSTOMER, "customer");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("sareeSecret1")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateTokenFromUsername(any(), any())).thenReturn("jwt-token");

        var response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("asha@example.com");
        assertThat(response.getUser().getRoles()).containsExactly("ROLE_CUSTOMER");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$encoded$");
        assertThat(captor.getValue().getRoles()).extracting(Role::getName)
            .containsExactly(RoleName.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("register rejects an already-registered email without touching the database")
    void registerDuplicateEmailRejected() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(sampleRegistration()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login delegates to AuthenticationManager, stamps lastLogin, returns token")
    void loginSuccess() {
        LoginRequest request = LoginRequest.builder()
            .email("asha@example.com").password("sareeSecret1").build();

        User user = new User();
        user.setEmail("asha@example.com");
        user.setPassword("$encoded$");
        user.setFirstName("Asha");
        user.setLastName("Weaver");
        user.setEnabled(true);
        Role role = new Role(RoleName.ROLE_CUSTOMER, "customer");
        user.setRoles(Set.of(role));

        Authentication auth = new UsernamePasswordAuthenticationToken(
            "asha@example.com", null, java.util.List.of());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(auth);
        when(userRepository.findByEmailWithRoles("asha@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(auth)).thenReturn("login-jwt");

        var response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("login-jwt");
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("bad credentials propagate as BadCredentialsException")
    void loginBadCredentials() {
        LoginRequest request = LoginRequest.builder()
            .email("asha@example.com").password("wrong").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("nope"));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("changePassword rejects a mismatched current password")
    void changePasswordWrongCurrent() {
        User user = new User();
        user.setEmail("asha@example.com");
        user.setPassword("$encoded$");

        when(userRepository.findByEmailWithRoles("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("not-it", "$encoded$")).thenReturn(false);

        assertThatThrownBy(() ->
            authService.changePassword("asha@example.com", "not-it", "newSecret1"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }
}