import React, { useState } from 'react';
import { ShoppingBag } from 'lucide-react';

export default function SafeImage({ 
  src, 
  alt = 'Luxury handwoven saree', 
  className = '', 
  fallbackSrc = 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80',
  productName = '', 
  category = 'Heritage Handloom'
}) {
  const [hasError, setHasError] = useState(false);
  const [imgSrc, setImgSrc] = useState(src || fallbackSrc);

  const handleError = () => {
    if (imgSrc !== fallbackSrc) {
      setImgSrc(fallbackSrc);
    } else {
      setHasError(true);
    }
  };

  if (hasError || !imgSrc) {
    return (
      <div className="w-full h-full min-h-[200px] bg-[#F5F7FA] border border-[#DDE4EA] flex flex-col items-center justify-center p-6 text-center rounded-[8px] select-none">
        <div className="w-10 h-10 rounded-full bg-white border border-[#DDE4EA] flex items-center justify-center text-[#E85D4F] mb-3 shadow-xs">
          <ShoppingBag className="w-4 h-4 stroke-[1.5]" />
        </div>
        <span className="text-[10px] uppercase tracking-[0.16em] font-black text-[#0F766E] block mb-1">
          {category}
        </span>
        {productName && (
          <h4 className="font-bold text-sm text-[#111827] truncate max-w-full">
            {productName}
          </h4>
        )}
      </div>
    );
  }

  return (
    <img
      src={imgSrc}
      alt={alt}
      onError={handleError}
      loading="lazy"
      decoding="async"
      className={`w-full h-full object-cover transition-opacity duration-300 ${className}`}
    />
  );
}
