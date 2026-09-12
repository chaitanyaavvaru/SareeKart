import api from '../api/axiosConfig';

/**
 * Wishlist API service for SareeKart
 */
const wishlistService = {
  /**
   * Fetch authenticated user's wishlist
   */
  getWishlist: async () => {
    const response = await api.get('/wishlist');
    return response.data;
  },

  /**
   * Get lightweight count of saved items
   */
  getWishlistCount: async () => {
    const response = await api.get('/wishlist/count');
    return response.data;
  },

  /**
   * Add a saree to wishlist (idempotent)
   */
  addToWishlist: async (productId) => {
    const response = await api.post(`/wishlist/${productId}`);
    return response.data;
  },

  /**
   * Remove a saree from wishlist
   */
  removeFromWishlist: async (productId) => {
    const response = await api.delete(`/wishlist/${productId}`);
    return response.data;
  },

  /**
   * Atomically move a saree from wishlist to shopping cart
   */
  moveWishlistToCart: async (productId) => {
    const response = await api.post(`/wishlist/move-to-cart/${productId}`);
    return response.data;
  },

  /**
   * Synchronize guest wishlist product IDs into database upon customer sign-in
   */
  syncGuestWishlist: async (productIds) => {
    const response = await api.post('/wishlist/sync', { productIds });
    return response.data;
  },
};

export default wishlistService;
