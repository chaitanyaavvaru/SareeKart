import { configureStore } from '@reduxjs/toolkit';
import productReducer from './slices/productSlice';
import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import orderReducer from './slices/orderSlice';
import trousseauReducer from './slices/trousseauSlice';

/**
 * Redux store for SareeKart
 */
const store = configureStore({
  reducer: {
    products: productReducer,
    auth: authReducer,
    cart: cartReducer,
    orders: orderReducer,
    trousseau: trousseauReducer,
  },
  devTools: import.meta.env.DEV,
});

export default store;
