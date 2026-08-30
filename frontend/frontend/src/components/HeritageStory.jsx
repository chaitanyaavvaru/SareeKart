
import { motion } from 'framer-motion';

export default function HeritageStory() {
  return (
    <section id="legacy-story" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 bg-white rounded-3xl border border-[#F4F2EB] shadow-xs overflow-hidden mt-6 scroll-mt-24">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
        
        {/* Left Column: Story & Philosophy */}
        <motion.div 
          initial={{ opacity: 0, x: -30 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, ease: "easeOut" }}
          className="lg:col-span-7 space-y-8"
        >
          <div className="space-y-3">
            <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
              Our Legacy
            </span>
            <h2 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">
              Preserving India’s Weaving Legacy
            </h2>
          </div>
          
          <p className="text-xs sm:text-sm text-text-secondary leading-relaxed font-sans font-light">
            For generations, the rhythmic click of the handloom has been the heartbeat of rural India. SareeKart was founded with a singular mission: to bring the purest, most exquisite handcrafted sarees directly from master weavers to connoisseurs of luxury.
          </p>
          
          <div className="border-l-2 border-[#C89B3C] pl-5 italic text-sm sm:text-base text-[#2B0F1E] font-serif leading-relaxed">
            "A single Banarasi or Kanchipuram drape takes anywhere from twenty days to three months of painstaking weaving. It is not just clothing; it is a canvas of heritage."
          </div>
          
          <p className="text-xs sm:text-sm text-text-secondary font-sans leading-relaxed font-light">
            By eliminating intermediaries, we ensure that our weavers receive fair trade wages while you receive authenticated silk mark products straight from artisan clusters in Varanasi, Kanchipuram, Uppada, and Pochampally.
          </p>

          <div className="grid grid-cols-3 gap-6 pt-4 border-t border-[#F4F2EB]">
            <div>
              <p className="text-3xl sm:text-4xl font-serif font-bold text-[#C89B3C]">2500+</p>
              <p className="text-[9px] uppercase font-bold text-text-muted font-sans tracking-widest mt-1">Master Weavers</p>
            </div>
            <div>
              <p className="text-3xl sm:text-4xl font-serif font-bold text-[#C89B3C]">100+</p>
              <p className="text-[9px] uppercase font-bold text-text-muted font-sans tracking-widest mt-1">Villages</p>
            </div>
            <div>
              <p className="text-3xl sm:text-4xl font-serif font-bold text-[#C89B3C]">40+</p>
              <p className="text-[9px] uppercase font-bold text-text-muted font-sans tracking-widest mt-1">Years of Heritage</p>
            </div>
          </div>
        </motion.div>

        {/* Right Column: Artisan Image */}
        <motion.div 
          initial={{ opacity: 0, scale: 0.96 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, delay: 0.1 }}
          className="lg:col-span-5 relative group"
        >
          {/* Decorative frame */}
          <div className="absolute inset-4 border border-[#C89B3C]/20 rounded-2xl transform translate-x-3 translate-y-3 -z-10 pointer-events-none"></div>

          <div className="aspect-[3/4] rounded-2xl overflow-hidden shadow-luxury border border-[#F4F2EB]">
            <img 
              src="https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=800" 
              alt="Master handloom artisan weaving pure silk saree on traditional wooden loom"
              className="w-full h-full object-cover group-hover:scale-102 transition-transform duration-500 object-center"
            />
          </div>
          {/* Overlay Tag */}
          <div className="absolute -bottom-4 -left-4 bg-[#2B0F1E] text-white p-5 rounded-xl shadow-md border border-[#200b16] max-w-xs hidden sm:block">
            <p className="font-serif text-[#C89B3C] text-xs font-bold uppercase tracking-wider">100% Traceable Craft</p>
            <p className="text-[10px] text-white/60 font-sans mt-1 leading-relaxed font-light">Every saree holds the weaver's signature and registration mark.</p>
          </div>
        </motion.div>

      </div>
    </section>
  );
}
