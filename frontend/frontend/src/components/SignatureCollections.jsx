
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { ArrowRight } from 'lucide-react';
import SafeImage from './common/SafeImage';

const COLLECTIONS = [
  {
    id: 1,
    title: "Royal Banarasi Silk",
    subtitle: "Imperial Brocade & Gold Zari",
    description: "Crafted in the historic city of Varanasi, our Banarasi weaves carry intricate floral vines (Bel) and foliage motifs inspired by Mughal court textiles. Woven meticulously on wooden handlooms with pure silk and genuine silver-dipped gold threads.",
    image: "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=1200",
    hoverImage: "https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250",
    url: "/products?category=Banarasi%20Silk",
    fabric: "Mulberry Silk",
    heritage: "Varanasi Guild"
  },
  {
    id: 2,
    title: "Kanchi Bridal Tissue",
    subtitle: "Heavy Temple Borders & Double-Warp",
    description: "Woven in the temple towns of Tamil Nadu, our Kanchipuram tissue sarees feature heavy double-warp body threads, structured temple spires (Gopurams), and rich gold zari borders. Made to withstand generations, they carry the weight of royal bridal lineage.",
    image: "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=1200",
    hoverImage: "https://kankatala.com/cdn/shop/files/1215863175_2.webp?v=1761908736",
    url: "/products?category=Kanchipuram%20Silk",
    fabric: "Bridal Tissue",
    heritage: "Kanchipuram Guild"
  },
  {
    id: 3,
    title: "Artisanal Jamdani",
    subtitle: "Translucent Muslin & Inlaid Weaves",
    description: "An ethereal blending of fine raw silk threads and hand-spun cotton. The decorative floral patterns are inlaid thread-by-thread directly onto the loom without any mechanical aids, creating a sheer, weightless fabric that floats effortlessly.",
    image: "https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=1200",
    hoverImage: "https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?w=1200",
    url: "/products",
    fabric: "Fine Cotton Silk",
    heritage: "Bengal Weft"
  }
];

