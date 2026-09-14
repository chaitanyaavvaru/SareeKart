import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  BarChart3,
  Boxes,
  Calendar,
  CheckCircle2,
  Clock,
  CreditCard,
  Download,
  Eye,
  FileSpreadsheet,
  FileText,
  Filter,
  IndianRupee,
  Layers,
  Loader2,
  MapPin,
  Package,
  Percent,
  RefreshCw,
  Search,
  ShoppingBag,
  Sparkles,
  Tag,
  TrendingDown,
  TrendingUp,
  Truck,
  Users,
} from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';

const formatCurrency = (value) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);

const formatNumber = (value) =>
  new Intl.NumberFormat('en-IN').format(Number(value) || 0);

const DATE_RANGES = [
  { key: 'TODAY', label: 'Today' },
  { key: '7D', label: '7D' },
  { key: '30D', label: '30D' },
  { key: '90D', label: '90D' },
  { key: 'YTD', label: 'YTD' },
  { key: 'ALL', label: 'All' },
  { key: 'CUSTOM', label: 'Custom' },
];

function PercentageIndicator({ value }) {
  if (value === null || value === undefined) return null;
  const num = Number(value);
  const isPositive = num > 0;
  const isZero = num === 0;

  return (
    <span
      className={`inline-flex items-center gap-0.5 text-xs font-bold ${
        isPositive ? 'text-emerald-700' : isZero ? 'text-[#71817A]' : 'text-rose-600'
      }`}
    >
      {isPositive ? (
        <ArrowUpRight className="h-3.5 w-3.5" />
      ) : isZero ? null : (
        <ArrowDownRight className="h-3.5 w-3.5" />
      )}
      {isPositive ? `+${num.toFixed(1)}%` : `${num.toFixed(1)}%`}
      <span className="ml-1 text-[10px] font-normal text-[#71817A]">vs prior</span>
    </span>
  );
}

function KpiCard({ icon: Icon, label, value, detail, percentChange, tone, testId }) {
  return (
    <div
      data-testid={testId}
      className="flex flex-col justify-between border border-[#DDD8CF] bg-white p-5 shadow-[0_8px_20px_rgba(23,33,31,0.04)] transition-all hover:shadow-[0_12px_24px_rgba(23,33,31,0.08)]"
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#71817A]">{label}</p>
        <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${tone}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <div className="mt-3">
        <p className="font-serif text-3xl font-medium tracking-tight text-[#17211F]">{value}</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
          {percentChange !== undefined ? <PercentageIndicator value={percentChange} /> : null}
          {detail && <span className="text-xs text-[#71817A]">{detail}</span>}
        </div>
      </div>
    </div>
  );
}

/**
 * Pure SVG Interactive Revenue Timeline Chart
 */
