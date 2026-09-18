package com.sareekart.service;

import com.sareekart.dto.trousseau.*;
import com.sareekart.entity.Product;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TrousseauCollaborationAndVotingTest {

    @Autowired
    private TrousseauService trousseauService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User brideUser;
    private User intruderUser;
    private Product bridalSaree;
    private TrousseauBoardResponse boardResponse;
    private TrousseauCeremonyResponse ceremonyResponse;
    private TrousseauItemResponse itemResponse;

    @BeforeEach
    void setUp() {
        brideUser = userRepository.save(User.builder()
                .firstName("Ananya")
                .lastName("Sharma")
                .email("ananya." + UUID.randomUUID() + "@example.com")
                .password("securePass123")
                .role(Role.CUSTOMER)
                .build());

        intruderUser = userRepository.save(User.builder()
                .firstName("Sneaky")
                .lastName("Intruder")
                .email("intruder." + UUID.randomUUID() + "@example.com")
                .password("securePass123")
                .role(Role.CUSTOMER)
                .build());

        bridalSaree = productRepository.save(Product.builder()
                .name("Handwoven Crimson Kanchipuram Silk")
                .description("Pure zari wedding silk")
                .price(new BigDecimal("78000.00"))
                .stockQuantity(5)
                .active(true)
                .build());

        // Create board
        boardResponse = trousseauService.createBoard(brideUser.getId(), CreateTrousseauBoardRequest.builder()
                .title("Ananya's Royal Wedding Curation")
                .weddingDate(LocalDate.now().plusMonths(4))
                .notes("Curating auspicious sarees with family")
                .isPublicVoting(true)
                .build());

        // Add ceremony
        ceremonyResponse = trousseauService.addCeremony(brideUser.getId(), boardResponse.getId(), CreateCeremonyRequest.builder()
                .ceremonyType("MUHURTHAM")
                .title("Main Wedding Muhurtham")
                .targetBudget(new BigDecimal("100000.00"))
                .colorTheme("Crimson & Gold")
                .displayOrder(1)
                .build());

        // Add item
        itemResponse = trousseauService.addItemToCeremony(brideUser.getId(), boardResponse.getId(), ceremonyResponse.getId(), AddCeremonyItemRequest.builder()
                .productId(bridalSaree.getId())
                .notes("Auspicious drape for muhurtham")
                .isAiRecommended(false)
                .build());
    }

    // =========================================================================
    // Collaborator Management Tests
    // =========================================================================

    @Test
    @DisplayName("Stage 5 Test 1: Board owner can invite, list, update, and remove collaborators")
    void testCollaboratorLifecycle() {
        // 1. Invite collaborator
        InviteCollaboratorRequest inviteReq = InviteCollaboratorRequest.builder()
                .name("Pooja Sharma")
                .phone("+919876543210")
                .email("pooja@example.com")
                .role("VOTER")
                .build();

        TrousseauCollaboratorResponse invited = trousseauService.inviteCollaborator(brideUser.getId(), boardResponse.getId(), inviteReq);
        assertNotNull(invited.getId());
        assertEquals("Pooja Sharma", invited.getName());
        assertEquals("+919876543210", invited.getPhone());
        assertEquals("VOTER", invited.getRole());
        assertEquals("PENDING", invited.getInviteStatus());

        // 2. List collaborators
        List<TrousseauCollaboratorResponse> list = trousseauService.getCollaborators(brideUser.getId(), boardResponse.getId());
        assertEquals(1, list.size());
        assertEquals(invited.getId(), list.get(0).getId());

        // 3. Update collaborator (e.g. Co-curator + accepted)
        UpdateCollaboratorRequest updateReq = UpdateCollaboratorRequest.builder()
                .role("CO_CURATOR")
                .inviteStatus("ACCEPTED")
                .build();

        TrousseauCollaboratorResponse updated = trousseauService.updateCollaborator(brideUser.getId(), boardResponse.getId(), invited.getId(), updateReq);
        assertEquals("CO_CURATOR", updated.getRole());
        assertEquals("ACCEPTED", updated.getInviteStatus());

        // 4. Remove collaborator
        trousseauService.removeCollaborator(brideUser.getId(), boardResponse.getId(), invited.getId());
        List<TrousseauCollaboratorResponse> afterRemoval = trousseauService.getCollaborators(brideUser.getId(), boardResponse.getId());
        assertTrue(afterRemoval.isEmpty());
    }

    @Test
    @DisplayName("Stage 5 Test 2: Intruder cannot invite, update, or remove collaborators from another family's board")
    void testCollaboratorAuthorizationProtection() {
        InviteCollaboratorRequest inviteReq = InviteCollaboratorRequest.builder()
                .name("Malicious Contact")
                .phone("+919999999999")
                .email("malicious@example.com")
                .role("VIEWER")
                .build();

        // Intruder tries to invite
        assertThrows(AccessDeniedException.class, () ->
                trousseauService.inviteCollaborator(intruderUser.getId(), boardResponse.getId(), inviteReq));

        // Bride invites valid collaborator
        TrousseauCollaboratorResponse validCollab = trousseauService.inviteCollaborator(brideUser.getId(), boardResponse.getId(), inviteReq);

        // Intruder tries to update
        UpdateCollaboratorRequest updateReq = UpdateCollaboratorRequest.builder().role("CO_CURATOR").build();
        assertThrows(AccessDeniedException.class, () ->
                trousseauService.updateCollaborator(intruderUser.getId(), boardResponse.getId(), validCollab.getId(), updateReq));

        // Intruder tries to remove
        assertThrows(AccessDeniedException.class, () ->
                trousseauService.removeCollaborator(intruderUser.getId(), boardResponse.getId(), validCollab.getId()));
    }

    // =========================================================================
    // Secure Public Sharing & Token Regeneration Tests
    // =========================================================================

    @Test
    @DisplayName("Stage 5 Test 3: Public access via secure 256-bit token exposes board without exposing customer PII")
    void testSharedBoardAccess() {
        String token = boardResponse.getShareToken();
        assertNotNull(token);
        assertTrue(token.startsWith("tkn_"));

        SharedTrousseauViewResponse sharedView = trousseauService.getSharedBoardByToken(token);
        assertNotNull(sharedView);
        assertEquals(token, sharedView.getShareToken());
        assertEquals("Ananya's Royal Wedding Curation", sharedView.getTitle());
        assertEquals("Ananya", sharedView.getBrideOrOwnerName());
        assertEquals(1, sharedView.getTotalCeremonies());
        assertEquals(new BigDecimal("100000.00"), sharedView.getTotalBudget());
        assertEquals(1, sharedView.getCeremonies().size());
        assertEquals(1, sharedView.getCeremonies().get(0).getItemCount());
        assertEquals(new BigDecimal("78000.00"), sharedView.getCeremonies().get(0).getAllocatedSpend());
    }

    @Test
    @DisplayName("Stage 5 Test 4: Regenerating share token immediately revokes previous token")
    void testShareTokenRegenerationAndRevocation() {
        String oldToken = boardResponse.getShareToken();

        // Regenerate token
        TrousseauBoardResponse updatedBoard = trousseauService.regenerateShareToken(brideUser.getId(), boardResponse.getId());
        String newToken = updatedBoard.getShareToken();

        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);

        // Old token lookup must fail
        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.getSharedBoardByToken(oldToken));

        // New token lookup must succeed
        SharedTrousseauViewResponse newSharedView = trousseauService.getSharedBoardByToken(newToken);
        assertNotNull(newSharedView);
        assertEquals(newToken, newSharedView.getShareToken());

        // Intruder cannot regenerate token
        assertThrows(AccessDeniedException.class, () ->
                trousseauService.regenerateShareToken(intruderUser.getId(), boardResponse.getId()));
    }

    @Test
    @DisplayName("Stage 5 Test 5: Inactive/Archived board rejects public token access and voting")
    void testInactiveBoardRejection() {
        // Archive the board
        trousseauService.updateBoard(brideUser.getId(), boardResponse.getId(), UpdateTrousseauBoardRequest.builder()
                .status("ARCHIVED")
                .build());

        String token = boardResponse.getShareToken();

        // Public view rejected
        BadRequestException viewEx = assertThrows(BadRequestException.class, () ->
                trousseauService.getSharedBoardByToken(token));
        assertTrue(viewEx.getMessage().contains("no longer active"));

        // Public voting rejected
        CastVoteRequest voteReq = CastVoteRequest.builder()
                .voterName("Auntie")
                .reaction("LOVE")
                .build();
        BadRequestException voteEx = assertThrows(BadRequestException.class, () ->
                trousseauService.castVote(token, itemResponse.getId(), voteReq));
        assertTrue(voteEx.getMessage().contains("no longer active"));
    }

    // =========================================================================
    // Family Voting & Deduplication Tests
    // =========================================================================

    @Test
    @DisplayName("Stage 5 Test 6: Guests can cast votes; duplicate voter updates previous reaction and aggregates counts correctly")
    void testVotingAndDeduplication() {
        String token = boardResponse.getShareToken();
        Long itemId = itemResponse.getId();

        // 1. First vote: Aunt Sunita votes LOVE
        CastVoteRequest vote1 = CastVoteRequest.builder()
                .voterName("Aunt Sunita")
                .voterPhone("+919876543210")
                .reaction("LOVE")
                .note("Absolutely regal for Muhurtham!")
                .build();

        TrousseauVoteResponse res1 = trousseauService.castVote(token, itemId, vote1);
        assertNotNull(res1.getId());
        assertEquals("Aunt Sunita", res1.getVoterName());
        assertEquals("LOVE", res1.getReaction());
        assertEquals("Absolutely regal for Muhurtham!", res1.getNote());
        // Phone masked: +919876543210 -> +91****3210
        assertNotNull(res1.getVoterPhoneMasked());
        assertTrue(res1.getVoterPhoneMasked().contains("****"));

        // Check item votes
        List<TrousseauVoteResponse> votes = trousseauService.getItemVotes(token, itemId);
        assertEquals(1, votes.size());

        // 2. Second vote: Cousin Rahul votes PASS
        CastVoteRequest vote2 = CastVoteRequest.builder()
                .voterName("Cousin Rahul")
                .voterPhone("+919123456789")
                .reaction("PASS")
                .note("A bit too traditional, maybe something modern?")
                .build();

        trousseauService.castVote(token, itemId, vote2);
        votes = trousseauService.getItemVotes(token, itemId);
        assertEquals(2, votes.size());

        // Verify board response aggregates reaction counts (LOVE=1, PASS=1, LIKE=0)
        TrousseauBoardResponse board = trousseauService.getBoardById(brideUser.getId(), boardResponse.getId());
        TrousseauItemResponse refreshedItem = board.getCeremonies().get(0).getItems().get(0);
        assertEquals(1L, refreshedItem.getLoveCount());
        assertEquals(0L, refreshedItem.getLikeCount());
        assertEquals(1L, refreshedItem.getPassCount());

        // 3. Deduplication / Vote update: Aunt Sunita changes mind from LOVE to LIKE
        CastVoteRequest vote1Update = CastVoteRequest.builder()
                .voterName("Aunt Sunita")
                .voterPhone("+919876543210")
                .reaction("LIKE")
                .note("Still lovely, but maybe check Banarasi too")
                .build();

        TrousseauVoteResponse res1Updated = trousseauService.castVote(token, itemId, vote1Update);
        assertEquals(res1.getId(), res1Updated.getId()); // Same vote entity updated!
        assertEquals("LIKE", res1Updated.getReaction());

        // Total votes remain 2 (not 3)
        votes = trousseauService.getItemVotes(token, itemId);
        assertEquals(2, votes.size());

        // Re-check aggregated reaction counts (LOVE=0, LIKE=1, PASS=1)
        board = trousseauService.getBoardById(brideUser.getId(), boardResponse.getId());
        refreshedItem = board.getCeremonies().get(0).getItems().get(0);
        assertEquals(0L, refreshedItem.getLoveCount());
        assertEquals(1L, refreshedItem.getLikeCount());
        assertEquals(1L, refreshedItem.getPassCount());
    }

    @Test
    @DisplayName("Stage 5 Test 7: Public voting disabled on board rejects new votes")
    void testDisabledPublicVotingRejection() {
        // Turn off public voting
        trousseauService.updateBoard(brideUser.getId(), boardResponse.getId(), UpdateTrousseauBoardRequest.builder()
                .isPublicVoting(false)
                .build());

        String token = boardResponse.getShareToken();
        CastVoteRequest voteReq = CastVoteRequest.builder()
                .voterName("Friend Neha")
                .reaction("LOVE")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                trousseauService.castVote(token, itemResponse.getId(), voteReq));
        assertTrue(ex.getMessage().contains("Public voting is currently disabled"));
    }

    @Test
    @DisplayName("Stage 5 Test 8: Voting on non-existent item or foreign item fails with 404")
    void testVoteOnNonExistentItem() {
        String token = boardResponse.getShareToken();
        CastVoteRequest voteReq = CastVoteRequest.builder()
                .voterName("Friend Neha")
                .reaction("LOVE")
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.castVote(token, 999999L, voteReq));
    }
}
