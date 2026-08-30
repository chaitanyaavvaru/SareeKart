import api from '../api/axiosConfig';

/**
 * Product API service for SareeKart
 */
const productService = {
  /**
   * Get all products with pagination and sorting
   */
  getProducts: async (params = {}) => {
    const { page = 0, size = 12, sortBy = 'createdAt', sortDir = 'desc' } = params;
    const response = await api.get('/products', {
      params: { page, size, sortBy, sortDir },
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
   * Search products by query string
   */
  searchProducts: async (query, params = {}) => {
    const { page = 0, size = 12 } = params;
    const response = await api.get('/products/search', {
      params: { q: query, page, size },
    });
    return response.data;
  },

  /**
   * Filter products by category, price, fabric
   */
  filterProducts: async (filters = {}) => {
    const { categoryId, minPrice, maxPrice, fabric, page = 0, size = 12, sortBy = 'createdAt', sortDir = 'desc' } = filters;
    const response = await api.get('/products/filter', {
      params: { categoryId, minPrice, maxPrice, fabric, page, size, sortBy, sortDir },
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
