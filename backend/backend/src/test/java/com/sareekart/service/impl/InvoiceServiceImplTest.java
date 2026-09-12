package com.sareekart.service.impl;

import com.sareekart.entity.Order;
import com.sareekart.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleOrder = Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("100.00"))
                .status(com.sareekart.entity.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
    }

    @Test
    void generatePdfInvoice_success() throws IOException {
        byte[] pdf = invoiceService.generateInvoice(1L, "PDF");
        assertNotNull(pdf);
        // Basic sanity check: PDF files start with "%PDF"
        String header = new String(pdf, 0, Math.min(pdf.length, 4));
        assertTrue(header.startsWith("%PDF"), "Generated content should start with %PDF");
    }

    @Test
    void generateExcelInvoice_success() throws IOException {
        byte[] xlsx = invoiceService.generateInvoice(1L, "EXCEL");
        assertNotNull(xlsx);
        // Excel files (XLSX) are ZIP archives; they start with PK header
        assertEquals('P', xlsx[0]);
        assertEquals('K', xlsx[1]);
    }

    @Test
    void generateInvoiceWithItemsAndAddress_success() throws IOException {
        com.sareekart.entity.Product product = com.sareekart.entity.Product.builder()
                .id(101L)
                .name("Kanchipuram Brocade Silk Saree")
                .fabric("Silk")
                .build();

        com.sareekart.entity.OrderItem item = com.sareekart.entity.OrderItem.builder()
                .id(501L)
                .product(product)
                .quantity(2)
                .price(new BigDecimal("7500.00"))
                .build();

        com.sareekart.entity.Address address = com.sareekart.entity.Address.builder()
                .fullName("Priya Sharma")
                .phone("9876543210")
                .streetAddress("42 Palm Grove Avenue")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .build();

        sampleOrder.setItems(java.util.List.of(item));
        sampleOrder.setShippingAddress(address);
        sampleOrder.setPaymentMethod("UPI");
        sampleOrder.setPaymentStatus("PAID");

        byte[] pdf = invoiceService.generateInvoice(1L, "PDF");
        assertNotNull(pdf);
        assertTrue(pdf.length > 500);

        byte[] excel = invoiceService.generateInvoice(1L, "EXCEL");
        assertNotNull(excel);
        assertTrue(excel.length > 500);
    }

    @Test
    void generateBulkInvoices_success() throws IOException {
        byte[] zip = invoiceService.generateBulkInvoices(java.util.List.of(1L), "PDF");
        assertNotNull(zip);
        // ZIP files start with PK
        assertEquals('P', zip[0]);
        assertEquals('K', zip[1]);
    }

    @Test
    void unsupportedFormat_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                invoiceService.generateInvoice(1L, "TXT"));
        assertTrue(ex.getMessage().contains("Unsupported format"));
    }
}
