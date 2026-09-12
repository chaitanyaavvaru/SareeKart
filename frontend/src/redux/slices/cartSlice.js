import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import cartService from '../../services/cartService';
import { getGuestCart, saveGuestCart, clearGuestCart } from '../../utils/guestCart';
import { MAX_QUANTITY_PER_SKU } from '../../constants/cartConstants';

// Helper to normalize cart response from backend
function normalizeServerCart(cartResponse) {
  if (!cartResponse) {
    return { items: [], totalPrice: 0, totalItems: 0, hasStockIssues: false };
  }

  const items = (cartResponse.items || []).map((item) => ({
    id: item.productId,
    productId: item.productId,
    cartItemId: item.id,
    name: item.productName,
    price: Number(item.price),
    image: item.productImage || '',
    qty: item.quantity,
    quantity: item.quantity,
    totalPrice: Number(item.totalPrice),
    availableStock: item.availableStock,
    isActive: item.isActive,
    isOutOfStock: item.isOutOfStock,
    quantityExceedsStock: item.quantityExceedsStock,
  }));

  return {
    items,
    totalPrice: Number(cartResponse.totalPrice || 0),
    totalItems: cartResponse.totalItems || items.reduce((sum, i) => sum + i.qty, 0),
    hasStockIssues: Boolean(cartResponse.hasStockIssues),
  };
}

// Async Thunks for Authenticated State Synchronization
export const fetchCart = createAsyncThunk('cart/fetchCart', async (_, { rejectWithValue }) => {
  try {
    const res = await cartService.getCart();
    return res.data;
  } catch (err) {
    return rejectWithValue(err.response?.data?.message || 'Failed to fetch cart');
  }
});

export const addToCartServer = createAsyncThunk(
  'cart/addToCartServer',
  async ({ productId, quantity = 1 }, { rejectWithValue }) => {
    try {
      const res = await cartService.addItem(productId, quantity);
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to add item to cart');
    }
  }
);

export const updateQuantityServer = createAsyncThunk(
  'cart/updateQuantityServer',
  async ({ productId, quantity }, { rejectWithValue }) => {
    try {
      const res = await cartService.updateQuantity(productId, quantity);
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to update quantity');
    }
  }
);

export const removeItemServer = createAsyncThunk(
  'cart/removeItemServer',
  async (productId, { rejectWithValue }) => {
    try {
      const res = await cartService.removeItem(productId);
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to remove item');
    }
  }
);

export const clearCartServer = createAsyncThunk(
  'cart/clearCartServer',
  async (_, { rejectWithValue }) => {
    try {
      await cartService.clearCart();
      return null;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to clear cart');
    }
  }
);

export const mergeGuestCart = createAsyncThunk(
  'cart/mergeGuestCart',
  async (guestItems, { rejectWithValue }) => {
    try {
      const res = await cartService.mergeGuestCart(guestItems);
      clearGuestCart();
      return res.data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to merge guest cart');
    }
  }
);

// Initial state initialized from unexpired guest cart in localStorage
const guestData = getGuestCart();
const initialItems = (guestData.items || []).map((i) => ({
  ...i,
  id: i.productId || i.id,
  productId: i.productId || i.id,
  qty: Math.min(i.qty || i.quantity || 1, MAX_QUANTITY_PER_SKU),
  quantity: Math.min(i.qty || i.quantity || 1, MAX_QUANTITY_PER_SKU),
}));

