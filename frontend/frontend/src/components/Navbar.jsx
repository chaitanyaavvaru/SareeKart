
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { User, LogOut, Menu, X, ChevronDown, Heart } from 'lucide-react';
import { logout } from '../redux/slices/authSlice';
import CartButton from './CartButton';
import SearchBar from './SearchBar';

const NAV_SECTIONS = {
  collections: ["Kanchipuram Silk", "Banarasi Silk", "Uppada Silk", "Paithani", "Pochampally", "Gadwal", "Tussar", "Organza"],
  wedding: ["Bridal Sarees", "Zari Brocade", "Kanchi Tissue", "Temple Border", "Heavy Embroidery"],
  "new arrivals": ["Just In", "Limited Edition", "Pre-order"]
};

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [activeDropdown, setActiveDropdown] = useState(null);
  const [isScrolled, setIsScrolled] = useState(false);
  const [scrollProgress, setScrollProgress] = useState(0);
  
  const dispatch = useDispatch();
  const navigate = useNavigate();
  
  const { isAuthenticated, user } = useSelector(state => state.auth);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 50);
      
      const totalScroll = document.documentElement.scrollHeight - window.innerHeight;
      if (totalScroll > 0) {
        setScrollProgress((window.scrollY / totalScroll) * 100);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const selectCategory = (category) => {
    navigate(`/products?category=${encodeURIComponent(category)}`);
    setActiveDropdown(null);
    setMobileMenuOpen(false);
  };

  const navItemClass = `px-3 py-2 text-xs font-semibold uppercase tracking-widest transition-colors relative group cursor-pointer ${
    isScrolled ? 'text-[#22181C] hover:text-[#C89B3C]' : 'text-white/90 hover:text-[#C89B3C]'
  }`;

  const iconClass = isScrolled ? 'text-[#22181C] hover:text-[#C89B3C]' : 'text-white hover:text-[#C89B3C]';

  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
      isScrolled 
        ? 'bg-[#F9F6F1]/95 backdrop-blur-md shadow-sm border-b border-[#E6DFD3] py-2' 
        : 'bg-transparent py-4'
    }`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          
          {/* Logo Left */}
          <div className="flex-shrink-0">
            <Link 
              to="/" 
              className={`text-2xl font-bold font-serif tracking-[0.2em] uppercase transition-colors ${
                isScrolled ? 'text-[#2B0F1E]' : 'text-white'
              }`}
            >
              SareeKart
            </Link>
          </div>

          {/* Menu Center */}
          <div className="hidden lg:flex items-center gap-6 h-full font-sans justify-center">
            <Link to="/" className={navItemClass}>
              Home
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-[#C89B3C] transition-all group-hover:w-full" />
            </Link>
            
            {/* Mega Dropdowns */}
            {Object.keys(NAV_SECTIONS).map((section) => (
              <div 
                key={section}
                className="relative h-full flex items-center"
                onMouseEnter={() => setActiveDropdown(section)}
                onMouseLeave={() => setActiveDropdown(null)}
              >
                <button className={`flex items-center gap-1.5 focus:outline-none cursor-pointer ${navItemClass}`}>
                  {section} <ChevronDown className="w-3 h-3" />
                </button>
                
                <AnimatePresence>
                  {activeDropdown === section && (
                    <motion.div 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: 10 }}
                      transition={{ duration: 0.2 }}
                      className="absolute top-full left-1/2 -translate-x-1/2 bg-[#F9F6F1] text-[#22181C] rounded-xl shadow-lg py-3 w-56 border border-[#E6DFD3] mt-1 overflow-hidden"
                    >
                      {NAV_SECTIONS[section].map((item) => (
                        <button
                          key={item}
                          onClick={() => selectCategory(item)}
                          className="block w-full text-left px-5 py-2.5 text-xs font-semibold hover:bg-[#FAF8F5] hover:text-[#C89B3C] transition-colors"
                        >
                          {item}
                        </button>
                      ))}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            ))}
            
            <Link to="/products" className={navItemClass}>
              Collection
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-[#C89B3C] transition-all group-hover:w-full" />
            </Link>
          </div>

          {/* Icons Right */}
          <div className="flex items-center gap-2 sm:gap-4">
            {/* Search Bar */}
            <div className="hidden md:block">
              <SearchBar isScrolled={isScrolled} />
            </div>

            {/* Wishlist Link */}
            <Link 
              to="/wishlist" 
              className={`p-2 rounded-full hover:bg-black/5 transition-all ${iconClass}`}
              title="My Wishlist"
              aria-label="Wishlist"
            >
              <Heart className="w-5 h-5" />
            </Link>

            {/* Cart Icon */}
            <CartButton isScrolled={isScrolled} />

            {/* User Profile */}
            <div className="relative group">
              {isAuthenticated ? (
                <div className="flex items-center gap-2">
                  <button className={`flex items-center gap-1.5 p-2 rounded-full hover:bg-black/5 transition-all focus:outline-none cursor-pointer ${iconClass}`}>
                    <User className="w-5 h-5" />
                    <span className="hidden sm:inline text-xs font-bold uppercase tracking-widest">{user?.firstName}</span>
                  </button>
                  
                  {/* Account Dropdown */}
                  <div className="absolute right-0 top-full hidden group-hover:block bg-[#F9F6F1] text-[#22181C] rounded-xl shadow-lg py-2 w-48 border border-[#E6DFD3] transition-all z-50">
                    <div className="px-4 py-2 border-b border-[#E6DFD3]">
                      <p className="text-[10px] text-text-muted uppercase tracking-wider">Logged in as</p>
                      <p className="text-xs font-semibold truncate text-[#2B0F1E]">{user?.email}</p>
                    </div>
                    {user?.role === 'ADMIN' && (
                      <Link to="/admin" className="block px-4 py-2 text-xs font-bold uppercase tracking-wider hover:bg-[#FAF8F5] hover:text-[#C89B3C] transition-colors">
                        Admin Panel
                      </Link>
                    )}
                    <Link to="/orders" className="block px-4 py-2 text-xs font-bold uppercase tracking-wider hover:bg-[#FAF8F5] hover:text-[#C89B3C] transition-colors">
                      My Orders
                    </Link>
                    <button 
                      onClick={() => dispatch(logout())}
                      className="w-full text-left flex items-center gap-2 px-4 py-2 text-xs font-bold uppercase tracking-wider text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                    >
                      <LogOut className="w-3.5 h-3.5" /> Sign Out
                    </button>
                  </div>
                </div>
              ) : (
                <Link 
                  to="/login"
                  className={`flex items-center gap-1.5 px-4 py-2 rounded-full border transition-all text-xs font-bold uppercase tracking-widest ${
                    isScrolled 
                      ? 'border-[#22181C] text-[#22181C] hover:bg-[#22181C] hover:text-white' 
                      : 'border-white/40 text-white hover:border-white hover:bg-white hover:text-[#2B0F1E]'
                  }`}
                >
                  <User className="w-3.5 h-3.5" /> Sign In
                </Link>
              )}
            </div>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className={`lg:hidden p-2 rounded-full hover:bg-black/5 transition-all focus:outline-none cursor-pointer ${iconClass}`}
            >
              {mobileMenuOpen ? <X className="w-5.5 h-5.5" /> : <Menu className="w-5.5 h-5.5" />}
            </button>

          </div>
        </div>
      </div>

      {/* Mobile Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden bg-[#2B0F1E] text-white border-t border-[#4A1E35] py-4 px-4 space-y-4">
          <div className="w-full">
            <SearchBar placeholder="Search sarees..." />
          </div>
          <div className="space-y-1">
            <Link 
              to="/" 
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-sm font-semibold hover:bg-white/10"
            >
              Home
            </Link>
            <Link 
              to="/products" 
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-sm font-semibold hover:bg-white/10"
            >
              Collection
            </Link>

            {Object.keys(NAV_SECTIONS).map((section) => (
              <div key={section} className="px-3 py-2">
                <p className="text-[10px] font-bold text-[#C89B3C] uppercase tracking-widest mb-1">{section}</p>
                <div className="grid grid-cols-2 gap-1 pl-2 border-l border-white/10">
                  {NAV_SECTIONS[section].map((item) => (
                    <button
                      key={item}
                      onClick={() => selectCategory(item)}
                      className="text-left py-1.5 text-xs font-medium hover:text-[#C89B3C] transition-colors truncate cursor-pointer text-white/80"
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
      {/* Scroll Progress Indicator Line */}
      <div 
        className="absolute bottom-0 left-0 h-0.5 bg-[#C89B3C] transition-all duration-75"
        style={{ width: `${scrollProgress}%` }}
      />
    </nav>
  );
}
