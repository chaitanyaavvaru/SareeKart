package com.sareekart.repository;

import com.sareekart.entity.Conversation;
import com.sareekart.entity.WhatsAppContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByContactId(Long contactId);
    
    // Find active conversation for a contact (assuming only one active at a time)
    Optional<Conversation> findFirstByContactAndStatusNotOrderByUpdatedAtDesc(WhatsAppContact contact, com.sareekart.entity.ConversationStatus status);

    List<Conversation> findAllByOrderByLastMessageAtDesc();

    List<Conversation> findByStatusOrderByLastMessageAtDesc(com.sareekart.entity.ConversationStatus status);
}
