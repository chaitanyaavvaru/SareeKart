package com.sareekart.service;

import com.sareekart.dto.trousseau.ShareTrousseauWhatsAppRequest;
import com.sareekart.dto.trousseau.TrousseauWhatsAppShareResponse;

public interface TrousseauWhatsAppService {

    /**
     * Dispatches a WhatsApp invitation to a recipient (family/friend/bridesmaid)
     * containing ceremony or board context and a secure 256-bit tokenized review link.
     */
    TrousseauWhatsAppShareResponse sendShareInvitation(Long userId, Long boardId, ShareTrousseauWhatsAppRequest request);

    /**
     * Dispatches a WhatsApp invitation to a pre-existing registered collaborator on the board.
     */
    TrousseauWhatsAppShareResponse sendCollaboratorInvitation(Long userId, Long boardId, Long collaboratorId);

    /**
     * Asynchronously alerts the board owner when a family member casts a reaction or leaves a note,
     * conditioned on owner opt-in preferences.
     */
    void notifyOwnerOfVote(Long boardId, Long ceremonyId, Long itemId, String voterName, String reaction, String note);
}
