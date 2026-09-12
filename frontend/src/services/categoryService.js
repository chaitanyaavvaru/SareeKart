import api from '../api/axiosConfig';

/**
 * Category API service for SareeKart (Phase 2 with Tree & Status Toggle)
 */
const categoryService = {
  /**
   * Get flat active categories
   */
  getCategories: async (activeOnly = true) => {
    const response = await api.get(`/categories?activeOnly=${activeOnly}`);
    return response.data;
  },

  /**
   * Get hierarchical category tree (root categories with nested subcategories)
   */
  getCategoryTree: async () => {
    const response = await api.get('/categories?tree=true');
    return response.data;
  },

  /**
   * Get all categories for Admin (including inactive and product counts)
   */
  getAdminCategories: async () => {
    const response = await api.get('/admin/categories');
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
   * Toggle active/inactive status (Admin)
   */
  toggleCategoryStatus: async (id) => {
    const response = await api.patch(`/admin/categories/${id}/toggle-status`);
    return response.data;
  },

  /**
   * Safe Delete a category (Admin). Rejects if products or children exist.
   */
  deleteCategory: async (id) => {
    const response = await api.delete(`/admin/categories/${id}`);
    return response.data;
  },
};

export default categoryService;
