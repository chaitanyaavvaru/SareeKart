import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';

const COLLECTIONS = [
  {
    id: 1,
    title: "Royal Banarasi Silk",
    subtitle: "Imperial Brocade & Gold Zari",
    description: "Crafted in the historic city of Varanasi, our Banarasi weaves carry intricate floral vines (Bel) and foliage motifs inspired by Mughal court textiles. Woven meticulously on wooden handlooms with pure silk and genuine silver-dipped gold threads.",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1200&q=80",
    url: "/products?category=Banarasi%20Silk",
    fabric: "Mulberry Silk",
    heritage: "Varanasi Guild"
  },
  {
    id: 2,
    title: "Kanchi Bridal Tissue",
    subtitle: "Heavy Temple Borders & Double-Warp",
    description: "Woven in the temple towns of Tamil Nadu, our Kanchipuram tissue sarees feature heavy double-warp body threads, structured temple spires (Gopurams), and rich gold zari borders. Made to withstand generations, they carry the weight of royal bridal lineage.",
    image: "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=1200&q=80",
    url: "/products?category=Kanchipuram%20Silk",
    fabric: "Bridal Tissue",
    heritage: "Kanchipuram Guild"
  },
  {
    id: 3,
    title: "Artisanal Jamdani",
    subtitle: "Translucent Muslin & Inlaid Weaves",
    description: "An ethereal blending of fine raw silk threads and hand-spun cotton. The decorative floral patterns are inlaid thread-by-thread directly onto the loom without any mechanical aids, creating a sheer, weightless fabric that floats effortlessly.",
    image: "https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?auto=format&fit=crop&fm=webp&w=1200&q=80",
    url: "/products",
    fabric: "Fine Cotton Silk",
    heritage: "Bengal Weft"
  }
];

export default function SignatureCollections() {
  const navigate = useNavigate();
  const [activeId, setActiveId] = useState(1);
  const activeCol = COLLECTIONS.find(c => c.id === activeId) || COLLECTIONS[0];

  return (
    <section className="bg-[#FAF9F6] w-full py-20 select-none border-b border-[#E5E5E5] text-left">
      <div className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 flex flex-col space-y-10 w-full">
        
        <div className="space-y-3 text-left">
          <span className="text-[#C5A059] font-serif text-xs font-semibold tracking-[0.25em] uppercase flex items-center gap-2 leading-none">
            <Sparkles className="w-3.5 h-3.5 text-[#C5A059]" /> Curated Archives
          </span>
          <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-[#121212] leading-tight">
            Signature Collections
          </h2>
          <p className="text-sm sm:text-base text-[#666666] max-w-2xl font-normal leading-relaxed">
            Explore iconic weaves celebrated across generations for their technical precision, majestic beauty, and royal inheritance.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-stretch w-full">
          
          <div className="lg:col-span-8 relative h-[520px] lg:h-[620px] rounded-2xl overflow-hidden border border-[#E5E5E5] shadow-lg bg-white">
            <img
              src={activeCol.image}
              alt={activeCol.title}
              className="w-full h-full object-cover object-top transition-opacity duration-300"
              loading="lazy"
            />

            <div className="absolute bottom-8 left-8 text-left text-[#121212] bg-white/95 backdrop-blur-md p-6 rounded-xl border border-[#E5E5E5] space-y-1 z-10 max-w-sm">
              <span className="text-[#C5A059] text-[10px] font-bold uppercase tracking-widest block">
                {activeCol.heritage}
              </span>
              <p className="text-xl font-cormorant font-bold leading-none text-[#121212]">
                {activeCol.title} · {activeCol.fabric}
              </p>
            </div>
          </div>

          <div className="lg:col-span-4 flex flex-col justify-between py-2 gap-6">
            {COLLECTIONS.map((col) => {
              const isActive = col.id === activeId;
              return (
                <div
                  key={col.id}
                  onMouseEnter={() => setActiveId(col.id)}
                  onClick={() => navigate(col.url)}
                  className={`flex flex-col text-left cursor-pointer transition-all duration-200 p-5 rounded-xl border ${
                    isActive ? 'opacity-100 bg-white shadow-md border-[#C5A059]' : 'opacity-70 hover:opacity-100 bg-[#FAF9F6] border-[#E5E5E5]'
                  } space-y-2`}
                >
                  <div className="space-y-0.5">
                    <span className="text-[10px] font-bold text-[#C5A059] uppercase tracking-wider block">
                      {col.heritage}
                    </span>
                    <h3 className="text-xl font-cormorant font-bold text-[#121212] transition-colors leading-tight">
                      {col.title}
                    </h3>
                  </div>

                  <p className="text-xs text-[#666666] leading-relaxed line-clamp-2">
                    {col.description}
                  </p>

                  <div className="pt-1 flex items-center gap-1.5 text-xs font-bold text-[#121212] hover:text-[#C5A059] transition-colors uppercase tracking-widest leading-none">
                    Discover Collection <ArrowRight className="w-3.5 h-3.5 shrink-0" />
                  </div>
                </div>
              );
            })}
          </div>

        </div>

      </div>
    </section>
  );
}
