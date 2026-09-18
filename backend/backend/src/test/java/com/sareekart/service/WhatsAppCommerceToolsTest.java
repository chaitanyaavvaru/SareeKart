package com.sareekart.service;

import com.sareekart.dto.response.*;
import com.sareekart.entity.*;
import com.sareekart.repository.ConversationRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.whatsapp.WhatsAppCommerceTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppCommerceToolsTest {

    @Mock
    private StylistGroundingService stylistGroundingService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private AiStylistService aiStylistService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WhatsAppCommerceTools commerceTools;

    private Product saree;

    @BeforeEach
    void setUp() {
        saree = Product.builder()
                .id(10L)
                .name("Kanchipuram Crimson Royal Silk")
                .price(new BigDecimal("45000.00"))
                .fabric("Silk")
                .color("Crimson Red")
                .stockQuantity(5)
                .active(true)
                .images(List.of("https://sareekart.com/img/saree1.jpg"))
                .build();
    }

    @Test
    @DisplayName("searchCatalog returns matched products within price limit")
    void testSearchCatalog() {
        ProductResponse productResp = ProductResponse.builder()
                .id(10L)
                .name("Kanchipuram Crimson Royal Silk")
                .price(new BigDecimal("45000.00"))
                .fabric("Silk")
                .color("Crimson Red")
                .stockQuantity(5)
                .images(List.of("https://sareekart.com/img/saree1.jpg"))
                .build();

        ScoredProductResponse scored = ScoredProductResponse.builder()
                .product(productResp)
                .recommendationScore(0.95)
                .build();

        when(stylistGroundingService.retrieveGroundedCandidates(any(), any(), any(), anyInt()))
                .thenReturn(List.of(scored));

        WhatsAppCommerceTools.CatalogSearchResult result = commerceTools.searchCatalog(
                new WhatsAppCommerceTools.CatalogSearchInput("Silk", "Silk", "Crimson Red", "WEDDING", new BigDecimal("50000"))
        );

        assertThat(result.totalFound()).isEqualTo(1);
        assertThat(result.items().get(0).name()).isEqualTo("Kanchipuram Crimson Royal Silk");
        assertThat(result.items().get(0).price()).isEqualByComparingTo("45000.00");
    }

    @Test
    @DisplayName("checkAvailability accurately checks active stock")
    void testCheckAvailability_InStock() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(saree));

        WhatsAppCommerceTools.StockCheckResult result = commerceTools.checkAvailability(
                new WhatsAppCommerceTools.StockCheckInput(10L)
        );

        assertThat(result.inStock()).isTrue();
        assertThat(result.availableQuantity()).isEqualTo(5);
        assertThat(result.name()).isEqualTo("Kanchipuram Crimson Royal Silk");
    }

    @Test
    @DisplayName("manageCart requires valid authenticated userId")
    void testManageCart_Unauthenticated() {
        WhatsAppCommerceTools.CartActionResult result = commerceTools.manageCart(
                new WhatsAppCommerceTools.CartActionInput("VIEW", null, null, null)
        );

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("link your SareeKart account");
    }

    @Test
    @DisplayName("manageCart adds item to cart successfully")
    void testManageCart_AddSuccess() {
        CartResponse mockCart = CartResponse.builder()
                .items(List.of(CartItemResponse.builder().id(1L).productId(10L).quantity(1).build()))
                .totalPrice(new BigDecimal("45000.00"))
                .build();

        when(cartService.addItemToCart(eq(100L), any())).thenReturn(mockCart);

        WhatsAppCommerceTools.CartActionResult result = commerceTools.manageCart(
                new WhatsAppCommerceTools.CartActionInput("ADD", 10L, 1, 100L)
        );

        assertThat(result.success()).isTrue();
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("45000.00");
    }

    @Test
    @DisplayName("trackOrder retrieves order milestones")
    void testTrackOrder_Success() {
        OrderResponse order = OrderResponse.builder()
                .id(777L)
                .status(OrderStatus.SHIPPED.name())
                .courierPartner("Blue Dart Apex Air")
                .trackingNumber("BD-998877")
                .estimatedDeliveryDate("Friday, 18 Sep")
                .build();

        when(orderService.getOrderById(777L, 100L)).thenReturn(order);

        WhatsAppCommerceTools.OrderTrackingResult result = commerceTools.trackOrder(
                new WhatsAppCommerceTools.OrderTrackingInput(777L, null, 100L, "+919876543210")
        );

        assertThat(result.found()).isTrue();
        assertThat(result.orderStatus()).isEqualTo("SHIPPED");
        assertThat(result.courierPartner()).isEqualTo("Blue Dart Apex Air");
        assertThat(result.trackingNumber()).isEqualTo("BD-998877");
    }

    @Test
    @DisplayName("escalateToHuman sets status to HUMAN_ESCALATION, sets ESCALATED_HUMAN_REQUEST tag, and broadcasts")
    void testEscalateToHuman_Success() {
        WhatsAppContact contact = WhatsAppContact.builder().id(1L).phoneNumber("9876543210").build();
        Conversation conv = Conversation.builder().id(42L).contact(contact).status(ConversationStatus.BOT_HANDLING).build();

        when(conversationRepository.findById(42L)).thenReturn(Optional.of(conv));

        WhatsAppCommerceTools.HumanHandoffResult result = commerceTools.escalateToHuman(
                new WhatsAppCommerceTools.HumanHandoffInput(42L, "I want to talk to a human stylist")
        );

        assertThat(result.escalated()).isTrue();
        assertThat(conv.getStatus()).isEqualTo(ConversationStatus.HUMAN_ESCALATION);
        assertThat(conv.getTags()).isEqualTo("ESCALATED_HUMAN_REQUEST");
        verify(conversationRepository).save(conv);
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/inbox"), any(Object.class));
    }
}
