# Milestone 2 Technical Specification: Order Card Eligibility Gate & Telemetry Drawer

**Document**: `m2_telemetry_spec.md`  
**Agent**: Explorer 2 (`teamwork_preview_explorer_m2_2_5`)  
**Target Files**:
- `frontend/src/pages/MyOrders.jsx` (Modification)
- `frontend/src/components/orders/ReturnStatusDrawer.jsx` (New Component)  
**Parent Mission**: Milestone 2 (Customer Returns & Exchanges Frontend Suite)  
**Integrity Mode**: Development / Read-Only Investigation  

---

## 1. Executive Summary

This specification defines the exact frontend architecture, business rules, visual tokens, and implementation code for:
1. **The 7-Day Order Return Eligibility Gate** in `frontend/src/pages/MyOrders.jsx`:
   - Enforces post-delivery return window ($\Delta t \le 7\text{ days}$) using order delivery timestamp hierarchy (`deliveredAt || updatedAt || createdAt`).
   - Renders an active "Return / Exchange" button with a dynamic countdown badge (`X days left`) for eligible orders without an active claim.
   - Renders a disabled button wrapped in an accessible CSS tooltip explaining why the order cannot be returned (`Order must be delivered to request a return` or `Return window expired (7 days cutoff from delivery)`).
2. **Order Card Return Telemetry** in `frontend/src/pages/MyOrders.jsx`:
   - Correlates user orders with submitted return claims via `returnService.getMyReturns()`.
   - Displays real-time status pills across all 5 backend states (`PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`).
   - Renders a prominent Rejection Alert Banner displaying admin explanation notes whenever a claim is rejected.
   - Replaces the initiation button with a "View Return Status" action that opens the drawer.
3. **The Customer Return Telemetry Drawer** in `frontend/src/components/orders/ReturnStatusDrawer.jsx`:
   - An accessible slide-over drawer featuring a 6-stage reverse logistics milestone stepper (`Requested`, `Approved`, `Pickup Scheduled`, `Picked Up`, `In Transit`, `Completed / Refunded`).
   - Reverse logistics carrier attribution with a 1-click copyable Air Waybill (AWB) code and doorstep handover instructions.
   - Atelier admin notes, submission/update timestamps, financial refund breakdown, defect photo gallery, and WhatsApp concierge link.

---

## 2. Order Card Eligibility Gate Specification (`MyOrders.jsx`)

### 2.1 Eligibility Evaluation Algorithm
An order is eligible for self-service return or exchange if and only if:
1. Its status is `DELIVERED` (case-insensitive check: `order.status?.toUpperCase() === 'DELIVERED'`).
2. Its post-delivery age is between 0 and 7 calendar days inclusive ($0 \le \Delta t_{\text{days}} \le 7$).
3. No prior return claim has been submitted for this order.

#### Timestamp Hierarchy
Orders delivered through the SareeKart logistics pipeline capture delivery time in `order.deliveredAt`. In case the database column is populated via fallback order update timestamps, the evaluation follows this strict hierarchy:
$$\text{deliveryDate} = \text{order.deliveredAt} \parallel \text{order.updatedAt} \parallel \text{order.createdAt}$$

#### Mathematical Calculation
```javascript
export function getReturnEligibility(order) {
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
  const daysSinceDelivery = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const daysRemaining = Math.max(0, 7 - daysSinceDelivery);

  if (daysSinceDelivery > 7) {
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
```

### 2.2 Edge Case Matrix

