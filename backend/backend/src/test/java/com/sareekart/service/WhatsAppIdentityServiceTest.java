package com.sareekart.service;

import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppContact;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WhatsAppContactRepository;
import com.sareekart.service.impl.WhatsAppIdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppIdentityServiceTest {

    @Mock
    private WhatsAppContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WhatsAppIdentityServiceImpl identityService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(202L)
                .firstName("Pooja")
                .lastName("Iyer")
                .email("pooja@example.com")
                .mobile("+919876543210")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Normalizes diverse Indian mobile phone formats to canonical 10-digit format")
    void testNormalizePhoneNumber() {
        assertThat(identityService.normalizePhoneNumber("+919876543210")).isEqualTo("9876543210");
        assertThat(identityService.normalizePhoneNumber("919876543210")).isEqualTo("9876543210");
        assertThat(identityService.normalizePhoneNumber("09876543210")).isEqualTo("9876543210");
        assertThat(identityService.normalizePhoneNumber("9876543210")).isEqualTo("9876543210");
        assertThat(identityService.normalizePhoneNumber("+91 98765-43210")).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("resolveContact returns existing contact if already present")
    void testResolveContact_Existing() {
        WhatsAppContact existing = WhatsAppContact.builder()
                .id(1L)
                .phoneNumber("9876543210")
                .name("Pooja")
                .build();

        when(contactRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(existing));

        WhatsAppContact resolved = identityService.resolveContact("+919876543210", "Pooja");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getId()).isEqualTo(1L);
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolveContact creates new contact and links to matching registered user")
    void testResolveContact_NewAndAutoLinked() {
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(contactRepository.save(any(WhatsAppContact.class))).thenAnswer(i -> {
            WhatsAppContact c = i.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.of(sampleUser));

        WhatsAppContact resolved = identityService.resolveContact("+919876543210", "Pooja Iyer");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getUser()).isEqualTo(sampleUser);
        assertThat(resolved.getPhoneNumber()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("linkUser explicitly binds contact to user")
    void testLinkUser() {
        WhatsAppContact contact = WhatsAppContact.builder().id(5L).phoneNumber("9876543210").build();

        boolean linked = identityService.linkUser(contact, sampleUser);

        assertThat(linked).isTrue();
        assertThat(contact.getUser()).isEqualTo(sampleUser);
        verify(contactRepository, times(1)).save(contact);
    }
}
