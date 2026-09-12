import React, { useState } from 'react';
import { IndianRupee, TrendingUp, ShieldCheck, FileText, Download, Check, AlertCircle, ArrowUpRight, ArrowDownLeft, RefreshCcw, Search, Filter, Lock, DollarSign, PieChart, CreditCard, Building } from 'lucide-react';
import SEO from '../../components/common/SEO';
import { motion, AnimatePresence } from 'framer-motion';

const MOCK_FINANCE_STATS = {
  grossRevenue: 2450000,
  netRevenue: 2205000,
  discountImpact: 245000,
  shippingExpenses: 120000,
  gatewayFees: 49000,
  gstLiability: 122500,
  vendorPayoutsPending: 85000,
  netMarginPercent: 32.4
};

const MOCK_LEDGER = [
  {
    id: 'TXN-98124',
    date: '2026-06-12',
    orderId: '#9812',
    type: 'REVENUE',
    customer: 'Chaitanya Chaitu',
    grossAmount: 120000,
    gstTax: 6000,
    gatewayFee: 2400,
    netAmount: 111600,
    gatewayStatus: 'RAZORPAY_SETTLED',
    reconciled: true
  },
  {
    id: 'TXN-98125',
    date: '2026-06-11',
    orderId: '#9811',
    type: 'PAYOUT',
    vendor: 'Varanasi Weaver Cooperative',
    grossAmount: 85000,
    commissionDeduction: 12750,
    tcsTax: 850,
    netAmount: 71400,
    gatewayStatus: 'BANK_TRANSFERRED',
    reconciled: true
  },
  {
    id: 'TXN-98126',
    date: '2026-06-10',
    orderId: '#9810',
    type: 'EXPENSE',
    vendor: 'BlueDart Logistics',
    grossAmount: 1200,
    gstTax: 60,
    gatewayFee: 0,
    netAmount: -1260,
    gatewayStatus: 'INVOICE_SETTLED',
    reconciled: true
  }
];

const formatCurrency = (val) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0,
  }).format(val || 0);

