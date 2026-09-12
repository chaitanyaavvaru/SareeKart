import React, { useState, useEffect } from 'react';
import { Globe, ChevronDown, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const SUPPORTED_LANGUAGES = [
  { code: 'en', name: 'English', nativeName: 'English', region: 'Pan-India / Global' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', region: 'Andhra / Telangana' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', region: 'North / Central India' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', region: 'Tamil Nadu' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', region: 'Karnataka' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', region: 'Kerala' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', region: 'Maharashtra' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', region: 'Gujarat' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', region: 'West Bengal' }
];

export default function I18nLanguageSelector() {
  const [selectedLang, setSelectedLang] = useState('en');
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    // Dynamically update document <html lang="..."> for screen reader accessibility
    document.documentElement.lang = selectedLang;
  }, [selectedLang]);

  const currentLanguage = SUPPORTED_LANGUAGES.find(l => l.code === selectedLang) || SUPPORTED_LANGUAGES[0];

  const handleSelectLanguage = (langCode) => {
    setSelectedLang(langCode);
    setIsOpen(false);
  };

  return (
    <div className="relative font-sans text-left">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-1.5 px-3 py-1.5 bg-[#FAF8F5] hover:bg-[#E6DFD3]/40 border border-[#E6DFD3] rounded-full text-xs font-bold text-[#3A0F1F] transition-all cursor-pointer shadow-2xs"
        aria-label="Select Language"
        aria-expanded={isOpen}
      >
        <Globe className="w-3.5 h-3.5 text-[#C8A04D]" />
        <span className="font-serif text-xs">{currentLanguage.nativeName}</span>
        <ChevronDown className={`w-3 h-3 text-[#6b5c4d] transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      <AnimatePresence>
        {isOpen && (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
            <motion.div
              initial={{ opacity: 0, y: 8, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 8, scale: 0.95 }}
              className="absolute right-0 mt-2 z-50 bg-white border border-[#E6DFD3] rounded-2xl p-2 shadow-2xl w-64 space-y-1"
            >
              <div className="px-3 py-2 border-b border-[#E6DFD3] text-[10px] uppercase tracking-wider font-bold text-[#6b5c4d]">
                Select Preferred Language
              </div>
              <div className="max-h-64 overflow-y-auto divide-y divide-[#FAF8F5]">
                {SUPPORTED_LANGUAGES.map((lang) => (
                  <button
                    key={lang.code}
                    onClick={() => handleSelectLanguage(lang.code)}
                    className={`w-full px-3 py-2.5 rounded-xl flex items-center justify-between text-left text-xs transition-colors cursor-pointer ${
                      selectedLang === lang.code
                        ? 'bg-[#3A0F1F] text-[#C8A04D] font-bold'
                        : 'hover:bg-[#FAF8F5] text-[#3A0F1F]'
                    }`}
                  >
                    <div>
                      <span className="block font-serif text-sm">{lang.nativeName}</span>
                      <span className={`text-[10px] block ${selectedLang === lang.code ? 'text-[#C8A04D]/80' : 'text-[#6b5c4d]'}`}>
                        {lang.name} • {lang.region}
                      </span>
                    </div>
                    {selectedLang === lang.code && <Check className="w-4 h-4 text-[#C8A04D] shrink-0" />}
                  </button>
                ))}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
