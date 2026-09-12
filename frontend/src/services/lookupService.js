import api from '../api/axiosConfig';

/**
 * Attribute Lookup Service for Phase 2: Fabrics, Occasions, and Colors
 */
const lookupService = {
  getFabrics: async (activeOnly = true) => {
    const response = await api.get(`/fabrics?activeOnly=${activeOnly}`);
    return response.data;
  },

  getOccasions: async (activeOnly = true) => {
    const response = await api.get(`/occasions?activeOnly=${activeOnly}`);
    return response.data;
  },

  getColors: async (activeOnly = true) => {
    const response = await api.get(`/colors?activeOnly=${activeOnly}`);
    return response.data;
  },
};

export default lookupService;
