import { useEffect, useState } from 'react';
import {
  Sparkles,
  X,
  Scissors,
  CheckCircle2,
  Crown,
  Layers,
  HeartHandshake,
  Gem,
  Palette,
  Flower2,
  ArrowRight,
  RefreshCw,
  Loader2,
  ShieldCheck
} from 'lucide-react';
import aiStylistService from '../../services/aiStylistService';

export default function AiStylistModal({
  isOpen,
  onClose,
  product,
  onCustomizeTailoring
}) {
  const [loading, setLoading] = useState(false);
  const [stylingData, setStylingData] = useState(null);
  const [error, setError] = useState('');
  const [selectedLookIndex, setSelectedLookIndex] = useState(0);

  const loadStyling = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await aiStylistService.styleDrape({
        productId: product?.id,
        sareeName: product?.name,
        fabric: product?.fabric,
        primaryColor: product?.color,
        occasion: product?.occasion || 'Bridal / Wedding Festivities',
        zariType: 'Pure Zari'
      });
      if (res && res.data) {
        setStylingData(res.data);
        setSelectedLookIndex(0);
      }
    } catch (err) {
      setError(err.message || 'Unable to generate styling recommendations right now.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && product) {
      loadStyling();
    }
  }, [isOpen, product]);

  if (!isOpen) return null;

  const currentLook = stylingData?.curatedLooks?.[selectedLookIndex];

  const handleApplyToTailoring = () => {
    if (!currentLook) return;

    if (stylingData?.consultationId) {
      aiStylistService.trackTailoring(stylingData.consultationId);
    }

    if (onCustomizeTailoring) {
      onCustomizeTailoring({
        blouseStyle: currentLook.blouse.blouseStyle || 'designer',
        frontNeck: currentLook.blouse.frontNeck,
        backNeck: currentLook.blouse.backNeck,
        sleeve: currentLook.blouse.sleeve,
        notes: `AI Stylist Preset (${currentLook.title}): Contrast color ${currentLook.blouse.contrastColor} in ${currentLook.blouse.fabric}. Recommended work: ${currentLook.blouse.recommendedWork}.`
      });
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs animate-fade-in overflow-y-auto">
      <div className="relative my-8 w-full max-w-3xl rounded-[16px] border border-[#C9C1B5] bg-[#FCFAF6] shadow-2xl overflow-hidden">
        
        {/* Header Banner */}
        <div className="relative bg-gradient-to-r from-[#17211F] via-[#1E6A62] to-[#17211F] px-6 py-5 text-white">
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-2.5 py-0.5 text-[10px] font-black uppercase tracking-widest text-[#E3F5F0]">
                <Sparkles className="h-3 w-3 text-[#FDE68A]" />
                SareeKart AI Drape & Contrast Concierge
              </div>
              <h2 className="text-xl sm:text-2xl font-serif font-black tracking-tight text-[#FAF8F5]">
                Bespoke Ensemble Curation
              </h2>
              <p className="text-xs text-[#E3F5F0]/80">
                Styling: <strong className="text-white">{product?.name}</strong>
              </p>
            </div>
            <button
              onClick={onClose}
              className="rounded-full bg-white/10 p-1.5 text-white/80 hover:bg-white/20 hover:text-white transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Modal Content */}
        <div className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
          {loading && (
            <div className="py-16 text-center space-y-4">
              <div className="inline-block relative">
                <div className="h-12 w-12 rounded-full border-3 border-[#E7E2D8] border-t-[#1E6A62] animate-spin" />
                <Sparkles className="h-5 w-5 text-[#B45309] absolute inset-0 m-auto" />
              </div>
              <div>
                <p className="text-sm font-bold font-serif text-[#17211F]">Analyzing Handloom Weave & Zari Harmony...</p>
                <p className="text-xs text-[#71817A] mt-1">Consulting traditional Indian color contrast matrices and regal temple styling rules</p>
              </div>
            </div>
          )}

          {error && (
            <div className="rounded-[10px] border border-red-200 bg-red-50 p-4 text-center space-y-3">
              <p className="text-xs font-bold text-red-700">{error}</p>
              <button
                onClick={loadStyling}
                className="inline-flex items-center gap-1.5 rounded-[6px] bg-[#1E6A62] px-3.5 py-1.5 text-xs font-bold text-white hover:bg-[#154e48]"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                Retry Analysis
              </button>
            </div>
          )}

          {!loading && currentLook && (
            <div className="space-y-6">
              {/* Look Selector Tabs */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 border-b border-[#DDD8CF] pb-4">
                {stylingData.curatedLooks.map((look, idx) => (
                  <button
                    key={look.id}
                    onClick={() => setSelectedLookIndex(idx)}
                    className={`rounded-[8px] p-3 text-left transition-all border ${
                      selectedLookIndex === idx
                        ? 'border-[#1E6A62] bg-[#E7F5F3] shadow-xs'
                        : 'border-[#E7E2D8] bg-white hover:bg-[#FAF8F5]'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-black uppercase tracking-wider text-[#71817A]">
                        Look 0{idx + 1}
                      </span>
                      {selectedLookIndex === idx && (
                        <CheckCircle2 className="h-3.5 w-3.5 text-[#1E6A62]" />
                      )}
                    </div>
                    <p className="mt-1 text-xs font-black text-[#17211F] truncate">{look.title}</p>
                    <p className="text-[10px] text-[#71817A] truncate">{look.subtitle}</p>
                  </button>
                ))}
              </div>

              {/* Look Banner Description */}
              <div className="rounded-[8px] border border-[#E7E2D8] bg-[#F7F4EE] p-4 text-xs">
                <p className="font-serif font-black text-[#17211F] text-sm">{currentLook.title}</p>
                <p className="text-[#4B5563] mt-1 leading-relaxed">{currentLook.description}</p>
                <p className="text-[11px] text-[#71817A] mt-2 italic border-t border-[#DDD8CF]/60 pt-2">
                  <strong>Master Stylist Note:</strong> {currentLook.stylingRationale}
                </p>
              </div>

              {/* Contrast Blouse Pairing Card */}
              <div className="rounded-[12px] border border-[#B8D8D1] bg-white p-5 shadow-xs space-y-4">
                <div className="flex items-center justify-between border-b border-[#F0ECE1] pb-3">
                  <div className="flex items-center gap-2">
                    <span className="rounded bg-[#E7F5F3] p-1.5 text-[#1E6A62]">
                      <Scissors className="h-4 w-4" />
                    </span>
                    <h3 className="text-sm font-black text-[#17211F]">Curated Contrast Blouse Ensemble</h3>
                  </div>
                  <span className="rounded-full bg-[#FEF3C7] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-[#92400E]">
                    Recommended Preset
                  </span>
                </div>

                {/* Color Swatch Harmony */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-center rounded-[8px] bg-[#FAF8F5] p-3.5 border border-[#EAE5DC]">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#71817A]">Saree Drape Color</span>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="h-5 w-5 rounded-full border border-black/10 shadow-xs shrink-0" style={{ backgroundColor: '#8B0000' }} />
                      <span className="text-xs font-black text-[#17211F]">{stylingData.primaryColor} ({stylingData.fabric})</span>
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#1E6A62]">Recommended Contrast Blouse</span>
                    <div className="flex items-center gap-2 mt-1">
                      <span
                        className="h-5 w-5 rounded-full border border-black/10 shadow-xs shrink-0"
                        style={{ backgroundColor: currentLook.blouse.colorHex || '#0E5B4B' }}
                      />
                      <span className="text-xs font-black text-[#1E6A62]">
                        {currentLook.blouse.contrastColor}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Blouse Architecture Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                  <div className="p-2.5 rounded-[6px] bg-[#F7F4EE]">
                    <span className="text-[10px] font-bold text-[#71817A] uppercase">Fabric</span>
                    <p className="font-bold text-[#17211F] mt-0.5">{currentLook.blouse.fabric}</p>
                  </div>
                  <div className="p-2.5 rounded-[6px] bg-[#F7F4EE]">
                    <span className="text-[10px] font-bold text-[#71817A] uppercase">Front Neckline</span>
                    <p className="font-bold text-[#17211F] mt-0.5">{currentLook.blouse.frontNeck}</p>
                  </div>
                  <div className="p-2.5 rounded-[6px] bg-[#F7F4EE]">
                    <span className="text-[10px] font-bold text-[#71817A] uppercase">Back Neckline</span>
                    <p className="font-bold text-[#17211F] mt-0.5">{currentLook.blouse.backNeck}</p>
                  </div>
                  <div className="p-2.5 rounded-[6px] bg-[#F7F4EE]">
                    <span className="text-[10px] font-bold text-[#71817A] uppercase">Sleeve Silhouette</span>
                    <p className="font-bold text-[#17211F] mt-0.5">{currentLook.blouse.sleeve}</p>
                  </div>
                </div>

                <div className="rounded-[6px] bg-[#F7F4EE] p-2.5 text-xs text-[#4B5563]">
                  <strong className="text-[#17211F]">Artisan Embroidery & Border Accent:</strong> {currentLook.blouse.recommendedWork}
                </div>

                <div className="flex justify-end pt-1">
                  <button
                    onClick={handleApplyToTailoring}
                    className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-5 py-2.5 text-xs font-bold text-white hover:bg-[#154e48] shadow-sm transition-all"
                  >
                    <Scissors className="h-4 w-4" />
                    Apply Preset to Tailoring Studio
                    <ArrowRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>

              {/* Jewelry & Accents Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                
                {/* Jewelry Box */}
                <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-4 space-y-2.5 text-xs shadow-2xs">
                  <div className="flex items-center gap-2 border-b border-[#F0ECE1] pb-2 text-[#17211F] font-black">
                    <Gem className="h-4 w-4 text-[#B45309]" />
                    <span>Jewelry Pairing: {currentLook.jewelry.category}</span>
                  </div>
                  <div className="space-y-1.5 text-[11px] text-[#4B5563]">
                    <p><strong>Necklace:</strong> {currentLook.jewelry.necklace}</p>
                    <p><strong>Earrings:</strong> {currentLook.jewelry.earrings}</p>
                    <p><strong>Bangles:</strong> {currentLook.jewelry.bangles}</p>
                  </div>
                </div>

                {/* Hair & Accents Box */}
                <div className="rounded-[10px] border border-[#DDD8CF] bg-white p-4 space-y-2.5 text-xs shadow-2xs">
                  <div className="flex items-center gap-2 border-b border-[#F0ECE1] pb-2 text-[#17211F] font-black">
                    <Flower2 className="h-4 w-4 text-[#0F766E]" />
                    <span>Florals, Footwear & Drape Technique</span>
                  </div>
                  <div className="space-y-1.5 text-[11px] text-[#4B5563]">
                    <p><strong>Hair & Flowers:</strong> {currentLook.accents.hairFlorals}</p>
                    <p><strong>Footwear & Clutch:</strong> {currentLook.accents.footwear} · {currentLook.accents.potliBag}</p>
                    <p><strong>Pallu Drape Tip:</strong> {currentLook.drapingTechnique}</p>
                  </div>
                </div>

              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="border-t border-[#DDD8CF] bg-[#FAF8F5] px-6 py-4 flex items-center justify-between text-xs">
          <span className="flex items-center gap-1.5 text-[#71817A]">
            <ShieldCheck className="h-4 w-4 text-[#1E6A62]" />
            Cultural Authenticity Assured by SareeKart Connoisseurs
          </span>
          <button
            onClick={onClose}
            className="rounded-[6px] border border-[#DDD8CF] bg-white px-4 py-2 font-bold text-[#17211F] hover:bg-[#F7F4EE] transition-colors"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
}
