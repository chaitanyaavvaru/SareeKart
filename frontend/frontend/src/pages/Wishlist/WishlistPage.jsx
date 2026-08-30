import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Heart, ShoppingBag, Trash2, Loader2, ArrowRight } from 'lucide-react';
import api from '../../api/axiosConfig';
import SafeImage from '../../components/common/SafeImage';
import { addToCart } from '../../redux/slices/cartSlice';
import SEO from '../../components/common/SEO';

export default function WishlistPage() {
  const dispatch = useDispatch();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchWishlist = async () => {
    try {
      setLoading(true); setError(null);
      const res = await api.get('/wishlist');
      const data = res.data?.data;
      const list = data?.items || data || [];
      setItems(Array.isArray(list) ? list : []);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load wishlist');
    } finally { setLoading(false); }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchWishlist();
  }, []);

  const handleRemove = async (productId) => {
    try { await api.delete(`/wishlist/${productId}`); setItems(prev => prev.filter(i => (i.product?.id || i.id) !== productId)); } catch (e) { alert(e.response?.data?.message || 'Remove failed'); }
  };

  const handleAddToCart = (product) => {
    dispatch(addToCart({ id: product.id, name: product.name, price: product.price, image: product.images?.[0] || product.image, qty: 1 }));
  };

  const handleClear = async () => {
    if (!window.confirm('Clear entire wishlist?')) return;
    for (const it of items) {
      const pid = it.product?.id || it.id;
      try { await api.delete(`/wishlist/${pid}`); } catch { /* ignore */ }
    }
    setItems([]);
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] py-10">
      <SEO title="Wishlist | SareeKart" description="Your saved handwoven sarees – wishlist curated for you." />
      <div className="max-w-[1320px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl md:text-4xl font-serif font-bold text-[#2B0F1E] flex items-center gap-3">
              <Heart className="w-7 h-7 text-[#C89B3C] fill-[#C89B3C]/20" /> My Wishlist
            </h1>
            <p className="text-sm text-text-secondary mt-2 font-light">Save heritage drapes for later – your wishlist is synced to your account.</p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs font-bold text-text-muted border border-border bg-white px-3 py-1.5 rounded-full">{items.length} items</span>
            {items.length > 0 && <button onClick={handleClear} className="px-4 py-2 bg-white border border-border hover:bg-red-50 hover:border-red-200 hover:text-red-700 rounded-xl text-xs font-bold flex items-center gap-1.5"><Trash2 className="w-4 h-4" /> Clear</button>}
          </div>
        </div>

        {loading ? (
          <div className="min-h-[40vh] flex flex-col items-center justify-center gap-3"><Loader2 className="w-8 h-8 text-maroon animate-spin" /><p className="text-xs text-text-secondary">Loading wishlist…</p></div>
        ) : error ? (
          <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-xl text-sm">{error} <button onClick={fetchWishlist} className="ml-2 underline font-bold">Retry</button></div>
        ) : items.length === 0 ? (
          <div className="bg-white border border-border rounded-2xl p-12 text-center space-y-4 shadow-soft">
            <div className="w-16 h-16 rounded-full bg-[#FAF8F5] border border-border mx-auto flex items-center justify-center text-[#C89B3C]"><Heart className="w-7 h-7" /></div>
            <h3 className="text-lg font-bold font-serif text-[#2B0F1E]">Your wishlist is empty</h3>
            <p className="text-sm text-text-secondary max-w-md mx-auto">Tap the heart on any saree to save it. Your wishlist syncs across devices when you’re signed in.</p>
            <Link to="/products" className="inline-flex items-center gap-2 px-6 py-3 bg-[#2B0F1E] hover:bg-[#200b16] text-white rounded-xl text-xs font-bold uppercase tracking-widest">Browse Collection <ArrowRight className="w-4 h-4" /></Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {items.map((entry) => {
              const p = entry.product || entry;
              const img = p.images?.[0] || p.image || 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600';
              return (
                <div key={p.id} className="bg-white rounded-[20px] border border-border overflow-hidden flex flex-col hover:shadow-luxury hover:-translate-y-1 transition-all group">
                  <Link to={`/products/${p.id}`} className="relative aspect-[4/5] bg-[#F9F6F1] overflow-hidden block">
                    <SafeImage src={img} alt={p.name} productName={p.name} category={p.fabric || 'Handloom'} className="group-hover:scale-103 transition-transform duration-700" />
                    <div className="absolute top-3 left-3 bg-white/90 backdrop-blur px-2.5 py-1 rounded-full text-[10px] font-bold tracking-widest uppercase border border-border">{p.fabric || 'Handloom'}</div>
                  </Link>
                  <div className="p-4 flex flex-col gap-3 flex-1">
                    <Link to={`/products/${p.id}`} className="font-serif font-bold text-sm text-[#2B0F1E] line-clamp-2 hover:text-[#C89B3C] transition-colors min-h-[40px]">{p.name}</Link>
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-extrabold text-[#2B0F1E]">₹{Number(p.price).toLocaleString('en-IN')}</span>
                      <span className="text-[11px] text-text-muted">{p.stockQuantity != null ? `${p.stockQuantity} in stock` : ''}</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 mt-auto">
                      <button onClick={() => handleAddToCart(p)} className="py-2.5 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] rounded-xl text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-1.5"><ShoppingBag className="w-3.5 h-3.5" /> Add</button>
                      <button onClick={() => handleRemove(p.id)} className="py-2.5 bg-white border border-border hover:bg-red-50 hover:border-red-200 hover:text-red-700 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5"><Trash2 className="w-3.5 h-3.5" /> Remove</button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