export default function ManageFinance() {
  const [activeTab, setActiveTab] = useState('LEDGER');
  const [ledger, setLedger] = useState(MOCK_LEDGER);
  const [searchTerm, setSearchTerm] = useState('');

  const filteredLedger = ledger.filter(item =>
    item.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (item.orderId && item.orderId.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleDownloadReport = (reportName) => {
    alert(`Downloading ${reportName} (PDF/CSV) format...`);
  };

  return (
    <div className="text-left space-y-8 animate-fade-in font-sans text-[#111827]">
      <SEO title="Finance, Accounting & Taxation | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold font-serif text-[#111827]">Finance, Taxation & Payout Console</h1>
          <p className="text-xs text-[#6b5c4d] mt-1">Payment gateway reconciliation, GST compliance, and vendor payout ledger</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => handleDownloadReport('GST Sales Register')}
            className="h-11 px-4 bg-white border border-[#DDE4EA] hover:bg-[#F5F7FA] text-[#111827] font-bold text-xs uppercase tracking-wider rounded-full shadow-xs transition-all flex items-center gap-2 cursor-pointer"
          >
            <Download className="w-4 h-4 text-[#E85D4F]" /> Export GST Tax Register
          </button>
          <button
            onClick={() => handleDownloadReport('Financial P&L Statement')}
            className="h-11 px-4 bg-[#111827] hover:bg-[#243B6B] text-[#E85D4F] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center gap-2 cursor-pointer"
          >
            <FileText className="w-4 h-4" /> Download P&L Statement
          </button>
        </div>
      </div>

      {/* Financial Telemetry Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-6">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
            <IndianRupee className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Gross Realized Sales</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">{formatCurrency(MOCK_FINANCE_STATS.grossRevenue)}</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Net Margin (Est.)</p>
            <h3 className="text-xl font-bold text-purple-800 mt-0.5">{MOCK_FINANCE_STATS.netMarginPercent}% (₹7.94L)</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">GST Tax Liability (5%)</p>
            <h3 className="text-xl font-bold text-amber-800 mt-0.5">{formatCurrency(MOCK_FINANCE_STATS.gstLiability)}</h3>
          </div>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
            <CreditCard className="w-6 h-6" />
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">Pending Vendor Payouts</p>
            <h3 className="text-xl font-bold text-[#111827] mt-0.5">{formatCurrency(MOCK_FINANCE_STATS.vendorPayoutsPending)}</h3>
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-2 flex gap-2 overflow-x-auto shadow-xs">
        {[
          { id: 'LEDGER', label: '💰 Executive Ledger' },
          { id: 'TAX', label: '🧾 Tax & GST Invoices (HSN 5007)' },
          { id: 'RECONCILIATION', label: '💳 Payment Gateway Sync' },
          { id: 'PAYOUTS', label: '👥 Vendor Payouts' },
          { id: 'PL', label: '📈 P&L Statement' },
          { id: 'AUDIT', label: '📋 Financial Audit Logs' }
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

      {/* Main Ledger Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs space-y-4">
        <div className="p-4 border-b border-[#DDE4EA] bg-[#F5F7FA] flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-2 bg-white border border-[#DDE4EA] rounded-xl px-3 py-2 w-full sm:w-80">
            <Search className="w-4 h-4 text-[#6b5c4d]" />
            <input
              type="text"
              placeholder="Search transaction ID or order ID..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="bg-transparent border-none outline-none text-xs w-full text-[#111827]"
            />
          </div>
          <span className="text-xs font-bold text-[#6b5c4d]">Showing {filteredLedger.length} Financial Ledger Records</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-[#F5F7FA] text-[#6b5c4d] uppercase tracking-wider text-[10px] font-bold border-b border-[#DDE4EA]">
              <tr>
                <th className="px-6 py-3.5">Txn ID / Date</th>
                <th className="px-6 py-3.5">Category Type</th>
                <th className="px-6 py-3.5">Order / Party</th>
                <th className="px-6 py-3.5 text-right">Gross Valuation</th>
                <th className="px-6 py-3.5 text-right">GST (5%) / Tax</th>
                <th className="px-6 py-3.5 text-right">Net Ledger Valuation</th>
                <th className="px-6 py-3.5">Reconciliation Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#DDE4EA] text-[#111827]">
              {filteredLedger.map((txn) => (
                <tr key={txn.id} className="hover:bg-[#F5F7FA]/60 transition-colors">
                  <td className="px-6 py-4 font-mono font-bold text-[#111827]">
                    <div>
                      <span className="block">{txn.id}</span>
                      <span className="text-[10px] text-[#6b5c4d] font-sans block">{txn.date}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 font-bold">
                    <span className={`px-2.5 py-1 rounded-full text-[10px] uppercase tracking-wider border ${
                      txn.type === 'REVENUE' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                      txn.type === 'PAYOUT' ? 'bg-blue-50 text-blue-800 border-blue-200' :
                      'bg-red-50 text-red-800 border-red-200'
                    }`}>
                      {txn.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-medium">
                    <div>
                      <span className="font-bold text-[#111827] block">{txn.customer || txn.vendor}</span>
                      <span className="text-[10px] font-mono text-[#E85D4F] block">{txn.orderId}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right font-bold text-[#111827]">{formatCurrency(txn.grossAmount)}</td>
                  <td className="px-6 py-4 text-right font-medium text-amber-700">{formatCurrency(txn.gstTax || txn.tcsTax || 0)}</td>
                  <td className="px-6 py-4 text-right font-extrabold text-emerald-800">{formatCurrency(txn.netAmount)}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1.5 text-[10px] font-bold text-emerald-700">
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                      <span>{txn.gatewayStatus}</span>
                    </div>
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
