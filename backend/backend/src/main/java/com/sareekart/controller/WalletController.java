package com.sareekart.controller;

import com.sareekart.dto.request.WalletRedemptionPreviewRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.WalletRedemptionPreviewResponse;
import com.sareekart.dto.response.WalletResponse;
import com.sareekart.dto.response.WalletTransactionResponse;
import com.sareekart.entity.User;
import com.sareekart.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/my-wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal User user) {
        log.info("Fetching Patron Wallet for user {}", user != null ? user.getId() : "unknown");
        WalletResponse response = walletService.getWalletResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Patron wallet fetched successfully", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getMyTransactions(@AuthenticationPrincipal User user) {
        log.info("Fetching Patron Wallet transactions for user {}", user != null ? user.getId() : "unknown");
        List<WalletTransactionResponse> transactions = walletService.getTransactions(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Wallet transactions fetched successfully", transactions));
    }

    @PostMapping("/preview-redemption")
    public ResponseEntity<ApiResponse<WalletRedemptionPreviewResponse>> previewRedemption(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WalletRedemptionPreviewRequest request) {
        log.info("Previewing redemption for user {} on order total ₹{}", user != null ? user.getId() : "unknown", request.getOrderTotal());
        WalletRedemptionPreviewResponse response = walletService.previewRedemption(user.getId(), request.getOrderTotal());
        return ResponseEntity.ok(ApiResponse.success("Redemption preview calculated successfully", response));
    }
}
