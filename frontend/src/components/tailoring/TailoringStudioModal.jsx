import { useState, useEffect } from 'react';
import {
  Scissors,
  X,
  CheckCircle2,
  Sparkles,
  Info,
  Clock,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { useCurrency } from '../../context/CurrencyContext';

const BUST_SIZES = ['32"', '34"', '36"', '38"', '40"', '42"', '44"', '46"'];
const FRONT_NECKS = ['Sweetheart', 'Deep U', 'Classic Round', 'Boat Neck', 'High Collar'];
const BACK_NECKS = ['Deep U with Dori', 'Keyhole', 'Square Back', 'Backless with Tassels'];
const SLEEVES = ['Elbow Length (Traditional)', 'Cap Sleeve', 'Sleeveless', '3/4th Sleeve', 'Full Length'];

export default function TailoringStudioModal({
  isOpen,
  onClose,
  product,
  currentTailoring,
  onApplyTailoring,
}) {
  const { formatPrice } = useCurrency();
  const [fallPico, setFallPico] = useState(true);
  const [blouseStyle, setBlouseStyle] = useState('unstitched'); // 'unstitched' | 'tailored' | 'designer'
  const [bust, setBust] = useState('36"');
  const [frontNeck, setFrontNeck] = useState('Sweetheart');
  const [backNeck, setBackNeck] = useState('Deep U with Dori');
  const [sleeve, setSleeve] = useState('Elbow Length (Traditional)');
  const [padded, setPadded] = useState(true);
  const [opening, setOpening] = useState('Back Hooks');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (isOpen && currentTailoring) {
      setFallPico(currentTailoring.fallPico ?? true);
      setBlouseStyle(currentTailoring.blouseStyle || 'unstitched');
      if (currentTailoring.measurements) {
        setBust(currentTailoring.measurements.bust || '36"');
        setFrontNeck(currentTailoring.measurements.frontNeck || 'Sweetheart');
        setBackNeck(currentTailoring.measurements.backNeck || 'Deep U with Dori');
        setSleeve(currentTailoring.measurements.sleeve || 'Elbow Length (Traditional)');
        setPadded(currentTailoring.measurements.padded ?? true);
        setOpening(currentTailoring.measurements.opening || 'Back Hooks');
        setNotes(currentTailoring.measurements.notes || '');
      }
    }
  }, [isOpen, currentTailoring]);

  if (!isOpen) return null;

  const isComplimentaryFall = (product?.price || 0) >= 5000;
  const fallPrice = fallPico ? (isComplimentaryFall ? 0 : 250) : 0;
  const blousePrice =
    blouseStyle === 'tailored' ? 1499 : blouseStyle === 'designer' ? 2799 : 0;
  const totalExtra = fallPrice + blousePrice;

  const handleSave = () => {
    onApplyTailoring({
      fallPico,
      fallPrice,
      blouseStyle,
      blousePrice,
      totalExtra,
      measurements:
        blouseStyle !== 'unstitched'
          ? {
              bust,
              frontNeck,
              backNeck,
              sleeve,
              padded,
              opening,
              notes: notes.trim(),
            }
          : null,
    });
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tailoring-studio-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[#1E6A62]/10 text-[#1E6A62]">
              <Scissors className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 id="tailoring-studio-title" className="text-xl font-bold text-[#17211F]">
                  Luxury Tailoring & Finishing Studio
                </h2>
                <span className="rounded-full bg-[#E7F5F3] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-[#0F766E]">
                  Master Artisans
                </span>
              </div>
              <p className="mt-0.5 text-xs font-semibold text-[#71817A]">
                Precision blouse stitching, authentic fall & pico edging tailored to your drape
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close tailoring dialog"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-5 space-y-6">
          {/* Section 1: Fall & Pico Finishing */}
          <div className="rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-4">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                  1. Saree Fall & Pico Finishing
                </span>
                <p className="mt-1 text-xs text-[#71817A]">
                  Color-matched pure cotton fall hand-stitched along hem with precision pico border
                  reinforcement so your saree is immediately ready to drape upon unboxing.
                </p>
              </div>
              <label className="relative ml-4 inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  checked={fallPico}
                  onChange={(e) => setFallPico(e.target.checked)}
                  className="peer sr-only"
                />
                <div className="peer h-6 w-11 rounded-full bg-[#DDD8CF] after:absolute after:top-[2px] after:left-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-all after:content-[''] peer-checked:bg-[#1E6A62] peer-checked:after:translate-x-full peer-focus:outline-none" />
              </label>
            </div>
            <div className="mt-2 text-[11px] font-bold">
              {fallPico ? (
                isComplimentaryFall ? (
                  <span className="text-emerald-700">✓ Complimentary Free with this saree</span>
                ) : (
                  <span className="text-[#1E6A62]">Added: {formatPrice(250)}</span>
                )
              ) : (
                <span className="text-[#71817A]">Unfinished edges (no fall/pico)</span>
              )}
            </div>
          </div>

          {/* Section 2: Blouse Stitching Mode */}
          <div>
            <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
              2. Blouse Piece Customization
            </label>
            <div className="mt-2 grid grid-cols-1 gap-2.5 sm:grid-cols-3">
              <button
                type="button"
                onClick={() => setBlouseStyle('unstitched')}
                className={`rounded-[8px] border p-3.5 text-left transition-all ${
                  blouseStyle === 'unstitched'
                    ? 'border-[#1E6A62] bg-[#1E6A62]/5 ring-1 ring-[#1E6A62]'
                    : 'border-[#DDD8CF] hover:border-[#1E6A62]/40'
                }`}
              >
                <p className="text-xs font-bold text-[#17211F]">Unstitched Fabric</p>
                <p className="mt-1 text-[11px] text-[#71817A]">Included in saree</p>
                <p className="mt-2 text-xs font-black text-emerald-800">FREE</p>
              </button>

              <button
                type="button"
                onClick={() => setBlouseStyle('tailored')}
                className={`rounded-[8px] border p-3.5 text-left transition-all ${
                  blouseStyle === 'tailored'
                    ? 'border-[#1E6A62] bg-[#1E6A62]/5 ring-1 ring-[#1E6A62]'
                    : 'border-[#DDD8CF] hover:border-[#1E6A62]/40'
                }`}
              >
                <p className="text-xs font-bold text-[#17211F]">Custom Tailored</p>
                <p className="mt-1 text-[11px] text-[#71817A]">Precision tailored fit</p>
                <p className="mt-2 text-xs font-black text-[#1E6A62]">+{formatPrice(1499)}</p>
              </button>

              <button
                type="button"
                onClick={() => setBlouseStyle('designer')}
                className={`rounded-[8px] border p-3.5 text-left transition-all ${
                  blouseStyle === 'designer'
                    ? 'border-[#1E6A62] bg-[#1E6A62]/5 ring-1 ring-[#1E6A62]'
                    : 'border-[#DDD8CF] hover:border-[#1E6A62]/40'
                }`}
              >
                <p className="text-xs font-bold text-[#17211F]">Designer Maggam</p>
                <p className="mt-1 text-[11px] text-[#71817A]">Zari embroidery work</p>
                <p className="mt-2 text-xs font-black text-[#9B6A27]">+{formatPrice(2799)}</p>
              </button>
            </div>
          </div>

          {/* Section 3: Measurements (if tailored or designer selected) */}
          {blouseStyle !== 'unstitched' && (
            <div className="space-y-4 rounded-[10px] border border-[#DDD8CF] bg-[#FAF8F5] p-4">
              <div className="flex items-center gap-2">
                <Scissors className="h-4 w-4 text-[#1E6A62]" />
                <span className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                  3. Enter Tailoring Measurements
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Bust Size
                  <select
                    value={bust}
                    onChange={(e) => setBust(e.target.value)}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    {BUST_SIZES.map((b) => (
                      <option key={b} value={b}>
                        {b}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Front Neckline
                  <select
                    value={frontNeck}
                    onChange={(e) => setFrontNeck(e.target.value)}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    {FRONT_NECKS.map((n) => (
                      <option key={n} value={n}>
                        {n}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Back Neckline
                  <select
                    value={backNeck}
                    onChange={(e) => setBackNeck(e.target.value)}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    {BACK_NECKS.map((n) => (
                      <option key={n} value={n}>
                        {n}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Sleeve Style
                  <select
                    value={sleeve}
                    onChange={(e) => setSleeve(e.target.value)}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    {SLEEVES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Opening
                  <select
                    value={opening}
                    onChange={(e) => setOpening(e.target.value)}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    <option value="Back Hooks">Back Hooks</option>
                    <option value="Front Hooks">Front Hooks</option>
                    <option value="Side Concealed Zipper">Side Concealed Zipper</option>
                  </select>
                </label>

                <label className="grid gap-1 text-xs font-black text-[#17211F]">
                  Cups / Padding
                  <select
                    value={padded ? 'yes' : 'no'}
                    onChange={(e) => setPadded(e.target.value === 'yes')}
                    className="h-10 rounded-[6px] border border-[#DDD8CF] bg-white px-2.5 text-xs font-bold outline-none focus:border-[#1E6A62]"
                  >
                    <option value="yes">Padded Cups</option>
                    <option value="no">Non-Padded</option>
                  </select>
                </label>
              </div>

              <label className="grid gap-1 text-xs font-black text-[#17211F]">
                Special Designer Instructions (Optional)
                <textarea
                  rows={2}
                  placeholder="e.g. Keep deep back with gold latkan tassels; sleeve border placed 1 inch from hem."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="rounded-[6px] border border-[#DDD8CF] bg-white p-2.5 text-xs font-medium outline-none placeholder:text-[#9AA5A5] focus:border-[#1E6A62]"
                />
              </label>
            </div>
          )}

          {/* Lead Time & Return Policy Notice */}
          <div className="rounded-[8px] bg-[#FFFDF7] border border-[#F3C56A]/40 p-3 text-xs text-[#71817A]">
            <div className="flex items-center gap-2 font-bold text-[#9B6A27]">
              <Clock className="h-4 w-4" />
              <span>Tailoring Lead Time Notice</span>
            </div>
            <p className="mt-1 leading-relaxed">
              Adding custom tailoring requires an extra <strong>5 to 7 working days</strong> for master artisan
              finishing. Customized blouses are stitched specifically to your measurements.
            </p>
          </div>

          {/* Total Bar */}
          <div className="flex items-center justify-between border-t border-[#EAE6DF] pt-4">
            <div>
              <p className="text-[11px] font-bold text-[#71817A]">Customization Total</p>
              <p className="text-base font-black text-[#17211F]">
                {totalExtra === 0 ? 'FREE (+0)' : `+${formatPrice(totalExtra)}`}
              </p>
            </div>

            <button
              type="button"
              onClick={handleSave}
              className="flex items-center gap-2 rounded-[8px] bg-[#1E6A62] py-2.5 px-6 text-xs font-black text-white hover:bg-[#154e48] transition-colors"
            >
              Save & Apply Tailoring
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
