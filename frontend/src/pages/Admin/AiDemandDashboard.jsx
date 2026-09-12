import React, { useState } from 'react';
import { TrendingUp, Package, AlertTriangle, Building2, Truck, CheckCircle2, RefreshCcw, Layers, Search, Filter, ArrowUpRight, Cpu } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_SKU_FORECASTS = [
  {
    sku: 'SK-KANCHI-01',
    category: 'Kanchipuram Gold Tissue',
    onHand: 12,
    predictedDemand: '45 Units (30 Days)',
    reorderRec: '+35 Units PO',
    vendor: 'Kanchipuram Silk Master Guild',
    status: 'REORDER_NEEDED'
  },
  {
    sku: 'SK-BANARASI-02',
    category: 'Banarasi Zari Brocade',
    onHand: 4,
    predictedDemand: '28 Units (30 Days)',
    reorderRec: '+25 Units PO',
    vendor: 'Varanasi Weaver Cooperative',
    status: 'CRITICAL_RISK'
  },
  {
    sku: 'SK-PAITHANI-03',
    category: 'Paithani Peacock Silk',
    onHand: 30,
    predictedDemand: '12 Units (30 Days)',
    reorderRec: '0 Units (Hold)',
    vendor: 'Govardhan Ikat Looms',
    status: 'OVERSTOCKED'
  }
];

const MOCK_VENDORS = [
  { name: 'Varanasi Weaver Cooperative', fillRate: '98.2%', leadTime: '10 Days', qualityScore: '99.1%', reliability: 'EXCELLENT' },
  { name: 'Kanchipuram Silk Master Guild', fillRate: '96.5%', leadTime: '14 Days', qualityScore: '98.4%', reliability: 'EXCELLENT' },
  { name: 'Pochampally Ikat Weavers', fillRate: '99.0%', leadTime: '7 Days', qualityScore: '99.5%', reliability: 'EXCELLENT' }
];

export default function AiDemandDashboard() {
  const [activeTab, setActiveTab] = useState('FORECASTS');
  const [forecasts, setForecasts] = useState(MOCK_SKU_FORECASTS);

  const handleIssuePo = (sku, rec) => {
    alert(`Issued automated AI Purchase Order for ${sku}: ${rec}`);
  };

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise AI Demand Forecasting Vault | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">AI Demand Forecasting & Replenishment Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Time-series demand prediction, stock-out risk reduction, and automated purchase order generation</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <TrendingUp className="w-4 h-4 text-emerald-600" />
          <span className="text-xs font-bold text-emerald-800">Forecast Accuracy: 94.2% (Passed)</span>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Forecast Accuracy</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">94.2% Accuracy</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-[#F5F7FA] text-[#111827] flex items-center justify-center shrink-0 border border-[#DDE4EA]">
            <AlertTriangle className="w-6 h-6 text-amber-600" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Stock-Out Reduction</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">-78.5% Outages</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Overstock Reduction</p>
            <h3 className="text-xl font-bold text-blue-800 mt-0.5">-42.0% Excess</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Cpu className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Real-Time API SLA</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">118 ms (Avg)</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'FORECASTS', label: '📈 Time-Series Demand Forecasts' },
          { id: 'REPLENISHMENT', label: '📦 Smart Replenishment POs' },
          { id: 'VENDORS', label: '🤝 Vendor Reliability & Fill Rates' },
          { id: 'ROADMAP', label: '🔮 AI Evolution Roadmap' }
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

      {/* Demand Forecast & Replenishment Table */}
      {activeTab === 'FORECASTS' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
          <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
            <h2 className="text-sm font-bold font-serif text-[#111827]">Live SKU-Level Demand Predictions</h2>
            <span className="text-xs font-bold text-[#6b5c4d]">3 Active SKUs Tracked</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">SKU ID / Category</th>
                  <th className="px-6 py-3.5">Stock On-Hand</th>
                  <th className="px-6 py-3.5">Predicted 30-Day Demand</th>
                  <th className="px-6 py-3.5">AI Reorder Recommendation</th>
                  <th className="px-6 py-3.5">Target Weaver Vendor</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {forecasts.map((f) => (
                  <tr key={f.sku} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-bold text-[#111827]">
                      <div>
                        <span className="block font-mono">{f.sku}</span>
                        <span className="text-[10px] text-[#E85D4F] uppercase tracking-wider font-sans block">{f.category}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-bold text-[#111827]">{f.onHand} Units</td>
                    <td className="px-6 py-4 font-bold text-purple-800">{f.predictedDemand}</td>
                    <td className="px-6 py-4 font-extrabold text-emerald-800">{f.reorderRec}</td>
                    <td className="px-6 py-4 font-medium text-[#6b5c4d]">{f.vendor}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        f.status === 'CRITICAL_RISK' ? 'bg-red-50 text-red-800 border-red-200' :
                        f.status === 'REORDER_NEEDED' ? 'bg-amber-50 text-amber-800 border-amber-200' :
                        'bg-blue-50 text-blue-800 border-blue-200'
                      }`}>
                        {f.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {f.reorderRec.includes('PO') && (
                        <button
                          onClick={() => handleIssuePo(f.sku, f.reorderRec)}
                          className="px-3 py-1.5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-[10px] uppercase tracking-wider rounded-full shadow-xs transition-all cursor-pointer inline-flex items-center gap-1"
                        >
                          Auto Issue PO
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Vendor Reliability Matrix */}
      {activeTab === 'VENDORS' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-6 shadow-xs space-y-4">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Weaver Guild Performance & Lead-Time Telemetry</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {MOCK_VENDORS.map((v) => (
              <div key={v.name} className="p-4 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl space-y-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="font-bold text-[#111827]">{v.name}</span>
                  <span className="font-mono text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">{v.reliability}</span>
                </div>
                <div className="text-[10px] text-[#6b5c4d] space-y-1 pt-1 font-mono">
                  <p><strong>Fill Rate:</strong> {v.fillRate}</p>
                  <p><strong>Avg Lead Time:</strong> {v.leadTime}</p>
                  <p><strong>Quality Audit Score:</strong> {v.qualityScore}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
