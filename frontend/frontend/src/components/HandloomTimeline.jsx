
import { motion } from 'framer-motion';
import { Paintbrush, Compass, Sliders, CheckCircle } from 'lucide-react';

const steps = [
  {
    icon: <Paintbrush className="w-4 h-4 text-white" />,
    title: "1. Thread Dyeing & Skeining",
    description: "Pure mulberry silk threads are washed and hand-dyed in boiling wood ash and organic vat dyes to preserve natural texture and color fastness."
  },
  {
    icon: <Sliders className="w-4 h-4 text-white" />,
    title: "2. Warp & Loom Preparation",
    description: "Up to 10,000 individual silk threads are carefully stretched and aligned onto the loom beam. This warping process alone takes 3-5 days."
  },
  {
    icon: <Compass className="w-4 h-4 text-white" />,
    title: "3. Design Card Punching",
    description: "Intricate floral and geometric borders are mapped onto traditional wooden Jacquard cards to guide the needle hooks automatically."
  },
  {
    icon: <CheckCircle className="w-4 h-4 text-white" />,
    title: "4. The Artisan Weave",
    description: "Shuttles cross continuously as the master weaver presses treadles rhythmically, joining border and body with the legendary interlock drape."
  }
];

export default function HandloomTimeline() {
  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 bg-[#F9F6F1] border-t border-[#F4F2EB]">
      <div className="text-center space-y-3 mb-16">
        <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
          The Artistry
        </span>
        <h2 className="text-3xl sm:text-5xl font-serif font-bold text-[#2B0F1E] leading-tight">
          How a Handloom Masterpiece is Born
        </h2>
        <p className="text-xs sm:text-sm text-text-secondary max-w-xl mx-auto font-light leading-relaxed">
          Behind every thread lies days of patience, precision, and generational knowledge.
        </p>
      </div>

      <div className="relative border-l border-[#C89B3C]/30 ml-4 md:ml-0 md:left-1/2 md:-translate-x-1/2 max-w-3xl space-y-12 pb-4">
        {steps.map((step, idx) => {
          const isEven = idx % 2 === 0;
          return (
            <motion.div 
              key={idx}
              initial={{ opacity: 0, y: 25 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.7, delay: idx * 0.15, ease: "easeOut" }}
              className={`relative flex flex-col md:flex-row items-start md:items-center ${
                isEven ? 'md:flex-row-reverse' : ''
              }`}
            >
              {/* Central Node Indicator */}
              <div className="absolute -left-[17px] md:left-1/2 md:-translate-x-1/2 w-8 h-8 rounded-full bg-[#2B0F1E] border-2 border-[#C89B3C] flex items-center justify-center shadow-sm z-10">
                {step.icon}
              </div>

              {/* Story Content Block */}
              <div className={`w-full md:w-1/2 pl-8 md:pl-0 ${
                isEven ? 'md:pr-12 md:text-right' : 'md:pl-12 md:text-left'
              }`}>
                <div className="bg-white border border-[#F4F2EB] rounded-2xl p-6 shadow-xs hover:shadow-soft transition-shadow duration-300">
                  <h3 className="font-serif font-bold text-md text-[#2B0F1E] mb-2">{step.title}</h3>
                  <p className="text-xs text-text-secondary font-sans leading-relaxed font-light">{step.description}</p>
                </div>
              </div>

              {/* Spacer for 2-column layout */}
              <div className="hidden md:block w-1/2"></div>
            </motion.div>
          );
        })}
      </div>
    </section>
  );
}
