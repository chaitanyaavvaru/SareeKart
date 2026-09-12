package com.sareekart.service;

import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long userId, OrderRequest request);

    OrderResponse getOrderById(Long orderId, Long userId);

    List<OrderResponse> getOrdersForUser(Long userId);

    List<OrderResponse> getAllOrders();

    OrderResponse updateOrderStatus(Long orderId, String status);

    OrderResponse cancelOrder(Long orderId, Long userId);
    
    OrderResponse trackOrder(Long orderId, String trackingNumber, String contact);
    
    OrderResponse updateOrderTracking(Long orderId, String trackingNumber, String courierPartner, String currentLocation, String estimatedDeliveryDate);
}
