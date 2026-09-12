import React, { createContext, useContext, useState } from 'react';

export const CURRENCIES = {
  INR: { code: 'INR', symbol: '₹', name: 'Indian Rupee', flag: '🇮🇳', rate: 1.0, locale: 'en-IN' },
  USD: { code: 'USD', symbol: '$', name: 'US Dollar', flag: '🇺🇸', rate: 0.012, locale: 'en-US' },
  GBP: { code: 'GBP', symbol: '£', name: 'British Pound', flag: '🇬🇧', rate: 0.0095, locale: 'en-GB' },
  EUR: { code: 'EUR', symbol: '€', name: 'Euro', flag: '🇪🇺', rate: 0.011, locale: 'de-DE' },
  AED: { code: 'AED', symbol: 'AED ', name: 'UAE Dirham', flag: '🇦🇪', rate: 0.044, locale: 'en-AE' },
  CAD: { code: 'CAD', symbol: 'CA$', name: 'Canadian Dollar', flag: '🇨🇦', rate: 0.016, locale: 'en-CA' },
  AUD: { code: 'AUD', symbol: 'AU$', name: 'Australian Dollar', flag: '🇦🇺', rate: 0.018, locale: 'en-AU' },
  SGD: { code: 'SGD', symbol: 'SG$', name: 'Singapore Dollar', flag: '🇸🇬', rate: 0.016, locale: 'en-SG' },
  MYR: { code: 'MYR', symbol: 'RM ', name: 'Malaysian Ringgit', flag: '🇲🇾', rate: 0.052, locale: 'en-MY' },
  CHF: { code: 'CHF', symbol: 'CHF ', name: 'Swiss Franc', flag: '🇨🇭', rate: 0.0105, locale: 'de-CH' },
};

const CurrencyContext = createContext();

export function CurrencyProvider({ children }) {
  const [currency, setCurrencyState] = useState(() => {
    return localStorage.getItem('sareekart_currency') || 'INR';
  });

  const setCurrency = (code) => {
    if (CURRENCIES[code]) {
      setCurrencyState(code);
      localStorage.setItem('sareekart_currency', code);
    }
  };

  const activeCurrency = CURRENCIES[currency] || CURRENCIES.INR;

  const getExchangeRateInfo = (code = currency) => {
    const curr = CURRENCIES[code] || activeCurrency;
    if (curr.code === 'INR') {
      return { code: 'INR', description: 'Base Currency (1 INR = ₹1.00)' };
    }
    const inrPerUnit = (1 / curr.rate).toFixed(2);
    return {
      code: curr.code,
      symbol: curr.symbol,
      rate: curr.rate,
      inrPerUnit,
      description: `1 ${curr.code} ≈ ₹${inrPerUnit} INR`,
    };
  };

  const formatPrice = (inrAmount) => {
    if (inrAmount === null || inrAmount === undefined || isNaN(Number(inrAmount))) {
      return `${activeCurrency.symbol}0`;
    }
    const numericInr = Number(inrAmount);
    const converted = numericInr * activeCurrency.rate;

    if (activeCurrency.code === 'INR') {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0,
      }).format(numericInr);
    }

    return new Intl.NumberFormat(activeCurrency.locale, {
      style: 'currency',
      currency: activeCurrency.code,
      maximumFractionDigits: 0,
    }).format(Math.round(converted));
  };

  return (
    <CurrencyContext.Provider
      value={{
        currency,
        setCurrency,
        activeCurrency,
        formatPrice,
        getExchangeRateInfo,
        currencies: CURRENCIES,
      }}
    >
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const context = useContext(CurrencyContext);
  if (!context) {
    return {
      currency: 'INR',
      setCurrency: () => {},
      activeCurrency: CURRENCIES.INR,
      formatPrice: (val) => `₹${Number(val || 0).toLocaleString('en-IN')}`,
      currencies: CURRENCIES,
    };
  }
  return context;
}
