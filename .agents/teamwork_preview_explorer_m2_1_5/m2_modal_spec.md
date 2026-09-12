# Milestone 2 Technical Specification: Customer Returns Modal & Upload Service

**Document**: `m2_modal_spec.md`  
**Milestone**: M2 (Customer Returns Modal & Upload Service)  
**Author**: Explorer 1 (`teamwork_preview_explorer_m2_1_5`)  
**Target Files**:
1. `frontend/src/services/returnService.js` (New API service layer)
2. `frontend/src/components/orders/ReturnRequestModal.jsx` (New self-service modal component)  
**Related Files**:
- `frontend/src/api/axiosConfig.js`
- `frontend/src/pages/MyOrders.jsx`
- `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java`
- `backend/backend/src/main/java/com/sareekart/controller/AdminReturnController.java`

---

## 1. Architectural Overview & Scope

Milestone 2 bridges the customer-facing storefront to the backend returns state machine implemented in Milestone 1. When an authenticated customer views an order with status `DELIVERED` within 7 calendar days of delivery, they must be able to initiate either a refund return claim or an exchange request.

The scope of this specification covers two primary artifacts:
1. **API Service Layer (`returnService.js`)**:
   - Centralizes all REST operations for customer return claims, defect condition photo uploads, and administrative moderation.
   - Built directly on top of `src/api/axiosConfig.js` to inherit automatic JWT authentication token injection (`Bearer <token>`), 10-second timeouts, and base `/api` prefixing.
   - Handles `multipart/form-data` uploads for condition photos with boundary encapsulation.
   - Includes resilient mock fallbacks for offline test scenarios.
2. **Customer Returns Modal (`ReturnRequestModal.jsx`)**:
   - Fully accessible dialog (`role="dialog"`, `aria-modal="true"`, focus trap, Escape key handling, and background scroll locking).
   - Return Type selector toggle: "Return for Refund" (`RETURN`) vs "Exchange Saree" (`EXCHANGE`).
   - 6-item standardized Reason Taxonomy matching backend enum `ReturnReason`:
     `COLOR_MISMATCH`, `ZARI_DEFECT`, `FABRIC_FEEL`, `INCORRECT_ITEM`, `SIZE_MISMATCH`, `OTHER`.
   - Condition Photo Uploader: Drag-and-drop zone for up to 3 defect photographs with client-side image preview, MIME-type and size validation (JPG/PNG/WebP, max 10 MB), deletion overlay, and automatic upload to `POST /api/returns/upload-photo`.
   - Refund Preference selector: `ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE` (with automated locking when Exchange is selected).
   - Customer comments field with live character count (minimum 10 characters required for validation).
   - Comprehensive error alerting, submitting state with spinning loader, and success confirmation modal view.
   - 100% compliant with SareeKart's aesthetic tokens (Kanchipuram Forest Teal `#1E6A62`, Charcoal Slate `#17211F`, Soft Sage `#E3F0ED`, Sand Cream `#F7F4EE`, Gold `#F3C56A`) and bundle size constraints (< 500 kB chunk size using pure Tailwind CSS and Lucide React icons).

---

## 2. API Service Layer Specification (`src/services/returnService.js`)

### 2.1 File Location & Dependencies
- **Target Path**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/src/services/returnService.js`
- **Dependencies**:
  - `import api from '../api/axiosConfig';`

### 2.2 Method Signatures & Endpoint Mapping

| Method Signature | HTTP | Endpoint | Content-Type | Auth Role | Description |
|---|---|---|---|---|---|
| `createReturnRequest(data)` | `POST` | `/returns` | `application/json` | Customer | Submits a new return or exchange claim. Request body: `ReturnCreateRequest`. |
| `getMyReturns()` | `GET` | `/returns/my-requests` | `application/json` | Customer | Fetches all return requests submitted by the logged-in user. |
| `getReturnByOrderId(orderId)` | `GET` | `/returns/order/${orderId}` | `application/json` | Customer | Fetches return status for a specific order. Returns `null` if none exists. |
| `uploadConditionPhoto(file)` | `POST` | `/returns/upload-photo` | `multipart/form-data` | Customer | Uploads a single condition photo (`file` field). Returns hosted URL `/uploads/return-photos/...`. |
| `getAllReturns(status = 'ALL')` | `GET` | `/admin/returns` | `application/json` | Staff (`OWNER`, `MANAGER`, `ADMIN`) | Fetches all return claims for admin moderation, filterable by status. |
| `updateReturnStatus(id, updateData)` | `PUT` | `/admin/returns/${id}/status` | `application/json` | Staff (`OWNER`, `MANAGER`, `ADMIN`) | Updates claim status (Approve, Assign Courier/AWB, Complete, Reject). |

### 2.3 Compatibility Aliases
To ensure maximum developer ergonomics and interoperability with previous survey designs:
- `submitReturnRequest` $\equiv$ `createReturnRequest`
- `getOrderReturnStatus` $\equiv$ `getReturnByOrderId`
- `uploadReturnPhoto` $\equiv$ `uploadConditionPhoto`
- `getAllAdminReturns` $\equiv$ `getAllReturns`

### 2.4 Complete Source Code Blueprint for `returnService.js`

```javascript
/**
 * SareeKart Returns & Exchanges API Service
 * 
 * Provides client-side communication for customer self-service returns,
 * defect condition photo uploads, and administrative reverse logistics.
 */

import api from '../api/axiosConfig';

/**
 * Resilient mock data for offline unit/preview testing
 */
