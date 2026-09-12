import ProductCard from './ProductCard';

export default function ProductGrid({
  products = [],
  wishlistIds = [],
  onAddSuccess,
  onWishlistToggle,
  limit = 4,
}) {
  const displayProducts = products.slice(0, limit);

  return (
    <div className="grid w-full grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 xl:gap-6">
      {displayProducts.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          initialWishlisted={wishlistIds.includes(product.id)}
          onAddSuccess={onAddSuccess}
          onWishlistToggle={onWishlistToggle}
        />
      ))}
    </div>
  );
}
