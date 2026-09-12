/**
 * SareeKart Returns & Exchanges API Service
 * 
 * Provides client-side communication for customer self-service returns,
 * defect condition photo uploads, and administrative reverse logistics.
 */

import api from '../api/axiosConfig';

/**
 * Resilient mock data for offline unit/preview testing
 */
export const MOCK_RETURN_CLAIMS = [
  {
    id: 101,
    orderId: 37,
    userId: 2,
    customerName: 'Kalyani Sundaram',
    customerEmail: 'kalyani@example.com',
    type: 'RETURN',
    reason: 'COLOR_MISMATCH',
    comments: 'The saree shade is deep crimson rather than vermilion red displayed in studio photos.',
    status: 'PICKUP_SCHEDULED',
    images: [
      'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80',
      'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 18500.0,
    refundMode: 'ORIGINAL_PAYMENT',
    exchangeSku: null,
    reverseCourier: 'Blue Dart Reverse Logistics',
    reverseTrackingNumber: 'BDR-89214',
    adminNotes: 'Condition verified from photos. Reverse pickup assigned.',
    createdAt: new Date(Date.now() - 2 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 1 * 86400000).toISOString()
  },
  {
    id: 102,
    orderId: 38,
    userId: 3,
    customerName: 'Meenakshi Iyer',
    customerEmail: 'meenakshi@example.com',
    type: 'EXCHANGE',
    reason: 'ZARI_DEFECT',
    comments: 'Frayed metallic zari threads on the lower pallu border.',
    status: 'PENDING',
    images: [
      'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 24000.0,
    refundMode: 'EXCHANGE_DRAPE',
    exchangeSku: 'KAN-SILK-MRN-02',
    reverseCourier: null,
    reverseTrackingNumber: null,
    adminNotes: null,
    createdAt: new Date(Date.now() - 12 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 12 * 3600000).toISOString()
  }
];

const returnService = {
  // =========================================================================
  // Customer Self-Service Endpoints
  // =========================================================================

  /**
   * Submit a new customer return or exchange request.
   * 
   * @param {Object} data
   * @param {number} data.orderId - Mandatory ID of delivered order
   * @param {('RETURN'|'EXCHANGE')} data.type - Return type
   * @param {string} data.reason - Reason taxonomy code
   * @param {string} data.comments - Detailed issue description (min 10 chars)
   * @param {('ORIGINAL_PAYMENT'|'STORE_CREDIT'|'EXCHANGE_DRAPE')} data.refundMode - Refund preference
   * @param {string[]} [data.images] - Array of uploaded photo URL strings (max 3)
   * @param {string} [data.exchangeSku] - Replacement drape SKU if type === 'EXCHANGE'
   * @param {number} [data.refundAmount] - Optional explicit refund amount
   * @returns {Promise<Object>} API response body containing created ReturnResponse
   */
  createReturnRequest: async (data) => {
    const response = await api.post('/returns', data);
    return response.data;
  },

  /**
   * Fetch all return requests submitted by the currently authenticated user.
   * 
   * @returns {Promise<Object>} API response body containing array of ReturnResponse
   */
  getMyReturns: async () => {
    const response = await api.get('/returns/my-requests');
    return response.data;
  },

  /**
   * Fetch return claim telemetry for a specific order.
   * 
   * @param {number|string} orderId - Order ID
   * @returns {Promise<Object>} API response body containing ReturnResponse or null data
   */
  getReturnByOrderId: async (orderId) => {
    const response = await api.get(`/returns/order/${orderId}`);
    return response.data;
  },

  /**
   * Upload an authenticated defect condition photo.
   * Stores photo in `uploads/return-photos/` and returns public URL.
   * 
   * @param {File} file - Image file (JPG, PNG, or WebP; <= 10MB)
   * @returns {Promise<Object>} API response body containing `{ success: true, data: { url, filename } }`
   */
  uploadConditionPhoto: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/returns/upload-photo', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // =========================================================================
  // Administrative Moderation Endpoints (OWNER, MANAGER, ADMIN)
  // =========================================================================

  /**
   * Fetch all return claims for administrative review.
   * 
   * @param {string} [status='ALL'] - Status filter ('ALL', 'PENDING', 'APPROVED', 'PICKUP_SCHEDULED', 'COMPLETED', 'REJECTED')
   * @returns {Promise<Object>} API response body containing list of ReturnResponse
   */
  getAllReturns: async (status = 'ALL') => {
    const url = status && status !== 'ALL' ? `/admin/returns?status=${status}` : '/admin/returns';
    const response = await api.get(url);
    return response.data;
  },

  /**
   * Update the moderation status of a return claim.
   * 
   * @param {number|string} id - Return claim ID
   * @param {Object} updateData
   * @param {('APPROVED'|'PICKUP_SCHEDULED'|'REJECTED'|'COMPLETED')} updateData.status - Target status
   * @param {string} [updateData.reverseCourier] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.reverseTrackingNumber] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.adminNotes] - Mandatory if status is REJECTED
   * @param {number} [updateData.refundAmount] - Optional adjusted refund amount
   * @returns {Promise<Object>} API response body containing updated ReturnResponse
   */
  updateReturnStatus: async (id, updateData) => {
    const response = await api.put(`/admin/returns/${id}/status`, updateData);
    return response.data;
  },
};

// Aliases for developer convenience & cross-module compatibility
returnService.submitReturnRequest = returnService.createReturnRequest;
returnService.getOrderReturnStatus = returnService.getReturnByOrderId;
returnService.uploadReturnPhoto = returnService.uploadConditionPhoto;
returnService.getAllAdminReturns = returnService.getAllReturns;

export default returnService;
