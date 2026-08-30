
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import SafeImage from './common/SafeImage';

const FABRICS = [
  { 
    id: 'katan-silk', 
    name: 'Katan Mulberry Silk', 
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600',
    description: 'Pure, organic silk threads twisted together to form dense, heavy royal bridal fabrics.' 
  },
  { 
    id: 'organza', 
    name: 'Kora Organza Silk', 
    image: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600', 
    description: 'Sheer, lightweight weaves carrying metallic gold zari lace temple borders.' 
  },
  { 
    id: 'georgette', 
    name: 'Khaddi Georgette', 
    image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=600', 
    description: 'Crinkled sheer fabric with beautiful fluid drape weight and zari highlights.' 
  },
  { 
    id: 'tussar', 
    name: 'Wild Tussar Silk', 
    image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600', 
    description: 'Coarse texture with a rich, natural gold sheen spun in forest regions sustainably.' 
  },
  { 
    id: 'cotton', 
    name: 'Fine Handspun Cotton', 
    image: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600', 
    description: 'Breathable, lightweight wraps dyed in indigo, madder, and organic minerals.' 
  },
  { 
    id: 'linen', 
    name: 'Artisanal Linen', 
    image: 'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=600', 
    description: 'Eco-friendly natural flax fibers carrying minimalist silver borders.' 
  }
];

export default function ShopByFabric() {
  const navigate = useNavigate();

  const handleSelect = (fabricName) => {
    navigate(`/products?fabric=${encodeURIComponent(fabricName)}`);
  };

  return (
    <section className="w-full min-h-[95vh] bg-white flex items-center justify-center py-32 border-t border-[#F4F2EB]">
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full space-y-16">
        
        {/* Header Block */}
        <div className="text-center space-y-3">
          <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
            Weft Curation
          </span>
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
            Shop by Fabric
          </h2>
          <p className="text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
            Filter by yarn varieties, spanning sheer organic organza to heavy double-ply mulberry silk.
          </p>
        </div>

        {/* 2 Rows of 3 full-height luxury fabric cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {FABRICS.map((fab) => (
            <div
              key={fab.id}
              onClick={() => handleSelect(fab.id)}
              className="group relative h-[380px] sm:h-[420px] rounded-[28px] overflow-hidden border border-[#E6DFD3] shadow-luxury transition-all duration-500 cursor-pointer bg-[#F9F6F1]"
            >
              {/* Image background block */}
              <div className="absolute inset-0 z-0">
                <SafeImage 
                  src={fab.image} 
                  alt={fab.name} 
                  productName={fab.name}
                  category="Fabric close-up"
                  aspectRatioClass="aspect-[4/3]"
                  className="w-full h-full object-cover object-top transition-transform duration-1000 ease-out group-hover:scale-103"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/35 to-transparent"></div>
              </div>

              {/* Text overlays */}
              <div className="absolute inset-0 z-10 flex flex-col justify-end p-8 text-left text-white">
                <div className="space-y-2 transform translate-y-3 group-hover:translate-y-0 transition-transform duration-500 ease-out">
                  <span className="text-[9px] uppercase font-bold text-[#C89B3C] tracking-widest font-sans">Premium Yarn</span>
                  
                  <h4 className="text-lg sm:text-xl font-bold font-serif text-white group-hover:text-[#C89B3C] transition-colors">
                    {fab.name}
                  </h4>
                  
                  <p className="text-xs text-white/80 font-sans font-light leading-relaxed line-clamp-2 opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                    {fab.description}
                  </p>

                  <div className="flex items-center gap-1 text-[10px] font-bold text-[#C89B3C] uppercase tracking-widest pt-2">
                    Explore fabric <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
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
