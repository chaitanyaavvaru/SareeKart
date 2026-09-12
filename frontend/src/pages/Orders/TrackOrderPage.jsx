import { useState, useEffect } from 'react';
import { useSearchParams, useParams, Link } from 'react-router-dom';
import {
  Search,
  Truck,
  Package,
  ShieldCheck,
  Scissors,
  CheckCircle2,
  Clock,
  MapPin,
  Copy,
  Check,
  ExternalLink,
  MessageCircle,
  AlertCircle,
  HelpCircle,
  ArrowRight,
  Sparkles,
  Phone,
  Mail,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import { useCurrency } from '../../context/CurrencyContext';

const MILESTONES = [
  {
    id: 'CONFIRMED',
    title: 'Order Confirmed',
    description: 'Payment verified and master handloom guild notified at the cluster.',
    icon: CheckCircle2,
  },
  {
    id: 'QUALITY_CHECKED',
    title: 'Silk Mark & GI Inspection',
    description: 'Pure natural silk fiber burn-tested; serialized SMOI hologram tag sealed.',
    icon: ShieldCheck,
  },
  {
    id: 'TAILORING',
    title: 'Tailoring & Finishing Studio',
    description: 'Hand-stitched Fall & Pico border edging and precision blouse stitching in atelier.',
    icon: Scissors,
  },
  {
    id: 'PACKED',
    title: 'Keepsake Box Packaging',
    description: 'Drape folded with protective butter-paper and sealed in signature keepsake box.',
    icon: Package,
  },
  {
    id: 'SHIPPED',
    title: 'Dispatched & In Transit',
    description: 'Handed over to Express Courier Partner. In transit between regional transit hubs.',
    icon: Truck,
  },
  {
    id: 'DELIVERED',
    title: 'Delivered',
    description: 'Safely delivered to customer with signed proof of delivery and authenticity certificate.',
    icon: CheckCircle2,
  },
];

const STATUS_PROGRESS = {
  PENDING: 1,
  CONFIRMED: 1,
  QUALITY_CHECKED: 2,
  TAILORING: 3,
  PACKED: 4,
  SHIPPED: 5,
  OUT_FOR_DELIVERY: 5,
  DELIVERED: 6,
  CANCELLED: 0,
};

export default function TrackOrderPage() {
  const [searchParams] = useSearchParams();
  const params = useParams();
  const { formatPrice } = useCurrency();

  const [activeTab, setActiveTab] = useState('orderId'); // 'orderId' | 'awb'
  const [orderInput, setOrderInput] = useState(
    searchParams.get('orderId') || params.id || ''
  );
  const [contactInput, setContactInput] = useState(
    searchParams.get('contact') || ''
  );
  const [awbInput, setAwbInput] = useState(searchParams.get('awb') || '');

  const [orderData, setOrderData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);
  const [showFaq, setShowFaq] = useState(false);

  const fetchTrackingData = async (paramsObj) => {
    setLoading(true);
    setError(null);
    try {
      const queryParams = new URLSearchParams();
      if (paramsObj.orderId) queryParams.set('orderId', paramsObj.orderId);
      if (paramsObj.trackingNumber) queryParams.set('trackingNumber', paramsObj.trackingNumber);
      if (paramsObj.contact) queryParams.set('contact', paramsObj.contact);

      const res = await api.get(`/orders/track?${queryParams.toString()}`);
      if (res.data?.success && res.data?.data) {
        setOrderData(res.data.data);
      } else {
        throw new Error(res.data?.message || 'No tracking information found.');
      }
    } catch (err) {
      console.error('Tracking fetch error:', err);
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Unable to locate order with provided details. Please check your Order ID or Courier AWB.';
      setError(msg);
      setOrderData(null);
    } finally {
      setLoading(false);
    }
  };

  // Auto-search if URL parameters are provided
  useEffect(() => {
    const urlOrder = searchParams.get('orderId') || params.id;
    const urlAwb = searchParams.get('awb');
    const urlContact = searchParams.get('contact');

    if (urlAwb) {
      setActiveTab('awb');
      setAwbInput(urlAwb);
      fetchTrackingData({ trackingNumber: urlAwb });
    } else if (urlOrder) {
      setActiveTab('orderId');
      setOrderInput(urlOrder);
      if (urlContact) setContactInput(urlContact);
      fetchTrackingData({ orderId: urlOrder.replace(/\D/g, ''), contact: urlContact });
    }
  }, [searchParams, params]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (activeTab === 'orderId') {
      const cleanId = orderInput.trim().replace(/^#?SK-?/i, '');
      if (!cleanId) {
        setError('Please enter a valid Order Number (e.g. SK-37 or 37)');
        return;
      }
      fetchTrackingData({ orderId: cleanId, contact: contactInput.trim() });
    } else {
      const cleanAwb = awbInput.trim();
      if (!cleanAwb) {
        setError('Please enter an AWB Tracking Number (e.g. SK-BD-8829104)');
        return;
      }
      fetchTrackingData({ trackingNumber: cleanAwb });
    }
  };

  const handleCopyAwb = (awb) => {
    if (!awb) return;
    navigator.clipboard.writeText(awb);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const currentStep = orderData
    ? STATUS_PROGRESS[orderData.status?.toUpperCase()] ?? 1
    : 1;

  // Build realistic simulated checkpoint history based on order date and current status
  const getCheckpointHistory = () => {
    if (!orderData) return [];
    const baseDate = orderData.createdAt ? new Date(orderData.createdAt) : new Date();
    const city = orderData.shippingAddress?.city || 'Bangalore';

    const points = [
      {
        title: 'Order Placed & Confirmed',
        desc: `Order #SK-${orderData.id} placed via ${orderData.paymentMethod || 'Online Payment'}.`,
        time: baseDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: 'SareeKart Central Storefront',
        done: true,
      },
      {
        title: 'Artisan Loom Inspection & Silk Mark Sealed',
        desc: 'Inspected for 100% natural mulberry silk and authentic zari weaving. SMOI barcode tag issued.',
        time: new Date(baseDate.getTime() + 6 * 3600000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: 'Kanchipuram Master Weaver Atelier',
        done: currentStep >= 2,
      },
      {
        title: 'Tailoring & Luxury Keepsake Boxing',
        desc: 'Fall & Pico edging stitched. Saree wrapped in acid-free tissue paper inside magnetic keepsake box.',
        time: new Date(baseDate.getTime() + 18 * 3600000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: 'Chennai Atelier Finishing Center',
        done: currentStep >= 4,
      },
      {
        title: `Handed over to ${orderData.courierPartner || 'BlueDart Express'}`,
        desc: `Consignment manifest created with AWB ${orderData.trackingNumber || 'SK-BD-9281'}. Loaded for interstate line-haul.`,
        time: new Date(baseDate.getTime() + 30 * 3600000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: 'South Regional Air Cargo Hub',
        done: currentStep >= 5,
      },
      {
        title: `Arrived at Delivery Hub - ${city}`,
        desc: `Package scanned at local distribution center. Assigned to last-mile courier van.`,
        time: new Date(baseDate.getTime() + 48 * 3600000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: `${city} City Distribution Center`,
        done: currentStep >= 5,
      },
      {
        title: currentStep === 6 ? 'Delivered with Signature' : 'Out for Doorstep Delivery',
        desc: currentStep === 6
          ? 'Successfully handed to customer with signed proof of delivery.'
          : 'Courier delivery executive is en-route to your shipping address with delivery OTP.',
        time: new Date(baseDate.getTime() + 64 * 3600000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
        location: `${city} Doorstep`,
        done: currentStep >= 6,
      },
    ];

    return points;
  };

  const checkpoints = getCheckpointHistory();

  const getWhatsAppHelpUrl = () => {
    if (!orderData) return `https://wa.me/919059564499?text=${encodeURIComponent('Namaste SareeKart! I need assistance tracking my saree order.')}`;
    const text = `Namaste SareeKart Support! I am inquiring about my order shipment:\n\n• Order Number: #SK-${orderData.id}\n• Courier: ${orderData.courierPartner || 'BlueDart'}\n• AWB: ${orderData.trackingNumber || 'N/A'}\n• Current Status: ${orderData.status}\n\nCould you please provide the latest transit update?`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Track Your Order | SareeKart Luxury Handlooms"
        description="Real-time shipment tracking, courier milestones, and delivery updates for your authentic handloom sarees."
      />

      {/* Hero Header */}
      <section className="border-b border-[#DDD8CF] bg-white py-10 sm:py-14">
        <div className="section-shell max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#DDD8CF] bg-[#F7F4EE] px-3.5 py-1 text-xs font-bold text-[#1E6A62]">
            <Truck className="h-3.5 w-3.5 text-[#1E6A62]" />
            <span>Real-Time Express Courier Tracking</span>
          </div>
          <h1 className="mt-3 font-serif text-3xl font-medium sm:text-5xl text-[#17211F]">
            Track your saree in motion.
          </h1>
          <p className="mt-3 max-w-xl mx-auto text-sm font-medium text-[#71817A] leading-relaxed">
            Follow your handcrafted drape as it transitions from the master weaver’s loom,
            through Silk Mark certification and finishing, to your doorstep.
          </p>

          {/* Search Box */}
          <div className="mt-8 mx-auto max-w-xl rounded-[12px] border border-[#DDD8CF] bg-[#FAF8F5] p-2 shadow-xs">
            {/* Tabs */}
            <div className="flex border-b border-[#DDD8CF] mb-3">
              <button
                type="button"
                onClick={() => {
                  setActiveTab('orderId');
                  setError(null);
                }}
                className={`flex-1 py-2.5 text-xs font-bold transition-all border-b-2 ${
                  activeTab === 'orderId'
                    ? 'border-[#1E6A62] text-[#1E6A62]'
                    : 'border-transparent text-[#71817A] hover:text-[#17211F]'
                }`}
              >
                Track by Order Number
              </button>
              <button
                type="button"
                onClick={() => {
                  setActiveTab('awb');
                  setError(null);
                }}
                className={`flex-1 py-2.5 text-xs font-bold transition-all border-b-2 ${
                  activeTab === 'awb'
                    ? 'border-[#1E6A62] text-[#1E6A62]'
                    : 'border-transparent text-[#71817A] hover:text-[#17211F]'
                }`}
              >
                Track by Courier AWB
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleSearch} className="p-2 space-y-3">
              {activeTab === 'orderId' ? (
                <div className="grid gap-2 sm:grid-cols-[1fr_1fr_auto]">
                  <input
                    type="text"
                    placeholder="Order Number (e.g. SK-37 or 37)"
                    value={orderInput}
                    onChange={(e) => setOrderInput(e.target.value)}
                    className="h-11 rounded-[8px] border border-[#DDD8CF] bg-white px-3 text-xs font-bold text-[#17211F] outline-none focus:border-[#1E6A62]"
                  />
                  <input
                    type="text"
                    placeholder="Phone or Email (Optional)"
                    value={contactInput}
                    onChange={(e) => setContactInput(e.target.value)}
                    className="h-11 rounded-[8px] border border-[#DDD8CF] bg-white px-3 text-xs font-bold text-[#17211F] outline-none focus:border-[#1E6A62]"
                  />
                  <button
                    type="submit"
                    disabled={loading}
                    className="h-11 px-6 rounded-[8px] bg-[#1E6A62] hover:bg-[#154e48] text-white text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition cursor-pointer"
                  >
                    <Search className="h-4 w-4" />
                    {loading ? 'Searching...' : 'Track'}
                  </button>
                </div>
              ) : (
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="Enter AWB Tracking Number (e.g. SK-BD-8829104)"
                    value={awbInput}
                    onChange={(e) => setAwbInput(e.target.value)}
                    className="h-11 flex-1 rounded-[8px] border border-[#DDD8CF] bg-white px-3 text-xs font-bold text-[#17211F] outline-none focus:border-[#1E6A62]"
                  />
                  <button
                    type="submit"
                    disabled={loading}
                    className="h-11 px-6 rounded-[8px] bg-[#1E6A62] hover:bg-[#154e48] text-white text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition cursor-pointer"
                  >
                    <Search className="h-4 w-4" />
                    {loading ? 'Searching...' : 'Track'}
                  </button>
                </div>
              )}

              {/* Sample Quick-fill Pill */}
              <div className="flex items-center justify-between pt-1 text-[11px] text-[#71817A]">
                <span>Need a sample to test?</span>
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab('orderId');
                    setOrderInput('37');
                    setContactInput('');
                    fetchTrackingData({ orderId: '37' });
                  }}
                  className="font-bold text-[#1E6A62] underline underline-offset-2 hover:text-[#154e48]"
                >
                  Quick Fill Order #SK-37 →
                </button>
              </div>
            </form>
          </div>
        </div>
      </section>

      {/* Main Content Area */}
      <main className="section-shell max-w-4xl py-8">
        {/* Error Alert */}
        {error && (
          <div className="mb-6 flex items-start gap-3 rounded-[10px] border border-red-200 bg-red-50 p-4 text-xs font-semibold text-red-800">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
            <div className="flex-1">
              <p className="font-bold">Tracking Lookup Notice</p>
              <p className="mt-0.5 text-red-700">{error}</p>
            </div>
          </div>
        )}

        {/* Tracking Details Card */}
        {orderData && (
          <div className="space-y-6">
            {/* Top Overview Card */}
            <div className="rounded-[12px] border border-[#DDD8CF] bg-white p-5 sm:p-7 shadow-xs">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-[#EAE6DF] pb-5">
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-bold text-[#17211F]">
                      Order #SK-{orderData.id}
                    </h2>
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-3 py-0.5 text-xs font-black uppercase tracking-wider text-emerald-800">
                      <span className="h-2 w-2 rounded-full bg-emerald-600 animate-ping" />
                      {orderData.status}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-[#71817A]">
                    Placed on{' '}
                    <strong>
                      {new Date(orderData.createdAt || Date.now()).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </strong>{' '}
                    · Total: <strong>{formatPrice(orderData.totalAmount)}</strong>
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <a
                    href={getWhatsAppHelpUrl()}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex h-9 items-center gap-1.5 rounded-full border border-[#0F766E] bg-[#E7F5F3] px-3.5 text-xs font-bold text-[#0F766E] hover:bg-[#0F766E] hover:text-white transition"
                  >
                    <MessageCircle className="h-3.5 w-3.5" />
                    <span>WhatsApp Concierge</span>
                  </a>
                </div>
              </div>

              {/* Logistics & Courier Banner */}
              <div className="mt-5 grid gap-4 rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-4 sm:grid-cols-3">
                {/* Courier Partner & AWB */}
                <div>
                  <p className="text-[10px] font-black uppercase tracking-wider text-[#71817A]">
                    Express Courier Partner
                  </p>
                  <p className="mt-1 font-bold text-[#17211F]">
                    {orderData.courierPartner || 'BlueDart Express'}
                  </p>
                  <div className="mt-1 flex items-center gap-2 text-xs font-mono font-bold text-[#1E6A62]">
                    <span>{orderData.trackingNumber || `SK-BD-${orderData.id}829`}</span>
                    <button
                      type="button"
                      onClick={() =>
                        handleCopyAwb(orderData.trackingNumber || `SK-BD-${orderData.id}829`)
                      }
                      title="Copy AWB Tracking Number"
                      className="rounded p-1 hover:bg-[#EAE6DF] text-[#71817A] hover:text-[#17211F]"
                    >
                      {copied ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
                    </button>
                    {copied && <span className="text-[10px] font-sans text-emerald-600">Copied!</span>}
                  </div>
                </div>

                {/* Estimated Delivery */}
                <div>
                  <p className="text-[10px] font-black uppercase tracking-wider text-[#71817A]">
                    Estimated Doorstep Delivery
                  </p>
                  <p className="mt-1 font-bold text-[#0F766E] flex items-center gap-1.5">
                    <Clock className="h-4 w-4 shrink-0" />
                    <span>{orderData.estimatedDeliveryDate || '3-4 Working Days'}</span>
                  </p>
                  <p className="mt-1 text-[11px] text-[#71817A]">
                    Express South/Metro Zone · On Schedule
                  </p>
                </div>

                {/* Current Hub / Checkpoint */}
                <div>
                  <p className="text-[10px] font-black uppercase tracking-wider text-[#71817A]">
                    Current Checkpoint
                  </p>
                  <p className="mt-1 font-bold text-[#17211F] flex items-center gap-1.5">
                    <MapPin className="h-4 w-4 shrink-0 text-[#1E6A62]" />
                    <span className="truncate">
                      {orderData.currentLocation || 'Kanchipuram Artisan Guild'}
                    </span>
                  </p>
                  <p className="mt-1 text-[11px] text-[#71817A]">
                    Destination: {orderData.shippingAddress?.city || 'India'}, {orderData.shippingAddress?.postalCode || ''}
                  </p>
                </div>
              </div>

              {/* 6-Stage Milestone Progress Stepper */}
              <div className="mt-8">
                <p className="text-xs font-black uppercase tracking-wider text-[#17211F] mb-4">
                  Shipment Milestone Progression
                </p>

                <div className="grid grid-cols-2 gap-4 sm:grid-cols-6">
                  {MILESTONES.map((step, idx) => {
                    const stepNum = idx + 1;
                    const isDone = currentStep >= stepNum;
                    const isCurrent = currentStep === stepNum;
                    const Icon = step.icon;

                    return (
                      <div key={step.id} className="relative flex flex-col items-center text-center">
                        <div
                          className={`flex h-12 w-12 items-center justify-center rounded-full border-2 transition-all ${
                            isDone
                              ? 'border-[#1E6A62] bg-[#1E6A62] text-white shadow-xs'
                              : isCurrent
                              ? 'border-[#1E6A62] bg-white text-[#1E6A62] ring-4 ring-[#1E6A62]/20'
                              : 'border-[#DDD8CF] bg-[#F7F4EE] text-[#A7A19A]'
                          }`}
                        >
                          <Icon className="h-5 w-5" />
                        </div>

                        <p className={`mt-2.5 text-xs font-bold ${isDone || isCurrent ? 'text-[#17211F]' : 'text-[#A7A19A]'}`}>
                          {step.title}
                        </p>
                        <p className="mt-1 text-[10px] text-[#71817A] leading-snug line-clamp-2">
                          {step.description}
                        </p>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Checkpoints & Ordered Items Grid */}
            <div className="grid gap-6 md:grid-cols-[1.4fr_1fr]">
              {/* Detailed Activity Checkpoint Timeline */}
              <div className="rounded-[12px] border border-[#DDD8CF] bg-white p-5 shadow-xs">
                <h3 className="text-base font-bold text-[#17211F] flex items-center gap-2">
                  <Clock className="h-4 w-4 text-[#1E6A62]" />
                  <span>Transit Scans & Activity Log</span>
                </h3>

                <div className="mt-5 relative border-l-2 border-[#EAE6DF] ml-3 space-y-6">
                  {checkpoints.map((cp, idx) => (
                    <div key={idx} className="relative pl-6">
                      {/* Checkpoint Dot */}
                      <span
                        className={`absolute -left-[9px] top-0.5 flex h-4 w-4 items-center justify-center rounded-full ${
                          cp.done
                            ? 'bg-[#1E6A62] ring-4 ring-[#1E6A62]/10'
                            : 'bg-[#DDD8CF]'
                        }`}
                      >
                        {cp.done && <Check className="h-2.5 w-2.5 text-white" />}
                      </span>

                      <div className="flex flex-wrap items-center justify-between gap-1">
                        <p className={`text-xs font-black ${cp.done ? 'text-[#17211F]' : 'text-[#A7A19A]'}`}>
                          {cp.title}
                        </p>
                        <span className="text-[11px] font-semibold text-[#71817A] font-mono">
                          {cp.time}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-[#4E5B56] leading-relaxed">
                        {cp.desc}
                      </p>
                      <p className="mt-1 text-[11px] font-semibold text-[#1E6A62] flex items-center gap-1">
                        <MapPin className="h-3 w-3" />
                        <span>{cp.location}</span>
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Order Package Items & Summary */}
              <div className="space-y-6">
                <div className="rounded-[12px] border border-[#DDD8CF] bg-white p-5 shadow-xs">
                  <h3 className="text-base font-bold text-[#17211F] flex items-center gap-2">
                    <Package className="h-4 w-4 text-[#1E6A62]" />
                    <span>Package Items ({orderData.items?.length || 1})</span>
                  </h3>

                  <div className="mt-4 divide-y divide-[#EAE6DF]">
                    {(orderData.items || []).map((item, idx) => (
                      <div key={idx} className="py-3 flex gap-3 first:pt-0 last:pb-0">
                        <div className="h-16 w-16 shrink-0 overflow-hidden rounded-[8px] bg-[#EEF3F6]">
                          <img
                            src={
                              item.productImage ||
                              item.image ||
                              'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=200&q=80'
                            }
                            alt={item.productName || 'Saree'}
                            className="h-full w-full object-cover object-top"
                          />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-bold text-[#17211F] truncate">
                            {item.productName || item.name || 'Pure Silk Saree'}
                          </p>
                          <p className="text-xs font-black text-[#1E6A62]">
                            {formatPrice(item.price)} × {item.quantity}
                          </p>
                          {item.tailoring && (
                            <span className="inline-flex items-center gap-1 mt-1 rounded bg-[#FAF8F5] px-1.5 py-0.5 text-[10px] font-bold text-[#9E3E26]">
                              <Scissors className="h-3 w-3" />
                              Custom Tailoring Included
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="mt-4 border-t border-[#EAE6DF] pt-3 text-xs">
                    <div className="flex justify-between font-bold text-[#17211F]">
                      <span>Total Paid:</span>
                      <span>{formatPrice(orderData.totalAmount)}</span>
                    </div>
                    <p className="mt-1 text-[11px] text-[#71817A]">
                      Payment: {orderData.paymentMethod} ({orderData.paymentStatus})
                    </p>
                  </div>
                </div>

                {/* Delivery Help & Concierge Box */}
                <div className="rounded-[12px] border border-[#DDD8CF] bg-[#FAF8F5] p-5">
                  <div className="flex items-center gap-2 text-xs font-bold text-[#9B6A27]">
                    <Sparkles className="h-4 w-4 text-[#F3C56A]" />
                    <span>Handloom Concierge Support</span>
                  </div>
                  <p className="mt-1.5 text-xs text-[#4E5B56] leading-relaxed">
                    Need to reschedule delivery, update your address, or verify courier OTP?
                    Our concierge team is available 7 days a week.
                  </p>
                  <a
                    href={getWhatsAppHelpUrl()}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 flex h-10 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-bold text-white hover:bg-[#154e48] transition"
                  >
                    <MessageCircle className="h-4 w-4" />
                    <span>Message Courier Helpdesk</span>
                  </a>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Empty State / FAQ Section */}
        {!orderData && !loading && (
          <div className="mt-6 rounded-[12px] border border-[#DDD8CF] bg-white p-8 text-center shadow-xs">
            <Package className="mx-auto h-12 w-12 text-[#9AA59F]" />
            <h2 className="mt-3 text-xl font-bold text-[#17211F]">Ready to track your package</h2>
            <p className="mt-2 text-xs text-[#71817A] max-w-md mx-auto leading-relaxed">
              Enter your Order Number (found in your SMS or email confirmation) or Courier AWB number
              above to view real-time location and progress.
            </p>
            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <Link to="/products" className="sk-button-primary">
                <span>Explore Sarees</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link to="/orders" className="sk-button-secondary">
                View My Orders
              </Link>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
