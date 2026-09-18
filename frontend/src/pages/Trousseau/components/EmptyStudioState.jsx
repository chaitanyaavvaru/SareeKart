import { useState } from 'react';
import { Sparkles, Plus, Loader2 } from 'lucide-react';

export default function EmptyStudioState({ onCreateBoard, loading = false }) {
  const [showModal, setShowModal] = useState(false);
  const [title, setTitle] = useState('');
  const [weddingDate, setWeddingDate] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState(null);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Please enter a title for your trousseau.');
      return;
    }

    setError(null);
    try {
      await onCreateBoard({
        title: title.trim(),
        weddingDate: weddingDate || null,
        notes: notes.trim() || null,
      });
    } catch (err) {
      setError(err || 'Failed to initialize trousseau board.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-12 px-4 sm:px-6">
      {/* Hero Welcome Card */}
      <div className="relative rounded-3xl bg-gradient-to-b from-[#FAF8F5] to-[#F4EFE6] border border-[#E6DFD3] p-8 sm:p-14 text-center space-y-8 overflow-hidden shadow-sm">
        {/* Decorative corner florals / circles */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-[#C89B3C]/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-[#2B0F1E]/5 rounded-full blur-3xl pointer-events-none" />

        <div className="space-y-3 relative">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#C89B3C]/15 text-[#8F6A1A] text-xs font-bold uppercase tracking-widest">
            <Sparkles className="w-4 h-4" />
            <span>Curated Bridal Experience</span>
          </div>

          <h1 className="font-serif text-3xl sm:text-5xl font-bold text-[#2B0F1E] tracking-tight">
            Collaborative Bridal Trousseau Studio
          </h1>

          <p className="text-sm sm:text-base text-text-muted max-w-xl mx-auto font-sans leading-relaxed">
            Curate your wedding ceremonies, shortlist authentic handlooms from Kanchipuram and Banaras, and invite your family on WhatsApp to vote on your favorite bridal drapes.
          </p>
        </div>

        {/* Feature Highlights Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left max-w-2xl mx-auto">
          <div className="bg-white/80 backdrop-blur-xs p-5 rounded-2xl border border-[#E6DFD3]/80 space-y-1.5">
            <span className="text-xl">🪷</span>
            <h4 className="font-serif text-sm font-semibold text-[#2B0F1E]">Ceremony Categorization</h4>
            <p className="text-xs text-text-muted">Organize looks for Haldi, Mehndi, Muhurtham, and Reception.</p>
          </div>

          <div className="bg-white/80 backdrop-blur-xs p-5 rounded-2xl border border-[#E6DFD3]/80 space-y-1.5">
            <span className="text-xl">💬</span>
            <h4 className="font-serif text-sm font-semibold text-[#2B0F1E]">Family Voting & WhatsApp</h4>
            <p className="text-xs text-text-muted">Share private links so family can react with Love, Like, or Pass.</p>
          </div>

          <div className="bg-white/80 backdrop-blur-xs p-5 rounded-2xl border border-[#E6DFD3]/80 space-y-1.5">
            <span className="text-xl">🛍️</span>
            <h4 className="font-serif text-sm font-semibold text-[#2B0F1E]">1-Click Cart Conversion</h4>
            <p className="text-xs text-text-muted">Directly convert approved bridal sarees into your SareeKart shopping cart.</p>
          </div>
        </div>

        {/* Call to Action */}
        <div>
          <button
            type="button"
            onClick={() => setShowModal(true)}
            className="inline-flex items-center gap-2.5 px-8 py-4 rounded-full text-sm font-bold uppercase tracking-widest bg-gradient-to-r from-[#2B0F1E] to-[#4A1E35] text-white hover:brightness-110 transition-all cursor-pointer shadow-lg hover:shadow-xl hover:-translate-y-0.5"
          >
            <Plus className="w-5 h-5 text-[#C89B3C]" />
            <span>Create Your Trousseau Board</span>
          </button>
        </div>
      </div>

      {/* Initialize Board Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-[#E6DFD3] overflow-hidden">
            <div className="p-6 border-b border-[#E6DFD3] bg-[#FAF8F5]">
              <span className="text-[10px] font-bold uppercase tracking-widest text-[#C89B3C]">
                Get Started
              </span>
              <h3 className="font-serif text-xl font-bold text-[#2B0F1E]">
                Set Up Your Trousseau
              </h3>
            </div>

            <form onSubmit={handleCreate} className="p-6 space-y-4">
              {error && (
                <div className="p-3 text-xs bg-rose-50 text-rose-700 rounded-xl border border-rose-200">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Board Title *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Priya's Wedding Trousseau"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Wedding Date (Optional)
                </label>
                <input
                  type="date"
                  value={weddingDate}
                  onChange={(e) => setWeddingDate(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted mb-1">
                  Notes / Inspiration (Optional)
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. South Indian traditional weaves, antique gold jewelry accents"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
                />
              </div>

              <div className="pt-3 border-t border-[#E6DFD3] flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 hover:text-gray-900 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading || !title.trim()}
                  className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] transition-colors disabled:opacity-50 cursor-pointer"
                >
                  {loading ? (
                    <Loader2 className="w-4 h-4 animate-spin text-[#C89B3C]" />
                  ) : (
                    <Sparkles className="w-4 h-4 text-[#C89B3C]" />
                  )}
                  <span>Launch Studio</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