| Order Status | Delivered Date Age | Claim Exists | `isEligible` | UI Rendering | Tooltip / Badge Copy |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `PENDING` | N/A | No | `false` | Disabled button | "Order must be delivered to request a return" |
| `PROCESSING` | N/A | No | `false` | Disabled button | "Order must be delivered to request a return" |
| `SHIPPED` | N/A | No | `false` | Disabled button | "Order must be delivered to request a return" |
| `CANCELLED` | N/A | No | `false` | Disabled button | "Order must be delivered to request a return" |
| `DELIVERED` | 0 days (today) | No | `true` | Active button | Badge: `7d left` |
| `DELIVERED` | 3 days | No | `true` | Active button | Badge: `4d left` |
| `DELIVERED` | 7 days (boundary) | No | `true` | Active button | Badge: `0d left` (or `Last day`) |
| `DELIVERED` | 8 days | No | `false` | Disabled button | "Return window expired (7 days cutoff from delivery)" |
| `DELIVERED` | 30 days | No | `false` | Disabled button | "Return window expired (7 days cutoff from delivery)" |
| `DELIVERED` | 2 days | Yes (`PENDING`) | N/A | Status Pill & "View Return Status" | Header pill: `Return: Pending Review` |
| `DELIVERED` | 5 days | Yes (`REJECTED`)| N/A | Status Pill & Rejection Alert Box | Banner: "Reason from Atelier: [adminNotes]" |

### 2.3 Accessible CSS Tooltip Structure
For non-eligible orders without a return claim:
```jsx
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
```

---

## 3. Order Card Return Telemetry (`MyOrders.jsx`)

### 3.1 Order-Claim Correlation
`MyOrders.jsx` queries the customer returns endpoint upon loading:
```javascript
const [returnClaims, setReturnClaims] = useState([]);

const loadReturnClaims = async () => {
  try {
    const res = await returnService.getMyReturns();
    const list = Array.isArray(res) ? res : (res?.data || []);
    setReturnClaims(list);
  } catch (err) {
    console.warn('Unable to load customer return claims:', err);
  }
};

useEffect(() => {
  loadReturnClaims();
}, []);

const returnClaimsMap = useMemo(() => {
  const map = {};
  (returnClaims || []).forEach((c) => {
    if (c && c.orderId != null) {
      map[c.orderId] = c;
    }
  });
  return map;
}, [returnClaims]);
```

### 3.2 Return Status Pill Styling & Mapping

```javascript
const returnStatusStyles = {
  PENDING: 'bg-[#F3E6C7] text-[#9B6A27] border-[#E5C98D]',
  APPROVED: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
  PICKUP_SCHEDULED: 'bg-indigo-50 text-indigo-800 border-indigo-200',
  COMPLETED: 'bg-[#E7F3EE] text-[#17644F] border-[#B7DCCB]',
  REJECTED: 'bg-[#FBE9E7] text-[#B84F49] border-[#EBC0BA]',
};

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
```

### 3.3 Card Header Placement
Rendered in the order card header alongside the order status pill:
```jsx
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
```

### 3.4 Rejection Alert Banner Placement
Rendered prominently below order items in the order card:
```jsx
{returnClaim?.status?.toUpperCase() === 'REJECTED' && (
  <div className="mt-3 rounded-[8px] border border-[#EBC0BA] bg-[#FBE9E7] p-3.5 text-xs">
    <div className="flex items-start gap-2.5 text-[#B84F49]">
      <XCircle className="h-4 w-4 shrink-0 mt-0.5" />
      <div>
        <p className="font-bold">Return Claim Rejected</p>
        <p className="mt-1 text-[#17211F] leading-relaxed">
          <span className="font-semibold text-[#71817A]">Reason from Atelier: </span>
          {returnClaim.adminNotes || 'Condition does not meet our return policy guidelines.'}
        </p>
      </div>
    </div>
  </div>
)}
```

### 3.5 Action Button Switch
In the card's right-hand action column:
- If `returnClaim` exists $\rightarrow$ Render **"View Return Status"** button.
- If no claim & eligible $\rightarrow$ Render active **"Return / Exchange (Xd left)"** button.
- If no claim & ineligible $\rightarrow$ Render disabled button with hover tooltip.

---

## 4. Return Status Drawer Component Specification

**File Path**: `frontend/src/components/orders/ReturnStatusDrawer.jsx`  
**Purpose**: An interactive slide-over drawer presenting reverse logistics telemetry, 6-stage milestone tracker, reverse courier details, copyable AWB code, defect photo review, and concierge help.

### 4.1 6-Stage Reverse Logistics Milestone Tracker

The 6 stages and their mapping to backend statuses:
1. **Stage 1: `Requested` (`PENDING`)**
   - Title: `Requested`
   - Description: Customer submitted claim with condition photographs and reason taxonomy.
