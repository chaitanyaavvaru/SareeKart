import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  ShoppingBag, 
  ClipboardList, 
  Users, 
  AlertTriangle,
  ArrowRight,
  Loader2,
  Calendar,
  IndianRupee
} from 'lucide-react';
import api from '../../api/axiosConfig';

export default function AdminStats() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await api.get('/admin/dashboard');
        if (response.data?.success && response.data?.data) {
          setStats(response.data.data);
        } else {
          setError('Failed to fetch dashboard stats.');
        }
      } catch (err) {
        console.error('Error fetching dashboard stats:', err);
        setError(err.response?.data?.message || 'Error communicating with server.');
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val || 0);
  };

  const getStatusBadge = (status) => {
    const st = status?.toUpperCase();
    switch (st) {
      case 'PENDING':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-100 uppercase">Pending</span>;
      case 'PROCESSING':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-orange-50 text-orange-700 border border-orange-100 uppercase">Processing</span>;
      case 'SHIPPED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-100 uppercase">Shipped</span>;
      case 'DELIVERED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-green-50 text-green-700 border border-green-100 uppercase">Delivered</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-50 text-red-700 border border-red-100 uppercase">Cancelled</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-gray-50 text-gray-700 border border-gray-100">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center space-y-4">
        <Loader2 className="w-10 h-10 text-maroon animate-spin" />
        <p className="text-sm font-semibold text-text-secondary">Generating dashboard telemetry...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg font-medium">
        <p className="font-bold">Error loading dashboard stats</p>
        <p className="text-xs mt-1">{error}</p>
        <button 
          onClick={() => window.location.reload()}
          className="mt-3 px-4 py-1.5 bg-maroon text-white font-bold rounded-lg text-xs"
        >
          Retry
        </button>
      </div>
    );
  }

  // Define simple chart data based on loaded statistics or static targets
  const revenueGoal = 1000000;
  const revenuePercent = Math.min(100, Math.round(((stats?.totalRevenue || 0) / revenueGoal) * 100));

  return (
    <div className="space-y-8 animate-fade-in">
      
      {/* Editorial Header */}
      <div>
        <h1 className="text-2xl font-extrabold font-serif text-maroon">Dashboard Overview</h1>
        <p className="text-sm text-text-secondary mt-1">Real-time statistics of product stock and client orders</p>
      </div>

      {/* Low Stock Warning Alert Banner */}
      {stats?.lowStockProductsCount > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3 shadow-xs">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-grow">
            <h4 className="text-sm font-bold text-amber-800">Inventory Alert: Low Stock Warning</h4>
            <p className="text-xs text-amber-700 mt-1">
              There are currently <span className="font-bold">{stats.lowStockProductsCount} products</span> running below the critical stock quantity threshold. Restock recommended.
            </p>
          </div>
          <Link 
            to="/admin/products"
            className="text-xs font-bold text-amber-800 hover:underline shrink-0 flex items-center gap-1 cursor-pointer"
          >
            Review Stock <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      )}

      {/* Grid of Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        
        {/* Total Revenue */}
        <div className="bg-white border border-[#F4F4F4] rounded-2xl p-6 shadow-xs hover:shadow-md transition-shadow flex items-center gap-4">
          <div className="w-12 h-12 bg-green-50 rounded-xl flex items-center justify-center text-green-600 shrink-0 border border-green-100">
            <IndianRupee className="w-6 h-6" />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] uppercase font-bold text-text-muted tracking-wider">Total Revenue</p>
            <h3 className="text-xl font-bold text-text-primary mt-1 truncate">{formatCurrency(stats?.totalRevenue)}<span className='text-xs text-green-600 font-medium ml-1'>↑ 12%</span></h3>
            <p className="text-[10px] text-green-600 mt-0.5 font-semibold">Processed bookings</p>
          </div>
        </div>

        {/* Total Orders */}
        <div className="bg-white border border-[#F4F4F4] rounded-2xl p-6 shadow-xs hover:shadow-md transition-shadow flex items-center gap-4">
          <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center text-blue-600 shrink-0 border border-blue-100">
            <ClipboardList className="w-6 h-6" />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] uppercase font-bold text-text-muted tracking-wider">Total Orders</p>
            <h3 className="text-xl font-bold text-text-primary mt-1">{stats?.totalOrders}<span className='text-xs text-green-600 font-medium ml-1'>↑ 8%</span></h3>
            <p className="text-[10px] text-text-secondary mt-0.5">Purchases registered</p>
          </div>
        </div>

        {/* Total Products */}
        <div className="bg-white border border-[#F4F4F4] rounded-2xl p-6 shadow-xs hover:shadow-md transition-shadow flex items-center gap-4">
          <div className="w-12 h-12 bg-purple-50 rounded-xl flex items-center justify-center text-purple-600 shrink-0 border border-purple-100">
            <ShoppingBag className="w-6 h-6" />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] uppercase font-bold text-text-muted tracking-wider">Active Products</p>
            <h3 className="text-xl font-bold text-text-primary mt-1">{stats?.totalProducts}<span className='text-xs text-green-600 font-medium ml-1'>↑ 5%</span></h3>
            <p className="text-[10px] text-text-secondary mt-0.5">In catalogs</p>
          </div>
        </div>

        {/* Total Customers */}
        <div className="bg-white border border-[#F4F4F4] rounded-2xl p-6 shadow-xs hover:shadow-md transition-shadow flex items-center gap-4">
          <div className="w-12 h-12 bg-amber-50 rounded-xl flex items-center justify-center text-amber-600 shrink-0 border border-amber-100">
            <Users className="w-6 h-6" />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] uppercase font-bold text-text-muted tracking-wider">Total Customers</p>
            <h3 className="text-xl font-bold text-text-primary mt-1">{stats?.totalCustomers}<span className='text-xs text-green-600 font-medium ml-1'>↑ 3%</span></h3>
            <p className="text-[10px] text-text-secondary mt-0.5">Registered accounts</p>
          </div>
        </div>

      </div>

      {/* Target Progress & Quick Visual Card */}
      <div className="bg-white border border-border rounded-2xl p-6 shadow-xs">
        <div className="flex justify-between items-center mb-4">
          <div>
            <h3 className="font-bold text-maroon text-sm uppercase tracking-wide">Revenue Goal Progress</h3>
            <p className="text-[11px] text-text-secondary mt-0.5">Current vs Target of {formatCurrency(revenueGoal)}</p>
          </div>
          <span className="text-xs font-bold text-maroon bg-cream border border-border-dark px-2.5 py-1 rounded-full">
            {revenuePercent}% Achieved
          </span>
        </div>
        <div className="w-full bg-[#f0ebe2] rounded-full h-3 overflow-hidden">
          <div 
            className="bg-gradient-to-r from-gold-dark to-gold h-full rounded-full transition-all duration-500" 
            style={{ width: `${revenuePercent}%` }}
          />
        </div>
      </div>

      {/* Recent Orders Table */}
      <div className="bg-white border border-border rounded-2xl shadow-xs overflow-hidden">
        <div className="px-6 py-5 border-b border-border flex items-center justify-between">
          <div>
            <h3 className="font-bold text-maroon font-serif text-base">Recent Orders</h3>
            <p className="text-xs text-text-secondary mt-0.5">Latest purchases made by clients</p>
          </div>
          <Link 
            to="/admin/orders"
            className="text-xs font-bold text-maroon hover:underline flex items-center gap-1 cursor-pointer"
          >
            Manage all orders <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
        
        {stats?.recentOrders?.length === 0 ? (
          <div className="p-8 text-center text-text-muted text-sm">
            No orders registered in the system.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-[#FAF8F5] border-b border-border">
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Order ID</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Client Name</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Date</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Total Price</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Payment Mode</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {stats?.recentOrders?.map((order) => (
                  <tr key={order.id} className="hover:bg-[#FAF8F5] transition-colors">
                    <td className="px-6 py-4 font-bold text-maroon">#SK-2026-{order.id}</td>
                    <td className="px-6 py-4 font-semibold text-text-primary">
                      {order.shippingAddress?.fullName || `User #${order.userId}`}
                    </td>
                    <td className="px-6 py-4 text-text-secondary text-xs flex items-center gap-1.5 mt-0.5">
                      <Calendar className="w-3.5 h-3.5 text-gold-dark" />
                      {new Date(order.createdAt).toLocaleDateString('en-IN')}
                    </td>
                    <td className="px-6 py-4 font-bold text-text-primary">{formatCurrency(order.totalAmount)}</td>
                    <td className="px-6 py-4 text-text-secondary text-xs capitalize">{order.paymentMethod?.toLowerCase()}</td>
                    <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
}
