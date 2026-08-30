import { useState, useEffect, lazy, Suspense } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AnimatePresence } from 'framer-motion';
import { closeAddedModal, setCartOpen } from '../../redux/slices/cartSlice';
import productService from '../../services/productService';
import LoadingSpinner from '../../components/common/LoadingSpinner';

// Premium Modular Homepage Components – above-the-fold eager
import OfferBar from '../../components/OfferBar';
import Hero from '../../components/Hero';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
import SilkLoader from '../../components/common/SilkLoader';

// Lazy below-the-fold for perf – code-split
const ProductGrid = lazy(() => import('../../components/ProductGrid'));
const CelebritySection = lazy(() => import('../../components/CelebritySection'));
const HeritageStory = lazy(() => import('../../components/HeritageStory'));
const WeddingCollection = lazy(() => import('../../components/WeddingCollection'));
const HandloomTimeline = lazy(() => import('../../components/HandloomTimeline'));
const ReviewsSection = lazy(() => import('../../components/ReviewsSection'));
const InstagramGallery = lazy(() => import('../../components/InstagramGallery'));
const SignatureCollections = lazy(() => import('../../components/SignatureCollections'));
const Newsletter = lazy(() => import('../../components/Newsletter'));
const ArtisanDocumentary = lazy(() => import('../../components/ArtisanDocumentary'));
const Lookbook = lazy(() => import('../../components/Lookbook'));
const FabricInspector = lazy(() => import('../../components/FabricInspector'));
const MeetArtisans = lazy(() => import('../../components/MeetArtisans'));
const ShopByOccasion = lazy(() => import('../../components/ShopByOccasion'));
const ShopByFabric = lazy(() => import('../../components/ShopByFabric'));
const ShopByRegion = lazy(() => import('../../components/ShopByRegion'));

const SectionFallback = () => (
  <div className="max-w-[1480px] mx-auto px-10 sm:px-20 py-16">
    <div className="h-64 bg-[#F9F6F1] rounded-[20px] animate-pulse border border-[#F4F2EB]" />
  </div>
);

