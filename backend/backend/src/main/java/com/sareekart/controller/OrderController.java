package com.sareekart.controller;

import com.sareekart.dto.request.OrderRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.entity.User;
import com.sareekart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.sareekart.entity.Order;
import com.sareekart.entity.Role;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final OrderRepository orderRepository;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.createOrder(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForUser(
            @AuthenticationPrincipal User user) {
        List<OrderResponse> orders = orderService.getOrdersForUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/orders/{id}/invoice")
    public ResponseEntity<byte[]> getOrderInvoice(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "PDF") String format) throws IOException {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        boolean isStaff = user.getRole() == Role.ADMIN
                || user.getRole() == Role.OWNER
                || user.getRole() == Role.MANAGER;

        if (!isStaff && (order.getUser() == null || !order.getUser().getId().equals(user.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] data = invoiceService.generateInvoice(id, format);
        String ext = "PDF".equalsIgnoreCase(format) ? ".pdf" : ".xlsx";
        MediaType mediaType = "PDF".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", "SareeKart_TaxInvoice_" + id + ext);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        OrderResponse order = orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    @PutMapping("/orders/{id}/cancel-pending")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelPendingOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        OrderResponse order = orderService.cancelPendingOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Pending order cancelled and stock restored successfully", order));
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/admin/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    @GetMapping("/orders/track")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String contact) {
        OrderResponse order = orderService.trackOrder(orderId, trackingNumber, contact);
        return ResponseEntity.ok(ApiResponse.success("Order tracking details retrieved", order));
    }

    @PutMapping("/admin/orders/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderTracking(
            @PathVariable Long id,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String courierPartner,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) String estimatedDeliveryDate) {
        OrderResponse order = orderService.updateOrderTracking(id, trackingNumber, courierPartner, currentLocation, estimatedDeliveryDate);
        return ResponseEntity.ok(ApiResponse.success("Order tracking details updated", order));
    }
}
