import React, { useState } from 'react';
import { Rocket, ShieldCheck, CheckCircle2, AlertTriangle, FileText, Users, Clock, RefreshCcw, Activity, Server, PhoneCall, HelpCircle, HardDrive } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_GOLIVE_CHECKLIST = [
  {
    category: 'Security & Compliance',
    item: 'OWASP ASVS Level 2 Security Audit',
    owner: 'DevSecOps Team',
    status: 'PASSED',
    notes: 'Zero critical or high vulnerabilities'
  },
  {
    category: 'Quality Assurance',
    item: 'Full Automated Test Suite (642 Suites)',
    owner: 'QA Lead',
    status: 'PASSED',
    notes: '88.4% Code Coverage achieved'
  },
  {
    category: 'Performance Engineering',
    item: 'Google Core Web Vitals Budget',
    owner: 'SRE Specialist',
    status: 'PASSED',
    notes: 'LCP: 1.18s, INP: 84ms, CLS: 0.02'
  },
  {
    category: 'Disaster Recovery',
    item: 'Automated MySQL Point-in-Time Backup',
    owner: 'DBA Team',
    status: 'PASSED',
    notes: 'RTO < 1h, RPO < 15m verified'
  }
];

const MOCK_RUNBOOKS = [
  {
    id: 'RB-01',
    title: 'High CPU Load (> 85%) Resolution',
    category: 'Infrastructure',
    steps: '1. Inspect top processes via top/htop. 2. Verify Redis cache hit ratio. 3. Auto-scale container pod instance.'
  },
  {
    id: 'RB-02',
    title: 'MySQL Database Connection Pool Contention',
    category: 'Database',
    steps: '1. Check active HikariCP connection count. 2. Terminate long-running idle queries. 3. Failover to secondary read replica.'
  },
  {
    id: 'RB-03',
    title: 'Payment Gateway (Razorpay) Outage Fallback',
    category: 'FinTech',
    steps: '1. Verify Razorpay API status webhook. 2. Switch gateway route to secondary fallback payment gateway. 3. Notify support desk.'
  }
];

export default function OperationsVault() {
  const [activeTab, setActiveTab] = useState('CHECKLIST');
  const [checklist, setChecklist] = useState(MOCK_GOLIVE_CHECKLIST);

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise Operations & Go-Live Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Production Operations & Go-Live Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Pre-launch readiness checklist, incident runbooks, and L1/L2/L3 support escalation matrix</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <Rocket className="w-4 h-4 text-emerald-600" />
          <span className="text-xs font-bold text-emerald-800">Go-Live Status: APPROVED (100% Ready)</span>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Production Readiness</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">100% Ready</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Activity className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">System Availability</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">99.94% Uptime</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Mean Time to Detect (MTTD)</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">3.8 Minutes</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <RefreshCcw className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Mean Time to Recover (MTTR)</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">11.2 Minutes</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'CHECKLIST', label: '🚀 Go-Live Approval Checklist' },
          { id: 'RUNBOOKS', label: '📋 Incident Response Runbooks' },
          { id: 'SUPPORT', label: '👥 Support Matrix & Escalation SLAs' },
          { id: 'CONTINUITY', label: '🛡 Business Continuity & DR' }
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

      {/* Go-Live Approval Checklist Table */}
      {activeTab === 'CHECKLIST' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
          <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
            <h2 className="text-sm font-bold font-serif text-[#111827]">Enterprise Production Readiness Gate</h2>
            <span className="text-xs font-bold text-[#6b5c4d]">4 Verified Milestones</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">Category</th>
                  <th className="px-6 py-3.5">Readiness Milestone</th>
                  <th className="px-6 py-3.5">Responsible Team</th>
                  <th className="px-6 py-3.5">Verification Notes</th>
                  <th className="px-6 py-3.5">Approval Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {checklist.map((item) => (
                  <tr key={item.item} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-bold text-[#111827]">{item.category}</td>
                    <td className="px-6 py-4 font-semibold text-[#111827]">{item.item}</td>
                    <td className="px-6 py-4 font-medium text-[#6b5c4d]">{item.owner}</td>
                    <td className="px-6 py-4 text-[#6b5c4d]">{item.notes}</td>
                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-emerald-50 text-emerald-800 border-emerald-200">
                        {item.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Incident Response Runbooks */}
      {activeTab === 'RUNBOOKS' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-6 shadow-xs space-y-4">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Automated SRE Incident Response Runbooks</h2>
          <div className="space-y-4">
            {MOCK_RUNBOOKS.map((rb) => (
              <div key={rb.id} className="p-4 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl space-y-2">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-[#111827] text-xs">{rb.title}</span>
                  <span className="font-mono text-[10px] font-bold text-[#E85D4F]">{rb.id} • {rb.category}</span>
                </div>
                <p className="text-xs text-[#6b5c4d] font-mono leading-relaxed bg-white p-3 rounded-lg border border-[#DDE4EA]">
                  {rb.steps}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
