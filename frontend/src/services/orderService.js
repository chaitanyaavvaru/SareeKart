import api from '../api/axiosConfig';

/**
 * Order API service for SareeKart
 */
const orderService = {
  /**
   * Get all orders for the currently authenticated user
   */
  getOrders: async () => {
    const response = await api.get('/orders');
    return response.data;
  },

  /**
   * Get details of a specific order
   */
  getOrderById: async (id) => {
    const response = await api.get(`/orders/${id}`);
    return response.data;
  },

  /**
   * Cancel an order (User only, status must be Pending/Processing)
   */
  cancelOrder: async (id) => {
    const response = await api.put(`/orders/${id}/cancel`);
    return response.data;
  },

  /**
   * Get all orders (Admin only)
   */
  getAllOrders: async () => {
    const response = await api.get('/admin/orders');
    return response.data;
  },

  /**
   * Update the status of an order (Admin only)
   * Status options: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
   */
  updateOrderStatus: async (id, status) => {
    const response = await api.put(`/admin/orders/${id}/status`, null, {
      params: { status }
    });
    return response.data;
  }
};

export default orderService;
