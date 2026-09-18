import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import store from './redux/store';
import AppRouter from './routes/AppRouter';
import { CurrencyProvider } from './context/CurrencyContext';
import ErrorBoundary from './components/common/ErrorBoundary';
import './index.css';

// Dynamic production analytics & search console initialization
if (typeof window !== 'undefined') {
  const ga4Id = import.meta.env.VITE_GA4_MEASUREMENT_ID;
  if (ga4Id && !window.gtag) {
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${ga4Id}`;
    document.head.appendChild(script);

    window.dataLayer = window.dataLayer || [];
    window.gtag = function () {
      window.dataLayer.push(arguments);
    };
    window.gtag('js', new Date());
    window.gtag('config', ga4Id, { send_page_view: false });
  }

  const gscToken = import.meta.env.VITE_GSC_VERIFICATION;
  if (gscToken && !document.querySelector('meta[name="google-site-verification"]')) {
    const meta = document.createElement('meta');
    meta.name = 'google-site-verification';
    meta.content = gscToken;
    document.head.appendChild(meta);
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <CurrencyProvider>
        <BrowserRouter>
          <ErrorBoundary>
            <AppRouter />
          </ErrorBoundary>
        </BrowserRouter>
      </CurrencyProvider>
    </Provider>
  </React.StrictMode>
);
