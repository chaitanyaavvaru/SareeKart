
import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Heart } from 'lucide-react';
import api from '../api/axiosConfig';

export default function WishlistButton({ productId, initialWishlisted = false, onToggleSuccess }) {
  const { user } = useSelector(state => state.auth);
  const [isWishlisted, setIsWishlisted] = useState(() => initialWishlisted);
  const [loading, setLoading] = useState(false);

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
    <button 
      onClick={handleToggle}
      disabled={loading}
      className="p-2 rounded-full bg-white/80 hover:bg-white shadow-md transition-all z-10 cursor-pointer text-text-muted hover:text-accent-red"
      aria-label="Wishlist"
    >
      <Heart className={`w-4 h-4 transition-colors ${isWishlisted ? "fill-accent-red text-accent-red" : "text-text-muted hover:text-accent-red"}`} />
    </button>
  );
}
