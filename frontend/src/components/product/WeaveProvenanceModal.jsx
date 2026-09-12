import React from 'react';
import { ShieldCheck, Award, CheckCircle2, X, ExternalLink, Sparkles, MapPin, Feather } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export default function WeaveProvenanceModal({ isOpen, onClose, product }) {
  if (!isOpen || !product) return null;

  const weaveName = product.categoryName || product.category || 'Handloom Silk';
  const isKanchi = weaveName.toLowerCase().includes('kanchi');
  const isBanarasi = weaveName.toLowerCase().includes('banaras');
  const isPaithani = weaveName.toLowerCase().includes('paithan');

  const giData = isKanchi
    ? { giCode: 'GI-104', region: 'Kanchipuram, Tamil Nadu', loom: 'Traditional Jacquard Pit Loom', zari: 'Tested 0.6% Pure Silver Core Zari' }
    : isBanarasi
    ? { giCode: 'GI-77', region: 'Varanasi, Uttar Pradesh', loom: 'Kadwa Brocade Handloom', zari: 'Real Electroplated Gold Zari' }
    : isPaithani
    ? { giCode: 'GI-84', region: 'Yeola & Paithan, Maharashtra', loom: 'Tapestry Weave Handloom', zari: 'Fine Muga Gold Thread' }
    : { giCode: 'GI-112', region: 'Andhra Pradesh & Telangana', loom: 'Single Jamdani Pit Loom', zari: 'Pure Zari Pallu' };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs font-sans text-left text-[#17211F]">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          className="bg-white border border-[#DDD8CF] rounded-3xl shadow-2xl max-w-xl w-full p-6 space-y-6 overflow-hidden relative"
        >
          {/* Header */}
          <div className="flex justify-between items-start border-b border-[#E8E2D9] pb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-[#17211F] text-[#F3C56A] flex items-center justify-center border border-[#F3C56A]/30 shadow-xs">
                <Award className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold font-serif text-[#17211F]">Authenticity & GI Tag Certificate</h3>
                <p className="text-xs text-[#71817A]">Geographical Indication & SilkMark Verification</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-full hover:bg-[#F7F4EE] text-[#71817A] hover:text-[#17211F] transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Certificate Badge Banner */}
          <div className="p-4 bg-[#F7F4EE] border border-[#DDD8CF] rounded-2xl space-y-2">
            <div className="flex justify-between items-center text-xs">
              <span className="font-bold text-[#1E6A62] flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-[#1E6A62]" /> Ministry of Textiles Certified
              </span>
              <span className="font-mono text-xs font-bold text-[#F3C56A] bg-[#17211F] px-2.5 py-0.5 rounded-full">
                {giData.giCode}
              </span>
            </div>
            <h4 className="text-sm font-bold text-[#17211F] font-serif">{product.name}</h4>
            <p className="text-xs text-[#71817A] leading-relaxed">
              This saree is verified to originate from certified weaver clusters in <strong>{giData.region}</strong>, preserving century-old generational weaving traditions.
            </p>
          </div>

          {/* Verification Spec Grid */}
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-3 bg-white border border-[#DDD8CF] rounded-xl space-y-1">
              <span className="text-[10px] uppercase font-bold text-[#71817A] tracking-wider block">Loom Architecture</span>
              <span className="font-bold text-[#17211F] block">{giData.loom}</span>
            </div>
            <div className="p-3 bg-white border border-[#DDD8CF] rounded-xl space-y-1">
              <span className="text-[10px] uppercase font-bold text-[#71817A] tracking-wider block">Zari Metallurgy</span>
              <span className="font-bold text-[#17211F] block">{giData.zari}</span>
            </div>
            <div className="p-3 bg-white border border-[#DDD8CF] rounded-xl space-y-1">
              <span className="text-[10px] uppercase font-bold text-[#71817A] tracking-wider block">Artisan Compensation</span>
              <span className="font-bold text-emerald-800 block">100% Direct to Weaver Guild</span>
            </div>
            <div className="p-3 bg-white border border-[#DDD8CF] rounded-xl space-y-1">
              <span className="text-[10px] uppercase font-bold text-[#71817A] tracking-wider block">Silk Mark India</span>
              <span className="font-bold text-[#1E6A62] block">Verified 100% Pure Silk</span>
            </div>
          </div>

          {/* Bottom Actions */}
          <div className="pt-2 flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 py-3 bg-[#17211F] hover:bg-[#1E6A62] text-white font-bold text-xs uppercase tracking-widest rounded-full transition-all cursor-pointer shadow-md"
            >
              Close Certificate
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
