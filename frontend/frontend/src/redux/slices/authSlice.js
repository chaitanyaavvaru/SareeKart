import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../../api/axiosConfig';

/**
 * Extracts the authenticated user payload from an AuthResponse
 * ({ token, user: { id, email, firstName, lastName, phone, roles[] } }) and
 * derives a convenience `role` field ('ADMIN' | 'CUSTOMER') consumed by
 * ProtectedRoute and Navbar.
 */
function extractAuth(data) {
  const user = {
    ...(data?.user || {}),
    role: data?.user?.roles?.includes('ROLE_ADMIN')
      ? 'ADMIN'
      : 'CUSTOMER',
  };
  return { token: data?.token || null, user };
}

// Async thunk to login user
export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await api.post('/auth/login', credentials);
      const body = response.data;
      if (body?.success && body?.data?.token) {
        const { token, user } = extractAuth(body.data);
        localStorage.setItem('sareekart_token', token);
        localStorage.setItem('sareekart_user', JSON.stringify(user));
        return { token, user };
      }
      return rejectWithValue(body?.message || 'Login failed');
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Login failed');
    }
  }
);

// Async thunk to register user
export const registerUser = createAsyncThunk(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await api.post('/auth/register', userData);
      const body = response.data;
      if (body?.success && body?.data?.token) {
        const { token, user } = extractAuth(body.data);
        localStorage.setItem('sareekart_token', token);
        localStorage.setItem('sareekart_user', JSON.stringify(user));
        return { token, user };
      }
      return rejectWithValue(body?.message || 'Registration failed');
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Registration failed');
    }
  }
);

let userFromStorage = null;
try {
  const storedUser = localStorage.getItem('sareekart_user');
  if (storedUser && storedUser !== 'undefined') {
    userFromStorage = JSON.parse(storedUser);
    // Backfill derived role for sessions persisted by older builds
    if (!userFromStorage.role) {
      userFromStorage.role = userFromStorage.roles?.includes('ROLE_ADMIN')
        ? 'ADMIN'
        : 'CUSTOMER';
    }
  }
} catch (e) {
  console.error('Failed to parse user from localStorage:', e);
}

const tokenFromStorage = localStorage.getItem('sareekart_token');

const initialState = {
  user: userFromStorage,
  token: tokenFromStorage || null,
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
    }
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
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.isAuthenticated = true;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Register
      .addCase(registerUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.isAuthenticated = true;
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  }
});

export const { logout, clearAuthError } = authSlice.actions;
export default authSlice.reducer;