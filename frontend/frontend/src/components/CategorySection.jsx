
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

const CRAFTS = [
  { 
    name: "Banarasi Weaves", 
    query: "Banarasi", 
    story: "Handcrafted in Varanasi, carrying pure gold zari threads inspired by imperial Mughal motifs.",
    image: "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600",
    size: "lg:col-span-8 aspect-[4/3] sm:aspect-[16/10] lg:aspect-[16/9]"
  },
  { 
    name: "Kanjivaram Silks", 
    query: "Kanchipuram", 
    story: "Temple town masterpieces woven with heavy double-warp pure silk and thick golden borders.",
    image: "https://images.unsplash.com/photo-1608976328321-260af4eb985c?w=600",
    size: "lg:col-span-4 aspect-[4/3] sm:aspect-[3/4] lg:aspect-[3/4]"
  },
  { 
    name: "Patan Patola", 
    query: "Patola Silk", 
    story: "Double-Ikat mathematical precision where yarn is tie-dyed before weaving. Truly a lifetime heirloom.",
    image: "https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=600",
    size: "lg:col-span-4 aspect-[4/3] sm:aspect-[3/4] lg:aspect-[3/4]"
  },
  { 
    name: "Pochampally Ikat", 
    query: "Pochampally", 
    story: "Dynamic geometric patterns woven in Telangana using traditional tie-and-dye weaving structures.",
    image: "https://images.unsplash.com/photo-1605001011156-cbf0b0f67a51?w=600",
    size: "lg:col-span-8 aspect-[4/3] sm:aspect-[16/10] lg:aspect-[16/9]"
  },
  { 
    name: "Sheer Chanderi / Organza", 
    query: "Chanderi", 
    story: "Ethereal transparency achieved by blending raw silk threads with fine handspun cotton warp.",
    image: "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=600",
    size: "lg:col-span-6 aspect-[4/3] lg:aspect-[4/3]"
  },
  { 
    name: "Forest Tussar & Bandhani", 
    query: "Tussar Silk", 
    story: "Deep, coarse wild silk drapes with a unique natural gold sheen, harvested sustainably in tribal belts.",
    image: "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=600",
    size: "lg:col-span-6 aspect-[4/3] lg:aspect-[4/3]"
  }
];

export default function CategorySection() {
  const navigate = useNavigate();

  const handleCraftClick = (query) => {
    navigate(`/products?search=${encodeURIComponent(query)}`);
  };

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 border-t border-[#F4F2EB] bg-[#FAF8F5]">
      <div className="text-center space-y-3 mb-16">
        <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
          Heritage Archives
        </span>
        <h2 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">
          Shop By Craft
        </h2>
        <p className="text-xs sm:text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
          Forget categories. Immerse yourself in the technical feats, historical regions, and artisanal narratives of Indian weavers.
        </p>
      </div>

      {/* Asymmetric Magazine Tile Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {CRAFTS.map((craft, idx) => (
          <motion.div 
            key={craft.name}
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: idx * 0.1, ease: [0.215, 0.610, 0.355, 1.000] }}
            onClick={() => handleCraftClick(craft.query)}
            className={`group cursor-pointer relative overflow-hidden rounded-3xl border border-[#F4F2EB] bg-white shadow-xs hover:shadow-luxury transition-all duration-700 ${craft.size}`}
          >
            {/* Immersive Photography with Ken Burns effect on hover */}
            <div className="absolute inset-0 z-0 bg-[#FAF8F5]">
              <img 
                src={craft.image} 
                alt={craft.name} 
                className="w-full h-full object-cover transition-transform duration-[1.5s] ease-out group-hover:scale-105 object-top"
                loading="lazy"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#2B0F1E]/80 via-[#2B0F1E]/20 to-transparent"></div>
            </div>
            
            {/* Storytelling Text Overlay (Slide up on hover) */}
            <div className="absolute inset-0 z-10 flex flex-col justify-end p-8 sm:p-10 text-white">
              <div className="space-y-2 transform translate-y-6 group-hover:translate-y-0 transition-transform duration-500 ease-out">
                {/* Handwritten Style Label */}
                <h3 className="font-serif italic font-medium text-2xl sm:text-3xl text-[#C89B3C] tracking-wide">
                  {craft.name}
                </h3>
                
                {/* Weaving Story */}
                <p className="text-xs text-white/80 font-sans max-w-lg font-light leading-relaxed opacity-0 group-hover:opacity-100 transition-opacity duration-500 delay-100">
                  {craft.story}
                </p>
                
                <span className="inline-flex items-center gap-1 text-[9px] uppercase tracking-widest text-[#C89B3C] font-semibold pt-2">
                  View Masterpieces →
                </span>
              </div>
            </div>

            {/* Subtle Silk Ribbon Overlay effect */}
            <div className="absolute inset-0 border border-[#C89B3C]/0 group-hover:border-[#C89B3C]/20 rounded-3xl transition-colors duration-500 pointer-events-none"></div>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
