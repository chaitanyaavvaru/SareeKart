import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';

const ARTISANS = [
  {
    id: 1,
    name: "Master Ramzan Ali",
    role: "Banarasi Silk Weaver",
    experience: "42 Years on Loom",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80"
  },
  {
    id: 2,
    name: "Srinivasa Mudaliar",
    role: "Kanchipuram Guild Elder",
    experience: "50 Years on Loom",
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=600&q=80"
  },
  {
    id: 3,
    name: "Devi Salvi",
    role: "Patan Ikat Specialist",
    experience: "35 Years on Loom",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80"
  }
];

export default function MeetArtisans() {
  const navigate = useNavigate();

  return (
    <section className="w-full bg-[#121212] text-white flex items-center justify-center relative select-none border-t border-white/10 py-20">
      <div className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 w-full space-y-12 text-left">
        
        {/* Top Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-3 max-w-2xl">
            <div className="flex items-center gap-2">
              <span className="h-px w-6 bg-[#C5A059]"></span>
              <span className="text-[#C5A059] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-1.5 leading-none">
                <Sparkles className="w-3.5 h-3.5 text-[#C5A059]" /> Meet The Master Artisans
              </span>
            </div>
            
            <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-white leading-tight">
              The Hands Behind Every Masterpiece
            </h2>
            
            <p className="text-sm text-white/80 leading-relaxed font-sans font-normal">
              Every saree carries the heartbeat of its maker. Our master weavers spend weeks stretching warps, boiling natural vat dyes, and double-shuttling gold borders. Through SareeKart, they receive direct patronage, ensuring fair wages.
            </p>
          </div>

          <button 
            onClick={() => navigate('/products')}
            className="h-12 px-8 bg-[#C5A059] hover:bg-[#b59049] text-[#121212] font-bold rounded-full text-xs uppercase tracking-widest transition-all duration-300 flex items-center gap-2 cursor-pointer shrink-0 self-start md:self-end"
          >
            Meet Our Artisans <ArrowRight className="w-4 h-4" />
          </button>
        </div>

        {/* 3 Artisan Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 w-full">
          {ARTISANS.map((art) => (
            <div
              key={art.id}
              className="rounded-2xl overflow-hidden shadow-xl bg-white/5 border border-white/10 group relative h-[450px]"
            >
              <img 
                src={art.image} 
                alt={art.name}
                loading="eager"
                decoding="sync"
                className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-500 object-top"
              />
              
              <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent flex flex-col justify-end p-6 text-left text-white">
                <span className="text-[10px] uppercase tracking-widest text-[#C5A059] font-semibold flex items-center gap-1">
                  <Sparkles className="w-3 h-3" /> {art.experience}
                </span>
                <h4 className="text-xl font-cormorant font-bold tracking-wide mt-1">{art.name}</h4>
                <p className="text-xs text-white/70 font-sans mt-0.5">{art.role}</p>
              </div>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
