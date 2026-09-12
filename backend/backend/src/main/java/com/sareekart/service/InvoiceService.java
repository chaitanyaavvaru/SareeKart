package com.sareekart.service;

import java.io.IOException;
import java.util.List;

public interface InvoiceService {
    /**
     * Generate an invoice for a single order.
     * @param orderId the order identifier
     * @param format "PDF" or "EXCEL"
     * @return byte array containing the generated file
     * @throws IOException if generation fails
     */
    byte[] generateInvoice(Long orderId, String format) throws IOException;

    /**
     * Generate invoices for a list of orders.
     * @param orderIds list of order identifiers
     * @param format "PDF" or "EXCEL"
     * @return byte array of a zip archive containing each invoice file
     * @throws IOException if generation fails
     */
    byte[] generateBulkInvoices(List<Long> orderIds, String format) throws IOException;
}
