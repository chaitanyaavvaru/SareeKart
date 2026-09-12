package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ApprovalService;
import com.sareekart.service.ExcelProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExcelTransactionController {

    private final ExcelProcessingService excelProcessingService;
    private final ApprovalService approvalService;

    @GetMapping("/templates/{type}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String type) {
        byte[] excelBytes = excelProcessingService.generateTemplate(type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SareeKart_" + type + "_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExcelProcessingService.BatchValidationResult>> previewExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "sales") String type) {
        try {
            ExcelProcessingService.BatchValidationResult result =
                    excelProcessingService.previewAndValidate(file.getInputStream(), type);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to read Excel file: " + e.getMessage(), null));
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "sales") String type,
            @RequestParam(value = "mode", defaultValue = "COMPLETED_SALES") String mode,
            @RequestParam(value = "stockAlreadyDeducted", defaultValue = "false") boolean stockAlreadyDeducted,
            @RequestParam(value = "confirmReceipt", defaultValue = "false") boolean confirmReceipt,
            @AuthenticationPrincipal User user) {
        try {
            ExcelProcessingService.BatchValidationResult validation =
                    excelProcessingService.previewAndValidate(file.getInputStream(), type);

            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed with " + validation.getErrors().size() + " errors. Batch rejected atomically.", null));
            }

            // Create Maker-Checker request for the batch
            String requestedValue = "Batch of " + validation.getTotalRows() + " rows. Mode: " + mode +
                    " (stockDeducted=" + stockAlreadyDeducted + ", receiptConfirmed=" + confirmReceipt + ")";

            approvalService.submitRequest(
                    "EXCEL_BATCH_IMPORT",
                    type.toUpperCase() + "-" + System.currentTimeMillis(),
                    "BATCH_IMPORT_" + mode,
                    "N/A",
                    requestedValue,
                    "Excel spreadsheet bulk upload by " + user.getEmail(),
                    user
            );

            return ResponseEntity.ok(ApiResponse.success("Excel batch validated successfully (" + validation.getTotalRows() + " rows). Queued with status: Pending owner approval.", "QUEUED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process Excel batch: " + e.getMessage(), null));
        }
    }
}
