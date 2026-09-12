import api from '../api/axiosConfig';

export const whatsAppService = {
  // Customer endpoints
  async getOrderTimeline(orderId) {
    const response = await api.get(`/whatsapp/order/${orderId}`);
    return response.data?.data || [];
  },

  async updateOptInPreference(optIn) {
    const response = await api.put('/whatsapp/preference', { optIn });
    return response.data?.data;
  },

  // Staff endpoints
  async getAdminLogs(eventType = 'ALL', page = 0, size = 20) {
    const response = await api.get('/admin/whatsapp/logs', {
      params: { eventType, page, size }
    });
    return response.data?.data;
  },

  async getTelemetry() {
    const response = await api.get('/admin/whatsapp/telemetry');
    return response.data?.data;
  },

  async simulateDispatch(payload) {
    const response = await api.post('/admin/whatsapp/simulate-dispatch', payload);
    return response.data?.data;
  },

  async resendNotification(logId) {
    const response = await api.post(`/admin/whatsapp/resend/${logId}`);
    return response.data?.data;
  }
};

export default whatsAppService;
