import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ArrowRight, ArrowUpDown, Check, RefreshCw, Search, SlidersHorizontal, X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import api from '../../api/axiosConfig';
import categoryService from '../../services/categoryService';
import lookupService from '../../services/lookupService';
import ProductCard from '../../components/ProductCard';
import Pagination from '../../components/common/Pagination';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl, truncateDescription } from '../../utils/seoUtils';
import { closeAddedModal, setCartOpen } from '../../redux/slices/cartSlice';
import { fetchProducts } from '../../redux/slices/productSlice';
import MobileFilterDrawer from '../../components/product/MobileFilterDrawer';
import eventTracker from '../../utils/eventTracker';

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

const fallbackCategories = [
  { id: 1, name: 'Silk Sarees', slug: 'silk-sarees' },
  { id: 2, name: 'Cotton Sarees', slug: 'cotton-sarees' },
  { id: 3, name: 'Chiffon Sarees', slug: 'chiffon-sarees' },
  { id: 4, name: 'Georgette Sarees', slug: 'georgette-sarees' },
  { id: 5, name: 'Banarasi Sarees', slug: 'banarasi-sarees' },
  { id: 6, name: 'Kanchipuram Sarees', slug: 'kanchipuram-sarees' },
  { id: 7, name: 'Designer Sarees', slug: 'designer-sarees' },
  { id: 8, name: 'Bridal Sarees', slug: 'bridal-sarees' },
];

