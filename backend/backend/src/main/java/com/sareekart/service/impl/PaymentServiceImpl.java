package com.sareekart.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.PaymentConfigurationException;
import com.sareekart.exception.PaymentGatewayException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.PaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    private RazorpayClient razorpayClient;

    // Testing isolation setters
    public void setRazorpayClient(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @PostConstruct
    public void init() {
        if (isBlank(keyId) || isBlank(keySecret)) {
            log.warn("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET before accepting online payments.");
            return;
        }

        try {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize RazorpayClient: {}", e.getMessage());
        }
    }

    @Override
    public PaymentOrderResponse createRazorpayOrder(Long orderId, Long userId) {
        ensureConfigured();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this order.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is already paid or cancelled.");
        }

        try {
            // Amount in paise
            BigDecimal amountInRupees = order.getTotalAmount();
            int amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_order_" + orderId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Save Razorpay Order ID in order entity
            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            return PaymentOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amountInRupees)
                    .currency("INR")
                    .keyId(keyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new PaymentGatewayException(toGatewayMessage(e));
        }
    }

    @Override
    public OrderResponse verifyPaymentSignature(PaymentVerificationRequest request, Long userId) {
        ensureConfigured();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized transaction verification request.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order has already been cancelled.");
        }

        // Idempotency: If order was already confirmed and completed, return existing confirmed order
        if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus()) && order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order #{} payment signature verification called for already confirmed order. Returning idempotently.", order.getId());
            return orderMapper.toResponse(order);
        }

        // Cross-order mismatch protection: verify that request razorpayOrderId matches order.razorpayOrderId
        if (order.getRazorpayOrderId() == null || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new BadRequestException("Transaction verification failed: Razorpay order ID mismatch.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isVerified = Utils.verifyPaymentSignature(options, keySecret);

            if (isVerified) {
                order.setPaymentStatus("COMPLETED");
                order.setStatus(OrderStatus.CONFIRMED);
                order.setRazorpayPaymentId(request.getRazorpayPaymentId());
                Order updatedOrder = orderRepository.save(order);
                return orderMapper.toResponse(updatedOrder);
            } else {
                throw new BadRequestException("Payment signature verification failed. Transaction was not secure.");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification failed: {}", e.getMessage());
            throw new PaymentGatewayException(toGatewayMessage(e));
        }
    }

    @Override
    public boolean processWebhook(String payload, String signatureHeader) {
        if (isBlank(webhookSecret)) {
            log.warn("Razorpay webhook received but RAZORPAY_WEBHOOK_SECRET is not configured.");
            throw new PaymentConfigurationException("Razorpay webhook secret is not configured.");
        }

        if (isBlank(signatureHeader)) {
            throw new BadRequestException("Missing X-Razorpay-Signature header.");
        }

        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            if (!isValid) {
                throw new BadRequestException("Invalid webhook signature.");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay webhook signature verification error: {}", e.getMessage());
            throw new BadRequestException("Invalid webhook signature: " + e.getMessage());
        }

        JSONObject json = new JSONObject(payload);
        String event = json.optString("event");
        JSONObject payloadObj = json.optJSONObject("payload");
        if (payloadObj == null) {
            log.warn("Webhook payload missing 'payload' object for event: {}", event);
            return true;
        }

        if ("order.paid".equalsIgnoreCase(event) || "payment.captured".equalsIgnoreCase(event)) {
            String razorpayOrderId = null;
            String razorpayPaymentId = null;

            JSONObject paymentObj = payloadObj.optJSONObject("payment");
            if (paymentObj != null) {
                JSONObject paymentEntity = paymentObj.optJSONObject("entity");
                if (paymentEntity != null) {
                    razorpayPaymentId = paymentEntity.optString("id");
                    razorpayOrderId = paymentEntity.optString("order_id");
                }
            }

            if (isBlank(razorpayOrderId)) {
                JSONObject orderObj = payloadObj.optJSONObject("order");
                if (orderObj != null) {
                    JSONObject orderEntity = orderObj.optJSONObject("entity");
                    if (orderEntity != null) {
                        razorpayOrderId = orderEntity.optString("id");
                    }
                }
            }

            if (!isBlank(razorpayOrderId)) {
                Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(razorpayOrderId);
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus()) && order.getStatus() == OrderStatus.CONFIRMED) {
                        log.info("Webhook duplicate replay for already confirmed order #{} (Razorpay Order: {})", order.getId(), razorpayOrderId);
                        return true;
                    }
                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        log.warn("Received payment webhook for already cancelled order #{} (Razorpay Order: {})", order.getId(), razorpayOrderId);
                        return true;
                    }

                    order.setPaymentStatus("COMPLETED");
                    order.setStatus(OrderStatus.CONFIRMED);
                    if (!isBlank(razorpayPaymentId)) {
                        order.setRazorpayPaymentId(razorpayPaymentId);
                    }
                    orderRepository.save(order);
                    log.info("Order #{} successfully confirmed via Razorpay webhook '{}'", order.getId(), event);
                } else {
                    log.warn("No order found matching Razorpay Order ID: {}", razorpayOrderId);
                }
            }
            return true;
        } else if ("payment.failed".equalsIgnoreCase(event)) {
            JSONObject paymentObj = payloadObj.optJSONObject("payment");
            if (paymentObj != null) {
                JSONObject paymentEntity = paymentObj.optJSONObject("entity");
                if (paymentEntity != null) {
                    String razorpayOrderId = paymentEntity.optString("order_id");
                    if (!isBlank(razorpayOrderId)) {
                        Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(razorpayOrderId);
                        if (orderOpt.isPresent()) {
                            Order order = orderOpt.get();
                            if (order.getStatus() == OrderStatus.PENDING) {
                                order.setPaymentStatus("FAILED");
                                orderRepository.save(order);
                                log.info("Order #{} payment status marked FAILED via webhook", order.getId());
                            }
                        }
                    }
                }
            }
            return true;
        }

        return true;
    }

    private void ensureConfigured() {
        if (razorpayClient == null || isBlank(keyId) || isBlank(keySecret)) {
            throw new PaymentConfigurationException(
                    "Online payment is temporarily unavailable. Configure RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET on the backend."
            );
        }
    }

    private String toGatewayMessage(RazorpayException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (message.contains("authentication") || message.contains("auth") || message.contains("credentials")) {
            return "Razorpay authentication failed. Use a matching test or live key ID and secret in RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.";
        }
        return "Razorpay could not process the payment request. Please try again or choose Cash on Delivery.";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

