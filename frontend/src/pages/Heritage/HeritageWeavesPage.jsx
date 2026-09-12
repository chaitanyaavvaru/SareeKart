import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Award,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Compass,
  Clock,
  MapPin,
  Layers,
  BookOpen,
} from 'lucide-react';
import SEO from '../../components/common/SEO';

const WEAVES_DATA = [
  {
    id: 'kanchipuram',
    name: 'Kanchipuram Silk',
    state: 'Tamil Nadu',
    giTag: 'GI-01 (Certified 2005)',
    duration: '20 – 45 Days per Saree',
    technique: 'Three-Shuttle Korvai & Petni Interlocking',
    zariType: 'Pure Silver Wire Dipped in 24K Gold (0.6% Gold Purity)',
    image: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Woven in the temple city of Kanchipuram, renowned for heavy Mulberry silk and the legendary Korvai technique where the contrasting borders and pallu are woven separately on three shuttles and interlocked with zig-zag temple petni stitches so strong the fabric will never tear at the joint.',
    motifs: ['Temple Gopuram', 'Mayil (Peacock)', 'Rudraksham', 'Yali (Mythical Beast)'],
    searchQuery: 'Kanchipuram',
  },
  {
    id: 'banarasi',
    name: 'Banarasi Katan & Shikargah',
    state: 'Uttar Pradesh',
    giTag: 'GI-99 (Certified 2009)',
    duration: '30 – 90 Days per Saree',
    technique: 'Kadwa & Fekwa Handloom Weft Brocade',
    zariType: 'Real Tested Silver Zari & Antique Antique Munga Zari',
    image: 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Born along the holy ghats of Varanasi, Banarasi sarees reflect Mughal royal opulence. In the intricate Kadwa technique, every single floral boota is engraved by hand without loose float threads behind the fabric. The Shikargah represents antique royal hunting scenes woven into golden jaals.',
    motifs: ['Shikargah Hunt', 'Kalka (Paisley)', 'Jhallar Fringe', 'Bel (Floral Creeper)'],
    searchQuery: 'Banarasi',
  },
  {
    id: 'patola',
    name: 'Patan Patola Double Ikat',
    state: 'Gujarat',
    giTag: 'GI-232 (Certified 2013)',
    duration: '6 – 12 Months per Saree',
    technique: 'Double Ikat Warp & Weft Resist Dyeing',
    zariType: 'Natural Plant & Mineral Dyes with Fine Silk',
    image: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'One of the rarest textile art forms on earth, masterfully preserved by only a handful of Salvi artisan families in Patan. Both the warp and weft threads are individually tied and resist-dyed before weaving, creating identical geometrical perfection on both front and back.',
    motifs: ['Nari Kunj (Puppet)', 'Pan Bhat (Betel Leaf)', 'Navratna (9 Jewels)', 'Popat (Parrot)'],
    searchQuery: 'Patola',
  },
  {
    id: 'paithani',
    name: 'Paithani Royal Silk',
    state: 'Maharashtra',
    giTag: 'GI-85 (Certified 2010)',
    duration: '25 – 60 Days per Saree',
    technique: 'Tapestry Weaving with Oblique Square Border',
    zariType: 'Pure Silver Zari Wire with Royal Silk Yarn',
    image: 'https://images.unsplash.com/photo-1609357605129-26f69add5d6e?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Favored by the Maratha royalty and Peshwas, Paithani sarees feature brilliant oblique square designs with kaleidoscope multi-color borders. The pallu is crafted using tapestry weaving where colored silk wefts form radiant peacocks and parakeets perched upon golden trees.',
    motifs: ['Mor (Peacock)', 'Tota-Maina (Parrots)', 'Asavali (Flower Vase)', 'Narali (Coconut)'],
    searchQuery: 'Paithani',
  },
  {
    id: 'uppada',
    name: 'Uppada Jamdani',
    state: 'Andhra Pradesh',
    giTag: 'GI-122 (Certified 2009)',
    duration: '15 – 35 Days per Saree',
    technique: 'Non-Structural Extra Weft Jamdani Weaving',
    zariType: 'Gossamer Fine Zari on Translucent Silk',
    image: 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Hailing from the coastal village of Uppada, this gossamer-light weave uses non-structural extra wefts placed by hand with bamboo needles. The result is a feather-weight drape where intricate golden zari motifs seem to float suspended inside luminous organza-like silk.',
    motifs: ['Floating Jamdani Florals', 'Geometric Georgette Jaal', 'Kaddi Zari Border'],
    searchQuery: 'Uppada',
  },
  {
    id: 'gadwal',
    name: 'Gadwal Handloom Silk & Cotton',
    state: 'Telangana',
    giTag: 'GI-139 (Certified 2010)',
    duration: '18 – 30 Days per Saree',
    technique: 'Kuttu Seaming of Cotton Body & Pure Silk Zari Border',
    zariType: 'Heavy Tussar Zari with Pure Combed Cotton',
    image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Celebrated for the architectural genius of combining a breathable, feather-light unstarched cotton body with sumptuous, heavy pure silk borders and pallu. The fabrics are seamlessly married on the loom using the ancient Kuttu interlocking technique.',
    motifs: ['Kuttu Temple Pyramids', 'Kodi Rekha Borders', 'Rudra Rudraksha Motifs'],
    searchQuery: 'Gadwal',
  },
  {
    id: 'pochampally',
    name: 'Pochampally Ikat (Pagdu Bandhu)',
    state: 'Telangana',
    giTag: 'GI-04 (Certified 2005)',
    duration: '15 – 25 Days per Saree',
    technique: 'Precision Geometric Tie-and-Dye (Chitki)',
    zariType: 'Silk Warp with Delicate Metallic Highlights',
    image: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'Known as the Silk City of India, Bhoodan Pochampally weaves hypnotic geometric precision. The yarn is mathematically grouped, tied, and dip-dyed multiple times so that when aligned on the loom, crisp chevron, diamond, and bird motifs emerge naturally.',
    motifs: ['Diamond Lattice', 'Elephant & Parrot Ikat', 'Chevron Zig-Zag Bands'],
    searchQuery: 'Ikat',
  },
  {
    id: 'ponduru',
    name: 'Ponduru Organic Fine Khadi',
    state: 'Andhra Pradesh',
    giTag: 'GI Proposed (Heritage Cluster)',
    duration: '30 – 60 Days per Saree',
    technique: 'Two-Spindle Charkha Handspun Organic Cotton',
    zariType: 'Pure Organic Cotton with Fine Zari Edge',
    image: 'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&fm=webp&w=800&q=80',
    story: 'The finest organic cotton in the world, crafted from short-staple red hill cotton cleaned with the jawbone of the local Valuga fish and handspun on wooden charkhas to achieve an astonishing 100 to 120 count softness that captivated Mahatma Gandhi.',
    motifs: ['Kumbham Borders', 'Plain Minimalist Weave', 'Handspun Micro Zari Lines'],
    searchQuery: 'cotton',
  },
];

