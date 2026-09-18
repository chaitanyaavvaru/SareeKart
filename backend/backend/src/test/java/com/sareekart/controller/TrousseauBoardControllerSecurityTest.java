package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sareekart.dto.trousseau.*;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.service.TrousseauService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrousseauBoardControllerSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TrousseauService trousseauService;

    @Mock
    private com.sareekart.service.TrousseauAiCurationService trousseauAiCurationService;

    @Mock
    private com.sareekart.service.TrousseauWhatsAppService trousseauWhatsAppService;

    private MockMvc mockMvc;

    private User brideUser;
    private User intruderUser;

    @BeforeEach
    void setUp() {
        brideUser = User.builder().id(101L).email("bride@example.com").firstName("Kavya").build();
        intruderUser = User.builder().id(202L).email("intruder@example.com").firstName("Intruder").build();

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                String authHeader = webRequest.getHeader("X-Simulate-User");
                if ("intruder".equalsIgnoreCase(authHeader)) {
                    return intruderUser;
                }
                return brideUser; // Default to brideUser
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(new TrousseauBoardController(trousseauService, trousseauAiCurationService, trousseauWhatsAppService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    @DisplayName("Stage 4 Controller Test 1: POST /api/trousseau creates board and returns 201 Created")
    void testCreateBoardSuccess() throws Exception {
        CreateTrousseauBoardRequest req = CreateTrousseauBoardRequest.builder()
                .title("Kavya & Arjun Wedding Trousseau")
                .weddingDate(LocalDate.now().plusMonths(3))
                .build();

        TrousseauBoardResponse res = TrousseauBoardResponse.builder()
                .id(42L)
                .userId(101L)
                .title("Kavya & Arjun Wedding Trousseau")
                .shareToken("tkn_sample123")
                .status("ACTIVE")
                .totalCeremonies(0)
                .totalBudget(BigDecimal.ZERO)
                .build();

        when(trousseauService.createBoard(eq(101L), any(CreateTrousseauBoardRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/trousseau")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.title").value("Kavya & Arjun Wedding Trousseau"));
    }

    @Test
    @DisplayName("Stage 4 Controller Test 2: GET /api/trousseau/{boardId} returns 403 when intruder accesses another user's board")
    void testUnauthorizedBoardAccessForbidden() throws Exception {
        when(trousseauService.getBoardById(eq(202L), eq(42L)))
                .thenThrow(new AccessDeniedException("You do not have permission to access this trousseau board"));

        mockMvc.perform(get("/api/trousseau/42")
                        .header("X-Simulate-User", "intruder"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
    }

    @Test
    @DisplayName("Stage 4 Controller Test 3: POST /api/trousseau/{boardId}/ceremonies/{ceremonyId}/items returns 400 on invalid/duplicate product")
    void testAddItemBadRequest() throws Exception {
        AddCeremonyItemRequest req = AddCeremonyItemRequest.builder()
                .productId(99L)
                .build();

        when(trousseauService.addItemToCeremony(eq(101L), eq(42L), eq(5L), any(AddCeremonyItemRequest.class)))
                .thenThrow(new BadRequestException("Product is currently out of stock and cannot be added to a bridal trousseau"));

        mockMvc.perform(post("/api/trousseau/42/ceremonies/5/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product is currently out of stock and cannot be added to a bridal trousseau"));
    }

    @Test
    @DisplayName("Stage 4 Controller Test 4: DELETE /api/trousseau/{boardId} successfully returns 200 OK")
    void testDeleteBoardSuccess() throws Exception {
        doNothing().when(trousseauService).deleteBoard(101L, 42L);

        mockMvc.perform(delete("/api/trousseau/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trousseau board deleted successfully"));

        verify(trousseauService, times(1)).deleteBoard(101L, 42L);
    }

    @Test
    @DisplayName("Stage 5 Controller Test 5: POST /api/trousseau/{boardId}/collaborators returns 201 Created")
    void testInviteCollaboratorSuccess() throws Exception {
        InviteCollaboratorRequest req = InviteCollaboratorRequest.builder()
                .name("Pooja Sharma")
                .phone("+919876543210")
                .role("VOTER")
                .build();

        TrousseauCollaboratorResponse res = TrousseauCollaboratorResponse.builder()
                .id(1L)
                .boardId(42L)
                .name("Pooja Sharma")
                .phone("+919876543210")
                .role("VOTER")
                .inviteStatus("PENDING")
                .build();

        when(trousseauService.inviteCollaborator(eq(101L), eq(42L), any(InviteCollaboratorRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/collaborators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Pooja Sharma"))
                .andExpect(jsonPath("$.data.role").value("VOTER"))
                .andExpect(jsonPath("$.data.inviteStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Stage 5 Controller Test 6: GET /api/trousseau/{boardId}/collaborators returns 200 OK")
    void testGetCollaboratorsSuccess() throws Exception {
        TrousseauCollaboratorResponse c1 = TrousseauCollaboratorResponse.builder()
                .id(1L)
                .boardId(42L)
                .name("Pooja Sharma")
                .role("VOTER")
                .inviteStatus("PENDING")
                .build();

        when(trousseauService.getCollaborators(101L, 42L)).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/trousseau/42/collaborators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Pooja Sharma"));
    }

    @Test
    @DisplayName("Stage 5 Controller Test 7: PATCH /api/trousseau/{boardId}/collaborators/{collaboratorId} returns 200 OK")
    void testUpdateCollaboratorSuccess() throws Exception {
        UpdateCollaboratorRequest req = UpdateCollaboratorRequest.builder()
                .role("CO_CURATOR")
                .inviteStatus("ACCEPTED")
                .build();

        TrousseauCollaboratorResponse res = TrousseauCollaboratorResponse.builder()
                .id(1L)
                .boardId(42L)
                .name("Pooja Sharma")
                .role("CO_CURATOR")
                .inviteStatus("ACCEPTED")
                .build();

        when(trousseauService.updateCollaborator(eq(101L), eq(42L), eq(1L), any(UpdateCollaboratorRequest.class))).thenReturn(res);

        mockMvc.perform(patch("/api/trousseau/42/collaborators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("CO_CURATOR"))
                .andExpect(jsonPath("$.data.inviteStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Stage 5 Controller Test 8: DELETE /api/trousseau/{boardId}/collaborators/{collaboratorId} returns 200 OK")
    void testRemoveCollaboratorSuccess() throws Exception {
        doNothing().when(trousseauService).removeCollaborator(101L, 42L, 1L);

        mockMvc.perform(delete("/api/trousseau/42/collaborators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Collaborator removed successfully"));

        verify(trousseauService, times(1)).removeCollaborator(101L, 42L, 1L);
    }

    @Test
    @DisplayName("Stage 5 Controller Test 9: POST /api/trousseau/{boardId}/regenerate-share-token returns 200 OK with new token")
    void testRegenerateShareTokenSuccess() throws Exception {
        TrousseauBoardResponse res = TrousseauBoardResponse.builder()
                .id(42L)
                .userId(101L)
                .title("Kavya & Arjun Wedding Trousseau")
                .shareToken("tkn_new_secure_token_456")
                .shareUrl("/trousseau/shared/tkn_new_secure_token_456")
                .build();

        when(trousseauService.regenerateShareToken(101L, 42L)).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/regenerate-share-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareToken").value("tkn_new_secure_token_456"));
    }

    @Test
    @DisplayName("Stage 7 Controller Test 10: POST /api/trousseau/{boardId}/ceremonies/{ceremonyId}/transfer-to-cart returns 200 OK")
    void testTransferCeremonyToCartSuccess() throws Exception {
        TransferToCartResponse res = TransferToCartResponse.builder()
                .boardId(42L)
                .ceremonyId(10L)
                .addedCount(2)
                .alreadyInCartCount(0)
                .outOfStockCount(0)
                .message("Transfer complete: 2 items added to cart")
                .build();

        when(trousseauService.transferCeremonyToCart(101L, 42L, 10L)).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/ceremonies/10/transfer-to-cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addedCount").value(2));
    }

    @Test
    @DisplayName("Stage 7 Controller Test 11: POST /api/trousseau/{boardId}/ceremonies/{ceremonyId}/items/{itemId}/transfer-to-cart returns 200 OK")
    void testTransferItemToCartSuccess() throws Exception {
        TransferToCartResponse res = TransferToCartResponse.builder()
                .boardId(42L)
                .ceremonyId(10L)
                .addedCount(1)
                .alreadyInCartCount(0)
                .outOfStockCount(0)
                .message("Transfer complete: 1 item added to cart")
                .build();

        when(trousseauService.transferItemToCart(101L, 42L, 10L, 5L)).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/ceremonies/10/items/5/transfer-to-cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addedCount").value(1));
    }

    @Test
    @DisplayName("Stage 7 Controller Test 12: POST /api/trousseau/{boardId}/transfer-all-to-cart returns 200 OK")
    void testTransferAllToCartSuccess() throws Exception {
        TransferToCartResponse res = TransferToCartResponse.builder()
                .boardId(42L)
                .addedCount(3)
                .alreadyInCartCount(0)
                .outOfStockCount(0)
                .message("Transfer complete: 3 items added to cart")
                .build();

        when(trousseauService.transferAllToCart(101L, 42L)).thenReturn(res);

        mockMvc.perform(post("/api/trousseau/42/transfer-all-to-cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addedCount").value(3));
    }
}
