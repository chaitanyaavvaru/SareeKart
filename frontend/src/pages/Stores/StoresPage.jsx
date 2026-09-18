import { useState } from 'react';
import {
  MapPin,
  Clock,
  Phone,
  MessageCircle,
  ExternalLink,
  Sparkles,
  ShieldCheck,
  Coffee,
  Car,
  Scissors,
  Award,
  Video,
  ChevronRight,
} from 'lucide-react';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl } from '../../utils/seoUtils';
import InStoreAppointmentModal, { BOUTIQUE_LOCATIONS } from '../../components/stores/InStoreAppointmentModal';

const BOUTIQUE_DETAILS = [
  {
    ...BOUTIQUE_LOCATIONS[0],
    image: 'https://images.unsplash.com/photo-1541123437800-1bb1317badc2?auto=format&fit=crop&fm=webp&w=800&q=80',
    description: 'A two-story palatial sanctuary dedicated to royal South Indian weaves, bridal muhurtham silk curation, and private bridal dressing suites.',
    curation: ['Bridal Kanchipuram', 'Rare Korvai Silk', 'Pure Zari Brocades'],
    leadStylist: 'Smt. Gayatri Devi (Master Drape Connoisseur)',
  },
  {
    ...BOUTIQUE_LOCATIONS[1],
    image: 'https://images.unsplash.com/photo-1555529771-7888783a18d3?auto=format&fit=crop&fm=webp&w=800&q=80',
    description: 'Nestled in historic Jayanagar, featuring a curated handloom gallery with authentic loom shuttles, silk burn-test counters, and quiet drape lounges.',
    curation: ['Mysore Silk Georgette', 'Kanchipuram Silk', 'Pochampally Ikat'],
    leadStylist: 'Sri Raghavendra K. (Silk Mark Certified Evaluator)',
  },
  {
    ...BOUTIQUE_LOCATIONS[2],
    image: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&fm=webp&w=800&q=80',
    description: 'Our northern flagship showcasing heritage Banarasi Shikargah, antique Kadwa brocades, and Patan Patola double ikats with natural dyes.',
    curation: ['Banarasi Katan', 'Patan Patola', 'Jamdani Muslin'],
    leadStylist: 'Smt. Vandana Mishra (Textile Historian)',
  },
  {
    ...BOUTIQUE_LOCATIONS[3],
    image: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&fm=webp&w=800&q=80',
    description: 'Our legacy coastal heritage center in Waltair Uplands, displaying traditional Uppada gossamer silks, Ponduru organic khadi, and royal trousseaus.',
    curation: ['Uppada Jamdani', 'Ponduru Khadi', 'Bridal Silks'],
    leadStylist: 'Smt. Lakshmi Sarada (Artisan Guild Mentor)',
  },
  {
    ...BOUTIQUE_LOCATIONS[4],
    image: 'https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5?auto=format&fit=crop&fm=webp&w=800&q=80',
    description: 'A grand bridal destination on MG Road catering to multi-generation Telugu wedding preparations with dedicated family viewing salons.',
    curation: ['Gadwal Handlooms', 'Bridal Pattu', 'Venkatagiri Zari'],
    leadStylist: 'Sri Venkateswara Rao (Weave Specialist)',
  },
];

const AMENITIES = [
  { icon: Sparkles, title: 'Private Bridal Suites', desc: 'Spacious salons for brides and families to try draped looks in natural lighting.' },
  { icon: Award, title: 'Certified Silk Mark Station', desc: 'On-site purity verification with microscopic thread analysis and burn test assurance.' },
  { icon: Scissors, title: 'On-Site Tailoring Atelier', desc: 'Master blouse measurement specialists and same-day express Fall & Pico edging.' },
  { icon: Car, title: 'Valet Parking', desc: 'Complimentary secure valet parking at all boutique locations.' },
  { icon: Coffee, title: 'Royal Refreshments', desc: 'Freshly brewed Kumbakonam degree filter coffee and herbal infusions.' },
  { icon: ShieldCheck, title: 'Lifetime Zari Appraisal', desc: 'Bring your family heirlooms for expert silver/gold zari purity assessment.' },
];

