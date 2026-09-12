import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Quote, ArrowRight, Sparkles } from 'lucide-react';

export default function CelebritySection() {
  const navigate = useNavigate();

  return (
    <section className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 py-20 select-none font-sans text-left">
      <div className="relative rounded-2xl overflow-hidden bg-[#121212] text-white p-8 md:p-14 shadow-xl border border-[#E5E5E5]/20 w-full">
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center w-full relative z-10">
          
          {/* Left Column */}
          <div className="space-y-6 text-left w-full">
            <div className="space-y-2">
              <span className="text-[#C5A059] font-serif text-xs font-semibold tracking-[0.2em] uppercase flex items-center gap-2 block leading-none">
                <Sparkles className="w-3.5 h-3.5 text-[#C5A059]" /> CHOSEN FAVOURITE
              </span>
              <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-white leading-tight tracking-wide">
                Celebrity Picks
              </h2>
            </div>
            
            <p className="text-xs text-[#C5A059] font-sans tracking-[0.15em] leading-none uppercase font-bold">
              THE STAR'S CHOICE, NOW YOURS TO OWN
            </p>

            <div className="bg-white/5 border border-[#C5A059]/30 rounded-2xl p-5 space-y-3 relative w-full backdrop-blur-xs">
              <Quote className="w-8 h-8 text-[#C5A059]/40 absolute top-4 right-5 stroke-[1.2]" />
              <p className="italic text-base text-white/95 font-cormorant leading-relaxed pr-8">
                "A handloom saree is not just a drape; it is India's heritage story woven into gold threads. Wearing it makes me feel connected to the heartbeat of our master weavers."
              </p>
              <p className="text-[11px] text-[#C5A059] font-bold uppercase tracking-wider leading-none">
                — Red Carpet Fashion Icon
              </p>
            </div>
            
            <p className="text-sm text-white/80 font-sans font-normal leading-relaxed">
              Worn on international red carpets and high-fashion galas by India's most celebrated artists. Each drape stands out for its high density zari borders, traditional natural dyes, and timeless regality.
            </p>

            <div className="grid grid-cols-2 gap-6 pt-4 border-t border-white/10 w-full">
              <div className="space-y-1">
                <p className="text-[#C5A059] text-xs font-bold uppercase tracking-widest leading-none">Handspun Weft</p>
                <p className="text-white/60 text-xs leading-normal font-sans font-normal">Requires 120 loom hours to weave using silk mark certified threads.</p>
              </div>
              <div className="space-y-1">
                <p className="text-[#C5A059] text-xs font-bold uppercase tracking-widest leading-none">Artisan Label</p>
                <p className="text-white/60 text-xs leading-normal font-sans font-normal">Includes an authentication serial card co-signed by the regional weaver cooperative.</p>
              </div>
            </div>
            
            <div className="pt-2">
              <button 
                onClick={() => navigate('/products')}
                className="h-12 px-8 bg-[#C5A059] hover:bg-[#b59049] text-[#121212] font-bold rounded-full text-xs tracking-widest uppercase transition-all shadow-md cursor-pointer flex items-center justify-center gap-2"
              >
                Explore Celebrity Styles <ArrowRight className="w-4 h-4 shrink-0" />
              </button>
            </div>
          </div>

          {/* Right Column */}
          <div className="w-full h-[480px] sm:h-[560px] overflow-hidden rounded-2xl border border-[#C5A059]/30 shadow-xl relative group bg-white/5">
            <img 
              src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1000&q=80" 
              alt="Editorial celebrity fashion drape showcase"
              loading="eager"
              decoding="sync"
              className="w-full h-full object-cover object-top group-hover:scale-103 transition-transform duration-700 ease-out"
            />
          </div>

        </div>

      </div>
    </section>
  );
}
