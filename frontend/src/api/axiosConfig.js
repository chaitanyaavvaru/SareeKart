import axios from 'axios';

/**
 * Configured Axios instance for SareeKart API calls.
 * Uses Vite proxy in dev (/api -> 127.0.0.1:8081)
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor - attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sareekart_token');
    const requestUrl = config.url || '';
    const isAuthRequest = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');
    if (token && !isAuthRequest) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Only force-redirect to login for non-auth API calls. Some Spring
      // Security configurations return 403 for an anonymous request instead
      // of 401, so both statuses must clear an invalid session.
      // Do NOT redirect for auth endpoints; authSlice should show the login
      // failure in the form.
      const requestUrl = error.config?.url || '';
      const isAuthEndpoint = requestUrl.includes('/auth/');

      if (!isAuthEndpoint) {
        localStorage.removeItem('sareekart_token');
        localStorage.removeItem('sareekart_user');
        // Preserve the current pathname as the redirect destination
        const currentPath = window.location.pathname;
        if (currentPath !== '/login') {
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
