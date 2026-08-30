import { useSelector, useDispatch } from 'react-redux';
import { X, Check } from 'lucide-react';
import { closeAddedModal, toggleCart } from '../../redux/slices/cartSlice';
import SafeImage from '../common/SafeImage';

export default function AddedToCartModal() {
  const dispatch = useDispatch();
  const { addedItem, showAddedModal } = useSelector(state => state.cart);

  if (!showAddedModal || !addedItem) return null;

  return (
    <div className="fixed inset-0 z-[150] flex items-center justify-center bg-black/60 backdrop-blur-xs">
      {/* Backdrop */}
      <div 
        onClick={() => dispatch(closeAddedModal())}
        className="absolute inset-0"
      ></div>

      {/* Modal Content */}
      <div className="bg-white rounded-3xl p-8 max-w-lg w-full shadow-luxury relative border border-[#F4F4F4] mx-4 animate-scaleUp z-10">
        
        {/* Close button */}
        <button 
          onClick={() => dispatch(closeAddedModal())}
          className="absolute top-4 right-4 p-1 rounded-full text-text-secondary hover:bg-black/5 hover:text-text-primary focus:outline-none transition-colors"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Animated Success Icon */}
        <div className="flex justify-center mb-4">
          <div className="w-12 h-12 rounded-full bg-green-100 text-green-600 flex items-center justify-center">
            <Check className="w-6 h-6" />
          </div>
        </div>

        {/* Modal Title */}
        <h2 className="text-xl sm:text-2xl font-bold font-serif text-text-primary mb-6 text-center">
          Added to Your Collection
        </h2>

        {/* Product Info Row */}
        <div className="flex gap-6 items-center mb-8 pb-6 border-b border-[#F4F4F4]">
          <div className="w-28 h-36 shrink-0 overflow-hidden rounded-xl border border-[#F4F4F4] bg-[#F9F6F1]">
            <SafeImage 
              src={addedItem.image} 
              alt={addedItem.name} 
              productName={addedItem.name}
              category={addedItem.fabric || 'Pure Handloom'}
              aspectRatioClass="aspect-[7/9]"
              className="w-full h-full object-cover"
            />
          </div>

          <div className="space-y-2">
            <h3 className="text-md sm:text-lg font-bold text-text-primary line-clamp-2">
              {addedItem.name}
            </h3>
            <p className="text-lg font-extrabold text-maroon">
              ₹{addedItem.price.toLocaleString('en-IN')}
            </p>
            <p className="text-xs text-text-muted font-medium">
              Qty: 1
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4">
          <button 
            onClick={() => dispatch(closeAddedModal())}
            className="flex-1 py-3.5 border border-[#3A1028] text-[#3A1028] hover:bg-[#FAF8F5] text-sm font-bold rounded-full transition-all cursor-pointer text-center uppercase tracking-wider"
          >
            Continue Shopping
          </button>
          <button 
            onClick={() => {
              dispatch(closeAddedModal());
              dispatch(toggleCart()); // Open cart drawer
            }}
            className="flex-1 py-3.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white text-sm font-bold rounded-full shadow-lg transition-all cursor-pointer text-center uppercase tracking-wider"
          >
            View Cart
          </button>
        </div>

      </div>
    </div>
  );
}
