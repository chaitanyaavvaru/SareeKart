import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Award, MapPin, Sparkles, ShieldCheck, ArrowRight, Heart, Users, Feather } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl } from '../../utils/seoUtils';
import { motion } from 'framer-motion';

const ARTISAN_CLUSTERS = [
  {
    id: 'kanchi',
    name: 'Kanchipuram Silk Guild',
    region: 'Kanchipuram, Tamil Nadu',
    giCode: 'GI-104',
    leadArtisan: 'Master Weaver Ramanathan S.',
    heritage: '38 Years of Mulberry Silk Mastery',
    drapeSpecialty: 'Korvai Weave & Temple Borders with 0.6% Silver Core Zari',
    activeLooms: 140,
    imageUrl: 'https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'kanchipuram'
  },
  {
    id: 'banarasi',
    name: 'Varanasi Zari Cooperative',
    region: 'Varanasi, Uttar Pradesh',
    giCode: 'GI-77',
    leadArtisan: 'Ustad Mukhtar Ansari',
    heritage: '4th Generation Kadwa Brocade Weaver',
    drapeSpecialty: 'Intricate Floral Jaal & Minakari Gold Foil Work',
    activeLooms: 210,
    imageUrl: 'https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'banarasi'
  },
  {
    id: 'paithani',
    name: 'Yeola Paithani Collective',
    region: 'Yeola & Paithan, Maharashtra',
    giCode: 'GI-84',
    leadArtisan: 'Shri Dattatray Shinde',
    heritage: 'State Awardee Tapestry Artisan',
    drapeSpecialty: 'Mor-Bangadi (Peacock & Bangle) Borders with Pure Muga Silk',
    activeLooms: 85,
    imageUrl: 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'paithani'
  },
  {
    id: 'uppada',
    name: 'Uppada Jamdani Looms',
    region: 'East Godavari, Andhra Pradesh',
    giCode: 'GI-112',
    leadArtisan: 'Smt. Subbalakshmi K.',
    heritage: 'Non-Mechanical Single-Warp Jamdani Specialist',
    drapeSpecialty: 'Featherlight Translucent Pure Cotton-Silk Drapes',
    activeLooms: 60,
    imageUrl: 'https://images.unsplash.com/photo-1583391265517-35bbdad01209?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'uppada'
  },
  {
    id: 'patola',
    name: 'Patan Double-Ikat Guild',
    region: 'Patan, Gujarat',
    giCode: 'GI-232',
    leadArtisan: 'Salvi Bharatbhai',
    heritage: '800-Year Ancestral Heritage of Double-Ikat Silk',
    drapeSpecialty: 'Resist-Dyed Silk Warp & Weft Alignment with Natural Indigo & Madder',
    activeLooms: 32,
    imageUrl: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'patola'
  },
  {
    id: 'chanderi',
    name: 'Chanderi Royal Looms Society',
    region: 'Pranpur & Chanderi, Madhya Pradesh',
    giCode: 'GI-28',
    leadArtisan: 'Ustad Aminuddin Ansari',
    heritage: 'Scindia Dynasty Master Weaver',
    drapeSpecialty: 'Gossamer Silk-Cotton Blends with Fine Gold Zari Bootis',
    activeLooms: 95,
    imageUrl: 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80',
    searchQuery: 'chanderi'
  }
];

