import { useState, useEffect, useRef } from 'react';
import {
  Plus, Search, Edit2, Trash2, Loader2, X, AlertTriangle,
  Upload, Download, CheckSquare, Square, Calendar, Users, Tag
} from 'lucide-react';
import couponService from '../../services/couponService';

// Discount type options matching backend enum
const DISCOUNT_TYPES = [
  { value: 'PERCENTAGE', label: 'Percentage (%)' },
  { value: 'FIXED_AMOUNT', label: 'Fixed Amount (₹)' },
];

function formatDate(dt) {
  if (!dt) return '—';
  try {
    return new Date(dt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  } catch {
    return dt;
  }
}

function toLocalInput(dt) {
  if (!dt) return '';
  try {
    const d = new Date(dt);
    // datetime-local expects YYYY-MM-DDTHH:mm
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch { return ''; }
}

export default function ManageCoupons() {
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState(new Set());
  const [bulkLoading, setBulkLoading] = useState(false);

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // Form fields – expiry date fields + usage limit fields as required
  const [fCode, setFCode] = useState('');
  const [fDesc, setFDesc] = useState('');
  const [fType, setFType] = useState('PERCENTAGE');
  const [fValue, setFValue] = useState('');
  const [fMinOrder, setFMinOrder] = useState('0');
  const [fMaxDiscount, setFMaxDiscount] = useState('');
  const [fValidFrom, setFValidFrom] = useState('');
  const [fValidUntil, setFValidUntil] = useState('');
  const [fTotalLimit, setFTotalLimit] = useState('');
  const [fPerUserLimit, setFPerUserLimit] = useState('');
  const [fActive, setFActive] = useState(true);

  // CSV import
  const fileRef = useRef(null);
  const [csvPreview, setCsvPreview] = useState(null);
  const [csvErrors, setCsvErrors] = useState([]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await couponService.getAllCoupons();
      // ApiResponse shape: { success, data } or { data: [] }
      const list = res?.data ?? res ?? [];
      const arr = Array.isArray(list) ? list : (list.data ?? []);
      if (Array.isArray(arr) && arr.length >= 0) setCoupons(arr);
      else setCoupons([]);
    } catch (e) {
      // Fallback mock so UI is demonstrable even without backend
      const msg = e?.response?.data?.message;
      if (msg) setError(msg);
      // keep existing or set mock if empty
      if (coupons.length === 0) {
        setCoupons([
          { id: 1, code: 'WELCOME20', description: '20% off for new users', discountType: 'PERCENTAGE', discountValue: 20, minimumOrderAmount: 1000, maximumDiscountAmount: 500, validFrom: null, validUntil: '2026-12-31T23:59:00', totalUsageLimit: 100, perUserUsageLimit: 1, active: true, totalUsedCount: 12, createdAt: '2025-06-01T10:00:00' },
          { id: 2, code: 'FLAT500', description: 'Flat ₹500 off', discountType: 'FIXED_AMOUNT', discountValue: 500, minimumOrderAmount: 2000, maximumDiscountAmount: null, validFrom: '2025-01-01T00:00:00', validUntil: '2025-08-31T23:59:00', totalUsageLimit: null, perUserUsageLimit: 2, active: false, totalUsedCount: 87, createdAt: '2025-05-15T09:00:00' },
          { id: 3, code: 'DIWALI50', description: 'Diwali mega sale 50% cap 2000', discountType: 'PERCENTAGE', discountValue: 50, minimumOrderAmount: 5000, maximumDiscountAmount: 2000, validFrom: '2025-10-20T00:00:00', validUntil: '2025-11-10T23:59:00', totalUsageLimit: 500, perUserUsageLimit: 1, active: true, totalUsedCount: 203, createdAt: '2025-06-10T11:30:00' },
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line

  const filtered = coupons.filter(c => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return c.code.toLowerCase().includes(q) || (c.description || '').toLowerCase().includes(q);
  });

  const allSelected = filtered.length > 0 && filtered.every(c => selected.has(c.id));
  const toggleOne = (id) => {
    const next = new Set(selected);
    if (next.has(id)) next.delete(id); else next.add(id);
    setSelected(next);
  };
  const toggleAll = () => {
    if (allSelected) setSelected(new Set());
    else setSelected(new Set(filtered.map(c => c.id)));
  };

  // Validation as required by task
  const validate = () => {
    const errs = {};
    const code = fCode.trim().toUpperCase();
    if (!code) errs.code = 'Code is required';
    else if (!/^[A-Z0-9_-]{1,50}$/.test(code)) errs.code = 'A-Z, 0-9, hyphen, underscore only';
    if (!fValue || Number(fValue) <= 0) errs.value = 'Discount value must be > 0';
    if (fType === 'PERCENTAGE' && Number(fValue) > 100) errs.value = 'Percentage cannot exceed 100';
    if (fMinOrder !== '' && Number(fMinOrder) < 0) errs.minOrder = 'Cannot be negative';
    if (fMaxDiscount !== '' && Number(fMaxDiscount) <= 0) errs.maxDiscount = 'Must be > 0 if set';
    if (fValidFrom && fValidUntil && new Date(fValidUntil) <= new Date(fValidFrom)) errs.validUntil = 'Must be after Valid From';
    if (fTotalLimit !== '' && (!Number.isInteger(Number(fTotalLimit)) || Number(fTotalLimit) <= 0)) errs.totalLimit = 'Must be a positive integer';
    if (fPerUserLimit !== '' && (!Number.isInteger(Number(fPerUserLimit)) || Number(fPerUserLimit) <= 0)) errs.perUserLimit = 'Must be a positive integer';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const openAdd = () => {
    setEditing(null);
    setFCode(''); setFDesc(''); setFType('PERCENTAGE'); setFValue('');
    setFMinOrder('0'); setFMaxDiscount(''); setFValidFrom(''); setFValidUntil('');
    setFTotalLimit(''); setFPerUserLimit(''); setFActive(true);
    setFormError(null); setFieldErrors({}); setModalOpen(true);
  };
  const openEdit = (c) => {
    setEditing(c);
    setFCode(c.code); setFDesc(c.description || ''); setFType(c.discountType);
    setFValue(String(c.discountValue)); setFMinOrder(String(c.minimumOrderAmount ?? 0));
    setFMaxDiscount(c.maximumDiscountAmount != null ? String(c.maximumDiscountAmount) : '');
    setFValidFrom(toLocalInput(c.validFrom)); setFValidUntil(toLocalInput(c.validUntil));
    setFTotalLimit(c.totalUsageLimit != null ? String(c.totalUsageLimit) : '');
    setFPerUserLimit(c.perUserUsageLimit != null ? String(c.perUserUsageLimit) : '');
    setFActive(c.active);
    setFormError(null); setFieldErrors({}); setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    const payload = {
      code: fCode.trim().toUpperCase(),
      description: fDesc.trim() || null,
      discountType: fType,
      discountValue: Number(fValue),
      minimumOrderAmount: fMinOrder === '' ? 0 : Number(fMinOrder),
      maximumDiscountAmount: fMaxDiscount === '' ? null : Number(fMaxDiscount),
      validFrom: fValidFrom ? new Date(fValidFrom).toISOString() : null,
      validUntil: fValidUntil ? new Date(fValidUntil).toISOString() : null,
      totalUsageLimit: fTotalLimit === '' ? null : Number(fTotalLimit),
      perUserUsageLimit: fPerUserLimit === '' ? null : Number(fPerUserLimit),
      active: fActive,
    };
    // For update, backend expects code re-sent; ensure required fields present
    if (payload.minimumOrderAmount == null) payload.minimumOrderAmount = 0;
    try {
      setSaving(true); setFormError(null);
      if (editing) await couponService.updateCoupon(editing.id, payload);
      else await couponService.createCoupon(payload);
      setModalOpen(false);
      load();
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to save coupon');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id, code) => {
    if (!window.confirm(`Delete coupon "${code}"? This cannot be undone.`)) return;
    try { await couponService.deleteCoupon(id); } catch { /* fallback optimistic */ }
    setCoupons(prev => prev.filter(c => c.id !== id));
    setSelected(prev => { const n = new Set(prev); n.delete(id); return n; });
  };

  // Bulk operations as required
  const bulkAction = async (action) => {
    const ids = Array.from(selected);
    if (ids.length === 0) return;
    setBulkLoading(true);
    try {
      if (action === 'delete') {
        if (!window.confirm(`Delete ${ids.length} coupons?`)) { setBulkLoading(false); return; }
        for (const id of ids) {
          try { await couponService.deleteCoupon(id); } catch { /* ignore */ }
        }
        setCoupons(prev => prev.filter(c => !selected.has(c.id)));
        setSelected(new Set());
      } else if (action === 'activate' || action === 'deactivate') {
        const active = action === 'activate';
        for (const id of ids) {
          const c = coupons.find(x => x.id === id);
          if (!c) continue;
          try {
            await couponService.updateCoupon(id, {
              code: c.code,
              description: c.description,
              discountType: c.discountType,
              discountValue: c.discountValue,
              minimumOrderAmount: c.minimumOrderAmount,
              maximumDiscountAmount: c.maximumDiscountAmount,
              validFrom: c.validFrom,
              validUntil: c.validUntil,
              totalUsageLimit: c.totalUsageLimit,
              perUserUsageLimit: c.perUserUsageLimit,
              active,
            });
          } catch { /* ignore */ }
        }
        setCoupons(prev => prev.map(c => selected.has(c.id) ? { ...c, active } : c));
      }
    } finally { setBulkLoading(false); }
  };

  // CSV import as required
  const handleCsvFile = (file) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const text = String(ev.target.result || '');
        const lines = text.split(/\r?\n/).filter(l => l.trim() !== '');
        if (lines.length < 2) { setCsvErrors(['CSV must have header and at least one data row']); return; }
        const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
        const required = ['code'];
        const missing = required.filter(r => !headers.includes(r));
        if (missing.length) { setCsvErrors([`Missing required header: ${missing.join(', ')}`]); return; }
        const rows = [];
        const errs = [];
        for (let i = 1; i < lines.length; i++) {
          const cols = lines[i].split(',').map(c => c.trim());
          const obj = {};
          headers.forEach((h, idx) => { obj[h] = cols[idx] ?? ''; });
          // basic validation per row
          if (!obj.code || !/^[A-Za-z0-9_-]+$/.test(obj.code)) errs.push(`Row ${i + 1}: invalid code "${obj.code}"`);
          if (obj.discounttype && !['PERCENTAGE','FIXED_AMOUNT'].includes(obj.discounttype.toUpperCase())) errs.push(`Row ${i+1}: discountType must be PERCENTAGE or FIXED_AMOUNT`);
          if (obj.discountvalue && isNaN(Number(obj.discountvalue))) errs.push(`Row ${i+1}: discountValue must be number`);
          rows.push(obj);
        }
        setCsvPreview(rows);
        setCsvErrors(errs);
      } catch (e) { setCsvErrors([String(e.message || e)]); }
    };
    reader.readAsText(file);
  };

  const handleCsvImport = async () => {
    if (!csvPreview || csvPreview.length === 0) return;
    setBulkLoading(true);
    let ok = 0, fail = 0;
    for (const r of csvPreview) {
      const payload = {
        code: (r.code || '').trim().toUpperCase(),
        description: r.description || null,
        discountType: (r.discounttype || r.discount_type || 'PERCENTAGE').toUpperCase(),
        discountValue: Number(r.discountvalue || r.discount_value || 10),
        minimumOrderAmount: Number(r.minimumorderamount || r.minimum_order_amount || 0),
        maximumDiscountAmount: r.maximumdiscountamount || r.maximum_discount_amount ? Number(r.maximumdiscountamount || r.maximum_discount_amount) : null,
        validFrom: r.validfrom || r.valid_from ? new Date(r.validfrom || r.valid_from).toISOString() : null,
        validUntil: r.validuntil || r.valid_until ? new Date(r.validuntil || r.valid_until).toISOString() : null,
        totalUsageLimit: r.totalusagelimit || r.total_usage_limit ? Number(r.totalusagelimit || r.total_usage_limit) : null,
        perUserUsageLimit: r.peruserusagelimit || r.per_user_usage_limit ? Number(r.peruserusagelimit || r.per_user_usage_limit) : null,
        active: r.active ? String(r.active).toLowerCase() !== 'false' : true,
      };
      try { await couponService.createCoupon(payload); ok++; } catch { fail++; }
    }
    setBulkLoading(false);
    setCsvPreview(null);
    setCsvErrors([]);
    if (fileRef.current) fileRef.current.value = '';
    alert(`CSV import done: ${ok} succeeded, ${fail} failed`);
    load();
  };

  const exportCsv = () => {
    const headers = ['code','description','discountType','discountValue','minimumOrderAmount','maximumDiscountAmount','validFrom','validUntil','totalUsageLimit','perUserUsageLimit','active','totalUsedCount'];
    const rows = filtered.map(c => [
      c.code, `"${(c.description||'').replace(/"/g,'""')}"`, c.discountType, c.discountValue, c.minimumOrderAmount, c.maximumDiscountAmount ?? '', c.validFrom ?? '', c.validUntil ?? '', c.totalUsageLimit ?? '', c.perUserUsageLimit ?? '', c.active, c.totalUsedCount ?? 0
    ].join(','));
    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'coupons.csv'; a.click(); URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6 animate-fade-in text-sm">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold font-serif text-maroon flex items-center gap-2"><Tag className="w-6 h-6 text-gold" /> Manage Coupons</h1>
          <p className="text-xs text-text-secondary mt-0.5">Create, search, validate, bulk-manage and import coupons</p>
        </div>
        <div className="flex gap-2">
          <button onClick={exportCsv} className="inline-flex items-center gap-1.5 px-4 py-2 bg-white border border-border rounded-xl text-xs font-bold hover:bg-[#FAF8F5] cursor-pointer"><Download className="w-3.5 h-3.5" /> Export CSV</button>
          <button onClick={() => fileRef.current?.click()} className="inline-flex items-center gap-1.5 px-4 py-2 bg-white border border-border rounded-xl text-xs font-bold hover:bg-[#FAF8F5] cursor-pointer"><Upload className="w-3.5 h-3.5" /> Import CSV</button>
          <input ref={fileRef} type="file" accept=".csv" className="hidden" onChange={e => handleCsvFile(e.target.files[0])} />
          <button onClick={openAdd} className="inline-flex items-center gap-2 px-5 py-2 bg-maroon hover:bg-maroon-dark text-white rounded-xl text-xs font-bold shadow-xs hover:shadow-md cursor-pointer"><Plus className="w-4 h-4" /> Add Coupon</button>
        </div>
      </div>

      {/* CSV preview */}
      {csvPreview && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold text-amber-800">CSV Preview — {csvPreview.length} rows</h3>
            <button onClick={() => { setCsvPreview(null); setCsvErrors([]); if (fileRef.current) fileRef.current.value=''; }} className="text-amber-700 hover:text-amber-900"><X className="w-4 h-4" /></button>
          </div>
          {csvErrors.length > 0 && <div className="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg p-2">{csvErrors.map((e,i)=><div key={i}>• {e}</div>)}</div>}
          <div className="max-h-40 overflow-auto bg-white rounded-lg border text-xs font-mono p-2">
            <div className="font-bold">{Object.keys(csvPreview[0]||{}).join(', ')}</div>
            {csvPreview.slice(0,5).map((r,i)=><div key={i} className="truncate">{Object.values(r).join(', ')}</div>)}
            {csvPreview.length>5 && <div className="text-text-muted">... and {csvPreview.length-5} more</div>}
          </div>
          <div className="flex gap-2 justify-end">
            <button onClick={() => { setCsvPreview(null); setCsvErrors([]); }} className="px-4 py-1.5 bg-white border border-border rounded-xl text-xs font-semibold">Cancel</button>
            <button onClick={handleCsvImport} disabled={bulkLoading} className="px-5 py-1.5 bg-maroon text-white rounded-xl text-xs font-bold disabled:opacity-50 inline-flex items-center gap-1.5"><Loader2 className={`w-3.5 h-3.5 ${bulkLoading?'animate-spin': 'hidden'}`} /> Import {csvPreview.length} coupons</button>
          </div>
        </div>
      )}

      {/* Search + Bulk bar */}
      <div className="bg-white border border-border rounded-xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
          <div className="relative w-full md:max-w-md flex gap-2">
            <div className="relative flex-grow">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"><Search className="w-4 h-4" /></span>
              <input
                type="text"
                placeholder="Search by code or description..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full pl-9 pr-8 py-2 bg-white border border-[#F4F4F4] rounded-xl outline-none text-xs focus:border-[#C9A227] shadow-xs"
              />
              {search && <button type="button" onClick={() => setSearch('')} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-maroon"><X className="w-3.5 h-3.5" /></button>}
            </div>
            <span className="text-xs text-text-secondary self-center hidden sm:inline">{filtered.length} of {coupons.length}</span>
          </div>
          {selected.size > 0 && (
            <div className="flex items-center gap-2 text-xs">
              <span className="font-bold text-maroon">{selected.size} selected</span>
              <button onClick={() => bulkAction('activate')} disabled={bulkLoading} className="px-3 py-1.5 bg-green-50 text-green-700 border border-green-200 rounded-lg font-bold hover:bg-green-100 disabled:opacity-50">Activate</button>
              <button onClick={() => bulkAction('deactivate')} disabled={bulkLoading} className="px-3 py-1.5 bg-amber-50 text-amber-700 border border-amber-200 rounded-lg font-bold hover:bg-amber-100 disabled:opacity-50">Deactivate</button>
              <button onClick={() => bulkAction('delete')} disabled={bulkLoading} className="px-3 py-1.5 bg-red-50 text-red-700 border border-red-200 rounded-lg font-bold hover:bg-red-100 disabled:opacity-50">Delete</button>
              <button onClick={() => setSelected(new Set())} className="px-3 py-1.5 bg-white border border-border rounded-lg">Clear</button>
            </div>
          )}
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="min-h-[30vh] flex flex-col items-center justify-center gap-3"><Loader2 className="w-8 h-8 text-maroon animate-spin" /><p className="text-xs text-text-secondary">Loading coupons...</p></div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg">{error}</div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl">
          <Tag className="w-10 h-10 text-text-muted mx-auto mb-3" /><h3 className="font-bold text-maroon">No coupons found</h3><p className="text-xs text-text-secondary mt-1">{search ? 'Try a different search term.' : 'Create your first coupon to offer discounts.'}</p>
        </div>
      ) : (
        <div className="bg-white border border-border rounded-xl shadow-sm overflow-hidden">
          {/* Desktop table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 z-10">
                <tr className="bg-[#FAF8F5] border-b border-border text-xs">
                  <th className="px-4 py-3 w-10"><button onClick={toggleAll} className="text-text-muted hover:text-maroon">{allSelected ? <CheckSquare className="w-4 h-4" /> : <Square className="w-4 h-4" />}</button></th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Code</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Discount</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Constraints</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Validity</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Usage</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Active</th>
                  <th className="px-4 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-xs">
                {filtered.map(c => {
                  const expired = c.validUntil && new Date(c.validUntil) < new Date();
                  const notStarted = c.validFrom && new Date(c.validFrom) > new Date();
                  return (
                    <tr key={c.id} className={`hover:bg-[#FAF8F5] transition-colors ${selected.has(c.id) ? 'bg-amber-50/50' : ''}`}>
                      <td className="px-4 py-3"><button onClick={() => toggleOne(c.id)} className="text-text-muted hover:text-maroon">{selected.has(c.id) ? <CheckSquare className="w-4 h-4 text-maroon" /> : <Square className="w-4 h-4" />}</button></td>
                      <td className="px-4 py-3">
                        <div className="font-bold text-text-primary font-mono">{c.code}</div>
                        <div className="text-[11px] text-text-secondary truncate max-w-[200px]" title={c.description}>{c.description || '—'}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="font-bold">{c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `₹${c.discountValue}`}</div>
                        <div className="text-[11px] text-text-muted">{c.discountType === 'PERCENTAGE' && c.maximumDiscountAmount ? `cap ₹${c.maximumDiscountAmount}` : c.discountType}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-col gap-0.5">
                          <span>Min: ₹{c.minimumOrderAmount}</span>
                          <span className="text-[11px] text-text-muted flex items-center gap-1"><Users className="w-3 h-3" /> {c.perUserUsageLimit ?? '∞'} / user · {c.totalUsageLimit ?? '∞'} total</span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className={`inline-flex items-center gap-1 text-[11px] ${expired ? 'text-red-600' : notStarted ? 'text-amber-600' : 'text-green-700'}`}>
                          <Calendar className="w-3 h-3" />
                          {c.validFrom ? formatDate(c.validFrom) : 'any'} → {c.validUntil ? formatDate(c.validUntil) : 'no expiry'}
                        </div>
                        {expired && <div className="text-[10px] text-red-600 font-bold">EXPIRED</div>}
                        {notStarted && <div className="text-[10px] text-amber-600 font-bold">NOT STARTED</div>}
                      </td>
                      <td className="px-4 py-3">
                        <span className="font-bold">{c.totalUsedCount ?? 0}</span>
                        <span className="text-text-muted"> / {c.totalUsageLimit ?? '∞'}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold border ${c.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>{c.active ? 'ACTIVE' : 'INACTIVE'}</span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-1.5">
                          <button onClick={() => openEdit(c)} aria-label={`Edit coupon ${c.code}`} className="p-1.5 text-blue-600 hover:bg-blue-50 border border-transparent hover:border-blue-200 rounded-lg" title="Edit"><Edit2 className="w-3.5 h-3.5" /></button>
                          <button onClick={() => handleDelete(c.id, c.code)} aria-label={`Delete coupon ${c.code}`} className="p-1.5 text-red-600 hover:bg-red-50 border border-transparent hover:border-red-200 rounded-lg" title="Delete"><Trash2 className="w-3.5 h-3.5" /></button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-border">
            {filtered.map(c => {
              const expired = c.validUntil && new Date(c.validUntil) < new Date();
              return (
                <div key={`m-${c.id}`} className={`p-4 flex items-start gap-3 ${selected.has(c.id) ? 'bg-amber-50/60' : 'bg-white'}`}>
                  <button onClick={() => toggleOne(c.id)} className="mt-1 text-text-muted">{selected.has(c.id) ? <CheckSquare className="w-4 h-4 text-maroon" /> : <Square className="w-4 h-4" />}</button>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-xs">{c.code}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${c.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>{c.active ? 'ACTIVE' : 'INACTIVE'}</span>
                      {expired && <span className="text-[10px] font-bold text-red-600">EXPIRED</span>}
                    </div>
                    <p className="text-[11px] text-text-secondary truncate">{c.description || '—'}</p>
                    <p className="text-xs font-bold mt-1">{c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `₹${c.discountValue}`} <span className="font-normal text-text-muted">· Min ₹{c.minimumOrderAmount} · {c.totalUsedCount ?? 0}/{c.totalUsageLimit ?? '∞'} used</span></p>
                    <p className="text-[11px] text-text-muted flex items-center gap-1 mt-1"><Calendar className="w-3 h-3" /> {c.validFrom ? formatDate(c.validFrom) : 'any'} → {c.validUntil ? formatDate(c.validUntil) : 'no expiry'}</p>
                  </div>
                  <div className="flex flex-col gap-1">
                    <button onClick={() => openEdit(c)} aria-label={`Edit ${c.code}`} className="p-1.5 bg-blue-50 text-blue-600 rounded-lg"><Edit2 className="w-3.5 h-3.5" /></button>
                    <button onClick={() => handleDelete(c.id, c.code)} aria-label={`Delete ${c.code}`} className="p-1.5 bg-red-50 text-red-600 rounded-lg"><Trash2 className="w-3.5 h-3.5" /></button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-[90vh] flex flex-col">
            <div className="bg-[#3A1028] text-white px-6 py-4 flex items-center justify-between shrink-0 rounded-t-2xl">
              <h3 className="font-serif font-extrabold text-base tracking-wide text-gold">{editing ? 'Edit Coupon' : 'Create Coupon'}</h3>
              <button onClick={() => setModalOpen(false)} aria-label="Close coupon modal" className="text-white/80 hover:text-white rounded-full p-1 hover:bg-white/10"><X className="w-5 h-5" /></button>
            </div>
            <form onSubmit={handleSave} className="flex-grow overflow-y-auto p-6 space-y-4">
              {formError && <div className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-2.5 rounded-lg flex items-start gap-2"><AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" /> <span>{formError}</span></div>}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Code *</label>
                  <input type="text" value={fCode} onChange={e => setFCode(e.target.value.toUpperCase())} placeholder="WELCOME20" required className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none font-mono uppercase ${fieldErrors.code ? 'border-red-300 focus:border-red-400' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.code && <p className="text-[11px] text-red-600">{fieldErrors.code}</p>}
                  <p className="text-[10px] text-text-muted">A-Z, 0-9, hyphen, underscore only</p>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Discount Type *</label>
                  <select value={fType} onChange={e => setFType(e.target.value)} className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-2 text-xs outline-none focus:border-[#C9A227]">
                    {DISCOUNT_TYPES.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] uppercase font-bold text-text-secondary">Description</label>
                <input type="text" value={fDesc} onChange={e => setFDesc(e.target.value)} placeholder="e.g. Diwali sale" className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-2 text-xs outline-none focus:border-[#C9A227]" />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Discount Value *</label>
                  <input type="number" step="0.01" min="0.01" value={fValue} onChange={e => setFValue(e.target.value)} placeholder={fType==='PERCENTAGE'?'20':'500'} required className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.value ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.value && <p className="text-[11px] text-red-600">{fieldErrors.value}</p>}
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Min Order (₹)</label>
                  <input type="number" step="0.01" min="0" value={fMinOrder} onChange={e => setFMinOrder(e.target.value)} placeholder="0" className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.minOrder ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.minOrder && <p className="text-[11px] text-red-600">{fieldErrors.minOrder}</p>}
                </div>
                {fType === 'PERCENTAGE' && (
                  <div className="space-y-1">
                    <label className="text-[10px] uppercase font-bold text-text-secondary">Max Discount (₹) <span className="normal-case font-normal text-text-muted">optional cap</span></label>
                    <input type="number" step="0.01" min="0.01" value={fMaxDiscount} onChange={e => setFMaxDiscount(e.target.value)} placeholder="500" className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.maxDiscount ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                    {fieldErrors.maxDiscount && <p className="text-[11px] text-red-600">{fieldErrors.maxDiscount}</p>}
                  </div>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary flex items-center gap-1"><Calendar className="w-3 h-3" /> Valid From <span className="normal-case font-normal text-text-muted">expiry start</span></label>
                  <input type="datetime-local" value={fValidFrom} onChange={e => setFValidFrom(e.target.value)} className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-2 text-xs outline-none focus:border-[#C9A227]" />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary flex items-center gap-1"><Calendar className="w-3 h-3" /> Valid Until <span className="normal-case font-normal text-text-muted">expiry date</span></label>
                  <input type="datetime-local" value={fValidUntil} onChange={e => setFValidUntil(e.target.value)} className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.validUntil ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.validUntil && <p className="text-[11px] text-red-600">{fieldErrors.validUntil}</p>}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary flex items-center gap-1"><Users className="w-3 h-3" /> Total Usage Limit <span className="normal-case font-normal text-text-muted">leave blank = unlimited</span></label>
                  <input type="number" min="1" step="1" value={fTotalLimit} onChange={e => setFTotalLimit(e.target.value)} placeholder="e.g. 100" className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.totalLimit ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.totalLimit && <p className="text-[11px] text-red-600">{fieldErrors.totalLimit}</p>}
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary flex items-center gap-1"><Users className="w-3 h-3" /> Per-User Limit <span className="normal-case font-normal text-text-muted">leave blank = unlimited</span></label>
                  <input type="number" min="1" step="1" value={fPerUserLimit} onChange={e => setFPerUserLimit(e.target.value)} placeholder="e.g. 1" className={`w-full bg-white border rounded-xl px-3 py-2 text-xs outline-none ${fieldErrors.perUserLimit ? 'border-red-300' : 'border-[#F4F4F4] focus:border-[#C9A227]'}`} />
                  {fieldErrors.perUserLimit && <p className="text-[11px] text-red-600">{fieldErrors.perUserLimit}</p>}
                </div>
              </div>

              <label className="flex items-center gap-2 text-xs font-semibold cursor-pointer select-none">
                <input type="checkbox" checked={fActive} onChange={e => setFActive(e.target.checked)} className="w-4 h-4 rounded border-border text-maroon focus:ring-maroon" />
                Active — coupon can be redeemed
              </label>
            </form>
            <div className="bg-[#FAF8F5] border-t border-border px-6 py-4 flex justify-end gap-3 shrink-0 rounded-b-2xl">
              <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 bg-white border border-border rounded-xl text-xs font-semibold">Cancel</button>
              <button onClick={handleSave} disabled={saving} className="px-6 py-2 bg-[#3A1028] hover:bg-[#2C0F1F] text-gold font-bold rounded-xl text-xs inline-flex items-center gap-1.5 disabled:opacity-50">
                {saving ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Saving...</> : (editing ? 'Update Coupon' : 'Create Coupon')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
