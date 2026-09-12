package com.sareekart.service;

import com.sareekart.entity.InventoryItem;
import com.sareekart.entity.Product;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.ProductRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelProcessingService {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Data
    @Builder
    public static class RowValidationError {
        private String sheet;
        private int row;
        private String column;
        private String reason;
    }

    @Data
    @Builder
    public static class BatchValidationResult {
        private boolean valid;
        private int totalRows;
        private List<RowValidationError> errors;
        private List<Map<String, String>> previewData;
    }

    /**
     * Generate standard downloadable .xlsx templates
     */
    public byte[] generateTemplate(String type) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(type != null ? type.toUpperCase() : "TEMPLATE");
            Row header = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            List<String> columns = switch (type.toLowerCase()) {
                case "sales" -> List.of("Reference_ID", "Customer_Email", "SKU", "Quantity", "Sale_Price", "Sale_Date");
                case "stock" -> List.of("Reference_ID", "SKU", "Warehouse_Code", "Quantity_Adjustment", "Reason");
                case "customer_bill" -> List.of("Bill_Number", "Order_Reference", "Customer_Name", "Invoice_Amount", "Bill_Date");
                case "supplier_bill" -> List.of("Invoice_Number", "Supplier_Name", "SKU", "Received_Quantity", "Unit_Cost", "Invoice_Date");
                default -> List.of("Reference_ID", "Item_Code", "Quantity", "Notes");
            };

            for (int i = 0; i < columns.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel template for {}", type, e);
            throw new RuntimeException("Could not generate Excel template", e);
        }
    }

    /**
     * Preview and validate spreadsheet rows atomically
     */
    public BatchValidationResult previewAndValidate(InputStream inputStream, String type) {
        List<RowValidationError> errors = new ArrayList<>();
        List<Map<String, String>> preview = new ArrayList<>();
        Set<String> seenReferences = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return BatchValidationResult.builder()
                        .valid(false)
                        .totalRows(0)
                        .errors(List.of(RowValidationError.builder().sheet("Sheet1").row(1).column("ALL").reason("Spreadsheet is empty").build()))
                        .build();
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return BatchValidationResult.builder()
                        .valid(false)
                        .totalRows(0)
                        .errors(List.of(RowValidationError.builder().sheet(sheet.getSheetName()).row(1).column("HEADER").reason("Missing header row").build()))
                        .build();
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            int lastRow = sheet.getLastRowNum();
            int validRowCount = 0;

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue; // ignore blank space outside table

                validRowCount++;
                Map<String, String> rowMap = new LinkedHashMap<>();

                for (int c = 0; c < headers.size(); c++) {
                    String colName = headers.get(c);
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell != null ? getCellValueAsString(cell).trim() : "";

                    if (val.isEmpty()) {
                        errors.add(RowValidationError.builder()
                                .sheet(sheet.getSheetName())
                                .row(r + 1)
                                .column(colName)
                                .reason("Mandatory field cannot be blank or whitespace")
                                .build());
                    }

                    // Check duplicate reference IDs
                    if (c == 0 && !val.isEmpty()) {
                        if (seenReferences.contains(val)) {
                            errors.add(RowValidationError.builder()
                                    .sheet(sheet.getSheetName())
                                    .row(r + 1)
                                    .column(colName)
                                    .reason("Duplicate Reference ID '" + val + "' found in file")
                                    .build());
                        } else {
                            seenReferences.add(val);
                        }
                    }

                    rowMap.put(colName, val);
                }
                preview.add(rowMap);
            }

            return BatchValidationResult.builder()
                    .valid(errors.isEmpty() && validRowCount > 0)
                    .totalRows(validRowCount)
                    .errors(errors)
                    .previewData(preview)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Excel batch", e);
            return BatchValidationResult.builder()
                    .valid(false)
                    .totalRows(0)
                    .errors(List.of(RowValidationError.builder().sheet("File").row(0).column("N/A").reason("Invalid Excel file format: " + e.getMessage()).build()))
                    .build();
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell c : row) {
            if (c != null && !getCellValueAsString(c).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
