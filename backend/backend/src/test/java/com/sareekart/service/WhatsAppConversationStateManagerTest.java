package com.sareekart.service;

import com.sareekart.dto.request.StylistChatMessage;
import com.sareekart.dto.whatsapp.WhatsAppSessionContext;
import com.sareekart.service.impl.WhatsAppConversationStateManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppConversationStateManagerTest {

    private WhatsAppConversationStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new WhatsAppConversationStateManagerImpl();
    }

    @Test
    @DisplayName("getSession creates new empty session if none exists")
    void testGetSession_New() {
        String phone = "9876543210";
        WhatsAppSessionContext session = stateManager.getSession(phone);

        assertThat(session).isNotNull();
        assertThat(session.getPhoneNumber()).isEqualTo(phone);
        assertThat(session.getRecentTurns()).isEmpty();
    }

    @Test
    @DisplayName("updateSession retains attributes and prunes history to last 4 turns")
    void testUpdateSessionAndPruning() {
        String phone = "9876543210";
        WhatsAppSessionContext session = stateManager.getSession(phone);
        session.setActiveOccasion("WEDDING");
        session.setPreferredFabric("SILK");
        session.setMaxBudget(new BigDecimal("50000"));

        for (int i = 1; i <= 6; i++) {
            session.getRecentTurns().add(new StylistChatMessage("user", "turn " + i, LocalDateTime.now()));
        }

        stateManager.updateSession(phone, session);

        WhatsAppSessionContext retrieved = stateManager.getSession(phone);
        assertThat(retrieved.getActiveOccasion()).isEqualTo("WEDDING");
        assertThat(retrieved.getPreferredFabric()).isEqualTo("SILK");
        assertThat(retrieved.getMaxBudget()).isEqualByComparingTo("50000");
        assertThat(retrieved.getRecentTurns()).hasSize(4);
        assertThat(retrieved.getRecentTurns().get(3).getContent()).isEqualTo("turn 6");
    }

    @Test
    @DisplayName("clearSession removes conversation context completely")
    void testClearSession() {
        String phone = "9876543210";
        WhatsAppSessionContext session = stateManager.getSession(phone);
        session.setActiveOccasion("RECEPTION");
        stateManager.updateSession(phone, session);

        stateManager.clearSession(phone);

        WhatsAppSessionContext newSession = stateManager.getSession(phone);
        assertThat(newSession.getActiveOccasion()).isNull();
    }
}
