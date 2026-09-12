package com.sareekart.repository;

import com.sareekart.entity.PincodeOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PincodeOverrideRepository extends JpaRepository<PincodeOverride, Long> {

    @Query("SELECT p FROM PincodeOverride p WHERE p.pincode = :pincode")
    Optional<PincodeOverride> findByPincode(@Param("pincode") String pincode);

    List<PincodeOverride> findAllByOrderByPincodeAsc();
}
