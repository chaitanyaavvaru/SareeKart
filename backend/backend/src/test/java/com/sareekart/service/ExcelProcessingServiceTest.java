package com.sareekart.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ExcelProcessingServiceTest {

    @InjectMocks
    private ExcelProcessingService excelProcessingService;

    @Test
    void testGenerateTemplateNotNull() {
        byte[] bytes = excelProcessingService.generateTemplate("sales");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void testValidationRejectsBlankCells() throws Exception {
        byte[] excelData;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SALES");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference_ID");
            header.createCell(1).setCellValue("Customer_Email");
            header.createCell(2).setCellValue("SKU");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("Sale_Price");
            header.createCell(5).setCellValue("Sale_Date");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("REF-001");
            row1.createCell(1).setCellValue(""); // Blank cell
            row1.createCell(2).setCellValue("SKU-123");
            row1.createCell(3).setCellValue(2);
            row1.createCell(4).setCellValue(12500);
            row1.createCell(5).setCellValue("2026-09-04");

            wb.write(out);
            excelData = out.toByteArray();
        }

        ExcelProcessingService.BatchValidationResult result =
                excelProcessingService.previewAndValidate(new ByteArrayInputStream(excelData), "sales");

        assertFalse(result.isValid());
        assertEquals(1, result.getTotalRows());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getColumn().equals("Customer_Email")));
    }

    @Test
    void testValidationRejectsDuplicateReferences() throws Exception {
        byte[] excelData;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SALES");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference_ID");
            header.createCell(1).setCellValue("Customer_Email");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("DUP-REF");
            row1.createCell(1).setCellValue("test1@example.com");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("DUP-REF"); // duplicate
            row2.createCell(1).setCellValue("test2@example.com");

            wb.write(out);
            excelData = out.toByteArray();
        }

        ExcelProcessingService.BatchValidationResult result =
                excelProcessingService.previewAndValidate(new ByteArrayInputStream(excelData), "sales");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getReason().contains("Duplicate Reference ID")));
    }
}
