import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  AlertTriangle,
  ArrowRight,
  Heart,
  Minus,
  Plus,
  ShieldCheck,
  ShoppingBag,
  Trash2,
  Truck,
} from 'lucide-react';
import {
  fetchCart,
  removeItem,
  removeItemServer,
  updateQty,
  updateQuantityServer,
} from '../redux/slices/cartSlice';
import wishlistService from '../services/wishlistService';
import { useCurrency } from '../context/CurrencyContext';
import SEO from '../components/common/SEO';
import { MAX_QUANTITY_PER_SKU } from '../constants/cartConstants';

export default function Cart() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { formatPrice } = useCurrency();

  const { items, hasStockIssues } = useSelector((state) => state.cart);
  const { isAuthenticated } = useSelector((state) => state.auth);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCart());
    }
  }, [dispatch, isAuthenticated]);

  const subtotal = items.reduce((sum, item) => sum + (Number(item.price) || 0) * item.qty, 0);
  const freeShippingThreshold = 5000;
  const shipping = subtotal >= freeShippingThreshold || subtotal === 0 ? 0 : 150;
  const total = subtotal + shipping;
  const amountToFreeShipping = Math.max(0, freeShippingThreshold - subtotal);

  const handleUpdateQty = (item, nextQty) => {
    const pid = item.productId || item.id;
    const maxStock = Math.min(item.availableStock ?? MAX_QUANTITY_PER_SKU, MAX_QUANTITY_PER_SKU);
    const safeQty = Math.min(nextQty, maxStock);

    if (isAuthenticated) {
      dispatch(updateQuantityServer({ productId: pid, quantity: safeQty }));
    } else {
      dispatch(updateQty({ id: pid, qty: safeQty }));
    }
  };

  const handleRemove = (item) => {
    const pid = item.productId || item.id;
    if (isAuthenticated) {
      dispatch(removeItemServer(pid));
    } else {
      dispatch(removeItem(pid));
    }
  };

  const handleMoveToWishlist = async (item) => {
    const pid = item.productId || item.id;
    if (isAuthenticated) {
      try {
        await wishlistService.addToWishlist(pid);
        dispatch(removeItemServer(pid));
      } catch (err) {
        console.error('Failed to move to wishlist:', err);
      }
    } else {
      const savedWishlist = JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]');
      if (!savedWishlist.some((w) => (w.id || w) === pid)) {
        savedWishlist.push(item);
        localStorage.setItem('sareekart_wishlist', JSON.stringify(savedWishlist));
      }
      dispatch(removeItem(pid));
    }
  };

  const handleProceedToCheckout = () => {
    if (hasStockIssues) return;
    navigate(isAuthenticated ? '/checkout' : '/login?redirect=checkout');
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F] font-sans text-left">
      <SEO
        title="Shopping Cart | SareeKart"
        description="Review your curated sarees and handloom drapes. Proceed to secure artisan checkout."
        noindex={true}
      />

      {/* Header Banner */}
      <section className="bg-white border-b border-[#DDD8CF] py-10 px-6 lg:px-16">
        <div className="max-w-6xl mx-auto flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-[#1E6A62]">
              <ShoppingBag className="w-4 h-4 text-[#1E6A62]" /> Shopping Cart
            </div>
            <h1 className="text-3xl sm:text-5xl font-serif font-bold text-[#17211F] mt-2">
              Your Selected Drapes.
            </h1>
          </div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#71817A] bg-[#F7F4EE] px-4 py-2 rounded-full border border-[#DDD8CF]">
            {items.reduce((sum, i) => sum + i.qty, 0)} {items.length === 1 ? 'Item' : 'Items'}
          </p>
        </div>
      </section>

      <div className="max-w-6xl mx-auto px-6 lg:px-16 py-10">
        {items.length === 0 ? (
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-14 text-center max-w-lg mx-auto space-y-6 shadow-xs">
            <div className="w-16 h-16 rounded-full bg-[#F7F4EE] text-[#1E6A62] flex items-center justify-center mx-auto">
              <ShoppingBag className="w-8 h-8" />
            </div>
            <div>
              <h2 className="text-2xl font-serif font-bold text-[#17211F]">Your Bag is Empty</h2>
              <p className="text-sm text-[#71817A] mt-2 leading-relaxed">
                Discover the finest pure silks, delicate organzas, and authentic Kanchipuram weaves ready for your celebratory drape.
              </p>
            </div>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 bg-[#1E6A62] text-white px-8 py-3.5 rounded-full font-bold text-sm tracking-wider uppercase hover:bg-[#154D47] transition-all shadow-md"
            >
              Explore Collection <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left 2 Cols: Cart Items List */}
            <div className="lg:col-span-2 space-y-4">
              {/* Shipping Progress */}
              <div className="bg-white border border-[#DDD8CF] rounded-2xl p-5 shadow-xs">
                <div className="flex items-center justify-between text-xs font-bold text-[#17211F] mb-2">
                  <span className="flex items-center gap-1.5 text-[#1E6A62]">
                    <Truck className="w-4 h-4" /> Free Express Shipping
                  </span>
                  <span>
                    {subtotal >= freeShippingThreshold
                      ? 'Unlocked'
                      : `Add ${formatPrice(amountToFreeShipping)} more to qualify`}
                  </span>
                </div>
                <div className="h-2 bg-[#F7F4EE] rounded-full overflow-hidden">
                  <div
                    className="h-full bg-[#1E6A62] transition-all duration-500 rounded-full"
                    style={{
                      width: `${Math.min((subtotal / freeShippingThreshold) * 100, 100)}%`,
                    }}
                  />
                </div>
              </div>

              {/* Stock Warning Notice */}
              {hasStockIssues && (
                <div className="bg-[#FEF2F2] border border-[#FCA5A5] rounded-2xl p-4 flex items-center gap-3 text-sm text-[#991B1B]">
                  <AlertTriangle className="w-5 h-5 shrink-0 text-[#DC2626]" />
                  <div>
                    <span className="font-bold">Inventory Alert:</span> Some items in your bag exceed available stock or have sold out. Please adjust before checking out.
                  </div>
                </div>
              )}

              {/* Items Card List */}
              <div className="space-y-3">
                {items.map((item) => {
                  const pid = item.productId || item.id;
                  const isExceeded =
                    item.quantityExceedsStock ||
                    (item.availableStock !== undefined && item.qty > item.availableStock);
                  const isSoldOut =
                    item.isOutOfStock ||
                    (item.availableStock !== undefined && item.availableStock <= 0);

                  return (
                    <article
                      key={pid}
                      className={`bg-white border rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row gap-5 shadow-xs transition-colors ${
                        isSoldOut || isExceeded ? 'border-[#FCA5A5] bg-[#FEF2F2]/20' : 'border-[#DDD8CF]'
                      }`}
                    >
                      {/* Image Thumbnail */}
                      <div className="w-24 h-32 sm:w-28 sm:h-36 rounded-xl bg-[#F7F4EE] overflow-hidden shrink-0 flex items-center justify-center border border-[#EBE7DF]">
                        {item.image ? (
                          <img
                            src={item.image}
                            alt={item.name}
                            className="w-full h-full object-cover object-top"
                          />
                        ) : (
                          <div className="text-center font-serif text-[11px] text-[#71817A] p-2">
                            Handloom Saree
                          </div>
                        )}
                      </div>

                      {/* Info & Controls */}
                      <div className="flex-1 flex flex-col justify-between">
                        <div>
                          <div className="flex items-start justify-between gap-2">
                            <Link to={`/products/${pid}`} className="hover:underline">
                              <h3 className="font-serif font-bold text-lg text-[#17211F] leading-snug">
                                {item.name}
                              </h3>
                            </Link>
                            <span className="font-bold text-base text-[#1E6A62] shrink-0">
                              {formatPrice((Number(item.price) || 0) * item.qty)}
                            </span>
                          </div>
                          <p className="text-xs text-[#71817A] mt-1">
                            Unit Price: {formatPrice(item.price)}
                          </p>

                          {/* Live Stock Warning */}
                          {isSoldOut ? (
                            <p className="text-xs font-bold text-[#DC2626] mt-2 flex items-center gap-1">
                              <AlertTriangle className="w-3.5 h-3.5 inline" /> Currently out of stock
                            </p>
                          ) : isExceeded ? (
                            <p className="text-xs font-bold text-[#DC2626] mt-2 flex items-center gap-1">
                              <AlertTriangle className="w-3.5 h-3.5 inline" />
                              Only {item.availableStock} sarees available. Please reduce quantity.
                            </p>
                          ) : null}
                        </div>

                        {/* Controls Bottom Bar */}
                        <div className="flex items-center justify-between pt-4 mt-2 border-t border-[#F7F4EE]">
                          {/* Quantity Stepper */}
                          <div className="flex items-center rounded-full border border-[#DDD8CF] bg-[#F7F4EE] p-1">
                            <button
                              onClick={() => handleUpdateQty(item, item.qty - 1)}
                              aria-label="Decrease quantity"
                              className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-white transition-colors"
                            >
                              <Minus className="w-3.5 h-3.5" />
                            </button>
                            <span className="min-w-8 text-center text-xs font-black text-[#17211F]">
                              {item.qty}
                            </span>
                            <button
                              onClick={() => handleUpdateQty(item, item.qty + 1)}
                              aria-label="Increase quantity"
                              disabled={
                                item.qty >=
                                Math.min(item.availableStock ?? MAX_QUANTITY_PER_SKU, MAX_QUANTITY_PER_SKU)
                              }
                              className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-white transition-colors disabled:opacity-30"
                            >
                              <Plus className="w-3.5 h-3.5" />
                            </button>
                          </div>

                          {/* Move to Wishlist & Delete */}
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => handleMoveToWishlist(item)}
                              className="text-xs font-bold text-[#71817A] hover:text-[#1E6A62] inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-[#F7F4EE] transition-colors"
                            >
                              <Heart className="w-3.5 h-3.5" /> Save for later
                            </button>
                            <button
                              onClick={() => handleRemove(item)}
                              className="w-8 h-8 flex items-center justify-center rounded-full text-[#71817A] hover:text-[#991B1B] hover:bg-[#FEF2F2] transition-colors"
                              title="Remove item"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            </div>

            {/* Right Column: Order Summary */}
            <div className="space-y-6">
              <div className="bg-white border border-[#DDD8CF] rounded-3xl p-6 sm:p-7 shadow-xs space-y-5 sticky top-24">
                <h2 className="font-serif font-bold text-xl text-[#17211F] border-b border-[#F7F4EE] pb-4">
                  Order Summary
                </h2>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between text-[#71817A]">
                    <span>Bag Subtotal</span>
                    <span className="font-bold text-[#17211F]">{formatPrice(subtotal)}</span>
                  </div>
                  <div className="flex justify-between text-[#71817A]">
                    <span>Shipping</span>
                    <span className={shipping === 0 ? 'font-bold text-[#1E6A62]' : 'font-bold text-[#17211F]'}>
                      {shipping === 0 ? 'FREE' : formatPrice(shipping)}
                    </span>
                  </div>
                  <div className="border-t border-[#DDD8CF] pt-4 flex justify-between text-base font-bold text-[#17211F]">
                    <span>Estimated Total</span>
                    <span className="text-xl text-[#1E6A62]">{formatPrice(total)}</span>
                  </div>
                </div>

                <button
                  onClick={handleProceedToCheckout}
                  disabled={hasStockIssues}
                  className={`w-full py-4 rounded-full font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 transition-all ${
                    hasStockIssues
                      ? 'bg-[#E5E7EB] text-[#9CA3AF] cursor-not-allowed'
                      : 'bg-[#1E6A62] text-white hover:bg-[#154D47] shadow-md'
                  }`}
                >
                  {hasStockIssues ? 'Resolve Stock Issues' : 'Proceed to Checkout'}
                  <ArrowRight className="w-4 h-4" />
                </button>

                <div className="pt-4 border-t border-[#F7F4EE] space-y-2 text-xs text-[#71817A]">
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-[#1E6A62]" />
                    <span>Silk Mark Certified 100% Genuine Handloom</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Truck className="w-4 h-4 text-[#1E6A62]" />
                    <span>Insured Delivery with Real-Time Tracking</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
