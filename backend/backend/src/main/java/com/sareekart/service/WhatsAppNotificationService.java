package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppDispatchSimulationRequest;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppTelemetryResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.ReturnRequest;
import com.sareekart.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WhatsAppNotificationService {

    WhatsAppNotificationResponse sendOrderPlacedNotification(Order order);

    WhatsAppNotificationResponse sendOrderShippedNotification(Order order);

    WhatsAppNotificationResponse sendOrderOutForDeliveryNotification(Order order);

    WhatsAppNotificationResponse sendOrderDeliveredNotification(Order order);

    WhatsAppNotificationResponse sendReturnPickupNotification(ReturnRequest returnRequest);

    WhatsAppNotificationResponse simulateManualDispatch(WhatsAppDispatchSimulationRequest request, User staff);

    WhatsAppNotificationResponse resendNotification(Long logId, User staff);

    List<WhatsAppNotificationResponse> getOrderNotifications(Long orderId, User user);

    Page<WhatsAppNotificationResponse> getAllLogs(String eventType, Pageable pageable);

    WhatsAppTelemetryResponse getTelemetry();

    boolean updateOptIn(User user, boolean optIn);
}
