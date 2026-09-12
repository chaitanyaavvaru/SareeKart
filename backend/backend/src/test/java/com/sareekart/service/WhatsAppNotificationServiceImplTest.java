package com.sareekart.service;

import com.sareekart.dto.whatsapp.WhatsAppDispatchSimulationRequest;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppTelemetryResponse;
import com.sareekart.entity.*;
import com.sareekart.enums.WhatsAppEventType;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WhatsAppNotificationLogRepository;
import com.sareekart.service.impl.WhatsAppNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WhatsAppNotificationServiceImplTest {

    @Mock
    private WhatsAppNotificationLogRepository notificationLogRepository;

    @Mock
    private WhatsAppApiClient whatsAppApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private WhatsAppNotificationServiceImpl notificationService;

    private User customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(101L)
                .firstName("Ananya")
                .lastName("Sharma")
                .email("ananya@example.com")
                .mobile("+919876543210")
                .role(Role.CUSTOMER)
                .whatsappOptIn(true)
                .build();

        Product saree = Product.builder()
                .id(1L)
                .name("Kanchipuram Crimson Royal Bridal Silk Saree")
                .price(new BigDecimal("42000.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(saree)
                .quantity(1)
                .price(new BigDecimal("42000.00"))
                .build();

        List<OrderItem> items = new ArrayList<>();
        items.add(item);

        order = Order.builder()
                .id(501L)
                .user(customer)
                .items(items)
                .totalAmount(new BigDecimal("42000.00"))
                .status(OrderStatus.CONFIRMED)
                .courierPartner("Blue Dart Apex Air")
                .trackingNumber("BD-778899")
                .estimatedDeliveryDate("Monday, 14 Sep")
                .shippingAddress(Address.builder()
                        .fullName("Ananya Sharma")
                        .streetAddress("108 Indiranagar")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .pincode("560038")
                        .phone("+919876543210")
                        .build())
                .build();
    }

    @Test
    @DisplayName("sendOrderPlacedNotification creates simulated luxury template when token omitted")
    void testSendOrderPlacedNotification_Success() {
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(1L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.sendOrderPlacedNotification(order);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(501L);
        assertThat(response.getEventType()).isEqualTo(WhatsAppEventType.ORDER_CONFIRMED);
        assertThat(response.getMessageContent()).contains("Namaste Ananya");
        assertThat(response.getMessageContent()).contains("Kanchipuram Crimson Royal Bridal Silk Saree");
        assertThat(response.getMessageContent()).contains("Silk Mark India");
        assertThat(response.getDeliveryStatus()).isEqualTo("SIMULATED");
        assertThat(response.getSimulated()).isTrue();

        verify(notificationLogRepository, times(1)).save(any(WhatsAppNotificationLog.class));
    }

    @Test
    @DisplayName("sendOrderPlacedNotification suppresses alert when customer has opted out")
    void testSendOrderPlacedNotification_OptedOut_Suppressed() {
        customer.setWhatsappOptIn(false);

        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(2L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.sendOrderPlacedNotification(order);

        assertThat(response).isNotNull();
        assertThat(response.getDeliveryStatus()).isEqualTo("SUPPRESSED");
        verify(whatsAppApiClient, never()).sendTextMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("sendOrderShippedNotification includes courier name, AWB, and live tracking URL")
    void testSendOrderShippedNotification_Success() {
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(3L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.sendOrderShippedNotification(order);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(WhatsAppEventType.SHIPPED);
        assertThat(response.getCourierPartner()).isEqualTo("Blue Dart Apex Air");
        assertThat(response.getTrackingNumber()).isEqualTo("BD-778899");
        assertThat(response.getTrackingUrl()).contains("bluedart.com/tracking?awb=BD-778899");
        assertThat(response.getMessageContent()).contains("Bengaluru Central Vault (WH-01)");
    }

    @Test
    @DisplayName("sendOrderDeliveredNotification includes silk care guide and 7-day return window")
    void testSendOrderDeliveredNotification_Success() {
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(4L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.sendOrderDeliveredNotification(order);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(WhatsAppEventType.DELIVERED);
        assertThat(response.getMessageContent()).contains("sareekart.com/saree-care");
        assertThat(response.getMessageContent()).contains("7 days");
    }

    @Test
    @DisplayName("sendReturnPickupNotification generates reverse logistics handover instructions")
    void testSendReturnPickupNotification_Success() {
        ReturnRequest returnRequest = ReturnRequest.builder()
                .id(88L)
                .order(order)
                .user(customer)
                .reverseCourier("Blue Dart Reverse Logistics")
                .reverseTrackingNumber("REV-BD-99881")
                .build();

        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(5L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.sendReturnPickupNotification(returnRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(WhatsAppEventType.RETURN_PICKUP);
        assertThat(response.getReturnRequestId()).isEqualTo(88L);
        assertThat(response.getCourierPartner()).isEqualTo("Blue Dart Reverse Logistics");
        assertThat(response.getTrackingNumber()).isEqualTo("REV-BD-99881");
        assertThat(response.getMessageContent()).contains("Reverse Pickup Scheduled");
    }

    @Test
    @DisplayName("simulateManualDispatch persists custom or simulated milestone message")
    void testSimulateManualDispatch_Success() {
        WhatsAppDispatchSimulationRequest request = WhatsAppDispatchSimulationRequest.builder()
                .orderId(501L)
                .recipientName("Meera Reddy")
                .recipientPhone("+919988776655")
                .eventType(WhatsAppEventType.OUT_FOR_DELIVERY)
                .courierPartner("Delhivery Express")
                .trackingNumber("DEL-554433")
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(notificationLogRepository.save(any(WhatsAppNotificationLog.class))).thenAnswer(invocation -> {
            WhatsAppNotificationLog log = invocation.getArgument(0);
            log.setId(6L);
            log.setCreatedAt(LocalDateTime.now());
            return log;
        });

        WhatsAppNotificationResponse response = notificationService.simulateManualDispatch(request, customer);

        assertThat(response).isNotNull();
        assertThat(response.getRecipientName()).isEqualTo("Meera Reddy");
        assertThat(response.getRecipientPhone()).isEqualTo("+919988776655");
        assertThat(response.getEventType()).isEqualTo(WhatsAppEventType.OUT_FOR_DELIVERY);
        assertThat(response.getSimulated()).isTrue();
    }

    @Test
    @DisplayName("getTelemetry calculates dispatched, simulated, and delivery rates accurately")
    void testGetTelemetry_Success() {
        when(notificationLogRepository.count()).thenReturn(10L);
        when(notificationLogRepository.countBySimulated(true)).thenReturn(8L);
        when(notificationLogRepository.countBySimulated(false)).thenReturn(2L);
        when(notificationLogRepository.countDelivered()).thenReturn(2L);
        when(notificationLogRepository.countFailed()).thenReturn(0L);

        when(notificationLogRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        WhatsAppTelemetryResponse telemetry = notificationService.getTelemetry();

        assertThat(telemetry).isNotNull();
        assertThat(telemetry.getTotalDispatched()).isEqualTo(10L);
        assertThat(telemetry.getSimulatedCount()).isEqualTo(8L);
        assertThat(telemetry.getLiveCount()).isEqualTo(2L);
        assertThat(telemetry.getDeliveryRatePercent()).isEqualTo(100.0);
    }
}
