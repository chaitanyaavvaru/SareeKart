package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppSessionContext;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppContact;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.WhatsAppAiCommerceServiceImpl;
import com.sareekart.service.whatsapp.WhatsAppCommerceTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppAiCommerceServiceImplTest {

    @Mock
    private WhatsAppIdentityService identityService;

    @Mock
    private WhatsAppConversationStateManager stateManager;

    @Mock
    private WhatsAppCommerceTools commerceTools;

    @Mock
    private WhatsAppApiClient whatsAppApiClient;

    @Mock
    private StylistIntentExtractor intentExtractor;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WhatsAppAiCommerceServiceImpl aiCommerceService;

    private User customer;
    private WhatsAppContact contact;
    private WhatsAppSessionContext session;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(101L)
                .firstName("Ananya")
                .lastName("Sharma")
                .mobile("+919876543210")
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .build();

        contact = WhatsAppContact.builder()
                .id(1L)
                .phoneNumber("9876543210")
                .name("Ananya Sharma")
                .user(customer)
                .build();

        session = new WhatsAppSessionContext("9876543210");

        when(identityService.normalizePhoneNumber(anyString())).thenReturn("9876543210");
        when(identityService.resolveContact(eq("9876543210"), any())).thenReturn(contact);
        when(identityService.getLinkedUser(contact)).thenReturn(customer);
        when(stateManager.getSession("9876543210")).thenReturn(session);
    }

    @Test
    @DisplayName("Handles STOP compliance command by unsubscribing user")
    void testHandleStopCommand() {
        aiCommerceService.processIncomingMessage("9876543210", "STOP", 1L);

        assertThat(customer.getWhatsappOptIn()).isFalse();
        verify(userRepository).save(customer);
        verify(stateManager).clearSession("9876543210");
        verify(whatsAppApiClient).sendTextMessage(eq("9876543210"), contains("unsubscribed"));
    }

    @Test
    @DisplayName("Handles START command by re-subscribing user")
    void testHandleStartCommand() {
        customer.setWhatsappOptIn(false);

        aiCommerceService.processIncomingMessage("9876543210", "START", 1L);

        assertThat(customer.getWhatsappOptIn()).isTrue();
        verify(userRepository).save(customer);
        verify(whatsAppApiClient).sendTextMessage(eq("9876543210"), contains("Welcome back"));
    }

    @Test
    @DisplayName("Handles Human escalation fast-path")
    void testHandleHumanEscalation() {
        when(commerceTools.escalateToHuman(any())).thenReturn(
                new WhatsAppCommerceTools.HumanHandoffResult(true, "A master stylist will join shortly!")
        );

        aiCommerceService.processIncomingMessage("9876543210", "I want to talk to an agent please", 1L);

        verify(commerceTools).escalateToHuman(any());
        verify(whatsAppApiClient).sendTextMessage(eq("9876543210"), contains("master stylist"));
    }

    @Test
    @DisplayName("Handles Cart view fast-path for logged in user")
    void testHandleCartView() {
        when(commerceTools.manageCart(any())).thenReturn(
                new WhatsAppCommerceTools.CartActionResult(true, "Success", 2, new BigDecimal("84000.00"))
        );

        aiCommerceService.processIncomingMessage("9876543210", "view cart", 1L);

        verify(commerceTools).manageCart(any());
        verify(whatsAppApiClient).sendInteractiveButtonsMessage(eq("9876543210"), contains("84000"), anyList());
    }

    @Test
    @DisplayName("Handles Greeting fast-path and sends luxury menu")
    void testHandleGreeting() {
        aiCommerceService.processIncomingMessage("9876543210", "Hello", 1L);

        verify(whatsAppApiClient).sendInteractiveButtonsMessage(eq("9876543210"), contains("Namaste, Ananya!"), anyList());
    }
}
