package com.sareekart.service.impl;

import com.sareekart.dto.response.AdminDashboardResponse;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.entity.Role;
import com.sareekart.mapper.OrderMapper;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;

    @Override
    public AdminDashboardResponse getDashboardStats() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();

        // Low stock count (threshold = 10)
        long lowStockCount = productRepository.findByStockQuantityLessThan(10).size();

        // Top 5 recent orders
        List<OrderResponse> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalRevenue(totalRevenue)
                .lowStockProductsCount(lowStockCount)
                .recentOrders(recentOrders)
                .build();
    }

    @Override
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productRepository.findByStockQuantityLessThan(threshold).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}