const MOTIF_SYMBOLISM = [
  {
    name: 'Mayil (The Sacred Peacock)',
    meaning: 'Royalty, celestial beauty, and celebration of prosperity. In Tamil and Sanskrit mythology, the peacock is the vahana of Lord Muruga, invoking divine grace upon the bride.',
  },
  {
    name: 'Rudraksham (Sacred Bead Motif)',
    meaning: 'Spiritual sanctity, inner peace, and divine protection. Rows of geometric rudraksha beads are traditionally woven alongside Kanchipuram and Gadwal borders.',
  },
  {
    name: 'Temple Gopuram (Pyramidal Spire)',
    meaning: 'Dravidian temple architecture rising towards the heavens. Interlocked using the petni technique to represent sacred boundaries and timeless endurance.',
  },
  {
    name: 'Shikargah (Royal Forest Hunt)',
    meaning: 'Antique Persian and Mughal miniature paintings translated onto silk. Depicts hunters on horseback, royal tigers, deer, and flowering trees in continuous gold jaal.',
  },
  {
    name: 'Hamsa (The Discerning Swan)',
    meaning: 'Wisdom, grace, and discernment. Mythologically believed to separate milk from water, symbolizing a pure mind capable of choosing virtue and beauty.',
  },
  {
    name: 'Kalka / Ashavali (The Eternal Paisley)',
    meaning: 'The mango or Persian cypress bud representing fertility, eternal life, and the bounty of nature across Banarasi and Paithani masterworks.',
  },
];

