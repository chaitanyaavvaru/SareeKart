package com.sareekart.service.impl;

import com.sareekart.dto.trousseau.*;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.*;
import com.sareekart.service.TrousseauService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrousseauServiceImpl implements TrousseauService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TrousseauBoardRepository boardRepository;
    private final TrousseauCeremonyRepository ceremonyRepository;
    private final TrousseauItemRepository itemRepository;
    private final TrousseauCollaboratorRepository collaboratorRepository;
    private final TrousseauVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.sareekart.service.CartService cartService;
    private final com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService;
    private final com.sareekart.service.TrousseauSseService trousseauSseService;

    // =========================================================================
    // Board Lifecycle
    // =========================================================================

    @Override
    @Transactional
    public TrousseauBoardResponse createBoard(Long userId, CreateTrousseauBoardRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String shareToken = generateUniqueShareToken();

        TrousseauBoard board = TrousseauBoard.builder()
                .user(user)
                .title(request.getTitle())
                .weddingDate(request.getWeddingDate())
                .notes(request.getNotes())
                .shareToken(shareToken)
                .isPublicVoting(request.getIsPublicVoting() == null || request.getIsPublicVoting())
                .status("ACTIVE")
                .build();

        if (request.getCeremonies() != null && !request.getCeremonies().isEmpty()) {
            int order = 1;
            for (CreateCeremonyRequest cReq : request.getCeremonies()) {
                TrousseauCeremony ceremony = TrousseauCeremony.builder()
                        .ceremonyType(cReq.getCeremonyType())
                        .title(cReq.getTitle())
                        .colorTheme(cReq.getColorTheme())
                        .targetBudget(cReq.getTargetBudget())
                        .displayOrder(cReq.getDisplayOrder() != null ? cReq.getDisplayOrder() : order++)
                        .build();
                board.addCeremony(ceremony);
            }
        }

        TrousseauBoard saved = boardRepository.save(board);
        log.info("Created TrousseauBoard ID {} for user {}", saved.getId(), userId);
        return mapToBoardResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TrousseauBoardResponse getBoardById(Long userId, Long boardId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        return mapToBoardResponse(board);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrousseauBoardResponse> getUserBoards(Long userId) {
        return boardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToBoardResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrousseauBoardResponse updateBoard(Long userId, Long boardId, UpdateTrousseauBoardRequest request) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            board.setTitle(request.getTitle());
        }
        if (request.getWeddingDate() != null) {
            board.setWeddingDate(request.getWeddingDate());
        }
        if (request.getNotes() != null) {
            board.setNotes(request.getNotes());
        }
        if (request.getIsPublicVoting() != null) {
            board.setIsPublicVoting(request.getIsPublicVoting());
        }
        if (request.getStatus() != null) {
            board.setStatus(request.getStatus());
        }

        TrousseauBoard updated = boardRepository.save(board);
        log.info("Updated TrousseauBoard ID {} for user {}", boardId, userId);
        return mapToBoardResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        boardRepository.delete(board);
        log.info("Deleted TrousseauBoard ID {} for user {}", boardId, userId);
    }

    // =========================================================================
    // Ceremony Management
    // =========================================================================

    @Override
    @Transactional
    public TrousseauCeremonyResponse addCeremony(Long userId, Long boardId, CreateCeremonyRequest request) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);

        int order = request.getDisplayOrder() != null ? request.getDisplayOrder() : board.getCeremonies().size() + 1;

        TrousseauCeremony ceremony = TrousseauCeremony.builder()
                .ceremonyType(request.getCeremonyType())
                .title(request.getTitle())
                .colorTheme(request.getColorTheme())
                .targetBudget(request.getTargetBudget())
                .displayOrder(order)
                .build();

        board.addCeremony(ceremony);
        TrousseauCeremony saved = ceremonyRepository.save(ceremony);
        log.info("Added ceremony ID {} to board {}", saved.getId(), boardId);
        TrousseauCeremonyResponse response = mapToCeremonyResponse(saved);
        if (trousseauSseService != null) {
            trousseauSseService.publish(boardId, "CEREMONY_ADDED", response);
        }
        return response;
    }

    @Override
    @Transactional
    public TrousseauCeremonyResponse updateCeremony(Long userId, Long boardId, Long ceremonyId, UpdateCeremonyRequest request) {
        findBoardAndValidateOwnership(userId, boardId);

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        ceremony.setTitle(request.getTitle());
        ceremony.setColorTheme(request.getColorTheme());
        ceremony.setTargetBudget(request.getTargetBudget());
        if (request.getDisplayOrder() != null) {
            ceremony.setDisplayOrder(request.getDisplayOrder());
        }

        TrousseauCeremony updated = ceremonyRepository.save(ceremony);
        return mapToCeremonyResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCeremony(Long userId, Long boardId, Long ceremonyId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        board.removeCeremony(ceremony);
        ceremonyRepository.delete(ceremony);
        log.info("Deleted ceremony ID {} from board {}", ceremonyId, boardId);
        if (trousseauSseService != null) {
            trousseauSseService.publish(boardId, "CEREMONY_REMOVED", java.util.Map.of("ceremonyId", ceremonyId));
        }
    }

    // =========================================================================
    // Item Management (Authoritative MySQL Grounding & Inventory Isolation)
    // =========================================================================

    @Override
    @Transactional
    public TrousseauItemResponse addItemToCeremony(Long userId, Long boardId, Long ceremonyId, AddCeremonyItemRequest request) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        // 1. Authoritative MySQL Product lookup
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        // 2. Active catalog status check
        if (Boolean.FALSE.equals(product.getActive())) {
            throw new BadRequestException("Product is inactive and cannot be added to a bridal trousseau");
        }

        // 3. Stock availability verification (Do NOT decrement physical inventory!)
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
            throw new BadRequestException("Product is currently out of stock and cannot be added to a bridal trousseau");
        }

        // 4. Duplicate item prevention
        if (itemRepository.existsByCeremonyIdAndProductId(ceremonyId, product.getId())) {
            throw new BadRequestException("This saree has already been shortlisted for this ceremony");
        }

        TrousseauItem item = TrousseauItem.builder()
                .board(board)
                .ceremony(ceremony)
                .product(product)
                .addedByUser(board.getUser())
                .isAiRecommended(Boolean.TRUE.equals(request.getIsAiRecommended()))
                .notes(request.getNotes())
                .status("SHORTLISTED")
                .build();

        ceremony.addItem(item);
        TrousseauItem saved = itemRepository.save(item);
        log.info("Pinned product ID {} to ceremony ID {} in board {}", product.getId(), ceremonyId, boardId);
        TrousseauItemResponse response = mapToItemResponse(saved);
        if (trousseauSseService != null) {
            trousseauSseService.publish(boardId, "ITEM_ADDED", response);
        }
        return response;
    }

    @Override
    @Transactional
    public TrousseauItemResponse updateCeremonyItem(Long userId, Long boardId, Long ceremonyId, Long itemId, UpdateCeremonyItemRequest request) {
        findBoardAndValidateOwnership(userId, boardId);

        TrousseauItem item = itemRepository.findByIdAndCeremonyId(itemId, ceremonyId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau item not found: " + itemId));

        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }

        TrousseauItem updated = itemRepository.save(item);
        return mapToItemResponse(updated);
    }

    @Override
    @Transactional
    public void removeItemFromCeremony(Long userId, Long boardId, Long ceremonyId, Long itemId) {
        findBoardAndValidateOwnership(userId, boardId);

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        TrousseauItem item = itemRepository.findByIdAndCeremonyId(itemId, ceremonyId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau item not found: " + itemId));

        ceremony.removeItem(item);
        itemRepository.delete(item);
        log.info("Removed trousseau item ID {} from ceremony ID {}", itemId, ceremonyId);
        if (trousseauSseService != null) {
            trousseauSseService.publish(boardId, "ITEM_REMOVED", java.util.Map.of("ceremonyId", ceremonyId, "itemId", itemId));
        }
    }

    // =========================================================================
    // Collaborators, Public Guest View & Cart Bridge (Stage 5 / Stage 7)
    // =========================================================================

    @Override
    @Transactional
    public TrousseauCollaboratorResponse inviteCollaborator(Long userId, Long boardId, InviteCollaboratorRequest request) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);

        TrousseauCollaborator collaborator = TrousseauCollaborator.builder()
                .board(board)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : "VOTER")
                .inviteStatus("PENDING")
                .build();

        board.addCollaborator(collaborator);
        TrousseauCollaborator saved = collaboratorRepository.save(collaborator);
        return mapToCollaboratorResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrousseauCollaboratorResponse> getCollaborators(Long userId, Long boardId) {
        findBoardAndValidateOwnership(userId, boardId);
        return collaboratorRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrousseauCollaboratorResponse updateCollaborator(Long userId, Long boardId, Long collaboratorId, UpdateCollaboratorRequest request) {
        findBoardAndValidateOwnership(userId, boardId);
        TrousseauCollaborator collaborator = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found: " + collaboratorId));

        if (!collaborator.getBoard().getId().equals(boardId)) {
            throw new BadRequestException("Collaborator does not belong to this trousseau board");
        }

        if (request.getRole() != null) {
            collaborator.setRole(request.getRole());
        }
        if (request.getInviteStatus() != null) {
            collaborator.setInviteStatus(request.getInviteStatus());
        }

        TrousseauCollaborator saved = collaboratorRepository.save(collaborator);
        return mapToCollaboratorResponse(saved);
    }

    @Override
    @Transactional
    public void removeCollaborator(Long userId, Long boardId, Long collaboratorId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        TrousseauCollaborator collaborator = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found: " + collaboratorId));
        board.removeCollaborator(collaborator);
        collaboratorRepository.delete(collaborator);
    }

    @Override
    @Transactional
    public TrousseauBoardResponse regenerateShareToken(Long userId, Long boardId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        String newToken = generateUniqueShareToken();
        board.setShareToken(newToken);
        TrousseauBoard saved = boardRepository.save(board);
        log.info("Regenerated share token for board ID {} (old token revoked)", boardId);
        return mapToBoardResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedTrousseauViewResponse getSharedBoardByToken(String shareToken) {
        TrousseauBoard board = boardRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired trousseau share link"));

        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau share link is no longer active");
        }

        log.info("Public view accessed for board ID {}", board.getId());

        List<TrousseauCeremonyResponse> ceremonies = board.getCeremonies().stream()
                .sorted(Comparator.comparing(TrousseauCeremony::getDisplayOrder))
                .map(this::mapToCeremonyResponse)
                .collect(Collectors.toList());

        BigDecimal totalBudget = board.getCeremonies().stream()
                .map(c -> c.getTargetBudget() != null ? c.getTargetBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String ownerName = board.getUser() != null ? board.getUser().getFirstName() : "Bride";

        return SharedTrousseauViewResponse.builder()
                .shareToken(board.getShareToken())
                .title(board.getTitle())
                .brideOrOwnerName(ownerName)
                .weddingDate(board.getWeddingDate())
                .notes(board.getNotes())
                .isPublicVoting(board.getIsPublicVoting())
                .status(board.getStatus())
                .totalCeremonies(ceremonies.size())
                .totalBudget(totalBudget)
                .ceremonies(ceremonies)
                .build();
    }

    @Override
    @Transactional
    public TrousseauVoteResponse castVote(String shareToken, Long itemId, CastVoteRequest request) {
        TrousseauBoard board = boardRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired trousseau share link"));

        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau share link is no longer active");
        }

        if (Boolean.FALSE.equals(board.getIsPublicVoting())) {
            throw new BadRequestException("Public voting is currently disabled for this trousseau board");
        }

        TrousseauItem item = itemRepository.findByIdAndBoardId(itemId, board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found on this trousseau board: " + itemId));

        // Safe duplicate/update handling to prevent ballot stuffing
        java.util.Optional<TrousseauVote> existingVote = voteRepository.findByItemIdAndVoterName(itemId, request.getVoterName().trim());
        TrousseauVote vote;
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            vote.setReaction(request.getReaction());
            vote.setNote(request.getNote());
            if (request.getVoterPhone() != null && !request.getVoterPhone().isBlank()) {
                vote.setVoterPhone(request.getVoterPhone());
            }
            log.info("Updated reaction to '{}' for voter '{}' on item {}", request.getReaction(), request.getVoterName(), itemId);
        } else {
            vote = TrousseauVote.builder()
                    .item(item)
                    .voterName(request.getVoterName().trim())
                    .voterPhone(request.getVoterPhone())
                    .reaction(request.getReaction())
                    .note(request.getNote())
                    .build();
            item.addVote(vote);
            log.info("Recorded reaction '{}' for voter '{}' on item {}", request.getReaction(), request.getVoterName(), itemId);
        }

        TrousseauVote saved = voteRepository.save(vote);

        // Notify board owner via WhatsApp adapter (fire-and-forget, non-blocking)
        try {
            Long ceremonyId = (item.getCeremony() != null) ? item.getCeremony().getId() : null;
            trousseauWhatsAppService.notifyOwnerOfVote(board.getId(), ceremonyId, item.getId(),
                    saved.getVoterName(), saved.getReaction(), saved.getNote());
        } catch (Exception e) {
            log.warn("Non-blocking owner notification failed: {}", e.getMessage());
        }

        // Broadcast live SSE event to active board subscribers
        if (trousseauSseService != null) {
            try {
                long loveCount = voteRepository.countByItemIdAndReaction(item.getId(), "LOVE");
                long likeCount = voteRepository.countByItemIdAndReaction(item.getId(), "LIKE");
                long passCount = voteRepository.countByItemIdAndReaction(item.getId(), "PASS");
                java.util.Map<String, Object> votePayload = java.util.Map.of(
                        "itemId", item.getId(),
                        "reaction", saved.getReaction(),
                        "voterName", saved.getVoterName(),
                        "loveCount", loveCount,
                        "likeCount", likeCount,
                        "passCount", passCount
                );
                trousseauSseService.publish(board.getId(), "VOTE_CAST", votePayload);
            } catch (Exception e) {
                log.warn("Failed to broadcast SSE VOTE_CAST event: {}", e.getMessage());
            }
        }

        return mapToVoteResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrousseauVoteResponse> getItemVotes(String shareToken, Long itemId) {
        TrousseauBoard board = boardRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired trousseau share link"));

        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau share link is no longer active");
        }

        itemRepository.findByIdAndBoardId(itemId, board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found on this trousseau board: " + itemId));

        return voteRepository.findByItemIdOrderByCreatedAtDesc(itemId).stream()
                .map(this::mapToVoteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransferToCartResponse transferItemToCart(Long userId, Long boardId, Long ceremonyId, Long itemId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau board is archived or inactive");
        }

        ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        TrousseauItem item = itemRepository.findByIdAndBoardId(itemId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));

        return convertItemsToCart(userId, boardId, ceremonyId, List.of(item));
    }

    @Override
    @Transactional
    public TransferToCartResponse transferCeremonyToCart(Long userId, Long boardId, Long ceremonyId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau board is archived or inactive");
        }

        TrousseauCeremony ceremony = ceremonyRepository.findByIdAndBoardId(ceremonyId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ceremony not found: " + ceremonyId));

        return convertItemsToCart(userId, boardId, ceremonyId, ceremony.getItems());
    }

    @Override
    @Transactional
    public TransferToCartResponse transferAllToCart(Long userId, Long boardId) {
        TrousseauBoard board = findBoardAndValidateOwnership(userId, boardId);
        if (!"ACTIVE".equalsIgnoreCase(board.getStatus())) {
            throw new BadRequestException("This trousseau board is archived or inactive");
        }

        List<TrousseauItem> allItems = new java.util.ArrayList<>();
        for (TrousseauCeremony ceremony : board.getCeremonies()) {
            allItems.addAll(ceremony.getItems());
        }

        return convertItemsToCart(userId, boardId, null, allItems);
    }

    private TransferToCartResponse convertItemsToCart(
            Long userId,
            Long boardId,
            Long ceremonyId,
            List<TrousseauItem> itemsToTransfer) {

        TransferToCartResponse response = TransferToCartResponse.builder()
                .boardId(boardId)
                .ceremonyId(ceremonyId)
                .addedCount(0)
                .alreadyInCartCount(0)
                .outOfStockCount(0)
                .addedProductIds(new java.util.ArrayList<>())
                .skippedProductIds(new java.util.ArrayList<>())
                .unavailableProductIds(new java.util.ArrayList<>())
                .build();

        if (itemsToTransfer == null || itemsToTransfer.isEmpty()) {
            response.setMessage("No items found to transfer to cart");
            return response;
        }

        // 1. Fetch current cart to guarantee idempotency and avoid duplicate quantities
        com.sareekart.dto.response.CartResponse currentCart = null;
        java.util.Set<Long> existingCartProductIds = new java.util.HashSet<>();
        try {
            currentCart = cartService.getCart(userId);
            if (currentCart != null && currentCart.getItems() != null) {
                for (com.sareekart.dto.response.CartItemResponse cartItem : currentCart.getItems()) {
                    if (cartItem.getProductId() != null) {
                        existingCartProductIds.add(cartItem.getProductId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to fetch existing cart for user {}: {}", userId, e.getMessage());
        }

        com.sareekart.dto.response.CartResponse lastCartResponse = currentCart;

        // 2. Iterate and transfer each item
        for (TrousseauItem item : itemsToTransfer) {
            if (item.getProduct() == null) {
                continue;
            }
            Long productId = item.getProduct().getId();

            // A. Idempotency Check: Already present in user's cart
            if (existingCartProductIds.contains(productId)) {
                response.getSkippedProductIds().add(productId);
                response.setAlreadyInCartCount(response.getAlreadyInCartCount() + 1);
                item.setStatus("IN_CART");
                continue;
            }

            // B. Live Catalog & Inventory Re-validation from MySQL
            java.util.Optional<Product> optProduct = productRepository.findById(productId);
            if (optProduct.isEmpty() || !Boolean.TRUE.equals(optProduct.get().getActive())) {
                log.warn("Product {} is delisted or inactive at conversion time", productId);
                response.getUnavailableProductIds().add(productId);
                response.setOutOfStockCount(response.getOutOfStockCount() + 1);
                continue;
            }

            Product liveProduct = optProduct.get();
            if (liveProduct.getStockQuantity() == null || liveProduct.getStockQuantity() < 1) {
                log.warn("Product {} has 0 stock at conversion time", productId);
                response.getUnavailableProductIds().add(productId);
                response.setOutOfStockCount(response.getOutOfStockCount() + 1);
                continue;
            }

            // C. Invoke Phase 4 CartService
            try {
                com.sareekart.dto.request.CartItemRequest cartReq = com.sareekart.dto.request.CartItemRequest.builder()
                        .productId(productId)
                        .quantity(1)
                        .build();

                lastCartResponse = cartService.addItemToCart(userId, cartReq);
                response.getAddedProductIds().add(productId);
                response.setAddedCount(response.getAddedCount() + 1);
                existingCartProductIds.add(productId);
                item.setStatus("IN_CART");
            } catch (Exception e) {
                log.warn("CartService rejected product {}: {}", productId, e.getMessage());
                response.getUnavailableProductIds().add(productId);
                response.setOutOfStockCount(response.getOutOfStockCount() + 1);
            }
        }

        // 3. Save trousseau item status updates
        itemRepository.saveAll(itemsToTransfer);

        // 4. Populate cart summary indicators
        if (lastCartResponse != null) {
            response.setTotalCartItems(lastCartResponse.getTotalItems());
            response.setCartTotalPrice(lastCartResponse.getTotalPrice());
        }

        String msg = String.format("Transfer complete: %d items added to cart, %d already in cart, %d unavailable.",
                response.getAddedCount(), response.getAlreadyInCartCount(), response.getOutOfStockCount());
        response.setMessage(msg);
        log.info("Trousseau to cart conversion: {}", msg);

        return response;
    }

    // =========================================================================
    // Internal Helper & Mapping Methods
    // =========================================================================

    private TrousseauBoard findBoardAndValidateOwnership(Long userId, Long boardId) {
        TrousseauBoard board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Trousseau board not found: " + boardId));
        if (board.getUser() == null || !board.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this trousseau board");
        }
        return board;
    }

    private String generateUniqueShareToken() {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "tkn_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private TrousseauBoardResponse mapToBoardResponse(TrousseauBoard board) {
        List<TrousseauCeremonyResponse> ceremonyResponses = board.getCeremonies().stream()
                .sorted(Comparator.comparing(TrousseauCeremony::getDisplayOrder))
                .map(this::mapToCeremonyResponse)
                .collect(Collectors.toList());

        List<TrousseauCollaboratorResponse> collaboratorResponses = board.getCollaborators().stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());

        BigDecimal totalBudget = board.getCeremonies().stream()
                .map(c -> c.getTargetBudget() != null ? c.getTargetBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAllocatedSpend = ceremonyResponses.stream()
                .map(TrousseauCeremonyResponse::getAllocatedSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItemsCount = ceremonyResponses.stream()
                .mapToInt(TrousseauCeremonyResponse::getItemCount)
                .sum();

        String ownerName = board.getUser() != null
                ? (board.getUser().getFirstName() + (board.getUser().getLastName() != null ? " " + board.getUser().getLastName() : "")).trim()
                : "Unknown";

        return TrousseauBoardResponse.builder()
                .id(board.getId())
                .userId(board.getUser() != null ? board.getUser().getId() : null)
                .ownerName(ownerName)
                .title(board.getTitle())
                .weddingDate(board.getWeddingDate())
                .notes(board.getNotes())
                .shareToken(board.getShareToken())
                .shareUrl("/trousseau/shared/" + board.getShareToken())
                .isPublicVoting(board.getIsPublicVoting())
                .status(board.getStatus())
                .totalCeremonies(ceremonyResponses.size())
                .totalBudget(totalBudget)
                .totalItemsCount(totalItemsCount)
                .totalAllocatedSpend(totalAllocatedSpend)
                .ceremonies(ceremonyResponses)
                .collaborators(collaboratorResponses)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private TrousseauCeremonyResponse mapToCeremonyResponse(TrousseauCeremony ceremony) {
        List<TrousseauItemResponse> itemResponses = ceremony.getItems().stream()
                .sorted(Comparator.comparing(TrousseauItem::getCreatedAt))
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        BigDecimal allocatedSpend = itemResponses.stream()
                .map(item -> item.getProductPrice() != null ? item.getProductPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TrousseauCeremonyResponse.builder()
                .id(ceremony.getId())
                .boardId(ceremony.getBoard() != null ? ceremony.getBoard().getId() : null)
                .ceremonyType(ceremony.getCeremonyType())
                .title(ceremony.getTitle())
                .colorTheme(ceremony.getColorTheme())
                .targetBudget(ceremony.getTargetBudget())
                .displayOrder(ceremony.getDisplayOrder())
                .itemCount(itemResponses.size())
                .allocatedSpend(allocatedSpend)
                .items(itemResponses)
                .createdAt(ceremony.getCreatedAt())
                .build();
    }

    private TrousseauItemResponse mapToItemResponse(TrousseauItem item) {
        Product p = item.getProduct();
        String primaryImage = (p != null && p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0) : null;
        String fabric = p != null ? (p.getFabricEntity() != null ? p.getFabricEntity().getName() : p.getFabric()) : null;
        String color = p != null ? (p.getColorEntity() != null ? p.getColorEntity().getName() : p.getColor()) : null;

        long loveCount = voteRepository.countByItemIdAndReaction(item.getId(), "LOVE");
        long likeCount = voteRepository.countByItemIdAndReaction(item.getId(), "LIKE");
        long passCount = voteRepository.countByItemIdAndReaction(item.getId(), "PASS");

        List<TrousseauVoteResponse> voteResponses = voteRepository.findByItemIdOrderByCreatedAtDesc(item.getId()).stream()
                .map(this::mapToVoteResponse)
                .collect(Collectors.toList());

        return TrousseauItemResponse.builder()
                .id(item.getId())
                .boardId(item.getBoard() != null ? item.getBoard().getId() : null)
                .ceremonyId(item.getCeremony() != null ? item.getCeremony().getId() : null)
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .productPrice(p != null ? p.getPrice() : null)
                .productImageUrl(primaryImage)
                .fabric(fabric)
                .color(color)
                .stockQuantity(p != null ? p.getStockQuantity() : null)
                .isAiRecommended(item.getIsAiRecommended())
                .notes(item.getNotes())
                .status(item.getStatus())
                .loveCount(loveCount)
                .likeCount(likeCount)
                .passCount(passCount)
                .votes(voteResponses)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private TrousseauVoteResponse mapToVoteResponse(TrousseauVote vote) {
        String maskedPhone = maskPhoneNumber(vote.getVoterPhone());
        return TrousseauVoteResponse.builder()
                .id(vote.getId())
                .itemId(vote.getItem() != null ? vote.getItem().getId() : null)
                .voterName(vote.getVoterName())
                .voterPhoneMasked(maskedPhone)
                .reaction(vote.getReaction())
                .note(vote.getNote())
                .createdAt(vote.getCreatedAt())
                .build();
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return null;
        }
        int len = phone.length();
        return phone.substring(0, Math.min(3, len)) + "****" + phone.substring(Math.max(len - 4, 3));
    }

    private TrousseauCollaboratorResponse mapToCollaboratorResponse(TrousseauCollaborator collaborator) {
        return TrousseauCollaboratorResponse.builder()
                .id(collaborator.getId())
                .boardId(collaborator.getBoard() != null ? collaborator.getBoard().getId() : null)
                .name(collaborator.getName())
                .phone(collaborator.getPhone())
                .email(collaborator.getEmail())
                .role(collaborator.getRole())
                .inviteStatus(collaborator.getInviteStatus())
                .createdAt(collaborator.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBoardOwnership(Long userId, Long boardId) {
        findBoardAndValidateOwnership(userId, boardId);
    }
}
