import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ArrowRight,
  Award,
  CheckCircle2,
  ChevronRight,
  Heart,
  MapPin,
  MessageCircle,
  Minus,
  Plus,
  RefreshCcw,
  Ruler,
  Scissors,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Star,
  Truck,
  Video,
} from 'lucide-react';
import api from '../../api/axiosConfig';
import productService from '../../services/productService';
import logisticsService from '../../services/logisticsService';
import visualSearchService from '../../services/visualSearchService';
import { addToCart } from '../../redux/slices/cartSlice';
import ProductGrid from '../../components/ProductGrid';
import SEO from '../../components/common/SEO';
import { getCanonicalUrl, truncateDescription, toAbsoluteImageUrl, DEFAULT_OG_IMAGE } from '../../utils/seoUtils';
import { HOMEPAGE_PRODUCTS } from '../../data/products';
import MobileProductGallery from '../../components/product/MobileProductGallery';
import WeaveProvenanceModal from '../../components/product/WeaveProvenanceModal';
import TailoringStudioModal from '../../components/tailoring/TailoringStudioModal';
import AiStylistModal from '../../components/stylist/AiStylistModal';
import { useCurrency } from '../../context/CurrencyContext';
import eventTracker from '../../utils/eventTracker';

const fallbackImage = 'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=1200&q=80';
const detailFallbacks = [
  fallbackImage,
  'https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=1200&q=80',
  'https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=1200&q=80',
  'https://images.unsplash.com/photo-1583391265517-35bbdad01209?auto=format&fit=crop&fm=webp&w=1200&q=80',
];

function getImages(product) {
  const images = product?.images || product?.imageUrls || [];
  const merged = [...images, product?.image].filter(Boolean);
  return merged.length ? merged : detailFallbacks;
}

