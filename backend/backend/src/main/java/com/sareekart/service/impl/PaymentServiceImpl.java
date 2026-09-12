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

    private RazorpayClient razorpayClient;

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
