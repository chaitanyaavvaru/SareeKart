package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.trousseau.*;
import com.sareekart.entity.User;
import com.sareekart.service.TrousseauService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trousseau")
@CrossOrigin(origins = "*")
public class TrousseauBoardController {

    private final TrousseauService trousseauService;
    private final com.sareekart.service.TrousseauAiCurationService trousseauAiCurationService;
    private final com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService;
    private final com.sareekart.service.TrousseauSseService trousseauSseService;

    @Autowired
    public TrousseauBoardController(
            TrousseauService trousseauService,
            com.sareekart.service.TrousseauAiCurationService trousseauAiCurationService,
            com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService,
            com.sareekart.service.TrousseauSseService trousseauSseService) {
        this.trousseauService = trousseauService;
        this.trousseauAiCurationService = trousseauAiCurationService;
        this.trousseauWhatsAppService = trousseauWhatsAppService;
        this.trousseauSseService = trousseauSseService;
    }

    public TrousseauBoardController(
            TrousseauService trousseauService,
            com.sareekart.service.TrousseauAiCurationService trousseauAiCurationService,
            com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService) {
        this(trousseauService, trousseauAiCurationService, trousseauWhatsAppService, null);
    }

    // =========================================================================
    // Board Lifecycle
    // =========================================================================

