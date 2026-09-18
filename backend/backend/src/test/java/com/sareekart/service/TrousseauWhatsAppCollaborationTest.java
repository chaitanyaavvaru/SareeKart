package com.sareekart.service;

import com.sareekart.dto.trousseau.*;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class TrousseauWhatsAppCollaborationTest {

    @Autowired
    private TrousseauService trousseauService;

    @Autowired
    private TrousseauWhatsAppService trousseauWhatsAppService;

    @MockBean
    private WhatsAppApiClient whatsAppApiClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TrousseauBoardRepository boardRepository;

    @Autowired
    private TrousseauCollaboratorRepository collaboratorRepository;

    private User brideUser;
    private User intruderUser;
    private Product kanchipuramRed;
    private Product banarasiGold;

    private TrousseauBoardResponse board;
    private TrousseauCeremonyResponse muhurthamCeremony;
    private TrousseauItemResponse item1;
    private TrousseauCollaboratorResponse collaborator;

    @BeforeEach
    void setUp() {
        // Reset mock
        reset(whatsAppApiClient);

        brideUser = userRepository.save(User.builder()
                .firstName("Radhika")
                .lastName("Merchant")
                .email("radhika." + UUID.randomUUID() + "@example.com")
                .mobile("+919876543210")
                .whatsappOptIn(true)
                .password("Password@123")
                .role(Role.CUSTOMER)
                .build());

        intruderUser = userRepository.save(User.builder()
                .firstName("Intruder")
                .lastName("User")
                .email("intruder." + UUID.randomUUID() + "@example.com")
                .mobile("+919111111111")
                .whatsappOptIn(true)
                .password("Password@123")
                .role(Role.CUSTOMER)
                .build());

        kanchipuramRed = productRepository.save(Product.builder()
                .name("Kanchipuram Crimson Silk Saree")
                .price(BigDecimal.valueOf(45000))
                .stockQuantity(5)
                .active(true)
                .fabric("Silk")
                .color("Crimson Red")
                .occasion("MUHURTHAM")
                .build());

        banarasiGold = productRepository.save(Product.builder()
                .name("Banarasi Antique Gold Brocade")
                .price(BigDecimal.valueOf(55000))
                .stockQuantity(3)
                .active(true)
                .fabric("Katan Silk")
                .color("Gold")
                .occasion("RECEPTION")
                .build());

        // Create Board
        board = trousseauService.createBoard(brideUser.getId(), CreateTrousseauBoardRequest.builder()
                .title("Radhika & Anant Royal Wedding")
                .weddingDate(LocalDate.of(2026, 12, 12))
                .notes("Heirloom bridal ensembles co-curated with family")
                .isPublicVoting(true)
                .build());

        // Create Ceremony
        muhurthamCeremony = trousseauService.addCeremony(brideUser.getId(), board.getId(), CreateCeremonyRequest.builder()
                .ceremonyType("MUHURTHAM")
                .title("Auspicious Muhurtham Ritual")
                .colorTheme("Crimson Red & Pure Zari Gold")
                .targetBudget(BigDecimal.valueOf(100000))
                .build());

        // Add Item
        item1 = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), muhurthamCeremony.getId(), AddCeremonyItemRequest.builder()
                .productId(kanchipuramRed.getId())
                .notes("Traditional Korvai weave for morning rituals")
                .build());

        // Add Collaborator
        collaborator = trousseauService.inviteCollaborator(brideUser.getId(), board.getId(), InviteCollaboratorRequest.builder()
                .name("Aunt Shobha")
                .phone("+919812345678")
                .email("shobha@example.com")
                .role("FAMILY")
                .build());
    }

    @Test
    @DisplayName("Stage 8 Test 1: Valid board owner generates and sends WhatsApp board share invitation")
    void testSendWhatsAppBoardShareInvitation() {
        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919812345678")
                .recipientName("Aunt Shobha")
                .customNote("Please review the sarees and let me know your thoughts!")
                .build();

        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendShareInvitation(
                brideUser.getId(), board.getId(), request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("+919812345678", response.getRecipientPhone());
        assertEquals("Aunt Shobha", response.getRecipientName());
        assertTrue(response.getShareUrl().contains(board.getShareToken()));
        assertTrue(response.getMessagePreview().contains("Radhika Merchant"));
        assertTrue(response.getMessagePreview().contains("Radhika & Anant Royal Wedding"));
        assertTrue(response.getMessagePreview().contains("Please review the sarees"));
    }

    @Test
    @DisplayName("Stage 8 Test 2: WhatsApp dispatch uses Phase 10 WhatsAppApiClient.sendTextMessage()")
    void testWhatsAppDispatchUsesPhase10Client() {
        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919812345678")
                .recipientName("Aunt Shobha")
                .build();

        trousseauWhatsAppService.sendShareInvitation(brideUser.getId(), board.getId(), request);

        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(whatsAppApiClient, times(1)).sendTextMessage(phoneCaptor.capture(), messageCaptor.capture());

        assertEquals("+919812345678", phoneCaptor.getValue());
        assertTrue(messageCaptor.getValue().contains("https://sareekart.com/trousseau/share/" + board.getShareToken()));
    }

    @Test
    @DisplayName("Stage 8 Test 3: Invalid, expired, or revoked share token is rejected")
    void testInvalidOrRevokedShareTokenRejected() {
        // Accessing with bogus token throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.getSharedBoardByToken("bogus-token-12345"));

        // Regenerate token (revoking old one)
        TrousseauBoardResponse regenerated = trousseauService.regenerateShareToken(brideUser.getId(), board.getId());
        assertNotEquals(board.getShareToken(), regenerated.getShareToken());

        // Old token is immediately rejected
        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.getSharedBoardByToken(board.getShareToken()));

        // New token succeeds
        SharedTrousseauViewResponse validView = trousseauService.getSharedBoardByToken(regenerated.getShareToken());
        assertNotNull(validView);
        assertEquals(board.getTitle(), validView.getTitle());
    }

    @Test
    @DisplayName("Stage 8 Test 4: Family member accesses permitted shared ceremony via token link without login")
    void testFamilyMemberAccessSharedBoardWithoutLogin() {
        SharedTrousseauViewResponse view = trousseauService.getSharedBoardByToken(board.getShareToken());

        assertNotNull(view);
        assertEquals(board.getTitle(), view.getTitle());
        assertEquals("Radhika", view.getBrideOrOwnerName());
        assertEquals(1, view.getCeremonies().size());

        TrousseauCeremonyResponse ceremony = view.getCeremonies().get(0);
        assertEquals("Auspicious Muhurtham Ritual", ceremony.getTitle());
        assertEquals("Crimson Red & Pure Zari Gold", ceremony.getColorTheme());
        assertEquals(1, ceremony.getItems().size());
        assertEquals(kanchipuramRed.getId(), ceremony.getItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Stage 8 Test 5: Family member submits vote with reaction and comment note")
    void testFamilyMemberSubmitsVoteWithNote() {
        CastVoteRequest voteRequest = CastVoteRequest.builder()
                .voterName("Aunt Shobha")
                .voterPhone("+919812345678")
                .reaction("LOVE")
                .note("The contrast pallu is breathtaking! Perfect for the mandap lighting.")
                .build();

        TrousseauVoteResponse voteResponse = trousseauService.castVote(board.getShareToken(), item1.getId(), voteRequest);

        assertNotNull(voteResponse);
        assertEquals("Aunt Shobha", voteResponse.getVoterName());
        assertEquals("LOVE", voteResponse.getReaction());
        assertEquals("The contrast pallu is breathtaking! Perfect for the mandap lighting.", voteResponse.getNote());

        // Verify votes retrieved via share token
        List<TrousseauVoteResponse> votes = trousseauService.getItemVotes(board.getShareToken(), item1.getId());
        assertEquals(1, votes.size());
        assertEquals("Aunt Shobha", votes.get(0).getVoterName());
    }

    @Test
    @DisplayName("Stage 8 Test 6: Duplicate vote from same voter safely updates reaction without duplicates")
    void testDuplicateVoteSafeUpdate() {
        CastVoteRequest vote1 = CastVoteRequest.builder()
                .voterName("Aunt Shobha")
                .reaction("LIKE")
                .note("Nice color.")
                .build();
        trousseauService.castVote(board.getShareToken(), item1.getId(), vote1);

        CastVoteRequest vote2 = CastVoteRequest.builder()
                .voterName("Aunt Shobha")
                .reaction("LOVE")
                .note("Changed my mind, absolutely love this one!")
                .build();
        trousseauService.castVote(board.getShareToken(), item1.getId(), vote2);

        List<TrousseauVoteResponse> votes = trousseauService.getItemVotes(board.getShareToken(), item1.getId());
        assertEquals(1, votes.size(), "Duplicate vote from same voter must be updated, not duplicated");
        assertEquals("LOVE", votes.get(0).getReaction());
        assertEquals("Changed my mind, absolutely love this one!", votes.get(0).getNote());
    }

    @Test
    @DisplayName("Stage 8 Test 7: Ceremony-specific WhatsApp share contains ceremony context, color theme, and URL")
    void testCeremonySpecificWhatsAppShare() {
        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919988776655")
                .recipientName("Grandmother Kamala")
                .ceremonyId(muhurthamCeremony.getId())
                .customNote("Does this match our heirloom jewelry?")
                .build();

        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendShareInvitation(
                brideUser.getId(), board.getId(), request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessagePreview().contains("Auspicious Muhurtham Ritual"));
        assertTrue(response.getMessagePreview().contains("Crimson Red & Pure Zari Gold"));
        assertTrue(response.getMessagePreview().contains("Does this match our heirloom jewelry?"));
        assertTrue(response.getMessagePreview().contains("https://sareekart.com/trousseau/share/" + board.getShareToken()));
    }

    @Test
    @DisplayName("Stage 8 Test 8: Unauthorized user cannot trigger WhatsApp share on another user's board")
    void testUnauthorizedUserCannotTriggerShare() {
        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919812345678")
                .recipientName("Someone")
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                trousseauWhatsAppService.sendShareInvitation(intruderUser.getId(), board.getId(), request));
    }

    @Test
    @DisplayName("Stage 8 Test 9: WhatsApp API failure is gracefully isolated without breaking trousseau board")
    void testWhatsAppApiFailureIsolated() {
        doThrow(new RuntimeException("Meta API connection timeout"))
                .when(whatsAppApiClient).sendTextMessage(anyString(), anyString());

        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919812345678")
                .recipientName("Aunt Shobha")
                .build();

        // Must not throw an unhandled exception
        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendShareInvitation(
                brideUser.getId(), board.getId(), request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("WhatsApp service unavailable"));
        assertNotNull(response.getShareUrl());

        // Web board remains healthy and queryable
        TrousseauBoardResponse boardCheck = trousseauService.getBoardById(brideUser.getId(), board.getId());
        assertNotNull(boardCheck);
        assertEquals("ACTIVE", boardCheck.getStatus());
    }

    @Test
    @DisplayName("Stage 8 Test 10: Collaborator invite status updates from PENDING to SENT after WhatsApp dispatch")
    void testCollaboratorInviteStatusUpdatesToSent() {
        assertEquals("PENDING", collaborator.getInviteStatus());

        // Send invite to collaborator via dedicated method
        TrousseauWhatsAppShareResponse response = trousseauWhatsAppService.sendCollaboratorInvitation(
                brideUser.getId(), board.getId(), collaborator.getId());

        assertTrue(response.isSuccess());

        // Verify status in DB
        TrousseauCollaborator updated = collaboratorRepository.findById(collaborator.getId()).orElseThrow();
        assertEquals("SENT", updated.getInviteStatus());
    }

    @Test
    @DisplayName("Stage 8 Test 11: Real-time vote alert dispatched when owner is opted in, suppressed when opted out")
    void testVoteAlertOptInAndOptOut() {
        // Case A: Owner opted in -> Notification is dispatched
        CastVoteRequest voteRequest = CastVoteRequest.builder()
                .voterName("Cousin Priya")
                .reaction("LOVE")
                .note("Stunning drape!")
                .build();

        trousseauService.castVote(board.getShareToken(), item1.getId(), voteRequest);

        verify(whatsAppApiClient, atLeastOnce()).sendTextMessage(eq("+919876543210"), contains("Cousin Priya"));

        // Case B: Owner opts out -> Notification is suppressed
        reset(whatsAppApiClient);
        brideUser.setWhatsappOptIn(false);
        userRepository.save(brideUser);

        CastVoteRequest voteRequest2 = CastVoteRequest.builder()
                .voterName("Uncle Ramesh")
                .reaction("LIKE")
                .note("Looks good.")
                .build();

        trousseauService.castVote(board.getShareToken(), item1.getId(), voteRequest2);

        verify(whatsAppApiClient, never()).sendTextMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Stage 8 Test 12: Payload audit confirms zero sensitive credentials, passwords, or JWTs leaked")
    void testPayloadAuditForSensitiveInformation() {
        ShareTrousseauWhatsAppRequest request = ShareTrousseauWhatsAppRequest.builder()
                .recipientPhone("+919812345678")
                .recipientName("Aunt Shobha")
                .build();

        trousseauWhatsAppService.sendShareInvitation(brideUser.getId(), board.getId(), request);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppApiClient).sendTextMessage(anyString(), messageCaptor.capture());

        String message = messageCaptor.getValue();

        // Invariance assertions: No sensitive data exposed
        assertFalse(message.contains("Password@123"), "Password must never appear in WhatsApp message");
        assertFalse(message.contains("Bearer"), "JWT tokens must never appear in WhatsApp message");
        assertFalse(message.contains("eyJ"), "JWT format must never appear in WhatsApp message");
        assertFalse(message.contains("@example.com"), "User email must not be exposed in WhatsApp share");
        assertTrue(message.contains("https://sareekart.com/trousseau/share/"), "Public share token URL must be present");
    }
}
