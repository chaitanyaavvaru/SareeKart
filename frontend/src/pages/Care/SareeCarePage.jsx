import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Sparkles,
  RefreshCw,
  Sun,
  AlertTriangle,
  MessageCircle,
  Scissors,
  CheckCircle2,
  XCircle,
  HelpCircle,
  ArrowRight,
} from 'lucide-react';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl } from '../../utils/seoUtils';

const CARE_COMMANDMENTS = [
  {
    step: '01',
    title: 'Wrap Exclusively in Pure Muslin or Cotton',
    short: 'Avoid plastic covers entirely.',
    desc: 'Pure silver and gold zari needs to breathe. Plastic or polyester bags trap atmospheric humidity, releasing chemical fumes that oxidize real silver into blackened silver sulfide. Always wrap heirlooms in unbleached white mulmul or pure cotton saree bags.',
    dos: ['Wrap in breathable 100% muslin cloth', 'Store in dry dark wooden wardrobes', 'Use acid-free butter paper between heavy pallu folds'],
    donts: ['Never store in plastic or PVC zip covers', 'Never hang heavy silk sarees on wire hangers (causes shoulder sagging)'],
  },
  {
    step: '02',
    title: 'The Quarterly Refolding Ritual',
    short: 'Change fold lines every 3 to 4 months.',
    desc: 'Real zari threads are composed of delicate silver wire wound around silk filament. Leaving a heavy saree folded in the exact same manner for over 6 months puts extreme mechanical strain on the crease, leading to permanent cuts along the metallic threads.',
    dos: ['Take your sarees out every 3-4 months', 'Refold along opposite diagonals', 'Keep the zari inwards to prevent snagging against raw embroidery'],
    donts: ['Never press hard against creases with your hands', 'Never stack more than 3-4 heavy silks directly on top of each other'],
  },
  {
    step: '03',
    title: 'Shield Zari from Perfumes & Deodorants',
    short: 'Apply fragrances before draping.',
    desc: 'Modern perfumes, hairsprays, and deodorants contain alcohol and synthetic aldehydes that instantly react with pure silver wire, causing immediate discoloration and tarnishing. Always apply perfume, body mists, and makeup at least 15 minutes before draping your saree.',
    dos: ['Allow body lotion and perfume to dry completely first', 'Wear fabric dress-shields under tight blouses to absorb sweat'],
    donts: ['Never spray perfume directly on zari borders or pallu', 'Never iron directly over zari without a protective cotton press cloth'],
  },
  {
    step: '04',
    title: 'Professional Petrol Dry Cleaning Only',
    short: 'Never wash pure handlooms in domestic water.',
    desc: 'Natural silk fibers shrink and lose their natural sericin protein luster when immersed in water. Always entrust your pure silks and bridal heirlooms exclusively to professional dry cleaners specializing in petrol dry cleaning for ethnic handlooms.',
    dos: ['Instruct dry cleaner to use hydrocarbon/petrol solvent', 'Dry clean only once after every 2-3 wedding wearings unless stained'],
    donts: ['Never machine wash or hand wash in buckets', 'Never use domestic stain removers or bleaching agents'],
  },
  {
    step: '05',
    title: 'Natural Neem Leaves Over Naphthalene',
    short: 'Prevent chemical contact with silver.',
    desc: 'Naphthalene balls emit sulfur gases that speed up the tarnishing of pure zari. Instead, place dried neem leaves wrapped in small breathable muslin pouches or cedarwood aromatics at the corners of your wardrobe away from direct contact with the fabrics.',
    dos: ['Use dried neem leaves wrapped in muslin', 'Place silica gel sachets in cupboards to control monsoon humidity'],
    donts: ['Never let naphthalene or camphor touch zari directly', 'Never leave damp sarees in closed cupboards after outdoor weddings'],
  },
  {
    step: '06',
    title: 'Gentle Airing in Morning Shade',
    short: 'Sunlight fades natural silk dyes.',
    desc: 'After an evening occasion, do not immediately pack your saree away. Air it out on a padded clothes rack in a well-ventilated room or under early morning indirect shade for 2-3 hours to allow any body heat and moisture to dissipate naturally.',
    dos: ['Air in cool, shaded room for 2-3 hours', 'Store only after the fabric feels completely cool and dry'],
    donts: ['Never expose pure silk to harsh direct afternoon sunlight', 'Never store while damp with perspiration'],
  },
];

