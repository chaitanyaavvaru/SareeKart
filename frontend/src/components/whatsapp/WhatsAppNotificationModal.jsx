import React, { useState } from 'react';
import { 
  X, Check, CheckCheck, Send, Phone, Video, MoreVertical, 
  ExternalLink, Sparkles, ShieldCheck, Truck, RefreshCw, MessageSquare, Loader2
} from 'lucide-react';
import whatsAppService from '../../services/whatsAppService';

export default function WhatsAppNotificationModal({
  isOpen,
  onClose,
  initialLog = null,
  order = null,
  onDispatched = null
}) {
  const [eventType, setEventType] = useState(initialLog?.eventType || 'ORDER_CONFIRMED');
  const [phone, setPhone] = useState(order?.shippingAddress?.phone || order?.user?.mobile || initialLog?.recipientPhone || '+91 98765 43210');
  const [recipientName, setRecipientName] = useState(order?.user?.firstName || initialLog?.recipientName || 'Patron');
  const [customNotes, setCustomNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeLog, setActiveLog] = useState(initialLog);
  const [notification, setNotification] = useState(null);

  if (!isOpen) return null;

  const handleSimulateDispatch = async () => {
    setLoading(true);
    setNotification(null);
    try {
      const payload = {
        orderId: order?.id || activeLog?.orderId,
        returnRequestId: activeLog?.returnRequestId,
        recipientPhone: phone,
        recipientName: recipientName,
        eventType: eventType,
        courierPartner: order?.courierPartner || 'Blue Dart Apex Air',
        trackingNumber: order?.trackingNumber || 'BD-882194',
        customMessage: customNotes.trim() || undefined
      };
      const res = await whatsAppService.simulateDispatch(payload);
      setActiveLog(res);
      setNotification({ type: 'success', message: `WhatsApp alert simulated successfully for ${eventType}!` });
      if (onDispatched) onDispatched(res);
    } catch (err) {
      setNotification({ type: 'error', message: err.response?.data?.message || 'Failed to simulate WhatsApp dispatch' });
    } finally {
      setLoading(false);
    }
  };

  const getPreviewText = () => {
    if (activeLog?.messageContent) return activeLog.messageContent;
    if (customNotes.trim()) return customNotes;

    const oid = order?.id || 108;
    const name = recipientName || 'Patron';

    switch (eventType) {
      case 'ORDER_CONFIRMED':
        return `🙏 *Namaste ${name}!*

Thank you for patronizing *SareeKart Handlooms*. Your bespoke order *#${oid}* has been confirmed!

✨ *Artisan Drape Summary:*
• Kanchipuram Pure Silk Drape (x1)

💰 *Total Amount:* ₹${order?.totalAmount ? Number(order.totalAmount).toLocaleString('en-IN') : '24,500.00'}
📅 *Estimated Delivery:* 2-3 Business Days

Our master weavers and curators are preparing your weave with the official *Silk Mark India* seal.

🔗 *Track Order:* https://sareekart.com/orders/${oid}`;

      case 'SHIPPED':
        return `📦 *Heirloom Saree Dispatched!*

*Namaste ${name}*, your handloom order *#${oid}* is en route!

🚚 *Courier Partner:* ${order?.courierPartner || 'Blue Dart Apex Air'}
🔖 *AWB Tracking Number:* ${order?.trackingNumber || 'BD-778899'}
📍 *Fulfillment Hub:* Bengaluru Central Vault (WH-01)
🎯 *Estimated Arrival:* 2 Business Days

Track your consignment in real time:
https://www.bluedart.com/tracking?awb=${order?.trackingNumber || 'BD-778899'}

Your weave is protected inside our signature breathable muslin dust bag.`;

      case 'OUT_FOR_DELIVERY':
        return `🚚 *Out for Delivery Today!*

*Namaste ${name}*, your SareeKart package for order *#${oid}* is out with our delivery partner.

🔔 *Doorstep Instructions:*
• Please verify the tamper-proof Silk Mark seal upon handover.
📍 *Destination:* Registered Address

Have your phone ready for courier handover.`;

      case 'DELIVERED':
        return `🌸 *Delivered with Reverence!*

*Namaste ${name}*, order *#${oid}* has been safely delivered to your doorstep.

🥻 We hope your authentic handloom drape brings timeless elegance to your celebrations.

📖 *Silk Care & Preservation Guide:*
https://sareekart.com/saree-care

🔄 *Doorstep Returns/Exchanges:* Eligible for 7 days via https://sareekart.com/orders

Thank you for sustaining India's generational weaving heritage.`;

      case 'RETURN_PICKUP':
        return `🔄 *Reverse Pickup Scheduled!*

*Namaste ${name}*, reverse pickup for return claim is scheduled.

🚚 *Reverse Courier Partner:* Blue Dart Reverse Logistics
🔖 *Return AWB:* REV-BD-89021

📦 *Handover Instructions:*
• Keep the saree securely packed in the original box with Silk Mark tags attached.`;

      default:
        return `🙏 *Namaste ${name}*, your order #${oid} is being processed with artisan care.`;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-fade-in">
      <div className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-[#DDE4EA] overflow-hidden flex flex-col md:flex-row max-h-[90vh]">
        
        {/* Left Column: Staff Simulation Controls */}
        <div className="w-full md:w-1/2 p-6 md:p-8 bg-[#FAF8F5] border-r border-[#E8E1D7] flex flex-col justify-between overflow-y-auto">
          <div>
            <div className="flex items-center justify-between pb-4 border-b border-[#E8E1D7]">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-[#128C7E]/10 flex items-center justify-center text-[#128C7E]">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#111827] font-serif">WhatsApp Dispatch Console</h3>
                  <p className="text-[11px] text-[#6b5c4d]">Event-driven order milestone clienteling</p>
                </div>
              </div>
              <span className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 uppercase tracking-wider">
                Simulated Sandbox
              </span>
            </div>

            {notification && (
              <div className={`mt-4 p-3 rounded-xl text-xs font-semibold flex items-center gap-2 ${
                notification.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-red-50 text-red-800 border border-red-200'
              }`}>
                {notification.type === 'success' ? <Check className="w-4 h-4 shrink-0" /> : <X className="w-4 h-4 shrink-0" />}
                <span>{notification.message}</span>
              </div>
            )}

            <div className="mt-5 space-y-4 text-left">
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#6b5c4d] mb-1.5">
                  Milestone Event Trigger
                </label>
                <select
                  value={eventType}
                  onChange={(e) => {
                    setEventType(e.target.value);
                    setActiveLog(null);
                  }}
                  className="w-full h-10 px-3 text-xs font-medium bg-white border border-[#DDE4EA] rounded-xl focus:outline-none focus:border-[#128C7E]"
                >
                  <option value="ORDER_CONFIRMED">ORDER_CONFIRMED (Order Placement & Est. SLA)</option>
                  <option value="SHIPPED">SHIPPED (Blue Dart AWB & Tracking Link)</option>
                  <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY (Doorstep Alert & Seal Prompt)</option>
                  <option value="DELIVERED">DELIVERED (Preservation Guide & Returns Link)</option>
                  <option value="RETURN_PICKUP">RETURN_PICKUP (Reverse Courier AWB Handover)</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-[#6b5c4d] mb-1.5">
                    Recipient Name
                  </label>
                  <input
                    type="text"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    className="w-full h-10 px-3 text-xs font-medium bg-white border border-[#DDE4EA] rounded-xl focus:outline-none focus:border-[#128C7E]"
                    placeholder="Recipient Name"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-wider text-[#6b5c4d] mb-1.5">
                    WhatsApp Number
                  </label>
                  <input
                    type="text"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="w-full h-10 px-3 text-xs font-medium bg-white border border-[#DDE4EA] rounded-xl focus:outline-none focus:border-[#128C7E]"
                    placeholder="+91 98765 43210"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#6b5c4d] mb-1.5">
                  Custom Concierge Note (Optional Override)
                </label>
                <textarea
                  value={customNotes}
                  onChange={(e) => setCustomNotes(e.target.value)}
                  rows={3}
                  className="w-full p-3 text-xs font-medium bg-white border border-[#DDE4EA] rounded-xl focus:outline-none focus:border-[#128C7E] resize-none"
                  placeholder="Leave empty to use official luxury handloom message template..."
                />
              </div>

              <div className="p-3.5 bg-amber-50/70 border border-amber-200/80 rounded-2xl text-[11px] text-amber-900 leading-relaxed">
                <p className="font-bold flex items-center gap-1.5 text-amber-950">
                  <ShieldCheck className="w-3.5 h-3.5 text-amber-700" />
                  Meta Business Compliance & Local Sandbox
                </p>
                <p className="mt-1 text-amber-800">
                  In local dev mode, alerts are rendered and stored with status <span className="font-bold">SIMULATED</span> with zero external latency.
                </p>
              </div>
            </div>
          </div>

          <div className="mt-6 flex items-center gap-3">
            <button
              onClick={handleSimulateDispatch}
              disabled={loading}
              className="flex-1 h-11 bg-[#128C7E] hover:bg-[#075E54] text-white text-xs font-bold uppercase tracking-wider rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              Dispatch Simulated Ping
            </button>
            <button
              onClick={onClose}
              className="h-11 px-5 bg-white border border-[#DDE4EA] hover:bg-gray-100 text-gray-700 text-xs font-bold rounded-xl transition-all cursor-pointer"
            >
              Close
            </button>
          </div>
        </div>

        {/* Right Column: Authentic WhatsApp Smartphone View */}
        <div className="w-full md:w-1/2 p-6 md:p-8 bg-[#EFEAE2] flex items-center justify-center">
          <div className="w-full max-w-[340px] bg-[#ECE5DD] rounded-[36px] shadow-2xl border-4 border-gray-800 overflow-hidden flex flex-col h-[520px] relative">
            
            {/* Phone Speaker Notch */}
            <div className="absolute top-2 left-1/2 -translate-x-1/2 w-28 h-4 bg-gray-800 rounded-full z-20 flex items-center justify-center">
              <div className="w-8 h-1 bg-gray-600 rounded-full" />
            </div>

            {/* WhatsApp Top App Bar */}
            <div className="bg-[#075E54] pt-7 pb-2.5 px-3 text-white flex items-center justify-between shadow-xs z-10">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-[#128C7E] border border-white/20 flex items-center justify-center text-amber-200 font-serif font-bold text-xs">
                  SK
                </div>
                <div>
                  <div className="flex items-center gap-1">
                    <span className="font-bold text-xs tracking-tight text-white">SareeKart Handlooms</span>
                    <span className="text-[10px] text-emerald-300">✔</span>
                  </div>
                  <p className="text-[9px] text-emerald-100/80 leading-none">Verified Business • Online</p>
                </div>
              </div>
              <div className="flex items-center gap-3 text-white/80">
                <Video className="w-3.5 h-3.5" />
                <Phone className="w-3.5 h-3.5" />
                <MoreVertical className="w-3.5 h-3.5" />
              </div>
            </div>

            {/* Chat Body */}
            <div className="flex-1 p-3 overflow-y-auto space-y-3 flex flex-col justify-end text-left">
              
              {/* Date Pill */}
              <div className="flex justify-center">
                <span className="px-2.5 py-0.5 bg-white/80 backdrop-blur-xs rounded-md text-[9px] font-bold text-gray-600 shadow-xs uppercase tracking-wider">
                  Today
                </span>
              </div>

              {/* Encryption Banner */}
              <div className="p-2 bg-[#FFFDE6] border border-[#E9E4C9] rounded-lg text-[9px] text-center text-gray-700 leading-snug shadow-2xs">
                🔒 Messages and calls are end-to-end encrypted. No one outside of this chat can read them.
              </div>

              {/* Outbound Message Bubble */}
              <div className="self-end max-w-[92%] bg-[#D9FDD3] text-gray-900 rounded-xl rounded-tr-xs p-3 shadow-xs text-xs relative">
                <div className="whitespace-pre-line text-[11px] leading-relaxed text-[#111827]">
                  {getPreviewText()}
                </div>

                {/* Footer Time & Double Checkmark */}
                <div className="mt-1 flex items-center justify-end gap-1 text-[9px] text-gray-500">
                  <span>{activeLog?.createdAtFormatted ? activeLog.createdAtFormatted.split(',')[1]?.trim() : '12:45'}</span>
                  <CheckCheck className="w-3 h-3 text-[#53BDEB]" />
                </div>
              </div>

              {/* Interactive Quick Action Buttons */}
              <div className="space-y-1 self-end w-[92%]">
                {eventType === 'SHIPPED' && (
                  <a
                    href="https://www.bluedart.com"
                    target="_blank"
                    rel="noreferrer"
                    className="block w-full py-2 bg-white/95 hover:bg-white text-[#00A884] font-bold text-center text-[10px] rounded-lg shadow-xs transition-colors flex items-center justify-center gap-1"
                  >
                    <Truck className="w-3 h-3" /> Track Live on Blue Dart
                  </a>
                )}
                {eventType === 'DELIVERED' && (
                  <a
                    href="/saree-care"
                    target="_blank"
                    rel="noreferrer"
                    className="block w-full py-2 bg-white/95 hover:bg-white text-[#00A884] font-bold text-center text-[10px] rounded-lg shadow-xs transition-colors flex items-center justify-center gap-1"
                  >
                    <ExternalLink className="w-3 h-3" /> View Silk Care Guide
                  </a>
                )}
              </div>

            </div>

            {/* Phone Home Bar */}
            <div className="h-4 bg-[#ECE5DD] flex items-center justify-center pb-1">
              <div className="w-24 h-1 bg-gray-400 rounded-full" />
            </div>

          </div>
        </div>

      </div>
    </div>
  );
}