2. **Stage 2: `Approved` (`APPROVED`)**
   - Title: `Approved`
   - Description: SareeKart atelier quality team validated photos and authorized reverse pickup.
3. **Stage 3: `Pickup Scheduled` (`PICKUP_SCHEDULED`)**
   - Title: `Pickup Scheduled`
   - Description: Reverse courier assigned (`reverseCourier`) and AWB tracking code generated.
4. **Stage 4: `Picked Up`**
   - Title: `Picked Up`
   - Description: Doorstep courier agent collected the packaged saree box from customer.
5. **Stage 5: `In Transit`**
   - Title: `In Transit`
   - Description: Parcel en route to SareeKart central artisan guild inspection hub.
6. **Stage 6: `Completed / Refunded` (`COMPLETED`)**
   - Title: `Completed / Refunded`
   - Description: Atelier physical inspection passed; refund disbursed to payment method or exchange saree dispatched.

#### State Determination Function
```javascript
function getStageState(stepNumber, returnClaim) {
  const status = returnClaim?.status?.toUpperCase();

  if (status === 'REJECTED') {
    if (stepNumber === 1) return 'completed';
    if (stepNumber === 2) return 'rejected';
    return 'disabled';
  }

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
}
```

### 4.2 Complete Source Code for `ReturnStatusDrawer.jsx`

```jsx
import { useState, useEffect } from 'react';
import {
  X,
  RotateCcw,
  Truck,
  CheckCircle2,
  ShieldCheck,
  Package,
  Calendar,
  FileText,
  Copy,
  Check,
  AlertCircle,
  XCircle,
  MessageCircle,
  Clock,
  PackageCheck,
  Info,
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

function formatDateTime(dateStr) {
  if (!dateStr) return 'Not available';
  const d = new Date(dateStr);
  return `${d.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })} at ${d.toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
  })}`;
}

export default function ReturnStatusDrawer({ isOpen, onClose, returnClaim, order, onRefresh }) {
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
```

---

## 5. Exact Implementation Diffs for `MyOrders.jsx`

### 5.1 Overview of Changes in `MyOrders.jsx`
1. **Imports**:
   - Add `RotateCcw` to `lucide-react` import.
   - Import `ReturnRequestModal` from `../components/orders/ReturnRequestModal`.
   - Import `ReturnStatusDrawer` from `../components/orders/ReturnStatusDrawer`.
   - Import `returnService` from `../services/returnService`.
2. **Eligibility & Style Helpers**:
   - Add `returnStatusStyles` dictionary.
   - Add `getReturnEligibility(order)` helper.
   - Add `getReturnPillLabel(claim)` helper.
3. **Component State**:
   - Add `returnClaims`, `returnModalOrder`, `selectedReturnDrawer`.
   - Add `loadReturnClaims()` and `useEffect()` on mount.
   - Add `returnClaimsMap = useMemo(...)`.
4. **Order Card Rendering**:
   - Render return status pill in order header.
   - Render rejection alert banner in main card area if `REJECTED`.
   - Replace or conditionally render action button:
     - If claim exists $\rightarrow$ "View Return Status"
     - If eligible $\rightarrow$ "Return / Exchange (Xd left)"
     - If ineligible $\rightarrow$ Disabled button with CSS hover tooltip
5. **Modals / Drawers**:
   - Mount `<ReturnRequestModal />` and `<ReturnStatusDrawer />` at the bottom of the component.

### 5.2 Unified Patch Diff

