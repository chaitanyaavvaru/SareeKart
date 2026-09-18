import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ArrowRight, Check, ChevronRight, Search, ShieldCheck, Sparkles, Truck } from 'lucide-react';
import { motion } from 'framer-motion';
import { closeAddedModal, setCartOpen } from '../../redux/slices/cartSlice';
import api from '../../api/axiosConfig';
import productService from '../../services/productService';
import ProductGrid from '../../components/ProductGrid';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl, toAbsoluteImageUrl } from '../../utils/seoUtils';
import { HOMEPAGE_PRODUCTS } from '../../data/products';
import eventTracker from '../../utils/eventTracker';

const imagePool = HOMEPAGE_PRODUCTS.map((product) => product.image);

const heroStories = [
  {
    eyebrow: 'The new season edit',
    title: 'Sarees with a point of view.',
    detail: 'From luminous Kanchipuram silks to easy everyday cottons, discover handlooms chosen for the way you dress now.',
    image: imagePool[1],
    link: '/products',
    linkLabel: 'Shop all sarees',
  },
  {
    eyebrow: 'For the days worth dressing for',
    title: 'The ceremony edit.',
    detail: 'Rich borders, soft shine, and color stories that hold their own from first look to last dance.',
    image: imagePool[8],
    link: '/products?search=bridal',
    linkLabel: 'Shop wedding sarees',
  },
];

const collectionTiles = [
  { title: 'Kanchipuram', kicker: 'Heirloom silk', query: 'Kanchipuram', image: imagePool[1] },
  { title: 'Banarasi', kicker: 'Woven shine', query: 'Banarasi', image: imagePool[0] },
  { title: 'Cotton', kicker: 'Everyday ease', query: 'Cotton', image: imagePool[2] },
  { title: 'Ikat', kicker: 'Graphic handloom', query: 'Ikat', image: imagePool[3] },
  { title: 'Organza', kicker: 'Light-catching', query: 'Organza', image: imagePool[4] },
  { title: 'Linen & tussar', kicker: 'Quiet texture', query: 'Linen', image: imagePool[11] },
];

const shopCategories = [
  { title: 'Wedding', detail: 'Silks with presence', query: 'Bridal', image: imagePool[8] },
  { title: 'Under Rs. 10,000', detail: 'Beautiful everyday finds', query: 'Cotton', image: imagePool[5] },
  { title: 'Fresh from the loom', detail: 'New arrivals', query: '', image: imagePool[6] },
  { title: 'Blends', detail: 'Easy, polished drapes', query: 'Gadwal', image: imagePool[7] },
];

const occasionTiles = [
  { title: 'Wedding', query: 'Bridal', image: imagePool[9] },
  { title: 'Festive', query: 'Festive', image: imagePool[7] },
  { title: 'Casual', query: 'Casual', image: imagePool[5] },
  { title: 'Formal', query: 'Mysore', image: imagePool[6] },
  { title: 'Party', query: 'Organza', image: imagePool[4] },
];

const trendTabs = [
  { label: 'New arrivals', query: '' },
  { label: 'Kanchipuram', query: 'Kanchipuram' },
  { label: 'Mysore silk', query: 'Mysore' },
  { label: 'Kalamkari', query: 'Kalamkari' },
  { label: 'Paithani', query: 'Paithani' },
  { label: 'Kora organza', query: 'Organza' },
];

const serviceNotes = [
  { icon: Truck, title: 'Free shipping', detail: 'Complimentary delivery over Rs. 5,000' },
  { icon: ShieldCheck, title: 'Quality checked', detail: 'Every drape reviewed before dispatch' },
  { icon: Sparkles, title: 'Styling support', detail: 'A second opinion when you need it' },
  { icon: Check, title: 'Easy returns', detail: 'A simple, transparent policy' },
];

function productText(product) {
  return `${product.name || ''} ${product.category || ''} ${product.categoryName || ''} ${product.fabric || ''} ${product.occasion || ''}`.toLowerCase();
}

