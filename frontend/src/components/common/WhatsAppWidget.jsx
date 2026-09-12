import React, { useState } from 'react';
import { MessageCircle, X, ExternalLink, Send, ShieldCheck, Truck } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export default function WhatsAppWidget({ productName = '', sku = '' }) {
  const [isOpen, setIsOpen] = useState(false);
  const [customMsg, setCustomMsg] = useState(
    productName
      ? `Namaste SareeKart! I have a question about the ${productName} (SKU: ${sku || 'SK-DEFAULT'}).`
      : `Namaste SareeKart! I would like help choosing an authentic handwoven saree.`
  );

  const whatsappNumber = "919059564499";

  const handleLaunchWhatsApp = () => {
    const encodedText = encodeURIComponent(customMsg);
    const whatsappUrl = `https://wa.me/${whatsappNumber}?text=${encodedText}`;
    window.open(whatsappUrl, '_blank', 'noopener,noreferrer');
    setIsOpen(false);
  };

  return (
    <>
      <motion.button
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.92 }}
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 left-6 z-50 bg-[#0F766E] text-white p-3.5 rounded-full shadow-2xl border border-white/70 flex items-center gap-2.5 cursor-pointer hover:bg-[#0B615B] transition-all group"
        aria-label="Open WhatsApp Concierge"
      >
        <MessageCircle className="w-5 h-5 text-white fill-white stroke-[1.5]" />
        <span className="text-xs font-serif font-bold uppercase tracking-wider hidden sm:inline text-white">
          WhatsApp Us
        </span>
      </motion.button>

      {/* WhatsApp Modal Window */}
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-start sm:p-6 pointer-events-auto text-left">
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="absolute inset-0 bg-black/50 backdrop-blur-xs"
            />

            {/* Modal Container */}
            <motion.div
              initial={{ y: 40, opacity: 0, scale: 0.95 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: 40, opacity: 0, scale: 0.95 }}
              transition={{ type: "spring", damping: 25, stiffness: 300 }}
              className="relative w-full sm:w-[380px] bg-[#F5F7FA] sm:rounded-[8px] border border-[#DDE4EA] shadow-2xl flex flex-col overflow-hidden text-[#111827] z-10"
            >
              {/* WhatsApp Header */}
              <div className="px-5 py-4 bg-[#111827] text-white flex items-center justify-between shrink-0">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-white border border-white/30">
                    <MessageCircle className="w-5 h-5 fill-white" />
                  </div>
                  <div>
                    <h3 className="text-sm font-black tracking-wide text-white leading-none">
                      SareeKart WhatsApp help
                    </h3>
                    <p className="text-[10px] text-emerald-200 mt-1 flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                      Typically replies in 5 minutes
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-1 rounded-full hover:bg-white/10 text-white/80 hover:text-white transition-colors cursor-pointer"
                  aria-label="Close Modal"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Body */}
              <div className="p-5 space-y-4 font-sans text-xs">
                <div className="p-3.5 bg-[#E7F5F3] text-[#0F766E] rounded-[8px] text-xs leading-relaxed border border-[#BFE7E2] shadow-xs font-semibold">
                  Chat with us for color matching, fabric advice, blouse options, or live order tracking.
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-[#111827] uppercase tracking-wider block">
                    Your Message Preview
                  </label>
                  <textarea
                    rows={3}
                    value={customMsg}
                    onChange={(e) => setCustomMsg(e.target.value)}
                    className="w-full bg-white border border-[#DDE4EA] focus:border-[#0F766E] rounded-[8px] p-3 text-xs text-[#111827] outline-none font-sans resize-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2 text-[10px] text-[#64748B] pt-1 font-bold">
                  <div className="flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-[#25D366]" /> 100% Verified Business
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Truck className="w-3.5 h-3.5 text-[#25D366]" /> Live Order Tracking
                  </div>
                </div>
              </div>

              {/* Footer CTA */}
              <div className="p-4 bg-white border-t border-[#DDE4EA] shrink-0">
                <button
                  onClick={handleLaunchWhatsApp}
                  className="w-full h-12 bg-[#0F766E] hover:bg-[#0B615B] text-white font-black text-xs rounded-full shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Send className="w-4 h-4" /> Start WhatsApp Chat
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
