import { useState, useEffect } from 'react';
import {
  Video,
  Plane,
  ShieldCheck,
  Scissors,
  ChevronLeft,
  ChevronRight,
  Globe,
  Sparkles,
} from 'lucide-react';
import { useCurrency } from '../../context/CurrencyContext';

const ANNOUNCEMENTS = [
  {
    id: 'video',
    icon: Video,
    text: 'Virtual Video Shopping — Book 1-on-1 live drape appointment with master connoisseurs',
    action: 'Schedule Now →',
    type: 'video',
  },
  {
    id: 'shipping',
    icon: Plane,
    text: 'Worldwide Express Shipping to USA, UK, UAE, Canada, Australia & 50+ countries',
    action: 'Learn More',
    type: 'shipping',
  },
  {
    id: 'silk-mark',
    icon: ShieldCheck,
    text: '100% Authentic Handlooms Certified by Silk Mark Organization of India (SMOI)',
    action: 'Verify →',
    type: 'silk-mark',
  },
  {
    id: 'tailoring',
    icon: Scissors,
    text: 'Complimentary Fall & Pico Finishing + Custom Designer Blouse Tailoring',
    action: 'Explore',
    type: 'tailoring',
  },
];

export default function AnnouncementBar({ onOpenVideoShopping, onOpenSilkMark }) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const { currency, setCurrency, currencies } = useCurrency();

  useEffect(() => {
    if (isPaused) return;
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % ANNOUNCEMENTS.length);
    }, 4500);
    return () => clearInterval(timer);
  }, [isPaused]);

  const activeAnnouncement = ANNOUNCEMENTS[currentIndex];
  const IconComponent = activeAnnouncement.icon;

  const handleActionClick = () => {
    if (activeAnnouncement.type === 'video' && onOpenVideoShopping) {
      onOpenVideoShopping();
    } else if (activeAnnouncement.type === 'silk-mark' && onOpenSilkMark) {
      onOpenSilkMark();
    }
  };

  return (
    <div
      className="bg-[#1E6A62] text-white"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      <div className="section-shell flex h-9 items-center justify-between gap-3 text-[11px] font-semibold">
        {/* Left Arrow */}
        <button
          type="button"
          onClick={() =>
            setCurrentIndex((prev) => (prev - 1 + ANNOUNCEMENTS.length) % ANNOUNCEMENTS.length)
          }
          aria-label="Previous announcement"
          className="hidden p-0.5 text-white/60 hover:text-white sm:block"
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </button>

        {/* Center Rotating Content */}
        <div className="flex flex-1 items-center justify-center gap-2 overflow-hidden text-center">
          <IconComponent className="h-3.5 w-3.5 shrink-0 text-[#F3C56A]" />
          <span className="truncate">{activeAnnouncement.text}</span>
          <button
            type="button"
            onClick={handleActionClick}
            className="hidden font-bold underline underline-offset-2 text-[#F3C56A] hover:text-white sm:inline"
          >
            {activeAnnouncement.action}
          </button>
        </div>

        {/* Right Arrow */}
        <button
          type="button"
          onClick={() => setCurrentIndex((prev) => (prev + 1) % ANNOUNCEMENTS.length)}
          aria-label="Next announcement"
          className="hidden p-0.5 text-white/60 hover:text-white sm:block"
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </button>

        {/* Global Multi-Currency Switcher Dropdown */}
        <div className="flex items-center gap-1.5 shrink-0 border-l border-white/20 pl-3">
          <Globe className="h-3 w-3 text-[#F3C56A]" />
          <select
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
            aria-label="Select currency"
            className="cursor-pointer bg-transparent text-[11px] font-bold text-white outline-none [&>option]:text-[#17211F]"
          >
            {Object.values(currencies).map((curr) => (
              <option key={curr.code} value={curr.code}>
                {curr.flag} {curr.code} ({curr.symbol.trim()})
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}
