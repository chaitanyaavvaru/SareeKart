package com.sareekart.service;

import com.sareekart.dto.order.AddressSnapshot;
import com.sareekart.dto.order.OrderRequest;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.entity.Address;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import com.sareekart.entity.Payment;
import com.sareekart.entity.Product;
import com.sareekart.entity.ProductImage;
import com.sareekart.entity.ProductVariant;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.PaymentMethod;
import com.sareekart.entity.enums.PaymentStatus;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.AddressRepository;
import com.sareekart.repository.CartItemRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.PaymentRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;

    /** Free shipping applies at or above this cart subtotal. */
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("150");
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");

    @Transactional
    public OrderResponse createOrder(String userEmail, OrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Address address = resolveAddress(user, request);

        // Pricing — server is the single source of truth. Mirrors the checkout
        // summary math: 5% GST on (subtotal - discount), flat ₹150 shipping,
        // free at or above ₹5000.
        BigDecimal subtotal = cartItems.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Coupon resolution — server-side only. The browser sends a CODE,
        // never amounts. Authoritative quota reservation happens post-save
        // under lock; any failure rolls the entire creation back.
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            couponCode = request.getCouponCode().trim().toUpperCase();
            discountAmount = couponService.quoteByCode(couponCode, subtotal);
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(GST_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal shippingAmount = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
            ? BigDecimal.ZERO
            : FLAT_SHIPPING_FEE;
        BigDecimal totalAmount = taxableAmount.add(taxAmount).add(shippingAmount);

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setShippingAmount(shippingAmount);
        order.setTaxAmount(taxAmount);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode);
        order.setTotalAmount(totalAmount);
        order.setShippingAddressJson(addressToJson(address));
        order.setBillingAddressJson(addressToJson(address));
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentMethod() == PaymentMethod.COD
            ? PaymentStatus.PENDING : PaymentStatus.PENDING);
        order.setNotes(request.getNotes());
        order.setItems(new ArrayList<>());

        final Order finalOrder = order;

        // Create order items
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            // Storefront has no variant picker yet: when the cart line carries no
            // explicit variant but the product has exactly one active variant,
            // bind it so inventory tracking and restocking work end-to-end.
            if (variant == null && product.getVariants() != null) {
                List<ProductVariant> active = product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getActive()))
                    .toList();
                if (active.size() == 1) {
                    variant = active.get(0);
                }
            }
            
            String imageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(product.getImages().get(0).getUrl());
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(finalOrder);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(imageUrl);
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            return orderItem;
        }).collect(Collectors.toList());

        order.setItems(orderItems);
        order = orderRepository.save(order);

        // COD commits inventory at placement (single transaction: a shortfall
        // rolls the whole order back and the cart survives). Online orders
        // commit only when payment verification/webhook flips them to PAID.
        if (couponCode != null) {
            couponService.reserveForOrder(order, couponCode);
        }

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            inventoryService.commitForOrder(order);
            // COD placement IS the commit point — mark coupon usage permanent
            // now so later cancellation cannot retroactively free quota.
            couponService.confirmForOrder(order.getId());
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(request.getPaymentMethod().name());
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(request.getPaymentMethod());
        paymentRepository.save(payment);

        // Clear cart
        cartItemRepository.deleteByUserId(user.getId());

        log.info("Created order: {} for user: {}", order.getOrderNumber(), userEmail);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to user");
        }

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String userEmail, String orderNumber) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to user");
        }

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to user");
        }

        if (!order.canCancel()) {
            throw new BadRequestException("Order cannot be cancelled at this stage");
        }

        // Restock exactly what was committed: COD at placement, online on payment.
        if (inventoryService.isCommitted(order)) {
            inventoryService.releaseForOrder(order);
        }
        // Coupon quota: released only while uncommitted (service enforces this).
        couponService.releaseIfReserved(orderId);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("Cancelled order: {} by user: {}", order.getOrderNumber(), userEmail);
        return OrderResponse.from(order);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersWithFilters(
            OrderStatus status, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return orderRepository.findWithFilters(status, userId, startDate, endDate, pageable)
            .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);

        // Update timestamps based on status
        switch (status) {
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> order.setCancelledAt(LocalDateTime.now());
        }

        order = orderRepository.save(order);
        log.info("Updated order status: {} from {} to {}", order.getOrderNumber(), oldStatus, status);
        return OrderResponse.from(order);
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(4);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + timestamp + random;
    }

    private String addressToJson(Address address) {
        return AddressSnapshot.toJson(AddressSnapshot.from(address));
    }

    /**
     * Resolves the delivery address from either a saved addressId or the
     * inline checkout form input. Inline inputs are persisted as new
     * SHIPPING addresses for future one-tap reorders.
     */
    private Address resolveAddress(User user, OrderRequest request) {
        if (request.getAddressId() != null) {
            Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));
            if (!address.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Address does not belong to user");
            }
            return address;
        }

        OrderRequest.AddressInput in = request.getShippingAddress();
        if (in == null
                || isBlank(in.getFullName()) || isBlank(in.getPhone())
                || isBlank(in.getStreetAddress()) || isBlank(in.getCity())
                || isBlank(in.getState()) || isBlank(in.getPincode())) {
            throw new BadRequestException("A complete shipping address is required");
        }

        Address address = new Address();
        address.setUser(user);
        address.setType(com.sareekart.entity.enums.AddressType.SHIPPING);
        address.setName(in.getFullName().trim());
        address.setPhone(in.getPhone().trim());
        address.setLine1(in.getStreetAddress().trim());
        address.setLine2(in.getLandmark() != null ? in.getLandmark().trim() : null);
        address.setCity(in.getCity().trim());
        address.setState(in.getState().trim());
        address.setPincode(in.getPincode().trim());
        address.setCountry(in.getCountry() != null && !in.getCountry().isBlank() ? in.getCountry().trim() : "India");
        address.setIsDefault(false);
        return addressRepository.save(address);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}