import { useState, useEffect } from 'react';
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
import lookupService from '../../services/lookupService';
import SareePhotoDropzone from '../../components/admin/SareePhotoDropzone';
import ColorSwatchPicker from '../../components/admin/ColorSwatchPicker';

export default function ManageSarees() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [categoryTree, setCategoryTree] = useState([]);
  const [fabrics, setFabrics] = useState([]);
  const [occasions, setOccasions] = useState([]);
  const [colors, setColors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchActive, setSearchActive] = useState(false);
  
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState(null);

  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formPrice, setFormPrice] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formStockQuantity, setFormStockQuantity] = useState('');
  const [formFabric, setFormFabric] = useState('');
  const [formFabricId, setFormFabricId] = useState(null);
  const [formOccasion, setFormOccasion] = useState('');
  const [formOccasionId, setFormOccasionId] = useState(null);
  const [formColor, setFormColor] = useState('');
  const [formColorId, setFormColorId] = useState(null);
  const [formImages, setFormImages] = useState([]); // replaces single formImageUrl

  useEffect(() => {
    const fetchLookups = async () => {
      try {
        const [catRes, treeRes, fabRes, occRes, colRes] = await Promise.all([
          categoryService.getCategories(),
          categoryService.getCategoryTree(),
          lookupService.getFabrics(),
          lookupService.getOccasions(),
          lookupService.getColors()
        ]);

        if (catRes.success && catRes.data) setCategories(catRes.data);
        if (treeRes.success && treeRes.data) setCategoryTree(treeRes.data);
        if (fabRes.success && fabRes.data) setFabrics(fabRes.data);
        if (occRes.success && occRes.data) setOccasions(occRes.data);
        if (colRes.success && colRes.data) setColors(colRes.data);
      } catch (err) {
        console.error('Failed to load lookup metadata:', err);
      }
    };
    fetchLookups();
  }, []);

  const fetchProducts = async () => {
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
  };

  useEffect(() => {
    fetchProducts();
  }, [page, searchActive]);

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

  const handleOpenAddModal = () => {
    setEditingProduct(null);
    setFormName('');
    setFormDescription('');
    setFormPrice('');
    setFormCategoryId(categories[0]?.id || '');
    setFormStockQuantity('');
    const defaultFabric = fabrics[0]?.name || 'Silk';
    const defaultFabricId = fabrics[0]?.id || null;
    const defaultOccasion = occasions[0]?.name || 'Wedding';
    const defaultOccasionId = occasions[0]?.id || null;
    setFormFabric(defaultFabric);
    setFormFabricId(defaultFabricId);
    setFormOccasion(defaultOccasion);
    setFormOccasionId(defaultOccasionId);
    setFormColor('');
    setFormColorId(null);
    setFormImages([]);
    setModalError(null);
    setModalOpen(true);
  };

  const handleOpenEditModal = async (product) => {
    // 1. Immediately reset form state so previous product's data/photos do not linger
    setEditingProduct(product);
    setFormName(product.name || '');
    setFormDescription(product.description || '');
    setFormPrice(product.price ? product.price.toString() : '');
    setFormCategoryId(product.categoryId || '');
    setFormStockQuantity(product.stockQuantity ? product.stockQuantity.toString() : '0');
    setFormFabric(product.fabric || 'Silk');
    setFormFabricId(product.fabricId || null);
    setFormOccasion(product.occasion || 'Wedding');
    setFormOccasionId(product.occasionId || null);
    setFormColor(product.color || '');
    setFormColorId(product.colorId || null);
    setFormImages([]); // Clear images immediately during fetch to avoid race condition / bleed
    setModalError(null);
    setModalOpen(true);

    try {
      const res = await productService.getProductById(product.id);
      const fullProduct = res.data || res;
      if (fullProduct) {
        setFormName(fullProduct.name || '');
        setFormDescription(fullProduct.description || '');
        setFormPrice(fullProduct.price ? fullProduct.price.toString() : '');
        setFormCategoryId(fullProduct.categoryId || fullProduct.category?.id || '');
        setFormStockQuantity(fullProduct.stockQuantity != null ? fullProduct.stockQuantity.toString() : '0');
        setFormFabric(fullProduct.fabric || 'Silk');
        setFormFabricId(fullProduct.fabricId || null);
        setFormOccasion(fullProduct.occasion || 'Wedding');
        setFormOccasionId(fullProduct.occasionId || null);
        setFormColor(fullProduct.color || '');
        setFormColorId(fullProduct.colorId || null);
        setFormImages(Array.isArray(fullProduct.images) ? [...fullProduct.images] : []);
      }
    } catch (err) {
      console.warn('Could not fetch fresh product details, falling back to row data', err);
      setFormImages(Array.isArray(product.images) ? [...product.images] : []);
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formName.trim() || !formPrice || !formStockQuantity) {
      setModalError('Please fill all required fields.');
      return;
    }

    const parsedCategoryId = formCategoryId ? parseInt(formCategoryId, 10) : (categories[0]?.id || null);

    const payload = {
      name: formName.trim(),
      description: formDescription,
      price: parseFloat(formPrice),
      categoryId: isNaN(parsedCategoryId) ? null : parsedCategoryId,
      stockQuantity: parseInt(formStockQuantity, 10) || 0,
      fabricId: formFabricId,
      occasionId: formOccasionId,
      colorId: formColorId,
      fabric: formFabric,
      occasion: formOccasion,
      color: formColor,
      images: Array.isArray(formImages) ? formImages : []
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
      const errMsg = err.response?.data?.message || err.response?.data?.error || err.message || 'Error occurred while saving product.';
      setModalError(errMsg);
    } finally {
      setModalLoading(false);
    }
  };

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
    <div className="space-y-6 text-sm text-left">
      
      {/* Header section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-cormorant font-bold text-[#121212]">Manage Sarees</h1>
          <p className="text-xs text-[#666666] mt-0.5 font-normal">Add, edit, or remove catalog sarees in the database</p>
        </div>
        <button 
          onClick={handleOpenAddModal}
          className="h-11 px-6 bg-[#121212] hover:bg-black text-white rounded-full text-xs font-semibold uppercase tracking-widest shadow-xs hover:shadow-md transition-all shrink-0 cursor-pointer flex items-center gap-2"
        >
          <Plus className="w-4 h-4 text-[#E85D4F]" /> Add Saree Product
        </button>
      </div>

      {/* Filters & Search */}
      <div className="bg-white border border-[#DDE4EA] rounded-2xl p-4 flex flex-col md:flex-row gap-4 items-center justify-between shadow-xs">
        <form onSubmit={handleSearchSubmit} className="relative w-full md:max-w-md flex gap-2">
          <div className="relative flex-grow">
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#888888]">
              <Search className="w-4 h-4" />
            </span>
            <input 
              type="text" 
              placeholder="Search product name or description..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-8 h-10 bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl outline-none text-xs focus:border-[#E85D4F]"
            />
            {searchQuery && (
              <button 
                type="button"
                onClick={handleClearSearch}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[#888888] hover:text-[#121212] focus:outline-none"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
          <button 
            type="submit"
            className="h-10 px-5 bg-[#121212] hover:bg-black text-white rounded-full text-xs font-semibold uppercase tracking-widest cursor-pointer"
          >
            Search
          </button>
        </form>
      </div>

      {/* Table view */}
      {loading ? (
        <div className="min-h-[40vh] flex flex-col items-center justify-center space-y-4">
          <Loader2 className="w-8 h-8 text-[#121212] animate-spin" />
          <p className="text-xs text-[#666666]">Refreshing catalog database...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-900 text-xs p-4 rounded-xl font-semibold">
          {error}
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-16 bg-white border border-[#DDE4EA] rounded-2xl space-y-2">
          <ImageIcon className="w-10 h-10 text-[#888888] mx-auto" />
          <h3 className="font-cormorant font-bold text-xl text-[#121212]">No products found</h3>
          <p className="text-xs text-[#666666]">Try resetting search or adding a new saree to your collection.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="bg-white border border-[#DDE4EA] rounded-2xl shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-[#F5F7FA] border-b border-[#DDE4EA] text-xs">
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Product</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Category</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Price</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Stock</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Fabric / Occasion</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Status</th>
                    <th className="px-6 py-3.5 text-[10px] uppercase font-bold text-[#121212] tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#DDE4EA] text-xs">
                  {products.map((p) => {
                    const isLowStock = p.stockQuantity < 10 && p.stockQuantity > 0;
                    const isOutOfStock = p.stockQuantity === 0;
                    
                    return (
                      <tr key={p.id} className="hover:bg-[#F5F7FA] transition-colors">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            {p.images && p.images.length > 0 && p.images[0] ? (
                              <img 
                                src={p.images[0]} 
                                alt={p.name} 
                                className="w-12 h-16 object-cover rounded-xl border border-[#DDE4EA] shrink-0 bg-white"
                              />
                            ) : (
                              <div className="w-12 h-16 rounded-xl border border-dashed border-[#DDE4EA] bg-[#F5F7FA] flex flex-col items-center justify-center text-center p-1 shrink-0">
                                <ImageIcon className="w-4 h-4 text-[#A0AEC0]" />
                                <span className="text-[8px] font-bold text-[#A0AEC0] mt-0.5 leading-none">No Drape</span>
                              </div>
                            )}
                            <div className="min-w-0">
                              <p className="font-bold text-[#121212] truncate max-w-[200px]" title={p.name}>{p.name}</p>
                              <p className="text-[10px] text-[#888888] mt-0.5 uppercase tracking-wider">ID: #{p.id}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 font-medium text-[#666666]">{p.categoryName || 'Saree'}</td>
                        <td className="px-6 py-4 font-bold text-[#121212]">{formatCurrency(p.price)}</td>
                        <td className="px-6 py-4">
                          <span className={`font-bold ${
                            isOutOfStock ? 'text-red-600' : isLowStock ? 'text-amber-600' : 'text-emerald-700'
                          }`}>
                            {p.stockQuantity}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <p className="text-[#121212] font-medium">{p.fabric || 'Silk'}</p>
                          <p className="text-[10px] text-[#888888] mt-0.5">{p.occasion || 'Wedding'}</p>
                        </td>
                        <td className="px-6 py-4">
                          {isOutOfStock ? (
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-50 text-red-700 border border-red-200">OUT OF STOCK</span>
                          ) : isLowStock ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                              <AlertTriangle className="w-3 h-3" /> LOW STOCK
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">ACTIVE</span>
                          )}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-2">
                            <button 
                              onClick={() => handleOpenEditModal(p)}
                              className="p-1.5 text-blue-600 hover:bg-blue-50 border border-transparent hover:border-blue-200 rounded-xl cursor-pointer"
                              title="Edit Product"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>
                            <button 
                              onClick={() => handleDeleteProduct(p.id, p.name)}
                              className="p-1.5 text-red-600 hover:bg-red-50 border border-transparent hover:border-red-200 rounded-xl cursor-pointer"
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
            <div className="flex justify-between items-center bg-white border border-[#DDE4EA] px-6 py-3.5 rounded-2xl shadow-xs text-xs font-semibold">
              <span className="text-[#666666]">Page {page + 1} of {totalPages}</span>
              <div className="flex gap-2 items-center">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 bg-white border border-[#DDE4EA] rounded-full disabled:opacity-50 flex items-center gap-1 cursor-pointer"
                >
                  <ChevronLeft className="w-3.5 h-3.5" /> Prev
                </button>
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setPage(i)}
                    className={`px-3 py-1 rounded-full text-xs font-bold cursor-pointer ${
                      page === i
                        ? 'bg-[#121212] text-white'
                        : 'bg-white border border-[#DDE4EA] text-[#666666] hover:bg-[#F5F7FA]'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                  className="px-3 py-1 bg-white border border-[#DDE4EA] rounded-full disabled:opacity-50 flex items-center gap-1 cursor-pointer"
                >
                  Next <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ADD / EDIT MODAL */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-[#DDE4EA] shadow-2xl w-full max-w-xl overflow-hidden max-h-[90vh] flex flex-col">
            
            <div className="bg-[#121212] text-white px-6 py-4 flex items-center justify-between shrink-0 rounded-t-2xl">
              <h3 className="font-cormorant font-bold text-lg text-white">
                {editingProduct ? 'Edit Saree Product' : 'Add New Saree Product'}
              </h3>
              <button 
                onClick={() => setModalOpen(false)}
                className="text-white/80 hover:text-white rounded-full p-1 hover:bg-white/10 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="flex-grow overflow-y-auto p-6 space-y-4 text-left">
              {modalError && (
                <div className="bg-red-50 border border-red-200 text-red-900 text-xs p-3 rounded-xl font-semibold">
                  {modalError}
                </div>
              )}

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Product Name *</label>
                <input 
                  type="text" 
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Royal Banarasi Silk Saree"
                  required
                  className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Category *</label>
                  <select
                    value={formCategoryId}
                    onChange={(e) => setFormCategoryId(e.target.value)}
                    required
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                  >
                    {categoryTree.length > 0 ? (
                      categoryTree.map((c) => (
                        c.subcategories && c.subcategories.length > 0 ? (
                          <optgroup key={c.id} label={c.name}>
                            <option value={c.id}>{c.name} (General)</option>
                            {c.subcategories.map((sub) => (
                              <option key={sub.id} value={sub.id}>{sub.name}</option>
                            ))}
                          </optgroup>
                        ) : (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        )
                      ))
                    ) : (
                      categories.map((c) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))
                    )}
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Fabric Type</label>
                  <select
                    value={formFabric}
                    onChange={(e) => {
                      const val = e.target.value;
                      setFormFabric(val);
                      const match = fabrics.find(f => f.name.toLowerCase() === val.toLowerCase());
                      setFormFabricId(match ? match.id : null);
                    }}
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                  >
                    {formFabric && !fabrics.some(f => f.name.toLowerCase() === formFabric.toLowerCase()) && (
                      <option value={formFabric}>{formFabric} (Existing)</option>
                    )}
                    {fabrics.map(f => (
                      <option key={f.id} value={f.name}>{f.name}</option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Occasion</label>
                  <select
                    value={formOccasion}
                    onChange={(e) => {
                      const val = e.target.value;
                      setFormOccasion(val);
                      const match = occasions.find(o => o.name.toLowerCase() === val.toLowerCase());
                      setFormOccasionId(match ? match.id : null);
                    }}
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                  >
                    {formOccasion && !occasions.some(o => o.name.toLowerCase() === formOccasion.toLowerCase()) && (
                      <option value={formOccasion}>{formOccasion} (Existing)</option>
                    )}
                    {occasions.map(o => (
                      <option key={o.id} value={o.name}>{o.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Price (INR) *</label>
                  <input 
                    type="number" 
                    step="0.01"
                    min="0.01"
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    placeholder="4999"
                    required
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Stock Quantity *</label>
                  <input 
                    type="number" 
                    min="0"
                    value={formStockQuantity}
                    onChange={(e) => setFormStockQuantity(e.target.value)}
                    placeholder="20"
                    required
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2.5 text-xs outline-none"
                  />
                </div>
                <ColorSwatchPicker
                  value={formColor}
                  colorId={formColorId}
                  availableColors={colors}
                  onChange={({ colorId, name }) => {
                    setFormColor(name);
                    setFormColorId(colorId);
                  }}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                  Product Photos
                  <span className="ml-1 font-normal text-[#888888] normal-case">(drag & drop or click · up to 6 · JPG/PNG/WebP)</span>
                </label>
                <SareePhotoDropzone
                  productId={editingProduct?.id}
                  value={formImages}
                  onChange={setFormImages}
                  maxPhotos={6}
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">Description</label>
                <textarea 
                  rows={4}
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder="Describe the weave pattern, zari craft details, craftsmanship etc."
                  className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl p-4 text-xs outline-none resize-none"
                />
              </div>
            </form>

            <div className="bg-[#F5F7FA] border-t border-[#DDE4EA] px-6 py-4 flex justify-end gap-3 shrink-0 rounded-b-2xl">
              <button 
                type="button"
                onClick={() => setModalOpen(false)}
                className="h-10 px-5 bg-white border border-[#DDE4EA] rounded-full text-xs font-medium cursor-pointer"
              >
                Cancel
              </button>
              <button 
                type="submit"
                onClick={handleFormSubmit}
                disabled={modalLoading}
                className="h-10 px-6 bg-[#121212] hover:bg-black text-white font-medium rounded-full text-xs flex items-center gap-2 cursor-pointer disabled:opacity-50 uppercase tracking-widest"
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
