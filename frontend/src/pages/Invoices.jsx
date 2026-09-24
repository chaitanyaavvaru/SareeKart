import { useEffect, useState, useCallback } from 'react';
import { FileText, Download, Loader2, Calendar, RefreshCw } from 'lucide-react';
import { downloadInvoice } from '../services/invoiceService';
import SEO from '../components/common/SEO';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val || 0);

const formatDate = (dateStr) => {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
};

const STATUS_COLORS = {
  PENDING:   'bg-[#F3E6C7] text-[#9B6A27] border-[#E5C98D]',
  CONFIRMED: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  SHIPPED:   'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  DELIVERED: 'bg-[#E7F3EE] text-[#17644F] border-[#B7DCCB]',
};

export default function Invoices() {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [status, setStatus] = useState('');
  const [downloading, setDownloading] = useState({});

  const fetchInvoices = useCallback(() => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams();
    if (from) params.append('from', new Date(from).toISOString());
    if (to)   params.append('to',   new Date(to).toISOString());
    if (status) params.append('status', status);

    fetch(`/api/admin/invoices?${params.toString()}`, { credentials: 'include' })
      .then((res) => { if (!res.ok) throw new Error(`HTTP ${res.status}`); return res.json(); })
      .then((data) => { setInvoices(data); setLoading(false); })
      .catch((e) => { setError(e.message); setLoading(false); });
  }, [from, to, status]);

  useEffect(() => { fetchInvoices(); }, [fetchInvoices]);

  const handleDownload = async (orderId, format) => {
    const key = `${orderId}-${format}`;
    setDownloading((d) => ({ ...d, [key]: true }));
    try {
      await downloadInvoice(orderId, format);
    } catch (e) {
      alert(`Download failed: ${e.message}`);
    } finally {
      setDownloading((d) => ({ ...d, [key]: false }));
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO title="Invoices | SareeKart" description="Download GST invoices for SareeKart orders." noindex={true} />

      {/* Header */}
      <section className="bg-white shadow-xs">
        <div className="section-shell py-8">
          <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Finance</p>
          <h1 className="mt-2 text-4xl font-bold sm:text-5xl">Invoices</h1>
          <p className="mt-3 text-sm font-medium text-[#71817A]">Download PDF or Excel GST invoices for any order.</p>
        </div>
      </section>

      <main className="section-shell py-8 space-y-6">
        {/* Filters */}
        <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-5">
          <p className="text-[11px] font-black uppercase tracking-[0.14em] text-[#71817A] mb-4">Filter invoices</p>
          <div className="flex flex-wrap gap-4 items-end">
            <label className="flex flex-col gap-1 text-xs font-bold text-[#42504C]">
              From
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)}
                className="h-9 rounded-[6px] border border-[#DDD8CF] px-3 text-sm" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-bold text-[#42504C]">
              To
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)}
                className="h-9 rounded-[6px] border border-[#DDD8CF] px-3 text-sm" />
            </label>
            <label className="flex flex-col gap-1 text-xs font-bold text-[#42504C]">
              Status
              <select value={status} onChange={(e) => setStatus(e.target.value)}
                className="h-9 rounded-[6px] border border-[#DDD8CF] px-3 text-sm bg-white">
                <option value="">All</option>
                <option value="PENDING">Pending</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="SHIPPED">Shipped</option>
                <option value="DELIVERED">Delivered</option>
              </select>
            </label>
            <button onClick={fetchInvoices}
              className="h-9 px-5 rounded-[6px] bg-[#17211F] text-white text-xs font-bold flex items-center gap-2 hover:bg-[#1E6A62] transition">
              <RefreshCw className="h-3.5 w-3.5" />
              Apply
            </button>
          </div>
        </div>

        {/* Table */}
        {error && (
          <div className="rounded-[8px] border border-[#E5C98D] bg-[#F3E6C7] p-4 text-sm font-bold text-[#9B6A27]">
            Could not load invoices: {error}
          </div>
        )}

        {loading ? (
          <div className="flex min-h-[200px] items-center justify-center gap-3 rounded-[8px] border border-[#DDD8CF] bg-white">
            <Loader2 className="h-7 w-7 animate-spin text-[#1E6A62]" />
            <span className="text-sm font-bold text-[#71817A]">Loading invoices…</span>
          </div>
        ) : invoices.length === 0 ? (
          <div className="flex min-h-[200px] flex-col items-center justify-center gap-3 rounded-[8px] border border-[#DDD8CF] bg-white">
            <FileText className="h-10 w-10 text-[#9AA59F]" />
            <p className="text-sm font-bold text-[#71817A]">No invoices match your filter.</p>
          </div>
        ) : (
          <div className="overflow-x-auto rounded-[8px] border border-[#DDD8CF] bg-white shadow-xs">
            <table className="w-full table-auto text-sm">
              <thead className="border-b border-[#DDD8CF] bg-[#F7F4EE]">
                <tr>
                  <th className="p-4 text-left text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">Order</th>
                  <th className="p-4 text-left text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">Date</th>
                  <th className="p-4 text-left text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">Status</th>
                  <th className="p-4 text-right text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">Amount</th>
                  <th className="p-4 text-center text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">PDF</th>
                  <th className="p-4 text-center text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">Excel</th>
                </tr>
              </thead>
              <tbody>
                {invoices.map((inv) => (
                  <tr key={inv.orderId} className="border-b border-[#DDD8CF] last:border-0 hover:bg-[#F7F4EE] transition">
                    <td className="p-4 font-bold text-[#17211F]">#SK-{inv.orderId}</td>
                    <td className="p-4 text-[#71817A] inline-flex items-center gap-1">
                      <Calendar className="h-3.5 w-3.5" />
                      {formatDate(inv.createdAt)}
                    </td>
                    <td className="p-4">
                      <span className={`rounded-full border px-3 py-1 text-[10px] font-black uppercase tracking-[0.1em] ${STATUS_COLORS[inv.status] || 'bg-[#EEF3F6] text-[#42504C] border-[#DDD8CF]'}`}>
                        {inv.status.toLowerCase()}
                      </span>
                    </td>
                    <td className="p-4 text-right font-bold text-[#17211F]">{formatCurrency(inv.totalAmount)}</td>
                    <td className="p-4 text-center">
                      <button
                        disabled={downloading[`${inv.orderId}-PDF`]}
                        onClick={() => handleDownload(inv.orderId, 'PDF')}
                        className="inline-flex items-center gap-1.5 rounded-[6px] border border-[#1E6A62] px-3 py-1.5 text-[11px] font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition disabled:opacity-50">
                        {downloading[`${inv.orderId}-PDF`]
                          ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          : <Download className="h-3.5 w-3.5" />}
                        PDF
                      </button>
                    </td>
                    <td className="p-4 text-center">
                      <button
                        disabled={downloading[`${inv.orderId}-EXCEL`]}
                        onClick={() => handleDownload(inv.orderId, 'EXCEL')}
                        className="inline-flex items-center gap-1.5 rounded-[6px] border border-[#1E6A62] px-3 py-1.5 text-[11px] font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition disabled:opacity-50">
                        {downloading[`${inv.orderId}-EXCEL`]
                          ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          : <Download className="h-3.5 w-3.5" />}
                        Excel
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
