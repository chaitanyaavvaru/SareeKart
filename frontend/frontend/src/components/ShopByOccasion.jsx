
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { ArrowRight } from 'lucide-react';
import SafeImage from './common/SafeImage';

const OCCASIONS = [
  { 
    id: 'wedding', 
    name: 'Wedding Muhurtham', 
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600',
    description: 'Heavy gold-woven bridal drapes and temple borders.' 
  },
  { 
    id: 'festival', 
    name: 'Festive Rituals', 
    image: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=600',
    description: 'Vibrant drapes with traditional geometric layout motifs.' 
  },
  { 
    id: 'reception', 
    name: 'Royal Reception', 
    image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=600',
    description: 'Elegant tissue silk weaves and modern metallic thread detailing.' 
  },
  { 
    id: 'office', 
    name: 'Formal Editorial', 
    image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600',
    description: 'Minimalist handloom cottons and fine linen weaves.' 
  },
  { 
    id: 'temple', 
    name: 'Temple Ceremonies', 
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=600',
    description: 'Heritage borders carrying traditional architecture contours.' 
  },
  { 
    id: 'party', 
    name: 'Evening Party', 
    image: 'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=600', 
    hoverImage: 'https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?w=600',
    description: 'Glamorous modern georgette weaves with gold borders.' 
  }
];

export default function ShopByOccasion() {
  const navigate = useNavigate();
  const [hoveredId, setHoveredId] = useState(null);

  const handleSelect = (occId) => {
    navigate(`/products?occasion=${occId}`);
  };

  return (
    <section className="w-full min-h-[95vh] bg-[#FAF8F5] flex items-center justify-center py-32">
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full space-y-16">
        
        {/* Header Block */}
        <div className="text-center space-y-3">
          <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
            Curated Folds
          </span>
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
            Shop by Occasion
          </h2>
          <p className="text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
            Select drapes crafted specifically for seasonal celebrations and heritage rituals.
          </p>
        </div>

        {/* 6 Large Editorial Cards (Height 400-500px) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {OCCASIONS.map((occ) => (
            <div
              key={occ.id}
              onClick={() => handleSelect(occ.id)}
              onMouseEnter={() => setHoveredId(occ.id)}
              onMouseLeave={() => setHoveredId(null)}
              className="group relative h-[450px] sm:h-[480px] rounded-[28px] overflow-hidden border border-[#E6DFD3] shadow-luxury transition-all duration-500 cursor-pointer bg-[#F9F6F1]"
            >
              {/* Double Image Visual Cross-Fade */}
              <div className="absolute inset-0 z-0">
                <SafeImage 
                  src={hoveredId === occ.id ? occ.hoverImage : occ.image}
                  alt={occ.name}
                  productName={occ.name}
                  category="Occasion drape"
                  aspectRatioClass="aspect-[4/5]"
                  className="w-full h-full object-cover object-top transition-transform duration-[1.5s] ease-out group-hover:scale-103"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent"></div>
              </div>

              {/* Card Copy Details (Bottom aligned) */}
              <div className="absolute inset-0 z-10 flex flex-col justify-end p-8 text-left text-white">
                <div className="space-y-3 transform translate-y-4 group-hover:translate-y-0 transition-transform duration-500 ease-out">
                  <span className="text-[9px] uppercase font-bold text-[#C89B3C] tracking-[0.2em] font-sans">
                    Loom Selection
                  </span>
                  
                  <h3 className="text-xl sm:text-2xl font-serif italic text-white group-hover:text-[#C89B3C] transition-colors duration-300">
                    {occ.name}
                  </h3>

                  <p className="text-xs text-white/80 font-sans font-light leading-relaxed opacity-0 group-hover:opacity-100 transition-opacity duration-700">
                    {occ.description}
                  </p>

                  <div className="pt-2 flex items-center gap-1.5 text-xs font-bold text-[#C89B3C] uppercase tracking-widest">
                    Explore Collection <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1.5 transition-transform duration-300" />
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
