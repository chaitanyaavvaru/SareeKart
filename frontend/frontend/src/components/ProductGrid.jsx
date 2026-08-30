
import ProductCard from './ProductCard';

export default function ProductGrid({ 
  products = [], 
  wishlistIds = [], 
  onAddSuccess, 
  onWishlistToggle 
}) {
  return (
    <div className="flex flex-wrap justify-center gap-8 w-full">
      {products.map((product) => (
        <div 
          key={product.id} 
          className="w-full sm:w-[calc(50%-16px)] lg:w-[calc(25%-24px)] min-w-[260px] max-w-[310px] md:min-h-[500px] lg:min-h-[540px] flex flex-col"
        >
          <ProductCard
            product={product}
            initialWishlisted={wishlistIds.includes(product.id)}
            onAddSuccess={onAddSuccess}
            onWishlistToggle={onWishlistToggle}
          />
        </div>
      ))}
    </div>
  );
}
