import { motion, AnimatePresence } from 'framer-motion';
import { X, RotateCcw, Check, Sparkles, SlidersHorizontal, ArrowUpDown } from 'lucide-react';

const COLOR_FAMILY_HEX = {
  Red: '#FF0000',
  Pink: '#FFC0CB',
  White: '#FFFFFF',
  Green: '#008000',
  Blue: '#000080',
  Yellow: '#FFFF00',
  Gold: '#D4AF37',
  Neutral: '#F5F5DC',
  Metallic: '#E5E4E2',
  Orange: '#E2725B',
  Black: '#000000',
};

export default function MobileFilterDrawer({
  isOpen,
  onClose,
  categories = [],
  selectedCategory,
  onSelectCategory,
  fabrics = [],
  selectedFabric,
  onSelectFabric,
  occasions = [],
  selectedOccasion = 'All',
  onSelectOccasion,
  colorFamilies = [],
  selectedColorFamily = 'All',
  onSelectColorFamily,
  priceOptions = [],
  priceRange,
  onSelectPriceRange,
  inStockOnly = false,
  onToggleInStock,
  sortBy,
  sortDir,
  onSelectSort,
  onClearAll,
  totalCount = 0,
}) {
  const sortOptions = [
    { label: 'Newest Arrivals', field: 'createdAt', dir: 'desc' },
    { label: 'Price: Low to High', field: 'price', dir: 'asc' },
    { label: 'Price: High to Low', field: 'price', dir: 'desc' },
    { label: 'Name: A to Z', field: 'name', dir: 'asc' },
  ];

  const hasActiveFilters =
    (selectedCategory && selectedCategory !== 'All') ||
    (selectedFabric && selectedFabric !== 'All') ||
    (selectedOccasion && selectedOccasion !== 'All') ||
    (selectedColorFamily && selectedColorFamily !== 'All') ||
    (priceRange && priceRange !== 'All') ||
    Boolean(inStockOnly);

  return (
    <AnimatePresence>
      {isOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Filter sarees mobile drawer"
          className="md:hidden fixed inset-0 z-50 flex flex-col justify-end"
        >
          {/* Dimmed Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-black/60 backdrop-blur-xs"
          />

          {/* Sliding Bottom Sheet Container */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 28, stiffness: 320 }}
            className="relative z-10 flex max-h-[85vh] flex-col rounded-t-2xl bg-white shadow-2xl pb-safe"
          >
            {/* Grab Handle */}
            <div className="flex justify-center pt-3 pb-1">
              <div className="h-1.5 w-12 rounded-full bg-[#DDD8CF]" />
            </div>

            {/* Header */}
            <div className="flex items-center justify-between border-b border-[#EDE7DC] px-5 py-3.5">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="h-4 w-4 text-[#C8A04D]" />
                <h2 className="text-base font-bold text-[#17211F]">Filter Sarees</h2>
                {hasActiveFilters && (
                  <span className="rounded-full bg-[#3A0F1F] px-2 py-0.5 text-[10px] font-black text-white">
                    Active
                  </span>
                )}
              </div>
              <div className="flex items-center gap-2">
                {hasActiveFilters && (
                  <button
                    onClick={onClearAll}
                    className="flex items-center gap-1 text-xs font-bold text-[#B84F49] hover:underline"
                  >
                    <RotateCcw className="h-3 w-3" />
                    Reset all
                  </button>
                )}
                <button
                  onClick={onClose}
                  aria-label="Close filters"
                  className="flex h-8 w-8 items-center justify-center rounded-full bg-[#F7F4EE] text-[#17211F] transition hover:bg-[#DDD8CF]"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Scrollable Filter Content */}
            <div className="flex-1 overflow-y-auto px-5 py-4 space-y-6">
              {/* Category Filter */}
              <div>
                <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                  Category
                </p>
                <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by category">
                  {['All', ...categories.map((c) => (typeof c === 'string' ? c : c.name))].map((catName) => {
                    const active = selectedCategory === catName;
                    return (
                      <button
                        key={catName}
                        onClick={() => onSelectCategory(catName)}
                        className={`h-9 rounded-full px-3.5 text-xs font-bold transition-all ${
                          active
                            ? 'bg-[#3A0F1F] text-white shadow-xs'
                            : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        {catName === 'All' ? 'All Categories' : catName}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Fabric Filter */}
              <div>
                <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                  Fabric & Weave
                </p>
                <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by fabric">
                  {['All', ...fabrics.map((f) => (typeof f === 'string' ? f : f.name))].map((fabric) => {
                    const active = selectedFabric === fabric;
                    return (
                      <button
                        key={fabric}
                        onClick={() => onSelectFabric(fabric)}
                        className={`h-9 rounded-full px-3.5 text-xs font-bold transition-all ${
                          active
                            ? 'bg-[#C8A04D] text-[#3A0F1F] shadow-xs'
                            : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        {fabric === 'All' ? 'All Fabrics' : fabric}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Occasion Filter */}
              {occasions.length > 0 && (
                <div>
                  <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                    Occasion
                  </p>
                  <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by occasion">
                    {['All', ...occasions.map((o) => (typeof o === 'string' ? o : o.name))].map((occasion) => {
                      const active = selectedOccasion === occasion;
                      return (
                        <button
                          key={occasion}
                          onClick={() => onSelectOccasion && onSelectOccasion(occasion)}
                          className={`h-9 rounded-full px-3.5 text-xs font-bold transition-all ${
                            active
                              ? 'bg-[#1E6A62] text-white shadow-xs'
                              : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                          }`}
                        >
                          {occasion === 'All' ? 'All Occasions' : occasion}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Color Family Swatches */}
              {colorFamilies.length > 0 && (
                <div>
                  <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                    Color Mood
                  </p>
                  <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by color">
                    {['All', ...colorFamilies].map((family) => {
                      const active = selectedColorFamily === family;
                      const hex = COLOR_FAMILY_HEX[family] || '#CCC';
                      return (
                        <button
                          key={family}
                          onClick={() => onSelectColorFamily && onSelectColorFamily(family)}
                          className={`h-9 rounded-full px-3.5 text-xs font-bold transition-all inline-flex items-center gap-2 ${
                            active
                              ? 'bg-[#17211F] text-white shadow-xs'
                              : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                          }`}
                        >
                          {family !== 'All' && (
                            <span
                              className="h-3 w-3 rounded-full border border-black/20"
                              style={{ backgroundColor: hex }}
                            />
                          )}
                          {family === 'All' ? 'All Colors' : family}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Price Range Filter */}
              <div>
                <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                  Price Range
                </p>
                <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by price">
                  {priceOptions.map((opt) => {
                    const active = priceRange === opt.value;
                    return (
                      <button
                        key={opt.value}
                        onClick={() => onSelectPriceRange(opt.value)}
                        className={`h-9 rounded-full px-3.5 text-xs font-bold transition-all ${
                          active
                            ? 'bg-[#1E6A62] text-white shadow-xs'
                            : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* In-Stock Toggle */}
              <div>
                <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5">
                  Availability
                </p>
                <label className="flex items-center gap-3 p-3 rounded-lg bg-[#F7F4EE] cursor-pointer">
                  <input
                    type="checkbox"
                    checked={inStockOnly}
                    onChange={(e) => onToggleInStock && onToggleInStock(e.target.checked)}
                    className="h-4 w-4 rounded accent-[#1E6A62]"
                  />
                  <span className="text-xs font-bold text-[#17211F]">
                    In-Stock Sarees Only (exclude backorders)
                  </span>
                </label>
              </div>

              {/* Sort By Filter */}
              <div>
                <p className="text-[11px] font-black uppercase tracking-wider text-[#71817A] mb-2.5 flex items-center gap-1">
                  <ArrowUpDown className="h-3 w-3" />
                  Sort Order
                </p>
                <div className="grid grid-cols-2 gap-2" role="group" aria-label="Sort order">
                  {sortOptions.map((opt) => {
                    const active = sortBy === opt.field && sortDir === opt.dir;
                    return (
                      <button
                        key={`${opt.field}-${opt.dir}`}
                        onClick={() => onSelectSort(opt.field, opt.dir)}
                        className={`h-10 rounded-lg px-3 text-left text-xs font-bold transition-all flex items-center justify-between border ${
                          active
                            ? 'border-[#3A0F1F] bg-[#F9F5F1] text-[#3A0F1F]'
                            : 'border-[#EDE7DC] bg-[#F7F4EE] text-[#42504C]'
                        }`}
                      >
                        <span>{opt.label}</span>
                        {active && <Check className="h-3.5 w-3.5 text-[#3A0F1F]" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Sticky Action Footer */}
            <div className="border-t border-[#EDE7DC] bg-white p-4">
              <button
                onClick={onClose}
                className="w-full h-12 rounded-full bg-[#1E6A62] hover:bg-[#154E48] text-white font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-md active:scale-98 transition"
              >
                <Sparkles className="h-4 w-4" />
                Apply Filters ({totalCount} Sarees)
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
