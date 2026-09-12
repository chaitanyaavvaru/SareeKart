import React, { useState } from 'react';
import { Sparkles, CheckCircle2, ArrowRight, ShieldCheck, HeartHandshake, Calendar } from 'lucide-react';
import SafeImage from './common/SafeImage';

export default function Newsletter() {
  const [email, setEmail] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (email) {
      setSuccess(true);
      setEmail('');
    }
  };

  const benefits = [
    {
      title: "Early Access",
      desc: "Receive private lookbook access invitations 48 hours before new drops go public.",
      icon: Sparkles
    },
    {
      title: "Private Launches",
      desc: "Order custom weaves featuring bespoke metallic threads and double-warp details.",
      icon: ShieldCheck
    },
    {
      title: "Artisan Stories",
      desc: "Receive direct loom updates, weaver logs, and heritage documentary diaries.",
      icon: HeartHandshake
    },
    {
      title: "Styling Sessions",
      desc: "Complimentary styling consults and wedding theme mapping with our design staff.",
      icon: Calendar
    }
  ];

  return (
    <div className="w-full min-h-[640px] bg-[#3A0F1F] rounded-[32px] overflow-hidden border border-[#E6DFD3] shadow-luxury flex items-center justify-center select-none relative font-sans p-[48px] md:p-[64px]">
      
      {/* Background weaving image with dark luxury overlay */}
      <div className="absolute inset-0 z-0 opacity-40">
        <SafeImage
          src="https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&fm=webp&w=1600&q=80"
          alt="Master artisan weaving loom process"
          productName="Weaving Loom"
          category="Heritage Weft"
          aspectRatioClass="h-full"
          className="w-full h-full object-cover object-center"
        />
        <div className="absolute inset-0 bg-[#2C0F1F]/90 mix-blend-multiply"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-[#2C0F1F] via-[#3A0F1F]/75 to-[#2C0F1F]/70"></div>
      </div>

      {/* Main Content Area (Centred, Editorial alignment, high density layout) */}
      <div className="relative z-10 w-full max-w-5xl flex flex-col justify-center items-center text-center text-white space-y-[40px]">
        
        <div className="space-y-[16px] max-w-3xl">
          <span className="text-[#C8A04D] font-serif text-[14px] font-bold tracking-[0.25em] uppercase flex items-center justify-center gap-2 leading-none">
            <Sparkles className="w-3.5 h-3.5 text-[#C8A04D]" /> Weaver Guild Registry
          </span>
          <h2 className="text-[48px] sm:text-[64px] font-serif font-bold text-white leading-tight">
            Be Part of Our Heritage Circle
          </h2>
          <p className="text-[18px] text-white/90 font-normal leading-relaxed max-w-xl mx-auto">
            Support traditional master weavers. Register your email to receive craft updates, artisan diaries, and private collection arrivals.
          </p>
          <p className="text-[13px] text-white/60 font-light max-w-md mx-auto">
            By joining the guild registry, you directly support handloom weaver households while enjoying direct access to highly limited loom drops.
          </p>
        </div>

        {/* Input Form container */}
        <div className="w-full max-w-xl">
          {success ? (
            <div className="p-[20px] bg-white/10 border border-[#C8A04D]/40 rounded-2xl flex items-center justify-center gap-[12px] shadow-sm backdrop-blur-md">
              <CheckCircle2 className="w-5 h-5 text-[#C8A04D] shrink-0" />
              <p className="text-[15px] font-semibold text-[#C8A04D]">You have registered in the SareeKart Guild Registry.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-[16px] w-full">
              <input
                type="email"
                placeholder="Enter email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="flex-grow px-[24px] py-[16px] bg-white/95 border border-[#E6DFD3] focus:border-[#C8A04D] rounded-xl text-[14px] font-sans text-gray-800 focus:outline-none placeholder-gray-400 shadow-inner"
              />
              <button
                type="submit"
                className="px-[32px] py-[16px] bg-gradient-to-r from-[#C8A04D] to-[#B8860B] hover:from-[#B8860B] hover:to-[#C8A04D] text-[#3A0F1F] font-bold rounded-xl text-[15px] uppercase tracking-widest transition-all duration-300 shadow-md hover:shadow-lg flex items-center justify-center gap-[8px] cursor-pointer"
              >
                Join Now <ArrowRight className="w-4 h-4 shrink-0" />
              </button>
            </form>
          )}
        </div>

        {/* 4 Premium Benefit Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-[24px] border-t border-white/15 pt-[40px] w-full text-left">
          {benefits.map((b) => {
            const Icon = b.icon;
            return (
              <div key={b.title} className="bg-white/5 border border-white/10 rounded-2xl p-[24px] space-y-[12px] hover:bg-white/10 hover:border-[#C8A04D]/40 transition-all duration-300">
                <div className="w-10 h-10 rounded-xl bg-[#C8A04D]/15 flex items-center justify-center border border-[#C8A04D]/30">
                  <Icon className="w-5 h-5 text-[#C8A04D] shrink-0" />
                </div>
                <h4 className="text-[17px] font-bold font-serif text-[#C8A04D] leading-none pt-[4px]">
                  {b.title}
                </h4>
                <p className="text-[13px] text-white/80 font-sans font-light leading-relaxed">
                  {b.desc}
                </p>
              </div>
            );
          })}
        </div>

      </div>

    </div>
  );
}