export default function StoresPage() {
  const [selectedCity, setSelectedCity] = useState('All');
  const [appointmentModalOpen, setAppointmentModalOpen] = useState(false);
  const [selectedBoutiqueId, setSelectedBoutiqueId] = useState(null);

  const cities = ['All', 'Hyderabad', 'Bengaluru', 'New Delhi', 'Visakhapatnam', 'Vijayawada'];

  const filteredStores =
    selectedCity === 'All'
      ? BOUTIQUE_DETAILS
      : BOUTIQUE_DETAILS.filter((s) => s.city === selectedCity);

  const handleBookAppointment = (boutiqueId) => {
    setSelectedBoutiqueId(boutiqueId);
    setAppointmentModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#17211F]">
      <SEO
        title="Our Flagship Boutiques | SareeKart"
        description="Experience heirloom handloom sarees in person. Visit SareeKart flagship boutiques across Hyderabad, Bengaluru, New Delhi, Visakhapatnam, and Vijayawada."
        canonical={getCanonicalUrl('/stores')}
      />

      {/* Hero Section */}
      <section className="relative overflow-hidden bg-[#17211F] text-white py-16 sm:py-24">
        <div className="absolute inset-0 opacity-20">
          <img
            src="https://images.unsplash.com/photo-1541123437800-1bb1317badc2?auto=format&fit=crop&fm=webp&w=1600&q=80"
            alt="Flagship Boutique Interior"
            className="h-full w-full object-cover"
          />
        </div>
        <div className="relative section-shell max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#CBB688]/40 bg-[#CBB688]/10 px-4 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-[#F3C56A]">
            <MapPin className="h-3.5 w-3.5" />
            <span>5 Flagship Boutiques Across India</span>
          </div>
          <h1 className="mt-4 font-serif text-4xl font-bold sm:text-6xl text-white">
            Where Indian Heritage Comes Alive.
          </h1>
          <p className="mt-4 text-base sm:text-lg leading-relaxed text-[#DDD8CF] max-w-2xl mx-auto">
            Step into our private salons to feel the genuine weight of mulberry silk, hear the rustle of real zari brocade, and drape heirlooms with master saree connoisseurs.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <button
              type="button"
              onClick={() => handleBookAppointment(null)}
              className="inline-flex items-center gap-2 rounded-[8px] bg-[#1E6A62] px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition-all shadow-md cursor-pointer"
            >
              <Sparkles className="h-4 w-4 text-[#F3C56A]" />
              Book In-Store VIP Drape Appointment
            </button>
            <button
              type="button"
              onClick={() => window.dispatchEvent(new CustomEvent('open-video-shopping'))}
              className="inline-flex items-center gap-2 rounded-[8px] border border-[#DDD8CF]/40 bg-white/10 px-6 py-3.5 text-xs font-black uppercase tracking-wider text-white hover:bg-white/20 transition-all cursor-pointer"
            >
              <Video className="h-4 w-4 text-[#F3C56A]" />
              Virtual Video Drape Instead
            </button>
          </div>
        </div>
      </section>

      {/* City Filter Navigation */}
      <section className="sticky top-[76px] sm:top-[84px] z-20 border-b border-[#DDD8CF] bg-white shadow-xs">
        <div className="section-shell flex items-center justify-between overflow-x-auto py-3 gap-2 no-scrollbar">
          <div className="flex items-center gap-2">
            <span className="text-xs font-black uppercase tracking-wider text-[#71817A] mr-2 shrink-0">
              Filter City:
            </span>
            {cities.map((city) => (
              <button
                key={city}
                type="button"
                onClick={() => setSelectedCity(city)}
                className={`rounded-full px-4 py-1.5 text-xs font-black transition-all shrink-0 cursor-pointer ${
                  selectedCity === city
                    ? 'bg-[#17211F] text-white shadow-xs'
                    : 'bg-[#F7F4EE] text-[#71817A] hover:bg-[#EAE6DF] hover:text-[#17211F]'
                }`}
              >
                {city}
              </button>
            ))}
          </div>
          <span className="hidden sm:inline-block text-xs font-bold text-[#71817A] shrink-0">
            Showing {filteredStores.length} {filteredStores.length === 1 ? 'Boutique' : 'Boutiques'}
          </span>
        </div>
      </section>

      {/* Boutiques Directory */}
      <main className="section-shell py-12 sm:py-16">
        <div className="grid gap-8 lg:grid-cols-2">
          {filteredStores.map((boutique) => (
            <article
              key={boutique.id}
              className="group overflow-hidden rounded-[12px] border border-[#DDD8CF] bg-white shadow-xs hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                {/* Image */}
                <div className="relative h-64 overflow-hidden bg-[#EEF3F6]">
                  <img
                    src={boutique.image}
                    alt={boutique.name}
                    className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute top-4 left-4 flex gap-2">
                    <span className="rounded-full bg-[#17211F]/90 px-3 py-1 text-[11px] font-black uppercase tracking-wider text-[#F3C56A] backdrop-blur-xs">
                      {boutique.city}
                    </span>
                    <span className="rounded-full bg-emerald-700/90 px-3 py-1 text-[11px] font-bold text-white backdrop-blur-xs flex items-center gap-1">
                      <span className="h-1.5 w-1.5 rounded-full bg-emerald-300 animate-pulse" />
                      Open Today
                    </span>
                  </div>
                </div>

                {/* Details */}
                <div className="p-6">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h2 className="font-serif text-2xl font-bold text-[#17211F]">
                        {boutique.name}
                      </h2>
                      <p className="mt-1 flex items-start gap-1.5 text-xs text-[#71817A]">
                        <MapPin className="h-4 w-4 shrink-0 text-[#C48B3C] mt-0.5" />
                        <span>{boutique.address}</span>
                      </p>
                    </div>
                  </div>

                  <p className="mt-3 text-xs leading-relaxed text-[#4E5B56]">
                    {boutique.description}
                  </p>

                  {/* Highlights */}
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {boutique.curation.map((c) => (
                      <span
                        key={c}
                        className="rounded-[4px] border border-[#DDD8CF] bg-[#F7F4EE] px-2.5 py-1 text-[11px] font-bold text-[#17211F]"
                      >
                        {c}
                      </span>
                    ))}
                  </div>

                  {/* Timings & Stylist */}
                  <div className="mt-5 grid grid-cols-2 gap-3 border-t border-[#EAE6DF] pt-4 text-xs">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] flex items-center gap-1">
                        <Clock className="h-3 w-3 text-[#1E6A62]" /> Visiting Hours
                      </span>
                      <p className="font-bold text-[#17211F] mt-0.5">{boutique.hours}</p>
                    </div>
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A] flex items-center gap-1">
                        <Award className="h-3 w-3 text-[#9B6A27]" /> Senior Stylist
                      </span>
                      <p className="font-bold text-[#17211F] mt-0.5 truncate">{boutique.leadStylist}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="bg-[#FAF8F5] p-4 border-t border-[#DDD8CF] grid grid-cols-2 gap-2 sm:grid-cols-3">
                <button
                  type="button"
                  onClick={() => handleBookAppointment(boutique.id)}
                  className="col-span-2 sm:col-span-1 flex h-10 items-center justify-center gap-1.5 rounded-[8px] bg-[#1E6A62] text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition cursor-pointer shadow-xs"
                >
                  <Sparkles className="h-3.5 w-3.5 text-[#F3C56A]" />
                  <span>Book VIP Drape</span>
                </button>

                <a
                  href={boutique.mapUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex h-10 items-center justify-center gap-1.5 rounded-[8px] border border-[#DDD8CF] bg-white text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] transition"
                >
                  <MapPin className="h-3.5 w-3.5 text-[#C48B3C]" />
                  <span>Directions</span>
                  <ExternalLink className="h-3 w-3 text-[#71817A]" />
                </a>

                <a
                  href={`https://wa.me/919059564499?text=${encodeURIComponent(`Hello SareeKart ${boutique.name}! I would like to inquire about visiting your boutique.`)}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex h-10 items-center justify-center gap-1.5 rounded-[8px] border border-[#25D366]/40 bg-[#25D366]/10 text-xs font-bold text-[#166534] hover:bg-[#25D366]/20 transition"
                >
                  <MessageCircle className="h-3.5 w-3.5 text-[#25D366]" />
                  <span>WhatsApp</span>
                </a>
              </div>
            </article>
          ))}
        </div>

        {/* Boutique Salon Experience Amenities */}
        <section className="mt-16 rounded-[12px] border border-[#DDD8CF] bg-white p-8 sm:p-12 shadow-xs">
          <div className="text-center max-w-2xl mx-auto">
            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#9B6A27]">
              The Flagship Experience
            </p>
            <h2 className="mt-2 font-serif text-3xl font-bold text-[#17211F]">
              Hospitality Crafted for Royalty.
            </h2>
            <p className="mt-2 text-sm text-[#71817A]">
              We have elevated saree shopping from a counter transaction into a cherished family celebration.
            </p>
          </div>

          <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {AMENITIES.map((item) => (
              <div
                key={item.title}
                className="flex items-start gap-4 rounded-[8px] border border-[#EAE6DF] bg-[#FAF8F5] p-5"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#1E6A62]/10 text-[#1E6A62]">
                  <item.icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#17211F]">{item.title}</h3>
                  <p className="mt-1 text-xs text-[#71817A] leading-relaxed">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* In-Store VIP Appointment Modal */}
      <InStoreAppointmentModal
        isOpen={appointmentModalOpen}
        onClose={() => setAppointmentModalOpen(false)}
        initialBoutiqueId={selectedBoutiqueId}
      />
    </div>
  );
}
