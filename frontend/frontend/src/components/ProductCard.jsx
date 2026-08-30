
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { memo, useState } from 'react';
import { motion } from 'framer-motion';
import { addToCart } from '../redux/slices/cartSlice';
import WishlistButton from './WishlistButton';
import Rating from './Rating';
import { ShoppingBag, Eye } from 'lucide-react';
import SafeImage from './common/SafeImage';

function ProductCard({ product, onAddSuccess, initialWishlisted = false, onWishlistToggle }) {
  const dispatch = useDispatch();
  const [isHovered, setIsHovered] = useState(false);

  const handleAddToCart = (e) => {
    e.preventDefault();
    e.stopPropagation();
    dispatch(addToCart(product));
    if (onAddSuccess) {
      onAddSuccess(product);
    }
  };

  const handleBuyNow = (e) => {
    e.preventDefault();
    e.stopPropagation();
    dispatch(addToCart(product));
    if (onAddSuccess) {
      onAddSuccess(product, true);
    }
  };

  const productImage = product.images && product.images.length > 0 
    ? product.images[0] 
    : (product.image || "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600");

  const alternateImage = product.images && product.images.length > 1
    ? product.images[1]
    : "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600";

  const originalPrice = product.price * 1.25;

  return (
    <motion.div 
      initial={{ opacity: 0, y: 12 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-40px" }}
      transition={{ duration: 0.45, ease: "easeOut" }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      className="group bg-white rounded-[28px] overflow-hidden border border-[#F4F2EB] transition-all duration-500 flex flex-col h-full relative hover:shadow-[0_16px_40px_rgba(44,15,31,0.10)] hover:-translate-y-1 md:min-h-[500px] lg:min-h-[540px]"
    >
      {/* Image Container */}
      <div className="relative aspect-[3/4] bg-[#F9F6F1] overflow-hidden">
        
        <Link to={`/products/${product.id}`} className="block w-full h-full relative">
          <SafeImage
            src={isHovered ? alternateImage : productImage}
            fallbackSrc={productImage}
            alt={product.name}
            productName={product.name}
            category={product.fabric || 'Pure Handloom'}
            className="group-hover:scale-103 transition-all duration-700 ease-out object-top"
          />
          {/* Gold shimmer weave glow overlay on hover */}
          <div className="absolute inset-0 bg-gradient-to-tr from-[#C89B3C]/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-700 pointer-events-none"></div>
        </Link>

        {/* Wishlist Heart Overlay (Top-Right) */}
        <div className="absolute top-4 right-4 z-20">
          <WishlistButton 
            productId={product.id} 
            initialWishlisted={initialWishlisted}
            onToggleSuccess={onWishlistToggle}
          />
        </div>

        {/* Elegant Badges Row (Top-Left) */}
        <div className="absolute top-4 left-4 flex flex-col gap-1.5 z-10 pointer-events-none">
          {product.price > 8000 ? (
            <span className="bg-[#2B0F1E] text-white text-[8px] font-bold uppercase tracking-[0.15em] px-2.5 py-1.5 rounded-md shadow-xs border border-[#C89B3C]/30">
              Exclusive
            </span>
          ) : product.price > 4000 ? (
            <span className="bg-[#C89B3C] text-[#2B0F1E] text-[8px] font-bold uppercase tracking-[0.15em] px-2.5 py-1.5 rounded-md shadow-xs">
              Handwoven
            </span>
          ) : (
            <span className="bg-white/90 backdrop-blur-xs text-[#2B0F1E] text-[8px] font-bold uppercase tracking-[0.15em] px-2.5 py-1.5 rounded-md shadow-xs border border-[#E6DFD3]">
              Limited
            </span>
          )}
        </div>

        {/* Hover Slide-up Actions Drawer */}
        <div className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/85 via-black/40 to-transparent translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out flex flex-col gap-2 z-10">
          <button 
            onClick={handleBuyNow}
            className="w-full py-2.5 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] text-xs font-bold rounded-xl uppercase tracking-widest transition-colors cursor-pointer flex items-center justify-center gap-1.5 shadow-sm"
          >
            Buy Now
          </button>
          <div className="grid grid-cols-2 gap-2">
            <button 
              onClick={handleAddToCart}
              className="py-2 bg-white/20 hover:bg-white/30 text-white text-[11px] font-bold rounded-lg uppercase tracking-wider transition-colors cursor-pointer flex items-center justify-center gap-1"
            >
              <ShoppingBag className="w-3.5 h-3.5" /> + Cart
            </button>
            <Link
              to={`/products/${product.id}`}
              className="py-2 bg-white/20 hover:bg-white/30 text-white text-[11px] font-bold rounded-lg uppercase tracking-wider transition-colors cursor-pointer flex items-center justify-center gap-1"
            >
              <Eye className="w-3.5 h-3.5" /> View
            </Link>
          </div>
        </div>
      </div>

      {/* Info Section */}
      <div className="p-5 flex-grow flex flex-col justify-between text-center font-sans space-y-2">
        <div className="space-y-1">
          <span className="text-[10px] text-[#C89B3C] font-bold tracking-widest uppercase block font-sans">
            {product.fabric || 'Pure Handloom'}
          </span>
          <Link to={`/products/${product.id}`} className="block">
            <h3 className="font-serif font-bold text-[#2B0F1E] text-[15px] leading-snug line-clamp-2 mt-1 hover:text-[#C89B3C] transition-colors min-h-[42px]">
              {product.name}
            </h3>
          </Link>
        </div>

        {/* Price reveals/fades-in with transition on hover */}
        <div className="space-y-2 pt-2 border-t border-[#F4F2EB]">
          <div className="flex items-center justify-center gap-2 transition-all duration-300">
            <span className="text-md font-extrabold text-[#2B0F1E]">₹{product.price.toLocaleString('en-IN')}</span>
            <span className="text-xs text-text-secondary line-through font-light opacity-60 group-hover:opacity-100 transition-opacity">
              ₹{originalPrice.toLocaleString('en-IN', {maximumFractionDigits:0})}
            </span>
          </div>
          
          <div className="flex items-center justify-center opacity-80 group-hover:opacity-100 transition-opacity">
            <Rating value={4.8} reviewsCount={24} />
          </div>
        </div>
      </div>
    </motion.div>
  );
}

export default memo(ProductCard);
