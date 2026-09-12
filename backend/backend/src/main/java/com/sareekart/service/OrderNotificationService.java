package com.sareekart.service;

import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final WhatsAppNotificationService whatsAppNotificationService;

    public void sendOrderPlacedNotification(Order order) {
        try {
            whatsAppNotificationService.sendOrderPlacedNotification(order);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp order placed alert for order #{}: {}", order != null ? order.getId() : "null", e.getMessage());
        }
    }

    public void sendOrderStatusUpdateNotification(Order order) {
        if (order == null || order.getStatus() == null) return;
        try {
            if (order.getStatus() == OrderStatus.SHIPPED) {
                whatsAppNotificationService.sendOrderShippedNotification(order);
            } else if (order.getStatus() == OrderStatus.DELIVERED) {
                whatsAppNotificationService.sendOrderDeliveredNotification(order);
            } else {
                // Fallback for general status changes
                whatsAppNotificationService.sendOrderShippedNotification(order);
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp order status alert for order #{}: {}", order.getId(), e.getMessage());
        }
    }
}