    @PostMapping
    public ResponseEntity<ApiResponse<TrousseauBoardResponse>> createBoard(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTrousseauBoardRequest request) {
        TrousseauBoardResponse response = trousseauService.createBoard(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trousseau board created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrousseauBoardResponse>>> getUserBoards(
            @AuthenticationPrincipal User user) {
        List<TrousseauBoardResponse> responses = trousseauService.getUserBoards(user.getId());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<TrousseauBoardResponse>> getBoardById(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        TrousseauBoardResponse response = trousseauService.getBoardById(user.getId(), boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<ApiResponse<TrousseauBoardResponse>> updateBoard(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateTrousseauBoardRequest request) {
        TrousseauBoardResponse response = trousseauService.updateBoard(user.getId(), boardId, request);
        return ResponseEntity.ok(ApiResponse.success("Trousseau board updated successfully", response));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        trousseauService.deleteBoard(user.getId(), boardId);
        return ResponseEntity.ok(ApiResponse.success("Trousseau board deleted successfully", null));
    }

    // =========================================================================
    // Ceremony Management
    // =========================================================================

    @PostMapping("/{boardId}/ceremonies")
    public ResponseEntity<ApiResponse<TrousseauCeremonyResponse>> addCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @Valid @RequestBody CreateCeremonyRequest request) {
        TrousseauCeremonyResponse response = trousseauService.addCeremony(user.getId(), boardId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ceremony added successfully", response));
    }

    @PutMapping("/{boardId}/ceremonies/{ceremonyId}")
    public ResponseEntity<ApiResponse<TrousseauCeremonyResponse>> updateCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @Valid @RequestBody UpdateCeremonyRequest request) {
        TrousseauCeremonyResponse response = trousseauService.updateCeremony(user.getId(), boardId, ceremonyId, request);
        return ResponseEntity.ok(ApiResponse.success("Ceremony updated successfully", response));
    }

    @DeleteMapping("/{boardId}/ceremonies/{ceremonyId}")
    public ResponseEntity<ApiResponse<Void>> deleteCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId) {
        trousseauService.deleteCeremony(user.getId(), boardId, ceremonyId);
        return ResponseEntity.ok(ApiResponse.success("Ceremony deleted successfully", null));
    }

    // =========================================================================
    // Item Management
    // =========================================================================

    @PostMapping("/{boardId}/ceremonies/{ceremonyId}/items")
    public ResponseEntity<ApiResponse<TrousseauItemResponse>> addItemToCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @Valid @RequestBody AddCeremonyItemRequest request) {
        TrousseauItemResponse response = trousseauService.addItemToCeremony(user.getId(), boardId, ceremonyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Saree shortlisted for ceremony successfully", response));
    }

    @PatchMapping("/{boardId}/ceremonies/{ceremonyId}/items/{itemId}")
    public ResponseEntity<ApiResponse<TrousseauItemResponse>> updateCeremonyItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCeremonyItemRequest request) {
        TrousseauItemResponse response = trousseauService.updateCeremonyItem(user.getId(), boardId, ceremonyId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Trousseau item updated successfully", response));
    }

    @DeleteMapping("/{boardId}/ceremonies/{ceremonyId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItemFromCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @PathVariable Long itemId) {
        trousseauService.removeItemFromCeremony(user.getId(), boardId, ceremonyId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from ceremony successfully", null));
    }

    // =========================================================================
    // AI Bridal Curation & Ensemble Generation
    // =========================================================================

    @PostMapping("/{boardId}/ceremonies/{ceremonyId}/curate")
    public ResponseEntity<ApiResponse<TrousseauAiCurationResponse>> curateCeremony(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @Valid @RequestBody(required = false) TrousseauAiCurationRequest request) {
        TrousseauAiCurationResponse response = trousseauAiCurationService.curateCeremonyEnsemble(
                user.getId(), boardId, ceremonyId, request != null ? request : new TrousseauAiCurationRequest());
        return ResponseEntity.ok(ApiResponse.success("AI bridal curation generated successfully", response));
    }

    // =========================================================================
    // Core Commerce Cart Bridge (1-Click Conversion)
    // =========================================================================

    @PostMapping("/{boardId}/ceremonies/{ceremonyId}/items/{itemId}/transfer-to-cart")
    public ResponseEntity<ApiResponse<TransferToCartResponse>> transferItemToCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId,
            @PathVariable Long itemId) {
        TransferToCartResponse response = trousseauService.transferItemToCart(user.getId(), boardId, ceremonyId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item transferred to cart successfully", response));
    }

    @PostMapping("/{boardId}/ceremonies/{ceremonyId}/transfer-to-cart")
    public ResponseEntity<ApiResponse<TransferToCartResponse>> transferCeremonyToCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long ceremonyId) {
        TransferToCartResponse response = trousseauService.transferCeremonyToCart(user.getId(), boardId, ceremonyId);
        return ResponseEntity.ok(ApiResponse.success("Ceremony items transferred to cart successfully", response));
    }

    @PostMapping("/{boardId}/transfer-all-to-cart")
    public ResponseEntity<ApiResponse<TransferToCartResponse>> transferAllToCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        TransferToCartResponse response = trousseauService.transferAllToCart(user.getId(), boardId);
        return ResponseEntity.ok(ApiResponse.success("All trousseau items transferred to cart successfully", response));
    }

    // =========================================================================
    // Share Token Management (Revocation / Regeneration)
    // =========================================================================

    @PostMapping("/{boardId}/regenerate-share-token")
    public ResponseEntity<ApiResponse<TrousseauBoardResponse>> regenerateShareToken(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        TrousseauBoardResponse response = trousseauService.regenerateShareToken(user.getId(), boardId);
        return ResponseEntity.ok(ApiResponse.success("Share link regenerated and previous token revoked", response));
    }

    // =========================================================================
    // Collaborator Management
    // =========================================================================

    @PostMapping("/{boardId}/collaborators")
    public ResponseEntity<ApiResponse<TrousseauCollaboratorResponse>> inviteCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @Valid @RequestBody InviteCollaboratorRequest request) {
        TrousseauCollaboratorResponse response = trousseauService.inviteCollaborator(user.getId(), boardId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Collaborator invited successfully", response));
    }

    @GetMapping("/{boardId}/collaborators")
    public ResponseEntity<ApiResponse<List<TrousseauCollaboratorResponse>>> getCollaborators(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        List<TrousseauCollaboratorResponse> responses = trousseauService.getCollaborators(user.getId(), boardId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{boardId}/collaborators/{collaboratorId}")
    public ResponseEntity<ApiResponse<TrousseauCollaboratorResponse>> updateCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long collaboratorId,
            @Valid @RequestBody UpdateCollaboratorRequest request) {
        TrousseauCollaboratorResponse response = trousseauService.updateCollaborator(user.getId(), boardId, collaboratorId, request);
        return ResponseEntity.ok(ApiResponse.success("Collaborator updated successfully", response));
    }

    @DeleteMapping("/{boardId}/collaborators/{collaboratorId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long collaboratorId) {
        trousseauService.removeCollaborator(user.getId(), boardId, collaboratorId);
        return ResponseEntity.ok(ApiResponse.success("Collaborator removed successfully", null));
    }

    // =========================================================================
    // WhatsApp Family Collaboration & Sharing
    // =========================================================================

    @PostMapping("/{boardId}/share-whatsapp")
    public ResponseEntity<ApiResponse<TrousseauWhatsAppShareResponse>> shareViaWhatsApp(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @Valid @RequestBody ShareTrousseauWhatsAppRequest request) {
        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendShareInvitation(user.getId(), boardId, request);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp invitation dispatched successfully", response));
    }

    @PostMapping("/{boardId}/collaborators/{collaboratorId}/share-whatsapp")
    public ResponseEntity<ApiResponse<TrousseauWhatsAppShareResponse>> shareCollaboratorViaWhatsApp(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @PathVariable Long collaboratorId) {
        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendCollaboratorInvitation(user.getId(), boardId, collaboratorId);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp invitation dispatched to collaborator successfully", response));
    }

    // =========================================================================
    // Real-time Live Collaboration (SSE Stream) - Hardened with Ownership Check
    // =========================================================================

    @GetMapping(value = {"/{boardId}/stream", "/boards/{boardId}/stream"}, produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamBoardEvents(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId) {
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required to subscribe to board events");
        }
        // STEP 1-4: Validate caller ownership BEFORE creating emitter or subscribing
        trousseauService.validateBoardOwnership(user.getId(), boardId);

        // STEP 5: Only after ownership validation passes, create and register emitter
        return trousseauSseService.subscribe(boardId);
    }
}
