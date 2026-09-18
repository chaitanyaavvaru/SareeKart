package com.sareekart.service;

import com.sareekart.dto.response.CartResponse;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TrousseauCartConversionIntegrationTest {

    @Autowired
    private TrousseauService trousseauService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User brideUser;
    private User intruderUser;
    private Product kanchipuramRed;
    private Product banarasiGold;
    private Product chanderiYellow;

    private TrousseauBoardResponse board;
    private TrousseauCeremonyResponse muhurthamCeremony;
    private TrousseauCeremonyResponse haldiCeremony;
    private TrousseauItemResponse muhurthamItem1;
    private TrousseauItemResponse muhurthamItem2;
    private TrousseauItemResponse haldiItem;

    @BeforeEach
    void setUp() {
        brideUser = userRepository.save(User.builder()
                .firstName("Sita")
                .lastName("Raman")
                .email("sita." + UUID.randomUUID() + "@example.com")
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

        // Products
        kanchipuramRed = productRepository.save(Product.builder()
                .name("Bridal Crimson Kanchipuram Brocade")
                .description("Authentic mulberry silk")
                .fabric("Kanchipuram Silk")
                .color("Crimson Red")
                .price(new BigDecimal("75000.00"))
                .stockQuantity(10)
                .active(true)
                .build());

        banarasiGold = productRepository.save(Product.builder()
                .name("Heirloom Gold Banarasi Katan")
                .description("Kadwa zari weave")
                .fabric("Banarasi Silk")
                .color("Gold")
                .price(new BigDecimal("55000.00"))
                .stockQuantity(6)
                .active(true)
                .build());

        chanderiYellow = productRepository.save(Product.builder()
                .name("Mustard Floral Chanderi")
                .description("Haldi morning drape")
                .fabric("Chanderi")
                .color("Yellow")
                .price(new BigDecimal("22000.00"))
                .stockQuantity(8)
                .active(true)
                .build());

        // Board & Ceremonies
        board = trousseauService.createBoard(brideUser.getId(), CreateTrousseauBoardRequest.builder()
                .title("Sita's Royal Trousseau")
                .weddingDate(LocalDate.now().plusMonths(6))
                .build());

        muhurthamCeremony = trousseauService.addCeremony(brideUser.getId(), board.getId(), CreateCeremonyRequest.builder()
                .ceremonyType("MUHURTHAM")
                .title("Muhurtham Main")
                .targetBudget(new BigDecimal("150000.00"))
                .displayOrder(1)
                .build());

        haldiCeremony = trousseauService.addCeremony(brideUser.getId(), board.getId(), CreateCeremonyRequest.builder()
                .ceremonyType("HALDI")
                .title("Haldi Morning")
                .targetBudget(new BigDecimal("30000.00"))
                .displayOrder(2)
                .build());

        // Shortlist items
        muhurthamItem1 = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), muhurthamCeremony.getId(),
                AddCeremonyItemRequest.builder().productId(kanchipuramRed.getId()).notes("Primary Muhurtham").build());

        muhurthamItem2 = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), muhurthamCeremony.getId(),
                AddCeremonyItemRequest.builder().productId(banarasiGold.getId()).notes("Muhurtham Alternate").build());

        haldiItem = trousseauService.addItemToCeremony(brideUser.getId(), board.getId(), haldiCeremony.getId(),
                AddCeremonyItemRequest.builder().productId(chanderiYellow.getId()).notes("Haldi Drape").build());
    }

    // =========================================================================
    // Stage 7 Conversion Tests
    // =========================================================================

    @Test
    @DisplayName("Stage 7 Test 1: Successful single-item conversion transfers to cart safely")
    void testSingleItemConversionSuccess() {
        TransferToCartResponse response = trousseauService.transferItemToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId(), muhurthamItem1.getId());

        assertNotNull(response);
        assertEquals(1, response.getAddedCount());
        assertEquals(0, response.getAlreadyInCartCount());
        assertEquals(0, response.getOutOfStockCount());
        assertTrue(response.getAddedProductIds().contains(kanchipuramRed.getId()));

        // Verify item in user's Cart via CartService
        CartResponse cart = cartService.getCart(brideUser.getId());
        assertNotNull(cart);
        assertEquals(1, cart.getTotalItems());
        assertEquals(kanchipuramRed.getId(), cart.getItems().get(0).getProductId());
        assertEquals(1, cart.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Stage 7 Test 2: Successful multi-item ceremony conversion transfers all valid items")
    void testMultiItemCeremonyConversionSuccess() {
        TransferToCartResponse response = trousseauService.transferCeremonyToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId());

        assertNotNull(response);
        assertEquals(2, response.getAddedCount());
        assertEquals(0, response.getAlreadyInCartCount());
        assertEquals(0, response.getOutOfStockCount());
        assertTrue(response.getAddedProductIds().contains(kanchipuramRed.getId()));
        assertTrue(response.getAddedProductIds().contains(banarasiGold.getId()));

        // Verify cart
        CartResponse cart = cartService.getCart(brideUser.getId());
        assertEquals(2, cart.getTotalItems());
        assertEquals(new BigDecimal("130000.00"), cart.getTotalPrice());
    }

    @Test
    @DisplayName("Stage 7 Test 3: Non-owner receives 403 AccessDeniedException")
    void testNonOwnerCannotTransferToCart() {
        assertThrows(AccessDeniedException.class, () ->
                trousseauService.transferCeremonyToCart(intruderUser.getId(), board.getId(), muhurthamCeremony.getId()));

        assertThrows(AccessDeniedException.class, () ->
                trousseauService.transferItemToCart(intruderUser.getId(), board.getId(), muhurthamCeremony.getId(), muhurthamItem1.getId()));

        assertThrows(AccessDeniedException.class, () ->
                trousseauService.transferAllToCart(intruderUser.getId(), board.getId()));
    }

    @Test
    @DisplayName("Stage 7 Test 4: Nonexistent board or ceremony returns 404 ResourceNotFoundException")
    void testNonExistentEntitiesReturn404() {
        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.transferCeremonyToCart(brideUser.getId(), 99999L, muhurthamCeremony.getId()));

        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.transferCeremonyToCart(brideUser.getId(), board.getId(), 99999L));

        assertThrows(ResourceNotFoundException.class, () ->
                trousseauService.transferItemToCart(brideUser.getId(), board.getId(), muhurthamCeremony.getId(), 99999L));
    }

    @Test
    @DisplayName("Stage 7 Test 5: Product deactivated after curation is safely rejected during conversion")
    void testDeactivatedProductRejectedAtConversionTime() {
        // Deactivate kanchipuramRed after curation
        kanchipuramRed.setActive(false);
        productRepository.save(kanchipuramRed);

        TransferToCartResponse response = trousseauService.transferCeremonyToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId());

        assertNotNull(response);
        assertEquals(1, response.getAddedCount()); // banarasiGold added
        assertEquals(1, response.getOutOfStockCount()); // kanchipuramRed unavailable
        assertTrue(response.getUnavailableProductIds().contains(kanchipuramRed.getId()));
        assertTrue(response.getAddedProductIds().contains(banarasiGold.getId()));

        // Cart contains only the valid item
        CartResponse cart = cartService.getCart(brideUser.getId());
        assertEquals(1, cart.getTotalItems());
        assertEquals(banarasiGold.getId(), cart.getItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Stage 7 Test 6: Product becoming out-of-stock after curation is safely handled")
    void testOutOfStockProductHandledAtConversionTime() {
        // Set stock to 0
        kanchipuramRed.setStockQuantity(0);
        productRepository.save(kanchipuramRed);

        TransferToCartResponse response = trousseauService.transferCeremonyToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId());

        assertNotNull(response);
        assertEquals(1, response.getAddedCount());
        assertEquals(1, response.getOutOfStockCount());
        assertTrue(response.getUnavailableProductIds().contains(kanchipuramRed.getId()));
        assertTrue(response.getAddedProductIds().contains(banarasiGold.getId()));
    }

    @Test
    @DisplayName("Stage 7 Test 7: Duplicate conversion requests are idempotent (prevents duplicate quantities)")
    void testDuplicateConversionIdempotency() {
        // First conversion
        TransferToCartResponse firstResponse = trousseauService.transferCeremonyToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId());
        assertEquals(2, firstResponse.getAddedCount());
        assertEquals(0, firstResponse.getAlreadyInCartCount());

        // Repeated conversion (bride clicks button again)
        TransferToCartResponse secondResponse = trousseauService.transferCeremonyToCart(
                brideUser.getId(), board.getId(), muhurthamCeremony.getId());

        assertNotNull(secondResponse);
        assertEquals(0, secondResponse.getAddedCount(), "No new items should be added on repeat conversion");
        assertEquals(2, secondResponse.getAlreadyInCartCount(), "Both items should be recognized as already in cart");
        assertTrue(secondResponse.getSkippedProductIds().contains(kanchipuramRed.getId()));
        assertTrue(secondResponse.getSkippedProductIds().contains(banarasiGold.getId()));

        // Cart quantity remains exactly 1 for each SKU (not doubled to 2)
        CartResponse cart = cartService.getCart(brideUser.getId());
        assertEquals(2, cart.getTotalItems());
        assertEquals(1, cart.getItems().get(0).getQuantity());
        assertEquals(1, cart.getItems().get(1).getQuantity());
    }

    @Test
    @DisplayName("Stage 7 Test 8: Inventory stock is NOT decremented by trousseau cart conversion")
    void testInventoryNotDecrementedByConversion() {
        int initialStock1 = kanchipuramRed.getStockQuantity();
        int initialStock2 = banarasiGold.getStockQuantity();

        trousseauService.transferCeremonyToCart(brideUser.getId(), board.getId(), muhurthamCeremony.getId());

        Product refreshed1 = productRepository.findById(kanchipuramRed.getId()).orElseThrow();
        Product refreshed2 = productRepository.findById(banarasiGold.getId()).orElseThrow();

        assertEquals(initialStock1, refreshed1.getStockQuantity(), "Stock must not be decremented during cart conversion");
        assertEquals(initialStock2, refreshed2.getStockQuantity(), "Stock must not be decremented during cart conversion");
    }

    @Test
    @DisplayName("Stage 7 Test 9: Board-wide transfer-all-to-cart transfers items across all ceremonies")
    void testTransferAllToCart() {
        TransferToCartResponse response = trousseauService.transferAllToCart(brideUser.getId(), board.getId());

        assertNotNull(response);
        assertEquals(3, response.getAddedCount()); // 2 from muhurtham + 1 from haldi
        assertEquals(0, response.getAlreadyInCartCount());
        assertEquals(0, response.getOutOfStockCount());

        CartResponse cart = cartService.getCart(brideUser.getId());
        assertEquals(3, cart.getTotalItems());
        assertEquals(new BigDecimal("152000.00"), cart.getTotalPrice());
    }

    @Test
    @DisplayName("Stage 7 Test 10: Inactive/Archived board rejects cart conversion with 400 BadRequestException")
    void testArchivedBoardRejectsConversion() {
        trousseauService.updateBoard(brideUser.getId(), board.getId(), UpdateTrousseauBoardRequest.builder()
                .status("ARCHIVED")
                .build());

        assertThrows(BadRequestException.class, () ->
                trousseauService.transferCeremonyToCart(brideUser.getId(), board.getId(), muhurthamCeremony.getId()));
    }
}
