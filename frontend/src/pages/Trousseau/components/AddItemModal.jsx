import { useState, useEffect } from 'react';
import { X, Search, Plus, Check, Loader2, Sparkles, AlertCircle } from 'lucide-react';
import productService from '../../../services/productService';

export default function AddItemModal({
  isOpen,
  onClose,
  ceremonyName,
  onAddItem,
  existingProductIds = [],
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleClose = () => {
    setSelectedProduct(null);
    setSearchQuery('');
    setNotes('');
    setError(null);
    onClose();
  };

  // Fetch products on open or query change
  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    const fetchCatalog = async () => {
      setLoading(true);
      setError(null);
      try {
        let res;
        if (searchQuery.trim()) {
          res = await productService.searchProducts(searchQuery.trim(), { size: 12 });
        } else {
          res = await productService.getProducts({ size: 12 });
        }
        if (isMounted) {
          const list = res?.data?.content || res?.data || res?.content || [];
          setProducts(Array.isArray(list) ? list : []);
        }
      } catch {
        if (isMounted) setError('Could not load products. Please try again.');
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    const debounceTimer = setTimeout(fetchCatalog, 300);
    return () => {
      isMounted = false;
      clearTimeout(debounceTimer);
    };
  }, [isOpen, searchQuery]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedProduct) return;

    setSubmitting(true);
    setError(null);
    try {
      await onAddItem({
        productId: selectedProduct.id,
        notes: notes.trim() || null,
      });
      handleClose();
    } catch (err) {
      setError(err || 'Failed to add saree to ceremony.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-[#E6DFD3] overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-[#E6DFD3] flex items-center justify-between bg-[#FAF8F5]">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-widest text-[#C89B3C]">
              Catalog Grounding
            </span>
            <h3 className="font-serif text-lg font-bold text-[#2B0F1E]">
              Pin Saree to {ceremonyName}
            </h3>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="p-2 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-black/5 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Bar */}
        <div className="p-4 border-b border-[#E6DFD3] bg-white">
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by weave, fabric, color, or motif (e.g. Kanchipuram, Banarasi, Crimson)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium placeholder:text-gray-400"
            />
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mx-4 mt-3 p-3 text-xs bg-rose-50 text-rose-700 rounded-xl border border-rose-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Body: Product Catalog Grid */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {loading ? (
            <div className="py-16 flex flex-col items-center justify-center gap-2 text-text-muted">
              <Loader2 className="w-6 h-6 animate-spin text-[#C89B3C]" />
              <span className="text-xs uppercase tracking-wider font-semibold">Searching Handloom Catalog...</span>
            </div>
          ) : products.length === 0 ? (
            <div className="py-12 text-center text-xs text-text-muted">
              No sarees found matching "{searchQuery}". Try a different keyword like "Silk" or "Zari".
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {products.map((p) => {
                const isSelected = selectedProduct?.id === p.id;
                const isAlreadyAdded = existingProductIds.includes(p.id);
                const displayImg = p.primaryImageUrl || p.imageUrl || (p.images && p.images[0]?.url);
                const formattedPrice = new Intl.NumberFormat('en-IN', {
                  style: 'currency',
                  currency: 'INR',
                  maximumFractionDigits: 0,
                }).format(p.price || 0);

                return (
                  <div
                    key={p.id}
                    onClick={() => {
                      if (!isAlreadyAdded) setSelectedProduct(isSelected ? null : p);
                    }}
                    className={`relative rounded-xl border p-2.5 transition-all text-left flex flex-col justify-between ${
                      isAlreadyAdded
                        ? 'opacity-50 cursor-not-allowed bg-gray-50 border-gray-200'
                        : isSelected
                        ? 'border-[#C89B3C] bg-[#FAF8F5] ring-2 ring-[#C89B3C]/40 shadow-xs cursor-pointer'
                        : 'border-[#E6DFD3] hover:border-[#C89B3C]/60 hover:bg-[#FAF8F5]/50 cursor-pointer'
                    }`}
                  >
                    <div className="aspect-[3/4] bg-[#F4EFE6] rounded-lg overflow-hidden mb-2 relative">
                      {displayImg ? (
                        <img
                          src={displayImg}
                          alt={p.name}
                          className="w-full h-full object-cover object-center"
                          loading="lazy"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-300 text-xs font-serif">
                          SareeKart
                        </div>
                      )}

                      {isSelected && (
                        <div className="absolute top-1.5 right-1.5 w-5 h-5 rounded-full bg-[#C89B3C] text-white flex items-center justify-center shadow-xs">
                          <Check className="w-3 h-3 stroke-[3]" />
                        </div>
                      )}

                      {isAlreadyAdded && (
                        <div className="absolute inset-0 bg-black/40 backdrop-blur-2xs flex items-center justify-center text-white text-[10px] font-bold uppercase tracking-wider">
                          Added
                        </div>
                      )}
                    </div>

                    <div className="space-y-1">
                      <h4 className="font-serif text-xs font-semibold text-[#22181C] line-clamp-1">
                        {p.name}
                      </h4>
                      <p className="text-xs font-bold text-[#2B0F1E] font-sans">
                        {formattedPrice}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Saree Notes if Selected */}
          {selectedProduct && (
            <div className="pt-3 border-t border-[#E6DFD3] space-y-2">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-text-muted">
                Stylist Notes for {selectedProduct.name} (Optional)
              </label>
              <textarea
                rows={2}
                placeholder="e.g. For evening Muhurtham ceremony with antique temple jewelry"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-[#E6DFD3] focus:border-[#C89B3C] outline-none text-xs font-medium"
              />
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t border-[#E6DFD3] bg-[#FAF8F5] flex items-center justify-between">
          <span className="text-xs text-text-muted">
            {selectedProduct ? `Selected: ${selectedProduct.name}` : 'Click a saree above to select'}
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 hover:text-gray-900 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={!selectedProduct || submitting}
              onClick={handleSubmit}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider bg-[#C89B3C] text-white hover:bg-[#B3872F] transition-colors disabled:opacity-50 cursor-pointer shadow-xs"
            >
              {submitting ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Plus className="w-3.5 h-3.5" />
              )}
              <span>Pin to Ceremony</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
