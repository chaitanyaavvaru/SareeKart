package com.sareekart.repository;

import com.sareekart.entity.WhatsAppContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppContactRepository extends JpaRepository<WhatsAppContact, Long> {
    Optional<WhatsAppContact> findByPhoneNumber(String phoneNumber);
}