export default function ArtisansPage() {
  const [selectedCluster, setSelectedCluster] = useState('ALL');

  const filteredClusters = selectedCluster === 'ALL'
    ? ARTISAN_CLUSTERS
    : ARTISAN_CLUSTERS.filter(c => c.id === selectedCluster);

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F] font-sans text-left">
      <SEO
        title="Master Weavers & Craft Guilds | SareeKart Heritage"
        description="Meet the generational master artisans and weaving cooperatives behind SareeKart's GI-certified handloom sarees."
        canonical={getCanonicalUrl('/artisans')}
      />

      {/* Hero Banner */}
      <section className="bg-[#17211F] text-white py-16 px-6 lg:px-16 relative overflow-hidden">
        <div className="max-w-6xl mx-auto space-y-4 relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#1E6A62] text-white text-xs font-bold uppercase tracking-widest rounded-full">
            <Award className="w-3.5 h-3.5 text-[#F3C56A]" /> Verified Handloom Guilds
          </div>
          <h1 className="text-4xl sm:text-6xl font-serif font-bold text-white leading-tight">
            The Hands Behind the Heritage.
          </h1>
          <p className="text-[#9AA5A5] text-base sm:text-lg max-w-2xl leading-relaxed">
            Every SareeKart drape represents weeks of painstaking handcraft by certified weaver guilds across India. No synthetic shortcuts, no middlemen markups.
          </p>

          {/* Value Stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-6 border-t border-white/10">
            <div>
              <p className="text-2xl sm:text-3xl font-bold font-serif text-[#F3C56A]">495+</p>
              <p className="text-xs text-[#9AA5A5] uppercase font-bold tracking-wider">Active Looms</p>
            </div>
            <div>
              <p className="text-2xl sm:text-3xl font-bold font-serif text-[#F3C56A]">72%</p>
              <p className="text-xs text-[#9AA5A5] uppercase font-bold tracking-wider">Direct Weaver Payout</p>
            </div>
            <div>
              <p className="text-2xl sm:text-3xl font-bold font-serif text-[#F3C56A]">4 GI Tags</p>
              <p className="text-xs text-[#9AA5A5] uppercase font-bold tracking-wider">Govt. Verified</p>
            </div>
            <div>
              <p className="text-2xl sm:text-3xl font-bold font-serif text-[#F3C56A]">100%</p>
              <p className="text-xs text-[#9AA5A5] uppercase font-bold tracking-wider">Pure SilkMark</p>
            </div>
          </div>
        </div>
      </section>

      {/* Clusters Showcase */}
      <section className="max-w-6xl mx-auto px-6 lg:px-16 py-12 space-y-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-[#DDD8CF] pb-4">
          <div>
            <h2 className="text-2xl font-bold font-serif text-[#17211F]">Certified Artisan Guilds</h2>
            <p className="text-xs text-[#71817A] mt-1">Select a weaving cluster to explore generational craft histories</p>
          </div>
          <div className="flex gap-2 overflow-x-auto">
            {['ALL', 'kanchi', 'banarasi', 'paithani', 'uppada', 'patola', 'chanderi'].map((c) => (
              <button
                key={c}
                onClick={() => setSelectedCluster(c)}
                className={`px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider transition-all cursor-pointer ${
                  selectedCluster === c
                    ? 'bg-[#17211F] text-[#F3C56A]'
                    : 'bg-white text-[#71817A] border border-[#DDD8CF] hover:bg-[#F7F4EE]'
                }`}
              >
                {c === 'ALL' ? 'All Clusters' : c}
              </button>
            ))}
          </div>
        </div>

        {/* Guild Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {filteredClusters.map((cluster) => (
            <motion.div
              key={cluster.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white border border-[#DDD8CF] rounded-3xl overflow-hidden shadow-xs hover:shadow-xl transition-all flex flex-col justify-between"
            >
              <div className="relative h-64 overflow-hidden bg-[#F7F4EE]">
                <img
                  src={cluster.imageUrl}
                  alt={cluster.name}
                  className="w-full h-full object-cover object-top hover:scale-105 transition-transform duration-500"
                />
                <div className="absolute top-4 left-4 flex gap-2">
                  <span className="px-3 py-1 bg-[#17211F]/90 backdrop-blur-xs text-[#F3C56A] font-mono text-xs font-bold rounded-full border border-[#F3C56A]/30">
                    {cluster.giCode}
                  </span>
                  <span className="px-3 py-1 bg-white/95 backdrop-blur-xs text-[#1E6A62] text-xs font-bold rounded-full border border-[#DDD8CF]">
                    {cluster.activeLooms} Active Looms
                  </span>
                </div>
              </div>

              <div className="p-6 space-y-4 flex-1 flex flex-col justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-1.5 text-xs text-[#71817A] font-bold">
                    <MapPin className="w-3.5 h-3.5 text-[#1E6A62]" /> {cluster.region}
                  </div>
                  <h3 className="text-xl font-bold font-serif text-[#17211F]">{cluster.name}</h3>
                  <p className="text-xs font-bold text-[#1E6A62]">{cluster.leadArtisan} • {cluster.heritage}</p>
                  <p className="text-xs text-[#71817A] leading-relaxed pt-1">
                    {cluster.drapeSpecialty}
                  </p>
                </div>

                <div className="pt-4 border-t border-[#F7F4EE] flex justify-between items-center">
                  <span className="text-xs font-bold text-[#1E6A62] flex items-center gap-1">
                    <ShieldCheck className="w-4 h-4 text-[#1E6A62]" /> Direct Guild Partner
                  </span>
                  <Link
                    to={`/products?search=${cluster.searchQuery}`}
                    className="px-4 py-2 bg-[#17211F] hover:bg-[#1E6A62] text-white font-bold text-xs uppercase tracking-wider rounded-full transition-all flex items-center gap-1.5 shadow-xs"
                  >
                    View Sarees <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
}
