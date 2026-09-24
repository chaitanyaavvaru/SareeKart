import { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  RotateCcw, 
  Search, 
  Clock, 
  Truck, 
  CheckCircle2, 
  XCircle, 
  ShieldCheck, 
  Eye, 
  Copy, 
  Check, 
  RefreshCw, 
  AlertTriangle, 
  ChevronRight, 
  ChevronLeft,
  X, 
} from 'lucide-react';
import returnService from '../../services/returnService';
import SEO from '../../components/common/SEO';

// Resilient Offline Mock Claims Fallback
const MOCK_ADMIN_RETURNS = [
  {
    id: 101,
    orderId: 37,
    userId: 2,
    customerName: 'Kalyani Sundaram',
    customerEmail: 'kalyani.s@example.com',
    customerMobile: '+91 98401 23456',
    orderTotalAmount: 18500,
    refundAmount: 18500,
    refundMode: 'ORIGINAL_PAYMENT',
    type: 'RETURN',
    reason: 'COLOR_MISMATCH',
    comments: 'The saree shade is deep crimson rather than vermilion red displayed on the product page. Photographs show studio daylight difference.',
    status: 'PENDING',
    exchangeSku: null,
    images: [
      'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80',
      'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80'
    ],
    reverseCourier: null,
    reverseTrackingNumber: null,
    adminNotes: null,
    createdAt: new Date(Date.now() - 14 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 14 * 3600000).toISOString(),
    orderDeliveredAt: new Date(Date.now() - 3 * 86400000).toISOString(),
    daysSinceDelivery: 3
  },
  {
    id: 102,
    orderId: 38,
    userId: 3,
    customerName: 'Meenakshi Iyer',
    customerEmail: 'meenakshi.iyer@example.com',
    customerMobile: '+91 98200 98765',
    orderTotalAmount: 24000,
    refundAmount: 24000,
    refundMode: 'EXCHANGE_DRAPE',
    type: 'EXCHANGE',
    reason: 'ZARI_DEFECT',
    comments: 'Frayed metallic zari threads on the lower pallu border. Warp threads loose.',
    status: 'APPROVED',
    exchangeSku: 'KAN-SILK-MRN-02',
    images: [
      'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=800&q=80'
    ],
    reverseCourier: null,
    reverseTrackingNumber: null,
    adminNotes: 'Condition verified from photos. Approved for reverse pickup.',
    createdAt: new Date(Date.now() - 36 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 10 * 3600000).toISOString(),
    orderDeliveredAt: new Date(Date.now() - 4 * 86400000).toISOString(),
    daysSinceDelivery: 4
  },
  {
    id: 103,
    orderId: 35,
    userId: 4,
    customerName: 'Ananya Sharma',
    customerEmail: 'ananya.sharma@example.com',
    customerMobile: '+91 99100 11223',
    orderTotalAmount: 14200,
    refundAmount: 14200,
    refundMode: 'STORE_CREDIT',
    type: 'RETURN',
    reason: 'FABRIC_FEEL',
    comments: 'Silk feel is stiffer than anticipated; requesting wallet store credit.',
    status: 'PICKUP_SCHEDULED',
    exchangeSku: null,
    images: [
      'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80'
    ],
    reverseCourier: 'Blue Dart Reverse Logistics',
    reverseTrackingNumber: 'BDR-RET-35-8912',
    adminNotes: 'Courier scheduled for doorstep pickup.',
    createdAt: new Date(Date.now() - 72 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 24 * 3600000).toISOString(),
    orderDeliveredAt: new Date(Date.now() - 5 * 86400000).toISOString(),
    daysSinceDelivery: 5
  },
  {
    id: 104,
    orderId: 31,
    userId: 5,
    customerName: 'Pooja Hegde',
    customerEmail: 'pooja.h@example.com',
    customerMobile: '+91 97400 55667',
    orderTotalAmount: 31000,
    refundAmount: 31000,
    refundMode: 'ORIGINAL_PAYMENT',
    type: 'RETURN',
    reason: 'INCORRECT_ITEM',
    comments: 'Received Banarasi Tanchoi instead of Kanchipuram Brocade ordered.',
    status: 'COMPLETED',
    exchangeSku: null,
    images: [
      'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80'
    ],
    reverseCourier: 'Delhivery Reverse',
    reverseTrackingNumber: 'DEL-RET-31-0045',
    adminNotes: 'Item received at Bengaluru central hub, inspection verified, refund disbursed.',
    createdAt: new Date(Date.now() - 120 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 48 * 3600000).toISOString(),
    orderDeliveredAt: new Date(Date.now() - 6 * 86400000).toISOString(),
    daysSinceDelivery: 6
  }
];

const REASON_LABELS = {
  COLOR_MISMATCH: 'Color Mismatch',
  ZARI_DEFECT: 'Zari / Weave Defect',
  FABRIC_FEEL: 'Fabric Feel / Texture',
  INCORRECT_ITEM: 'Incorrect Item Delivered',
  SIZE_MISMATCH: 'Dimension Deficient',
  OTHER: 'Other Issue'
};