```diff
--- a/frontend/src/pages/MyOrders.jsx
+++ b/frontend/src/pages/MyOrders.jsx
@@ -14,6 +14,7 @@
   Truck,
   XCircle,
   FileText,
+  RotateCcw,
 } from 'lucide-react';
 import { downloadInvoice } from '../services/invoiceService';
 import { addToCart } from '../redux/slices/cartSlice';
@@ -21,6 +22,9 @@
 import SEO from '../components/common/SEO';
 import OrderTrackingModal from '../components/orders/OrderTrackingModal';
 import { useCurrency } from '../context/CurrencyContext';
+import ReturnRequestModal from '../components/orders/ReturnRequestModal';
+import ReturnStatusDrawer from '../components/orders/ReturnStatusDrawer';
+import returnService from '../services/returnService';
 
 const formatCurrency = (val) =>
   new Intl.NumberFormat('en-IN', {
@@ -46,6 +50,75 @@
   CANCELLED: 'bg-[#FBE9E7] text-[#B84F49] border-[#EBC0BA]',
 };
 
+const returnStatusStyles = {
+  PENDING: 'bg-[#F3E6C7] text-[#9B6A27] border-[#E5C98D]',
+  APPROVED: 'bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]',
+  PICKUP_SCHEDULED: 'bg-indigo-50 text-indigo-800 border-indigo-200',
+  COMPLETED: 'bg-[#E7F3EE] text-[#17644F] border-[#B7DCCB]',
+  REJECTED: 'bg-[#FBE9E7] text-[#B84F49] border-[#EBC0BA]',
+};
+
+function getReturnEligibility(order) {
+  const isDelivered = order?.status?.toUpperCase() === 'DELIVERED';
+  const rawDeliveryDate = order?.deliveredAt || order?.updatedAt || order?.createdAt;
+
+  if (!isDelivered) {
+    return {
+      isEligible: false,
+      reason: 'Order must be delivered to request a return',
+      daysRemaining: 0,
+      daysSinceDelivery: 999,
+    };
+  }
+
+  if (!rawDeliveryDate) {
+    return {
+      isEligible: false,
+      reason: 'Order delivery date unavailable',
+      daysRemaining: 0,
+      daysSinceDelivery: 999,
+    };
+  }
+
+  const deliveryTime = new Date(rawDeliveryDate).getTime();
+  if (isNaN(deliveryTime)) {
+    return {
+      isEligible: false,
+      reason: 'Invalid delivery timestamp',
+      daysRemaining: 0,
+      daysSinceDelivery: 999,
+    };
+  }
+
+  const now = Date.now();
+  const diffMs = Math.max(0, now - deliveryTime);
+  const daysSinceDelivery = Math.floor(diffMs / (1000 * 60 * 60 * 24));
+  const daysRemaining = Math.max(0, 7 - daysSinceDelivery);
+
+  if (daysSinceDelivery > 7) {
+    return {
+      isEligible: false,
+      reason: 'Return window expired (7 days cutoff from delivery)',
+      daysRemaining: 0,
+      daysSinceDelivery,
+    };
+  }
+
+  return {
+    isEligible: true,
+    reason: null,
+    daysRemaining,
+    daysSinceDelivery,
+  };
+}
+
+function getReturnPillLabel(claim) {
+  if (!claim) return '';
+  const status = claim.status?.toUpperCase();
+  const isExchange = claim.type?.toUpperCase() === 'EXCHANGE';
+  const prefix = isExchange ? 'Exchange' : 'Return';
+
+  switch (status) {
+    case 'PENDING':
+      return `${prefix}: Pending Review`;
+    case 'APPROVED':
+      return `${prefix}: Approved`;
+    case 'PICKUP_SCHEDULED':
+      if (claim.reverseCourier && claim.reverseTrackingNumber) {
+        return `Pickup Scheduled - ${claim.reverseCourier} (AWB: ${claim.reverseTrackingNumber})`;
+      }
+      if (claim.reverseCourier) {
+        return `Pickup Scheduled - ${claim.reverseCourier}`;
+      }
+      return 'Pickup Scheduled';
+    case 'COMPLETED':
+      return `${prefix} Completed`;
+    case 'REJECTED':
+      return `${prefix} Rejected`;
+    default:
+      return `${prefix}: ${status}`;
+  }
+}
+
 function normalizeOrderItem(item) {
   return {
     id: item.productId || item.id,
@@ -62,6 +135,9 @@
   const { orders, loading, error } = useSelector((state) => state.orders);
   const [activeStatus, setActiveStatus] = useState('All');
   const [trackingOrder, setTrackingOrder] = useState(null);
   const [downloadingId, setDownloadingId] = useState(null);
+  const [returnClaims, setReturnClaims] = useState([]);
+  const [returnModalOrder, setReturnModalOrder] = useState(null);
+  const [selectedReturnDrawer, setSelectedReturnDrawer] = useState(null);
 
   const handleDownloadInvoice = async (orderId, format = 'PDF') => {
     try {
@@ -75,8 +151,28 @@
   };
 
+  const loadReturnClaims = async () => {
+    try {
+      const res = await returnService.getMyReturns();
+      const list = Array.isArray(res) ? res : (res?.data || []);
+      setReturnClaims(list);
+    } catch (err) {
+      console.warn('Unable to load customer return claims:', err);
+    }
+  };
+
   useEffect(() => {
     dispatch(fetchUserOrders());
+    loadReturnClaims();
   }, [dispatch]);
 
   const displayOrders = orders;
+  const returnClaimsMap = useMemo(() => {
+    const map = {};
+    (returnClaims || []).forEach((c) => {
+      if (c && c.orderId != null) {
+        map[c.orderId] = c;
+      }
+    });
+    return map;
+  }, [returnClaims]);
 
@@ -171,6 +267,8 @@
               const status = order.status?.toUpperCase() || 'PENDING';
               const mainItem = order.items?.[0];
               const canCancel = ['PENDING', 'PROCESSING'].includes(status);
+              const returnClaim = returnClaimsMap[order.id];
+              const eligibility = getReturnEligibility(order);
 
               return (
                 <article key={order.id} className="rounded-[8px] border border-[#DDD8CF] bg-white p-4 shadow-xs sm:p-5">
@@ -190,6 +288,16 @@
                           <span className="inline-flex items-center gap-1 text-xs font-black text-[#71817A]">
                             <Calendar className="h-3.5 w-3.5" />
                             {formatDate(order.createdAt)}
                           </span>
+                          {returnClaim && (
+                            <span
+                              className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] ${
+                                returnStatusStyles[returnClaim.status?.toUpperCase()] || returnStatusStyles.PENDING
+                              }`}
+                            >
+                              <RotateCcw className="h-3 w-3" />
+                              <span>{getReturnPillLabel(returnClaim)}</span>
+                            </span>
+                          )}
                         </div>
 
                         <h2 className="mt-3 text-2xl font-bold text-[#17211F]">
