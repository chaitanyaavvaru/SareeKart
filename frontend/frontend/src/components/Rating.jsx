
import { Star } from 'lucide-react';

export default function Rating({ value = 5, reviewsCount }) {
  const rounded = Math.round(value);
  return (
    <div className="flex items-center gap-0.5 text-gold pt-1">
      {[...Array(5)].map((_, i) => (
        <Star 
          key={i} 
          className={`w-3.5 h-3.5 ${i < rounded ? 'fill-current text-gold' : 'text-gray-300'}`} 
        />
      ))}
      <span className="text-[10px] text-text-secondary ml-1 font-medium font-sans">
        ({value.toFixed(1)}{reviewsCount !== undefined && ` • ${reviewsCount} reviews`})
      </span>
    </div>
  );
}
