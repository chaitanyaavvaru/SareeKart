import React, { useState, useEffect } from 'react';
import { 
  X, MessageSquare, CheckCheck, Truck, ShieldCheck, 
  ExternalLink, Calendar, Loader2, ArrowRight
} from 'lucide-react';
import whatsAppService from '../../services/whatsAppService';

export default function WhatsAppTimelineDrawer({
  isOpen,
  onClose,
  order
}) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadTimeline = async () => {
    setLoading(true);
    try {
      const data = await whatsAppService.getOrderTimeline(order.id);
      setLogs(data);
    } catch (err) {
      console.error('Failed to load WhatsApp timeline:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && order?.id) {
      loadTimeline();
    }
  }, [isOpen, order?.id]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/50 backdrop-blur-xs animate-fade-in flex justify-end">
      <div className="w-full max-w-md bg-[#FAF8F5] h-full shadow-2xl flex flex-col justify-between border-l border-[#E8E1D7]">
        
        {/* Header */}
        <div className="p-6 bg-[#075E54] text-white flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#128C7E] flex items-center justify-center text-amber-200 font-serif font-bold text-sm border border-white/20">
              SK
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-sm font-serif">SareeKart WhatsApp Updates</h3>
                <span className="text-xs text-emerald-300">✔</span>
              </div>
              <p className="text-xs text-emerald-100/80">Order #{order?.id} • Live Clienteling</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Chat Timeline Body */}
        <div className="flex-1 p-5 overflow-y-auto bg-[#ECE5DD] space-y-4 text-left">
          
          <div className="flex justify-center">
            <span className="px-3 py-1 bg-white/80 backdrop-blur-xs rounded-full text-[10px] font-bold text-gray-600 shadow-2xs uppercase tracking-wider">
              Official SareeKart Verified Channel
            </span>
          </div>

          {loading ? (
            <div className="flex flex-col items-center justify-center py-16 text-gray-500">
              <Loader2 className="w-8 h-8 animate-spin text-[#128C7E]" />
              <p className="mt-2 text-xs font-semibold">Loading dispatch timeline...</p>
            </div>
          ) : logs.length === 0 ? (
            <div className="bg-white/90 rounded-2xl p-6 text-center border border-[#DDE4EA] shadow-xs">
              <MessageSquare className="w-8 h-8 text-[#128C7E] mx-auto mb-2 opacity-60" />
              <h4 className="text-sm font-bold text-[#111827]">No WhatsApp Alerts Yet</h4>
              <p className="text-xs text-[#6b5c4d] mt-1 leading-relaxed">
                As soon as your order is confirmed, dispatched with Blue Dart, or out for delivery, live message pings will appear here.
              </p>
            </div>
          ) : (
            logs.map((log) => (
              <div key={log.id} className="space-y-1.5">
                <div className="flex justify-center">
                  <span className="text-[9px] font-semibold text-gray-500 uppercase tracking-wider">
                    {log.createdAtFormatted}
                  </span>
                </div>

                <div className="bg-[#D9FDD3] text-gray-900 rounded-2xl p-3.5 shadow-xs text-xs space-y-2 border border-emerald-100">
                  <div className="whitespace-pre-line text-[11px] leading-relaxed text-[#111827]">
                    {log.messageContent}
                  </div>

                  <div className="pt-2 border-t border-emerald-200/50 flex items-center justify-between text-[10px] text-gray-500">
                    <span className="font-bold text-emerald-800 uppercase tracking-wider text-[9px]">
                      {log.eventType.replace('_', ' ')}
                    </span>
                    <div className="flex items-center gap-1">
                      <span>{log.deliveryStatus}</span>
                      <CheckCheck className="w-3.5 h-3.5 text-[#53BDEB]" />
                    </div>
                  </div>
                </div>

                {log.trackingUrl && (
                  <a
                    href={log.trackingUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="block w-full py-2 bg-white hover:bg-emerald-50 text-[#00A884] font-bold text-center text-[10px] rounded-xl shadow-xs border border-emerald-100 transition-colors flex items-center justify-center gap-1.5"
                  >
                    <Truck className="w-3 h-3" /> Track Shipment Live
                  </a>
                )}
              </div>
            ))
          )}

        </div>

        {/* Footer */}
        <div className="p-4 bg-white border-t border-[#E8E1D7] flex items-center justify-between">
          <span className="text-[11px] text-[#6b5c4d] flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5 text-[#128C7E]" />
            Meta Business Verified
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-all cursor-pointer"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
}
