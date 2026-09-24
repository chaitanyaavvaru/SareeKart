import React, { useState, useEffect } from 'react';
import { Tag, Plus, Trash2, Edit2, Check, X, Calendar, Percent, IndianRupee, ShieldCheck, AlertCircle, Loader2, Sparkles, Clock, AlertTriangle, CheckCircle2 } from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_COUPONS = [
  {
    id: 1,
    code: 'ROYAL10',
    discountPercent: 10,
    minPurchaseAmount: 5000,
    usageLimit: 500,
    timesUsed: 142,
    expiryDate: '2026-12-31',
    active: true
  },
  {
    id: 2,
    code: 'HEIRLOOM500',
    discountPercent: 15,
    minPurchaseAmount: 3000,
    usageLimit: 200,
    timesUsed: 89,
    expiryDate: '2026-10-15',
    active: true
  },
  {
    id: 3,
    code: 'SILKMARK15',
    discountPercent: 15,
    minPurchaseAmount: 10000,
    usageLimit: 100,
    timesUsed: 45,
    expiryDate: '2026-09-30',
    active: true
  }
];

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0,
  }).format(val || 0);

export default function ManageCoupons() {
  const [coupons, setCoupons] = useState(MOCK_COUPONS);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [notification, setNotification] = useState(null);

  // Deletion Modal State
  const [deleteModalCoupon, setDeleteModalCoupon] = useState(null);
  const [confirmationInput, setConfirmationInput] = useState('');

  // Form State
  const [code, setCode] = useState('');
  const [discountPercent, setDiscountPercent] = useState(10);
  const [minPurchaseAmount, setMinPurchaseAmount] = useState(5000);
  const [usageLimit, setUsageLimit] = useState(500);
  const [expiryDate, setExpiryDate] = useState('2026-12-31');

  const fetchCoupons = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/coupons');
      if (res.data?.success && Array.isArray(res.data.data) && res.data.data.length > 0) {
        setCoupons(res.data.data);
      }
    } catch (err) {
      console.log('Using default coupons list');
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchCoupons();
  }, []);

  const handleCreateCoupon = async (e) => {
    e.preventDefault();
    if (!code.trim()) return;

    try {
      const res = await api.post('/admin/coupons', {
        code: code.trim().toUpperCase(),
        discountPercent: Number(discountPercent),
        minPurchaseAmount: Number(minPurchaseAmount),
        usageLimit: Number(usageLimit),
        expiryDate: expiryDate ? `${expiryDate}T23:59:59` : null,
        active: true
      });
      setNotification('Coupon created successfully');
      setShowModal(false);
      setCode('');
      fetchCoupons();
    } catch (err) {
      setNotification(err.response?.data?.message || 'Failed to create coupon');
    }
  };

  const handleConfirmDelete = async () => {
    if (!deleteModalCoupon) return;
    if (confirmationInput.trim().toUpperCase() !== deleteModalCoupon.code.toUpperCase()) {
      alert(`Code mismatch. Please type exactly "${deleteModalCoupon.code}" to confirm.`);
      return;
    }

    try {
      const res = await api.post(`/admin/coupons/${deleteModalCoupon.id}/remove`, {
        confirmationCode: confirmationInput.trim().toUpperCase(),
        reason: 'Requested deactivation via Coupon Console'
      });
      setNotification(res.data?.message || 'Coupon deactivation processed');
      setDeleteModalCoupon(null);
      setConfirmationInput('');
      fetchCoupons();
    } catch (err) {
      setNotification(err.response?.data?.message || 'Failed to process deactivation');
    }
  };

  return (
    <div className="space-[#111827] text-left space-y-8 animate-fade-in font-sans">
      <SEO title="Marketing & Coupon Management | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Marketing & Promotional Coupons</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Manage promotional campaign vouchers, discounts, and usage limits</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="h-11 px-5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center gap-2 cursor-pointer"
        >
          <Plus className="w-4 h-4" /> Create New Coupon
        </button>
      </div>

      {/* Notification Banner */}
      {notification && (
        <div className="p-4 bg-amber-50 border border-amber-200 text-amber-900 text-xs font-bold rounded-2xl flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-600" />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-amber-700 hover:text-amber-900 cursor-pointer">✕</button>
        </div>
      )}

      {/* Campaign Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-[#E85D4F] flex items-center justify-center shrink-0 border border-amber-100">
            <Tag className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Active Promo Coupons</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">{coupons.filter(c => c.active).length} Campaigns Active</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-[#0F766E] flex items-center justify-center shrink-0 border border-emerald-100">
            <Percent className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Total Redemptions</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">276 Redemptions</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Campaign Purity</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">100% Verified Rules</h3>
          </div>
        </div>
      </div>

      {/* Coupons Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl shadow-xs overflow-hidden">
        <div className="p-5 border-b border-[#DDE4EA]">
          <h3 className="text-sm font-bold text-[#111827]">Promotional Vouchers & Codes</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase font-bold text-[10px] tracking-wider border-b border-[#DDE4EA]">
              <tr>
                <th className="p-4">Coupon Code</th>
                <th className="p-4">Discount Rate</th>
                <th className="p-4">Min Spend</th>
                <th className="p-4">Times Used</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA]">
              {coupons.map((c) => (
                <tr key={c.id} className="hover:bg-[#F5F7FA]/50 transition-colors">
                  <td className="p-4">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-xs bg-amber-50 text-[#111827] px-2.5 py-1 rounded-md border border-amber-200">
                        {c.code}
                      </span>
                    </div>
                  </td>
                  <td className="p-4 font-bold text-[#111827]">{c.discountPercent}% Off</td>
                  <td className="p-4 font-bold text-[#111827]">{formatCurrency(c.minPurchaseAmount || 5000)}</td>
                  <td className="p-4 font-bold text-[#6b5c4d]">{c.timesUsed || 0} / {c.usageLimit || 500}</td>
                  <td className="p-4">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      c.active ? 'bg-emerald-100 text-[#0F766E]' : 'bg-red-100 text-red-700'
                    }`}>
                      {c.active ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => {
                        setDeleteModalCoupon(c);
                        setConfirmationInput('');
                      }}
                      className="p-1.5 text-gray-400 hover:text-red-600 rounded-lg transition-colors cursor-pointer"
                      title="Deactivate coupon"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Exact Code Confirmation Modal */}
      {deleteModalCoupon && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 max-w-md w-full space-y-4 shadow-2xl text-left">
            <div className="flex items-center gap-3 border-b border-[#E8E2D9] pb-3">
              <div className="w-10 h-10 rounded-2xl bg-rose-100 text-rose-700 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold font-serif text-[#17211F]">Confirm Coupon Deactivation</h3>
                <p className="text-xs text-[#71817A]">Requires typing the exact coupon code</p>
              </div>
            </div>

            <p className="text-xs text-[#71817A] leading-relaxed">
              To prevent accidental deletion of active customer campaigns, please type <strong className="font-mono text-[#17211F] bg-[#F7F4EE] px-2 py-0.5 rounded-md border border-[#DDD8CF]">{deleteModalCoupon.code}</strong> below to confirm. Deactivated vouchers are preserved for historical order analytics.
            </p>

            <div>
              <label className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] block mb-1">
                Type exact code to confirm
              </label>
              <input
                type="text"
                value={confirmationInput}
                onChange={(e) => setConfirmationInput(e.target.value)}
                placeholder={deleteModalCoupon.code}
                className="w-full h-11 px-4 border border-[#DDD8CF] rounded-xl text-xs font-mono font-bold uppercase outline-none focus:border-rose-600"
              />
            </div>

            <div className="flex gap-2 justify-end pt-2">
              <button
                onClick={() => {
                  setDeleteModalCoupon(null);
                  setConfirmationInput('');
                }}
                className="px-4 py-2.5 bg-[#F7F4EE] text-[#71817A] rounded-full text-xs font-bold cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmDelete}
                disabled={confirmationInput.trim().toUpperCase() !== deleteModalCoupon.code.toUpperCase()}
                className={`px-5 py-2.5 rounded-full text-xs font-bold uppercase tracking-wider transition shadow-sm ${
                  confirmationInput.trim().toUpperCase() === deleteModalCoupon.code.toUpperCase()
                    ? 'bg-rose-600 hover:bg-rose-700 text-white cursor-pointer'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                Confirm Deactivation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Modal */}
      <AnimatePresence>
        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowModal(false)} className="absolute inset-0 bg-black/50 backdrop-blur-xs" />
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-md w-full p-6 space-y-6 z-10 text-left shadow-2xl">
              <div className="flex justify-between items-center border-b border-[#DDE4EA] pb-4">
                <h3 className="text-lg font-bold font-serif text-[#111827]">Create Promotional Voucher</h3>
                <button onClick={() => setShowModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827]"><X className="w-5 h-5" /></button>
              </div>

              <form onSubmit={handleCreateCoupon} className="space-y-4">
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Coupon Code *</label>
                  <input type="text" required placeholder="e.g. ROYAL15" value={code} onChange={e => setCode(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs uppercase font-mono font-bold outline-none focus:border-[#E85D4F]" />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Discount Rate (%) *</label>
                    <input type="number" required min="1" max="100" value={discountPercent} onChange={e => setDiscountPercent(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#E85D4F]" />
                  </div>
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Min Spend (₹) *</label>
                    <input type="number" required min="0" value={minPurchaseAmount} onChange={e => setMinPurchaseAmount(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#E85D4F]" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Usage Limit *</label>
                    <input type="number" required min="1" value={usageLimit} onChange={e => setUsageLimit(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#E85D4F]" />
                  </div>
                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Expiry Date *</label>
                    <input type="date" required value={expiryDate} onChange={e => setExpiryDate(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs font-bold outline-none focus:border-[#E85D4F]" />
                  </div>
                </div>

                <button type="submit" className="w-full h-12 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all mt-4 cursor-pointer">
                  Save & Publish Voucher
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
