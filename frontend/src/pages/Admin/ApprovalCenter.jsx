import React, { useState, useEffect } from 'react';
import { ShieldCheck, CheckCircle2, XCircle, Clock, AlertTriangle, User, FileText, ArrowRight, RefreshCw, MessageSquare, Truck } from 'lucide-react';
import api from '../../api/axiosConfig';
import { useSelector } from 'react-redux';
import SEO from '../../components/common/SEO';
import { motion } from 'framer-motion';

export default function ApprovalCenter() {
  const { user } = useSelector((state) => state.auth);
  const isOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';

  const [activeTab, setActiveTab] = useState('PENDING');
  const [pendingRequests, setPendingRequests] = useState([]);
  const [pendingTransfers, setPendingTransfers] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [rejectModalId, setRejectModalId] = useState(null);
  const [rejectType, setRejectType] = useState('APPROVAL'); // APPROVAL or TRANSFER
  const [rejectReason, setRejectReason] = useState('');
  const [errorMsg, setErrorMsg] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const [pendingRes, histRes, transRes] = await Promise.allSettled([
        api.get('/approvals/pending'),
        api.get('/approvals/history'),
        api.get('/admin/inventory/transfers?status=PENDING_APPROVAL')
      ]);
      if (pendingRes.status === 'fulfilled' && pendingRes.value.data?.success) {
        setPendingRequests(pendingRes.value.data.data || []);
      }
      if (histRes.status === 'fulfilled' && histRes.value.data?.success) {
        setHistory(histRes.value.data.data || []);
      }
      if (transRes.status === 'fulfilled' && transRes.value.data?.success) {
        setPendingTransfers(transRes.value.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load approval records', err);
      setErrorMsg(err.response?.data?.message || 'Failed to load approval records');
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleApprove = async (id) => {
    if (!isOwner) {
      setErrorMsg('Not authorised to perform this action');
      return;
    }
    setActionLoadingId(id);
    setErrorMsg(null);
    try {
      const res = await api.post(`/approvals/${id}/approve`, { note: 'Approved via Owner Console' });
      if (res.data?.success) {
        setSuccessMsg(`Request #${id} successfully approved and applied.`);
        fetchData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to approve request');
    }
    setActionLoadingId(null);
  };

  const handleApproveTransfer = async (id) => {
    if (!isOwner) {
      setErrorMsg('Not authorised to perform this action');
      return;
    }
    setActionLoadingId(id);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/inventory/transfer/${id}/approve`, { note: 'Approved via Owner Console' });
      if (res.data?.success) {
        setSuccessMsg(`Stock Transfer #${id} approved and balance updated.`);
        fetchData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to approve transfer');
    }
    setActionLoadingId(null);
  };

  const handleRejectConfirm = async () => {
    if (!isOwner) {
      setErrorMsg('Not authorised to perform this action');
      return;
    }
    if (!rejectReason.trim()) {
      setErrorMsg('Please specify a rejection reason.');
      return;
    }
    const id = rejectModalId;
    setActionLoadingId(id);
    setRejectModalId(null);
    try {
      let res;
      if (rejectType === 'TRANSFER') {
        res = await api.put(`/admin/inventory/transfer/${id}/reject`, { note: rejectReason });
      } else {
        res = await api.post(`/approvals/${id}/reject`, { note: rejectReason });
      }
      if (res.data?.success) {
        setSuccessMsg(`Request #${id} rejected.`);
        setRejectReason('');
        fetchData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to reject request');
    }
    setActionLoadingId(null);
  };

  return (
    <div className="space-y-6 font-sans text-left text-[#17211F]">
      <SEO
        title="Approval Center & Maker-Checker Console | SareeKart Admin"
        description="Owner-controlled approval center for manual price edits, inventory adjustments, and Excel batch transactions."
      />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-[#17211F] text-[#F3C56A] text-xs font-bold uppercase tracking-wider rounded-full">
              Maker-Checker Protocol
            </span>
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${isOwner ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
              Role: {user?.role || 'MANAGER'}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-serif font-bold text-[#17211F] mt-2">Owner Approval Center</h1>
          <p className="text-xs text-[#71817A] mt-1">
            Operational changes and warehouse transfers submitted by managers require owner verification before commitment.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchData}
            className="p-2.5 bg-[#F7F4EE] hover:bg-[#E8E2D9] text-[#17211F] rounded-full transition cursor-pointer"
            title="Refresh requests"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Feedback Alerts */}
      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" /> {errorMsg}
        </div>
      )}
      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-2xl flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" /> {successMsg}
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex flex-wrap gap-3 border-b border-[#DDD8CF] pb-2">
        <button
          onClick={() => setActiveTab('PENDING')}
          className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer flex items-center gap-2 ${
            activeTab === 'PENDING'
              ? 'bg-[#17211F] text-[#F3C56A]'
              : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
          }`}
        >
          <Clock className="w-3.5 h-3.5" /> Pending Approvals ({pendingRequests.length})
        </button>
        <button
          onClick={() => setActiveTab('TRANSFERS')}
          className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer flex items-center gap-2 ${
            activeTab === 'TRANSFERS'
              ? 'bg-[#17211F] text-[#F3C56A]'
              : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
          }`}
        >
          <Truck className="w-3.5 h-3.5" /> Warehouse Transfers ({pendingTransfers.length})
        </button>
        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer flex items-center gap-2 ${
            activeTab === 'HISTORY'
              ? 'bg-[#17211F] text-[#F3C56A]'
              : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
          }`}
        >
          <FileText className="w-3.5 h-3.5" /> Audit History ({history.length})
        </button>
      </div>

      {/* Tab 1: Pending Requests */}
      {activeTab === 'PENDING' && (
        <div className="space-y-4">
          {loading ? (
            <div className="py-12 text-center text-xs font-bold text-[#71817A]">
              Loading pending verification queue...
            </div>
          ) : pendingRequests.length === 0 ? (
            <div className="bg-white border border-[#DDD8CF] rounded-3xl p-12 text-center space-y-3">
              <CheckCircle2 className="w-12 h-12 text-[#1E6A62] mx-auto" />
              <h3 className="text-lg font-serif font-bold text-[#17211F]">Approval Queue Clear</h3>
              <p className="text-xs text-[#71817A]">No operations are currently waiting for owner sign-off.</p>
            </div>
          ) : (
            pendingRequests.map((req) => (
              <motion.div
                key={req.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs hover:shadow-md transition space-y-4"
              >
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 border-b border-[#F7F4EE] pb-3">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold px-2.5 py-0.5 bg-[#F7F4EE] rounded-full text-[#17211F]">
                      REQ-{req.id}
                    </span>
                    <span className="text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-100 text-amber-900 border border-amber-200">
                      Pending owner approval
                    </span>
                    <span className="text-xs font-bold text-[#1E6A62]">{req.action}</span>
                  </div>
                  <span className="text-[11px] text-[#71817A]">Target: {req.targetEntityId}</span>
                </div>

                {/* Diff Comparison */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                  <div className="p-3 bg-[#F7F4EE] rounded-2xl border border-[#E8E2D9]">
                    <span className="text-[10px] uppercase font-bold text-[#71817A] block mb-1">Current / Previous Value</span>
                    <span className="font-mono text-xs text-[#17211F] font-bold">{req.previousValue || 'None'}</span>
                  </div>
                  <div className="p-3 bg-emerald-50/60 rounded-2xl border border-emerald-200">
                    <span className="text-[10px] uppercase font-bold text-emerald-800 block mb-1">Requested Change</span>
                    <span className="font-mono text-xs text-emerald-900 font-bold">{req.requestedValue}</span>
                  </div>
                </div>

                {/* Requester Details */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 pt-2 text-xs text-[#71817A]">
                  <div className="flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-[#1E6A62]" />
                    <span>Requested by: <strong className="text-[#17211F]">{req.requestedByEmail}</strong></span>
                    {req.reason && <span className="italic pl-2">("{req.reason}")</span>}
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2 w-full sm:w-auto">
                    <button
                      onClick={() => {
                        setRejectType('APPROVAL');
                        setRejectModalId(req.id);
                      }}
                      disabled={actionLoadingId === req.id}
                      className="flex-1 sm:flex-none px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-800 font-bold text-xs rounded-full border border-rose-200 transition cursor-pointer"
                    >
                      Reject
                    </button>
                    <button
                      onClick={() => handleApprove(req.id)}
                      disabled={actionLoadingId === req.id}
                      className="flex-1 sm:flex-none px-5 py-2 bg-[#17211F] hover:bg-[#1E6A62] text-white font-bold text-xs uppercase tracking-wider rounded-full transition cursor-pointer shadow-xs"
                    >
                      {actionLoadingId === req.id ? 'Committing...' : 'Approve & Commit'}
                    </button>
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </div>
      )}

      {/* Tab 2: Stock Transfers */}
      {activeTab === 'TRANSFERS' && (
        <div className="space-y-4">
          {loading ? (
            <div className="py-12 text-center text-xs font-bold text-[#71817A]">
              Loading pending stock transfers...
            </div>
          ) : pendingTransfers.length === 0 ? (
            <div className="bg-white border border-[#DDD8CF] rounded-3xl p-12 text-center space-y-3">
              <CheckCircle2 className="w-12 h-12 text-[#1E6A62] mx-auto" />
              <h3 className="text-lg font-serif font-bold text-[#17211F]">Transfer Queue Clear</h3>
              <p className="text-xs text-[#71817A]">No inter-warehouse stock transfers are awaiting owner authorization.</p>
            </div>
          ) : (
            pendingTransfers.map((t) => (
              <motion.div
                key={t.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-white border border-[#DDD8CF] rounded-3xl p-6 shadow-xs hover:shadow-md transition space-y-4"
              >
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 border-b border-[#F7F4EE] pb-3">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold px-2.5 py-0.5 bg-[#F7F4EE] rounded-full text-[#17211F]">
                      TRF-{t.id}
                    </span>
                    <span className="text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-100 text-amber-900 border border-amber-200">
                      Pending Owner Approval
                    </span>
                    <span className="text-xs font-bold text-[#1E6A62]">{t.sku}</span>
                  </div>
                  <span className="text-[11px] text-[#71817A]">SKU: {t.sku}</span>
                </div>

                {/* Hub Rebalance Route */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                  <div className="p-3 bg-[#F7F4EE] rounded-2xl border border-[#E8E2D9]">
                    <span className="text-[10px] uppercase font-bold text-[#71817A] block mb-1">Source Warehouse</span>
                    <span className="font-mono text-xs text-[#17211F] font-bold">{t.sourceWarehouse}</span>
                  </div>
                  <div className="p-3 bg-emerald-50/60 rounded-2xl border border-emerald-200">
                    <span className="text-[10px] uppercase font-bold text-emerald-800 block mb-1">Target Warehouse Hub</span>
                    <span className="font-mono text-xs text-emerald-900 font-bold">{t.targetWarehouse}</span>
                  </div>
                  <div className="p-3 bg-[#FAF8F5] rounded-2xl border border-[#DDD8CF]">
                    <span className="text-[10px] uppercase font-bold text-[#71817A] block mb-1">Transfer Units</span>
                    <span className="font-mono text-xs text-[#17211F] font-bold">{t.quantity} Units</span>
                  </div>
                </div>

                {/* Requester and Actions */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 pt-2 text-xs text-[#71817A]">
                  <div className="flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-[#1E6A62]" />
                    <span>Requested by: <strong className="text-[#17211F]">{t.requestedByEmail}</strong></span>
                    {t.reason && <span className="italic pl-2">("{t.reason}")</span>}
                  </div>

                  <div className="flex gap-2 w-full sm:w-auto">
                    <button
                      onClick={() => {
                        setRejectType('TRANSFER');
                        setRejectModalId(t.id);
                      }}
                      disabled={actionLoadingId === t.id}
                      className="flex-1 sm:flex-none px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-800 font-bold text-xs rounded-full border border-rose-200 transition cursor-pointer"
                    >
                      Reject
                    </button>
                    <button
                      onClick={() => handleApproveTransfer(t.id)}
                      disabled={actionLoadingId === t.id}
                      className="flex-1 sm:flex-none px-5 py-2 bg-[#17211F] hover:bg-[#1E6A62] text-white font-bold text-xs uppercase tracking-wider rounded-full transition cursor-pointer shadow-xs"
                    >
                      {actionLoadingId === t.id ? 'Committing...' : 'Approve & Rebalance'}
                    </button>
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </div>
      )}

      {/* Tab 3: Audit History */}
      {activeTab === 'HISTORY' && (
        <div className="bg-white border border-[#DDD8CF] rounded-3xl overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F7F4EE] border-b border-[#DDD8CF] text-[10px] uppercase font-bold text-[#71817A] tracking-wider">
                <tr>
                  <th className="p-4">Req ID</th>
                  <th className="p-4">Action</th>
                  <th className="p-4">Target</th>
                  <th className="p-4">Requested By</th>
                  <th className="p-4">Status</th>
                  <th className="p-4">Reviewed By</th>
                  <th className="p-4">Review Note</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F7F4EE]">
                {history.map((h) => (
                  <tr key={h.id} className="hover:bg-[#F7F4EE]/50 transition">
                    <td className="p-4 font-mono font-bold">REQ-{h.id}</td>
                    <td className="p-4 font-bold text-[#17211F]">{h.action}</td>
                    <td className="p-4 text-[#71817A]">{h.targetEntityId}</td>
                    <td className="p-4 text-[#71817A]">{h.requestedByEmail}</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        h.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' :
                        h.status === 'REJECTED' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {h.status}
                      </span>
                    </td>
                    <td className="p-4 text-[#71817A]">{h.reviewedByEmail || '—'}</td>
                    <td className="p-4 text-[#71817A] italic">{h.reviewNote || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Rejection Modal */}
      {rejectModalId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <h3 className="text-base font-bold font-serif text-[#17211F]">Reject Request #{rejectModalId}</h3>
            <p className="text-xs text-[#71817A]">
              Please provide a clear justification reason. This will be permanently recorded in the audit logs.
            </p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="e.g. Disapproved due to margin threshold..."
              rows={3}
              className="w-full p-3 bg-[#F7F4EE] border border-[#DDD8CF] rounded-2xl text-xs outline-none focus:border-[#1E6A62]"
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setRejectModalId(null)}
                className="px-4 py-2 bg-[#F7F4EE] text-[#71817A] rounded-full text-xs font-bold cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={handleRejectConfirm}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-full text-xs font-bold cursor-pointer"
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
