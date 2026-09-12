import React from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin } from 'lucide-react';
import SafeImage from './SafeImage';

export default function AddressChangeBanner() {
  const navigate = useNavigate();

  return (
    <div className="w-full min-h-[600px] h-[600px] rounded-[28px] overflow-hidden border border-[#E6DFD3] shadow-luxury flex items-center justify-center select-none relative z-10 font-sans group">
      
      {/* ── BACKGROUND: Full-bleed Luxury Photography & Dark Overlay ── */}
      <div className="absolute inset-0 z-0">
        <SafeImage
          src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1200&q=80"
          alt="Palace Corridor Address Vignettes"
          productName="Logistics Fulfillment Hub"
          category="Heritage Logistics"
          aspectRatioClass="h-full"
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-1000 ease-out object-top"
        />
        {/* Dark luxury overlay vignette filter */}
        <div className="absolute inset-0 bg-[#3A0F1F]/80 mix-blend-multiply"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-[#3A0F1F] via-[#3A0F1F]/40 to-[#3A0F1F]/80"></div>
        
        {/* Subtle fabric grid texture */}
        <div className="absolute inset-0 opacity-5 bg-[radial-gradient(#FAF8F5_1px,transparent_1px)] [background-size:20px_20px] pointer-events-none"></div>
      </div>

      {/* ── FOREGROUND: Centered Glassmorphic Card ── */}
      <div className="relative z-10 w-full max-w-[480px] mx-6 p-8 sm:p-10 bg-white/5 backdrop-blur-md border border-white/10 rounded-[24px] shadow-luxury text-center flex flex-col justify-between items-center text-white space-y-6">
        
        <div className="space-y-3">
          <div className="w-12 h-12 rounded-full bg-[#C9A14A]/10 border border-[#C9A14A]/30 flex items-center justify-center mx-auto text-[#C9A14A]">
            <MapPin className="w-5 h-5 stroke-[1.5]" />
          </div>
          
          <h2 className="text-2xl sm:text-3xl font-serif font-bold text-white leading-tight">
            Update Your Delivery Address
          </h2>
          
          <p className="text-xs text-white/70 font-sans font-light leading-relaxed">
            Moving or gifting? Keep your shipping details current to ensure smooth direct-to-weaver packaging delivery across domestic or international hubs.
          </p>
        </div>

        <button
          onClick={() => navigate('/orders')}
          className="px-8 py-3.5 bg-[#C9A14A] hover:bg-[#A37E30] text-[#3A0F1F] text-xs font-bold rounded-xl uppercase tracking-widest transition-all duration-300 cursor-pointer flex items-center gap-2 shadow-lg"
        >
          Manage Address
        </button>

        {/* Footer verification notes */}
        <div className="border-t border-white/10 pt-4 w-full text-[9px] text-white/50 font-sans font-light">
          DHL Express / BlueDart logistics hubs coordinate direct shipping across 120+ international countries.
        </div>

      </div>

    </div>
  );
}
