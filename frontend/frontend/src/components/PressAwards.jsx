
import { motion } from 'framer-motion';

const press = [
  { logo: "VOGUE", quote: "A breathtaking preservation of weavers clusters.", issue: "September Issue" },
  { logo: "THE HINDU", quote: "Bridging the gap between looms and luxury patrons.", issue: "Weekend Edit" },
  { logo: "FORBES", quote: "The brand redefining fair-trade luxury textile globally.", issue: "Retail Spotlight" }
];

export default function PressAwards() {
  return (
    <section className="bg-white text-[#2B0F1E] py-24 border-t border-[#F4F2EB]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-start">
          
          {/* Left Column: Accolades */}
          <div className="lg:col-span-5 space-y-8 text-left">
            <div className="space-y-3">
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
                Recognition
              </span>
              <h2 className="text-3xl sm:text-4xl font-serif font-bold leading-tight">
                Global Press <br />& Guild Honors
              </h2>
            </div>
            
            <p className="text-xs text-text-secondary leading-relaxed font-sans font-light">
              Our quest to authenticate and elevate traditional Indian weaves has been honored by international media and national handloom committees.
            </p>

            <div className="border-t border-[#F4F2EB] pt-6 space-y-4">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold font-sans tracking-wide">National Craft Excellence Award</span>
                <span className="text-[#C89B3C] font-semibold">Winner 2025</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold font-sans tracking-wide">Fair Trade Handloom Guild</span>
                <span className="text-[#C89B3C] font-semibold">Certified Patron</span>
              </div>
            </div>
          </div>

          {/* Right Column: Press Quotes Grid */}
          <div className="lg:col-span-7 grid grid-cols-1 gap-6">
            {press.map((item, idx) => (
              <motion.div 
                key={idx}
                initial={{ opacity: 0, y: 15 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: idx * 0.1 }}
                className="bg-[#F9F6F1] border border-[#F4F2EB] p-8 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 hover:shadow-soft transition-all duration-300"
              >
                <div className="space-y-2 text-left">
                  <span className="text-xs font-bold text-[#C89B3C] tracking-[0.25em] uppercase block font-serif">
                    {item.logo}
                  </span>
                  <p className="text-sm font-serif italic text-[#2B0F1E] font-medium">
                    "{item.quote}"
                  </p>
                </div>
                <span className="text-[10px] text-text-muted shrink-0 uppercase tracking-widest font-sans font-semibold">
                  {item.issue}
                </span>
              </motion.div>
            ))}
          </div>

        </div>

      </div>
    </section>
  );
}
