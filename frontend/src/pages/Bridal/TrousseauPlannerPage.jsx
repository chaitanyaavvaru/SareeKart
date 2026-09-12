import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import {
  Sparkles,
  CheckCircle2,
  Calendar,
  MessageCircle,
  ShoppingBag,
  ArrowRight,
  ShieldCheck,
  Award,
  Heart,
  Scissors,
  Package,
  Clock,
  Compass,
} from 'lucide-react';
import SEO from '../../components/common/SEO';
import { useCurrency } from '../../context/CurrencyContext';
import { addToCart } from '../../redux/slices/cartSlice';

const CEREMONIES = [
  {
    id: 'engagement',
    step: 1,
    title: 'Engagement & Ring Ceremony',
    tagline: 'Luminous pastels and delicate metallic luster for the first promise.',
    colorTheme: 'Pastel Rose, Powder Blue, Muted Gold',
    recommendedWeave: 'Tissue Organza & Pastel Kanchipuram',
    sarees: [
      {
        id: 'eng-1',
        name: 'Pastel Blush Pink Kanchipuram Tissue Silk',
        fabric: 'Pure Mulberry Silk with Silver Tissue Zari',
        price: 34500,
        image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Brocade Korvai Border',
      },
      {
        id: 'eng-2',
        name: 'Champagne Gold Banarasi Katan Georgette',
        fabric: 'Pure Katan Silk with Kadwa Floral Jaal',
        price: 28900,
        image: 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Delicate Floral Kaddi Border',
      },
    ],
  },
  {
    id: 'haldi',
    step: 2,
    title: 'Haldi & Mehendi Rituals',
    tagline: 'Auspicious sun-drenched yellows, light movement, and floral charm.',
    colorTheme: 'Mustard Yellow, Turmeric Gold, Parrot Green',
    recommendedWeave: 'Chanderi Silk & Uppada Gossamer Jamdani',
    sarees: [
      {
        id: 'hal-1',
        name: 'Marigold Yellow Uppada Handloom Jamdani',
        fabric: 'Fine Cotton Silk with Real Zari Extra Weft',
        price: 21500,
        image: 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Feather-Light Geometric Border',
      },
      {
        id: 'hal-2',
        name: 'Emerald Green & Turmeric Gadwal Saree',
        fabric: 'Combed Cotton Body with Pure Silk Zari Pallu',
        price: 18500,
        image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Kuttu Interlocked Temple Border',
      },
    ],
  },
  {
    id: 'sangeet',
    step: 3,
    title: 'Sangeet & Cocktail Soirée',
    tagline: 'Effortless royal drama, fluid drape, and dazzling reflection under lights.',
    colorTheme: 'Jewel Plum, Midnight Navy, Vintage Wine',
    recommendedWeave: 'Banarasi Shikargah & Royal Georgette Brocade',
    sarees: [
      {
        id: 'san-1',
        name: 'Royal Midnight Navy Banarasi Kadwa Brocade',
        fabric: 'Handwoven Katan Silk with Antique Zari Jaal',
        price: 42000,
        image: 'https://images.unsplash.com/photo-1609357605129-26f69add5d6e?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Intricate Shikargah Animal Jaal Border',
      },
      {
        id: 'san-2',
        name: 'Deep Wine Organza with Hand-Cut Munga Work',
        fabric: 'Pure Sheer Silk Organza with Scalloped Edging',
        price: 36000,
        image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Hand-Cut Zari Scalloped Hem',
      },
    ],
  },
  {
    id: 'muhurtham',
    step: 4,
    title: 'The Sacred Muhurtham (Wedding)',
    tagline: 'The timeless crown jewel of your trousseau. Uncut heirloom gold purity.',
    colorTheme: 'Crimson Sindoor, Kunkum Red, Royal Temple Gold',
    recommendedWeave: 'Pure Korvai Kanchipuram with 24K Dipped Zari',
    sarees: [
      {
        id: 'muh-1',
        name: 'Heritage Crimson Korvai Kanchipuram Bridal Silk',
        fabric: '100% Pure Mulberry Silk with 24K Gold-Dipped Tested Zari',
        price: 68000,
        image: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Three-Shuttle Petni Temple Border',
      },
      {
        id: 'muh-2',
        name: 'Auspicious Golden Mustard Kanchipuram Silk',
        fabric: 'Pure Mulberry Silk with Heavy Peacock Mayil Pallu',
        price: 58500,
        image: 'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Rudraksha & Temple Gopuram Border',
      },
    ],
  },
  {
    id: 'reception',
    step: 5,
    title: 'The Grand Wedding Reception',
    tagline: 'Modern regal magnificence as you celebrate with family and guests.',
    colorTheme: 'Imperial Peacock Blue, Regal Purple, Antique Rust',
    recommendedWeave: 'Royal Paithani Tapestry & Zari Brocade',
    sarees: [
      {
        id: 'rec-1',
        name: 'Royal Peacock Blue Paithani Silk Saree',
        fabric: 'Pure Silk with Tapestry Handwoven Multi-Color Mor Pallu',
        price: 52000,
        image: 'https://images.unsplash.com/photo-1555529771-7888783a18d3?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Oblique Square Kaleidoscope Zari Border',
      },
      {
        id: 'rec-2',
        name: 'Vintage Copper-Zari Banarasi Shikargah Silk',
        fabric: 'Pure Katan Silk with Antique Rose-Gold Zari',
        price: 46000,
        image: 'https://images.unsplash.com/photo-1541123437800-1bb1317badc2?auto=format&fit=crop&fm=webp&w=600&q=80',
        border: 'Continuous Persian Hunting Tapestry',
      },
    ],
  },
];

