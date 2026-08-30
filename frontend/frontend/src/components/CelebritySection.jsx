
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import SafeImage from './common/SafeImage';

export default function CelebritySection() {
  const navigate = useNavigate();

  return (
    <section className="w-full min-h-screen bg-[#2B0F1E] text-white flex items-center justify-center py-32 relative overflow-hidden border-t border-white/5">
      {/* Background silk overlays */}
      <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:24px_24px] pointer-events-none"></div>

      <div className="max-w-[1480px] mx-auto px-10 sm:px-20 w-full grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
        
        {/* Left: Giant Hero Image (5 columns) */}
        <div className="lg:col-span-5 w-full">
          <div className="relative h-[550px] lg:h-[650px] rounded-[28px] overflow-hidden border border-[#C89B3C]/30 shadow-luxury bg-white/5">
            <SafeImage 
              src="https://images.unsplash.com/photo-1618336753974-aae8e04506aa?w=800" 
              alt="Celebrity choice flagship drape" 
              productName="Flagship Zari Drape"
              category="Celebrity Pick"
              aspectRatioClass="aspect-[3/4]"
              className="w-full h-full object-cover object-top hover:scale-103 transition-transform duration-1000"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent"></div>
          </div>
        </div>

        {/* Center: Editorial Content & Typography (4 columns) */}
        <div className="lg:col-span-4 space-y-8 text-left lg:px-4">
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <span className="h-px w-6 bg-[#C89B3C]"></span>
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase">Red Carpet Spotlight</span>
            </div>
            <h2 className="text-4xl sm:text-6xl font-serif font-bold text-white leading-tight">
              Celebrity Picks
            </h2>
            <p className="text-xs uppercase font-bold text-[#C89B3C]/80 tracking-[0.2em] font-sans">
              Styled by leading tastemakers
            </p>
          </div>

          <p className="text-xs sm:text-sm text-white/70 leading-relaxed font-sans font-light">
            The screen's most coveted sarees, hand-selected and styled for iconic award galas and premium lifestyle editorials. These weaves represent the high point of regional handloom complexity combined with contemporary vision.
          </p>

          <div className="pt-4">
            <button 
              onClick={() => navigate('/products')}
              className="px-8 py-4 bg-[#C89B3C] hover:bg-[#A37E30] text-[#2B0F1E] font-bold rounded-xl text-xs uppercase tracking-widest transition-all duration-300 flex items-center gap-2 cursor-pointer shadow-lg"
            >
              Explore Styles <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Right: Three Supporting Images Stack (3 columns) */}
        <div className="lg:col-span-3 space-y-6 flex flex-col justify-center">
          <div className="h-[180px] rounded-2xl overflow-hidden border border-white/10 shadow bg-white/5">
            <SafeImage 
              src="https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=500" 
              alt="Details view" 
              productName="Brocade Closeup"
              category="Detail"
              aspectRatioClass="aspect-[16/9]"
              className="w-full h-full object-cover object-top"
            />
          </div>
          <div className="h-[180px] rounded-2xl overflow-hidden border border-white/10 shadow bg-white/5">
            <SafeImage 
              src="https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=500" 
              alt="Saree border closeup" 
              productName="Border Detail"
              category="Detail"
              aspectRatioClass="aspect-[16/9]"
              className="w-full h-full object-cover object-top"
            />
          </div>
          <div className="h-[180px] rounded-2xl overflow-hidden border border-white/10 shadow bg-white/5">
            <SafeImage 
              src="https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=500" 
              alt="Model posing lifestyle" 
              productName="Lifestyle Portrait"
              category="Detail"
              aspectRatioClass="aspect-[16/9]"
              className="w-full h-full object-cover object-top"
            />
          </div>
        </div>

      </div>
    </section>
  );
}
