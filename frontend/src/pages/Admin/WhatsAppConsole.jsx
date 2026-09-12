import React, { useState, useEffect } from 'react';
import { 
  MessageSquare, Send, CheckCheck, AlertTriangle, ShieldCheck, 
  RotateCcw, ExternalLink, Search, Filter, Loader2, Sparkles, Truck, Phone, RefreshCw, Eye
} from 'lucide-react';
import whatsAppService from '../../services/whatsAppService';
import WhatsAppNotificationModal from '../../components/whatsapp/WhatsAppNotificationModal';
import SEO from '../../components/common/SEO';

export default function WhatsAppConsole() {
  const [telemetry, setTelemetry] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEventType, setSelectedEventType] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeModalLog, setActiveModalLog] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [actionNotice, setActionNotice] = useState(null);

  useEffect(() => {
    loadData();
  }, [selectedEventType]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [teleData, logData] = await Promise.all([
        whatsAppService.getTelemetry(),
        whatsAppService.getAdminLogs(selectedEventType, 0, 50)
      ]);
      setTelemetry(teleData);
      setLogs(logData?.content || []);
    } catch (err) {
      console.error('Failed to load WhatsApp data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async (logId) => {
    try {
      const resent = await whatsAppService.resendNotification(logId);
      setActionNotice({ type: 'success', message: `Notification #${logId} resent successfully as #${resent.id}!` });
      loadData();
    } catch (err) {
      setActionNotice({ type: 'error', message: err.response?.data?.message || 'Failed to resend notification' });
    }
  };

  const filteredLogs = logs.filter(l => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      (l.recipientName && l.recipientName.toLowerCase().includes(q)) ||
      (l.recipientPhone && l.recipientPhone.includes(q)) ||
      (l.orderId && String(l.orderId).includes(q)) ||
      (l.trackingNumber && l.trackingNumber.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-8 animate-fade-in font-sans text-left pb-12">
      <SEO title="WhatsApp Dispatch & Clienteling Console | SareeKart Admin" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold font-serif text-[#111827]">WhatsApp Dispatch & Clienteling</h1>
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#128C7E]/10 text-[#128C7E] border border-[#128C7E]/20 uppercase">
              Meta Cloud API & Sandbox
            </span>
          </div>
          <p className="text-xs text-[#6b5c4d] mt-1">
            Automated luxury order milestones, Blue Dart dispatch pings, and reverse pickup clienteling
          </p>
        </div>

        <button
          onClick={() => {
            setActiveModalLog(null);
            setIsModalOpen(true);
          }}
          className="h-11 px-5 bg-[#128C7E] hover:bg-[#075E54] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-md transition-all flex items-center gap-2 cursor-pointer"
        >
          <Send className="w-4 h-4" /> Simulate Dispatch Ping
        </button>
      </div>

      {actionNotice && (
        <div className={`p-4 rounded-2xl text-xs font-bold flex items-center justify-between shadow-xs ${
          actionNotice.type === 'success' ? 'bg-emerald-50 text-emerald-900 border border-emerald-200' : 'bg-red-50 text-red-900 border border-red-200'
        }`}>
          <span>{actionNotice.message}</span>
          <button onClick={() => setActionNotice(null)} className="cursor-pointer">✕</button>
        </div>
      )}

      {/* Telemetry KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-[#6b5c4d]">
            <span className="text-[11px] font-bold uppercase tracking-wider">Total Dispatched</span>
            <MessageSquare className="w-4 h-4 text-[#128C7E]" />
          </div>
          <div className="mt-2 text-2xl font-bold text-[#111827]">
            {telemetry?.totalDispatched ?? 0}
          </div>
          <p className="text-[10px] text-gray-500 mt-1">
            {telemetry?.liveCount ?? 0} live • {telemetry?.simulatedCount ?? 0} simulated
          </p>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-[#6b5c4d]">
            <span className="text-[11px] font-bold uppercase tracking-wider">Delivery Success</span>
            <CheckCheck className="w-4 h-4 text-[#0F766E]" />
          </div>
          <div className="mt-2 text-2xl font-bold text-[#0F766E]">
            {telemetry?.deliveryRatePercent ?? 100}%
          </div>
          <p className="text-[10px] text-gray-500 mt-1">
            {telemetry?.deliveredCount ?? 0} delivered • {telemetry?.failedCount ?? 0} failed
          </p>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-[#6b5c4d]">
            <span className="text-[11px] font-bold uppercase tracking-wider">Dispatch Milestones</span>
            <Truck className="w-4 h-4 text-blue-600" />
          </div>
          <div className="mt-2 text-2xl font-bold text-[#111827]">
            {telemetry?.eventBreakdown?.SHIPPED ?? 0}
          </div>
          <p className="text-[10px] text-gray-500 mt-1">
            En route with courier tracking AWB
          </p>
        </div>

        <div className="bg-white border border-[#DDE4EA] rounded-2xl p-5 shadow-xs">
          <div className="flex items-center justify-between text-[#6b5c4d]">
            <span className="text-[11px] font-bold uppercase tracking-wider">Return Pickups</span>
            <RotateCcw className="w-4 h-4 text-purple-600" />
          </div>
          <div className="mt-2 text-2xl font-bold text-[#111827]">
            {telemetry?.eventBreakdown?.RETURN_PICKUP ?? 0}
          </div>
          <p className="text-[10px] text-gray-500 mt-1">
            Reverse courier collections assigned
          </p>
        </div>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-4 shadow-xs flex flex-col md:flex-row items-center justify-between gap-4">
        
        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-2 md:pb-0">
          {[
            { id: 'ALL', label: 'All Alerts' },
            { id: 'ORDER_CONFIRMED', label: 'Confirmed' },
            { id: 'SHIPPED', label: 'Shipped' },
            { id: 'OUT_FOR_DELIVERY', label: 'Out for Delivery' },
            { id: 'DELIVERED', label: 'Delivered' },
            { id: 'RETURN_PICKUP', label: 'Returns' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setSelectedEventType(tab.id)}
              className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer ${
                selectedEventType === tab.id
                  ? 'bg-[#128C7E] text-white shadow-xs'
                  : 'bg-[#FAF8F5] text-[#6b5c4d] hover:bg-gray-100'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative w-full md:w-72">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search order #, phone, patron..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full h-9 pl-9 pr-4 bg-[#FAF8F5] border border-[#DDE4EA] rounded-xl text-xs font-medium focus:outline-none focus:border-[#128C7E]"
          />
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl overflow-hidden shadow-xs">
        <div className="p-4 border-b border-[#DDE4EA] flex items-center justify-between">
          <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827]">
            Real-time Outbound Dispatch Ledger ({filteredLogs.length} entries)
          </h2>
          <button
            onClick={loadData}
            className="p-1.5 hover:bg-gray-100 rounded-lg text-gray-500 transition-colors cursor-pointer"
            title="Refresh Ledger"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center text-gray-500">
            <Loader2 className="w-8 h-8 animate-spin text-[#128C7E]" />
            <p className="mt-2 text-xs font-semibold">Fetching WhatsApp dispatch records...</p>
          </div>
        ) : filteredLogs.length === 0 ? (
          <div className="py-16 text-center text-gray-500">
            <MessageSquare className="w-8 h-8 text-gray-400 mx-auto mb-2 opacity-50" />
            <p className="text-xs font-bold">No WhatsApp notification logs found</p>
            <p className="text-[11px] text-[#6b5c4d] mt-1">Try simulating an alert using the button above.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#FAF8F5] border-b border-[#DDE4EA] text-[10px] uppercase font-bold text-[#6b5c4d] tracking-wider">
                <tr>
                  <th className="py-3.5 px-4">Log ID</th>
                  <th className="py-3.5 px-4">Order / Claim</th>
                  <th className="py-3.5 px-4">Recipient</th>
                  <th className="py-3.5 px-4">Milestone Event</th>
                  <th className="py-3.5 px-4">Courier / AWB</th>
                  <th className="py-3.5 px-4">Delivery Status</th>
                  <th className="py-3.5 px-4">Dispatched At</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#DDE4EA]">
                {filteredLogs.map(log => (
                  <tr key={log.id} className="hover:bg-[#FAF8F5]/80 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-gray-700">
                      #{log.id}
                    </td>
                    <td className="py-3 px-4 font-bold text-[#111827]">
                      {log.orderId ? (
                        <span className="text-[#128C7E]">Order #{log.orderId}</span>
                      ) : log.returnRequestId ? (
                        <span className="text-purple-700">Claim #{log.returnRequestId}</span>
                      ) : (
                        <span className="text-gray-400">Direct Ping</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <p className="font-bold text-[#111827]">{log.recipientName}</p>
                      <p className="text-[10px] text-[#6b5c4d] font-mono">{log.recipientPhone}</p>
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-block px-2.5 py-1 rounded-md text-[10px] font-bold bg-[#FAF8F5] border border-[#DDE4EA] text-gray-800 tracking-wide">
                        {log.eventType}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      {log.trackingNumber ? (
                        <div>
                          <p className="font-semibold text-gray-900">{log.courierPartner || 'Courier'}</p>
                          <p className="text-[10px] font-mono text-gray-500">{log.trackingNumber}</p>
                        </div>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        log.deliveryStatus === 'DELIVERED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : log.deliveryStatus === 'SIMULATED'
                          ? 'bg-blue-100 text-blue-800'
                          : log.deliveryStatus === 'SUPPRESSED'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {log.deliveryStatus === 'DELIVERED' && <CheckCheck className="w-3 h-3 text-emerald-600" />}
                        {log.deliveryStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-gray-500 whitespace-nowrap">
                      {log.createdAtFormatted}
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => {
                            setActiveModalLog(log);
                            setIsModalOpen(true);
                          }}
                          className="p-1.5 text-gray-600 hover:text-[#128C7E] hover:bg-[#128C7E]/10 rounded-lg transition-colors cursor-pointer"
                          title="Preview Smartphone Chat View"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleResend(log.id)}
                          className="p-1.5 text-gray-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                          title="Resend / Re-simulate Ping"
                        >
                          <RotateCcw className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Smartphone Chat Preview & Simulation Modal */}
      <WhatsAppNotificationModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        initialLog={activeModalLog}
        onDispatched={() => {
          loadData();
        }}
      />
    </div>
  );
}