const priceOptions = [
  { label: 'Any price', value: 'All' },
  { label: 'Under Rs. 3,000', value: 'under-3000', maxPrice: 3000 },
  { label: 'Rs. 3,000 to Rs. 8,000', value: '3000-8000', minPrice: 3000, maxPrice: 8000 },
  { label: 'Above Rs. 8,000', value: 'above-8000', minPrice: 8000 },
];

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const dispatch = useDispatch();

  const { user } = useSelector((state) => state.auth);
  const { products, loading, error, pagination } = useSelector((state) => state.products);

  // Dynamic lookups state
  const [categories, setCategories] = useState(fallbackCategories);
  const [fabrics, setFabrics] = useState([]);
  const [occasions, setOccasions] = useState([]);
  const [colors, setColors] = useState([]);
  const [wishlistProductIds, setWishlistProductIds] = useState([]);
  const [filtersOpen, setFiltersOpen] = useState(false);

  // Derived filter state directly from URL searchParams (URL is Single Source of Truth)
  const searchQuery = searchParams.get('q') || searchParams.get('search') || '';
  const selectedCategory = searchParams.get('category') || 'All';
  const selectedFabric = searchParams.get('fabric') || 'All';
  const selectedOccasion = searchParams.get('occasion') || 'All';
  const selectedColorFamily = searchParams.get('colorFamily') || 'All';
  const priceRange = searchParams.get('price') || 'All';
  const inStockOnly = searchParams.get('inStock') === 'true';
  const sortBy = searchParams.get('sortBy') || 'createdAt';
  const sortDir = searchParams.get('sortDir') || 'desc';
  const page = parseInt(searchParams.get('page') || '0', 10);

  // 1. Fetch dynamic taxonomy and attribute lookups from API
  useEffect(() => {
    const loadLookups = async () => {
      try {
        const [catRes, fabRes, occRes, colRes] = await Promise.allSettled([
          categoryService.getCategories(),
          lookupService.getFabrics(),
          lookupService.getOccasions(),
          lookupService.getColors(),
        ]);

        if (catRes.status === 'fulfilled' && catRes.value?.data?.length) {
          setCategories(catRes.value.data);
        }
        if (fabRes.status === 'fulfilled' && fabRes.value?.data?.length) {
          setFabrics(fabRes.value.data);
        }
        if (occRes.status === 'fulfilled' && occRes.value?.data?.length) {
          setOccasions(occRes.value.data);
        }
        if (colRes.status === 'fulfilled' && colRes.value?.data?.length) {
          setColors(colRes.value.data);
        }
      } catch (err) {
        console.error('Failed to load dynamic lookups', err);
      }
    };
    loadLookups();
  }, []);

  // Compute distinct canonical color families from database colors (Safeguard 3)
  const colorFamilies = useMemo(() => {
    if (!colors.length) return ['Red', 'Pink', 'White', 'Green', 'Blue', 'Yellow', 'Gold', 'Neutral', 'Metallic', 'Orange', 'Black'];
    const families = colors.map((c) => c.family).filter(Boolean);
    return [...new Set(families)];
  }, [colors]);

  // Wishlist fetch
  useEffect(() => {
    const fetchWishlist = async () => {
      if (!user) return;
      try {
        const response = await api.get('/wishlist');
        const wishlist = response.data?.data || response.data?.products || [];
        setWishlistProductIds(wishlist.map((product) => product.id));
      } catch (err) {
        console.error('Failed to fetch wishlist', err);
      }
    };
    fetchWishlist();
  }, [user]);

  // 2. Query dispatch on any URL change
  useEffect(() => {
    const selectedPrice = priceOptions.find((option) => option.value === priceRange) || priceOptions[0];

    const criteria = {
      page,
      size: 12,
      sortBy,
      sortDir,
    };

    if (searchQuery.trim()) {
      criteria.q = searchQuery.trim();
    }

    if (selectedCategory !== 'All') {
      const match = categories.find((c) => c.slug === selectedCategory || c.name === selectedCategory);
      criteria.category = match?.slug || selectedCategory;
    }

    if (selectedFabric !== 'All') {
      const match = fabrics.find((f) => f.slug === selectedFabric || f.name === selectedFabric);
      criteria.fabric = match?.slug || selectedFabric;
    }

    if (selectedOccasion !== 'All') {
      const match = occasions.find((o) => o.slug === selectedOccasion || o.name === selectedOccasion);
      criteria.occasion = match?.slug || selectedOccasion;
    }

    if (selectedColorFamily !== 'All') {
      criteria.colorFamily = selectedColorFamily;
    }

    if (selectedPrice.minPrice) {
      criteria.minPrice = selectedPrice.minPrice;
    }
    if (selectedPrice.maxPrice) {
      criteria.maxPrice = selectedPrice.maxPrice;
    }

    if (inStockOnly) {
      criteria.inStock = true;
    }

    dispatch(fetchProducts(criteria));
  }, [categories, dispatch, fabrics, inStockOnly, occasions, page, priceRange, searchQuery, selectedCategory, selectedColorFamily, selectedFabric, selectedOccasion, sortBy, sortDir]);

  // Telemetry: track SEARCH_QUERY and CATEGORY_VIEW once fetch completes
  const lastTrackedQueryRef = useRef('');
  const lastTrackedCategoryRef = useRef('');

  useEffect(() => {
    if (loading) return;

    if (searchQuery.trim() && searchQuery.trim() !== lastTrackedQueryRef.current) {
      lastTrackedQueryRef.current = searchQuery.trim();
      eventTracker.trackSearch(searchQuery.trim(), pagination?.totalElements ?? products.length, {
        category: selectedCategory,
        fabric: selectedFabric,
        occasion: selectedOccasion,
      });
    }

    if (selectedCategory && selectedCategory !== 'All' && selectedCategory !== lastTrackedCategoryRef.current) {
      lastTrackedCategoryRef.current = selectedCategory;
      const catObj = categories.find((c) => c.slug === selectedCategory || c.name === selectedCategory);
      eventTracker.trackCategoryView(catObj?.id || null, catObj?.name || selectedCategory, {
        slug: selectedCategory,
        resultsCount: pagination?.totalElements ?? products.length,
      });
    }
  }, [loading, searchQuery, selectedCategory, pagination?.totalElements, products.length, categories, selectedFabric, selectedOccasion]);

  // Helper to update a URL search param
  const updateParam = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (!value || value === 'All' || value === false || value === '') {
      next.delete(key);
      if (key === 'q') next.delete('search');
    } else {
      next.set(key, value);
      if (key === 'q') next.delete('search');
    }
    next.delete('page'); // Reset to page 0 on any filter change
    setSearchParams(next);
  };

  const clearFilters = () => {
    setSearchParams({});
  };

  const handlePageChange = (nextPage) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage === 0) {
      next.delete('page');
    } else {
      next.set('page', String(nextPage));
    }
    setSearchParams(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleAddSuccess = (product, isBuyNow) => {
    if (isBuyNow) {
      dispatch(closeAddedModal());
      dispatch(setCartOpen(true));
    }
  };

  const handleWishlistToggle = (productId, isAdded) => {
    setWishlistProductIds((prev) => (
      isAdded ? [...prev, productId] : prev.filter((id) => id !== productId)
    ));
  };

  const hasActiveFilters = useMemo(() => (
    selectedCategory !== 'All' ||
    selectedFabric !== 'All' ||
    selectedOccasion !== 'All' ||
    selectedColorFamily !== 'All' ||
    priceRange !== 'All' ||
    inStockOnly ||
    Boolean(searchQuery)
  ), [inStockOnly, priceRange, searchQuery, selectedCategory, selectedColorFamily, selectedFabric, selectedOccasion]);

  const activeChips = [
    searchQuery && { label: `"${searchQuery}"`, onRemove: () => updateParam('q', '') },
    selectedCategory !== 'All' && {
      label: categories.find((c) => c.slug === selectedCategory || c.name === selectedCategory)?.name || selectedCategory,
      onRemove: () => updateParam('category', 'All'),
    },
    selectedFabric !== 'All' && {
      label: fabrics.find((f) => f.slug === selectedFabric || f.name === selectedFabric)?.name || selectedFabric,
      onRemove: () => updateParam('fabric', 'All'),
    },
    selectedOccasion !== 'All' && {
      label: occasions.find((o) => o.slug === selectedOccasion || o.name === selectedOccasion)?.name || selectedOccasion,
      onRemove: () => updateParam('occasion', 'All'),
    },
    selectedColorFamily !== 'All' && {
      label: `Color: ${selectedColorFamily}`,
      onRemove: () => updateParam('colorFamily', 'All'),
    },
    priceRange !== 'All' && {
      label: priceOptions.find((option) => option.value === priceRange)?.label,
      onRemove: () => updateParam('price', 'All'),
    },
    inStockOnly && {
      label: 'In-Stock Only',
      onRemove: () => updateParam('inStock', false),
    },
  ].filter(Boolean);

  const selectedCatObj = categories.find((c) => c.slug === selectedCategory || c.name === selectedCategory);
  const isCategorySelected = selectedCategory && selectedCategory !== 'All';
  const hasFacetedFilters = Boolean(
    searchQuery ||
    (selectedFabric && selectedFabric !== 'All') ||
    (selectedOccasion && selectedOccasion !== 'All') ||
    (selectedColorFamily && selectedColorFamily !== 'All') ||
    (priceRange && priceRange !== 'All') ||
    inStockOnly ||
    page > 0 ||
    sortBy !== 'createdAt'
  );

  const canonicalUrl = isCategorySelected
    ? getCanonicalUrl('/products', { category: selectedCatObj?.slug || selectedCategory }, ['category'])
    : getCanonicalUrl('/products');

  const seoTitle = isCategorySelected
    ? `${selectedCatObj?.name || selectedCategory} Sarees | Pure Silk & Handloom | SareeKart`
    : (hasFacetedFilters ? 'Curated Handloom Sarees Collection | SareeKart' : 'Luxury Indian Handloom Sarees Catalog | SareeKart');

  const seoDescription = isCategorySelected
    ? truncateDescription(selectedCatObj?.description || `Explore authentic handwoven ${selectedCatObj?.name || selectedCategory} sarees directly from master artisan clusters at SareeKart.`)
    : 'Browse SareeKart\'s authentic collection of certified handwoven sarees: Banarasi, Kanchipuram, Chanderi, and Tussar silk directly from master weavers.';

  const categoryBreadcrumbSchema = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    "itemListElement": [
      {
        "@type": "ListItem",
        "position": 1,
        "name": "Home",
        "item": getCanonicalUrl('/')
      },
      {
        "@type": "ListItem",
        "position": 2,
        "name": "Catalog",
        "item": getCanonicalUrl('/products')
      },
      ...(isCategorySelected ? [{
        "@type": "ListItem",
        "position": 3,
        "name": selectedCatObj?.name || selectedCategory,
        "item": canonicalUrl
      }] : [])
    ]
  };

  return (
    <div className="min-h-screen bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title={seoTitle}
        description={seoDescription}
        canonical={canonicalUrl}
        noindex={hasFacetedFilters}
        schemaData={categoryBreadcrumbSchema}
      />

      <section className="bg-white">
        <div className="section-shell grid gap-8 py-10 lg:grid-cols-[1fr_380px] lg:items-end">
          <div>
            <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#B84F49]">The edit</p>
            <h1 className="mt-2 max-w-3xl text-4xl font-bold leading-tight sm:text-6xl">
              Handloom, with a point of view.
            </h1>
            <p className="mt-4 max-w-2xl text-base font-medium leading-8 text-[#71817A]">
              Find the right drape by category, fabric, occasion, color mood, and budget.
            </p>
          </div>
          <div className="rounded-[8px] bg-[#F3E6C7] p-5">
            <p className="text-sm font-black text-[#17211F]">Start with a story</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {[
                { label: 'Wedding silk', q: 'wedding silk' },
                { label: 'Red Banarasi', q: 'red banarasi' },
                { label: 'Bridal Kanchipuram', q: 'bridal kanchipuram' },
                { label: 'Office cotton', q: 'cotton' },
              ].map((item) => (
                <Link
                  key={item.label}
                  to={`/products?q=${encodeURIComponent(item.q)}`}
                  className="rounded-full bg-white px-3 py-2 text-xs font-black text-[#1E6A62] shadow-xs hover:bg-[#EDE7DC] transition"
                >
                  {item.label}
                </Link>
              ))}
            </div>
          </div>
        </div>
      </section>

      <div className="section-shell py-8 sm:py-10">
        <section className="rounded-[8px] border border-[#DDD8CF] bg-white p-4 shadow-xs">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            {/* Category horizontal pills */}
            <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
              <button
                onClick={() => updateParam('category', 'All')}
                className={`h-10 shrink-0 rounded-full px-4 text-sm font-black transition ${
                  selectedCategory === 'All'
                    ? 'bg-[#17211F] text-white'
                    : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                }`}
              >
                All sarees
              </button>
              {categories.map((category) => {
                const identifier = category.slug || category.name;
                const active = selectedCategory === identifier || selectedCategory === category.name;
                return (
                  <button
                    key={category.id || category.name}
                    onClick={() => updateParam('category', identifier)}
                    className={`h-10 shrink-0 rounded-full px-4 text-sm font-black transition ${
                      active
                        ? 'bg-[#17211F] text-white'
                        : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                    }`}
                  >
                    {category.name}
                  </button>
                );
              })}
            </div>

            {/* Filter Toggle & Sort selector */}
            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                onClick={() => setFiltersOpen((open) => !open)}
                className={`h-11 rounded-full px-4 text-sm font-black transition inline-flex items-center justify-center gap-2 ${
                  filtersOpen ? 'bg-[#1E6A62] text-white' : 'bg-[#F7F4EE] text-[#17211F]'
                }`}
                aria-expanded={filtersOpen}
              >
                <SlidersHorizontal className="h-4 w-4" />
                Filters
                {hasActiveFilters && (
                  <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-[#B84F49] px-1 text-[10px] font-black text-white">
                    {activeChips.length}
                  </span>
                )}
              </button>

              <label className="flex h-11 items-center gap-2 rounded-full bg-[#F7F4EE] px-4 text-sm font-black text-[#17211F]">
                <ArrowUpDown className="h-4 w-4 text-[#1E6A62]" />
                <span className="sr-only">Sort products</span>
                <select
                  value={`${sortBy}-${sortDir}`}
                  onChange={(event) => {
                    const [field, direction] = event.target.value.split('-');
                    const next = new URLSearchParams(searchParams);
                    next.set('sortBy', field);
                    next.set('sortDir', direction);
                    next.delete('page');
                    setSearchParams(next);
                  }}
                  className="bg-transparent outline-none cursor-pointer"
                >
                  <option value="createdAt-desc">Newest Arrivals</option>
                  <option value="price-asc">Price: Low to High</option>
                  <option value="price-desc">Price: High to Low</option>
                  <option value="name-asc">Name: A to Z</option>
                </select>
              </label>
            </div>
          </div>

          {/* Desktop Expandable Filter Panel */}
          <AnimatePresence>
            {filtersOpen && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="hidden md:block overflow-hidden"
              >
                <div className="mt-5 grid gap-5 border-t border-[#DDD8CF] pt-5 md:grid-cols-4">
                  {/* Fabric Column */}
                  <div>
                    <h2 className="mb-3 text-[12px] font-black uppercase tracking-[0.16em] text-[#1E6A62]">Fabric</h2>
                    <div className="grid gap-1.5 max-h-56 overflow-y-auto pr-1">
                      <button
                        onClick={() => updateParam('fabric', 'All')}
                        className={`rounded-[8px] px-3 py-2 text-left text-xs font-black transition ${
                          selectedFabric === 'All' ? 'bg-[#E3F0ED] text-[#1E6A62]' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        All fabrics
                      </button>
                      {(fabrics.length ? fabrics : [{ id: 1, name: 'Silk', slug: 'silk' }, { id: 2, name: 'Cotton', slug: 'cotton' }]).map((f) => {
                        const identifier = f.slug || f.name;
                        const active = selectedFabric === identifier || selectedFabric === f.name;
                        return (
                          <button
                            key={f.id || f.name}
                            onClick={() => updateParam('fabric', identifier)}
                            className={`rounded-[8px] px-3 py-2 text-left text-xs font-black transition ${
                              active ? 'bg-[#E3F0ED] text-[#1E6A62]' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                            }`}
                          >
                            {f.name}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Occasion Column */}
                  <div>
                    <h2 className="mb-3 text-[12px] font-black uppercase tracking-[0.16em] text-[#1E6A62]">Occasion</h2>
                    <div className="grid gap-1.5 max-h-56 overflow-y-auto pr-1">
                      <button
                        onClick={() => updateParam('occasion', 'All')}
                        className={`rounded-[8px] px-3 py-2 text-left text-xs font-black transition ${
                          selectedOccasion === 'All' ? 'bg-[#E3F0ED] text-[#1E6A62]' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        All occasions
                      </button>
                      {occasions.map((o) => {
                        const identifier = o.slug || o.name;
                        const active = selectedOccasion === identifier || selectedOccasion === o.name;
                        return (
                          <button
                            key={o.id || o.name}
                            onClick={() => updateParam('occasion', identifier)}
                            className={`rounded-[8px] px-3 py-2 text-left text-xs font-black transition ${
                              active ? 'bg-[#E3F0ED] text-[#1E6A62]' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                            }`}
                          >
                            {o.name}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Color Mood Column (Safeguard 3: DB canonical color families) */}
                  <div>
                    <h2 className="mb-3 text-[12px] font-black uppercase tracking-[0.16em] text-[#1E6A62]">Color Mood</h2>
                    <div className="flex flex-wrap gap-1.5 max-h-56 overflow-y-auto pr-1">
                      <button
                        onClick={() => updateParam('colorFamily', 'All')}
                        className={`rounded-full px-3 py-1.5 text-xs font-black transition ${
                          selectedColorFamily === 'All' ? 'bg-[#17211F] text-white' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                        }`}
                      >
                        All colors
                      </button>
                      {colorFamilies.map((family) => {
                        const active = selectedColorFamily === family;
                        const hex = COLOR_FAMILY_HEX[family] || '#CCC';
                        return (
                          <button
                            key={family}
                            onClick={() => updateParam('colorFamily', family)}
                            className={`rounded-full px-3 py-1.5 text-xs font-black transition inline-flex items-center gap-1.5 ${
                              active ? 'bg-[#17211F] text-white shadow-xs' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                            }`}
                          >
                            <span
                              className="h-2.5 w-2.5 rounded-full border border-black/20"
                              style={{ backgroundColor: hex }}
                            />
                            {family}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Price & Availability Column */}
                  <div>
                    <h2 className="mb-3 text-[12px] font-black uppercase tracking-[0.16em] text-[#1E6A62]">Price & Stock</h2>
                    <div className="grid gap-1.5">
                      {priceOptions.map((option) => (
                        <button
                          key={option.value}
                          onClick={() => updateParam('price', option.value)}
                          className={`rounded-[8px] px-3 py-2 text-left text-xs font-black transition ${
                            priceRange === option.value ? 'bg-[#F3E6C7] text-[#9B6A27]' : 'bg-[#F7F4EE] text-[#42504C] hover:bg-[#EDE7DC]'
                          }`}
                        >
                          {option.label}
                        </button>
                      ))}

                      {/* In-Stock Toggle */}
                      <label className="mt-2 flex items-center gap-2 rounded-[8px] bg-[#F7F4EE] p-2.5 cursor-pointer hover:bg-[#EDE7DC] transition">
                        <input
                          type="checkbox"
                          checked={inStockOnly}
                          onChange={(e) => updateParam('inStock', e.target.checked)}
                          className="h-4 w-4 rounded accent-[#1E6A62]"
                        />
                        <span className="text-xs font-bold text-[#17211F]">In-Stock Only</span>
                      </label>
                    </div>

                    <div className="mt-4 rounded-[8px] bg-[#17211F] p-3 text-white flex items-center justify-between">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-[0.16em] text-[#F3C56A]">Matches</p>
                        <p className="text-xl font-black">{pagination.totalElements || products.length}</p>
                      </div>
                      <button onClick={clearFilters} className="inline-flex items-center gap-1.5 rounded-full bg-white px-3 py-1.5 text-xs font-black text-[#17211F] hover:bg-[#EDE7DC] transition">
                        <RefreshCw className="h-3.5 w-3.5" />
                        Reset
                      </button>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </section>

        {/* Active Filter Chips */}
        {hasActiveFilters && (
          <div className="mt-5 flex flex-wrap items-center gap-2">
            {activeChips.map((chip) => (
              <span key={chip.label} className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-2 text-sm font-black text-[#17211F] shadow-xs">
                {chip.label}
                <button onClick={chip.onRemove} aria-label={`Remove ${chip.label}`}>
                  <X className="h-4 w-4 text-[#71817A] hover:text-[#B84F49]" />
                </button>
              </span>
            ))}
            <button onClick={clearFilters} className="text-sm font-black text-[#B84F49] hover:underline">
              Clear all
            </button>
          </div>
        )}

        {/* Product Grid & States */}
        <main className="mt-8">
          {loading ? (
            <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-12 text-center font-black text-[#71817A]">
              Loading the edit...
            </div>
          ) : error ? (
            <div className="rounded-[8px] border border-red-200 bg-red-50 p-6 text-center font-bold text-red-800">
              {error}
            </div>
          ) : products.length === 0 ? (
            <div className="mx-auto max-w-lg rounded-[8px] border border-[#DDD8CF] bg-white p-10 text-center shadow-xs">
              <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#F7F4EE] text-[#71817A]">
                <Search className="h-6 w-6" />
              </div>
              <h3 className="mt-5 text-2xl font-bold text-[#17211F]">Nothing matched this edit</h3>
              <p className="mt-2 text-sm font-medium leading-7 text-[#71817A]">
                Your filters are a little specific. Try a broader search or another fabric.
              </p>
              <button onClick={clearFilters} className="sk-button-primary mt-6">
                Reset filters
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.35 }}
              className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 xl:gap-6"
            >
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  initialWishlisted={wishlistProductIds.includes(product.id)}
                  onAddSuccess={handleAddSuccess}
                  onWishlistToggle={handleWishlistToggle}
                />
              ))}
            </motion.div>
          )}

          {products.length > 0 && pagination.totalPages > 1 && (
            <div className="mt-10 flex justify-center border-t border-[#DDD8CF] pt-8">
              <Pagination
                page={page}
                totalPages={pagination.totalPages}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </main>
      </div>

      {/* Mobile Bottom-Sheet Filter Drawer */}
      <MobileFilterDrawer
        isOpen={filtersOpen}
        onClose={() => setFiltersOpen(false)}
        categories={categories}
        selectedCategory={selectedCategory}
        onSelectCategory={(catName) => {
          updateParam('category', catName);
        }}
        fabrics={fabrics}
        selectedFabric={selectedFabric}
        onSelectFabric={(fabric) => {
          updateParam('fabric', fabric);
        }}
        occasions={occasions}
        selectedOccasion={selectedOccasion}
        onSelectOccasion={(occ) => {
          updateParam('occasion', occ);
        }}
        colorFamilies={colorFamilies}
        selectedColorFamily={selectedColorFamily}
        onSelectColorFamily={(family) => {
          updateParam('colorFamily', family);
        }}
        priceOptions={priceOptions}
        priceRange={priceRange}
        onSelectPriceRange={(val) => {
          updateParam('price', val);
        }}
        inStockOnly={inStockOnly}
        onToggleInStock={(val) => {
          updateParam('inStock', val);
        }}
        sortBy={sortBy}
        sortDir={sortDir}
        onSelectSort={(field, dir) => {
          const next = new URLSearchParams(searchParams);
          next.set('sortBy', field);
          next.set('sortDir', dir);
          next.delete('page');
          setSearchParams(next);
        }}
        onClearAll={clearFilters}
        totalCount={pagination?.totalElements || products.length}
      />
    </div>
  );
}
