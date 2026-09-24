import axios from 'axios';
import { useState, useEffect } from 'react';

/**
 * Normalizes and resolves the API base URL.
 * Supports:
 * - Empty/undefined: defaults to '/api' (for local Vite proxy)
 * - 'https://backend.onrender.com' -> normalizes to 'https://backend.onrender.com/api'
 * - 'https://backend.onrender.com/api' -> preserved
 */
export const resolveBaseUrl = () => {
  const envUrl = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL);
  if (!envUrl) {
    return '/api';
  }
  let trimmed = envUrl.trim().replace(/\/+$/, '');
  if (!trimmed.endsWith('/api')) {
    trimmed = `${trimmed}/api`;
  }
  return trimmed;
};

// State store for cold-start resilience telemetry
let coldStartState = {
  isColdStarting: false,
  message: '',
  attempt: 0,
};

const coldStartListeners = new Set();

export const getColdStartState = () => coldStartState;

export const subscribeToColdStart = (listener) => {
  coldStartListeners.add(listener);
  try {
    listener(coldStartState);
  } catch (e) {
    console.error('Error in cold-start listener invocation', e);
  }
  return () => coldStartListeners.delete(listener);
};

const updateColdStartState = (nextState) => {
  coldStartState = { ...coldStartState, ...nextState };
  coldStartListeners.forEach((listener) => {
    try {
      listener(coldStartState);
    } catch (e) {
      console.error('Error in cold-start listener execution', e);
    }
  });

  if (typeof window !== 'undefined') {
    window.dispatchEvent(
      new CustomEvent('sareekart:cold-start', {
        detail: coldStartState,
      })
    );
  }
};

/**
 * Custom React hook for tracking backend container cold-boot status.
 */
export function useColdStart() {
  const [state, setState] = useState(getColdStartState());

  useEffect(() => {
    return subscribeToColdStart(setState);
  }, []);

  return state;
}

let activeRequests = 0;
let coldStartTimer = null;

const startColdStartTimer = () => {
  if (!coldStartTimer) {
    coldStartTimer = setTimeout(() => {
      if (activeRequests > 0) {
        updateColdStartState({
          isColdStarting: true,
          message: 'Awakening our boutique atelier... Cloud containers on our zero-cost tier take ~30–45s to spin up.',
          attempt: 1,
        });
      }
    }, 3500); // 3.5s threshold for awakening notice
  }
};

const clearColdStartTimer = () => {
  if (coldStartTimer) {
    clearTimeout(coldStartTimer);
    coldStartTimer = null;
  }
};

const decrementActiveRequests = () => {
  activeRequests = Math.max(0, activeRequests - 1);
  if (activeRequests === 0) {
    clearColdStartTimer();
    updateColdStartState({
      isColdStarting: false,
      message: '',
      attempt: 0,
    });
  }
};

/**
 * Configured Axios instance for SareeKart API calls.
 * BaseURL defaults to '/api' in development (Vite proxy) and connects
 * to the containerized HTTPS backend in production.
 */
const api = axios.create({
  baseURL: resolveBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30s timeout accommodating cold starts on free-tier hosting
});

// Request interceptor - attach JWT token & track pending requests
api.interceptors.request.use(
  (config) => {
    if (!config.__retryCount) {
      activeRequests += 1;
      startColdStartTimer();
    }

    config.headers = config.headers || {};
    if (!config.headers['X-Request-ID']) {
      config.headers['X-Request-ID'] = `req-${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 8)}`;
    }

    const token = localStorage.getItem('sareekart_token');
    const requestUrl = config.url || '';
    const isAuthRequest = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');
    if (token && !isAuthRequest) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    decrementActiveRequests();
    return Promise.reject(error);
  }
);

// Retry settings for cold-boot absorption
const MAX_RETRIES = 2;
const INITIAL_BACKOFF_MS = 2000;

// Response interceptor - handle cold start retries and auth errors
api.interceptors.response.use(
  (response) => {
    decrementActiveRequests();
    return response;
  },
  async (error) => {
    const config = error.config;
    if (!config) {
      decrementActiveRequests();
      return Promise.reject(error);
    }

    // Determine if the failure is typical of container sleep/cold-boot
    const isTimeout =
      error.code === 'ECONNABORTED' ||
      error.code === 'ETIMEDOUT' ||
      (error.message && error.message.toLowerCase().includes('timeout'));
    const isNetworkError = !error.response && Boolean(error.message);
    const isGatewayError = error.response && [502, 503, 504].includes(error.response.status);
    const isColdStartError = isTimeout || isNetworkError || isGatewayError;

    // Only retry safe / idempotent HTTP methods automatically
    const method = (config.method || 'get').toLowerCase();
    const isIdempotent = ['get', 'head', 'options'].includes(method);

    config.__retryCount = config.__retryCount || 0;

    if (isColdStartError && isIdempotent && config.__retryCount < MAX_RETRIES) {
      config.__retryCount += 1;
      const delayMs = INITIAL_BACKOFF_MS * Math.pow(2, config.__retryCount - 1);

      updateColdStartState({
        isColdStarting: true,
        message: `Awakening our boutique atelier... Reconnecting (attempt ${config.__retryCount}/${MAX_RETRIES})...`,
        attempt: config.__retryCount,
      });

      await new Promise((resolve) => setTimeout(resolve, delayMs));

      return api(config);
    }

    // All retries exhausted or non-retryable failure
    decrementActiveRequests();

    if (error.response?.status === 401 || error.response?.status === 403) {
      const requestUrl = config.url || '';
      const isAuthEndpoint = requestUrl.includes('/auth/');

      if (!isAuthEndpoint) {
        localStorage.removeItem('sareekart_token');
        localStorage.removeItem('sareekart_user');
        const currentPath = window.location.pathname;
        if (currentPath !== '/login') {
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
        }
      }
    }

    const requestId = error.response?.headers?.['x-request-id'] || config.headers?.['X-Request-ID'];
    if (requestId) {
      error.requestId = requestId;
    }

    return Promise.reject(error);
  }
);

export default api;
