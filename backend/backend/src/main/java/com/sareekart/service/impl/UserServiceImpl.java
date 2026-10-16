package com.sareekart.service.impl;

import com.sareekart.dto.request.LoginRequest;
import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.dto.response.RecoveryKeyResponse;
import com.sareekart.entity.AuditLog;
import com.sareekart.entity.PasswordResetToken;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.AuditLogRepository;
import com.sareekart.repository.PasswordResetTokenRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.security.JwtTokenProvider;
import com.sareekart.security.RecoveryKeyUtil;
import com.sareekart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedFirstName = request.getFirstName().trim();
        String normalizedLastName = request.getLastName() == null ? null : request.getLastName().trim();
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedMobile = request.getMobile().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email address already in use.");
        }

        User user = User.builder()
                .firstName(normalizedFirstName)
                .lastName(normalizedLastName)
                .email(normalizedEmail)
                .mobile(normalizedMobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER) // Default role
                .build();

        User savedUser = userRepository.save(user);

        // Generate token immediately on registration
        String token = tokenProvider.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("No user logged in.");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email cannot be empty.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent user account");
            return null;
        }

        User user = userOpt.get();
        // Invalidate old active tokens for this user
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findAllByUserAndUsedFalse(user);
        for (PasswordResetToken t : oldTokens) {
            t.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(oldTokens);

        // Generate new token valid for 30 minutes
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token generated successfully");

        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyResetToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return passwordResetTokenRepository.findByTokenAndUsedFalse(token.trim())
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Password reset token cannot be empty.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset");
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public String verifyRecoveryKeyAndGenerateResetToken(String email, String recoveryKey) {
        if (email == null || email.isBlank() || recoveryKey == null || recoveryKey.isBlank()) {
            throw new BadRequestException("Invalid recovery details provided.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanKey = recoveryKey.trim().toUpperCase(Locale.ROOT);

        // Reject static demo keys immediately to eliminate backdoors (SEC-02)
        if ("SK-REC-CUST-2026".equalsIgnoreCase(cleanKey) || "SK-REC-ADMN-2026".equalsIgnoreCase(cleanKey)) {
            log.warn("Attempted use of deprecated static demo recovery key");
            throw new BadRequestException("Invalid recovery details provided.");
        }

        // Validate key format
        if (!RecoveryKeyUtil.isValidFormat(cleanKey)) {
            throw new BadRequestException("Invalid recovery details provided.");
        }

        // Concurrency-safe atomic retrieval with pessimistic write lock (SEC-03)
        User user = userRepository.findByEmailForUpdate(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid recovery details provided."));

        String storedHash = user.getRecoveryKeyHash();
        if (storedHash == null || storedHash.isBlank()) {
            throw new BadRequestException("Invalid recovery details provided.");
        }

        // Defend against legacy plaintext or malformed verifiers in persistent storage
        if (!isBcryptHash(storedHash)) {
            log.warn("Stored recovery verifier is legacy plaintext or malformed; safely invalidating in persistent storage");
            user.setRecoveryKeyHash(null);
            userRepository.saveAndFlush(user);
            throw new BadRequestException("Invalid recovery details provided.");
        }

        String canonicalCandidate = RecoveryKeyUtil.canonicalize(cleanKey);
        boolean matches;
        try {
            matches = passwordEncoder.matches(canonicalCandidate, storedHash);
        } catch (Exception ex) {
            log.warn("BCrypt verification exception encountered for recovery verifier: {}", ex.getMessage());
            user.setRecoveryKeyHash(null);
            userRepository.saveAndFlush(user);
            throw new BadRequestException("Invalid recovery details provided.");
        }

        if (!matches) {
            throw new BadRequestException("Invalid recovery details provided.");
        }

        // Atomic single-use consumption: clear the recovery key verifier immediately (SEC-03)
        user.setRecoveryKeyHash(null);
        userRepository.saveAndFlush(user);

        // Invalidate old active tokens for this user
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findAllByUserAndUsedFalse(user);
        for (PasswordResetToken t : oldTokens) {
            t.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(oldTokens);

        // Generate new token valid for 30 minutes
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Record audit event
        AuditLog auditLog = AuditLog.builder()
                .action("RECOVERY_KEY_CONSUMED")
                .entityType("User")
                .entityId(user.getId() != null ? user.getId().toString() : "UNKNOWN")
                .performedByUserId(user.getId())
                .performedByEmail(user.getEmail())
                .performedByRole(user.getRole() != null ? user.getRole().name() : "CUSTOMER")
                .details("Emergency recovery key successfully verified and consumed")
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Recovery key validated and consumed, password reset token issued");
        return token;
    }

    @Override
    public RecoveryKeyResponse generateAndProvisionRecoveryKey(String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new BadRequestException("Current password is required.");
        }

        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(currentPassword.trim(), currentUser.getPassword())) {
            throw new BadRequestException("Current password verification failed.");
        }

        // Generate cryptographically secure 128-bit key (SEC-01)
        String rawKey = RecoveryKeyUtil.generateRecoveryKey();
        String canonicalKey = RecoveryKeyUtil.canonicalize(rawKey);

        // Store salted one-way BCrypt verifier only (SEC-01)
        currentUser.setRecoveryKeyHash(passwordEncoder.encode(canonicalKey));
        userRepository.save(currentUser);

        // Record audit event
        AuditLog auditLog = AuditLog.builder()
                .action("RECOVERY_KEY_GENERATED")
                .entityType("User")
                .entityId(currentUser.getId() != null ? currentUser.getId().toString() : "UNKNOWN")
                .performedByUserId(currentUser.getId())
                .performedByEmail(currentUser.getEmail())
                .performedByRole(currentUser.getRole() != null ? currentUser.getRole().name() : "CUSTOMER")
                .details("Emergency recovery key generated and provisioned")
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Emergency recovery key generated for user ID [{}]", currentUser.getId());

        return RecoveryKeyResponse.builder()
                .recoveryKey(rawKey)
                .message("Store this emergency recovery key securely. It will not be shown again.")
                .build();
    }

    private static final java.util.regex.Pattern BCRYPT_PATTERN =
            java.util.regex.Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private boolean isBcryptHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        return BCRYPT_PATTERN.matcher(hash.trim()).matches();
    }
}
