import { useState, useEffect } from 'react';
import {
  X,
  Truck,
  CheckCircle2,
  ShieldCheck,
  Calendar,
  FileText,
  Copy,
  Check,
  XCircle,
  MessageCircle,
  PackageCheck,
  Maximize2,
} from 'lucide-react';
import { useCurrency } from '../../context/CurrencyContext';

const RETURN_MILESTONES = [
  {
    step: 1,
    id: 'REQUESTED',
    title: 'Requested',
    shortTitle: 'Claim Lodged',
    description: 'Claim submitted with condition photographs and reason taxonomy.',
    icon: FileText,
  },
  {
    step: 2,
    id: 'APPROVED',
    title: 'Approved',
    shortTitle: 'Atelier Approved',
    description: 'Atelier quality review verified photos and authorized reverse pickup.',
    icon: ShieldCheck,
  },
  {
    step: 3,
    id: 'PICKUP_SCHEDULED',
    title: 'Pickup Scheduled',
    shortTitle: 'Pickup Scheduled',
    description: 'Reverse logistics partner assigned and reverse AWB tracking generated.',
    icon: Calendar,
  },
  {
    step: 4,
    id: 'PICKED_UP',
    title: 'Picked Up',
    shortTitle: 'Picked Up',
    description: 'Package handed over to courier pickup agent at customer doorstep.',
    icon: PackageCheck,
  },
  {
    step: 5,
    id: 'IN_TRANSIT',
    title: 'In Transit',
    shortTitle: 'In Transit',
    description: 'Reverse package en route to central Kanchipuram artisan guild hub.',
    icon: Truck,
  },
  {
    step: 6,
    id: 'COMPLETED',
    title: 'Completed / Refunded',
    shortTitle: 'Completed',
    description: 'Physical inspection passed; refund disbursed or exchange saree shipped.',
    icon: CheckCircle2,
  },
];

const REASON_LABELS = {
  COLOR_MISMATCH: 'Color Mismatch (Shade differs from studio photo)',
  ZARI_DEFECT: 'Zari / Weave Defect (Frayed, tarnished, or broken threads)',
  FABRIC_FEEL: 'Fabric Feel / Texture (Silk weight or texture not as expected)',
  INCORRECT_ITEM: 'Incorrect Item Delivered (Wrong SKU or design sent)',
  SIZE_MISMATCH: 'Length / Blouse Deficient (Saree or blouse fabric short)',
  OTHER: 'Other Issue',
};

const REFUND_MODE_LABELS = {
  ORIGINAL_PAYMENT: 'Original Payment Source (Bank / UPI)',
  STORE_CREDIT: 'SareeKart Store Credit (+5% Patronage Bonus)',
  EXCHANGE_DRAPE: 'Replacement Saree Drape',
};

