import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  items: [], // [{ id, name, price, image, qty }]
  isOpen: false,
  addedItem: null,
  showAddedModal: false,
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
    addToCart: (state, action) => {
      const product = action.payload;
      const img = product.images && product.images.length > 0 ? product.images[0] : (product.image || '');
      const existing = state.items.find(i => i.id === product.id);
      if (existing) {
        existing.qty += 1;
      } else {
        state.items.push({
          id: product.id,
          name: product.name,
          price: product.price,
          image: img,
          qty: 1
        });
      }
      state.addedItem = {
        id: product.id,
        name: product.name,
        price: product.price,
        image: img
      };
      state.showAddedModal = true;
    },
    closeAddedModal: (state) => {
      state.showAddedModal = false;
      state.addedItem = null;
    },
    updateQty: (state, action) => {
      const { id, qty } = action.payload;
      const item = state.items.find(i => i.id === id);
      if (item) {
        if (qty <= 0) {
          state.items = state.items.filter(i => i.id !== id);
        } else {
          item.qty = qty;
        }
      }
    },
    removeItem: (state, action) => {
      state.items = state.items.filter(i => i.id !== action.payload);
    },
    clearCart: (state) => {
      state.items = [];
    }
  }
});

export const { toggleCart, setCartOpen, addToCart, closeAddedModal, updateQty, removeItem, clearCart } = cartSlice.actions;
export default cartSlice.reducer;
