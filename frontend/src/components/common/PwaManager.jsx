import React, { useState, useEffect } from 'react';
import { Download, Wifi, WifiOff, X, Smartphone, ShieldCheck, Sparkles } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export default function PwaManager() {
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [showInstallBanner, setShowInstallBanner] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    // 0. Register Service Worker for offline catalog caching
    if ('serviceWorker' in navigator && import.meta.env.PROD) {
      navigator.serviceWorker.register('/sw.js').catch((err) => {
        console.warn('Service worker registration failed:', err);
      });
    }

    // 1. Listen for Online / Offline connection changes
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // 2. Capture PWA BeforeInstallPromptEvent
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallBanner(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === 'accepted') {
      console.log('User accepted the SareeKart PWA install prompt');
    }
    setDeferredPrompt(null);
    setShowInstallBanner(false);
  };

  return (
    <>
      {/* Network Status Badge (Shown when offline or restoring online) */}
      <AnimatePresence>
        {!isOnline && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className="fixed top-20 left-1/2 -translate-x-1/2 z-50 bg-[#3A0F1F] text-[#C8A04D] px-4 py-2 rounded-full border border-[#C8A04D]/40 shadow-xl flex items-center gap-2.5 text-xs font-bold font-sans"
          >
            <WifiOff className="w-4 h-4 text-amber-400 animate-pulse" />
            <span>Offline Mode Active • Cached Catalog Available</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Floating PWA Install Prompt Banner */}
      <AnimatePresence>
        {showInstallBanner && (
          <motion.div
            initial={{ opacity: 0, y: 100 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 100 }}
            className="fixed bottom-6 right-6 z-40 bg-white border border-[#E6DFD3] rounded-3xl p-5 shadow-2xl max-w-sm w-full text-left space-y-3 font-sans text-[#3A0F1F]"
          >
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-[#3A0F1F] text-[#C8A04D] flex items-center justify-center shrink-0 border border-[#C8A04D]/30 shadow-xs">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold font-serif text-[#3A0F1F]">Install SareeKart App</h4>
                  <p className="text-[10px] text-[#6b5c4d]">Fast, offline-ready luxury shopping</p>
                </div>
              </div>
              <button
                onClick={() => setShowInstallBanner(false)}
                className="p-1 text-[#6b5c4d] hover:text-[#3A0F1F] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-[#6b5c4d] leading-relaxed">
              Add SareeKart to your home screen for instant access, offline catalog browsing, and exclusive push alerts.
            </p>

            <div className="flex gap-2 pt-1">
              <button
                onClick={handleInstallClick}
                className="flex-1 h-10 bg-[#3A0F1F] hover:bg-[#5B1832] text-[#C8A04D] font-bold text-xs uppercase tracking-widest rounded-full shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                <Download className="w-4 h-4" /> Install App
              </button>
              <button
                onClick={() => setShowInstallBanner(false)}
                className="px-4 h-10 bg-white border border-[#E6DFD3] hover:bg-[#FAF8F5] text-[#6b5c4d] font-bold text-xs rounded-full transition-all cursor-pointer"
              >
                Not Now
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