export default function HeritageWeavesPage() {
  const [activeWeaveId, setActiveWeaveId] = useState(WEAVES_DATA[0].id);

  const activeWeave =
    WEAVES_DATA.find((w) => w.id === activeWeaveId) || WEAVES_DATA[0];

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#17211F]">
      <SEO
        title="Heritage Weaves of India & Motif Symbolism | SareeKart"
        description="Explore 8 GI-certified Indian handloom clusters: Kanchipuram, Banarasi, Patola, Paithani, Uppada, Gadwal, Pochampally, and Ponduru Khadi with authentic motif symbolism."
      />

      {/* Hero */}
      <section className="relative overflow-hidden bg-[#17211F] text-white py-16 sm:py-24">
        <div className="absolute inset-0 opacity-25">
          <img
            src="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1600&q=80"
            alt="Handloom Weave Background"
            className="h-full w-full object-cover"
          />
        </div>
        <div className="relative section-shell max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#CBB688]/40 bg-[#CBB688]/10 px-4 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-[#F3C56A]">
            <BookOpen className="h-3.5 w-3.5" />
            <span>Living Handloom Encyclopedia</span>
          </div>
          <h1 className="mt-4 font-serif text-4xl font-bold sm:text-6xl text-white">
            The Heritage Weaves of India.
          </h1>
          <p className="mt-4 text-base sm:text-lg leading-relaxed text-[#DDD8CF] max-w-2xl mx-auto">
            From the sacred ghats of Varanasi to the temple town of Kanchipuram, journey through 8 legendary handloom clusters preserved by master weavers across generations.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/products"
              className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition-all shadow-md"
            >
              <span>Explore Entire Catalog</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
            <button
              type="button"
              onClick={() => window.dispatchEvent(new CustomEvent('open-silk-mark'))}
              className="inline-flex items-center gap-2 rounded-[8px] border border-[#DDD8CF]/40 bg-white/10 px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-white/20 transition-all cursor-pointer"
            >
              <ShieldCheck className="h-4 w-4 text-[#F3C56A]" />
              <span>Silk Mark Purity Standard</span>
            </button>
          </div>
        </div>
      </section>

      {/* Main Interactive Weave Explorer */}
      <main className="section-shell py-12 sm:py-16">
        {/* Weave Tabs */}
        <div className="flex gap-2 overflow-x-auto no-scrollbar border-b border-[#DDD8CF] pb-4">
          {WEAVES_DATA.map((weave) => (
            <button
              key={weave.id}
              type="button"
              onClick={() => setActiveWeaveId(weave.id)}
              className={`rounded-full px-5 py-2.5 text-xs font-black transition-all shrink-0 cursor-pointer ${
                activeWeaveId === weave.id
                  ? 'bg-[#17211F] text-white shadow-xs'
                  : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:border-[#1E6A62] hover:text-[#17211F]'
              }`}
            >
              {weave.name}
            </button>
          ))}
        </div>

        {/* Active Weave Card Showcase */}
        <article className="mt-8 overflow-hidden rounded-[16px] border border-[#DDD8CF] bg-white shadow-sm">
          <div className="grid gap-8 lg:grid-cols-2">
            {/* Image Showcase */}
            <div className="relative min-h-[380px] lg:min-h-[500px] overflow-hidden bg-[#EEF3F6]">
              <img
                src={activeWeave.image}
                alt={activeWeave.name}
                className="h-full w-full object-cover"
              />
              <div className="absolute top-4 left-4 flex flex-col gap-2">
                <span className="rounded-full bg-[#17211F]/90 px-3 py-1 text-[11px] font-black uppercase tracking-wider text-[#F3C56A] backdrop-blur-xs">
                  {activeWeave.state}
                </span>
                <span className="rounded-full bg-emerald-700/90 px-3 py-1 text-[11px] font-bold text-white backdrop-blur-xs flex items-center gap-1.5">
                  <Award className="h-3.5 w-3.5" />
                  {activeWeave.giTag}
                </span>
              </div>
            </div>

            {/* Content & Specs */}
            <div className="p-6 sm:p-10 flex flex-col justify-between">
              <div>
                <span className="text-xs font-black uppercase tracking-[0.2em] text-[#9B6A27]">
                  Master Guild Provenance
                </span>
                <h2 className="mt-1 font-serif text-3xl sm:text-4xl font-bold text-[#17211F]">
                  {activeWeave.name}
                </h2>
                <p className="mt-4 text-sm leading-relaxed text-[#4E5B56]">
                  {activeWeave.story}
                </p>

                {/* Specs Grid */}
                <div className="mt-6 grid gap-4 rounded-[12px] border border-[#EAE6DF] bg-[#FAF8F5] p-5 text-xs">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] flex items-center gap-1">
                        <Layers className="h-3 w-3 text-[#1E6A62]" /> Weaving Technique
                      </span>
                      <p className="font-bold text-[#17211F] mt-0.5">{activeWeave.technique}</p>
                    </div>
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] flex items-center gap-1">
                        <Clock className="h-3 w-3 text-[#1E6A62]" /> Loom Duration
                      </span>
                      <p className="font-bold text-[#17211F] mt-0.5">{activeWeave.duration}</p>
                    </div>
                  </div>

                  <div className="border-t border-[#EAE6DF] pt-3">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] flex items-center gap-1">
                      <Sparkles className="h-3 w-3 text-[#9B6A27]" /> Zari Composition & Material
                    </span>
                    <p className="font-bold text-[#17211F] mt-0.5">{activeWeave.zariType}</p>
                  </div>
                </div>

                {/* Signature Motifs */}
                <div className="mt-6">
                  <span className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                    Signature Cultural Motifs:
                  </span>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {activeWeave.motifs.map((motif) => (
                      <span
                        key={motif}
                        className="rounded-full border border-[#CBB688] bg-[#F3E6C7]/30 px-3 py-1 text-xs font-bold text-[#9B6A27]"
                      >
                        {motif}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Action Button */}
              <div className="mt-8 border-t border-[#EAE6DF] pt-6 flex flex-wrap items-center gap-4">
                <Link
                  to={`/products?search=${encodeURIComponent(activeWeave.searchQuery)}`}
                  className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition shadow-xs"
                >
                  <span>Explore {activeWeave.name} Sarees</span>
                  <ArrowRight className="h-4 w-4" />
                </Link>

                <button
                  type="button"
                  onClick={() => window.dispatchEvent(new CustomEvent('open-video-shopping', { detail: { name: activeWeave.name } }))}
                  className="inline-flex items-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-white px-5 py-3.5 text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] transition cursor-pointer"
                >
                  <span>Book Drape Consultation</span>
                </button>
              </div>
            </div>
          </div>
        </article>

        {/* Motif Symbolism Guide */}
        <section className="mt-20">
          <div className="text-center max-w-2xl mx-auto">
            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#9B6A27]">
              Iconography & Mythology
            </p>
            <h2 className="mt-2 font-serif text-3xl font-bold text-[#17211F]">
              The Language of Indian Motifs.
            </h2>
            <p className="mt-2 text-sm text-[#71817A]">
              Every curve of zari is an ancient spiritual benediction. Understand the sacred stories woven into your bridal and festive drapes.
            </p>
          </div>

          <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {MOTIF_SYMBOLISM.map((item) => (
              <div
                key={item.name}
                className="rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-xs hover:border-[#1E6A62] transition"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#9B6A27]/10 text-[#9B6A27]">
                  <Sparkles className="h-5 w-5" />
                </div>
                <h3 className="mt-4 font-serif text-lg font-bold text-[#17211F]">
                  {item.name}
                </h3>
                <p className="mt-2 text-xs leading-relaxed text-[#71817A]">
                  {item.meaning}
                </p>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
