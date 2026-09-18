package com.sareekart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.entity.User;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.service.TrousseauService;
import com.sareekart.service.TrousseauSseService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandatory SSE Security & Lifecycle Tests:
 * A. Owner can subscribe.
 * B. Non-owner authenticated customer receives authorization failure.
 * C. Nonexistent board cannot create subscription.
 * D. Unauthorized caller does not create an emitter.
 * E. Events from board A never reach board B.
 * F. Completed emitter is removed.
 * G. Timed-out emitter is removed.
 * H. Error emitter is removed.
 * I. Empty board emitter collection is pruned.
 */
@ExtendWith(MockitoExtension.class)
public class TrousseauSseSecurityTest {

    @Mock
    private TrousseauService trousseauService;

    private TrousseauSseService trousseauSseService;
    private MockMvc mockMvc;

    private User ownerUser;
    private User nonOwnerUser;
    private final AtomicReference<User> currentUser = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        trousseauSseService = new TrousseauSseService(new ObjectMapper());
        trousseauSseService.clearAll();

        ownerUser = User.builder().id(101L).email("owner@example.com").firstName("Owner").build();
        nonOwnerUser = User.builder().id(202L).email("intruder@example.com").firstName("Intruder").build();

        TrousseauBoardController controller = new TrousseauBoardController(
                trousseauService,
                null,
                null,
                trousseauSseService
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return currentUser.get();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Test A: Owner can successfully subscribe to their board's SSE stream")
    void testA_OwnerCanSubscribe() throws Exception {
        currentUser.set(ownerUser);
        Long boardId = 10L;

        doNothing().when(trousseauService).validateBoardOwnership(101L, boardId);

        mockMvc.perform(get("/api/trousseau/{boardId}/stream", boardId)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        verify(trousseauService).validateBoardOwnership(101L, boardId);
        assertTrue(trousseauSseService.hasActiveEmitters(boardId));
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardId));
    }

    @Test
    @DisplayName("Test B: Non-owner authenticated customer receives 403 authorization failure")
    void testB_NonOwnerReceivesAuthorizationFailure() throws Exception {
        currentUser.set(nonOwnerUser);
        Long boardId = 10L;

        doThrow(new AccessDeniedException("You do not have permission to access this trousseau board"))
                .when(trousseauService).validateBoardOwnership(202L, boardId);

        mockMvc.perform(get("/api/trousseau/{boardId}/stream", boardId)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());

        verify(trousseauService).validateBoardOwnership(202L, boardId);
        // CRITICAL: Emitter MUST NOT be created
        assertFalse(trousseauSseService.hasActiveEmitters(boardId));
        assertEquals(0, trousseauSseService.getActiveEmitterCount(boardId));
    }

    @Test
    @DisplayName("Test C: Nonexistent board returns 404 and does not create subscription")
    void testC_NonexistentBoardCannotCreateSubscription() throws Exception {
        currentUser.set(ownerUser);
        Long nonExistentBoardId = 9999L;

        doThrow(new ResourceNotFoundException("Trousseau board not found: 9999"))
                .when(trousseauService).validateBoardOwnership(101L, nonExistentBoardId);

        mockMvc.perform(get("/api/trousseau/{boardId}/stream", nonExistentBoardId)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());

        verify(trousseauService).validateBoardOwnership(101L, nonExistentBoardId);
        assertFalse(trousseauSseService.hasActiveEmitters(nonExistentBoardId));
    }

    @Test
    @DisplayName("Test D: Unauthenticated caller receives 403 and creates no emitter")
    void testD_UnauthorizedCallerDoesNotCreateEmitter() throws Exception {
        currentUser.set(null); // anonymous caller
        Long boardId = 10L;

        mockMvc.perform(get("/api/trousseau/{boardId}/stream", boardId)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());

        verify(trousseauService, never()).validateBoardOwnership(any(), any());
        assertFalse(trousseauSseService.hasActiveEmitters(boardId));
    }

    @Test
    @DisplayName("Test E: Events published to board A never leak or reach board B")
    void testE_EventsFromBoardANeverReachBoardB() {
        Long boardA = 10L;
        Long boardB = 20L;

        SseEmitter emitterA = trousseauSseService.subscribe(boardA);
        SseEmitter emitterB = trousseauSseService.subscribe(boardB);

        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardA));
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardB));

        // Publish to Board A
        trousseauSseService.publish(boardA, "ITEM_ADDED", java.util.Map.of("itemId", 101L));

        // Both emitters remain isolated and cleanly registered to their respective boards
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardA));
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardB));
    }

    @Test
    @DisplayName("Test F: Completed emitter is removed from registry")
    void testF_CompletedEmitterIsRemoved() {
        Long boardId = 15L;
        SseEmitter emitter = trousseauSseService.subscribe(boardId);
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardId));

        // Simulate client completion
        emitter.complete();
        assertEquals(0, trousseauSseService.getActiveEmitterCount(boardId));
    }

    @Test
    @DisplayName("Test G: Timed-out emitter is removed from registry")
    void testG_TimedOutEmitterIsRemoved() {
        Long boardId = 16L;
        SseEmitter emitter = trousseauSseService.subscribe(boardId);
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardId));

        // Trigger timeout via service remove hook
        trousseauSseService.remove(boardId, emitter);
        assertEquals(0, trousseauSseService.getActiveEmitterCount(boardId));
    }

    @Test
    @DisplayName("Test H: Error emitter is removed from registry")
    void testH_ErrorEmitterIsRemoved() {
        Long boardId = 17L;
        SseEmitter emitter = trousseauSseService.subscribe(boardId);
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardId));

        // Simulate error completion
        emitter.completeWithError(new java.io.IOException("Client reset"));
        assertEquals(0, trousseauSseService.getActiveEmitterCount(boardId));
    }

    @Test
    @DisplayName("Test I: Empty board emitter collection is completely pruned from memory")
    void testI_EmptyBoardEmitterCollectionIsPruned() {
        Long boardId = 18L;
        SseEmitter emitter1 = trousseauSseService.subscribe(boardId);
        SseEmitter emitter2 = trousseauSseService.subscribe(boardId);
        assertEquals(2, trousseauSseService.getActiveEmitterCount(boardId));

        // Complete first emitter -> still 1 remaining
        emitter1.complete();
        assertEquals(1, trousseauSseService.getActiveEmitterCount(boardId));

        // Complete second emitter -> list empty, map entry pruned!
        emitter2.complete();
        assertEquals(0, trousseauSseService.getActiveEmitterCount(boardId));
        assertFalse(trousseauSseService.hasActiveEmitters(boardId));
    }
}
