import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';
import SafeImage from './common/SafeImage';

export default function Hero() {
  const navigate = useNavigate();

  return (
    <section className="relative w-full min-h-[90vh] bg-[#FAF8F5] text-[#3A0F1F] flex items-center overflow-hidden select-none border-b border-[#E6DFD3]">
      <div className="max-w-[1640px] w-full mx-auto px-6 sm:px-12 md:px-16 py-16 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center z-10 text-left">
        
        {/* Left Column: Typography & Action Buttons */}
        <div className="lg:col-span-6 space-y-8">
          <motion.div
            initial={{ opacity: 0, y: 25 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
            className="space-y-6"
          >
            <div className="flex items-center gap-3">
              <span className="h-px w-8 bg-[#C8A04D]"></span>
              <span className="text-[#C8A04D] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-2">
                <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Artisanal Handloom Collection
              </span>
            </div>
            
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-serif font-bold text-[#3A0F1F] leading-[1.08] tracking-tight">
              Heritage Woven <br />
              <span className="text-[#C8A04D] italic font-normal block mt-1">In Pure Silk & Zari</span>
            </h1>
            
            <p className="text-sm sm:text-base text-[#6b5c4d] max-w-xl font-sans font-normal leading-relaxed">
              Explore curated collections of pure Kanchipuram tissue, Varanasi brocade, and Patola handlooms directly from master weaver guilds.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="pt-2 flex flex-wrap gap-4"
          >
            <button 
              onClick={() => navigate('/products')}
              className="inline-flex items-center justify-center gap-3 h-12 px-8 bg-[#3A0F1F] hover:bg-[#5B1832] text-[#C8A04D] font-bold rounded-full shadow-soft hover:shadow-luxury transition-all duration-300 group font-sans text-xs tracking-widest uppercase cursor-pointer focus-visible:ring-2 focus-visible:ring-[#C8A04D]"
            >
              Explore Masterpieces <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform duration-300" />
            </button>
            
            <button 
              onClick={() => navigate('/products?category=Bridal')}
              className="inline-flex items-center justify-center gap-3 h-12 px-8 border border-[#3A0F1F] hover:bg-[#3A0F1F] text-[#3A0F1F] hover:text-[#C8A04D] font-bold rounded-full transition-all duration-300 font-sans text-xs tracking-widest uppercase cursor-pointer focus-visible:ring-2 focus-visible:ring-[#C8A04D]"
            >
              Bridal Collection
            </button>
          </motion.div>
        </div>

        {/* Right Column: Editorial Showcase */}
        <div className="lg:col-span-6 relative">
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
            className="w-full h-[520px] sm:h-[620px] rounded-3xl overflow-hidden shadow-luxury border border-[#E6DFD3] bg-white relative"
          >
            <SafeImage 
              src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1200&q=85" 
              alt="Luxury Indian bride in authentic Kanchipuram silk saree with gold zari temple details"
              productName="Loom Heritage Cover"
              category="Campaign Banner"
              className="w-full h-full object-cover object-top hover:scale-102 transition-transform duration-700"
            />
            <div className="absolute bottom-6 left-6 right-6 p-6 bg-white/90 backdrop-blur-md rounded-2xl border border-[#E6DFD3] flex justify-between items-center text-left">
              <div>
                <span className="text-[10px] text-[#C8A04D] uppercase tracking-widest font-bold block">100% Certified</span>
                <p className="font-serif text-base font-bold text-[#3A0F1F]">Authentic Silk Mark Guilds</p>
              </div>
              <span className="text-xs font-bold text-[#3A0F1F] underline cursor-pointer hover:text-[#C8A04D]" onClick={() => navigate('/products')}>
                Discover
              </span>
            </div>
          </motion.div>
        </div>

      </div>
    </section>
  );
}
