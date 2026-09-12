package com.sareekart.service;

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
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WalletRepository;
import com.sareekart.repository.WalletTransactionRepository;
import com.sareekart.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User testCustomer;
    private User adminUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testCustomer = User.builder()
                .id(101L)
                .firstName("Ananya")
                .lastName("Sharma")
                .email("ananya@example.com")
                .role(Role.CUSTOMER)
                .build();

        adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("Staff")
                .email("admin@sareekart.com")
                .role(Role.ADMIN)
                .build();

        testWallet = Wallet.builder()
                .id(201L)
                .user(testCustomer)
                .balance(new BigDecimal("1000.00"))
                .loyaltyPoints(50)
                .tier(PatronTier.SILVER)
                .lifetimeSpent(new BigDecimal("5000.00"))
                .transactions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should initialize new wallet with 0 balance and SILVER tier when none exists")
    void testGetOrCreateWallet_NewUser() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setId(999L);
            return w;
        });

        Wallet wallet = walletService.getOrCreateWallet(testCustomer);

        assertNotNull(wallet);
        assertEquals(BigDecimal.ZERO.setScale(2), wallet.getBalance());
        assertEquals(0, wallet.getLoyaltyPoints());
        assertEquals(PatronTier.SILVER, wallet.getTier());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should return existing wallet without recreating")
    void testGetOrCreateWallet_ExistingUser() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));

        Wallet wallet = walletService.getOrCreateWallet(testCustomer);

        assertEquals(testWallet.getId(), wallet.getId());
        assertEquals(new BigDecimal("1000.00"), wallet.getBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should credit return refund with 5% patronage bonus and record transaction")
    void testCreditReturnRefund_WithFivePercentBonus() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal refundAmount = new BigDecimal("2000.00"); // 5% bonus is 100.00, total = 2100.00
        Wallet result = walletService.creditReturnRefund(testCustomer.getId(), refundAmount, 55L);

        // 1000 + 2100 = 3100
        assertEquals(new BigDecimal("3100.00"), result.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getAmount().compareTo(new BigDecimal("2100.00")) == 0 &&
                tx.getType() == WalletTransactionType.CREDIT_RETURN_REFUND &&
                tx.getReferenceId().equals(55L)
        ));
    }

    @Test
    @DisplayName("Should earn loyalty points on delivery and auto-promote to GOLD tier when lifetime spend crosses ₹25,000")
    void testCreditLoyaltyEarned_AndTierProgression() {
        testWallet.setLifetimeSpent(new BigDecimal("20000.00")); // + 10000 will be 30000 -> GOLD
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal orderTotal = new BigDecimal("10000.00");
        Wallet result = walletService.creditLoyaltyEarned(testCustomer.getId(), orderTotal, 77L);

        // Lifetime spent is now 30,000 -> Gold Connoisseur (1.25x multiplier)
        assertEquals(PatronTier.GOLD, result.getTier());
        // Base points: 10000 / 100 = 100 points * 1.25 = 125 points
        // 50 initial + 125 = 175 points
        assertEquals(175, result.getLoyaltyPoints());
        // Balance: 1000 initial + 125 = 1125
        assertEquals(new BigDecimal("1125.00"), result.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getPoints() == 125 &&
                tx.getType() == WalletTransactionType.CREDIT_LOYALTY_EARNED
        ));
    }

    @Test
    @DisplayName("Should redeem wallet store credit for checkout and record debit transaction")
    void testRedeemWallet_Success() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal redeemAmount = new BigDecimal("400.00");
        Wallet result = walletService.redeemWallet(testCustomer, redeemAmount, 88L);

        // 1000 - 400 = 600
        assertEquals(new BigDecimal("600.00"), result.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getAmount().compareTo(new BigDecimal("-400.00")) == 0 &&
                tx.getType() == WalletTransactionType.DEBIT_CHECKOUT_REDEMPTION
        ));
    }

    @Test
    @DisplayName("Should throw BadRequestException when redemption amount exceeds balance (overdraft protection)")
    void testRedeemWallet_OverdraftThrowsException() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));

        BigDecimal excessiveAmount = new BigDecimal("1500.00");
        assertThrows(BadRequestException.class, () ->
                walletService.redeemWallet(testCustomer, excessiveAmount, 88L)
        );
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should revert wallet credit upon order cancellation")
    void testRevertOrderRedemption() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal refundAmt = new BigDecimal("400.00");
        Wallet result = walletService.revertOrderRedemption(testCustomer.getId(), refundAmt, 88L);

        assertEquals(new BigDecimal("1400.00"), result.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getAmount().compareTo(new BigDecimal("400.00")) == 0 &&
                tx.getType() == WalletTransactionType.CREDIT_ORDER_CANCEL_REFUND
        ));
    }

    @Test
    @DisplayName("Should calculate redemption preview correctly")
    void testPreviewRedemption() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));

        // Order total ₹2500, wallet has ₹1000 -> Max redeemable ₹1000, remaining ₹1500, not fully covered
        WalletRedemptionPreviewResponse preview = walletService.previewRedemption(testCustomer.getId(), new BigDecimal("2500.00"));

        assertEquals(new BigDecimal("1000.00"), preview.getMaxRedeemable());
        assertEquals(new BigDecimal("1500.00"), preview.getRemainingOrderTotal());
        assertFalse(preview.isFullyCovered());

        // Order total ₹800, wallet has ₹1000 -> Max redeemable ₹800, remaining ₹0, fully covered
        WalletRedemptionPreviewResponse previewFull = walletService.previewRedemption(testCustomer.getId(), new BigDecimal("800.00"));

        assertEquals(new BigDecimal("800.00"), previewFull.getMaxRedeemable());
        assertEquals(new BigDecimal("0.00"), previewFull.getRemainingOrderTotal());
        assertTrue(previewFull.isFullyCovered());
    }

    @Test
    @DisplayName("Staff can issue promotional credit bonus")
    void testAdminAdjustCredit_Success() {
        when(walletRepository.findByUserId(testCustomer.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletCreditRequest req = WalletCreditRequest.builder()
                .amount(new BigDecimal("500.00"))
                .points(20)
                .reason("Festival Goodwill Perk")
                .build();

        WalletResponse resp = walletService.adminAdjustCredit(testCustomer.getId(), req, adminUser);

        assertEquals(new BigDecimal("1500.00"), resp.getBalance());
        assertEquals(70, resp.getLoyaltyPoints());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getType() == WalletTransactionType.CREDIT_PROMO_BONUS
        ));
    }

    @Test
    @DisplayName("Customer cannot issue admin adjustments (RBAC)")
    void testAdminAdjustCredit_UnauthorizedCustomer() {
        WalletCreditRequest req = WalletCreditRequest.builder()
                .amount(new BigDecimal("500.00"))
                .reason("Hacking credit")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                walletService.adminAdjustCredit(testCustomer.getId(), req, testCustomer)
        );
    }
}
