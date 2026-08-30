import api from '../api/axiosConfig';

/**
 * Refund API service for SareeKart
 */
const refundService = {
  /**
   * Initiate a refund for an order
   */
  initiateRefund: async (orderId, refundData) => {
    const response = await api.post(`/admin/orders/${orderId}/refund`, refundData);
    return response.data;
  },

  /**
   * Get all refunds (Admin)
   */
  getAllRefunds: async (params = {}) => {
    const response = await api.get('/admin/refunds', { params });
    return response.data;
  },

  /**
   * Get refund by ID
   */
  getRefundById: async (id) => {
    const response = await api.get(`/admin/refunds/${id}`);
    return response.data;
  },

  /**
   * Get refunds for an order
   */
  getRefundsByOrderId: async (orderId) => {
    const response = await api.get(`/orders/${orderId}/refunds`);
    return response.data;
  }
};

export default refundService;