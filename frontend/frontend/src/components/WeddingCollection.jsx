
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

export default function WeddingCollection() {
  const navigate = useNavigate();

  const handleShopBridal = () => {
    navigate('/products?category=Kanchipuram%20Silk');
  };

  return (
    <section className="relative w-full overflow-hidden bg-[#2B0F1E] py-24 sm:py-32">
      {/* Background Graphic & Details */}
      <div className="absolute inset-0 opacity-10 bg-cover bg-center mix-blend-overlay pointer-events-none" style={{ backgroundImage: "url('https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=1000')" }}></div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
          
          {/* Column 1: Image Frame */}
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            className="lg:col-span-5 relative"
          >
            {/* Elegant Background Gold Border Frame */}
            <div className="absolute inset-4 border border-[#C89B3C]/30 rounded-3xl transform translate-x-3 translate-y-3 z-0 pointer-events-none"></div>

            <div className="aspect-[3/4] rounded-3xl overflow-hidden border border-white/10 shadow-2xl relative z-10 bg-[#FAF8F5]">
              <img 
                src="https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=800" 
                alt="Cinematic traditional Indian wedding scene Muhurtham ritual"
                className="w-full h-full object-cover object-top hover:scale-103 transition-transform duration-[1.2s] ease-out"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#2B0F1E]/50 via-transparent to-transparent"></div>
            </div>
          </motion.div>

          {/* Column 2: Details */}
          <motion.div 
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.15, ease: "easeOut" }}
            className="lg:col-span-7 space-y-6 text-white text-center lg:text-left"
          >
            <div className="flex items-center justify-center lg:justify-start gap-2">
              <span className="h-px w-6 bg-[#C89B3C]"></span>
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">
                The Bridal Saga
              </span>
            </div>
            
            <h2 className="text-3xl sm:text-6xl font-serif font-bold text-white leading-tight">
              The Golden Muhurtham
            </h2>
            <p className="text-xs sm:text-sm text-white/70 leading-relaxed font-sans font-light max-w-xl mx-auto lg:mx-0">
              Immerse yourself in the opulence of double-warp Kanchipuram tissue silks and Royal Banarasi zari brocades. Designed as timeless heirlooms to mark your sacred moments.
            </p>
            
            <div className="pt-4 flex justify-center lg:justify-start">
              <button 
                onClick={handleShopBridal}
                className="px-8 py-4 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl text-xs tracking-[0.15em] uppercase transition-colors shadow-lg cursor-pointer"
              >
                Explore Bridal Collection
              </button>
            </div>
          </motion.div>

        </div>
      </div>
    </section>
  );
}
