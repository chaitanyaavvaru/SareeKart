package com.sareekart.service;

import com.sareekart.entity.WebhookEvent;
import com.sareekart.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deduplication ledger for webhook deliveries.
 *
 * The insert runs in an INDEPENDENT transaction (REQUIRES_NEW): catching the
 * unique-constraint violation inside the caller's transaction would mark it
 * rollback-only and blow up at commit. Returns null when the event id was
 * already recorded (duplicate/at-least-once delivery).
 */
@Service
@RequiredArgsConstructor
public class WebhookLedgerService {

    private final WebhookEventRepository webhookEventRepository;

    /** @return the persisted record, or null if this event was already seen. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookEvent recordIfFirst(String eventId, String eventType, String payloadJson) {
        if (webhookEventRepository.existsByEventId(eventId)) {
            return null;
        }
        WebhookEvent record = new WebhookEvent();
        record.setEventId(eventId);
        record.setEventType(eventType);
        record.setPayload(payloadJson);
        try {
            return webhookEventRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException raceLost) {
            // Concurrent delivery won the unique race.
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(WebhookEvent record, String errorOrNull) {
        record.setProcessed(errorOrNull == null);
        record.setErrorMessage(errorOrNull);
        record.setProcessedAt(java.time.LocalDateTime.now());
        webhookEventRepository.save(record);
    }
}