import { Link } from 'react-router-dom';
import { ArrowLeft, Compass, Home, Search, Sparkles } from 'lucide-react';
import SEO from '../components/common/SEO';

export default function NotFoundPage() {
  return (
    <div className="min-h-[70vh] bg-[#F7F4EE] py-16 px-4 sm:px-6 lg:px-8 text-[#17211F]">
      <SEO
        title="Page Not Found | SareeKart"
        description="The luxury handloom drape or page you are looking for does not exist or has been moved."
        noindex={true}
        nofollow={true}
      />

      <div className="max-w-2xl mx-auto text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[#EFE8DA] text-[#3A0F1F] mb-6">
          <Compass className="w-8 h-8 text-[#B84F49]" />
        </div>

        <p className="text-xs font-black uppercase tracking-[0.2em] text-[#B84F49] mb-2">404 — Page Not Found</p>
        <h1 className="text-3xl sm:text-4xl font-bold font-serif text-[#17211F] mb-4">
          This Drape Has Moved
        </h1>
        <p className="text-sm text-[#71817A] max-w-md mx-auto mb-8 leading-relaxed">
          The weave or collection you were looking for is unavailable, may have archived, or the link may have changed.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link
            to="/products"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-[6px] bg-[#3A0F1F] text-[#F7F4EE] text-xs font-bold uppercase tracking-wider hover:bg-[#2A0B16] transition-colors shadow-sm"
          >
            <Search className="w-4 h-4" />
            Browse Catalog
          </Link>
          <Link
            to="/"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-[6px] border border-[#DDD8CF] bg-white text-[#17211F] text-xs font-bold uppercase tracking-wider hover:bg-[#F3EFE6] transition-colors"
          >
            <Home className="w-4 h-4 text-[#71817A]" />
            Return Home
          </Link>
          <Link
            to="/stylist"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-[6px] border border-[#DDD8CF] bg-white text-[#17211F] text-xs font-bold uppercase tracking-wider hover:bg-[#F3EFE6] transition-colors"
          >
            <Sparkles className="w-4 h-4 text-[#D4AF37]" />
            Consult AI Stylist
          </Link>
        </div>

        <div className="mt-12 pt-8 border-t border-[#E8E2D5] flex items-center justify-center gap-6 text-xs text-[#71817A]">
          <Link to="/heritage-weaves" className="hover:text-[#3A0F1F] transition-colors">Heritage Weaves</Link>
          <span>•</span>
          <Link to="/artisans" className="hover:text-[#3A0F1F] transition-colors">Master Artisans</Link>
          <span>•</span>
          <Link to="/stores" className="hover:text-[#3A0F1F] transition-colors">Boutiques</Link>
          <span>•</span>
          <Link to="/saree-care" className="hover:text-[#3A0F1F] transition-colors">Saree Care</Link>
        </div>
      </div>
    </div>
  );
}
