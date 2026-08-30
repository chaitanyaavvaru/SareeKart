package com.sareekart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.client.RazorpayGateway;
import com.sareekart.config.RazorpayProperties;
import com.sareekart.dto.payment.PaymentOrderResponse;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.dto.payment.VerifyPaymentRequest;
import com.sareekart.entity.Order;
import com.sareekart.entity.Payment;
import com.sareekart.entity.WebhookEvent;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.exception.ServiceUnavailableException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.PaymentRepository;
import com.sareekart.repository.WebhookEventRepository;
import com.sareekart.security.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Razorpay payment lifecycle.
 *
 * Idempotency model:
 *  - create-order: reuses the stored Payment row's providerOrderId, so
 *    retries never mint a second gateway order.
 *  - verify: pessimistic row lock + PENDING→SUCCESS guard ⇒ exactly-once
 *    inventory commit; repeat calls return current state.
 *  - webhook: unique event_id ledger (webhook_events) dedupes at-least-once
 *    delivery before any state transition is attempted.
 *
 * Amount authority: the payable amount ALWAYS comes from the stored Order
 * (order.totalAmount). Frontend-supplied amounts are ignored everywhere.
 */
@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final RefundService refundService;
    private final WebhookLedgerService webhookLedger;
    private final RazorpayProperties razorpayProperties;
    private final ObjectMapper objectMapper;
    /** Absent in COD-only mode; resolved lazily so optional wiring stays explicit. */
    private final org.springframework.beans.factory.ObjectProvider<RazorpayGateway> gatewayProvider;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          WebhookEventRepository webhookEventRepository,
                          InventoryService inventoryService,
                          CouponService couponService,
                          RefundService refundService,
                          WebhookLedgerService webhookLedger,
                          RazorpayProperties razorpayProperties,
                          ObjectMapper objectMapper,
                          org.springframework.beans.factory.ObjectProvider<RazorpayGateway> gatewayProvider) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.refundService = refundService;
        this.webhookLedger = webhookLedger;
        this.razorpayProperties = razorpayProperties;
        this.objectMapper = objectMapper;
        this.gatewayProvider = gatewayProvider;
    }

    private RazorpayGateway gateway() {
        RazorpayGateway g = gatewayProvider.getIfAvailable();
        if (!razorpayProperties.isEnabled() || g == null) {
            throw new ServiceUnavailableException(
                "Online payments are currently unavailable. Please use Cash on Delivery.");
        }
        return g;
    }

    // ── 1. Create Razorpay order ─────────────────────────────────────────────

    @Transactional
    public PaymentOrderResponse createPaymentOrder(String userEmail, Long orderId) {
        requireEnabled();
        Order order = loadOwnedOrder(userEmail, orderId);
        assertOnlinePending(order);

        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Idempotent: reuse the gateway order we already minted for this SareeKart order.
        if (payment.getProviderOrderId() != null && !payment.getProviderOrderId().isBlank()) {
            log.info("Reusing existing Razorpay order {} for SK order {}",
                mask(payment.getProviderOrderId()), order.getOrderNumber());
            return toResponse(order, payment);
        }

        var rzp = gateway().createOrder(
            String.valueOf(order.getId()),
            order.getTotalAmount().movePointRight(2).longValueExact(), // rupees -> paise
            "INR");

        if (rzp == null || rzp.getId() == null) {
            throw new IllegalStateException("Razorpay returned no order id");
        }

        payment.setProvider("RAZORPAY");
        payment.setProviderOrderId(rzp.getId());
        payment.setAmount(order.getTotalAmount()); // server-authoritative snapshot
        payment.setStatus(PaymentStatus.PENDING);
        appendHistory(payment, null, PaymentStatus.PENDING, "CREATE_ORDER");
        paymentRepository.save(payment);

        log.info("Created Razorpay order {} for SK order {} amount={}",
            mask(rzp.getId()), order.getOrderNumber(), order.getTotalAmount());
        return toResponse(order, payment);
    }

    // ── 2. Verify checkout callback ──────────────────────────────────────────

    @Transactional
    public OrderResponse verifyPayment(String userEmail, VerifyPaymentRequest request) {
        requireEnabled();

        // Lock the payment row so duplicate verifications serialize.
        Payment payment = paymentRepository.findByProviderOrderIdForUpdate(request.getRazorpayOrderId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Payment", "razorpayOrderId", request.getRazorpayOrderId()));

        Order order = payment.getOrder();
        if (!order.getUser().getEmail().equals(userEmail)) {
            // Do not reveal existence of other users' payments.
            throw new BadRequestException("Order does not belong to user");
        }
        if (!order.getId().equals(Long.valueOf(request.getOrderId()))) {
            throw new BadRequestException("Order/payment mismatch");
        }
        // Sweeper-expired (or user-cancelled) orders are dead — reject late
        // captures instead of resurrecting them (Phase 8 hardening).
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getPaymentStatus() == PaymentStatus.FAILED) {
            log.warn("Late verification rejected for non-payable order {}", order.getOrderNumber());
            throw new BadRequestException(
                "This order is no longer payable. Please place a new order.");
        }

        // Already paid → idempotent success, never re-transition or re-decrement stock.
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Duplicate verification ignored for order {}", order.getOrderNumber());
            return OrderResponse.from(order);
        }
        if (payment.getStatus() == PaymentStatus.FAILED
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            return OrderResponse.from(order);
        }

        boolean valid = SignatureUtil.verifyPaymentSignature(
            request.getRazorpayOrderId(),
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature(),
            razorpayProperties.getKeySecret());

        if (!valid) {
            markFailed(payment, "INVALID_SIGNATURE", "VERIFY");
            throw new BadRequestException("Payment signature verification failed");
        }

        applySuccessfulPayment(payment, request.getRazorpayPaymentId(), "VERIFY");
        return OrderResponse.from(order);
    }

    // ── 3. Webhooks ──────────────────────────────────────────────────────────

    /**
     * Handles a Razorpay webhook delivery. Signature-checked against the RAW
     * body; deduplicated by event id; state transitions are the same guarded,
     * exactly-once paths used by /verify.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature, String eventIdHeader) {
        if (!SignatureUtil.verifyWebhookSignature(rawBody, signature, razorpayProperties.getWebhookSecret())) {
            log.warn("Webhook rejected: invalid signature");
            throw new BadRequestException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }

        String eventId = eventIdHeader != null && !eventIdHeader.isBlank()
            ? eventIdHeader
            : deriveEventId(rawBody);
        String eventType = root.path("event").asText("unknown");

        WebhookEvent record = webhookLedger.recordIfFirst(eventId, eventType, rawBody);
        if (record == null) {
            log.info("Webhook {} ({}) already processed — skipping", eventId, eventType);
            return;
        }

        try {
            dispatchWebhookEvent(root);
            webhookLedger.markProcessed(record, null);
        } catch (Exception e) {
            String err = truncate(e.getMessage());
            webhookLedger.markProcessed(record, err);
            log.error("Webhook {} ({}) processing failed: {}", eventId, eventType, err);
        }
    }

    private void dispatchWebhookEvent(JsonNode root) {
        String event = root.path("event").asText("");
        JsonNode paymentNode = root.path("payload").path("payment").path("entity");

        switch (event) {
            case "payment.captured", "order.paid" -> {
                String rzpPaymentId = paymentNode.path("id").asText(null);
                String rzpOrderId = paymentNode.path("order_id").asText(null);
                applyCapturedFromWebhook(rzpOrderId, rzpPaymentId, paymentNode.path("amount").asLong(-1));
            }
            case "refund.processed", "refund.failed" -> {
                JsonNode refundNode = root.path("payload").path("refund").path("entity");
                refundService.applyRefundWebhook(event,
                    refundNode.path("id").asText(null),
                    refundNode.path("payment_id").asText(null),
                    refundNode.path("amount").asLong(-1));
            }
            case "payment.failed" -> {
                String rzpOrderId = paymentNode.path("order_id").asText(null);
                if (rzpOrderId != null) {
                    paymentRepository.findByProviderOrderIdForUpdate(rzpOrderId)
                        .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                        .ifPresent(p -> markFailed(p, "GATEWAY_EVENT", "WEBHOOK"));
                }
            }
            default -> log.debug("Ignoring webhook event type: {}", event);
        }
    }

    private void applyCapturedFromWebhook(String rzpOrderId, String rzpPaymentId, long amountPaise) {
        if (rzpOrderId == null || rzpPaymentId == null) return;

        Optional<Payment> locked = paymentRepository.findByProviderOrderIdForUpdate(rzpOrderId);
        if (locked.isEmpty()) {
            log.warn("Webhook captured event for unknown Razorpay order {}", mask(rzpOrderId));
            return;
        }
        Payment payment = locked.get();

        if (payment.getStatus() == PaymentStatus.PAID) {
            return; // idempotent replay
        }
        // Expired/cancelled while the gateway was still processing: never
        // resurrect the order — automatically return the money instead.
        if (payment.getOrder() != null
                && payment.getOrder().getStatus() == OrderStatus.CANCELLED) {
            log.warn("Late capture on expired order {} — initiating reconciliation refund",
                payment.getOrder().getOrderNumber());
            refundService.reconcileLateCapture(payment, rzpPaymentId, amountPaise);
            return;
        }
        // Defensive amount authority check against our own stored total.
        long expectedPaise = payment.getAmount().movePointRight(2).longValueExact();
        if (amountPaise >= 0 && amountPaise != expectedPaise) {
            log.error("Webhook amount mismatch for order {}: expected {} got {}",
                payment.getOrder().getOrderNumber(), expectedPaise, amountPaise);
            markFailed(payment, "AMOUNT_MISMATCH", "WEBHOOK");
            return;
        }
        applySuccessfulPayment(payment, rzpPaymentId, "WEBHOOK");
    }

    // ── Shared transition (exactly-once semantics live here) ────────────────

    private void applySuccessfulPayment(Payment payment, String razorpayPaymentId, String source) {
        Order order = payment.getOrder();
        PaymentStatus previous = payment.getStatus();

        // Capture pre-mutation state: isCommitted() keys off paymentStatus,
        // so evaluating AFTER setting PAID would wrongly skip the commit.
        boolean alreadyCommitted = inventoryService.isCommitted(order);

        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderPaymentId(razorpayPaymentId);
        appendHistory(payment, previous, PaymentStatus.PAID, source);

        order.setPaymentStatus(PaymentStatus.PAID);
        // Exactly-once stock commit: serialized by the pessimistic row lock
        // taken on the payment before we got here.
        if (!alreadyCommitted) {
            inventoryService.commitForOrder(order);
            couponService.confirmForOrder(order.getId()); // usage counted once
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PROCESSING); // paid orders enter fulfillment
        }

        orderRepository.save(order);
        paymentRepository.save(payment);
        log.info("Payment PAID via {} for order {} (provider payment {}, inventoryCommitted={})",
            source, order.getOrderNumber(), mask(razorpayPaymentId), !alreadyCommitted);
    }

    private void markFailed(Payment payment, String reason, String source) {
        PaymentStatus previous = payment.getStatus();
        boolean wasPendingOrder = payment.getOrder() != null
            && payment.getOrder().getPaymentStatus() == PaymentStatus.PENDING;

        payment.setStatus(PaymentStatus.FAILED);
        appendHistory(payment, previous, PaymentStatus.FAILED, source + ":" + reason);

        Order order = payment.getOrder();
        if (order != null && wasPendingOrder) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            // Failed online payments never consume quota — release the hold.
            couponService.releaseIfReserved(order.getId());
        }
        paymentRepository.save(payment);
        log.warn("Payment FAILED via {} for reason={} order={}",
            source, reason, order != null ? order.getOrderNumber() : "?");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void requireEnabled() {
        if (!razorpayProperties.isEnabled() || gatewayProvider.getIfAvailable() == null) {
            throw new ServiceUnavailableException(
                "Online payments are currently unavailable. Please use Cash on Delivery.");
        }
    }

    private Order loadOwnedOrder(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("Order does not belong to user");
        }
        return order;
    }

    private void assertOnlinePending(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new BadRequestException("This order is not an online-payment order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay a cancelled order");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }
    }

    private PaymentOrderResponse toResponse(Order order, Payment payment) {
        return PaymentOrderResponse.builder()
            .orderId(order.getId())
            .keyId(razorpayProperties.getKeyId()) // public identifier only
            .razorpayOrderId(payment.getProviderOrderId())
            .amount(order.getTotalAmount().movePointRight(2).longValueExact())
            .currency("INR")
            .build();
    }

    private void appendHistory(Payment payment, PaymentStatus from, PaymentStatus to, String by) {
        try {
            String entry = objectMapper.writeValueAsString(java.util.Map.of(
                "from", from != null ? from.name() : "NEW",
                "to", to.name(),
                "by", by,
                "at", LocalDateTime.now().toString()));
            String existing = payment.getStatusTransitionHistory();
            String updated = existing == null || existing.isBlank()
                ? "[" + entry + "]"
                : existing.substring(0, existing.length() - 1) + "," + entry + "]";
            payment.setStatusTransitionHistory(updated);
        } catch (Exception e) {
            log.warn("Failed to append payment history: {}", e.getMessage());
        }
    }

    /** Never log full provider ids/secrets — keep last 6 chars for correlation. */
    private String mask(String value) {
        if (value == null || value.length() <= 6) return "***";
        return "***" + value.substring(value.length() - 6);
    }

    private String deriveEventId(String rawBody) {
        return "sha256-" + SignatureUtil.hmacSha256Hex(rawBody, "dedupe");
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}