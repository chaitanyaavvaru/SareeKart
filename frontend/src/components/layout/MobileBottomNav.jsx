import { useLocation, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Home, Sparkles, Search, ShoppingBag, UserRound } from 'lucide-react';
import { setCartOpen } from '../../redux/slices/cartSlice';

export default function MobileBottomNav() {
  const location = useLocation();
  const dispatch = useDispatch();
  const { totalItems } = useSelector((state) => state.cart);
  const { isAuthenticated } = useSelector((state) => state.auth);

  // Hide mobile bottom nav inside admin portal
  if (location.pathname.startsWith('/admin')) {
    return null;
  }

  const navItems = [
    {
      id: 'home',
      label: 'Home',
      icon: Home,
      to: '/',
      isActive: location.pathname === '/',
    },
    {
      id: 'explore',
      label: 'Explore',
      icon: Sparkles,
      to: '/products',
      isActive: location.pathname === '/products' && !location.search.includes('search='),
    },
    {
      id: 'search',
      label: 'Search',
      icon: Search,
      to: '/products?search=',
      isActive: location.pathname === '/products' && location.search.includes('search='),
    },
    {
      id: 'bag',
      label: 'Bag',
      icon: ShoppingBag,
      onClick: () => dispatch(setCartOpen(true)),
      badge: totalItems,
      isActive: false,
    },
    {
      id: 'account',
      label: isAuthenticated ? 'Orders' : 'Sign In',
      icon: UserRound,
      to: isAuthenticated ? '/orders' : '/login',
      isActive: location.pathname === '/orders' || location.pathname === '/login',
    },
  ];

  return (
    <nav
      aria-label="Mobile Navigation"
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#3A0F1F] border-t border-[#C8A04D]/30 shadow-[0_-4px_20px_rgba(0,0,0,0.15)] pb-safe"
    >
      <div className="flex items-center justify-around h-14 max-w-md mx-auto px-2">
        {navItems.map((item) => {
          const Icon = item.icon;
          const activeClass = item.isActive ? 'text-[#C8A04D]' : 'text-[#E6DFD3] hover:text-[#C8A04D]';

          if (item.onClick) {
            return (
              <button
                key={item.id}
                type="button"
                onClick={item.onClick}
                aria-label={`Open Shopping Bag (${totalItems} items)`}
                className={`relative flex flex-col items-center justify-center flex-1 h-full py-1 text-center transition-colors ${activeClass}`}
              >
                <div className="relative">
                  <Icon className="w-5 h-5" />
                  {item.badge > 0 && (
                    <span className="absolute -top-1.5 -right-2 bg-[#C8A04D] text-[#3A0F1F] text-[10px] font-black h-4 min-w-4 px-1 rounded-full flex items-center justify-center shadow-xs">
                      {item.badge > 99 ? '99+' : item.badge}
                    </span>
                  )}
                </div>
                <span className="text-[10px] font-semibold mt-1 tracking-tight">{item.label}</span>
              </button>
            );
          }

          return (
            <Link
              key={item.id}
              to={item.to}
              aria-label={item.label}
              className={`flex flex-col items-center justify-center flex-1 h-full py-1 text-center transition-colors ${activeClass}`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[10px] font-semibold mt-1 tracking-tight">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
