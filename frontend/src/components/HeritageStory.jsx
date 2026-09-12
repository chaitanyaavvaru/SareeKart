import React from 'react';

export default function HeritageStory() {
  return (
    <section className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 py-20 bg-white rounded-2xl border border-[#E5E5E5] shadow-xs mt-12 select-none font-sans text-left">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center w-full">
        
        <div className="lg:col-span-7 space-y-6 text-left w-full">
          <div className="space-y-2">
            <span className="text-[#C5A059] font-serif text-xs font-semibold tracking-[0.25em] uppercase block leading-none">
              OUR HERITAGE
            </span>
            <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-[#121212] leading-tight">
              Preserving India’s Weaving Legacy
            </h2>
          </div>

          <p className="text-sm sm:text-base text-[#666666] leading-relaxed font-sans font-normal">
            For generations, the rhythmic click of the handloom has been the heartbeat of rural India. SareeKart was founded with a singular mission: to bring the purest, most exquisite handcrafted sarees directly from master weavers to connoisseurs of luxury worldwide.
          </p>
          
          <div className="border-l-2 border-[#C5A059] pl-5 italic text-base sm:text-lg text-[#121212] font-cormorant leading-relaxed bg-[#FAF9F6] py-4 rounded-r-xl border-t border-b border-r border-[#E5E5E5]">
            "A single Banarasi or Kanchipuram drape takes anywhere from twenty days to three months of painstaking weaving. It is not just clothing; it is a canvas of living Indian heritage."
          </div>
          
          <p className="text-sm sm:text-base text-[#666666] font-sans font-normal leading-relaxed">
            By eliminating intermediaries, we ensure that our weavers receive fair trade wages while you receive authenticated silk mark products straight from artisan clusters in Varanasi, Kanchipuram, Uppada, and Pochampally.
          </p>

          <div className="grid grid-cols-3 gap-6 pt-6 border-t border-[#E5E5E5] w-full">
            <div className="bg-[#FAF9F6] py-5 px-4 rounded-xl border border-[#E5E5E5] text-center space-y-1">
              <p className="text-3xl font-cormorant font-bold text-[#C5A059] leading-none">2,500+</p>
              <p className="text-[10px] uppercase font-bold text-[#121212] font-sans tracking-widest leading-none mt-1">Master Weavers</p>
            </div>
            <div className="bg-[#FAF9F6] py-5 px-4 rounded-xl border border-[#E5E5E5] text-center space-y-1">
              <p className="text-3xl font-cormorant font-bold text-[#C5A059] leading-none">100%</p>
              <p className="text-[10px] uppercase font-bold text-[#121212] font-sans tracking-widest leading-none mt-1">Pure Silk Mark</p>
            </div>
            <div className="bg-[#FAF9F6] py-5 px-4 rounded-xl border border-[#E5E5E5] text-center space-y-1">
              <p className="text-3xl font-cormorant font-bold text-[#C5A059] leading-none">50+</p>
              <p className="text-[10px] uppercase font-bold text-[#121212] font-sans tracking-widest leading-none mt-1">Artisan Clusters</p>
            </div>
          </div>
        </div>

        <div className="lg:col-span-5 relative group w-full">
          <div className="aspect-[4/5] rounded-2xl overflow-hidden shadow-lg border border-[#E5E5E5] relative bg-white">
            <img 
              src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80" 
              alt="Master artisan weaving silk saree on traditional handloom"
              className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-500 object-top"
              loading="lazy"
            />
          </div>
          
          <div className="absolute bottom-6 left-6 bg-white/95 backdrop-blur-md text-[#121212] p-5 rounded-xl shadow-md border border-[#E5E5E5] max-w-xs hidden sm:block text-left space-y-1">
            <p className="font-cormorant text-[#C5A059] text-xs font-bold leading-none">100% Traceable Craftsmanship</p>
            <p className="text-[11px] text-[#666666] font-sans mt-0.5 font-normal leading-normal">Every saree holds the master weaver's signature registration mark.</p>
          </div>
        </div>

      </div>
    </section>
  );
}
