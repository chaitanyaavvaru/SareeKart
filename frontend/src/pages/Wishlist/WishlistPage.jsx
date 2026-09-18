import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Heart, ShoppingBag, Trash2, ArrowRight, Sparkles, ShieldCheck } from 'lucide-react';
import wishlistService from '../../services/wishlistService';
import { addToCart, setCartFromServer } from '../../redux/slices/cartSlice';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

export default function WishlistPage() {
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state) => state.auth);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    const loadWishlist = async () => {
      setLoading(true);
      if (isAuthenticated) {
        try {
          // Sync any offline guest wishlist items
          const savedLocal = JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]');
          if (Array.isArray(savedLocal) && savedLocal.length > 0) {
            const productIds = savedLocal.map((i) => (typeof i === 'object' ? i.id : i)).filter(Boolean);
            if (productIds.length > 0) {
              await wishlistService.syncGuestWishlist(productIds);
            }
            localStorage.removeItem('sareekart_wishlist');
          }

          const res = await wishlistService.getWishlist();
          if (res?.success && Array.isArray(res.data)) {
            setItems(res.data);
          } else {
            setItems([]);
          }
        } catch (err) {
          console.error('Failed to load wishlist from server', err);
          loadLocalWishlist();
        }
      } else {
        loadLocalWishlist();
      }
      setLoading(false);
    };

    const loadLocalWishlist = () => {
      try {
        const saved = JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]');
        setItems(Array.isArray(saved) ? saved : []);
      } catch {
        setItems([]);
      }
    };

    loadWishlist();
  }, [isAuthenticated]);

  const handleRemove = async (productId) => {
    setItems((prev) => prev.filter((item) => item.id !== productId));
    if (isAuthenticated) {
      try {
        await wishlistService.removeFromWishlist(productId);
      } catch (err) {
        console.error('Failed to delete wishlist item', err);
      }
    } else {
      const saved = JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]');
      const updated = saved.filter((item) => (item.id || item) !== productId);
      localStorage.setItem('sareekart_wishlist', JSON.stringify(updated));
    }
  };

  const handleMoveToBag = async (product) => {
    setActionError(null);
    if (isAuthenticated) {
      try {
        const res = await wishlistService.moveWishlistToCart(product.id);
        if (res?.data) {
          dispatch(setCartFromServer(res.data));
        }
        setItems((prev) => prev.filter((item) => item.id !== product.id));
      } catch (err) {
        console.error('Failed to move to bag:', err);
        setActionError(err.response?.data?.message || 'Unable to move saree to shopping bag.');
      }
    } else {
      dispatch(
        addToCart({
          id: product.id,
          name: product.name,
          price: product.price,
          image: product.image || (product.images && product.images[0]) || '',
          stockQuantity: product.stockQuantity,
          active: product.active,
        })
      );
      handleRemove(product.id);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F] font-sans text-left">
      <SEO
        title="My Wishlist | SareeKart"
        description="Your curated handloom wishlist. Review saved Kanchipuram, Banarasi, and pure silk sarees."
        noindex={true}
      />

      <section className="bg-white border-b border-[#DDD8CF] py-12 px-6 lg:px-16">
        <div className="max-w-6xl mx-auto flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-[#1E6A62]">
              <Heart className="w-4 h-4 fill-[#1E6A62]" /> Personal Collection
            </div>
            <h1 className="text-3xl sm:text-5xl font-serif font-bold text-[#17211F] mt-2">
              Saved for Special Moments.
            </h1>
            <p className="text-sm text-[#71817A] mt-2 max-w-xl">
              Revisit your favored weaves, compare zari details, and move styles directly to your shopping bag when you are ready to drape.
            </p>
          </div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#71817A] bg-[#F7F4EE] px-4 py-2 rounded-full border border-[#DDD8CF]">
            {items.length} {items.length === 1 ? 'Saree' : 'Sarees'} Saved
          </p>
        </div>
      </section>

      {actionError && (
        <div className="max-w-6xl mx-auto px-6 lg:px-16 pt-6">
          <div className="bg-[#FEF2F2] border border-[#FCA5A5] text-[#991B1B] text-sm p-4 rounded-xl flex items-center justify-between">
            <span>{actionError}</span>
            <button onClick={() => setActionError(null)} className="font-bold underline text-xs">Dismiss</button>
          </div>
        </div>
      )}

      <section className="max-w-6xl mx-auto px-6 lg:px-16 py-12">
        {loading ? (
          <div className="flex justify-center items-center py-20 text-[#71817A]">
            <div className="animate-spin rounded-full h-8 w-8 border-2 border-[#DDD8CF] border-t-[#1E6A62]" />
          </div>
        ) : items.length === 0 ? (
          <div className="bg-white border border-[#DDD8CF] rounded-3xl p-12 text-center max-w-xl mx-auto space-y-6 shadow-xs">
            <div className="w-16 h-16 rounded-full bg-[#F7F4EE] text-[#1E6A62] flex items-center justify-center mx-auto">
              <Heart className="w-8 h-8" />
            </div>
            <div>
              <h2 className="text-2xl font-serif font-bold text-[#17211F]">Your wishlist is waiting</h2>
              <p className="text-sm text-[#71817A] mt-2">
                Explore our handpicked curation of handloom sarees and tap the heart icon to save the weaves you love.
              </p>
            </div>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 bg-[#1E6A62] text-white px-8 py-3.5 rounded-full font-bold text-sm tracking-wider uppercase hover:bg-[#154D47] transition-all shadow-md"
            >
              Explore the Edit <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
            <AnimatePresence>
              {items.map((product) => {
                const img = product.images && product.images.length > 0 ? product.images[0] : (product.image || '');
                const isOutOfStock = product.stockQuantity <= 0;

                return (
                  <motion.div
                    key={product.id}
                    layout
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9 }}
                    className="bg-white rounded-2xl border border-[#DDD8CF] overflow-hidden flex flex-col group relative shadow-xs hover:shadow-md transition-shadow"
                  >
                    <div className="relative aspect-3/4 overflow-hidden bg-[#EFECE6]">
                      {img ? (
                        <img
                          src={img}
                          alt={product.name}
                          className="w-full h-full object-cover object-top group-hover:scale-105 transition-transform duration-500"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-[#71817A] font-serif text-sm">
                          Handloom Silk Saree
                        </div>
                      )}

                      <button
                        onClick={() => handleRemove(product.id)}
                        className="absolute top-3 right-3 w-9 h-9 rounded-full bg-white/90 backdrop-blur-md flex items-center justify-center text-[#71817A] hover:text-[#991B1B] hover:bg-white transition-colors shadow-xs"
                        title="Remove from wishlist"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>

                      {isOutOfStock && (
                        <div className="absolute bottom-3 left-3 bg-[#17211F]/80 backdrop-blur-md text-white text-[11px] font-bold px-3 py-1 rounded-full uppercase tracking-wider">
                          Out of Stock
                        </div>
                      )}
                    </div>

                    <div className="p-5 flex flex-col flex-1 justify-between gap-4">
                      <div>
                        <p className="text-[11px] font-bold uppercase tracking-widest text-[#B3874B]">
                          {product.fabric || product.category?.name || 'Artisanal Weave'}
                        </p>
                        <Link to={`/products/${product.id}`} className="hover:underline">
                          <h3 className="font-serif font-bold text-lg text-[#17211F] line-clamp-1 mt-1">
                            {product.name}
                          </h3>
                        </Link>
                        <p className="text-base font-bold text-[#1E6A62] mt-2">
                          {formatCurrency(product.price)}
                        </p>
                      </div>

                      <div className="pt-3 border-t border-[#F7F4EE] flex items-center gap-2">
                        <button
                          onClick={() => handleMoveToBag(product)}
                          disabled={isOutOfStock}
                          className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all ${
                            isOutOfStock
                              ? 'bg-[#E5E7EB] text-[#9CA3AF] cursor-not-allowed'
                              : 'bg-[#1E6A62] text-white hover:bg-[#154D47] shadow-xs'
                          }`}
                        >
                          <ShoppingBag className="w-3.5 h-3.5" />
                          {isOutOfStock ? 'Sold Out' : 'Move to Bag'}
                        </button>
                        <Link
                          to={`/products/${product.id}`}
                          className="px-3 py-2.5 rounded-xl border border-[#DDD8CF] text-[#71817A] hover:text-[#17211F] hover:bg-[#F7F4EE] text-xs font-bold transition-colors"
                        >
                          Details
                        </Link>
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>
          </div>
        )}
      </section>
    </div>
  );
}
