/**
 * SareeKart AI Luxury Saree Stylist & Visual Drape Concierge Service
 */

import api from '../api/axiosConfig';

export const aiStylistService = {
  /**
   * Request AI styling ensembles for a given saree
   * @param {Object} drapeData - { productId, sareeName, fabric, primaryColor, occasion, zariType }
   */
  async styleDrape(drapeData) {
    try {
      const response = await api.post('/ai/stylist/drape', drapeData);
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message, { cause: error });
      }
      throw error;
    }
  },

  /**
   * Converse with AI Luxury Saree Stylist Concierge in natural language
   * @param {Object} chatData - { message, sessionId, referenceProductId, occasion, budgetRange, preferredWeave, conversationHistory }
   */
  async chat(chatData) {
    try {
      const response = await api.post('/ai/stylist/chat', chatData);
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message, { cause: error });
      }
      throw error;
    }
  },

  /**
   * Submit occasion and undertone consultation quiz
   * @param {Object} quizData - { occasion, skinUndertone, preferredWeave, budgetRange }
   */
  async consultQuiz(quizData) {
    try {
      const response = await api.post('/ai/stylist/consult', quizData);
      return response.data;
    } catch (error) {
      if (error.response?.data?.message) {
        throw new Error(error.response.data.message, { cause: error });
      }
      throw error;
    }
  },

  /**
   * Record conversion when patron customizes blouse in Tailoring Studio
   * @param {number|string} consultationId
   */
  async trackTailoring(consultationId) {
    if (!consultationId) return;
    try {
      await api.post(`/ai/stylist/track-tailoring/${consultationId}`);
    } catch (err) {
      console.warn('Could not track tailoring conversion:', err);
    }
  },

  /**
   * Fetch staff telemetry for AI Stylist dashboard
   */
  async getTelemetry() {
    const response = await api.get('/admin/ai-stylist/telemetry');
    return response.data;
  }
};

export default aiStylistService;
