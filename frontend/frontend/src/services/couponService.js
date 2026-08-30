import api from '../api/axiosConfig';

/**
 * Coupon API service for SareeKart
 */
const couponService = {
  /**
   * Preview a coupon against the current cart
   */
  previewCoupon: async (code) => {
    const response = await api.post('/coupons/preview', { code });
    return response.data;
  },

  /**
   * Validate a coupon code (legacy endpoint)
   */
  validateCoupon: async (code) => {
    const response = await api.get('/coupons/validate', {
      params: { code }
    });
    return response.data;
  },

  // ===== Admin Coupon Management =====

  /**
   * Get all coupons (admin)
   */
  getAllCoupons: async () => {
    const response = await api.get('/admin/coupons');
    return response.data;
  },

  /**
   * Create a new coupon (Admin)
   */
  createCoupon: async (couponData) => {
    const response = await api.post('/admin/coupons', couponData);
    return response.data;
  },

  /**
   * Update a coupon (Admin)
   */
  updateCoupon: async (id, couponData) => {
    const response = await api.put(`/admin/coupons/${id}`, couponData);
    return response.data;
  },

  /**
   * Delete a coupon (Admin)
   */
  deleteCoupon: async (id) => {
    const response = await api.delete(`/admin/coupons/${id}`);
    return response.data;
  }
};

export default couponService;