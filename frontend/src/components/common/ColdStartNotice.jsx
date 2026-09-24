import React, { useState } from 'react';
import { Sparkles, X } from 'lucide-react';
import { useColdStart } from '../../api/axiosConfig';

export default function ColdStartNotice() {
  const { isColdStarting, message, attempt } = useColdStart();
  const [dismissed, setDismissed] = useState(false);

  if (!isColdStarting || dismissed) {
    return null;
  }

  return (
    <aside
      aria-live="polite"
      role="status"
      className="relative z-50 border-b border-[#F3C56A]/30 bg-[#1E6A62] px-4 py-2.5 text-white shadow-md transition-all duration-300 sm:px-6"
    >
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 text-xs sm:text-sm">
        <div className="flex items-center gap-2.5">
          <Sparkles className="h-4 w-4 shrink-0 animate-spin text-[#F3C56A]" />
          <div className="flex flex-col sm:flex-row sm:items-center sm:gap-2">
            <span className="font-serif font-semibold tracking-wide text-[#F3C56A]">
              Awakening our boutique atelier...
            </span>
            <span className="text-white/85">
              {message ||
                'Cloud containers on our zero-cost tier spin down when idle (~30–45s wake time). Your curated collection is loading.'}
            </span>
            {attempt > 1 && (
              <span className="inline-block rounded-full bg-black/20 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-white/90">
                Retry {attempt}/2
              </span>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={() => setDismissed(true)}
          aria-label="Dismiss awakening notice"
          className="rounded p-1 text-white/70 transition hover:bg-white/10 hover:text-white"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
    </aside>
  );
}
