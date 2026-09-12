import { useState, useEffect } from 'react';
import {
  X,
  Truck,
  CheckCircle2,
  ShieldCheck,
  Scissors,
  Package,
  Clock,
  MapPin,
  Copy,
  Check,
  MessageCircle,
  ExternalLink,
} from 'lucide-react';
import { useCurrency } from '../../context/CurrencyContext';

const MILESTONES = [
  { id: 'CONFIRMED', title: 'Order Confirmed', icon: CheckCircle2 },
  { id: 'QUALITY_CHECKED', title: 'Silk Mark Inspection', icon: ShieldCheck },
  { id: 'TAILORING', title: 'Atelier Tailoring', icon: Scissors },
  { id: 'PACKED', title: 'Keepsake Boxed', icon: Package },
  { id: 'SHIPPED', title: 'In Transit', icon: Truck },
  { id: 'DELIVERED', title: 'Delivered', icon: CheckCircle2 },
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

export default function OrderTrackingModal({ isOpen, onClose, order }) {
  const { formatPrice } = useCurrency();
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !order) return null;

  const currentStep = STATUS_PROGRESS[order.status?.toUpperCase()] ?? 1;
  const awb = order.trackingNumber || `SK-BD-${order.id}829`;
  const courier = order.courierPartner || 'BlueDart Express';
  const location = order.currentLocation || 'Kanchipuram Artisan Guild - Atelier Central Hub';

  const handleCopy = () => {
    navigator.clipboard.writeText(awb);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getWhatsAppUrl = () => {
    const text = `Namaste SareeKart Support! I am inquiring about Order #SK-${order.id} (AWB: ${awb}, Status: ${order.status}). Could you please update me on delivery timing?`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tracking-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 id="tracking-modal-title" className="text-xl font-bold text-[#17211F]">
                Shipment Tracker
              </h2>
              <span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-black uppercase text-emerald-800">
                {order.status}
              </span>
            </div>
            <p className="mt-1 text-xs text-[#71817A]">
              Order #SK-{order.id} · Total: <strong>{formatPrice(order.totalAmount)}</strong>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close tracking modal"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Courier & AWB Box */}
        <div className="mt-5 rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-4 text-xs">
          <div className="grid gap-3 sm:grid-cols-3">
            <div>
              <p className="font-bold text-[#71817A]">Express Courier</p>
              <p className="mt-0.5 font-black text-[#17211F]">{courier}</p>
              <div className="mt-1 flex items-center gap-1.5 font-mono font-bold text-[#1E6A62]">
                <span>{awb}</span>
                <button
                  type="button"
                  onClick={handleCopy}
                  className="rounded p-0.5 hover:bg-[#DDD8CF] text-[#71817A]"
                  title="Copy AWB"
                >
                  {copied ? <Check className="h-3 w-3 text-emerald-600" /> : <Copy className="h-3 w-3" />}
                </button>
                {copied && <span className="text-[10px] text-emerald-600 font-sans">Copied</span>}
              </div>
            </div>

            <div>
              <p className="font-bold text-[#71817A]">Estimated Delivery</p>
              <p className="mt-0.5 font-black text-[#0F766E] flex items-center gap-1">
                <Clock className="h-3.5 w-3.5" />
                <span>{order.estimatedDeliveryDate || '3-4 Working Days'}</span>
              </p>
              <p className="mt-1 text-[11px] text-[#71817A]">On Schedule</p>
            </div>

            <div>
              <p className="font-bold text-[#71817A]">Current Location</p>
              <p className="mt-0.5 font-black text-[#17211F] flex items-center gap-1 truncate">
                <MapPin className="h-3.5 w-3.5 text-[#1E6A62] shrink-0" />
                <span className="truncate">{location}</span>
              </p>
              <p className="mt-1 text-[11px] text-[#71817A]">
                Destination: {order.shippingAddress?.city || 'India'}
              </p>
            </div>
          </div>
        </div>

        {/* 6-Stage Stepper */}
        <div className="mt-6">
          <p className="text-xs font-black uppercase tracking-wider text-[#17211F] mb-3">
            Transit Progression
          </p>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-6 text-center">
            {MILESTONES.map((st, idx) => {
              const stepNum = idx + 1;
              const isDone = currentStep >= stepNum;
              const isCurrent = currentStep === stepNum;
              const Icon = st.icon;

              return (
                <div key={st.id} className="flex flex-col items-center">
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full border-2 transition-all ${
                      isDone
                        ? 'border-[#1E6A62] bg-[#1E6A62] text-white'
                        : isCurrent
                        ? 'border-[#1E6A62] bg-white text-[#1E6A62] ring-2 ring-[#1E6A62]/30'
                        : 'border-[#DDD8CF] bg-[#F7F4EE] text-[#A7A19A]'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                  </div>
                  <p className={`mt-2 text-[11px] font-bold ${isDone || isCurrent ? 'text-[#17211F]' : 'text-[#A7A19A]'}`}>
                    {st.title}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="mt-7 flex flex-col gap-2.5 sm:flex-row sm:items-center sm:justify-between border-t border-[#EAE6DF] pt-5">
          <a
            href={getWhatsAppUrl()}
            target="_blank"
            rel="noreferrer"
            className="flex h-10 items-center justify-center gap-2 rounded-[8px] bg-[#25D366] px-4 text-xs font-bold text-white hover:bg-[#20b858] transition cursor-pointer"
          >
            <MessageCircle className="h-4 w-4" />
            <span>Delivery Help on WhatsApp</span>
          </a>

          <a
            href={`/track-order?orderId=${order.id}`}
            className="flex h-10 items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-white px-4 text-xs font-bold text-[#1E6A62] hover:bg-[#FAF8F5] transition"
          >
            <span>Full Tracking Portal</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </a>
        </div>
      </div>
    </div>
  );
}
