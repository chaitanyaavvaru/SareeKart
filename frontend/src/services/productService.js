import api from '../api/axiosConfig';

/**
 * Product API service for SareeKart
 */
const productService = {
  /**
   * Get all products with unified search, filtering, pagination and sorting
   */
  getProducts: async (params = {}) => {
    const endpoint = params.q ? '/products/search' : '/products';
    const response = await api.get(endpoint, {
      params,
    });
    return response.data;
  },

  /**
   * Get a single product by ID
   */
  getProductById: async (id) => {
    const response = await api.get(`/products/${id}`);
    return response.data;
  },

  /**
   * Search products by query string (backward compatible)
   */
  searchProducts: async (query, params = {}) => {
    const response = await api.get('/products/search', {
      params: { q: query, ...params },
    });
    return response.data;
  },

  /**
   * Filter products with multi-faceted criteria
   */
  filterProducts: async (filters = {}) => {
    const response = await api.get('/products/filter', {
      params: filters,
    });
    return response.data;
  },

  /**
   * Create a new product (Admin)
   */
  createProduct: async (productData) => {
    const response = await api.post('/admin/products', productData);
    return response.data;
  },

  /**
   * Update an existing product (Admin)
   */
  updateProduct: async (id, productData) => {
    const response = await api.put(`/admin/products/${id}`, productData);
    return response.data;
  },

  /**
   * Delete a product (Admin)
   */
  deleteProduct: async (id) => {
    const response = await api.delete(`/admin/products/${id}`);
    return response.data;
  },
};

export default productService;
