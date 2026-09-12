import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, ChevronRight, Maximize2, ZoomIn, ZoomOut, X, Sparkles } from 'lucide-react';

export default function MobileProductGallery({ images = [], productName = 'Handloom Saree', category = 'SareeKart Edit' }) {
  const [activeIdx, setActiveIdx] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1);
  const touchStartX = useRef(0);
  const touchEndX = useRef(0);

  const displayImages = images.length > 0 ? images : ['/placeholder.jpg'];
  const total = displayImages.length;

  const nextImage = () => {
    setActiveIdx((prev) => (prev + 1) % total);
  };

  const prevImage = () => {
    setActiveIdx((prev) => (prev - 1 + total) % total);
  };

  // Touch Swipe Handlers for smooth mobile gestures
  const handleTouchStart = (e) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchMove = (e) => {
    touchEndX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = () => {
    const diff = touchStartX.current - touchEndX.current;
    const minSwipeDistance = 45;
    if (diff > minSwipeDistance) {
      // Swiped Left -> Next
      nextImage();
    } else if (diff < -minSwipeDistance) {
      // Swiped Right -> Prev
      prevImage();
    }
  };

  const toggleZoom = () => {
    setZoomLevel((prev) => (prev === 1 ? 2.2 : 1));
  };

  return (
    <div className="grid gap-4 lg:grid-cols-[96px_1fr]">
      {/* Desktop Vertical Thumbnails / Mobile Horizontal Thumbnails */}
      <div className="order-2 flex gap-2.5 overflow-x-auto no-scrollbar lg:order-1 lg:flex-col" role="tablist" aria-label="Product thumbnails">
        {displayImages.map((img, idx) => (
          <button
            key={`${img}-${idx}`}
            onClick={() => setActiveIdx(idx)}
            role="tab"
            aria-selected={activeIdx === idx}
            aria-label={`View image ${idx + 1} of ${total}`}
            className={`relative h-20 w-16 shrink-0 overflow-hidden rounded-md border bg-white transition-all lg:h-24 lg:w-full ${
              activeIdx === idx
                ? 'border-[#C8A04D] ring-2 ring-[#C8A04D]/40 scale-102'
                : 'border-[#DDD8CF] opacity-75 hover:opacity-100'
            }`}
          >
            <img src={img} alt={`${productName} thumbnail ${idx + 1}`} className="h-full w-full object-cover object-top" />
          </button>
        ))}
      </div>

      {/* Main Touch & Swipe Carousel Container */}
      <div className="order-1 overflow-hidden rounded-lg bg-white shadow-soft lg:order-2">
        <div
          className="relative aspect-[4/5] max-h-[820px] select-none touch-pan-y"
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          {/* Active Saree Image with AnimatePresence */}
          <AnimatePresence mode="wait">
            <motion.img
              key={activeIdx}
              src={displayImages[activeIdx]}
              alt={`${productName} view ${activeIdx + 1}`}
              initial={{ opacity: 0.8, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0.8, x: -20 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
              className="h-full w-full object-cover object-top cursor-zoom-in"
              onClick={() => setIsLightboxOpen(true)}
            />
          </AnimatePresence>

          {/* Floating Category Tag */}
          <div className="absolute left-3.5 top-3.5 rounded-full bg-white/95 px-3.5 py-1.5 text-[11px] font-black uppercase tracking-wider text-[#3A0F1F] shadow-xs backdrop-blur-sm">
            {category}
          </div>

          {/* Floating Image Counter Badge (e.g. 1 / 4) */}
          <div className="absolute right-3.5 top-3.5 rounded-full bg-black/60 px-3 py-1 text-xs font-bold text-white backdrop-blur-sm">
            {activeIdx + 1} / {total}
          </div>

          {/* Tap-to-Zoom Action Button */}
          <button
            onClick={() => setIsLightboxOpen(true)}
            aria-label="Open high-resolution zari inspection lightbox"
            className="absolute right-3.5 bottom-3.5 flex h-10 w-10 items-center justify-center rounded-full bg-white/90 text-[#3A0F1F] shadow-md backdrop-blur-sm transition hover:bg-white hover:scale-105 active:scale-95"
          >
            <Maximize2 className="h-4 w-4" />
          </button>

          {/* Desktop Left/Right Navigation Arrows */}
          {total > 1 && (
            <>
              <button
                onClick={prevImage}
                aria-label="Previous image"
                className="hidden lg:flex absolute left-3 top-1/2 -translate-y-1/2 h-10 w-10 items-center justify-center rounded-full bg-white/85 text-[#17211F] shadow-md transition hover:bg-white hover:scale-105 active:scale-95"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <button
                onClick={nextImage}
                aria-label="Next image"
                className="hidden lg:flex absolute right-3 top-1/2 -translate-y-1/2 h-10 w-10 items-center justify-center rounded-full bg-white/85 text-[#17211F] shadow-md transition hover:bg-white hover:scale-105 active:scale-95"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </>
          )}

          {/* Mobile Dot Indicators */}
          {total > 1 && (
            <div className="lg:hidden absolute bottom-3.5 left-1/2 -translate-x-1/2 flex items-center gap-1.5 bg-black/35 px-3 py-1.5 rounded-full backdrop-blur-xs">
              {displayImages.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setActiveIdx(idx)}
                  aria-label={`Jump to image ${idx + 1}`}
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    activeIdx === idx ? 'w-5 bg-[#C8A04D]' : 'w-1.5 bg-white/60'
                  }`}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Full-Screen Zari Inspection Lightbox Modal */}
      <AnimatePresence>
        {isLightboxOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            role="dialog"
            aria-modal="true"
            aria-label="High-resolution zari inspection lightbox"
            className="fixed inset-0 z-50 flex flex-col bg-black/95 backdrop-blur-md"
          >
            {/* Lightbox Header Bar */}
            <header className="flex items-center justify-between px-5 py-4 text-white border-b border-white/15">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-[#C8A04D]" />
                <span className="text-xs font-bold uppercase tracking-wider text-[#E6DFD3]">
                  Zari & Weave Inspection ({activeIdx + 1} of {total})
                </span>
              </div>
              <div className="flex items-center gap-3">
                <button
                  onClick={toggleZoom}
                  aria-label={zoomLevel === 1 ? '2.2x Zoom' : 'Reset zoom'}
                  className="flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-white/25"
                >
                  {zoomLevel === 1 ? <ZoomIn className="h-4 w-4" /> : <ZoomOut className="h-4 w-4" />}
                  <span>{zoomLevel === 1 ? '2.2x Zoom' : 'Reset'}</span>
                </button>
                <button
                  onClick={() => {
                    setIsLightboxOpen(false);
                    setZoomLevel(1);
                  }}
                  aria-label="Close inspection lightbox"
                  className="flex h-9 w-9 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/25 active:scale-95"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
            </header>

            {/* Lightbox Body with Zoom & Pan */}
            <div className="relative flex flex-1 items-center justify-center overflow-hidden p-4">
              <motion.img
                src={displayImages[activeIdx]}
                alt={`${productName} zoomed inspection`}
                animate={{ scale: zoomLevel }}
                transition={{ type: 'spring', stiffness: 280, damping: 25 }}
                className={`max-h-[82vh] max-w-full object-contain cursor-grab active:cursor-grabbing transition-transform ${
                  zoomLevel > 1 ? 'cursor-move' : ''
                }`}
                onDoubleClick={toggleZoom}
              />

              {/* Lightbox Left/Right Navigation */}
              {total > 1 && (
                <>
                  <button
                    onClick={prevImage}
                    aria-label="Previous photo in lightbox"
                    className="absolute left-4 top-1/2 -translate-y-1/2 flex h-12 w-12 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/30"
                  >
                    <ChevronLeft className="h-6 w-6" />
                  </button>
                  <button
                    onClick={nextImage}
                    aria-label="Next photo in lightbox"
                    className="absolute right-4 top-1/2 -translate-y-1/2 flex h-12 w-12 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/30"
                  >
                    <ChevronRight className="h-6 w-6" />
                  </button>
                </>
              )}
            </div>

            {/* Lightbox Footer Guidance */}
            <footer className="py-3 text-center text-xs text-[#A8A29E] border-t border-white/10">
              Double-tap or tap &quot;2.2x Zoom&quot; to inspect handloom zari threads. Swipe or tap arrows to browse.
            </footer>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
