import React from 'react';
import { ShieldCheck, Truck, Star } from 'lucide-react';
import { motion } from 'framer-motion';

const features = [
  {
    icon: <ShieldCheck className="w-5 h-5 text-[#C5A059]" />,
    title: "100% Authentic Handloom",
    description: "Certified silk mark products straight from artisan looms."
  },
  {
    icon: <Truck className="w-5 h-5 text-[#C5A059]" />,
    title: "Free Insured Shipping",
    description: "Complimentary delivery on orders exceeding ₹5,000 across India."
  },
  {
    icon: <Star className="w-5 h-5 text-[#C5A059]" />,
    title: "Artisan Empowerment",
    description: "Direct patronage and fair trade wages for our master weavers."
  }
];

export default function FeatureBar() {
  return (
    <section className="max-w-[1640px] mx-auto px-6 sm:px-12 md:px-16 py-8">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 bg-white border border-[#E5E5E5] p-6 sm:p-8 rounded-2xl shadow-xs">
        {features.map((item, idx) => (
          <div 
            key={idx}
            className="flex gap-4 items-start p-4 rounded-xl hover:bg-[#FAF9F6] transition-all duration-200 text-left"
          >
            <div className="p-3 bg-[#FAF9F6] rounded-xl text-[#1A1A1A] shrink-0 border border-[#E5E5E5]">
              {item.icon}
            </div>
            <div className="space-y-1">
              <h4 className="font-bold text-[#1A1A1A] text-sm sm:text-base font-serif tracking-wide">{item.title}</h4>
              <p className="text-xs sm:text-sm text-[#666666] leading-relaxed font-sans">{item.description}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