export default function ProductDetailPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const { formatPrice } = useCurrency();
  const formatCurrency = formatPrice;

  const [product, setProduct] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [relatedProducts, setRelatedProducts] = useState(HOMEPAGE_PRODUCTS.slice(0, 4));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [qty, setQty] = useState(1);
  const [blouseOption, _setBlouseOption] = useState('Unstitched blouse fabric');
  const [giftOption, setGiftOption] = useState('Standard recyclable box');
  const [isTailoringOpen, setIsTailoringOpen] = useState(false);
  const [tailoringSpecs, setTailoringSpecs] = useState({
    fallPico: true,
    fallPrice: 0,
    blouseStyle: 'unstitched',
    blousePrice: 0,
    totalExtra: 0,
    measurements: null,
  });
  const [pincode, setPincode] = useState('');
  const [deliveryEstimate, setDeliveryEstimate] = useState(null);
  const [pincodeError, setPincodeError] = useState('');
  const [checkingPincode, setCheckingPincode] = useState(false);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewError, setReviewError] = useState(null);
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [provenanceModalOpen, setProvenanceModalOpen] = useState(false);
  const [isAiStylistOpen, setIsAiStylistOpen] = useState(false);
  const [visuallySimilarDrapes, setVisuallySimilarDrapes] = useState([]);

  useEffect(() => {
    let startTime = Date.now();
    let viewedProduct = null;

    const loadProductData = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await productService.getProductById(id);
        const loadedProduct = response.data;
        viewedProduct = loadedProduct;
        setProduct(loadedProduct);
        eventTracker.trackProductView(loadedProduct, 0);

        // Fetch AI visually similar drapes
        try {
          const vsRes = await visualSearchService.getSimilarDrapes(id, 4);
          if (vsRes?.data) setVisuallySimilarDrapes(vsRes.data);
        } catch (vsErr) {
          console.warn('Could not load visually similar drapes:', vsErr);
        }

        try {
          const revRes = await api.get(`/products/${id}/reviews`);
          if (revRes.data?.success) setReviews(revRes.data.data || []);
        } catch (reviewLoadError) {
          console.error('Error loading reviews', reviewLoadError);
        }

        if (user) {
          try {
            const wishRes = await api.get('/wishlist');
            const wishlist = wishRes.data?.data || wishRes.data?.products || [];
            setIsWishlisted(wishlist.some((item) => item.id === loadedProduct.id));
          } catch (wishlistError) {
            console.error('Error checking wishlist status', wishlistError);
          }
        }

        if (loadedProduct.categoryId) {
          const relatedResponse = await productService.filterProducts({
            categoryId: loadedProduct.categoryId,
            size: 4,
          });
          const items = (relatedResponse.data?.content || []).filter((item) => item.id !== loadedProduct.id);
          if (items.length) setRelatedProducts(items.slice(0, 4));
        }
      } catch (loadError) {
        console.error('Failed to load product details', loadError);
        const status = loadError?.response?.status;
        if (status === 404 || loadError?.message?.includes('404')) {
          setProduct(null);
          setNotFound(true);
        } else {
          // If server error or offline, only use local fallback if valid item matches ID
          const fallbackProduct = HOMEPAGE_PRODUCTS.find((item) => item.id === Number(id));
          if (fallbackProduct) {
            setProduct(fallbackProduct);
            setError('Showing a local product preview while the backend is unavailable.');
          } else {
            setProduct(null);
            setNotFound(true);
          }
        }
      } finally {
        setLoading(false);
      }
    };

    loadProductData();

    return () => {
      if (viewedProduct) {
        const dwellTimeMs = Date.now() - startTime;
        if (dwellTimeMs > 1000) {
          eventTracker.trackProductView(viewedProduct, dwellTimeMs);
        }
      }
    };
  }, [id, user]);

  const productImages = useMemo(() => getImages(product), [product]);
  const stock = product?.stockQuantity ?? product?.stock ?? 8;
  const discount = product?.discount || 12;
  const originalPrice = product?.price ? Math.round(product.price / (1 - discount / 100)) : 0;
  const averageRating = reviews.length
    ? (reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length).toFixed(1)
    : (product?.rating || 4.8).toFixed(1);

  const handleAddToCart = () => {
    if (!product) return;
    const finalPrice = product.price + (tailoringSpecs.totalExtra || 0);
    const resolvedBlouse =
      tailoringSpecs.blouseStyle === 'unstitched'
        ? 'Unstitched blouse fabric'
        : tailoringSpecs.blouseStyle === 'tailored'
        ? `Custom Tailored (+${formatPrice(1499)})`
        : `Designer Maggam (+${formatPrice(2799)})`;

    for (let count = 0; count < qty; count += 1) {
      dispatch(
        addToCart({
          ...product,
          price: finalPrice,
          basePrice: product.price,
          selectedBlouse: resolvedBlouse,
          selectedGift: giftOption,
          tailoring: tailoringSpecs,
        })
      );
    }
    eventTracker.trackAddToCart(product, qty);
  };

  const handleCheckPincode = async (e) => {
    e.preventDefault();
    setPincodeError('');
    const cleanPin = pincode.trim();
    if (!/^\d{6}$/.test(cleanPin)) {
      setPincodeError('Please enter a valid 6-digit Indian PIN code.');
      setDeliveryEstimate(null);
      return;
    }

    setCheckingPincode(true);
    try {
      const res = await logisticsService.checkPincode(cleanPin);
      if (res && res.data) {
        const d = res.data;
        if (!d.serviceable) {
          setPincodeError(`PIN code ${cleanPin} is currently outside our delivery network.`);
          setDeliveryEstimate(null);
        } else {
          setDeliveryEstimate({
            date: d.estimatedDeliveryDate,
            courier: d.courierPartner,
            location: `${d.city}, ${d.state}`,
            zone: d.zoneDisplayName,
            hub: d.fulfillmentHub,
            window: d.deliveryWindowLabel,
            cod: d.codAvailable,
            override: d.customOverrideApplied
          });
        }
      }
    } catch (err) {
      setPincodeError(err.message || 'Unable to check PIN code delivery status.');
      setDeliveryEstimate(null);
    } finally {
      setCheckingPincode(false);
    }
  };

  const handleToggleWishlist = async () => {
    if (!user) {
      alert('Please login to manage your wishlist.');
      return;
    }
    try {
      setWishlistLoading(true);
      if (isWishlisted) {
        await api.delete(`/wishlist/${product.id}`);
        setIsWishlisted(false);
        eventTracker.trackWishlistRemove(product);
      } else {
        await api.post(`/wishlist/${product.id}`);
        setIsWishlisted(true);
        eventTracker.trackWishlistAdd(product);
      }
    } catch (wishlistError) {
      console.error('Failed to toggle wishlist', wishlistError);
    } finally {
      setWishlistLoading(false);
    }
  };

  const blouseRecommendation = useMemo(() => {
    const isSilk = /silk/i.test(product?.fabric || product?.category || '');
    if (isSilk) {
      return {
        style: 'Elbow-Length Raw Silk or Brocade',
        note: 'Opt for contrast sweetheart neckline with subtle gold zari piping to frame the rich pallu beautifully.',
      };
    }
    return {
      style: 'Boat Neck Handloom Cotton Blouse',
      note: 'Monochrome or contrast woven elbow sleeves with handcrafted fabric buttons along the back.',
    };
  }, [product]);

  const jewelryRecommendation = useMemo(() => {
    const isBridal = /bridal|wedding|kanchipuram|banarasi/i.test(
      `${product?.occasion || ''} ${product?.category || ''}`
    );
    if (isBridal) {
      return {
        style: '22K Antique Temple Gold or Kundan',
        note: 'Pair with layered jhumkas, a coin necklace (kasu mala), and fresh jasmine gajra for a regal finish.',
      };
    }
    return {
      style: 'Oxidized Silver or Terracotta Accents',
      note: 'Minimal silver studs, a sleek choker, and a structured terracotta bangle stack.',
    };
  }, [product]);

  const drapingRecommendation = useMemo(() => {
    const isSilk = /silk/i.test(product?.fabric || '');
    if (isSilk) {
      return {
        style: 'Classic Nivi Drape with Broad Pleats',
        note: 'Pin pleats securely at the shoulder to highlight the intricate gold zari border across the pallu.',
      };
    }
    return {
      style: 'Casual Floating Pallu or Seedha Pallu',
      note: 'Keep the pallu loose over the forearm for effortless all-day movement and breezy drape.',
    };
  }, [product]);

  const handleOpenAiStylist = () => {
    setIsAiStylistOpen(true);
  };

  const handleCustomizeFromStylist = (preset) => {
    const isDesigner = preset.blouseStyle === 'designer';
    const bPrice = isDesigner ? 2799 : 1499;
    setTailoringSpecs((prev) => ({
      ...prev,
      blouseStyle: preset.blouseStyle || 'designer',
      blousePrice: bPrice,
      totalExtra: (prev.fallPrice || 0) + bPrice,
      measurements: {
        ...(prev.measurements || {}),
        frontNeck: preset.frontNeck || 'Sweetheart',
        backNeck: preset.backNeck || 'Deep U with Dori',
        sleeve: preset.sleeve || 'Elbow Length (Traditional)',
        notes: preset.notes || '',
      },
    }));
    setIsTailoringOpen(true);
  };

  const handleSubmitReview = async (event) => {
    event.preventDefault();
    if (!user) {
      setReviewError('Please login to submit a review.');
      return;
    }
    if (!reviewComment.trim()) {
      setReviewError('Please enter review comments.');
      return;
    }
    try {
      setReviewSubmitting(true);
      setReviewError(null);
      const res = await api.post(`/products/${product.id}/reviews`, {
        rating: reviewRating,
        comment: reviewComment,
      });
      if (res.data?.success && res.data?.data) {
        setReviews((prev) => [res.data.data, ...prev]);
        setReviewComment('');
        setReviewRating(5);
      }
    } catch (submitError) {
      setReviewError(submitError.response?.data?.message || 'Failed to submit review.');
    } finally {
      setReviewSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#F5F7FA] text-sm font-black text-[#64748B]">
        Loading product...
      </div>
    );
  }

  if (notFound || !product) {
    return (
      <div className="min-h-[70vh] bg-[#F7F4EE] py-16 px-4 text-[#17211F]">
        <SEO
          title="Drape Not Found | SareeKart"
          description="The luxury handloom drape you are looking for is unavailable or does not exist."
          noindex={true}
          nofollow={true}
        />
        <div className="max-w-md mx-auto text-center">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-[#B84F49] mb-2">404 — Drape Not Found</p>
          <h1 className="text-3xl font-bold font-serif mb-4 text-[#17211F]">Drape Not Found</h1>
          <p className="text-sm text-[#71817A] mb-8 leading-relaxed">
            The handloom saree drape requested (#SK-{id}) is not available in our active collection, has been archived, or does not exist.
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Link
              to="/products"
              className="px-5 py-2.5 rounded-[6px] bg-[#3A0F1F] text-[#F7F4EE] text-xs font-bold uppercase tracking-wider hover:bg-[#2A0B16] transition-colors"
            >
              Explore Available Drapes
            </Link>
            <Link
              to="/"
              className="px-5 py-2.5 rounded-[6px] border border-[#DDD8CF] bg-white text-[#17211F] text-xs font-bold uppercase tracking-wider hover:bg-[#F3EFE6] transition-colors"
            >
              Return Home
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const canonicalUrl = getCanonicalUrl(`/products/${product.id}`);
  const metaDescription = truncateDescription(
    product.description || `Shop authentic handwoven ${product.name} crafted by master artisans at SareeKart.`
  );
  const primaryImage = productImages?.[0] || DEFAULT_OG_IMAGE;

  // Schema.org Product
  const productSchema = {
    "@context": "https://schema.org",
    "@type": "Product",
    "name": product.name,
    "description": metaDescription,
    "image": productImages.map(toAbsoluteImageUrl),
    "sku": `SK-${product.id}`,
    "brand": {
      "@type": "Brand",
      "name": "SareeKart"
    },
    ...(product.fabric && { "material": product.fabric }),
    "offers": {
      "@type": "Offer",
      "url": canonicalUrl,
      "priceCurrency": "INR",
      "price": product.price,
      "availability": (product.stockQuantity > 0)
        ? "https://schema.org/InStock"
        : "https://schema.org/OutOfStock",
      "itemCondition": "https://schema.org/NewCondition"
    },
    ...(reviews.length > 0 && {
      "aggregateRating": {
        "@type": "AggregateRating",
        "ratingValue": (reviews.reduce((acc, r) => acc + (r.rating || 5), 0) / reviews.length).toFixed(1),
        "reviewCount": reviews.length
      }
    })
  };

  // Schema.org BreadcrumbList
  const breadcrumbSchema = {
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
      ...(product.categoryName || product.category ? [{
        "@type": "ListItem",
        "position": 3,
        "name": product.categoryName || product.category,
        "item": getCanonicalUrl('/products', { category: product.categoryName || product.category }, ['category'])
      }] : []),
      {
        "@type": "ListItem",
        "position": (product.categoryName || product.category) ? 4 : 3,
        "name": product.name,
        "item": canonicalUrl
      }
    ]
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA] pb-20 text-[#111827]">
      <SEO
        title={`${product.name} | SareeKart Luxury Handlooms`}
        description={metaDescription}
        canonical={canonicalUrl}
        ogType="product"
        ogImage={primaryImage}
        ogImageAlt={`${product.name} - Luxury Handloom Saree`}
        schemaData={[productSchema, breadcrumbSchema]}
      />

      <div className="section-shell py-6">
        <nav className="flex flex-wrap items-center gap-2 text-xs font-black text-[#64748B]" aria-label="Breadcrumb navigation">
          <Link to="/" className="hover:text-[#243B6B]">Home</Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <Link to="/products" className="hover:text-[#243B6B]">Catalog</Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <span className="max-w-[260px] truncate text-[#111827]">{product.name}</span>
        </nav>
      </div>

      {error && (
        <div className="section-shell mb-5">
          <div className="rounded-[8px] border border-[#FFE0C2] bg-[#FFF7ED] p-4 text-sm font-bold text-[#9A6200]">
            {error}
          </div>
        </div>
      )}

      <section className="section-shell grid gap-8 lg:grid-cols-[1.08fr_0.92fr] lg:items-start">
        <MobileProductGallery
          images={productImages}
          productName={product.name}
          category={product.categoryName || product.category || 'SareeKart edit'}
        />

        <aside className="lg:sticky lg:top-32">
          <div className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-soft sm:p-6">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[#E7F5F3] px-3 py-1 text-[11px] font-black uppercase tracking-[0.12em] text-[#0F766E]">
                {stock <= 3 ? 'Low stock' : 'Ready to ship'}
              </span>
              <span className="inline-flex items-center gap-1 text-[11px] font-black uppercase tracking-[0.12em] text-[#E85D4F]">
                <Sparkles className="h-3.5 w-3.5" />
                {discount}% off
              </span>
            </div>

            <h1 className="mt-4 text-3xl font-bold leading-tight sm:text-4xl">{product.name}</h1>
            <p className="mt-4 text-sm font-medium leading-7 text-[#64748B]">
              {product.description || 'A modern handloom pick selected for color, texture, and practical styling.'}
            </p>

            <div className="mt-5 flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-1 text-[#F2B84B]">
                {[...Array(5)].map((_, index) => (
                  <Star key={index} className="h-4 w-4 fill-current" />
                ))}
              </div>
              <span className="text-sm font-black text-[#64748B]">{averageRating} rating · {reviews.length || product.reviews || 24} reviews</span>
            </div>

            <div className="my-6 rounded-[8px] bg-[#F5F7FA] p-4">
              <p className="text-[11px] font-black uppercase tracking-[0.14em] text-[#64748B]">Price</p>
              <div className="mt-1 flex flex-wrap items-baseline gap-3">
                <span className="text-3xl font-black text-[#111827]">{formatCurrency(product.price)}</span>
                {originalPrice > product.price && (
                  <span className="text-lg font-bold text-[#94A3B8] line-through">{formatCurrency(originalPrice)}</span>
                )}
              </div>
              <p className="mt-2 inline-flex items-center gap-2 text-sm font-black text-[#0F766E]">
                <CheckCircle2 className="h-4 w-4" />
                Inclusive of taxes. Dispatch in 24 to 48 hours.
              </p>

              <button
                type="button"
                onClick={() => window.dispatchEvent(new CustomEvent('open-silk-mark'))}
                className="mt-3 flex items-center justify-between rounded-[6px] border border-[#F3C56A]/60 bg-[#FFFDF7] p-2.5 text-left hover:bg-[#F3E6C7]/30 transition cursor-pointer w-full"
              >
                <div className="flex items-center gap-2">
                  <Award className="h-4 w-4 text-[#9B6A27]" />
                  <span className="text-xs font-black text-[#9B6A27]">Silk Mark Certified · 100% Pure Natural Silk</span>
                </div>
                <span className="text-[11px] font-bold text-[#1E6A62] underline underline-offset-2">View Certificate →</span>
              </button>
            </div>

            <div className="grid gap-4">
              {/* Luxury Tailoring Studio Card */}
              <div className="rounded-[8px] border border-[#DDD8CF] bg-[#FAF8F5] p-3.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Scissors className="h-4 w-4 text-[#1E6A62]" />
                    <span className="text-xs font-black uppercase tracking-wider text-[#17211F]">
                      Custom Tailoring & Fall/Pico
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setIsTailoringOpen(true)}
                    className="text-xs font-black text-[#1E6A62] hover:text-[#B84F49] underline underline-offset-2"
                  >
                    Customize Fit →
                  </button>
                </div>
                <div className="mt-2 space-y-1 text-xs">
                  <p className="text-[#4E5B56]">
                    • Fall & Pico: <strong className="text-[#17211F]">{tailoringSpecs.fallPico ? 'Included (Hand-stitched)' : 'None'}</strong>
                  </p>
                  <p className="text-[#4E5B56]">
                    • Blouse: <strong className="text-[#17211F]">
                      {tailoringSpecs.blouseStyle === 'unstitched'
                        ? 'Unstitched Fabric (Free)'
                        : tailoringSpecs.blouseStyle === 'tailored'
                        ? `Custom Tailored (+${formatPrice(1499)})`
                        : `Designer Maggam (+${formatPrice(2799)})`}
                    </strong>
                    {tailoringSpecs.measurements && (
                      <span className="ml-1 text-[11px] text-[#71817A] font-semibold">
                        ({tailoringSpecs.measurements.bust}, {tailoringSpecs.measurements.frontNeck})
                      </span>
                    )}
                  </p>
                </div>
              </div>

              {/* Pincode Delivery Estimator */}
              <div className="rounded-[8px] border border-[#DDD8CF] bg-white p-3.5 shadow-2xs">
                <div className="flex items-center gap-1.5 text-xs font-black text-[#17211F]">
                  <MapPin className="h-3.5 w-3.5 text-[#1E6A62]" />
                  <span>Check Delivery & Courier Serviceability</span>
                </div>
                <form onSubmit={handleCheckPincode} className="mt-2 flex gap-2">
                  <input
                    type="text"
                    maxLength={6}
                    placeholder="Enter 6-digit PIN code"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value.replace(/\D/g, ''))}
                    className="h-10 flex-1 rounded-[6px] border border-[#DDD8CF] bg-[#F7F4EE] px-3 font-mono text-xs font-bold outline-none focus:border-[#1E6A62]"
                  />
                  <button
                    type="submit"
                    disabled={checkingPincode}
                    className="h-10 rounded-[6px] bg-[#1E6A62] px-4 text-xs font-bold text-white hover:bg-[#154e48] transition-colors disabled:opacity-50"
                  >
                    {checkingPincode ? 'Checking...' : 'Check'}
                  </button>
                </form>

                {pincodeError && (
                  <p className="mt-2 text-[11px] font-bold text-red-600 flex items-center gap-1.5">
                    <span className="inline-block h-1.5 w-1.5 rounded-full bg-red-600"></span>
                    {pincodeError}
                  </p>
                )}

                {deliveryEstimate && (
                  <div className="mt-3 rounded-[6px] bg-[#F7F4EE] p-2.5 space-y-1.5 text-xs border border-[#E7E2D8]">
                    <div className="flex items-center justify-between">
                      <p className="font-bold text-[#0F766E] flex items-center gap-1.5">
                        <CheckCircle2 className="h-4 w-4 shrink-0 text-[#0F766E]" />
                        <span>Arriving by <strong>{deliveryEstimate.date}</strong></span>
                      </p>
                      {deliveryEstimate.override && (
                        <span className="rounded bg-amber-100 px-1.5 py-0.5 text-[9px] font-black uppercase text-amber-800">
                          Priority Route
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] font-medium text-[#4B5563]">
                      <strong className="text-[#17211F]">{deliveryEstimate.location}</strong> ({deliveryEstimate.zone})
                    </p>
                    <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-[#DDD8CF]/50 text-[10px]">
                      <span className="flex items-center gap-1 font-semibold text-[#1E6A62]">
                        <Truck className="h-3 w-3" />
                        {deliveryEstimate.courier}
                      </span>
                      <span className="text-[#9CA3AF]">·</span>
                      <span className={`font-semibold ${deliveryEstimate.cod ? 'text-emerald-700' : 'text-amber-700'}`}>
                        {deliveryEstimate.cod ? '✓ Cash on Delivery Available' : '⚠️ Prepaid / Online Only (No COD)'}
                      </span>
                    </div>
                  </div>
                )}
              </div>

              <label className="grid gap-2 text-sm font-black text-[#111827]">
                Packaging
                <select
                  value={giftOption}
                  onChange={(event) => setGiftOption(event.target.value)}
                  className="h-12 rounded-[8px] border border-[#DDE4EA] bg-white px-4 text-sm font-bold outline-none focus:border-[#243B6B]"
                >
                  <option>Standard recyclable box</option>
                  <option>Gift wrap with note (+Rs. 250)</option>
                  <option>Premium keepsake box (+Rs. 750)</option>
                </select>
              </label>

              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-black text-[#111827]">Quantity</p>
                  <p className="text-xs font-bold text-[#64748B]">{stock} available</p>
                </div>
                <div className="flex items-center rounded-full border border-[#DDE4EA] bg-[#F5F7FA] p-1">
                  <button onClick={() => setQty((prev) => Math.max(1, prev - 1))} className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white" aria-label="Decrease quantity">
                    <Minus className="h-4 w-4" />
                  </button>
                  <span className="min-w-10 text-center text-sm font-black">{qty}</span>
                  <button onClick={() => setQty((prev) => Math.min(stock, prev + 1))} className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white" aria-label="Increase quantity">
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-[1fr_auto]">
              <button onClick={handleAddToCart} className="sk-button-primary w-full">
                <ShoppingBag className="h-4 w-4" />
                Add to bag
              </button>
              <button
                onClick={handleToggleWishlist}
                disabled={wishlistLoading}
                className={`flex h-12 w-full items-center justify-center gap-2 rounded-full border px-5 text-sm font-black transition sm:w-14 ${
                  isWishlisted ? 'border-[#E85D4F] bg-[#FFF0EE] text-[#E85D4F]' : 'border-[#DDE4EA] bg-white text-[#111827] hover:border-[#E85D4F]'
                }`}
                aria-label="Toggle wishlist"
              >
                <Heart className={`h-5 w-5 ${isWishlisted ? 'fill-current' : ''}`} />
                <span className="sm:hidden">Wishlist</span>
              </button>
            </div>

            <div className="mt-3 grid gap-2.5 sm:grid-cols-2">
              <button
                type="button"
                onClick={handleOpenAiStylist}
                className="flex h-12 items-center justify-center gap-2 rounded-full border border-[#DDD8CF] bg-[#F7F4EE] text-xs font-bold uppercase tracking-wider text-[#17211F] transition hover:border-[#1E6A62] hover:bg-white shadow-xs"
                aria-label="Style with Gemini AI"
              >
                <Sparkles className="h-4 w-4 text-[#F3C56A]" />
                Style with AI
              </button>
              <a
                href={`https://wa.me/919059564499?text=${encodeURIComponent(
                  `Namaste SareeKart! I would like to inquire about the "${product.name}" (${formatCurrency(product.price)}). Could you please share availability and weave details?`
                )}`}
                target="_blank"
                rel="noreferrer"
                className="flex h-12 items-center justify-center gap-2 rounded-full border border-[#0F766E] bg-white text-xs font-bold uppercase tracking-wider text-[#0F766E] transition hover:bg-[#E7F5F3] shadow-xs"
              >
                <MessageCircle className="h-4 w-4" />
                Ask on WhatsApp
              </a>
            </div>

            <button
              type="button"
              onClick={() =>
                window.dispatchEvent(
                  new CustomEvent('open-video-shopping', { detail: { sareeName: product?.name || '' } })
                )
              }
              className="mt-2.5 flex w-full h-11 items-center justify-center gap-2 rounded-full border border-[#9E3E26] bg-[#FCF7F4] text-xs font-bold uppercase tracking-wider text-[#9E3E26] transition hover:bg-[#9E3E26] hover:text-white shadow-xs cursor-pointer"
            >
              <Video className="h-4 w-4" />
              Book 1-on-1 Video Draping Consultation
            </button>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            {[
              { icon: Truck, text: 'Free shipping' },
              { icon: RefreshCcw, text: '7 day returns' },
              { icon: ShieldCheck, text: 'Quality checked' },
            ].map((item) => {
              const Icon = item.icon;
              return (
                <div key={item.text} className="rounded-[8px] border border-[#DDE4EA] bg-white p-4 text-center shadow-xs">
                  <Icon className="mx-auto h-5 w-5 text-[#0F766E]" />
                  <p className="mt-2 text-xs font-black text-[#111827]">{item.text}</p>
                </div>
              );
            })}
          </div>

          <button
            type="button"
            onClick={() => setProvenanceModalOpen(true)}
            className="mt-4 flex w-full items-center justify-between rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] p-3.5 text-left transition hover:border-[#1E6A62] cursor-pointer"
          >
            <div className="flex items-center gap-3">
              <Award className="h-5 w-5 text-[#1E6A62] shrink-0" />
              <div>
                <p className="text-xs font-bold text-[#17211F]">Geographical Indication (GI) & SilkMark</p>
                <p className="text-[11px] text-[#71817A]">Verified authentic handloom artisan provenance</p>
              </div>
            </div>
            <span className="text-xs font-bold text-[#1E6A62] underline underline-offset-2">View Certificate</span>
          </button>
        </aside>
      </section>

      <section className="section-shell grid gap-6 py-12 lg:grid-cols-[0.85fr_1.15fr]">
        <div>
          <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#E85D4F]">Product notes</p>
          <h2 className="mt-2 text-3xl font-bold">What to know before you buy</h2>
          <p className="mt-4 text-sm font-medium leading-7 text-[#64748B]">
            Useful purchase details are grouped here so the buying panel can stay focused and quick.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          {[
            ['Fabric', product.fabric || 'Handloom blend'],
            ['Occasion', product.occasion || 'Festive and daily wear'],
            ['Color', product.color || 'Color varies by light'],
            ['Care', 'Dry clean recommended'],
            ['Blouse', blouseOption],
            ['Fit note', '6.2m saree with blouse fabric'],
          ].map(([label, value]) => (
            <div key={label} className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-xs">
              <p className="text-[11px] font-black uppercase tracking-[0.14em] text-[#0F766E]">{label}</p>
              <p className="mt-2 text-lg font-extrabold text-[#111827]">{value}</p>
            </div>
          ))}
        </div>
      </section>

      {/* AI Complete the Look & Draping Guide */}
      <section className="section-shell pb-12">
        <div className="rounded-2xl border border-[#DDD8CF] bg-white p-6 shadow-xs sm:p-8">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#E8E2D9] pb-6">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#17211F] text-[#F3C56A] shadow-xs">
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-serif text-xl font-bold text-[#17211F]">AI Complete The Look & Draping Guide</h3>
                  <span className="rounded-full bg-emerald-100 border border-emerald-300 px-2 py-0.5 text-[9px] font-bold uppercase tracking-widest text-emerald-800">
                    Gemini Curated
                  </span>
                </div>
                <p className="text-xs text-[#71817A]">Artisanal pairing suggestions & traditional drape tips for this {product.fabric || 'handloom'} saree</p>
              </div>
            </div>

            <button
              type="button"
              onClick={handleOpenAiStylist}
              className="inline-flex items-center gap-2 rounded-full bg-[#1E6A62] px-4 py-2.5 text-xs font-bold uppercase tracking-wider text-white transition hover:bg-[#16524C] shadow-xs cursor-pointer"
            >
              <Sparkles className="h-4 w-4 text-[#F3C56A]" />
              Consult AI Stylist
            </button>
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-xl border border-[#E8E2D9] bg-[#F7F4EE] p-4 shadow-xs">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#1E6A62]">Blouse Pairing</span>
              <h4 className="mt-1 text-sm font-bold text-[#17211F]">{blouseRecommendation.style}</h4>
              <p className="mt-1.5 text-xs text-[#52605B] leading-relaxed">{blouseRecommendation.note}</p>
            </div>

            <div className="rounded-xl border border-[#E8E2D9] bg-[#F7F4EE] p-4 shadow-xs">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#1E6A62]">Jewelry Accents</span>
              <h4 className="mt-1 text-sm font-bold text-[#17211F]">{jewelryRecommendation.style}</h4>
              <p className="mt-1.5 text-xs text-[#52605B] leading-relaxed">{jewelryRecommendation.note}</p>
            </div>

            <div className="rounded-xl border border-[#E8E2D9] bg-[#F7F4EE] p-4 shadow-xs">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#1E6A62]">Draping Style</span>
              <h4 className="mt-1 text-sm font-bold text-[#17211F]">{drapingRecommendation.style}</h4>
              <p className="mt-1.5 text-xs text-[#52605B] leading-relaxed">{drapingRecommendation.note}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-white py-12">
        <div className="section-shell">
          <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#0F766E]">Customer Reviews</p>
              <h2 className="mt-2 text-3xl font-bold font-serif text-[#17211F]">Patron Chronicles & Feedback</h2>
            </div>
            <div className="inline-flex items-center gap-2 rounded-full bg-[#FFF4E0] px-4 py-2 text-sm font-bold text-[#9A6200]">
              <Ruler className="h-4 w-4" />
              Verified feedback on drape feel, fall, and handloom texture
            </div>
          </div>

          {/* Rating Breakdown & Analytics */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 p-6 bg-[#FAF8F5] rounded-3xl border border-[#DDD8CF] mb-8">
            <div className="flex flex-col items-center justify-center border-b sm:border-b-0 sm:border-r border-[#DDD8CF] pr-0 sm:pr-6 pb-4 sm:pb-0">
              <span className="text-4xl font-serif font-bold text-[#17211F]">{averageRating}</span>
              <div className="flex gap-1 text-[#F3C56A] my-1.5">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className={`w-4 h-4 ${i < Math.round(Number(averageRating)) ? 'fill-current text-[#F3C56A]' : 'text-gray-300'}`} />
                ))}
              </div>
              <span className="text-xs text-[#71817A] font-bold">{reviews.length} Customer Reviews</span>
            </div>
            <div className="sm:col-span-2 space-y-1.5 flex flex-col justify-center">
              {[5, 4, 3, 2, 1].map((star) => {
                const count = reviews.filter((r) => r.rating === star).length;
                const pct = reviews.length ? Math.round((count / reviews.length) * 100) : (star >= 4 ? (star === 5 ? 75 : 25) : 0);
                return (
                  <div key={star} className="flex items-center gap-2 text-xs text-[#71817A]">
                    <span className="w-7 font-bold text-[#17211F]">{star}★</span>
                    <div className="flex-1 h-2 bg-[#E8E2D9] rounded-full overflow-hidden">
                      <div className="h-full bg-[#1E6A62] rounded-full transition-all duration-500" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="w-10 text-right font-mono text-[11px] font-bold text-[#17211F]">{pct}%</span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="grid gap-4">
              {reviews.length === 0 ? (
                <div className="rounded-[8px] border border-[#DDE4EA] bg-[#F5F7FA] p-8 text-center text-sm font-bold text-[#64748B]">
                  No reviews yet. Be the first to share a note.
                </div>
              ) : (
                reviews.map((review) => (
                  <article key={review.id} className="rounded-[8px] border border-[#DDE4EA] bg-white p-5 shadow-xs">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="font-black text-[#111827]">{review.userName}</p>
                        {review.verifiedBuyer ? (
                          <p className="inline-flex items-center gap-1 text-xs font-black text-[#0F766E]">
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            Verified Buyer
                          </p>
                        ) : (
                          <p className="inline-flex items-center gap-1 text-xs font-medium text-[#71817A]">
                            Shopper Note
                          </p>
                        )}
                      </div>
                      <div className="flex items-center gap-1 text-[#F2B84B]">
                        {[...Array(5)].map((_, index) => (
                          <Star key={index} className={`h-4 w-4 ${index < review.rating ? 'fill-current' : 'text-gray-300'}`} />
                        ))}
                      </div>
                    </div>
                    <p className="mt-4 text-sm font-medium leading-7 text-[#64748B]">{review.comment}</p>
                  </article>
                ))
              )}
            </div>

            <form onSubmit={handleSubmitReview} className="h-fit rounded-[8px] border border-[#DDE4EA] bg-[#F5F7FA] p-5">
              <h3 className="text-2xl font-bold text-[#111827]">Write a review</h3>
              {user ? (
                <div className="mt-5 grid gap-4">
                  <div>
                    <p className="mb-2 text-sm font-black">Rating</p>
                    <div className="flex gap-1">
                      {[1, 2, 3, 4, 5].map((value) => (
                        <button key={value} type="button" onClick={() => setReviewRating(value)} aria-label={`Set rating to ${value}`}>
                          <Star className={`h-6 w-6 ${value <= reviewRating ? 'fill-[#F2B84B] text-[#F2B84B]' : 'text-[#CBD5E1]'}`} />
                        </button>
                      ))}
                    </div>
                  </div>
                  <label className="grid gap-2 text-sm font-black">
                    Comment
                    <textarea
                      value={reviewComment}
                      onChange={(event) => setReviewComment(event.target.value)}
                      rows={5}
                      className="rounded-[8px] border border-[#DDE4EA] bg-white p-4 text-sm font-medium outline-none focus:border-[#243B6B]"
                      placeholder="Tell shoppers about fabric feel, color, and styling..."
                    />
                  </label>
                  {reviewError && <p className="text-sm font-bold text-[#E85D4F]">{reviewError}</p>}
                  <button disabled={reviewSubmitting} className="sk-button-primary w-full">
                    {reviewSubmitting ? 'Posting...' : 'Post review'}
                  </button>
                </div>
              ) : (
                <div className="mt-5 text-sm font-bold text-[#64748B]">
                  Login to share a verified review.
                  <Link to="/login" className="mt-4 sk-button-primary w-full">
                    Login
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              )}
            </form>
          </div>
        </div>
      </section>

      {visuallySimilarDrapes && visuallySimilarDrapes.length > 0 && (
        <section className="section-shell py-10 border-t border-[#DDE4EA]">
          <div className="mb-6 flex flex-col sm:flex-row sm:items-end justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-purple-100 border border-purple-300 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-purple-800 flex items-center gap-1">
                  <Sparkles className="w-3 h-3 text-purple-600" /> AI Visual Drape Matcher
                </span>
              </div>
              <h2 className="mt-2 text-2xl sm:text-3xl font-bold font-serif text-[#111827]">Visually Similar Handloom Drapes</h2>
              <p className="text-xs text-[#6b5c4d] mt-1">Matched by fabric weave texture, border luster, and color palette harmony</p>
            </div>
            <Link to="/products" className="hidden sm:inline-flex items-center gap-2 text-sm font-bold text-[#1E6A62] hover:underline">
              Explore Vault
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {visuallySimilarDrapes.map((drape) => (
              <div key={drape.id} className="group relative rounded-2xl border border-[#DDE4EA] bg-white overflow-hidden shadow-xs hover:border-[#1E6A62] hover:shadow-md transition flex flex-col justify-between">
                <div>
                  <Link to={`/products/${drape.id}`} className="block relative aspect-3/4 overflow-hidden bg-gray-100">
                    <img
                      src={drape.image || fallbackImage}
                      alt={drape.name}
                      className="h-full w-full object-cover object-top transition duration-500 group-hover:scale-105"
                    />
                    <div className="absolute top-3 left-3">
                      <span className="rounded-full bg-emerald-900/90 backdrop-blur-xs px-2.5 py-1 text-[10px] font-bold text-white shadow-xs">
                        {drape.confidence || `${drape.confidenceScore}% Match`}
                      </span>
                    </div>
                  </Link>
                  <div className="p-4 space-y-2">
                    <div className="flex items-center justify-between text-[11px] font-bold text-[#6b5c4d]">
                      <span className="uppercase tracking-wider truncate">{drape.fabric}</span>
                      <span className="text-emerald-700 shrink-0">{drape.color}</span>
                    </div>
                    <Link to={`/products/${drape.id}`}>
                      <h3 className="text-sm font-bold text-[#111827] line-clamp-1 group-hover:text-[#1E6A62] transition">
                        {drape.name}
                      </h3>
                    </Link>
                    {drape.matchReason && (
                      <p className="text-[11px] text-[#6b5c4d] line-clamp-1 italic">
                        "{drape.matchReason}"
                      </p>
                    )}
                  </div>
                </div>

                <div className="p-4 pt-0 border-t border-[#F5F7FA] mt-2 flex items-center justify-between">
                  <span className="text-sm font-bold font-serif text-[#111827]">
                    {formatPrice(drape.price)}
                  </span>
                  <Link
                    to={`/products/${drape.id}`}
                    onClick={() => eventTracker.trackRecommendationClick(drape.id, 'AI_VISUAL_SIMILAR', { source: 'product_detail' })}
                    className="px-3 py-1.5 rounded-lg bg-[#111827] hover:bg-[#1E6A62] text-white text-[11px] font-bold uppercase tracking-wider transition cursor-pointer flex items-center gap-1"
                  >
                    View Drape
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="section-shell py-12">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="text-[12px] font-black uppercase tracking-[0.18em] text-[#E85D4F]">More to try</p>
            <h2 className="mt-2 text-3xl font-bold">Similar sarees</h2>
          </div>
          <Link to="/products" className="hidden items-center gap-2 text-sm font-black text-[#243B6B] sm:inline-flex">
            View all
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        <ProductGrid products={relatedProducts} limit={4} />
      </section>

      {/* Mobile Sticky Bottom Action Bar */}
      <div className="md:hidden fixed bottom-14 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-[#DDD8CF] p-3 px-4 shadow-[0_-4px_16px_rgba(0,0,0,0.08)]">
        <div className="flex items-center justify-between gap-3 max-w-md mx-auto">
          <div>
            <p className="text-[10px] uppercase font-bold tracking-wider text-[#71817A]">Price</p>
            <p className="text-base font-bold text-[#17211F]">{formatCurrency(product.price)}</p>
          </div>
          <button
            onClick={handleAddToCart}
            className="flex-1 h-11 bg-[#1E6A62] hover:bg-[#154E48] text-white rounded-full font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-xs transition active:scale-95"
          >
            <ShoppingBag className="w-4 h-4" />
            Add to Bag
          </button>
        </div>
      </div>

      <WeaveProvenanceModal
        isOpen={provenanceModalOpen}
        onClose={() => setProvenanceModalOpen(false)}
        product={product}
      />
      <TailoringStudioModal
        isOpen={isTailoringOpen}
        onClose={() => setIsTailoringOpen(false)}
        product={product}
        currentTailoring={tailoringSpecs}
        onApplyTailoring={(updated) => setTailoringSpecs(updated)}
      />
      <AiStylistModal
        isOpen={isAiStylistOpen}
        onClose={() => setIsAiStylistOpen(false)}
        product={product}
        onCustomizeTailoring={handleCustomizeFromStylist}
      />
    </div>
  );
}
