import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { SlidersHorizontal, ArrowUpDown, RefreshCw, Search } from 'lucide-react';
import { closeAddedModal, setCartOpen } from '../../redux/slices/cartSlice';
import { filterProducts, searchProducts } from '../../redux/slices/productSlice';
import categoryService from '../../services/categoryService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Pagination from '../../components/common/Pagination';
import ProductCard from '../../components/product/ProductCard';
import api from '../../api/axiosConfig';
import { motion } from 'framer-motion';

const FABRICS = ["Silk", "Cotton", "Chiffon", "Georgette"];

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const dispatch = useDispatch();

  const { user } = useSelector(state => state.auth);
  const { products, loading, error, pagination } = useSelector(state => state.products);

  const [categories, setCategories] = useState([]);
  const [wishlistProductIds, setWishlistProductIds] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || 'All');
  const [selectedFabric, setSelectedFabric] = useState(searchParams.get('fabric') || 'All');
  const [priceRange, setPriceRange] = useState('All');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);

  // Sync state from query parameters on load
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const response = await categoryService.getCategories();
        setCategories(response?.data || []);
      } catch (err) {
        console.error('Failed to load categories', err);
      }
    };
    loadCategories();
  }, []);

  useEffect(() => {
    if (user) {
      const fetchWishlist = async () => {
        try {
          const wishRes = await api.get('/wishlist');
          if (wishRes.data?.success) {
            setWishlistProductIds((wishRes.data.data?.items || wishRes.data.data || []).map(w => w.id));
          }
        } catch (e) {
          console.error("Failed to load wishlist in ProductsPage", e);
        }
      };
      fetchWishlist();
    } else {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setWishlistProductIds([]);
    }
  }, [user]);

  useEffect(() => {
    const catParam = searchParams.get('category');
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (catParam) setSelectedCategory(catParam);

    const fabricParam = searchParams.get('fabric');
    if (fabricParam) setSelectedFabric(fabricParam);
  }, [searchParams]);

  // Load products when filters/sort/page changes
  useEffect(() => {
    const loadProducts = () => {
      const searchParam = searchParams.get('search') || '';
      
      // Map Category Name to ID
      let categoryId = null;
      if (selectedCategory !== 'All') {
        const cat = categories.find(c => c.name === selectedCategory);
        if (cat) categoryId = cat.id;
      }

      // Map Price Range
      let minPrice = null;
      let maxPrice = null;
      if (priceRange === 'under-3000') {
        maxPrice = 3000;
      } else if (priceRange === '3000-8000') {
        minPrice = 3000;
        maxPrice = 8000;
      } else if (priceRange === 'above-8000') {
        minPrice = 8000;
      }

      const fabric = selectedFabric === 'All' ? null : selectedFabric;

      // If there is a search query, run full-text search
      if (searchParam) {
        dispatch(searchProducts({ query: searchParam, params: { page, size: 12 } }));
      } else {
        // Run filtering
        dispatch(filterProducts({
          categoryId,
          minPrice,
          maxPrice,
          fabric,
          page,
          size: 12,
          sortBy,
          sortDir
        }));
      }
    };

    if (categories.length > 0 || selectedCategory === 'All') {
      loadProducts();
    }
  }, [selectedCategory, selectedFabric, priceRange, sortBy, sortDir, page, categories, searchParams, dispatch]);

  const handlePageChange = (newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const clearFilters = () => {
    setSelectedCategory('All');
    setSelectedFabric('All');
    setPriceRange('All');
    setSearchParams({});
    setPage(0);
  };

  const handleAddSuccess = (product, isBuyNow) => {
    if (isBuyNow) {
      dispatch(closeAddedModal());
      dispatch(setCartOpen(true));
    }
  };

  return (
    <div className="min-h-screen bg-[#FAF8F5] py-10 font-sans">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Elegant Page Header Banner */}
        <div className="text-center max-w-2xl mx-auto mb-12 space-y-3 mt-4">
          <span className="text-[10px] uppercase font-bold tracking-[0.25em] text-[#C9A227]">The SareeKart Edit</span>
          <h1 className="font-serif text-3xl md:text-4xl text-[#3A1028] font-bold">Premium Sarees</h1>
          <div className="w-16 h-px bg-[#C9A227] mx-auto my-3" />
          <p className="text-xs text-text-secondary leading-relaxed max-w-lg mx-auto font-light">
            Explore our curated collection of luxury handwoven sarees. Handpicked heritage drapes from the finest artisans across India.
          </p>
        </div>

        {/* Search status header */}
        {searchParams.get('search') && (
          <div className="mb-6 bg-white p-4 rounded-xl border border-[#F4F4F4] shadow-xs flex justify-between items-center">
            <p className="text-sm text-text-secondary">
              Showing search results for "<span className="font-bold text-[#3A1028]">{searchParams.get('search')}</span>"
            </p>
            <button 
              onClick={() => setSearchParams({})} 
              className="text-xs font-bold text-[#3A1028] hover:text-[#C9A227] hover:underline uppercase tracking-wider"
            >
              Clear Search
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* ── SIDEBAR FILTERS ── */}
          <aside className="col-span-1 lg:col-span-3 space-y-6">
            <div className="bg-white rounded-2xl border border-[#F4F4F4] p-6 space-y-6 shadow-soft">
              <div className="flex items-center justify-between pb-3 border-b border-[#F4F4F4]">
                <h2 className="font-bold text-[#3A1028] text-md flex items-center gap-2">
                  <SlidersHorizontal className="w-4 h-4 text-[#C9A227]" /> Filters
                </h2>
                <button 
                  onClick={clearFilters}
                  className="text-xs font-semibold text-text-muted hover:text-[#3A1028] flex items-center gap-1 cursor-pointer transition-colors"
                >
                  <RefreshCw className="w-3 h-3" /> Reset
                </button>
              </div>

              {/* Category Filter */}
              <div className="space-y-3">
                <h3 className="text-[11px] font-bold uppercase tracking-widest text-[#C9A227]">Fabric Category</h3>
                <div className="space-y-2">
                  <label className="flex items-center gap-2.5 text-sm cursor-pointer select-none group">
                    <input 
                      type="radio" 
                      name="category"
                      checked={selectedCategory === 'All'}
                      onChange={() => { setSelectedCategory('All'); setPage(0); }}
                      className="sr-only"
                    />
                    <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                      selectedCategory === 'All' ? 'border-[#C9A227] bg-[#3A1028]/5' : 'border-gray-300 group-hover:border-[#C9A227]'
                    }`}>
                      {selectedCategory === 'All' && <div className="w-2 h-2 rounded-full bg-[#C9A227]" />}
                    </div>
                    <span className={selectedCategory === 'All' ? 'font-semibold text-[#3A1028]' : 'text-text-secondary group-hover:text-[#3A1028] transition-colors'}>All Sarees</span>
                  </label>
                  {categories.map((cat) => (
                    <label key={cat.id} className="flex items-center gap-2.5 text-sm cursor-pointer select-none group">
                      <input 
                        type="radio" 
                        name="category"
                        checked={selectedCategory === cat.name}
                        onChange={() => { setSelectedCategory(cat.name); setPage(0); }}
                        className="sr-only"
                      />
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                        selectedCategory === cat.name ? 'border-[#C9A227] bg-[#3A1028]/5' : 'border-gray-300 group-hover:border-[#C9A227]'
                      }`}>
                        {selectedCategory === cat.name && <div className="w-2 h-2 rounded-full bg-[#C9A227]" />}
                      </div>
                      <span className={selectedCategory === cat.name ? 'font-semibold text-[#3A1028]' : 'text-text-secondary group-hover:text-[#3A1028] transition-colors'}>{cat.name}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Ornamental Gold Divider */}
              <div className="border-t border-[#C9A227]/20 my-4" />

              {/* Price Filter */}
              <div className="space-y-3">
                <h3 className="text-[11px] font-bold uppercase tracking-widest text-[#C9A227]">Price Range</h3>
                <div className="space-y-2">
                  {[
                    { label: "All Prices", value: "All" },
                    { label: "Under ₹3,000", value: "under-3000" },
                    { label: "₹3,000 - ₹8,000", value: "3000-8000" },
                    { label: "Above ₹8,000", value: "above-8000" }
                  ].map((option) => (
                    <label key={option.value} className="flex items-center gap-2.5 text-sm cursor-pointer select-none group">
                      <input 
                        type="radio" 
                        name="price"
                        checked={priceRange === option.value}
                        onChange={() => { setPriceRange(option.value); setPage(0); }}
                        className="sr-only"
                      />
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                        priceRange === option.value ? 'border-[#C9A227] bg-[#3A1028]/5' : 'border-gray-300 group-hover:border-[#C9A227]'
                      }`}>
                        {priceRange === option.value && <div className="w-2 h-2 rounded-full bg-[#C9A227]" />}
                      </div>
                      <span className={priceRange === option.value ? 'font-semibold text-[#3A1028]' : 'text-text-secondary group-hover:text-[#3A1028] transition-colors'}>{option.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Ornamental Gold Divider */}
              <div className="border-t border-[#C9A227]/20 my-4" />

              {/* Fabric Material Filter */}
              <div className="space-y-3">
                <h3 className="text-[11px] font-bold uppercase tracking-widest text-[#C9A227]">Fabric Material</h3>
                <div className="space-y-2">
                  <label className="flex items-center gap-2.5 text-sm cursor-pointer select-none group">
                    <input 
                      type="radio" 
                      name="fabric"
                      checked={selectedFabric === 'All'}
                      onChange={() => { setSelectedFabric('All'); setPage(0); }}
                      className="sr-only"
                    />
                    <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                      selectedFabric === 'All' ? 'border-[#C9A227] bg-[#3A1028]/5' : 'border-gray-300 group-hover:border-[#C9A227]'
                    }`}>
                      {selectedFabric === 'All' && <div className="w-2 h-2 rounded-full bg-[#C9A227]" />}
                    </div>
                    <span className={selectedFabric === 'All' ? 'font-semibold text-[#3A1028]' : 'text-text-secondary group-hover:text-[#3A1028] transition-colors'}>All Fabrics</span>
                  </label>
                  {FABRICS.map((fabric) => (
                    <label key={fabric} className="flex items-center gap-2.5 text-sm cursor-pointer select-none group">
                      <input 
                        type="radio" 
                        name="fabric"
                        checked={selectedFabric === fabric}
                        onChange={() => { setSelectedFabric(fabric); setPage(0); }}
                        className="sr-only"
                      />
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                        selectedFabric === fabric ? 'border-[#C9A227] bg-[#3A1028]/5' : 'border-gray-300 group-hover:border-[#C9A227]'
                      }`}>
                        {selectedFabric === fabric && <div className="w-2 h-2 rounded-full bg-[#C9A227]" />}
                      </div>
                      <span className={selectedFabric === fabric ? 'font-semibold text-[#3A1028]' : 'text-text-secondary group-hover:text-[#3A1028] transition-colors'}>{fabric}</span>
                    </label>
                  ))}
                </div>
              </div>

            </div>
          </aside>
          
          {/* ── PRODUCTS COLUMN ── */}
          <section className="col-span-1 lg:col-span-9 space-y-6">
          
          {/* Controls Bar */}
          <div className="bg-white p-4 rounded-2xl border border-[#F4F4F4] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 shadow-soft">
            <p className="text-sm text-text-secondary font-medium">
              Showing <span className="font-bold text-[#3A1028]">{pagination.totalElements}</span> premium sarees
            </p>
            
            {/* Sorting */}
            <div className="flex items-center gap-2">
              <ArrowUpDown className="w-4 h-4 text-text-muted" />
              <select 
                value={`${sortBy}-${sortDir}`}
                onChange={(e) => {
                  const [field, direction] = e.target.value.split('-');
                  setSortBy(field);
                  setSortDir(direction);
                  setPage(0);
                }}
                className="bg-[#FAF8F5] border border-[#F4F4F4] rounded-xl px-3 py-2 text-sm outline-none font-semibold text-text-secondary focus:border-[#C9A227] cursor-pointer shadow-xs"
              >
                <option value="createdAt-desc">New Arrivals</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="name-asc">Alphabetical: A-Z</option>
              </select>
            </div>
          </div>

          {/* Grid display */}
          {loading ? (
            <div className="py-24">
              <LoadingSpinner message="Curating premium collection..." />
            </div>
          ) : error ? (
            <div className="bg-red-50 border border-red-200 text-red-800 p-4 rounded-xl text-center font-medium">
              {error}
            </div>
          ) : products.length === 0 ? (
            <div className="bg-white border border-[#F4F4F4] rounded-2xl p-16 text-center space-y-4 shadow-soft">
              <div className="w-12 h-12 rounded-full bg-cream mx-auto flex items-center justify-center text-[#C9A227]">
                <Search className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-text-primary">No sarees matched your filters</h3>
              <p className="text-sm text-text-secondary max-w-sm mx-auto">Try resetting filters or adjusting price limits to explore more drapes.</p>
              <button 
                onClick={clearFilters}
                className="px-6 py-2.5 border border-[#3A1028] text-[#3A1028] hover:bg-[#3A1028] hover:text-white text-xs font-bold rounded-xl uppercase tracking-widest transition-all cursor-pointer"
              >
                Reset Filters
              </button>
            </div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4 }}
              className="space-y-8"
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {products.map((p) => (
                  <ProductCard 
                    key={p.id} 
                    product={p} 
                    initialWishlisted={wishlistProductIds.includes(p.id)}
                    onAddSuccess={handleAddSuccess} 
                  />
                ))}
              </div>

              {/* Pagination controls */}
              <Pagination 
                page={page}
                totalPages={pagination.totalPages}
                onPageChange={handlePageChange}
              />
            </motion.div>
          )}

        </section>

      </div>
    </div>
  </div>
  );
}
