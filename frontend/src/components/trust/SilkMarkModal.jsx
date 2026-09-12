import { ShieldCheck, Award, X, CheckCircle2, Sparkles, Feather, ExternalLink } from 'lucide-react';

export default function SilkMarkModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="silk-mark-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[#F3E6C7] text-[#9B6A27]">
              <Award className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 id="silk-mark-title" className="text-xl font-bold text-[#17211F]">
                  Silk Mark Certified
                </h2>
                <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-black uppercase tracking-wider text-emerald-800">
                  100% Pure Silk
                </span>
              </div>
              <p className="mt-0.5 text-xs font-semibold text-[#71817A]">
                Silk Mark Organization of India (SMOI) Quality Guarantee
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Silk Mark dialog"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-5 space-y-5">
          {/* Trust Banner */}
          <div className="rounded-[10px] border border-[#F3C56A]/40 bg-[#FFFDF7] p-4 text-xs">
            <div className="flex items-center gap-2 font-black text-[#9B6A27]">
              <Sparkles className="h-4 w-4 text-[#F3C56A]" />
              <span>Government-Approved Authenticity Guarantee</span>
            </div>
            <p className="mt-1.5 leading-relaxed text-[#4E5B56]">
              Every pure silk saree at SareeKart carries the prestigious <strong>Silk Mark</strong> tag,
              issued by the Silk Mark Organization of India (initiated by the Central Silk Board,
              Ministry of Textiles, Govt. of India).
            </p>
          </div>

          {/* 3 Pillars */}
          <div className="space-y-3 text-xs">
            <div className="flex items-start gap-3 rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] p-3.5">
              <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-[#1E6A62]" />
              <div>
                <p className="font-bold text-[#17211F]">100% Natural Silk Fibers</p>
                <p className="mt-0.5 text-[#71817A]">
                  Guaranteed free from synthetic polyester, art-silk, nylon, or viscose blending.
                  Tested via microscopic fiber burn tests and laboratory spectrometer analysis.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] p-3.5">
              <Award className="mt-0.5 h-4 w-4 shrink-0 text-[#1E6A62]" />
              <div>
                <p className="font-bold text-[#17211F]">GI Tag Protected Weave Clusters</p>
                <p className="mt-0.5 text-[#71817A]">
                  Direct provenance protection for authentic geographical indications including
                  <strong> Kanchipuram</strong>, <strong>Varanasi Brocade</strong>, <strong>Uppada Jamdani</strong>, and <strong>Paithani</strong>.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] p-3.5">
              <Feather className="mt-0.5 h-4 w-4 shrink-0 text-[#1E6A62]" />
              <div>
                <p className="font-bold text-[#17211F]">Direct From Master Weaver Guilds</p>
                <p className="mt-0.5 text-[#71817A]">
                  Zero intermediaries. Fair artisan compensation that honors generations of handloom
                  knowledge and authentic korvai, kadwa, and warp techniques.
                </p>
              </div>
            </div>
          </div>

          {/* Hologram / Certificate Note */}
          <div className="rounded-[8px] bg-[#17211F] p-4 text-xs text-white">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-black uppercase tracking-widest text-[#F3C56A]">
                Physical Security Tag
              </span>
              <CheckCircle2 className="h-4 w-4 text-[#F3C56A]" />
            </div>
            <p className="mt-2 text-xs leading-relaxed text-[#DDE4EA]">
              Your saree will arrive packaged with its official serialized <strong>Silk Mark Hologram</strong> barcode
              affixed to the drape, which can be verified directly on the national SMOI portal.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="flex h-11 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-bold text-white hover:bg-[#154e48] transition-colors"
          >
            Understood & Close
          </button>
        </div>
      </div>
    </div>
  );
}
