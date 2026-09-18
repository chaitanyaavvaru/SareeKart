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
public class TrousseauLifecycleServiceTest {

    @Autowired
    private TrousseauService trousseauService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User brideUser;
    private User intruderUser;
    private Product kanchipuramSaree;
    private Product banarasiSaree;
    private Product outOfStockSaree;
    private Product inactiveSaree;

    @BeforeEach
    void setUp() {
        brideUser = userRepository.save(User.builder()
                .firstName("Kavya")
                .lastName("Reddy")
                .email("kavya." + UUID.randomUUID() + "@example.com")
                .password("securePass123")
                .role(Role.CUSTOMER)
                .build());

        intruderUser = userRepository.save(User.builder()
                .firstName("Intruder")
                .lastName("Sneak")
                .email("intruder." + UUID.randomUUID() + "@example.com")
                .password("securePass123")
                .role(Role.CUSTOMER)
                .build());

        kanchipuramSaree = productRepository.save(Product.builder()
                .name("Bridal Kanchipuram Brocade Saree")
                .description("Authentic mulberry silk with gold zari")
                .price(new BigDecimal("65000.00"))
                .stockQuantity(10)
                .active(true)
                .build());

        banarasiSaree = productRepository.save(Product.builder()
                .name("Royal Banarasi Katan Silk")
                .description("Kadwa weave gold zari")
                .price(new BigDecimal("42000.00"))
                .stockQuantity(5)
                .active(true)
                .build());

        outOfStockSaree = productRepository.save(Product.builder()
                .name("Sold Out Silk Saree")
                .description("Zero stock")
                .price(new BigDecimal("30000.00"))
                .stockQuantity(0)
                .active(true)
                .build());

        inactiveSaree = productRepository.save(Product.builder()
                .name("Delisted Handloom Saree")
                .description("Deactivated from catalog")
                .price(new BigDecimal("25000.00"))
                .stockQuantity(8)
                .active(false)
                .build());
    }

    @Test
    @DisplayName("Stage 4 Test 1: Full board and ceremony lifecycle with budget aggregation")
    void testBoardAndCeremonyLifecycle() {
        CreateCeremonyRequest c1 = CreateCeremonyRequest.builder()
                .ceremonyType("MUHURTHAM")
                .title("Morning Muhurtham")
                .colorTheme("Crimson & Gold")
                .targetBudget(new BigDecimal("100000.00"))
                .displayOrder(1)
                .build();

        CreateCeremonyRequest c2 = CreateCeremonyRequest.builder()
                .ceremonyType("RECEPTION")
                .title("Evening Reception")
                .colorTheme("Pastel Peach")
                .targetBudget(new BigDecimal("80000.00"))
                .displayOrder(2)
                .build();

        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Kavya & Arjun Wedding Trousseau")
                .weddingDate(LocalDate.now().plusMonths(4))
                .notes("Curating heirloom sarees across South Indian ceremonies")
                .isPublicVoting(true)
                .ceremonies(List.of(c1, c2))
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);
        assertNotNull(board.getId());
        assertEquals("Kavya & Arjun Wedding Trousseau", board.getTitle());
        assertEquals(2, board.getTotalCeremonies());
        assertEquals(new BigDecimal("180000.00"), board.getTotalBudget());
        assertNotNull(board.getShareToken());

        // Update board
        UpdateTrousseauBoardRequest updateReq = UpdateTrousseauBoardRequest.builder()
                .title("Kavya & Arjun Royal Wedding")
                .weddingDate(LocalDate.now().plusMonths(5))
                .notes("Updated notes with destination theme")
                .status("ACTIVE")
                .build();

        TrousseauBoardResponse updatedBoard = trousseauService.updateBoard(brideUser.getId(), board.getId(), updateReq);
        assertEquals("Kavya & Arjun Royal Wedding", updatedBoard.getTitle());

        // Add a 3rd ceremony
        CreateCeremonyRequest c3 = CreateCeremonyRequest.builder()
                .ceremonyType("SANGEET")
                .title("Sangeet Gala")
                .colorTheme("Emerald Green")
                .targetBudget(new BigDecimal("50000.00"))
                .displayOrder(3)
                .build();

