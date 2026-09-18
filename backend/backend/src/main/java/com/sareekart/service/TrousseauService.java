package com.sareekart.service;

import com.sareekart.dto.trousseau.*;

import java.util.List;

public interface TrousseauService {

    // Board Lifecycle
    TrousseauBoardResponse createBoard(Long userId, CreateTrousseauBoardRequest request);

    TrousseauBoardResponse getBoardById(Long userId, Long boardId);

    List<TrousseauBoardResponse> getUserBoards(Long userId);

    TrousseauBoardResponse updateBoard(Long userId, Long boardId, UpdateTrousseauBoardRequest request);

    void deleteBoard(Long userId, Long boardId);

    // Ceremony Management
    TrousseauCeremonyResponse addCeremony(Long userId, Long boardId, CreateCeremonyRequest request);

    TrousseauCeremonyResponse updateCeremony(Long userId, Long boardId, Long ceremonyId, UpdateCeremonyRequest request);

    void deleteCeremony(Long userId, Long boardId, Long ceremonyId);

    // Item Management
    TrousseauItemResponse addItemToCeremony(Long userId, Long boardId, Long ceremonyId, AddCeremonyItemRequest request);

    TrousseauItemResponse updateCeremonyItem(Long userId, Long boardId, Long ceremonyId, Long itemId, UpdateCeremonyItemRequest request);

    void removeItemFromCeremony(Long userId, Long boardId, Long ceremonyId, Long itemId);

    // Collaborator Management
    TrousseauCollaboratorResponse inviteCollaborator(Long userId, Long boardId, InviteCollaboratorRequest request);

    List<TrousseauCollaboratorResponse> getCollaborators(Long userId, Long boardId);

    TrousseauCollaboratorResponse updateCollaborator(Long userId, Long boardId, Long collaboratorId, UpdateCollaboratorRequest request);

    void removeCollaborator(Long userId, Long boardId, Long collaboratorId);

    // Share Token Management
    TrousseauBoardResponse regenerateShareToken(Long userId, Long boardId);

    // Public / Share Token Guest Access & Voting
    SharedTrousseauViewResponse getSharedBoardByToken(String shareToken);

    TrousseauVoteResponse castVote(String shareToken, Long itemId, CastVoteRequest request);

    List<TrousseauVoteResponse> getItemVotes(String shareToken, Long itemId);

    // Core Commerce Cart Bridge
    TransferToCartResponse transferItemToCart(Long userId, Long boardId, Long ceremonyId, Long itemId);

    TransferToCartResponse transferCeremonyToCart(Long userId, Long boardId, Long ceremonyId);

    TransferToCartResponse transferAllToCart(Long userId, Long boardId);

    // Ownership Authorization
    void validateBoardOwnership(Long userId, Long boardId);
}
