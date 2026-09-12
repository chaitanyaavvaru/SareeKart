import { useState, useEffect, useRef, useCallback } from 'react';
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
  ShieldCheck,
  ArrowRight,
} from 'lucide-react';
import returnService from '../../services/returnService';
import { useCurrency } from '../../context/CurrencyContext';

const RETURN_REASONS = [
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
  const primaryItem = order?.items?.[0];

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

  const handleClose = useCallback(() => {
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
  }, [onClose]);

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
  }, [isOpen, handleClose]);

  if (!isOpen || !order) return null;

  const handleTypeChange = (newType) => {
    setReturnType(newType);
    if (newType === 'EXCHANGE') {
      setRefundMode('EXCHANGE_DRAPE');
      if (!exchangeSku.trim()) {
        const defaultSku = primaryItem?.sku || primaryItem?.productSku || primaryItem?.productName || primaryItem?.name || '';
        if (defaultSku) {
          setExchangeSku(defaultSku);
        }
      }
    } else {
      setRefundMode('ORIGINAL_PAYMENT');
    }
    if (fieldErrors.type) {
      setFieldErrors((prev) => ({ ...prev, type: null }));
    }
    if (fieldErrors.exchangeSku) {
      setFieldErrors((prev) => ({ ...prev, exchangeSku: null }));
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
        } else if (res?.data && typeof res.data === 'string') {
          uploadedUrls.push(res.data);
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
    if (returnType === 'EXCHANGE' && (!exchangeSku || !exchangeSku.trim())) {
      errors.exchangeSku = 'Exchange SKU or preferred replacement saree title is required for exchange requests.';
    }
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
      } else if (res?.id) {
        setSuccessData(res);
        if (onSuccess) onSuccess(res);
      } else {
        setSubmitError(res?.message || 'Failed to submit return request.');
      }
    } catch (err) {
      setSubmitError(err.response?.data?.message || err.message || 'An error occurred while submitting your return claim.');
    } finally {
      setSubmitting(false);
    }
  };

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
              <span>Done & View Orders</span>
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
                          className="flex h-7 w-7 items-center justify-center rounded-full bg-red-600 text-white shadow-md hover:bg-red-700 transition cursor-pointer"
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

            {/* Preferred Replacement SKU / Title (Required when EXCHANGE is active) */}
            {returnType === 'EXCHANGE' && (
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#17211F] mb-1">
                  Preferred Replacement SKU / Title <span className="text-red-500">*</span>
                </label>
                {order.items && order.items.length > 0 && (
                  <div className="mb-2 flex flex-wrap items-center gap-1.5">
                    <span className="text-[11px] text-[#71817A]">Quick select:</span>
                    {order.items.map((item, idx) => {
                      const itemTitle = item.productName || item.name || item.sku || `Item #${idx + 1}`;
                      const skuVal = item.sku || itemTitle;
                      return (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setExchangeSku(skuVal);
                            if (fieldErrors.exchangeSku) {
                              setFieldErrors((prev) => ({ ...prev, exchangeSku: null }));
                            }
                          }}
                          className={`rounded-full border px-2.5 py-0.5 text-[11px] font-medium transition cursor-pointer ${
                            exchangeSku === skuVal
                              ? 'border-[#1E6A62] bg-[#E3F0ED] text-[#1E6A62] font-bold'
                              : 'border-[#DDD8CF] bg-white text-[#42504C] hover:border-[#1E6A62]'
                          }`}
                        >
                          Same item ({itemTitle})
                        </button>
                      );
                    })}
                  </div>
                )}
                <input
                  type="text"
                  value={exchangeSku}
                  onChange={(e) => {
                    setExchangeSku(e.target.value);
                    if (fieldErrors.exchangeSku && e.target.value.trim()) {
                      setFieldErrors((prev) => ({ ...prev, exchangeSku: null }));
                    }
                  }}
                  placeholder="e.g. KAN-SILK-MRN-02 or Vermilion Red Kanchipuram"
                  className={`w-full rounded-[8px] border px-3.5 py-2.5 text-xs text-[#17211F] focus:outline-hidden ${
                    fieldErrors.exchangeSku
                      ? 'border-red-500 focus:border-red-500'
                      : 'border-[#DDD8CF] focus:border-[#1E6A62]'
                  }`}
                />
                {fieldErrors.exchangeSku ? (
                  <p className="mt-1 text-xs font-semibold text-red-600">{fieldErrors.exchangeSku}</p>
                ) : (
                  <p className="mt-1 text-[11px] text-[#71817A]">
                    Specify the replacement saree name or SKU you would like dispatched in exchange.
                  </p>
                )}
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
