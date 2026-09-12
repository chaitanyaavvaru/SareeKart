import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Trophy, Award, Landmark } from 'lucide-react';

export default function WeddingCollection() {
  const navigate = useNavigate();

  const handleShopBridal = () => {
    navigate('/products?category=Kanchipuram%20Silk');
  };

  return (
    <section className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 py-20 select-none font-sans text-left">
      <div className="relative rounded-2xl overflow-hidden bg-[#121212] text-white p-8 md:p-14 flex flex-col lg:flex-row items-center gap-12 shadow-xl border border-[#E5E5E5]/20 w-full">
        
        {/* Left Column: Image Frame */}
        <div className="w-full lg:w-5/12 h-[480px] sm:h-[560px] rounded-2xl overflow-hidden border-2 border-[#C5A059]/40 shadow-2xl relative shrink-0 z-10 bg-white/5">
          <img 
            src="https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=1000&q=80" 
            alt="Luxury bridal tissue saree drape with gold zari embroidery close-up"
            loading="eager"
            decoding="sync"
            className="w-full h-full object-cover object-top hover:scale-103 transition-transform duration-700 ease-out"
          />
        </div>

        {/* Right Column: Details */}
        <div className="flex-grow space-y-6 text-white text-left z-10 w-full">
          <div className="space-y-2">
            <span className="text-[#C5A059] font-serif text-xs font-semibold tracking-[0.25em] uppercase flex items-center gap-2 block leading-none">
              <Sparkles className="w-3.5 h-3.5 text-[#C5A059]" /> THE BRIDAL SAGA
            </span>
            <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-white leading-tight">
              The Golden Muhurtham
            </h2>
          </div>

          <p className="text-sm sm:text-base text-white/90 leading-relaxed font-sans font-normal max-w-3xl">
            Immerse yourself in the opulence of double-warp Kanchipuram tissue silks and Royal Banarasi zari brocades. Designed as timeless heirloom masterpieces to mark your sacred, monumental moments.
          </p>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-white/15 w-full max-w-3xl">
            <div className="space-y-1 flex gap-3 items-start p-3 bg-white/5 rounded-xl border border-white/10">
              <Sparkles className="w-4 h-4 text-[#C5A059] shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <p className="text-[#C5A059] text-xs font-bold tracking-widest uppercase leading-none">Double-Warp Threads</p>
                <p className="text-white/70 text-xs leading-normal font-sans font-normal">Premium Mulberry silk filaments boiled and wound to ensure high texture density.</p>
              </div>
            </div>
            <div className="space-y-1 flex gap-3 items-start p-3 bg-white/5 rounded-xl border border-white/10">
              <Trophy className="w-4 h-4 text-[#C5A059] shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <p className="text-[#C5A059] text-xs font-bold tracking-widest uppercase leading-none">Real Metallic Zari</p>
                <p className="text-white/70 text-xs leading-normal font-sans font-normal">Certified pure silver threads dipped in 24k gold, interlaced thread-by-thread on pit looms.</p>
              </div>
            </div>
            <div className="space-y-1 flex gap-3 items-start p-3 bg-white/5 rounded-xl border border-white/10">
              <Landmark className="w-4 h-4 text-[#C5A059] shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <p className="text-[#C5A059] text-xs font-bold tracking-widest uppercase leading-none">Generational Motifs</p>
                <p className="text-white/70 text-xs leading-normal font-sans font-normal">Structured temple spires (Gopurams) and floral creepers celebrating historical designs.</p>
              </div>
            </div>
            <div className="space-y-1 flex gap-3 items-start p-3 bg-white/5 rounded-xl border border-white/10">
              <Award className="w-4 h-4 text-[#C5A059] shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <p className="text-[#C5A059] text-xs font-bold tracking-widest uppercase leading-none">Weaver Mark Insignia</p>
                <p className="text-white/70 text-xs leading-normal font-sans font-normal">Individually signed by weaving guild weavers. 100% traceable fair trade source.</p>
              </div>
            </div>
          </div>

          <div className="pt-2">
            <button 
              onClick={handleShopBridal}
              className="h-12 px-8 bg-[#C5A059] hover:bg-[#b59049] text-[#121212] font-bold rounded-full text-xs tracking-widest uppercase transition-all shadow-md cursor-pointer"
            >
              Explore Bridal Collection
            </button>
          </div>
        </div>

      </div>
    </section>
  );
}