        TrousseauCeremonyResponse sangeet = trousseauService.addCeremony(brideUser.getId(), board.getId(), c3);
        assertNotNull(sangeet.getId());
        assertEquals("SANGEET", sangeet.getCeremonyType());

        // Verify total ceremonies increased
        TrousseauBoardResponse refreshed = trousseauService.getBoardById(brideUser.getId(), board.getId());
        assertEquals(3, refreshed.getTotalCeremonies());
        assertEquals(new BigDecimal("230000.00"), refreshed.getTotalBudget());

        // Delete the 3rd ceremony
        trousseauService.deleteCeremony(brideUser.getId(), board.getId(), sangeet.getId());
        TrousseauBoardResponse afterDelete = trousseauService.getBoardById(brideUser.getId(), board.getId());
        assertEquals(2, afterDelete.getTotalCeremonies());
    }

    @Test
    @DisplayName("Stage 4 Test 2: Board ownership enforcement — unauthorized user receives AccessDeniedException (403)")
    void testBoardOwnershipEnforcement() {
        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Private Bridal Board")
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);

        // Intruder attempts to view private board
        assertThrows(AccessDeniedException.class, () -> {
            trousseauService.getBoardById(intruderUser.getId(), board.getId());
        });

        // Intruder attempts to update board
        UpdateTrousseauBoardRequest updateReq = UpdateTrousseauBoardRequest.builder()
                .title("Hacked Title")
                .build();
        assertThrows(AccessDeniedException.class, () -> {
            trousseauService.updateBoard(intruderUser.getId(), board.getId(), updateReq);
        });

        // Intruder attempts to delete board
        assertThrows(AccessDeniedException.class, () -> {
            trousseauService.deleteBoard(intruderUser.getId(), board.getId());
        });

        // Intruder attempts to add ceremony
        CreateCeremonyRequest cReq = CreateCeremonyRequest.builder()
                .ceremonyType("HALDI")
                .title("Haldi")
                .build();
        assertThrows(AccessDeniedException.class, () -> {
            trousseauService.addCeremony(intruderUser.getId(), board.getId(), cReq);
        });
    }

    @Test
    @DisplayName("Stage 4 Test 3: Product validation & inventory isolation — adding item does NOT alter stock")
    void testProductValidationAndInventoryIsolation() {
        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Bridal Curation")
                .ceremonies(List.of(CreateCeremonyRequest.builder()
                        .ceremonyType("MUHURTHAM")
                        .title("Muhurtham")
                        .targetBudget(new BigDecimal("100000.00"))
                        .build()))
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);
        Long ceremonyId = board.getCeremonies().get(0).getId();

        int initialStock = kanchipuramSaree.getStockQuantity();

        // 1. Add valid in-stock product
        AddCeremonyItemRequest itemReq = AddCeremonyItemRequest.builder()
                .productId(kanchipuramSaree.getId())
                .notes("Heirloom brocade for muhurtham")
                .isAiRecommended(false)
                .build();

        TrousseauItemResponse itemResponse = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, itemReq);
        assertNotNull(itemResponse.getId());
        assertEquals("SHORTLISTED", itemResponse.getStatus());
        assertEquals("Bridal Kanchipuram Brocade Saree", itemResponse.getProductName());
        assertEquals(new BigDecimal("65000.00"), itemResponse.getProductPrice());

        // CRITICAL INVENTORY ISOLATION CHECK: Stock must NOT be decremented!
        Product productAfterAdd = productRepository.findById(kanchipuramSaree.getId()).orElseThrow();
        assertEquals(initialStock, productAfterAdd.getStockQuantity(), "Inventory must remain 100% untouched when adding to trousseau board");

        // Verify allocated spend in ceremony and board
        TrousseauBoardResponse refreshed = trousseauService.getBoardById(brideUser.getId(), board.getId());
        assertEquals(new BigDecimal("65000.00"), refreshed.getTotalAllocatedSpend());
        assertEquals(1, refreshed.getTotalItemsCount());

        // Remove item from ceremony
        trousseauService.removeItemFromCeremony(brideUser.getId(), board.getId(), ceremonyId, itemResponse.getId());

        // Inventory must still be exactly initialStock
        Product productAfterRemove = productRepository.findById(kanchipuramSaree.getId()).orElseThrow();
        assertEquals(initialStock, productAfterRemove.getStockQuantity(), "Inventory must remain untouched after removing from trousseau board");

        TrousseauBoardResponse emptyRefreshed = trousseauService.getBoardById(brideUser.getId(), board.getId());
        assertEquals(0, emptyRefreshed.getTotalItemsCount());
        assertEquals(BigDecimal.ZERO, emptyRefreshed.getTotalAllocatedSpend());
    }

    @Test
    @DisplayName("Stage 4 Test 4: Duplicate product protection in same ceremony")
    void testDuplicateItemProtection() {
        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Duplicate Test Board")
                .ceremonies(List.of(CreateCeremonyRequest.builder()
                        .ceremonyType("MUHURTHAM")
                        .title("Muhurtham")
                        .build()))
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);
        Long ceremonyId = board.getCeremonies().get(0).getId();

        AddCeremonyItemRequest itemReq = AddCeremonyItemRequest.builder()
                .productId(kanchipuramSaree.getId())
                .build();

        trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, itemReq);

        // Attempting to add the exact same product again to the same ceremony must be rejected
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, itemReq);
        });
        assertTrue(ex.getMessage().contains("already been shortlisted"));
    }

    @Test
    @DisplayName("Stage 4 Test 5: Reject nonexistent, out-of-stock, and inactive products")
    void testInvalidProductHandling() {
        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Product Validation Board")
                .ceremonies(List.of(CreateCeremonyRequest.builder()
                        .ceremonyType("RECEPTION")
                        .title("Reception")
                        .build()))
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);
        Long ceremonyId = board.getCeremonies().get(0).getId();

        // 1. Nonexistent product ID (e.g. 999999)
        AddCeremonyItemRequest nonExistentReq = AddCeremonyItemRequest.builder()
                .productId(999999L)
                .build();
        assertThrows(ResourceNotFoundException.class, () -> {
            trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, nonExistentReq);
        });

        // 2. Out of stock product
        AddCeremonyItemRequest oosReq = AddCeremonyItemRequest.builder()
                .productId(outOfStockSaree.getId())
                .build();
        BadRequestException oosEx = assertThrows(BadRequestException.class, () -> {
            trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, oosReq);
        });
        assertTrue(oosEx.getMessage().contains("out of stock"));

        // 3. Inactive product
        AddCeremonyItemRequest inactiveReq = AddCeremonyItemRequest.builder()
                .productId(inactiveSaree.getId())
                .build();
        BadRequestException inactiveEx = assertThrows(BadRequestException.class, () -> {
            trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId, inactiveReq);
        });
        assertTrue(inactiveEx.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("Stage 4 Test 6: Update ceremony item status and notes")
    void testUpdateCeremonyItem() {
        CreateTrousseauBoardRequest createReq = CreateTrousseauBoardRequest.builder()
                .title("Status Update Board")
                .ceremonies(List.of(CreateCeremonyRequest.builder()
                        .ceremonyType("MUHURTHAM")
                        .title("Muhurtham")
                        .build()))
                .build();

        TrousseauBoardResponse board = trousseauService.createBoard(brideUser.getId(), createReq);
        Long ceremonyId = board.getCeremonies().get(0).getId();

        TrousseauItemResponse item = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), ceremonyId,
                AddCeremonyItemRequest.builder().productId(kanchipuramSaree.getId()).build());

        assertEquals("SHORTLISTED", item.getStatus());

        UpdateCeremonyItemRequest updateReq = UpdateCeremonyItemRequest.builder()
                .status("SELECTED")
                .notes("Family approved this saree after voting")
                .build();

        TrousseauItemResponse updated = trousseauService.updateCeremonyItem(brideUser.getId(), board.getId(), ceremonyId, item.getId(), updateReq);
        assertEquals("SELECTED", updated.getStatus());
        assertEquals("Family approved this saree after voting", updated.getNotes());
    }
}
