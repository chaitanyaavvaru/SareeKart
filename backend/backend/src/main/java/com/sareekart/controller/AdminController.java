package com.sareekart.controller;

import com.sareekart.dto.admin.AdminUserResponse;
import com.sareekart.dto.admin.DashboardStatsResponse;
import com.sareekart.dto.category.CategoryRequest;
import com.sareekart.dto.coupon.CouponAdminResponse;
import com.sareekart.dto.coupon.CouponRequest;
import com.sareekart.dto.refund.RefundRequest;
import com.sareekart.dto.refund.RefundResponse;
import com.sareekart.entity.enums.RefundStatus;
import com.sareekart.dto.category.CategoryResponse;
import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.entity.enums.OrderStatus;

import com.sareekart.dto.product.ProductRequest;
import com.sareekart.dto.product.ProductResponse;
import com.sareekart.entity.enums.OrderStatus;
import com.sareekart.service.AdminService;
import com.sareekart.service.CategoryService;
import com.sareekart.service.CouponService;
import com.sareekart.service.OrderSweeperService;
import com.sareekart.service.RefundReconciliationService;
import com.sareekart.service.RefundService;
import com.sareekart.service.OrderService;
import com.sareekart.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin surface. Every endpoint requires ROLE_ADMIN (enforced by both the
 * URL pattern /admin/** and method security below).
 *
 * Route shapes mirror the frontend service layer:
 *  - AdminStats.jsx      -> GET /admin/dashboard
 *  - ManageOrders.jsx    -> GET /admin/orders, PUT /admin/orders/{id}/status?status=
 *  - ManageUsers.jsx     -> GET /admin/orders (array), GET /admin/users
 *  - ManageSarees.jsx    -> POST|PUT|DELETE /admin/products, /admin/categories
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only management APIs")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final OrderSweeperService orderSweeperService;
    private final RefundService refundService;
    private final RefundReconciliationService reconciliationService;
    private final com.sareekart.repository.ReconciliationAnomalyRepository anomalyRepository;

    // ── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard statistics: revenue, orders, products, customers, low-stock alerts, recent orders")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // ── Orders ───────────────────────────────────────────────────────────────

    /**
     * Returns a plain array (not paginated) — ManageOrders.jsx and
     * ManageUsers.jsx consume `response.data.data` directly as a list.
     */
    @GetMapping("/orders")
    @Operation(summary = "All orders with items (array response)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllOrders()));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOrderById(id)));
    }

    /** Matches orderService.updateOrderStatus: PUT with ?status= query param. */
    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Update order status (fulfillment workflow)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
            "Order status updated", orderService.updateOrderStatus(id, status)));
    }

    // ── Products ─────────────────────────────────────────────────────────────

    @PostMapping("/products")
    @Operation(summary = "Create product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Product created successfully", productService.createProduct(request)));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Product updated successfully", productService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Soft-delete product (deactivates catalog listing)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @PatchMapping("/products/{id}/stock")
    @Operation(summary = "Quick stock adjustment")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        return ResponseEntity.ok(ApiResponse.success(
            "Stock updated successfully", productService.updateStock(id, stock)));
    }

    // ── Categories ───────────────────────────────────────────────────────────

    @PostMapping("/categories")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Category created successfully", categoryService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Category updated successfully", categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }

    // ── Users / Customers ────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Registered customers with lifetime order count and spend")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getCustomers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCustomers()));
    }

    // ── Coupons ──────────────────────────────────────────────────────────────

    @PostMapping("/coupons")
    @Operation(summary = "Create coupon")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Coupon created successfully", couponService.create(request)));
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update coupon (deactivate to stop new redemptions)")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Coupon updated successfully", couponService.update(id, request)));
    }

    @GetMapping("/coupons")
    @Operation(summary = "List coupons with usage counters")
    public ResponseEntity<ApiResponse<List<CouponAdminResponse>>> listCoupons() {
        return ResponseEntity.ok(ApiResponse.success(couponService.list()));
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Delete coupon (soft-deactivate if already used)")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted", null));
    }

    // ── Operations ───────────────────────────────────────────────────────────

    @PostMapping("/sweeper/run")
    @Operation(summary = "Trigger one abandoned-checkout sweep pass immediately")
    public ResponseEntity<ApiResponse<OrderSweeperService.SweepSummary>> runSweeper() {
        return ResponseEntity.ok(ApiResponse.success(orderSweeperService.runOnce()));
    }

    // ── Refunds ──────────────────────────────────────────────────────────────

    @PostMapping("/orders/{orderId}/refund")
    @Operation(summary = "Initiate a full (default) or partial refund against the captured payment")
    public ResponseEntity<ApiResponse<RefundResponse>> initiateRefund(
            @AuthenticationPrincipal UserDetails admin,
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) RefundRequest request) {
        String initiator = admin != null ? admin.getUsername() : "admin";
        return ResponseEntity.ok(ApiResponse.success(
            "Refund initiated", refundService.initiate(initiator, orderId, request)));
    }

    @GetMapping("/refunds")
    @Operation(summary = "List refunds, optionally filtered by status (ops/reconciliation view)")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> listRefunds(
            @RequestParam(required = false) RefundStatus status) {
        return ResponseEntity.ok(ApiResponse.success(refundService.list(status)));
    }

    // ── Reconciliation ops ───────────────────────────────────────────────────

    @PostMapping("/reconciliation/refunds/run")
    @Operation(summary = "Trigger one refund-reconciliation pass immediately")
    public ResponseEntity<ApiResponse<RefundReconciliationService.ReconcileSummary>> runRefundReconciliation() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.runOnce()));
    }

    @GetMapping("/reconciliation/anomalies")
    @Operation(summary = "Open reconciliation anomalies (optionally by code)")
    public ResponseEntity<ApiResponse<List<com.sareekart.entity.ReconciliationAnomaly>>> listAnomalies(
            @RequestParam(required = false) com.sareekart.entity.enums.AnomalyCode code) {
        List<com.sareekart.entity.ReconciliationAnomaly> rows = code == null
            ? anomalyRepository.findByResolvedFalseOrderByCreatedAtDesc()
            : anomalyRepository.findByCodeOrderByCreatedAtDesc(code);
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @PatchMapping("/reconciliation/anomalies/{id}/resolve")
    @Operation(summary = "Mark an anomaly resolved after manual review")
    public ResponseEntity<ApiResponse<Void>> resolveAnomaly(
            @AuthenticationPrincipal UserDetails admin,
            @PathVariable Long id) {
        java.util.Optional<com.sareekart.entity.ReconciliationAnomaly> row =
            anomalyRepository.findById(id);
        if (row.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Anomaly not found"));
        }
        com.sareekart.entity.ReconciliationAnomaly a = row.get();
        a.setResolved(true);
        a.setResolvedBy(admin != null ? admin.getUsername() : "admin");
        anomalyRepository.save(a);
        return ResponseEntity.ok(ApiResponse.success("Anomaly resolved", null));
    }
}
