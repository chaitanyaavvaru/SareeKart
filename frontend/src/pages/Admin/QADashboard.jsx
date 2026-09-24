import React, { useState } from 'react';
import { CheckCircle2, ShieldCheck, Cpu, Code2, GitPullRequest, AlertCircle, FileCheck, Layers, Play, RefreshCcw, Search, BarChart2 } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_TEST_SUITES = [
  {
    name: 'OrderCheckoutFlow.spec.js',
    type: 'Playwright E2E',
    duration: '42s',
    testsPassed: '14 / 14',
    coverage: '100%',
    status: 'PASSED'
  },
  {
    name: 'InventoryServiceTest.java',
    type: 'JUnit 5 + Mockito',
    duration: '1.2s',
    testsPassed: '28 / 28',
    coverage: '92.1%',
    status: 'PASSED'
  },
  {
    name: 'CartSlice.test.jsx',
    type: 'Vitest + RTL',
    duration: '0.8s',
    testsPassed: '18 / 18',
    coverage: '94.5%',
    status: 'PASSED'
  },
  {
    name: 'AuthEndpointsApiTest.java',
    type: 'REST Assured API',
    duration: '3.4s',
    testsPassed: '32 / 32',
    coverage: '89.0%',
    status: 'PASSED'
  }
];

export default function QADashboard() {
  const [activeTab, setActiveTab] = useState('LEDGER');
  const [suites, setSuites] = useState(MOCK_TEST_SUITES);

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise QA & Test Automation Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Quality Assurance & CI/CD Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Martin Fowler's test pyramid telemetry, SonarQube static analysis, and CI/CD quality gates</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          <span className="text-xs font-bold text-emerald-800">CI/CD Quality Gate: PASSED (Zero Defects)</span>
        </div>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Overall Code Coverage</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">88.4% Line Cover</h3>
            <p className="text-[10px] text-emerald-600 font-semibold mt-0.5">Target: &gt; 85% (Passed)</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Cpu className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Build Pass Rate</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">99.2% Passed</h3>
            <p className="text-[10px] text-purple-600 font-semibold mt-0.5">Last 30 Builds</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Code2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Automated Test Suites</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">642 Suites</h3>
            <p className="text-[10px] text-[#6b5c4d] font-semibold mt-0.5">Unit, API & E2E</p>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <GitPullRequest className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">SonarQube Blocker Flaws</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">0 Defects</h3>
            <p className="text-[10px] text-emerald-600 font-semibold mt-0.5">Clean Code Rating: A</p>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'LEDGER', label: '🧪 Test Execution Ledger' },
          { id: 'PYRAMID', label: '📐 Test Pyramid & Coverage' },
          { id: 'GATES', label: '🚦 CI/CD Quality Gates' },
          { id: 'RELEASE', label: '🚀 Release Candidate Notes' }
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

      {/* Test Execution Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Automated Test Suite Execution Ledger</h2>
          <span className="text-xs font-bold text-[#6b5c4d]">4 Executed Test Files</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">Test File Name</th>
                <th className="px-6 py-3.5">Testing Framework</th>
                <th className="px-6 py-3.5">Execution Duration</th>
                <th className="px-6 py-3.5">Passed Assertions</th>
                <th className="px-6 py-3.5">Code Coverage</th>
                <th className="px-6 py-3.5">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {suites.map((s) => (
                <tr key={s.name} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-[#111827]">{s.name}</td>
                  <td className="px-6 py-4 font-medium text-[#6b5c4d]">{s.type}</td>
                  <td className="px-6 py-4 font-bold text-[#111827]">{s.duration}</td>
                  <td className="px-6 py-4 font-bold text-emerald-800">{s.testsPassed}</td>
                  <td className="px-6 py-4 font-bold text-purple-800">{s.coverage}</td>
                  <td className="px-6 py-4">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-emerald-50 text-emerald-800 border-emerald-200">
                      {s.status}
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
