import api from '../api/axiosConfig';

/**
 * Shopping Cart API service for SareeKart
 */
const cartService = {
  /**
   * Fetch authenticated user's cart
   */
  getCart: async () => {
    const response = await api.get('/cart');
    return response.data;
  },

  /**
   * Add item to cart
   */
  addItem: async (productId, quantity = 1) => {
    const response = await api.post('/cart/items', { productId, quantity });
    return response.data;
  },

  /**
   * Update quantity for a product in cart
   */
  updateQuantity: async (productId, quantity) => {
    const response = await api.put(`/cart/items/${productId}`, null, {
      params: { quantity },
    });
    return response.data;
  },

  /**
   * Remove item from cart
   */
  removeItem: async (productId) => {
    const response = await api.delete(`/cart/items/${productId}`);
    return response.data;
  },

  /**
   * Clear entire cart
   */
  clearCart: async () => {
    const response = await api.delete('/cart');
    return response.data;
  },

  /**
   * Atomically merge guest items into customer's authenticated cart
   */
  mergeGuestCart: async (items) => {
    const payload = {
      items: items.map((i) => ({
        productId: i.productId || i.id,
        quantity: i.quantity || i.qty || 1,
      })),
    };
    const response = await api.post('/cart/merge', payload);
    return response.data;
  },
};

export default cartService;
