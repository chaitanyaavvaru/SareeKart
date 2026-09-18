package com.sareekart.service.impl;

import com.sareekart.dto.trousseau.ShareTrousseauWhatsAppRequest;
import com.sareekart.dto.trousseau.TrousseauWhatsAppShareResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.*;
import com.sareekart.service.TrousseauWhatsAppService;
import com.sareekart.service.WhatsAppApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrousseauWhatsAppServiceImpl implements TrousseauWhatsAppService {

    private final WhatsAppApiClient whatsAppApiClient;
    private final TrousseauBoardRepository boardRepository;
    private final TrousseauCeremonyRepository ceremonyRepository;
    private final TrousseauCollaboratorRepository collaboratorRepository;
    private final TrousseauItemRepository itemRepository;

    @Override
    @Transactional
    public TrousseauWhatsAppShareResponse sendShareInvitation(Long userId, Long boardId, ShareTrousseauWhatsAppRequest request) {
        log.info("Sending trousseau WhatsApp share invitation for boardId={} by userId={}", boardId, userId);

        TrousseauBoard board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau board not found: " + boardId));

        if (!board.getUser().getId().equals(userId)) {
            log.warn("Unauthorized attempt to share boardId={} by userId={}", boardId, userId);
            throw new ResourceNotFoundException("Trousseau board not found or access denied: " + boardId);
        }

        if ("ARCHIVED".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("Cannot share an archived trousseau board");
        }

        String rawPhone = request.getRecipientPhone().trim().replaceAll("[\\s\\-\\(\\)]", "");
        String recipient = (request.getRecipientName() != null && !request.getRecipientName().isBlank())
                ? request.getRecipientName().trim() : "Family";

        String brideName = (board.getUser().getFirstName() != null) ? board.getUser().getFirstName() : "The bride";
        if (board.getUser().getLastName() != null && !board.getUser().getLastName().isBlank()) {
            brideName += " " + board.getUser().getLastName();
        }

        String shareUrl = "https://sareekart.com/trousseau/share/" + board.getShareToken();
        String messageBody;

        if (request.getCeremonyId() != null) {
            TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(request.getCeremonyId(), boardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found on board: " + request.getCeremonyId()));

            long ceremonyItemsCount = itemRepository.countByCeremonyId(ceremony.getId());
            String themeText = (ceremony.getColorTheme() != null && !ceremony.getColorTheme().isBlank())
                    ? ceremony.getColorTheme() : "Traditional Bridal Palette";

            StringBuilder sb = new StringBuilder();
            sb.append("🌸 *SareeKart Bridal Trousseau — ").append(ceremony.getTitle()).append("* 🌸\n\n");
            sb.append("Dear ").append(recipient).append(",\n");
            sb.append(brideName).append(" would love your blessings and advice on the shortlisted sarees for the *")
                    .append(ceremony.getTitle()).append("* ceremony!\n\n");
            sb.append("🎨 *Color Theme:* ").append(themeText).append("\n");
            sb.append("👗 *Shortlisted Looks:* ").append(ceremonyItemsCount).append(" authentic handloom sarees\n\n");
            sb.append("Tap the link below to view the shortlisted looks and vote:\n");
            sb.append("👉 ").append(shareUrl).append("\n\n");
            sb.append("You can cast your ❤️ Love, 👍 Like, or 🌸 Suggest Change reactions with personal notes!");

            if (request.getCustomNote() != null && !request.getCustomNote().isBlank()) {
                String sanitizedNote = request.getCustomNote().trim().replaceAll("[\\r\\n]+", " ");
                sb.append("\n\n_Note from bride:_ \"").append(sanitizedNote).append("\"");
            }
            messageBody = sb.toString();
        } else {
            List<TrousseauCeremony> ceremonies = ceremonyRepository.findByBoardIdOrderByDisplayOrderAsc(boardId);
            List<TrousseauItem> allItems = itemRepository.findByBoardIdOrderByCreatedAtAsc(boardId);

            StringBuilder sb = new StringBuilder();
            sb.append("🌸 *SareeKart Collaborative Bridal Trousseau* 🌸\n\n");
            sb.append("Dear ").append(recipient).append(",\n");
            sb.append(brideName).append(" has invited you to co-curate her wedding trousseau *\"")
                    .append(board.getTitle()).append("\"*!\n\n");

            if (board.getWeddingDate() != null) {
                sb.append("💍 *Wedding Date:* ")
                        .append(board.getWeddingDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                        .append("\n");
            }

            sb.append("✨ *Ceremonies:* ").append(ceremonies.size()).append(" wedding celebrations\n");
            sb.append("👗 *Shortlisted Looks:* ").append(allItems.size()).append(" handcrafted sarees\n\n");
            sb.append("Review the curated looks and help finalize the wedding wardrobe:\n");
            sb.append("👉 ").append(shareUrl).append("\n\n");
            sb.append("Cast your ❤️ Love, 👍 Like, or 🌸 Suggest Change reactions with personal notes!");

            if (request.getCustomNote() != null && !request.getCustomNote().isBlank()) {
                String sanitizedNote = request.getCustomNote().trim().replaceAll("[\\r\\n]+", " ");
                sb.append("\n\n_Note from bride:_ \"").append(sanitizedNote).append("\"");
            }
            messageBody = sb.toString();
        }

        // WhatsApp Dispatch through Phase 10 Client with Failure Isolation
        boolean dispatchSuccess = true;
        String statusMessage = "WhatsApp invitation dispatched successfully";
        try {
            whatsAppApiClient.sendTextMessage(rawPhone, messageBody);
            log.info("Dispatched WhatsApp trousseau share to {} for boardId={}", rawPhone, boardId);
        } catch (Exception e) {
            log.error("WhatsApp API dispatch failed for {}: {}", rawPhone, e.getMessage());
            dispatchSuccess = false;
            statusMessage = "WhatsApp service unavailable; share link generated successfully";
        }

        // Update matching collaborator invite status if registered
        collaboratorRepository.findByBoardIdAndPhone(boardId, rawPhone).ifPresent(collab -> {
            collab.setInviteStatus("SENT");
            collaboratorRepository.save(collab);
            log.info("Updated collaboratorId={} invite status to SENT", collab.getId());
        });

        return TrousseauWhatsAppShareResponse.builder()
                .success(dispatchSuccess)
                .message(statusMessage)
                .recipientPhone(rawPhone)
                .recipientName(recipient)
                .shareUrl(shareUrl)
                .messagePreview(messageBody)
                .dispatchedAt(LocalDateTime.now())
                .simulated(true)
                .build();
    }

    @Override
    @Transactional
    public TrousseauWhatsAppShareResponse sendCollaboratorInvitation(Long userId, Long boardId, Long collaboratorId) {
        log.info("Sending collaborator WhatsApp invite for boardId={}, collaboratorId={}", boardId, collaboratorId);

        TrousseauBoard board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau board not found: " + boardId));

        if (!board.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Trousseau board not found or access denied: " + boardId);
        }

        TrousseauCollaborator collaborator = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found: " + collaboratorId));

        if (!collaborator.getBoard().getId().equals(boardId)) {
            throw new ResourceNotFoundException("Collaborator not found on this board: " + collaboratorId);
        }

        if (collaborator.getPhone() == null || collaborator.getPhone().isBlank()) {
            throw new BadRequestException("Collaborator does not have a phone number registered");
        }

        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone(collaborator.getPhone())
                .recipientName(collaborator.getName())
                .customNote("You have been invited as a " + collaborator.getRole() + " to review our wedding trousseau.")
                .build();

        return sendShareInvitation(userId, boardId, request);
    }

    @Override
    public void notifyOwnerOfVote(Long boardId, Long ceremonyId, Long itemId, String voterName, String reaction, String note) {
        try {
            TrousseauBoard board = boardRepository.findById(boardId).orElse(null);
            if (board == null || board.getUser() == null) {
                return;
            }

            User owner = board.getUser();
            if (owner.getWhatsappOptIn() == null || !owner.getWhatsappOptIn() || owner.getMobile() == null || owner.getMobile().isBlank()) {
                log.debug("Suppressing WhatsApp vote alert: owner has opted out or has no mobile number");
                return;
            }

            TrousseauCeremony ceremony = (ceremonyId != null) ? ceremonyRepository.findById(ceremonyId).orElse(null) : null;
            String ceremonyTitle = (ceremony != null) ? ceremony.getTitle() : "Wedding Ceremony";

            TrousseauItem item = (itemId != null) ? itemRepository.findById(itemId).orElse(null) : null;
            String sareeName = (item != null && item.getProduct() != null) ? item.getProduct().getName() : "a shortlisted saree";

            String voter = (voterName != null && !voterName.isBlank()) ? voterName.trim() : "A family member";
            String reactionSymbol = switch (reaction != null ? reaction.toUpperCase() : "") {
                case "LOVE" -> "❤️ Loved";
                case "LIKE" -> "👍 Liked";
                case "PASS" -> "⏭️ Passed on";
                case "NEEDS_REVIEW" -> "🌸 Suggested Changes for";
                default -> "voted on";
            };

            StringBuilder sb = new StringBuilder();
            sb.append("🌸 *SareeKart Trousseau Alert* 🌸\n\n");
            sb.append(voter).append(" has *").append(reactionSymbol).append("* *\"")
                    .append(sareeName).append("\"* for your *").append(ceremonyTitle).append("*!\n");

            if (note != null && !note.isBlank()) {
                String sanitizedNote = note.trim().replaceAll("[\\r\\n]+", " ");
                sb.append("\n💬 _Comment:_ \"").append(sanitizedNote).append("\"");
            }

            sb.append("\n\nView updated family consensus on your SareeKart Trousseau Studio.");

            whatsAppApiClient.sendTextMessage(owner.getMobile(), sb.toString());
            log.info("Sent WhatsApp vote notification to board owner at {}", owner.getMobile());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp vote alert to board owner: {}", e.getMessage());
            // Failure isolation: never bubble exception up to caller
        }
    }
}
