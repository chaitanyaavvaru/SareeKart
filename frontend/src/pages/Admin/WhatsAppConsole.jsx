import React, { useState, useEffect } from 'react';
import { 
  MessageSquare, Send, CheckCheck, AlertTriangle, ShieldCheck, 
  RotateCcw, ExternalLink, Search, Filter, Loader2, Sparkles, Truck, Phone, RefreshCw, Eye,
  User, Bot, CheckCircle2, MessageCircle, ArrowRight
} from 'lucide-react';
import whatsAppService from '../../services/whatsAppService';
import WhatsAppNotificationModal from '../../components/whatsapp/WhatsAppNotificationModal';
import SEO from '../../components/common/SEO';

export default function WhatsAppConsole() {
  const [consoleTab, setConsoleTab] = useState('dispatch'); // 'dispatch' | 'clienteling'
  const [telemetry, setTelemetry] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEventType, setSelectedEventType] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeModalLog, setActiveModalLog] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [actionNotice, setActionNotice] = useState(null);

  // Clienteling state
  const [conversations, setConversations] = useState([]);
  const [convLoading, setConvLoading] = useState(false);
  const [convFilter, setConvFilter] = useState('ALL'); // 'ALL' | 'OPEN' | 'BOT_HANDLING' | 'CLOSED'
  const [selectedConv, setSelectedConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [msgLoading, setMsgLoading] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [sendingReply, setSendingReply] = useState(false);

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

  const loadConversations = async () => {
    setConvLoading(true);
    try {
      const list = await whatsAppService.getConversations(convFilter);
      setConversations(list || []);
      if (list && list.length > 0 && !selectedConv) {
        setSelectedConv(list[0]);
      }
    } catch (err) {
      console.error('Failed to load conversations:', err);
    } finally {
      setConvLoading(false);
    }
  };

  const loadMessages = async (convId) => {
    setMsgLoading(true);
    try {
      const msgList = await whatsAppService.getConversationMessages(convId);
      setMessages(msgList || []);
    } catch (err) {
      console.error('Failed to load messages for conversation:', err);
    } finally {
      setMsgLoading(false);
    }
  };

  useEffect(() => {
    if (consoleTab === 'dispatch') {
      loadData();
    } else {
      loadConversations();
    }
  }, [consoleTab, selectedEventType, convFilter]);

  useEffect(() => {
    if (selectedConv) {
      loadMessages(selectedConv.id);
    }
  }, [selectedConv]);

  const handleResend = async (logId) => {
    try {
      const resent = await whatsAppService.resendNotification(logId);
      setActionNotice({ type: 'success', message: `Notification #${logId} resent successfully as #${resent.id}!` });
      loadData();
    } catch (err) {
      setActionNotice({ type: 'error', message: err.response?.data?.message || 'Failed to resend notification' });
    }
  };

  const handleSendReply = async (e) => {
    e?.preventDefault();
    if (!selectedConv || !replyText.trim() || sendingReply) return;

    setSendingReply(true);
    try {
      await whatsAppService.replyToConversation(selectedConv.id, replyText.trim());
      setReplyText('');
      setActionNotice({ type: 'success', message: `Staff reply dispatched to +${selectedConv.contactPhone}!` });
      await loadMessages(selectedConv.id);
      loadConversations();
    } catch (err) {
      setActionNotice({ type: 'error', message: err.response?.data?.message || 'Failed to dispatch reply' });
    } finally {
      setSendingReply(false);
    }
  };

  const handleStatusChange = async (convId, newStatus) => {
    try {
      await whatsAppService.updateConversationStatus(convId, newStatus);
      setActionNotice({ type: 'success', message: `Conversation #${convId} status updated to ${newStatus}` });
      if (selectedConv && selectedConv.id === convId) {
        setSelectedConv(prev => ({ ...prev, status: newStatus, tags: newStatus === 'BOT_HANDLING' ? null : prev.tags }));
      }
      loadConversations();
    } catch (err) {
      setActionNotice({ type: 'error', message: 'Failed to update conversation status' });
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
      <SEO title="WhatsApp Dispatch & Clienteling Console | SareeKart Admin" noindex={true} />

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
            Automated luxury order milestones, Blue Dart dispatch pings, and live customer concierge escalations
          </p>
        </div>

        {consoleTab === 'dispatch' && (
          <button
            onClick={() => {
              setActiveModalLog(null);
              setIsModalOpen(true);
            }}
            className="h-11 px-5 bg-[#128C7E] hover:bg-[#075E54] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-md transition-all flex items-center gap-2 cursor-pointer"
          >
            <Send className="w-4 h-4" /> Simulate Dispatch Ping
          </button>
        )}
      </div>

      {/* Primary Top-Level Tabs */}
      <div className="flex items-center gap-2 border-b border-[#DDE4EA] pb-3">
        <button
          onClick={() => setConsoleTab('dispatch')}
          className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
            consoleTab === 'dispatch'
              ? 'bg-[#128C7E] text-white shadow-sm'
              : 'bg-white text-[#6b5c4d] border border-[#DDE4EA] hover:bg-gray-50'
          }`}
        >
          <Truck className="w-4 h-4" />
          <span>Dispatch & Audit Ledger</span>
        </button>

        <button
          onClick={() => setConsoleTab('clienteling')}
          className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer relative ${
            consoleTab === 'clienteling'
              ? 'bg-[#128C7E] text-white shadow-sm'
              : 'bg-white text-[#6b5c4d] border border-[#DDE4EA] hover:bg-gray-50'
          }`}
        >
          <MessageCircle className="w-4 h-4" />
          <span>Live Clienteling & Escalations</span>
          {conversations.some(c => c.status === 'OPEN' || c.tags?.includes('ESCALATED')) && (
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-ping"></span>
          )}
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

      {consoleTab === 'dispatch' ? (
        <>
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
        </>
      ) : (
        /* Live Clienteling & Escalations Tab View */
        <div className="space-y-6">
          {/* Sub-Filters */}
          <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-[#DDE4EA] shadow-xs">
            <div className="flex items-center gap-2">
              {[
                { id: 'ALL', label: 'All Conversations' },
                { id: 'OPEN', label: '⚠️ Escalated / Open' },
                { id: 'BOT_HANDLING', label: '🤖 AI Bot Active' },
                { id: 'CLOSED', label: 'Closed' }
              ].map(f => (
                <button
                  key={f.id}
                  onClick={() => setConvFilter(f.id)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    convFilter === f.id
                      ? 'bg-[#128C7E] text-white shadow-xs'
                      : 'bg-[#FAF8F5] text-[#6b5c4d] hover:bg-gray-100'
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>

            <button
              onClick={loadConversations}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-[#FAF8F5] hover:bg-gray-100 border border-[#DDE4EA] rounded-xl text-xs font-bold text-[#6b5c4d] transition-colors cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" /> Refresh Inbox
            </button>
          </div>

          {/* Two-Pane Messaging Interface */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            
            {/* Conversations List Pane */}
            <div className="lg:col-span-5 bg-white border border-[#DDE4EA] rounded-2xl shadow-xs overflow-hidden">
              <div className="p-4 border-b border-[#DDE4EA] bg-[#FAF8F5]">
                <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827]">
                  Patron Threads ({conversations.length})
                </h2>
              </div>

              {convLoading ? (
                <div className="py-16 flex flex-col items-center justify-center text-gray-500">
                  <Loader2 className="w-6 h-6 animate-spin text-[#128C7E]" />
                  <p className="mt-2 text-xs font-semibold">Loading patron conversations...</p>
                </div>
              ) : conversations.length === 0 ? (
                <div className="py-16 text-center text-gray-500 p-6">
                  <MessageCircle className="w-8 h-8 text-gray-400 mx-auto mb-2 opacity-50" />
                  <p className="text-xs font-bold">No active conversations found</p>
                  <p className="text-[11px] text-[#6b5c4d] mt-1">
                    Incoming customer WhatsApp queries will appear here automatically.
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-[#DDE4EA] max-h-[600px] overflow-y-auto">
                  {conversations.map(c => {
                    const isEscalated = c.status === 'OPEN' || c.tags?.includes('ESCALATED');
                    const isSelected = selectedConv?.id === c.id;

                    return (
                      <div
                        key={c.id}
                        onClick={() => setSelectedConv(c)}
                        className={`p-4 transition-colors cursor-pointer text-left ${
                          isSelected ? 'bg-[#128C7E]/5 border-l-4 border-l-[#128C7E]' : 'hover:bg-[#FAF8F5]'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-bold text-xs text-[#111827] flex items-center gap-1.5">
                            {c.contactName || 'Patron'}
                            {c.linkedUserId && (
                              <span className="text-[9px] px-1.5 py-0.2 bg-purple-50 text-purple-700 border border-purple-200 rounded font-bold">
                                Member
                              </span>
                            )}
                          </span>
                          <span className="text-[10px] text-gray-400 font-mono">
                            {c.contactPhone}
                          </span>
                        </div>

                        <div className="flex items-center gap-2 mb-2">
                          {isEscalated ? (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-900 border border-amber-300 flex items-center gap-1 animate-pulse">
                              <AlertTriangle className="w-3 h-3 text-amber-700" /> Escalated to Stylist
                            </span>
                          ) : c.status === 'BOT_HANDLING' ? (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 flex items-center gap-1">
                              <Bot className="w-3 h-3 text-emerald-600" /> AI Bot Active
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-gray-100 text-gray-700 border border-gray-200">
                              Closed
                            </span>
                          )}
                        </div>

                        <p className="text-xs text-[#6b5c4d] line-clamp-2">
                          {c.lastMessageSnippet || 'No messages yet'}
                        </p>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Conversation Messages Thread & Direct Reply Pane */}
            <div className="lg:col-span-7 bg-white border border-[#DDE4EA] rounded-2xl shadow-xs overflow-hidden flex flex-col h-[650px]">
              {selectedConv ? (
                <>
                  {/* Thread Header */}
                  <div className="p-4 border-b border-[#DDE4EA] bg-[#FAF8F5] flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-sm text-[#111827] flex items-center gap-2">
                        {selectedConv.contactName || 'Patron'}
                        <span className="text-xs font-mono text-gray-500 font-normal">
                          +{selectedConv.contactPhone}
                        </span>
                      </h3>
                      <p className="text-[11px] text-[#6b5c4d] mt-0.5">
                        Conversation #{selectedConv.id} • {selectedConv.status}
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      {selectedConv.status !== 'BOT_HANDLING' && (
                        <button
                          onClick={() => handleStatusChange(selectedConv.id, 'BOT_HANDLING')}
                          className="px-2.5 py-1 bg-white hover:bg-emerald-50 border border-emerald-300 text-emerald-800 text-[11px] font-bold rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                          title="Transfer back to AI Stylist Bot"
                        >
                          <Bot className="w-3 h-3 text-emerald-600" /> Re-assign to Bot
                        </button>
                      )}

                      {selectedConv.status !== 'OPEN' && (
                        <button
                          onClick={() => handleStatusChange(selectedConv.id, 'OPEN')}
                          className="px-2.5 py-1 bg-white hover:bg-amber-50 border border-amber-300 text-amber-800 text-[11px] font-bold rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                          title="Take Over Conversation as Human Stylist"
                        >
                          <User className="w-3 h-3 text-amber-600" /> Take Over
                        </button>
                      )}

                      {selectedConv.status !== 'CLOSED' && (
                        <button
                          onClick={() => handleStatusChange(selectedConv.id, 'CLOSED')}
                          className="px-2.5 py-1 bg-white hover:bg-gray-100 border border-gray-300 text-gray-700 text-[11px] font-bold rounded-lg transition-colors cursor-pointer"
                        >
                          Resolve & Close
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Messages Scroll Area */}
                  <div className="flex-1 p-4 overflow-y-auto space-y-3 bg-[#FAF8F5]/50">
                    {msgLoading ? (
                      <div className="py-20 flex flex-col items-center justify-center text-gray-500">
                        <Loader2 className="w-6 h-6 animate-spin text-[#128C7E]" />
                        <p className="mt-2 text-xs font-semibold">Loading messages...</p>
                      </div>
                    ) : messages.length === 0 ? (
                      <div className="py-20 text-center text-gray-500">
                        <p className="text-xs">No messages recorded in this conversation.</p>
                      </div>
                    ) : (
                      messages.map(m => {
                        const isCustomer = m.senderType === 'CUSTOMER';
                        return (
                          <div
                            key={m.id}
                            className={`flex flex-col ${isCustomer ? 'items-start' : 'items-end'}`}
                          >
                            <div
                              className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-xs shadow-xs ${
                                isCustomer
                                  ? 'bg-white border border-[#DDE4EA] text-gray-900 rounded-tl-none'
                                  : 'bg-[#128C7E] text-white rounded-tr-none'
                              }`}
                            >
                              <div className="flex items-center justify-between gap-4 mb-1 text-[10px] opacity-75">
                                <span className="font-bold uppercase tracking-wider">
                                  {isCustomer ? 'Customer' : m.senderType === 'ADMIN' ? 'Staff Stylist' : 'AI Concierge'}
                                </span>
                                <span>{m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                              </div>
                              <p className="whitespace-pre-wrap leading-relaxed">{m.content}</p>
                            </div>
                            {m.deliveryStatus && !isCustomer && (
                              <span className="text-[9px] text-gray-400 mt-0.5 mr-1 font-mono">
                                {m.deliveryStatus}
                              </span>
                            )}
                          </div>
                        );
                      })
                    )}
                  </div>

                  {/* Reply Input Bar */}
                  <form onSubmit={handleSendReply} className="p-3 border-t border-[#DDE4EA] bg-white flex items-center gap-2">
                    <input
                      type="text"
                      placeholder="Type a response as Master Stylist to send via WhatsApp..."
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      className="flex-1 h-10 px-4 bg-[#FAF8F5] border border-[#DDE4EA] rounded-xl text-xs font-medium focus:outline-none focus:border-[#128C7E]"
                    />
                    <button
                      type="submit"
                      disabled={sendingReply || !replyText.trim()}
                      className="h-10 px-5 bg-[#128C7E] hover:bg-[#075E54] disabled:opacity-50 text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-xs transition-all flex items-center gap-1.5 cursor-pointer shrink-0"
                    >
                      {sendingReply ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                      Reply
                    </button>
                  </form>
                </>
              ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-gray-400 p-8 text-center">
                  <MessageCircle className="w-12 h-12 mb-3 opacity-30 text-[#128C7E]" />
                  <p className="text-sm font-bold text-gray-700">Select a Patron Conversation</p>
                  <p className="text-xs text-[#6b5c4d] mt-1 max-w-sm">
                    Select any conversation from the list to inspect customer queries, view recommendations, or respond directly as a boutique stylist.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

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
