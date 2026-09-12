import React, { useState } from 'react';
import { Server, Cpu, HardDrive, Terminal, GitCommit, CheckCircle2, RefreshCcw, Layers, ShieldCheck, Box, Activity, ArrowUpRight, Play } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_CONTAINERS = [
  {
    name: 'sareekart-frontend',
    image: 'sareekart-frontend:v2.4.0',
    port: '5173:80',
    memory: '38 MB / 512 MB',
    cpu: '0.8%',
    uptime: '6 Days 14 Hours',
    status: 'RUNNING'
  },
  {
    name: 'sareekart-backend',
    image: 'sareekart-backend:v2.4.0',
    port: '8081:8081',
    memory: '480 MB / 2048 MB',
    cpu: '2.4%',
    uptime: '6 Days 14 Hours',
    status: 'RUNNING'
  },
  {
    name: 'sareekart-redis',
    image: 'redis:7-alpine',
    port: '6379:6379',
    memory: '24 MB / 256 MB',
    cpu: '0.1%',
    uptime: '6 Days 14 Hours',
    status: 'RUNNING'
  },
  {
    name: 'sareekart-mysql',
    image: 'mysql:9.0',
    port: '3306:3306',
    memory: '840 MB / 4096 MB',
    cpu: '1.2%',
    uptime: '6 Days 14 Hours',
    status: 'RUNNING'
  }
];

export default function DevOpsDashboard() {
  const [activeTab, setActiveTab] = useState('CONTAINERS');
  const [containers, setContainers] = useState(MOCK_CONTAINERS);

  const handleTriggerBackup = () => {
    alert('Initiated automated MySQL point-in-time database snapshot. Uploading to encrypted S3 vault...');
  };

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise DevOps & Infrastructure Console | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">DevOps & Infrastructure Control Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Multi-stage Docker containers, Nginx reverse proxy, and automated disaster recovery</p>
        </div>
        <button
          onClick={handleTriggerBackup}
          className="h-11 px-5 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center gap-2 cursor-pointer"
        >
          <HardDrive className="w-4 h-4" /> Trigger DB Backup Snapshot
        </button>
      </div>

      {/* Telemetry Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <Box className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Active Containers</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">4 Containers</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <Cpu className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Total CPU Load</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">4.5% Overall</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Server className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Deployment SLA</p>
            <h3 className="text-xl font-bold text-emerald-800 mt-0.5">3.2 Minutes</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <HardDrive className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">RTO / RPO Target</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">&lt; 1h / &lt; 15m</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'CONTAINERS', label: '🐳 Container & Docker Status' },
          { id: 'PIPELINE', label: '🚀 CI/CD Pipeline Executions' },
          { id: 'LEDGER', label: '📜 Deployment History' },
          { id: 'DR', label: '💾 Disaster Recovery & S3 Backups' }
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

      {/* Docker Container Telemetry Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex justify-between items-center">
          <h2 className="text-sm font-bold font-serif text-[#111827]">Live Docker Compose Microservices</h2>
          <span className="text-xs font-bold text-[#6b5c4d]">4 Active Containers</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">Container Name</th>
                <th className="px-6 py-3.5">Image Tag</th>
                <th className="px-6 py-3.5">Port Mapping</th>
                <th className="px-6 py-3.5">RAM Memory</th>
                <th className="px-6 py-3.5">CPU Load</th>
                <th className="px-6 py-3.5">Uptime</th>
                <th className="px-6 py-3.5">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {containers.map((c) => (
                <tr key={c.name} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-bold text-[#111827]">{c.name}</td>
                  <td className="px-6 py-4 font-mono text-[#6b5c4d]">{c.image}</td>
                  <td className="px-6 py-4 font-mono font-bold text-[#E85D4F]">{c.port}</td>
                  <td className="px-6 py-4 font-bold text-[#111827]">{c.memory}</td>
                  <td className="px-6 py-4 font-bold text-emerald-800">{c.cpu}</td>
                  <td className="px-6 py-4 font-medium text-[#6b5c4d]">{c.uptime}</td>
                  <td className="px-6 py-4">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-emerald-50 text-emerald-800 border-emerald-200">
                      {c.status}
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
