import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Heart } from 'lucide-react';
import api from '../api/axiosConfig';

import { motion } from 'framer-motion';

export default function WishlistButton({ productId, initialWishlisted = false, onToggleSuccess }) {
  const { user } = useSelector(state => state.auth);
  const [isWishlisted, setIsWishlisted] = useState(initialWishlisted);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setIsWishlisted(initialWishlisted);
  }, [initialWishlisted]);

  const handleToggle = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!user) {
      alert("Please login to manage your wishlist.");
      return;
    }
    try {
      setLoading(true);
      if (isWishlisted) {
        await api.delete(`/wishlist/${productId}`);
        setIsWishlisted(false);
        if (onToggleSuccess) onToggleSuccess(productId, false);
      } else {
        await api.post(`/wishlist/${productId}`);
        setIsWishlisted(true);
        if (onToggleSuccess) onToggleSuccess(productId, true);
      }
    } catch (err) {
      console.error("Failed to toggle wishlist", err);
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
      <Heart className={`h-4 w-4 transition-colors ${isWishlisted ? "fill-[#B84F49] text-[#B84F49]" : "text-[#71817A] hover:text-[#B84F49]"}`} />
    </motion.button>
  );
}
