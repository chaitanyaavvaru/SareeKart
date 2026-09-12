package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.config.CartConstants;
import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.request.CartMergeRequest;
import com.sareekart.dto.response.CartItemResponse;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.User;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.service.CartService;
import com.sareekart.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartAndWishlistSecurityControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @Mock
    private WishlistService wishlistService;

    private MockMvc cartMockMvc;
    private MockMvc wishlistMockMvc;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(101L).email("userA@example.com").build();
        userB = User.builder().id(202L).email("userB@example.com").build();

        // Custom argument resolver to inject authenticated principal
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
                if ("userB".equalsIgnoreCase(authHeader)) {
                    return userB;
                }
                return userA; // Default to userA
            }
        };

        cartMockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userResolver)
                .build();

        wishlistMockMvc = MockMvcBuilders.standaloneSetup(new WishlistController(wishlistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    void getCart_derivesIdentityExclusivelyFromPrincipal_userA() throws Exception {
        CartResponse cartA = CartResponse.builder()
                .id(1L)
                .userId(101L)
                .totalPrice(new BigDecimal("15000"))
                .totalItems(1)
                .items(List.of(CartItemResponse.builder().id(10L).productId(5L).quantity(1).build()))
                .build();

        when(cartService.getCart(101L)).thenReturn(cartA);

        cartMockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101L))
                .andExpect(jsonPath("$.data.items[0].productId").value(5L));

        verify(cartService).getCart(101L);
        verify(cartService, never()).getCart(202L);
    }

    @Test
    void getCart_derivesIdentityExclusivelyFromPrincipal_userB() throws Exception {
        CartResponse cartB = CartResponse.builder()
                .id(2L)
                .userId(202L)
                .totalPrice(new BigDecimal("32000"))
                .totalItems(2)
                .items(List.of(CartItemResponse.builder().id(20L).productId(8L).quantity(2).build()))
                .build();

        when(cartService.getCart(202L)).thenReturn(cartB);

        cartMockMvc.perform(get("/api/cart").header("X-Simulate-User", "userB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(202L));

        verify(cartService).getCart(202L);
        verify(cartService, never()).getCart(101L);
    }

    @Test
    void addItemToCart_usesAuthenticatedPrincipalOnly() throws Exception {
        CartItemRequest req = CartItemRequest.builder().productId(12L).quantity(2).build();
        when(cartService.addItemToCart(eq(101L), any(CartItemRequest.class)))
                .thenReturn(CartResponse.builder().id(1L).userId(101L).build());

        cartMockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartService).addItemToCart(eq(101L), any(CartItemRequest.class));
    }

    @Test
    void mergeGuestCart_authenticatedPrincipalIsolation() throws Exception {
        CartMergeRequest mergeReq = CartMergeRequest.builder()
                .items(List.of(CartItemRequest.builder().productId(15L).quantity(1).build()))
                .build();

        when(cartService.mergeGuestCart(eq(101L), any(CartMergeRequest.class)))
                .thenReturn(CartResponse.builder().id(1L).userId(101L).build());

        cartMockMvc.perform(post("/api/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartService).mergeGuestCart(eq(101L), any(CartMergeRequest.class));
    }

    @Test
    void wishlist_userIsolation_userACannotAccessUserBWishlist() throws Exception {
        when(wishlistService.getWishlist(101L)).thenReturn(List.of(
                ProductResponse.builder().id(33L).name("Kanchipuram Silk").build()
        ));

        wishlistMockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(33L));

        verify(wishlistService).getWishlist(101L);
        verify(wishlistService, never()).getWishlist(202L);
    }

    @Test
    void wishlist_moveWishlistToCart_usesPrincipalIdentity() throws Exception {
        when(wishlistService.moveWishlistToCart(101L, 45L))
                .thenReturn(CartResponse.builder().id(1L).userId(101L).build());

        wishlistMockMvc.perform(post("/api/wishlist/move-to-cart/45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(wishlistService).moveWishlistToCart(101L, 45L);
        verify(wishlistService, never()).moveWishlistToCart(eq(202L), anyLong());
    }
}
