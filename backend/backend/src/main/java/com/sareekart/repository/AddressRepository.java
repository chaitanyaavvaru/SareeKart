package com.sareekart.repository;

import com.sareekart.entity.Address;
import com.sareekart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    List<Address> findByUserIdAndType(Long userId, com.sareekart.entity.enums.AddressType type);
}