package com.sareekart.controller;

import com.sareekart.dto.request.ReturnStatusUpdateRequest;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReturnControllerTest {

    @Mock
    private ReturnService returnService;

    @InjectMocks
    private AdminReturnController adminReturnController;

    private User adminUser;
    private ReturnResponse mockResponse;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@sareekart.com")
                .role(Role.ADMIN)
                .build();

        mockResponse = ReturnResponse.builder()
                .id(1L)
                .orderId(100L)
                .userId(10L)
                .status("APPROVED")
                .refundAmount(new BigDecimal("2499.00"))
                .build();
    }

    @Test
    void testGetAllReturns_Success() {
        when(returnService.getAllReturnsForAdmin("PENDING")).thenReturn(List.of(mockResponse));

        ResponseEntity<ApiResponse<List<ReturnResponse>>> response =
                adminReturnController.getAllReturns(adminUser, "PENDING");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void testUpdateReturnStatus_Success() {
        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .adminNotes("Verified defect photos")
                .build();

        when(returnService.updateReturnStatus(eq(1L), eq(updateReq), eq(adminUser)))
                .thenReturn(mockResponse);

        ResponseEntity<ApiResponse<ReturnResponse>> response =
                adminReturnController.updateReturnStatus(1L, adminUser, updateReq);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("APPROVED", response.getBody().getData().getStatus());
    }
}
