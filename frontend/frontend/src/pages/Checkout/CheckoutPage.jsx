import { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { 
  MapPin, 
  CreditCard, 
  ShieldCheck, 
  ArrowRight, 
  ArrowLeft, 
  Loader2, 
  ShoppingBag,
  Truck,
  Check
} from 'lucide-react';
import { clearCart } from '../../redux/slices/cartSlice';
import api from '../../api/axiosConfig';
import SafeImage from '../../components/common/SafeImage';

function trackAnalytics(event, payload = {}) {
  try {
    const entry = { event, payload, ts: new Date().toISOString() };
    // console for dev visibility
    if (typeof window !== 'undefined' && window.console) console.debug(`[analytics] ${event}`, payload);
    const key = 'sareekart_analytics';
    const prev = JSON.parse(localStorage.getItem(key) || '[]');
    prev.push(entry);
    // keep last 100
    if (prev.length > 100) prev.splice(0, prev.length - 100);
    localStorage.setItem(key, JSON.stringify(prev));
  } catch { /* ignore */ }
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  
  const { user } = useSelector(state => state.auth);
  const { items } = useSelector(state => state.cart);
  const [deliveryWindow] = useState(() => {
    const start = new Date(Date.now() + 4*24*60*60*1000).toLocaleDateString('en-IN', { day: 'numeric', month: 'short'});
    const end = new Date(Date.now() + 6*24*60*60*1000).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric'});
    return `${start} – ${end}`;
  });
  
  // Checkout Steps: 'syncing' | 'address' | 'payment' | 'submitting' | 'success'
  const [step, setStep] = useState('syncing');
  const [syncError, setSyncError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  
  // Form Fields
  const [fullName, setFullName] = useState(user ? `${user.firstName} ${user.lastName || ''}`.trim() : '');
  const [phone, setPhone] = useState('');
  const [streetAddress, setStreetAddress] = useState('');
  const [city, setCity] = useState('');
  const [stateName, setStateName] = useState('');
  const [pincode, setPincode] = useState('');
  const [country] = useState('India');
  
  // Validation Errors
  const [validationErrors, setValidationErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  
  // Order Success Information
  const [placedOrder, setPlacedOrder] = useState(null);
  
  // Selected Payment Method: 'COD' | 'RAZORPAY'
  const [paymentMethod, setPaymentMethod] = useState('COD');

  // Coupon promo code states
  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponError, setCouponError] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);

  // Calculate pricing
  const subtotal = items.reduce((sum, item) => sum + (item.price * item.qty), 0);
  // SERVER PREVIEW is the display source of truth for coupon discounts.
  // The client never computes money values for a applied coupon.
  const discountAmount = appliedCoupon?.valid
    ? Number(appliedCoupon.discount)
    : 0;
  const taxAmount = Math.round((subtotal - discountAmount) * 0.05); // 5% GST
  const shippingFee = subtotal >= 5000 ? 0 : 150;
  const grandTotal = subtotal - discountAmount + taxAmount + shippingFee;

  const handleApplyCoupon = async (e) => {
    e.preventDefault();
    const code = couponInput.trim().toUpperCase();
    if (!code) return;
    if (!/^[A-Z0-9_-]{1,50}$/.test(code)) { setCouponError('Code: A-Z, 0-9, hyphen, underscore only'); return; }
    trackAnalytics('coupon_apply_attempt', { code });
    try {
      setCouponLoading(true);
      setCouponError(null);
      const res = await api.post(`/coupons/preview`, { code });
      const data = res.data?.data;
      if (res.data?.success && data?.valid) {
        setAppliedCoupon(data);
        trackAnalytics('coupon_apply_success', { code, discount: data.discount });
      } else {
        const msg = data?.message || 'Invalid coupon code.';
        setCouponError(msg);
        trackAnalytics('coupon_apply_failed', { code, reason: msg });
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid coupon code.';
      setCouponError(msg);
      trackAnalytics('coupon_apply_failed', { code, reason: msg });
    } finally {
      setCouponLoading(false);
    }
  };

  const handleRemoveCoupon = () => {
    const code = appliedCoupon?.code;
    setAppliedCoupon(null);
    setCouponInput('');
    setCouponError(null);
    if (code) trackAnalytics('coupon_removed', { code });
  };

  // Sync local Redux cart items with DB on mount
  useEffect(() => {
    trackAnalytics('checkout_step_view', { step });
  }, [step]);

  useEffect(() => {
    if (items.length === 0) {
      if (step !== 'success') {
        navigate('/products');
      }
      return;
    }

    const syncCartWithDatabase = async () => {
      try {
        setSyncError(null);
        trackAnalytics('checkout_sync_start', { itemCount: items.length, attempt: retryCount + 1 });
        await api.delete('/cart');
        
        for (const item of items) {
          await api.post('/cart/items', {
            productId: item.id,
            quantity: item.qty
          });
        }
        
        trackAnalytics('checkout_sync_success', { itemCount: items.length });
        setStep('address');
      } catch (err) {
        console.error('Cart synchronization failed:', err);
        const msg = err.response?.data?.message || 'Could not synchronize your cart with the server. Please reload and try again.';
        setSyncError(msg);
        trackAnalytics('checkout_sync_failed', { error: msg, attempt: retryCount + 1 });
      }
    };

    syncCartWithDatabase();
  }, [items, navigate, step]); // eslint-disable-line react-hooks/exhaustive-deps

  // Dynamic Razorpay SDK script loader
  const loadRazorpayScript = () => {
    return new Promise((resolve) => {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  // Validate fields for Step 1
  const handleAddressSubmit = (e) => {
    e.preventDefault();
    trackAnalytics('checkout_address_submit_attempt', { fullName, city, state: stateName });
    const errors = {};

    if (!fullName.trim()) errors.fullName = 'Full Name is required';
    
    if (!phone.trim()) {
      errors.phone = 'Phone number is required';
    } else if (!/^[0-9]{10,15}$/.test(phone)) {
      errors.phone = 'Phone must be between 10 and 15 digits';
    }
    
    if (!streetAddress.trim()) errors.streetAddress = 'Street Address is required';
    if (!city.trim()) errors.city = 'City is required';
    if (!stateName.trim()) errors.stateName = 'State is required';
    
    if (!pincode.trim()) {
      errors.pincode = 'Pincode is required';
    } else if (!/^[0-9]{6}$/.test(pincode)) {
      errors.pincode = 'Pincode must be exactly 6 digits';
    }

    setValidationErrors(errors);
    
    if (Object.keys(errors).length === 0) {
      trackAnalytics('checkout_address_valid', { city, state: stateName });
      setStep('payment');
    } else {
      trackAnalytics('checkout_address_invalid', { errors: Object.keys(errors) });
    }
  };

  // Handle final order submit
  const handlePlaceOrder = async () => {
    trackAnalytics('order_place_attempt', { paymentMethod, coupon: appliedCoupon?.code || null, grandTotal, itemCount: items.length });
    try {
      setSubmitError(null);
      setStep('submitting');

      const orderPayload = {
        shippingAddress: {
          fullName,
          phone,
          streetAddress,
          city,
          state: stateName,
          pincode
        },
        paymentMethod,
        couponCode: appliedCoupon ? appliedCoupon.code : null
      };

      const orderResponse = await api.post('/orders', orderPayload);
      const orderData = orderResponse.data.data;

      if (paymentMethod === 'COD') {
        setPlacedOrder(orderData);
        dispatch(clearCart());
        trackAnalytics('order_place_success', { orderId: orderData.id, paymentMethod: 'COD', total: orderData.totalAmount });
        setStep('success');
      } else {
        const scriptLoaded = await loadRazorpayScript();
        if (!scriptLoaded) {
          throw new Error('Razorpay SDK failed to load. Check your network connection.');
        }

        const paymentOrderResponse = await api.post(`/payments/create-order/${orderData.id}`);
        const paymentDetails = paymentOrderResponse.data.data;

        const options = {
          key: paymentDetails.keyId,
          amount: paymentDetails.amount * 100,
          currency: paymentDetails.currency,
          name: 'SareeKart',
          description: `Order Payment for #${orderData.id}`,
          image: 'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=200',
          order_id: paymentDetails.razorpayOrderId,
          handler: async function (response) {
            try {
              setStep('submitting');
              const verificationPayload = {
                orderId: orderData.id,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature
              };
              
              const verificationResponse = await api.post('/payments/verify', verificationPayload);
              setPlacedOrder(verificationResponse.data.data);
              dispatch(clearCart());
              trackAnalytics('order_place_success', { orderId: orderData.id, paymentMethod: 'RAZORPAY', razorpayPaymentId: response.razorpay_payment_id });
              setStep('success');
            } catch (err) {
              console.error('Payment verification failed:', err);
              const msg = err.response?.data?.message || 'Payment verification failed. Please contact support.';
              setSubmitError(msg);
              trackAnalytics('payment_verification_failed', { orderId: orderData.id, error: msg });
              setStep('payment');
            }
          },
          prefill: {
            name: fullName,
            contact: phone,
            email: user?.email || ''
          },
          theme: {
            color: '#2B0F1E'
          },
          modal: {
            ondismiss: function () {
              trackAnalytics('razorpay_modal_dismissed', { orderId: orderData.id });
              setStep('payment');
              setSubmitError('Payment modal closed. The order has not been completed. You can retry or choose Cash on Delivery.');
            }
          }
        };

        const rzp = new window.Razorpay(options);
        rzp.open();
      }
    } catch (err) {
      console.error('Failed to place order:', err);
      const msg = err.response?.data?.message || 'An error occurred while placing your order. Please try again.';
      setSubmitError(msg);
      trackAnalytics('order_place_failed', { error: msg, paymentMethod });
      setStep('payment');
    }
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  const getStepIcon = (stepIndex) => {
    if (stepIndex === 1) return <MapPin className="w-4 h-4" />;
    if (stepIndex === 2) return <CreditCard className="w-4 h-4" />;
    return <Check className="w-4 h-4" />;
  };

  const getStepState = (stepIndex) => {
    const stepOrder = { address: 1, payment: 2, submitting: 2, success: 3 };
    const currentStepNum = stepOrder[step] || 1;
    if (stepIndex < currentStepNum) return 'completed';
    if (stepIndex === currentStepNum) return 'active';
    return 'pending';
  };

  if (step === 'syncing') {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center space-y-4 px-4 bg-[#FAF8F5]">
        {syncError ? (
          <div className="text-center space-y-4 max-w-md">
            <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-xl font-medium">
              {syncError}
            </div>
            <div className="flex gap-3 justify-center">
              <button 
                onClick={() => { setRetryCount(c => c + 1); setSyncError(null); setStep('syncing'); window.location.reload(); }}
                className="px-6 py-2.5 bg-[#2B0F1E] hover:bg-[#200b16] text-white font-bold rounded-xl shadow transition cursor-pointer"
              >
                Retry Sync
              </button>
              <button onClick={() => navigate('/cart')} className="px-6 py-2.5 bg-white border border-border hover:bg-[#FAF8F5] text-text-primary font-bold rounded-xl">Back to Cart</button>
            </div>
            <p className="text-[11px] text-text-muted">Attempt {retryCount + 1} • If this persists, please contact support.</p>
          </div>
        ) : (
          <>
            <Loader2 className="w-12 h-12 text-[#2B0F1E] animate-spin" />
            <h2 className="text-xl font-bold font-serif text-[#2B0F1E]">Syncing Secure Checkout...</h2>
            <p className="text-xs text-text-secondary text-center max-w-xs font-light">
              We are synchronizing your shopping cart with our database. Please don't close this page.
            </p>
          </>
        )}
      </div>
    );
  }

  if (step === 'success' && placedOrder) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-24 text-center space-y-8 animate-fade-in bg-[#FAF8F5]">
        <div className="inline-flex items-center justify-center w-20 h-20 bg-green-50 text-green-600 rounded-full border border-green-200">
          <Check className="w-10 h-10" strokeWidth={3} />
        </div>
        
        <div className="space-y-3">
          <h1 className="text-4xl font-bold font-serif text-[#2B0F1E]">Order Placed Successfully!</h1>
          <p className="text-sm text-text-secondary max-w-md mx-auto font-light leading-relaxed">
            Thank you for shopping at SareeKart. Your purchase helps support India's heritage weaving artisans.
          </p>
        </div>

        {/* Order Details Receipt Card */}
        <div className="bg-white border border-[#F4F2EB] rounded-[20px] shadow-luxury text-left p-8 space-y-6 max-w-lg mx-auto">
          <div className="flex justify-between border-b border-[#F4F2EB] pb-4">
            <div>
              <p className="text-[10px] text-text-muted uppercase font-bold tracking-widest">Order Number</p>
              <p className="text-base font-bold text-[#2B0F1E]">#SK-2026-{placedOrder.id}</p>
            </div>
            <div className="text-right">
              <p className="text-[10px] text-text-muted uppercase font-bold tracking-widest">Total Paid</p>
              <p className="text-base font-bold text-[#2B0F1E]">{formatCurrency(placedOrder.totalAmount)}</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <p className="text-[10px] text-text-muted uppercase font-bold flex items-center gap-1.5 mb-1 tracking-widest">
                <Truck className="w-3.5 h-3.5 text-[#C89B3C]" /> Shipping Destination
              </p>
              <p className="text-xs font-semibold text-text-primary">{placedOrder.shippingAddress.fullName}</p>
              <p className="text-xs text-text-secondary mt-0.5 leading-relaxed font-light">
                {placedOrder.shippingAddress.streetAddress}, {placedOrder.shippingAddress.city}, {placedOrder.shippingAddress.state} - {placedOrder.shippingAddress.pincode}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4 border-t border-[#F4F2EB] pt-4">
              <div>
                <p className="text-[10px] text-text-muted uppercase font-bold tracking-widest">Payment Method</p>
                <p className="text-xs font-bold text-text-primary capitalize mt-0.5">{placedOrder.paymentMethod}</p>
              </div>
              <div>
                <p className="text-[10px] text-text-muted uppercase font-bold tracking-widest">Status</p>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-md text-[9px] font-bold bg-blue-50 text-blue-700 border border-blue-100 uppercase mt-1">
                  {placedOrder.status}
                </span>
              </div>
            </div>
            {/* Coupon saved - success state */}
            {placedOrder.couponCode && (
              <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex items-center justify-between">
                <div className="text-xs">
                  <p className="text-[10px] uppercase font-bold tracking-widest text-green-700">Coupon Applied</p>
                  <p className="font-bold text-green-800 font-mono">{placedOrder.couponCode} <span className="font-normal">saved {formatCurrency(placedOrder.discountAmount || discountAmount)}</span></p>
                </div>
                <span className="text-green-700 text-[10px] font-bold border border-green-300 bg-white px-2 py-1 rounded-full">SAVED</span>
              </div>
            )}
            {/* Estimated delivery + inventory notice - success hardening */}
            <div className="bg-[#F9F6F1] border border-[#E6DFD3] rounded-xl p-4 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-text-primary"><Truck className="w-4 h-4 text-[#C89B3C]" /> Estimated Delivery: <span className="text-maroon">{deliveryWindow}</span> <span className="ml-auto text-[10px] font-normal text-text-muted">3–5 business days</span></div>
              <p className="text-[11px] text-text-secondary leading-relaxed">You’ll receive SMS & email confirmation shortly. Need to cancel? Refunds before shipment automatically restock inventory and return funds to your original payment method. After shipment, refunds are financial-only.</p>
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
          <button 
            onClick={() => navigate('/orders')}
            className="px-8 py-3.5 border border-[#2B0F1E] text-[#2B0F1E] font-bold rounded-xl hover:bg-[#FAF8F5] transition duration-300 text-xs uppercase tracking-widest flex items-center justify-center gap-2 cursor-pointer"
          >
            <ShoppingBag className="w-4 h-4" /> View Order History
          </button>
          <button 
            onClick={() => navigate('/')}
            className="px-8 py-3.5 bg-[#2B0F1E] hover:bg-[#200b16] text-white font-bold rounded-xl shadow transition duration-300 text-xs uppercase tracking-widest flex items-center justify-center gap-2 cursor-pointer"
          >
            Continue Shopping <ArrowRight className="w-4 h-4" />
          </button>
        </div>
        <p className="text-[11px] text-text-muted max-w-lg mx-auto">Order confirmation sent to <span className="font-bold text-text-primary">{user?.email}</span> • Need help? <a href="/orders" className="underline hover:text-maroon">Track / request refund from My Orders</a></p>
      </div>
    );
  }

  return (
    <div className="w-full bg-[#FAF8F5] min-h-screen pt-28 pb-20">
      <div className="max-w-[1320px] mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Editorial Header */}
        <div className="text-center space-y-3 mb-16">
          <h1 className="text-4xl sm:text-5xl font-bold font-serif text-[#2B0F1E]">Secure Checkout</h1>
          <p className="text-xs sm:text-sm text-text-secondary font-light">Exquisite handcrafted weaves shipped directly from artisan weavers.</p>
        </div>

        {/* Checkout Progress Stepper */}
        <div className="max-w-xl mx-auto mb-16">
          <div className="flex items-center justify-between relative">
            <div className="absolute top-5 left-0 right-0 h-px bg-[#E6DFD3] z-0"></div>
            <div 
              className="absolute top-5 left-0 h-px bg-[#2B0F1E] z-0 transition-all duration-500"
              style={{ width: step === 'payment' || step === 'submitting' ? '50%' : '0%' }}
            ></div>
            
            {/* Step 1: Address */}
            <div className="relative z-10 flex flex-col items-center">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
                getStepState(1) === 'completed'
                  ? 'bg-[#C89B3C] text-white shadow-sm'
                  : getStepState(1) === 'active'
                    ? 'bg-[#2B0F1E] text-white shadow-sm'
                    : 'bg-[#F5EFEB] text-text-muted border border-[#E6DFD3]'
              }`}>
                {getStepIcon(1)}
              </div>
              <span className="text-[10px] font-bold text-text-primary mt-2.5 uppercase tracking-widest">Address</span>
            </div>

            {/* Step 2: Payment */}
            <div className="relative z-10 flex flex-col items-center">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
                getStepState(2) === 'completed'
                  ? 'bg-[#C89B3C] text-white shadow-sm'
                  : getStepState(2) === 'active'
                    ? 'bg-[#2B0F1E] text-white shadow-sm'
                    : 'bg-[#F5EFEB] text-text-muted border border-[#E6DFD3]'
              }`}>
                {getStepIcon(2)}
              </div>
              <span className={`text-[10px] font-bold mt-2.5 uppercase tracking-widest ${
                getStepState(2) !== 'pending' ? 'text-text-primary' : 'text-text-muted'
              }`}>Payment</span>
            </div>

            {/* Step 3: Confirmation */}
            <div className="relative z-10 flex flex-col items-center">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
                getStepState(3) === 'completed'
                  ? 'bg-[#C89B3C] text-white shadow-sm'
                  : getStepState(3) === 'active'
                    ? 'bg-[#2B0F1E] text-white shadow-sm'
                    : 'bg-[#F5EFEB] text-text-muted border border-[#E6DFD3]'
              }`}>
                {getStepIcon(3)}
              </div>
              <span className="text-[10px] font-bold text-text-muted mt-2.5 uppercase tracking-widest">Placed</span>
            </div>
          </div>
        </div>

        {/* 12-Column Responsive Grid System */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-start">
          
          {/* Left Side: Address / Payment form cards (Span 8 cols on desktop, 7 cols on tablet) */}
          <div className="col-span-1 md:col-span-7 lg:col-span-8 bg-white border border-[#F4F2EB] rounded-[20px] shadow-luxury p-8 space-y-8 min-h-[500px]">
            
            {submitError && (
              <div className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-3 rounded-xl font-medium">
                {submitError}
              </div>
            )}

            {step === 'address' && (
              <form onSubmit={handleAddressSubmit} className="space-y-8">
                <div className="flex items-center gap-2.5 border-b border-[#F4F2EB] pb-4">
                  <MapPin className="w-5 h-5 text-[#2B0F1E]" />
                  <h2 className="text-xl font-bold font-serif text-[#2B0F1E]">Delivery Information</h2>
                </div>

                <div className="space-y-5">
                  {/* Row 1: Full Name & Phone Number */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">Full Name *</label>
                      <input
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="Recipient's full name"
                        className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                      />
                      {validationErrors.fullName && <p className="text-xs text-red-600 font-medium">{validationErrors.fullName}</p>}
                    </div>

                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">Phone Number *</label>
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="10-digit mobile number"
                        className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                      />
                      {validationErrors.phone && <p className="text-xs text-red-600 font-medium">{validationErrors.phone}</p>}
                    </div>
                  </div>

                  {/* Row 2: Street Address */}
                  <div className="space-y-1.5 text-left">
                    <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">Street Address *</label>
                    <input
                      type="text"
                      value={streetAddress}
                      onChange={(e) => setStreetAddress(e.target.value)}
                      placeholder="Flat/House number, Apartment/Street details"
                      className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                    />
                    {validationErrors.streetAddress && <p className="text-xs text-red-600 font-medium">{validationErrors.streetAddress}</p>}
                  </div>

                  {/* Row 3: City & State */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">City *</label>
                      <input
                        type="text"
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        placeholder="City name"
                        className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                      />
                      {validationErrors.city && <p className="text-xs text-red-600 font-medium">{validationErrors.city}</p>}
                    </div>

                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">State *</label>
                      <input
                        type="text"
                        value={stateName}
                        onChange={(e) => setStateName(e.target.value)}
                        placeholder="State name"
                        className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                      />
                      {validationErrors.stateName && <p className="text-xs text-red-600 font-medium">{validationErrors.stateName}</p>}
                    </div>
                  </div>

                  {/* Row 4: PIN Code & Country */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">PIN Code *</label>
                      <input
                        type="text"
                        maxLength={6}
                        value={pincode}
                        onChange={(e) => setPincode(e.target.value)}
                        placeholder="6-digit PIN"
                        className="w-full bg-white border border-[#E6DFD3] focus:border-[#C89B3C] focus:outline-none rounded-xl px-5 h-[56px] text-sm transition-all placeholder-text-muted font-semibold text-[#22181C]"
                      />
                      {validationErrors.pincode && <p className="text-xs text-red-600 font-medium">{validationErrors.pincode}</p>}
                    </div>

                    <div className="space-y-1.5 text-left">
                      <label className="text-[10px] font-bold text-text-secondary uppercase tracking-widest">Country *</label>
                      <input
                        type="text"
                        disabled
                        value={country}
                        className="w-full bg-[#FAF8F5] border border-[#E6DFD3] rounded-xl px-5 h-[56px] text-sm font-semibold text-[#8E8288] select-none"
                      />
                    </div>
                  </div>
                </div>

                <div className="pt-6 border-t border-[#F4F2EB] flex justify-end">
                  <button
                    type="submit"
                    className="w-full sm:w-auto px-8 h-[56px] bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] hover:text-white font-bold rounded-xl shadow transition duration-300 flex items-center justify-center gap-2 cursor-pointer uppercase text-xs tracking-widest"
                  >
                    Proceed to Payment <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </form>
            )}

            {step === 'payment' && (
              <div className="space-y-8 animate-fade-in">
                <div className="flex items-center gap-2.5 border-b border-[#F4F2EB] pb-4">
                  <CreditCard className="w-5 h-5 text-[#2B0F1E]" />
                  <h2 className="text-xl font-bold font-serif text-[#2B0F1E]">Select Payment Method</h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                  {/* COD Option */}
                  <label className={`flex items-start gap-4 p-6 rounded-2xl border transition-all duration-300 cursor-pointer select-none ${
                    paymentMethod === 'COD' 
                      ? 'border-[#C89B3C] bg-[#F9F6F1] shadow-sm' 
                      : 'border-[#E6DFD3] hover:border-[#C89B3C] bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={paymentMethod === 'COD'}
                      onChange={() => { setPaymentMethod('COD'); trackAnalytics('payment_method_selected', { method: 'COD' }); }}
                      className="mt-1 accent-[#2B0F1E] focus:ring-0"
                    />
                    <div className="text-left">
                      <p className="text-sm font-bold text-text-primary uppercase tracking-wide">Cash on Delivery</p>
                      <p className="text-xs text-text-secondary mt-1.5 leading-relaxed font-light">
                        Pay with cash upon receipt. Perfect if you prefer offline transactions.
                      </p>
                    </div>
                  </label>

                  {/* Razorpay Option */}
                  <label className={`flex items-start gap-4 p-6 rounded-2xl border transition-all duration-300 cursor-pointer select-none ${
                    paymentMethod === 'RAZORPAY' 
                      ? 'border-[#C89B3C] bg-[#F9F6F1] shadow-sm' 
                      : 'border-[#E6DFD3] hover:border-[#C89B3C] bg-white'
                  }`}>
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="RAZORPAY"
                      checked={paymentMethod === 'RAZORPAY'}
                      onChange={() => { setPaymentMethod('RAZORPAY'); trackAnalytics('payment_method_selected', { method: 'RAZORPAY' }); }}
                      className="mt-1 accent-[#2B0F1E] focus:ring-0"
                    />
                    <div className="text-left">
                      <p className="text-sm font-bold text-text-primary uppercase tracking-wide">Online Payment</p>
                      <p className="text-xs text-text-secondary mt-1.5 leading-relaxed font-light">
                        Pay securely with Credit Cards, UPI, NetBanking, or Wallet via Razorpay.
                      </p>
                    </div>
                  </label>
                </div>

                {paymentMethod === 'RAZORPAY' && (
                  <div className="bg-[#F9F6F1] border border-[#E6DFD3] p-4 rounded-xl flex items-start gap-3">
                    <ShieldCheck className="w-5 h-5 text-green-600 shrink-0 mt-0.5" />
                    <p className="text-xs text-text-secondary text-left font-light leading-relaxed">
                      Your transaction is encrypted. We do not store card details or credentials. Payment is verified in real-time.
                    </p>
                  </div>
                )}

                {/* Navigation Actions */}
                <div className="pt-6 border-t border-[#F4F2EB] flex flex-col sm:flex-row gap-4 justify-between items-center">
                  <button
                    onClick={() => setStep('address')}
                    className="flex items-center gap-1.5 text-xs font-bold text-[#2B0F1E] uppercase tracking-widest hover:underline focus:outline-none"
                  >
                    <ArrowLeft className="w-4 h-4" /> Back to Shipping
                  </button>
                  
                  <button
                    onClick={handlePlaceOrder}
                    className="w-full sm:w-auto px-8 h-[56px] bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] hover:text-white font-bold rounded-xl shadow transition duration-300 flex items-center justify-center gap-2 cursor-pointer uppercase text-xs tracking-widest"
                  >
                    Place Order <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}

            {step === 'submitting' && (
              <div className="min-h-[350px] flex flex-col items-center justify-center space-y-4 py-12">
                <Loader2 className="w-12 h-12 text-[#2B0F1E] animate-spin" />
                <h3 className="text-lg font-bold font-serif text-[#2B0F1E]">Processing Transaction...</h3>
                <p className="text-xs text-text-secondary text-center max-w-xs font-light leading-relaxed">
                  We are validating inventory, creating your invoice, and contacting secure gateways. Please do not refresh.
                </p>
              </div>
            )}

          </div>

          {/* Right Side: Order Summary Sidebar (Span 4 cols on desktop, 5 cols on tablet) */}
          <div className="col-span-1 md:col-span-5 lg:col-span-4 bg-white border border-[#F4F2EB] rounded-[20px] shadow-luxury p-8 space-y-6">
            <h2 className="text-xl font-bold font-serif text-[#2B0F1E] border-b border-[#F4F2EB] pb-4 text-left">Order Summary</h2>

            {/* Cart items list */}
            <div className="divide-y divide-[#F4F2EB] overflow-y-auto max-h-[350px] pr-2">
              {items.map((item) => (
                <div key={item.id} className="flex gap-5 py-4 first:pt-0 last:pb-0 items-start">
                  <div className="w-[120px] h-[120px] shrink-0 overflow-hidden rounded-xl border border-[#F4F2EB] bg-[#F9F6F1]">
                    <SafeImage 
                      src={item.image} 
                      alt={item.name}
                      productName={item.name}
                      category={item.fabric || 'Pure Handloom'}
                      aspectRatioClass="aspect-square"
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="flex-1 min-w-0 space-y-1.5 text-left pt-1">
                    <h4 className="text-xs font-bold text-text-primary uppercase tracking-wide truncate" title={item.name}>{item.name}</h4>
                    <p className="text-[10px] uppercase font-bold text-text-muted tracking-widest">Quantity: {item.qty}</p>
                    <p className="text-sm font-bold text-[#2B0F1E]">{formatCurrency(item.price)}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* Coupon Code Form – hardened with validation, search hint, expiry */}
            <div className="border-t border-[#F4F2EB] pt-4 space-y-2">
              <label className="text-[9px] uppercase font-bold text-text-secondary block tracking-widest text-left">Apply Promo Code</label>
              {appliedCoupon ? (
                <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-3 text-xs text-green-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <div className="font-medium text-left">
                      <span className="font-bold">{appliedCoupon.code}</span> applied — you save {'₹'}{Number(appliedCoupon.discount).toLocaleString('en-IN')}
                    </div>
                    <button 
                      type="button"
                      onClick={handleRemoveCoupon}
                      className="text-red-600 hover:text-red-800 font-bold ml-2 cursor-pointer uppercase text-[10px] tracking-wider"
                    >
                      Remove
                    </button>
                  </div>
                  {appliedCoupon.description && <p className="text-[11px] text-green-700/80 text-left">{appliedCoupon.description}</p>}
                  {appliedCoupon.validUntil && <p className="text-[10px] text-green-700/60 text-left">Expires {new Date(appliedCoupon.validUntil).toLocaleDateString('en-IN')}</p>}
                </div>
              ) : (
                <form onSubmit={handleApplyCoupon} className="flex gap-2">
                  <input 
                    type="text" 
                    value={couponInput}
                    onChange={(e) => { setCouponInput(e.target.value.toUpperCase()); if (couponError) setCouponError(null); }}
                    placeholder="e.g. WELCOME10"
                    pattern="^[A-Z0-9_-]+$"
                    maxLength={50}
                    className="flex-grow bg-white border border-[#E6DFD3] focus:border-[#C89B3C] rounded-xl px-4 h-[44px] text-xs outline-none uppercase font-bold"
                  />
                  <button 
                    type="submit"
                    disabled={couponLoading || !couponInput.trim()}
                    className="px-5 h-[44px] bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] hover:text-white font-bold rounded-xl text-xs disabled:opacity-50 cursor-pointer shrink-0 uppercase tracking-widest transition-colors"
                  >
                    {couponLoading ? '...' : 'Apply'}
                  </button>
                </form>
              )}
              {couponError && <p className="text-[10px] text-red-600 font-medium text-left bg-red-50 border border-red-200 rounded-lg px-3 py-1.5">{couponError}</p>}
              {!appliedCoupon && !couponError && <p className="text-[10px] text-text-muted text-left">Try <span className="font-mono font-bold">WELCOME20</span> or <span className="font-mono font-bold">FLAT500</span> • codes are case-insensitive</p>}
            </div>

            {/* Totals Invoice */}
            <div className="border-t border-[#F4F2EB] pt-4 space-y-3">
              <div className="flex justify-between text-xs text-text-secondary font-medium">
                <span>Subtotal</span>
                <span className="font-bold text-text-primary">{formatCurrency(subtotal)}</span>
              </div>
              {appliedCoupon && (
                <div className="flex justify-between text-xs text-green-700 font-medium">
                  <span>Coupon Discount ({appliedCoupon.code})</span>
                  <span className="font-bold">- {formatCurrency(discountAmount)}</span>
                </div>
              )}
              <div className="flex justify-between text-xs text-text-secondary font-medium">
                <span>GST (Tax 5%)</span>
                <span className="font-bold text-text-primary">{formatCurrency(taxAmount)}</span>
              </div>
              <div className="flex justify-between text-xs text-text-secondary font-medium">
                <span>Shipping Fee</span>
                {shippingFee === 0 ? (
                  <span className="font-bold text-green-600">FREE</span>
                ) : (
                  <span className="font-bold text-text-primary">{formatCurrency(shippingFee)}</span>
                )}
              </div>
              
              <div className="border-t border-[#F4F2EB] pt-4 flex justify-between text-sm font-extrabold text-[#2B0F1E] uppercase tracking-wider">
                <span>Grand Total</span>
                <span className="text-base text-[#2B0F1E]">{formatCurrency(grandTotal)}</span>
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
}
