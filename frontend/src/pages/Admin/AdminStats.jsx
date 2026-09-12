import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowUpRight,
  Boxes,
  Calendar,
  ClipboardList,
  IndianRupee,
  Loader2,
  RefreshCw,
  ShoppingBag,
  Users,
} from 'lucide-react';
import api from '../../api/axiosConfig';

const formatCurrency = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
}).format(Number(value) || 0);

const statusStyles = {
  PENDING: 'border-[#E8D8F5] bg-[#F8F1FC] text-[#7A4A9B]',
  PROCESSING: 'border-[#F2D8B7] bg-[#FFF7EC] text-[#A7611E]',
  SHIPPED: 'border-[#C9DDEE] bg-[#F0F7FC] text-[#2C648A]',
  DELIVERED: 'border-[#C9E0D8] bg-[#EEF8F3] text-[#2C765F]',
  CANCELLED: 'border-[#E8C9C5] bg-[#FFF2F0] text-[#A0443D]',
};

function StatusBadge({ status }) {
  const normalized = status?.toUpperCase() || 'UNKNOWN';
  return <span className={`inline-flex border px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.08em] ${statusStyles[normalized] || 'border-[#DDD8CF] bg-[#F7F4EE] text-[#71817A]'}`}>{status || 'Unknown'}</span>;
}

