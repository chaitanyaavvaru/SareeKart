package com.sareekart.controller;

import com.sareekart.dto.request.ForgotPasswordRequest;
import com.sareekart.dto.request.GenerateRecoveryKeyRequest;
import com.sareekart.dto.request.LoginRequest;
import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.request.ResetPasswordRequest;
import com.sareekart.dto.request.VerifyRecoveryKeyRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.dto.response.RecoveryKeyResponse;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.security.RecoveryRateLimiterService;
import com.sareekart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final RecoveryRateLimiterService recoveryRateLimiterService;

    public AuthController(UserService userService) {
        this.userService = userService;
        this.recoveryRateLimiterService = null;
    }

    @Autowired
    public AuthController(UserService userService, RecoveryRateLimiterService recoveryRateLimiterService) {
        this.userService = userService;
        this.recoveryRateLimiterService = recoveryRateLimiterService;
    }

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${app.auth.expose-reset-token-in-response:false}")
    private boolean exposeResetTokenInResponse;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        User user = userService.getCurrentUser();
        // Clear password in response for security
        user.setPassword(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = userService.requestPasswordReset(request.getEmail());
        Map<String, String> data = new HashMap<>();
        if (token != null && exposeResetTokenInResponse) {
            data.put("resetToken", token);
            data.put("resetUrl", frontendUrl + "/reset-password?token=" + token);
        }
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for this email, password reset instructions have been sent.",
                data
        ));
    }

    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(@RequestParam String token) {
        boolean valid = userService.verifyResetToken(token);
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired password reset token.", false));
        }
        return ResponseEntity.ok(ApiResponse.success("Token is valid", true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been successfully reset. Please sign in with your new password.", "SUCCESS"));
    }

    @PostMapping("/verify-recovery-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyRecoveryKey(
            @Valid @RequestBody VerifyRecoveryKeyRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = recoveryRateLimiterService != null ? recoveryRateLimiterService.resolveClientIp(httpRequest) : "UNKNOWN";
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";

        if (recoveryRateLimiterService != null) {
            recoveryRateLimiterService.checkRateLimit(clientIp, normalizedEmail);
        }

        try {
            String token = userService.verifyRecoveryKeyAndGenerateResetToken(request.getEmail(), request.getRecoveryKey());
            Map<String, String> data = new HashMap<>();
            data.put("resetToken", token);
            data.put("resetUrl", frontendUrl + "/reset-password?token=" + token);
            return ResponseEntity.ok(ApiResponse.success("Emergency Security Recovery Key verified successfully.", data));
        } catch (BadRequestException ex) {
            if (recoveryRateLimiterService != null) {
                recoveryRateLimiterService.recordFailedAttempt(clientIp, normalizedEmail);
            }
            throw ex;
        }
    }

    @PostMapping("/generate-recovery-key")
    public ResponseEntity<ApiResponse<RecoveryKeyResponse>> generateRecoveryKey(
            @Valid @RequestBody GenerateRecoveryKeyRequest request) {
        RecoveryKeyResponse response = userService.generateAndProvisionRecoveryKey(request.getCurrentPassword());
        return ResponseEntity.ok(ApiResponse.success("Emergency Security Recovery Key provisioned successfully.", response));
    }
}
