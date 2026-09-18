package com.sareekart.service.impl;

import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppContact;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WhatsAppContactRepository;
import com.sareekart.service.WhatsAppIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppIdentityServiceImpl implements WhatsAppIdentityService {

    private final WhatsAppContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    public String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");

        // Indian mobile: 12 digits starting with 91 -> 10 digits
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        // Indian mobile with leading zero -> 10 digits
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        // Standard 10-digit number
        if (digits.length() == 10) {
            return digits;
        }
        return digits;
    }

    @Override
    public String normalizeToE164(String rawPhone) {
        String tenDigits = normalizePhoneNumber(rawPhone);
        if (tenDigits.length() == 10) {
            return "+91" + tenDigits;
        }
        if (rawPhone == null) return "";
        String trimmed = rawPhone.trim();
        return trimmed.startsWith("+") ? trimmed : ("+" + trimmed.replaceAll("[^0-9]", ""));
    }

    @Override
    public String toMetaRecipientPhone(String rawPhone) {
        String tenDigits = normalizePhoneNumber(rawPhone);
        if (tenDigits.length() == 10) {
            return "91" + tenDigits;
        }
        if (rawPhone == null) return "";
        return rawPhone.replaceAll("[^0-9]", "");
    }

    @Override
    public boolean isValidIndianMobile(String rawPhone) {
        String tenDigits = normalizePhoneNumber(rawPhone);
        return tenDigits.length() == 10 && tenDigits.matches("^[6-9]\\d{9}$");
    }

    @Override
    @Transactional
    public WhatsAppContact resolveContact(String rawPhone, String profileName) {
        String normalized = normalizePhoneNumber(rawPhone);
        String e164 = normalizeToE164(rawPhone);
        String metaPhone = toMetaRecipientPhone(rawPhone);
        String name = (profileName != null && !profileName.isBlank()) ? profileName.trim() : "Patron";

        WhatsAppContact contact = contactRepository.findByPhoneNumber(rawPhone)
                .or(() -> contactRepository.findByPhoneNumber(normalized))
                .or(() -> contactRepository.findByPhoneNumber(e164))
                .or(() -> contactRepository.findByPhoneNumber(metaPhone))
                .orElseGet(() -> {
                    WhatsAppContact newContact = WhatsAppContact.builder()
                            .phoneNumber(normalized.isEmpty() ? rawPhone : normalized)
                            .name(name)
                            .optedIn(true)
                            .build();
                    return contactRepository.save(newContact);
                });

        // Attempt automatic linking if contact is not linked to a user yet
        if (contact.getUser() == null && !normalized.isEmpty()) {
            Optional<User> userOpt = userRepository.findByMobile(normalized)
                    .or(() -> userRepository.findByMobile("+91" + normalized))
                    .or(() -> userRepository.findByMobile("91" + normalized));

            if (userOpt.isPresent()) {
                contact.setUser(userOpt.get());
                contact = contactRepository.save(contact);
                log.info("Automatically linked WhatsApp contact {} to registered User ID: {}", normalized, userOpt.get().getId());
            }
        }

        return contact;
    }

    @Override
    @Transactional(readOnly = true)
    public User getLinkedUser(WhatsAppContact contact) {
        return contact != null ? contact.getUser() : null;
    }

    @Override
    @Transactional
    public boolean linkUser(WhatsAppContact contact, User user) {
        if (contact == null || user == null) return false;
        contact.setUser(user);
        contactRepository.save(contact);
        log.info("Explicitly linked WhatsApp contact {} to User ID: {}", contact.getPhoneNumber(), user.getId());
        return true;
    }
}
