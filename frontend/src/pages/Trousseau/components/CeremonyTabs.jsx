import { Plus, Trash2 } from 'lucide-react';

export default function CeremonyTabs({
  ceremonies = [],
  activeCeremonyId,
  onSelectCeremony,
  onOpenAddCeremony,
  onRemoveCeremony,
  isOwner = true,
}) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-[#E6DFD3] no-scrollbar">
      {ceremonies.map((ceremony) => {
        const isActive = ceremony.id === activeCeremonyId;
        const itemCount = ceremony.items?.length || ceremony.itemCount || 0;
        const ceremonyTitle = ceremony.title || ceremony.name || 'Ceremony';

        return (
          <div
            key={ceremony.id}
            className={`group relative flex items-center shrink-0 rounded-xl px-4 py-2.5 transition-all text-xs font-semibold cursor-pointer border ${
              isActive
                ? 'bg-[#2B0F1E] text-white border-[#2B0F1E] shadow-sm'
                : 'bg-white text-[#22181C] border-[#E6DFD3] hover:border-[#C89B3C] hover:bg-[#FAF8F5]'
            }`}
            onClick={() => onSelectCeremony(ceremony.id)}
          >
            <div className="flex items-center gap-2">
              <span className="tracking-wide uppercase font-serif text-sm">
                {ceremonyTitle}
              </span>
              <span
                className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                  isActive ? 'bg-[#C89B3C] text-[#2B0F1E]' : 'bg-black/5 text-[#22181C]'
                }`}
              >
                {itemCount}
              </span>
            </div>

            {/* Quick remove ceremony button (owner only) */}
            {isOwner && ceremonies.length > 1 && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  if (window.confirm(`Delete "${ceremonyTitle}" ceremony and all its sarees?`)) {
                    onRemoveCeremony(ceremony.id);
                  }
                }}
                title="Delete ceremony"
                className={`ml-2 p-1 rounded-md opacity-0 group-hover:opacity-100 transition-opacity ${
                  isActive ? 'hover:bg-white/20 text-white/80' : 'hover:bg-rose-50 text-rose-600'
                }`}
              >
                <Trash2 className="w-3 h-3" />
              </button>
            )}
          </div>
        );
      })}

      {/* Add Ceremony Tab Button */}
      {isOwner && (
        <button
          type="button"
          onClick={onOpenAddCeremony}
          className="shrink-0 inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold border border-dashed border-[#C89B3C] text-[#C89B3C] hover:bg-[#C89B3C]/10 transition-colors cursor-pointer"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>New Ceremony</span>
        </button>
      )}
    </div>
  );
}
