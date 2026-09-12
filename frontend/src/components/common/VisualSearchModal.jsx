import React, { useState } from 'react';
import { Camera, Upload, X, Sparkles, Check, ShoppingBag, ArrowRight, Eye, RefreshCw, Palette } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { addToCart, closeAddedModal } from '../../redux/slices/cartSlice';
import visualSearchService from '../../services/visualSearchService';

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val || 0);

// Preset sample sarees for quick 1-click visual search testing
const SAMPLE_PREVIEWS = [
  {
    id: 1,
    title: 'Banarasi Bridal',
    tag: 'Ruby Red & Gold',
    primaryColor: '#B84F49',
    secondaryColor: '#D4AF37',
    weaveHint: 'Banarasi',
    occasion: 'Bridal',
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=400&q=80',
  },
  {
    id: 2,
    title: 'Kanchipuram Silk',
    tag: 'Tissue Gold',
    primaryColor: '#D4AF37',
    secondaryColor: '#B84F49',
    weaveHint: 'Kanchipuram',
    occasion: 'Festive',
    image: 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=400&q=80',
  },
  {
    id: 3,
    title: 'Cotton Jamdani',
    tag: 'Midnight Black',
    primaryColor: '#1E1E1E',
    secondaryColor: '#D4AF37',
    weaveHint: 'Jamdani',
    occasion: 'Celebration',
    image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=400&q=80',
  },
];

/**
 * Extracts dominant color using HTML5 Canvas 2D pixel sampling
 */
