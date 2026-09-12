import { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  MapPin,
  MessageCircle,
  X,
  CheckCircle2,
  Sparkles,
  Users,
  ShieldCheck,
  CalendarPlus,
  Compass,
} from 'lucide-react';

export const BOUTIQUE_LOCATIONS = [
  {
    id: 'hyderabad',
    city: 'Hyderabad',
    name: 'Jubilee Hills Flagship',
    address: 'Plot 798, Road No. 36, Jubilee Hills, Hyderabad, Telangana 500033',
    phone: '+91 40 2355 8899',
    hours: '10:30 AM – 9:00 PM',
    mapUrl: 'https://maps.google.com/?q=Jubilee+Hills+Hyderabad',
  },
  {
    id: 'bengaluru',
    city: 'Bengaluru',
    name: 'Jayanagar Experience Salon',
    address: '11th Main Rd, 4th Block, Jayanagar, Bengaluru, Karnataka 560011',
    phone: '+91 80 4123 7700',
    hours: '10:30 AM – 9:00 PM',
    mapUrl: 'https://maps.google.com/?q=Jayanagar+Bengaluru',
  },
  {
    id: 'delhi',
    city: 'New Delhi',
    name: 'South Extension Heritage House',
    address: 'G-12, South Extension Part II, New Delhi, Delhi 110049',
    phone: '+91 11 4988 5544',
    hours: '10:30 AM – 9:00 PM',
    mapUrl: 'https://maps.google.com/?q=South+Extension+Part+II+New+Delhi',
  },
  {
    id: 'visakhapatnam',
    city: 'Visakhapatnam',
    name: 'V Square Heritage Flagship',
    address: 'V Square, Waltair Uplands, Siripuram, Visakhapatnam, AP 530003',
    phone: '+91 891 256 4433',
    hours: '10:00 AM – 9:00 PM',
    mapUrl: 'https://maps.google.com/?q=Waltair+Uplands+Visakhapatnam',
  },
  {
    id: 'vijayawada',
    city: 'Vijayawada',
    name: 'MG Road Bridal Emporium',
    address: 'Opp. PB Siddhartha College, MG Road, Vijayawada, AP 520010',
    phone: '+91 866 247 1122',
    hours: '10:30 AM – 9:00 PM',
    mapUrl: 'https://maps.google.com/?q=MG+Road+Vijayawada',
  },
];

const CONSULTATION_TYPES = [
  { id: 'bridal', title: 'Bridal Trousseau', desc: 'Muhurtham silks, custom zari borders, and matching family drapes.' },
  { id: 'heirloom', title: 'Heirloom Silk Curation', desc: 'Rare Kanchipuram Korvai, Patan Patola, and antique Shikargah weaves.' },
  { id: 'festive', title: 'Festive & Reception Drape', desc: 'Banarasi organza, pastel weaves, and contemporary handlooms.' },
  { id: 'custom', title: 'Custom Guild Commission', desc: 'Bespoke weaving with artisan motifs and personalized pallu inscriptions.' },
];

const SLOTS = [
  '11:00 AM – 12:30 PM',
  '02:00 PM – 03:30 PM',
  '04:30 PM – 06:00 PM',
  '06:30 PM – 08:00 PM',
];

