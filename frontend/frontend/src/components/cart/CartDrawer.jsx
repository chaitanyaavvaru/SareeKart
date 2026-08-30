import { X, Trash2, Plus, Minus, ArrowRight, ShoppingBag } from 'lucide-react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { toggleCart, updateQty, removeItem } from '../../redux/slices/cartSlice';
import { motion, AnimatePresence } from 'framer-motion';
import SafeImage from '../common/SafeImage';

export default function CartDrawer() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  
  const { items, isOpen } = useSelector(state => state.cart);
  const { isAuthenticated } = useSelector(state => state.auth);

  const subtotal = items.reduce((sum, item) => sum + (item.price * item.qty), 0);
  const shipping = subtotal > 5000 || subtotal === 0 ? 0 : 150;
  const total = subtotal + shipping;

  const handleCheckout = () => {
    dispatch(toggleCart());
    if (isAuthenticated) {
      navigate('/checkout');
    } else {
      navigate('/login?redirect=checkout');
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[100] overflow-hidden">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            onClick={() => dispatch(toggleCart())}
            className="absolute inset-0 bg-black/50 backdrop-blur-xs transition-opacity"
          ></motion.div>

          <div className="absolute inset-y-0 right-0 max-w-full flex">
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.3 }}
              className="w-screen max-w-md bg-white shadow-2xl flex flex-col h-full border-l border-border"
            >
              
              {/* Header */}
              <div className="px-4 py-6 sm:px-6 border-b border-border flex items-center justify-between bg-cream">
                <h2 className="text-lg font-bold text-maroon flex items-center gap-2">
                  <ShoppingBag className="w-5 h-5" /> Shopping Cart ({items.length})
                </h2>
                <button
                  onClick={() => dispatch(toggleCart())}
                  className="p-1 rounded-full text-text-secondary hover:bg-black/5 hover:text-text-primary focus:outline-none"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Shipping Progress Bar */}
              {items.length > 0 && subtotal < 999 && (
                <div className="px-4 sm:px-6 pt-4 pb-2 space-y-2">
                  <div className="h-1 bg-[#F4F4F4] rounded-full overflow-hidden">
                    <div
                      className="h-full bg-[#C9A227] rounded-full transition-all duration-500"
                      style={{ width: `${Math.min((subtotal / 999) * 100, 100)}%` }}
                    ></div>
                  </div>
                  <p className="text-xs text-[#C9A227] font-medium text-center">
                    Add ₹{(999 - subtotal).toLocaleString('en-IN')} more for FREE shipping
                  </p>
                </div>
              )}

              {/* Cart Items List */}
              <div className="flex-1 overflow-y-auto py-4 px-4 sm:px-6 divide-y divide-border">
                {items.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full gap-4 text-center py-12">
                    <ShoppingBag className="w-16 h-16 text-text-muted stroke-[1.5]" />
                    <div>
                      <p className="text-md font-semibold text-text-primary">Your cart is empty</p>
                      <p className="text-sm text-text-secondary mt-1">Explore our exquisite collection to add sarees.</p>
                    </div>
                    <button
                      onClick={() => {
                        dispatch(toggleCart());
                        navigate('/products');
                      }}
                      className="mt-2 px-6 py-2 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-semibold text-sm rounded-full shadow-md transition-all"
                    >
                      Shop Sarees
                    </button>
                  </div>
                ) : (
                  items.map((item) => (
                    <div key={item.id} className="py-4 flex gap-4">
                      <div className="w-20 h-28 shrink-0 overflow-hidden rounded-xl border border-border bg-[#F9F6F1]">
                        <SafeImage 
                          src={item.image} 
                          alt={item.name}
                          productName={item.name}
                          category={item.fabric || 'Pure Handloom'}
                          aspectRatioClass="aspect-[5/7]"
                          className="w-full h-full object-cover"
                        />
                      </div>

                      <div className="flex-1 flex flex-col justify-between">
                        <div>
                          <h4 className="text-sm font-semibold text-text-primary line-clamp-2">{item.name}</h4>
                          <p className="text-sm font-bold text-maroon mt-1">₹{item.price.toLocaleString('en-IN')}</p>
                        </div>

                        <div className="flex items-center justify-between mt-2">
                          {/* Quantity Controls */}
                          <div className="flex items-center gap-1 bg-cream rounded-full px-1.5 py-0.5">
                            <button 
                              onClick={() => dispatch(updateQty({ id: item.id, qty: item.qty - 1 }))}
                              className="rounded-full w-7 h-7 border border-[#F4F4F4] hover:border-[#C9A227] text-[#3A1028] flex items-center justify-center transition-colors"
                            >
                              <Minus className="w-3.5 h-3.5" />
                            </button>
                            <span className="px-2 text-sm font-bold text-text-primary min-w-[20px] text-center">{item.qty}</span>
                            <button 
                              onClick={() => dispatch(updateQty({ id: item.id, qty: item.qty + 1 }))}
                              className="rounded-full w-7 h-7 border border-[#F4F4F4] hover:border-[#C9A227] text-[#3A1028] flex items-center justify-center transition-colors"
                            >
                              <Plus className="w-3.5 h-3.5" />
                            </button>
                          </div>

                          {/* Remove Button */}
                          <button 
                            onClick={() => dispatch(removeItem(item.id))}
                            className="text-text-muted hover:text-error p-1 rounded-full hover:bg-red-50 transition-colors"
                          >
                            <Trash2 className="w-4.5 h-4.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* Footer Summary & Checkout */}
              {items.length > 0 && (
                <div className="border-t border-border bg-cream p-4 sm:p-6 space-y-4">
                  <div className="space-y-2 text-sm text-text-secondary">
                    <div className="flex justify-between">
                      <span>Subtotal</span>
                      <span className="font-semibold text-text-primary">₹{subtotal.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Shipping</span>
                      <span>{shipping === 0 ? <span className="text-success font-semibold">FREE</span> : `₹${shipping}`}</span>
                    </div>
                    {shipping > 0 && (
                      <p className="text-xs text-gold-dark font-medium">Add ₹{(5000 - subtotal).toLocaleString('en-IN')} more for FREE shipping!</p>
                    )}
                    <div className="flex justify-between text-base font-bold text-text-primary border-t border-border-dark pt-3 mt-1">
                      <span>Total Amount</span>
                      <span className="text-maroon">₹{total.toLocaleString('en-IN')}</span>
                    </div>
                  </div>

                  <button
                    onClick={handleCheckout}
                    className="w-full flex items-center justify-center gap-2 py-3.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl shadow-lg transition-all cursor-pointer group"
                  >
                    Proceed to Checkout <ArrowRight className="w-4.5 h-4.5 group-hover:translate-x-1 transition-transform" />
                  </button>
                </div>
              )}

            </motion.div>
          </div>
        </div>
      )}
    </AnimatePresence>
  );
}
