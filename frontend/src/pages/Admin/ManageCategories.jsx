import { useState, useEffect, useMemo } from 'react';
import { 
  FolderTree, 
  Plus, 
  Search, 
  Edit2, 
  Trash2, 
  CheckCircle2, 
  XCircle, 
  ChevronRight, 
  ChevronDown, 
  Package, 
  Layers, 
  Sparkles, 
  AlertCircle,
  X,
  Loader2,
  ToggleLeft,
  ToggleRight
} from 'lucide-react';
import categoryService from '../../services/categoryService';
import SEO from '../../components/common/SEO';

export default function ManageCategories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedParents, setExpandedParents] = useState({});

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Form State
  const [formName, setFormName] = useState('');
  const [formSlug, setFormSlug] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formParentId, setFormParentId] = useState('');
  const [formDisplayOrder, setFormDisplayOrder] = useState('0');
  const [formActive, setFormActive] = useState(true);

  // Delete Alert Modal
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const fetchCategories = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await categoryService.getAdminCategories();
      if (res.success && res.data) {
        setCategories(res.data);
      } else {
        setError('Failed to load categories catalog.');
      }
    } catch (err) {
      console.error('Error fetching admin categories:', err);
      setError(err.response?.data?.message || 'Error communicating with server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  // Filter root categories & subcategories
  const rootCategories = useMemo(() => {
    return categories.filter(c => !c.parentId);
  }, [categories]);

  const subcategoryMap = useMemo(() => {
    const map = {};
    categories.forEach(c => {
      if (c.parentId) {
        if (!map[c.parentId]) map[c.parentId] = [];
        map[c.parentId].push(c);
      }
    });
    return map;
  }, [categories]);

  const toggleExpand = (id) => {
    setExpandedParents(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const handleOpenAddModal = (parentId = '') => {
    setEditingCategory(null);
    setFormName('');
    setFormSlug('');
    setFormDescription('');
    setFormParentId(parentId ? parentId.toString() : '');
    setFormDisplayOrder((categories.length + 1).toString());
    setFormActive(true);
    setModalError(null);
    setModalOpen(true);
  };

  const handleOpenEditModal = (cat) => {
    setEditingCategory(cat);
    setFormName(cat.name || '');
    setFormSlug(cat.slug || '');
    setFormDescription(cat.description || '');
    setFormParentId(cat.parentId ? cat.parentId.toString() : '');
    setFormDisplayOrder((cat.displayOrder ?? 0).toString());
    setFormActive(Boolean(cat.active));
    setModalError(null);
    setModalOpen(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formName.trim()) {
      setModalError('Category name is required.');
      return;
    }

    const payload = {
      name: formName.trim(),
      slug: formSlug.trim() || undefined,
      description: formDescription.trim(),
      parentId: formParentId ? parseInt(formParentId, 10) : null,
      displayOrder: parseInt(formDisplayOrder, 10) || 0,
      active: formActive
    };

    try {
      setModalLoading(true);
      setModalError(null);

      if (editingCategory) {
        await categoryService.updateCategory(editingCategory.id, payload);
      } else {
        await categoryService.createCategory(payload);
      }

      setModalOpen(false);
      fetchCategories();
    } catch (err) {
      console.error('Failed to save category:', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to save category.';
      setModalError(msg);
    } finally {
      setModalLoading(false);
    }
  };

  const handleToggleStatus = async (cat) => {
    try {
      await categoryService.toggleCategoryStatus(cat.id);
      fetchCategories();
    } catch (err) {
      console.error('Failed to toggle status:', err);
      alert(err.response?.data?.message || 'Failed to toggle category status.');
    }
  };

  const handlePromptDelete = (cat) => {
    setCategoryToDelete(cat);
    setDeleteError(null);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return;
    try {
      await categoryService.deleteCategory(categoryToDelete.id);
      setDeleteModalOpen(false);
      setCategoryToDelete(null);
      fetchCategories();
    } catch (err) {
      console.error('Delete rejected:', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Cannot delete category.';
      setDeleteError(msg);
    }
  };

  // Metrics
  const totalProductsLinked = categories.reduce((sum, c) => sum + (c.productCount || 0), 0);
  const activeCount = categories.filter(c => c.active).length;

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      <SEO 
        title="Admin Category Taxonomy | SareeKart" 
        description="Manage SareeKart product categories, subcategories, and artisanal collections." 
        noindex={true}
      />

      {/* Top Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-2xl border border-[#DDE4EA] shadow-xs">
        <div>
          <div className="flex items-center gap-2 text-xs font-semibold text-[#888888] tracking-widest uppercase mb-1">
            <FolderTree className="w-3.5 h-3.5 text-[#E85D4F]" />
            <span>Catalog Architecture</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold font-cormorant text-[#121212]">
            Category & Collection Taxonomy
          </h1>
          <p className="text-xs text-[#666666] mt-0.5">
            Hierarchical 2-tier tree organizing artisanal handloom weaves, fabrics, and bridal trousseau.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => handleOpenAddModal()}
            className="h-10 px-5 bg-[#121212] hover:bg-black text-white text-xs font-semibold rounded-full flex items-center gap-2 cursor-pointer transition-all shadow-xs"
          >
            <Plus className="w-4 h-4" /> Add Root Category
          </button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-[#DDE4EA] shadow-2xs">
          <div className="text-[10px] font-bold uppercase tracking-wider text-[#888888]">Total Categories</div>
          <div className="text-2xl font-bold text-[#121212] font-cormorant mt-1">{categories.length}</div>
          <div className="text-[10px] text-[#666666] mt-0.5">{activeCount} active · {categories.length - activeCount} disabled</div>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-[#DDE4EA] shadow-2xs">
          <div className="text-[10px] font-bold uppercase tracking-wider text-[#888888]">Root Collections</div>
          <div className="text-2xl font-bold text-[#121212] font-cormorant mt-1">{rootCategories.length}</div>
          <div className="text-[10px] text-[#666666] mt-0.5">Primary navigation axes</div>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-[#DDE4EA] shadow-2xs">
          <div className="text-[10px] font-bold uppercase tracking-wider text-[#888888]">Subcategories / Weaves</div>
          <div className="text-2xl font-bold text-[#121212] font-cormorant mt-1">{categories.length - rootCategories.length}</div>
          <div className="text-[10px] text-[#666666] mt-0.5">Nested specific crafts</div>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-[#DDE4EA] shadow-2xs">
          <div className="text-[10px] font-bold uppercase tracking-wider text-[#888888]">Catalog Drapes Linked</div>
          <div className="text-2xl font-bold text-[#121212] font-cormorant mt-1">{totalProductsLinked}</div>
          <div className="text-[10px] text-emerald-600 font-semibold mt-0.5">Safe deletion guarded</div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="bg-white rounded-2xl border border-[#DDE4EA] shadow-xs overflow-hidden">
        {/* Controls Bar */}
        <div className="p-4 border-b border-[#F0F0F0] flex flex-col sm:flex-row justify-between items-center gap-3">
          <div className="relative w-full sm:w-80">
            <Search className="w-3.5 h-3.5 text-[#888888] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search category or slug..."
              className="w-full bg-[#F5F7FA] border border-[#DDE4EA] rounded-xl pl-9 pr-3 py-2 text-xs outline-none focus:border-[#E85D4F]"
            />
          </div>
          <div className="text-xs text-[#888888] font-medium">
            Showing {categories.length} taxonomy records
          </div>
        </div>

        {/* Tree Table */}
        {loading ? (
          <div className="p-12 text-center text-xs text-[#888888] flex flex-col items-center justify-center gap-2">
            <Loader2 className="w-6 h-6 animate-spin text-[#E85D4F]" />
            Loading catalog taxonomy...
          </div>
        ) : error ? (
          <div className="p-8 text-center text-xs text-red-600 font-medium">
            {error}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-[#FAFBFD] border-b border-[#DDE4EA] text-[10px] uppercase font-bold text-[#666666] tracking-wider">
                  <th className="py-3 px-6">Category Name & Hierarchy</th>
                  <th className="py-3 px-4">Slug</th>
                  <th className="py-3 px-4 text-center">Order</th>
                  <th className="py-3 px-4 text-center">Drapes</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#F0F0F0]">
                {rootCategories.map(root => {
                  const children = subcategoryMap[root.id] || [];
                  const isExpanded = expandedParents[root.id] ?? true;
                  const matchesSearch = !searchQuery || 
                    root.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                    root.slug?.toLowerCase().includes(searchQuery.toLowerCase());

                  return (
                    <div key={root.id} style={{ display: 'contents' }}>
                      {matchesSearch && (
                        <tr className="hover:bg-[#F9FAFB] transition-colors">
                          <td className="py-3.5 px-6 font-semibold text-[#121212]">
                            <div className="flex items-center gap-2">
                              {children.length > 0 ? (
                                <button
                                  type="button"
                                  onClick={() => toggleExpand(root.id)}
                                  className="p-1 text-[#888888] hover:text-[#121212] rounded cursor-pointer"
                                >
                                  {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
                                </button>
                              ) : (
                                <span className="w-5" />
                              )}
                              <span className="font-bold text-sm text-[#121212]">{root.name}</span>
                              <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-[#F0F4F8] text-[#555555]">
                                Root
                              </span>
                            </div>
                            {root.description && (
                              <div className="text-[11px] text-[#888888] font-normal pl-7 mt-0.5 max-w-md truncate">
                                {root.description}
                              </div>
                            )}
                          </td>
                          <td className="py-3.5 px-4 font-mono text-[11px] text-[#666666]">
                            {root.slug || '—'}
                          </td>
                          <td className="py-3.5 px-4 text-center font-mono text-[11px] text-[#888888]">
                            {root.displayOrder ?? 0}
                          </td>
                          <td className="py-3.5 px-4 text-center">
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-900 border border-amber-200">
                              <Package className="w-3 h-3" />
                              {root.productCount ?? 0}
                            </span>
                          </td>
                          <td className="py-3.5 px-4 text-center">
                            <button
                              type="button"
                              onClick={() => handleToggleStatus(root)}
                              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold cursor-pointer transition-colors ${
                                root.active 
                                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' 
                                  : 'bg-gray-100 text-gray-600 border border-gray-300'
                              }`}
                            >
                              {root.active ? <CheckCircle2 className="w-3 h-3 text-emerald-600" /> : <XCircle className="w-3 h-3 text-gray-500" />}
                              {root.active ? 'Active' : 'Inactive'}
                            </button>
                          </td>
                          <td className="py-3.5 px-6 text-right">
                            <div className="flex items-center justify-end gap-1.5">
                              <button
                                type="button"
                                onClick={() => handleOpenAddModal(root.id)}
                                title="Add Subcategory"
                                className="p-1.5 text-xs text-blue-600 hover:bg-blue-50 rounded-lg cursor-pointer"
                              >
                                <Plus className="w-3.5 h-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleOpenEditModal(root)}
                                title="Edit Category"
                                className="p-1.5 text-xs text-[#555555] hover:bg-gray-100 rounded-lg cursor-pointer"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handlePromptDelete(root)}
                                title="Delete Category"
                                className="p-1.5 text-xs text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      )}

                      {/* Nested Subcategories */}
                      {isExpanded && children.map(sub => {
                        const subMatches = !searchQuery || 
                          sub.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          sub.slug?.toLowerCase().includes(searchQuery.toLowerCase());
                        if (!subMatches) return null;

                        return (
                          <tr key={sub.id} className="bg-[#FAFBFD]/60 hover:bg-[#F5F7FA] transition-colors">
                            <td className="py-2.5 px-6 pl-12 font-medium text-[#333333]">
                              <div className="flex items-center gap-2">
                                <span className="text-gray-300">↳</span>
                                <span className="text-xs font-semibold text-[#222222]">{sub.name}</span>
                                <span className="px-1.5 py-0.2 rounded-full text-[8px] font-semibold bg-gray-200 text-gray-700">
                                  Sub
                                </span>
                              </div>
                            </td>
                            <td className="py-2.5 px-4 font-mono text-[11px] text-[#888888]">
                              {sub.slug || '—'}
                            </td>
                            <td className="py-2.5 px-4 text-center font-mono text-[11px] text-[#888888]">
                              {sub.displayOrder ?? 0}
                            </td>
                            <td className="py-2.5 px-4 text-center">
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-gray-100 text-gray-800">
                                <Package className="w-2.5 h-2.5 text-gray-500" />
                                {sub.productCount ?? 0}
                              </span>
                            </td>
                            <td className="py-2.5 px-4 text-center">
                              <button
                                type="button"
                                onClick={() => handleToggleStatus(sub)}
                                className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-bold cursor-pointer transition-colors ${
                                  sub.active 
                                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' 
                                    : 'bg-gray-100 text-gray-600 border border-gray-300'
                                }`}
                              >
                                {sub.active ? 'Active' : 'Inactive'}
                              </button>
                            </td>
                            <td className="py-2.5 px-6 text-right">
                              <div className="flex items-center justify-end gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => handleOpenEditModal(sub)}
                                  className="p-1.5 text-xs text-[#555555] hover:bg-gray-100 rounded-lg cursor-pointer"
                                >
                                  <Edit2 className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handlePromptDelete(sub)}
                                  className="p-1.5 text-xs text-red-600 hover:bg-red-50 rounded-lg cursor-pointer"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </div>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ADD / EDIT CATEGORY MODAL */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-[#DDE4EA] shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
            <div className="bg-[#121212] text-white px-6 py-4 flex items-center justify-between shrink-0">
              <h3 className="font-cormorant font-bold text-lg text-white">
                {editingCategory ? 'Edit Category' : 'Create Category'}
              </h3>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="text-white/80 hover:text-white rounded-full p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="p-6 space-y-4 text-left">
              {modalError && (
                <div className="bg-red-50 border border-red-200 text-red-900 text-xs p-3 rounded-xl font-semibold">
                  {modalError}
                </div>
              )}

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                  Category Name *
                </label>
                <input
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Pure Silk Sarees, Kanchipuram Weaves"
                  required
                  className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2 text-xs outline-none"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                  URL Slug (Optional — auto-generated if left blank)
                </label>
                <input
                  type="text"
                  value={formSlug}
                  onChange={(e) => setFormSlug(e.target.value)}
                  placeholder="pure-silk-sarees"
                  className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-4 py-2 text-xs outline-none font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                    Parent Collection
                  </label>
                  <select
                    value={formParentId}
                    onChange={(e) => setFormParentId(e.target.value)}
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-3 py-2 text-xs outline-none"
                  >
                    <option value="">None (Top-Level Root)</option>
                    {rootCategories
                      .filter(r => !editingCategory || r.id !== editingCategory.id)
                      .map(r => (
                        <option key={r.id} value={r.id}>{r.name}</option>
                      ))
                    }
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                    Display Order
                  </label>
                  <input
                    type="number"
                    value={formDisplayOrder}
                    onChange={(e) => setFormDisplayOrder(e.target.value)}
                    className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl px-3 py-2 text-xs outline-none font-mono"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase font-bold text-[#121212] tracking-wider block">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder="Artisanal provenance, weave heritage, fabric notes..."
                  className="w-full bg-[#F5F7FA] border border-[#DDE4EA] focus:border-[#E85D4F] rounded-xl p-3 text-xs outline-none resize-none"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="categoryActiveCheckbox"
                  checked={formActive}
                  onChange={(e) => setFormActive(e.target.checked)}
                  className="rounded text-[#E85D4F] focus:ring-0 cursor-pointer"
                />
                <label htmlFor="categoryActiveCheckbox" className="text-xs font-semibold text-[#121212] cursor-pointer">
                  Active (Visible on Storefront & Filters)
                </label>
              </div>

              <div className="bg-[#F5F7FA] border-t border-[#DDE4EA] -mx-6 -mb-6 p-4 flex justify-end gap-3 mt-4">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-5 py-2 bg-white border border-[#DDE4EA] rounded-full text-xs font-medium cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={modalLoading}
                  className="px-6 py-2 bg-[#121212] hover:bg-black text-white text-xs font-medium rounded-full cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                >
                  {modalLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : null}
                  {editingCategory ? 'Update Category' : 'Create Category'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE CONFIRMATION & SAFEGUARD MODAL */}
      {deleteModalOpen && categoryToDelete && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-xs z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-[#DDE4EA] shadow-2xl w-full max-w-md p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-red-50 border border-red-200 flex items-center justify-center shrink-0">
                <AlertCircle className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="font-bold text-sm text-[#121212]">Delete Category</h3>
                <p className="text-xs text-[#666666]">Target: <span className="font-semibold text-[#121212]">{categoryToDelete.name}</span></p>
              </div>
            </div>

            {categoryToDelete.productCount > 0 ? (
              <div className="bg-amber-50 border border-amber-200 text-amber-900 text-xs p-4 rounded-xl space-y-2">
                <div className="font-bold flex items-center gap-1.5">
                  <AlertCircle className="w-4 h-4 text-amber-700" /> Safe Deletion Guard
                </div>
                <p>
                  Cannot delete category <strong>"{categoryToDelete.name}"</strong> because it currently contains <strong>{categoryToDelete.productCount} product(s)</strong>.
                </p>
                <p className="text-[11px] text-amber-800">
                  To protect catalog integrity, please reassign those products or deactivate this category instead.
                </p>
              </div>
            ) : (
              <p className="text-xs text-[#555555]">
                Are you sure you want to permanently delete category <strong>"{categoryToDelete.name}"</strong>? This action cannot be undone.
              </p>
            )}

            {deleteError && (
              <div className="bg-red-50 border border-red-200 text-red-900 text-xs p-3 rounded-xl font-semibold">
                {deleteError}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeleteModalOpen(false)}
                className="px-4 py-2 bg-white border border-[#DDE4EA] rounded-full text-xs font-semibold text-[#555555] cursor-pointer"
              >
                Close
              </button>
              {categoryToDelete.productCount === 0 && (
                <button
                  type="button"
                  onClick={handleConfirmDelete}
                  className="px-5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-full text-xs font-semibold cursor-pointer"
                >
                  Confirm Delete
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
