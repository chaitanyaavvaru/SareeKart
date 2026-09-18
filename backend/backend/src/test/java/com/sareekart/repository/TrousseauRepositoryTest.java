package com.sareekart.repository;

import com.sareekart.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TrousseauRepositoryTest {

    @Autowired
    private TrousseauBoardRepository boardRepository;

    @Autowired
    private TrousseauCeremonyRepository ceremonyRepository;

    @Autowired
    private TrousseauItemRepository itemRepository;

    @Autowired
    private TrousseauCollaboratorRepository collaboratorRepository;

    @Autowired
    private TrousseauVoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .firstName("Pooja")
                .lastName("Sharma")
                .email("pooja." + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Kanchipuram Brocade Silk Saree")
                .description("Pure mulberry silk with gold zari")
                .price(new BigDecimal("45000.00"))
                .stockQuantity(5)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Stage 2 Test 1: Save TrousseauBoard and retrieve by shareToken and userId")
    void testCreateAndFindBoard() {
        String token = "tkn_" + UUID.randomUUID().toString().replace("-", "");
        TrousseauBoard board = TrousseauBoard.builder()
                .user(testUser)
                .title("Pooja & Vikram's Wedding Trousseau")
                .weddingDate(LocalDate.now().plusMonths(3))
                .notes("South Indian wedding ceremonies")
                .shareToken(token)
                .isPublicVoting(true)
                .status("ACTIVE")
                .build();

        TrousseauBoard saved = boardRepository.save(board);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<TrousseauBoard> byToken = boardRepository.findByShareToken(token);
        assertTrue(byToken.isPresent());
        assertEquals("Pooja & Vikram's Wedding Trousseau", byToken.get().getTitle());
        assertEquals(testUser.getId(), byToken.get().getUser().getId());

        List<TrousseauBoard> userBoards = boardRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertEquals(1, userBoards.size());
    }

    @Test
    @DisplayName("Stage 2 Test 2: Verify share_token uniqueness constraint")
    void testUniqueShareTokenConstraint() {
        String duplicateToken = "fixed_token_abc123";
        TrousseauBoard board1 = TrousseauBoard.builder()
                .user(testUser)
                .title("Board One")
                .shareToken(duplicateToken)
                .build();
        boardRepository.saveAndFlush(board1);

        TrousseauBoard board2 = TrousseauBoard.builder()
                .user(testUser)
                .title("Board Two")
                .shareToken(duplicateToken)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            boardRepository.saveAndFlush(board2);
        });
    }

    @Test
    @DisplayName("Stage 2 Test 3: Add ceremonies to board and verify displayOrder sorting")
    void testCeremoniesOrdered() {
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Ceremony Ordering Board")
                .shareToken("tkn_" + UUID.randomUUID())
                .build());

        TrousseauCeremony c2 = ceremonyRepository.save(TrousseauCeremony.builder()
                .board(board)
                .ceremonyType("SANGEET")
                .title("Sangeet Gala")
                .targetBudget(new BigDecimal("50000.00"))
                .displayOrder(2)
                .build());

        TrousseauCeremony c1 = ceremonyRepository.save(TrousseauCeremony.builder()
                .board(board)
                .ceremonyType("MUHURTHAM")
                .title("Morning Muhurtham")
                .colorTheme("Crimson Red")
                .targetBudget(new BigDecimal("120000.00"))
                .displayOrder(1)
                .build());

        List<TrousseauCeremony> ordered = ceremonyRepository.findByBoardIdOrderByDisplayOrderAsc(board.getId());
        assertEquals(2, ordered.size());
        assertEquals("MUHURTHAM", ordered.get(0).getCeremonyType());
        assertEquals("SANGEET", ordered.get(1).getCeremonyType());
    }

    @Test
    @DisplayName("Stage 2 Test 4: Pin Product to Ceremony as TrousseauItem and verify integrity")
    void testTrousseauItemMapping() {
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Bridal Wardrobe")
                .shareToken("tkn_" + UUID.randomUUID())
                .build());

        TrousseauCeremony ceremony = ceremonyRepository.save(TrousseauCeremony.builder()
                .board(board)
                .ceremonyType("MUHURTHAM")
                .title("Muhurtham Ceremony")
                .displayOrder(1)
                .build());

        TrousseauItem item = itemRepository.save(TrousseauItem.builder()
                .board(board)
                .ceremony(ceremony)
                .product(testProduct)
                .addedByUser(testUser)
                .isAiRecommended(false)
                .notes("Traditional temple border saree for muhurtham")
                .status("SHORTLISTED")
                .build());

        assertNotNull(item.getId());
        assertEquals("SHORTLISTED", item.getStatus());
        assertEquals(testProduct.getId(), item.getProduct().getId());
        assertEquals(ceremony.getId(), item.getCeremony().getId());
        assertEquals(board.getId(), item.getBoard().getId());

        assertTrue(itemRepository.existsByCeremonyIdAndProductId(ceremony.getId(), testProduct.getId()));
        assertEquals(1, itemRepository.countByCeremonyId(ceremony.getId()));
    }

    @Test
    @DisplayName("Stage 2 Test 5: Collaborator invitations and status tracking")
    void testTrousseauCollaboratorMapping() {
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Family Board")
                .shareToken("tkn_" + UUID.randomUUID())
                .build());

        TrousseauCollaborator collab = collaboratorRepository.save(TrousseauCollaborator.builder()
                .board(board)
                .name("Ananya Sister")
                .phone("+919876543210")
                .email("ananya@example.com")
                .role("CO_CURATOR")
                .inviteStatus("ACCEPTED")
                .build());

        assertNotNull(collab.getId());
        Optional<TrousseauCollaborator> found = collaboratorRepository.findByBoardIdAndPhone(board.getId(), "+919876543210");
        assertTrue(found.isPresent());
        assertEquals("CO_CURATOR", found.get().getRole());
        assertEquals("ACCEPTED", found.get().getInviteStatus());
    }

    @Test
    @DisplayName("Stage 2 Test 6: Multi-party voting and reaction aggregation")
    void testTrousseauVotesAndReactions() {
        TrousseauBoard board = boardRepository.save(TrousseauBoard.builder()
                .user(testUser)
                .title("Voting Board")
                .shareToken("tkn_" + UUID.randomUUID())
                .build());

        TrousseauCeremony ceremony = ceremonyRepository.save(TrousseauCeremony.builder()
                .board(board)
                .ceremonyType("RECEPTION")
                .title("Evening Reception")
                .displayOrder(1)
                .build());

        TrousseauItem item = itemRepository.save(TrousseauItem.builder()
                .board(board)
                .ceremony(ceremony)
                .product(testProduct)
                .status("SHORTLISTED")
                .build());

        voteRepository.save(TrousseauVote.builder()
                .item(item)
                .voterName("Aunt Malini")
                .reaction("LOVE")
                .note("Pure gold zari is divine!")
                .build());

        voteRepository.save(TrousseauVote.builder()
                .item(item)
                .voterName("Cousin Rohit")
                .reaction("LOVE")
                .note("Looks regal.")
                .build());

        voteRepository.save(TrousseauVote.builder()
                .item(item)
                .voterName("Priya")
                .reaction("LIKE")
                .note("Good choice")
                .build());

        assertEquals(3, voteRepository.countByItemId(item.getId()));
        assertEquals(2, voteRepository.countByItemIdAndReaction(item.getId(), "LOVE"));
        assertEquals(1, voteRepository.countByItemIdAndReaction(item.getId(), "LIKE"));
        assertEquals(0, voteRepository.countByItemIdAndReaction(item.getId(), "PASS"));

        List<TrousseauVote> votes = voteRepository.findByItemIdOrderByCreatedAtDesc(item.getId());
        assertEquals(3, votes.size());
    }

    @Test
    @DisplayName("Stage 2 Test 7: Cascade isolation: deleting board cascades ceremonies and items without deleting product or user")
    void testCascadeIsolation() {
        TrousseauBoard board = TrousseauBoard.builder()
                .user(testUser)
                .title("Cascade Test Board")
                .shareToken("tkn_" + UUID.randomUUID())
                .build();

        TrousseauCeremony ceremony = TrousseauCeremony.builder()
                .ceremonyType("ENGAGEMENT")
                .title("Ring Ceremony")
                .displayOrder(1)
                .build();

        TrousseauItem item = TrousseauItem.builder()
                .product(testProduct)
                .status("SHORTLISTED")
                .build();

        TrousseauVote vote = TrousseauVote.builder()
                .voterName("Guest")
                .reaction("LOVE")
                .build();

        // Build aggregate hierarchy
        board.addCeremony(ceremony);
        ceremony.addItem(item);
        item.addVote(vote);

        TrousseauBoard savedBoard = boardRepository.saveAndFlush(board);

        Long boardId = savedBoard.getId();
        Long ceremonyId = savedBoard.getCeremonies().get(0).getId();
        Long itemId = savedBoard.getCeremonies().get(0).getItems().get(0).getId();
        Long productId = testProduct.getId();
        Long userId = testUser.getId();

        // Delete board
        boardRepository.delete(savedBoard);
        boardRepository.flush();

        // Ceremonies, items, and votes are cascade-deleted with the board
        assertFalse(boardRepository.findById(boardId).isPresent());
        assertFalse(ceremonyRepository.findById(ceremonyId).isPresent());
        assertFalse(itemRepository.findById(itemId).isPresent());

        // Product and User are completely preserved (Core Commerce Isolation)
        assertTrue(productRepository.findById(productId).isPresent(), "Catalog Product must NOT be deleted when a trousseau is deleted");
        assertTrue(userRepository.findById(userId).isPresent(), "User account must NOT be deleted when a trousseau is deleted");
    }
}
