import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Heart } from 'lucide-react';
import wishlistService from '../services/wishlistService';
import { getGuestWishlist, saveGuestWishlist } from '../utils/guestCart';
import { motion } from 'framer-motion';

export default function WishlistButton({ productId, initialWishlisted = false, onToggleSuccess }) {
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [isWishlisted, setIsWishlisted] = useState(initialWishlisted);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      setIsWishlisted(initialWishlisted);
    } else {
      const guestList = getGuestWishlist();
      const exists = guestList.some((id) => (typeof id === 'object' ? id.id === productId : id === productId));
      setIsWishlisted(exists);
    }
  }, [initialWishlisted, isAuthenticated, productId]);

  const handleToggle = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      // Guest mode
      const guestList = getGuestWishlist();
      let updated;
      if (isWishlisted) {
        updated = guestList.filter((id) => (typeof id === 'object' ? id.id !== productId : id !== productId));
        setIsWishlisted(false);
        onToggleSuccess?.(productId, false);
      } else {
        updated = [...guestList, productId];
        setIsWishlisted(true);
        onToggleSuccess?.(productId, true);
      }
      saveGuestWishlist(updated);
      window.dispatchEvent(new Event('wishlist-updated'));
      return;
    }

    try {
      setLoading(true);
      if (isWishlisted) {
        await wishlistService.removeFromWishlist(productId);
        setIsWishlisted(false);
        onToggleSuccess?.(productId, false);
      } else {
        await wishlistService.addToWishlist(productId);
        setIsWishlisted(true);
        onToggleSuccess?.(productId, true);
      }
      window.dispatchEvent(new Event('wishlist-updated'));
    } catch (err) {
      console.error('Failed to toggle wishlist:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.button
      onClick={handleToggle}
      disabled={loading}
      whileHover={{ scale: 1.1 }}
      whileTap={{ scale: 0.85 }}
      className="z-10 cursor-pointer rounded-full border border-white/70 bg-white/90 p-2 text-[#71817A] shadow-md transition-colors hover:bg-white hover:text-[#B84F49]"
      aria-label="Wishlist"
    >
      <Heart
        className={`h-4 w-4 transition-colors ${
          isWishlisted
            ? 'fill-[#B84F49] text-[#B84F49]'
            : 'text-[#71817A] hover:text-[#B84F49]'
        }`}
      />
    </motion.button>
  );
}