const POLICY_REASONS = [
  'Item shows evidence of wear, perfume, or laundering.',
  'Security tag / Silk Mark authentic seal removed.',
  'Defect not substantiated by submitted condition photographs.',
  'Return window exceeded policy (7 days from delivery).',
  'Blouse piece has already been cut or altered.'
];

export default function ManageReturns() {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [errorMsg, setErrorMsg] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Modals & Drawer State
  const [inspectClaim, setInspectClaim] = useState(null);
  const [inspectPhotoIndex, setInspectPhotoIndex] = useState(0);
  
  // Courier Modal State
  const [scheduleModalClaim, setScheduleModalClaim] = useState(null);
  const [courierPartner, setCourierPartner] = useState('Blue Dart Reverse Logistics');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [pickupNotes, setPickupNotes] = useState('Please pack in original keepsake box with tags.');

  // Rejection Modal State
  const [rejectModalClaim, setRejectModalClaim] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');

  // Clipboard feedback
  const [copiedAwb, setCopiedAwb] = useState(null);

  const fetchClaims = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res = await returnService.getAllReturns(activeTab);
      if (res?.success && Array.isArray(res.data)) {
        setClaims(res.data);
      } else if (Array.isArray(res)) {
        setClaims(res);
      } else {
        // Resilient fallback
        setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
      }
    } catch (err) {
      console.warn('Backend endpoint unavailable, using offline fallback claims', err);
      setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchClaims();
  }, [fetchClaims]);

  // Copy AWB utility
  const handleCopyAwb = (awb) => {
    navigator.clipboard.writeText(awb);
    setCopiedAwb(awb);
    setTimeout(() => setCopiedAwb(null), 2000);
  };

  // 1-Click Action: Approve Return
  const handleApprove = async (claimId) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'APPROVED',
        adminNotes: 'Condition verified from photographs. Approved for reverse pickup.'
      });
      if (res?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} has been approved.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'APPROVED' }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Schedule Pickup Modal
  const openScheduleModal = (claim) => {
    setScheduleModalClaim(claim);
    setErrorMsg(null);
    setCourierPartner('Blue Dart Reverse Logistics');
    setTrackingNumber(`BDR-RET-${claim.orderId}-${Date.now().toString().slice(-4)}`);
    setPickupNotes('Doorstep reverse pickup scheduled. Pack in original box with tags intact.');
  };

  // Submit Schedule Pickup
  const handleConfirmSchedule = async () => {
    if (!scheduleModalClaim) return;
    if (!trackingNumber.trim()) {
      setErrorMsg('Please provide a valid reverse AWB tracking number.');
      return;
    }
    const claimId = scheduleModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'PICKUP_SCHEDULED',
        reverseCourier: courierPartner,
        reverseTrackingNumber: trackingNumber.trim(),
        adminNotes: pickupNotes.trim()
      });
      if (res?.success) {
        setSuccessMsg(`Reverse pickup scheduled for claim #RET-${claimId}. AWB: ${trackingNumber}`);
        fetchClaims();
        setScheduleModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({
            ...prev,
            status: 'PICKUP_SCHEDULED',
            reverseCourier: courierPartner,
            reverseTrackingNumber: trackingNumber.trim(),
            adminNotes: pickupNotes.trim()
          }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // 1-Click Action: Complete Refund
  const handleCompleteRefund = async (claimId, refundAmount) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'COMPLETED',
        refundAmount: refundAmount || 0,
        adminNotes: 'Item received at central hub, physical inspection passed, refund processed.'
      });
      if (res?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'COMPLETED' }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Rejection Modal
  const openRejectModal = (claim) => {
    setRejectModalClaim(claim);
    setErrorMsg(null);
    setRejectionReason('');
  };

  // Submit Rejection
  const handleConfirmReject = async () => {
    if (!rejectModalClaim) return;
    if (!rejectionReason.trim()) {
      setErrorMsg('Mandatory rejection reason must be provided.');
      return;
    }
    const claimId = rejectModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'REJECTED',
        adminNotes: rejectionReason.trim()
      });
      if (res?.success) {
        setSuccessMsg(`Claim #RET-${claimId} has been rejected.`);
        fetchClaims();
        setRejectModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'REJECTED', adminNotes: rejectionReason.trim() }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Filtered claims based on search input
  const filteredClaims = useMemo(() => {
    if (!searchQuery.trim()) return claims;
    const q = searchQuery.toLowerCase();
    return claims.filter((c) => 
      c.id?.toString().includes(q) ||
      c.orderId?.toString().includes(q) ||
      c.customerName?.toLowerCase().includes(q) ||
      c.customerEmail?.toLowerCase().includes(q) ||
      c.reverseTrackingNumber?.toLowerCase().includes(q) ||
      c.reverseCourier?.toLowerCase().includes(q) ||
      c.reason?.toLowerCase().includes(q)
    );
  }, [claims, searchQuery]);

  // Aggregate KPI Metrics
  const kpiMetrics = useMemo(() => {
    const totalCount = claims.length;
    const totalValue = claims.reduce((acc, c) => acc + (c.refundAmount || c.orderTotalAmount || 0), 0);
    const pendingCount = claims.filter((c) => c.status === 'PENDING').length;
    const scheduledCount = claims.filter((c) => c.status === 'PICKUP_SCHEDULED').length;
    const completedClaims = claims.filter((c) => c.status === 'COMPLETED');
    const completedCount = completedClaims.length;
    const completedValue = completedClaims.reduce((acc, c) => acc + (c.refundAmount || 0), 0);

    return { totalCount, totalValue, pendingCount, scheduledCount, completedCount, completedValue };
  }, [claims]);

  const formatINR = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val || 0);
  };

  const formatDate = (isoStr) => {
    if (!isoStr) return '—';
    const d = new Date(isoStr);
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  return (
    <div className="space-y-6 font-sans text-left text-[#17211F]">
      <SEO
        title="Returns & Reverse Logistics Moderation | SareeKart Admin"
        description="Inspect customer defect photographs, approve return/exchange requests, schedule reverse courier pickups, and authorize refunds."
        noindex={true}
      />

      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-[#17211F] text-[#F3C56A] text-xs font-bold uppercase tracking-wider rounded-full flex items-center gap-1.5">
              <RotateCcw className="w-3.5 h-3.5 text-[#F3C56A]" /> Reverse Logistics
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#E3F0ED] text-[#1E6A62]">
              Staff Console
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-serif font-bold text-[#17211F] mt-2">
            Returns & Exchanges Moderation
          </h1>
          <p className="text-xs text-[#71817A] mt-1">
            Review customer claims, verify defect photographs, assign reverse courier AWBs, and disburse refunds.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchClaims}
            className="p-2.5 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#17211F] rounded-full transition cursor-pointer"
            title="Refresh returns"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Feedback Alerts */}
      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-2xl flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          <button onClick={() => setErrorMsg(null)} className="text-rose-600 hover:text-rose-800 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}
      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-2xl flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-600 hover:text-emerald-800 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 4-Card KPI Metric Summary Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total Claims */}
        <div className="bg-white border border-[#DDD8CF] rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-[#71817A]">Total Claims</p>
            <p className="text-2xl font-serif font-bold text-[#17211F] mt-1">{kpiMetrics.totalCount}</p>
            <p className="text-[11px] text-[#71817A] mt-0.5">Value: {formatINR(kpiMetrics.totalValue)}</p>
          </div>
          <div className="h-12 w-12 rounded-2xl bg-[#F7F4EE] border border-[#DDD8CF] flex items-center justify-center text-[#17211F]">
            <RotateCcw className="w-5 h-5" />
          </div>
        </div>

        {/* Card 2: Pending Review */}
        <div className="bg-amber-50/70 border border-amber-200 rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-amber-800">Pending Review</p>
            <p className="text-2xl font-serif font-bold text-amber-950 mt-1">{kpiMetrics.pendingCount}</p>
            <p className="text-[11px] font-semibold text-amber-700 mt-0.5">Requires photo review</p>
          </div>
          <div className="h-12 w-12 rounded-2xl bg-amber-100 border border-amber-300 flex items-center justify-center text-amber-800">
            <Clock className="w-5 h-5" />
          </div>
        </div>

        {/* Card 3: Pickups Scheduled */}
        <div className="bg-[#E3F0ED]/70 border border-[#B8D8D1] rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-[#1E6A62]">Pickups Scheduled</p>
            <p className="text-2xl font-serif font-bold text-[#17211F] mt-1">{kpiMetrics.scheduledCount}</p>
            <p className="text-[11px] font-semibold text-[#1E6A62] mt-0.5">AWB generated / in-transit</p>
          </div>
          <div className="h-12 w-12 rounded-2xl bg-[#E3F0ED] border border-[#B8D8D1] flex items-center justify-center text-[#1E6A62]">
            <Truck className="w-5 h-5" />
          </div>
        </div>

        {/* Card 4: Completed Refunds */}
        <div className="bg-emerald-50/70 border border-emerald-200 rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-emerald-800">Completed Refunds</p>
            <p className="text-2xl font-serif font-bold text-emerald-950 mt-1">{kpiMetrics.completedCount}</p>
            <p className="text-[11px] font-semibold text-emerald-700 mt-0.5">Disbursed: {formatINR(kpiMetrics.completedValue)}</p>
          </div>
          <div className="h-12 w-12 rounded-2xl bg-emerald-100 border border-emerald-300 flex items-center justify-center text-emerald-800">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Filter Tabs & Search Controls */}
      <div className="bg-white border border-[#DDD8CF] rounded-3xl p-5 shadow-xs space-y-4">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          {/* Status Tabs */}
          <div className="flex flex-wrap gap-2">
            {[
              { id: 'ALL', label: 'All Claims' },
              { id: 'PENDING', label: 'Pending Review' },
              { id: 'APPROVED', label: 'Approved' },
              { id: 'PICKUP_SCHEDULED', label: 'Pickups Scheduled' },
              { id: 'COMPLETED', label: 'Completed' },
              { id: 'REJECTED', label: 'Rejected' },
            ].map((tab) => {
              const count = tab.id === 'ALL' 
                ? claims.length 
                : claims.filter((c) => c.status === tab.id).length;
              const isActive = activeTab === tab.id;

              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer flex items-center gap-1.5 ${
                    isActive
                      ? 'bg-[#17211F] text-[#F3C56A]'
                      : 'bg-[#F7F4EE] text-[#71817A] hover:bg-[#E8E2D9]'
                  }`}
                >
                  <span>{tab.label}</span>
                  <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                    isActive ? 'bg-white/20 text-white' : 'bg-[#DDD8CF] text-[#17211F]'
                  }`}>
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Live Search */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-[#71817A] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search Claim #, Order #, Customer, AWB..."
              className="w-full pl-9 pr-4 py-2 bg-[#F7F4EE] border border-[#DDD8CF] rounded-full text-xs outline-none focus:border-[#1E6A62] text-[#17211F]"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#71817A] hover:text-[#17211F] cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Claims Table */}
        <div className="overflow-x-auto border border-[#DDD8CF] rounded-2xl">
          <table className="w-full text-left text-xs text-[#17211F]">
            <thead className="bg-[#F7F4EE] border-b border-[#DDD8CF] uppercase text-[10px] font-bold tracking-wider text-[#71817A]">
              <tr>
                <th className="p-3.5">Claim ID</th>
                <th className="p-3.5">Customer & Order</th>
                <th className="p-3.5">Reason & Comments</th>
                <th className="p-3.5">Refund / Mode</th>
                <th className="p-3.5 text-center">Defect Photos</th>
                <th className="p-3.5">Reverse Logistics</th>
                <th className="p-3.5">Status</th>
                <th className="p-3.5 text-right">Action Controls</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDD8CF]">
              {filteredClaims.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-[#71817A]">
                    <RotateCcw className="w-8 h-8 text-[#DDD8CF] mx-auto mb-2" />
                    <p className="font-semibold">No return or exchange claims found.</p>
                    <p className="text-[11px] mt-1">Try switching tabs or clearing your search filter.</p>
                  </td>
                </tr>
              ) : (
                filteredClaims.map((claim) => {
                  const isActionLoading = actionLoadingId === claim.id;

                  return (
                    <tr key={claim.id} className="hover:bg-[#F7F4EE]/50 transition">
                      {/* Claim ID & Type */}
                      <td className="p-3.5 align-top">
                        <div className="font-mono font-bold text-[#17211F]">#RET-{claim.id}</div>
                        <span className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          claim.type === 'EXCHANGE' 
                            ? 'bg-purple-100 text-purple-800 border border-purple-200' 
                            : 'bg-indigo-50 text-indigo-800 border border-indigo-200'
                        }`}>
                          {claim.type}
                        </span>
                        <p className="text-[10px] text-[#71817A] mt-1">{formatDate(claim.createdAt)}</p>
                      </td>

                      {/* Customer & Order */}
                      <td className="p-3.5 align-top">
                        <p className="font-bold text-[#17211F]">{claim.customerName}</p>
                        <p className="text-[11px] text-[#71817A]">{claim.customerEmail}</p>
                        <div className="mt-1 flex items-center gap-1.5 text-[11px]">
                          <span className="font-mono font-semibold text-[#1E6A62]">Order #SK-{claim.orderId}</span>
                          <span className="text-[#DDD8CF]">•</span>
                          <span className="text-[#71817A]">{claim.daysSinceDelivery ?? 0}d post-delivery</span>
                        </div>
                      </td>

                      {/* Reason & Comments */}
                      <td className="p-3.5 align-top max-w-xs">
                        <span className="inline-block px-2 py-0.5 rounded-md text-[10px] font-semibold bg-[#F7F4EE] border border-[#DDD8CF] text-[#17211F]">
                          {REASON_LABELS[claim.reason] || claim.reason}
                        </span>
                        <p className="text-[11px] text-[#71817A] mt-1 line-clamp-2" title={claim.comments}>
                          &ldquo;{claim.comments}&rdquo;
                        </p>
                      </td>

                      {/* Refund / Mode */}
                      <td className="p-3.5 align-top">
                        <p className="font-bold text-[#17211F]">{formatINR(claim.refundAmount || claim.orderTotalAmount)}</p>
                        <span className="inline-block mt-1 px-2 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-neutral-100 text-neutral-700">
                          {claim.refundMode?.replace('_', ' ')}
                        </span>
                      </td>

                      {/* Defect Photos Thumbnails */}
                      <td className="p-3.5 align-top text-center">
                        {claim.images && claim.images.length > 0 ? (
                          <div className="flex items-center justify-center gap-1.5">
                            {claim.images.slice(0, 3).map((imgUrl, i) => (
                              <button
                                key={i}
                                type="button"
                                onClick={() => {
                                  setInspectClaim(claim);
                                  setInspectPhotoIndex(i);
                                }}
                                className="group relative h-10 w-10 shrink-0 rounded-lg overflow-hidden border border-[#DDD8CF] hover:border-[#1E6A62] transition cursor-pointer"
                                title="Click to inspect photo in lightbox"
                              >
                                <img
                                  src={imgUrl}
                                  alt={`Defect ${i + 1}`}
                                  className="h-full w-full object-cover group-hover:scale-110 transition duration-200"
                                  onError={(e) => {
                                    e.target.src = 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=150&q=80';
                                  }}
                                />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition text-white">
                                  <Eye className="w-3.5 h-3.5" />
                                </div>
                              </button>
                            ))}
                          </div>
                        ) : (
                          <span className="text-[10px] text-[#71817A] italic">No photos</span>
                        )}
                      </td>

                      {/* Reverse Logistics */}
                      <td className="p-3.5 align-top">
                        {claim.reverseCourier ? (
                          <div>
                            <p className="font-semibold text-[11px] text-[#17211F] flex items-center gap-1">
                              <Truck className="w-3 h-3 text-[#1E6A62]" />
                              {claim.reverseCourier}
                            </p>
                            <div className="mt-1 flex items-center gap-1">
                              <span className="font-mono text-[10px] bg-neutral-100 px-1.5 py-0.5 rounded border border-[#DDD8CF]">
                                {claim.reverseTrackingNumber}
                              </span>
                              <button
                                type="button"
                                onClick={() => handleCopyAwb(claim.reverseTrackingNumber)}
                                className="p-1 text-[#71817A] hover:text-[#1E6A62] transition cursor-pointer"
                                title="Copy AWB code"
                              >
                                {copiedAwb === claim.reverseTrackingNumber ? (
                                  <Check className="w-3 h-3 text-emerald-600" />
                                ) : (
                                  <Copy className="w-3 h-3" />
                                )}
                              </button>
                            </div>
                          </div>
                        ) : (
                          <span className="text-[11px] text-[#71817A]">—</span>
                        )}
                      </td>

                      {/* Status */}
                      <td className="p-3.5 align-top">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold ${
                          claim.status === 'PENDING' ? 'bg-amber-100 text-amber-800 border border-amber-200' :
                          claim.status === 'APPROVED' ? 'bg-sky-100 text-sky-800 border border-sky-200' :
                          claim.status === 'PICKUP_SCHEDULED' ? 'bg-[#E3F0ED] text-[#1E6A62] border border-[#B8D8D1]' :
                          claim.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-200' :
                          'bg-rose-100 text-rose-800 border border-rose-200'
                        }`}>
                          {claim.status === 'PENDING' && <Clock className="w-3 h-3" />}
                          {claim.status === 'APPROVED' && <ShieldCheck className="w-3 h-3" />}
                          {claim.status === 'PICKUP_SCHEDULED' && <Truck className="w-3 h-3" />}
                          {claim.status === 'COMPLETED' && <CheckCircle2 className="w-3 h-3" />}
                          {claim.status === 'REJECTED' && <XCircle className="w-3 h-3" />}
                          <span>{claim.status?.replace('_', ' ')}</span>
                        </span>
                      </td>

                      {/* 1-Click Action Controls */}
                      <td className="p-3.5 align-top text-right">
                        <div className="flex flex-col sm:flex-row items-end sm:items-center justify-end gap-1.5">
                          {/* When PENDING: Approve or Reject */}
                          {claim.status === 'PENDING' && (
                            <>
                              <button
                                type="button"
                                disabled={isActionLoading}
                                onClick={() => handleApprove(claim.id)}
                                className="px-2.5 py-1 bg-[#1E6A62] hover:bg-[#16514B] text-white rounded-lg text-[11px] font-bold transition shadow-2xs cursor-pointer flex items-center gap-1 disabled:opacity-50"
                              >
                                {isActionLoading ? <RefreshCw className="w-3 h-3 animate-spin" /> : <ShieldCheck className="w-3 h-3" />}
                                <span>Approve</span>
                              </button>
                              <button
                                type="button"
                                disabled={isActionLoading}
                                onClick={() => openRejectModal(claim)}
                                className="px-2 py-1 bg-white hover:bg-rose-50 text-rose-700 border border-rose-200 rounded-lg text-[11px] font-bold transition cursor-pointer disabled:opacity-50"
                              >
                                Reject
                              </button>
                            </>
                          )}

                          {/* When APPROVED: Schedule Pickup or Reject */}
                          {claim.status === 'APPROVED' && (
                            <>
                              <button
                                type="button"
                                disabled={isActionLoading}
                                onClick={() => openScheduleModal(claim)}
                                className="px-2.5 py-1 bg-[#17211F] hover:bg-[#1E6A62] text-[#F3C56A] rounded-lg text-[11px] font-bold transition shadow-2xs cursor-pointer flex items-center gap-1 disabled:opacity-50"
                              >
                                <Truck className="w-3 h-3" />
                                <span>Schedule Pickup</span>
                              </button>
                              <button
                                type="button"
                                disabled={isActionLoading}
                                onClick={() => openRejectModal(claim)}
                                className="px-2 py-1 bg-white hover:bg-rose-50 text-rose-700 border border-rose-200 rounded-lg text-[11px] font-bold transition cursor-pointer disabled:opacity-50"
                              >
                                Reject
                              </button>
                            </>
                          )}

                          {/* When PICKUP_SCHEDULED: Complete Refund */}
                          {claim.status === 'PICKUP_SCHEDULED' && (
                            <button
                              type="button"
                              disabled={isActionLoading}
                              onClick={() => handleCompleteRefund(claim.id, claim.refundAmount)}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[11px] font-bold transition shadow-2xs cursor-pointer flex items-center gap-1 disabled:opacity-50"
                            >
                              <CheckCircle2 className="w-3 h-3" />
                              <span>Complete Refund</span>
                            </button>
                          )}

                          {/* When COMPLETED or REJECTED */}
                          {claim.status === 'COMPLETED' && (
                            <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                              Disbursed
                            </span>
                          )}
                          {claim.status === 'REJECTED' && (
                            <span className="text-[11px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded border border-rose-200" title={claim.adminNotes}>
                              Declined
                            </span>
                          )}

                          {/* Inspection trigger */}
                          <button
                            type="button"
                            onClick={() => {
                              setInspectClaim(claim);
                              setInspectPhotoIndex(0);
                            }}
                            className="p-1.5 text-[#71817A] hover:text-[#17211F] rounded-md transition cursor-pointer"
                            title="Inspect claim details & defect photos"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* Side-by-Side Defect Photos Inspection Lightbox Modal                       */}
      {/* ========================================================================= */}
      {inspectClaim && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-labelledby="inspect-dialog-title"
        >
          <div className="bg-white border border-[#DDD8CF] rounded-3xl max-w-5xl w-full max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#DDD8CF] bg-[#F7F4EE]">
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold text-sm text-[#17211F]">
                  Defect Inspection: Claim #RET-{inspectClaim.id}
                </span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                  inspectClaim.status === 'PENDING' ? 'bg-amber-100 text-amber-800' :
                  inspectClaim.status === 'APPROVED' ? 'bg-sky-100 text-sky-800' :
                  inspectClaim.status === 'PICKUP_SCHEDULED' ? 'bg-[#E3F0ED] text-[#1E6A62]' :
                  inspectClaim.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
                  'bg-rose-100 text-rose-800'
                }`}>
                  {inspectClaim.status?.replace('_', ' ')}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setInspectClaim(null)}
                className="p-1.5 text-[#71817A] hover:text-[#17211F] rounded-full hover:bg-white transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Split Content: Left (Photo Lightbox) vs Right (Context & Controls) */}
            <div className="flex-1 overflow-y-auto grid grid-cols-1 lg:grid-cols-12 divide-y lg:divide-y-0 lg:divide-x divide-[#DDD8CF]">
              {/* Left Pane: High-Res Defect Photos (7 cols) */}
              <div className="lg:col-span-7 p-6 flex flex-col bg-neutral-900 text-white justify-between min-h-[360px]">
                {inspectClaim.images && inspectClaim.images.length > 0 ? (
                  <>
                    <div className="relative flex-1 flex items-center justify-center overflow-hidden rounded-2xl bg-black/40">
                      <img
                        src={inspectClaim.images[inspectPhotoIndex] || inspectClaim.images[0]}
                        alt={`Evidence photo ${inspectPhotoIndex + 1}`}
                        className="max-h-[420px] w-full object-contain rounded-xl"
                        onError={(e) => {
                          e.target.src = 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80';
                        }}
                      />
                      {inspectClaim.images.length > 1 && (
                        <>
                          <button
                            type="button"
                            onClick={() => setInspectPhotoIndex((prev) => (prev > 0 ? prev - 1 : inspectClaim.images.length - 1))}
                            className="absolute left-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black text-white cursor-pointer"
                          >
                            <ChevronLeft className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setInspectPhotoIndex((prev) => (prev < inspectClaim.images.length - 1 ? prev + 1 : 0))}
                            className="absolute right-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black text-white cursor-pointer"
                          >
                            <ChevronRight className="w-4 h-4" />
                          </button>
                        </>
                      )}
                    </div>

                    {/* Thumbnail Switcher Bar */}
                    <div className="mt-4 flex items-center justify-between">
                      <span className="text-xs text-white/70">
                        Photo {inspectPhotoIndex + 1} of {inspectClaim.images.length}
                      </span>
                      <div className="flex gap-2">
                        {inspectClaim.images.map((img, idx) => (
                          <button
                            key={idx}
                            type="button"
                            onClick={() => setInspectPhotoIndex(idx)}
                            className={`h-12 w-12 rounded-lg overflow-hidden border-2 transition cursor-pointer ${
                              inspectPhotoIndex === idx ? 'border-[#F3C56A]' : 'border-white/30 opacity-60 hover:opacity-100'
                            }`}
                          >
                            <img src={img} alt="" className="h-full w-full object-cover" />
                          </button>
                        ))}
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="flex-1 flex flex-col items-center justify-center text-white/60">
                    <Eye className="w-10 h-10 mb-2 opacity-40" />
                    <p className="text-xs">No defect photographs uploaded with this claim.</p>
                  </div>
                )}
              </div>

              {/* Right Pane: Claim Context & Direct Action Controls (5 cols) */}
              <div className="lg:col-span-5 p-6 flex flex-col justify-between space-y-4 bg-white">
                <div className="space-y-4">
                  <div>
                    <h2 id="inspect-dialog-title" className="text-base font-serif font-bold text-[#17211F]">
                      Order Reference: #SK-{inspectClaim.orderId}
                    </h2>
                    <p className="text-xs text-[#71817A] mt-0.5">
                      Customer: <strong className="text-[#17211F]">{inspectClaim.customerName}</strong> ({inspectClaim.customerEmail})
                    </p>
                    <p className="text-xs text-[#71817A]">
                      Mobile: {inspectClaim.customerMobile || '—'}
                    </p>
                  </div>

                  <div className="p-3 bg-[#F7F4EE] border border-[#DDD8CF] rounded-2xl space-y-1.5 text-xs">
                    <div className="flex justify-between">
                      <span className="text-[#71817A]">Claim Reason:</span>
                      <span className="font-bold text-[#17211F]">{REASON_LABELS[inspectClaim.reason] || inspectClaim.reason}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#71817A]">Claim Type:</span>
                      <span className="font-bold text-[#17211F]">{inspectClaim.type}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#71817A]">Refund Amount:</span>
                      <span className="font-bold text-[#1E6A62]">{formatINR(inspectClaim.refundAmount || inspectClaim.orderTotalAmount)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#71817A]">Refund Mode:</span>
                      <span className="font-bold text-[#17211F]">{inspectClaim.refundMode?.replace('_', ' ')}</span>
                    </div>
                    {inspectClaim.exchangeSku && (
                      <div className="flex justify-between">
                        <span className="text-[#71817A]">Exchange Saree SKU:</span>
                        <span className="font-mono font-bold text-purple-700">{inspectClaim.exchangeSku}</span>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                      Customer Statement
                    </label>
                    <div className="mt-1 p-3 bg-neutral-50 border border-neutral-200 rounded-xl text-xs text-[#17211F] italic">
                      &ldquo;{inspectClaim.comments}&rdquo;
                    </div>
                  </div>

                  {inspectClaim.adminNotes && (
                    <div>
                      <label className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                        Staff Moderation Notes
                      </label>
                      <div className="mt-1 p-3 bg-amber-50/70 border border-amber-200 rounded-xl text-xs text-amber-900">
                        {inspectClaim.adminNotes}
                      </div>
                    </div>
                  )}

                  {inspectClaim.reverseCourier && (
                    <div className="p-3 bg-[#E3F0ED] border border-[#B8D8D1] rounded-xl text-xs">
                      <p className="font-bold text-[#1E6A62] flex items-center gap-1.5">
                        <Truck className="w-3.5 h-3.5" />
                        {inspectClaim.reverseCourier}
                      </p>
                      <p className="font-mono text-[11px] text-[#17211F] mt-1">
                        AWB: {inspectClaim.reverseTrackingNumber}
                      </p>
                    </div>
                  )}
                </div>

                {/* Inline Action Controls Inside Lightbox */}
                <div className="pt-4 border-t border-[#DDD8CF] space-y-2">
                  {errorMsg && (
                    <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                      <span>{errorMsg}</span>
                    </div>
                  )}
                  <p className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                    Moderation Actions
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {inspectClaim.status === 'PENDING' && (
                      <>
                        <button
                          type="button"
                          onClick={() => handleApprove(inspectClaim.id)}
                          className="flex-1 py-2 bg-[#1E6A62] hover:bg-[#16514B] text-white rounded-xl text-xs font-bold transition shadow-xs cursor-pointer flex items-center justify-center gap-1.5"
                        >
                          <ShieldCheck className="w-4 h-4" />
                          <span>Approve Claim</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => openRejectModal(inspectClaim)}
                          className="px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-bold transition cursor-pointer"
                        >
                          Reject
                        </button>
                      </>
                    )}

                    {inspectClaim.status === 'APPROVED' && (
                      <>
                        <button
                          type="button"
                          onClick={() => openScheduleModal(inspectClaim)}
                          className="flex-1 py-2 bg-[#17211F] hover:bg-[#1E6A62] text-[#F3C56A] rounded-xl text-xs font-bold transition shadow-xs cursor-pointer flex items-center justify-center gap-1.5"
                        >
                          <Truck className="w-4 h-4" />
                          <span>Schedule Reverse Pickup</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => openRejectModal(inspectClaim)}
                          className="px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-bold transition cursor-pointer"
                        >
                          Reject
                        </button>
                      </>
                    )}

                    {inspectClaim.status === 'PICKUP_SCHEDULED' && (
                      <button
                        type="button"
                        onClick={() => handleCompleteRefund(inspectClaim.id, inspectClaim.refundAmount)}
                        className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition shadow-xs cursor-pointer flex items-center justify-center gap-1.5"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Confirm Receipt & Complete Refund</span>
                      </button>
                    )}

                    {(inspectClaim.status === 'COMPLETED' || inspectClaim.status === 'REJECTED') && (
                      <div className="w-full text-center py-2 text-xs font-semibold text-[#71817A] bg-[#F7F4EE] rounded-xl">
                        Claim has reached terminal state ({inspectClaim.status}).
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* Assign Courier & Schedule Pickup Modal                                    */}
      {/* ========================================================================= */}
      {scheduleModalClaim && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-3">
              <div className="flex items-center gap-2">
                <Truck className="w-5 h-5 text-[#1E6A62]" />
                <h3 className="text-base font-serif font-bold text-[#17211F]">
                  Schedule Reverse Pickup
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setScheduleModalClaim(null)}
                className="text-[#71817A] hover:text-[#17211F] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-[#71817A]">
              Assign reverse logistics partner and AWB tracking for claim <strong className="text-[#17211F]">#RET-{scheduleModalClaim.id}</strong> (Order #SK-{scheduleModalClaim.orderId}).
            </p>

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <div className="space-y-3 text-xs">
              {/* Courier Partner Selection */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#71817A] mb-1">
                  Courier Logistics Partner
                </label>
                <select
                  value={courierPartner}
                  onChange={(e) => setCourierPartner(e.target.value)}
                  className="w-full p-2.5 bg-[#F7F4EE] border border-[#DDD8CF] rounded-xl text-xs outline-none focus:border-[#1E6A62] text-[#17211F]"
                >
                  <option value="Blue Dart Reverse Logistics">Blue Dart Reverse Logistics</option>
                  <option value="Delhivery Reverse">Delhivery Reverse</option>
                  <option value="DTDC Express">DTDC Express</option>
                  <option value="India Post Speed Post">India Post Speed Post</option>
                </select>
              </div>

              {/* Reverse AWB Tracking Number */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-bold uppercase tracking-wider text-[#71817A]">
                    Reverse AWB Tracking Number *
                  </label>
                  <button
                    type="button"
                    onClick={() => setTrackingNumber(`BDR-RET-${scheduleModalClaim.orderId}-${Date.now().toString().slice(-4)}`)}
                    className="text-[10px] text-[#1E6A62] hover:underline cursor-pointer"
                  >
                    Regenerate
                  </button>
                </div>
                <input
                  type="text"
                  value={trackingNumber}
                  onChange={(e) => setTrackingNumber(e.target.value)}
                  placeholder="e.g. BDR-RET-37-9021"
                  className="w-full p-2.5 bg-[#F7F4EE] border border-[#DDD8CF] rounded-xl text-xs font-mono outline-none focus:border-[#1E6A62] text-[#17211F]"
                />
              </div>

              {/* Warehouse Intake / Customer Handover Notes */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#71817A] mb-1">
                  Handover & Intake Instructions
                </label>
                <textarea
                  value={pickupNotes}
                  onChange={(e) => setPickupNotes(e.target.value)}
                  rows={2}
                  className="w-full p-2.5 bg-[#F7F4EE] border border-[#DDD8CF] rounded-xl text-xs outline-none focus:border-[#1E6A62] text-[#17211F]"
                />
              </div>
            </div>

            <div className="flex gap-2 justify-end pt-2">
              <button
                type="button"
                onClick={() => setScheduleModalClaim(null)}
                className="px-4 py-2 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#71817A] rounded-full text-xs font-bold cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmSchedule}
                className="px-5 py-2 bg-[#1E6A62] hover:bg-[#16514B] text-white rounded-full text-xs font-bold transition shadow-xs cursor-pointer flex items-center gap-1.5"
              >
                <Truck className="w-3.5 h-3.5" />
                <span>Confirm Pickup</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* Reject Return Modal with Mandatory Explanation                           */}
      {/* ========================================================================= */}
      {rejectModalClaim && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
        >
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-3">
              <div className="flex items-center gap-2">
                <XCircle className="w-5 h-5 text-rose-600" />
                <h3 className="text-base font-serif font-bold text-[#17211F]">
                  Reject Return Claim #RET-{rejectModalClaim.id}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setRejectModalClaim(null)}
                className="text-[#71817A] hover:text-[#17211F] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-rose-800 bg-rose-50 border border-rose-200 p-2.5 rounded-xl font-medium">
              Mandatory: A clear justification reason must be specified. This explanation will be permanently recorded and displayed directly on the customer&apos;s order card.
            </p>

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Quick Policy Chips */}
            <div>
              <label className="block text-[10px] font-bold uppercase tracking-wider text-[#71817A] mb-1.5">
                Standard Policy Clauses (Click to insert):
              </label>
              <div className="flex flex-wrap gap-1.5">
                {POLICY_REASONS.map((chip, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setRejectionReason(chip)}
                    className="px-2 py-1 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#17211F] text-[10px] font-semibold rounded-md border border-[#DDD8CF] transition text-left cursor-pointer"
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>

            {/* Custom Notes */}
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-[#71817A] mb-1">
                Rejection Explanation *
              </label>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Detail specifically why this return claim cannot be honored..."
                rows={3}
                className="w-full p-3 bg-[#F7F4EE] border border-[#DDD8CF] rounded-xl text-xs outline-none focus:border-rose-500 text-[#17211F]"
              />
            </div>

            <div className="flex gap-2 justify-end pt-2">
              <button
                type="button"
                onClick={() => setRejectModalClaim(null)}
                className="px-4 py-2 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#71817A] rounded-full text-xs font-bold cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!rejectionReason.trim()}
                onClick={handleConfirmReject}
                className="px-5 py-2 bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white rounded-full text-xs font-bold transition shadow-xs cursor-pointer"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
