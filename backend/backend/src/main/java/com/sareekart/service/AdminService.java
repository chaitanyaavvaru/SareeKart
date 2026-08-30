package com.sareekart.service;

import com.sareekart.dto.admin.AdminUserResponse;
import com.sareekart.dto.admin.DashboardStatsResponse;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.entity.Order;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.entity.enums.RoleName;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin-only business logic: dashboard analytics, order oversight and
 * customer aggregates. All data is derived from real SareeKart records —
 * nothing is mocked or estimated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    /** Products at or below this total stock count trigger the low-stock alert. */
    private static final int LOW_STOCK_THRESHOLD = 3;

    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final int RECENT_ORDERS_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.sumRevenueExcludingCancelled(OrderStatus.CANCELLED);
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.countByActiveTrue();
        long totalCustomers = userRepository.countByRoles_Name(RoleName.ROLE_CUSTOMER);
        long lowStockCount = productRepository.findLowStockProductIds(LOW_STOCK_THRESHOLD).size();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);

        List<Object[]> topProductRows = productRepository.findTopSellingProducts(
            OrderStatus.CANCELLED, PageRequest.of(0, TOP_PRODUCTS_LIMIT));

        List<DashboardStatsResponse.TopProduct> topProducts = topProductRows.stream()
            .map(row -> DashboardStatsResponse.TopProduct.builder()
                .productId(row[0] != null ? ((Number) row[0]).longValue() : null)
                .productName((String) row[1])
                .unitsSold(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .revenue(row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO)
                .build())
            .collect(Collectors.toList());

        List<OrderResponse> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
            .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
            .totalOrders(totalOrders)
            .totalProducts(totalProducts)
            .totalCustomers(totalCustomers)
            .lowStockProductsCount(lowStockCount)
            .pendingOrdersCount(pendingOrders)
            .topSellingProducts(topProducts)
            .recentOrders(recentOrders)
            .build();
    }

    /**
     * Full order list for admin tables. Returns every order with items so the
     * frontend detail modal renders without extra requests.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable.unpaged())
            .getContent().stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return OrderResponse.from(order);
    }

    /**
     * Customers (ROLE_CUSTOMER users) enriched with lifetime order count and
     * spend aggregated from real orders.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getCustomers() {
        List<User> customers = userRepository.findAllByRole(RoleName.ROLE_CUSTOMER);

        Map<Long, Object[]> aggregates = orderRepository
            .aggregateOrderStatsPerUser(OrderStatus.CANCELLED).stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                Function.identity(),
                (a, b) -> a));

        return AdminUserResponse.fromList(customers, aggregates);
    }
}