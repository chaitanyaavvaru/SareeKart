import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import store from './redux/store';
import AppRouter from './routes/AppRouter';
import { CurrencyProvider } from './context/CurrencyContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <CurrencyProvider>
        <BrowserRouter>
          <AppRouter />
        </BrowserRouter>
      </CurrencyProvider>
    </Provider>
  </React.StrictMode>
);