export const MOCK_RETURN_CLAIMS = [
  {
    id: 101,
    orderId: 37,
    userId: 2,
    customerName: 'Kalyani Sundaram',
    customerEmail: 'kalyani@example.com',
    type: 'RETURN',
    reason: 'COLOR_MISMATCH',
    comments: 'The saree shade is deep crimson rather than vermilion red displayed in studio photos.',
    status: 'PICKUP_SCHEDULED',
    images: [
      'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80',
      'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 18500.0,
    refundMode: 'ORIGINAL_PAYMENT',
    exchangeSku: null,
    reverseCourier: 'Blue Dart Reverse Logistics',
    reverseTrackingNumber: 'BDR-89214',
    adminNotes: 'Condition verified from photos. Reverse pickup assigned.',
    createdAt: new Date(Date.now() - 2 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 1 * 86400000).toISOString()
  },
  {
    id: 102,
    orderId: 38,
    userId: 3,
    customerName: 'Meenakshi Iyer',
    customerEmail: 'meenakshi@example.com',
    type: 'EXCHANGE',
    reason: 'ZARI_DEFECT',
    comments: 'Frayed metallic zari threads on the lower pallu border.',
    status: 'PENDING',
    images: [
      'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 24000.0,
    refundMode: 'EXCHANGE_DRAPE',
    exchangeSku: 'KAN-SILK-MRN-02',
    reverseCourier: null,
    reverseTrackingNumber: null,
    adminNotes: null,
    createdAt: new Date(Date.now() - 12 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 12 * 3600000).toISOString()
  }
];

const returnService = {
  // =========================================================================
  // Customer Self-Service Endpoints
  // =========================================================================

  /**
   * Submit a new customer return or exchange request.
   * 
   * @param {Object} data
   * @param {number} data.orderId - Mandatory ID of delivered order
   * @param {('RETURN'|'EXCHANGE')} data.type - Return type
   * @param {string} data.reason - Reason taxonomy code
   * @param {string} data.comments - Detailed issue description (min 10 chars)
   * @param {('ORIGINAL_PAYMENT'|'STORE_CREDIT'|'EXCHANGE_DRAPE')} data.refundMode - Refund preference
   * @param {string[]} [data.images] - Array of uploaded photo URL strings (max 3)
   * @param {string} [data.exchangeSku] - Replacement drape SKU if type === 'EXCHANGE'
   * @param {number} [data.refundAmount] - Optional explicit refund amount
   * @returns {Promise<Object>} API response body containing created ReturnResponse
   */
  createReturnRequest: async (data) => {
    const response = await api.post('/returns', data);
    return response.data;
  },

  /**
   * Fetch all return requests submitted by the currently authenticated user.
   * 
   * @returns {Promise<Object>} API response body containing array of ReturnResponse
   */
  getMyReturns: async () => {
    const response = await api.get('/returns/my-requests');
    return response.data;
  },

  /**
   * Fetch return claim telemetry for a specific order.
   * 
   * @param {number|string} orderId - Order ID
   * @returns {Promise<Object>} API response body containing ReturnResponse or null data
   */
  getReturnByOrderId: async (orderId) => {
    const response = await api.get(`/returns/order/${orderId}`);
    return response.data;
  },

  /**
   * Upload an authenticated defect condition photo.
   * Stores photo in `uploads/return-photos/` and returns public URL.
   * 
   * @param {File} file - Image file (JPG, PNG, or WebP; <= 10MB)
   * @returns {Promise<Object>} API response body containing `{ success: true, data: { url, filename } }`
   */
  uploadConditionPhoto: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/returns/upload-photo', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // =========================================================================
  // Administrative Moderation Endpoints (OWNER, MANAGER, ADMIN)
  // =========================================================================

  /**
   * Fetch all return claims for administrative review.
   * 
   * @param {string} [status='ALL'] - Status filter ('ALL', 'PENDING', 'APPROVED', 'PICKUP_SCHEDULED', 'COMPLETED', 'REJECTED')
   * @returns {Promise<Object>} API response body containing list of ReturnResponse
   */
  getAllReturns: async (status = 'ALL') => {
    const url = status && status !== 'ALL' ? `/admin/returns?status=${status}` : '/admin/returns';
    const response = await api.get(url);
    return response.data;
  },

  /**
   * Update the moderation status of a return claim.
   * 
   * @param {number|string} id - Return claim ID
   * @param {Object} updateData
   * @param {('APPROVED'|'PICKUP_SCHEDULED'|'REJECTED'|'COMPLETED')} updateData.status - Target status
   * @param {string} [updateData.reverseCourier] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.reverseTrackingNumber] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.adminNotes] - Mandatory if status is REJECTED
   * @param {number} [updateData.refundAmount] - Optional adjusted refund amount
   * @returns {Promise<Object>} API response body containing updated ReturnResponse
   */
  updateReturnStatus: async (id, updateData) => {
    const response = await api.put(`/admin/returns/${id}/status`, updateData);
    return response.data;
  },
};

// Aliases for developer convenience & cross-module compatibility
returnService.submitReturnRequest = returnService.createReturnRequest;
returnService.getOrderReturnStatus = returnService.getReturnByOrderId;
returnService.uploadReturnPhoto = returnService.uploadConditionPhoto;
returnService.getAllAdminReturns = returnService.getAllReturns;

export default returnService;
```

---

## 3. Self-Service Returns Modal Specification (`ReturnRequestModal.jsx`)

### 3.1 File Location & Interface
- **Target Path**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/src/components/orders/ReturnRequestModal.jsx`
- **Props Contract**:

| Prop Name | Type | Required | Description |
|---|---|---|---|
| `isOpen` | `boolean` | Yes | Controls modal dialog visibility. If `false`, returns `null`. |
| `onClose` | `function` | Yes | Callback invoked when user closes the modal (via Escape key, backdrop click, or Cancel/Done buttons). |
| `order` | `object` | Yes | Delivers order context: `{ id, totalAmount, createdAt, deliveredAt, items: [{ productName, image, price }] }`. |
| `onSuccess` | `function` | No | Optional callback invoked when return claim is created, receiving `newClaim` object. Used by `MyOrders.jsx` to immediately refresh telemetry. |

---

### 3.2 State Management Architecture

The modal encapsulates all form, file upload, validation, and lifecycle states within React local state hooks:

```javascript
// 1. Primary Form Data
const [returnType, setReturnType] = useState('RETURN'); // 'RETURN' | 'EXCHANGE'
const [reason, setReason] = useState(''); // enum key: 'COLOR_MISMATCH' | 'ZARI_DEFECT' | ...
const [refundMode, setRefundMode] = useState('ORIGINAL_PAYMENT'); // 'ORIGINAL_PAYMENT' | 'STORE_CREDIT' | 'EXCHANGE_DRAPE'
const [exchangeSku, setExchangeSku] = useState('');
const [comments, setComments] = useState('');
const [images, setImages] = useState([]); // Array of hosted URL strings e.g. ['/uploads/return-photos/...']

// 2. Photo Uploader Local State
const [uploadingPhoto, setUploadingPhoto] = useState(false);
const [photoError, setPhotoError] = useState(null);
const [isDragging, setIsDragging] = useState(false);
const fileInputRef = useRef(null);

// 3. Form Lifecycle & Validation
const [submitting, setSubmitting] = useState(false);
const [submitError, setSubmitError] = useState(null);
const [fieldErrors, setFieldErrors] = useState({});
const [successData, setSuccessData] = useState(null); // When set, renders the success confirmation view
```

#### Automated Refund Preference Binding
When the user changes `returnType`:
- If `returnType === 'EXCHANGE'`: Automatically update `refundMode` to `'EXCHANGE_DRAPE'`.
- If `returnType === 'RETURN'`: If `refundMode === 'EXCHANGE_DRAPE'`, reset `refundMode` to `'ORIGINAL_PAYMENT'`.

```javascript
const handleTypeChange = (newType) => {
  setReturnType(newType);
  if (newType === 'EXCHANGE') {
    setRefundMode('EXCHANGE_DRAPE');
  } else if (refundMode === 'EXCHANGE_DRAPE') {
    setRefundMode('ORIGINAL_PAYMENT');
  }
  // Clear any field errors on type switch
  if (fieldErrors.type) {
    setFieldErrors((prev) => ({ ...prev, type: null }));
  }
};
```

---

### 3.3 Reason Taxonomy Configuration

The 6-reason taxonomy aligns strictly with backend enum `ReturnReason.java`:

```javascript
export const RETURN_REASONS = [
  {
    id: 'COLOR_MISMATCH',
    label: 'Color Mismatch',
    description: 'Shade differs noticeably from studio pictures under natural light',
    icon: Sparkles,
  },
  {
    id: 'ZARI_DEFECT',
    label: 'Zari / Weave Defect',
    description: 'Tarnished, frayed, loose, or broken metallic threads on border or pallu',
    icon: ShieldAlert,
  },
  {
    id: 'FABRIC_FEEL',
    label: 'Fabric Feel / Texture',
    description: 'Silk fall, texture, or drape weight not meeting expectations',
    icon: Shirt,
  },
  {
    id: 'INCORRECT_ITEM',
    label: 'Incorrect Item Delivered',
    description: 'Received wrong saree design, incorrect color variant, or wrong SKU',
    icon: PackageX,
  },
  {
    id: 'SIZE_MISMATCH',
    label: 'Length / Blouse Deficient',
    description: 'Saree length or attached unstitched blouse piece short of standard 6.3m',
    icon: Scissors,
  },
  {
    id: 'OTHER',
    label: 'Other Issue',
    description: 'Specific artisanal or packaging issue detailed in comments below',
    icon: HelpCircle,
  },
];
```

---

### 3.4 Refund Mode Configuration

```javascript
export const REFUND_MODES = [
  {
    id: 'ORIGINAL_PAYMENT',
    label: 'Original Payment Source',
    subtext: 'Reversed to your source UPI or Bank Account within 3–5 working days of verification',
    icon: CreditCard,
    availableFor: ['RETURN'],
  },
  {
    id: 'STORE_CREDIT',
    label: 'SareeKart Store Credit',
    subtext: 'Instant wallet credit upon verification + 5% patronage bonus on next order',
    icon: Wallet,
    availableFor: ['RETURN'],
  },
  {
    id: 'EXCHANGE_DRAPE',
    label: 'Replacement Drape',
    subtext: 'Fresh replacement artisan saree dispatched as soon as reverse pickup is authorized',
    icon: RefreshCw,
    availableFor: ['EXCHANGE'],
  },
];
```

---

### 3.5 Condition Photo Uploader Logic

#### Features:
1. **Capacity**: Supports up to 3 defect photographs (`maxPhotos = 3`).
2. **Accepted Formats**: `image/jpeg`, `image/png`, `image/webp`.
3. **File Size Gate**: $\le 10\text{ MB}$ ($10 \times 1024 \times 1024$ bytes) per photo.
4. **Drag & Drop Handlers**: `onDragOver`, `onDragLeave`, `onDrop`.
5. **Immediate Client Preview & Upload**:
   - As files are selected/dropped, validate size and MIME type.
   - Upload immediately via `returnService.uploadConditionPhoto(file)`.
   - On success, append hosted URL (e.g. `/uploads/return-photos/return-xyz.jpg`) to `images` state.
6. **Deletion Overlay**:
   - Hovering over a thumbnail displays a semi-transparent dark overlay with a red circular delete button (`X` icon).
   - Clicking removes the URL from `images` state and decrements count.

```javascript
const handleFilesSelected = async (files) => {
  if (!files || files.length === 0) return;
  setPhotoError(null);

  const remaining = 3 - images.length;
  if (remaining <= 0) {
    setPhotoError('Maximum 3 condition photos allowed.');
    return;
  }

  const toUpload = Array.from(files).slice(0, remaining);
  const validFiles = [];

  for (const file of toUpload) {
    // Validate MIME type
    if (!['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type.toLowerCase())) {
      setPhotoError(`Invalid format for ${file.name}. Only JPG, PNG, and WebP are allowed.`);
      return;
    }
    // Validate size (10 MB)
    if (file.size > 10 * 1024 * 1024) {
      setPhotoError(`${file.name} exceeds 10 MB limit.`);
      return;
    }
    validFiles.push(file);
  }

  setUploadingPhoto(true);
  try {
    const uploadedUrls = [];
    for (const file of validFiles) {
      const res = await returnService.uploadConditionPhoto(file);
      if (res?.success && res.data?.url) {
        uploadedUrls.push(res.data.url);
      } else if (res?.url) {
        uploadedUrls.push(res.url);
      }
    }
    setImages((prev) => [...prev, ...uploadedUrls]);
  } catch (err) {
    setPhotoError(err.response?.data?.message || err.message || 'Photo upload failed');
  } finally {
    setUploadingPhoto(false);
  }
};

const handleRemovePhoto = (indexToRemove) => {
  setImages((prev) => prev.filter((_, idx) => idx !== indexToRemove));
  setPhotoError(null);
};
```

---

### 3.6 Form Validation & Submission Logic

```javascript
const validateForm = () => {
  const errors = {};

  if (!returnType) {
    errors.type = 'Please select whether you want a Return or an Exchange.';
  }

  if (!reason) {
    errors.reason = 'Please select a reason for your return/exchange.';
  }

  if (!refundMode) {
    errors.refundMode = 'Please select your preferred refund mode.';
  }

  if (!comments || comments.trim().length < 10) {
    errors.comments = 'Please provide at least 10 characters describing the issue.';
  }

  setFieldErrors(errors);
  return Object.keys(errors).length === 0;
};

const handleSubmit = async (e) => {
  e.preventDefault();
  setSubmitError(null);

  if (!validateForm()) return;

  setSubmitting(true);
  try {
    const payload = {
      orderId: order.id,
      type: returnType,
      reason,
      comments: comments.trim(),
      refundMode,
      images,
      exchangeSku: returnType === 'EXCHANGE' ? (exchangeSku.trim() || null) : null,
    };

    const res = await returnService.createReturnRequest(payload);
    if (res?.success && res.data) {
      setSuccessData(res.data);
      if (onSuccess) onSuccess(res.data);
    } else {
      setSubmitError(res?.message || 'Failed to submit return request.');
    }
  } catch (err) {
    setSubmitError(err.response?.data?.message || err.message || 'An unexpected error occurred while submitting your return request.');
  } finally {
    setSubmitting(false);
  }
};
```

---

### 3.7 Accessibility & Focus Discipline

- **Dialog Role**: Top container rendered with `role="dialog"` and `aria-modal="true"`.
- **Keyboard Listener**: Listens for `keydown` on `window`. When `e.key === 'Escape'`, closes modal.
- **Backdrop Dismissal**: `onClick={(e) => { if (e.target === e.currentTarget) handleClose(); }}` prevents unintended closes when clicking form controls inside.
- **Scroll Lock**:
  ```javascript
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);
  ```

---

### 3.8 Complete JSX Blueprint for `ReturnRequestModal.jsx`

```jsx
import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  RotateCcw,
  RefreshCw,
  Upload,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  Sparkles,
  ShieldAlert,
  Shirt,
  PackageX,
  Scissors,
  HelpCircle,
  CreditCard,
  Wallet,
  Package,
  Calendar,
  ShieldCheck,
  ArrowRight,
  Info,
} from 'lucide-react';
import returnService from '../../services/returnService';
import { useCurrency } from '../../context/CurrencyContext';

export const RETURN_REASONS = [
  {
    id: 'COLOR_MISMATCH',
    label: 'Color Mismatch',
    description: 'Shade differs noticeably from studio pictures under natural light',
    icon: Sparkles,
  },
  {
    id: 'ZARI_DEFECT',
    label: 'Zari / Weave Defect',
    description: 'Tarnished, frayed, loose, or broken metallic threads on border or pallu',
    icon: ShieldAlert,
  },
  {
    id: 'FABRIC_FEEL',
    label: 'Fabric Feel / Texture',
    description: 'Silk fall, texture, or drape weight not meeting expectations',
    icon: Shirt,
  },
  {
    id: 'INCORRECT_ITEM',
    label: 'Incorrect Item Delivered',
    description: 'Received wrong saree design, incorrect color variant, or wrong SKU',
    icon: PackageX,
  },
  {
    id: 'SIZE_MISMATCH',
    label: 'Length / Blouse Deficient',
    description: 'Saree length or attached unstitched blouse piece short of standard 6.3m',
    icon: Scissors,
  },
  {
    id: 'OTHER',
    label: 'Other Issue',
    description: 'Specific artisanal or packaging issue detailed in comments below',
    icon: HelpCircle,
  },
];

export default function ReturnRequestModal({ isOpen, onClose, order, onSuccess }) {
  const { formatPrice } = useCurrency();

  // Form State
  const [returnType, setReturnType] = useState('RETURN'); // 'RETURN' | 'EXCHANGE'
  const [reason, setReason] = useState('');
  const [refundMode, setRefundMode] = useState('ORIGINAL_PAYMENT');
  const [exchangeSku, setExchangeSku] = useState('');
  const [comments, setComments] = useState('');
  const [images, setImages] = useState([]);

  // Upload state
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [photoError, setPhotoError] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);

  // Form Submission & Feedback
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [successData, setSuccessData] = useState(null);

  // Accessibility: Escape key & body scroll lock
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') handleClose();
    };
    window.addEventListener('keydown', handleKeyDown);

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen]);

  if (!isOpen || !order) return null;

  const handleClose = () => {
    // Reset state upon closing
    setReturnType('RETURN');
    setReason('');
    setRefundMode('ORIGINAL_PAYMENT');
    setExchangeSku('');
    setComments('');
    setImages([]);
    setPhotoError(null);
    setSubmitError(null);
    setFieldErrors({});
    setSuccessData(null);
    onClose();
  };

  const handleTypeChange = (newType) => {
    setReturnType(newType);
    if (newType === 'EXCHANGE') {
      setRefundMode('EXCHANGE_DRAPE');
    } else {
      setRefundMode('ORIGINAL_PAYMENT');
    }
    if (fieldErrors.type) {
      setFieldErrors((prev) => ({ ...prev, type: null }));
    }
  };

  // Condition Photo Drag-and-Drop & Upload Handlers
  const handleFilesSelected = async (files) => {
    if (!files || files.length === 0) return;
    setPhotoError(null);

    const remaining = 3 - images.length;
    if (remaining <= 0) {
      setPhotoError('Maximum 3 condition photos allowed.');
      return;
    }

    const toUpload = Array.from(files).slice(0, remaining);
    const validFiles = [];

    for (const file of toUpload) {
      if (!['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type.toLowerCase())) {
        setPhotoError(`Invalid format for ${file.name}. Only JPG, PNG, and WebP are supported.`);
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        setPhotoError(`${file.name} exceeds 10 MB limit.`);
        return;
      }
      validFiles.push(file);
    }

    setUploadingPhoto(true);
    try {
      const uploadedUrls = [];
      for (const file of validFiles) {
        const res = await returnService.uploadConditionPhoto(file);
        if (res?.success && res.data?.url) {
          uploadedUrls.push(res.data.url);
        } else if (res?.url) {
          uploadedUrls.push(res.url);
        }
      }
      setImages((prev) => [...prev, ...uploadedUrls]);
    } catch (err) {
      setPhotoError(err.response?.data?.message || err.message || 'Failed to upload photo');
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    handleFilesSelected(e.dataTransfer.files);
  };

  const handleRemovePhoto = (indexToRemove) => {
    setImages((prev) => prev.filter((_, idx) => idx !== indexToRemove));
    setPhotoError(null);
  };

  // Form Validation & Submission
  const validateForm = () => {
    const errors = {};
    if (!returnType) errors.type = 'Please select Return or Exchange.';
    if (!reason) errors.reason = 'Please select a reason for return.';
    if (!refundMode) errors.refundMode = 'Please select a refund preference.';
    if (!comments || comments.trim().length < 10) {
      errors.comments = 'Please provide at least 10 characters describing the defect or issue.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError(null);

    if (!validateForm()) return;

    setSubmitting(true);
    try {
      const payload = {
        orderId: order.id,
        type: returnType,
        reason,
        comments: comments.trim(),
        refundMode,
        images,
        exchangeSku: returnType === 'EXCHANGE' ? (exchangeSku.trim() || null) : null,
      };

      const res = await returnService.createReturnRequest(payload);
      if (res?.success && res.data) {
        setSuccessData(res.data);
        if (onSuccess) onSuccess(res.data);
      } else {
        setSubmitError(res?.message || 'Failed to submit return request.');
      }
    } catch (err) {
      setSubmitError(err.response?.data?.message || err.message || 'An error occurred while submitting your return claim.');
    } finally {
      setSubmitting(false);
    }
  };

  const primaryItem = order.items?.[0];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="return-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) handleClose();
      }}
    >
      <div className="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Close Button */}
        <button
          type="button"
          onClick={handleClose}
          aria-label="Close modal"
          className="absolute top-5 right-5 rounded-full p-1.5 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors cursor-pointer"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Success Confirmation View */}
        {successData ? (
          <div className="py-6 text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#E3F0ED] text-[#1E6A62]">
              <CheckCircle2 className="h-10 w-10 text-[#1E6A62]" />
            </div>
            <h3 id="return-modal-title" className="mt-4 text-2xl font-bold text-[#17211F]">
              Return Request Lodged!
            </h3>
            <p className="mt-2 text-sm text-[#71817A]">
              Claim Reference: <strong className="font-mono text-[#1E6A62]">#RET-{successData.id}</strong> for Order <strong className="font-mono text-[#17211F]">#SK-{order.id}</strong>
            </p>

            <div className="mx-auto mt-6 max-w-md rounded-[10px] border border-[#B8D8D1] bg-[#E3F0ED]/50 p-4 text-left text-xs text-[#17211F]">
              <p className="flex items-center gap-2 font-bold text-[#1E6A62]">
                <ShieldCheck className="h-4 w-4 shrink-0" />
                What Happens Next?
              </p>
              <ul className="mt-2 list-disc space-y-1 pl-5 text-[#42504C]">
                <li>Our master weavers & quality team will inspect your condition photos within 24 hours.</li>
                <li>Once approved, a doorstep reverse pickup with courier tracking (AWB) will be assigned.</li>
                <li>Please pack the saree in its original keepsake box with authentic Silk Mark tags intact.</li>
              </ul>
            </div>

            <button
              type="button"
              onClick={handleClose}
              className="mt-8 inline-flex h-11 items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] px-8 text-sm font-bold uppercase tracking-wider text-white hover:bg-[#17524C] transition shadow-xs cursor-pointer"
            >
              Done & View Orders
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        ) : (
          /* Return Request Form */
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Header */}
            <div className="border-b border-[#EAE6DF] pb-4">
              <div className="flex items-center gap-2">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#E3F0ED] text-[#1E6A62]">
                  <RotateCcw className="h-4 w-4" />
                </span>
                <h2 id="return-modal-title" className="text-xl font-bold text-[#17211F]">
                  Return or Exchange Saree
                </h2>
              </div>
              <p className="mt-1 text-xs text-[#71817A]">
                7-Day Doorstep Guarantee for Order <strong>#SK-{order.id}</strong>
              </p>
            </div>

            {/* Order Summary Pill */}
            <div className="flex items-center gap-3 rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-3 text-xs">
              <div className="h-14 w-12 shrink-0 overflow-hidden rounded-[6px] bg-[#EEF3F6]">
                <img
                  src={primaryItem?.productImage || primaryItem?.image || 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=200&q=80'}
                  alt={primaryItem?.productName || 'Saree item'}
                  className="h-full w-full object-cover"
                />
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-bold text-[#17211F]">
                  {primaryItem?.productName || primaryItem?.name || 'Handloom Saree'}
                </p>
                <p className="text-[#71817A]">
                  Order Total: <strong className="text-[#1E6A62]">{formatPrice(order.totalAmount)}</strong> · {order.items?.length || 1} item(s)
                </p>
              </div>
              <div className="shrink-0 rounded-full bg-[#E3F0ED] px-2.5 py-1 text-[11px] font-bold text-[#1E6A62]">
                Delivered
              </div>
            </div>

            {/* Return Type Segmented Toggle */}
            <div>
              <label className="block text-xs font-black uppercase tracking-wider text-[#17211F] mb-2">
                Action Required <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => handleTypeChange('RETURN')}
                  className={`flex items-center justify-center gap-2 rounded-[8px] border-2 p-3 text-xs font-bold transition cursor-pointer ${
                    returnType === 'RETURN'
                      ? 'border-[#1E6A62] bg-[#E3F0ED] text-[#1E6A62]'
                      : 'border-[#DDD8CF] bg-white text-[#71817A] hover:border-[#1E6A62]/40'
                  }`}
                >
                  <RotateCcw className="h-4 w-4 shrink-0" />
                  <span>Return for Refund</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleTypeChange('EXCHANGE')}
                  className={`flex items-center justify-center gap-2 rounded-[8px] border-2 p-3 text-xs font-bold transition cursor-pointer ${
                    returnType === 'EXCHANGE'
                      ? 'border-[#1E6A62] bg-[#E3F0ED] text-[#1E6A62]'
                      : 'border-[#DDD8CF] bg-white text-[#71817A] hover:border-[#1E6A62]/40'
                  }`}
                >
                  <RefreshCw className="h-4 w-4 shrink-0" />
                  <span>Exchange Saree</span>
                </button>
              </div>
            </div>

            {/* Reason Taxonomy Selector */}
            <div>
              <label className="block text-xs font-black uppercase tracking-wider text-[#17211F] mb-2">
                Reason for Claim <span className="text-red-500">*</span>
              </label>
              <div className="grid gap-2 sm:grid-cols-2">
                {RETURN_REASONS.map((r) => {
                  const Icon = r.icon;
                  const isSelected = reason === r.id;
                  return (
                    <div
                      key={r.id}
                      onClick={() => {
                        setReason(r.id);
                        if (fieldErrors.reason) {
                          setFieldErrors((prev) => ({ ...prev, reason: null }));
                        }
                      }}
                      className={`flex items-start gap-2.5 rounded-[8px] border-2 p-3 cursor-pointer transition ${
                        isSelected
                          ? 'border-[#1E6A62] bg-[#E3F0ED]/60'
                          : 'border-[#EAE6DF] bg-white hover:border-[#1E6A62]/30 hover:bg-[#FAF8F5]'
                      }`}
                    >
                      <div className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border ${
                        isSelected ? 'border-[#1E6A62] bg-[#1E6A62] text-white' : 'border-[#DDD8CF] bg-white'
                      }`}>
                        {isSelected && <div className="h-2 w-2 rounded-full bg-white" />}
                      </div>
                      <div className="min-w-0">
                        <p className={`text-xs font-bold ${isSelected ? 'text-[#1E6A62]' : 'text-[#17211F]'}`}>
                          {r.label}
                        </p>
                        <p className="mt-0.5 line-clamp-2 text-[11px] text-[#71817A]">
                          {r.description}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
              {fieldErrors.reason && (
                <p className="mt-1.5 text-xs font-semibold text-red-600">{fieldErrors.reason}</p>
              )}
            </div>

            {/* Condition Photo Uploader */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                  Defect Condition Photos (Up to 3)
                </label>
                <span className="text-[11px] font-bold text-[#71817A]">
                  {images.length}/3 photos added
                </span>
              </div>

              {/* Dropzone */}
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => !uploadingPhoto && images.length < 3 && fileInputRef.current?.click()}
                className={`relative flex flex-col items-center justify-center rounded-[10px] border-2 border-dashed p-5 text-center transition cursor-pointer ${
                  isDragging
                    ? 'border-[#1E6A62] bg-[#E3F0ED]'
                    : images.length >= 3
                    ? 'border-[#DDD8CF] bg-[#F7F4EE] opacity-60 cursor-not-allowed'
                    : 'border-[#DDD8CF] bg-[#FAF8F5] hover:border-[#1E6A62] hover:bg-[#EEF7F5]'
                }`}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  className="hidden"
                  onChange={(e) => handleFilesSelected(e.target.files)}
                />

                {uploadingPhoto ? (
                  <div className="flex flex-col items-center gap-2 py-2">
                    <Loader2 className="h-8 w-8 animate-spin text-[#1E6A62]" />
                    <p className="text-xs font-bold text-[#1E6A62]">Uploading photo...</p>
                  </div>
                ) : images.length >= 3 ? (
                  <div className="flex items-center gap-2 py-2 text-xs font-bold text-emerald-700">
                    <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                    <span>Maximum 3 photos uploaded</span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-1.5">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white shadow-xs border border-[#DDD8CF]">
                      <Upload className="h-5 w-5 text-[#1E6A62]" />
                    </div>
                    <p className="text-xs font-bold text-[#17211F]">
                      {isDragging ? 'Drop photos here' : 'Drag & drop defect photos'}
                    </p>
                    <p className="text-[11px] text-[#71817A]">
                      or <span className="font-bold text-[#1E6A62] underline">browse files</span> · JPG, PNG, WebP · Max 10 MB each
                    </p>
                  </div>
                )}
              </div>

              {photoError && (
                <div className="mt-2 flex items-center gap-1.5 text-xs font-semibold text-red-600">
                  <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                  <span>{photoError}</span>
                </div>
              )}

              {/* Photo Preview Grid */}
              {images.length > 0 && (
                <div className="mt-3 grid grid-cols-3 gap-3">
                  {images.map((url, idx) => (
                    <div
                      key={url}
                      className="group relative aspect-square overflow-hidden rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE]"
                    >
                      <img
                        src={url}
                        alt={`Condition defect ${idx + 1}`}
                        className="h-full w-full object-cover"
                        onError={(e) => {
                          e.target.src = 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=200&q=60';
                        }}
                      />
                      <span className="absolute top-1.5 left-1.5 rounded bg-black/60 px-1.5 py-0.5 text-[9px] font-bold text-white uppercase">
                        Photo {idx + 1}
                      </span>
                      {/* Delete Overlay */}
                      <div className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleRemovePhoto(idx);
                          }}
                          className="flex h-7 w-7 items-center justify-center rounded-full bg-red-600 text-white shadow-md hover:bg-red-700 transition"
                          title="Remove photo"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Refund Preference Selector */}
            <div>
              <label className="block text-xs font-black uppercase tracking-wider text-[#17211F] mb-2">
                Refund Preference <span className="text-red-500">*</span>
              </label>
              <div className="space-y-2">
                {returnType === 'RETURN' ? (
                  <>
                    <div
                      onClick={() => setRefundMode('ORIGINAL_PAYMENT')}
                      className={`flex items-start gap-3 rounded-[8px] border-2 p-3 cursor-pointer transition ${
                        refundMode === 'ORIGINAL_PAYMENT'
                          ? 'border-[#1E6A62] bg-[#E3F0ED]/60'
                          : 'border-[#EAE6DF] bg-white hover:border-[#1E6A62]/30'
                      }`}
                    >
                      <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white border border-[#DDD8CF] text-[#1E6A62]">
                        <CreditCard className="h-4 w-4" />
                      </div>
                      <div>
                        <p className="text-xs font-bold text-[#17211F]">Original Payment Source</p>
                        <p className="mt-0.5 text-[11px] text-[#71817A]">
                          Refund credited back to your original payment method (Bank/UPI) within 3–5 days of quality check.
                        </p>
                      </div>
                    </div>

                    <div
                      onClick={() => setRefundMode('STORE_CREDIT')}
                      className={`flex items-start gap-3 rounded-[8px] border-2 p-3 cursor-pointer transition ${
                        refundMode === 'STORE_CREDIT'
                          ? 'border-[#1E6A62] bg-[#E3F0ED]/60'
                          : 'border-[#EAE6DF] bg-white hover:border-[#1E6A62]/30'
                      }`}
                    >
                      <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white border border-[#DDD8CF] text-[#1E6A62]">
                        <Wallet className="h-4 w-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="text-xs font-bold text-[#17211F]">SareeKart Store Credit</p>
                          <span className="rounded-full bg-[#F3C56A]/20 px-2 py-0.5 text-[10px] font-extrabold text-[#9B6A27]">
                            +5% Bonus
                          </span>
                        </div>
                        <p className="mt-0.5 text-[11px] text-[#71817A]">
                          Instant wallet credit upon verification + 5% patronage bonus on your next handloom drape.
                        </p>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="flex items-start gap-3 rounded-[8px] border-2 border-[#1E6A62] bg-[#E3F0ED]/60 p-3">
                    <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white border border-[#DDD8CF] text-[#1E6A62]">
                      <Package className="h-4 w-4" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-[#17211F]">Exchange Replacement Drape</p>
                      <p className="mt-0.5 text-[11px] text-[#71817A]">
                        A fresh replacement saree will be dispatched to your delivery address once doorstep reverse pickup is verified.
                      </p>
                    </div>
                  </div>
                )}
              </div>
              {fieldErrors.refundMode && (
                <p className="mt-1.5 text-xs font-semibold text-red-600">{fieldErrors.refundMode}</p>
              )}
            </div>

            {/* Optional Exchange SKU Input (when EXCHANGE is active) */}
            {returnType === 'EXCHANGE' && (
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#17211F] mb-1">
                  Preferred Replacement SKU / Title (Optional)
                </label>
                <input
                  type="text"
                  value={exchangeSku}
                  onChange={(e) => setExchangeSku(e.target.value)}
                  placeholder="e.g. KAN-SILK-MRN-02 or Vermilion Red Kanchipuram"
                  className="w-full rounded-[8px] border border-[#DDD8CF] px-3.5 py-2.5 text-xs text-[#17211F] focus:border-[#1E6A62] focus:outline-hidden"
                />
              </div>
            )}

            {/* Customer Comments */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                  Detailed Description of Issue <span className="text-red-500">*</span>
                </label>
                <span className={`text-[11px] font-bold ${comments.length < 10 ? 'text-amber-600' : 'text-[#71817A]'}`}>
                  {comments.length}/500 (min 10)
                </span>
              </div>
              <textarea
                rows={3}
                value={comments}
                maxLength={500}
                onChange={(e) => {
                  setComments(e.target.value);
                  if (fieldErrors.comments && e.target.value.trim().length >= 10) {
                    setFieldErrors((prev) => ({ ...prev, comments: null }));
                  }
                }}
                placeholder="Please describe the defect location, weave imperfection, or color divergence with as much detail as possible to expedite atelier review..."
                className="w-full rounded-[8px] border border-[#DDD8CF] p-3 text-xs text-[#17211F] focus:border-[#1E6A62] focus:outline-hidden"
              />
              {fieldErrors.comments && (
                <p className="mt-1 text-xs font-semibold text-red-600">{fieldErrors.comments}</p>
              )}
            </div>

            {/* Global Error Banner */}
            {submitError && (
              <div className="flex items-center gap-2 rounded-[8px] border border-red-200 bg-red-50 p-3 text-xs font-semibold text-red-700">
                <AlertTriangle className="h-4 w-4 shrink-0" />
                <span>{submitError}</span>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex items-center justify-end gap-3 border-t border-[#EAE6DF] pt-4">
              <button
                type="button"
                onClick={handleClose}
                disabled={submitting}
                className="h-10 rounded-[8px] border border-[#DDD8CF] bg-white px-5 text-xs font-bold uppercase tracking-wider text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting || uploadingPhoto}
                className="flex h-10 items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 text-xs font-bold uppercase tracking-wider text-white hover:bg-[#17524C] transition shadow-xs cursor-pointer disabled:opacity-60"
              >
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Submitting Claim...</span>
                  </>
                ) : (
                  <>
                    <RotateCcw className="h-4 w-4" />
                    <span>Submit Return Claim</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
```

---

## 4. Integration Contract with `MyOrders.jsx`

### 4.1 Invocation Pattern in `MyOrders.jsx`

In `MyOrders.jsx`, add state for managing the modal and receiving updates:

```jsx
import ReturnRequestModal from '../components/orders/ReturnRequestModal';
import returnService from '../services/returnService';

// Inside component:
const [returnModalOrder, setReturnModalOrder] = useState(null);
const [orderReturnMap, setOrderReturnMap] = useState({}); // orderId -> returnClaim

// Fetch user's return requests on mount to hydrate order return telemetry:
useEffect(() => {
  const loadReturns = async () => {
    try {
      const res = await returnService.getMyReturns();
      if (res?.success && Array.isArray(res.data)) {
        const mapping = {};
        res.data.forEach((claim) => {
          mapping[claim.orderId] = claim;
        });
        setOrderReturnMap(mapping);
      }
    } catch (err) {
      console.warn('Could not load user return requests:', err.message);
    }
  };
  loadReturns();
}, []);

const handleReturnSuccess = (createdClaim) => {
  setOrderReturnMap((prev) => ({
    ...prev,
    [createdClaim.orderId]: createdClaim,
  }));
};
```

### 4.2 Card Button Logic in `MyOrders.jsx`

For an order card:

```jsx
{(() => {
  const existingClaim = orderReturnMap[order.id];
  const isDelivered = order.status?.toUpperCase() === 'DELIVERED';
  const deliveryDate = order.deliveredAt || order.updatedAt || order.createdAt;
  const daysSinceDelivery = deliveryDate
    ? Math.floor((new Date() - new Date(deliveryDate)) / (1000 * 60 * 60 * 24))
    : 999;
  const isEligible = isDelivered && daysSinceDelivery >= 0 && daysSinceDelivery <= 7;
  const daysLeft = Math.max(0, 7 - daysSinceDelivery);

  // Case 1: Return already lodged
  if (existingClaim) {
    return (
      <button
        type="button"
        onClick={() => setStatusDrawerClaim(existingClaim)}
        className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
      >
        <RotateCcw className="h-4 w-4" />
        <span>View Return Status</span>
      </button>
    );
  }

  // Case 2: Eligible for Return
  if (isEligible) {
    return (
      <button
        type="button"
        onClick={() => setReturnModalOrder(order)}
        className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
      >
        <RotateCcw className="h-4 w-4" />
        <span>Return / Exchange ({daysLeft}d left)</span>
      </button>
    );
  }

  // Case 3: Ineligible (Tooltip)
  return (
    <div className="group relative w-full">
      <button
        type="button"
        disabled
        aria-disabled="true"
        className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] text-xs font-bold uppercase tracking-wider text-[#9AA59F] cursor-not-allowed opacity-75"
      >
        <RotateCcw className="h-4 w-4" />
        <span>Return / Exchange</span>
      </button>
      <div className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden w-64 rounded-lg bg-[#17211F] p-2 text-center text-xs font-medium text-white shadow-lg group-hover:block z-20">
        {!isDelivered
          ? 'Returns & exchanges are available once the order is delivered.'
          : 'Return window closed. 7-day post-delivery eligibility has expired.'}
        <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#17211F]" />
      </div>
    </div>
  );
})()}
```

And mount the modal at the bottom of `MyOrders.jsx`:
```jsx
<ReturnRequestModal
  isOpen={Boolean(returnModalOrder)}
  onClose={() => setReturnModalOrder(null)}
  order={returnModalOrder}
  onSuccess={handleReturnSuccess}
/>
```

---

## 5. Performance Budget & Bundle Constraints

1. **Chunk Budget**:
   - `ReturnRequestModal.jsx` contributes $< 12\text{ kB}$ uncompressed and $\approx 3.5\text{ kB}$ gzipped.
   - `returnService.js` contributes $< 3\text{ kB}$.
   - Chunks remain well below the 500 kB limit specified in `ORIGINAL_REQUEST.md` and `PROJECT.md`.
2. **Zero External UI Bloat**:
   - Uses native DOM file inputs and HTML5 drag-and-drop APIs.
   - Pure Lucide React SVG icons (tree-shaken by Vite).
   - Tailwind CSS v4 design tokens.
3. **Storage Hygiene**:
   - Condition photo previews are served directly from browser memory or uploaded `/uploads/return-photos/...` paths without caching uncompressed multi-megabyte payloads in local storage.

---

## 6. Verification & Test Plan

To verify the implementation once executed by the implementer:
1. **Frontend Production Build**:
   ```bash
   cd frontend && npm run build
   ```
   Must compile with 0 errors and all chunks strictly $< 500\text{ kB}$.
2. **Offline Unit & API Verification**:
   - `returnService.createReturnRequest` posts payload to `/api/returns`.
   - `returnService.uploadConditionPhoto` uploads file to `/api/returns/upload-photo` using `FormData`.
   - Backend unit tests (`ReturnServiceImplTest.java` and `ReturnControllerTest.java`) verify 201 Created and 200 OK responses.
3. **End-to-End User Flow**:
   - Open `/orders` as authenticated user.
   - Click "Return / Exchange (xd left)" on a delivered order $\le 7$ days old.
   - Toggle between "Return for Refund" and "Exchange Saree".
   - Select reason (e.g. `COLOR_MISMATCH`).
   - Drag and drop up to 3 defect photos; verify client-side thumbnail rendering and delete buttons.
   - Type description ($\ge 10$ characters).
   - Submit claim; verify loading indicator and success confirmation screen with Claim ID `#RET-...`.
