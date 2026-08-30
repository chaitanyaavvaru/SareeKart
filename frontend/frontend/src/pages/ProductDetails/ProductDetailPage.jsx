import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ShoppingCart, Heart, ShieldCheck, Truck, RefreshCcw, Star, ChevronRight } from 'lucide-react';
import { addToCart } from '../../redux/slices/cartSlice';
import productService from '../../services/productService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import api from '../../api/axiosConfig';
import SafeImage from '../../components/common/SafeImage';

export default function ProductDetailPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  
  const { user } = useSelector(state => state.auth);

  const [product, setProduct] = useState(null);
  const [activeImage, setActiveImage] = useState('');
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Reviews States
  const [reviews, setReviews] = useState([]);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewError, setReviewError] = useState(null);
  const [reviewSubmitting, setReviewSubmitting] = useState(false);

  // Wishlist States
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);

  const averageRating = reviews.length > 0
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
    : '4.8';

  useEffect(() => {
    const loadProductData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Load target product
        const response = await productService.getProductById(id);
        const prod = response.data;
        setProduct(prod);
        
        if (prod.images && prod.images.length > 0) {
          setActiveImage(prod.images[0]);
        } else {
          setActiveImage('https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600');
        }

        // Load reviews
        try {
          const revRes = await api.get(`/products/${id}/reviews`);
          if (revRes.data?.success) {
            setReviews(revRes.data.data || []);
          }
        } catch (e) {
          console.error("Error loading reviews", e);
        }

        // Load wishlist status if authenticated
        if (user) {
          try {
            const wishRes = await api.get('/wishlist');
            if (wishRes.data?.success) {
              const inWishlist = (wishRes.data.data || []).some(w => w.id === prod.id);
              setIsWishlisted(inWishlist);
            }
          } catch (e) {
            console.error("Error checking wishlist status", e);
          }
        }

        // Load related products from the same category
        if (prod.categoryId) {
          const relatedResponse = await productService.filterProducts({
            categoryId: prod.categoryId,
            size: 4
          });
          // Exclude the current product
          const items = (relatedResponse.data?.content || []).filter(item => item.id !== prod.id);
          setRelatedProducts(items.slice(0, 4));
        }

      } catch (err) {
        console.error('Failed to load product details', err);
        setError('Failed to retrieve product details. The saree may have been unlisted.');
      } finally {
        setLoading(false);
      }
    };

    loadProductData();
  }, [id, user]);

  const handleAddToCart = () => {
    if (product) {
      dispatch(addToCart(product));
    }
  };

  const handleToggleWishlist = async () => {
    if (!user) {
      alert("Please login to manage your wishlist.");
      return;
    }
    try {
      setWishlistLoading(true);
      if (isWishlisted) {
        await api.delete(`/wishlist/${product.id}`);
        setIsWishlisted(false);
      } else {
        await api.post(`/wishlist/${product.id}`);
        setIsWishlisted(true);
      }
    } catch (err) {
      console.error("Failed to toggle wishlist", err);
    } finally {
      setWishlistLoading(false);
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!user) {
      setReviewError("Please login to submit a review.");
      return;
    }
    if (!reviewComment.trim()) {
      setReviewError("Please enter review comments.");
      return;
    }
    try {
      setReviewSubmitting(true);
      setReviewError(null);
      const res = await api.post(`/products/${product.id}/reviews`, {
        rating: reviewRating,
        comment: reviewComment
      });
      if (res.data?.success && res.data?.data) {
        setReviews(prev => [res.data.data, ...prev]);
        setReviewComment('');
        setReviewRating(5);
      }
    } catch (err) {
      setReviewError(err.response?.data?.message || "Failed to submit review.");
    } finally {
      setReviewSubmitting(false);
    }
  };

  if (loading) return <div className="py-24 bg-[#FAF8F5] min-h-screen flex items-center justify-center"><LoadingSpinner message="Unfolding saree details..." /></div>;
  if (error) return <div className="max-w-3xl mx-auto px-4 py-20 text-center bg-[#FAF8F5] min-h-screen"><div className="bg-red-50 border border-red-200 text-red-800 p-6 rounded-xl font-medium">{error}</div><Link to="/products" className="mt-4 inline-block text-[#3A1028] hover:underline font-bold">Back to products</Link></div>;
  if (!product) return null;

  return (
    <div className="bg-[#FAF8F5] min-h-screen font-sans">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-16">
        
        {/* ── BREADCRUMB ── */}
        <nav className="text-xs sm:text-sm text-text-secondary flex items-center gap-2">
          <Link to="/" className="hover:text-[#3A1028] transition-colors">Home</Link>
          <ChevronRight className="w-3.5 h-3.5 text-text-muted" />
          <Link to="/products" className="hover:text-[#3A1028] transition-colors">Collection</Link>
          <ChevronRight className="w-3.5 h-3.5 text-text-muted" />
          <span className="text-text-muted truncate max-w-[150px] sm:max-w-xs">{product.name}</span>
        </nav>

        {/* ── PRODUCT CORE DETAIL ── */}
        <section className="grid grid-cols-1 md:grid-cols-2 gap-10 lg:gap-16">
          
          {/* Images Columns */}
          <div className="space-y-4">
            <div className="h-[500px] sm:h-[600px] overflow-hidden rounded-2xl border border-[#F4F4F4] bg-white shadow-soft group relative">
              <SafeImage 
                src={activeImage} 
                alt={product.name} 
                productName={product.name}
                category={product.fabric || 'Pure Handloom'}
                aspectRatioClass="aspect-[5/6]"
                className="w-full h-full object-cover object-top group-hover:scale-103 transition-transform duration-500"
              />
            </div>
            
            {/* Thumbnails */}
            {product.images && product.images.length > 1 && (
              <div className="flex gap-3 overflow-x-auto pb-1">
                {product.images.map((imgUrl, idx) => (
                  <button
                    key={idx}
                    onClick={() => setActiveImage(imgUrl)}
                    className={`w-20 h-26 rounded-xl overflow-hidden border-2 shrink-0 bg-white transition-all ${
                      activeImage === imgUrl ? 'border-[#3A1028] shadow-md' : 'border-[#F4F4F4] hover:border-gray-300'
                    }`}
                  >
                    <SafeImage 
                      src={imgUrl} 
                      alt="Saree detail" 
                      productName={product.name}
                      category={product.fabric || 'Detail'}
                      aspectRatioClass="aspect-[10/13]"
                      className="w-full h-full object-cover object-top" 
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Info Column */}
          <div className="flex flex-col justify-between space-y-6">
            <div className="space-y-5">
              
              {/* Badges / Fabric */}
              <div className="flex items-center gap-3">
                <span className="px-3.5 py-1 bg-[#3A1028]/5 text-[#3A1028] text-[10px] font-bold font-serif uppercase tracking-widest rounded-full border border-[#3A1028]/10">
                  {product.categoryName || 'Saree'}
                </span>
                <span className="text-xs text-text-secondary font-semibold">
                  Fabric: <span className="text-[#3A1028] font-bold">{product.fabric || 'Pure Handloom'}</span>
                </span>
              </div>

              <h1 className="text-2xl sm:text-4xl font-extrabold font-serif text-[#3A1028] leading-tight">{product.name}</h1>
              
              {/* Reviews dynamically loaded */}
              <div className="flex items-center gap-1 text-[#C9A227]">
                {[...Array(5)].map((_, i) => {
                  const isFilled = i < Math.round(Number(averageRating));
                  return <Star key={i} className={`w-4 h-4 ${isFilled ? 'fill-current' : 'text-gray-200'}`} />;
                })}
                <span className="text-xs text-text-secondary ml-2 font-medium">({averageRating} rating • {reviews.length} reviews)</span>
              </div>

              {/* Price */}
              <div className="py-3 px-5 bg-white rounded-2xl border border-[#F4F4F4] inline-flex items-center gap-3 shadow-xs">
                <span className="text-3xl font-extrabold text-[#3A1028]">₹{product.price.toLocaleString('en-IN')}</span>
                <span className="text-xs text-text-secondary line-through font-medium">₹{(product.price * 1.25).toLocaleString('en-IN', {maximumFractionDigits: 0})}</span>
                <span className="text-xs bg-[#C9A227] text-[#2C0F1F] rounded-lg px-2.5 py-1 font-bold">20% OFF</span>
              </div>

              {/* Specs Grid */}
              <div className="grid grid-cols-2 gap-4 border-y border-[#F4F4F4] py-4 my-2 text-sm text-text-secondary">
                <div>
                  <span>Occasion: </span>
                  <span className="font-bold text-text-primary capitalize">{product.occasion || 'Wedding / Festivities'}</span>
                </div>
                <div>
                  <span>Main Color: </span>
                  <span className="font-bold text-text-primary capitalize">{product.color || 'Multi'}</span>
                </div>
                <div>
                  <span>Authenticity: </span>
                  <span className="font-bold text-text-primary">100% Silk Mark certified</span>
                </div>
                <div>
                  <span>Stock Status: </span>
                  {product.stockQuantity === 0 ? (
                    <span className="text-red-600 font-bold">Out of stock</span>
                  ) : product.stockQuantity <= 5 ? (
                    <span className="text-amber-600 font-bold">Low Stock ({product.stockQuantity} left)</span>
                  ) : (
                    <span className="text-emerald-600 font-bold">In Stock</span>
                  )}
                </div>
              </div>

              {/* Description */}
              <div className="space-y-2">
                <h3 className="font-bold text-[#3A1028] text-sm sm:text-base">Product Description</h3>
                <p className="text-sm text-text-secondary leading-relaxed font-light">{product.description || 'No description provided.'}</p>
              </div>

            </div>

            {/* Action button */}
            <div className="pt-6 border-t border-[#F4F4F4] flex flex-col sm:flex-row gap-4 items-center">
              <button
                onClick={handleAddToCart}
                disabled={product.stockQuantity === 0}
                className="w-full sm:flex-1 flex items-center justify-center gap-2 py-4 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl shadow-lg transition-all disabled:bg-gray-400 disabled:cursor-not-allowed cursor-pointer tracking-wider text-xs uppercase"
              >
                <ShoppingCart className="w-5 h-5" /> Add to Shopping Bag
              </button>
              <button 
                onClick={handleToggleWishlist}
                disabled={wishlistLoading}
                className={`p-4 border rounded-xl transition-all focus:outline-none cursor-pointer ${
                  isWishlisted 
                    ? 'border-[#3A1028] bg-red-50/50 text-[#3A1028] hover:bg-red-50' 
                    : 'border-[#F4F4F4] hover:border-[#C9A227] hover:text-[#3A1028] text-text-secondary'
                }`}
                title={isWishlisted ? "Remove from Wishlist" : "Add to Wishlist"}
              >
                <Heart className={`w-5 h-5 ${isWishlisted ? 'fill-current text-[#cc0000]' : ''}`} />
              </button>
            </div>

            {/* Trust assurances */}
            <div className="grid grid-cols-3 gap-4 text-center text-[10px] sm:text-xs text-text-secondary pt-4 bg-[#FAF8F5] border border-[#F4F4F4] p-4 rounded-2xl shadow-xs">
              <div className="flex flex-col items-center gap-1.5">
                <Truck className="w-5 h-5 text-[#C9A227]" />
                <span className="font-semibold text-text-primary">Free Delivery</span>
              </div>
              <div className="flex flex-col items-center gap-1.5">
                <RefreshCcw className="w-5 h-5 text-[#C9A227]" />
                <span className="font-semibold text-text-primary">Easy 7-day Returns</span>
              </div>
              <div className="flex flex-col items-center gap-1.5">
                <ShieldCheck className="w-5 h-5 text-[#C9A227]" />
                <span className="font-semibold text-text-primary">Secured Checkout</span>
              </div>
            </div>

          </div>

        </section>

        {/* ── CUSTOMER REVIEWS ── */}
        <section className="border-t border-[#F4F4F4] pt-16 grid grid-cols-1 lg:grid-cols-12 gap-12">
          
          {/* Reviews List Column */}
          <div className="lg:col-span-7 space-y-6">
            <h2 className="text-2xl font-bold font-serif text-[#3A1028] mb-6">Customer Appreciations ({reviews.length})</h2>
            
            {reviews.length === 0 ? (
              <div className="p-8 bg-white border border-[#F4F4F4] text-center rounded-2xl text-text-secondary text-sm shadow-soft">
                No reviews have been written for this masterpiece yet. Be the first to share your experience!
              </div>
            ) : (
              <div className="space-y-6 max-h-[500px] overflow-y-auto pr-2">
                {reviews.map((rev) => (
                  <div key={rev.id} className="p-5 bg-white border border-[#F4F4F4] rounded-2xl space-y-3 shadow-soft">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-sm text-text-primary">{rev.userName}</span>
                      <span className="text-[10px] text-text-muted">{new Date(rev.createdAt).toLocaleDateString('en-IN', {year: 'numeric', month: 'long', day: 'numeric'})}</span>
                    </div>
                    <div className="flex items-center gap-1 text-[#C9A227]">
                      {[...Array(5)].map((_, idx) => (
                        <Star key={idx} className={`w-3.5 h-3.5 ${idx < rev.rating ? 'fill-current' : 'text-gray-200'}`} />
                      ))}
                    </div>
                    <p className="text-xs sm:text-sm text-text-secondary leading-relaxed italic">"{rev.comment}"</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Write a Review Column */}
          <div className="lg:col-span-5 bg-white border border-[#F4F4F4] rounded-2xl p-6 shadow-soft h-fit space-y-4">
            <h3 className="text-lg font-bold font-serif text-[#3A1028]">Share Your Experience</h3>
            
            {user ? (
              <form onSubmit={handleSubmitReview} className="space-y-4">
                <div>
                  <label className="text-[10px] uppercase font-bold text-text-secondary block mb-1.5 tracking-wider">Your Rating</label>
                  <div className="flex items-center gap-1.5">
                    {[1, 2, 3, 4, 5].map((starVal) => (
                      <button
                        key={starVal}
                        type="button"
                        onClick={() => setReviewRating(starVal)}
                        className="focus:outline-none transition-transform hover:scale-110 cursor-pointer text-gray-300"
                      >
                        <Star className={`w-6 h-6 ${starVal <= reviewRating ? 'fill-[#C9A227] text-[#C9A227]' : ''}`} />
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-[10px] uppercase font-bold text-text-secondary block mb-1.5 tracking-wider">Your Appreciation Review</label>
                  <textarea
                    value={reviewComment}
                    onChange={(e) => setReviewComment(e.target.value)}
                    placeholder="Tell us about the drape, the texture, or the colors..."
                    rows="4"
                    className="w-full bg-white border border-[#F4F4F4] focus:border-[#C9A227] rounded-xl p-3.5 text-xs sm:text-sm outline-none resize-none shadow-xs"
                  />
                </div>

                {reviewError && <p className="text-xs text-red-600 font-medium">{reviewError}</p>}

                <button
                  type="submit"
                  disabled={reviewSubmitting}
                  className="w-full py-3.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl text-xs sm:text-sm uppercase tracking-widest shadow-md transition-all disabled:opacity-50 cursor-pointer"
                >
                  {reviewSubmitting ? 'Submitting...' : 'Post Appreciation'}
                </button>
              </form>
            ) : (
              <div className="text-center py-6 space-y-3">
                <p className="text-xs text-text-secondary">You must be signed in to leave reviews and appreciations.</p>
                <Link 
                  to="/login"
                  className="inline-block px-6 py-2.5 bg-[#3A1028] hover:bg-[#2C0F1F] text-white font-bold rounded-xl text-xs uppercase tracking-widest transition-all"
                >
                  Login Account
                </Link>
              </div>
            )}
          </div>

        </section>

        {/* ── RELATED PRODUCTS ── */}
        {relatedProducts.length > 0 && (
          <section className="border-t border-[#F4F4F4] pt-16">
            <h2 className="text-2xl font-bold font-serif text-[#3A1028] mb-8">Related Masterpieces</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
              {relatedProducts.map((p) => (
                <Link 
                  to={`/products/${p.id}`}
                  key={p.id}
                  className="group bg-white border border-[#F4F4F4] rounded-2xl overflow-hidden shadow-soft hover:shadow-luxury transition-all flex flex-col h-full"
                >
                  <div className="relative h-72 overflow-hidden rounded-xl">
                    <SafeImage 
                      src={p.images && p.images.length > 0 ? p.images[0] : ""} 
                      alt={p.name} 
                      productName={p.name}
                      category={p.fabric || 'Silk'}
                      aspectRatioClass="aspect-[3/4]"
                      className="w-full h-full object-cover object-top group-hover:scale-103 transition-transform duration-500"
                    />
                  </div>
                  <div className="p-4 flex-grow flex flex-col justify-between space-y-2">
                    <div>
                      <span className="text-[10px] text-[#C9A227] font-semibold tracking-wider font-serif uppercase">{p.fabric || 'Silk'}</span>
                      <h3 className="font-bold text-text-primary text-sm line-clamp-1 mt-0.5 group-hover:text-[#3A1028] transition-colors">{p.name}</h3>
                    </div>
                    <div className="flex items-center justify-between pt-1">
                      <span className="text-sm font-extrabold text-[#3A1028]">₹{p.price.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        )}

      </div>
    </div>
  );
}