const EMERGENCY_STAIN_GUIDE = [
  {
    stain: 'Haldi / Turmeric / Curry',
    action: 'Dab (never rub!) immediately with a clean, dry white cotton cloth to absorb excess liquid. Sprinkle talcum powder to absorb oils. Take immediately to an artisanal dry cleaner within 24 hours.',
  },
  {
    stain: 'Tea, Coffee & Beverages',
    action: 'Place an absorbent cotton napkin underneath the stain. Gently blot from the top using a cloth lightly dampened with cold distilled water. Do not apply domestic soap.',
  },
  {
    stain: 'Oil or Ghee Drops',
    action: 'Heavily dust the oily spot with cornstarch or talcum powder. Allow it to sit for 30 minutes to draw out the grease, then gently brush away with a soft bristle brush before professional cleaning.',
  },
  {
    stain: 'Water Spots on Silk',
    action: 'Pure silks can develop water ring marks. Cover with a clean muslin cloth and steam iron gently from a 2-inch distance without touching the iron plate directly to the silk.',
  },
];

export default function SareeCarePage() {
  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#17211F]">
      <SEO
        title="Pure Silk & Heirloom Saree Care Guide | SareeKart"
        description="Master artisan guide to preserving heirloom pure silk and zari sarees. Expert advice on muslin wrapping, zari refolding, stain emergencies, and restoration."
        canonical={getCanonicalUrl('/saree-care')}
      />

      {/* Hero */}
      <section className="relative overflow-hidden bg-[#17211F] text-white py-16 sm:py-24">
        <div className="absolute inset-0 opacity-25">
          <img
            src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1600&q=80"
            alt="Handloom silk preservation"
            className="h-full w-full object-cover"
          />
        </div>
        <div className="relative section-shell max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#CBB688]/40 bg-[#CBB688]/10 px-4 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-[#F3C56A]">
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>Artisanal Longevity Standard</span>
          </div>
          <h1 className="mt-4 font-serif text-4xl font-bold sm:text-6xl text-white">
            Preserving Heirloom Drapes.
          </h1>
          <p className="mt-4 text-base sm:text-lg leading-relaxed text-[#DDD8CF] max-w-2xl mx-auto">
            A handcrafted pure silk saree is an heirloom woven to last across generations. Follow our master weavers’ ritual care commandments to keep your zari gleaming forever.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <a
              href="https://wa.me/919059564499?text=Namaste%20SareeKart!%20I%20have%20an%20inquiry%20regarding%20saree%20care%20or%20heirloom%20zari%20restoration."
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition-all shadow-md"
            >
              <MessageCircle className="h-4 w-4 text-[#F3C56A]" />
              <span>Contact Textile Restoration Concierge</span>
            </a>
            <Link
              to="/stores"
              className="inline-flex items-center gap-2 rounded-[8px] border border-[#DDD8CF]/40 bg-white/10 px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-white/20 transition-all"
            >
              <span>Visit Flagship Boutique Appraisal</span>
            </Link>
          </div>
        </div>
      </section>

      {/* Main Commandments */}
      <main className="section-shell py-12 sm:py-16">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-[#9B6A27]">
            The 6 Longevity Commandments
          </p>
          <h2 className="mt-2 font-serif text-3xl sm:text-4xl font-bold text-[#17211F]">
            How to Care for Real Zari & Pure Silk.
          </h2>
          <p className="mt-2 text-sm text-[#71817A]">
            Rooted in 80 years of traditional weaver wisdom, these practices ensure your bridal silks remain as lustrous for your daughter as they were on your wedding day.
          </p>
        </div>

        <div className="grid gap-8 lg:grid-cols-2">
          {CARE_COMMANDMENTS.map((rule) => (
            <article
              key={rule.step}
              className="rounded-[16px] border border-[#DDD8CF] bg-white p-6 sm:p-8 shadow-xs flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-black text-[#1E6A62] bg-[#E3F0ED] px-2.5 py-1 rounded-[6px]">
                    RULE {rule.step}
                  </span>
                  <span className="text-xs font-bold text-[#9B6A27]">{rule.short}</span>
                </div>

                <h3 className="mt-4 font-serif text-xl sm:text-2xl font-bold text-[#17211F]">
                  {rule.title}
                </h3>
                <p className="mt-3 text-xs sm:text-sm leading-relaxed text-[#4E5B56]">
                  {rule.desc}
                </p>

                <div className="mt-6 grid gap-4 sm:grid-cols-2 border-t border-[#EAE6DF] pt-4 text-xs">
                  <div>
                    <span className="font-black uppercase tracking-wider text-emerald-800 flex items-center gap-1.5 mb-2">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" /> Recommended (Do's)
                    </span>
                    <ul className="space-y-1.5 text-[#4E5B56]">
                      {rule.dos.map((d) => (
                        <li key={d} className="flex items-start gap-1.5">
                          <span className="text-emerald-600 font-bold">•</span>
                          <span>{d}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div>
                    <span className="font-black uppercase tracking-wider text-red-800 flex items-center gap-1.5 mb-2">
                      <XCircle className="h-4 w-4 text-red-600 shrink-0" /> Avoid (Don'ts)
                    </span>
                    <ul className="space-y-1.5 text-[#4E5B56]">
                      {rule.donts.map((d) => (
                        <li key={d} className="flex items-start gap-1.5">
                          <span className="text-red-600 font-bold">•</span>
                          <span>{d}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>

        {/* Emergency Stain Treatment Guide */}
        <section className="mt-16 rounded-[16px] border border-[#DDD8CF] bg-white p-8 sm:p-12 shadow-xs">
          <div className="text-center max-w-2xl mx-auto">
            <div className="inline-flex items-center gap-2 rounded-full bg-amber-50 border border-amber-200 px-3 py-1 text-xs font-black text-amber-800">
              <AlertTriangle className="h-3.5 w-3.5 text-amber-600" />
              <span>Wedding Day First Aid</span>
            </div>
            <h2 className="mt-3 font-serif text-3xl font-bold text-[#17211F]">
              Emergency Stain Response Guide.
            </h2>
            <p className="mt-2 text-sm text-[#71817A]">
              Accidents happen at joyous celebrations. Never panic, never rub with water, and follow these instantaneous actions.
            </p>
          </div>

          <div className="mt-10 grid gap-6 sm:grid-cols-2">
            {EMERGENCY_STAIN_GUIDE.map((item) => (
              <div
                key={item.stain}
                className="rounded-[12px] border border-[#EAE6DF] bg-[#FAF8F5] p-5"
              >
                <h3 className="font-serif text-lg font-bold text-[#17211F] flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-[#9B6A27]" />
                  {item.stain}
                </h3>
                <p className="mt-2 text-xs sm:text-sm text-[#4E5B56] leading-relaxed">
                  {item.action}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* Heirloom Restoration Concierge Banner */}
        <section className="mt-16 rounded-[16px] border border-[#CBB688] bg-gradient-to-r from-[#17211F] to-[#1E6A62] text-white p-8 sm:p-12 shadow-lg">
          <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr] lg:items-center">
            <div>
              <span className="text-xs font-black uppercase tracking-[0.2em] text-[#F3C56A]">
                Preservation & Repair Service
              </span>
              <h2 className="mt-2 font-serif text-3xl sm:text-4xl font-bold text-white">
                Have an Antique Family Saree Needing Restoration?
              </h2>
              <p className="mt-3 text-sm leading-relaxed text-[#DDD8CF] max-w-xl">
                Our master weaver guild offers specialized services for vintage heirlooms: real silver zari chemical re-polishing, torn korvai border reconstruction, fall replacement, and fabric relining.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row lg:flex-col gap-3 justify-end">
              <a
                href="https://wa.me/919059564499?text=Namaste%20SareeKart!%20I%20would%20like%20to%20consult%20your%20textile%20artisan%20team%20for%20antique%20saree%20restoration."
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-[8px] bg-[#25D366] px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-[#20b858] transition shadow-md"
              >
                <MessageCircle className="h-4 w-4" />
                <span>Consult Master Weaver on WhatsApp</span>
              </a>

              <Link
                to="/stores"
                className="inline-flex items-center justify-center gap-2 rounded-[8px] border border-white/30 bg-white/10 px-6 py-3.5 text-xs font-bold text-white hover:bg-white/20 transition"
              >
                <span>Bring to Flagship Boutique</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
