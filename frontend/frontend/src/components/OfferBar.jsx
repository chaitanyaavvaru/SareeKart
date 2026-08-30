
import { useState } from 'react';
import { X } from 'lucide-react';

export default function OfferBar() {
  const [isVisible, setIsVisible] = useState(true);

  if (!isVisible) return null;

  return (
    <div className="hidden sm:block bg-[#F9F6F0] text-[#2C0F1F] py-2.5 border-b border-border overflow-hidden select-none relative pr-10">
      <div className="w-full px-8 flex justify-between items-center text-xs font-bold uppercase tracking-widest">
        <div className="w-full relative flex overflow-x-hidden">
          <div className="animate-marquee whitespace-nowrap flex gap-12">
            <span>✨ Flat 20% OFF on Wedding Sarees ✨</span>
            <span>🚚 Free Shipping across India on orders above ₹5,000 🚚</span>
            <span>✨ Handcrafted by Artisans directly from looms ✨</span>
          </div>

          <div className="absolute top-0 animate-marquee2 whitespace-nowrap flex gap-12">
            <span>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; ✨ Flat 20% OFF on Wedding Sarees ✨</span>
            <span>🚚 Free Shipping across India on orders above ₹5,000 🚚</span>
            <span>✨ Handcrafted by Artisans directly from looms ✨</span>
          </div>
        </div>
      </div>

      <button 
        onClick={() => setIsVisible(false)}
        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:text-[#C9A227] transition-colors cursor-pointer"
        aria-label="Close Announcement"
      >
        <X className="w-3.5 h-3.5" />
      </button>
      
      {/* Styles for Marquee */}
      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes marquee {
          0% { transform: translateX(0%); }
          100% { transform: translateX(-100%); }
        }
        @keyframes marquee2 {
          0% { transform: translateX(100%); }
          100% { transform: translateX(0%); }
        }
        .animate-marquee {
          animation: marquee 25s linear infinite;
        }
        .animate-marquee2 {
          animation: marquee2 25s linear infinite;
        }
      `}} />
    </div>
  );
}
