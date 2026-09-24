import React, { useState } from 'react';
import { Shield, ShieldAlert, Key, Lock, UserCheck, AlertTriangle, FileText, CheckCircle2, XCircle, Search, Filter, RefreshCcw, Cpu, Eye, Lock as LockIcon } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_AUDIT_LOGS = [
  {
    id: 'AUD-9012',
    timestamp: 'Today, 10:42 AM',
    user: 'admin@sareekart.com',
    role: 'ROLE_ADMIN',
    action: 'RELEASE_VENDOR_PAYOUT',
    details: 'Approved ₹85,000 payout to Varanasi Weaver Cooperative',
    ip: '103.22.14.1',
    status: 'SUCCESS'
  },
  {
    id: 'AUD-9011',
    timestamp: 'Today, 09:15 AM',
    user: 'anonymous_user',
    role: 'UNAUTHENTICATED',
    action: 'FAILED_LOGIN_ATTEMPT',
    details: 'Invalid password credential check on admin route',
    ip: '185.220.101.4',
    status: 'BLOCKED'
  },
  {
    id: 'AUD-9010',
    timestamp: 'Yesterday, 04:30 PM',
    user: 'customer@sareekart.com',
    role: 'ROLE_CUSTOMER',
    action: 'UPDATE_PASSWORD',
    details: 'BCrypt hash updated via OTP verification',
    ip: '49.37.120.12',
    status: 'SUCCESS'
  }
];

const MOCK_ROLES = [
  { role: 'ROLE_CUSTOMER', count: 1240, access: 'Storefront Read / Buy, Customer Profile, Order History' },
  { role: 'ROLE_VENDOR', count: 34, access: 'Vendor Inventory Console, Product Submission, Payout Ledger' },
  { role: 'ROLE_WAREHOUSE', count: 8, access: 'Multi-Warehouse Bin Mapping, Stock Intake, Dispatch Marking' },
  { role: 'ROLE_FINANCE', count: 4, access: 'P&L Reports, GST Tax Invoices, Gateway Reconciliation, Payout Release' },
  { role: 'ROLE_ADMIN', count: 3, access: 'Full Administrative Portal, User Management, CMS Publishing' },
  { role: 'ROLE_SUPERADMIN', count: 1, access: 'Unrestricted System Access, IAM Security Policies, Database Backup' }
];

export default function SecurityDashboard() {
  const [activeTab, setActiveTab] = useState('LOGS');
  const [auditLogs, setAuditLogs] = useState(MOCK_AUDIT_LOGS);
  const [searchTerm, setSearchTerm] = useState('');

  const filteredLogs = auditLogs.filter(log =>
    log.user.toLowerCase().includes(searchTerm.toLowerCase()) ||
    log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
    log.ip.includes(searchTerm)
  );

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Enterprise Security & IAM Vault | SareeKart Admin" noindex={true} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Enterprise Security & IAM Vault</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">OWASP ASVS compliance, RBAC authorization matrix, and immutable security audit logs</p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-full">
          <Shield className="w-4 h-4 text-emerald-600" />
          <span className="text-xs font-bold text-emerald-800">Security Posture: 98 / 100 (Optimal)</span>
        </div>
      </div>

      {/* Telemetry Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <UserCheck className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Active User Sessions</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">142 Sessions</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Failed Logins (24h)</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">3 Blocked</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-100">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Blocked IPs</p>
            <h3 className="text-xl font-bold text-red-800 mt-0.5">2 Addresses</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <Lock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Encryption Standard</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">AES-256 GCM</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'LOGS', label: '📋 Immutable Security Audit Ledger' },
          { id: 'ROLES', label: '👥 IAM & RBAC Matrix' },
          { id: 'OWASP', label: '🛡 OWASP Compliance Checklist' },
          { id: 'SESSIONS', label: '💻 Active Session Inspector' }
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

      {/* Tab Content 1: Immutable Security Audit Ledger */}
      {activeTab === 'LOGS' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
          <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex flex-col sm:flex-row justify-between items-center gap-4">
            <div className="flex items-center gap-2 bg-white border border-[#DDE4EA] rounded-xl px-3 py-2 w-full sm:w-80">
              <Search className="w-4 h-4 text-[#6b5c4d]" />
              <input
                type="text"
                placeholder="Search user, action or IP address..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                className="bg-transparent border-none outline-none text-xs w-full text-[#111827]"
              />
            </div>
            <span className="text-xs font-bold text-[#6b5c4d]">Showing {filteredLogs.length} Audit Events</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
                <tr>
                  <th className="px-6 py-3.5">Log ID / Timestamp</th>
                  <th className="px-6 py-3.5">User Identity & Role</th>
                  <th className="px-6 py-3.5">Security Action</th>
                  <th className="px-6 py-3.5">Action Details</th>
                  <th className="px-6 py-3.5">IP Address</th>
                  <th className="px-6 py-3.5">Outcome</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
                {filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-[#F5F7FA]/60 transition-colors">
                    <td className="px-6 py-4 font-mono font-bold text-[#111827]">
                      <div>
                        <span className="block">{log.id}</span>
                        <span className="text-[10px] text-[#6b5c4d] font-sans block">{log.timestamp}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-medium">
                      <div>
                        <span className="font-bold text-[#111827] block">{log.user}</span>
                        <span className="text-[10px] font-mono text-[#E85D4F] uppercase block">{log.role}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono font-bold text-[#111827]">{log.action}</td>
                    <td className="px-6 py-4 text-[#6b5c4d]">{log.details}</td>
                    <td className="px-6 py-4 font-mono text-[#6b5c4d]">{log.ip}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                        log.status === 'SUCCESS' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                        'bg-red-50 text-red-800 border-red-200'
                      }`}>
                        {log.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab Content 2: IAM & RBAC Matrix */}
      {activeTab === 'ROLES' && (
        <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs p-6 space-y-4">
          <h2 className="text-sm font-bold font-serif text-[#111827]">System Role-Based Access Control (RBAC) Hierarchy</h2>
          <div className="divide-y divide-[#DDE4EA]">
            {MOCK_ROLES.map((r) => (
              <div key={r.role} className="py-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                <div>
                  <span className="font-mono font-bold text-xs text-[#111827] bg-[#F5F7FA] px-2.5 py-1 rounded-md border border-[#DDE4EA]">{r.role}</span>
                  <p className="text-xs text-[#6b5c4d] mt-2">{r.access}</p>
                </div>
                <span className="text-xs font-bold text-[#E85D4F] bg-[#F5F7FA] px-3 py-1 rounded-full border border-[#DDE4EA] shrink-0">
                  {r.count} Assigned Accounts
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
