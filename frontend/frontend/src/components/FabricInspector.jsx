
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldCheck, Cpu, Compass } from 'lucide-react';

const HIGHLIGHTS = [
  {
    id: 1,
    x: '35%',
    y: '45%',
    title: '24K Gold Zari Threading',
    description: 'Genuine gold-washed silver threads intricately wrapped around a premium silk core for an unmatched royal shimmer.',
    icon: <Compass className="w-4 h-4" />
  },
  {
    id: 2,
    x: '62%',
    y: '30%',
    title: 'Mulberry Silk Weft',
    description: 'Grade 6A mulberry silk threads woven double-width to ensure optimal weight, longevity, and a fluid, liquid-like drape.',
    icon: <ShieldCheck className="w-4 h-4" />
  },
  {
    id: 3,
    x: '50%',
    y: '70%',
    title: 'Kora Organza Weave Density',
    description: 'Fine, hand-spun sheer fibers yielding a crisp structure that holds signature folds while breathing effortlessly.',
    icon: <Cpu className="w-4 h-4" />
  }
];

export default function FabricInspector() {
  const [activeHighlight, setActiveHighlight] = useState(null);
  const [zoomPos, setZoomPos] = useState({ x: 50, y: 50 });
  const [isHovered, setIsHovered] = useState(false);
  const containerRef = useRef(null);

  const handleMouseMove = (e) => {
    if (!containerRef.current) return;
    const { left, top, width, height } = containerRef.current.getBoundingClientRect();
    const x = ((e.clientX - left) / width) * 100;
    const y = ((e.clientY - top) / height) * 100;
    setZoomPos({ x, y });
  };

  return (
    <section className="w-full min-h-screen bg-white flex items-center justify-center border-t border-[#F4F2EB] py-32">
      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
        
        {/* Left Side: Massive ultra-HD fabric image (Span 7 cols, min-height 700px) */}
        <div className="lg:col-span-7 w-full">
          <div 
            ref={containerRef}
            onMouseMove={handleMouseMove}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => {
              setIsHovered(false);
              setActiveHighlight(null);
            }}
            className="relative w-full min-h-[500px] lg:min-h-[700px] rounded-[28px] overflow-hidden border border-[#E6DFD3] shadow-luxury cursor-zoom-in bg-[#F9F6F1]"
          >
            {/* Base Macro Image */}
            <img 
              src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=1200" 
              alt="Macro saree weaving texture" 
              className="absolute inset-0 w-full h-full object-cover select-none pointer-events-none transition-opacity duration-300"
              style={{ opacity: isHovered ? 0.3 : 1 }}
            />

            {/* Magnified Texture Background Layer (Reveals on Hover) */}
            {isHovered && (
              <div 
                className="absolute inset-0 bg-cover bg-no-repeat transition-all duration-100 ease-out select-none pointer-events-none"
                style={{
                  backgroundImage: `url('https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=2000')`,
                  backgroundPosition: `${zoomPos.x}% ${zoomPos.y}%`,
                  backgroundSize: '220%'
                }}
              />
            )}

            {/* Interactive Pulse Hotspots */}
            {!isHovered && HIGHLIGHTS.map((item) => (
              <div
                key={item.id}
                className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer z-20 group"
                style={{ left: item.x, top: item.y }}
                onMouseEnter={() => setActiveHighlight(item)}
                onMouseLeave={() => setActiveHighlight(null)}
              >
                {/* Ping Ring */}
                <div className="absolute w-8 h-8 rounded-full bg-[#C89B3C]/40 animate-ping"></div>
                {/* Core Dot */}
                <div className="relative w-5 h-5 rounded-full bg-[#C89B3C] border-2 border-white shadow flex items-center justify-center transition-transform group-hover:scale-125">
                  <div className="w-2.5 h-2.5 rounded-full bg-[#2B0F1E]"></div>
                </div>
              </div>
            ))}

            {/* Hover Magnifying Lens Glass Guide Overlay */}
            {isHovered && (
              <div 
                className="absolute border-2 border-[#C89B3C] rounded-full w-32 h-32 pointer-events-none transform -translate-x-1/2 -translate-y-1/2 shadow-lg hidden md:block z-30"
                style={{ left: `${zoomPos.x}%`, top: `${zoomPos.y}%` }}
              >
                <div className="absolute inset-0 rounded-full border border-white/20 bg-white/5 backdrop-blur-[1px]"></div>
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Luxury explanation content (Span 5 cols) */}
        <div className="lg:col-span-5 space-y-8 text-left lg:pl-6">
          <div className="space-y-3">
            <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
              Tactile Luxury
            </span>
            <h2 className="text-4xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">
              Inspect the Microweave
            </h2>
            <p className="text-sm text-text-secondary font-light leading-relaxed font-sans">
              Every thread holds a legacy. Hover over the silk weave on the left to magnify the texture, or tap active gold highlights to examine our gold zari embroidery details.
            </p>
          </div>

          <div className="space-y-4 pt-6 border-t border-[#F4F2EB]">
            {HIGHLIGHTS.map((item) => (
              <div 
                key={item.id}
                onMouseEnter={() => setActiveHighlight(item)}
                onMouseLeave={() => setActiveHighlight(null)}
                className={`p-5 rounded-2xl border transition-all duration-300 cursor-pointer ${
                  activeHighlight?.id === item.id 
                    ? 'bg-[#F9F6F1] border-[#C89B3C] shadow-soft' 
                    : 'border-[#F4F2EB] bg-white'
                }`}
              >
                <div className="flex items-center gap-3 text-[#2B0F1E]">
                  <div className="text-[#C89B3C]">{item.icon}</div>
                  <h4 className="text-xs font-bold uppercase tracking-widest font-sans">{item.title}</h4>
                </div>
                <p className="text-xs text-text-secondary mt-2 font-light leading-relaxed pl-7">
                  {item.description}
                </p>
              </div>
            ))}
          </div>

          <AnimatePresence>
            {activeHighlight && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 10 }}
                className="bg-[#2B0F1E] text-white p-6 rounded-2xl border border-[#C89B3C]/30 shadow-luxury"
              >
                <div className="flex items-center gap-2 text-[#C89B3C] mb-2">
                  {activeHighlight.icon}
                  <h5 className="text-xs font-bold uppercase tracking-widest font-sans">{activeHighlight.title}</h5>
                </div>
                <p className="text-xs text-[#FAF8F5]/85 leading-relaxed font-light font-sans">
                  {activeHighlight.description}
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

      </div>
    </section>
  );
}
