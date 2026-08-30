package com.sareekart.repository;

import com.sareekart.entity.ReconciliationAnomaly;
import com.sareekart.entity.enums.AnomalyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationAnomalyRepository extends JpaRepository<ReconciliationAnomaly, Long> {

    /** Open-anomaly dedup for per-refund codes (prevents webhook/replay spam). */
    @Query("SELECT COUNT(a) FROM ReconciliationAnomaly a WHERE a.code = :code " +
           "AND a.refund.id = :refundId AND a.resolved = false")
    long countOpenByCodeAndRefund(@Param("code") AnomalyCode code, @Param("refundId") Long refundId);

    List<ReconciliationAnomaly> findByResolvedFalseOrderByCreatedAtDesc();

    List<ReconciliationAnomaly> findByCodeOrderByCreatedAtDesc(AnomalyCode code);
}
