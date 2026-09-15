package com.sareekart.repository;

import com.sareekart.entity.GraphSyncFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphSyncFailureRepository extends JpaRepository<GraphSyncFailure, Long> {

    List<GraphSyncFailure> findByResolvedAtIsNullOrderByCreatedAtAsc();

    long countByResolvedAtIsNull();
}
