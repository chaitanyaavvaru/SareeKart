import { Link } from 'react-router-dom';
import { Trash2, ExternalLink, Check, ShoppingBag, Sparkles } from 'lucide-react';
import VoteTally from './VoteTally';

export default function ItemCard({
  item,
  isSelected = false,
  onToggleSelect = null,
  onRemove = null,
  onVote = null,
  userVote = null,
  isOwner = true,
}) {
  const price = item.productPrice ?? item.price ?? 0;
  const imageUrl = item.productImageUrl ?? item.primaryImageUrl ?? item.image;
  const productName = item.productName ?? item.name ?? 'Handloom Saree';

  const formattedPrice = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(price);

  return (
    <div
      className={`group relative bg-white rounded-2xl border transition-all duration-300 overflow-hidden shadow-xs hover:shadow-md ${
        isSelected
          ? 'border-[#C89B3C] ring-2 ring-[#C89B3C]/30'
          : 'border-[#E6DFD3] hover:border-[#C89B3C]/60'
      }`}
    >
      {/* Top Banner Selection Checkbox */}
      {onToggleSelect && (
        <button
          type="button"
          onClick={() => onToggleSelect(item.id)}
          aria-label={isSelected ? 'Deselect saree' : 'Select saree'}
          className={`absolute top-3 left-3 z-10 w-7 h-7 rounded-lg flex items-center justify-center transition-all cursor-pointer shadow-sm ${
            isSelected
              ? 'bg-[#C89B3C] text-white'
              : 'bg-white/90 text-gray-400 hover:text-gray-700 border border-black/10'
          }`}
        >
          {isSelected && <Check className="w-4 h-4 stroke-[3]" />}
        </button>
      )}

      {/* Delete Item Button (Owner only) */}
      {isOwner && onRemove && (
        <button
          type="button"
          onClick={() => onRemove(item.id)}
          title="Remove from ceremony"
          className="absolute top-3 right-3 z-10 w-7 h-7 rounded-lg bg-white/90 hover:bg-rose-50 text-gray-400 hover:text-rose-600 flex items-center justify-center transition-all border border-black/10 shadow-sm cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      )}

      {/* AI Recommended Badge */}
      {item.isAiRecommended && (
        <div className="absolute top-3 left-12 z-10 inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#FAF8F5]/95 text-[#8F6A1A] border border-[#C89B3C]/40 shadow-xs backdrop-blur-xs">
          <Sparkles className="w-2.5 h-2.5 text-[#C89B3C]" />
          <span>AI Pick</span>
        </div>
      )}

      {/* Product Image */}
      <div className="relative aspect-[3/4] bg-[#F4EFE6] overflow-hidden">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={productName}
            className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex flex-col items-center justify-center text-[#2B0F1E]/30 gap-2">
            <ShoppingBag className="w-10 h-10" />
            <span className="text-xs uppercase tracking-wider font-semibold">Handloom Saree</span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="p-4 space-y-3">
        <div>
          <div className="flex items-baseline justify-between gap-2">
            <h4 className="font-serif text-sm font-semibold text-[#22181C] line-clamp-1 group-hover:text-[#C89B3C] transition-colors">
              {productName}
            </h4>
            <span className="text-sm font-bold text-[#2B0F1E] shrink-0 font-sans">
              {formattedPrice}
            </span>
          </div>

          {(item.fabric || item.color) && (
            <p className="text-[11px] text-text-muted mt-0.5">
              {[item.fabric, item.color].filter(Boolean).join(' • ')}
            </p>
          )}

          {item.notes && (
            <p className="text-xs text-text-muted italic mt-1 line-clamp-2 bg-[#FAF8F5] p-1.5 rounded-md border border-[#E6DFD3]/60">
              "{item.notes}"
            </p>
          )}
        </div>

        {/* Voting row & Product link */}
        <div className="pt-2 border-t border-[#E6DFD3]/60 flex items-center justify-between gap-2">
          <VoteTally
            voteCounts={item.voteCounts}
            item={item}
            onVote={onVote}
            userVote={userVote}
            disabled={!onVote}
          />

          <Link
            to={`/products/${item.productId}`}
            target="_blank"
            rel="noopener noreferrer"
            title="View full saree specifications"
            className="p-1.5 rounded-lg text-gray-400 hover:text-[#2B0F1E] hover:bg-[#FAF8F5] transition-colors"
          >
            <ExternalLink className="w-4 h-4" />
          </Link>
        </div>
      </div>
    </div>
  );
}
