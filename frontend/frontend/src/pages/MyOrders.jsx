import { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate, Link } from 'react-router-dom';
import { 
  ShoppingBag, 
  Calendar, 
  MapPin, 
  CreditCard, 
  Trash2, 
  ArrowLeft,
  Loader2,
  XCircle,
  Clock,
  Truck,
  CheckCircle2,
  Heart,
  Printer,
  X
} from 'lucide-react';
import { fetchUserOrders, cancelUserOrder } from '../redux/slices/orderSlice';
import ProductCard from '../components/product/ProductCard';
import api from '../api/axiosConfig';

export default function MyOrders() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { orders, loading, error } = useSelector(state => state.orders);
  const { isAuthenticated } = useSelector(state => state.auth);
  
  const [cancellingId, setCancellingId] = useState(null);
  const [activeTab, setActiveTab] = useState('orders'); // 'orders' | 'wishlist'
  const [wishlist, setWishlist] = useState([]);
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const [invoiceOrder, setInvoiceOrder] = useState(null);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login?redirect=/orders');
      return;
    }
    dispatch(fetchUserOrders());
  }, [isAuthenticated, dispatch, navigate]);

  useEffect(() => {
    if (isAuthenticated && activeTab === 'wishlist') {
      const loadWishlist = async () => {
        try {
          setWishlistLoading(true);
          const res = await api.get('/wishlist');
          if (res.data?.success) {
            setWishlist(res.data.data || []);
          }
        } catch (e) {
          console.error("Failed to load wishlist", e);
        } finally {
          setWishlistLoading(false);
        }
      };
      loadWishlist();
    }
  }, [isAuthenticated, activeTab]);

  const handleWishlistToggle = (productId, isAdded) => {
    if (!isAdded) {
      setWishlist(prev => prev.filter(p => p.id !== productId));
    }
  };

  const handleCancelOrder = async (orderId) => {
    if (window.confirm('Are you sure you want to cancel this order? This action cannot be undone.')) {
      try {
        setCancellingId(orderId);
        await dispatch(cancelUserOrder(orderId)).unwrap();
      } catch (err) {
        alert(err || 'Failed to cancel the order. Please try again.');
      } finally {
        setCancellingId(null);
      }
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

  const getStatusBadge = (status) => {
    const st = status?.toUpperCase();
    switch (st) {
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
            <Clock className="w-3.5 h-3.5" /> PENDING
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-orange-50 text-orange-700 border border-orange-200">
            <Loader2 className="w-3.5 h-3.5 animate-spin" /> PROCESSING
          </span>
        );
      case 'SHIPPED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
            <Truck className="w-3.5 h-3.5" /> SHIPPED
          </span>
        );
      case 'DELIVERED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-green-50 text-green-700 border border-green-200">
            <CheckCircle2 className="w-3.5 h-3.5" /> DELIVERED
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-red-50 text-red-700 border border-red-200">
            <XCircle className="w-3.5 h-3.5" /> CANCELLED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-gray-50 text-gray-700 border border-gray-200">
            {st}
          </span>
        );
    }
  };

  if (loading && orders.length === 0) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center space-y-4 bg-[#FAF8F5]">
        <Loader2 className="w-10 h-10 text-maroon animate-spin" />
        <p className="text-sm font-semibold text-text-secondary">Loading your orders...</p>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-12 bg-[#FAF8F5] min-h-screen">
      
      {/* Back Button */}
      <Link 
        to="/products" 
        className="inline-flex items-center gap-2 text-sm font-bold text-maroon hover:underline mb-8"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Store
      </Link>

      <div className="flex flex-col md:flex-row md:items-center md:justify-between pb-6 gap-4">
        <div>
          <h1 className="text-3xl sm:text-4xl font-extrabold font-serif text-maroon">Customer Dashboard</h1>
          <p className="text-sm text-text-secondary mt-1">Track status and manage your bookings, wishlist, and profiles</p>
        </div>
        <div className="bg-cream-dark/50 border border-[#F4F4F4] px-4 py-2 rounded-xl shrink-0 flex items-center gap-3">
          <ShoppingBag className="w-5 h-5 text-gold-dark" />
          <div>
            <p className="text-[10px] uppercase font-bold text-text-muted">Total Orders</p>
            <p className="text-sm font-bold text-maroon">{orders.length} bookings</p>
          </div>
        </div>
      </div>

      {/* Tab Switcher */}
      <div className="flex border-b border-[#F4F4F4] mb-8">
        <button
          onClick={() => setActiveTab('orders')}
          className={`flex items-center gap-2 px-6 py-3 border-b-2 font-serif text-sm font-bold transition-all duration-300 cursor-pointer ${
            activeTab === 'orders'
              ? 'border-maroon text-maroon'
              : 'border-transparent text-text-secondary hover:text-maroon'
          }`}
        >
          <ShoppingBag className="w-4 h-4" /> My Bookings ({orders.length})
        </button>
        <button
          onClick={() => setActiveTab('wishlist')}
          className={`flex items-center gap-2 px-6 py-3 border-b-2 font-serif text-sm font-bold transition-all duration-300 cursor-pointer ${
            activeTab === 'wishlist'
              ? 'border-maroon text-maroon'
              : 'border-transparent text-text-secondary hover:text-maroon'
          }`}
        >
          <Heart className="w-4 h-4" /> My Wishlist ({wishlist.length})
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-xl font-medium mb-6">
          {error}
        </div>
      )}

      {activeTab === 'orders' ? (
        orders.length === 0 ? (
          <div className="text-center py-16 bg-white border border-[#F4F4F4] rounded-3xl shadow-xs max-w-lg mx-auto">
            <div className="flex justify-center mb-4">
              <ShoppingBag className="w-12 h-12 text-[#C9A227]" />
            </div>
            <h2 className="text-xl font-bold font-serif text-maroon mb-2">No Orders Placed Yet</h2>
            <p className="text-sm text-text-secondary max-w-xs mx-auto mb-6">
              You haven't bought any premium sarees yet. Explore our handcrafted collection to make your first booking!
            </p>
            <Link 
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl transition"
            >
              Start Shopping
            </Link>
          </div>
        ) : (
          <div className="space-y-8 animate-fade-in">
            {orders.map((order) => {
              const isCancellable = order.status === 'PENDING' || order.status === 'PROCESSING';
              return (
                <div 
                  key={order.id} 
                  className="bg-white border border-[#F4F4F4] rounded-3xl overflow-hidden shadow-xs hover:shadow-md transition-shadow"
                >
                  
                  {/* Order Summary Header */}
                  <div className="bg-[#FAF8F5] border-b border-[#F4F4F4] px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="grid grid-cols-2 md:flex md:items-center gap-x-6 gap-y-2 text-sm">
                      <div>
                        <p className="text-[10px] uppercase font-bold text-text-muted">Order Placed</p>
                        <p className="font-semibold text-text-primary mt-0.5 flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-gold-dark" />
                          {formatDate(order.createdAt)}
                        </p>
                      </div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-text-muted">Order Number</p>
                        <p className="font-semibold text-maroon mt-0.5">#SK-2026-{order.id}</p>
                      </div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-text-muted">Payment Mode</p>
                        <p className="font-semibold text-text-primary mt-0.5 flex items-center gap-1.5 capitalize">
                          <CreditCard className="w-3.5 h-3.5 text-gold-dark" />
                          {order.paymentMethod?.toLowerCase()} ({order.paymentStatus})
                        </p>
                      </div>
                      <div>
                        <p className="text-[10px] uppercase font-bold text-text-muted">Grand Total</p>
                        <p className="font-bold text-[#3A1028] mt-0.5">{formatCurrency(order.totalAmount)}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      {getStatusBadge(order.status)}
                      <button 
                        onClick={() => setInvoiceOrder(order)}
                        className="p-1.5 border border-[#F4F4F4] hover:border-[#C9A227] hover:text-maroon text-text-secondary bg-white rounded-xl transition-colors cursor-pointer"
                        title="Print Invoice"
                      >
                        <Printer className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Order Body Grid */}
                  <div className="p-6 grid grid-cols-1 lg:grid-cols-12 gap-8">
                    
                    {/* Items List */}
                    <div className="lg:col-span-8 space-y-4">
                      <div className="divide-y divide-[#F4F4F4]">
                        {order.items?.map((item) => (
                          <div key={item.id} className="flex gap-4 py-4 first:pt-0 last:pb-0 items-center">
                            <div className="w-16 h-20 bg-cream border border-[#F4F4F4] rounded-xl overflow-hidden shrink-0 flex items-center justify-center">
                              <ShoppingBag className="w-5 h-5 text-[#C9A227]" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="font-bold text-sm text-text-primary truncate">
                                {item.productName}
                              </h4>
                              <p className="text-xs text-text-secondary mt-1">
                                Price: {formatCurrency(item.price)}
                              </p>
                              <p className="text-xs text-text-muted mt-0.5">
                                Quantity: {item.quantity}
                              </p>
                            </div>
                            <div className="text-right shrink-0">
                              <p className="font-bold text-sm text-maroon">
                                {formatCurrency(item.totalPrice)}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Delivery Location & Actions */}
                    <div className="lg:col-span-4 bg-[#FAF8F5]/40 border border-[#F4F4F4] rounded-xl p-5 space-y-5 flex flex-col justify-between">
                      <div className="space-y-3.5">
                        <h3 className="text-xs font-bold uppercase tracking-wider text-gold-dark border-b border-[#F4F4F4] pb-1 flex items-center gap-1.5">
                          <MapPin className="w-3.5 h-3.5" /> Shipping Address
                        </h3>
                        {order.shippingAddress ? (
                          <div className="text-xs space-y-1">
                            <p className="font-bold text-text-primary">{order.shippingAddress.fullName}</p>
                            <p className="text-text-secondary line-clamp-2">{order.shippingAddress.streetAddress}</p>
                            <p className="text-text-secondary">
                              {order.shippingAddress.city}, {order.shippingAddress.state} - {order.shippingAddress.pincode}
                            </p>
                            <p className="text-text-muted pt-1">Phone: {order.shippingAddress.phone}</p>
                          </div>
                        ) : (
                          <p className="text-xs text-text-muted italic">No address provided</p>
                        )}
                      </div>

                      {isCancellable && (
                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          disabled={cancellingId === order.id}
                          className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-white border border-red-200 text-red-600 hover:bg-red-50 disabled:bg-gray-50 disabled:text-gray-400 disabled:border-gray-200 font-bold rounded-xl text-xs shadow-xs hover:shadow-md transition-all cursor-pointer"
                        >
                          {cancellingId === order.id ? (
                            <>
                              <Loader2 className="w-3.5 h-3.5 animate-spin" /> Cancelling...
                            </>
                          ) : (
                            <>
                              <Trash2 className="w-3.5 h-3.5" /> Cancel Order
                            </>
                          )}
                        </button>
                      )}
                    </div>

                  </div>

                </div>
              );
            })}
          </div>
        )
      ) : wishlistLoading ? (
        <div className="py-24"><Loader2 className="w-10 h-10 text-maroon animate-spin mx-auto" /></div>
      ) : wishlist.length === 0 ? (
        <div className="text-center py-16 bg-white border border-[#F4F4F4] rounded-3xl shadow-xs max-w-lg mx-auto">
          <div className="flex justify-center mb-4">
            <Heart className="w-12 h-12 text-[#C9A227]" />
          </div>
          <h2 className="text-xl font-bold font-serif text-maroon mb-2">Your Wishlist is Empty</h2>
          <p className="text-sm text-text-secondary max-w-xs mx-auto mb-6">
            Bookmark your favorite handwoven designs by tapping the heart icon on any product page.
          </p>
          <Link 
            to="/products"
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl transition"
          >
            Explore Sarees
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 animate-fade-in">
          {wishlist.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              initialWishlisted={true}
              onWishlistToggle={handleWishlistToggle}
            />
          ))}
        </div>
      )}

      {/* ── PRINTABLE INVOICE OVERLAY ── */}
      {invoiceOrder && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50 overflow-y-auto">
          <div className="bg-white rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl relative my-8">
            
            {/* Modal Header */}
            <div className="bg-[#3A1028] text-[#C9A227] px-6 py-4 flex items-center justify-between no-print">
              <span className="font-serif font-bold text-sm sm:text-base flex items-center gap-2">
                <Printer className="w-5 h-5" /> Order Receipt SK-2026-{invoiceOrder.id}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => window.print()}
                  className="px-4 py-1.5 bg-[#C9A227] hover:bg-gold-light text-[#3A1028] font-bold rounded-xl text-xs shadow transition cursor-pointer"
                >
                  Print Invoice
                </button>
                <button
                  onClick={() => setInvoiceOrder(null)}
                  className="p-1 bg-white/10 hover:bg-white/20 rounded-xl transition cursor-pointer text-white"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Printable Area */}
            <div id="printable-invoice" className="p-8 space-y-8 text-text-primary bg-white">
              
              {/* Brand Header */}
              <div className="flex justify-between items-start border-b border-[#F4F4F4] pb-6">
                <div>
                  <h1 className="text-3xl font-extrabold font-serif text-maroon tracking-wider">SAREEKART</h1>
                  <p className="text-[10px] uppercase font-bold text-text-muted mt-1">Luxury Artisanal Drapes</p>
                  <p className="text-[10px] text-text-secondary mt-1">Weavers Colony, Hyderabad, India</p>
                </div>
                <div className="text-right">
                  <h2 className="text-lg font-bold font-serif text-text-primary">INVOICE</h2>
                  <p className="text-xs text-text-secondary font-semibold mt-1">Invoice: SK-2026-{invoiceOrder.id}</p>
                  <p className="text-xs text-text-secondary font-semibold">Date: {formatDate(invoiceOrder.createdAt)}</p>
                </div>
              </div>

              {/* Billing Info */}
              <div className="grid grid-cols-2 gap-8 text-xs border-b border-[#F4F4F4] pb-6">
                <div>
                  <h3 className="font-bold text-text-muted uppercase tracking-wider mb-2">Sold To:</h3>
                  <p className="font-bold text-text-primary text-sm">{invoiceOrder.shippingAddress?.fullName}</p>
                  <p className="text-text-secondary mt-0.5">{invoiceOrder.shippingAddress?.streetAddress}</p>
                  <p className="text-text-secondary">{invoiceOrder.shippingAddress?.city}, {invoiceOrder.shippingAddress?.state} - {invoiceOrder.shippingAddress?.pincode}</p>
                  <p className="text-text-muted mt-1.5">Phone: {invoiceOrder.shippingAddress?.phone}</p>
                </div>
                <div>
                  <h3 className="font-bold text-text-muted uppercase tracking-wider mb-2">Payment Details:</h3>
                  <p className="text-text-secondary"><span className="font-bold">Method:</span> {invoiceOrder.paymentMethod}</p>
                  <p className="text-text-secondary"><span className="font-bold">Status:</span> {invoiceOrder.paymentStatus}</p>
                  <p className="text-text-secondary"><span className="font-bold">Drapes Count:</span> {invoiceOrder.items?.reduce((sum, i) => sum + i.quantity, 0)}</p>
                </div>
              </div>

              {/* Items Table */}
              <table className="w-full text-xs text-left border-collapse">
                <thead>
                  <tr className="border-b-2 border-[#F4F4F4] pb-2 text-text-muted uppercase font-bold tracking-wider">
                    <th className="py-2.5">Saree Masterpiece</th>
                    <th className="py-2.5 text-center">Qty</th>
                    <th className="py-2.5 text-right">Price</th>
                    <th className="py-2.5 text-right">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#F4F4F4]">
                  {invoiceOrder.items?.map((item) => (
                    <tr key={item.id} className="py-2.5">
                      <td className="py-2.5 font-semibold text-text-primary">{item.productName}</td>
                      <td className="py-2.5 text-center">{item.quantity}</td>
                      <td className="py-2.5 text-right">{formatCurrency(item.price)}</td>
                      <td className="py-2.5 text-right font-bold text-maroon">{formatCurrency(item.totalPrice)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Invoice Totals */}
              <div className="flex justify-end pt-6 border-t border-[#F4F4F4]">
                <div className="w-48 space-y-2 text-xs">
                  <div className="flex justify-between text-text-secondary">
                    <span>Tax (incl.)</span>
                    <span>₹0.00</span>
                  </div>
                  <div className="flex justify-between text-text-secondary">
                    <span>Shipping</span>
                    <span>FREE</span>
                  </div>
                  <div className="flex justify-between text-base font-extrabold text-maroon pt-2 border-t border-[#F4F4F4]">
                    <span>Amount Paid</span>
                    <span>{formatCurrency(invoiceOrder.totalAmount)}</span>
                  </div>
                </div>
              </div>

              {/* Footer notes */}
              <div className="text-center text-[10px] text-text-muted italic border-t border-[#F4F4F4] pt-6">
                Thank you for supporting handloom weavers. Wear with pride!
              </div>

            </div>

            {/* Print Hider Stylesheet */}
            <style dangerouslySetInnerHTML={{__html: `
              @media print {
                body * {
                  visibility: hidden;
                }
                #printable-invoice, #printable-invoice * {
                  visibility: visible;
                }
                #printable-invoice {
                  position: absolute;
                  left: 0;
                  top: 0;
                  width: 100%;
                  margin: 0;
                  padding: 20px;
                }
                .no-print {
                  display: none !important;
                }
              }
            `}} />
            
          </div>
        </div>
      )}

    </div>
  );
}
