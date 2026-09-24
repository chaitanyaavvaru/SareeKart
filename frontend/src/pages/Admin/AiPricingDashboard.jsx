import React, { useState } from 'react';
import { DollarSign, TrendingUp, ShieldCheck, Sliders, CheckCircle2, AlertCircle, RefreshCcw, Layers, Search, Filter, ArrowUpRight, Zap, IndianRupee, Clock } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_PRICE_RECS = [
  {
    sku: 'SK-KANCHI-01',
    name: 'Kanchipuram Gold Tissue Silk',
    cost: 8000,
    currentPrice: 12500,
    recPrice: 13800,
    margin: '42.5%',
    strategy: 'DEMAND_SURGE',
    elasticity: 'Inelastic (0.4)'
  },
  {
    sku: 'SK-BANARASI-02',
    name: 'Banarasi Zari Brocade Saree',
    cost: 12000,
    currentPrice: 18900,
    recPrice: 17500,
    margin: '31.2%',
    strategy: 'INVENTORY_CLEARANCE',
    elasticity: 'Elastic (1.6)'
  },
  {
    sku: 'SK-PAITHANI-03',
    name: 'Paithani Peacock Silk Saree',
    cost: 9500,
    currentPrice: 15000,
    recPrice: 15000,
    margin: '36.6%',
    strategy: 'OPTIMAL_HOLD',
    elasticity: 'Moderate (0.9)'
  }
];

export default function AiPricingDashboard() {
  const [activeTab, setActiveTab] = useState('TELEMETRY');
  const [priceRecs, setPriceRecs] = useState(MOCK_PRICE_RECS);
  const [simPriceAdj, setSimPriceAdj] = useState(5); // % adjustment slider

  const handleApplyPrice = (sku, recPrice) => {
    alert(`Applied AI Dynamic Price for ${sku}: ₹${recPrice}. Updated in storefront catalog.`);
  };

  const formatCurrency = (val) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0,
    }).format(val || 0);

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise AI Dynamic Pricing Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">AI Dynamic Pricing & Profitability Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Price elasticity profiling, margin guardrails, and What-If revenue simulation engine</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          <span className="text-xs font-bold text-emerald-800">Margin Guardrail Status: PROTECTED (25% Floor)</span>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Net Margin Lift</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">+18.4% Margin</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <IndianRupee className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Avg Selling Price</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">₹14,200 ASP</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Promo ROI Multiplier</p>
            <h3 className="text-xl font-bold text-blue-800 mt-0.5">4.2x ROI</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <Clock className="w-6 h-6 text-amber-600" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Real-Time API SLA</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">115 ms (Avg)</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'TELEMETRY', label: '💰 Price & Margin Telemetry' },
          { id: 'SIMULATOR', label: '🎛 What-If Price Simulator' },
          { id: 'GUARDRAILS', label: '🛡 Margin Guardrails & Floors' },
          { id: 'AUDIT', label: '📜 Price Audit Ledger' }
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

      {/* Dynamic Price Recommendations Table */}
      {activeTab === 'TELEMETRY' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
          <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
            <h2 className="text-sm font-bold font-serif text-[#111827]">Live Dynamic Price Optimization Recommendations</h2>
            <span className="text-xs font-bold text-[#6b5c4d]">3 Active SKUs Analyzed</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">SKU ID / Title</th>
                  <th className="px-6 py-3.5">Unit Cost</th>
                  <th className="px-6 py-3.5">Current Price</th>
                  <th className="px-6 py-3.5">AI Recommended Price</th>
                  <th className="px-6 py-3.5">Projected Net Margin</th>
                  <th className="px-6 py-3.5">AI Strategy</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {priceRecs.map((p) => (
                  <tr key={p.sku} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-bold text-[#111827]">
                      <div>
                        <span className="block font-mono">{p.sku}</span>
                        <span className="text-[10px] text-[#6b5c4d] font-sans block">{p.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-medium text-[#6b5c4d]">{formatCurrency(p.cost)}</td>
                    <td className="px-6 py-4 font-bold text-[#111827]">{formatCurrency(p.currentPrice)}</td>
                    <td className="px-6 py-4 font-extrabold text-emerald-800">{formatCurrency(p.recPrice)}</td>
                    <td className="px-6 py-4 font-bold text-purple-800">{p.margin}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        p.strategy === 'DEMAND_SURGE' ? 'bg-[#111827] text-[#E85D4F] border-[#111827]' :
                        p.strategy === 'INVENTORY_CLEARANCE' ? 'bg-amber-50 text-amber-800 border-amber-200' :
                        'bg-blue-50 text-blue-800 border-blue-200'
                      }`}>
                        {p.strategy.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {p.currentPrice !== p.recPrice && (
                        <button
                          onClick={() => handleApplyPrice(p.sku, p.recPrice)}
                          className="px-3 py-1.5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-[10px] uppercase tracking-wider rounded-full shadow-xs transition-all cursor-pointer inline-flex items-center gap-1"
                        >
                          Apply Price
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

      {/* What-If Price Simulator */}
      {activeTab === 'SIMULATOR' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-6 shadow-xs space-y-6">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Interactive "What-If" Revenue & Margin Simulator</h2>
          
          <div className="p-5 bg-[#F5F7FA] border border-[#DDE4EA] rounded-2xl space-y-4">
            <div className="flex justify-between items-center text-xs font-bold text-[#111827]">
              <span>Simulated Price Adjustment Percentage:</span>
              <span className="text-sm font-mono text-[#E85D4F]">{simPriceAdj > 0 ? `+${simPriceAdj}%` : `${simPriceAdj}%`}</span>
            </div>
            <input
              type="range"
              min="-20"
              max="20"
              value={simPriceAdj}
              onChange={e => setSimPriceAdj(Number(e.target.value))}
              className="w-full h-2 bg-white rounded-lg appearance-none cursor-pointer accent-[#111827]"
            />
            <div className="flex justify-between text-[10px] text-[#6b5c4d] font-mono">
              <span>-20% Markdown</span>
              <span>0% Baseline</span>
              <span>+20% Premium</span>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-left">
            <div className="p-4 bg-white border border-[#DDE4EA] rounded-xl space-y-1">
              <span className="text-[10px] text-[#6b5c4d] font-bold uppercase tracking-wider">Projected Revenue Lift</span>
              <h4 className="text-lg font-bold text-emerald-800">{formatCurrency(2450000 * (1 + simPriceAdj * 0.015))}</h4>
            </div>
            <div className="p-4 bg-white border border-[#DDE4EA] rounded-xl space-y-1">
              <span className="text-[10px] text-[#6b5c4d] font-bold uppercase tracking-wider">Projected Net Margin</span>
              <h4 className="text-lg font-bold text-purple-800">{(32.4 + simPriceAdj * 0.4).toFixed(1)}%</h4>
            </div>
            <div className="p-4 bg-white border border-[#DDE4EA] rounded-xl space-y-1">
              <span className="text-[10px] text-[#6b5c4d] font-bold uppercase tracking-wider">Projected Sales Volume</span>
              <h4 className="text-lg font-bold text-[#111827]">{Math.round(148 * (1 - simPriceAdj * 0.008))} Units</h4>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
