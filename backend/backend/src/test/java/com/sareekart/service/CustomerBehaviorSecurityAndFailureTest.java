package com.sareekart.service;

import com.sareekart.controller.CustomerEventController;
import com.sareekart.dto.request.CustomerEventRequest;
import com.sareekart.dto.request.EventBatchRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.entity.CustomerEventType;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerBehaviorSecurityAndFailureTest {

    @Mock
    private CustomerBehaviorService customerBehaviorService;

    @InjectMocks
    private CustomerEventController customerEventController;

    @Test
    @DisplayName("1. Telemetry Failure Isolation: Backend Exception In Ingestion Does Not Break Storefront")
    void trackEvent_handlesBackendExceptionGracefully() {
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId("sess_fault_test_001")
                .eventType(CustomerEventType.PRODUCT_VIEW.name())
                .metadata(Map.of("productId", 101))
                .build();

        // Simulate database timeout or connection failure in service layer
        when(customerBehaviorService.recordEvent(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Simulated Database Connection Outage / Timeout"));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.168.1.1");
        servletRequest.addHeader("User-Agent", "Mozilla/5.0");

        // Controller catches error and returns success(null) to guarantee non-blocking execution
        ResponseEntity<ApiResponse<CustomerEventResponse>> response =
                customerEventController.trackEvent(null, req, servletRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getData(), "Data is null but response is success so browser doesn't retry frantically");
    }

    @Test
    @DisplayName("2. Batch Telemetry Failure Isolation: Batch Error Returns Empty List Without 500")
    void trackEventBatch_handlesBatchExceptionGracefully() {
        EventBatchRequest batchReq = EventBatchRequest.builder()
                .events(List.of(
                        CustomerEventRequest.builder().sessionId("sess_1").eventType(CustomerEventType.ADD_TO_CART.name()).build()
                ))
                .build();

        when(customerBehaviorService.recordBatch(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Simulated Transient Cluster Error"));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        ResponseEntity<ApiResponse<List<CustomerEventResponse>>> response =
                customerEventController.trackEventBatch(null, batchReq, servletRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getData().isEmpty());
    }

    @Test
    @DisplayName("3. Identity Resolution Requires Authenticated User Context")
    void identifySession_rejectsUnauthenticatedUser() {
        com.sareekart.dto.request.IdentifySessionRequest req =
                new com.sareekart.dto.request.IdentifySessionRequest("sess_unauth_999");

        // Calling identify without authentication must throw BadRequestException
        assertThrows(BadRequestException.class, () ->
                customerEventController.identifySession(null, req));
    }

    @Test
    @DisplayName("4. Server Context Enforces Authoritative User ID Over Browser Claims")
    void trackEvent_usesServerAuthenticatedUserId() {
        User authenticatedUser = User.builder().id(42L).email("auth@sareekart.com").build();
        CustomerEventRequest req = CustomerEventRequest.builder()
                .sessionId("sess_server_auth_test")
                .eventType(CustomerEventType.ADD_TO_CART.name())
                .build();

        when(customerBehaviorService.recordEvent(eq(req), eq(42L), any(), any()))
                .thenReturn(CustomerEventResponse.builder().id(100L).userId(42L).build());

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        ResponseEntity<ApiResponse<CustomerEventResponse>> res =
                customerEventController.trackEvent(authenticatedUser, req, servletRequest);

        assertNotNull(res);
        assertEquals(42L, res.getBody().getData().getUserId());
        // Verify service was called with authenticated user ID 42L
        verify(customerBehaviorService).recordEvent(req, 42L, "127.0.0.1", null);
    }
}
