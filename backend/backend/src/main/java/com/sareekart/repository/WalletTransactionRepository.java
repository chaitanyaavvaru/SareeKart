package com.sareekart.repository;

import com.sareekart.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet.id = :walletId ORDER BY t.createdAt DESC")
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId);

    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet.user.id = :userId ORDER BY t.createdAt DESC")
    List<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
