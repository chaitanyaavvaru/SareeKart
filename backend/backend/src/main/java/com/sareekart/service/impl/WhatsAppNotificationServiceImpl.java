package com.sareekart.service.impl;

import com.sareekart.dto.whatsapp.WhatsAppDispatchSimulationRequest;
import com.sareekart.dto.whatsapp.WhatsAppNotificationResponse;
import com.sareekart.dto.whatsapp.WhatsAppTelemetryResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.ReturnRequest;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppNotificationLog;
import com.sareekart.enums.WhatsAppEventType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WhatsAppNotificationLogRepository;
import com.sareekart.service.WhatsAppApiClient;
import com.sareekart.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import com.sareekart.entity.WhatsAppContact;
import com.sareekart.repository.WhatsAppContactRepository;
import com.sareekart.service.WhatsAppIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationServiceImpl implements WhatsAppNotificationService {

    private final WhatsAppNotificationLogRepository notificationLogRepository;
    private final WhatsAppApiClient whatsAppApiClient;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    @Autowired(required = false)
    private WhatsAppContactRepository contactRepository;

    @Autowired(required = false)
    private WhatsAppIdentityService identityService;

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    @Transactional
    public WhatsAppNotificationResponse sendOrderPlacedNotification(Order order) {
        if (order == null || order.getUser() == null) {
            log.warn("Cannot send WhatsApp order placed notification: order or user is null");
            return null;
        }
        User user = order.getUser();
        String mobile = resolveMobile(user, order);
        if (mobile == null || mobile.isBlank()) {
            log.info("Skipping WhatsApp alert for order #{}: no mobile number", order.getId());
            return null;
        }

        String orderRef = getOrderReference(order);
        String trackingUrl = "https://sareekart.com/orders/track?orderNumber=" + orderRef;

        String itemSummary = order.getItems() != null && !order.getItems().isEmpty()
                ? order.getItems().stream()
                .map(item -> String.format("• %s (x%d)", item.getProduct() != null ? item.getProduct().getName() : "Handloom Saree", item.getQuantity()))
                .collect(Collectors.joining("\n"))
                : "• Authentic Handloom Saree (x1)";

        String estDate = order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate() : "3-5 Business Days";

        String message = String.format(
                "🙏 *Namaste %s!*\n\n" +
                        "Thank you for patronizing *SareeKart Handlooms*. Your bespoke order *#%s* has been confirmed!\n\n" +
                        "✨ *Artisan Drape Summary:*\n%s\n\n" +
                        "💰 *Total Amount:* ₹%,.2f\n" +
                        "📅 *Estimated Delivery:* %s\n\n" +
                        "Our master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n" +
                        "🔗 *Track Order:* %s",
                user.getFirstName(), orderRef, itemSummary,
                order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
                estDate, trackingUrl
        );

        return processAndPersist(order.getId(), null, user, user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : ""),
                mobile, WhatsAppEventType.ORDER_CONFIRMED, "tpl_order_confirmed_v1", message, orderRef, null,
                trackingUrl);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse sendOrderShippedNotification(Order order) {
        if (order == null || order.getUser() == null) {
            return null;
        }
        User user = order.getUser();
        String mobile = resolveMobile(user, order);
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String orderRef = getOrderReference(order);
        String courier = order.getCourierPartner() != null ? order.getCourierPartner() : "Blue Dart Apex Air";
        String awb = order.getTrackingNumber() != null ? order.getTrackingNumber() : "BD-" + (100000 + (order.getId() * 73));
        String trackingUrl = buildTrackingUrl(courier, awb);
        String estDate = order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate() : "2-3 Business Days";

        String message = String.format(
                "📦 *Heirloom Saree Dispatched!*\n\n" +
                        "*Namaste %s*, your handloom order *#%s* is en route!\n\n" +
                        "🚚 *Courier Partner:* %s\n" +
                        "🔖 *AWB Tracking Number:* %s\n" +
                        "📍 *Fulfillment Hub:* Bengaluru Central Vault (WH-01)\n" +
                        "🎯 *Estimated Arrival:* %s\n\n" +
                        "Track your consignment in real time:\n%s\n\n" +
                        "Your weave is protected inside our signature breathable muslin dust bag.",
                user.getFirstName(), orderRef, courier, awb, estDate, trackingUrl
        );

        return processAndPersist(order.getId(), null, user, user.getFirstName(), mobile,
                WhatsAppEventType.SHIPPED, "tpl_order_shipped_v1", message, awb, courier, trackingUrl);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse sendOrderOutForDeliveryNotification(Order order) {
        if (order == null || order.getUser() == null) {
            return null;
        }
        User user = order.getUser();
        String mobile = resolveMobile(user, order);
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String orderRef = getOrderReference(order);
        String codNotice = "CASH_ON_DELIVERY".equalsIgnoreCase(order.getPaymentMethod())
                ? String.format("• COD Amount: Please keep exact cash of ₹%,.2f ready.", order.getTotalAmount())
                : "• Contactless Prepaid Delivery: Secure OTP verification with delivery agent.";

        String destination = order.getShippingAddress() != null
                ? order.getShippingAddress().getCity() + " (" + order.getShippingAddress().getPincode() + ")"
                : "Registered Address";

        String message = String.format(
                "🚚 *Out for Delivery Today!*\n\n" +
                        "*Namaste %s*, your SareeKart package for order *#%s* is out with our delivery partner.\n\n" +
                        "🔔 *Doorstep Instructions:*\n%s\n" +
                        "📍 *Destination:* %s\n\n" +
                        "Please verify the tamper-proof Silk Mark seal upon handover.",
                user.getFirstName(), orderRef, codNotice, destination
        );

        return processAndPersist(order.getId(), null, user, user.getFirstName(), mobile,
                WhatsAppEventType.OUT_FOR_DELIVERY, "tpl_out_for_delivery_v1", message,
                order.getTrackingNumber() != null ? order.getTrackingNumber() : orderRef, order.getCourierPartner(), null);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse sendOrderDeliveredNotification(Order order) {
        if (order == null || order.getUser() == null) {
            return null;
        }
        User user = order.getUser();
        String mobile = resolveMobile(user, order);
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String orderRef = getOrderReference(order);
        String message = String.format(
                "🌸 *Delivered with Reverence!*\n\n" +
                        "*Namaste %s*, order *#%s* has been safely delivered to your doorstep.\n\n" +
                        "🥻 We hope your authentic handloom drape brings timeless elegance to your celebrations.\n\n" +
                        "📖 *Silk Care & Preservation Guide:*\nhttps://sareekart.com/saree-care\n\n" +
                        "🔄 *Doorstep Returns/Exchanges:* Eligible for 7 days via https://sareekart.com/orders/track?orderNumber=%s\n\n" +
                        "Thank you for sustaining India's generational weaving heritage.",
                user.getFirstName(), orderRef, orderRef
        );

        return processAndPersist(order.getId(), null, user, user.getFirstName(), mobile,
                WhatsAppEventType.DELIVERED, "tpl_order_delivered_v1", message,
                order.getTrackingNumber() != null ? order.getTrackingNumber() : orderRef, order.getCourierPartner(), null);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse sendReturnPickupNotification(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getUser() == null) {
            return null;
        }
        User user = returnRequest.getUser();
        String mobile = user.getMobile();
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        String returnRef = getReturnReference(returnRequest);
        String orderRef = returnRequest.getOrder() != null ? getOrderReference(returnRequest.getOrder()) : "SK-ORD-000000";
        String courier = returnRequest.getReverseCourier() != null ? returnRequest.getReverseCourier() : "Blue Dart Reverse Logistics";
        String awb = returnRequest.getReverseTrackingNumber() != null ? returnRequest.getReverseTrackingNumber() : "REV-AWB-" + returnRef;
        String trackingUrl = buildTrackingUrl(courier, awb);

        String message = String.format(
                "🔄 *Reverse Pickup Scheduled!*\n\n" +
                        "*Namaste %s*, reverse pickup for return claim *#%s* (Order *#%s*) is scheduled.\n\n" +
                        "🚚 *Reverse Courier Partner:* %s\n" +
                        "🔖 *Return AWB:* %s\n\n" +
                        "📦 *Handover Instructions:*\n" +
                        "• Keep the saree securely packed in the original box with Silk Mark tags.\n" +
                        "• The courier partner will inspect exterior seal before issuing pickup receipt.\n\n" +
                        "Track Reverse Shipment:\n%s",
                user.getFirstName(), returnRef, orderRef,
                courier, awb, trackingUrl
        );

        return processAndPersist(returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : null,
                returnRequest.getId(), user, user.getFirstName(), mobile,
                WhatsAppEventType.RETURN_PICKUP, "tpl_return_pickup_v1", message, awb, courier, trackingUrl);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse simulateManualDispatch(WhatsAppDispatchSimulationRequest request, User staff) {
        if (request == null) {
            throw new BadRequestException("Dispatch simulation request must not be null");
        }

        User targetUser = null;
        Order order = null;
        ReturnRequest returnRequest = null;

        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId()).orElse(null);
            if (order != null) {
                targetUser = order.getUser();
            }
        }

        if (targetUser == null && request.getReturnRequestId() != null) {
            returnRequest = returnRequestRepository.findById(request.getReturnRequestId()).orElse(null);
            if (returnRequest != null) {
                targetUser = returnRequest.getUser();
            }
        }

        if (targetUser == null) {
            targetUser = staff != null ? staff : userRepository.findAll().stream().findFirst().orElseThrow(
                    () -> new BadRequestException("No valid user available for simulation")
            );
        }

        String recipientName = request.getRecipientName() != null && !request.getRecipientName().isBlank()
                ? request.getRecipientName()
                : targetUser.getFirstName();

        String message;
        if (request.getCustomMessage() != null && !request.getCustomMessage().isBlank()) {
            message = request.getCustomMessage();
        } else {
            message = buildTemplateMessageForEvent(request.getEventType(), recipientName, request.getOrderId(),
                    request.getCourierPartner(), request.getTrackingNumber());
        }

        String trackingUrl = buildTrackingUrl(request.getCourierPartner(), request.getTrackingNumber());

        return processAndPersist(request.getOrderId(), request.getReturnRequestId(), targetUser,
                recipientName, request.getRecipientPhone(), request.getEventType(),
                "tpl_simulated_" + request.getEventType().name().toLowerCase(),
                message, request.getTrackingNumber(), request.getCourierPartner(), trackingUrl);
    }

    @Override
    @Transactional
    public WhatsAppNotificationResponse resendNotification(Long logId, User staff) {
        WhatsAppNotificationLog existing = notificationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsAppNotificationLog", "id", logId));

        WhatsAppNotificationLog retryLog = WhatsAppNotificationLog.builder()
                .orderId(existing.getOrderId())
                .returnRequestId(existing.getReturnRequestId())
                .userId(existing.getUserId())
                .recipientName(existing.getRecipientName())
                .recipientPhone(existing.getRecipientPhone())
                .eventType(existing.getEventType())
                .templateName(existing.getTemplateName())
                .messageContent(existing.getMessageContent())
                .trackingNumber(existing.getTrackingNumber())
                .courierPartner(existing.getCourierPartner())
                .trackingUrl(existing.getTrackingUrl())
                .deliveryStatus("SENT")
                .simulated(isLiveConfigured() ? false : true)
                .build();

        if (isLiveConfigured()) {
            try {
                whatsAppApiClient.sendTextMessage(retryLog.getRecipientPhone(), retryLog.getMessageContent());
                retryLog.setDeliveryStatus("DELIVERED");
            } catch (Exception e) {
                log.error("Failed to resend live WhatsApp message to {}: {}", retryLog.getRecipientPhone(), e.getMessage());
                retryLog.setDeliveryStatus("FAILED");
            }
        } else {
            retryLog.setDeliveryStatus("SIMULATED");
        }

        WhatsAppNotificationLog saved = notificationLogRepository.save(retryLog);
        log.info("Staff {} resent WhatsApp notification log #{} as new log #{}", staff != null ? staff.getEmail() : "system", logId, saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatsAppNotificationResponse> getOrderNotifications(Long orderId, User user) {
        if (orderId == null) {
            throw new BadRequestException("Order ID is required");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (user != null && !isStaff(user) && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not authorized to view WhatsApp logs for this order");
        }

        return notificationLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WhatsAppNotificationResponse> getAllLogs(String eventType, Pageable pageable) {
        Page<WhatsAppNotificationLog> page;
        if (eventType != null && !eventType.isBlank() && !"ALL".equalsIgnoreCase(eventType)) {
            try {
                WhatsAppEventType type = WhatsAppEventType.valueOf(eventType.toUpperCase());
                page = notificationLogRepository.findByEventTypeOrderByCreatedAtDesc(type, pageable);
            } catch (IllegalArgumentException e) {
                page = notificationLogRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            page = notificationLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsAppTelemetryResponse getTelemetry() {
        long total = notificationLogRepository.count();
        long simulated = notificationLogRepository.countBySimulated(true);
        long live = notificationLogRepository.countBySimulated(false);
        long delivered = notificationLogRepository.countDelivered();
        long failed = notificationLogRepository.countFailed();

        double deliveryRate = total > 0 ? ((double) (delivered + simulated) / total) * 100.0 : 100.0;

        Map<String, Long> eventBreakdown = new LinkedHashMap<>();
        for (WhatsAppEventType type : WhatsAppEventType.values()) {
            eventBreakdown.put(type.name(), notificationLogRepository.countByEventType(type));
        }

        List<WhatsAppNotificationResponse> recent = notificationLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return WhatsAppTelemetryResponse.builder()
                .totalDispatched(total)
                .simulatedCount(simulated)
                .liveCount(live)
                .deliveredCount(delivered)
                .failedCount(failed)
                .deliveryRatePercent(Math.round(deliveryRate * 10.0) / 10.0)
                .eventBreakdown(eventBreakdown)
                .recentLogs(recent)
                .build();
    }

    @Override
    @Transactional
    public boolean updateOptIn(User user, boolean optIn) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("User must be authenticated to update WhatsApp preference");
        }
        User persistentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        persistentUser.setWhatsappOptIn(optIn);
        userRepository.save(persistentUser);
        log.info("Updated WhatsApp opt-in preference for user {} to {}", user.getEmail(), optIn);
        return optIn;
    }

    // --- Internal Processing & Helper Methods ---

    private WhatsAppNotificationResponse processAndPersist(
            Long orderId, Long returnRequestId, User user, String recipientName, String phone,
            WhatsAppEventType eventType, String templateName, String content,
            String trackingNumber, String courier, String trackingUrl) {

        String normalizedPhone = (identityService != null && phone != null) ? identityService.normalizePhoneNumber(phone) : (phone != null ? phone.replaceAll("\\D", "") : "");
        boolean contactOptedOut = false;
        if (contactRepository != null && phone != null) {
            Optional<WhatsAppContact> contactOpt = contactRepository.findByPhoneNumber(phone)
                    .or(() -> contactRepository.findByPhoneNumber(normalizedPhone));
            if (contactOpt.isPresent() && Boolean.FALSE.equals(contactOpt.get().getOptedIn())) {
                contactOptedOut = true;
            }
        }
        boolean userOptedOut = (user != null && Boolean.FALSE.equals(user.getWhatsappOptIn()));
        boolean isOptedIn = !contactOptedOut && !userOptedOut;

        if (!isOptedIn) {
            log.info("Recipient {} (user={}) has opted out of WhatsApp updates; suppressing message", phone, user != null ? user.getEmail() : "guest");
            WhatsAppNotificationLog suppressedLog = WhatsAppNotificationLog.builder()
                    .orderId(orderId)
                    .returnRequestId(returnRequestId)
                    .userId(user != null ? user.getId() : 0L)
                    .recipientName(recipientName)
                    .recipientPhone(phone)
                    .eventType(eventType)
                    .templateName(templateName)
                    .messageContent(content)
                    .trackingNumber(trackingNumber)
                    .courierPartner(courier)
                    .trackingUrl(trackingUrl)
                    .deliveryStatus("SUPPRESSED")
                    .simulated(true)
                    .build();
            return toResponse(notificationLogRepository.save(suppressedLog));
        }

        boolean live = isLiveConfigured();
        String status = "SENT";

        if (live) {
            try {
                whatsAppApiClient.sendTextMessage(phone, content);
                status = "DELIVERED";
                log.info("Live WhatsApp message dispatched to {}", phone);
            } catch (Exception e) {
                log.error("Live WhatsApp dispatch to {} failed: {}", phone, e.getMessage());
                status = "FAILED";
            }
        } else {
            status = "SIMULATED";
            log.info("Simulated WhatsApp dispatch for event {} to {}", eventType, phone);
        }

        WhatsAppNotificationLog logEntity = WhatsAppNotificationLog.builder()
                .orderId(orderId)
                .returnRequestId(returnRequestId)
                .userId(user != null ? user.getId() : 0L)
                .recipientName(recipientName)
                .recipientPhone(phone)
                .eventType(eventType)
                .templateName(templateName)
                .messageContent(content)
                .trackingNumber(trackingNumber)
                .courierPartner(courier)
                .trackingUrl(trackingUrl)
                .deliveryStatus(status)
                .simulated(!live)
                .build();

        WhatsAppNotificationLog saved = notificationLogRepository.save(logEntity);
        return toResponse(saved);
    }

    private boolean isLiveConfigured() {
        return apiToken != null && !apiToken.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    private String resolveMobile(User user, Order order) {
        if (order != null && order.getShippingAddress() != null && order.getShippingAddress().getPhone() != null && !order.getShippingAddress().getPhone().isBlank()) {
            return order.getShippingAddress().getPhone();
        }
        return user.getMobile();
    }

    private String buildTrackingUrl(String courier, String awb) {
        if (awb == null || awb.isBlank()) return null;
        if (courier != null && courier.toLowerCase().contains("blue dart")) {
            return "https://www.bluedart.com/tracking?awb=" + awb;
        } else if (courier != null && courier.toLowerCase().contains("delhivery")) {
            return "https://www.delhivery.com/track/package/" + awb;
        } else if (courier != null && courier.toLowerCase().contains("dtdc")) {
            return "https://www.dtdc.in/tracking?awb=" + awb;
        }
        return "https://sareekart.com/track/" + awb;
    }

    private String buildTemplateMessageForEvent(WhatsAppEventType event, String name, Long orderId, String courier, String awb) {
        long oid = orderId != null ? orderId : 1001L;
        String cur = courier != null ? courier : "Blue Dart Apex Air";
        String trk = awb != null ? awb : "BD-882194";
        String url = buildTrackingUrl(cur, trk);

        return switch (event) {
            case ORDER_CONFIRMED -> String.format(
                    "🙏 *Namaste %s!*\n\nYour heirloom order *#%d* has been confirmed at SareeKart.\nArtisan weavers are preparing your drape with the official *Silk Mark India* seal.\n\nTrack: https://sareekart.com/orders/%d",
                    name, oid, oid);
            case SHIPPED -> String.format(
                    "📦 *Heirloom Saree Dispatched!*\n\n*Namaste %s*, order *#%d* is en route via %s.\nAWB: %s\nTrack: %s",
                    name, oid, cur, trk, url);
            case OUT_FOR_DELIVERY -> String.format(
                    "🚚 *Out for Delivery!*\n\n*Namaste %s*, your SareeKart package for order *#%d* will be delivered today. Please inspect the tamper-proof seal.",
                    name, oid);
            case DELIVERED -> String.format(
                    "🌸 *Delivered with Reverence!*\n\n*Namaste %s*, order *#%d* has been safely delivered. Care guide: https://sareekart.com/saree-care",
                    name, oid);
            case RETURN_PICKUP -> String.format(
                    "🔄 *Reverse Pickup Scheduled!*\n\n*Namaste %s*, reverse pickup for order *#%d* is assigned to %s (AWB: %s).",
                    name, oid, cur, trk);
            case MANUAL_CONCIERGE -> String.format(
                    "✨ *SareeKart Luxury Concierge*\n\n*Namaste %s*, our drape stylist is standing by to assist with your recent selection. How may we serve you?",
                    name);
        };
    }

    private boolean isStaff(User user) {
        return user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER);
    }

    private WhatsAppNotificationResponse toResponse(WhatsAppNotificationLog entity) {
        if (entity == null) return null;
        return WhatsAppNotificationResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .returnRequestId(entity.getReturnRequestId())
                .userId(entity.getUserId())
                .recipientName(entity.getRecipientName())
                .recipientPhone(entity.getRecipientPhone())
                .eventType(entity.getEventType())
                .templateName(entity.getTemplateName())
                .messageContent(entity.getMessageContent())
                .trackingNumber(entity.getTrackingNumber())
                .courierPartner(entity.getCourierPartner())
                .trackingUrl(entity.getTrackingUrl())
                .deliveryStatus(entity.getDeliveryStatus())
                .simulated(entity.getSimulated())
                .createdAtFormatted(entity.getCreatedAt() != null ? entity.getCreatedAt().format(DATE_FORMATTER) : "Just now")
                .build();
    }

    private String getOrderReference(Order order) {
        if (order == null) return "SK-ORD-000000";
        return String.format("SK-ORD-%06d", order.getId() != null ? order.getId() : 0);
    }

    private String getReturnReference(ReturnRequest returnRequest) {
        if (returnRequest == null) return "SK-RET-000000";
        return String.format("SK-RET-%06d", returnRequest.getId() != null ? returnRequest.getId() : 0);
    }
}
