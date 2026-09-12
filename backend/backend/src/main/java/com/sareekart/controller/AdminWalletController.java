package com.sareekart.controller;

import com.sareekart.dto.request.WalletCreditRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.WalletResponse;
import com.sareekart.dto.response.WalletTransactionResponse;
import com.sareekart.entity.User;
import com.sareekart.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wallets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getAllWallets() {
        log.info("Staff requesting all patron wallets");
        List<WalletResponse> list = walletService.getAllWalletsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Wallets fetched successfully", list));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getUserTransactions(@PathVariable Long userId) {
        log.info("Staff requesting audit ledger for user {}", userId);
        List<WalletTransactionResponse> list = walletService.getTransactionsForAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success("User wallet transactions fetched successfully", list));
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<WalletResponse>> issuePromotionalCredit(
            @PathVariable Long userId,
            @AuthenticationPrincipal User staffUser,
            @Valid @RequestBody WalletCreditRequest request) {
        log.info("Staff {} issuing promotional credit to user {}", staffUser != null ? staffUser.getEmail() : "anon", userId);
        WalletResponse response = walletService.adminAdjustCredit(userId, request, staffUser);
        return ResponseEntity.ok(ApiResponse.success("Credit bonus disbursed successfully", response));
    }
}
