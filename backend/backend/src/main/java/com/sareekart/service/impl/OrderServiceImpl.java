package com.sareekart.service.impl;

import com.sareekart.dto.request.AddressRequest;
import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.dto.response.PincodeLookupResponse;
import com.sareekart.service.LogisticsService;
import com.sareekart.service.OrderNotificationService;
import com.sareekart.service.OrderService;
import com.sareekart.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService notificationService;
    private final WalletService walletService;
    private final LogisticsService logisticsService;

    @Override
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty.");
        }

        // Validate stock
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName() + 
                        ". Available stock: " + product.getStockQuantity());
            }
        }

        // Map AddressRequest to Address
        AddressRequest addrReq = request.getShippingAddress();
        Address shippingAddress = Address.builder()
                .fullName(addrReq.getFullName())
                .phone(addrReq.getPhone())
                .streetAddress(addrReq.getStreetAddress())
                .city(addrReq.getCity())
                .state(addrReq.getState())
                .pincode(addrReq.getPincode())
                .build();

        // Calculate total price with discounts and shipping fees
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Optional<Coupon> couponOpt = couponRepository.findByCode(request.getCouponCode().trim().toUpperCase());
            if (couponOpt.isPresent() && couponOpt.get().getActive()) {
                Coupon coupon = couponOpt.get();
                discount = subtotal.multiply(BigDecimal.valueOf(coupon.getDiscountPercent() / 100.0));
            }
        }

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(150);
        BigDecimal grossTotal = subtotal.subtract(discount).add(shippingFee);

        BigDecimal walletCreditUsed = BigDecimal.ZERO;
        if (request.getWalletDebitAmount() != null && request.getWalletDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            walletCreditUsed = request.getWalletDebitAmount().min(grossTotal);
        }
        BigDecimal totalAmount = grossTotal.subtract(walletCreditUsed).max(BigDecimal.ZERO);

        String paymentMethod = request.getPaymentMethod();
        String paymentStatus = "PENDING";
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0 && walletCreditUsed.compareTo(BigDecimal.ZERO) > 0) {
            paymentMethod = "WALLET";
            paymentStatus = "COMPLETED";
        }

        // Validate Pincode and Courier Logistics
        String pincode = shippingAddress.getPincode();
        PincodeLookupResponse logistics = null;
        if (logisticsService != null && pincode != null && !pincode.isBlank()) {
            logistics = logisticsService.checkPincode(pincode);
            if (!logistics.isServiceable()) {
                throw new BadRequestException("Delivery is currently not serviceable to PIN code " + pincode + ". Please provide an alternative delivery address.");
            }
            if ("COD".equalsIgnoreCase(paymentMethod) && !logistics.isCodAvailable()) {
                throw new BadRequestException("Cash on Delivery (COD) is not available for PIN code " + pincode + ". Please choose an online payment method or Store Credit.");
            }
        }

        long randomAwb = 1000000L + (long) (Math.random() * 9000000L);
        String trackingNo = "SK-BD-" + randomAwb;

        String courier = (logistics != null && logistics.getCourierPartner() != null)
                ? logistics.getCourierPartner()
                : "BlueDart Express";
        String hub = (logistics != null && logistics.getFulfillmentHub() != null)
                ? logistics.getFulfillmentHub()
                : "Kanchipuram Artisan Guild - Atelier Central Hub";
        String estDelivery = (logistics != null && logistics.getDeliveryWindowLabel() != null)
                ? logistics.getDeliveryWindowLabel()
                : "3-4 Working Days";

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .totalAmount(totalAmount)
                .walletCreditUsed(walletCreditUsed)
                .trackingNumber(trackingNo)
                .courierPartner(courier)
                .currentLocation(hub)
                .estimatedDeliveryDate(estDelivery)
                .build();

        // Convert cart items to order items and update stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            
            // Update stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        if (walletCreditUsed.compareTo(BigDecimal.ZERO) > 0 && walletService != null) {
            walletService.redeemWallet(user, walletCreditUsed, savedOrder.getId());
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        // Send WhatsApp notification
        notificationService.sendOrderPlacedNotification(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if owner or admin
        if (!user.getRole().equals(Role.ADMIN) && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to view this order.");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
            
            if (orderStatus == OrderStatus.DELIVERED) {
                order.setPaymentStatus("COMPLETED");
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(LocalDateTime.now());
                }
                if (walletService != null && order.getUser() != null) {
                    try {
                        walletService.creditLoyaltyEarned(order.getUser().getId(), order.getTotalAmount(), order.getId());
                    } catch (Exception e) {
                        log.error("Failed to credit loyalty points for delivered order #{}: {}", order.getId(), e.getMessage());
                    }
                }
            }
            
            Order updatedOrder = orderRepository.save(order);
            
            // Send WhatsApp notification
            notificationService.sendOrderStatusUpdateNotification(updatedOrder);

            return orderMapper.toResponse(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + status);
        }
    }

    @Override
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Authorization check
        if (!user.getRole().equals(Role.ADMIN) && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this order.");
        }

        // Check if cancellation is allowed
        if (order.getStatus() == OrderStatus.SHIPPED || 
                order.getStatus() == OrderStatus.DELIVERED || 
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order cannot be cancelled. Current status is: " + order.getStatus());
        }

        // Revert stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // Revert wallet credit if any was used on this order
        if (order.getWalletCreditUsed() != null && order.getWalletCreditUsed().compareTo(BigDecimal.ZERO) > 0 && walletService != null) {
            try {
                walletService.revertOrderRedemption(order.getUser().getId(), order.getWalletCreditUsed(), order.getId());
            } catch (Exception e) {
                log.error("Failed to revert wallet credit for cancelled order #{}: {}", order.getId(), e.getMessage());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus("REFUNDED/CANCELLED");
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse trackOrder(Long orderId, String trackingNumber, String contact) {
        Order order = null;
        if (trackingNumber != null && !trackingNumber.trim().isBlank()) {
            order = orderRepository.findByTrackingNumber(trackingNumber.trim()).orElse(null);
            if (order == null) {
                for (Order o : orderRepository.findAll()) {
                    if (o.getTrackingNumber() != null && o.getTrackingNumber().equalsIgnoreCase(trackingNumber.trim())) {
                        order = o;
                        break;
                    }
                }
            }
        }

        if (order == null && orderId != null) {
            order = orderRepository.findById(orderId).orElse(null);
        }

        if (order == null) {
            throw new ResourceNotFoundException("Order not found with provided tracking number or order ID.");
        }

        if (contact != null && !contact.trim().isBlank()) {
            String cleanContact = contact.trim().toLowerCase();
            String userEmail = order.getUser() != null && order.getUser().getEmail() != null
                    ? order.getUser().getEmail().toLowerCase() : "";
            String userMobile = order.getUser() != null && order.getUser().getMobile() != null
                    ? order.getUser().getMobile().replaceAll("[^0-9]", "") : "";
            String addressMobile = order.getShippingAddress() != null && order.getShippingAddress().getPhone() != null
                    ? order.getShippingAddress().getPhone().replaceAll("[^0-9]", "") : "";
            String cleanDigits = cleanContact.replaceAll("[^0-9]", "");

            boolean emailMatches = !userEmail.isEmpty() && userEmail.contains(cleanContact);
            boolean mobileMatches = !cleanDigits.isEmpty() && (
                    (!userMobile.isEmpty() && userMobile.endsWith(cleanDigits)) ||
                    (!addressMobile.isEmpty() && addressMobile.endsWith(cleanDigits))
            );

            if (!emailMatches && !mobileMatches) {
                throw new BadRequestException("Verification failed: Mobile number or email does not match order records.");
            }
        }

        // Backfill defaults if empty for existing demo orders
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            order.setTrackingNumber("SK-BD-" + (1000000L + (order.getId() != null ? order.getId() : 1L) * 777L % 9000000L));
            order.setCourierPartner("BlueDart Express");
            order.setCurrentLocation("Kanchipuram Artisan Guild - Atelier Central Hub");
            order.setEstimatedDeliveryDate("3-4 Working Days");
            order = orderRepository.save(order);
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderTracking(Long orderId, String trackingNumber, String courierPartner, String currentLocation, String estimatedDeliveryDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (trackingNumber != null && !trackingNumber.trim().isBlank()) {
            order.setTrackingNumber(trackingNumber.trim());
        }
        if (courierPartner != null && !courierPartner.trim().isBlank()) {
            order.setCourierPartner(courierPartner.trim());
        }
        if (currentLocation != null && !currentLocation.trim().isBlank()) {
            order.setCurrentLocation(currentLocation.trim());
        }
        if (estimatedDeliveryDate != null && !estimatedDeliveryDate.trim().isBlank()) {
            order.setEstimatedDeliveryDate(estimatedDeliveryDate.trim());
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }
}
