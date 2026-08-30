import { useState, useEffect } from 'react';
import { ShoppingBag, Loader2 } from 'lucide-react';

export default function SafeImage({ 
  src, 
  alt = 'Luxury handwoven saree', 
  className = '', 
  fallbackSrc = 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=600', // Premium editorial red bridal fallback
  productName = '', 
  category = 'Heritage Handloom', 
  aspectRatioClass = 'aspect-[3/4]'
}) {
  const [status, setStatus] = useState('loading'); // 'loading' | 'loaded' | 'error'
  const [retryCount, setRetryCount] = useState(0);
  const [currentSrc, setCurrentSrc] = useState('');

  useEffect(() => {
    // Check if src is valid before attempting load
    const updateState = () => {
      if (!src || typeof src !== 'string' || src.trim() === '' || src === 'null' || src === 'undefined') {
        setStatus('error');
      } else {
        setStatus('loading');
        setCurrentSrc(src);
      }
    };
    updateState();
  }, [src]);

  const handleLoad = () => {
    setStatus('loaded');
  };

  const handleError = () => {
    if (retryCount === 0) {
      // Retry once by appending cache-buster query parameter to force reload
      setRetryCount(1);
      const separator = currentSrc.includes('?') ? '&' : '?';
      setCurrentSrc(`${currentSrc}${separator}retry=1`);
    } else if (currentSrc !== fallbackSrc && fallbackSrc) {
      // Fallback to primary editorial image
      setCurrentSrc(fallbackSrc);
    } else {
      // Ultimate failure, render luxury placeholder card
      setStatus('error');
    }
  };

  return (
    <div className={`relative w-full ${aspectRatioClass} overflow-hidden rounded-2xl`}>
      {/* 1. Loading Skeleton Overlay */}
      {status === 'loading' && (
        <div className="absolute inset-0 bg-[#F9F6F1] flex items-center justify-center animate-pulse z-10 border border-[#E6DFD3]">
          <Loader2 className="w-6 h-6 text-[#C89B3C] animate-spin" />
        </div>
      )}

      {/* 2. Main Image Element */}
      {status !== 'error' && currentSrc && (
        <img
          src={currentSrc}
          alt={alt}
          onLoad={handleLoad}
          onError={handleError}
          loading="lazy"
          decoding="async"
          className={`${className} w-full h-full object-cover transition-all duration-700 ${
            status === 'loaded' ? 'opacity-100' : 'opacity-0'
          }`}
        />
      )}

      {/* 3. Luxury Placeholder Component (Success recover state) */}
      {status === 'error' && (
        <div className="absolute inset-0 bg-[#F9F6F1] border border-[#C89B3C]/30 flex flex-col justify-between p-6 text-center select-none z-10">
          {/* Subtle gold pattern grid overlay */}
          <div className="absolute inset-0 opacity-5 pointer-events-none bg-[radial-gradient(#C89B3C_1px,transparent_1px)] [background-size:12px_12px]"></div>

          <div className="flex flex-col items-center justify-center flex-grow space-y-3 z-10">
            <div className="w-10 h-10 rounded-full bg-[#2B0F1E]/5 border border-[#C89B3C]/20 flex items-center justify-center text-[#C89B3C]">
              <ShoppingBag className="w-4 h-4 stroke-[1.5]" />
            </div>
            
            <div className="space-y-1">
              <span className="text-[8px] uppercase tracking-[0.2em] font-bold text-[#C89B3C] block">
                {category}
              </span>
              <p className="text-[10px] uppercase tracking-[0.15em] font-semibold text-white/90 bg-[#2B0F1E] px-2 py-1 rounded-md inline-block">
                Image Coming Soon
              </p>
            </div>
          </div>

          {/* Product details block */}
          {productName && (
            <div className="border-t border-[#E6DFD3] pt-4 z-10">
              <h4 className="font-serif italic font-medium text-xs text-[#2B0F1E] truncate max-w-full">
                {productName}
              </h4>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
