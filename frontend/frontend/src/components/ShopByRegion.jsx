
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowRight, Compass, User } from 'lucide-react';
import SafeImage from './common/SafeImage';

const REGIONS_DATA = [
  {
    id: 'varanasi',
    name: 'Varanasi (Banarasi)',
    history: 'Dating back to the Rigveda, Varanasi brocades reached their golden age under the Mughal Empire. Artisans weave real gold threads into silk structures.',
    artisan: 'Ramzan Ali (Master Weaver, Banarasi Guild)',
    sarees: ['Katan Gold Jamdani', 'Tanchoi Silk Brocade', 'Shattir Floral Saree'],
    originStory: 'The weaving process uses wooden pit-looms with thousands of cards (Jala) guiding warp lines.',
    image: 'https://images.unsplash.com/photo-1561361530-256e1530-256e1530256e?w=800',
    stats: '120,000 active weavers in the guild'
  },
  {
    id: 'kanchipuram',
    name: 'Kanchipuram',
    history: 'Preserved by the Pallava dynasty since the 7th century. Weavers dye Mulberry threads and load them using three separate shuttles.',
    artisan: 'Srinivasa Mudaliar (National Award Winner)',
    sarees: ['Taranga Bridal Tissue Gold', 'Muttela Temple border Silk', 'Korvai Contrast Silk Saree'],
    originStory: 'The border is woven separately and joined to the body with a lock-weave (Korvai) technique.',
    image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800',
    stats: 'Temple town heritage, Tamil Nadu'
  },
  {
    id: 'patan',
    name: 'Patan (Patola)',
    history: 'A royal weave originating in Gujarat. Only a handful of families preserve the double-ikat tie-dye system.',
    artisan: 'Sanjay Salvi (Patan Handloom Guild)',
    sarees: ['Shakuntala Double Ikat Patola', 'Nari Kunjar Silk Drape'],
    originStory: 'Dyeing warp and weft coordinates before loading them onto the loom ensures patterns match on both sides.',
    image: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800',
    stats: 'Up to 6 months to weave a single drape'
  },
  {
    id: 'pochampally',
    name: 'Pochampally',
    history: 'Located in Telangana, this region is famous for transferring tie-and-dye configurations to heavy silk.',
    artisan: 'Mallaiah Devangan (Ikat Coordinator)',
    sarees: ['Geometric Ikkat Silk Saree', 'Navrang Double Ikat'],
    originStory: 'The threads are wrapped in rubber ties to dye specific parts before weaving.',
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=800',
    stats: 'Deccan plateau geographic registration'
  },
  {
    id: 'venkatagiri',
    name: 'Venkatagiri',
    history: 'Favored by the kings of Venkatagiri, these sheer cotton weaves are inlaid with golden Jamdani motifs.',
    artisan: 'Aruna Devi (Weaving Guild President)',
    sarees: ['Black Butta Cotton Jamdani', 'Ivory Zari Silk Cotton Saree'],
    originStory: 'Weavers loop fine gold threads by hand into the cotton warp without using a shuttle.',
    image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800',
    stats: 'Royal court patronage since the 14th century'
  }
];

