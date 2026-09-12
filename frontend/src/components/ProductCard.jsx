import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { ArrowUpRight, ShoppingBag, Sparkles } from 'lucide-react';
import { addToCart } from '../redux/slices/cartSlice';
import WishlistButton from './WishlistButton';
import Rating from './Rating';
import SafeImage from './common/SafeImage';
import { useCurrency } from '../context/CurrencyContext';

const fallbackImages = [
  'https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070',
  'https://kankatala.com/cdn/shop/files/1216743146_1.webp?v=1786693084&width=1070',
  'https://kankatala.com/cdn/shop/files/1216719148_5.webp?v=1786345549&width=1070',
  'https://kankatala.com/cdn/shop/files/1216730670_1.webp?v=1786097422&width=1070',
];

function getProductImage(product) {
  const images = product.images || product.imageUrls || [];
  return images[0] || product.image || null;
}

export default function ProductCard({
  product,
  onAddSuccess,
  initialWishlisted = false,
  onWishlistToggle,
}) {
  const dispatch = useDispatch();
  const { formatPrice } = useCurrency();
  const image = useMemo(() => (
    getProductImage(product) || fallbackImages[(product.id || 0) % fallbackImages.length]
  ), [product]);

  const stock = product.stockQuantity ?? product.stock ?? 8;
  const discount = product.discount || 0;
  const originalPrice = discount > 0 ? Math.round((product.price || 0) / (1 - discount / 100)) : null;
  const badge = stock <= 3 ? 'Low stock' : product.occasion || product.categoryName || 'New edit';

  const handleAddToCart = (event) => {
    event.preventDefault();
    event.stopPropagation();
    dispatch(addToCart(product));
    onAddSuccess?.(product);
  };

  return (
    <article className="group min-w-0 bg-transparent">
      <div className="relative aspect-[0.76] overflow-hidden bg-[#E9E1D5]">
        <Link to={`/products/${product.id}`} className="block h-full w-full">
          <SafeImage
            src={image}
            fallbackSrc={fallbackImages[(product.id || 0) % fallbackImages.length]}
            alt={product.name}
            productName={product.name}
            category={product.fabric || product.categoryName || 'Handloom saree'}
            aspectRatioClass="h-full w-full"
            className="h-full w-full object-cover object-center transition duration-700 group-hover:scale-[1.035]"
          />
        </Link>

        <div className="absolute left-3 top-3 flex flex-wrap gap-2">
          <span className="inline-flex items-center gap-1 bg-white/92 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.08em] text-[#17211F] shadow-sm backdrop-blur">
            <Sparkles className="h-3 w-3 text-[#C48B3C]" />
            {badge}
          </span>
          {discount > 0 && (
            <span className="bg-[#B84F49] px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.08em] text-white">
              {discount}% off
            </span>
          )}
        </div>

        <div className="absolute right-3 top-3">
          <WishlistButton
            productId={product.id}
            initialWishlisted={initialWishlisted}
            onToggleSuccess={onWishlistToggle}
          />
        </div>

        <button
          onClick={handleAddToCart}
          className="absolute inset-x-3 bottom-3 hidden h-11 items-center justify-center gap-2 bg-white/95 text-xs font-bold uppercase tracking-[0.1em] text-[#17211F] opacity-0 shadow-lg backdrop-blur transition group-hover:flex group-hover:opacity-100"
        >
          <ShoppingBag className="h-4 w-4" />
          Add to bag
        </button>
      </div>

      <div className="grid gap-2.5 pt-4">
        <div className="flex items-center justify-between gap-3 text-[10px] font-bold uppercase tracking-[0.14em] text-[#71817A]">
          <span className="truncate">{product.fabric || product.categoryName || 'Handloom'}</span>
          <span className="shrink-0">{stock} left</span>
        </div>
        <Link to={`/products/${product.id}`} className="group/title block">
          <h3 className="line-clamp-2 min-h-[44px] font-serif text-[19px] font-medium leading-[1.15] text-[#17211F] transition group-hover/title:text-[#1E6A62]">
            {product.name}
          </h3>
        </Link>
        <div className="flex items-end justify-between gap-2 border-b border-[#DDD8CF] pb-4">
          <div>
            <div className="flex items-baseline gap-2">
              <span className="text-[15px] font-bold text-[#17211F]">{formatPrice(product.price)}</span>
              {originalPrice && originalPrice > product.price && (
                <span className="text-xs text-[#A7A19A] line-through">{formatPrice(originalPrice)}</span>
              )}
            </div>
            <Rating value={product.rating || 4.8} reviewsCount={product.reviews || 24} />
          </div>
          <Link to={`/products/${product.id}`} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#C9C1B5] text-[#1E6A62] transition hover:bg-[#1E6A62] hover:text-white" aria-label={`View ${product.name}`}>
            <ArrowUpRight className="h-4 w-4" />
          </Link>
        </div>
      </div>
    </article>
  );
}
