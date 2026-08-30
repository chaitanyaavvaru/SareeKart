
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowRight, Sparkles, Heart } from 'lucide-react';

const ARTISANS = [
  {
    id: 1,
    name: 'Srinivasa Chari',
    region: 'Kanchipuram, TN',
    expertise: '3-Shuttle Temple Borders',
    portrait: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600', // Senior master craftsman profile
    story: 'Inheriting the wooden pit-looms of his family, Srinivasa spends up to 28 days handweaving a single Kanchipuram silk drape. His specialized multi-shuttle weaving ensures that border edges transition with surgical precision.',
    achievement: 'National Handloom Award, 2018'
  },
  {
    id: 2,
    name: 'Lakshmi Devangan',
    region: 'Pochampally, TS',
    expertise: 'Double-Ikat Tie & Dye',
    portrait: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600', // Traditional female weaver profile
    story: 'Lakshmi calculates complex geometric alignment formulas purely by memory. She ties and dyes thread bundles meticulously before loading them onto the loom, transforming silk threads into sharp graphic artworks.',
    achievement: 'Stree Shakti Artisanal Award, 2021'
  },
  {
    id: 3,
    name: 'Kabir Ansari',
    region: 'Varanasi, UP',
    expertise: '24K Gold Zardozi Embroidery',
    portrait: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=600', // Elderly artisan profile
    story: 'Kabir sits cross-legged over wooden slate frames in the heart of Banaras, sewing tiny bullion wires and seed pearls onto thick silk brocades. His embroidery adds weight and heritage grandeur to royal drapes.',
    achievement: 'Sangeet Natak Guild Medal, 2015'
  }
];

export default function MeetArtisans() {
  const [selectedArtisan, setSelectedArtisan] = useState(ARTISANS[0]);

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 bg-white rounded-3xl border border-[#F4F2EB] shadow-xs mt-6">
      <div className="text-center space-y-3 mb-16">
        <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
          Behind the Looms
        </span>
        <h2 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">
          Meet the Master Artisans
        </h2>
        <p className="text-xs sm:text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
          We strip away the middleman to fund weavers directly, preserving generations of manual handloom craftsmanship.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
        {/* Left Side: Portraits Selector Cards (Span 5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          {ARTISANS.map((item) => (
            <div 
              key={item.id}
              onClick={() => setSelectedArtisan(item)}
              className={`p-5 rounded-2xl border transition-all duration-500 cursor-pointer flex gap-4 items-center ${
                selectedArtisan.id === item.id 
                  ? 'border-[#C89B3C] bg-[#F9F6F1] shadow-soft' 
                  : 'border-[#F4F2EB] bg-white hover:border-[#C89B3C]/50'
              }`}
            >
              <div className="w-16 h-16 rounded-full overflow-hidden shrink-0 border border-[#E6DFD3]">
                <img 
                  src={item.portrait} 
                  alt={item.name} 
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="text-left">
                <span className="text-[9px] uppercase font-bold text-[#C89B3C] tracking-widest">{item.region}</span>
                <h4 className="text-sm font-bold text-[#2B0F1E] font-sans mt-0.5">{item.name}</h4>
                <p className="text-xs text-text-secondary font-light font-sans">{item.expertise}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Right Side: Immersive Feature Details Box (Span 7 cols) */}
        <div className="lg:col-span-7 bg-[#F9F6F1]/50 border border-[#F4F2EB] rounded-[28px] p-8 md:p-12 text-left relative overflow-hidden min-h-[400px] flex flex-col justify-between">
          <div className="absolute top-0 right-0 w-32 h-32 bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:16px_16px] opacity-10 pointer-events-none"></div>

          <AnimatePresence mode="wait">
            <motion.div
              key={selectedArtisan.id}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.4 }}
              className="space-y-6"
            >
              <div className="flex items-center gap-3">
                <span className="bg-[#2B0F1E] text-white text-[8px] font-bold uppercase tracking-[0.15em] px-2.5 py-1 rounded-md">
                  {selectedArtisan.region}
                </span>
                <span className="flex items-center gap-1 text-[10px] text-[#C89B3C] font-bold uppercase tracking-wider font-sans">
                  <Sparkles className="w-3.5 h-3.5" /> {selectedArtisan.achievement}
                </span>
              </div>

              <div className="space-y-2">
                <h3 className="text-2xl sm:text-3xl font-bold font-serif text-[#2B0F1E]">{selectedArtisan.name}</h3>
                <p className="text-xs text-[#C89B3C] font-semibold font-sans italic">{selectedArtisan.expertise}</p>
              </div>

              <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-light font-sans">
                "{selectedArtisan.story}"
              </p>
            </motion.div>
          </AnimatePresence>

          <div className="pt-8 border-t border-[#E6DFD3] mt-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
            <span className="text-[10px] text-text-muted flex items-center gap-1.5 uppercase font-bold tracking-wider font-sans">
              <Heart className="w-4 h-4 text-red-500 fill-current" /> Funds go 100% direct to weaver
            </span>
            <button className="text-xs font-bold text-[#2B0F1E] hover:text-[#C89B3C] uppercase tracking-widest flex items-center gap-1.5 transition-colors font-sans cursor-pointer">
              Explore drapes from this region <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
