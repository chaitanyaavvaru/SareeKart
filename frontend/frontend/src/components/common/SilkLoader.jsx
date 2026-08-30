import { useEffect } from 'react';
import { motion } from 'framer-motion';

export default function SilkLoader({ onComplete }) {
  useEffect(() => {
    const timer = setTimeout(() => {
      if (onComplete) onComplete();
    }, 2800);
    return () => clearTimeout(timer);
  }, [onComplete]);

  return (
    <motion.div
      initial={{ y: 0 }}
      exit={{ 
        y: '-100%',
        transition: { duration: 1.1, ease: [0.76, 0, 0.24, 1] } 
      }}
      className="fixed inset-0 z-[100] bg-[#2B0F1E] flex flex-col items-center justify-center overflow-hidden"
    >
      {/* Background Micro Weaving Pattern */}
      <div className="absolute inset-0 opacity-10 pointer-events-none bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:16px_16px]"></div>
      
      {/* Golden Thread Weaving Path (SVG) */}
      <div className="relative w-32 h-32 flex items-center justify-center">
        <svg className="w-full h-full text-[#C89B3C]" viewBox="0 0 100 100" fill="none">
          {/* Outer circle thread */}
          <motion.circle
            cx="50"
            cy="50"
            r="40"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            initial={{ pathLength: 0, rotate: 0 }}
            animate={{ 
              pathLength: [0, 1, 1, 0],
              rotate: [0, 180, 360, 540]
            }}
            transition={{ 
              duration: 2.5,
              repeat: Infinity,
              ease: "easeInOut"
            }}
          />
          {/* Inner weave symbol */}
          <motion.path
            d="M30,50 Q50,20 70,50 Q50,80 30,50"
            stroke="currentColor"
            strokeWidth="1"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: [0, 1, 1, 0] }}
            transition={{
              duration: 2.5,
              repeat: Infinity,
              ease: "easeInOut",
              delay: 0.2
            }}
          />
        </svg>
        
        {/* Soft Glowing Particle Center */}
        <div className="absolute w-2 h-2 rounded-full bg-[#C89B3C] shadow-[0_0_15px_#C89B3C] animate-pulse"></div>
      </div>

      {/* Typography Fade-in */}
      <div className="mt-8 text-center space-y-2 z-10">
        <motion.h1
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.4, ease: "easeOut" }}
          className="text-white text-3xl font-serif tracking-[0.25em] uppercase font-bold"
        >
          SareeKart
        </motion.h1>
        
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.6 }}
          transition={{ duration: 1, delay: 0.9 }}
          className="text-[#C89B3C] text-[9px] uppercase tracking-[0.3em] font-sans font-semibold"
        >
          Woven For Generations
        </motion.p>
      </div>

      {/* Soft Ambient Light Glow */}
      <div className="absolute bottom-[-10%] w-[80%] h-[30%] bg-[#C89B3C]/10 rounded-full blur-[120px] pointer-events-none"></div>
    </motion.div>
  );
}