const initialState = {
  items: initialItems,
  isOpen: false,
  addedItem: null,
  showAddedModal: false,
  loading: false,
  error: null,
  totalPrice: initialItems.reduce((sum, item) => sum + (Number(item.price) || 0) * item.qty, 0),
  totalItems: initialItems.reduce((sum, item) => sum + item.qty, 0),
  hasStockIssues: false,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    toggleCart: (state) => {
      state.isOpen = !state.isOpen;
    },
    setCartOpen: (state, action) => {
      state.isOpen = action.payload;
    },
    closeAddedModal: (state) => {
      state.showAddedModal = false;
      state.addedItem = null;
    },
    // Guest local actions
    addToCart: (state, action) => {
      const product = action.payload;
      const pid = product.productId || product.id;
      const img =
        product.images && product.images.length > 0
          ? product.images[0]
          : product.image || product.productImage || '';

      const existing = state.items.find((i) => (i.productId || i.id) === pid);
      if (existing) {
        existing.qty = Math.min(existing.qty + 1, MAX_QUANTITY_PER_SKU);
        existing.quantity = existing.qty;
      } else {
        state.items.push({
          id: pid,
          productId: pid,
          name: product.name || product.productName,
          price: Number(product.price),
          image: img,
          qty: 1,
          quantity: 1,
          availableStock: product.stockQuantity ?? product.availableStock ?? 10,
          isActive: product.active ?? product.isActive ?? true,
          isOutOfStock: false,
          quantityExceedsStock: false,
        });
      }

      state.totalItems = state.items.reduce((sum, item) => sum + item.qty, 0);
      state.totalPrice = state.items.reduce(
        (sum, item) => sum + (Number(item.price) || 0) * item.qty,
        0
      );

      saveGuestCart(state.items);

      state.addedItem = {
        id: pid,
        name: product.name || product.productName,
        price: product.price,
        image: img,
      };
      state.showAddedModal = true;
    },
    updateQty: (state, action) => {
      const { id, qty } = action.payload;
      const targetId = Number(id);
      const item = state.items.find((i) => (i.productId || i.id) === targetId);

      if (item) {
        if (qty <= 0) {
          state.items = state.items.filter((i) => (i.productId || i.id) !== targetId);
        } else {
          item.qty = Math.min(qty, MAX_QUANTITY_PER_SKU);
          item.quantity = item.qty;
        }
      }

      state.totalItems = state.items.reduce((sum, i) => sum + i.qty, 0);
      state.totalPrice = state.items.reduce(
        (sum, i) => sum + (Number(i.price) || 0) * i.qty,
        0
      );

      saveGuestCart(state.items);
    },
    removeItem: (state, action) => {
      const targetId = Number(action.payload);
      state.items = state.items.filter((i) => (i.productId || i.id) !== targetId);
      state.totalItems = state.items.reduce((sum, i) => sum + i.qty, 0);
      state.totalPrice = state.items.reduce(
        (sum, i) => sum + (Number(i.price) || 0) * i.qty,
        0
      );
      saveGuestCart(state.items);
    },
    clearCart: (state) => {
      state.items = [];
      state.totalItems = 0;
      state.totalPrice = 0;
      state.hasStockIssues = false;
      clearGuestCart();
    },
    setCartFromServer: (state, action) => {
      const normalized = normalizeServerCart(action.payload);
      state.items = normalized.items;
      state.totalPrice = normalized.totalPrice;
      state.totalItems = normalized.totalItems;
      state.hasStockIssues = normalized.hasStockIssues;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch Cart
      .addCase(fetchCart.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.loading = false;
        const normalized = normalizeServerCart(action.payload);
        state.items = normalized.items;
        state.totalPrice = normalized.totalPrice;
        state.totalItems = normalized.totalItems;
        state.hasStockIssues = normalized.hasStockIssues;
      })
      .addCase(fetchCart.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      // Add to Cart Server
      .addCase(addToCartServer.fulfilled, (state, action) => {
        const normalized = normalizeServerCart(action.payload);
        state.items = normalized.items;
        state.totalPrice = normalized.totalPrice;
        state.totalItems = normalized.totalItems;
        state.hasStockIssues = normalized.hasStockIssues;
        state.showAddedModal = true;
      })

      // Update Quantity Server
      .addCase(updateQuantityServer.fulfilled, (state, action) => {
        const normalized = normalizeServerCart(action.payload);
        state.items = normalized.items;
        state.totalPrice = normalized.totalPrice;
        state.totalItems = normalized.totalItems;
        state.hasStockIssues = normalized.hasStockIssues;
      })

      // Remove Item Server
      .addCase(removeItemServer.fulfilled, (state, action) => {
        const normalized = normalizeServerCart(action.payload);
        state.items = normalized.items;
        state.totalPrice = normalized.totalPrice;
        state.totalItems = normalized.totalItems;
        state.hasStockIssues = normalized.hasStockIssues;
      })

      // Clear Cart Server
      .addCase(clearCartServer.fulfilled, (state) => {
        state.items = [];
        state.totalPrice = 0;
        state.totalItems = 0;
        state.hasStockIssues = false;
        clearGuestCart();
      })

      // Merge Guest Cart
      .addCase(mergeGuestCart.fulfilled, (state, action) => {
        const normalized = normalizeServerCart(action.payload);
        state.items = normalized.items;
        state.totalPrice = normalized.totalPrice;
        state.totalItems = normalized.totalItems;
        state.hasStockIssues = normalized.hasStockIssues;
      });
  },
});

export const {
  toggleCart,
  setCartOpen,
  closeAddedModal,
  addToCart,
  updateQty,
  removeItem,
  clearCart,
  setCartFromServer,
} = cartSlice.actions;

export default cartSlice.reducer;
