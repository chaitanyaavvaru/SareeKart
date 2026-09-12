import { useState, useEffect } from 'react';
import {
  Video,
  Calendar,
  Clock,
  MessageCircle,
  X,
  CheckCircle2,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  CalendarPlus,
} from 'lucide-react';

const CURATION_TAGS = [
  'Bridal Kanchipuram',
  'Banarasi Katan Silk',
  'Pure Zari Brocade',
  'Uppada Jamdani',
  'Paithani & Patola',
  'Festive Organza',
  'Custom Wedding Trousseau',
];

const TIME_SLOTS = [
  { id: 'slot-1', label: '11:00 AM - 11:30 AM IST', region: 'India / Asia' },
  { id: 'slot-2', label: '02:30 PM - 03:00 PM IST', region: 'Europe / Gulf (UAE)' },
  { id: 'slot-3', label: '06:00 PM - 06:30 PM IST', region: 'US East Coast / UK' },
  { id: 'slot-4', label: '08:30 PM - 09:00 PM IST', region: 'US West / Pacific' },
];

export default function VideoShoppingModal({ isOpen, onClose, initialSaree = null }) {
  const [selectedTag, setSelectedTag] = useState(
    initialSaree?.category || initialSaree?.name || 'Bridal Kanchipuram'
  );
  const [date, setDate] = useState(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  });
  const [selectedSlot, setSelectedSlot] = useState(TIME_SLOTS[0].id);
  const [name, setName] = useState('');
  const [mobile, setMobile] = useState('');
  const [platform, setPlatform] = useState('whatsapp'); // 'whatsapp' | 'gmeet' | 'zoom'
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setConfirmedBooking(null);
      if (initialSaree?.name) {
        setSelectedTag(initialSaree.name);
      }
    }
  }, [isOpen, initialSaree]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    const activeSlotObj = TIME_SLOTS.find((s) => s.id === selectedSlot) || TIME_SLOTS[0];
    const bookingId = `SK-VID-${Math.floor(1000 + Math.random() * 9000)}`;

    setConfirmedBooking({
      id: bookingId,
      name: name.trim(),
      mobile: mobile.trim(),
      tag: selectedTag,
      date,
      slot: activeSlotObj.label,
      platform,
    });
  };

  const getWhatsAppBookingUrl = () => {
    if (!confirmedBooking) return '#';
    const text = `Namaste SareeKart! I have booked a 1-on-1 Video Shopping Appointment.\n\n• Booking ID: ${confirmedBooking.id}\n• Name: ${confirmedBooking.name}\n• Date: ${confirmedBooking.date}\n• Time: ${confirmedBooking.slot}\n• Interested in: ${confirmedBooking.tag}\n• Platform: ${confirmedBooking.platform.toUpperCase()}\n\nPlease assign a master saree connoisseur to connect with me.`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  const downloadCalendarEvent = () => {
    if (!confirmedBooking) return;
    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//SareeKart//Virtual Video Shopping//EN',
      'BEGIN:VEVENT',
      `SUMMARY:SareeKart Luxury Saree Video Consultation (${confirmedBooking.tag})`,
      `DESCRIPTION:1-on-1 Video shopping session with SareeKart master stylist. Booking ID: ${confirmedBooking.id}`,
      `DTSTART:${confirmedBooking.date.replace(/-/g, '')}T110000Z`,
      `DTEND:${confirmedBooking.date.replace(/-/g, '')}T113000Z`,
      'LOCATION:WhatsApp Video (+91 90595 64499)',
      'STATUS:CONFIRMED',
      'END:VEVENT',
      'END:VCALENDAR',
    ].join('\r\n');

    const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.setAttribute('download', `SareeKart_Video_Shopping_${confirmedBooking.id}.ics`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="video-shopping-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[#1E6A62]/10 text-[#1E6A62]">
              <Video className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 id="video-shopping-title" className="text-xl font-bold text-[#17211F]">
                  Virtual Video Shopping
                </h2>
                <span className="rounded-full bg-[#F3E6C7] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-[#9B6A27]">
                  VIP Concierge
                </span>
              </div>
              <p className="mt-0.5 text-xs font-semibold text-[#71817A]">
                1-on-1 private video drape session with master saree connoisseurs
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close video shopping modal"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {!confirmedBooking ? (
          <form onSubmit={handleSubmit} className="mt-5 grid gap-5">
            {/* Curation Preference */}
            <div>
              <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                1. Select Saree Weave / Collection
              </label>
              <div className="mt-2 flex flex-wrap gap-2">
                {CURATION_TAGS.map((tag) => (
                  <button
                    key={tag}
                    type="button"
                    onClick={() => setSelectedTag(tag)}
                    className={`rounded-[6px] border px-3 py-1.5 text-xs font-bold transition-all ${
                      selectedTag === tag
                        ? 'border-[#1E6A62] bg-[#1E6A62] text-white shadow-xs'
                        : 'border-[#DDD8CF] bg-[#F7F4EE] text-[#17211F] hover:border-[#1E6A62]'
                    }`}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            </div>

            {/* Date & Time Slot */}
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                2. Preferred Date
                <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                  <Calendar className="mr-2.5 h-4 w-4 shrink-0 text-[#71817A]" />
                  <input
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full bg-transparent text-xs font-bold outline-none text-[#17211F]"
                  />
                </div>
              </label>

              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                3. Video Platform
                <select
                  value={platform}
                  onChange={(e) => setPlatform(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                >
                  <option value="whatsapp">WhatsApp Video Call</option>
                  <option value="gmeet">Google Meet</option>
                  <option value="zoom">Zoom Video</option>
                </select>
              </label>
            </div>

            {/* Time Slots */}
            <div>
              <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                4. Select Time Slot
              </label>
              <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                {TIME_SLOTS.map((slot) => (
                  <button
                    key={slot.id}
                    type="button"
                    onClick={() => setSelectedSlot(slot.id)}
                    className={`flex items-center justify-between rounded-[8px] border p-3 text-left transition-all ${
                      selectedSlot === slot.id
                        ? 'border-[#1E6A62] bg-[#1E6A62]/5 ring-1 ring-[#1E6A62]'
                        : 'border-[#DDD8CF] hover:border-[#1E6A62]/40'
                    }`}
                  >
                    <div>
                      <p className="text-xs font-black text-[#17211F]">{slot.label}</p>
                      <p className="text-[11px] font-semibold text-[#71817A]">{slot.region}</p>
                    </div>
                    <Clock
                      className={`h-4 w-4 ${
                        selectedSlot === slot.id ? 'text-[#1E6A62]' : 'text-[#A7A19A]'
                      }`}
                    />
                  </button>
                ))}
              </div>
            </div>

            {/* Client Contact Info */}
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                Your Full Name
                <input
                  type="text"
                  required
                  placeholder="Priya Sharma"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                />
              </label>

              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                WhatsApp / Phone Number
                <input
                  type="tel"
                  required
                  placeholder="+91 98765 43210"
                  value={mobile}
                  onChange={(e) => setMobile(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                />
              </label>
            </div>

            {/* Trust Notice */}
            <div className="rounded-[8px] bg-[#F7F4EE] p-3 text-[11px] text-[#71817A]">
              <div className="flex items-center gap-1.5 font-bold text-[#17211F]">
                <ShieldCheck className="h-3.5 w-3.5 text-[#1E6A62]" />
                <span>Zero Obligation Luxury Experience</span>
              </div>
              <p className="mt-1 leading-relaxed">
                Experience high-definition live draping, pure zari close-ups, and genuine pallu luster.
                No commitment to purchase required.
              </p>
            </div>

            <button
              type="submit"
              className="mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition-colors"
            >
              <Video className="h-4 w-4" />
              Book Video Shopping Appointment
            </button>
          </form>
        ) : (
          /* Confirmation State */
          <div className="mt-5 space-y-5">
            <div className="rounded-[10px] border border-emerald-200 bg-emerald-50/80 p-5 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                <CheckCircle2 className="h-6 w-6" />
              </div>
              <h3 className="mt-3 text-lg font-bold text-[#17211F]">Appointment Confirmed!</h3>
              <p className="mt-1 font-mono text-xs font-black text-[#1E6A62]">
                Reference #{confirmedBooking.id}
              </p>
              <p className="mt-2 text-xs font-medium text-[#4E5B56]">
                We have reserved a personal saree connoisseur for you on{' '}
                <strong>{confirmedBooking.date}</strong> at <strong>{confirmedBooking.slot}</strong>.
              </p>
            </div>

            <div className="rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] p-4 text-xs">
              <p className="font-black uppercase tracking-wider text-[#9B6A27]">
                Appointment Summary
              </p>
              <div className="mt-3 grid grid-cols-2 gap-2 text-[#17211F]">
                <div>
                  <span className="text-[#71817A]">Client:</span>{' '}
                  <strong>{confirmedBooking.name}</strong>
                </div>
                <div>
                  <span className="text-[#71817A]">Contact:</span>{' '}
                  <strong>{confirmedBooking.mobile}</strong>
                </div>
                <div>
                  <span className="text-[#71817A]">Curation:</span>{' '}
                  <strong>{confirmedBooking.tag}</strong>
                </div>
                <div>
                  <span className="text-[#71817A]">Platform:</span>{' '}
                  <strong className="uppercase">{confirmedBooking.platform}</strong>
                </div>
              </div>
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <a
                href={getWhatsAppBookingUrl()}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-11 items-center justify-center gap-2 rounded-[8px] bg-[#25D366] text-xs font-bold text-white hover:bg-[#20b858] transition-colors"
              >
                <MessageCircle className="h-4 w-4" />
                Notify Stylist via WhatsApp
              </a>

              <button
                type="button"
                onClick={downloadCalendarEvent}
                className="flex h-11 items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-white text-xs font-bold text-[#17211F] hover:bg-[#F7F4EE] transition-colors"
              >
                <CalendarPlus className="h-4 w-4 text-[#1E6A62]" />
                Add to Calendar (.ics)
              </button>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="w-full text-center text-xs font-bold text-[#71817A] hover:text-[#17211F] transition-colors"
            >
              Done & Return to Storefront
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
