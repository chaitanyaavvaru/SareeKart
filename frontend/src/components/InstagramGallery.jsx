import React from 'react';
import { motion } from 'framer-motion';
import { Sparkles } from 'lucide-react';

const InstagramIcon = ({ className }) => (
  <svg 
    className={className} 
    viewBox="0 0 24 24" 
    fill="none" 
    stroke="currentColor" 
    strokeWidth="2" 
    strokeLinecap="round" 
    strokeLinejoin="round"
  >
    <rect x="2" y="2" width="20" height="20" rx="5" ry="5"/>
    <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/>
    <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/>
  </svg>
);

const MASONRY_COLUMNS = [
  [
    { id: 1, image: "https://images.unsplash.com/photo-1583939003579-730e3918a45a?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[450px]", caption: "Bridal Red Heritage" },
    { id: 2, image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[280px]", caption: "Palace Corridor Details" }
  ],
  [
    { id: 3, image: "https://images.unsplash.com/photo-1566552881560-0be862a7c445?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[300px]", caption: "Royal Weft Threads" },
    { id: 4, image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[480px]", caption: "Temple Architecture Textures" }
  ],
  [
    { id: 5, image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[420px]", caption: "Artisan Courtyard Life" },
    { id: 6, image: "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=600&q=80", height: "h-[320px]", caption: "Mulberry Silk Folds" }
  ]
];

export default function InstagramGallery() {
  return (
    <section className="w-full min-h-screen bg-[#FAF8F5] flex items-center justify-center py-32 border-t border-[#E6DFD3]">
      <div className="max-w-[1640px] mx-auto px-[80px] w-full space-y-16 select-none">
        
        {/* Left-Aligned Header Block */}
        <div className="text-left space-y-3 w-full">
          <span className="text-[#C8A04D] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-1.5 block">
            <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Visual Journal
          </span>
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#3A0F1F] leading-tight">
            Visual Tales of Silk
          </h2>
          <p className="text-sm text-[#6b5c4d] font-sans font-normal">
            Follow our journey <a href="https://instagram.com" target="_blank" rel="noreferrer" className="font-bold text-[#C8A04D] hover:underline inline-flex items-center gap-1">@SareeKartLuxe <InstagramIcon className="w-3.5 h-3.5" /></a> for behind-the-scenes handloom stories.
          </p>
        </div>

        {/* Pinterest-style Masonry Columns Grid (Height 100vh overall) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">
          {MASONRY_COLUMNS.map((column, colIdx) => (
            <div key={colIdx} className="flex flex-col gap-6 w-full">
              {column.map((item, itemIdx) => (
                <motion.div 
                  key={item.id}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.8, delay: (colIdx * 2 + itemIdx) * 0.1, ease: "easeOut" }}
                  className={`group relative ${item.height} rounded-3xl overflow-hidden shadow-luxury border border-[#E6DFD3] bg-white cursor-pointer`}
                >
                  <img 
                    src={item.image} 
                    alt={item.caption} 
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-[1.5s] ease-out object-center"
                    loading="lazy"
                  />
                  {/* Hover Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/35 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 flex flex-col justify-end p-6 text-left text-white">
                    <span className="text-[9px] uppercase font-bold text-[#C8A04D] tracking-widest font-sans flex items-center gap-1.5 mb-1">
                      <InstagramIcon className="w-3 h-3" /> Instagram Diary
                    </span>
                    <h4 className="text-md font-serif italic text-[#FAF8F5]">{item.caption}</h4>
                  </div>
                </motion.div>
              ))}
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
