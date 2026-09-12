import React, { useState } from 'react';
import { Layout, Image, FileText, Globe, Sparkles, Plus, Edit2, Trash2, Check, X, Search, Eye, RefreshCcw, Layers, Search as SearchIcon, UploadCloud } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_SECTIONS = [
  {
    id: 'SEC-HERO',
    name: 'Hero Campaign Main Banner',
    component: 'Hero.jsx',
    lastUpdated: 'Today, 10:30 AM',
    status: 'PUBLISHED',
    author: 'Editorial Team'
  },
  {
    id: 'SEC-TICKER',
    name: 'Announcement Bar Ticker',
    component: 'OfferBar.jsx',
    lastUpdated: 'Yesterday',
    status: 'PUBLISHED',
    author: 'Marketing Team'
  },
  {
    id: 'SEC-ARTISAN',
    name: 'Master Weaver Artisan Showcase',
    component: 'ArtisanSection.jsx',
    lastUpdated: 'June 15, 2026',
    status: 'DRAFT',
    author: 'Content Manager'
  }
];

export default function ManageCMS() {
  const [activeTab, setActiveTab] = useState('HOMEPAGE');
  const [sections, setSections] = useState(MOCK_SECTIONS);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedSection, setSelectedSection] = useState(null);

  // Form State
  const [bannerTitle, setBannerTitle] = useState('Heritage Weaves Woven for Generations');
  const [bannerTagline, setBannerTagline] = useState('SareeKart Signature Collection');

  const handleEdit = (section) => {
    setSelectedSection(section);
    setShowEditModal(true);
  };

  const handleSaveSection = (e) => {
    e.preventDefault();
    alert(`Saved section changes for ${selectedSection?.name}. Published to live storefront.`);
    setShowEditModal(false);
  };

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise CMS & SEO Management | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Headless CMS & SEO Control Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Zero-code homepage builder, schema.org SEO metadata, and WebP media library</p>
        </div>
        <button
          onClick={() => setShowEditModal(true)}
          className="h-11 px-5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center gap-2 cursor-pointer"
        >
          <Plus className="w-4 h-4" /> Create Landing Page
        </button>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Layout className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Published Pages</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">18 Active Pages</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Image className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">WebP Media Assets</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">340 Optimized</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Globe className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">SEO Health Score</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">96 / 100 (Optimal)</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <FileText className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Draft Revisions</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">4 Revisions</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'HOMEPAGE', label: '🖼 Homepage & Section Builder' },
          { id: 'LANDING', label: '📄 Festival Landing Pages' },
          { id: 'MEDIA', label: '📸 WebP Media Library' },
          { id: 'BLOG', label: '✍ Handloom Journal / Blog' },
          { id: 'SEO', label: '🔍 Schema.org & SEO Console' },
          { id: 'REVISIONS', label: '📜 Content Revision History' }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer border whitespace-nowrap ${
              activeTab === tab.id
                ? 'bg-[#111827] text-[#E85D4F] border-[#111827]'
                : 'bg-white text-[#6b5c4d] border-transparent hover:bg-[#F5F7FA]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Section Builder Data Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Live Homepage Visual Sections</h2>
          <span className="text-xs font-bold text-[#6b5c4d]">3 Active Components</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">Section Name</th>
                <th className="px-6 py-3.5">React Component</th>
                <th className="px-6 py-3.5">Last Updated</th>
                <th className="px-6 py-3.5">Author</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {sections.map((sec) => (
                <tr key={sec.id} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-bold text-[#111827]">
                    <div>
                      <span className="block">{sec.name}</span>
                      <span className="text-[10px] font-mono text-[#E85D4F] block">{sec.id}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 font-mono font-medium text-[#6b5c4d]">{sec.component}</td>
                  <td className="px-6 py-4 font-medium text-[#6b5c4d]">{sec.lastUpdated}</td>
                  <td className="px-6 py-4 font-medium">{sec.author}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                      sec.status === 'PUBLISHED' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                      'bg-amber-50 text-amber-800 border-amber-200'
                    }`}>
                      {sec.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleEdit(sec)}
                      className="p-1.5 rounded-lg text-[#111827] hover:bg-[#F5F7FA] transition-colors cursor-pointer border border-[#DDE4EA] inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider px-2"
                    >
                      <Edit2 className="w-3.5 h-3.5 text-[#E85D4F]" /> Edit Content
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Section Modal */}
      <AnimatePresence>
        {showEditModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowEditModal(false)} className="absolute inset-0 bg-black/50 backdrop-blur-xs" />
            <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="relative bg-white rounded-3xl border border-[#DDE4EA] max-w-md w-full p-6 space-y-6 z-10 text-left shadow-2xl">
              <div className="flex justify-between items-center border-b border-[#DDE4EA] pb-4">
                <h3 className="text-lg font-bold font-serif text-[#111827]">Edit Visual Section Content</h3>
                <button onClick={() => setShowEditModal(false)} className="p-1 text-[#6b5c4d] hover:text-[#111827]"><X className="w-5 h-5" /></button>
              </div>

              <form onSubmit={handleSaveSection} className="space-y-4">
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Section Headline *</label>
                  <input type="text" required value={bannerTitle} onChange={e => setBannerTitle(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#E85D4F]" />
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Sub-Tagline *</label>
                  <input type="text" required value={bannerTagline} onChange={e => setBannerTagline(e.target.value)} className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#E85D4F]" />
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase tracking-wider text-[#111827] block mb-1">Background Image URL (WebP) *</label>
                  <input type="text" required value="https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=1000&q=80" className="w-full h-11 px-4 border border-[#DDE4EA] rounded-xl text-xs outline-none focus:border-[#E85D4F] font-mono text-[10px]" />
                </div>

                <button type="submit" className="w-full h-12 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all mt-4 cursor-pointer">
                  Publish Content Changes
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
