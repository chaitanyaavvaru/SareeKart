package com.sareekart.service;

import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.repository.CustomerEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Phase 7: Asynchronous Behavioral Event Projector.
 * 
 * Non-blocking event projection: Dispatches events to Neo4j asynchronously.
 * If Neo4j is offline or slow, user requests are never blocked or delayed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Neo4jEventProjector {

    private final GraphService graphService;
    private final CustomerEventRepository customerEventRepository;

    @Async
    public void projectEventAsync(CustomerEventResponse event) {
        if (event == null || event.getUserId() == null) {
            // Guest events are staged in MySQL until identity resolution
            return;
        }

        try {
            Long userId = event.getUserId();
            String type = event.getEventType();
            Long entityId = event.getEntityId();
            String timestamp = event.getCreatedAt() != null ?
                    DateTimeFormatter.ISO_INSTANT.format(event.getCreatedAt().atZone(java.time.ZoneId.of("UTC")).toInstant()) :
                    DateTimeFormatter.ISO_INSTANT.format(Instant.now());

            Map<String, Object> meta = event.getMetadata();

            if ("PRODUCT_VIEW".equalsIgnoreCase(type) && entityId != null) {
                long dwell = 5000L;
                if (meta != null && meta.containsKey("dwellTimeMs")) {
                    try {
                        dwell = Long.parseLong(meta.get("dwellTimeMs").toString());
                    } catch (Exception ignored) {}
                }
                graphService.recordProductView(userId, entityId, dwell, timestamp);
            } else if ("ADD_TO_CART".equalsIgnoreCase(type) && entityId != null) {
                graphService.recordAddToCart(userId, entityId, timestamp);
            } else if ("ADD_TO_WISHLIST".equalsIgnoreCase(type) && entityId != null) {
                graphService.recordWishlist(userId, entityId, true, timestamp);
            } else if ("REMOVE_FROM_WISHLIST".equalsIgnoreCase(type) && entityId != null) {
                graphService.recordWishlist(userId, entityId, false, timestamp);
            } else if ("ORDER_COMPLETED".equalsIgnoreCase(type) && entityId != null) {
                double amount = 0.0;
                if (meta != null && meta.containsKey("totalAmount")) {
                    try {
                        amount = Double.parseDouble(meta.get("totalAmount").toString());
                    } catch (Exception ignored) {}
                }
                graphService.recordPurchase(userId, entityId, amount, timestamp);
            } else if ("CATEGORY_VIEW".equalsIgnoreCase(type) && entityId != null) {
                graphService.recordCategoryAffinity(userId, entityId, timestamp);
            }
        } catch (Exception ex) {
            log.warn("Asynchronous graph projection non-blocking failure: {}", ex.getMessage());
        }
    }

    @Async
    public void projectSessionIdentifiedAsync(String sessionId, Long userId) {
        if (sessionId == null || userId == null) return;
        try {
            List<CustomerEvent> events = customerEventRepository.findBySessionId(sessionId);
            log.info("Projecting {} historical events for newly identified user {}", events.size(), userId);
            for (CustomerEvent ev : events) {
                String type = ev.getEventType();
                Long entityId = ev.getEntityId();
                if (entityId == null) continue;

                String timestamp = ev.getCreatedAt() != null ?
                        DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt().atZone(java.time.ZoneId.of("UTC")).toInstant()) :
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now());

                if ("PRODUCT_VIEW".equalsIgnoreCase(type)) {
                    graphService.recordProductView(userId, entityId, 5000L, timestamp);
                } else if ("ADD_TO_CART".equalsIgnoreCase(type)) {
                    graphService.recordAddToCart(userId, entityId, timestamp);
                } else if ("ADD_TO_WISHLIST".equalsIgnoreCase(type)) {
                    graphService.recordWishlist(userId, entityId, true, timestamp);
                } else if ("CATEGORY_VIEW".equalsIgnoreCase(type)) {
                    graphService.recordCategoryAffinity(userId, entityId, timestamp);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to project identified session events to graph: {}", ex.getMessage());
        }
    }
}
