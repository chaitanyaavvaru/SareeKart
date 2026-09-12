package com.sareekart.service.impl;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.*;
import com.sareekart.enums.RefundMode;
import com.sareekart.enums.ReturnReason;
import com.sareekart.enums.ReturnStatus;
import com.sareekart.enums.ReturnType;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.NotificationEventService;
import com.sareekart.service.ReturnService;
import com.sareekart.service.WalletService;
import com.sareekart.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationEventService notificationEventService;
    private final WalletService walletService;
    private final WhatsAppNotificationService whatsAppNotificationService;

    @Override
    @Transactional
    public ReturnResponse createReturnRequest(ReturnCreateRequest request, Long userId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        // 1. Order ownership check
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to request a return for this order.");
        }

        return processReturnCreation(request, order);
    }

    @Override
    @Transactional
    public ReturnResponse createReturnRequest(ReturnCreateRequest request, User customer) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        Long customerId = customer != null ? customer.getId() : null;
        if (order.getUser() == null || !order.getUser().getId().equals(customerId)) {
            throw new BadRequestException("You are not authorized to request a return for this order.");
        }

        return processReturnCreation(request, order);
    }

    private ReturnResponse processReturnCreation(ReturnCreateRequest request, Order order) {
        // 2. DELIVERED status check
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders are eligible for return or exchange. Current order status: " + order.getStatus());
        }

        // 3. 7-day post-delivery cutoff check
        LocalDateTime deliveryTime = resolveDeliveryTimestamp(order);
        LocalDateTime cutoff = deliveryTime.plusDays(7);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BadRequestException("Return window has expired. Orders are only eligible for return or exchange within 7 calendar days of delivery.");
        }

        // 4. Duplicate check
        if (returnRequestRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("A return or exchange request has already been submitted for Order #" + order.getId() + ".");
        }

        // 5. Exchange SKU validation
        String typeStr = request.getType() != null ? request.getType().trim().toUpperCase() : "RETURN";
        if ("EXCHANGE".equals(typeStr)) {
            if (request.getExchangeSku() == null || request.getExchangeSku().trim().isEmpty()) {
                throw new BadRequestException("Exchange SKU is required when selecting saree exchange.");
            }
        }

        // 6. Defect images count validation
        if (request.getImages() != null && request.getImages().size() > 3) {
            throw new BadRequestException("Maximum 3 defect photos allowed.");
        }

        // Parse enums safely
        ReturnType returnType = ReturnType.RETURN;
        try {
            returnType = ReturnType.valueOf(typeStr);
        } catch (Exception ignored) {}

        ReturnReason returnReason = ReturnReason.OTHER;
        if (request.getReason() != null) {
            try {
                returnReason = ReturnReason.valueOf(request.getReason().trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        RefundMode refundMode = RefundMode.ORIGINAL_PAYMENT;
        if (request.getRefundMode() != null) {
            try {
                refundMode = RefundMode.valueOf(request.getRefundMode().trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount()
                : (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .user(order.getUser())
                .type(returnType)
                .reason(returnReason)
                .refundMode(refundMode)
                .comments(request.getComments())
                .exchangeSku(request.getExchangeSku())
                .refundAmount(refundAmount)
                .images(request.getImages() != null ? new ArrayList<>(request.getImages()) : new ArrayList<>())
                .status(ReturnStatus.PENDING)
                .build();

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // Fail-safe notification dispatch
        try {
            if (notificationEventService != null) {
                notificationEventService.notifyReturnSubmitted(saved, order, order.getUser());
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch return submission notification: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getMyReturnRequests(Long userId) {
        return returnRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getMyReturnRequests(User customer) {
        Long userId = customer != null ? customer.getId() : null;
        if (userId == null) {
            return Collections.emptyList();
        }
        return getMyReturnRequests(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturnRequestByOrderId(Long orderId, Long userId) {
        Optional<ReturnRequest> opt = returnRequestRepository.findByOrderId(orderId);
        if (opt.isEmpty()) {
            return null;
        }
        ReturnRequest returnRequest = opt.get();
        if (userId != null && returnRequest.getUser() != null && !returnRequest.getUser().getId().equals(userId)) {
            User caller = userRepository != null ? userRepository.findById(userId).orElse(null) : null;
            if (caller == null || caller.getRole() == Role.CUSTOMER) {
                throw new AccessDeniedException("Not authorised to perform this action");
            }
        }
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturnRequestByOrderId(Long orderId, User user) {
        Optional<ReturnRequest> opt = returnRequestRepository.findByOrderId(orderId);
        if (opt.isEmpty()) {
            return null;
        }
        ReturnRequest returnRequest = opt.get();
        if (user != null && returnRequest.getUser() != null && !returnRequest.getUser().getId().equals(user.getId())) {
            if (user.getRole() == Role.CUSTOMER) {
                throw new AccessDeniedException("Not authorised to perform this action");
            }
        }
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturnRequestById(Long returnId, Long userId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", returnId));

        if (userId != null && returnRequest.getUser() != null && !returnRequest.getUser().getId().equals(userId)) {
            User caller = userRepository != null ? userRepository.findById(userId).orElse(null) : null;
            if (caller == null || caller.getRole() == Role.CUSTOMER) {
                throw new AccessDeniedException("Not authorised to perform this action");
            }
        }
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturnRequestById(Long returnId, User user) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", returnId));

        if (user != null && returnRequest.getUser() != null && !returnRequest.getUser().getId().equals(user.getId())) {
            if (user.getRole() == Role.CUSTOMER) {
                throw new AccessDeniedException("Not authorised to perform this action");
            }
        }
        return toResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getAllReturnsForAdmin(String statusFilter) {
        List<ReturnRequest> list;
        if (statusFilter == null || statusFilter.trim().isEmpty() || "ALL".equalsIgnoreCase(statusFilter.trim())) {
            list = returnRequestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            try {
                ReturnStatus statusEnum = ReturnStatus.valueOf(statusFilter.trim().toUpperCase());
                list = returnRequestRepository.findByStatusOrderByCreatedAtDesc(statusEnum);
            } catch (IllegalArgumentException e) {
                list = returnRequestRepository.findAllByOrderByCreatedAtDesc();
            }
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResponse> getAllReturnsForAdmin(String statusFilter, User staffUser) {
        validateStaffRole(staffUser);
        return getAllReturnsForAdmin(statusFilter);
    }

    @Override
    @Transactional
    public ReturnResponse updateReturnStatus(Long returnId, ReturnStatusUpdateRequest request, User staffUser) {
        // 1. Staff role validation
        validateStaffRole(staffUser);

        // 2. Fetch claim
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", returnId));

        String targetStatusStr = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
        if (targetStatusStr.isEmpty()) {
            throw new BadRequestException("Target status is mandatory.");
        }

        ReturnStatus currentStatus = returnRequest.getStatus() != null ? returnRequest.getStatus() : ReturnStatus.PENDING;

        // 3. Terminal state checks
        if (currentStatus == ReturnStatus.COMPLETED) {
            throw new BadRequestException("Cannot alter status of an already completed return request.");
        }
        if (currentStatus == ReturnStatus.REJECTED) {
            throw new BadRequestException("Cannot alter status of an already rejected return request.");
        }

        // 4. Handle REJECTED transition
        if ("REJECTED".equals(targetStatusStr)) {
            if (request.getAdminNotes() == null || request.getAdminNotes().trim().isEmpty()) {
                throw new BadRequestException("Mandatory rejection reason must be provided in admin notes.");
            }
            returnRequest.setStatus(ReturnStatus.REJECTED);
            returnRequest.setAdminNotes(request.getAdminNotes().trim());
        }
        // 5. Status-specific forward progressions
        else if (currentStatus == ReturnStatus.PENDING) {
            if ("APPROVED".equals(targetStatusStr)) {
                returnRequest.setStatus(ReturnStatus.APPROVED);
                if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                    returnRequest.setAdminNotes(request.getAdminNotes().trim());
                }
            } else {
                throw new BadRequestException("Invalid state transition from PENDING to " + targetStatusStr + ". Claim must first be APPROVED.");
            }
        } else if (currentStatus == ReturnStatus.APPROVED) {
            if ("PICKUP_SCHEDULED".equals(targetStatusStr)) {
                if (request.getReverseCourier() == null || request.getReverseCourier().trim().isEmpty() ||
                        request.getReverseTrackingNumber() == null || request.getReverseTrackingNumber().trim().isEmpty()) {
                    throw new BadRequestException("Reverse courier and tracking number are required to schedule pickup.");
                }
                returnRequest.setStatus(ReturnStatus.PICKUP_SCHEDULED);
                returnRequest.setReverseCourier(request.getReverseCourier().trim());
                returnRequest.setReverseTrackingNumber(request.getReverseTrackingNumber().trim());
                if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                    returnRequest.setAdminNotes(request.getAdminNotes().trim());
                }
            } else {
                throw new BadRequestException("Invalid state transition from APPROVED to " + targetStatusStr);
            }
        } else if (currentStatus == ReturnStatus.PICKUP_SCHEDULED) {
            if ("COMPLETED".equals(targetStatusStr)) {
                returnRequest.setStatus(ReturnStatus.COMPLETED);
                if (request.getRefundAmount() != null) {
                    returnRequest.setRefundAmount(request.getRefundAmount());
                }
                if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                    returnRequest.setAdminNotes(request.getAdminNotes().trim());
                }

                // If refund mode is STORE_CREDIT, automatically disburse to customer wallet with 5% bonus
                if (returnRequest.getRefundMode() == RefundMode.STORE_CREDIT && walletService != null) {
                    BigDecimal creditAmt = returnRequest.getRefundAmount() != null
                            ? returnRequest.getRefundAmount()
                            : (returnRequest.getOrder() != null ? returnRequest.getOrder().getTotalAmount() : BigDecimal.ZERO);
                    if (creditAmt != null && creditAmt.compareTo(BigDecimal.ZERO) > 0 && returnRequest.getUser() != null) {
                        try {
                            walletService.creditReturnRefund(returnRequest.getUser().getId(), creditAmt, returnRequest.getId());
                        } catch (Exception e) {
                            log.error("Failed to credit store credit wallet for return #{}: {}", returnRequest.getId(), e.getMessage());
                        }
                    }
                }
            } else {
                throw new BadRequestException("Invalid state transition from PICKUP_SCHEDULED to " + targetStatusStr);
            }
        } else {
            throw new BadRequestException("Unknown or invalid current status: " + currentStatus);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // Dispatch WhatsApp reverse pickup alert if pickup scheduled
        if (saved.getStatus() == ReturnStatus.PICKUP_SCHEDULED && whatsAppNotificationService != null) {
            try {
                whatsAppNotificationService.sendReturnPickupNotification(saved);
            } catch (Exception e) {
                log.warn("Failed to dispatch WhatsApp reverse pickup alert: {}", e.getMessage());
            }
        }

        // Fail-safe status update notification
        try {
            if (notificationEventService != null) {
                notificationEventService.notifyReturnStatusUpdated(saved);
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch return status notification: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    private void validateStaffRole(User user) {
        if (user == null || user.getRole() == null || user.getRole() == Role.CUSTOMER) {
            throw new AccessDeniedException("Not authorised to perform this action");
        }
    }

    private LocalDateTime resolveDeliveryTimestamp(Order order) {
        if (order.getDeliveredAt() != null) {
            return order.getDeliveredAt();
        }
        if (order.getUpdatedAt() != null) {
            return order.getUpdatedAt();
        }
        if (order.getCreatedAt() != null) {
            return order.getCreatedAt();
        }
        return LocalDateTime.now();
    }

    private ReturnResponse toResponse(ReturnRequest entity) {
        if (entity == null) {
            return null;
        }

        Order order = entity.getOrder();
        User user = entity.getUser();

        LocalDateTime deliveredTime = order != null ? resolveDeliveryTimestamp(order) : null;
        Long daysSinceDelivery = null;
        if (deliveredTime != null) {
            daysSinceDelivery = ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
        }

        String customerName = null;
        String customerEmail = null;
        String customerMobile = null;
        if (user != null) {
            customerEmail = user.getEmail();
            customerMobile = user.getMobile();
            customerName = (user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "")).trim();
        }

        return ReturnResponse.builder()
                .id(entity.getId())
                .orderId(order != null ? order.getId() : entity.getOrderId())
                .userId(user != null ? user.getId() : entity.getUserId())
                .customerEmail(customerEmail)
                .customerName(customerName)
                .customerMobile(customerMobile)
                .orderTotalAmount(order != null ? order.getTotalAmount() : null)
                .refundAmount(entity.getRefundAmount())
                .refundMode(entity.getRefundMode() != null ? entity.getRefundMode().name() : null)
                .type(entity.getType() != null ? entity.getType().name() : null)
                .reason(entity.getReason() != null ? entity.getReason().name() : null)
                .comments(entity.getComments())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .exchangeSku(entity.getExchangeSku())
                .images(entity.getImages() != null ? entity.getImages() : Collections.emptyList())
                .reverseCourier(entity.getReverseCourier())
                .reverseTrackingNumber(entity.getReverseTrackingNumber())
                .adminNotes(entity.getAdminNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .orderDeliveredAt(deliveredTime)
                .daysSinceDelivery(daysSinceDelivery)
                .build();
    }
}
