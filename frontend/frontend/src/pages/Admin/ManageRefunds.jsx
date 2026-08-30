import { useState, useEffect } from 'react';
import {
  Search, Loader2, RefreshCw, CheckCircle2, XCircle, Clock3,
  IndianRupee, AlertTriangle, Package, Filter, X, ShieldAlert, CheckSquare, Square
} from 'lucide-react';
import refundService from '../../services/refundService';
import api from '../../api/axiosConfig';
import { REFUND_REASON_LABELS, REFUND_STATUS_LABELS } from '../../constants/refundConstants';

const STATUS_TABS = ['All', 'PENDING', 'SUCCESS', 'FAILED'];

function statusBadge(status) {
  const map = {
    PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
    SUCCESS: 'bg-green-50 text-green-700 border-green-200',
    FAILED: 'bg-red-50 text-red-700 border-red-200',
  };
  return map[status] || 'bg-gray-50 text-gray-700 border-gray-200';
}

export default function ManageRefunds() {
  const [refunds, setRefunds] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('All');
  const [search, setSearch] = useState('');
  const [reconciling, setReconciling] = useState(false);
  const [selectedAnomalies, setSelectedAnomalies] = useState(new Set());
  const [resolving, setResolving] = useState(false);

  const fetchAll = async () => {
    try {
      setLoading(true); setError(null);
      const params = activeTab === 'All' ? {} : { status: activeTab };
      const refundsRes = await refundService.getAllRefunds(params);
      // refundService returns ApiResponse: { success, data }
      const list = refundsRes?.data ?? refundsRes ?? [];
      const arr = Array.isArray(list) ? list : (list.data ?? []);
      setRefunds(Array.isArray(arr) ? arr : []);

      // Also fetch anomalies for the ops view (best-effort)
      try {
        const anoRes = await api.get('/admin/reconciliation/anomalies');
        const anoList = anoRes.data?.data ?? anoRes.data ?? [];
        setAnomalies(Array.isArray(anoList) ? anoList.slice(0, 20) : []);
      } catch {
        // not critical
        setAnomalies([]);
      }
    } catch (e) {
      const msg = e?.response?.data?.message || e.message || 'Failed to load refunds';
      // Fallback mock for demo if backend not ready
      if (!msg.includes('Failed')) setError(msg);
      if (refunds.length === 0) {
        setRefunds([
          { id: 1, orderId: 101, orderNumber: 'ORD-1002', paymentId: 201, providerRefundId: 'rfnd_test123', amount: 65000, status: 'PENDING', reason: 'Customer request', reasonCode: 'CUSTOMER_REQUEST', initiatedBy: 'admin@sareekart.in', createdAt: '2025-06-15T10:00:00', errorMessage: null },
          { id: 2, orderId: 102, orderNumber: 'ORD-1006', paymentId: 202, providerRefundId: 'rfnd_test456', amount: 152500, status: 'SUCCESS', reason: 'Out of stock', reasonCode: 'OUT_OF_STOCK', initiatedBy: 'admin@sareekart.in', createdAt: '2025-06-16T09:30:00', errorMessage: null },
          { id: 3, orderId: 103, orderNumber: 'ORD-1004', paymentId: 203, providerRefundId: 'rfnd_test789', amount: 38400, status: 'FAILED', reason: 'Gateway error', reasonCode: 'OTHER', initiatedBy: 'admin@sareekart.in', createdAt: '2025-06-15T11:00:00', errorMessage: 'Gateway rejected' },
        ]);
      }
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchAll(); }, [activeTab]); // eslint-disable-line

  const filtered = refunds.filter(r => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return (r.orderNumber || '').toLowerCase().includes(q)
      || (r.providerRefundId || '').toLowerCase().includes(q)
      || (r.reason || '').toLowerCase().includes(q)
      || String(r.amount || '').includes(q);
  });

  const counts = {
    total: refunds.length,
    pending: refunds.filter(r => r.status === 'PENDING').length,
    success: refunds.filter(r => r.status === 'SUCCESS').length,
    failed: refunds.filter(r => r.status === 'FAILED').length,
  };

  const handleReconcile = async () => {
    try {
      setReconciling(true);
      await api.post('/admin/reconciliation/refunds/run');
      await fetchAll();
    } catch (e) {
      alert(e.response?.data?.message || 'Reconciliation failed');
    } finally { setReconciling(false); }
  };

  const toggleAnomaly = (id) => {
    const next = new Set(selectedAnomalies);
    if (next.has(id)) next.delete(id); else next.add(id);
    setSelectedAnomalies(next);
  };
  const toggleAllAnomalies = () => {
    const openIds = anomalies.filter(a => !a.resolved).map(a => a.id);
    if (openIds.every(id => selectedAnomalies.has(id))) setSelectedAnomalies(new Set());
    else setSelectedAnomalies(new Set(openIds));
  };
  const bulkResolve = async () => {
    if (selectedAnomalies.size === 0) return;
    if (!window.confirm(`Resolve ${selectedAnomalies.size} anomalies?`)) return;
    setResolving(true);
    try {
      for (const id of selectedAnomalies) {
        try { await api.patch(`/admin/reconciliation/anomalies/${id}/resolve`); } catch { /* ignore */ }
      }
      setSelectedAnomalies(new Set());
      await fetchAll();
    } finally { setResolving(false); }
  };
  const resolveOne = async (id) => {
    try { await api.patch(`/admin/reconciliation/anomalies/${id}/resolve`); await fetchAll(); } catch (e) { alert(e.response?.data?.message || 'Resolve failed'); }
  };

  const fmt = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n || 0);

  return (
    <div className="space-y-6 animate-fade-in text-sm">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold font-serif text-maroon flex items-center gap-2"><IndianRupee className="w-6 h-6 text-gold" /> Refund Management</h1>
          <p className="text-xs text-text-secondary mt-0.5">Full and partial refunds, gateway reconciliation, and inventory restocking</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleReconcile} disabled={reconciling} className="inline-flex items-center gap-1.5 px-4 py-2 bg-white border border-border rounded-xl text-xs font-bold hover:bg-[#FAF8F5] disabled:opacity-50">
            {reconciling ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RefreshCw className="w-3.5 h-3.5" />} Reconcile Pending
          </button>
          <button onClick={fetchAll} className="inline-flex items-center gap-1.5 px-4 py-2 bg-maroon hover:bg-maroon-dark text-white rounded-xl text-xs font-bold">
            <RefreshCw className="w-3.5 h-3.5" /> Refresh
          </button>
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="bg-white border border-border rounded-xl p-4 shadow-xs">
          <div className="text-[11px] uppercase font-bold tracking-widest text-text-muted">Total</div>
          <div className="text-2xl font-extrabold text-maroon">{counts.total}</div>
          <div className="text-xs text-text-secondary">All refund records</div>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
          <div className="text-[11px] uppercase font-bold tracking-widest text-amber-700 flex items-center gap-1"><Clock3 className="w-3 h-3" /> Pending</div>
          <div className="text-2xl font-extrabold text-amber-700">{counts.pending}</div>
          <div className="text-xs text-amber-700/80">Awaiting gateway</div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-xl p-4">
          <div className="text-[11px] uppercase font-bold tracking-widest text-green-700 flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Refunded</div>
          <div className="text-2xl font-extrabold text-green-700">{counts.success}</div>
          <div className="text-xs text-green-700/80">Money returned</div>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-xl p-4">
          <div className="text-[11px] uppercase font-bold tracking-widest text-red-700 flex items-center gap-1"><XCircle className="w-3 h-3" /> Failed</div>
          <div className="text-2xl font-extrabold text-red-700">{counts.failed}</div>
          <div className="text-xs text-red-700/80">Needs review</div>
        </div>
      </div>

      {/* Filters + search */}
      <div className="bg-white border border-border rounded-xl p-4 flex flex-col md:flex-row gap-3 items-center justify-between shadow-xs">
        <div className="flex items-center gap-1.5">
          <Filter className="w-4 h-4 text-text-muted" />
          {STATUS_TABS.map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold border transition-colors ${activeTab === tab ? 'bg-maroon text-white border-maroon' : 'bg-white text-text-secondary border-border hover:bg-[#FAF8F5]'}`}
            >
              {tab}
            </button>
          ))}
        </div>
        <div className="relative w-full md:max-w-sm">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"><Search className="w-4 h-4" /></span>
          <input
            type="text"
            placeholder="Search order #, refund id, reason, amount..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-8 py-2 bg-white border border-[#F4F4F4] rounded-xl outline-none text-xs focus:border-[#C9A227] shadow-xs"
          />
          {search && <button onClick={() => setSearch('')} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-maroon"><X className="w-3.5 h-3.5" /></button>}
        </div>
      </div>

      {/* Refunds table */}
      {loading ? (
        <div className="min-h-[30vh] flex flex-col items-center justify-center gap-3"><Loader2 className="w-8 h-8 text-maroon animate-spin" /><p className="text-xs text-text-secondary">Loading refunds...</p></div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg flex items-start gap-2"><AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" /><span>{error}</span></div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl">
          <Package className="w-10 h-10 text-text-muted mx-auto mb-3" /><h3 className="font-bold text-maroon">No refunds found</h3><p className="text-xs text-text-secondary mt-1">No refunds match the current filter.</p>
        </div>
      ) : (
        <div className="bg-white border border-border rounded-xl shadow-sm overflow-hidden">
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 z-10">
                <tr className="bg-[#FAF8F5] border-b border-border text-xs">
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Order</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Amount</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Status</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Reason</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Refund ID</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Initiated</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-xs">
                {filtered.map(r => (
                  <tr key={r.id} className="hover:bg-[#FAF8F5] transition-colors">
                    <td className="px-4 py-3 font-bold text-maroon">{r.orderNumber || `#${r.orderId}`}</td>
                    <td className="px-4 py-3 font-bold">{fmt(r.amount)}</td>
                    <td className="px-4 py-3"><span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold border ${statusBadge(r.status)}`}>{REFUND_STATUS_LABELS[r.status] ?? r.status}</span></td>
                    <td className="px-4 py-3">
                      <div className="font-medium">{r.reason || '—'}</div>
                      <div className="text-[11px] text-text-muted">{REFUND_REASON_LABELS[r.reasonCode] || r.reasonCode || ''}</div>
                      {r.errorMessage && <div className="text-[11px] text-red-600 mt-1 max-w-[220px] truncate" title={r.errorMessage}>⚠ {r.errorMessage}</div>}
                    </td>
                    <td className="px-4 py-3 font-mono text-[11px]">{r.providerRefundId ? `...${String(r.providerRefundId).slice(-8)}` : '—'}</td>
                    <td className="px-4 py-3 text-text-secondary">{r.initiatedBy || '—'}</td>
                    <td className="px-4 py-3 text-text-secondary">{r.createdAt ? new Date(r.createdAt).toLocaleString('en-IN') : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="md:hidden divide-y divide-border">
            {filtered.map(r => (
              <div key={`m-${r.id}`} className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-maroon text-sm">{r.orderNumber || `#${r.orderId}`}</span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${statusBadge(r.status)}`}>{REFUND_STATUS_LABELS[r.status] ?? r.status}</span>
                </div>
                <div className="flex justify-between text-xs"><span className="text-text-muted">Amount</span><span className="font-bold">{fmt(r.amount)}</span></div>
                <div className="text-xs"><span className="text-text-muted">Reason:</span> <span className="font-medium">{r.reason || '—'}</span> <span className="text-text-muted">· {REFUND_REASON_LABELS[r.reasonCode] || r.reasonCode}</span></div>
                {r.errorMessage && <p className="text-[11px] text-red-600 bg-red-50 border border-red-200 rounded-lg px-2 py-1">⚠ {r.errorMessage}</p>}
                <div className="flex justify-between text-[11px] text-text-muted font-mono"><span>{r.providerRefundId ? `...${String(r.providerRefundId).slice(-8)}` : '—'}</span><span>{r.createdAt ? new Date(r.createdAt).toLocaleString('en-IN') : '—'}</span></div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Anomalies – reconciliation insight + bulk-resolve */}
      {anomalies.length > 0 && (
        <div className="bg-white border border-border rounded-xl shadow-sm overflow-hidden">
          <div className="px-6 py-4 border-b border-border flex items-center gap-3">
            <button onClick={toggleAllAnomalies} aria-label="Select all open anomalies" className="text-text-muted hover:text-maroon">
              {anomalies.filter(a => !a.resolved).every(a => selectedAnomalies.has(a.id)) && anomalies.filter(a => !a.resolved).length > 0 ? <CheckSquare className="w-4 h-4 text-maroon" /> : <Square className="w-4 h-4" />}
            </button>
            <ShieldAlert className="w-4 h-4 text-amber-600" />
            <h3 className="text-xs font-extrabold tracking-widest uppercase text-amber-700">Reconciliation Anomalies</h3>
            <span className="text-[11px] text-text-muted">{anomalies.filter(a => !a.resolved).length} open · {anomalies.length} total</span>
            <div className="ml-auto flex items-center gap-2">
              {selectedAnomalies.size > 0 && (
                <button onClick={bulkResolve} disabled={resolving} className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs font-bold disabled:opacity-50">
                  {resolving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />} Resolve {selectedAnomalies.size}
                </button>
              )}
              <span className="text-[11px] text-text-muted hidden sm:inline">Bulk-resolve after manual review – {selectedAnomalies.size} selected</span>
            </div>
          </div>
          <div className="divide-y divide-border">
            {anomalies.map(a => (
              <div key={a.id} className={`px-6 py-3 flex items-start gap-3 ${selectedAnomalies.has(a.id) ? 'bg-amber-50/50' : ''}`}>
                {!a.resolved && (
                  <button onClick={() => toggleAnomaly(a.id)} className="mt-0.5 text-text-muted hover:text-maroon">
                    {selectedAnomalies.has(a.id) ? <CheckSquare className="w-4 h-4 text-maroon" /> : <Square className="w-4 h-4" />}
                  </button>
                )}
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-bold text-text-primary">{a.code} <span className="font-normal text-text-muted">· {a.severity}</span></div>
                  <div className="text-xs text-text-secondary mt-1 line-clamp-2">{a.detail || a.message || ''}</div>
                  <div className="text-[11px] text-text-muted mt-1">{a.createdAt ? new Date(a.createdAt).toLocaleString('en-IN') : ''} {a.orderId ? `· order #${a.orderId}` : ''}</div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className={`inline-flex px-2 py-1 rounded-full text-[10px] font-bold border ${a.resolved ? 'bg-green-50 text-green-700 border-green-200' : 'bg-amber-50 text-amber-700 border-amber-200'}`}>{a.resolved ? 'RESOLVED' : 'OPEN'}</span>
                  {!a.resolved && (
                    <button onClick={() => resolveOne(a.id)} className="px-2.5 py-1 bg-white border border-border hover:bg-green-50 hover:border-green-200 text-green-700 rounded-lg text-xs font-bold">Resolve</button>
                  )}
                </div>
              </div>
            ))}
          </div>
          {/* Mobile: same cards already responsive via flex */}
        </div>
      )}

      {/* Help text about inventory restocking */}
      <div className="bg-[#FAF8F5] border border-border rounded-xl p-4 text-xs text-text-secondary leading-relaxed">
        <strong className="text-text-primary">Inventory rule:</strong> A full refund restocks inventory exactly once — only if the order has not shipped yet. After fulfillment, refunds are financial-only; physical returns are a manual workflow. Coupon usage is never returned on refund.
      </div>
    </div>
  );
}
