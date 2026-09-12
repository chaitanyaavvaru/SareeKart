import { AlertTriangle, ArrowRight, Minus, Plus, Scissors, ShoppingBag, Trash2, Truck, X } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  removeItem,
  removeItemServer,
  toggleCart,
  updateQty,
  updateQuantityServer,
} from '../../redux/slices/cartSlice';
import { useCurrency } from '../../context/CurrencyContext';
import { MAX_QUANTITY_PER_SKU } from '../../constants/cartConstants';

export default function CartDrawer() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { formatPrice } = useCurrency();

  const { items, isOpen, hasStockIssues } = useSelector((state) => state.cart);
  const { isAuthenticated } = useSelector((state) => state.auth);

  const subtotal = items.reduce((sum, item) => sum + item.price * item.qty, 0);
  const freeShippingAt = 5000;
  const remainingForFreeShip = Math.max(freeShippingAt - subtotal, 0);
  const shipping = subtotal === 0 || subtotal >= freeShippingAt ? 0 : 150;
  const total = subtotal + shipping;

  const handleUpdateQty = (item, nextQty) => {
    const pid = item.productId || item.id;
    const maxAllowed = Math.min(item.availableStock ?? MAX_QUANTITY_PER_SKU, MAX_QUANTITY_PER_SKU);
    const safeQty = Math.min(nextQty, maxAllowed);

    if (isAuthenticated) {
      dispatch(updateQuantityServer({ productId: pid, quantity: safeQty }));
    } else {
      dispatch(updateQty({ id: pid, qty: safeQty }));
    }
  };

  const handleRemoveItem = (item) => {
    const pid = item.productId || item.id;
    if (isAuthenticated) {
      dispatch(removeItemServer(pid));
    } else {
      dispatch(removeItem(pid));
    }
  };

  const handleCheckout = () => {
    if (hasStockIssues) return;
    dispatch(toggleCart());
    navigate(isAuthenticated ? '/checkout' : '/login?redirect=checkout');
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[100] overflow-hidden text-left" role="dialog" aria-modal="true" aria-label="Shopping bag">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => dispatch(toggleCart())}
            className="absolute inset-0 bg-[#111827]/45 backdrop-blur-sm"
          />

          <div className="absolute inset-y-0 right-0 flex max-w-full">
            <motion.aside
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
              className="flex h-full w-screen max-w-[460px] flex-col border-l border-[#DDE4EA] bg-white shadow-luxury"
            >
              <header className="flex items-center justify-between border-b border-[#DDE4EA] px-5 py-5">
                <div>
                  <p className="text-[11px] font-black uppercase tracking-[0.16em] text-[#0F766E]">
                    Your edit
                  </p>
                  <h2 className="mt-1 flex items-center gap-2 text-2xl font-bold text-[#111827]">
                    <ShoppingBag className="h-5 w-5 text-[#E85D4F]" />
                    Bag ({items.length})
                  </h2>
                </div>
                <button
                  onClick={() => dispatch(toggleCart())}
                  aria-label="Close cart drawer"
                  className="flex h-10 w-10 items-center justify-center rounded-full bg-[#F5F7FA] text-[#111827] transition hover:bg-[#E9EEF4]"
                >
                  <X className="h-5 w-5" />
                </button>
              </header>

              {items.length > 0 && (
                <div className="border-b border-[#DDE4EA] bg-[#F5F7FA] px-5 py-4">
                  <div className="mb-2 flex items-center justify-between gap-3 text-xs font-black text-[#111827]">
                    <span className="inline-flex items-center gap-1.5">
                      <Truck className="h-4 w-4 text-[#0F766E]" />
                      Shipping progress
                    </span>
                    <span>{subtotal >= freeShippingAt ? 'Free shipping unlocked' : `${formatPrice(remainingForFreeShip)} away`}</span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-white">
                    <div
                      className="h-full rounded-full bg-[#0F766E] transition-all duration-500"
                      style={{ width: `${Math.min((subtotal / freeShippingAt) * 100, 100)}%` }}
                    />
                  </div>
                </div>
              )}

              {hasStockIssues && (
                <div className="bg-[#FEF2F2] border-b border-[#FCA5A5] px-5 py-3 text-xs text-[#991B1B] flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0 text-[#DC2626]" />
                  <span>Some items exceed available stock. Adjust quantities to proceed.</span>
                </div>
              )}

              <div className="flex-1 overflow-y-auto px-5 py-5">
                {items.length === 0 ? (
                  <div className="flex h-full flex-col items-center justify-center gap-5 text-center">
                    <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#F5F7FA] text-[#64748B]">
                      <ShoppingBag className="h-7 w-7" />
                    </div>
                    <div>
                      <h3 className="text-2xl font-bold text-[#111827]">Your bag is empty</h3>
                      <p className="mt-2 max-w-[260px] text-sm leading-relaxed text-[#64748B]">
                        Add a color-rich drape and come back here when you are ready.
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        dispatch(toggleCart());
                        navigate('/products');
                      }}
                      className="sk-button-primary"
                    >
                      Shop catalog
                    </button>
                  </div>
                ) : (
                  <div className="grid gap-4">
                    {items.map((item) => {
                      const isItemStockIssue =
                        item.quantityExceedsStock ||
                        item.isOutOfStock ||
                        (item.availableStock !== undefined && item.qty > item.availableStock);

                      return (
                        <article
                          key={item.productId || item.id}
                          className={`grid grid-cols-[88px_1fr] gap-4 rounded-[8px] border p-3 shadow-xs transition-colors ${
                            isItemStockIssue ? 'border-[#FCA5A5] bg-[#FEF2F2]/30' : 'border-[#DDE4EA] bg-white'
                          }`}
                        >
                          <div className="h-28 overflow-hidden rounded-[8px] bg-[#EEF3F6] flex items-center justify-center">
                            {item.image ? (
                              <img
                                src={item.image}
                                alt={item.name}
                                className="h-full w-full object-cover object-top"
                                loading="lazy"
                              />
                            ) : (
                              <div className="text-center font-serif text-xs text-[#71817A] p-2">
                                Handloom Silk
                              </div>
                            )}
                          </div>
                          <div className="flex min-w-0 flex-col justify-between">
                            <div>
                              <h3 className="line-clamp-2 text-sm font-extrabold leading-snug text-[#111827]">{item.name}</h3>
                              <p className="mt-1 text-sm font-black text-[#243B6B]">{formatPrice(item.price)}</p>

                              {isItemStockIssue && (
                                <p className="mt-1 text-[11px] font-bold text-[#DC2626] flex items-center gap-1">
                                  <AlertTriangle className="w-3 h-3 inline" />
                                  {item.isOutOfStock || (item.availableStock !== undefined && item.availableStock <= 0)
                                    ? 'Sold out'
                                    : `Only ${item.availableStock} left in stock`}
                                </p>
                              )}

                              {item.tailoring && (
                                <div className="mt-1 flex flex-wrap items-center gap-1.5 text-[11px]">
                                  <span className="inline-flex items-center gap-1 rounded bg-[#F7F4EE] px-1.5 py-0.5 font-bold text-[#1E6A62]">
                                    <Scissors className="h-3 w-3" />
                                    {item.tailoring.blouseStyle !== 'unstitched' ? 'Tailored Blouse' : 'Fall & Pico'}
                                  </span>
                                  {item.tailoring.totalExtra > 0 && (
                                    <span className="font-semibold text-[#71817A]">(+{formatPrice(item.tailoring.totalExtra)})</span>
                                  )}
                                </div>
                              )}
                            </div>
                            <div className="flex items-center justify-between gap-3">
                              <div className="flex items-center rounded-full border border-[#DDE4EA] bg-[#F5F7FA] p-1">
                                <button
                                  onClick={() => handleUpdateQty(item, item.qty - 1)}
                                  aria-label="Decrease quantity"
                                  className="flex h-7 w-7 items-center justify-center rounded-full hover:bg-white"
                                >
                                  <Minus className="h-3.5 w-3.5" />
                                </button>
                                <span className="min-w-8 text-center text-sm font-black">{item.qty}</span>
                                <button
                                  onClick={() => handleUpdateQty(item, item.qty + 1)}
                                  aria-label="Increase quantity"
                                  disabled={item.qty >= Math.min(item.availableStock ?? MAX_QUANTITY_PER_SKU, MAX_QUANTITY_PER_SKU)}
                                  className="flex h-7 w-7 items-center justify-center rounded-full hover:bg-white disabled:opacity-40 disabled:hover:bg-transparent"
                                >
                                  <Plus className="h-3.5 w-3.5" />
                                </button>
                              </div>
                              <button
                                onClick={() => handleRemoveItem(item)}
                                aria-label={`Remove ${item.name} from cart`}
                                className="flex h-9 w-9 items-center justify-center rounded-full text-[#64748B] transition hover:bg-red-50 hover:text-[#E85D4F]"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                )}
              </div>

              {items.length > 0 && (
                <footer className="border-t border-[#DDE4EA] bg-[#F8FAFC] p-5">
                  <div className="grid gap-3 rounded-[8px] bg-white p-4 text-sm font-semibold text-[#64748B]">
                    <div className="flex justify-between">
                      <span>Subtotal</span>
                      <span className="font-black text-[#111827]">{formatPrice(subtotal)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Shipping</span>
                      <span className={shipping === 0 ? 'font-black text-[#0F766E]' : 'font-black text-[#111827]'}>
                        {shipping === 0 ? 'Free' : formatPrice(shipping)}
                      </span>
                    </div>
                    <div className="flex justify-between border-t border-[#DDE4EA] pt-3 text-lg font-black text-[#111827]">
                      <span>Total</span>
                      <span>{formatPrice(total)}</span>
                    </div>
                  </div>

                  <button
                    onClick={handleCheckout}
                    disabled={hasStockIssues}
                    className={`mt-4 w-full flex items-center justify-center gap-2 py-3.5 rounded-full font-bold text-sm uppercase tracking-wider transition-all ${
                      hasStockIssues
                        ? 'bg-[#E5E7EB] text-[#9CA3AF] cursor-not-allowed'
                        : 'bg-[#1E6A62] text-white hover:bg-[#154D47] shadow-md'
                    }`}
                  >
                    {hasStockIssues ? 'Fix Stock Issues to Proceed' : 'Proceed to Checkout'}
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </footer>
              )}
            </motion.aside>
          </div>
        </div>
      )}
    </AnimatePresence>
  );
}
