import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, ArrowRight } from 'lucide-react';

const CATEGORY_ITEMS = [
  { 
    id: 1, 
    name: "Silk Sarees", 
    label: "ROYAL HERITAGE", 
    desc: "Pure mulberry silk from renowned handloom weavers",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80"
  },
  { 
    id: 2, 
    name: "Banarasi Sarees", 
    label: "IMPERIAL WEAVE", 
    desc: "Fine brocades & genuine silver-gilded zari",
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80"
  },
  { 
    id: 3, 
    name: "Cotton Sarees", 
    label: "SUMMER ELEGANCE", 
    desc: "Comfortable, breathable handspun Jamdani cotton",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80"
  },
  { 
    id: 4, 
    name: "Georgette Sarees", 
    label: "PARTY ELEGANCE", 
    desc: "Graceful georgette sarees with rich stone work",
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80"
  },
  { 
    id: 5, 
    name: "Chiffon Sarees", 
    label: "FEATHERLIGHT GLIDE", 
    desc: "Lightweight flowing drapes with delicate embroidery",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80"
  },
  { 
    id: 6, 
    name: "Kanchipuram Sarees", 
    label: "WEDDING DRAPE", 
    desc: "Heavy gold borders & heritage Kanchi spires",
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80"
  }
];

export default function CategorySection() {
  const navigate = useNavigate();

  const handleCategoryClick = (name) => {
    navigate(`/products?category=${encodeURIComponent(name)}`);
  };

  const renderCategoryTile = (cat, heightClass) => (
    <div
      key={cat.id}
      onClick={() => handleCategoryClick(cat.name)}
      className={`group cursor-pointer relative ${heightClass} rounded-2xl overflow-hidden border border-[#E5E5E5] bg-[#FAF9F6] shadow-xs hover:shadow-lg transition-all duration-300 w-full shrink-0 flex flex-col justify-between select-none text-left`}
    >
      <div className="absolute inset-0 w-full h-full">
        <img
          src={cat.image}
          alt={cat.name}
          loading="eager"
          decoding="sync"
          className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-700 ease-out object-top"
        />
      </div>

      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />

      <div className="absolute bottom-6 left-6 right-6 text-left text-white space-y-1.5 z-10">
        <span className="text-[#C5A059] text-[10px] font-semibold uppercase tracking-widest block leading-none flex items-center gap-1.5">
          <Sparkles className="w-3 h-3 text-[#C5A059]" /> {cat.label}
        </span>
        <h3 className="text-xl sm:text-2xl font-cormorant font-bold leading-tight text-white">
          {cat.name}
        </h3>
        <p className="text-xs text-white/80 font-normal leading-relaxed">
          {cat.desc}
        </p>
        <span className="text-xs font-bold text-[#C5A059] uppercase tracking-wider inline-flex items-center gap-1 pt-1 leading-none group-hover:translate-x-1 transition-transform">
          Explore Collection <ArrowRight className="w-3.5 h-3.5" />
        </span>
      </div>
    </div>
  );

  return (
    <section className="bg-[#FAF9F6] w-full py-20 select-none border-b border-[#E5E5E5]">
      <div className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 flex flex-col space-y-10 w-full">
        
        <div className="text-left space-y-2 w-full">
          <span className="text-[#C5A059] font-serif text-xs font-semibold tracking-[0.25em] uppercase block leading-none">
            Craft Collections
          </span>
          <h2 className="text-3xl sm:text-5xl font-cormorant font-bold text-[#121212] leading-tight">
            Shop By Category
          </h2>
          <p className="text-sm sm:text-base text-[#666666] max-w-2xl font-normal leading-relaxed">
            Discover exquisite weaves categorized by heritage regions and handspun fabrics.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 w-full items-start justify-center">
          <div className="flex flex-col gap-8 w-full">
            {renderCategoryTile(CATEGORY_ITEMS[0], "h-[420px]")}
            {renderCategoryTile(CATEGORY_ITEMS[1], "h-[480px]")}
          </div>

          <div className="flex flex-col gap-8 w-full">
            {renderCategoryTile(CATEGORY_ITEMS[2], "h-[480px]")}
            {renderCategoryTile(CATEGORY_ITEMS[3], "h-[420px]")}
          </div>

          <div className="flex flex-col gap-8 w-full">
            {renderCategoryTile(CATEGORY_ITEMS[4], "h-[420px]")}
            {renderCategoryTile(CATEGORY_ITEMS[5], "h-[480px]")}
          </div>
        </div>

      </div>
    </section>
  );
}
