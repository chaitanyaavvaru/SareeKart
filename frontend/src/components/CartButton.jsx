import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { ShoppingBag } from 'lucide-react';
import { toggleCart } from '../redux/slices/cartSlice';
import { motion } from 'framer-motion';

export default function CartButton() {
  const dispatch = useDispatch();
  const { items } = useSelector(state => state.cart);
  const cartItemCount = items.reduce((total, item) => total + item.qty, 0);

  return (
    <motion.button 
      whileHover={{ scale: 1.08 }}
      whileTap={{ scale: 0.92 }}
      onClick={() => dispatch(toggleCart())}
      className="relative flex h-9 w-9 items-center justify-center rounded-full text-inherit transition-colors hover:bg-black/5 focus:outline-none sm:h-10 sm:w-10"
      aria-label="Shopping Cart"
    >
      <ShoppingBag className="w-[18px] h-[18px] stroke-[1.8]" />
      {cartItemCount > 0 && (
        <motion.span 
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full border border-white bg-[#B84F49] text-[9px] font-bold text-white shadow-sm"
        >
          {cartItemCount}
        </motion.span>
      )}
    </motion.button>
  );
}
