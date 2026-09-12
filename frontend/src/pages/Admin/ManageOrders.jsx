import { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { 
  ClipboardList, 
  Calendar, 
  MapPin, 
  CreditCard, 
  Eye, 
  Loader2, 
  X, 
  ChevronDown,
  Truck,
  Save,
} from 'lucide-react';
import { fetchAdminOrders, updateOrderStatus } from '../../redux/slices/orderSlice';
import api from '../../api/axiosConfig';

export default function ManageOrders() {
  const dispatch = useDispatch();
  
  const { adminOrders, loading, error } = useSelector(state => state.orders);
  
  // Filter States
  const [statusFilter, setStatusFilter] = useState('ALL');
  
  // Order Detail Modal State
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  // Tracking Form State
  const [editCourier, setEditCourier] = useState('BlueDart Express');
  const [editAwb, setEditAwb] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editEstDelivery, setEditEstDelivery] = useState('');
  const [savingTracking, setSavingTracking] = useState(false);

  useEffect(() => {
    dispatch(fetchAdminOrders());
  }, [dispatch]);

  const openOrderModal = (order) => {
    setSelectedOrder(order);
    setEditCourier(order.courierPartner || 'BlueDart Express');
    setEditAwb(order.trackingNumber || `SK-BD-${order.id}829`);
    setEditLocation(order.currentLocation || 'Kanchipuram Artisan Guild - Atelier Central Hub');
    setEditEstDelivery(order.estimatedDeliveryDate || '3-4 Working Days');
  };

  const handleSaveTracking = async () => {
    if (!selectedOrder) return;
    try {
      setSavingTracking(true);
      const query = new URLSearchParams();
      if (editAwb) query.set('trackingNumber', editAwb);
      if (editCourier) query.set('courierPartner', editCourier);
      if (editLocation) query.set('currentLocation', editLocation);
      if (editEstDelivery) query.set('estimatedDeliveryDate', editEstDelivery);

      const res = await api.put(`/admin/orders/${selectedOrder.id}/tracking?${query.toString()}`);
      if (res.data?.success) {
        setSelectedOrder(res.data.data);
        dispatch(fetchAdminOrders());
        alert('Tracking & logistics details updated successfully!');
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update tracking');
    } finally {
      setSavingTracking(false);
    }
  };

  const handleStatusChange = async (orderId, newStatus) => {
    try {
      setUpdatingId(orderId);
      await dispatch(updateOrderStatus({ orderId, status: newStatus })).unwrap();
      // Auto-update modal if open
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder(prev => ({ ...prev, status: newStatus }));
      }
    } catch (err) {
      alert(err || 'Failed to update order status. Please try again.');
    } finally {
      setUpdatingId(null);
    }
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  };

  const getStatusColor = (status) => {
    const st = status?.toUpperCase();
    switch (st) {
      case 'PENDING':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'PROCESSING':
        return 'bg-orange-50 text-orange-700 border-orange-200';
      case 'SHIPPED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'DELIVERED':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'CANCELLED':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  const filteredOrders = statusFilter === 'ALL'
    ? adminOrders
    : adminOrders.filter(o => o.status?.toUpperCase() === statusFilter);

  return (
    <div className="space-y-6 animate-fade-in text-sm text-text-primary">
      
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold font-serif text-[#111827]">Manage Orders</h1>
        <p className="text-xs text-text-secondary mt-0.5">List client orders, change shipping status and verify payments</p>
      </div>

      {/* Filters bar */}
      <div className="bg-white border border-border rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-xs">
        <div className="flex gap-2 flex-wrap">
          {['ALL', 'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-4 py-1.5 rounded-xl text-xs font-bold transition-all border cursor-pointer ${
                statusFilter === st 
                  ? 'bg-[#111827] text-white border-[#111827]' 
                  : 'bg-white text-text-secondary border-border hover:bg-[#EEF3F6]/10'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
        <div className="text-xs font-semibold text-text-secondary">
          Showing {filteredOrders.length} of {adminOrders.length} bookings
        </div>
      </div>

      {/* Orders Table */}
      {loading && adminOrders.length === 0 ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-4">
          <Loader2 className="w-8 h-8 text-[#111827] animate-spin" />
          <p className="text-xs text-text-secondary">Loading system bookings...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg font-medium">
          {error}
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl shadow-xs">
          <ClipboardList className="w-10 h-10 text-text-muted mx-auto mb-3" />
          <h3 className="font-bold text-[#111827]">No Orders Registered</h3>
          <p className="text-xs text-text-secondary mt-1">There are currently no purchases matching this selection status.</p>
        </div>
      ) : (
        <div className="bg-white border border-border rounded-xl shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-[#F5F7FA] border-b border-border text-xs">
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Order ID</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Customer</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Date</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Price Total</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Payment</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Status</th>
                  <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-text-muted tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-xs">
                {filteredOrders.map((o) => (
                  <tr key={o.id} className="hover:bg-[#F5F7FA] transition-colors">
                    <td className="px-6 py-4 font-bold text-[#111827]">#SK-2026-{o.id}</td>
                    <td className="px-6 py-4 font-semibold text-text-primary">
                      {o.shippingAddress?.fullName || `User #${o.userId}`}
                    </td>
                    <td className="px-6 py-4 text-text-secondary text-xs flex items-center gap-1.5 mt-0.5">
                      <Calendar className="w-3.5 h-3.5 text-[#B5463D]" />
                      {formatDate(o.createdAt)}
                    </td>
                    <td className="px-6 py-4 font-bold text-text-primary">{formatCurrency(o.totalAmount)}</td>
                    <td className="px-6 py-4">
                      <p className="font-semibold capitalize">{o.paymentMethod?.toLowerCase()}</p>
                      <p className="text-[9px] text-text-muted mt-0.5 uppercase tracking-wider">{o.paymentStatus}</p>
                    </td>
                    <td className="px-6 py-4">
                      {updatingId === o.id ? (
                        <div className="flex items-center gap-1.5 text-text-muted">
                          <Loader2 className="w-3.5 h-3.5 animate-spin" /> Changing...
                        </div>
                      ) : (
                        <div className="relative inline-block">
                          <select
                            value={o.status || 'PENDING'}
                            onChange={(e) => handleStatusChange(o.id, e.target.value)}
                            className={`pl-3 pr-8 py-1 rounded-xl text-[10px] font-bold border border-[#DDE4EA] outline-none bg-white cursor-pointer select-none appearance-none shadow-xs ${getStatusColor(o.status)}`}
                          >
                            <option value="PENDING">PENDING</option>
                            <option value="PROCESSING">PROCESSING</option>
                            <option value="SHIPPED">SHIPPED</option>
                            <option value="DELIVERED">DELIVERED</option>
                            <option value="CANCELLED">CANCELLED</option>
                          </select>
                          <span className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-current">
                            <ChevronDown className="w-3 h-3" />
                          </span>
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => openOrderModal(o)}
                        className="inline-flex items-center gap-1 px-3 py-1 bg-white border border-border hover:bg-[#EEF3F6]/10 rounded-xl text-xs font-semibold cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5" /> Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── DETAILED VIEWS MODAL ── */}
      {selectedOrder && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-[90vh] flex flex-col">
            
            {/* Modal Header */}
            <div className="bg-[#111827] text-white px-6 py-4 flex items-center justify-between shrink-0 rounded-t-2xl">
              <div>
                <h3 className="font-serif font-extrabold text-base tracking-wide text-[#E85D4F]">
                  Order Details
                </h3>
                <p className="text-[10px] text-white/60 tracking-wider font-semibold mt-0.5">#SK-2026-{selectedOrder.id}</p>
              </div>
              <button 
                onClick={() => setSelectedOrder(null)}
                className="text-white/80 hover:text-white rounded-full p-1 hover:bg-white/10 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="flex-grow overflow-y-auto p-6 space-y-6">
              
              {/* Order Status Controller */}
              <div className="bg-[#F5F7FA] border border-border p-4 rounded-xl flex items-center justify-between flex-wrap gap-4">
                <div>
                  <p className="text-[10px] uppercase font-bold text-text-muted">Current status</p>
                  <p className="font-bold text-[#111827] text-sm mt-0.5 uppercase">{selectedOrder.status}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-text-secondary font-semibold">Change status:</span>
                  <div className="relative">
                    <select
                      value={selectedOrder.status || 'PENDING'}
                      onChange={(e) => handleStatusChange(selectedOrder.id, e.target.value)}
                      className={`pl-3 pr-8 py-1 rounded-xl text-xs font-bold border border-[#DDE4EA] outline-none bg-white cursor-pointer appearance-none shadow-xs ${getStatusColor(selectedOrder.status)}`}
                    >
                      <option value="PENDING">PENDING</option>
                      <option value="PROCESSING">PROCESSING</option>
                      <option value="SHIPPED">SHIPPED</option>
                      <option value="DELIVERED">DELIVERED</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                    <span className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-current">
                      <ChevronDown className="w-3 h-3" />
                    </span>
                  </div>
                </div>
              </div>

              {/* Logistics & Courier Tracking Manager */}
              <div className="rounded-xl border border-[#DDD8CF] bg-[#FAF8F5] p-4 text-xs space-y-3">
                <div className="flex items-center justify-between border-b border-[#EAE6DF] pb-2">
                  <div className="flex items-center gap-2 font-black text-[#17211F]">
                    <Truck className="h-4 w-4 text-[#1E6A62]" />
                    <span className="uppercase tracking-wider">Logistics & Courier Tracking</span>
                  </div>
                  <button
                    type="button"
                    onClick={handleSaveTracking}
                    disabled={savingTracking}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-[#1E6A62] px-3 py-1 text-white font-bold hover:bg-[#154e48] transition cursor-pointer"
                  >
                    <Save className="h-3.5 w-3.5" />
                    <span>{savingTracking ? 'Saving...' : 'Update Tracking'}</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <label className="grid gap-1 font-bold text-[#71817A]">
                    Courier Partner:
                    <select
                      value={editCourier}
                      onChange={(e) => setEditCourier(e.target.value)}
                      className="h-9 rounded-lg border border-[#DDD8CF] bg-white px-2.5 font-bold text-[#17211F] outline-none"
                    >
                      <option value="BlueDart Express">BlueDart Express</option>
                      <option value="Delhivery Express">Delhivery Express</option>
                      <option value="DTDC Express">DTDC Express</option>
                      <option value="India Post Speed Post">India Post Speed Post</option>
                      <option value="DHL Express Global">DHL Express Global</option>
                    </select>
                  </label>

                  <label className="grid gap-1 font-bold text-[#71817A]">
                    AWB / Tracking Number:
                    <input
                      type="text"
                      value={editAwb}
                      onChange={(e) => setEditAwb(e.target.value)}
                      placeholder="e.g. SK-BD-8829104"
                      className="h-9 rounded-lg border border-[#DDD8CF] bg-white px-2.5 font-mono font-bold text-[#17211F] outline-none"
                    />
                  </label>

                  <label className="grid gap-1 font-bold text-[#71817A]">
                    Current Checkpoint / Hub:
                    <input
                      type="text"
                      value={editLocation}
                      onChange={(e) => setEditLocation(e.target.value)}
                      placeholder="e.g. Regional Hub - In Transit"
                      className="h-9 rounded-lg border border-[#DDD8CF] bg-white px-2.5 font-bold text-[#17211F] outline-none"
                    />
                  </label>

                  <label className="grid gap-1 font-bold text-[#71817A]">
                    Estimated Delivery:
                    <input
                      type="text"
                      value={editEstDelivery}
                      onChange={(e) => setEditEstDelivery(e.target.value)}
                      placeholder="e.g. 3-4 Working Days"
                      className="h-9 rounded-lg border border-[#DDD8CF] bg-white px-2.5 font-bold text-[#17211F] outline-none"
                    />
                  </label>
                </div>
              </div>

              {/* Customer & Payment details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Shipping Location */}
                <div className="space-y-3">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-[#B5463D] border-b border-border pb-1 flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5" /> Shipping Address
                  </h4>
                  {selectedOrder.shippingAddress ? (
                    <div className="text-xs space-y-1">
                      <p className="font-bold text-text-primary">{selectedOrder.shippingAddress.fullName}</p>
                      <p className="text-text-secondary">{selectedOrder.shippingAddress.streetAddress}</p>
                      <p className="text-text-secondary">
                        {selectedOrder.shippingAddress.city}, {selectedOrder.shippingAddress.state} - {selectedOrder.shippingAddress.pincode}
                      </p>
                      <p className="text-text-muted pt-1">Phone: {selectedOrder.shippingAddress.phone}</p>
                    </div>
                  ) : (
                    <p className="text-xs text-text-muted italic">Address not available</p>
                  )}
                </div>

                {/* Transaction details */}
                <div className="space-y-3">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-[#B5463D] border-b border-border pb-1 flex items-center gap-1.5">
                    <CreditCard className="w-3.5 h-3.5" /> Payment & Transaction
                  </h4>
                  <div className="text-xs space-y-1.5">
                    <p className="text-text-secondary">
                      <span className="font-bold text-text-primary">Method:</span> <span className="capitalize">{selectedOrder.paymentMethod?.toLowerCase()}</span>
                    </p>
                    <p className="text-text-secondary">
                      <span className="font-bold text-text-primary">Status:</span> <span className="font-semibold text-[#111827]">{selectedOrder.paymentStatus}</span>
                    </p>
                    {selectedOrder.razorpayOrderId && (
                      <p className="text-text-secondary font-mono text-[10px]">
                        <span className="font-bold text-text-primary font-sans">Razorpay Order:</span> {selectedOrder.razorpayOrderId}
                      </p>
                    )}
                    {selectedOrder.razorpayPaymentId && (
                      <p className="text-text-secondary font-mono text-[10px]">
                        <span className="font-bold text-text-primary font-sans">Payment ID:</span> {selectedOrder.razorpayPaymentId}
                      </p>
                    )}
                  </div>
                </div>

              </div>

              {/* Items Table */}
              <div className="space-y-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-[#B5463D] border-b border-border pb-1">
                  Ordered Items
                </h4>
                <div className="border border-border rounded-lg overflow-hidden">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-[#F5F7FA] border-b border-border font-bold">
                        <th className="px-4 py-2.5">Saree Description</th>
                        <th className="px-4 py-2.5">Price</th>
                        <th className="px-4 py-2.5">Qty</th>
                        <th className="px-4 py-2.5 text-right">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {selectedOrder.items?.map((item) => (
                        <tr key={item.id} className="hover:bg-[#F5F7FA] transition-colors">
                          <td className="px-4 py-3 font-semibold text-text-primary">{item.productName}</td>
                          <td className="px-4 py-3">{formatCurrency(item.price)}</td>
                          <td className="px-4 py-3 font-bold text-text-secondary">{item.quantity}</td>
                          <td className="px-4 py-3 text-right font-bold text-[#111827]">{formatCurrency(item.totalPrice)}</td>
                        </tr>
                      ))}
                      <tr className="bg-[#F5F7FA] font-bold text-sm">
                        <td colSpan="3" className="px-4 py-3 text-right font-bold text-text-secondary">Grand Total</td>
                        <td className="px-4 py-3 text-right text-[#111827] font-extrabold">{formatCurrency(selectedOrder.totalAmount)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

            </div>

            {/* Modal Actions */}
            <div className="bg-[#F5F7FA] border-t border-border px-6 py-4 flex justify-end shrink-0 rounded-b-2xl">
              <button 
                onClick={() => setSelectedOrder(null)}
                className="px-5 py-1.5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold rounded-xl text-xs cursor-pointer"
              >
                Close
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
