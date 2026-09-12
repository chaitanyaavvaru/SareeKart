import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import productService from '../../services/productService';

export const MOCK_PRODUCTS = [
  {
    id: 1,
    name: "Banarasi Silk Saree",
    price: 4999,
    categoryName: "Banarasi",
    fabric: "Silk",
    occasion: "Festive",
    images: ["https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250"],
    description: "Elegant Banarasi silk saree with gold zari work."
  },
  {
    id: 2,
    name: "Taranga Kanchi Silk Tissue Brocade Gold Saree",
    price: 26133,
    categoryName: "Kanchipuram",
    fabric: "Silk",
    occasion: "Bridal",
    images: ["https://kankatala.com/cdn/shop/files/1215863175_2.webp?v=1761908736"],
    description: "Exquisite handwoven gold tissue Kanchipuram silk saree."
  },
  {
    id: 3,
    name: "Venkatagiri Cotton Butta Black Saree With Jamdani Pallu",
    price: 18667,
    categoryName: "Designer",
    fabric: "Cotton",
    occasion: "Casual",
    images: ["https://kankatala.com/cdn/shop/files/1216423158_1.webp?v=1780134877"],
    description: "Comfortable and traditional daily wear cotton saree."
  },
  {
    id: 4,
    name: "Pochampally Silk Ikat Purple Saree",
    price: 34111,
    categoryName: "Pochampally",
    fabric: "Silk",
    occasion: "Party",
    images: ["https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134"],
    description: "Vibrant Pochampally silk ikat saree in royal purple."
  },
  {
    id: 5,
    name: "Uppada Silk Saree",
    price: 38400,
    categoryName: "Uppada",
    fabric: "Silk",
    occasion: "Premium",
    images: ["https://kankatala.com/cdn/shop/files/1216039129_1.webp?v=1777711821"],
    description: "Premium Uppada silk saree directly from master weavers."
  },
  {
    id: 6,
    name: "Tussar Silk Saree",
    price: 26510,
    categoryName: "Tussar",
    fabric: "Silk",
    occasion: "Traditional",
    images: ["https://kankatala.com/cdn/shop/files/1215745847_1.webp?v=1777723358&width=1946"],
    description: "Elegant Tussar silk saree with delicate borders."
  }
];

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
    q: '',
    category: null,
    categoryId: null,
    categoryName: null,
    minPrice: null,
    maxPrice: null,
    fabric: null,
    occasion: null,
    color: null,
    colorFamily: null,
    inStock: false,
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
        const fetched = action.payload?.content || [];
        state.products = fetched;
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements ?? fetched.length,
          totalPages: action.payload?.totalPages ?? (fetched.length > 0 ? 1 : 0),
          last: action.payload?.last ?? true,
        };
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.pagination = {
          page: 0,
          size: 12,
          totalElements: 0,
          totalPages: 0,
          last: true,
        };
        state.error = action.payload || 'Failed to fetch products';
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
        const id = action.meta.arg;
        const mockProd = MOCK_PRODUCTS.find(p => p.id === Number(id));
        if (mockProd) {
          state.selectedProduct = mockProd;
          state.error = null;
        } else {
          state.error = action.payload;
        }
      })
      // Search products
      .addCase(searchProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(searchProducts.fulfilled, (state, action) => {
        state.loading = false;
        const fetched = action.payload?.content || [];
        state.products = fetched;
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements ?? fetched.length,
          totalPages: action.payload?.totalPages ?? (fetched.length > 0 ? 1 : 0),
          last: action.payload?.last ?? true,
        };
      })
      .addCase(searchProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.pagination = {
          page: 0,
          size: 12,
          totalElements: 0,
          totalPages: 0,
          last: true,
        };
        state.error = action.payload || 'Search failed';
      })
      // Filter products
      .addCase(filterProducts.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(filterProducts.fulfilled, (state, action) => {
        state.loading = false;
        const fetched = action.payload?.content || [];
        state.products = fetched;
        state.pagination = {
          page: action.payload?.page || 0,
          size: action.payload?.size || 12,
          totalElements: action.payload?.totalElements ?? fetched.length,
          totalPages: action.payload?.totalPages ?? (fetched.length > 0 ? 1 : 0),
          last: action.payload?.last ?? true,
        };
      })
      .addCase(filterProducts.rejected, (state, action) => {
        state.loading = false;
        state.products = [];
        state.pagination = {
          page: 0,
          size: 12,
          totalElements: 0,
          totalPages: 0,
          last: true,
        };
        state.error = action.payload || 'Filter failed';
      });
  },
});

export const { setFilters, clearFilters, setSearchQuery, clearSelectedProduct, clearError } = productSlice.actions;
export default productSlice.reducer;
