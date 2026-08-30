
import { useSelector, useDispatch } from 'react-redux';
import { ShoppingCart } from 'lucide-react';
import { toggleCart } from '../redux/slices/cartSlice';

export default function CartButton() {
  const dispatch = useDispatch();
  const { items } = useSelector(state => state.cart);
  const cartItemCount = items.reduce((total, item) => total + item.qty, 0);

  return (
    <button 
      onClick={() => dispatch(toggleCart())}
      className="relative p-2 rounded-full hover:bg-white/10 transition-colors focus:outline-none cursor-pointer"
      aria-label="Shopping Cart"
    >
      <ShoppingCart className="w-5.5 h-5.5" />
      {cartItemCount > 0 && (
        <span className="absolute top-0.5 right-0.5 bg-gold text-maroon font-bold text-xs w-5 h-5 rounded-full flex items-center justify-center border-2 border-maroon">
          {cartItemCount}
        </span>
      )}
    </button>
  );
}
