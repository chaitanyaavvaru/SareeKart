import { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  AlertTriangle,
  Check,
  CheckCircle2,
  CreditCard,
  Loader2,
  MapPin,
  MessageCircle,
  ShieldCheck,
  ShoppingBag,
  Tag,
  Truck,
  Wallet,
  X,
} from 'lucide-react';
import api from '../../api/axiosConfig';
import { clearCart } from '../../redux/slices/cartSlice';
import SEO from '../../components/common/SEO';
import walletService from '../../services/walletService';
import logisticsService from '../../services/logisticsService';
import whatsAppService from '../../services/whatsAppService';
import cartService from '../../services/cartService';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

const inputClass = 'h-12 rounded-[8px] border border-[#DDE4EA] bg-[#F5F7FA] px-4 text-sm font-semibold text-[#111827] outline-none placeholder:text-[#94A3B8] focus:border-[#243B6B]';

function getApiErrorMessage(error, fallback) {
  const responseMessage = error?.response?.data?.message;
  if (responseMessage) return responseMessage;

  if (error?.code === 'ERR_NETWORK' || !error?.response) {
    return 'Cannot reach the SareeKart backend at 127.0.0.1:8081. Start the backend and try again.';
  }

  if (error.response.status === 401) {
    return 'Your session has expired. Please sign in again to continue checkout.';
  }

  if (error.response.status === 403) {
    return 'Your account is not allowed to update this cart. Please sign in again.';
  }

  return `${fallback} (HTTP ${error.response.status})`;
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const { items } = useSelector((state) => state.cart);

  const [step, setStep] = useState('syncing');
  const [syncError, setSyncError] = useState(null);
  const [syncAttempt, setSyncAttempt] = useState(0);
  const lastSyncKeyRef = useRef(null);
  const syncResumeStepRef = useRef('address');
  const [submitError, setSubmitError] = useState(null);
  const [placedOrder, setPlacedOrder] = useState(null);
  const [pendingPaymentOrder, setPendingPaymentOrder] = useState(null);

  const [fullName, setFullName] = useState(user ? `${user.firstName} ${user.lastName || ''}`.trim() : '');
  const [phone, setPhone] = useState('');
  const [streetAddress, setStreetAddress] = useState('');
  const [city, setCity] = useState('');
  const [stateName, setStateName] = useState('');
  const [pincode, setPincode] = useState('');
  const [logisticsInfo, setLogisticsInfo] = useState(null);
  const [checkingLogistics, setCheckingLogistics] = useState(false);
  const [logisticsError, setLogisticsError] = useState('');
  const [validationErrors, setValidationErrors] = useState({});
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [whatsappOptIn, setWhatsappOptIn] = useState(true);

  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponError, setCouponError] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);

  const [wallet, setWallet] = useState(null);
  const [applyWalletCredit, setApplyWalletCredit] = useState(false);

  useEffect(() => {
    const cleanPin = pincode.replace(/\D/g, '').slice(0, 6);
    if (cleanPin.length === 6) {
      setCheckingLogistics(true);
      setLogisticsError('');
      logisticsService.checkPincode(cleanPin)
        .then((res) => {
          if (res && res.data) {
            const data = res.data;
            setLogisticsInfo(data);
            if (!data.serviceable) {
              setLogisticsError(`PIN code ${cleanPin} is outside our courier delivery network.`);
            } else {
              setCity(data.city);
              setStateName(data.state);
              if (!data.codAvailable && paymentMethod === 'COD') {
                setPaymentMethod('RAZORPAY');
              }
            }
          }
        })
        .catch((err) => {
          setLogisticsError(err.message || 'Unable to check courier serviceability.');
        })
        .finally(() => {
          setCheckingLogistics(false);
        });
    } else {
      setLogisticsInfo(null);
      setLogisticsError('');
    }
  }, [pincode]);

  useEffect(() => {
    if (user) {
      walletService.getMyWallet().then((w) => setWallet(w)).catch(() => {});
    }
  }, [user]);

  const subtotal = items.reduce((sum, item) => sum + item.price * item.qty, 0);
  const discountAmount = appliedCoupon ? Math.round(subtotal * (appliedCoupon.discountPercent / 100)) : 0;
  const shippingFee = subtotal >= 5000 || subtotal === 0 ? 0 : 150;
  const netBeforeWallet = Math.max(0, subtotal - discountAmount + shippingFee);
  const availableWalletCredit = wallet?.balance || 0;
  const walletDeduction = applyWalletCredit ? Math.min(availableWalletCredit, netBeforeWallet) : 0;
  const grandTotal = Math.max(0, netBeforeWallet - walletDeduction);
  const isFullyCoveredByWallet = applyWalletCredit && grandTotal === 0 && walletDeduction > 0;

  useEffect(() => {
    if (items.length === 0) {
      if (step !== 'success') navigate('/products');
      return;
    }

    if (!user) {
      navigate('/login?redirect=checkout');
      return;
    }

    if (step !== 'syncing') return;

    const syncKey = `${user.id || user.email}:${items.map((item) => `${item.id}:${item.qty}`).join('|')}:${syncAttempt}`;
    if (lastSyncKeyRef.current === syncKey) return;
    lastSyncKeyRef.current = syncKey;

    const syncCartWithDatabase = async () => {
      try {
        setSyncError(null);
        // Atomically merge guest items into backend cart with capped summation and stock check
        const mergeRes = await cartService.mergeGuestCart(items);
        if (mergeRes.data?.hasStockIssues) {
          setSyncError('Some items in your cart exceed available stock. Please adjust your cart before checking out.');
          return;
        }

        if (lastSyncKeyRef.current === syncKey) {
          const nextStep = syncResumeStepRef.current;
          syncResumeStepRef.current = 'address';
          setStep(nextStep);
        }
      } catch (error) {
        console.error('Cart synchronization failed:', error);
        if (error.response?.status === 401 || error.response?.status === 403) {
          navigate('/login?redirect=checkout');
          return;
        }
        if (lastSyncKeyRef.current === syncKey) {
          setSyncError(getApiErrorMessage(error, 'Could not synchronize your cart with the server.'));
        }
      }
    };

    syncCartWithDatabase();
  }, [items, navigate, step, syncAttempt, user]);

  const loadRazorpayScript = () => new Promise((resolve) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });

  const handleApplyCoupon = async (event) => {
    event.preventDefault();
    if (!couponInput.trim()) return;
    try {
      setCouponLoading(true);
      setCouponError(null);
      const res = await api.get('/coupons/validate', {
        params: { code: couponInput.trim().toUpperCase() },
      });
      if (res.data?.success && res.data?.data) {
        setAppliedCoupon(res.data.data);
      } else {
        setCouponError('Invalid coupon code.');
      }
    } catch (error) {
      setCouponError(error.response?.data?.message || 'Invalid coupon code.');
    } finally {
      setCouponLoading(false);
    }
  };

  const handleAddressSubmit = (event) => {
    event.preventDefault();
    const errors = {};

    if (!fullName.trim()) errors.fullName = 'Full Name is required';
    if (!phone.trim()) errors.phone = 'Phone number is required';
    else if (!/^[0-9]{10,15}$/.test(phone)) errors.phone = 'Phone must be between 10 and 15 digits';
    if (!streetAddress.trim()) errors.streetAddress = 'Street Address is required';
    if (!city.trim()) errors.city = 'City is required';
    if (!stateName.trim()) errors.stateName = 'State is required';
    if (!pincode.trim()) errors.pincode = 'Pincode is required';
    else if (!/^[0-9]{6}$/.test(pincode)) errors.pincode = 'Pincode must be exactly 6 digits';
    else if (logisticsInfo && !logisticsInfo.serviceable) errors.pincode = 'Delivery is currently not available for this PIN code';

    setValidationErrors(errors);
    if (Object.keys(errors).length === 0) setStep('payment');
  };

  const handlePlaceOrder = async () => {
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
          pincode,
        },
        paymentMethod: isFullyCoveredByWallet ? 'WALLET' : paymentMethod,
        couponCode: appliedCoupon ? appliedCoupon.code : null,
        walletDebitAmount: walletDeduction > 0 ? walletDeduction : null,
      };

      let orderData = pendingPaymentOrder;
      if (!orderData) {
        const orderResponse = await api.post('/orders', orderPayload);
        orderData = orderResponse.data.data;
        if (!isFullyCoveredByWallet && paymentMethod === 'RAZORPAY') setPendingPaymentOrder(orderData);
      }

      if (isFullyCoveredByWallet || paymentMethod === 'COD') {
        setPlacedOrder(orderData);
        setPendingPaymentOrder(null);
        setStep('success');
        dispatch(clearCart());
        return;
      }

      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) throw new Error('Razorpay SDK failed to load. Check your network connection.');

      const paymentOrderResponse = await api.post(`/payments/create-order/${orderData.id}`);
      const paymentDetails = paymentOrderResponse.data.data;

      const options = {
        key: paymentDetails.keyId,
        amount: paymentDetails.amount * 100,
        currency: paymentDetails.currency,
        name: 'SareeKart',
        description: `Order Payment for #${orderData.id}`,
        order_id: paymentDetails.razorpayOrderId,
        prefill: {
          name: fullName,
          contact: phone,
          email: user?.email || '',
        },
        theme: { color: '#243B6B' },
        handler: async (response) => {
          try {
            setStep('submitting');
            const verificationResponse = await api.post('/payments/verify', {
              orderId: orderData.id,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            setPlacedOrder(verificationResponse.data.data);
            setPendingPaymentOrder(null);
            setStep('success');
            dispatch(clearCart());
          } catch (error) {
            console.error('Payment verification failed:', error);
            setSubmitError(error.response?.data?.message || 'Payment verification failed. Please contact support.');
            setStep('payment');
          }
        },
        modal: {
          ondismiss: () => {
            setStep('payment');
            setSubmitError('Payment modal closed. The order has not been completed.');
          },
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.open();
    } catch (error) {
      console.error('Failed to place order:', error);
      const backendMessage = error.response?.data?.message || '';
      if (/shopping cart is empty/i.test(backendMessage)) {
        setSubmitError(null);
        setSyncError(null);
        setPendingPaymentOrder(null);
        syncResumeStepRef.current = 'payment';
        setSyncAttempt((attempt) => attempt + 1);
        setStep('syncing');
        return;
      }
      setSubmitError(error.response?.data?.message || error.message || 'An error occurred while placing your order. Please try again.');
      setStep('payment');
    }
  };

  if (step === 'syncing') {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-5 bg-[#F5F7FA] px-4 text-center">
        <SEO title="Checkout | SareeKart" description="Secure SareeKart checkout." />
        {syncError ? (
          <div className="max-w-md rounded-[8px] border border-red-200 bg-red-50 p-5 text-sm font-bold text-red-900">
            <p>{syncError}</p>
            <button
              onClick={() => {
                setSyncError(null);
                setSyncAttempt((attempt) => attempt + 1);
              }}
              className="sk-button-primary mt-5 w-full"
            >
              Retry checkout
            </button>
          </div>
        ) : (
          <>
            <Loader2 className="h-9 w-9 animate-spin text-[#243B6B]" />
            <h1 className="text-3xl font-bold text-[#111827]">Preparing checkout</h1>
            <p className="max-w-sm text-sm font-medium leading-7 text-[#64748B]">
              Syncing your bag so stock, totals, and delivery are current.
            </p>
          </>
        )}
      </div>
    );
  }

  if (step === 'success' && placedOrder) {
    return (
      <div className="min-h-screen bg-[#F5F7FA] py-12 text-[#111827]">
        <SEO title="Order Placed | SareeKart" description="SareeKart order confirmation." />
        <section className="section-shell">
          <div className="mx-auto max-w-2xl rounded-[8px] border border-[#DDE4EA] bg-white p-6 text-center shadow-soft sm:p-10">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#E7F5F3] text-[#0F766E]">
              <Check className="h-8 w-8" />
            </div>
            <p className="mt-6 text-[12px] font-black uppercase tracking-[0.18em] text-[#0F766E]">Order placed</p>
            <h1 className="mt-2 text-4xl font-bold">Thank you for shopping.</h1>
            <p className="mx-auto mt-4 max-w-md text-sm font-medium leading-7 text-[#64748B]">
              Your order has been received. We will update tracking as it moves through packing and delivery.
            </p>

            <div className="mt-8 grid gap-3 rounded-[8px] bg-[#F5F7FA] p-5 text-left">
              <div className="flex justify-between gap-4">
                <span className="font-bold text-[#64748B]">Order ID</span>
                <span className="font-black text-[#111827]">#SK-{placedOrder.id}</span>
              </div>
              <div className="flex justify-between gap-4">
                <span className="font-bold text-[#64748B]">Total</span>
                <span className="font-black text-[#111827]">{formatCurrency(placedOrder.totalAmount)}</span>
              </div>
              <div className="flex justify-between gap-4">
                <span className="font-bold text-[#64748B]">Payment</span>
                <span className="font-black text-[#111827]">{placedOrder.paymentMethod}</span>
              </div>
            </div>

            {/* WhatsApp Order & Tracking Updates CTA */}
            <a
              href={`https://wa.me/919059564499?text=${encodeURIComponent(
                `Namaste SareeKart! Please send booking and dispatch tracking updates for my order #SK-${placedOrder.id} (Total: ${formatCurrency(placedOrder.totalAmount)}).`
              )}`}
              target="_blank"
              rel="noreferrer"
              className="mt-4 flex items-center justify-center gap-2 rounded-[8px] border border-[#25D366] bg-[#E8F8EE] p-3 text-xs font-bold text-[#075E54] hover:bg-[#D4F4E2] transition shadow-xs"
            >
              <MessageCircle className="h-4 w-4 text-[#25D366]" />
              Get live booking & tracking updates on WhatsApp
            </a>

            <div className="mt-8 grid gap-3 sm:grid-cols-2">
              <button onClick={() => navigate('/orders')} className="sk-button-secondary w-full">
                <ShoppingBag className="h-4 w-4" />
                View orders
              </button>
              <button onClick={() => navigate('/products')} className="sk-button-primary w-full">
                Continue shopping
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F5F7FA] py-8 text-[#111827]">
      <SEO title="Checkout | SareeKart" description="Secure SareeKart checkout and delivery." />

      <section className="section-shell">
        <div className="mb-8">
          <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#E85D4F]">Checkout</p>
          <h1 className="mt-2 text-4xl font-bold sm:text-5xl">Finish your order.</h1>
          <p className="mt-3 text-sm font-medium leading-7 text-[#64748B]">
            Confirm delivery and choose how you want to pay.
          </p>
        </div>

        <div className="mb-8 grid max-w-2xl grid-cols-2 gap-3">
          {[
            ['address', 'Delivery', MapPin],
            ['payment', 'Payment', CreditCard],
          ].map(([key, label, Icon]) => {
            const active = step === key || (key === 'payment' && step === 'submitting');
            const done = key === 'address' && ['payment', 'submitting'].includes(step);
            return (
              <div key={key} className={`rounded-[8px] border p-4 ${active || done ? 'border-[#243B6B] bg-white' : 'border-[#DDE4EA] bg-white/70'}`}>
                <Icon className={`h-5 w-5 ${active || done ? 'text-[#243B6B]' : 'text-[#94A3B8]'}`} />
                <p className="mt-2 text-sm font-black">{label}</p>
              </div>
            );
          })}
        </div>

        <div className="grid gap-6 lg:grid-cols-[1fr_420px] lg:items-start">
          <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-xs sm:p-6">
            {submitError && (
              <div className="mb-5 rounded-[8px] border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-900">
                {submitError}
              </div>
            )}

            {step === 'address' && (
              <form onSubmit={handleAddressSubmit} className="grid gap-5" noValidate>
                <div className="flex items-center gap-3 border-b border-[#DDE4EA] pb-4">
                  <MapPin className="h-5 w-5 text-[#E85D4F]" />
                  <h2 className="text-2xl font-bold">Delivery details</h2>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="grid gap-2 text-sm font-black">
                    Full name *
                    <input className={inputClass} value={fullName} onChange={(event) => setFullName(event.target.value)} placeholder="Recipient name" />
                    {validationErrors.fullName && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.fullName}</span>}
                  </label>
                  <label className="grid gap-2 text-sm font-black">
                    Phone *
                    <input className={inputClass} value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="10-digit mobile" />
                    {validationErrors.phone && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.phone}</span>}
                  </label>
                </div>

                {/* WhatsApp Dispatch Alerts Opt-in */}
                <div className="p-3 bg-emerald-50/70 border border-emerald-200/80 rounded-xl flex items-center justify-between gap-3 text-left">
                  <label className="flex items-center gap-2.5 cursor-pointer text-xs font-semibold text-emerald-950 select-none">
                    <input
                      type="checkbox"
                      checked={whatsappOptIn}
                      onChange={(e) => {
                        setWhatsappOptIn(e.target.checked);
                        whatsAppService.updateOptInPreference(e.target.checked).catch(() => {});
                      }}
                      className="w-4 h-4 rounded text-[#128C7E] focus:ring-[#128C7E] border-emerald-300"
                    />
                    <span>Receive instant Blue Dart dispatch pings, live AWB tracking & delivery alerts via WhatsApp</span>
                  </label>
                  <span className="hidden sm:inline-flex items-center gap-1 text-[10px] font-bold text-[#128C7E] bg-white px-2 py-0.5 rounded-md border border-emerald-200 shadow-2xs shrink-0">
                    WhatsApp Verified ✔
                  </span>
                </div>

                <label className="grid gap-2 text-sm font-black">
                  Street address *
                  <input className={inputClass} value={streetAddress} onChange={(event) => setStreetAddress(event.target.value)} placeholder="Flat, house, street" />
                  {validationErrors.streetAddress && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.streetAddress}</span>}
                </label>

                <div className="grid gap-4 sm:grid-cols-3">
                  <label className="grid gap-2 text-sm font-black">
                    City *
                    <input className={inputClass} value={city} onChange={(event) => setCity(event.target.value)} placeholder="City" />
                    {validationErrors.city && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.city}</span>}
                  </label>
                  <label className="grid gap-2 text-sm font-black">
                    State *
                    <input className={inputClass} value={stateName} onChange={(event) => setStateName(event.target.value)} placeholder="State" />
                    {validationErrors.stateName && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.stateName}</span>}
                  </label>
                  <label className="grid gap-2 text-sm font-black">
                    Pincode *
                    <div className="relative">
                      <input 
                        className={`${inputClass} w-full`} 
                        maxLength={6} 
                        value={pincode} 
                        onChange={(event) => setPincode(event.target.value.replace(/\D/g, ''))} 
                        placeholder="6 digits" 
                      />
                      {checkingLogistics && (
                        <span className="absolute right-3 top-3.5 text-xs text-[#1E6A62]">
                          <Loader2 className="h-4 w-4 animate-spin" />
                        </span>
                      )}
                    </div>
                    {validationErrors.pincode && <span className="text-xs font-bold text-[#E85D4F]">{validationErrors.pincode}</span>}
                  </label>
                </div>

                {logisticsInfo && (
                  <div className={`mt-3 rounded-[8px] p-3.5 text-xs border ${
                    logisticsInfo.serviceable 
                      ? 'bg-[#E7F5F3] border-[#BFE7E2] text-[#0F766E]' 
                      : 'bg-red-50 border-red-200 text-red-700'
                  }`}>
                    {logisticsInfo.serviceable ? (
                      <div className="space-y-1.5">
                        <div className="flex flex-wrap items-center justify-between font-bold gap-1">
                          <span className="flex items-center gap-1.5">
                            <CheckCircle2 className="h-4 w-4 shrink-0 text-[#0F766E]" />
                            Serviceable via {logisticsInfo.courierPartner}
                          </span>
                          <span className="text-[11px] text-[#134E48] font-semibold bg-white/70 px-2 py-0.5 rounded border border-[#BFE7E2]">
                            Est. Arrival: <strong>{logisticsInfo.estimatedDeliveryDate}</strong>
                          </span>
                        </div>
                        <p className="text-[11px] text-[#134E48]/80">
                          Dispatched from <strong>{logisticsInfo.fulfillmentHub}</strong> ({logisticsInfo.zoneDisplayName})
                        </p>
                        {!logisticsInfo.codAvailable && (
                          <div className="mt-1 flex items-center gap-1.5 text-[11px] font-bold text-amber-900 bg-amber-50 p-2 rounded border border-amber-200">
                            <AlertTriangle className="h-3.5 w-3.5 shrink-0 text-amber-700" />
                            <span>Cash on Delivery (COD) is disabled for this corridor ({logisticsInfo.city}). Online Payment or Store Credit required.</span>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 font-bold">
                        <AlertTriangle className="h-4 w-4 shrink-0 text-red-600" />
                        <span>Delivery is not serviceable to PIN code {pincode}. Please enter an alternative delivery address.</span>
                      </div>
                    )}
                  </div>
                )}

                <div className="flex justify-end border-t border-[#DDE4EA] pt-5">
                  <button type="submit" className="sk-button-primary">
                    Continue to payment
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </form>
            )}

            {step === 'payment' && (
              <div className="grid gap-5">
                <div className="flex items-center gap-3 border-b border-[#DDE4EA] pb-4">
                  <CreditCard className="h-5 w-5 text-[#E85D4F]" />
                  <h2 className="text-2xl font-bold">Payment method</h2>
                </div>

                {/* Store Credit Redemption Card */}
                {availableWalletCredit > 0 && (
                  <div className="rounded-[8px] border border-[#C9C1B5] bg-gradient-to-r from-[#FAF8F5] via-white to-[#FAF8F5] p-5 shadow-xs">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3">
                        <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#E7F3EE] text-[#1E6A62]">
                          <Wallet className="h-4 w-4" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-bold text-[#17211F]">SareeKart Store Credit & Rewards</span>
                            <span className="rounded-full bg-[#FEF3C7] px-2 py-0.5 text-[10px] font-bold text-[#92400E]">
                              {wallet?.tierDisplayName || 'Patron'}
                            </span>
                          </div>
                          <p className="mt-0.5 text-xs text-[#71817A]">
                            Available Balance: <strong className="text-[#1E6A62]">{formatCurrency(availableWalletCredit)}</strong>
                          </p>
                        </div>
                      </div>

                      <label className="relative inline-flex cursor-pointer items-center">
                        <input
                          type="checkbox"
                          checked={applyWalletCredit}
                          onChange={(e) => {
                            setApplyWalletCredit(e.target.checked);
                            if (pendingPaymentOrder) setPendingPaymentOrder(null);
                          }}
                          className="h-5 w-5 rounded border-[#DDD8CF] text-[#1E6A62] accent-[#1E6A62]"
                        />
                      </label>
                    </div>

                    {applyWalletCredit && (
                      <div className="mt-3 border-t border-[#F0ECE1] pt-3 text-xs">
                        <div className="flex items-center justify-between font-medium text-[#1E6A62]">
                          <span>Store Credit Applied:</span>
                          <span className="font-bold">-{formatCurrency(walletDeduction)}</span>
                        </div>
                        {isFullyCoveredByWallet ? (
                          <div className="mt-2 rounded-[6px] bg-[#E7F3EE] p-2.5 font-bold text-[#17644F]">
                            ✓ 100% of your order total is covered by store credit! No card or cash payment needed.
                          </div>
                        ) : (
                          <p className="mt-1 text-[11px] text-[#71817A]">
                            Remaining balance of <strong>{formatCurrency(grandTotal)}</strong> will be paid via your selected method below.
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {!isFullyCoveredByWallet ? (
                  <div className="grid gap-3 sm:grid-cols-2">
                    {[
                      ['COD', 'Cash on delivery', 'Pay when your order arrives.'],
                      ['RAZORPAY', 'Online payment', 'Cards, UPI, wallets, and netbanking.'],
                    ].map(([value, label, text]) => {
                      const isCodDisabled = value === 'COD' && logisticsInfo && !logisticsInfo.codAvailable;
                      return (
                        <label
                          key={value}
                          className={`cursor-pointer rounded-[8px] border p-5 transition-all ${
                            isCodDisabled
                              ? 'opacity-60 cursor-not-allowed bg-gray-50 border-gray-200'
                              : paymentMethod === value
                              ? 'border-[#1E6A62] bg-[#E7F5F3]'
                              : 'border-[#DDE4EA] bg-white'
                          }`}
                        >
                          <input
                            type="radio"
                            name="paymentMethod"
                            value={value}
                            disabled={isCodDisabled}
                            checked={paymentMethod === value}
                            onChange={() => {
                              if (!isCodDisabled) {
                                if (pendingPaymentOrder && value !== pendingPaymentOrder.paymentMethod) setPendingPaymentOrder(null);
                                setPaymentMethod(value);
                              }
                            }}
                            className="accent-[#1E6A62]"
                          />
                          <div className="flex items-center justify-between mt-3">
                            <p className="text-base font-black">{label}</p>
                            {isCodDisabled && (
                              <span className="rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-black uppercase text-amber-800">
                                COD Disabled
                              </span>
                            )}
                          </div>
                          <p className="mt-1 text-sm font-medium leading-6 text-[#64748B]">
                            {isCodDisabled ? 'Unavailable for remote / high-value postal corridor. Please use Online Payment.' : text}
                          </p>
                        </label>
                      );
                    })}
                  </div>
                ) : (
                  <div className="rounded-[8px] border border-[#B8D8D1] bg-[#E3F0ED] p-4 text-xs font-semibold text-[#1E6A62]">
                    Payment Mode: <strong>Full Store Credit Redemption</strong>. Click "Place Order" below to complete immediately.
                  </div>
                )}

                {paymentMethod === 'RAZORPAY' && (
                  <div className="flex items-start gap-3 rounded-[8px] border border-[#BFE7E2] bg-[#E7F5F3] p-4 text-sm font-bold text-[#0F766E]">
                    <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0" />
                    Secure payment is handled by Razorpay. SareeKart does not store card details.
                  </div>
                )}

                <div className="flex flex-col-reverse gap-3 border-t border-[#DDE4EA] pt-5 sm:flex-row sm:items-center sm:justify-between">
                  <button onClick={() => setStep('address')} className="sk-button-secondary">
                    <ArrowLeft className="h-4 w-4" />
                    Back
                  </button>
                  <button onClick={handlePlaceOrder} className="sk-button-primary">
                    Place order
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}

            {step === 'submitting' && (
              <div className="flex min-h-[320px] flex-col items-center justify-center gap-4 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#243B6B]" />
                <h2 className="text-2xl font-bold">Processing order</h2>
                <p className="max-w-sm text-sm font-medium leading-7 text-[#64748B]">
                  Please keep this page open while we confirm your order.
                </p>
              </div>
            )}
          </div>

          <aside className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-xs sm:p-6 lg:sticky lg:top-32">
            <h2 className="text-2xl font-bold">Order summary</h2>
            <div className="mt-5 grid max-h-[360px] gap-3 overflow-y-auto pr-1">
              {items.map((item) => (
                <article key={item.id} className="grid grid-cols-[74px_1fr] gap-3 rounded-[8px] bg-[#F5F7FA] p-2">
                  <img
                    src={item.image || 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=240&q=80'}
                    alt={item.name}
                    className="h-24 w-full rounded-[8px] object-cover object-top"
                  />
                  <div className="min-w-0 py-1">
                    <h3 className="line-clamp-2 text-sm font-black text-[#111827]">{item.name}</h3>
                    <p className="mt-1 text-xs font-bold text-[#64748B]">Qty {item.qty}</p>
                    <p className="mt-2 text-sm font-black text-[#243B6B]">{formatCurrency(item.price)}</p>
                  </div>
                </article>
              ))}
            </div>

            <form onSubmit={handleApplyCoupon} className="mt-5 rounded-[8px] border border-[#DDE4EA] bg-[#F5F7FA] p-3">
              <label className="mb-2 flex items-center gap-2 text-sm font-black">
                <Tag className="h-4 w-4 text-[#E85D4F]" />
                Coupon
              </label>
              {appliedCoupon ? (
                <div className="flex items-center justify-between gap-3 rounded-full bg-white px-4 py-3 text-sm font-black text-[#0F766E]">
                  {appliedCoupon.code} applied
                  <button type="button" onClick={() => { setAppliedCoupon(null); setCouponInput(''); setCouponError(null); }} aria-label="Remove coupon">
                    <X className="h-4 w-4" />
                  </button>
                </div>
              ) : (
                <div className="flex gap-2">
                  <input value={couponInput} onChange={(event) => setCouponInput(event.target.value)} placeholder="WELCOME10" className="min-w-0 flex-1 rounded-full border border-[#DDE4EA] bg-white px-4 text-sm font-bold outline-none" />
                  <button disabled={couponLoading} className="rounded-full bg-[#111827] px-4 text-sm font-black text-white disabled:opacity-60">
                    Apply
                  </button>
                </div>
              )}
              {couponError && <p className="mt-2 text-xs font-bold text-[#E85D4F]">{couponError}</p>}
            </form>

            <div className="mt-5 grid gap-3 text-sm font-semibold text-[#64748B]">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span className="font-black text-[#111827]">{formatCurrency(subtotal)}</span>
              </div>
              {discountAmount > 0 && (
                <div className="flex justify-between text-[#0F766E]">
                  <span>Discount</span>
                  <span className="font-black">-{formatCurrency(discountAmount)}</span>
                </div>
              )}
              {walletDeduction > 0 && (
                <div className="flex justify-between text-[#0F766E]">
                  <span>Store credit</span>
                  <span className="font-black">-{formatCurrency(walletDeduction)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span>Shipping</span>
                <span className={shippingFee === 0 ? 'font-black text-[#0F766E]' : 'font-black text-[#111827]'}>
                  {shippingFee === 0 ? 'Free' : formatCurrency(shippingFee)}
                </span>
              </div>
              <div className="flex justify-between border-t border-[#DDE4EA] pt-4 text-xl font-black text-[#111827]">
                <span>Total</span>
                <span>{formatCurrency(grandTotal)}</span>
              </div>
            </div>

            <div className="mt-5 flex items-center gap-2 rounded-[8px] bg-[#E7F5F3] p-3 text-sm font-bold text-[#0F766E]">
              <Truck className="h-4 w-4 shrink-0" />
              Free shipping above Rs. 5,000.
            </div>
          </aside>
        </div>
      </section>
    </div>
  );
}
