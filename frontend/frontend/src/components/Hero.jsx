
import { Link } from 'react-router-dom';
import { ArrowRight, Play } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Hero() {
  return (
    <section className="relative min-h-[120vh] bg-[#2B0F1E] flex items-center justify-center overflow-hidden z-10 pt-20">
      
      {/* Background Texture & Weave Pattern Overlay */}
      <div className="absolute inset-0 opacity-15 pointer-events-none mix-blend-overlay z-0 bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:24px_24px]"></div>
      
      {/* Cinematic Spotlight Gradient */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(200,155,60,0.15),transparent_55%)] z-0"></div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full py-16 sm:py-24">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          
          {/* Left Column: Typography Spread */}
          <div className="lg:col-span-6 space-y-8 text-left">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, ease: "easeOut" }}
              className="space-y-4"
            >
              <div className="flex items-center gap-2">
                <span className="h-px w-8 bg-[#C89B3C]"></span>
                <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">
                  Since 1987
                </span>
              </div>
              
              <h1 className="text-4xl sm:text-6xl lg:text-7xl font-serif font-bold text-white leading-[1.1] tracking-tight">
                Timeless Elegance, <br />
                <span className="text-[#C89B3C] font-serif italic font-medium">Woven For Generations.</span>
              </h1>
              
              <p className="text-xs sm:text-sm md:text-base text-white/70 max-w-lg font-light leading-relaxed font-sans pt-2">
                Every saree carries generations of artistry, woven by master craftsmen across India. Handloom silk and heritage tissue curated for the discerning eye.
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.3, ease: "easeOut" }}
              className="flex flex-wrap gap-4 pt-4"
            >
              <Link 
                to="/products"
                className="inline-flex items-center justify-center gap-2.5 px-8 py-4 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl shadow-lg transition-all font-sans text-xs tracking-[0.15em] uppercase cursor-pointer"
              >
                Explore Collection <ArrowRight className="w-3.5 h-3.5" />
              </Link>
              <button 
                onClick={() => {
                  const target = document.getElementById("legacy-story");
                  if (target) target.scrollIntoView({ behavior: 'smooth' });
                }}
                className="inline-flex items-center justify-center gap-2.5 px-7 py-4 border border-white/20 hover:border-white hover:bg-white/5 text-white font-bold rounded-xl transition-all font-sans text-xs tracking-[0.15em] uppercase cursor-pointer"
              >
                <Play className="w-3.5 h-3.5 fill-white text-white" /> Watch Craft Story
              </button>
            </motion.div>
          </div>

          {/* Right Column: Editorial Lifestyle Image */}
          <div className="lg:col-span-6 relative flex justify-center">
            
            {/* Elegant Background Gold Border Frame */}
            <div className="absolute inset-4 border border-[#C89B3C]/30 rounded-2xl transform translate-x-3 translate-y-3 -z-10 pointer-events-none"></div>
            
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              transition={{ duration: 1, delay: 0.2 }}
              className="relative w-full max-w-md aspect-[3/4] rounded-2xl overflow-hidden shadow-luxury border border-white/10 bg-white/5 backdrop-blur-xs group"
            >
              <img 
                src="https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=1000" 
                alt="Luxury handwoven Banarasi tissue saree bridal photoshoot"
                className="w-full h-full object-cover object-top transition-transform duration-1000 ease-out group-hover:scale-103"
              />
              
              {/* Subtle Floating Gold Particles (Decorative Absolute Overlay) */}
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_bottom,rgba(200,155,60,0.1),transparent_70%)] pointer-events-none"></div>
            </motion.div>
          </div>

        </div>
      </div>

      {/* Elegant Bottom Scroll Indicator */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 text-white/40 text-[9px] font-bold uppercase tracking-[0.25em] font-sans pointer-events-none select-none">
        <span>Scroll</span>
        <motion.div 
          animate={{ y: [0, 6, 0] }} 
          transition={{ repeat: Infinity, duration: 1.5, ease: "easeInOut" }}
          className="w-0.5 h-4 bg-[#C89B3C] rounded-full"
        />
      </div>
    </section>
  );
}
