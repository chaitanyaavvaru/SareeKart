package com.sareekart.service;

import com.sareekart.dto.request.LoginRequest;
import com.sareekart.dto.request.RegisterRequest;
import com.sareekart.dto.response.AuthResponse;
import com.sareekart.entity.User;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User getCurrentUser();

    String requestPasswordReset(String email);

    boolean verifyResetToken(String token);

    void resetPassword(String token, String newPassword);

    String verifyRecoveryKeyAndGenerateResetToken(String email, String recoveryKey);
}
