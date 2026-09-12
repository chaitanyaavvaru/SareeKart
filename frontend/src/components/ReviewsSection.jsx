import React, { useState } from 'react';
import { Star, Quote, Award, Sparkles, Medal } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const STORIES = [
  {
    id: 1,
    name: "Aradhana Sen",
    location: "Kolkata",
    rating: 5,
    quote: "The weave density on the Kanchi Tissue Silk is breathtaking. It is light yet carries a gorgeous metallic golden sheen. Reminds me of the sarees in my grandmother's trousseau.",
    product: "Taranga Kanchi Silk Saree"
  },
  {
    id: 2,
    name: "Priya Chandrasekhar",
    location: "Chennai",
    rating: 5,
    quote: "Purchased the Venkatagiri cotton drape. Absolute luxury for everyday workwear! The fabric breathes beautifully and the Jamdani weaves are sharp and precise. Will buy again.",
    product: "Venkatagiri Cotton Saree"
  },
  {
    id: 3,
    name: "Meera Deshmukh",
    location: "Mumbai",
    rating: 5,
    quote: "Magnificent Pochampally Ikat! The geometric colors match the pictures exactly. Delivery was fast and packed inside a gorgeous organic cotton trunk box.",
    product: "Pochampally Silk Ikat Saree"
  }
];

const PRESS_ACCREDITATIONS = [
  {
    id: 1,
    source: "Vogue India",
    acclaim: "SareeKart is redefining digital heritage. It brings the precision of luxury design to Indian handloom preservation.",
    logo: "VOGUE"
  },
  {
    id: 2,
    source: "National Weaves Guild",
    acclaim: "Awarded the Craft Innovation Prize 2026 for creating transparent, direct-to-artisan funding networks.",
    logo: "GUILD HONORS"
  },
  {
    id: 3,
    source: "Silk Mark India",
    acclaim: "100% certified organic threads, verifying absolute pure mulberry silk authenticity on all catalog items.",
    logo: "SILK MARK CERTIFIED"
  }
];

export default function ReviewsSection() {
  const [activeStoryIdx, setActiveStoryIdx] = useState(0);

  return (
    <section className="w-full min-h-[90vh] bg-white flex items-center justify-center py-32 border-t border-[#E6DFD3]">
      <div className="max-w-[1640px] mx-auto px-[80px] w-full grid grid-cols-1 lg:grid-cols-12 gap-16 items-start select-none">
        
        {/* Left Side: Testimonials & Customer Stories (Span 6 cols) */}
        <div className="lg:col-span-6 space-y-12 text-left">
          <div className="space-y-3">
            <span className="text-[#C8A04D] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-1.5 block">
              <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Patron Chronicles
            </span>
            <h2 className="text-4xl sm:text-5xl font-serif font-bold text-[#3A0F1F] leading-tight">
              Customer Stories
            </h2>
            <p className="text-sm text-[#6b5c4d] font-light font-sans">
              Patrons who celebrate handwoven luxury and support weaver families.
            </p>
          </div>

          <div className="relative bg-[#FAF8F5] border border-[#E6DFD3] p-10 rounded-[28px] shadow-soft min-h-[320px] flex flex-col justify-between">
            <Quote className="absolute top-8 right-8 w-12 h-12 text-[#C8A04D]/15" />
            
            <AnimatePresence mode="wait">
              <motion.div
                key={activeStoryIdx}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.3 }}
                className="space-y-6"
              >
                {/* Stars */}
                <div className="flex gap-1 text-[#C8A04D]">
                  {[...Array(STORIES[activeStoryIdx].rating)].map((_, i) => (
                    <Star key={i} className="w-4 h-4 fill-current text-[#C8A04D]" />
                  ))}
                </div>

                <p className="text-base sm:text-lg italic text-[#3A0F1F] font-serif leading-relaxed pr-6">
                  "{STORIES[activeStoryIdx].quote}"
                </p>

                <div className="pt-6 border-t border-[#E6DFD3] flex justify-between items-center text-xs">
                  <div>
                    <p className="font-bold text-[#3A0F1F] font-sans text-sm">{STORIES[activeStoryIdx].name}</p>
                    <p className="text-[#6b5c4d] mt-0.5 font-sans font-light">{STORIES[activeStoryIdx].location}</p>
                  </div>
                  <span className="text-[10px] uppercase tracking-widest text-[#C8A04D] font-bold bg-white px-3 py-1.5 border border-[#E6DFD3] rounded-lg font-sans shadow-sm">
                    {STORIES[activeStoryIdx].product}
                  </span>
                </div>
              </motion.div>
            </AnimatePresence>

            {/* Slide triggers */}
            <div className="flex gap-2.5 pt-6 justify-start z-10">
              {STORIES.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setActiveStoryIdx(idx)}
                  className={`h-2.5 rounded-full transition-all duration-300 cursor-pointer ${
                    activeStoryIdx === idx ? 'w-8 bg-[#3A0F1F]' : 'w-2.5 bg-[#E6DFD3] hover:bg-[#C8A04D]'
                  }`}
                  aria-label={`Go to slide ${idx + 1}`}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Right Side: Press Mentions & Certifications (Span 6 cols) */}
        <div className="lg:col-span-6 space-y-12 text-left lg:pl-6">
          <div className="space-y-3">
            <span className="text-[#C8A04D] font-serif text-xs font-bold tracking-[0.25em] uppercase flex items-center gap-1.5 block">
              <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Global Recognition
            </span>
            <h2 className="text-4xl sm:text-5xl font-serif font-bold text-[#3A0F1F] leading-tight">
              Craft Appreciations
            </h2>
            <p className="text-sm text-[#6b5c4d] font-light font-sans">
              Press recognitions, weave quality awards, and certification logs.
            </p>
          </div>

          <div className="space-y-6">
            {PRESS_ACCREDITATIONS.map((press) => (
              <div 
                key={press.id}
                className="p-6 bg-[#FAF8F5] border border-[#E6DFD3] rounded-2xl flex gap-5 items-start hover:border-[#C8A04D]/60 hover:shadow-soft transition-all duration-300"
              >
                <div className="w-12 h-12 rounded-full bg-white border border-[#E6DFD3] flex items-center justify-center shrink-0 text-[#C8A04D] shadow-sm">
                  {press.id === 1 ? <Sparkles className="w-5 h-5" /> : press.id === 2 ? <Award className="w-5 h-5" /> : <Medal className="w-5 h-5" />}
                </div>
                
                <div className="space-y-1.5">
                  <div className="flex items-center gap-3">
                    <h4 className="text-xs font-bold font-sans uppercase tracking-wider text-[#3A0F1F]">{press.source}</h4>
                    <span className="text-[8px] font-bold uppercase text-[#C8A04D] bg-white px-2 py-0.5 rounded border border-[#E6DFD3]">{press.logo}</span>
                  </div>
                  <p className="text-xs text-[#6b5c4d] leading-relaxed font-sans font-light">
                    {press.acclaim}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
    </section>
  );
}