function MetricCard({ icon: Icon, label, value, detail, tone }) {
  return (
    <div className="flex min-h-[142px] items-start justify-between gap-4 border border-[#DDD8CF] bg-white p-5 shadow-[0_8px_20px_rgba(23,33,31,0.04)]">
      <div>
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#71817A]">{label}</p>
        <p className="mt-3 font-serif text-3xl font-medium text-[#17211F]">{value}</p>
        <p className="mt-2 text-xs font-semibold text-[#71817A]">{detail}</p>
      </div>
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center ${tone}`}><Icon className="h-5 w-5" /></div>
    </div>
  );
}

export default function AdminStats() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchStats = useCallback(async (isRefresh = false) => {
    try {
      setError(null);
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      const response = await api.get('/admin/dashboard');
      if (response.data?.success && response.data?.data) setStats(response.data.data);
      else setError(response.data?.message || 'The dashboard did not return usable sales data.');
    } catch (err) {
      console.error('Error fetching dashboard stats:', err);
      setError(err.response?.data?.message || 'The dashboard service is unavailable. Start the backend and try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => fetchStats(), 0);
    return () => window.clearTimeout(timer);
  }, [fetchStats]);

  const recentOrders = useMemo(() => stats?.recentOrders || [], [stats]);
  const revenueGoal = 1000000;
  const revenue = Number(stats?.totalRevenue) || 0;
  const revenuePercent = Math.min(100, Math.round((revenue / revenueGoal) * 100));
  const statusCounts = useMemo(() => recentOrders.reduce((counts, order) => {
    const status = order.status?.toUpperCase() || 'UNKNOWN';
    counts[status] = (counts[status] || 0) + 1;
    return counts;
  }, {}), [recentOrders]);

  if (loading) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center text-[#71817A]">
        <Loader2 className="h-8 w-8 animate-spin text-[#1E6A62]" />
        <p className="mt-4 text-xs font-bold uppercase tracking-[0.14em]">Loading sales workspace</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="border border-[#E8C9C5] bg-[#FFF2F0] p-5 text-[#8B3E37]">
        <p className="text-sm font-bold">Could not load the sales dashboard</p>
        <p className="mt-2 text-xs leading-5">{error}</p>
        <button type="button" onClick={() => fetchStats()} className="mt-4 inline-flex min-h-10 items-center gap-2 bg-[#17211F] px-4 text-xs font-bold uppercase tracking-[0.08em] text-white hover:bg-[#1E6A62]">
          Try again <RefreshCw className="h-3.5 w-3.5" />
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.22em] text-[#B84F49]">Sales overview</p>
          <h2 className="mt-2 font-serif text-4xl font-medium text-[#17211F]">Know what is moving.</h2>
          <p className="mt-2 max-w-xl text-sm leading-6 text-[#71817A]">A focused view of revenue, orders, customers, and stock signals for the SareeKart team.</p>
        </div>
        <button type="button" onClick={() => fetchStats(true)} disabled={refreshing} className="inline-flex min-h-10 items-center justify-center gap-2 border border-[#C9C1B5] bg-white px-4 text-xs font-bold uppercase tracking-[0.08em] text-[#4E5B56] transition hover:border-[#1E6A62] hover:text-[#1E6A62] disabled:cursor-wait disabled:opacity-60">
          <RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} /> Refresh data
        </button>
      </div>

      {stats?.lowStockProductsCount > 0 && (
        <div className="flex flex-col gap-3 border border-[#F2D8B7] bg-[#FFF7EC] p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-[#C48B3C]" />
            <div><p className="text-sm font-bold text-[#87531C]">Inventory needs attention</p><p className="mt-1 text-xs leading-5 text-[#9A6A32]">{stats.lowStockProductsCount} products are below the critical stock threshold.</p></div>
          </div>
          <Link to="/admin/products" className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.08em] text-[#87531C] hover:text-[#B84F49]">Review stock <ArrowUpRight className="h-4 w-4" /></Link>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={IndianRupee} label="Gross sales" value={formatCurrency(stats?.totalRevenue)} detail="Processed order value" tone="bg-[#EEF8F3] text-[#2C765F]" />
        <MetricCard icon={ClipboardList} label="Orders" value={stats?.totalOrders ?? 0} detail="Purchases registered" tone="bg-[#F0F7FC] text-[#2C648A]" />
        <MetricCard icon={ShoppingBag} label="Active sarees" value={stats?.totalProducts ?? 0} detail="Products in catalog" tone="bg-[#F8F1FC] text-[#7A4A9B]" />
        <MetricCard icon={Users} label="Customers" value={stats?.totalCustomers ?? 0} detail="Registered accounts" tone="bg-[#FFF7EC] text-[#A7611E]" />
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
        <section className="border border-[#DDD8CF] bg-[#17211F] p-6 text-white sm:p-7">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div><p className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#F3C56A]">Monthly target</p><h3 className="mt-2 font-serif text-3xl font-medium">Revenue goal progress</h3><p className="mt-2 text-xs text-white/62">Current sales against a {formatCurrency(revenueGoal)} target.</p></div>
            <span className="inline-flex w-fit border border-white/15 px-3 py-1.5 text-xs font-bold text-[#F3C56A]">{revenuePercent}% achieved</span>
          </div>
          <div className="mt-8 h-3 bg-white/12"><div className="h-full bg-[#F3C56A] transition-all duration-500" style={{ width: `${revenuePercent}%` }} /></div>
          <div className="mt-6 grid grid-cols-2 gap-5 border-t border-white/12 pt-5 sm:grid-cols-3">
            <div><p className="text-[10px] uppercase tracking-[0.12em] text-white/45">Current</p><p className="mt-1 text-lg font-bold">{formatCurrency(revenue)}</p></div>
            <div><p className="text-[10px] uppercase tracking-[0.12em] text-white/45">Remaining</p><p className="mt-1 text-lg font-bold">{formatCurrency(Math.max(0, revenueGoal - revenue))}</p></div>
            <div className="col-span-2 sm:col-span-1"><p className="text-[10px] uppercase tracking-[0.12em] text-white/45">Signal</p><p className="mt-1 text-lg font-bold text-[#F3C56A]">{revenuePercent >= 70 ? 'On track' : 'Needs a push'}</p></div>
          </div>
        </section>

        <section className="border border-[#DDD8CF] bg-white p-6 sm:p-7">
          <div className="flex items-start justify-between gap-4"><div><p className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#1E6A62]">Order pulse</p><h3 className="mt-2 font-serif text-3xl font-medium text-[#17211F]">Recent sales mix</h3></div><Boxes className="h-5 w-5 text-[#C48B3C]" /></div>
          <div className="mt-6 grid gap-3">
            {['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'].map((status) => {
              const count = statusCounts[status] || 0;
              const percent = recentOrders.length ? Math.round((count / recentOrders.length) * 100) : 0;
              return <div key={status}><div className="flex items-center justify-between text-xs font-bold"><span className="text-[#4E5B56]">{status.toLowerCase()}</span><span className="text-[#71817A]">{count}</span></div><div className="mt-1.5 h-2 bg-[#F7F4EE]"><div className="h-full bg-[#1E6A62]" style={{ width: `${percent}%` }} /></div></div>;
            })}
          </div>
          <Link to="/admin/orders" className="mt-6 inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.08em] text-[#1E6A62] hover:text-[#B84F49]">Open order desk <ArrowUpRight className="h-4 w-4" /></Link>
        </section>
      </div>

      <section className="border border-[#DDD8CF] bg-white">
        <div className="flex flex-col justify-between gap-3 border-b border-[#DDD8CF] px-5 py-5 sm:flex-row sm:items-center sm:px-6">
          <div><p className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#1E6A62]">Latest activity</p><h3 className="mt-1 font-serif text-2xl font-medium text-[#17211F]">Recent orders</h3></div>
          <Link to="/admin/orders" className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.08em] text-[#1E6A62]">Manage all <ArrowUpRight className="h-4 w-4" /></Link>
        </div>

        {recentOrders.length === 0 ? (
          <div className="px-6 py-14 text-center text-sm text-[#71817A]">No orders are registered in the system yet.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-left text-sm">
              <thead><tr className="border-b border-[#DDD8CF] bg-[#F7F4EE]"><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Order</th><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Customer</th><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Date</th><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Total</th><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Payment</th><th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">Status</th></tr></thead>
              <tbody className="divide-y divide-[#DDD8CF]">
                {recentOrders.map((order) => <tr key={order.id} className="transition hover:bg-[#F7F4EE]"><td className="px-6 py-4 font-bold text-[#17211F]">#SK-2026-{order.id}</td><td className="px-6 py-4 font-semibold text-[#4E5B56]">{order.shippingAddress?.fullName || `User #${order.userId}`}</td><td className="px-6 py-4 text-xs text-[#71817A]"><span className="inline-flex items-center gap-1.5"><Calendar className="h-3.5 w-3.5 text-[#B84F49]" />{order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-IN') : '—'}</span></td><td className="px-6 py-4 font-bold text-[#17211F]">{formatCurrency(order.totalAmount)}</td><td className="px-6 py-4 text-xs font-semibold capitalize text-[#71817A]">{order.paymentMethod?.toLowerCase() || '—'}</td><td className="px-6 py-4"><StatusBadge status={order.status} /></td></tr>)}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
