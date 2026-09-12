import { useEffect, useMemo, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ArrowRight,
  Calendar,
  CheckCircle2,
  Clock,
  Loader2,
  Package,
  RefreshCw,
  ShoppingBag,
  Truck,
  XCircle,
  FileText,
  RotateCcw,
  MessageSquare,
} from 'lucide-react';
import { downloadInvoice } from '../services/invoiceService';
import { addToCart } from '../redux/slices/cartSlice';
import { cancelUserOrder, fetchUserOrders } from '../redux/slices/orderSlice';
import SEO from '../components/common/SEO';
import OrderTrackingModal from '../components/orders/OrderTrackingModal';
import { useCurrency } from '../context/CurrencyContext';
import ReturnRequestModal from '../components/orders/ReturnRequestModal';
import ReturnStatusDrawer from '../components/orders/ReturnStatusDrawer';
import returnService from '../services/returnService';
import WhatsAppTimelineDrawer from '../components/whatsapp/WhatsAppTimelineDrawer';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

const formatDate = (dateStr) => {
  if (!dateStr) return 'Not available';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

const statusStyles = {
  PENDING: 'bg-[#F3E6C7] text-[#9B6A27] border-[#E5C98D]',
  PROCESSING: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  PACKED: 'bg-[#EAF0ED] text-[#42504C] border-[#C9D8D2]',
  SHIPPED: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  DELIVERED: 'bg-[#E7F3EE] text-[#17644F] border-[#B7DCCB]',
  CANCELLED: 'bg-[#FBE9E7] text-[#B84F49] border-[#EBC0BA]',
};

const returnStatusStyles = {
  PENDING: 'bg-[#F3E6C7] text-[#9B6A27] border-[#E5C98D]',
  APPROVED: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  PICKUP_SCHEDULED: 'bg-indigo-50 text-indigo-800 border-indigo-200',
  COMPLETED: 'bg-[#E7F3EE] text-[#17644F] border-[#B7DCCB]',
  REJECTED: 'bg-[#FBE9E7] text-[#B84F49] border-[#EBC0BA]',
};

function getReturnEligibility(order) {
  const isDelivered = order?.status?.toUpperCase() === 'DELIVERED';
  const rawDeliveryDate = order?.deliveredAt || order?.updatedAt || order?.createdAt;

  if (!isDelivered) {
    return {
      isEligible: false,
      reason: 'Order must be delivered to request a return',
      daysRemaining: 0,
      daysSinceDelivery: 999,
    };
  }

  if (!rawDeliveryDate) {
    return {
      isEligible: false,
      reason: 'Order delivery date unavailable',
      daysRemaining: 0,
      daysSinceDelivery: 999,
    };
  }

  const deliveryTime = new Date(rawDeliveryDate).getTime();
  if (isNaN(deliveryTime)) {
    return {
      isEligible: false,
      reason: 'Invalid delivery timestamp',
      daysRemaining: 0,
      daysSinceDelivery: 999,
    };
  }

  const now = Date.now();
  const diffMs = Math.max(0, now - deliveryTime);
  const diffDays = diffMs / (1000 * 60 * 60 * 24);
  const daysSinceDelivery = Math.floor(diffDays);
  const daysRemaining = Math.max(0, Math.ceil(7 - diffDays));

  if (diffDays > 7) {
    return {
      isEligible: false,
      reason: 'Return window expired (7 days cutoff from delivery)',
      daysRemaining: 0,
      daysSinceDelivery,
    };
  }

  return {
    isEligible: true,
    reason: null,
    daysRemaining,
    daysSinceDelivery,
  };
}

function getReturnPillLabel(claim) {
  if (!claim) return '';
  const status = claim.status?.toUpperCase();
  const isExchange = claim.type?.toUpperCase() === 'EXCHANGE';
  const prefix = isExchange ? 'Exchange' : 'Return';

  switch (status) {
    case 'PENDING':
      return `${prefix}: Pending Review`;
    case 'APPROVED':
      return `${prefix}: Approved`;
    case 'PICKUP_SCHEDULED':
      if (claim.reverseCourier && claim.reverseTrackingNumber) {
        return `Pickup Scheduled - ${claim.reverseCourier} (AWB: ${claim.reverseTrackingNumber})`;
      }
      if (claim.reverseCourier) {
        return `Pickup Scheduled - ${claim.reverseCourier}`;
      }
      return 'Pickup Scheduled';
    case 'COMPLETED':
      return `${prefix} Completed`;
    case 'REJECTED':
      return `${prefix} Rejected`;
    default:
      return `${prefix}: ${status}`;
  }
}

function normalizeOrderItem(item) {
  return {
    id: item.productId || item.id,
    name: item.productName || item.name || 'Saree',
    price: item.price || item.totalPrice || 0,
    image: item.productImage || item.image || '',
  };
}

export default function MyOrders() {
  const dispatch = useDispatch();
  const { formatPrice } = useCurrency();
  const { orders, loading, error } = useSelector((state) => state.orders);
  const [activeStatus, setActiveStatus] = useState('All');
  const [trackingOrder, setTrackingOrder] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [returnClaims, setReturnClaims] = useState([]);
  const [returnModalOrder, setReturnModalOrder] = useState(null);
  const [selectedReturnDrawer, setSelectedReturnDrawer] = useState(null);
  const [whatsAppDrawerOrder, setWhatsAppDrawerOrder] = useState(null);

  const handleDownloadInvoice = async (orderId, format = 'PDF') => {
    try {
      setDownloadingId(`${orderId}-${format}`);
      await downloadInvoice(orderId, format, false);
    } catch (err) {
      alert(err.message || 'Failed to download invoice');
    } finally {
      setDownloadingId(null);
    }
  };

  const loadReturnClaims = useCallback(async () => {
    try {
      const res = await returnService.getMyReturns();
      const list = Array.isArray(res) ? res : (res?.data || []);
      setReturnClaims(list);
    } catch (err) {
      console.warn('Unable to load customer return claims:', err);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    dispatch(fetchUserOrders());
    returnService.getMyReturns()
      .then((res) => {
        if (isMounted) {
          const list = Array.isArray(res) ? res : (res?.data || []);
          setReturnClaims(list);
        }
      })
      .catch((err) => {
        console.warn('Unable to load customer return claims:', err);
      });
    return () => {
      isMounted = false;
    };
  }, [dispatch]);

  const displayOrders = orders;
  const filteredOrders = activeStatus === 'All'
    ? displayOrders
    : displayOrders.filter((order) => order.status?.toUpperCase() === activeStatus);

  const returnClaimsMap = useMemo(() => {
    const map = {};
    (returnClaims || []).forEach((c) => {
      if (c && c.orderId != null) {
        map[c.orderId] = c;
      }
    });
    return map;
  }, [returnClaims]);

  const stats = useMemo(() => {
    const totalSpent = displayOrders.reduce((sum, order) => sum + (order.totalAmount || 0), 0);
    const active = displayOrders.filter((order) => !['DELIVERED', 'CANCELLED'].includes(order.status?.toUpperCase())).length;
    return [
      { label: 'Orders', value: displayOrders.length },
      { label: 'Active', value: active },
      { label: 'Total spent', value: formatCurrency(totalSpent) },
    ];
  }, [displayOrders]);

  const handleReorder = (order) => {
    (order.items || []).forEach((item) => dispatch(addToCart(normalizeOrderItem(item))));
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="My Orders | SareeKart"
        description="Track SareeKart orders, delivery status, and reorder favorite sarees."
      />

      <section className="bg-white">
        <div className="section-shell grid gap-8 py-10 lg:grid-cols-[1fr_420px] lg:items-end">
          <div>
            <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#1E6A62]">Orders</p>
            <h1 className="mt-2 text-4xl font-bold sm:text-6xl">Your sarees, in motion.</h1>
            <p className="mt-4 max-w-2xl text-base font-medium leading-8 text-[#71817A]">
              Follow delivery progress and revisit the styles you loved.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-3">
            {stats.map((stat) => (
              <div key={stat.label} className="rounded-[8px] bg-[#F7F4EE] p-4 shadow-xs">
                <p className="truncate text-xl font-black text-[#17211F]">{stat.value}</p>
                <p className="text-[11px] font-black uppercase tracking-[0.12em] text-[#71817A]">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <main className="section-shell py-8">
        {error && (
          <div className="mb-5 rounded-[8px] border border-[#E5C98D] bg-[#F3E6C7] p-4 text-sm font-bold text-[#9B6A27]">
            We could not load your orders from the server: {error}
          </div>
        )}

        <div className="mb-6 flex gap-2 overflow-x-auto no-scrollbar">
          {['All', 'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((status) => (
            <button
              key={status}
              onClick={() => setActiveStatus(status)}
              className={`h-10 shrink-0 rounded-full px-4 text-sm font-black ${
                activeStatus === status ? 'bg-[#17211F] text-white' : 'bg-white text-[#42504C] shadow-xs'
              }`}
            >
              {status === 'All' ? 'All orders' : status.toLowerCase()}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex min-h-[300px] flex-col items-center justify-center gap-4 rounded-[8px] border border-[#DDD8CF] bg-white">
            <Loader2 className="h-8 w-8 animate-spin text-[#1E6A62]" />
            <p className="text-sm font-black text-[#71817A]">Loading orders...</p>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="mx-auto max-w-lg rounded-[8px] border border-[#DDD8CF] bg-white p-10 text-center shadow-xs">
            <ShoppingBag className="mx-auto h-12 w-12 text-[#9AA59F]" />
            <h2 className="mt-4 text-2xl font-bold">No orders here</h2>
            <p className="mt-2 text-sm font-medium leading-7 text-[#71817A]">
              Try another status or start shopping the new catalog.
            </p>
            <Link to="/products" className="sk-button-primary mt-6">
              Shop sarees
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        ) : (
          <div className="grid gap-5">
            {filteredOrders.map((order) => {
              const status = order.status?.toUpperCase() || 'PENDING';
              const mainItem = order.items?.[0];
              const canCancel = ['PENDING', 'PROCESSING'].includes(status);
              const returnClaim = returnClaimsMap[order.id];
              const eligibility = getReturnEligibility(order);

              return (
                <article key={order.id} className="rounded-[8px] border border-[#DDD8CF] bg-white p-4 shadow-xs sm:p-5">
                  <div className="grid gap-5 lg:grid-cols-[1fr_280px] lg:items-start">
                    <div className="grid gap-4 sm:grid-cols-[116px_1fr]">
                      <div className="h-36 overflow-hidden rounded-[8px] bg-[#EEF3F6]">
                        <img
                          src={mainItem?.productImage || mainItem?.image || 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=400&q=80'}
                          alt={mainItem?.productName || 'Saree order'}
                          className="h-full w-full object-cover object-top"
                        />
                      </div>

                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className={`rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] ${statusStyles[status] || statusStyles.PENDING}`}>
                            {status.toLowerCase()}
                          </span>
                          <span className="inline-flex items-center gap-1 text-xs font-black text-[#71817A]">
                            <Calendar className="h-3.5 w-3.5" />
                            {formatDate(order.createdAt)}
                          </span>
                          {returnClaim && (
                            <span
                              className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] ${
                                returnStatusStyles[returnClaim.status?.toUpperCase()] || returnStatusStyles.PENDING
                              }`}
                            >
                              <RotateCcw className="h-3 w-3" />
                              <span>{getReturnPillLabel(returnClaim)}</span>
                            </span>
                          )}
                        </div>

                        <h2 className="mt-3 text-2xl font-bold text-[#17211F]">
                          Order #SK-{order.id}
                        </h2>
                        <p className="mt-1 line-clamp-1 text-sm font-bold text-[#71817A]">
                          {mainItem?.productName || mainItem?.name || 'Saree order'}
                          {order.items?.length > 1 ? ` + ${order.items.length - 1} more` : ''}
                        </p>

                        {returnClaim?.status?.toUpperCase() === 'REJECTED' && (
                          <div className="mt-3 rounded-[8px] border border-[#EBC0BA] bg-[#FBE9E7] p-3 text-xs">
                            <div className="flex items-start gap-2 text-[#B84F49]">
                              <XCircle className="h-4 w-4 shrink-0 mt-0.5" />
                              <div>
                                <p className="font-bold">Return Claim Rejected</p>
                                <p className="mt-0.5 text-[#71817A] leading-relaxed">
                                  <strong className="text-[#17211F]">Reason from Atelier: </strong>
                                  {returnClaim.adminNotes || 'Condition does not meet our return policy guidelines.'}
                                </p>
                              </div>
                            </div>
                          </div>
                        )}

                        <div className="mt-4 grid gap-3 text-sm font-semibold text-[#71817A] sm:grid-cols-3">
                          <span className="inline-flex items-center gap-2">
                            <Package className="h-4 w-4 text-[#1E6A62]" />
                            {order.items?.length || 1} item
                          </span>
                          <span className="inline-flex items-center gap-2">
                            <Truck className="h-4 w-4 text-[#1E6A62]" />
                            {order.shippingAddress?.city || 'Delivery city'}
                          </span>
                          <span className="inline-flex items-center gap-2">
                            {status === 'DELIVERED' ? <CheckCircle2 className="h-4 w-4 text-[#1E6A62]" /> : <Clock className="h-4 w-4 text-[#C48B3C]" />}
                            {order.paymentMethod || 'Payment'}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="rounded-[8px] bg-[#F7F4EE] p-4">
                      <p className="text-[11px] font-black uppercase tracking-[0.14em] text-[#71817A]">Total</p>
                      <p className="mt-1 text-2xl font-black text-[#17211F]">{formatPrice(order.totalAmount)}</p>
                      <div className="mt-4 grid gap-2">
                        <button
                          type="button"
                          onClick={() => handleDownloadInvoice(order.id, 'PDF')}
                          disabled={downloadingId === `${order.id}-PDF`}
                          className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-white text-xs font-bold uppercase tracking-wider text-[#17211F] hover:bg-[#F7F4EE] hover:border-[#1E6A62] transition shadow-xs cursor-pointer disabled:opacity-60"
                        >
                          {downloadingId === `${order.id}-PDF` ? (
                            <Loader2 className="h-4 w-4 animate-spin text-[#1E6A62]" />
                          ) : (
                            <FileText className="h-4 w-4 text-[#1E6A62]" />
                          )}
                          <span>Tax Invoice (PDF)</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setTrackingOrder(order)}
                          className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#FAF8F5] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
                        >
                          <Truck className="h-4 w-4 text-[#F3C56A]" />
                          <span>Track Shipment</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setWhatsAppDrawerOrder(order)}
                          className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-emerald-300 bg-emerald-50/60 text-xs font-bold uppercase tracking-wider text-[#075E54] hover:bg-[#128C7E] hover:text-white transition shadow-xs cursor-pointer"
                        >
                          <MessageSquare className="h-4 w-4 text-[#128C7E]" />
                          <span>WhatsApp Updates</span>
                        </button>
                        {returnClaim ? (
                          <button
                            type="button"
                            onClick={() => setSelectedReturnDrawer({ claim: returnClaim, order })}
                            className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
                          >
                            <RotateCcw className="h-4 w-4" />
                            <span>View Return Status</span>
                          </button>
                        ) : eligibility.isEligible ? (
                          <button
                            type="button"
                            onClick={() => setReturnModalOrder(order)}
                            className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
                          >
                            <RotateCcw className="h-4 w-4" />
                            <span>Return / Exchange</span>
                            <span className="rounded-full bg-[#1E6A62] px-2 py-0.5 text-[10px] font-black text-white">
                              {eligibility.daysRemaining}d left
                            </span>
                          </button>
                        ) : (
                          <div className="group relative w-full">
                            <button
                              type="button"
                              disabled
                              aria-disabled="true"
                              aria-describedby={`tooltip-return-${order.id}`}
                              className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] text-xs font-bold uppercase tracking-wider text-[#9AA59F] cursor-not-allowed opacity-75"
                            >
                              <RotateCcw className="h-4 w-4" />
                              <span>Return / Exchange</span>
                            </button>
                            <div
                              id={`tooltip-return-${order.id}`}
                              role="tooltip"
                              className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden w-64 rounded-[8px] bg-[#17211F] p-2.5 text-center text-xs font-medium text-white shadow-xl group-hover:block z-30 leading-snug"
                            >
                              {eligibility.reason}
                              <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#17211F]" />
                            </div>
                          </div>
                        )}
                        <button onClick={() => handleReorder(order)} className="sk-button-primary w-full">
                          <RefreshCw className="h-4 w-4" />
                          Reorder
                        </button>
                        {canCancel && (
                          <button
                            onClick={() => dispatch(cancelUserOrder(order.id))}
                            className="sk-button-secondary w-full text-[#B84F49]"
                          >
                            <XCircle className="h-4 w-4" />
                            Cancel
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>

      <OrderTrackingModal
        isOpen={Boolean(trackingOrder)}
        onClose={() => setTrackingOrder(null)}
        order={trackingOrder}
      />

      <ReturnRequestModal
        isOpen={Boolean(returnModalOrder)}
        onClose={() => setReturnModalOrder(null)}
        order={returnModalOrder}
        onSuccess={(newClaim) => {
          setReturnModalOrder(null);
          loadReturnClaims();
          if (newClaim) {
            setSelectedReturnDrawer({ claim: newClaim, order: returnModalOrder });
          }
        }}
      />

      <ReturnStatusDrawer
        isOpen={Boolean(selectedReturnDrawer)}
        onClose={() => setSelectedReturnDrawer(null)}
        returnClaim={selectedReturnDrawer?.claim}
        order={selectedReturnDrawer?.order}
        onRefresh={loadReturnClaims}
      />

      <WhatsAppTimelineDrawer
        isOpen={Boolean(whatsAppDrawerOrder)}
        onClose={() => setWhatsAppDrawerOrder(null)}
        order={whatsAppDrawerOrder}
      />
    </div>
  );
}
