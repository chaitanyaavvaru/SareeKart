package com.sareekart.controller;

import com.sareekart.dto.BulkInvoiceRequest;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.repository.OrderRepository;
import com.sareekart.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderRepository orderRepository;

    // ─────────────────────────────────────────────────────────────
    // LIST – GET /api/admin/invoices
    // Returns metadata of all non-cancelled orders (available for invoice generation).
    // Optional query params: from, to (ISO datetime), status.
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listInvoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String status) {

        List<Order> orders;
        if (from != null && to != null) {
            orders = orderRepository.findByCreatedAtBetweenAndStatusNot(from, to, OrderStatus.CANCELLED);
        } else {
            orders = orderRepository.findByStatusNot(OrderStatus.CANCELLED);
        }

        // Optional status filter
        if (status != null && !status.isBlank()) {
            OrderStatus filter = OrderStatus.valueOf(status.toUpperCase());
            orders = orders.stream()
                    .filter(o -> o.getStatus() == filter)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = orders.stream().map(o -> Map.<String, Object>of(
                "orderId",    o.getId(),
                "status",     o.getStatus().name(),
                "totalAmount", o.getTotalAmount(),
                "createdAt",  o.getCreatedAt()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────
    // SINGLE – GET /api/admin/invoices/{orderId}?format=PDF|EXCEL
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{orderId}")
    public ResponseEntity<byte[]> getInvoice(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "PDF") String format) throws IOException {
        byte[] data = invoiceService.generateInvoice(orderId, format);
        String ext = "PDF".equalsIgnoreCase(format) ? ".pdf" : ".xlsx";
        MediaType mediaType = "PDF".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", "invoice_" + orderId + ext);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────
    // BULK – POST /api/admin/invoices/bulk
    // Body: { "orderIds": [1,2,3], "format": "PDF" }
    // Returns a ZIP archive.
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/bulk")
    public ResponseEntity<byte[]> getBulkInvoices(@RequestBody BulkInvoiceRequest request) throws IOException {
        byte[] zipData = invoiceService.generateBulkInvoices(request.getOrderIds(), request.getFormat());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "invoices.zip");
        return new ResponseEntity<>(zipData, headers, HttpStatus.OK);
    }
}
