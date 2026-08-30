import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import productService from '../../services/productService';

/**
 * Async thunk to fetch products with pagination
 */
export const fetchProducts = createAsyncThunk(
  'products/fetchProducts',
  async (params = {}, { rejectWithValue }) => {
    try {
      const response = await productService.getProducts(params);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch products');
    }
  }
);

/**
 * Async thunk to fetch a single product
 */
export const fetchProductById = createAsyncThunk(
  'products/fetchProductById',
  async (id, { rejectWithValue }) => {
    try {
      const response = await productService.getProductById(id);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch product');
    }
  }
);

/**
 * Async thunk to search products
 */
export const searchProducts = createAsyncThunk(
  'products/searchProducts',
  async ({ query, params = {} }, { rejectWithValue }) => {
    try {
      const response = await productService.searchProducts(query, params);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Search failed');
    }
  }
);

/**
 * Async thunk to filter products
 */
export const filterProducts = createAsyncThunk(
  'products/filterProducts',
  async (filters = {}, { rejectWithValue }) => {
    try {
      const response = await productService.filterProducts(filters);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Filter failed');
    }
  }
);

const initialState = {
  products: [],
  selectedProduct: null,
  loading: false,
  error: null,
  pagination: {
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    last: true,
  },
  filters: {
    categoryId: null,
    minPrice: null,
    maxPrice: null,
    fabric: null,
    sortBy: 'createdAt',
    sortDir: 'desc',
  },
  searchQuery: '',
};

const productSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    setFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = initialState.filters;
    },
    setSearchQuery: (state, action) => {
      state.searchQuery = action.payload;
    },
    clearSelectedProduct: (state) => {
      state.selectedProduct = null;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch products
      .addCase(fetchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload?.content || [];
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements || 0,
          totalPages: action.payload?.totalPages || 0,
          last: action.payload?.last ?? true,
        };
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.error = action.payload || 'Failed to load products';
      })
      // Fetch product by ID
      .addCase(fetchProductById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProductById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedProduct = action.payload || null;
      })
      .addCase(fetchProductById.rejected, (state, action) => {
        state.loading = false;
        state.selectedProduct = null;
        state.error = action.payload || 'Failed to load product';
      })
      // Search products
      .addCase(searchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(searchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload?.content || [];
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements || 0,
          totalPages: action.payload?.totalPages || 0,
          last: true,
        };
      })
      .addCase(searchProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.error = action.payload || 'Search failed';
      })
      // Filter products
      .addCase(filterProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(filterProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.products = action.payload?.content || [];
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements || 0,
          totalPages: action.payload?.totalPages || 0,
          last: action.payload?.last ?? true,
        };
      })
      .addCase(filterProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.error = action.payload || 'Failed to filter products';
      });
  },
});

export const { setFilters, clearFilters, setSearchQuery, clearSelectedProduct, clearError } = productSlice.actions;
export default productSlice.reducer;