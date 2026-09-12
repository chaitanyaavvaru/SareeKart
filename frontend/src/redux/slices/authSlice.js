import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axiosConfig';

/**
 * Maps raw network/server errors to human-readable authentication error messages.
 */
function getServerMessage(error) {
  const responseData = error.response?.data;

  if (typeof responseData === 'string' && responseData.trim()) {
    return responseData.trim();
  }

  if (responseData?.message && responseData.message !== 'Validation failed') {
    return responseData.message;
  }

  const validationMessages = responseData?.data && typeof responseData.data === 'object'
    ? Object.values(responseData.data).filter(Boolean)
    : [];

  return validationMessages.length > 0
    ? validationMessages.join(' ')
    : responseData?.message;
}

function extractAuthError(error) {
  // Network error — no response at all (server down, wrong port, ECONNREFUSED)
  if (!error.response) {
    if (error.code === 'ECONNABORTED') {
      return 'Request timed out. Please check your connection and try again.';
    }
    return 'Cannot connect to server. Please make sure the backend is running on port 8081.';
  }

  const status = error.response.status;
  const serverMessage = getServerMessage(error);

  switch (status) {
    case 502:
    case 503:
    case 504:
      return 'The backend service is unavailable. Please start the server on port 8081 and try again.';
    case 400:
      return serverMessage || 'Invalid request. Please check your input.';
    case 401:
      // Backend now returns specific messages for wrong password, disabled accounts etc.
      return serverMessage || 'Incorrect email or password. Please try again.';
    case 403:
      return serverMessage || 'This account is not authorized to complete registration.';
    case 409:
      return serverMessage || 'An account with this email already exists.';
    case 404:
      return 'No account found with this email address.';
    case 500:
      return 'A server error occurred. Please try again later.';
    default:
      return serverMessage || 'An unexpected error occurred. Please try again.';
  }
}

// Async thunk to login user
export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await api.post('/auth/login', credentials);
      const data = response.data;
      if (data.success && data.data) {
        localStorage.setItem('sareekart_token', data.data.token);
        localStorage.setItem('sareekart_user', JSON.stringify(data.data));
        return data.data;
      }
      return rejectWithValue(data.message || 'Login failed. Please try again.');
    } catch (error) {
      return rejectWithValue(extractAuthError(error));
    }
  }
);

// Async thunk to register user
export const registerUser = createAsyncThunk(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await api.post('/auth/register', userData);
      const data = response.data;
      if (data.success && data.data) {
        localStorage.setItem('sareekart_token', data.data.token);
        localStorage.setItem('sareekart_user', JSON.stringify(data.data));
        return data.data;
      }
      return rejectWithValue(data.message || 'Registration failed. Please try again.');
    } catch (error) {
      return rejectWithValue(extractAuthError(error));
    }
  }
);

// Safe localStorage hydration
let userFromStorage = null;
try {
  const storedUser = localStorage.getItem('sareekart_user');
  if (storedUser && storedUser !== 'undefined' && storedUser !== 'null') {
    userFromStorage = JSON.parse(storedUser);
  }
} catch (e) {
  console.error('Failed to parse user from localStorage:', e);
  localStorage.removeItem('sareekart_user');
}

const tokenFromStorage = localStorage.getItem('sareekart_token');

const initialState = {
  user: userFromStorage,
  token: tokenFromStorage || null,
  // Only mark as authenticated if BOTH token AND user data exist
  isAuthenticated: !!tokenFromStorage && !!userFromStorage,
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      localStorage.removeItem('sareekart_token');
      localStorage.removeItem('sareekart_user');
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      state.error = null;
    },
    clearAuthError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(loginUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.token = action.payload.token;
        state.isAuthenticated = true;
        state.error = null;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
        state.isAuthenticated = false;
      })
      // Register
      .addCase(registerUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.token = action.payload.token;
        state.isAuthenticated = true;
        state.error = null;
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
        state.isAuthenticated = false;
      });
  },
});

export const { logout, clearAuthError } = authSlice.actions;
export default authSlice.reducer;
