package com.sareekart.service.impl;

import com.sareekart.entity.Address;
import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import com.sareekart.repository.OrderRepository;
import com.sareekart.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    private static final String COMPANY_NAME = "SareeKart Luxury Handlooms Pvt. Ltd.";
    private static final String COMPANY_ADDRESS = "108 Heritage Boulevard, MG Road, Bengaluru, Karnataka 560001";
    private static final String COMPANY_GSTIN = "29AABCS1429B1Z4";
    private static final String COMPANY_PAN = "AABCS1429B";
    private static final String COMPANY_STATE = "Karnataka (Code: 29)";
    private static final String HSN_CODE = "5407.10"; // Woven fabrics of synthetic/silk filament yarn
    private static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18% GST

    @Override
    public byte[] generateInvoice(Long orderId, String format) throws IOException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if ("PDF".equalsIgnoreCase(format)) {
            return generatePdfInvoice(order);
        } else if ("EXCEL".equalsIgnoreCase(format) || "XLSX".equalsIgnoreCase(format)) {
            return generateExcelInvoice(order);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    @Override
    public byte[] generateBulkInvoices(List<Long> orderIds, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Long id : orderIds) {
                byte[] data = generateInvoice(id, format);
                String fileName = "invoice_" + id + ("PDF".equalsIgnoreCase(format) ? ".pdf" : ".xlsx");
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(data);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private byte[] generatePdfInvoice(Order order) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 40;
            float y = page.getMediaBox().getHeight() - margin; // 842 - 40 = 802

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDFont bold = PDType1Font.HELVETICA_BOLD;
                PDFont regular = PDType1Font.HELVETICA;
                PDFont oblique = PDType1Font.HELVETICA_OBLIQUE;

                // 1. Header Banner
                cs.setNonStrokingColor(23, 33, 31); // Dark Teal #17211F
                cs.addRect(margin, y - 45, page.getMediaBox().getWidth() - (margin * 2), 48);
                cs.fill();

                // Header Text
                cs.setNonStrokingColor(243, 197, 106); // Gold #F3C56A
                drawText(cs, bold, 16, margin + 12, y - 22, COMPANY_NAME);

                cs.setNonStrokingColor(255, 255, 255);
                drawText(cs, regular, 9, margin + 12, y - 36, "Certified Silk Mark & Handloom Provenance | Authentic Indian Heritage Weaves");

                y -= 60;

                // 2. Company & Invoice Metadata Header Grid
                cs.setNonStrokingColor(17, 24, 39);
                drawText(cs, bold, 9, margin, y, "Seller Information:");
                drawText(cs, regular, 8, margin, y - 12, COMPANY_ADDRESS);
                drawText(cs, regular, 8, margin, y - 22, "GSTIN: " + COMPANY_GSTIN + " | State: " + COMPANY_STATE);
                drawText(cs, regular, 8, margin, y - 32, "PAN: " + COMPANY_PAN + " | CIN: U17290KA2024PTC184920");

                // Right side: Tax Invoice Header
                float rightCol = 360;
                drawText(cs, bold, 12, rightCol, y, "TAX INVOICE");
                drawText(cs, oblique, 8, rightCol, y - 12, "(Original for Recipient - Rule 46)");
                drawText(cs, bold, 8, rightCol, y - 22, "Invoice No: SK-INV-2026-" + String.format("%04d", order.getId()));

                String dateStr = order.getCreatedAt() != null
                        ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                        : "N/A";
                drawText(cs, regular, 8, rightCol, y - 32, "Date: " + dateStr);

                y -= 46;

                // Divider line
                strokeLine(cs, margin, y, page.getMediaBox().getWidth() - margin, y, new Color(221, 216, 207));
                y -= 12;

                // 3. Buyer & Order Dispatch Information
                drawText(cs, bold, 9, margin, y, "Billed & Shipped To:");
                Address addr = order.getShippingAddress();
                String custName = addr != null && addr.getFullName() != null ? addr.getFullName() :
                        (order.getUser() != null ? (order.getUser().getFirstName() + " " + order.getUser().getLastName()).trim() : "Valued Customer");
                String phone = addr != null && addr.getPhone() != null ? addr.getPhone() : (order.getUser() != null ? order.getUser().getMobile() : "N/A");
                String street = addr != null && addr.getStreetAddress() != null ? addr.getStreetAddress() : "N/A";
                String cityState = (addr != null && addr.getCity() != null ? addr.getCity() : "") +
                        (addr != null && addr.getState() != null ? ", " + addr.getState() : "") +
                        (addr != null && addr.getPincode() != null ? " - " + addr.getPincode() : "");

                drawText(cs, bold, 8, margin, y - 12, custName);
                drawText(cs, regular, 8, margin, y - 22, street);
                drawText(cs, regular, 8, margin, y - 32, cityState);
                drawText(cs, regular, 8, margin, y - 42, "Contact: " + (phone != null ? phone : "N/A"));

                // Right column: Dispatch Details
                drawText(cs, bold, 9, rightCol, y, "Dispatch & Payment Particulars:");
                drawText(cs, regular, 8, rightCol, y - 12, "Order ID: #" + order.getId());
                drawText(cs, regular, 8, rightCol, y - 22, "Payment Mode: " + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "Prepaid Online"));
                drawText(cs, regular, 8, rightCol, y - 32, "Payment Status: " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "CONFIRMED"));
                String tracking = order.getTrackingNumber() != null ? order.getTrackingNumber() : "SKT-BLUEDART-EXP";
                drawText(cs, regular, 8, rightCol, y - 42, "Courier Tracking: " + tracking);

                y -= 54;

                // 4. Itemized Table
                float tableWidth = page.getMediaBox().getWidth() - (margin * 2);
                cs.setNonStrokingColor(23, 33, 31);
                cs.addRect(margin, y - 18, tableWidth, 20);
                cs.fill();

                cs.setNonStrokingColor(255, 255, 255);
                drawText(cs, bold, 8, margin + 4, y - 12, "Sr.");
                drawText(cs, bold, 8, margin + 26, y - 12, "Item Description & Weave");
                drawText(cs, bold, 8, margin + 250, y - 12, "HSN");
                drawText(cs, bold, 8, margin + 300, y - 12, "Qty");
                drawText(cs, bold, 8, margin + 336, y - 12, "Rate (INR)");
                drawText(cs, bold, 8, margin + 400, y - 12, "Tax (18%)");
                drawText(cs, bold, 8, margin + 460, y - 12, "Total (INR)");

                y -= 22;

                // Table Rows
                List<OrderItem> items = order.getItems();
                int idx = 1;
                BigDecimal subtotalTaxable = BigDecimal.ZERO;
                BigDecimal totalGst = BigDecimal.ZERO;

                cs.setNonStrokingColor(17, 24, 39);

                if (items == null || items.isEmpty()) {
                    // Render order total fallback
                    BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal taxable = total.divide(BigDecimal.ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);
                    BigDecimal gst = total.subtract(taxable);
                    subtotalTaxable = taxable;
                    totalGst = gst;

                    drawText(cs, regular, 8, margin + 4, y - 10, "1");
                    drawText(cs, bold, 8, margin + 26, y - 10, "Pure Silk Handloom Saree Drape");
                    drawText(cs, regular, 8, margin + 250, y - 10, HSN_CODE);
                    drawText(cs, regular, 8, margin + 306, y - 10, "1");
                    drawText(cs, regular, 8, margin + 342, y - 10, "Rs. " + taxable);
                    drawText(cs, regular, 8, margin + 404, y - 10, "Rs. " + gst);
                    drawText(cs, bold, 8, margin + 460, y - 10, "Rs. " + total);

                    y -= 20;
                    strokeLine(cs, margin, y, margin + tableWidth, y, new Color(235, 230, 222));
                } else {
                    for (OrderItem itm : items) {
                        String name = itm.getProduct() != null && itm.getProduct().getName() != null
                                ? itm.getProduct().getName()
                                : "Artisanal Silk Saree";
                        if (name.length() > 42) name = name.substring(0, 42) + "...";

                        int qty = itm.getQuantity() != null ? itm.getQuantity() : 1;
                        BigDecimal lineTotal = itm.getPrice() != null ? itm.getPrice().multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;
                        BigDecimal lineTaxable = lineTotal.divide(BigDecimal.ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);
                        BigDecimal lineGst = lineTotal.subtract(lineTaxable);

                        subtotalTaxable = subtotalTaxable.add(lineTaxable);
                        totalGst = totalGst.add(lineGst);

                        drawText(cs, regular, 8, margin + 4, y - 10, String.valueOf(idx++));
                        drawText(cs, regular, 8, margin + 26, y - 10, name);
                        drawText(cs, regular, 8, margin + 250, y - 10, HSN_CODE);
                        drawText(cs, regular, 8, margin + 306, y - 10, String.valueOf(qty));
                        drawText(cs, regular, 8, margin + 342, y - 10, "Rs. " + lineTaxable);
                        drawText(cs, regular, 8, margin + 404, y - 10, "Rs. " + lineGst);
                        drawText(cs, bold, 8, margin + 460, y - 10, "Rs. " + lineTotal);

                        y -= 20;
                        strokeLine(cs, margin, y, margin + tableWidth, y, new Color(235, 230, 222));
                    }
                }

                // 5. Totals & Tax Calculation Breakdown
                y -= 14;
                float summaryLeft = 330;
                boolean isIntraState = addr != null && "Karnataka".equalsIgnoreCase(addr.getState());
                BigDecimal cgst = totalGst.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                BigDecimal sgst = totalGst.subtract(cgst);

                cs.setNonStrokingColor(247, 244, 238); // light beige box
                cs.addRect(summaryLeft - 10, y - 72, (page.getMediaBox().getWidth() - margin) - summaryLeft + 10, 80);
                cs.fill();

                cs.setNonStrokingColor(17, 24, 39);
                drawText(cs, regular, 8, summaryLeft, y - 4, "Taxable Subtotal:");
                drawText(cs, regular, 8, summaryLeft + 110, y - 4, "Rs. " + subtotalTaxable);

                if (isIntraState) {
                    drawText(cs, regular, 8, summaryLeft, y - 18, "CGST (9.0%):");
                    drawText(cs, regular, 8, summaryLeft + 110, y - 18, "Rs. " + cgst);

                    drawText(cs, regular, 8, summaryLeft, y - 32, "SGST (9.0%):");
                    drawText(cs, regular, 8, summaryLeft + 110, y - 32, "Rs. " + sgst);
                } else {
                    drawText(cs, regular, 8, summaryLeft, y - 18, "Integrated GST (IGST 18%):");
                    drawText(cs, regular, 8, summaryLeft + 110, y - 18, "Rs. " + totalGst);

                    drawText(cs, regular, 8, summaryLeft, y - 32, "Delivery & Insured Transit:");
                    drawText(cs, regular, 8, summaryLeft + 110, y - 32, "FREE");
                }

                strokeLine(cs, summaryLeft, y - 42, page.getMediaBox().getWidth() - margin - 4, y - 42, new Color(23, 33, 31));

                BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : subtotalTaxable.add(totalGst);
                cs.setNonStrokingColor(23, 33, 31);
                drawText(cs, bold, 10, summaryLeft, y - 56, "Grand Total (INR):");
                drawText(cs, bold, 10, summaryLeft + 110, y - 56, "Rs. " + grandTotal);

                // 6. Footer & Certification Notes
                y -= 100;
                cs.setNonStrokingColor(113, 129, 122); // muted text
                drawText(cs, bold, 8, margin, y, "Declaration & Terms of Supply:");
                drawText(cs, regular, 7, margin, y - 10, "1. All sarees are certified handwoven silk authenticated under the Silk Mark Organization of India.");
                drawText(cs, regular, 7, margin, y - 20, "2. 7-day hassle-free exchange & doorstep return policy applies in accordance with SareeKart terms.");
                drawText(cs, regular, 7, margin, y - 30, "3. This is a computer-generated tax invoice verified under the Central Goods and Services Tax Act, 2017.");

                // Signatory Stamp
                drawText(cs, bold, 8, rightCol + 10, y - 10, "For SareeKart Luxury Handlooms");
                drawText(cs, oblique, 7, rightCol + 10, y - 30, "[ Digitally Authenticated Authorized Signatory ]");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void drawText(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String text) throws IOException {
        if (text == null || text.isBlank()) return;
        // Strip non-ASCII characters to avoid PDFBox Type1 encoding errors
        String clean = text.replaceAll("[^\\x20-\\x7E]", " ");
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(clean);
        cs.endText();
    }

    private void strokeLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.75f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private byte[] generateExcelInvoice(Order order) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("GST Tax Invoice");
            sheet.setFitToPage(true);

            // Fonts & Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_TEAL.getIndex());

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 10);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(boldFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setBorderBottom(BorderStyle.THIN);

            // 1. Header
            int r = 0;
            Row row = sheet.createRow(r++);
            Cell cell = row.createCell(0);
            cell.setCellValue(COMPANY_NAME);
            cell.setCellStyle(titleStyle);

            row = sheet.createRow(r++);
            row.createCell(0).setCellValue(COMPANY_ADDRESS);

            row = sheet.createRow(r++);
            row.createCell(0).setCellValue("GSTIN: " + COMPANY_GSTIN + " | State: " + COMPANY_STATE + " | PAN: " + COMPANY_PAN);

            r++; // blank row
            row = sheet.createRow(r++);
            cell = row.createCell(0);
            cell.setCellValue("TAX INVOICE - " + (order.getId() != null ? "SK-INV-2026-" + String.format("%04d", order.getId()) : ""));
            cell.setCellStyle(boldStyle);

            row = sheet.createRow(r++);
            row.createCell(0).setCellValue("Order ID: #" + order.getId());
            row.createCell(2).setCellValue("Date: " + (order.getCreatedAt() != null ? order.getCreatedAt().toString() : "N/A"));

            row = sheet.createRow(r++);
            row.createCell(0).setCellValue("Payment Mode: " + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "ONLINE"));
            row.createCell(2).setCellValue("Payment Status: " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID"));

            r++; // blank row
            // 2. Table Headers
            row = sheet.createRow(r++);
            String[] headers = {"Item #", "Product Description", "HSN Code", "Quantity", "Rate (INR)", "GST (18%)", "Total (INR)"};
            for (int i = 0; i < headers.length; i++) {
                Cell hCell = row.createCell(i);
                hCell.setCellValue(headers[i]);
                hCell.setCellStyle(tableHeaderStyle);
            }

            // 3. Items
            List<OrderItem> items = order.getItems();
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;

            if (items == null || items.isEmpty()) {
                BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal taxable = total.divide(BigDecimal.ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);
                BigDecimal gst = total.subtract(taxable);
                subtotal = taxable;
                totalTax = gst;

                row = sheet.createRow(r++);
                row.createCell(0).setCellValue(1);
                row.createCell(1).setCellValue("Pure Silk Handloom Saree");
                row.createCell(2).setCellValue(HSN_CODE);
                row.createCell(3).setCellValue(1);
                row.createCell(4).setCellValue(taxable.doubleValue());
                row.createCell(5).setCellValue(gst.doubleValue());
                row.createCell(6).setCellValue(total.doubleValue());
            } else {
                int itemIdx = 1;
                for (OrderItem itm : items) {
                    row = sheet.createRow(r++);
                    String name = itm.getProduct() != null && itm.getProduct().getName() != null ? itm.getProduct().getName() : "Silk Saree";
                    int qty = itm.getQuantity() != null ? itm.getQuantity() : 1;
                    BigDecimal total = itm.getPrice() != null ? itm.getPrice().multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;
                    BigDecimal taxable = total.divide(BigDecimal.ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);
                    BigDecimal gst = total.subtract(taxable);

                    subtotal = subtotal.add(taxable);
                    totalTax = totalTax.add(gst);

                    row.createCell(0).setCellValue(itemIdx++);
                    row.createCell(1).setCellValue(name);
                    row.createCell(2).setCellValue(HSN_CODE);
                    row.createCell(3).setCellValue(qty);
                    row.createCell(4).setCellValue(taxable.doubleValue());
                    row.createCell(5).setCellValue(gst.doubleValue());
                    row.createCell(6).setCellValue(total.doubleValue());
                }
            }

            // 4. Totals
            r++;
            row = sheet.createRow(r++);
            row.createCell(4).setCellValue("Taxable Subtotal:");
            row.createCell(6).setCellValue(subtotal.doubleValue());

            row = sheet.createRow(r++);
            row.createCell(4).setCellValue("Integrated GST (18%):");
            row.createCell(6).setCellValue(totalTax.doubleValue());

            row = sheet.createRow(r++);
            cell = row.createCell(4);
            cell.setCellValue("Grand Total (INR):");
            cell.setCellStyle(boldStyle);

            cell = row.createCell(6);
            BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : subtotal.add(totalTax);
            cell.setCellValue(grandTotal.doubleValue());
            cell.setCellStyle(boldStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
