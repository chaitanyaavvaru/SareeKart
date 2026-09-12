package com.sareekart.service;

import com.sareekart.dto.request.WalletCreditRequest;
import com.sareekart.dto.response.WalletRedemptionPreviewResponse;
import com.sareekart.dto.response.WalletResponse;
import com.sareekart.dto.response.WalletTransactionResponse;
import com.sareekart.entity.User;
import com.sareekart.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(User user);
    Wallet getOrCreateWallet(Long userId);
    WalletResponse getWalletResponse(User user);
    WalletResponse getWalletResponse(Long userId);
    List<WalletTransactionResponse> getTransactions(Long userId);
    WalletRedemptionPreviewResponse previewRedemption(Long userId, BigDecimal orderTotal);

    Wallet creditReturnRefund(Long userId, BigDecimal amount, Long returnId);
    Wallet creditLoyaltyEarned(Long userId, BigDecimal orderTotal, Long orderId);
    Wallet redeemWallet(User user, BigDecimal requestedAmount, Long orderId);
    Wallet revertOrderRedemption(Long userId, BigDecimal amount, Long orderId);

    // Staff / Admin methods
    List<WalletResponse> getAllWalletsForAdmin();
    List<WalletTransactionResponse> getTransactionsForAdmin(Long userId);
    WalletResponse adminAdjustCredit(Long userId, WalletCreditRequest request, User staffUser);
}
