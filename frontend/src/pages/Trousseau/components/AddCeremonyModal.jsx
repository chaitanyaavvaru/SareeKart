import { useState } from 'react';
import { X, Plus, Loader2 } from 'lucide-react';

const CEREMONY_OPTIONS = [
  { value: 'MUHURTHAM', label: 'Wedding (Muhurtham)' },
  { value: 'ENGAGEMENT', label: 'Engagement / Roka' },
  { value: 'HALDI', label: 'Haldi Rituals' },
  { value: 'MEHNDI', label: 'Mehndi & Henna' },
  { value: 'SANGEET', label: 'Sangeet Night' },
  { value: 'RECEPTION', label: 'Grand Reception' },
  { value: 'OTHER', label: 'Pooja / Special Occasion' },
];

export default function AddCeremonyModal({ isOpen, onClose, onAddCeremony }) {
  const [title, setTitle] = useState('');
  const [ceremonyType, setCeremonyType] = useState('MUHURTHAM');
  const [colorTheme, setColorTheme] = useState('');
  const [targetBudget, setTargetBudget] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Please enter a ceremony title.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await onAddCeremony({
        title: title.trim(),
        ceremonyType,
        colorTheme: colorTheme.trim() || null,
        targetBudget: targetBudget ? Number(targetBudget) : 0,
      });
      setTitle('');
      setColorTheme('');
      setTargetBudget('');
      onClose();
    } catch (err) {
      setError(err || 'Failed to add ceremony.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-[#E6DFD3] overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-[#E6DFD3] flex items-center justify-between bg-[#FAF8F5]">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-widest text-[#C89B3C]">
              Wedding Milestone
            </span>
            <h3 className="font-serif text-lg font-bold text-[#2B0F1E]">
              Add Wedding Ceremony
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-black/5 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {error && (
            <div className="p-3 text-xs bg-rose-50 text-rose-700 rounded-xl border border-rose-200">
              {error}
            </div>
          )}

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
              Ceremony Title *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Traditional Muhurtham, Pastel Ring Ceremony"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                Ceremony Type *
              </label>
              <select
                value={ceremonyType}
                onChange={(e) => setCeremonyType(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium bg-white"
              >
                {CEREMONY_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                Target Budget (INR)
              </label>
              <input
                type="number"
                placeholder="e.g. 50000"
                value={targetBudget}
                onChange={(e) => setTargetBudget(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
              />
            </div>
          </div>

          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
              Color Theme / Palette (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Crimson & Gold, Pastels, Sunshine Yellow"
              value={colorTheme}
              onChange={(e) => setColorTheme(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
            />
          </div>

          {/* Actions */}
          <div className="pt-3 border-t border-[#E6DFD3] flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 hover:text-gray-900 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || !title.trim()}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] transition-colors disabled:opacity-50 cursor-pointer shadow-xs"
            >
              {submitting ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#C89B3C]" />
              ) : (
                <Plus className="w-3.5 h-3.5 text-[#C89B3C]" />
              )}
              <span>Add Ceremony</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