export default function TrousseauPlannerPage() {
  const { formatPrice, activeCurrency, getExchangeRateInfo } = useCurrency();
  const dispatch = useDispatch();

  // Selected saree IDs for each ceremony
  const [selections, setSelections] = useState({
    engagement: CEREMONIES[0].sarees[0],
    haldi: CEREMONIES[1].sarees[0],
    sangeet: CEREMONIES[2].sarees[0],
    muhurtham: CEREMONIES[3].sarees[0],
    reception: CEREMONIES[4].sarees[0],
  });

  // Tailoring add-on toggle per ceremony (custom blouse +₹1,499)
  const [tailoringOptions, setTailoringOptions] = useState({
    engagement: false,
    haldi: false,
    sangeet: true,
    muhurtham: true,
    reception: true,
  });

  const [addedToCartToast, setAddedToCartToast] = useState(false);

  const handleSelectSaree = (ceremonyId, saree) => {
    setSelections((prev) => ({
      ...prev,
      [ceremonyId]: saree,
    }));
  };

  const toggleTailoring = (ceremonyId) => {
    setTailoringOptions((prev) => ({
      ...prev,
      [ceremonyId]: !prev[ceremonyId],
    }));
  };

  // Calculations
  const selectedSareesList = Object.values(selections).filter(Boolean);
  const baseSareesTotal = selectedSareesList.reduce((sum, item) => sum + item.price, 0);
  const tailoringTotal = Object.values(tailoringOptions).filter(Boolean).length * 1499;
  const grandTotal = baseSareesTotal + tailoringTotal;

  const rateInfo = getExchangeRateInfo();

  const handleAddAllToCart = () => {
    selectedSareesList.forEach((saree) => {
      dispatch(
        addToCart({
          id: saree.id,
          name: saree.name,
          price: saree.price,
          image: saree.image,
          fabric: saree.fabric,
        })
      );
    });
    setAddedToCartToast(true);
    setTimeout(() => setAddedToCartToast(false), 4000);
  };

  const getWhatsAppTrousseauUrl = () => {
    let text = `Namaste SareeKart Bridal Concierge! I have curated my Royal Bridal Trousseau (${selectedSareesList.length} ceremonies):\n\n`;
    CEREMONIES.forEach((c) => {
      const chosen = selections[c.id];
      if (chosen) {
        text += `• ${c.title.toUpperCase()}: ${chosen.name} (${formatPrice(chosen.price)})\n`;
      }
    });
    text += `\nEstimated Trousseau Total: ${formatPrice(grandTotal)} (${activeCurrency.code})\n\nPlease connect me with a Senior Bridal Stylist to review fabrics, blouse measurements, and dispatch timelines.`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#17211F]">
      <SEO
        title="Royal Bridal Trousseau Planner | SareeKart"
        description="Plan and curate your complete Indian bridal trousseau across 5 ceremonies: Engagement, Haldi, Sangeet, Muhurtham, and Reception with real-time currency pricing."
      />

      {/* Hero Section */}
      <section className="relative overflow-hidden bg-[#17211F] text-white py-16 sm:py-24">
        <div className="absolute inset-0 opacity-25">
          <img
            src="https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=1600&q=80"
            alt="Bridal Silk Saree Collection"
            className="h-full w-full object-cover"
          />
        </div>
        <div className="relative section-shell max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#CBB688]/40 bg-[#CBB688]/10 px-4 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-[#F3C56A]">
            <Sparkles className="h-3.5 w-3.5" />
            <span>The Master Bridal Atelier</span>
          </div>
          <h1 className="mt-4 font-serif text-4xl font-bold sm:text-6xl text-white">
            Curate Your Sacred Wedding Drapes.
          </h1>
          <p className="mt-4 text-base sm:text-lg leading-relaxed text-[#DDD8CF] max-w-2xl mx-auto">
            From the intimate promise of your Roka to the immortal majesty of your Muhurtham, assemble your complete 5-ceremony bridal wardrobe with dedicated artisan connoisseurs.
          </p>

          {/* Currency Transparency Notice */}
          <div className="mt-6 inline-flex items-center gap-2 rounded-[8px] bg-white/10 px-4 py-2 text-xs font-bold text-[#F3C56A] backdrop-blur-xs border border-white/20">
            <span>Displaying live bridal investment in <strong>{activeCurrency.name} ({activeCurrency.code})</strong></span>
            {activeCurrency.code !== 'INR' && (
              <span className="text-white/80">• ({rateInfo.description})</span>
            )}
          </div>
        </div>
      </section>

      {/* Main Ceremony Steps */}
      <main className="section-shell py-12 sm:py-16">
        <div className="space-y-12">
          {CEREMONIES.map((ceremony) => {
            const currentSelection = selections[ceremony.id];
            const hasTailoring = tailoringOptions[ceremony.id];

            return (
              <section
                key={ceremony.id}
                className="overflow-hidden rounded-[16px] border border-[#DDD8CF] bg-white shadow-sm transition hover:shadow-md"
              >
                {/* Ceremony Header Banner */}
                <div className="bg-[#FAF8F5] border-b border-[#DDD8CF] p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#17211F] text-sm font-black text-[#F3C56A]">
                      0{ceremony.step}
                    </span>
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="font-serif text-2xl font-bold text-[#17211F]">
                          {ceremony.title}
                        </h2>
                        <CheckCircle2 className="h-5 w-5 text-[#1E6A62]" />
                      </div>
                      <p className="text-xs text-[#71817A] mt-0.5">{ceremony.tagline}</p>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="font-bold text-[#71817A]">Recommended:</span>
                    <span className="rounded-full bg-[#E3F0ED] px-3 py-1 font-bold text-[#1E6A62]">
                      {ceremony.recommendedWeave}
                    </span>
                  </div>
                </div>

                {/* Saree Options Grid */}
                <div className="p-6 sm:p-8">
                  <span className="text-xs font-black uppercase tracking-wider text-[#71817A] block mb-4">
                    Select Curated Saree for {ceremony.title}:
                  </span>

                  <div className="grid gap-6 md:grid-cols-2">
                    {ceremony.sarees.map((saree) => {
                      const isSelected = currentSelection?.id === saree.id;

                      return (
                        <div
                          key={saree.id}
                          onClick={() => handleSelectSaree(ceremony.id, saree)}
                          className={`relative flex gap-4 rounded-[12px] border p-4 cursor-pointer transition-all ${
                            isSelected
                              ? 'border-[#1E6A62] bg-[#FAF8F5] ring-2 ring-[#1E6A62] shadow-xs'
                              : 'border-[#DDD8CF] hover:border-[#1E6A62]/40 bg-white'
                          }`}
                        >
                          <div className="h-28 w-24 shrink-0 overflow-hidden rounded-[8px] bg-[#EEF3F6]">
                            <img
                              src={saree.image}
                              alt={saree.name}
                              className="h-full w-full object-cover"
                            />
                          </div>

                          <div className="flex flex-col justify-between flex-1">
                            <div>
                              <div className="flex items-start justify-between gap-2">
                                <h3 className="font-serif text-base font-bold text-[#17211F]">
                                  {saree.name}
                                </h3>
                                {isSelected && (
                                  <span className="rounded-full bg-[#1E6A62] px-2 py-0.5 text-[10px] font-black uppercase tracking-wider text-white shrink-0">
                                    Selected
                                  </span>
                                )}
                              </div>
                              <p className="mt-1 text-xs text-[#71817A]">{saree.fabric}</p>
                              <span className="mt-1 inline-block text-[11px] font-bold text-[#9B6A27]">
                                {saree.border}
                              </span>
                            </div>

                            <div className="mt-3 flex items-center justify-between">
                              <span className="text-base font-black text-[#17211F]">
                                {formatPrice(saree.price)}
                              </span>
                              <span className="text-[11px] font-bold text-[#1E6A62] hover:underline">
                                {isSelected ? '✓ In Trousseau' : '+ Select Saree'}
                              </span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Tailoring & Customization Controls */}
                  <div className="mt-6 border-t border-[#EAE6DF] pt-4 flex flex-wrap items-center justify-between gap-4 text-xs">
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="h-4 w-4 text-[#1E6A62]" />
                      <span className="font-bold text-[#4E5B56]">
                        Complimentary Fall & Pico Hand-Stitching Included
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => toggleTailoring(ceremony.id)}
                      className={`inline-flex items-center gap-1.5 rounded-[6px] border px-3 py-1.5 font-bold transition cursor-pointer ${
                        hasTailoring
                          ? 'border-[#1E6A62] bg-[#1E6A62] text-white'
                          : 'border-[#DDD8CF] bg-[#F7F4EE] text-[#17211F] hover:border-[#1E6A62]'
                      }`}
                    >
                      <Scissors className="h-3.5 w-3.5" />
                      <span>
                        {hasTailoring
                          ? `Custom Designer Blouse Added (+${formatPrice(1499)})`
                          : `Add Custom Designer Blouse (+${formatPrice(1499)})`}
                      </span>
                    </button>
                  </div>
                </div>
              </section>
            );
          })}
        </div>

        {/* Sticky Trousseau Investment Summary Bar */}
        <aside className="sticky bottom-6 mt-12 z-30 overflow-hidden rounded-[16px] border border-[#CBB688] bg-[#17211F] text-white p-6 sm:p-8 shadow-2xl backdrop-blur-md">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
            <div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-[#F3C56A] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-[#17211F]">
                  Trousseau Summary
                </span>
                <span className="text-xs font-bold text-[#DDD8CF]">
                  {selectedSareesList.length} of 5 Ceremonies Curated
                </span>
              </div>
              <div className="mt-2 flex items-baseline gap-3">
                <span className="font-serif text-3xl sm:text-4xl font-bold text-white">
                  {formatPrice(grandTotal)}
                </span>
                <span className="text-xs font-semibold text-[#DDD8CF]">
                  ({activeCurrency.code} Total Investment)
                </span>
              </div>
              <p className="mt-1 text-xs text-[#A7A19A]">
                Includes complimentary Fall & Pico on all sarees, silk mark verification, and luxury keepsake boxes.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <a
                href={getWhatsAppTrousseauUrl()}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-12 items-center justify-center gap-2 rounded-[8px] bg-[#25D366] px-6 text-xs font-black uppercase tracking-wider text-white hover:bg-[#20b858] transition shadow-md cursor-pointer"
              >
                <MessageCircle className="h-4 w-4" />
                <span>Send to Bridal Concierge</span>
              </a>

              <button
                type="button"
                onClick={handleAddAllToCart}
                className="flex h-12 items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition shadow-md cursor-pointer"
              >
                <ShoppingBag className="h-4 w-4 text-[#F3C56A]" />
                <span>Add 5 Sarees to Bag</span>
              </button>

              <Link
                to="/stores"
                className="flex h-12 items-center justify-center gap-2 rounded-[8px] border border-white/30 bg-white/10 px-5 text-xs font-bold text-white hover:bg-white/20 transition cursor-pointer"
              >
                <Compass className="h-4 w-4" />
                <span>Book In-Store VIP Salon</span>
              </Link>
            </div>
          </div>
        </aside>

        {/* Toast Notification when adding all to bag */}
        {addedToCartToast && (
          <div className="fixed top-24 right-6 z-50 rounded-[8px] border border-emerald-300 bg-emerald-900 text-white p-4 shadow-xl flex items-center gap-3">
            <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0" />
            <div className="text-xs">
              <p className="font-bold">Bridal Trousseau added to your shopping bag!</p>
              <Link to="/checkout" className="underline font-black mt-0.5 inline-block text-emerald-200">
                Proceed to Checkout →
              </Link>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
