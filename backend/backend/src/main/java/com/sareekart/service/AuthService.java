package com.sareekart.service;

import com.sareekart.dto.auth.AuthResponse;
import com.sareekart.dto.auth.LoginRequest;
import com.sareekart.dto.auth.RegisterRequest;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.RoleName;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.RoleRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
            .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRoles(java.util.Set.of(customerRole));

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String token = jwtTokenProvider.generateTokenFromUsername(
            user.getEmail(),
            user.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.joining(","))
        );

        return AuthResponse.from(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailWithRoles(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(authentication);
        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.from(token, user);
    }

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmailWithRoles(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthResponse.UserDto.from(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmailWithRoles(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }
}