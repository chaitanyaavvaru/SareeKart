import React from 'react';
import { Sparkles, Compass, Scissors, Award, Settings, Feather, ShieldCheck } from 'lucide-react';
import { motion } from 'framer-motion';

const STEPS = [
  {
    id: "01",
    title: "Silkworm Cultivation",
    desc: "Organic sericulture harvesting of pure mulberry and wild tussar cocoons.",
    icon: Feather,
    image: "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "02",
    title: "Thread Spinning",
    desc: "Hand-reeling raw silk filaments into fine double-warp yarn strands.",
    icon: Compass,
    image: "https://images.unsplash.com/photo-1566552881560-0be862a7c445?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "03",
    title: "Vat Dyeing",
    desc: "Natural extraction dyes from madder root, indigo, and mineral salts.",
    icon: Settings,
    image: "https://images.unsplash.com/photo-1566552881560-0be862a7c445?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "04",
    title: "Handloom Weaving",
    desc: "Interlacing warp & weft on traditional wooden pit looms with gold zari.",
    icon: Sparkles,
    image: "https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "05",
    title: "Zari Embroidery",
    desc: "Needlework embellishment with authentic silver and 24K gold wire.",
    icon: ShieldCheck,
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "06",
    title: "Hand Finishing",
    desc: "Edge fringing, loom-cutting, and 14-point artisan quality control.",
    icon: Scissors,
    image: "https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=400&q=80"
  },
  {
    id: "07",
    title: "Luxury Packaging",
    desc: "Heirloom protection packing in organic cotton trunks with Silk Mark.",
    icon: Award,
    image: "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=400&q=80"
  }
];

export default function HandloomTimeline() {
  return (
    <section className="w-full py-24 bg-[#FAF8F5] select-none relative z-10 border-t border-[#E6DFD3]">
      
      {/* Heading */}
      <div className="max-w-[1640px] mx-auto px-[80px] text-left space-y-4 mb-16 w-full">
        <span className="text-[#C8A04D] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-1.5 block">
          <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Artisanal Chronicles
        </span>
        <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#3A0F1F] leading-tight">
          How a Handloom Masterpiece is Born
        </h2>
        <p className="text-base text-[#6b5c4d] max-w-xl font-normal leading-relaxed font-sans">
          Tracing the seven slow, sacred craft stages behind every heirloom drape in our archives.
        </p>
      </div>

      {/* 7-Step Timeline Horizontal Grid */}
      <div className="max-w-[1640px] mx-auto px-[80px] w-full relative">
        
        {/* Connector Line */}
        <div className="absolute top-16 left-16 right-16 h-0.5 bg-[#E6DFD3] pointer-events-none hidden lg:block">
          <motion.div 
            initial={{ width: 0 }}
            whileInView={{ width: "100%" }}
            viewport={{ once: true }}
            transition={{ duration: 1.8, ease: "easeInOut" }}
            className="h-full bg-[#C8A04D]"
          />
        </div>

        {/* Steps Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-6 items-start relative z-10">
          {STEPS.map((step, idx) => {
            const Icon = step.icon;

            return (
              <motion.div
                key={step.id}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: idx * 0.1 }}
                className="flex flex-col items-center text-center space-y-4 group bg-white p-4 rounded-2xl border border-[#E6DFD3] hover:border-[#C8A04D] hover:shadow-luxury transition-all duration-500 h-full justify-between"
              >
                <div className="relative w-full aspect-square rounded-xl overflow-hidden mb-2 border border-[#E6DFD3]">
                  <img 
                    src={step.image} 
                    alt={step.title}
                    loading="lazy"
                    decoding="async"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 object-center"
                  />
                  <div className="absolute inset-0 bg-[#3A0F1F]/20 group-hover:bg-transparent transition-colors"></div>
                  <span className="absolute top-2 right-2 bg-[#3A0F1F] text-[#C8A04D] text-[10px] font-bold w-6 h-6 rounded-full flex items-center justify-center border border-[#C8A04D]/40 shadow font-sans">
                    {step.id}
                  </span>
                </div>

                <div className="space-y-2 px-1">
                  <div className="flex items-center justify-center gap-1.5 text-[#C8A04D]">
                    <Icon className="w-4 h-4" />
                  </div>
                  <h3 className="font-serif font-bold text-sm text-[#3A0F1F] leading-snug group-hover:text-[#C8A04D] transition-colors">
                    {step.title}
                  </h3>
                  <p className="text-[11px] text-[#6b5c4d] leading-relaxed font-sans font-light line-clamp-3">
                    {step.desc}
                  </p>
                </div>

              </motion.div>
            );
          })}
        </div>

      </div>

    </section>
  );
}
