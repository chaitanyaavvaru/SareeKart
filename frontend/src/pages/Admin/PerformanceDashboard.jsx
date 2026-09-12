import React, { useState } from 'react';
import { Zap, Activity, Cpu, Database, Server, Gauge, CheckCircle2, Clock, ArrowUpRight, BarChart3, Layers, RefreshCcw, HardDrive } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_API_TELEMETRY = [
  {
    endpoint: 'GET /api/sarees',
    avgLatency: '42 ms',
    p95Latency: '88 ms',
    cacheHitRatio: '96.4%',
    cacheEngine: 'Redis Cache-Aside',
    status: 'OPTIMAL'
  },
  {
    endpoint: 'GET /api/sarees/{id}',
    avgLatency: '28 ms',
    p95Latency: '54 ms',
    cacheHitRatio: '98.1%',
    cacheEngine: 'Redis Cache-Aside',
    status: 'OPTIMAL'
  },
  {
    endpoint: 'POST /api/orders',
    avgLatency: '185 ms',
    p95Latency: '310 ms',
    cacheHitRatio: 'N/A (DB Write)',
    cacheEngine: 'HikariCP Pool',
    status: 'OPTIMAL'
  },
  {
    endpoint: 'GET /api/admin/dashboard',
    avgLatency: '65 ms',
    p95Latency: '112 ms',
    cacheHitRatio: '89.2%',
    cacheEngine: 'Spring Cache',
    status: 'OPTIMAL'
  }
];

export default function PerformanceDashboard() {
  const [activeTab, setActiveTab] = useState('VITALS');
  const [telemetry, setTelemetry] = useState(MOCK_API_TELEMETRY);

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise SRE & Performance Telemetry | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">SRE & Performance Telemetry Console</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Google Core Web Vitals, Redis cache hit rates, and HikariCP connection pool telemetry</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <Activity className="w-4 h-4 text-emerald-600 animate-pulse" />
          <span className="text-xs font-bold text-emerald-800">System Uptime SLA: 99.94% (Passed)</span>
        </div>
      </div>

      {/* Core Web Vitals Telemetry Gauges */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Largest Contentful Paint</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">LCP: 1.18s</h3>
            <p className="text-[10px] text-emerald-600 font-semibold mt-0.5">Target: &lt; 2.5s (Passed)</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Interaction to Next Paint</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">INP: 84ms</h3>
            <p className="text-[10px] text-purple-600 font-semibold mt-0.5">Target: &lt; 200ms (Passed)</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Gauge className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Cumulative Layout Shift</p>
            <h3 className="text-xl font-bold text-blue-800 mt-0.5">CLS: 0.02</h3>
            <p className="text-[10px] text-blue-600 font-semibold mt-0.5">Target: &lt; 0.1 (Passed)</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <Server className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Redis Cache Hit Ratio</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">94.2% Hits</h3>
            <p className="text-[10px] text-amber-600 font-semibold mt-0.5">340 KB Memory Used</p>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'VITALS', label: '⚡ Core Web Vitals Telemetry' },
          { id: 'LATENCY', label: '🚀 REST API Latency & SLA' },
          { id: 'CACHE', label: '💾 Redis Caching Tier' },
          { id: 'POOL', label: '🗄 HikariCP Connection Pool' }
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

      {/* API Latency Telemetry Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Live API Endpoint Latency & SLA Compliance</h2>
          <span className="text-xs font-bold text-[#6b5c4d]">4 Monitored Endpoints</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">API Route</th>
                <th className="px-6 py-3.5">Avg Response Latency</th>
                <th className="px-6 py-3.5">95th Percentile (p95)</th>
                <th className="px-6 py-3.5">Cache Hit Ratio</th>
                <th className="px-6 py-3.5">Execution Engine</th>
                <th className="px-6 py-3.5">SLA Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {telemetry.map((t) => (
                <tr key={t.endpoint} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-[#111827]">{t.endpoint}</td>
                  <td className="px-6 py-4 font-bold text-emerald-800">{t.avgLatency}</td>
                  <td className="px-6 py-4 font-medium text-amber-800">{t.p95Latency}</td>
                  <td className="px-6 py-4 font-bold text-purple-800">{t.cacheHitRatio}</td>
                  <td className="px-6 py-4 font-medium text-[#6b5c4d]">{t.cacheEngine}</td>
                  <td className="px-6 py-4">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-emerald-50 text-emerald-800 border-emerald-200">
                      {t.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
