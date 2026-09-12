package com.sareekart.controller;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnControllerTest {

    @Mock
    private ReturnService returnService;

    @InjectMocks
    private ReturnController returnController;

    private User testUser;
    private ReturnResponse mockResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(10L)
                .email("priya@example.com")
                .role(Role.CUSTOMER)
                .build();

        mockResponse = ReturnResponse.builder()
                .id(1L)
                .orderId(100L)
                .userId(10L)
                .status("PENDING")
                .refundAmount(new BigDecimal("2499.00"))
                .build();
    }

    @Test
    void testCreateReturnRequest_Success() {
        ReturnCreateRequest request = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("RETURN")
                .reason("COLOR_MISMATCH")
                .refundMode("ORIGINAL_PAYMENT")
                .build();

        when(returnService.createReturnRequest(eq(request), eq(10L))).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<ReturnResponse>> response = returnController.createReturnRequest(testUser, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testGetMyReturnRequests_Success() {
        when(returnService.getMyReturnRequests(10L)).thenReturn(List.of(mockResponse));

        ResponseEntity<ApiResponse<List<ReturnResponse>>> response = returnController.getMyReturnRequests(testUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void testGetReturnByOrderId_Success() {
        when(returnService.getReturnRequestByOrderId(100L, 10L)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<ReturnResponse>> response = returnController.getReturnByOrderId(testUser, 100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PENDING", response.getBody().getData().getStatus());
    }

    @Test
    void testGetReturnByOrderId_NotFoundReturnsNullData() {
        when(returnService.getReturnRequestByOrderId(999L, 10L)).thenReturn(null);

        ResponseEntity<ApiResponse<ReturnResponse>> response = returnController.getReturnByOrderId(testUser, 999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getData());
    }

    @Test
    void testUploadPhoto_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "defect.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = returnController.uploadPhoto(testUser, file, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().get("url").startsWith("/uploads/return-photos/"));
    }

    @Test
    void testUploadPhoto_EmptyFileReturns400() {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        ResponseEntity<ApiResponse<Map<String, String>>> response = returnController.uploadPhoto(testUser, file, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void testUploadPhoto_UnsupportedFormatReturns400() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "bad".getBytes());

        ResponseEntity<ApiResponse<Map<String, String>>> response = returnController.uploadPhoto(testUser, file, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
    }
}