export default function HomePage() {
  const { user } = useSelector((state) => state.auth);
  const [featuredProducts, setFeaturedProducts] = useState(HOMEPAGE_PRODUCTS);
  const [wishlistProductIds, setWishlistProductIds] = useState([]);
  const [activeTrend, setActiveTrend] = useState(0);
  const [activeHero, setActiveHero] = useState(0);
  const [loading, setLoading] = useState(true);
  const dispatch = useDispatch();

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        const prodData = await productService.getProducts({ size: 16 });
        const fetchedProducts = prodData?.data?.content || [];
        if (fetchedProducts.length > 0) setFeaturedProducts(fetchedProducts);

        if (user) {
          try {
            const wishRes = await api.get('/wishlist');
            if (wishRes.data?.success) {
              setWishlistProductIds((wishRes.data.data || []).map((item) => item.id));
            }
          } catch (error) {
            console.error('Failed to load wishlist', error);
          }
        }
      } catch (error) {
        console.error('Failed to load homepage data', error);
        setFeaturedProducts(HOMEPAGE_PRODUCTS);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [user]);

  useEffect(() => {
    eventTracker.trackLanding({ source: 'homepage' });
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveHero((current) => (current + 1) % heroStories.length);
    }, 8000);
    return () => window.clearInterval(timer);
  }, []);

  const activeStory = heroStories[activeHero];
  const visibleProducts = useMemo(() => {
    const query = trendTabs[activeTrend].query.toLowerCase();
    if (!query) return featuredProducts;
    const filtered = featuredProducts.filter((product) => productText(product).includes(query));
    return filtered.length > 0 ? filtered : featuredProducts;
  }, [activeTrend, featuredProducts]);

  const handleAddSuccess = (product, isBuyNow) => {
    if (isBuyNow) {
      dispatch(closeAddedModal());
      dispatch(setCartOpen(true));
    }
  };

  const handleWishlistToggle = (productId, isAdded) => {
    setWishlistProductIds((prev) => (
      isAdded ? [...new Set([...prev, productId])] : prev.filter((id) => id !== productId)
    ));
  };

  return (
    <div className="min-h-screen overflow-x-hidden bg-white text-[#17211F]">
      <SEO
        title="SareeKart | Luxury Indian Handloom Sarees"
        description="Discover authenticated handloom sarees across Kanchipuram, Banarasi, Paithani, cotton, and organza directly from master weavers at SareeKart."
        canonical={getCanonicalUrl('/')}
        schemaData={[
          {
            "@context": "https://schema.org",
            "@type": "WebSite",
            "name": "SareeKart",
            "url": getCanonicalUrl('/'),
            "potentialAction": {
              "@type": "SearchAction",
              "target": `${getCanonicalUrl('/products')}?search={search_term_string}`,
              "query-input": "required name=search_term_string"
            }
          },
          {
            "@context": "https://schema.org",
            "@type": "Organization",
            "name": "SareeKart Handlooms",
            "url": getCanonicalUrl('/'),
            "logo": toAbsoluteImageUrl('/favicon.svg')
          }
        ]}
      />

      <section className="bg-[#F7F4EE] py-3 sm:py-4">
        <div className="section-shell grid overflow-hidden bg-white lg:grid-cols-[1.25fr_0.75fr]">
          <div className="relative aspect-[0.82] sm:aspect-[0.9] lg:aspect-auto lg:min-h-[520px]">
            <motion.img
              key={activeStory.image}
              initial={{ opacity: 0, scale: 1.03 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.8 }}
              src={activeStory.image}
              alt="Handwoven saree from the SareeKart edit"
              className="absolute inset-x-0 top-0 aspect-[0.82] w-full object-cover object-center sm:aspect-[0.9] lg:inset-0 lg:aspect-auto lg:h-full"
            />
            <div className="absolute bottom-5 left-5 right-5 flex items-center justify-between bg-[#17211F]/82 px-4 py-3 text-white backdrop-blur-sm sm:bottom-7 sm:left-7 sm:right-7 sm:px-5">
              <span className="text-[10px] font-bold uppercase tracking-[0.18em]">SareeKart / {String(activeHero + 1).padStart(2, '0')}</span>
              <div className="flex items-center gap-2">
                {heroStories.map((story, index) => (
                  <button
                    key={story.eyebrow}
                    type="button"
                    onClick={() => setActiveHero(index)}
                    className={`h-1 transition-all ${index === activeHero ? 'w-12 bg-[#F3C56A]' : 'w-5 bg-white/45'}`}
                    aria-label={`Show story ${index + 1}`}
                  />
                ))}
              </div>
            </div>
          </div>

          <div className="flex items-center bg-white px-7 py-14 sm:px-12 lg:px-14">
            <motion.div
              key={activeStory.title}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45 }}
              className="max-w-md"
            >
              <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#B84F49]">{activeStory.eyebrow}</p>
              <h1 className="mt-5 font-serif text-5xl font-medium leading-[0.98] text-[#17211F] sm:text-6xl">{activeStory.title}</h1>
              <div className="mt-8 h-px w-20 bg-[#C48B3C]" />
              <p className="mt-7 text-sm leading-7 text-[#71817A]">{activeStory.detail}</p>
              <Link to={activeStory.link} className="mt-8 inline-flex min-h-12 items-center gap-3 bg-[#17211F] px-6 text-sm font-bold uppercase tracking-[0.1em] text-white transition hover:bg-[#1E6A62]">
                {activeStory.linkLabel}
                <ArrowRight className="h-4 w-4" />
              </Link>
              <div className="mt-12 grid grid-cols-2 gap-x-5 gap-y-5 border-t border-[#DDD8CF] pt-6 text-xs text-[#71817A]">
                <div><span className="block font-serif text-2xl text-[#17211F]">300+</span>distinct drapes</div>
                <div><span className="block font-serif text-2xl text-[#17211F]">12</span>regional weaves</div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      <section className="border-b border-[#DDD8CF] bg-white">
        <div className="section-shell grid divide-y divide-[#DDD8CF] py-2 sm:grid-cols-2 sm:divide-x sm:divide-y-0 lg:grid-cols-4">
          {serviceNotes.map((note) => {
            const Icon = note.icon;
            return (
              <div key={note.title} className="flex items-center gap-3 px-4 py-4 sm:justify-center sm:px-6">
                <Icon className="h-5 w-5 shrink-0 text-[#1E6A62]" />
                <div><p className="text-xs font-bold uppercase tracking-[0.08em] text-[#17211F]">{note.title}</p><p className="mt-0.5 text-xs text-[#71817A]">{note.detail}</p></div>
              </div>
            );
          })}
        </div>
      </section>

      <section className="bg-white py-16 sm:py-20">
        <div className="section-shell">
          <div className="mb-8 flex items-end justify-between gap-5">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#1E6A62]">Shop by story</p>
              <h2 className="mt-3 font-serif text-4xl font-medium text-[#17211F] sm:text-5xl">Find your kind of drape.</h2>
            </div>
            <Link to="/products" className="hidden items-center gap-2 text-xs font-bold uppercase tracking-[0.12em] text-[#1E6A62] sm:inline-flex">View all <ArrowRight className="h-4 w-4" /></Link>
          </div>

          <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:gap-5">
            {collectionTiles.map((tile, index) => (
              <Link key={tile.title} to={`/products?search=${encodeURIComponent(tile.query)}`} className={`group relative overflow-hidden bg-[#E9E1D5] ${index === 0 ? 'aspect-[0.84] md:row-span-2 md:aspect-auto' : 'aspect-[1.05]'}`}>
                <img src={tile.image} alt={tile.title} loading="lazy" className="h-full w-full object-cover transition duration-700 group-hover:scale-105" />
                <div className="absolute inset-x-3 bottom-3 bg-[#17211F]/82 p-4 text-white sm:inset-x-4 sm:bottom-4">
                  <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#F3C56A]">{tile.kicker}</p>
                  <h3 className="mt-1 font-serif text-2xl font-medium sm:text-3xl">{tile.title}</h3>
                  <span className="mt-3 inline-flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.12em] text-white/75">Shop now <ArrowRight className="h-3.5 w-3.5" /></span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-[#F7F4EE] py-16 sm:py-20">
        <div className="section-shell">
          <div className="text-center">
            <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#B84F49]">What is moving now</p>
            <h2 className="mt-3 font-serif text-4xl font-medium text-[#17211F] sm:text-5xl">Trending at SareeKart</h2>
          </div>

          <div className="no-scrollbar mt-8 flex justify-start gap-2 overflow-x-auto pb-1 sm:justify-center">
            {trendTabs.map((tab, index) => (
              <button key={tab.label} type="button" onClick={() => setActiveTrend(index)} className={`shrink-0 border px-5 py-2.5 text-xs font-bold transition ${index === activeTrend ? 'border-[#17211F] bg-[#17211F] text-white' : 'border-[#C9C1B5] bg-white text-[#71817A] hover:border-[#1E6A62] hover:text-[#1E6A62]'}`}>
                {tab.label}
              </button>
            ))}
          </div>

          <div className="mt-9">
            {loading && visibleProducts.length === 0 ? <div className="py-16 text-center text-sm font-semibold text-[#71817A]">Finding the right drapes...</div> : <ProductGrid products={visibleProducts} wishlistIds={wishlistProductIds} onAddSuccess={handleAddSuccess} onWishlistToggle={handleWishlistToggle} limit={4} />}
          </div>

          <div className="mt-10 flex justify-center">
            <Link to="/products" className="inline-flex items-center gap-3 border-b border-[#1E6A62] pb-2 text-sm font-bold uppercase tracking-[0.12em] text-[#1E6A62]">View all new arrivals <ChevronRight className="h-4 w-4" /></Link>
          </div>
        </div>
      </section>

      <section className="bg-white py-16 sm:py-20">
        <div className="section-shell">
          <div className="mb-8 flex items-end justify-between gap-5">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#1E6A62]">Curated for your calendar</p>
              <h2 className="mt-3 font-serif text-4xl font-medium text-[#17211F] sm:text-5xl">Shop categories</h2>
            </div>
            <Link to="/products" className="hidden items-center gap-2 text-xs font-bold uppercase tracking-[0.12em] text-[#1E6A62] sm:inline-flex">Explore all <ArrowRight className="h-4 w-4" /></Link>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {shopCategories.map((category) => (
              <Link key={category.title} to={`/products?search=${encodeURIComponent(category.query)}`} className="group relative aspect-[0.88] overflow-hidden bg-[#E9E1D5]">
                <img src={category.image} alt={category.title} loading="lazy" className="h-full w-full object-cover transition duration-700 group-hover:scale-105" />
                <div className="absolute inset-x-0 bottom-0 bg-[#17211F]/88 px-5 py-5 text-white">
                  <h3 className="font-serif text-2xl font-medium">{category.title}</h3>
                  <p className="mt-1 text-xs text-white/70">{category.detail}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-[#17211F] py-16 text-white sm:py-20">
        <div className="section-shell grid overflow-hidden lg:grid-cols-[0.85fr_1.15fr]">
          <div className="flex items-center px-7 py-14 sm:px-12 lg:px-14">
            <div className="max-w-md">
              <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#F3C56A]">The ceremony edit</p>
              <h2 className="mt-5 font-serif text-5xl font-medium leading-[0.98] sm:text-6xl">A little more occasion.</h2>
              <p className="mt-6 text-sm leading-7 text-white/70">Silks and zari for the invitations already in your calendar. Make an entrance, then keep the saree forever.</p>
              <Link to="/products?search=bridal" className="mt-8 inline-flex min-h-12 items-center gap-3 border border-white/70 px-6 text-sm font-bold uppercase tracking-[0.1em] transition hover:bg-white hover:text-[#17211F]">Shop wedding sarees <ArrowRight className="h-4 w-4" /></Link>
            </div>
          </div>
          <div className="min-h-[420px] bg-[#1E6A62]">
            <img src={imagePool[9]} alt="Wedding silk saree detail" loading="lazy" className="h-full w-full object-cover object-center opacity-90" />
          </div>
        </div>
      </section>

      <section className="bg-[#F7F4EE] py-16 sm:py-20">
        <div className="section-shell">
          <div className="mb-8 text-center">
            <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#B84F49]">Dress for the feeling</p>
            <h2 className="mt-3 font-serif text-4xl font-medium text-[#17211F] sm:text-5xl">Shop by occasion</h2>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {occasionTiles.map((tile) => (
              <Link key={tile.title} to={`/products?search=${encodeURIComponent(tile.query)}`} className="group relative aspect-[0.74] overflow-hidden bg-[#E9E1D5]">
                <img src={tile.image} alt={tile.title} loading="lazy" className="h-full w-full object-cover transition duration-700 group-hover:scale-105" />
                <div className="absolute inset-x-0 bottom-0 bg-white/92 px-4 py-4 text-center"><h3 className="font-serif text-2xl font-medium text-[#17211F]">{tile.title}</h3><span className="mt-1 block text-[10px] font-bold uppercase tracking-[0.13em] text-[#1E6A62]">Shop now</span></div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="border-y border-[#DDD8CF] bg-white py-14 sm:py-16">
        <div className="section-shell grid gap-8 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#1E6A62]">A better way to browse</p>
            <h2 className="mt-3 max-w-lg font-serif text-4xl font-medium text-[#17211F] sm:text-5xl">Not sure where to start?</h2>
            <p className="mt-4 max-w-lg text-sm leading-7 text-[#71817A]">Search by color, fabric, or occasion. We will help you narrow the edit down to the drapes that feel like you.</p>
          </div>
          <div className="flex flex-wrap gap-3 lg:justify-end">
            {['Red silk', 'Pastel sarees', 'Cotton under Rs. 10,000', 'Gold zari', 'Black sarees'].map((term) => (
              <Link key={term} to={`/products?search=${encodeURIComponent(term)}`} className="inline-flex min-h-12 items-center gap-2 border border-[#C9C1B5] px-5 text-sm font-semibold text-[#4E5B56] transition hover:border-[#1E6A62] hover:text-[#1E6A62]"><Search className="h-4 w-4" />{term}</Link>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-[#F3E6C7] py-12 sm:py-16">
        <div className="section-shell grid gap-7 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#1E6A62]">The handloom journal</p>
            <h2 className="mt-3 font-serif text-4xl font-medium text-[#17211F]">Discover elegant sarees first.</h2>
            <p className="mt-3 max-w-lg text-sm leading-6 text-[#4E5B56]">New arrivals, weave notes, styling ideas, and early access to the pieces we are most excited about.</p>
          </div>
          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={(event) => event.preventDefault()}>
            <label htmlFor="home-email" className="sr-only">Email address</label>
            <input id="home-email" type="email" placeholder="Your email address" className="h-13 min-w-0 flex-1 border border-[#C9C1B5] bg-white px-5 text-sm text-[#17211F] outline-none focus:border-[#1E6A62]" />
            <button type="submit" className="inline-flex h-13 items-center justify-center gap-2 bg-[#17211F] px-6 text-sm font-bold uppercase tracking-[0.1em] text-white transition hover:bg-[#1E6A62]">Subscribe <ArrowRight className="h-4 w-4" /></button>
          </form>
        </div>
      </section>
    </div>
  );
}