export default function HomePage() {
  const { user } = useSelector(state => state.auth);
  const [featuredProducts, setFeaturedProducts] = useState([]);
  const [wishlistProductIds, setWishlistProductIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Custom Curtain Loader State
  const [showLoader, setShowLoader] = useState(true);

  const dispatch = useDispatch();

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError(null);
        const prodData = await productService.getProducts({ size: 4 });
        setFeaturedProducts(prodData?.data?.content || []);

        if (user) {
          try {
            const wishRes = await api.get('/wishlist');
            if (wishRes.data?.success) {
              setWishlistProductIds((wishRes.data.data?.items || wishRes.data.data || []).map(w => w.id));
            }
          } catch (e) {
            console.error("Failed to load wishlist in HomePage", e);
          }
        }
      } catch (err) {
        console.error('Failed to load home page data', err);
        setError('Could not load the collection right now. Please refresh to try again.');
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [user]);

  const handleAddSuccess = (product, isBuyNow) => {
    if (isBuyNow) {
      dispatch(closeAddedModal());
      dispatch(setCartOpen(true));
    }
  };

  const handleWishlistToggle = (productId, isAdded) => {
    if (isAdded) {
      setWishlistProductIds(prev => [...prev, productId]);
    } else {
      setWishlistProductIds(prev => prev.filter(id => id !== productId));
    }
  };

  return (
    <>
      {/* 01. Brand Loom Silk Loader */}
      <AnimatePresence mode="wait">
        {showLoader && (
          <SilkLoader onComplete={() => setShowLoader(false)} />
        )}
      </AnimatePresence>

      <div className="bg-[#FAF8F5] min-h-screen text-[#22181C]">
        <SEO 
          title="SareeKart | Luxury Handwoven Artisanal Sarees" 
          description="Shop absolute premium, handcrafted Indian heritage Banarasi silk, Kanchipuram bridal tissue, and Jamdani cotton sarees directly from weavers."
        />
        
        {/* Sticky Announcement Bar */}
        <OfferBar />

        {/* 1. Cinematic Hero (120vh) */}
        <Hero />

        {/* 2. Brand Story (Our Legacy Narrative Section) */}
        <Suspense fallback={<SectionFallback />}><HeritageStory /></Suspense>

        {/* 3. Craftsmanship (Artisan Documentary Video Block) */}
        <Suspense fallback={<SectionFallback />}><ArtisanDocumentary /></Suspense>

        {/* 4. Signature Collections (100vh split layouts) */}
        <Suspense fallback={<SectionFallback />}><SignatureCollections /></Suspense>

        {/* 5. Featured Sarees (Bestsellers Product Gallery Showcase) */}
        <section className="max-w-[1480px] mx-auto px-10 sm:px-20 py-32 border-t border-[#F4F2EB] bg-white rounded-[20px] shadow-luxury mt-6 min-h-[110vh] flex flex-col justify-between">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 gap-6">
            <div className="space-y-3 text-left">
              <span className="text-[#C89B3C] font-serif text-xs font-bold tracking-[0.25em] uppercase block">
                Patron Favourites
              </span>
              <h2 className="text-4xl sm:text-6xl font-serif font-bold text-[#2B0F1E] leading-tight">
                Featured Sarees
              </h2>
              <p className="text-sm text-text-secondary font-light max-w-xl">
                Explore highly coveted drapes this season. Handcrafted statements designed to last lifetimes.
              </p>
            </div>
            <Link 
              to="/products" 
              className="border border-[#2B0F1E] text-[#2B0F1E] hover:bg-[#2B0F1E] hover:text-white px-8 py-4 rounded-xl text-xs font-bold transition-all uppercase tracking-widest font-sans self-start md:self-end shrink-0"
            >
              View Full Archive
            </Link>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-xl text-center font-medium mb-8">
              {error}
            </div>
          )}

          {loading && featuredProducts.length === 0 ? (
            <LoadingSpinner message="Fetching best sellers..." />
          ) : featuredProducts.length === 0 ? (
            <div className="text-center py-16 text-text-secondary">
              <p className="font-serif text-lg text-[#2B0F1E] font-bold">The collection is being curated.</p>
              <p className="text-sm mt-2">New handwoven arrivals will appear here shortly.</p>
              <Link
                to="/products"
                className="inline-block mt-6 border border-[#2B0F1E] text-[#2B0F1E] hover:bg-[#2B0F1E] hover:text-white px-8 py-4 rounded-xl text-xs font-bold transition-all uppercase tracking-widest font-sans"
              >
                Browse the Archive
              </Link>
            </div>
          ) : (
            <Suspense fallback={<SectionFallback />}><ProductGrid 
              products={featuredProducts} 
              wishlistIds={wishlistProductIds} 
              onAddSuccess={handleAddSuccess}
              onWishlistToggle={handleWishlistToggle}
            /></Suspense>
          )}
        </section>

        {/* Fabric zoom experience (100vh) */}
        <Suspense fallback={<SectionFallback />}><FabricInspector /></Suspense>

        {/* 6. Wedding Collection (The Golden Muhurtham Wedding Feature) */}
        <Suspense fallback={<SectionFallback />}><WeddingCollection /></Suspense>

        {/* 7. Shop by Occasion (95vh) */}
        <Suspense fallback={<SectionFallback />}><ShopByOccasion /></Suspense>

        {/* 8. Shop by Fabric (95vh) */}
        <Suspense fallback={<SectionFallback />}><ShopByFabric /></Suspense>

        {/* 9. Shop by Region */}
        <Suspense fallback={<SectionFallback />}><ShopByRegion /></Suspense>

        {/* 10. Meet the Artisans */}
        <Suspense fallback={<SectionFallback />}><MeetArtisans /></Suspense>

        {/* 11. Heritage Timeline (Weaver Making & Production Timeline) */}
        <Suspense fallback={<SectionFallback />}><HandloomTimeline /></Suspense>

        {/* 12. Celebrity Inspirations (Celebrity Picks showcase) */}
        <Suspense fallback={<SectionFallback />}><CelebritySection /></Suspense>

        {/* 13. Customer Stories (Customer Appreciation Reviews) */}
        <Suspense fallback={<SectionFallback />}><ReviewsSection /></Suspense>

        {/* Lookbook Editorial Slider Carousel */}
        <Suspense fallback={<SectionFallback />}><Lookbook /></Suspense>

        {/* 14. Instagram visual gallery diary */}
        <Suspense fallback={<SectionFallback />}><InstagramGallery /></Suspense>

        {/* 15. Newsletter (Weaver Guild Registration) */}
        <Suspense fallback={<SectionFallback />}><Newsletter /></Suspense>

      </div>
    </>
  );
}