@@ -200,6 +308,19 @@
                           {order.items?.length > 1 ? ` + ${order.items.length - 1} more` : ''}
                         </p>
 
+                        {returnClaim?.status?.toUpperCase() === 'REJECTED' && (
+                          <div className="mt-3 rounded-[8px] border border-[#EBC0BA] bg-[#FBE9E7] p-3 text-xs">
+                            <div className="flex items-start gap-2 text-[#B84F49]">
+                              <XCircle className="h-4 w-4 shrink-0 mt-0.5" />
+                              <div>
+                                <p className="font-bold">Return Claim Rejected</p>
+                                <p className="mt-0.5 text-[#71817A] leading-relaxed">
+                                  <strong className="text-[#17211F]">Reason from Atelier: </strong>
+                                  {returnClaim.adminNotes || 'Condition does not meet our return policy guidelines.'}
+                                </p>
+                              </div>
+                            </div>
+                          </div>
+                        )}
+
                         <div className="mt-4 grid gap-3 text-sm font-semibold text-[#71817A] sm:grid-cols-3">
@@ -244,6 +365,39 @@
                           <span>Track Shipment</span>
                         </button>
+                        {returnClaim ? (
+                          <button
+                            type="button"
+                            onClick={() => setSelectedReturnDrawer({ claim: returnClaim, order })}
+                            className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
+                          >
+                            <RotateCcw className="h-4 w-4" />
+                            <span>View Return Status</span>
+                          </button>
+                        ) : eligibility.isEligible ? (
+                          <button
+                            type="button"
+                            onClick={() => setReturnModalOrder(order)}
+                            className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
+                          >
+                            <RotateCcw className="h-4 w-4" />
+                            <span>Return / Exchange</span>
+                            <span className="rounded-full bg-[#1E6A62] px-2 py-0.5 text-[10px] font-black text-white">
+                              {eligibility.daysRemaining}d left
+                            </span>
+                          </button>
+                        ) : (
+                          <div className="group relative w-full">
+                            <button
+                              type="button"
+                              disabled
+                              aria-disabled="true"
+                              aria-describedby={`tooltip-return-${order.id}`}
+                              className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] text-xs font-bold uppercase tracking-wider text-[#9AA59F] cursor-not-allowed opacity-75"
+                            >
+                              <RotateCcw className="h-4 w-4" />
+                              <span>Return / Exchange</span>
+                            </button>
+                            <div
+                              id={`tooltip-return-${order.id}`}
+                              role="tooltip"
+                              className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden w-64 rounded-[8px] bg-[#17211F] p-2.5 text-center text-xs font-medium text-white shadow-xl group-hover:block z-30 leading-snug"
+                            >
+                              {eligibility.reason}
+                              <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#17211F]" />
+                            </div>
+                          </div>
+                        )}
                         <button onClick={() => handleReorder(order)} className="sk-button-primary w-full">
                           <RefreshCw className="h-4 w-4" />
                           Reorder
@@ -271,6 +425,24 @@
         isOpen={Boolean(trackingOrder)}
         onClose={() => setTrackingOrder(null)}
         order={trackingOrder}
       />
+
+      <ReturnRequestModal
+        isOpen={Boolean(returnModalOrder)}
+        onClose={() => setReturnModalOrder(null)}
+        order={returnModalOrder}
+        onSuccess={(newClaim) => {
+          setReturnModalOrder(null);
+          loadReturnClaims();
+          if (newClaim) {
+            setSelectedReturnDrawer({ claim: newClaim, order: returnModalOrder });
+          }
+        }}
+      />
+
+      <ReturnStatusDrawer
+        isOpen={Boolean(selectedReturnDrawer)}
+        onClose={() => setSelectedReturnDrawer(null)}
+        returnClaim={selectedReturnDrawer?.claim}
+        order={selectedReturnDrawer?.order}
+        onRefresh={loadReturnClaims}
+      />
     </div>
   );
 }
