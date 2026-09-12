import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Heart, ShoppingBag, Trash2, ArrowRight, Sparkles, ShieldCheck } from 'lucide-react';
import api from '../../api/axiosConfig';
import { addToCart } from '../../redux/slices/cartSlice';
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
  const navigate = useNavigate();
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadWishlist = async () => {
      setLoading(true);
      if (isAuthenticated) {
        try {
          const res = await api.get('/wishlist');
          if (res.data?.success && Array.isArray(res.data.data)) {
            setItems(res.data.data);
          } else {
            setItems([]);
          }
        } catch (err) {
          console.error('Failed to load wishlist from server', err);
          // Fallback to local storage
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
        setItems(saved);
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
        await api.delete(`/wishlist/${productId}`);
      } catch (err) {
        console.error('Failed to delete wishlist item', err);
      }
    } else {
      const saved = JSON.parse(localStorage.getItem('sareekart_wishlist') || '[]');
      const updated = saved.filter((item) => item.id !== productId);
      localStorage.setItem('sareekart_wishlist', JSON.stringify(updated));
    }
  };

  const handleMoveToBag = (product) => {
    dispatch(
      addToCart({
        id: product.id,
        name: product.name,
        price: product.price,
        image: product.image || (product.images && product.images[0]) || '',
      })
    );
    handleRemove(product.id);
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F] font-sans text-left">
      <SEO
        title="My Wishlist | SareeKart"
        description="Your curated handloom wishlist. Review saved Kanchipuram, Banarasi, and pure silk sarees."
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
            <div className="space-y-2">
              <h3 className="text-2xl font-serif font-bold text-[#17211F]">Your wishlist is waiting.</h3>
              <p className="text-xs text-[#71817A] max-w-md mx-auto leading-relaxed">
                As you explore our handloom catalog, tap the heart icon on any saree to save it here for wedding celebrations or festive gifting.
              </p>
            </div>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-3 bg-[#17211F] hover:bg-[#1E6A62] text-white text-xs font-bold uppercase tracking-widest rounded-full transition-all shadow-md"
            >
              Explore the Edit <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <AnimatePresence>
              {items.map((product) => (
                <motion.div
                  key={product.id}
                  layout
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="bg-white border border-[#DDD8CF] rounded-3xl overflow-hidden shadow-xs hover:shadow-lg transition-all flex flex-col justify-between group"
                >
                  <div className="relative aspect-[3/4] overflow-hidden bg-[#F7F4EE]">
                    <img
                      src={product.image || (product.images && product.images[0]) || 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80'}
                      alt={product.name}
                      className="w-full h-full object-cover object-top group-hover:scale-105 transition-transform duration-500"
                    />
                    <button
                      onClick={() => handleRemove(product.id)}
                      className="absolute top-3 right-3 p-2 bg-white/90 backdrop-blur-xs rounded-full text-[#71817A] hover:text-[#B84F49] hover:bg-white transition-all shadow-xs cursor-pointer"
                      title="Remove from wishlist"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                    {product.fabric && (
                      <span className="absolute bottom-3 left-3 px-3 py-1 bg-[#17211F]/80 backdrop-blur-xs text-white text-[10px] font-bold uppercase tracking-wider rounded-full">
                        {product.fabric}
                      </span>
                    )}
                  </div>

                  <div className="p-5 space-y-3 flex-1 flex flex-col justify-between">
                    <div>
                      <Link
                        to={`/products/${product.id}`}
                        className="font-serif font-bold text-base text-[#17211F] hover:text-[#1E6A62] transition-colors line-clamp-1"
                      >
                        {product.name}
                      </Link>
                      <p className="text-xs font-bold text-[#1E6A62] mt-1">{formatCurrency(product.price)}</p>
                    </div>

                    <div className="pt-3 border-t border-[#F7F4EE] flex gap-2">
                      <button
                        onClick={() => handleMoveToBag(product)}
                        className="flex-1 py-2.5 bg-[#17211F] hover:bg-[#1E6A62] text-white text-xs font-bold uppercase tracking-widest rounded-full flex items-center justify-center gap-1.5 transition-all shadow-xs cursor-pointer"
                      >
                        <ShoppingBag className="w-3.5 h-3.5" /> Move to Bag
                      </button>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </section>
    </div>
  );
}
