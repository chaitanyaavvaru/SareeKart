
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import ProductCard from './ProductCard';
import { HOMEPAGE_PRODUCTS } from '../data/products';

export default function NewArrivals({ wishlistProductIds = [], handleAddSuccess, handleWishlistToggle }) {
  const navigate = useNavigate();
  // Filter products for New Arrivals (e.g. products 2 and 4)
  const arrivalProducts = HOMEPAGE_PRODUCTS.slice(1, 3);

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 bg-[#F9F6F1]">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
        
        {/* Left Column: Editorial Banner */}
        <motion.div 
          initial={{ opacity: 0, x: -30 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="lg:col-span-4 rounded-3xl bg-[#2B0F1E] text-white p-8 sm:p-12 flex flex-col justify-between relative overflow-hidden shadow-luxury min-h-[400px]"
        >
          {/* Background Silk Texture Overlay */}
          <div className="absolute inset-0 opacity-20 bg-cover bg-center pointer-events-none" style={{ backgroundImage: "url('https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?w=800')" }}></div>
          
          <div className="space-y-4 z-10">
            <div className="flex items-center gap-2">
              <span className="h-px w-6 bg-[#C89B3C]"></span>
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">
                Just Launched
              </span>
            </div>
            <h2 className="text-3xl sm:text-5xl font-serif font-bold text-white leading-tight">
              New Arrivals
            </h2>
            <p className="text-xs text-white/70 font-sans leading-relaxed max-w-xs font-light">
              Presenting fresh loom drops featuring fine double-warp zari embroidery and custom block dyes.
            </p>
          </div>

          <div className="pt-8 z-10">
            <button 
              onClick={() => navigate('/products')}
              className="px-7 py-3.5 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl text-xs uppercase tracking-widest transition-colors cursor-pointer shadow-lg"
            >
              Explore New Drops
            </button>
          </div>
        </motion.div>

        {/* Right Column: 2 Asymmetric Product Cards */}
        <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-8">
          {arrivalProducts.map((prod, idx) => (
            <motion.div 
              key={prod.id}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.8, delay: idx * 0.15, ease: "easeOut" }}
            >
              <ProductCard
                product={prod}
                initialWishlisted={wishlistProductIds.includes(prod.id)}
                onAddSuccess={handleAddSuccess}
                onWishlistToggle={handleWishlistToggle}
              />
            </motion.div>
          ))}
        </div>

      </div>
    </section>
  );
}
