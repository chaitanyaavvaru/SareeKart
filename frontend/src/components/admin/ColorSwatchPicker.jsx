import React, { useState, useEffect } from 'react';
import { Palette, Check, Sparkles } from 'lucide-react';

export default function ColorSwatchPicker({ 
  value = '', 
  colorId = null, 
  availableColors = [], 
  onChange 
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Find currently selected color from lookup if exists
  const selectedColor = availableColors.find(
    c => (colorId && c.id === colorId) || 
         (value && c.name?.toLowerCase() === value?.toLowerCase())
  );

  const activeHex = selectedColor?.hexCode || '#E85D4F';

  const filteredColors = availableColors.filter(c => 
    !searchQuery || 
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    c.family?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Group by color family
  const groupedFamilies = filteredColors.reduce((acc, c) => {
    const fam = c.family || 'Other';
    if (!acc[fam]) acc[fam] = [];
    acc[fam].push(c);
    return acc;
  }, {});

  const handleSelect = (c) => {
    onChange({
      colorId: c.id,
      name: c.name,
      hexCode: c.hexCode,
      family: c.family
    });
    setIsOpen(false);
  };

  const handleCustomChange = (e) => {
    const rawVal = e.target.value;
    const match = availableColors.find(c => c.name.toLowerCase() === rawVal.trim().toLowerCase());
    if (match) {
      onChange({
        colorId: match.id,
        name: match.name,
        hexCode: match.hexCode,
        family: match.family
      });
    } else {
      onChange({
        colorId: null,
        name: rawVal,
        hexCode: null,
        family: null
      });
    }
  };

  return (
    <div className="relative space-y-1.5">
      <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider flex items-center justify-between">
        <span>Color Shade *</span>
        {selectedColor && (
          <span className="text-[9px] font-medium text-[#888888] flex items-center gap-1">
            <span 
              className="inline-block w-2.5 h-2.5 rounded-full border border-black/20" 
              style={{ backgroundColor: activeHex }} 
            />
            {selectedColor.family} · {selectedColor.hexCode}
          </span>
        )}
      </label>

      <div className="flex gap-2">
        <div className="relative flex-grow">
          <input
            type="text"
            value={value}
            onChange={handleCustomChange}
            placeholder="e.g. Ruby Red, baby pink"
            className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl pl-9 pr-3 py-2.5 text-xs outline-none"
          />
          <button
            type="button"
            onClick={() => setIsOpen(!isOpen)}
            className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border border-black/20 cursor-pointer shadow-xs transition-transform hover:scale-110"
            style={{ backgroundColor: activeHex }}
            title="Open Color Palette"
          />
        </div>

        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className={`px-3 py-2 rounded-xl border text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer ${
            isOpen 
              ? 'bg-[#121212] text-white border-[#121212]' 
              : 'bg-white border-[#DDE4EA] text-[#444444] hover:bg-[#F5F7FA]'
          }`}
        >
          <Palette className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Palette</span>
        </button>
      </div>

      {/* Floating Swatch Palette Dropdown */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 z-40 bg-white border border-[#DDE4EA] shadow-xl rounded-2xl p-4 max-h-72 overflow-y-auto space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-[#F0F0F0]">
            <span className="text-[11px] font-bold uppercase tracking-wider text-[#121212] flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5 text-[#E85D4F]" /> Canonical Palette
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search shade..."
              className="bg-[#F5F7FA] border border-[#E0E0E0] rounded-lg px-2.5 py-1 text-[11px] outline-none w-36"
            />
          </div>

          <div className="space-y-3">
            {Object.keys(groupedFamilies).length === 0 ? (
              <div className="text-[11px] text-[#888888] text-center py-3">No matching shades found</div>
            ) : (
              Object.entries(groupedFamilies).map(([family, colors]) => (
                <div key={family} className="space-y-1.5">
                  <div className="text-[9px] uppercase font-bold text-[#888888] tracking-wider">{family}</div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
                    {colors.map((c) => {
                      const isSelected = selectedColor?.id === c.id || value?.toLowerCase() === c.name.toLowerCase();
                      return (
                        <button
                          key={c.id}
                          type="button"
                          onClick={() => handleSelect(c)}
                          className={`flex items-center gap-2 p-1.5 rounded-lg border text-left cursor-pointer transition-all ${
                            isSelected 
                              ? 'border-[#121212] bg-[#F5F7FA] ring-1 ring-[#121212]' 
                              : 'border-transparent hover:border-[#DDE4EA] hover:bg-[#FAFBFD]'
                          }`}
                        >
                          <span
                            className="w-4 h-4 rounded-full shrink-0 border border-black/15 shadow-2xs"
                            style={{ backgroundColor: c.hexCode }}
                          />
                          <span className="text-[11px] font-medium text-[#222222] truncate">{c.name}</span>
                          {isSelected && <Check className="w-3 h-3 text-[#121212] ml-auto shrink-0" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