export default function InStoreAppointmentModal({
  isOpen,
  onClose,
  initialBoutiqueId = null,
}) {
  const [selectedBoutique, setSelectedBoutique] = useState(
    initialBoutiqueId || BOUTIQUE_LOCATIONS[0].id
  );
  const [consultationType, setConsultationType] = useState('bridal');
  const [date, setDate] = useState(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  });
  const [slot, setSlot] = useState(SLOTS[0]);
  const [guests, setGuests] = useState('2');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [confirmedPass, setConfirmedPass] = useState(null);

  useEffect(() => {
    if (initialBoutiqueId) {
      setSelectedBoutique(initialBoutiqueId);
    }
  }, [initialBoutiqueId]);

  useEffect(() => {
    if (isOpen) {
      setConfirmedPass(null);
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const currentBoutique =
    BOUTIQUE_LOCATIONS.find((b) => b.id === selectedBoutique) || BOUTIQUE_LOCATIONS[0];
  const activeFocus =
    CONSULTATION_TYPES.find((c) => c.id === consultationType) || CONSULTATION_TYPES[0];

  const handleSubmit = (e) => {
    e.preventDefault();
    const passCode = `SK-VIP-${Math.floor(10000 + Math.random() * 90000)}`;
    setConfirmedPass({
      passCode,
      name: name.trim(),
      phone: phone.trim(),
      boutique: currentBoutique,
      focus: activeFocus.title,
      date,
      slot,
      guests,
      notes: notes.trim(),
    });
  };

  const getWhatsAppConfirmationUrl = () => {
    if (!confirmedPass) return '#';
    const text = `Namaste SareeKart! I have booked a VIP In-Store Drape Appointment.\n\n• VIP Pass: ${confirmedPass.passCode}\n• Guest Name: ${confirmedPass.name}\n• Boutique: ${confirmedPass.boutique.name} (${confirmedPass.boutique.city})\n• Date: ${confirmedPass.date}\n• Time: ${confirmedPass.slot}\n• Party Size: ${confirmedPass.guests} Guests\n• Consultation: ${confirmedPass.focus}\n\nPlease prepare a private salon and have our senior saree connoisseur ready.`;
    return `https://wa.me/919059564499?text=${encodeURIComponent(text)}`;
  };

  const downloadCalendarEvent = () => {
    if (!confirmedPass) return;
    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//SareeKart//VIP In-Store Appointment//EN',
      'BEGIN:VEVENT',
      `SUMMARY:SareeKart VIP Salon Appointment (${confirmedPass.boutique.name})`,
      `DESCRIPTION:Private 1-on-1 drape consultation for ${confirmedPass.focus}. VIP Pass: ${confirmedPass.passCode}. Contact: ${confirmedPass.boutique.phone}`,
      `LOCATION:${confirmedPass.boutique.address}`,
      `DTSTART:${confirmedPass.date.replace(/-/g, '')}T110000Z`,
      `DTEND:${confirmedPass.date.replace(/-/g, '')}T123000Z`,
      'STATUS:CONFIRMED',
      'END:VEVENT',
      'END:VCALENDAR',
    ].join('\r\n');

    const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.setAttribute('download', `SareeKart_VIP_Appointment_${confirmedPass.passCode}.ics`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="in-store-appointment-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative max-h-[94vh] w-full max-w-2xl overflow-y-auto rounded-[12px] border border-[#DDD8CF] bg-white p-6 shadow-2xl sm:p-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-[#EAE6DF] pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[#9B6A27]/10 text-[#9B6A27]">
              <Compass className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 id="in-store-appointment-title" className="text-xl font-bold text-[#17211F]">
                  Book VIP In-Store Drape Salon
                </h2>
                <span className="rounded-full bg-[#F3E6C7] px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider text-[#9B6A27]">
                  Private Lounge
                </span>
              </div>
              <p className="mt-0.5 text-xs font-semibold text-[#71817A]">
                Complimentary 1-on-1 styling with master saree connoisseurs in our flagship boutiques
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close appointment modal"
            className="rounded-full p-1 text-[#71817A] hover:bg-[#F7F4EE] hover:text-[#17211F] transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {!confirmedPass ? (
          <form onSubmit={handleSubmit} className="mt-5 grid gap-5">
            {/* Boutique Selector */}
            <div>
              <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                1. Select Flagship Boutique
              </label>
              <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                {BOUTIQUE_LOCATIONS.map((b) => (
                  <button
                    key={b.id}
                    type="button"
                    onClick={() => setSelectedBoutique(b.id)}
                    className={`flex flex-col items-start rounded-[8px] border p-3 text-left transition-all ${
                      selectedBoutique === b.id
                        ? 'border-[#1E6A62] bg-[#1E6A62]/5 ring-1 ring-[#1E6A62]'
                        : 'border-[#DDD8CF] hover:border-[#1E6A62]/40'
                    }`}
                  >
                    <div className="flex items-center justify-between w-full">
                      <span className="text-xs font-black text-[#17211F]">{b.city}</span>
                      <span className="text-[10px] font-bold text-[#1E6A62]">{b.hours}</span>
                    </div>
                    <span className="mt-0.5 text-[11px] font-bold text-[#4E5B56]">{b.name}</span>
                    <span className="mt-1 line-clamp-1 text-[10px] text-[#71817A]">{b.address}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Consultation Focus */}
            <div>
              <label className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                2. Styling Focus
              </label>
              <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                {CONSULTATION_TYPES.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setConsultationType(c.id)}
                    className={`flex flex-col items-start rounded-[8px] border p-3 text-left transition-all ${
                      consultationType === c.id
                        ? 'border-[#9B6A27] bg-[#F3E6C7]/20 ring-1 ring-[#9B6A27]'
                        : 'border-[#DDD8CF] hover:border-[#9B6A27]/40'
                    }`}
                  >
                    <span className="text-xs font-black text-[#17211F]">{c.title}</span>
                    <span className="mt-0.5 text-[10px] text-[#71817A]">{c.desc}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Date, Time & Guests */}
            <div className="grid gap-3 sm:grid-cols-3">
              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                3. Date
                <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                  <Calendar className="mr-2 h-4 w-4 shrink-0 text-[#71817A]" />
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
                4. Time Slot
                <select
                  value={slot}
                  onChange={(e) => setSlot(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                >
                  {SLOTS.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </label>

              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                5. Party Size
                <div className="flex h-11 items-center rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 focus-within:border-[#1E6A62]">
                  <Users className="mr-2 h-4 w-4 shrink-0 text-[#71817A]" />
                  <select
                    value={guests}
                    onChange={(e) => setGuests(e.target.value)}
                    className="w-full bg-transparent text-xs font-bold outline-none text-[#17211F]"
                  >
                    <option value="1">1 Guest (Solo Drape)</option>
                    <option value="2">2 Guests (Bride & Mother)</option>
                    <option value="3">3 Guests (Bride & Family)</option>
                    <option value="4">4 Guests (Bridal Party)</option>
                    <option value="5">5+ Guests (Full Entourage)</option>
                  </select>
                </div>
              </label>
            </div>

            {/* Contact Details */}
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                Full Name
                <input
                  type="text"
                  required
                  placeholder="Deepika Rao"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                />
              </label>

              <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
                Mobile / WhatsApp Number
                <input
                  type="tel"
                  required
                  placeholder="+91 98480 12345"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-bold outline-none focus:border-[#1E6A62]"
                />
              </label>
            </div>

            {/* Special Request */}
            <label className="grid gap-1.5 text-xs font-black text-[#17211F]">
              Special Weave Notes or Drape Requests (Optional)
              <input
                type="text"
                placeholder="Looking for crimson korvai bridal silk with temple borders and antique zari..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="h-11 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 text-xs font-semibold outline-none focus:border-[#1E6A62]"
              />
            </label>

            {/* VIP Guarantee */}
            <div className="rounded-[8px] bg-[#FAF8F5] p-3 text-[11px] text-[#71817A] border border-[#EAE6DF]">
              <div className="flex items-center gap-1.5 font-bold text-[#17211F]">
                <ShieldCheck className="h-3.5 w-3.5 text-[#9B6A27]" />
                <span>The Flagship Salon Privilege</span>
              </div>
              <p className="mt-1 leading-relaxed">
                Enjoy a private draping room, dedicated senior master artisan, curated South Indian filter coffee / royal tea service, and complimentary valet parking.
              </p>
            </div>

            <button
              type="submit"
              className="mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-[8px] bg-[#1E6A62] text-xs font-black uppercase tracking-wider text-white hover:bg-[#154e48] transition-colors"
            >
              <Sparkles className="h-4 w-4 text-[#F3C56A]" />
              Reserve In-Store VIP Drape Appointment
            </button>
          </form>
        ) : (
          /* Confirmation State - VIP Pass */
          <div className="mt-5 space-y-5">
            <div className="rounded-[12px] border border-[#CBB688] bg-gradient-to-br from-[#FAF8F5] via-[#F3E6C7]/30 to-[#FAF8F5] p-6 text-center shadow-md">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#9B6A27]/15 text-[#9B6A27]">
                <CheckCircle2 className="h-6 w-6" />
              </div>
              <p className="mt-3 text-[11px] font-black uppercase tracking-[0.2em] text-[#9B6A27]">
                Official VIP Salon Invitation
              </p>
              <h3 className="mt-1 text-2xl font-bold font-serif text-[#17211F]">
                VIP Drape Pass Confirmed
              </h3>
              <p className="mt-1 font-mono text-sm font-black text-[#1E6A62]">
                Pass #{confirmedPass.passCode}
              </p>

              <div className="mt-5 border-t border-b border-[#EAE6DF] py-4 grid grid-cols-2 gap-3 text-left text-xs">
                <div>
                  <span className="text-[#71817A] block text-[10px] uppercase font-bold">Client</span>
                  <strong className="text-[#17211F] text-sm">{confirmedPass.name}</strong>
                </div>
                <div>
                  <span className="text-[#71817A] block text-[10px] uppercase font-bold">Party Size</span>
                  <strong className="text-[#17211F] text-sm">{confirmedPass.guests} Guests</strong>
                </div>
                <div>
                  <span className="text-[#71817A] block text-[10px] uppercase font-bold">Boutique</span>
                  <strong className="text-[#17211F] text-sm">{confirmedPass.boutique.name}</strong>
                  <span className="text-[#71817A] block text-[10px]">{confirmedPass.boutique.city}</span>
                </div>
                <div>
                  <span className="text-[#71817A] block text-[10px] uppercase font-bold">Date & Slot</span>
                  <strong className="text-[#17211F] text-sm">{confirmedPass.date}</strong>
                  <span className="text-[#71817A] block text-[10px]">{confirmedPass.slot}</span>
                </div>
              </div>

              <div className="mt-3 text-left">
                <span className="text-[#71817A] block text-[10px] uppercase font-bold">Address & Contact</span>
                <p className="text-xs font-medium text-[#4E5B56]">{confirmedPass.boutique.address}</p>
                <p className="text-xs font-bold text-[#1E6A62] mt-0.5">{confirmedPass.boutique.phone}</p>
              </div>
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <a
                href={getWhatsAppConfirmationUrl()}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-11 items-center justify-center gap-2 rounded-[8px] bg-[#25D366] text-xs font-bold text-white hover:bg-[#20b858] transition-colors"
              >
                <MessageCircle className="h-4 w-4" />
                Notify Boutique via WhatsApp
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
              Done & Return to Boutiques
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
