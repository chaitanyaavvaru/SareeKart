package com.sareekart.service;

import com.sareekart.entity.Notification;
import com.sareekart.entity.Order;
import com.sareekart.entity.ReturnRequest;
import com.sareekart.entity.Review;
import com.sareekart.entity.StockTransfer;
import com.sareekart.entity.User;
import com.sareekart.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification notifyOrderPlaced(Order order) {
        // Customer notification
        Notification customerNotif = Notification.builder()
                .userId(order.getUser().getId())
                .title("Order Confirmed #" + order.getId())
                .message("Your royal handloom drape order #" + order.getId() + " has been placed for ₹" + order.getTotalAmount() + ". Preparing for dispatch.")
                .type("ORDER_PLACED")
                .linkUrl("/orders")
                .build();
        notificationRepository.save(customerNotif);

        // Staff notification
        Notification staffNotif = Notification.builder()
                .targetRole("MANAGER")
                .title("New Order Received #" + order.getId())
                .message("Order #" + order.getId() + " placed by " + order.getUser().getEmail() + " for ₹" + order.getTotalAmount() + ".")
                .type("ORDER_PLACED")
                .linkUrl("/admin/orders")
                .build();
        notificationRepository.save(staffNotif);

        log.info("Dispatched order placed notifications for order #{}", order.getId());
        return customerNotif;
    }

    @Transactional
    public Notification notifyOrderDispatched(Order order, String awb, String courier) {
        Notification notif = Notification.builder()
                .userId(order.getUser().getId())
                .title("Drape Dispatched #" + order.getId())
                .message("Your order has been handed to " + courier + ". AWB Tracking Number: " + awb + ".")
                .type("SHIPMENT_DISPATCHED")
                .linkUrl("/track-order?awb=" + awb)
                .build();
        log.info("Dispatched shipment telemetry alert for order #{}", order.getId());
        return notificationRepository.save(notif);
    }

    @Transactional
    public Notification notifyOrderDelivered(Order order) {
        Notification notif = Notification.builder()
                .userId(order.getUser().getId())
                .title("Order Delivered #" + order.getId())
                .message("Your heirloom saree has been safely delivered. We invite you to share a review.")
                .type("ORDER_DELIVERED")
                .linkUrl("/orders")
                .build();
        log.info("Dispatched delivery confirmation for order #{}", order.getId());
        return notificationRepository.save(notif);
    }

    @Transactional
    public Notification notifyLowStock(String sku, String productName, int available) {
        Notification notif = Notification.builder()
                .targetRole("MANAGER")
                .title("Low Stock Warning: " + sku)
                .message("SKU " + sku + " (" + productName + ") has dropped to " + available + " units on hand. Rebalance or issue purchase order.")
                .type("LOW_STOCK")
                .linkUrl("/admin/inventory")
                .build();
        log.warn("Created low stock alert for SKU {}", sku);
        return notificationRepository.save(notif);
    }

    @Transactional
    public Notification notifyStockTransferRequested(StockTransfer transfer) {
        Notification notif = Notification.builder()
                .targetRole("OWNER")
                .title("Stock Transfer Request #" + transfer.getId())
                .message(transfer.getRequestedByEmail() + " requested transfer of " + transfer.getQuantity() + " units (" + transfer.getSku() + ") from " + transfer.getSourceWarehouse() + " to " + transfer.getTargetWarehouse() + ".")
                .type("STOCK_TRANSFER")
                .linkUrl("/admin/approvals")
                .build();
        log.info("Created stock transfer maker-checker alert #{}", transfer.getId());
        return notificationRepository.save(notif);
    }

    @Transactional
    public Notification notifyReviewSubmitted(Review review) {
        Notification notif = Notification.builder()
                .targetRole("ADMIN")
                .title("New Review Submitted (" + review.getRating() + "★)")
                .message("Review submitted by " + review.getUserName() + " on product #" + review.getProductId() + ": \"" + review.getComment() + "\"")
                .type("REVIEW_SUBMITTED")
                .linkUrl("/admin/reviews")
                .build();
        log.info("Dispatched review submission alert for product #{}", review.getProductId());
        return notificationRepository.save(notif);
    }

    @Transactional
    public Notification notifyReturnSubmitted(ReturnRequest returnRequest, Order order, User customer) {
        // Customer notification
        Notification customerNotif = Notification.builder()
                .userId(customer != null ? customer.getId() : (returnRequest.getUser() != null ? returnRequest.getUser().getId() : null))
                .title("Return Request Submitted #" + returnRequest.getId())
                .message("Your " + (returnRequest.getType() != null ? returnRequest.getType().name().toLowerCase() : "return") + " request for Order #" + order.getId() + " has been received and is pending review.")
                .type("RETURN_REQUESTED")
                .linkUrl("/orders")
                .build();
        notificationRepository.save(customerNotif);

        // Staff notification
        Notification staffNotif = Notification.builder()
                .targetRole("MANAGER")
                .title("New Return Claim: Order #" + order.getId())
                .message((customer != null ? customer.getEmail() : "Customer") + " submitted a " + (returnRequest.getType() != null ? returnRequest.getType().name() : "RETURN") + " claim for Order #" + order.getId() + " (" + returnRequest.getReason() + ").")
                .type("RETURN_REQUESTED")
                .linkUrl("/admin/returns")
                .build();
        notificationRepository.save(staffNotif);

        log.info("Dispatched return requested notifications for order #{}", order.getId());
        return customerNotif;
    }

    @Transactional
    public Notification notifyReturnStatusUpdated(ReturnRequest returnRequest) {
        Long customerId = returnRequest.getUser() != null ? returnRequest.getUser().getId() : returnRequest.getUserId();
        Long orderId = returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : returnRequest.getOrderId();
        String status = returnRequest.getStatus() != null ? returnRequest.getStatus().name() : "UPDATED";

        String title;
        String message;
        String type;

        switch (status) {
            case "APPROVED":
                title = "Return Claim Approved: Order #" + orderId;
                message = "Your return request for Order #" + orderId + " has been approved. Doorstep pickup will be scheduled shortly.";
                type = "RETURN_APPROVED";
                break;
            case "PICKUP_SCHEDULED":
                title = "Reverse Pickup Scheduled: Order #" + orderId;
                message = "Reverse pickup scheduled via " + returnRequest.getReverseCourier() + " (AWB: " + returnRequest.getReverseTrackingNumber() + ") for Order #" + orderId + ".";
                type = "RETURN_PICKUP_SCHEDULED";
                break;
            case "COMPLETED":
                title = "Return Completed: Order #" + orderId;
                message = "Your return for Order #" + orderId + " is completed. Refund of ₹" + returnRequest.getRefundAmount() + " processed via " + returnRequest.getRefundMode() + ".";
                type = "RETURN_COMPLETED";
                break;
            case "REJECTED":
                title = "Return Request Rejected: Order #" + orderId;
                message = "Your return request for Order #" + orderId + " was rejected. Reason: " + returnRequest.getAdminNotes();
                type = "RETURN_REJECTED";
                break;
            default:
                title = "Return Status Updated: Order #" + orderId;
                message = "Your return claim for Order #" + orderId + " has been updated to " + status + ".";
                type = "RETURN_STATUS_UPDATED";
                break;
        }

        Notification notif = Notification.builder()
                .userId(customerId)
                .title(title)
                .message(message)
                .type(type)
                .linkUrl("/orders")
                .build();

        log.info("Dispatched return status notification ({}) for return claim #{}", status, returnRequest.getId());
        return notificationRepository.save(notif);
    }
}