function formatDate(dateStr) {
  if (!dateStr) return 'Not available';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export default function ReturnStatusDrawer({ isOpen, onClose, returnClaim, order }) {
  const { formatPrice } = useCurrency();
  const [copiedAwb, setCopiedAwb] = useState(false);
  const [activeLightboxImage, setActiveLightboxImage] = useState(null);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        if (activeLightboxImage) {
          setActiveLightboxImage(null);
        } else {
          onClose();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, activeLightboxImage, onClose]);

  if (!isOpen || !returnClaim) return null;

  const awb = returnClaim.reverseTrackingNumber || 'Pending Assignment';
  const courier = returnClaim.reverseCourier || 'Reverse Courier Assignment in Progress';
  const isRejected = returnClaim.status?.toUpperCase() === 'REJECTED';
  const isExchange = returnClaim.type?.toUpperCase() === 'EXCHANGE';
  const claimTypeLabel = isExchange ? 'Saree Exchange' : 'Return for Refund';

  const handleCopyAwb = () => {
    if (!returnClaim.reverseTrackingNumber) return;
    navigator.clipboard.writeText(returnClaim.reverseTrackingNumber);
    setCopiedAwb(true);
    setTimeout(() => setCopiedAwb(false), 2000);
  };

  const getWhatsAppUrl = () => {
    const text = `Namaste SareeKart Atelier Support! I am inquiring regarding Return Claim #RET-${returnClaim.id} for Order #SK-${returnClaim.orderId} (Status: ${returnClaim.status}). Could you please update me on reverse logistics progression?`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  const getStepState = (stepNumber) => {
    if (isRejected) {
      if (stepNumber === 1) return 'completed';
      if (stepNumber === 2) return 'rejected';
      return 'disabled';
    }
    const status = returnClaim.status?.toUpperCase();
    switch (status) {
      case 'PENDING':
        return stepNumber === 1 ? 'current' : 'pending';
      case 'APPROVED':
        if (stepNumber === 1) return 'completed';
        if (stepNumber === 2) return 'current';
        return 'pending';
      case 'PICKUP_SCHEDULED':
        if (stepNumber <= 3) return 'completed';
        if (stepNumber === 4) return 'current';
        return 'pending';
      case 'COMPLETED':
        return 'completed';
      default:
        return stepNumber === 1 ? 'current' : 'pending';
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs transition-opacity duration-300"
      role="dialog"
      aria-modal="true"
      aria-labelledby="return-drawer-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative flex h-full w-full max-w-xl flex-col bg-white shadow-2xl overflow-hidden animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] px-6 py-5 bg-[#FAF8F5]">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[#1E6A62] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-white">
                {claimTypeLabel}
              </span>
              <span
                className={`rounded-full border px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider ${
                  isRejected
                    ? 'border-[#EBC0BA] bg-[#FBE9E7] text-[#B84F49]'
                    : returnClaim.status === 'COMPLETED'
                    ? 'border-[#B7DCCB] bg-[#E7F3EE] text-[#17644F]'
                    : returnClaim.status === 'PICKUP_SCHEDULED'
                    ? 'border-indigo-200 bg-indigo-50 text-indigo-800'
                    : 'border-[#B8D8D1] bg-[#E3F0ED] text-[#1E6A62]'
                }`}
              >
                {returnClaim.status}
              </span>
            </div>
            <h2 id="return-drawer-title" className="mt-2 text-xl font-bold text-[#17211F]">
              Return Claim #RET-{returnClaim.id}
            </h2>
            <p className="text-xs font-semibold text-[#71817A]">
              Associated with Order #SK-{returnClaim.orderId} · Submitted {formatDate(returnClaim.createdAt)}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close return telemetry drawer"
            className="rounded-full p-1.5 text-[#71817A] hover:bg-[#DDD8CF] hover:text-[#17211F] transition-colors cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Drawer Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6 text-[#17211F]">
          {/* Rejection Alert Box */}
          {isRejected && (
            <div className="rounded-[10px] border border-[#EBC0BA] bg-[#FBE9E7] p-4 text-xs shadow-xs">
              <div className="flex items-start gap-3">
                <XCircle className="h-5 w-5 text-[#B84F49] shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-bold text-[#B84F49] text-sm">Return Claim Disapproved</h3>
                  <p className="mt-1 text-[#17211F] leading-relaxed">
                    <span className="font-semibold text-[#71817A]">Reason from Atelier: </span>
                    {returnClaim.adminNotes ||
                      'Condition does not meet SareeKart 7-day post-delivery quality guidelines.'}
                  </p>
                  <p className="mt-2 text-[11px] text-[#71817A]">
                    If you believe this determination was made in error, contact our concierge below with additional photos.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Reverse Logistics Courier Card */}
          <div className="rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-4 text-xs shadow-xs">
            <p className="font-black uppercase tracking-[0.14em] text-[#71817A]">Reverse Logistics</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <div>
                <span className="font-bold text-[#71817A]">Courier Partner:</span>
                <p className="mt-0.5 font-bold text-[#17211F] flex items-center gap-1.5">
                  <Truck className="h-4 w-4 text-[#1E6A62]" />
                  <span>{courier}</span>
                </p>
              </div>

              <div>
                <span className="font-bold text-[#71817A]">Reverse Tracking AWB:</span>
                <div className="mt-0.5 flex items-center gap-2">
                  <span className="font-mono font-bold text-[#1E6A62] text-sm">{awb}</span>
                  {returnClaim.reverseTrackingNumber && (
                    <button
                      type="button"
                      onClick={handleCopyAwb}
                      className="rounded p-1 text-[#71817A] hover:bg-[#DDD8CF] hover:text-[#17211F] transition cursor-pointer"
                      title="Copy AWB Code"
                    >
                      {copiedAwb ? (
                        <Check className="h-3.5 w-3.5 text-emerald-600" />
                      ) : (
                        <Copy className="h-3.5 w-3.5" />
                      )}
                    </button>
                  )}
                  {copiedAwb && <span className="text-[10px] font-bold text-emerald-600">Copied!</span>}
                </div>
              </div>
            </div>

            <div className="mt-3.5 rounded-[6px] border border-[#EAE6DF] bg-white p-3 text-[11px] text-[#71817A] leading-relaxed">
              <span className="font-bold text-[#17211F]">Doorstep Handover Protocol: </span>
              Place the saree in its original SareeKart keepsake box with authenticity seals and tags intact. Hand over to the reverse courier agent only after matching the AWB code.
            </div>
          </div>

          {/* 6-Stage Milestone Tracker */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xs font-black uppercase tracking-[0.14em] text-[#17211F]">
                Reverse Transit Milestones
              </h3>
              <span className="text-[11px] font-bold text-[#71817A]">
                6-Stage Progression
              </span>
            </div>

            <div className="relative pl-6 space-y-6 before:absolute before:left-3 before:top-2 before:bottom-2 before:w-[2px] before:bg-[#DDD8CF]">
              {RETURN_MILESTONES.map((milestone) => {
                const state = getStepState(milestone.step);
                const Icon = milestone.icon;

                let nodeStyles = 'border-[#DDD8CF] bg-[#F7F4EE] text-[#A7A19A]';
                let titleStyles = 'text-[#A7A19A]';
                let descStyles = 'text-[#A7A19A]';

                if (state === 'completed') {
                  nodeStyles = 'border-[#1E6A62] bg-[#1E6A62] text-white ring-4 ring-[#E3F0ED]';
                  titleStyles = 'text-[#17211F] font-bold';
                  descStyles = 'text-[#71817A]';
                } else if (state === 'current') {
                  nodeStyles = 'border-[#1E6A62] bg-white text-[#1E6A62] ring-4 ring-[#1E6A62]/20 animate-pulse';
                  titleStyles = 'text-[#1E6A62] font-black';
                  descStyles = 'text-[#17211F]';
                } else if (state === 'rejected') {
                  nodeStyles = 'border-[#B84F49] bg-[#B84F49] text-white ring-4 ring-[#FBE9E7]';
                  titleStyles = 'text-[#B84F49] font-bold';
                  descStyles = 'text-[#71817A]';
                }

                return (
                  <div key={milestone.id} className="relative group">
                    {/* Stepper Node */}
                    <div
                      className={`absolute -left-6 top-0 flex h-6 w-6 -translate-x-1/2 items-center justify-center rounded-full border-2 transition-colors ${nodeStyles}`}
                    >
                      <Icon className="h-3 w-3" />
                    </div>

                    {/* Step Body */}
                    <div className="pl-4">
                      <div className="flex items-center justify-between">
                        <p className={`text-xs ${titleStyles}`}>
                          Stage {milestone.step}: {milestone.title}
                        </p>
                        {state === 'completed' && milestone.step === 1 && (
                          <span className="text-[10px] font-semibold text-[#71817A]">
                            {formatDate(returnClaim.createdAt)}
                          </span>
                        )}
                        {state === 'completed' && milestone.step === 3 && returnClaim.updatedAt && (
                          <span className="text-[10px] font-semibold text-[#71817A]">
                            {formatDate(returnClaim.updatedAt)}
                          </span>
                        )}
                      </div>
                      <p className={`mt-0.5 text-[11px] leading-relaxed ${descStyles}`}>
                        {milestone.description}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Atelier Admin Notes (if present and not rejected) */}
          {returnClaim.adminNotes && !isRejected && (
            <div className="rounded-[10px] border border-[#B8D8D1] bg-[#E3F0ED] p-4 text-xs shadow-xs">
              <p className="font-black uppercase tracking-[0.14em] text-[#1E6A62] flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4" />
                <span>Atelier Verification Notes</span>
              </p>
              <p className="mt-1.5 font-medium text-[#17211F] leading-relaxed">
                {returnClaim.adminNotes}
              </p>
            </div>
          )}

          {/* Claim & Financial Telemetry Details */}
          <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-4 text-xs shadow-xs space-y-3">
            <h3 className="font-black uppercase tracking-[0.14em] text-[#71817A]">
              Claim Specifications
            </h3>

            <div className="grid gap-2 sm:grid-cols-2">
              <div>
                <span className="font-bold text-[#71817A]">Reason Category:</span>
                <p className="mt-0.5 font-bold text-[#17211F]">
                  {REASON_LABELS[returnClaim.reason] || returnClaim.reason}
                </p>
              </div>

              <div>
                <span className="font-bold text-[#71817A]">Refund Preference:</span>
                <p className="mt-0.5 font-bold text-[#17211F]">
                  {REFUND_MODE_LABELS[returnClaim.refundMode] || returnClaim.refundMode}
                </p>
              </div>

              <div>
                <span className="font-bold text-[#71817A]">Authorized Refund:</span>
                <p className="mt-0.5 text-base font-black text-[#1E6A62]">
                  {formatPrice(returnClaim.refundAmount || order?.totalAmount || 0)}
                </p>
              </div>

              {returnClaim.exchangeSku && (
                <div>
                  <span className="font-bold text-[#71817A]">Target Exchange SKU:</span>
                  <p className="mt-0.5 font-mono font-bold text-[#17211F]">
                    {returnClaim.exchangeSku}
                  </p>
                </div>
              )}
            </div>

            {returnClaim.comments && (
              <div className="border-t border-[#EAE6DF] pt-3">
                <span className="font-bold text-[#71817A]">Customer Comments:</span>
                <p className="mt-1 font-medium text-[#42504C] italic leading-relaxed">
                  "{returnClaim.comments}"
                </p>
              </div>
            )}
          </div>

          {/* Defect Condition Photos Gallery */}
          {returnClaim.images && returnClaim.images.length > 0 && (
            <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-4 text-xs shadow-xs">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-black uppercase tracking-[0.14em] text-[#71817A]">
                  Condition Evidence ({returnClaim.images.length} Photos)
                </h3>
                <span className="text-[10px] text-[#71817A]">Click to expand</span>
              </div>

              <div className="grid grid-cols-3 gap-2.5">
                {returnClaim.images.map((imgUrl, idx) => (
                  <div
                    key={idx}
                    onClick={() => setActiveLightboxImage(imgUrl)}
                    className="group relative aspect-square overflow-hidden rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] cursor-pointer"
                  >
                    <img
                      src={imgUrl}
                      alt={`Condition evidence ${idx + 1}`}
                      className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                      onError={(e) => {
                        e.target.src =
                          'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=400&q=80';
                      }}
                    />
                    <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                      <Maximize2 className="h-4 w-4 text-white" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Associated Order Snapshot */}
          {order && (
            <div className="rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-3 text-xs flex items-center gap-3">
              <div className="h-12 w-12 shrink-0 overflow-hidden rounded-[6px] bg-[#EEF3F6]">
                <img
                  src={
                    order.items?.[0]?.productImage ||
                    order.items?.[0]?.image ||
                    'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=200&q=80'
                  }
                  alt="Order saree"
                  className="h-full w-full object-cover"
                />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-bold text-[#17211F] truncate">
                  {order.items?.[0]?.productName || order.items?.[0]?.name || 'Saree Order'}
                </p>
                <p className="text-[11px] text-[#71817A]">
                  Total: {formatPrice(order.totalAmount)} · {order.items?.length || 1} Item(s)
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Drawer Footer Actions */}
        <div className="border-t border-[#EAE6DF] bg-white px-6 py-4 flex flex-col sm:flex-row gap-2.5 sm:items-center sm:justify-between">
          <a
            href={getWhatsAppUrl()}
            target="_blank"
            rel="noreferrer"
            className="flex h-10 items-center justify-center gap-2 rounded-[8px] bg-[#25D366] px-4 text-xs font-bold text-white hover:bg-[#20b858] transition cursor-pointer shadow-xs"
          >
            <MessageCircle className="h-4 w-4" />
            <span>Concierge Assistance</span>
          </a>

          <button
            type="button"
            onClick={onClose}
            className="flex h-10 items-center justify-center rounded-[8px] border border-[#DDD8CF] bg-white px-5 text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] transition cursor-pointer"
          >
            Close Drawer
          </button>
        </div>
      </div>

      {/* Defect Photo Lightbox Modal */}
      {activeLightboxImage && (
        <div
          className="fixed inset-0 z-60 flex items-center justify-center bg-black/80 p-4"
          onClick={() => setActiveLightboxImage(null)}
        >
          <div
            className="relative max-h-[85vh] max-w-3xl overflow-hidden rounded-[12px] bg-white p-2 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setActiveLightboxImage(null)}
              className="absolute right-4 top-4 z-10 rounded-full bg-black/60 p-1.5 text-white hover:bg-black/90 transition cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
            <img
              src={activeLightboxImage}
              alt="High resolution defect evidence"
              className="max-h-[80vh] w-auto object-contain rounded-[8px]"
            />
          </div>
        </div>
      )}
    </div>
  );
}