export default function ShopByRegion() {
  const [selectedRegion, setSelectedRegion] = useState(REGIONS_DATA[0]);
  const navigate = useNavigate();

  const handleShopRegion = (regionName) => {
    navigate(`/products?search=${encodeURIComponent(regionName)}`);
  };

  return (
    <section className="w-full min-h-screen bg-[#FAF8F5] flex items-center justify-center py-32 border-t border-[#F4F2EB]">
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full space-y-16">
        
        {/* Header Block */}
        <div className="text-center space-y-3">
          <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
            Geographic Heritage
          </span>
          <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
            Shop by Weaving Region
          </h2>
          <p className="text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
            Explore drapes mapped to regional guilds, celebrating local materials and age-old weaving secrets.
          </p>
        </div>

        {/* Dynamic Interactive Region Explorer */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-start">
          
          {/* Left Column: Interactive Regions Selector (Span 5 cols) */}
          <div className="lg:col-span-5 flex flex-col divide-y divide-[#E6DFD3] text-left">
            {REGIONS_DATA.map((reg) => (
              <button
                key={reg.id}
                onClick={() => setSelectedRegion(reg)}
                className={`py-6 flex items-center justify-between group focus:outline-none transition-all ${
                  selectedRegion.id === reg.id ? 'pl-4' : 'pl-0'
                }`}
              >
                <div className="space-y-1">
                  <h3 className={`text-xl sm:text-3xl font-serif italic transition-colors duration-300 ${
                    selectedRegion.id === reg.id ? 'text-[#C89B3C] font-bold' : 'text-[#2B0F1E] hover:text-[#C89B3C]'
                  }`}>
                    {reg.name}
                  </h3>
                  <p className="text-xs text-text-secondary font-sans font-light">
                    {reg.stats}
                  </p>
                </div>
                <div className={`w-8 h-8 rounded-full border flex items-center justify-center transition-all ${
                  selectedRegion.id === reg.id ? 'bg-[#2B0F1E] text-white border-[#C89B3C]' : 'border-[#E6DFD3] text-[#2B0F1E]'
                }`}>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </button>
            ))}
          </div>

          {/* Right Column: Immersive Detail Card (Span 7 cols) */}
          <div className="lg:col-span-7 bg-[#2B0F1E] text-white rounded-[28px] p-8 sm:p-12 border border-[#C89B3C]/30 shadow-luxury relative overflow-hidden min-h-[600px] flex flex-col justify-between">
            <div className="absolute top-0 right-0 w-48 h-48 bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:20px_20px] opacity-10 pointer-events-none"></div>

            <AnimatePresence mode="wait">
              <motion.div
                key={selectedRegion.id}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -15 }}
                transition={{ duration: 0.4 }}
                className="grid grid-cols-1 sm:grid-cols-12 gap-8 items-start text-left"
              >
                {/* Details Text Content (Span 7 cols) */}
                <div className="sm:col-span-7 space-y-6">
                  <div className="space-y-2">
                    <span className="text-[10px] uppercase font-bold text-[#C89B3C] tracking-[0.2em] font-sans">Origin Story & history</span>
                    <h3 className="text-2xl sm:text-4xl font-serif font-bold text-white leading-tight">
                      {selectedRegion.name}
                    </h3>
                  </div>

                  <p className="text-xs sm:text-sm text-white/80 font-light leading-relaxed font-sans">
                    {selectedRegion.history}
                  </p>

                  <div className="space-y-3 pt-4 border-t border-white/10">
                    <div className="flex gap-2 items-center text-xs">
                      <User className="w-4 h-4 text-[#C89B3C]" />
                      <div>
                        <p className="text-[9px] uppercase text-white/50 font-bold tracking-widest font-sans">Lead Artisan</p>
                        <p className="font-semibold text-white/90 mt-0.5 font-sans">{selectedRegion.artisan}</p>
                      </div>
                    </div>
                    <div className="flex gap-2 items-center text-xs pt-1">
                      <Compass className="w-4 h-4 text-[#C89B3C]" />
                      <div>
                        <p className="text-[9px] uppercase text-white/50 font-bold tracking-widest font-sans">Weaving Technique</p>
                        <p className="font-semibold text-white/90 mt-0.5 font-sans">{selectedRegion.originStory}</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Regional Image & Signature Sarees List (Span 5 cols) */}
                <div className="sm:col-span-5 space-y-6">
                  <div className="aspect-[4/5] rounded-xl overflow-hidden border border-white/10 shadow bg-black/20">
                    <SafeImage 
                      src={selectedRegion.image} 
                      alt={selectedRegion.name} 
                      productName={selectedRegion.name}
                      category="Regional weave"
                      aspectRatioClass="aspect-[4/5]"
                      className="w-full h-full object-cover"
                    />
                  </div>

                  <div className="space-y-2 text-xs">
                    <span className="text-[9px] uppercase font-bold text-[#C89B3C] tracking-widest font-sans block">Signature Sarees</span>
                    <ul className="space-y-1 text-white/85 pl-4 list-disc font-sans font-light">
                      {selectedRegion.sarees.map((s, idx) => (
                        <li key={idx}>{s}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>

            <div className="pt-6 border-t border-white/10 mt-8 flex justify-end">
              <button 
                onClick={() => handleShopRegion(selectedRegion.name)}
                className="w-full sm:w-auto px-8 py-4 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl text-xs uppercase tracking-widest transition-all duration-300 flex items-center justify-center gap-2 cursor-pointer shadow"
              >
                Shop Region Collection <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