export default function SignatureCollections() {
  const navigate = useNavigate();
  const [hoveredId, setHoveredId] = useState(null);

  return (
    <section className="bg-[#F9F6F1] w-full">
      {/* Title Header Card (approx 40vh) */}
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 pt-28 pb-16 text-center space-y-4">
        <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
          Curated Archives
        </span>
        <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
          Signature Collections
        </h2>
        <p className="text-sm text-text-secondary max-w-2xl mx-auto font-light leading-relaxed">
          Explore iconic weaves celebrated across generations for their technical precision, majestic beauty, and royal inheritance.
        </p>
      </div>

      {/* Alternating 100vh Full Screen Editorial Sections */}
      <div className="space-y-16 lg:space-y-0">
        {COLLECTIONS.map((col, idx) => {
          const isEven = idx % 2 === 1;
          const isLast = idx === COLLECTIONS.length - 1;

          return (
            <div 
              key={col.id} 
              onMouseEnter={() => setHoveredId(col.id)}
              onMouseLeave={() => setHoveredId(null)}
              className="w-full min-h-[90vh] lg:min-h-screen flex items-center justify-center border-t border-[#E6DFD3] relative py-16 lg:py-0"
            >
              {/* Desktop layouts */}
              <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
                
                {/* Condition 1: Collection 1 (Image 65%, Story 35%) */}
                {!isEven && !isLast && (
                  <>
                    {/* Image Block */}
                    <div className="lg:col-span-8 relative aspect-[16/10] overflow-hidden rounded-[20px] shadow-luxury bg-[#FAF8F5] border border-[#E6DFD3]">
                      <SafeImage 
                        src={hoveredId === col.id ? col.hoverImage : col.image}
                        alt={col.title}
                        productName={col.title}
                        category={col.fabric}
                        aspectRatioClass="aspect-[16/10]"
                        className="w-full h-full object-cover object-top transition-transform duration-1000 group-hover:scale-103"
                      />
                    </div>
                    {/* Story Block */}
                    <div className="lg:col-span-4 text-left space-y-6 lg:pl-6">
                      <span className="text-[10px] uppercase font-bold text-[#C89B3C] tracking-widest block">{col.heritage}</span>
                      <h3 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">{col.title}</h3>
                      <p className="text-xs uppercase font-bold text-text-muted tracking-widest">{col.subtitle}</p>
                      <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-light font-sans">{col.description}</p>
                      <div className="pt-4">
                        <button 
                          onClick={() => navigate(col.url)}
                          className="px-8 py-4 bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] font-bold rounded-xl text-xs uppercase tracking-widest transition-all duration-300 flex items-center gap-2 cursor-pointer shadow"
                        >
                          Discover Archive <ArrowRight className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </>
                )}

                {/* Condition 2: Collection 2 (Reverse: Story 35%, Image 65%) */}
                {isEven && (
                  <>
                    {/* Story Block */}
                    <div className="lg:col-span-4 text-left space-y-6 lg:pr-6 order-2 lg:order-1">
                      <span className="text-[10px] uppercase font-bold text-[#C89B3C] tracking-widest block">{col.heritage}</span>
                      <h3 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">{col.title}</h3>
                      <p className="text-xs uppercase font-bold text-text-muted tracking-widest">{col.subtitle}</p>
                      <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-light font-sans">{col.description}</p>
                      <div className="pt-4">
                        <button 
                          onClick={() => navigate(col.url)}
                          className="px-8 py-4 bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] font-bold rounded-xl text-xs uppercase tracking-widest transition-all duration-300 flex items-center gap-2 cursor-pointer shadow"
                        >
                          Discover Archive <ArrowRight className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                    {/* Image Block */}
                    <div className="lg:col-span-8 relative aspect-[16/10] overflow-hidden rounded-[20px] shadow-luxury bg-[#FAF8F5] border border-[#E6DFD3] order-1 lg:order-2">
                      <SafeImage 
                        src={hoveredId === col.id ? col.hoverImage : col.image}
                        alt={col.title}
                        productName={col.title}
                        category={col.fabric}
                        aspectRatioClass="aspect-[16/10]"
                        className="w-full h-full object-cover object-top transition-transform duration-1000 group-hover:scale-103"
                      />
                    </div>
                  </>
                )}

                {/* Condition 3: Collection 3 (Split Layout: 50/50) */}
                {!isEven && isLast && (
                  <>
                    {/* Image Block */}
                    <div className="lg:col-span-6 relative aspect-square overflow-hidden rounded-[20px] shadow-luxury bg-[#FAF8F5] border border-[#E6DFD3]">
                      <SafeImage 
                        src={hoveredId === col.id ? col.hoverImage : col.image}
                        alt={col.title}
                        productName={col.title}
                        category={col.fabric}
                        aspectRatioClass="aspect-square"
                        className="w-full h-full object-cover object-top transition-transform duration-1000 group-hover:scale-103"
                      />
                    </div>
                    {/* Story Block */}
                    <div className="lg:col-span-6 text-left space-y-6 lg:pl-10">
                      <span className="text-[10px] uppercase font-bold text-[#C89B3C] tracking-widest block">{col.heritage}</span>
                      <h3 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">{col.title}</h3>
                      <p className="text-xs uppercase font-bold text-text-muted tracking-widest">{col.subtitle}</p>
                      <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-light font-sans">{col.description}</p>
                      <div className="pt-4">
                        <button 
                          onClick={() => navigate(col.url)}
                          className="px-8 py-4 bg-[#2B0F1E] hover:bg-[#200b16] text-[#C89B3C] font-bold rounded-xl text-xs uppercase tracking-widest transition-all duration-300 flex items-center gap-2 cursor-pointer shadow"
                        >
                          Discover Archive <ArrowRight className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </>
                )}

              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
