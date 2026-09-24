import React, { useEffect, useState } from 'react';
import {
  Sparkles,
  Shirt,
  ShoppingBag,
  TrendingUp,
  CheckCircle2,
  RefreshCw,
  Zap,
  Scissors,
  Layers,
  Crown
} from 'lucide-react';
import SEO from '../../components/common/SEO';
import aiStylistService from '../../services/aiStylistService';

export default function AiStylistDashboard() {
  const [telemetry, setTelemetry] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchTelemetry = async () => {
    try {
      setLoading(true);
      const res = await aiStylistService.getTelemetry();
      if (res && res.data) {
        setTelemetry(res.data);
      }
    } catch (err) {
      console.error('Failed to load AI stylist telemetry:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTelemetry();
  }, []);

  const total = telemetry?.totalConsultations || 0;
  const conversions = telemetry?.convertedToTailoringCount || 0;
  const rate = telemetry?.tailoringConversionRatePercent || 0;

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise AI Fashion Stylist Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-[#DDD8CF] pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded bg-purple-50 text-purple-700">
              <Sparkles className="h-5 w-5" />
            </span>
            <h1 className="text-2xl font-bold font-serif text-[#111827]">
              AI Fashion Stylist & Drape Intelligence Vault
            </h1>
          </div>
          <p className="text-xs text-[#6b5c4d] mt-1">
            Real-time handloom styling graph telemetry, contrast blouse recommendations, and tailoring studio conversions.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchTelemetry}
            className="inline-flex items-center gap-1.5 rounded-[8px] border border-[#DDD8CF] bg-white px-3.5 py-2 text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] shadow-2xs transition-all"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <div className="flex items-center gap-2 px-4 py-2 bg-purple-50 border border-purple-200 rounded-full">
            <Sparkles className="w-4 h-4 text-purple-600 animate-pulse" />
            <span className="text-xs font-bold text-purple-800">
              Live Tailoring Conversion: {rate}%
            </span>
          </div>
        </div>
      </div>

      {/* Telemetry KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Total Style Invocations</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">{total} Sessions</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Scissors className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Tailoring Conversions</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">{conversions} Orders</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Shirt className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Conversion Rate</p>
            <h3 className="text-xl font-bold text-blue-800 mt-0.5">{rate}%</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <Zap className="w-6 h-6 text-amber-600" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Styling SLA Latency</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">&lt; 50 ms (Local)</h3>
          </div>
        </div>
      </div>

      {/* Top Occasions & Sarees Breakdown */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs space-y-3">
          <h3 className="text-sm font-bold font-serif text-[#111827]">Top Styled Occasions</h3>
          <div className="space-y-2 text-xs">
            {telemetry?.topOccasions && telemetry.topOccasions.length > 0 ? (
              telemetry.topOccasions.map((occ, idx) => (
                <div key={idx} className="flex items-center justify-between p-2 rounded bg-[#F7F4EE]">
                  <span className="font-semibold text-[#17211F]">{occ.occasion}</span>
                  <span className="rounded bg-white px-2 py-0.5 font-bold text-[#1E6A62] shadow-2xs">{occ.count} sessions</span>
                </div>
              ))
            ) : (
              <p className="text-[#71817A] text-xs">No occasion telemetry recorded yet.</p>
            )}
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs space-y-3">
          <h3 className="text-sm font-bold font-serif text-[#111827]">Most Styled Heirloom Sarees</h3>
          <div className="space-y-2 text-xs">
            {telemetry?.topStyledSarees && telemetry.topStyledSarees.length > 0 ? (
              telemetry.topStyledSarees.map((s, idx) => (
                <div key={idx} className="flex items-center justify-between p-2 rounded bg-[#F7F4EE]">
                  <span className="font-semibold text-[#17211F] truncate max-w-xs">{s.sareeName}</span>
                  <span className="rounded bg-white px-2 py-0.5 font-bold text-[#1E6A62] shadow-2xs">{s.count} times</span>
                </div>
              ))
            ) : (
              <p className="text-[#71817A] text-xs">No saree telemetry recorded yet.</p>
            )}
          </div>
        </div>
      </div>

      {/* Recent Consultations Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Recent Clienteling Style Consultations</h2>
          <span className="text-xs font-bold text-[#6b5c4d]">
            {telemetry?.recentConsultations?.length || 0} Recent Sessions
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">ID</th>
                <th className="px-6 py-3.5">Saree Styled</th>
                <th className="px-6 py-3.5">Occasion</th>
                <th className="px-6 py-3.5">Curated Ensemble</th>
                <th className="px-6 py-3.5">Contrast Blouse Color</th>
                <th className="px-6 py-3.5">Tailoring Conversion</th>
                <th className="px-6 py-3.5">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {telemetry?.recentConsultations && telemetry.recentConsultations.length > 0 ? (
                telemetry.recentConsultations.map((c) => (
                  <tr key={c.id} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-[#71817A]">#{c.id}</td>
                    <td className="px-6 py-4 font-bold text-[#17211F]">{c.sareeName}</td>
                    <td className="px-6 py-4 text-[#71817A]">{c.occasion}</td>
                    <td className="px-6 py-4 font-semibold text-purple-800">{c.chosenLookTitle}</td>
                    <td className="px-6 py-4 font-semibold text-[#1E6A62]">{c.contrastColor}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        c.convertedToTailoring
                          ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                          : 'bg-gray-100 text-gray-600 border-gray-200'
                      }`}>
                        {c.convertedToTailoring ? '✓ Converted' : 'Explored'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-[#71817A]">{c.createdAt}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-[#71817A]">
                    No client style consultations recorded yet. Try styling a saree on a product page or via the /stylist studio.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
