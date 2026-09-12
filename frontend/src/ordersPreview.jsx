import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './redux/slices/authSlice';
import orderReducer from './redux/slices/orderSlice';
import cartReducer from './redux/slices/cartSlice';
import productReducer from './redux/slices/productSlice';
import MyOrders from './pages/MyOrders';
import './index.css';

const store = configureStore({
  reducer: {
    auth: authReducer,
    orders: orderReducer,
    cart: cartReducer,
    products: productReducer
  },
  preloadedState: {
    auth: {
      user: {
        firstName: 'Chaitanya',
        lastName: 'Patron',
        role: 'CUSTOMER',
        token: 'local-preview'
      },
      token: 'local-preview',
      isAuthenticated: true,
      loading: false,
      error: null
    },
    orders: {
      orders: [],
      adminOrders: [],
      loading: false,
      error: null
    }
  }
});

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <MemoryRouter initialEntries={['/orders']}>
        <MyOrders />
      </MemoryRouter>
    </Provider>
  </StrictMode>
);