```

---

## 6. Verification and Acceptance Criteria Mapping

| Acceptance Criteria Item | Verification Method | Expected Outcome |
| :--- | :--- | :--- |
| **7-Day Eligibility Check** | Test with orders: delivered 2 days ago, delivered 8 days ago, `SHIPPED`, `CANCELLED` | Delivered $\le 7$ days renders active button with `Xd left` badge. Non-delivered or $> 7$ days renders disabled button with tooltip. |
| **Accessible Tooltip** | Hover on disabled button; inspect DOM for `role="tooltip"`, `aria-describedby` | Tooltip appears on hover with clear text: "Order must be delivered..." or "Return window expired...". Accessible to assistive technology. |
| **Return Status Telemetry** | Order with claim in `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `COMPLETED`, `REJECTED` | Appropriate badge pill rendered in card header. `PICKUP_SCHEDULED` shows reverse courier and AWB. |
| **Rejection Alert Box** | Order with `REJECTED` status | Rose alert box displays admin notes verbatim with explanatory prompt. |
| **"View Return Status" Action** | Click "View Return Status" button on card | Opens `ReturnStatusDrawer` with populated claim data. |
| **6-Stage Milestone Tracker** | Inspect milestones in drawer | Progress steps illuminate correctly: Step 1 (`Requested`), Step 2 (`Approved`), Step 3 (`Pickup Scheduled`), Step 4 (`Picked Up`), Step 5 (`In Transit`), Step 6 (`Completed / Refunded`). |
| **Reverse Courier & AWB Tracking** | Click copy button next to AWB | Copies tracking number to clipboard and displays "Copied!" for 2 seconds. |
| **Defect Photos Lightbox** | Click any photo in drawer gallery | Full-resolution modal lightbox displays photo with close option on backdrop click or Escape. |
| **Concierge WhatsApp Integration** | Click "Concierge Assistance" | Navigates to `wa.me/919059564499` with pre-filled claim ID, order ID, and status message. |
| **Bundle Budget** | Run `npm run build` | Chunk size strictly below 500 kB budget (SVG icons and Tailwind CSS only, no bulky external dependencies). |
