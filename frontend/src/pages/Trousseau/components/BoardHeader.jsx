import { Calendar, Users, Wifi, WifiOff, Archive, Sparkles, MessageCircle, Share2, Edit2 } from 'lucide-react';

export default function BoardHeader({
  board,
  sseConnected = false,
  onOpenCollaborators,
  onArchiveBoard,
  onOpenEditModal,
  onShareWhatsApp,
  isOwner = true,
}) {
  // Calculate total spend and items
  let totalCost = 0;
  let totalItems = 0;
  (board.ceremonies || []).forEach((c) => {
    (c.items || []).forEach((item) => {
      totalItems += 1;
      totalCost += ((item.productPrice ?? item.price) || 0) * (item.quantity || 1);
    });
  });

  const budgetLimit = board.totalBudget || board.budgetLimit || 0;
  const percentUsed = budgetLimit > 0 ? Math.min(Math.round((totalCost / budgetLimit) * 100), 100) : 0;
  const isOverBudget = budgetLimit > 0 && totalCost > budgetLimit;

  const formattedTotalCost = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(totalCost);

  const formattedBudgetLimit = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(budgetLimit);

  const formattedWeddingDate = board.weddingDate
    ? new Date(board.weddingDate).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : null;

  return (
    <div className="bg-[#FAF8F5] border border-[#E6DFD3] rounded-3xl p-6 sm:p-8 space-y-6 shadow-xs relative overflow-hidden">
      {/* Decorative top gold accent line */}
      <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[#C89B3C] via-[#E8C872] to-[#C89B3C]" />

      {/* Top row: Title, wedding date, and actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1.5">
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold tracking-widest uppercase bg-[#C89B3C]/15 text-[#8F6A1A]">
              <Sparkles className="w-3.5 h-3.5" />
              Bridal Trousseau Studio
            </span>

            {/* SSE Live Sync Pill */}
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                sseConnected
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                  : 'bg-amber-50 text-amber-700 border-amber-200'
              }`}
            >
              {sseConnected ? (
                <>
                  <Wifi className="w-3 h-3 text-emerald-600 animate-pulse" />
                  Live Sync
                </>
              ) : (
                <>
                  <WifiOff className="w-3 h-3 text-amber-600" />
                  Connecting...
                </>
              )}
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-serif font-bold text-[#2B0F1E]">
            {board.title}
          </h1>

          <div className="flex flex-wrap items-center gap-4 text-xs text-text-muted">
            {board.ownerName && (
              <p>Curated for: <span className="font-semibold text-[#22181C]">{board.ownerName}</span></p>
            )}
            {formattedWeddingDate && (
              <span className="flex items-center gap-1 text-[#22181C] font-medium">
                <Calendar className="w-3.5 h-3.5 text-[#C89B3C]" />
                {formattedWeddingDate}
              </span>
            )}
            <span>•</span>
            <span>{totalItems} Sarees across {board.ceremonies?.length || 0} Ceremonies</span>
          </div>
        </div>

        {/* Action buttons */}
        {isOwner && (
          <div className="flex items-center gap-2.5 shrink-0 flex-wrap">
            {/* WhatsApp Share */}
            {onShareWhatsApp && (
              <button
                type="button"
                onClick={onShareWhatsApp}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-[#25D366]/10 text-[#128C7E] hover:bg-[#25D366]/20 transition-colors cursor-pointer border border-[#25D366]/30 shadow-2xs"
                title="Share bridal studio with family on WhatsApp"
              >
                <MessageCircle className="w-4 h-4 text-[#25D366]" />
                <span>WhatsApp Share</span>
              </button>
            )}

            {/* Collaborators Drawer Trigger */}
            <button
              type="button"
              onClick={onOpenCollaborators}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-white text-[#22181C] hover:bg-[#FAF8F5] transition-colors cursor-pointer border border-[#E6DFD3] shadow-2xs"
            >
              <Users className="w-4 h-4 text-[#C89B3C]" />
              <span>Family ({board.collaborators?.length || 0})</span>
            </button>

            {/* Edit Button */}
            {onOpenEditModal && (
              <button
                type="button"
                onClick={onOpenEditModal}
                className="p-2 rounded-xl text-gray-500 hover:text-[#22181C] hover:bg-black/5 transition-colors cursor-pointer"
                title="Edit Board Details"
              >
                <Edit2 className="w-4 h-4" />
              </button>
            )}

            {/* Archive Board */}
            {onArchiveBoard && (
              <button
                type="button"
                onClick={() => {
                  if (window.confirm('Are you sure you want to archive this trousseau board?')) {
                    onArchiveBoard(board.id);
                  }
                }}
                className="p-2 rounded-xl text-gray-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                title="Archive Board"
              >
                <Archive className="w-4 h-4" />
              </button>
            )}
          </div>
        )}
      </div>

      {/* Budget & Spend Progress Bar */}
      {budgetLimit > 0 && (
        <div className="space-y-2 pt-2 border-t border-[#E6DFD3]">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-[#22181C]">
              Budget Utilization:{' '}
              <span className={isOverBudget ? 'text-rose-600 font-bold' : 'text-[#8F6A1A] font-bold'}>
                {formattedTotalCost}
              </span>{' '}
              of {formattedBudgetLimit}
            </span>
            <span className={`font-bold ${isOverBudget ? 'text-rose-600' : 'text-text-muted'}`}>
              {percentUsed}% {isOverBudget && '(Exceeded)'}
            </span>
          </div>

          <div className="w-full bg-[#E6DFD3] h-2 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isOverBudget
                  ? 'bg-rose-500'
                  : percentUsed > 80
                  ? 'bg-amber-500'
                  : 'bg-gradient-to-r from-[#C89B3C] to-[#E8C872]'
              }`}
              style={{ width: `${percentUsed}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