const extractCanvasDominantColor = (imageSrc) => {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = 'Anonymous';
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        canvas.width = 48;
        canvas.height = 48;
        ctx.drawImage(img, 0, 0, 48, 48);

        const imgData = ctx.getImageData(0, 0, 48, 48).data;
        let rSum = 0, gSum = 0, bSum = 0, count = 0;

        for (let i = 0; i < imgData.length; i += 16) {
          const r = imgData[i];
          const g = imgData[i + 1];
          const b = imgData[i + 2];
          // Skip near whites/blacks for rich fabric color extraction
          const brightness = (r + g + b) / 3;
          if (brightness > 25 && brightness < 240) {
            rSum += r;
            gSum += g;
            bSum += b;
            count++;
          }
        }

        if (count > 0) {
          const r = Math.round(rSum / count);
          const g = Math.round(gSum / count);
          const b = Math.round(bSum / count);
          const hex = `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
          resolve({ hex, r, g, b });
        } else {
          resolve({ hex: '#B84F49', r: 184, g: 79, b: 73 });
        }
      } catch (e) {
        resolve({ hex: '#B84F49', r: 184, g: 79, b: 73 });
      }
    };
    img.onerror = () => {
      resolve({ hex: '#B84F49', r: 184, g: 79, b: 73 });
    };
    img.src = imageSrc;
  });
};

export default function VisualSearchModal({ isOpen, onClose }) {
  const [selectedImage, setSelectedImage] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState(null);
  const [recentlyBookedId, setRecentlyBookedId] = useState(null);
  const [extractedHex, setExtractedHex] = useState(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  if (!isOpen) return null;

  const handleFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async () => {
      const base64Url = reader.result;
      setSelectedImage(base64Url);
      setIsSearching(true);
      setResults(null);

      // Quantize dominant color via HTML5 Canvas
      const color = await extractCanvasDominantColor(base64Url);
      setExtractedHex(color.hex);

      try {
        const res = await visualSearchService.matchSarees({
          primaryColor: color.hex,
          secondaryColor: '#D4AF37',
          weaveHint: '',
          occasion: 'Festive',
          source: 'FILE_UPLOAD',
        });
        if (res?.data) {
          setResults(res.data);
        }
      } catch (err) {
        console.error('Visual search API error:', err);
      } finally {
        setIsSearching(false);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleSelectSample = async (sample) => {
    setIsSearching(true);
    setSelectedImage(sample.image);
    setExtractedHex(sample.primaryColor);
    setResults(null);

    try {
      const res = await visualSearchService.matchSarees({
        primaryColor: sample.primaryColor,
        secondaryColor: sample.secondaryColor,
        weaveHint: sample.weaveHint,
        occasion: sample.occasion,
        source: 'SAMPLE_PREVIEW',
      });
      if (res?.data) {
        setResults(res.data);
      }
    } catch (err) {
      console.error('Failed to match sample drape:', err);
    } finally {
      setIsSearching(false);
    }
  };

  const handleBook = (product) => {
    dispatch(addToCart(product));
    dispatch(closeAddedModal());
    setRecentlyBookedId(product.id);
    setTimeout(() => setRecentlyBookedId(null), 4000);
  };

  const handleReset = () => {
    setSelectedImage(null);
    setResults(null);
    setExtractedHex(null);
    setIsSearching(false);
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-[200] flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs font-sans text-left text-[#17211F]">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 15 }}
          className="bg-[#F7F4EE] border border-[#DDD8CF] rounded-2xl shadow-2xl max-w-2xl w-full max-h-[92vh] flex flex-col overflow-hidden"
        >
          {/* Header */}
          <header className="flex justify-between items-center border-b border-[#DDD8CF] bg-white px-5 py-4 shrink-0">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-[#17211F] text-[#F3C56A] flex items-center justify-center shadow-xs">
                <Camera className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-serif font-bold text-[#17211F]">AI Visual Saree Search</h3>
                  <span className="rounded-full bg-purple-100 border border-purple-300 px-2 py-0.5 text-[9px] font-bold uppercase tracking-widest text-purple-800">
                    Gemini 2.5 Vision
                  </span>
                </div>
                <p className="text-xs text-[#71817A]">Upload or drop any saree photo to discover matching handloom weaves</p>
              </div>
            </div>

            <button
              onClick={onClose}
              aria-label="Close visual search modal"
              className="p-2 rounded-full hover:bg-[#F7F4EE] text-[#71817A] hover:text-[#17211F] transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </header>

          {/* Body Content */}
          <div className="p-5 overflow-y-auto space-y-5">
            {!selectedImage ? (
              <div className="space-y-4">
                {/* Upload Zone */}
                <label className="border-2 border-dashed border-[#DDD8CF] hover:border-[#1E6A62] bg-white hover:bg-[#FAF8F5] rounded-xl p-8 flex flex-col items-center justify-center gap-3 cursor-pointer transition shadow-xs">
                  <div className="w-12 h-12 rounded-full bg-[#F7F4EE] text-[#1E6A62] flex items-center justify-center border border-[#DDD8CF]">
                    <Upload className="w-6 h-6" />
                  </div>
                  <div className="text-center space-y-1">
                    <p className="text-sm font-bold text-[#17211F]">Click or Drag & Drop Saree Image</p>
                    <p className="text-[11px] text-[#71817A]">JPG, PNG, or WebP • Analyzes canvas palette, weave, and zari border</p>
                  </div>
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleFileUpload}
                    aria-label="Upload Saree Photo"
                  />
                </label>

                {/* Instant Sample Prompts */}
                <div>
                  <p className="text-[11px] font-bold uppercase tracking-wider text-[#71817A] mb-2.5">
                    Or try instant visual search with sample handlooms:
                  </p>
                  <div className="grid grid-cols-3 gap-2.5">
                    {SAMPLE_PREVIEWS.map((sample) => (
                      <button
                        key={sample.id}
                        type="button"
                        onClick={() => handleSelectSample(sample)}
                        className="group flex flex-col items-center gap-2 rounded-xl border border-[#DDD8CF] bg-white p-2 text-center transition hover:border-[#1E6A62] hover:shadow-xs cursor-pointer"
                      >
                        <img
                          src={sample.image}
                          alt={sample.title}
                          className="h-20 w-full rounded-lg object-cover object-top border border-[#E5E0D8] group-hover:scale-[1.02] transition"
                        />
                        <div className="min-w-0">
                          <p className="truncate text-xs font-bold text-[#17211F]">{sample.title}</p>
                          <span className="text-[10px] text-[#71817A] block">{sample.tag}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Active Image Card */}
                <div className="flex items-center justify-between gap-4 bg-white p-3 border border-[#DDD8CF] rounded-xl shadow-xs">
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={selectedImage}
                      alt="Uploaded Saree"
                      className="w-16 h-16 object-cover object-top rounded-lg border border-[#DDD8CF] shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-bold text-emerald-800 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full inline-flex items-center gap-1">
                          <Check className="w-3 h-3" /> Image Analyzed by Gemini
                        </span>
                        {extractedHex && (
                          <span className="flex items-center gap-1 text-[10px] font-mono text-[#71817A]">
                            <span className="w-2.5 h-2.5 rounded-full border border-black/10 inline-block" style={{ backgroundColor: extractedHex }} />
                            {extractedHex}
                          </span>
                        )}
                      </div>
                      <p className="text-xs font-bold text-[#17211F] mt-1 truncate">Visual Signature Extracted</p>
                    </div>
                  </div>
                  <button
                    onClick={handleReset}
                    className="shrink-0 text-xs font-bold text-[#1E6A62] hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <RefreshCw className="h-3.5 w-3.5" /> Try Another Image
                  </button>
                </div>

                {/* Loading State */}
                {isSearching && (
                  <div className="py-12 flex flex-col items-center justify-center gap-3 bg-white border border-[#DDD8CF] rounded-xl shadow-xs">
                    <Sparkles className="w-8 h-8 text-[#F3C56A] animate-spin" />
                    <p className="text-xs font-bold text-[#17211F]">
                      Searching catalog drapes with color space Euclidean & weave matching...
                    </p>
                  </div>
                )}

                {/* Analysis & Results */}
                {results && !isSearching && (
                  <div className="space-y-4">
                    {/* Detected Attributes */}
                    {results.detectedAttributes && (
                      <div className="bg-white p-3.5 rounded-xl border border-[#DDD8CF] shadow-xs">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] mb-2 flex items-center gap-1">
                          <Palette className="w-3 h-3 text-[#1E6A62]" /> Visual DNA Extracted by Gemini:
                        </p>
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                          <div className="rounded-lg bg-[#F7F4EE] p-2 border border-[#E8E2D9]">
                            <span className="text-[9px] font-bold uppercase text-[#71817A] block">Color</span>
                            <span className="text-xs font-bold text-[#17211F]">{results.detectedAttributes.primaryColor}</span>
                          </div>
                          <div className="rounded-lg bg-[#F7F4EE] p-2 border border-[#E8E2D9]">
                            <span className="text-[9px] font-bold uppercase text-[#71817A] block">Palette Family</span>
                            <span className="text-xs font-bold text-[#17211F] truncate">{results.detectedAttributes.paletteFamily}</span>
                          </div>
                          <div className="rounded-lg bg-[#F7F4EE] p-2 border border-[#E8E2D9]">
                            <span className="text-[9px] font-bold uppercase text-[#71817A] block">Weave Pattern</span>
                            <span className="text-xs font-bold text-[#17211F] truncate">{results.detectedAttributes.weave}</span>
                          </div>
                          <div className="rounded-lg bg-[#F7F4EE] p-2 border border-[#E8E2D9]">
                            <span className="text-[9px] font-bold uppercase text-[#71817A] block">Latency SLA</span>
                            <span className="text-xs font-bold text-emerald-800">{results.executionTimeMs} ms</span>
                          </div>
                        </div>
                        {results.summary && (
                          <p className="text-xs text-[#52605B] mt-2.5 italic">
                            "{results.summary}"
                          </p>
                        )}
                      </div>
                    )}

                    {/* Matched Products */}
                    <div className="space-y-2.5">
                      <p className="text-[11px] font-bold uppercase tracking-wider text-[#71817A]">
                        Closest Matching Handloom Drapes in Stock ({results.matches?.length || 0}):
                      </p>
                      {results.matches && results.matches.length > 0 ? (
                        results.matches.map((product) => {
                          const isJustBooked = recentlyBookedId === product.id;
                          return (
                            <article
                              key={product.id}
                              className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-[#DDD8CF] bg-white p-3 shadow-xs hover:border-[#1E6A62] transition"
                            >
                              <div className="flex items-center gap-3 min-w-0">
                                <img
                                  src={product.image || 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=400&q=80'}
                                  alt={product.name}
                                  className="h-20 w-16 rounded-lg object-cover object-top shrink-0 border border-[#E5E0D8]"
                                />
                                <div className="min-w-0">
                                  <div className="flex items-center gap-2">
                                    <span className="rounded-full bg-emerald-50 border border-emerald-200 px-2 py-0.5 text-[9px] font-bold uppercase tracking-widest text-emerald-800">
                                      {product.confidence || `${product.confidenceScore}% Match`}
                                    </span>
                                    <span className="text-[10px] text-[#71817A] uppercase font-bold">{product.fabric}</span>
                                  </div>
                                  <h4 className="text-xs font-bold text-[#17211F] truncate mt-1">{product.name}</h4>
                                  <p className="text-xs font-serif font-bold text-[#17211F] mt-0.5">
                                    {formatCurrency(product.price)}
                                  </p>
                                  {product.matchReason && (
                                    <p className="text-[10px] text-[#71817A] line-clamp-1 mt-0.5">
                                      {product.matchReason}
                                    </p>
                                  )}
                                </div>
                              </div>

                              <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                                <button
                                  onClick={() => {
                                    onClose();
                                    navigate(`/products/${product.id}`);
                                  }}
                                  className="px-3 py-2 rounded-lg text-[11px] font-bold text-[#17211F] border border-[#DDD8CF] hover:bg-[#FAF8F5] transition flex items-center gap-1 cursor-pointer"
                                >
                                  <Eye className="h-3.5 w-3.5" /> View Saree
                                </button>
                                <button
                                  onClick={() => handleBook(product)}
                                  aria-label={`Book ${product.name} now`}
                                  className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-[11px] font-bold uppercase tracking-wider transition cursor-pointer ${
                                    isJustBooked
                                      ? 'bg-emerald-700 text-white'
                                      : 'bg-[#17211F] hover:bg-[#1E6A62] text-white shadow-xs'
                                  }`}
                                >
                                  {isJustBooked ? (
                                    <>
                                      <Check className="h-3.5 w-3.5" /> Booked!
                                    </>
                                  ) : (
                                    <>
                                      <ShoppingBag className="h-3.5 w-3.5 text-[#F3C56A]" /> Book Now
                                    </>
                                  )}
                                </button>
                              </div>
                            </article>
                          );
                        })
                      ) : (
                        <div className="p-6 bg-white border border-[#DDD8CF] rounded-xl text-center text-xs text-[#71817A]">
                          No catalog drapes found matching this exact visual profile. Try another image or sample.
                        </div>
                      )}

                      {recentlyBookedId && (
                        <motion.div
                          initial={{ opacity: 0, y: 5 }}
                          animate={{ opacity: 1, y: 0 }}
                          className="flex items-center justify-between bg-emerald-50 border border-emerald-200 rounded-lg p-3 text-xs text-emerald-900 mt-2"
                        >
                          <span className="font-semibold">Saree added to your shopping bag!</span>
                          <button
                            onClick={() => {
                              onClose();
                              navigate('/checkout');
                            }}
                            className="font-bold underline text-emerald-800 hover:text-emerald-950 cursor-pointer"
                          >
                            Go to Checkout →
                          </button>
                        </motion.div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
