import React from 'react';

export default function OfferBar() {
  const items = [
    "FREE SHIPPING ABOVE ₹1999",
    "COD AVAILABLE",
    "EASY RETURNS",
    "HANDWOVEN SINCE 1950"
  ];

  return (
    <div className="bg-[#3A0F1F] text-[#C8A04D] h-[40px] flex items-center justify-center border-b border-[#C8A04D]/35 select-none w-full text-[11px] font-bold uppercase tracking-[0.22em] font-sans relative z-50">
      <div className="max-w-[1640px] mx-auto px-[80px] flex items-center justify-center gap-[32px] w-full">
        {items.map((item, idx) => (
          <React.Fragment key={item}>
            <span className="hover:text-white transition-colors duration-300">{item}</span>
            {idx < items.length - 1 && (
              <span className="w-px h-[12px] bg-[#C8A04D]/30 block shrink-0" />
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
