import api from '../api/axiosConfig';

/**
 * Category API service for SareeKart
 */
const categoryService = {
  /**
   * Get all categories
   */
  getCategories: async () => {
    const response = await api.get('/categories');
    return response.data;
  },

  /**
   * Get a single category by ID
   */
  getCategoryById: async (id) => {
    const response = await api.get(`/categories/${id}`);
    return response.data;
  },

  /**
   * Create a new category (Admin)
   */
  createCategory: async (categoryData) => {
    const response = await api.post('/admin/categories', categoryData);
    return response.data;
  },

  /**
   * Update a category (Admin)
   */
  updateCategory: async (id, categoryData) => {
    const response = await api.put(`/admin/categories/${id}`, categoryData);
    return response.data;
  },

  /**
   * Delete a category (Admin)
   */
  deleteCategory: async (id) => {
    const response = await api.delete(`/admin/categories/${id}`);
    return response.data;
  },
};

export default categoryService;
