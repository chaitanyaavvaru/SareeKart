/**
 * SareeKart AI Visual Fabric Similarity & Saree Photo Matcher Service
 */

import api from '../api/axiosConfig';

export const visualSearchService = {
  /**
   * Match an image's extracted color and weave attributes against catalog
   * @param {Object} searchData - { primaryColor, secondaryColor, weaveHint, occasion, source }
   */
  async matchSarees(searchData) {
    try {
      const response = await api.post('/ai/visual-search/match', searchData);
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message);
      }
      throw error;
    }
  },

  /**
   * Fetch visually similar drapes for a given product ID (PDP)
   * @param {number|string} productId
   * @param {number} limit
   */
  async getSimilarDrapes(productId, limit = 4) {
    try {
      const response = await api.get(`/ai/visual-search/similar/${productId}?limit=${limit}`);
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message);
      }
      throw error;
    }
  },

  /**
   * Fetch staff telemetry for AI Visual Search dashboard
   */
  async getTelemetry() {
    try {
      const response = await api.get('/admin/visual-search/telemetry');
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message);
      }
      throw error;
    }
  }
};

export default visualSearchService;
