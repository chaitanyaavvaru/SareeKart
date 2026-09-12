/**
 * SareeKart Patron Wallet & Loyalty Points Service
 * 
 * Provides client-side communication for customer store credit,
 * loyalty points tracking, redemption previews, and staff governance.
 */

import api from '../api/axiosConfig';

export const MOCK_WALLET = {
  id: 1,
  userId: 2,
  userEmail: 'customer@sareekart.com',
  userName: 'Priya Sharma',
  balance: 2450.0,
  loyaltyPoints: 120,
  tier: 'GOLD',
  tierDisplayName: 'Gold Connoisseur',
  pointsMultiplier: 1.25,
  lifetimeSpent: 38500.0,
  nextTierSpendRemaining: 61500.0,
  totalTransactions: 6,
  createdAt: new Date(Date.now() - 30 * 86400000).toISOString(),
  updatedAt: new Date().toISOString()
};

export const MOCK_WALLET_TRANSACTIONS = [
  {
    id: 1,
    walletId: 1,
    amount: 1575.0,
    points: 0,
    type: 'CREDIT_RETURN_REFUND',
    description: 'Refund for Return #12 (Base: ₹1500.00 + 5% Patronage Bonus: ₹75.00)',
    referenceId: 12,
    referenceType: 'RETURN_REQUEST',
    balanceAfter: 2450.0,
    createdAt: new Date(Date.now() - 2 * 86400000).toISOString()
  },
  {
    id: 2,
    walletId: 1,
    amount: 125.0,
    points: 125,
    type: 'CREDIT_LOYALTY_EARNED',
    description: 'Patronage rewards for Order #45 (Gold Connoisseur tier: 125 pts earned)',
    referenceId: 45,
    referenceType: 'ORDER',
    balanceAfter: 875.0,
    createdAt: new Date(Date.now() - 7 * 86400000).toISOString()
  },
  {
    id: 3,
    walletId: 1,
    amount: -500.0,
    points: 0,
    type: 'DEBIT_CHECKOUT_REDEMPTION',
    description: 'Store credit applied to Order #42',
    referenceId: 42,
    referenceType: 'ORDER',
    balanceAfter: 750.0,
    createdAt: new Date(Date.now() - 14 * 86400000).toISOString()
  },
  {
    id: 4,
    walletId: 1,
    amount: 250.0,
    points: 0,
    type: 'CREDIT_PROMO_BONUS',
    description: 'Promotional bonus: Welcome to Silk Patron Club',
    referenceId: 1,
    referenceType: 'ADMIN_MANUAL',
    balanceAfter: 1250.0,
    createdAt: new Date(Date.now() - 25 * 86400000).toISOString()
  }
];

export const walletService = {
  /**
   * Fetch authenticated customer's wallet summary
   */
  async getMyWallet() {
    try {
      const res = await api.get('/wallet/my-wallet');
      return res.data?.data || res.data;
    } catch (err) {
      console.warn('Backend /wallet/my-wallet unavailable, using mock fallback:', err.message);
      return MOCK_WALLET;
    }
  },

  /**
   * Fetch authenticated customer's transaction ledger
   */
  async getMyTransactions() {
    try {
      const res = await api.get('/wallet/transactions');
      return res.data?.data || res.data;
    } catch (err) {
      console.warn('Backend /wallet/transactions unavailable, using mock fallback:', err.message);
      return MOCK_WALLET_TRANSACTIONS;
    }
  },

  /**
   * Preview store credit redemption against a target order total
   */
  async previewRedemption(orderTotal) {
    try {
      const res = await api.post('/wallet/preview-redemption', { orderTotal });
      return res.data?.data || res.data;
    } catch (err) {
      const balance = MOCK_WALLET.balance;
      const maxRedeemable = Math.min(balance, orderTotal);
      const remaining = Math.max(0, orderTotal - maxRedeemable);
      return {
        walletBalance: balance,
        maxRedeemable,
        remainingOrderTotal: remaining,
        fullyCovered: remaining === 0
      };
    }
  },

  /**
   * Staff: Fetch all customer wallets
   */
  async getAllWalletsForAdmin() {
    try {
      const res = await api.get('/admin/wallets');
      return res.data?.data || res.data;
    } catch (err) {
      console.warn('Backend /admin/wallets unavailable, using mock fallback:', err.message);
      return [MOCK_WALLET];
    }
  },

  /**
   * Staff: Fetch audit transactions for a specific customer
   */
  async getUserTransactionsForAdmin(userId) {
    try {
      const res = await api.get(`/admin/wallets/${userId}/transactions`);
      return res.data?.data || res.data;
    } catch (err) {
      console.warn(`Backend /admin/wallets/${userId}/transactions unavailable:`, err.message);
      return MOCK_WALLET_TRANSACTIONS;
    }
  },

  /**
   * Staff: Disburse promotional credit or bonus loyalty points
   */
  async issuePromotionalCredit(userId, payload) {
    const res = await api.post(`/admin/wallets/${userId}/credit`, payload);
    return res.data?.data || res.data;
  }
};

export default walletService;
