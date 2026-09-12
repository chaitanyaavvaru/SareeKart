package com.sareekart.service;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;

import java.util.List;

public interface ReturnService {

    /**
     * Customer: Submit a self-service return or exchange request.
     */
    ReturnResponse createReturnRequest(ReturnCreateRequest request, Long userId);

    ReturnResponse createReturnRequest(ReturnCreateRequest request, User customer);

    /**
     * Customer: Retrieve all return/exchange claims submitted by authenticated customer.
     */
    List<ReturnResponse> getMyReturnRequests(Long userId);

    List<ReturnResponse> getMyReturnRequests(User customer);

    /**
     * Customer/Staff: Retrieve return claim details for a specific order.
     * Returns null if no return claim exists for the order.
     */
    ReturnResponse getReturnRequestByOrderId(Long orderId, Long userId);

    ReturnResponse getReturnRequestByOrderId(Long orderId, User user);

    /**
     * Customer/Staff: Retrieve return claim by claim ID.
     */
    ReturnResponse getReturnRequestById(Long returnId, Long userId);

    ReturnResponse getReturnRequestById(Long returnId, User user);

    /**
     * Staff (OWNER, MANAGER, ADMIN): Retrieve all return claims for admin console.
     * Supports optional status filtering: "ALL" (or null/empty), "PENDING", "APPROVED",
     * "PICKUP_SCHEDULED", "REJECTED", "COMPLETED".
     */
    List<ReturnResponse> getAllReturnsForAdmin(String statusFilter);

    List<ReturnResponse> getAllReturnsForAdmin(String statusFilter, User staffUser);

    /**
     * Staff (OWNER, MANAGER, ADMIN): Update return status and logistics details.
     */
    ReturnResponse updateReturnStatus(Long returnId, ReturnStatusUpdateRequest request, User staffUser);
}
