package com.sareekart.dto.admin;

import com.sareekart.dto.order.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated figures rendered by the admin dashboard (GET /admin/dashboard).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    /** Revenue across all non-cancelled orders. */
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProducts;
    private long totalCustomers;
    /** Active products whose total variant stock is at or below the low-stock threshold. */
    private long lowStockProductsCount;
    private long pendingOrdersCount;
    private List<TopProduct> topSellingProducts;
    private List<OrderResponse> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private Long productId;
        private String productName;
        private Long unitsSold;
        private BigDecimal revenue;
    }
}