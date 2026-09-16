package com.sareekart.service.impl;

import com.sareekart.dto.whatsapp.WhatsAppSessionContext;
import com.sareekart.service.WhatsAppConversationStateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WhatsAppConversationStateManagerImpl implements WhatsAppConversationStateManager {

    private final Map<String, WhatsAppSessionContext> sessionMap = new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MS = 30 * 60 * 1000L; // 30 minutes

    @Override
    public WhatsAppSessionContext getSession(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new WhatsAppSessionContext();
        }

        WhatsAppSessionContext ctx = sessionMap.get(phoneNumber);
        long now = System.currentTimeMillis();

        // Expire if idle for > 30 minutes
        if (ctx == null || (now - ctx.getLastActivityTime()) > SESSION_TTL_MS) {
            ctx = WhatsAppSessionContext.builder()
                    .phoneNumber(phoneNumber)
                    .lastActivityTime(now)
                    .build();
            sessionMap.put(phoneNumber, ctx);
        } else {
            ctx.setLastActivityTime(now);
        }

        return ctx;
    }

    @Override
    public void updateSession(String phoneNumber, WhatsAppSessionContext context) {
        if (phoneNumber != null && context != null) {
            context.setLastActivityTime(System.currentTimeMillis());
            
            // Limit recent turns to 4
            if (context.getRecentTurns() != null && context.getRecentTurns().size() > 4) {
                int size = context.getRecentTurns().size();
                context.setRecentTurns(context.getRecentTurns().subList(size - 4, size));
            }
            
            sessionMap.put(phoneNumber, context);
        }
    }

    @Override
    public void clearSession(String phoneNumber) {
        if (phoneNumber != null) {
            sessionMap.remove(phoneNumber);
        }
    }

    @Scheduled(fixedRate = 600_000) // Cleanup every 10 minutes
    public void evictExpiredSessions() {
        long cutoff = System.currentTimeMillis() - SESSION_TTL_MS;
        sessionMap.entrySet().removeIf(e -> e.getValue().getLastActivityTime() < cutoff);
    }
}
