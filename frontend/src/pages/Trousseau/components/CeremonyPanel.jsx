import { useState } from 'react';
import { Plus, ShoppingCart, Sparkles, CheckSquare, Square, Loader2 } from 'lucide-react';
import ItemCard from './ItemCard';

export default function CeremonyPanel({
  ceremony,
  onOpenAddItem,
  onRemoveItem,
  onVoteItem = null,
  onConvertToCart = null,
  onAiCurate = null,
  isOwner = true,
  actionLoading = false,
  aiCurating = false,
}) {
  const [selectedItemIds, setSelectedItemIds] = useState([]);

  if (!ceremony) {
    return (
      <div className="text-center py-12 text-text-muted">
        Select or create a ceremony to view items.
      </div>
    );
  }

  const items = ceremony.items || [];
  const ceremonyTitle = ceremony.title || ceremony.name || 'Ceremony';

  const handleToggleSelect = (itemId) => {
    setSelectedItemIds((prev) =>
      prev.includes(itemId) ? prev.filter((id) => id !== itemId) : [...prev, itemId]
    );
  };

  const handleSelectAll = () => {
    if (selectedItemIds.length === items.length) {
      setSelectedItemIds([]);
    } else {
      setSelectedItemIds(items.map((i) => i.id));
    }
  };

  const handleConvert = () => {
    if (selectedItemIds.length === 0 || !onConvertToCart) return;
    onConvertToCart(ceremony.id, selectedItemIds);
    setSelectedItemIds([]);
  };

  // Selected items total
  const selectedTotal = items
    .filter((i) => selectedItemIds.includes(i.id))
    .reduce((sum, i) => sum + ((i.productPrice ?? i.price) || 0) * (i.quantity || 1), 0);

  const formattedSelectedTotal = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(selectedTotal);

  return (
    <div className="space-y-6">
      {/* Ceremony Header Toolbar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-[#FAF8F5] p-4 rounded-2xl border border-[#E6DFD3]">
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-serif font-bold text-[#2B0F1E] capitalize">
              {ceremonyTitle}
            </h2>
            <span className="text-xs px-2 py-0.5 rounded-full bg-[#C89B3C]/15 text-[#8F6A1A] font-medium">
              {ceremony.ceremonyType}
            </span>
            {ceremony.colorTheme && (
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-white border border-[#E6DFD3] text-[#22181C]">
                🎨 {ceremony.colorTheme}
              </span>
            )}
          </div>
          {ceremony.notes && (
            <p className="text-xs text-text-muted">{ceremony.notes}</p>
          )}
        </div>

        <div className="flex items-center gap-2.5 flex-wrap">
          {/* AI Bridal Curation Button */}
          {isOwner && onAiCurate && (
            <button
              type="button"
              disabled={aiCurating}
              onClick={() => onAiCurate(ceremony.id)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider bg-white border border-[#C89B3C] text-[#8F6A1A] hover:bg-[#FAF8F5] transition-colors cursor-pointer shadow-2xs disabled:opacity-50"
              title="Auto-curate ensemble using Gemini 2.5 Bridal AI"
            >
              {aiCurating ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#C89B3C]" />
              ) : (
                <Sparkles className="w-3.5 h-3.5 text-[#C89B3C]" />
              )}
              <span>{aiCurating ? 'Curating...' : 'AI Curate'}</span>
            </button>
          )}

          {isOwner && items.length > 0 && (
            <button
              type="button"
              onClick={handleSelectAll}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#22181C] hover:text-[#C89B3C] cursor-pointer"
            >
              {selectedItemIds.length === items.length ? (
                <>
                  <CheckSquare className="w-4 h-4 text-[#C89B3C]" />
                  <span>Deselect All</span>
                </>
              ) : (
                <>
                  <Square className="w-4 h-4" />
                  <span>Select All ({items.length})</span>
                </>
              )}
            </button>
          )}

          {isOwner && (
            <button
              type="button"
              onClick={onOpenAddItem}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#C89B3C] hover:bg-[#B3872F] text-white transition-colors cursor-pointer shadow-xs"
            >
              <Plus className="w-4 h-4" />
              <span>Add Saree</span>
            </button>
          )}
        </div>
      </div>

      {/* Grid of Sarees */}
      {items.length === 0 ? (
        <div className="border-2 border-dashed border-[#E6DFD3] rounded-3xl p-12 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-[#C89B3C]/10 text-[#C89B3C] flex items-center justify-center mx-auto">
            <Sparkles className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h3 className="font-serif text-base font-semibold text-[#2B0F1E]">
              No Sarees Pinned for {ceremonyTitle} Yet
            </h3>
            <p className="text-xs text-text-muted max-w-md mx-auto">
              Search the authentic handloom catalog to pin pure Kanchipuram, Banarasi, or Paithani silks, or use our AI bridal stylist to curate an ensemble.
            </p>
          </div>
          {isOwner && (
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={onOpenAddItem}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] transition-colors cursor-pointer"
              >
                <Plus className="w-4 h-4 text-[#C89B3C]" />
                <span>Browse Catalog</span>
              </button>

              {onAiCurate && (
                <button
                  type="button"
                  disabled={aiCurating}
                  onClick={() => onAiCurate(ceremony.id)}
                  className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-white border border-[#C89B3C] text-[#8F6A1A] hover:bg-[#FAF8F5] transition-colors cursor-pointer shadow-xs"
                >
                  <Sparkles className="w-4 h-4 text-[#C89B3C]" />
                  <span>AI Auto-Curate</span>
                </button>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {items.map((item) => (
            <ItemCard
              key={item.id}
              item={item}
              isSelected={selectedItemIds.includes(item.id)}
              onToggleSelect={isOwner ? handleToggleSelect : null}
              onRemove={isOwner ? onRemoveItem : null}
              onVote={onVoteItem}
              isOwner={isOwner}
            />
          ))}
        </div>
      )}

      {/* Bottom Sticky Action Bar for 1-Click Cart Conversion */}
      {isOwner && selectedItemIds.length > 0 && (
        <div className="sticky bottom-6 z-40 bg-[#2B0F1E] text-white p-4 rounded-2xl shadow-xl flex flex-col sm:flex-row items-center justify-between gap-4 border border-[#4A1E35] animate-in fade-in slide-in-from-bottom-4 duration-300">
          <div className="flex items-center gap-3">
            <span className="w-7 h-7 rounded-lg bg-[#C89B3C] text-[#2B0F1E] font-bold text-xs flex items-center justify-center">
              {selectedItemIds.length}
            </span>
            <div className="space-y-0.5">
              <p className="text-xs font-semibold">
                {selectedItemIds.length} {selectedItemIds.length === 1 ? 'saree' : 'sarees'} selected for cart conversion
              </p>
              <p className="text-[11px] text-white/70">
                Total: <span className="font-bold text-[#E8C872]">{formattedSelectedTotal}</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={() => setSelectedItemIds([])}
              className="px-3 py-2 rounded-xl text-xs font-semibold text-white/80 hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={actionLoading}
              onClick={handleConvert}
              className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-gradient-to-r from-[#C89B3C] to-[#E8C872] text-[#2B0F1E] hover:brightness-105 transition-all cursor-pointer shadow-md disabled:opacity-50"
            >
              <ShoppingCart className="w-4 h-4" />
              <span>{actionLoading ? 'Converting...' : '1-Click Add to Cart'}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
