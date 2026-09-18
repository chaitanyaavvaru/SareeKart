package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Event (SSE) subscriptions keyed by boardId.
 * Provides live real-time broadcasts for ceremony, item, and voting lifecycle changes.
 *
 * Implements strict lifecycle bounds:
 * - Bounded connection timeout (30 minutes)
 * - Automatic emitter cleanup on completion, timeout, and client disconnect
 * - Empty board collection pruning to prevent memory leaks
 * - Safe JSON serialization via ObjectMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrousseauSseService {

    /** Default bounded timeout: 30 minutes (1,800,000 ms) */
    public static final long DEFAULT_SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public static class BoundedSseEmitter extends SseEmitter {
        private Runnable completionCallback;
        private Runnable timeoutCallback;
        private java.util.function.Consumer<Throwable> errorCallback;

        public BoundedSseEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            super.onCompletion(callback);
            this.completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            super.onTimeout(callback);
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(java.util.function.Consumer<Throwable> callback) {
            super.onError(callback);
            this.errorCallback = callback;
        }

        @Override
        public synchronized void complete() {
            super.complete();
            if (completionCallback != null) {
                completionCallback.run();
            }
        }

        @Override
        public synchronized void completeWithError(Throwable ex) {
            super.completeWithError(ex);
            if (errorCallback != null) {
                errorCallback.accept(ex);
            }
        }
    }

    /**
     * Subscribe a client to real-time events for a specific board.
     * Note: Board ownership MUST be validated before calling this method.
     *
     * @param boardId The validated board ID
     * @return SseEmitter instance with bounded timeout
     */
    public SseEmitter subscribe(Long boardId) {
        return subscribe(boardId, DEFAULT_SSE_TIMEOUT);
    }

    /**
     * Overload for custom or test timeouts.
     */
    public SseEmitter subscribe(Long boardId, long timeoutMs) {
        SseEmitter emitter = new BoundedSseEmitter(timeoutMs);
        emitters.computeIfAbsent(boardId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(boardId, emitter));
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out for board {}", boardId);
            remove(boardId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE emitter error for board {}: {}", boardId, e.getMessage());
            remove(boardId, emitter);
        });

        // Send initial connection confirmation event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("{\"boardId\":" + boardId + "}"));
        } catch (IOException e) {
            log.warn("Could not send initial SSE heartbeat to board {}", boardId);
            remove(boardId, emitter);
        }

        return emitter;
    }

    /**
     * Publish a named event to all active subscribers of the given board.
     * Dead emitters are automatically pruned.
     *
     * @param boardId   The board ID
     * @param eventName Name of the event (e.g. CEREMONY_ADDED, ITEM_ADDED, VOTE_CAST)
     * @param payload   Object payload to serialize as JSON
     */
    public void publish(Long boardId, String eventName, Object payload) {
        List<SseEmitter> boardEmitters = emitters.get(boardId);
        if (boardEmitters == null || boardEmitters.isEmpty()) {
            return;
        }

        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Could not serialize SSE payload for event {} on board {}", eventName, boardId, e);
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : boardEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        if (!dead.isEmpty()) {
            boardEmitters.removeAll(dead);
            if (boardEmitters.isEmpty()) {
                emitters.remove(boardId, boardEmitters);
            }
        }
    }

    /**
     * Safely remove an emitter and prune empty collections.
     */
    public void remove(Long boardId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(boardId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(boardId, list);
            }
        }
    }

    /**
     * Get the count of active emitters for a board (used in telemetry and testing).
     */
    public int getActiveEmitterCount(Long boardId) {
        List<SseEmitter> list = emitters.get(boardId);
        return list != null ? list.size() : 0;
    }

    /**
     * Check whether a board has any active emitters.
     */
    public boolean hasActiveEmitters(Long boardId) {
        return getActiveEmitterCount(boardId) > 0;
    }

    /**
     * Reset all emitters (testing utility).
     */
    public void clearAll() {
        emitters.clear();
    }
}
