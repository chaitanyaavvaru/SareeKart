
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play, X } from 'lucide-react';

export default function ArtisanDocumentary() {
  const [videoOpen, setVideoOpen] = useState(false);

  return (
    <section className="bg-[#1A0C11] text-[#FAF8F5] py-24 sm:py-32 overflow-hidden relative">
      {/* Background Graphic Grid */}
      <div className="absolute inset-0 opacity-5 pointer-events-none bg-[linear-gradient(rgba(200,155,60,0.15)_1px,transparent_1px)] [background-size:40px_40px]"></div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
          
          {/* Left Column: Visual Film Frame */}
          <motion.div 
            initial={{ opacity: 0, scale: 0.96 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
            className="lg:col-span-6 relative aspect-[16/10] rounded-3xl overflow-hidden border border-white/10 group cursor-pointer shadow-luxury"
            onClick={() => setVideoOpen(true)}
          >
            <img 
              src="https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=800" 
              alt="Artisan loom movie cover photography"
              className="w-full h-full object-cover group-hover:scale-102 transition-transform duration-[1.2s] ease-out"
            />
            {/* Cinematic dark overlay */}
            <div className="absolute inset-0 bg-black/35 group-hover:bg-black/20 transition-colors duration-500"></div>

            {/* Pulsing Play Button */}
            <div className="absolute inset-0 flex items-center justify-center">
              <motion.div 
                animate={{ scale: [1, 1.08, 1] }}
                transition={{ repeat: Infinity, duration: 2 }}
                className="w-16 h-16 rounded-full bg-[#C89B3C] hover:bg-[#A37E30] transition-colors flex items-center justify-center shadow-lg text-[#2B0F1E]"
              >
                <Play className="w-6 h-6 fill-current text-[#2B0F1E] translate-x-0.5" />
              </motion.div>
            </div>
            
            <div className="absolute bottom-4 left-6 text-[10px] uppercase tracking-widest text-[#C89B3C] font-sans font-semibold">
              Artisan Documentary • 4:20 Min Film
            </div>
          </motion.div>

          {/* Right Column: Documentary Text & Concept */}
          <motion.div 
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.15 }}
            className="lg:col-span-6 space-y-6 text-left"
          >
            <div className="flex items-center gap-2">
              <span className="h-px w-6 bg-[#C89B3C]"></span>
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">
                Artisan Documentary
              </span>
            </div>
            
            <h2 className="text-3xl sm:text-5xl font-serif font-bold text-[#FAF8F5] leading-tight">
              Threads of Fate: <br />
              <span className="italic font-medium text-[#C89B3C]">The Weaver's Hand.</span>
            </h2>
            
            <p className="text-xs sm:text-sm text-white/70 font-sans leading-relaxed font-light">
              Step inside the wooden loom compounds of Varanasi and Patan. This short documentary showcases the generational struggles, rhythmic precision, and technical triumph of weavers turning gold thread into royal silk canvas.
            </p>

            <button 
              onClick={() => setVideoOpen(true)}
              className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-[#C89B3C] hover:text-white transition-colors"
            >
              Watch Documentary Film <span className="text-md">→</span>
            </button>
          </motion.div>

        </div>
      </div>

      {/* Embedded Video Modal Overlay */}
      <AnimatePresence>
        {videoOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[200] bg-black/95 flex items-center justify-center p-4 backdrop-blur-xs"
          >
            <button 
              onClick={() => setVideoOpen(false)}
              className="absolute top-6 right-6 text-white/60 hover:text-white transition-colors"
            >
              <X className="w-8 h-8" />
            </button>
            <div className="w-full max-w-4xl aspect-video rounded-3xl overflow-hidden shadow-2xl border border-white/10 bg-black">
              {/* Premium fallback showing visual weaving loop */}
              <iframe
                className="w-full h-full"
                src="https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1"
                title="Loom Documentary Video"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              ></iframe>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  );
}
