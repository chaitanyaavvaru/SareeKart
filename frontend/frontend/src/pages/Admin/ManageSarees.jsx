import { useState, useEffect, useCallback } from 'react';
import { 
  Plus, 
  Search, 
  Edit2, 
  Trash2, 
  Loader2, 
  X, 
  AlertTriangle,
  Image as ImageIcon,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import productService from '../../services/productService';
import categoryService from '../../services/categoryService';
import SafeImage from '../../components/common/SafeImage';

export default function ManageSarees() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Pagination & Search States
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchActive, setSearchActive] = useState(false);
  
  // Modal States
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Form Fields
  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formPrice, setFormPrice] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formStockQuantity, setFormStockQuantity] = useState('');
  const [formFabric, setFormFabric] = useState('');
  const [formOccasion, setFormOccasion] = useState('');
  const [formColor, setFormColor] = useState('');
  const [formImageUrl, setFormImageUrl] = useState('');

  // Fetch Categories & Products on Mount
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await categoryService.getCategories();
        if (response.success && response.data) {
          setCategories(response.data);
        }
      } catch (err) {
        console.error('Failed to load categories:', err);
      }
    };
    fetchCategories();
  }, []);

  const fetchProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      let response;
      if (searchActive && searchQuery.trim()) {
        response = await productService.searchProducts(searchQuery, { page, size: 10 });
      } else {
        response = await productService.getProducts({ page, size: 10, sortBy: 'createdAt', sortDir: 'desc' });
      }
      
      if (response.success && response.data) {
        setProducts(response.data.content || []);
        setTotalPages(response.data.totalPages || 1);
      } else {
        setError('Failed to retrieve products list.');
      }
    } catch (err) {
      console.error('Error fetching products:', err);
      setError(err.response?.data?.message || 'Error communicating with server.');
    } finally {
      setLoading(false);
    }
  }, [page, searchActive, searchQuery]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchProducts();
  }, [fetchProducts]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setSearchActive(true);
  };

  const handleClearSearch = () => {
    setSearchQuery('');
    setPage(0);
    setSearchActive(false);
  };

  // Open Modal for Add
  const handleOpenAddModal = () => {
    setEditingProduct(null);
    setFormName('');
    setFormDescription('');
    setFormPrice('');
    setFormCategoryId(categories[0]?.id || '');
    setFormStockQuantity('');
    setFormFabric('Silk');
    setFormOccasion('Wedding');
    setFormColor('');
    setFormImageUrl('');
    setModalError(null);
    setModalOpen(true);
  };

  // Open Modal for Edit
  const handleOpenEditModal = (product) => {
    setEditingProduct(product);
    setFormName(product.name || '');
    setFormDescription(product.description || '');
    setFormPrice(product.price ? product.price.toString() : '');
    setFormCategoryId(product.categoryId || '');
    setFormStockQuantity(product.stockQuantity ? product.stockQuantity.toString() : '0');
    setFormFabric(product.fabric || 'Silk');
    setFormOccasion(product.occasion || 'Wedding');
    setFormColor(product.color || '');
    setFormImageUrl(product.images && product.images.length > 0 ? product.images[0] : '');
    setModalError(null);
    setModalOpen(true);
  };

  // Handle Form Submit
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formName.trim() || !formPrice || !formStockQuantity) {
      setModalError('Please fill all required fields.');
      return;
    }

    const payload = {
      name: formName,
      description: formDescription,
      price: parseFloat(formPrice),
      categoryId: parseInt(formCategoryId),
      stockQuantity: parseInt(formStockQuantity),
      fabric: formFabric,
      occasion: formOccasion,
      color: formColor,
      images: formImageUrl.trim() ? [formImageUrl.trim()] : ['https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600']
    };

    try {
      setModalLoading(true);
      setModalError(null);
      
      if (editingProduct) {
        await productService.updateProduct(editingProduct.id, payload);
      } else {
        await productService.createProduct(payload);
      }
      
      setModalOpen(false);
      fetchProducts();
    } catch (err) {
      console.error('Failed to save product:', err);
      setModalError(err.response?.data?.message || 'Error occurred while saving product.');
    } finally {
      setModalLoading(false);
    }
  };

  // Handle Product Delete
  const handleDeleteProduct = async (id, name) => {
    if (window.confirm(`Are you sure you want to delete product "${name}"?`)) {
      try {
        setLoading(true);
        await productService.deleteProduct(id);
        fetchProducts();
      } catch (err) {
        console.error('Failed to delete product:', err);
        alert(err.response?.data?.message || 'Error deleting product.');
        setLoading(false);
      }
    }
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(val);
  };

  return (
    <div className="space-y-6 animate-fade-in text-sm">
      
      {/* Header section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold font-serif text-maroon">Manage Sarees</h1>
          <p className="text-xs text-text-secondary mt-0.5">Add, edit, or remove catalog sarees in the database</p>
        </div>
        <button 
          onClick={handleOpenAddModal}
          className="inline-flex items-center gap-2 px-5 py-2 bg-maroon hover:bg-maroon-dark text-white rounded-xl text-xs font-bold shadow-xs hover:shadow-md transition-all shrink-0 cursor-pointer"
        >
          <Plus className="w-4 h-4" /> Add Saree Product
        </button>
      </div>

      {/* Filters & Search */}
      <div className="bg-white border border-border rounded-xl p-4 flex flex-col md:flex-row gap-4 items-center justify-between shadow-xs">
        <form onSubmit={handleSearchSubmit} className="relative w-full md:max-w-md flex gap-2">
          <div className="relative flex-grow">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted">
              <Search className="w-4 h-4" />
            </span>
            <input 
              type="text" 
              placeholder="Search product name or description..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-8 py-1.5 bg-white border border-[#F4F4F4] rounded-xl outline-none text-xs focus:border-[#C9A227] shadow-xs"
            />
            {searchQuery && (
              <button 
                type="button"
                onClick={handleClearSearch}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-maroon focus:outline-none"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
          <button 
            type="submit"
            className="px-4 py-1.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-gold rounded-xl text-xs font-bold cursor-pointer"
          >
            Search
          </button>
        </form>
      </div>

      {/* Main Table view */}
      {loading ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-4">
          <Loader2 className="w-8 h-8 text-maroon animate-spin" />
          <p className="text-xs text-text-secondary">Refreshing catalog database...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg font-medium">
          {error}
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-16 bg-white border border-border rounded-2xl">
          <ImageIcon className="w-10 h-10 text-text-muted mx-auto mb-3" />
          <h3 className="font-bold text-maroon">No products found</h3>
          <p className="text-xs text-text-secondary mt-1">Try resetting search or adding a new saree to your collection.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="bg-white border border-border rounded-xl shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-[#FAF8F5] border-b border-border text-xs">
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Product</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Category</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Price</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Stock</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Fabric / Occasion</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Status</th>
                    <th className="px-6 py-3 text-[10px] uppercase font-bold text-text-muted tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border text-xs">
                  {products.map((p) => {
                    const isLowStock = p.stockQuantity < 10 && p.stockQuantity > 0;
                    const isOutOfStock = p.stockQuantity === 0;
                    
                    return (
                      <tr key={p.id} className="hover:bg-[#FAF8F5] transition-colors">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-14 h-18 shrink-0 overflow-hidden rounded-xl border border-border bg-cream">
                              <SafeImage 
                                src={p.images && p.images.length > 0 ? p.images[0] : ""} 
                                alt={p.name} 
                                productName={p.name}
                                category={p.categoryName || 'Saree'}
                                aspectRatioClass="aspect-[7/9]"
                                className="w-full h-full object-cover"
                              />
                            </div>
                            <div className="min-w-0">
                              <p className="font-bold text-text-primary truncate max-w-[200px]" title={p.name}>{p.name}</p>
                              <p className="text-[10px] text-text-muted mt-0.5 uppercase tracking-wider">ID: #{p.id}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 font-medium text-text-secondary">{p.categoryName || 'Saree'}</td>
                        <td className="px-6 py-4 font-bold text-text-primary">{formatCurrency(p.price)}</td>
                        <td className="px-6 py-4">
                          <span className={`font-bold ${
                            isOutOfStock ? 'text-red-600' : isLowStock ? 'text-amber-600' : 'text-green-600'
                          }`}>
                            {p.stockQuantity}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <p className="text-text-primary font-medium">{p.fabric || 'Silk'}</p>
                          <p className="text-[10px] text-text-muted mt-0.5">{p.occasion || 'Wedding'}</p>
                        </td>
                        <td className="px-6 py-4">
                          {isOutOfStock ? (
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-50 text-red-700 border border-red-200">OUT OF STOCK</span>
                          ) : isLowStock ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                              <AlertTriangle className="w-3 h-3" /> LOW STOCK
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-green-50 text-green-700 border border-green-200">ACTIVE</span>
                          )}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-2">
                            <button 
                              onClick={() => handleOpenEditModal(p)}
                              className="p-1 text-blue-600 hover:bg-blue-50 border border-transparent hover:border-blue-200 rounded-xl cursor-pointer"
                              title="Edit Product"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>
                            <button 
                              onClick={() => handleDeleteProduct(p.id, p.name)}
                              className="p-1 text-red-600 hover:bg-red-50 border border-transparent hover:border-red-200 rounded-xl cursor-pointer"
                              title="Delete Product"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center bg-white border border-border px-6 py-3.5 rounded-xl shadow-xs text-xs font-semibold">
              <span className="text-text-secondary">Page {page + 1} of {totalPages}</span>
              <div className="flex gap-2 items-center">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 bg-white border border-border rounded-xl disabled:opacity-50 flex items-center gap-1 cursor-pointer"
                >
                  <ChevronLeft className="w-3.5 h-3.5" /> Prev
                </button>
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setPage(i)}
                    className={`px-3 py-1 rounded-lg text-xs font-bold cursor-pointer ${
                      page === i
                        ? 'bg-[#3A1028] text-white'
                        : 'bg-white border border-border text-text-secondary hover:bg-[#FAF8F5]'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                  className="px-3 py-1 bg-white border border-border rounded-xl disabled:opacity-50 flex items-center gap-1 cursor-pointer"
                >
                  Next <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── ADD / EDIT MODAL ── */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden max-h-[90vh] flex flex-col">
            
            {/* Modal Header */}
            <div className="bg-[#3A1028] text-white px-6 py-4 flex items-center justify-between shrink-0 rounded-t-2xl">
              <h3 className="font-serif font-extrabold text-base tracking-wide text-gold">
                {editingProduct ? 'Edit Saree Product' : 'Add New Saree Product'}
              </h3>
              <button 
                onClick={() => setModalOpen(false)}
                className="text-white/80 hover:text-white rounded-full p-1 hover:bg-white/10 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Content */}
            <form onSubmit={handleFormSubmit} className="flex-grow overflow-y-auto p-6 space-y-4">
              {modalError && (
                <div className="bg-red-50 border border-red-200 text-red-800 text-xs px-4 py-2.5 rounded-lg font-medium">
                  {modalError}
                </div>
              )}

              {/* Product Name */}
              <div className="space-y-1">
                <label className="text-[10px] uppercase font-bold text-text-secondary">Product Name *</label>
                <input 
                  type="text" 
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Royal Banarasi Silk Saree"
                  required
                  className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                />
              </div>

              {/* Category, Fabric, Occasion Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Category *</label>
                  <select
                    value={formCategoryId}
                    onChange={(e) => setFormCategoryId(e.target.value)}
                    required
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Fabric Type</label>
                  <select
                    value={formFabric}
                    onChange={(e) => setFormFabric(e.target.value)}
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  >
                    {["Silk", "Cotton", "Chiffon", "Georgette", "Linen", "Organza"].map(f => (
                      <option key={f} value={f}>{f}</option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Occasion</label>
                  <select
                    value={formOccasion}
                    onChange={(e) => setFormOccasion(e.target.value)}
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  >
                    {["Wedding", "Festive", "Casual", "Party", "Formal"].map(o => (
                      <option key={o} value={o}>{o}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Price, Stock, Color Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Price (INR) *</label>
                  <input 
                    type="number" 
                    step="0.01"
                    min="0.01"
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    placeholder="4999"
                    required
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Stock Quantity *</label>
                  <input 
                    type="number" 
                    min="0"
                    value={formStockQuantity}
                    onChange={(e) => setFormStockQuantity(e.target.value)}
                    placeholder="20"
                    required
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] uppercase font-bold text-text-secondary">Color</label>
                  <input 
                    type="text" 
                    value={formColor}
                    onChange={(e) => setFormColor(e.target.value)}
                    placeholder="e.g. Red"
                    className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                  />
                </div>
              </div>

              {/* Product Image URL */}
              <div className="space-y-1">
                <label className="text-[10px] uppercase font-bold text-text-secondary">Product Image URL</label>
                <input 
                  type="url" 
                  value={formImageUrl}
                  onChange={(e) => setFormImageUrl(e.target.value)}
                  placeholder="https://images.unsplash.com/... or leave blank for placeholder"
                  className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227]"
                />
              </div>

              {/* Description */}
              <div className="space-y-1">
                <label className="text-[10px] uppercase font-bold text-text-secondary">Description</label>
                <textarea 
                  rows={4}
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder="Describe the weave pattern, zari craft details, craftsmanship etc."
                  className="w-full bg-white border border-[#F4F4F4] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-[#C9A227] resize-none"
                />
              </div>
            </form>

            {/* Modal Actions */}
            <div className="bg-[#FAF8F5] border-t border-border px-6 py-4 flex justify-end gap-3 shrink-0 rounded-b-2xl">
              <button 
                type="button"
                onClick={() => setModalOpen(false)}
                className="px-4 py-1.5 bg-white border border-border rounded-xl text-xs font-semibold cursor-pointer"
              >
                Cancel
              </button>
              <button 
                type="submit"
                onClick={handleFormSubmit}
                disabled={modalLoading}
                className="px-5 py-1.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-gold font-bold rounded-xl text-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                {modalLoading ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" /> Saving...
                  </>
                ) : (
                  'Save Product'
                )}
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
