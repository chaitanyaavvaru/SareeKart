package com.sareekart.service.impl;

import com.sareekart.dto.request.WalletCreditRequest;
import com.sareekart.dto.response.WalletRedemptionPreviewResponse;
import com.sareekart.dto.response.WalletResponse;
import com.sareekart.dto.response.WalletTransactionResponse;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.Wallet;
import com.sareekart.entity.WalletTransaction;
import com.sareekart.enums.PatronTier;
import com.sareekart.enums.WalletTransactionType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WalletRepository;
import com.sareekart.repository.WalletTransactionRepository;
import com.sareekart.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("User must not be null to retrieve or create wallet");
        }
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .loyaltyPoints(0)
                            .tier(PatronTier.SILVER)
                            .lifetimeSpent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .build();
                    log.info("Initialized new Patron Wallet for user {} ({})", user.getId(), user.getEmail());
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID must not be null");
        }
        Optional<Wallet> existing = walletRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return getOrCreateWallet(user);
    }

    @Override
    @Transactional
    public WalletResponse getWalletResponse(User user) {
        Wallet wallet = getOrCreateWallet(user);
        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse getWalletResponse(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(Long userId) {
        return walletTransactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WalletRedemptionPreviewResponse previewRedemption(Long userId, BigDecimal orderTotal) {
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) < 0) {
            orderTotal = BigDecimal.ZERO;
        }
        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

        BigDecimal maxRedeemable = currentBalance.min(orderTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = orderTotal.subtract(maxRedeemable).setScale(2, RoundingMode.HALF_UP);
        boolean fullyCovered = remaining.compareTo(BigDecimal.ZERO) <= 0;

        return WalletRedemptionPreviewResponse.builder()
                .walletBalance(currentBalance)
                .maxRedeemable(maxRedeemable)
                .remainingOrderTotal(remaining)
                .fullyCovered(fullyCovered)
                .build();
    }

    @Override
    @Transactional
    public Wallet creditReturnRefund(Long userId, BigDecimal amount, Long returnId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Skipping wallet refund credit: non-positive amount {}", amount);
            return getOrCreateWallet(userId);
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal baseRefund = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = baseRefund.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredited = baseRefund.add(bonus);

        wallet.setBalance(wallet.getBalance().add(totalCredited));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(saved)
                .amount(totalCredited)
                .points(0)
                .type(WalletTransactionType.CREDIT_RETURN_REFUND)
                .description("Refund for Return #" + returnId + " (Base: ₹" + baseRefund + " + 5% Patronage Bonus: ₹" + bonus + ")")
                .referenceId(returnId)
                .referenceType("RETURN_REQUEST")
                .balanceAfter(saved.getBalance())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Credited ₹{} to user {} wallet for return claim #{}", totalCredited, userId, returnId);
        return saved;
    }

    @Override
    @Transactional
    public Wallet creditLoyaltyEarned(Long userId, BigDecimal orderTotal, Long orderId) {
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return getOrCreateWallet(userId);
        }

        Wallet wallet = getOrCreateWallet(userId);

        // Update lifetime spend and compute tier
        BigDecimal updatedLifetime = wallet.getLifetimeSpent().add(orderTotal).setScale(2, RoundingMode.HALF_UP);
        wallet.setLifetimeSpent(updatedLifetime);
        PatronTier newTier = PatronTier.calculateTier(updatedLifetime);
        wallet.setTier(newTier);

        // Base points: 1 point per 100 INR spent
        int basePoints = Math.max(1, orderTotal.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue());
        int earnedPoints = (int) Math.round(basePoints * newTier.getPointsMultiplier());

        wallet.setLoyaltyPoints(wallet.getLoyaltyPoints() + earnedPoints);
        BigDecimal creditEquivalent = BigDecimal.valueOf(earnedPoints).setScale(2, RoundingMode.HALF_UP);
        wallet.setBalance(wallet.getBalance().add(creditEquivalent));

        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(saved)
                .amount(creditEquivalent)
                .points(earnedPoints)
                .type(WalletTransactionType.CREDIT_LOYALTY_EARNED)
                .description("Patronage rewards for Order #" + orderId + " (" + newTier.getDisplayName() + " tier: " + earnedPoints + " pts earned)")
                .referenceId(orderId)
                .referenceType("ORDER")
                .balanceAfter(saved.getBalance())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Awarded {} loyalty points (₹{}) to user {} for order #{}", earnedPoints, creditEquivalent, userId, orderId);
        return saved;
    }

    @Override
    @Transactional
    public Wallet redeemWallet(User user, BigDecimal requestedAmount, Long orderId) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return getOrCreateWallet(user);
        }

        Wallet wallet = getOrCreateWallet(user);
        requestedAmount = requestedAmount.setScale(2, RoundingMode.HALF_UP);

        if (requestedAmount.compareTo(wallet.getBalance()) > 0) {
            throw new BadRequestException("Requested redemption amount ₹" + requestedAmount +
                    " exceeds available store credit balance ₹" + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(requestedAmount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(saved)
                .amount(requestedAmount.negate())
                .points(0)
                .type(WalletTransactionType.DEBIT_CHECKOUT_REDEMPTION)
                .description("Store credit applied to Order #" + orderId)
                .referenceId(orderId)
                .referenceType("ORDER")
                .balanceAfter(saved.getBalance())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Debited ₹{} from user {} wallet for order #{}", requestedAmount, user.getId(), orderId);
        return saved;
    }

    @Override
    @Transactional
    public Wallet revertOrderRedemption(Long userId, BigDecimal amount, Long orderId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getOrCreateWallet(userId);
        }

        Wallet wallet = getOrCreateWallet(userId);
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(saved)
                .amount(amount)
                .points(0)
                .type(WalletTransactionType.CREDIT_ORDER_CANCEL_REFUND)
                .description("Store credit refund for cancelled Order #" + orderId)
                .referenceId(orderId)
                .referenceType("ORDER")
                .balanceAfter(saved.getBalance())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Reverted ₹{} store credit to user {} for cancelled order #{}", amount, userId, orderId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWalletsForAdmin() {
        return walletRepository.findAllByOrderByBalanceDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactionsForAdmin(Long userId) {
        return getTransactions(userId);
    }

    @Override
    @Transactional
    public WalletResponse adminAdjustCredit(Long userId, WalletCreditRequest request, User staffUser) {
        validateStaffRole(staffUser);

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal amountToAdd = request.getAmount() != null ? request.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        int pointsToAdd = request.getPoints() != null ? request.getPoints() : 0;

        wallet.setBalance(wallet.getBalance().add(amountToAdd));
        wallet.setLoyaltyPoints(wallet.getLoyaltyPoints() + pointsToAdd);
        Wallet saved = walletRepository.save(wallet);

        String note = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : "Staff promotional credit injection by " + staffUser.getEmail();

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(saved)
                .amount(amountToAdd)
                .points(pointsToAdd)
                .type(WalletTransactionType.CREDIT_PROMO_BONUS)
                .description("Promotional bonus: " + note)
                .referenceId(staffUser.getId())
                .referenceType("ADMIN_MANUAL")
                .balanceAfter(saved.getBalance())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Staff {} injected ₹{} ({} pts) into user {} wallet", staffUser.getEmail(), amountToAdd, pointsToAdd, userId);
        return toResponse(saved);
    }

    private void validateStaffRole(User user) {
        if (user == null || user.getRole() == null || user.getRole() == Role.CUSTOMER) {
            throw new AccessDeniedException("Not authorised to perform this action");
        }
    }

    private WalletResponse toResponse(Wallet wallet) {
        User user = wallet.getUser();
        PatronTier tier = wallet.getTier() != null ? wallet.getTier() : PatronTier.SILVER;

        BigDecimal nextTierSpend = BigDecimal.ZERO;
        if (tier == PatronTier.SILVER) {
            nextTierSpend = PatronTier.GOLD.getMinimumSpend().subtract(wallet.getLifetimeSpent()).max(BigDecimal.ZERO);
        } else if (tier == PatronTier.GOLD) {
            nextTierSpend = PatronTier.ROYAL_PATRON.getMinimumSpend().subtract(wallet.getLifetimeSpent()).max(BigDecimal.ZERO);
        }

        int totalTx = wallet.getTransactions() != null ? wallet.getTransactions().size() : 0;

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : "")
                .userName(user != null ? (user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim() : "")
                .balance(wallet.getBalance())
                .loyaltyPoints(wallet.getLoyaltyPoints())
                .tier(tier.name())
                .tierDisplayName(tier.getDisplayName())
                .pointsMultiplier(tier.getPointsMultiplier())
                .lifetimeSpent(wallet.getLifetimeSpent())
                .nextTierSpendRemaining(nextTierSpend)
                .totalTransactions(totalTx)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWallet() != null ? tx.getWallet().getId() : null)
                .amount(tx.getAmount())
                .points(tx.getPoints())
                .type(tx.getType() != null ? tx.getType().name() : "")
                .description(tx.getDescription())
                .referenceId(tx.getReferenceId())
                .referenceType(tx.getReferenceType())
                .balanceAfter(tx.getBalanceAfter())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
