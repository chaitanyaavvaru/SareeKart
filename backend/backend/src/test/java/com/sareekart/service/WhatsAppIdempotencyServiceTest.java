package com.sareekart.service;

import com.sareekart.entity.WhatsAppMessage;
import com.sareekart.repository.WhatsAppMessageRepository;
import com.sareekart.service.impl.WhatsAppIdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppIdempotencyServiceTest {

    @Mock
    private WhatsAppMessageRepository messageRepository;

    @InjectMocks
    private WhatsAppIdempotencyServiceImpl idempotencyService;

    @BeforeEach
    void setUp() {
        when(messageRepository.findByWamId(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("Acquiring lock on new wam_id succeeds")
    void testAcquireLockSuccess() {
        String wamId = "wam_unique_12345";

        boolean acquired = idempotencyService.tryAcquireLock(wamId);

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("Duplicate delivery of identical wam_id is rejected by atomic cache")
    void testDuplicateDeliveryRejected() {
        String wamId = "wam_dup_99999";

        boolean firstAttempt = idempotencyService.tryAcquireLock(wamId);
        boolean secondAttempt = idempotencyService.tryAcquireLock(wamId);

        assertThat(firstAttempt).isTrue();
        assertThat(secondAttempt).isFalse();
    }

    @Test
    @DisplayName("Message already persisted in DB is rejected even if cache expired")
    void testMessageAlreadyInDatabase() {
        String wamId = "wam_db_already_saved";
        when(messageRepository.findByWamId(wamId)).thenReturn(new WhatsAppMessage());

        boolean acquired = idempotencyService.tryAcquireLock(wamId);

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("Releasing lock allows re-acquisition")
    void testReleaseLock() {
        String wamId = "wam_retry_5555";

        assertThat(idempotencyService.tryAcquireLock(wamId)).isTrue();
        assertThat(idempotencyService.tryAcquireLock(wamId)).isFalse();

        idempotencyService.releaseLock(wamId);

        assertThat(idempotencyService.tryAcquireLock(wamId)).isTrue();
    }

    @Test
    @DisplayName("isProcessed detects both in-flight and database records")
    void testIsProcessed() {
        String wamId = "wam_check_1111";

        assertThat(idempotencyService.isProcessed(wamId)).isFalse();

        idempotencyService.tryAcquireLock(wamId);
        assertThat(idempotencyService.isProcessed(wamId)).isTrue();
    }
}
