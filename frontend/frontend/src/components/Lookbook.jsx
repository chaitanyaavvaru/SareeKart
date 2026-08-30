
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, ArrowRight } from 'lucide-react';

const slides = [
  {
    image: "https://kankatala.com/cdn/shop/files/1215863175_2.webp?v=1761908736",
    title: "Chitra: The Golden Hour",
    description: "Wrapped in heavy tissue zari border, catching the soft rays of twilight. Inspired by the royal archives of Jaipur, 1923.",
    category: "LooKBook Drop • Edition I"
  },
  {
    image: "https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250",
    title: "Mayura: Crimson Reverie",
    description: "Intricately spun red Banarasi borders detailing handloom peacocks and floral creepers. A majestic wedding statement.",
    category: "LookBook Drop • Edition II"
  },
  {
    image: "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=1000",
    title: "Kamala: Lotus Sanctuary",
    description: "Pure Kanchipuram bridal drapes woven with organic red dye and temple zaris, symbolizing sacred union.",
    category: "LookBook Drop • Edition III"
  }
];

export default function Lookbook() {
  const [activeSlide, setActiveSlide] = useState(0);

  const nextSlide = () => {
    setActiveSlide((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
  };

  const prevSlide = () => {
    setActiveSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
  };

  // Keyboard a11y for carousel
  const onKeyDown = (e) => {
    if (e.key === 'ArrowRight') nextSlide();
    if (e.key === 'ArrowLeft') prevSlide();
  };

  return (
    <section onKeyDown={onKeyDown} tabIndex={0} aria-label="Heritage Lookbook carousel" className="bg-[#2B0F1E] text-white py-24 sm:py-32 overflow-hidden relative focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Lookbook Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 gap-6">
          <div className="space-y-3">
            <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
              Editorial Feature
            </span>
            <h2 className="text-3xl sm:text-5xl font-serif font-bold text-white leading-tight">
              The Heritage Lookbook
            </h2>
          </div>
          
          <div className="flex gap-4">
            <button 
              onClick={prevSlide}
              aria-label="Previous slide"
              className="w-12 h-12 rounded-full border border-white/20 hover:border-[#C89B3C] hover:text-[#C89B3C] focus-visible:ring-2 focus-visible:ring-gold flex items-center justify-center transition-colors cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            </button>
            <button 
              onClick={nextSlide}
              aria-label="Next slide"
              className="w-12 h-12 rounded-full border border-white/20 hover:border-[#C89B3C] hover:text-[#C89B3C] focus-visible:ring-2 focus-visible:ring-gold flex items-center justify-center transition-colors cursor-pointer"
            >
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </button>
          </div>
        </div>

        {/* Carousel Visual Spread */}
        <div className="relative h-[450px] sm:h-[600px] rounded-3xl overflow-hidden shadow-luxury border border-white/10 bg-[#1A0C11]">
          <AnimatePresence mode="wait">
            <motion.div 
              key={activeSlide}
              initial={{ opacity: 0, scale: 1.02 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.98 }}
              transition={{ duration: 0.9, ease: "easeInOut" }}
              className="absolute inset-0 grid grid-cols-1 lg:grid-cols-12 items-stretch"
            >
              {/* Photo Pane */}
              <div className="lg:col-span-7 relative h-[250px] lg:h-auto overflow-hidden">
                <img 
                  src={slides[activeSlide].image} 
                  alt={slides[activeSlide].title} 
                  className="w-full h-full object-cover object-top"
                />
                <div className="absolute inset-0 bg-gradient-to-r from-transparent to-[#2B0F1E]/30"></div>
              </div>

              {/* Info Pane */}
              <div className="lg:col-span-5 p-8 sm:p-12 flex flex-col justify-center space-y-6 text-left bg-[#2B0F1E] border-t lg:border-t-0 lg:border-l border-white/10">
                <span className="text-[#C89B3C] font-sans text-[10px] font-bold uppercase tracking-[0.2em]">
                  {slides[activeSlide].category}
                </span>
                
                <h3 className="font-serif font-bold text-2xl sm:text-3xl text-white leading-tight">
                  {slides[activeSlide].title}
                </h3>
                
                <p className="text-xs sm:text-sm text-white/70 font-sans font-light leading-relaxed">
                  {slides[activeSlide].description}
                </p>

                <div className="pt-4 border-t border-white/10 flex items-center justify-between">
                  <span className="text-[10px] uppercase font-bold text-white/50 tracking-widest">
                    Slide {activeSlide + 1} of {slides.length}
                  </span>
                  <div className="flex items-center gap-1.5" role="tablist" aria-label="Lookbook slides">
                    {slides.map((_, idx) => (
                      <button
                        key={idx}
                        role="tab"
                        aria-selected={idx === activeSlide}
                        aria-label={`Go to slide ${idx + 1}`}
                        onClick={() => setActiveSlide(idx)}
                        className={`h-1.5 rounded-full transition-all ${idx === activeSlide ? 'w-6 bg-[#C89B3C]' : 'w-1.5 bg-white/30 hover:bg-white/50'}`}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

      </div>
    </section>
  );
}
