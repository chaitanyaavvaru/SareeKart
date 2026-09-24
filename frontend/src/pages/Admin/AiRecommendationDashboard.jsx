import React, { useState } from 'react';
import { Sparkles, Brain, Cpu, TrendingUp, Zap, Target, Layers, ArrowUpRight, CheckCircle2, Search, Filter, RefreshCcw } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_ALGORITHMS = [
  {
    strategy: 'Similar Sarees (PDP)',
    model: 'Content-Based Vector Match',
    ctr: '18.2%',
    conversionLift: '+32.4%',
    latency: '42 ms',
    status: 'ACTIVE'
  },
  {
    strategy: 'Recommended For You (Homepage)',
    model: 'Session Collaborative Filter',
    ctr: '14.5%',
    conversionLift: '+26.8%',
    latency: '88 ms',
    status: 'ACTIVE'
  },
  {
    strategy: 'Cart Pairings (Cross-Sell)',
    model: 'Co-occurrence Matrix',
    ctr: '12.1%',
    conversionLift: '+21.5%',
    latency: '65 ms',
    status: 'ACTIVE'
  },
  {
    strategy: 'AI Search Re-ranking',
    model: 'Semantic Intent Parser',
    ctr: '16.4%',
    conversionLift: '+30.1%',
    latency: '112 ms',
    status: 'ACTIVE'
  }
];

const MOCK_SEGMENTS = [
  { segment: 'Bridal Heritage Seekers', count: '4,280 Patrons', topWeave: 'Kanchipuram Gold Tissue', avgSpend: '₹85,000' },
  { segment: 'Varanasi Zari Connoisseurs', count: '3,150 Patrons', topWeave: 'Banarasi Kadwa Brocade', avgSpend: '₹62,000' },
  { segment: 'Festival Silk Enthusiasts', count: '8,900 Patrons', topWeave: 'Paithani Peacock Silk', avgSpend: '₹35,000' }
];

export default function AiRecommendationDashboard() {
  const [activeTab, setActiveTab] = useState('TELEMETRY');
  const [algorithms, setAlgorithms] = useState(MOCK_ALGORITHMS);

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise AI Recommendation Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">AI Recommendation & Personalization Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Content-based vector matching, collaborative filtering, and real-time customer intelligence</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-purple-50 border border-purple-200 rounded-full">
          <Sparkles className="w-4 h-4 text-purple-600 animate-pulse" />
          <span className="text-xs font-bold text-purple-800">AI Revenue Attribution: 34.2% of Total Sales</span>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Brain className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">AI Revenue Share</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">34.2% Share</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Target className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Recommendation CTR</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">14.8% Click Rate</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Conversion Lift</p>
            <h3 className="text-xl font-bold text-blue-800 mt-0.5">+28.4% Lift</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">API Latency SLA</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">112 ms (Avg)</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'TELEMETRY', label: '🧠 Recommendation Telemetry' },
          { id: 'PERFORMANCE', label: '🎯 Algorithm Performance' },
          { id: 'SEGMENTS', label: '👤 Customer Persona Segments' },
          { id: 'ROADMAP', label: '🔮 5-Phase AI Roadmap' }
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

      {/* Algorithm Performance Table */}
      {activeTab === 'TELEMETRY' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
          <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
            <h2 className="text-sm font-bold font-serif text-[#111827]">Live AI Recommendation Algorithms</h2>
            <span className="text-xs font-bold text-[#6b5c4d]">4 Active Engine Models</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">Algorithm Strategy</th>
                  <th className="px-6 py-3.5">Model Architecture</th>
                  <th className="px-6 py-3.5">Click-Through Rate (CTR)</th>
                  <th className="px-6 py-3.5">Conversion Lift</th>
                  <th className="px-6 py-3.5">Avg API Latency</th>
                  <th className="px-6 py-3.5">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {algorithms.map((alg) => (
                  <tr key={alg.strategy} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-bold text-[#111827]">{alg.strategy}</td>
                    <td className="px-6 py-4 font-medium text-[#6b5c4d]">{alg.model}</td>
                    <td className="px-6 py-4 font-bold text-purple-800">{alg.ctr}</td>
                    <td className="px-6 py-4 font-bold text-emerald-800">{alg.conversionLift}</td>
                    <td className="px-6 py-4 font-mono font-bold text-[#111827]">{alg.latency}</td>
                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-emerald-50 text-emerald-800 border-emerald-200">
                        {alg.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Customer Persona Segments */}
      {activeTab === 'SEGMENTS' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-6 shadow-xs space-y-4">
          <h2 className="text-sm font-bold font-serif text-[#111827]">AI-Derived Customer Intelligence Segments</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {MOCK_SEGMENTS.map((seg) => (
              <div key={seg.segment} className="p-4 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl space-y-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-bold text-[#111827]">{seg.segment}</span>
                  <span className="font-mono text-[10px] font-bold text-[#E85D4F]">{seg.count}</span>
                </div>
                <div className="text-[10px] text-[#6b5c4d] space-y-1 pt-1">
                  <p><strong>Top Affinity:</strong> {seg.topWeave}</p>
                  <p><strong>Avg Basket Value:</strong> {seg.avgSpend}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
