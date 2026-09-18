package com.sareekart.controller;

import com.sareekart.dto.request.ForgotPasswordRequest;
import com.sareekart.dto.request.LoginRequest;
import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.request.ResetPasswordRequest;
import com.sareekart.dto.request.VerifyRecoveryKeyRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.entity.User;
import com.sareekart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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
        if (token != null) {
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
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyRecoveryKey(@Valid @RequestBody VerifyRecoveryKeyRequest request) {
        String token = userService.verifyRecoveryKeyAndGenerateResetToken(request.getEmail(), request.getRecoveryKey());
        Map<String, String> data = new HashMap<>();
        data.put("resetToken", token);
        data.put("resetUrl", frontendUrl + "/reset-password?token=" + token);
        return ResponseEntity.ok(ApiResponse.success("Emergency Security Recovery Key verified successfully.", data));
    }
}