function RevenueTimelineChart({ timeline = [] }) {
  const [hoveredPoint, setHoveredPoint] = useState(null);

  const data = useMemo(() => {
    if (!timeline || timeline.length === 0) return [];
    return timeline.map((item, idx) => ({
      index: idx,
      date: item.date,
      displayDate: new Date(item.date).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }),
      revenue: Number(item.revenue) || 0,
      orders: Number(item.orders) || 0,
      units: Number(item.units) || 0,
    }));
  }, [timeline]);

  if (data.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center text-sm text-[#71817A]">
        No sales data available for this timeline.
      </div>
    );
  }

  const maxRevenue = Math.max(...data.map((d) => d.revenue), 1000);
  const width = 700;
  const height = 260;
  const padding = { top: 20, right: 25, bottom: 40, left: 65 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  const getX = (idx) => padding.left + (idx / Math.max(data.length - 1, 1)) * chartWidth;
  const getY = (val) => padding.top + chartHeight - (val / maxRevenue) * chartHeight;

  // Build SVG Path
  const points = data.map((d, i) => `${getX(i)},${getY(d.revenue)}`).join(' ');
  const areaPath = `${points} L ${getX(data.length - 1)},${padding.top + chartHeight} L ${getX(0)},${padding.top + chartHeight} Z`;

  // Grid steps (4 horizontal lines)
  const yTicks = [0, maxRevenue * 0.33, maxRevenue * 0.66, maxRevenue];

  // X Axis label sample (max 7 labels)
  const step = Math.max(1, Math.floor(data.length / 6));
  const xLabels = data.filter((_, i) => i % step === 0 || i === data.length - 1);

  return (
    <div className="relative">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-auto select-none"
        style={{ overflow: 'visible' }}
      >
        <defs>
          <linearGradient id="revGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#1E6A62" stopOpacity="0.35" />
            <stop offset="100%" stopColor="#1E6A62" stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Y-Axis Gridlines & Labels */}
        {yTicks.map((tick, i) => {
          const y = getY(tick);
          return (
            <g key={i}>
              <line
                x1={padding.left}
                y1={y}
                x2={width - padding.right}
                y2={y}
                stroke="#E5E0D8"
                strokeDasharray="4,4"
                strokeWidth="1"
              />
              <text
                x={padding.left - 8}
                y={y + 4}
                textAnchor="end"
                fontSize="10"
                fill="#71817A"
                fontFamily="system-ui"
              >
                ₹{tick >= 1000 ? `${(tick / 1000).toFixed(0)}k` : tick.toFixed(0)}
              </text>
            </g>
          );
        })}

        {/* Area fill */}
        <polygon points={areaPath} fill="url(#revGradient)" />

        {/* Line */}
        <polyline
          fill="none"
          stroke="#1E6A62"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={points}
        />

        {/* Interactive Dots and Trigger Areas */}
        {data.map((d, i) => {
          const cx = getX(i);
          const cy = getY(d.revenue);
          const isHovered = hoveredPoint?.index === i;

          return (
            <g key={i} className="cursor-pointer" onMouseEnter={() => setHoveredPoint(d)}>
              {/* Invisible large hover hitbox */}
              <circle cx={cx} cy={cy} r="14" fill="transparent" />

              {/* Visible dot */}
              <circle
                cx={cx}
                cy={cy}
                r={isHovered ? '5' : '3'}
                fill={isHovered ? '#F3C56A' : '#1E6A62'}
                stroke="#FFFFFF"
                strokeWidth={isHovered ? '2' : '1.5'}
                className="transition-all"
              />
            </g>
          );
        })}

        {/* X Axis Labels */}
        {xLabels.map((lbl) => {
          const x = getX(lbl.index);
          return (
            <text
              key={lbl.index}
              x={x}
              y={height - 10}
              textAnchor="middle"
              fontSize="10"
              fill="#71817A"
              fontFamily="system-ui"
            >
              {lbl.displayDate}
            </text>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {hoveredPoint && (
        <div
          className="absolute z-10 rounded-lg border border-[#DDD8CF] bg-white p-2.5 shadow-lg text-xs pointer-events-none transition-all"
          style={{
            left: `${Math.min(
              85,
              Math.max(15, (hoveredPoint.index / Math.max(data.length - 1, 1)) * 100)
            )}%`,
            top: '10px',
            transform: 'translateX(-50%)',
          }}
        >
          <div className="font-bold text-[#17211F]">{hoveredPoint.date}</div>
          <div className="mt-1 flex items-center gap-2 text-emerald-700 font-bold">
            <span>Revenue:</span>
            <span>{formatCurrency(hoveredPoint.revenue)}</span>
          </div>
          <div className="text-[11px] text-[#71817A] mt-0.5">
            Orders: {hoveredPoint.orders} | Units: {hoveredPoint.units}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Pure SVG Payment Distribution Donut Chart
 */
function PaymentDistributionChart({ distribution = [] }) {
  const totalAmount = distribution.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
  const colors = ['#1E6A62', '#F3C56A', '#3B82F6', '#9333EA', '#E11D48'];

  if (distribution.length === 0 || totalAmount === 0) {
    return (
      <div className="flex h-48 items-center justify-center text-xs text-[#71817A]">
        No payment telemetry recorded for this timeframe.
      </div>
    );
  }

  // Calculate donut segments
  let cumulativeAngle = 0;
  const segments = [];
  for (let idx = 0; idx < distribution.length; idx++) {
    const item = distribution[idx];
    const amount = Number(item.amount) || 0;
    const fraction = totalAmount > 0 ? amount / totalAmount : 0;
    const angle = fraction * 360;
    const startAngle = cumulativeAngle;
    cumulativeAngle += angle;

    segments.push({
      ...item,
      color: colors[idx % colors.length],
      fraction,
      startAngle,
      angle,
    });
  }

  return (
    <div className="flex flex-col sm:flex-row items-center gap-6">
      {/* SVG Donut */}
      <div className="relative w-40 h-40 shrink-0">
        <svg viewBox="0 0 100 100" className="w-full h-full transform -rotate-90">
          {segments.map((seg, idx) => {
            const strokeDasharray = `${seg.fraction * 251.2} 251.2`;
            const strokeDashoffset = -segments
              .slice(0, idx)
              .reduce((acc, s) => acc + s.fraction * 251.2, 0);

            return (
              <circle
                key={idx}
                cx="50"
                cy="50"
                r="40"
                fill="transparent"
                stroke={seg.color}
                strokeWidth="16"
                strokeDasharray={strokeDasharray}
                strokeDashoffset={strokeDashoffset}
                className="transition-all duration-300"
              />
            );
          })}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">Total</span>
          <span className="text-xs font-bold text-[#17211F]">{formatCurrency(totalAmount)}</span>
        </div>
      </div>

      {/* Legend List */}
      <div className="flex-1 space-y-2.5 w-full">
        {segments.map((item, idx) => (
          <div key={idx} className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <span className="h-3 w-3 rounded-full shrink-0" style={{ backgroundColor: item.color }} />
              <span className="font-semibold text-[#17211F]">{item.method}</span>
              <span className="text-[11px] text-[#71817A]">({item.count} orders)</span>
            </div>
            <div className="text-right">
              <span className="font-bold text-[#17211F]">{formatCurrency(item.amount)}</span>
              <span className="ml-1.5 text-[11px] font-semibold text-[#71817A]">
                {item.percentage ? `${item.percentage}%` : `${((item.amount / totalAmount) * 100).toFixed(1)}%`}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function AnalyticsDashboard() {
  const [selectedRange, setSelectedRange] = useState('30D');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [exportLoading, setExportLoading] = useState(null);

  // Analytical Data States
  const [overview, setOverview] = useState(null);
  const [sales, setSales] = useState(null);
  const [inventory, setInventory] = useState(null);
  const [customers, setCustomers] = useState(null);
  const [behavior, setBehavior] = useState(null);

  // Active Sub-Tab
  const [inventoryTab, setInventoryTab] = useState('FAST'); // 'FAST' | 'SLOW' | 'STOCKOUT'

  const fetchAllAnalytics = useCallback(async (isRefresh = false) => {
    try {
      setError(null);
      if (isRefresh) setRefreshing(true);
      else setLoading(true);

      const params = { range: selectedRange };
      if (selectedRange === 'CUSTOM') {
        if (!customStartDate || !customEndDate) {
          setError('Please select both start date and end date for custom range.');
          setLoading(false);
          setRefreshing(false);
          return;
        }
        params.startDate = customStartDate;
        params.endDate = customEndDate;
      }

      const [overviewRes, salesRes, inventoryRes, customersRes, behaviorRes] = await Promise.all([
        api.get('/admin/analytics/overview', { params }),
        api.get('/admin/analytics/sales', { params }),
        api.get('/admin/analytics/inventory'),
        api.get('/admin/analytics/customers', { params }),
        api.get('/admin/customer-behavior/overview', { params }).catch(() => ({ data: { success: false } })),
      ]);

      if (overviewRes.data?.success) setOverview(overviewRes.data.data);
      if (salesRes.data?.success) setSales(salesRes.data.data);
      if (inventoryRes.data?.success) setInventory(inventoryRes.data.data);
      if (customersRes.data?.success) setCustomers(customersRes.data.data);
      if (behaviorRes.data?.success) setBehavior(behaviorRes.data.data);
    } catch (err) {
      console.error('Error loading analytics:', err);
      setError(err.response?.data?.message || 'Failed to load telemetry data. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [selectedRange, customStartDate, customEndDate]);

  useEffect(() => {
    fetchAllAnalytics();
  }, [fetchAllAnalytics]);

  // Handle Export File Download
  const handleExport = async (reportType, format) => {
    try {
      setExportLoading(`${reportType}-${format}`);
      const endpoint =
        reportType === 'sales'
          ? `/admin/analytics/export/sales`
          : `/admin/analytics/export/inventory`;

      const params = { format };
      if (reportType === 'sales') {
        params.range = selectedRange;
        if (selectedRange === 'CUSTOM') {
          params.startDate = customStartDate;
          params.endDate = customEndDate;
        }
      }

      const response = await api.get(endpoint, {
        params,
        responseType: 'blob',
      });

      const blob = new Blob([response.data]);
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      const extension = format === 'xlsx' ? 'xlsx' : 'csv';
      link.setAttribute('download', `${reportType}_report_${selectedRange.toLowerCase()}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (err) {
      console.error('Export download failed:', err);
      alert('Failed to export report. Please try again.');
    } finally {
      setExportLoading(null);
    }
  };

  const percentageChanges = overview?.percentageChanges || {};

  return (
    <div className="space-y-8 text-left font-sans text-[#17211F]">
      <SEO
        title="Sales & Business Analytics | SareeKart Admin"
        description="Comprehensive e-commerce analytics, sales telemetry, inventory velocity, customer retention cohorts, and accounting reports."
      />

      {/* Top Header & Range Selection Toolbar */}
      <div className="flex flex-col gap-4 border-b border-[#DDD8CF] pb-6 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-serif text-2xl font-semibold tracking-tight text-[#17211F]">
              Analytics & Financial Telemetry
            </h1>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-[11px] font-bold text-emerald-800">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Live Telemetry
            </span>
          </div>
          <p className="mt-1 text-xs text-[#71817A]">
            Real-time sales aggregation, SKU run rates, customer LTV cohorts, and inventory health
          </p>
        </div>

        {/* Actions Toolbar */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Export Buttons */}
          <div className="flex items-center gap-1.5">
            <button
              data-testid="export-sales-csv"
              onClick={() => handleExport('sales', 'csv')}
              disabled={!!exportLoading}
              className="inline-flex items-center gap-1.5 border border-[#DDD8CF] bg-white px-3 py-2 text-xs font-semibold text-[#17211F] shadow-xs transition hover:bg-[#F7F4EE] disabled:opacity-50"
            >
              <Download className="h-3.5 w-3.5 text-[#1E6A62]" />
              {exportLoading === 'sales-csv' ? 'Exporting...' : 'Sales CSV'}
            </button>
            <button
              data-testid="export-sales-excel"
              onClick={() => handleExport('sales', 'xlsx')}
              disabled={!!exportLoading}
              className="inline-flex items-center gap-1.5 border border-[#DDD8CF] bg-white px-3 py-2 text-xs font-semibold text-[#17211F] shadow-xs transition hover:bg-[#F7F4EE] disabled:opacity-50"
            >
              <FileSpreadsheet className="h-3.5 w-3.5 text-emerald-700" />
              {exportLoading === 'sales-xlsx' ? 'Exporting...' : 'Sales Excel'}
            </button>
            <button
              data-testid="export-inventory-csv"
              onClick={() => handleExport('inventory', 'csv')}
              disabled={!!exportLoading}
              className="inline-flex items-center gap-1.5 border border-[#DDD8CF] bg-white px-3 py-2 text-xs font-semibold text-[#17211F] shadow-xs transition hover:bg-[#F7F4EE] disabled:opacity-50"
            >
              <Download className="h-3.5 w-3.5 text-[#1E6A62]" />
              {exportLoading === 'inventory-csv' ? 'Exporting...' : 'Inventory CSV'}
            </button>
          </div>

          {/* Refresh Button */}
          <button
            onClick={() => fetchAllAnalytics(true)}
            disabled={refreshing}
            aria-label="Refresh telemetry data"
            className="flex h-9 w-9 items-center justify-center border border-[#DDD8CF] bg-white text-[#71817A] shadow-xs transition hover:text-[#17211F] disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin text-[#1E6A62]' : ''}`} />
          </button>
        </div>
      </div>

      {/* Date Range Filter Pills */}
      <div className="flex flex-wrap items-center justify-between gap-4 border border-[#DDD8CF] bg-white p-3 shadow-xs">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-[#71817A]" />
          <span className="text-xs font-bold uppercase tracking-wider text-[#71817A]">Date Range:</span>
        </div>
        <div data-testid="date-range-pills" className="flex flex-wrap items-center gap-1.5">
          {DATE_RANGES.map((range) => {
            const isActive = selectedRange === range.key;
            return (
              <button
                key={range.key}
                onClick={() => setSelectedRange(range.key)}
                className={`px-3 py-1.5 text-xs font-bold transition ${
                  isActive
                    ? 'bg-[#17211F] text-white shadow-xs'
                    : 'bg-[#F7F4EE] text-[#71817A] hover:bg-[#EAE4D9] hover:text-[#17211F]'
                }`}
              >
                {range.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Custom Date Pickers */}
      {selectedRange === 'CUSTOM' && (
        <div className="flex flex-wrap items-center gap-4 border border-[#DDD8CF] bg-[#FAF8F5] p-4">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-[#71817A]">From:</span>
            <input
              type="date"
              value={customStartDate}
              onChange={(e) => setCustomStartDate(e.target.value)}
              className="border border-[#DDD8CF] bg-white px-3 py-1.5 text-xs font-medium"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-[#71817A]">To:</span>
            <input
              type="date"
              value={customEndDate}
              onChange={(e) => setCustomEndDate(e.target.value)}
              className="border border-[#DDD8CF] bg-white px-3 py-1.5 text-xs font-medium"
            />
          </div>
          <button
            onClick={() => fetchAllAnalytics()}
            className="bg-[#1E6A62] px-4 py-1.5 text-xs font-bold text-white hover:bg-[#16524C]"
          >
            Apply Range
          </button>
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div className="flex items-start gap-3 border border-[#E8C9C5] bg-[#FFF2F0] p-4 text-[#8B3E37]">
          <AlertTriangle className="h-5 w-5 shrink-0" />
          <div>
            <p className="text-xs font-bold uppercase tracking-wider">Analytics Service Notification</p>
            <p className="mt-1 text-xs">{error}</p>
          </div>
        </div>
      )}

      {/* Loading Skeleton / State */}
      {loading ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center text-[#71817A]">
          <Loader2 className="h-8 w-8 animate-spin text-[#1E6A62]" />
          <p className="mt-4 text-xs font-bold uppercase tracking-[0.14em]">
            Compiling Analytical Telemetry...
          </p>
        </div>
      ) : (
        <>
          {/* R1: Primary KPI Summary Cards */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <KpiCard
              testId="kpi-gross-sales"
              icon={IndianRupee}
              label="Gross Sales"
              value={formatCurrency(overview?.grossSales || 0)}
              percentChange={percentageChanges.grossSales}
              detail={`Prior: ${formatCurrency(overview?.priorGrossSales || 0)}`}
              tone="bg-emerald-50 text-emerald-700"
            />
            <KpiCard
              testId="kpi-net-revenue"
              icon={TrendingUp}
              label="Net Revenue"
              value={formatCurrency(overview?.netRevenue || 0)}
              percentChange={percentageChanges.netRevenue}
              detail={`Tax: ${formatCurrency(overview?.taxAmount || 0)} | Ship: ${formatCurrency(
                overview?.shippingAmount || 0
              )}`}
              tone="bg-teal-50 text-teal-700"
            />
            <KpiCard
              testId="kpi-aov"
              icon={ShoppingBag}
              label="Average Order Value"
              value={formatCurrency(overview?.aov || 0)}
              percentChange={percentageChanges.aov}
              detail={`Prior AOV: ${formatCurrency(overview?.priorAov || 0)}`}
              tone="bg-amber-50 text-amber-700"
            />
            <KpiCard
              testId="kpi-orders"
              icon={ShoppingBag}
              label="Completed Orders"
              value={formatNumber(overview?.completedOrders || 0)}
              percentChange={percentageChanges.completedOrders}
              detail={`${overview?.totalUnitsSold || 0} total units dispatched`}
              tone="bg-blue-50 text-blue-700"
            />
          </div>

          {/* R1: Revenue Timeline & Payment Distribution */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* Daily Revenue Timeline Graph */}
            <div className="border border-[#DDD8CF] bg-white p-6 shadow-xs lg:col-span-2">
              <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-4">
                <div>
                  <h2 className="font-serif text-base font-semibold text-[#17211F]">
                    Revenue & Order Velocity Trend
                  </h2>
                  <p className="mt-0.5 text-xs text-[#71817A]">
                    Daily sales distribution and volume trajectory across active timeframe
                  </p>
                </div>
                <div className="flex items-center gap-4 text-xs font-semibold text-[#71817A]">
                  <span className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full bg-[#1E6A62]" /> Revenue (₹)
                  </span>
                </div>
              </div>

              <div className="mt-6">
                <RevenueTimelineChart timeline={sales?.timeline || []} />
              </div>
            </div>

            {/* Payment Method Distribution */}
            <div className="flex flex-col justify-between border border-[#DDD8CF] bg-white p-6 shadow-xs">
              <div>
                <h2 className="font-serif text-base font-semibold text-[#17211F]">
                  Payment Channels Split
                </h2>
                <p className="mt-0.5 text-xs text-[#71817A]">
                  Channel settlement telemetry & transaction volume
                </p>

                <div className="mt-6">
                  <PaymentDistributionChart
                    distribution={sales?.paymentDistribution || []}
                  />
                </div>
              </div>

              {/* Coupon / Campaign ROI Teaser */}
              <div className="mt-6 border-t border-[#DDD8CF] pt-4">
                <div className="flex items-center justify-between text-xs">
                  <span className="flex items-center gap-1 font-semibold text-[#71817A]">
                    <Tag className="h-3.5 w-3.5 text-[#1E6A62]" /> Discount Deductions:
                  </span>
                  <span className="font-bold text-[#17211F]">
                    {formatCurrency(sales?.discountAmount || 0)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Coupon / Promo Campaign Utilization Table */}
          {sales?.couponUtilization && sales.couponUtilization.length > 0 && (
            <div className="border border-[#DDD8CF] bg-white p-6 shadow-xs">
              <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-4">
                <div>
                  <h2 className="font-serif text-base font-semibold text-[#17211F]">
                    Promotional Campaigns & Coupon ROI
                  </h2>
                  <p className="mt-0.5 text-xs text-[#71817A]">
                    Utilization frequency, discount expenditures, and campaign revenue generation
                  </p>
                </div>
              </div>

              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[#DDD8CF] text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                      <th className="pb-3">Coupon Code</th>
                      <th className="pb-3 text-center">Redemptions</th>
                      <th className="pb-3 text-right">Discount Total</th>
                      <th className="pb-3 text-right">Revenue Generated</th>
                      <th className="pb-3 text-right">ROI Multiplier</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#DDD8CF]/50">
                    {sales.couponUtilization.map((coupon, idx) => (
                      <tr key={idx} className="hover:bg-[#F7F4EE]/60 transition">
                        <td className="py-3 font-mono font-bold text-[#1E6A62]">{coupon.code}</td>
                        <td className="py-3 text-center">{coupon.uses}</td>
                        <td className="py-3 text-right font-medium text-rose-600">
                          {formatCurrency(coupon.discountTotal)}
                        </td>
                        <td className="py-3 text-right font-semibold text-[#17211F]">
                          {formatCurrency(coupon.revenueGenerated)}
                        </td>
                        <td className="py-3 text-right font-bold text-emerald-700">
                          {coupon.roi > 0 ? `${coupon.roi.toFixed(1)}x` : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* R2: Inventory Velocity & Stock Telemetry */}
          <div className="border border-[#DDD8CF] bg-white p-6 shadow-xs">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-[#DDD8CF] pb-4">
              <div>
                <h2 className="font-serif text-base font-semibold text-[#17211F]">
                  Inventory Velocity & Supply Telemetry
                </h2>
                <p className="mt-0.5 text-xs text-[#71817A]">
                  Run-rate velocity, days of inventory remaining, stock aging, and replenishment priority
                </p>
              </div>

              {/* Sub tabs */}
              <div className="flex items-center gap-1 border border-[#DDD8CF] p-1 bg-[#F7F4EE]">
                <button
                  onClick={() => setInventoryTab('FAST')}
                  className={`px-3 py-1 text-xs font-bold transition ${
                    inventoryTab === 'FAST'
                      ? 'bg-white text-[#17211F] shadow-xs'
                      : 'text-[#71817A] hover:text-[#17211F]'
                  }`}
                >
                  Fast Moving SKUs
                </button>
                <button
                  onClick={() => setInventoryTab('SLOW')}
                  className={`px-3 py-1 text-xs font-bold transition ${
                    inventoryTab === 'SLOW'
                      ? 'bg-white text-[#17211F] shadow-xs'
                      : 'text-[#71817A] hover:text-[#17211F]'
                  }`}
                >
                  Slow Moving SKUs
                </button>
                <button
                  onClick={() => setInventoryTab('STOCKOUT')}
                  className={`px-3 py-1 text-xs font-bold transition ${
                    inventoryTab === 'STOCKOUT'
                      ? 'bg-white text-rose-700 shadow-xs'
                      : 'text-[#71817A] hover:text-[#17211F]'
                  }`}
                >
                  Stockout Alerts ({inventory?.stockoutAlerts?.length || 0})
                </button>
              </div>
            </div>

            {/* Inventory Aging Breakdown Summary */}
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
              {(inventory?.agingCategories || []).map((aging, idx) => (
                <div
                  key={idx}
                  className="border border-[#DDD8CF] bg-[#FAF8F5] p-4 flex flex-col justify-between"
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-[#17211F]">{aging.displayName}</span>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                      {aging.itemCount} SKUs
                    </span>
                  </div>
                  <div className="mt-3">
                    <span className="font-serif text-xl font-medium text-[#17211F]">
                      {formatCurrency(aging.valuation)}
                    </span>
                    <p className="mt-1 text-[11px] text-[#71817A]">
                      {formatNumber(aging.totalUnits)} total units ({aging.valuationPercentage?.toFixed(1)}% catalog value)
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* SKU Table View */}
            <div className="mt-6 overflow-x-auto">
              {inventoryTab === 'STOCKOUT' ? (
                /* Stockout Alerts Table */
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[#DDD8CF] text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                      <th className="pb-3">SKU / Product</th>
                      <th className="pb-3">Warehouse Hub</th>
                      <th className="pb-3 text-center">Available Stock</th>
                      <th className="pb-3 text-center">Daily Run Rate</th>
                      <th className="pb-3 text-center">Days Remaining</th>
                      <th className="pb-3 text-center">Priority</th>
                      <th className="pb-3 text-right">Reorder Recommendation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#DDD8CF]/50">
                    {(inventory?.stockoutAlerts || []).length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-8 text-center text-[#71817A]">
                          No critical stockouts or low-stock items detected across fulfillment centers.
                        </td>
                      </tr>
                    ) : (
                      inventory.stockoutAlerts.map((alert, idx) => (
                        <tr key={idx} className="hover:bg-[#FFF2F0]/40 transition">
                          <td className="py-3">
                            <span className="font-bold text-[#17211F] block">{alert.productName || alert.name}</span>
                            <span className="font-mono text-[11px] text-[#71817A]">{alert.sku}</span>
                          </td>
                          <td className="py-3 text-[#71817A]">{alert.warehouseName || alert.warehouseCode}</td>
                          <td className="py-3 text-center font-bold text-rose-600">
                            {alert.availableStock || alert.available} units
                          </td>
                          <td className="py-3 text-center font-medium text-[#17211F]">
                            {alert.dailyRunRate} / day
                          </td>
                          <td className="py-3 text-center">
                            <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-bold text-rose-800">
                              {alert.daysRemaining?.toFixed(0)} days
                            </span>
                          </td>
                          <td className="py-3 text-center">
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-bold uppercase ${
                                alert.priorityLevel === 'CRITICAL'
                                  ? 'bg-rose-200 text-rose-900'
                                  : alert.priorityLevel === 'HIGH'
                                  ? 'bg-amber-100 text-amber-800'
                                  : 'bg-blue-100 text-blue-800'
                              }`}
                            >
                              {alert.priorityLevel} (Score: {alert.priorityScore})
                            </span>
                          </td>
                          <td className="py-3 text-right font-bold text-[#1E6A62]">
                            +{alert.recommendedReorderQty} units PO
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              ) : (
                /* Fast / Slow Moving SKUs Table */
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[#DDD8CF] text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                      <th className="pb-3">SKU & Product Name</th>
                      <th className="pb-3">Category</th>
                      <th className="pb-3 text-center">30D Sold</th>
                      <th className="pb-3 text-center">Daily Run Rate</th>
                      <th className="pb-3 text-center">Available Stock</th>
                      <th className="pb-3 text-center">Days Remaining</th>
                      <th className="pb-3 text-right">Unit Price</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#DDD8CF]/50">
                    {(inventoryTab === 'FAST'
                      ? inventory?.fastMovingSkus || []
                      : inventory?.slowMovingSkus || []
                    ).map((skuItem, idx) => (
                      <tr key={idx} className="hover:bg-[#F7F4EE]/60 transition">
                        <td className="py-3">
                          <span className="font-bold text-[#17211F] block">{skuItem.productName || skuItem.name}</span>
                          <span className="font-mono text-[11px] text-[#71817A]">{skuItem.sku}</span>
                        </td>
                        <td className="py-3 text-[#71817A]">{skuItem.category}</td>
                        <td className="py-3 text-center font-bold text-[#17211F]">
                          {skuItem.unitsSold30d || skuItem.unitsSold}
                        </td>
                        <td className="py-3 text-center font-medium text-[#71817A]">
                          {skuItem.dailyRunRate || skuItem.runRate}
                        </td>
                        <td className="py-3 text-center font-semibold text-[#17211F]">
                          {skuItem.availableStock}
                        </td>
                        <td className="py-3 text-center">
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                              skuItem.daysOfInventoryRemaining < 30
                                ? 'bg-rose-100 text-rose-800'
                                : skuItem.daysOfInventoryRemaining < 90
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-emerald-100 text-emerald-800'
                            }`}
                          >
                            {skuItem.daysOfInventoryRemaining} days
                          </span>
                        </td>
                        <td className="py-3 text-right font-medium text-[#17211F]">
                          {formatCurrency(skuItem.unitPrice)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          {/* R3: Customer Cohorts & Retention Analytics */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* LTV Tiers & Retention Cards */}
            <div className="border border-[#DDD8CF] bg-white p-6 shadow-xs lg:col-span-2">
              <div className="border-b border-[#DDD8CF] pb-4">
                <h2 className="font-serif text-base font-semibold text-[#17211F]">
                  Customer Lifetime Value (LTV) & Retention
                </h2>
                <p className="mt-0.5 text-xs text-[#71817A]">
                  Buyer spending tiers, repurchase rate, and new vs returning customer contribution
                </p>
              </div>

              {/* LTV Tier Cards */}
              <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
                {(customers?.ltvTiers || []).map((tier, idx) => (
                  <div
                    key={idx}
                    className="border border-[#DDD8CF] bg-[#FAF8F5] p-4 flex flex-col justify-between"
                  >
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                        {tier.displayName || tier.tier}
                      </span>
                      <p className="mt-2 font-serif text-xl font-medium text-[#17211F]">
                        {formatCurrency(tier.totalRevenue || tier.totalSpend)}
                      </p>
                      <p className="mt-1 text-xs text-[#71817A]">
                        {tier.customerCount} Customers ({tier.percentageOfRevenue?.toFixed(1)}% sales)
                      </p>
                    </div>
                    <div className="mt-3 border-t border-[#DDD8CF] pt-2 text-[11px] text-[#71817A]">
                      Avg LTV: {formatCurrency(tier.averageLtv || tier.averageSpend)}
                    </div>
                  </div>
                ))}
              </div>

              {/* Retention & Funnel Metrics */}
              <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="border border-[#DDD8CF] p-4">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-[#17211F]">Repeat Purchase Rate</span>
                    <span className="font-bold text-emerald-700">
                      {customers?.newVsReturning?.repeatPurchaseRate?.toFixed(1)}%
                    </span>
                  </div>
                  <div className="mt-3 h-2 w-full rounded-full bg-[#EAE4D9]">
                    <div
                      className="h-2 rounded-full bg-emerald-600 transition-all"
                      style={{
                        width: `${Math.min(100, customers?.newVsReturning?.repeatPurchaseRate || 0)}%`,
                      }}
                    />
                  </div>
                  <div className="mt-3 flex items-center justify-between text-[11px] text-[#71817A]">
                    <span>Returning Rev: {formatCurrency(customers?.newVsReturning?.returningRevenue || 0)}</span>
                    <span>New Rev: {formatCurrency(customers?.newVsReturning?.newRevenue || 0)}</span>
                  </div>
                </div>

                <div className="border border-[#DDD8CF] p-4">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-[#17211F]">Checkout Abandonment</span>
                    <span className="font-bold text-amber-700">
                      {customers?.conversionFunnel?.checkoutAbandonmentRate?.toFixed(1)}%
                    </span>
                  </div>
                  <div className="mt-3 flex items-center justify-between text-[11px] text-[#71817A]">
                    <span>Carts: {customers?.conversionFunnel?.cartsCreated || 0}</span>
                    <span>Initiated: {customers?.conversionFunnel?.checkoutInitiated || 0}</span>
                    <span className="font-bold text-[#17211F]">
                      Orders: {customers?.conversionFunnel?.ordersCompleted || 0}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Regional Breakdown (Top States & Cities) */}
            <div className="border border-[#DDD8CF] bg-white p-6 shadow-xs">
              <div className="border-b border-[#DDD8CF] pb-4">
                <h2 className="font-serif text-base font-semibold text-[#17211F]">
                  Regional Demand Breakdown
                </h2>
                <p className="mt-0.5 text-xs text-[#71817A]">
                  Top geographic buying hubs by order volume
                </p>
              </div>

              <div className="mt-6 space-y-3">
                {(customers?.topStates || customers?.regionalBreakdown?.topStates || []).map(
                  (st, idx) => (
                    <div key={idx} className="border-b border-[#DDD8CF]/40 pb-2.5 last:border-0">
                      <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center gap-1.5 font-bold text-[#17211F]">
                          <MapPin className="h-3.5 w-3.5 text-[#1E6A62]" />
                          <span>{st.region || st.name}</span>
                        </div>
                        <span className="font-bold text-[#17211F]">{formatCurrency(st.revenue)}</span>
                      </div>
                      <div className="mt-1 flex items-center justify-between text-[11px] text-[#71817A]">
                        <span>{st.orderCount} completed orders</span>
                        <span>{st.percentage?.toFixed(1)}% share</span>
                      </div>
                    </div>
                  )
                )}
              </div>
            </div>
          </div>

          {/* Phase 6: Customer Behavioral Telemetry & Conversion Funnel */}
          <div data-testid="behavioral-telemetry-section" className="border border-[#DDD8CF] bg-white p-6 shadow-xs">
            <div className="flex flex-col gap-2 border-b border-[#DDD8CF] pb-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <Activity className="h-5 w-5 text-[#1E6A62]" />
                  <h2 className="font-serif text-base font-semibold text-[#17211F]">
                    Customer Behavioral Telemetry & Event Funnel
                  </h2>
                  <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-black text-emerald-800 border border-emerald-200">
                    Phase 6 Live Stream
                  </span>
                </div>
                <p className="mt-0.5 text-xs text-[#71817A]">
                  Micro-interactions, true multi-stage conversion funnels, top explored sarees, and search queries
                </p>
              </div>

              {/* Behavior Quick Stats */}
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span className="rounded-md bg-[#FAF8F5] border border-[#DDD8CF] px-2.5 py-1 font-bold text-[#17211F]">
                  Events: {formatNumber(behavior?.totalEvents || 0)}
                </span>
                <span className="rounded-md bg-[#FAF8F5] border border-[#DDD8CF] px-2.5 py-1 font-bold text-[#17211F]">
                  Sessions: {formatNumber(behavior?.activeSessions || 0)}
                </span>
              </div>
            </div>

            {/* 4-Stage Conversion Funnel Visualizer */}
            <div className="mt-6">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[#71817A] mb-3">
                4-Stage Behavior-Driven Conversion Funnel
              </h3>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {(behavior?.funnel || [
                  { stage: 'PRODUCT_VIEW', label: 'Product View', totalEvents: 0, uniqueSessions: 0, conversionRateFromPrevious: 100, overallConversionRate: 100 },
                  { stage: 'ADD_TO_CART', label: 'Add to Cart', totalEvents: 0, uniqueSessions: 0, conversionRateFromPrevious: 0, overallConversionRate: 0 },
                  { stage: 'CHECKOUT_INITIATED', label: 'Checkout Started', totalEvents: 0, uniqueSessions: 0, conversionRateFromPrevious: 0, overallConversionRate: 0 },
                  { stage: 'ORDER_COMPLETED', label: 'Order Completed', totalEvents: 0, uniqueSessions: 0, conversionRateFromPrevious: 0, overallConversionRate: 0 },
                ]).map((stage, idx) => {
                  const maxSessions = Math.max(1, behavior?.funnel?.[0]?.uniqueSessions || 1);
                  const barPercent = Math.min(100, Math.max(4, Math.round(((stage.uniqueSessions || stage.totalEvents || 0) / maxSessions) * 100)));
                  return (
                    <div key={stage.stage || idx} className="border border-[#DDD8CF] bg-[#FAF8F5] p-4 flex flex-col justify-between">
                      <div>
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-black uppercase tracking-wider text-[#71817A]">
                            Stage {idx + 1}: {stage.label || stage.stage}
                          </span>
                          <span className="text-[11px] font-bold text-emerald-800">
                            {idx === 0 ? 'Entry' : `${stage.conversionRateFromPrevious?.toFixed(1) || 0}% conv`}
                          </span>
                        </div>
                        <p className="mt-2 font-serif text-2xl font-medium text-[#17211F]">
                          {formatNumber(stage.uniqueSessions || 0)}
                          <span className="ml-1 text-xs font-sans font-normal text-[#71817A]">sessions</span>
                        </p>
                        <p className="text-[11px] text-[#71817A]">
                          {formatNumber(stage.totalEvents || 0)} total occurrences
                        </p>
                      </div>

                      <div className="mt-4">
                        <div className="h-2 w-full rounded-full bg-[#EAE4D9]">
                          <div
                            className="h-2 rounded-full bg-[#1E6A62] transition-all"
                            style={{ width: `${barPercent}%` }}
                          />
                        </div>
                        <div className="mt-1 flex justify-between text-[10px] text-[#71817A]">
                          <span>Funnel share</span>
                          <span className="font-bold text-[#17211F]">{stage.overallConversionRate?.toFixed(1) || 0}% overall</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Trending Products & Search Telemetry */}
            <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
              {/* Top Explored Sarees */}
              <div className="border border-[#DDD8CF] p-4">
                <div className="flex items-center gap-2 border-b border-[#DDD8CF] pb-2">
                  <Eye className="h-4 w-4 text-[#1E6A62]" />
                  <h4 className="text-xs font-bold uppercase tracking-wider text-[#17211F]">
                    Top Explored Sarees (Behavioral Interest)
                  </h4>
                </div>
                <div className="mt-3 space-y-2">
                  {(behavior?.topProducts || []).length === 0 ? (
                    <p className="py-4 text-center text-xs text-[#71817A]">No product view events recorded yet.</p>
                  ) : (
                    (behavior?.topProducts || []).slice(0, 5).map((prod, idx) => (
                      <div key={idx} className="flex items-center justify-between border-b border-[#DDD8CF]/40 pb-2 text-xs last:border-0">
                        <div>
                          <p className="font-bold text-[#17211F]">{prod.productName || `Product #${prod.productId}`}</p>
                          <p className="text-[11px] text-[#71817A]">
                            {prod.category || 'Handloom'} {prod.fabric ? `• ${prod.fabric}` : ''}
                          </p>
                        </div>
                        <div className="text-right">
                          <span className="font-bold text-[#1E6A62]">{formatNumber(prod.viewCount)} views</span>
                          <p className="text-[10px] text-[#71817A]">{formatNumber(prod.uniqueViewers)} unique viewers</p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Trending Customer Searches */}
              <div className="border border-[#DDD8CF] p-4">
                <div className="flex items-center gap-2 border-b border-[#DDD8CF] pb-2">
                  <Search className="h-4 w-4 text-[#1E6A62]" />
                  <h4 className="text-xs font-bold uppercase tracking-wider text-[#17211F]">
                    Trending Search Queries
                  </h4>
                </div>
                <div className="mt-3 space-y-2">
                  {(behavior?.topSearches || []).length === 0 ? (
                    <p className="py-4 text-center text-xs text-[#71817A]">No search queries logged yet.</p>
                  ) : (
                    (behavior?.topSearches || []).slice(0, 5).map((item, idx) => (
                      <div key={idx} className="flex items-center justify-between border-b border-[#DDD8CF]/40 pb-2 text-xs last:border-0">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-[#17211F]">"{item.query}"</span>
                          {item.zeroResults && (
                            <span className="rounded bg-rose-100 px-1.5 py-0.5 text-[9px] font-bold text-rose-800">
                              0 Results
                            </span>
                          )}
                        </div>
                        <span className="font-bold text-[#71817A]">{formatNumber(item.queryCount)} searches</span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
