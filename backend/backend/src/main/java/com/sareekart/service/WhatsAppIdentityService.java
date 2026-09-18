package com.sareekart.service;

import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppContact;

/**
 * Normalizes phone identities and resolves WhatsApp contacts to registered SareeKart accounts.
 */
public interface WhatsAppIdentityService {

    /**
     * Normalizes raw international or local phone numbers to a canonical 10-digit Indian standard.
     */
    String normalizePhoneNumber(String rawPhone);

    /**
     * Normalizes phone to standard international E.164 format (+919876543210).
     */
    String normalizeToE164(String rawPhone);

    /**
     * Formats phone number for Meta WhatsApp Cloud API recipient parameter (919876543210).
     */
    String toMetaRecipientPhone(String rawPhone);

    /**
     * Validates whether raw phone corresponds to an active Indian mobile number (starts with 6-9).
     */
    boolean isValidIndianMobile(String rawPhone);

    /**
     * Resolves an existing WhatsApp contact or registers a new contact record.
     */
    WhatsAppContact resolveContact(String rawPhone, String profileName);

    /**
     * Retrieves the linked registered SareeKart user, or null if guest.
     */
    User getLinkedUser(WhatsAppContact contact);

    /**
     * Binds a WhatsApp contact to a registered SareeKart customer account.
     */
    boolean linkUser(WhatsAppContact contact, User user);
}
