import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, BookOpen, Sparkles } from 'lucide-react';

export default function NewArrivals() {
  const navigate = useNavigate();

  // Scrolling ticker texts for that magazine campaign vibe
  const tickerWords = ["HERITAGE", "HANDWOVEN", "AUTHENTIC", "PURE SILK", "ARTISANAL", "SAREEKART SIGNATURE", "TRADITION"];

  return (
    <section className="bg-[#FAF8F5] w-full py-[120px] select-none border-b border-[#E6DFD3]">
      <div className="max-w-[1640px] mx-auto px-[80px] w-full">
        
        {/* Luxury Editorial storytelling block: Left 40% (lg:col-span-5), Right 60% (lg:col-span-7) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-[48px] items-stretch min-h-[720px] lg:min-h-[820px] w-full">
          
          {/* LEFT COLUMN: Campaign information */}
          <motion.div 
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            className="lg:col-span-5 flex flex-col justify-center space-y-[32px] text-left pr-[24px]"
          >
            <div className="space-y-[16px]">
              <span className="text-[#C8A04D] font-serif text-[14px] font-bold tracking-[0.25em] uppercase flex items-center gap-2 block leading-none">
                <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> NEW ARRIVALS
              </span>
              <h2 className="text-[56px] lg:text-[72px] font-serif font-bold text-[#3A0F1F] leading-tight">
                Discover Our Latest Masterpieces
              </h2>
            </div>

            <div className="space-y-[16px] text-[18px] text-[#6b5c4d] font-normal leading-relaxed">
              <p>
                Every handloom drape tells a story of patience, devotion, and centuries of heritage. Our new season campaign celebrates the raw emotion of pure mulberry threads woven together by third-generation master weavers.
              </p>
              <p className="text-[14px] italic text-[#C8A04D] border-l-2 border-[#C8A04D] pl-[16px]">
                "A single drape holds the hands of twenty weavers, carrying generations of Indian handloom heritage."
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-[16px] pt-[8px]">
              <button 
                onClick={() => navigate('/products')}
                className="px-[32px] py-[16px] bg-[#3A0F1F] hover:bg-[#5B1832] text-[#C8A04D] hover:text-white font-semibold rounded-xl text-[15px] uppercase tracking-widest transition-all duration-300 flex items-center justify-center gap-[8px] cursor-pointer shadow-md"
              >
                Explore New Collection <ArrowRight className="w-5 h-5 shrink-0" />
              </button>
              <button 
                onClick={() => navigate('/products')}
                className="px-[32px] py-[16px] border border-[#E6DFD3] hover:border-[#3A0F1F] text-[#6b5c4d] hover:text-[#3A0F1F] font-semibold rounded-xl text-[15px] uppercase tracking-widest transition-all duration-300 flex items-center justify-center gap-[8px] cursor-pointer bg-white"
              >
                View Lookbook <BookOpen className="w-5 h-5 shrink-0" />
              </button>
            </div>
          </motion.div>

          {/* RIGHT COLUMN: Full-Height Hero Campaign Image (Magazine style, scrolling ticker) */}
          <motion.div 
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            className="lg:col-span-7 relative rounded-[32px] overflow-hidden border border-[#E6DFD3] shadow-luxury bg-gradient-to-br from-[#3A0F1F] to-[#5B1832] flex flex-col justify-between"
          >
            {/* Background parallax image */}
            <div className="absolute inset-0 w-full h-full pointer-events-none">
              <img 
                src="https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=1200&q=80" 
                alt="Luxury Lifestyle Drape Showcase" 
                className="w-full h-full object-cover object-top opacity-85 group-hover:scale-103 transition-transform duration-1000"
              />
            </div>

            {/* Pattern weave grid */}
            <div className="absolute inset-0 opacity-[0.06] pointer-events-none"
              style={{ backgroundImage: 'radial-gradient(circle, #C8A04D 1.5px, transparent 1.5px)', backgroundSize: '16px 16px' }}
            />
            {/* Dark gold shadow layer overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-[#3A0F1F]/90 via-[#3A0F1F]/20 to-transparent pointer-events-none" />

            {/* Top: Scrolling text campaign ticker banner */}
            <div className="relative w-full bg-[#C8A04D] text-[#3A0F1F] py-[12px] overflow-hidden flex whitespace-nowrap z-10 font-bold uppercase tracking-widest text-[13px] border-b border-[#C8A04D]/50">
              <div className="animate-[marquee_20s_linear_infinite] flex gap-[32px] shrink-0">
                {Array(4).fill(tickerWords).flat().map((word, i) => (
                  <span key={i} className="inline-block flex items-center gap-[8px]">
                    <span>{word}</span>
                    <span className="text-[#3A0F1F]/60">✦</span>
                  </span>
                ))}
              </div>
            </div>

            {/* Bottom: Signature text block */}
            <div className="relative z-10 p-[40px] text-white space-y-[8px] text-left">
              <span className="text-[#C8A04D] text-[13px] font-semibold uppercase tracking-[0.2em] block leading-none">
                CAMPAIGN THEME
              </span>
              <h3 className="text-[32px] font-serif font-bold leading-tight text-[#FAF8F5]">
                Authentic Handloom Weaving
              </h3>
              <p className="text-[14px] text-white/80 font-normal leading-relaxed max-w-md">
                Woven step-by-step without mechanical shortcuts. A celebration of natural threads, pure silks, and rich dyes.
              </p>
            </div>

          </motion.div>

        </div>

      </div>

      {/* Ticker animation stylesheet override */}
      <style dangerouslySetInnerHTML={{ __html: `@keyframes marquee { 0% { transform: translateX(0%); } 100% { transform: translateX(-50%); } }` }} />
    </section>
  );
}
