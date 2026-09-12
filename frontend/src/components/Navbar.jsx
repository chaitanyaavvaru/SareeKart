import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { ArrowUpRight, Award, Camera, ChevronDown, Heart, LogOut, Menu, Search, Sparkles, UserRound, Video, Wallet, X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { logout } from '../redux/slices/authSlice';
import CartButton from './CartButton';
import I18nLanguageSelector from './common/I18nLanguageSelector';
import VisualSearchModal from './common/VisualSearchModal';
import AnnouncementBar from './common/AnnouncementBar';
import VideoShoppingModal from './video/VideoShoppingModal';
import SilkMarkModal from './trust/SilkMarkModal';
import NotificationBell from './common/NotificationBell';

const categoryMenus = [
  {
    label: 'Kanchipuram',
    intro: 'Heirloom silks with luminous zari and temple borders.',
    links: ['All Kanchipuram', 'Bridal silks', 'Pastel silks', 'Pure zari', 'Korvai borders'],
  },
  {
    label: 'Weaves',
    intro: 'Distinct regional techniques, collected in one place.',
    links: ['Banarasi', 'Paithani', 'Patan Patola', 'Ikat', 'Gadwal', 'Uppada'],
  },
  {
    label: 'Fabrics',
    intro: 'Choose your drape by hand-feel, weight, and movement.',
    links: ['Silk', 'Cotton', 'Organza', 'Linen', 'Tussar', 'Chanderi'],
  },
  {
    label: 'Crafts',
    intro: 'Print, stitch, and surface work with a point of view.',
    links: ['Kalamkari', 'Bandhani', 'Printed', 'Embroidered', 'Jamdani'],
  },
  {
    label: 'Occasion',
    intro: 'A considered edit for the days you will remember.',
    links: ['Wedding', 'Festive', 'Formal', 'Casual', 'Party'],
  },
  {
    label: 'Accessories',
    intro: 'The finishing pieces that complete the drape.',
    links: ['Blouses', 'Dupattas', 'Lehengas', 'Bags & clutches', 'Dhoti'],
  },
];

const mobileLinks = [
  { label: "What's new", to: '/products' },
  { label: 'Patron Wallet & Rewards', to: '/wallet' },
  { label: 'Heritage Weaves', to: '/heritage-weaves' },
  { label: 'Our Boutiques', to: '/stores' },
  { label: 'Trousseau Planner', to: '/trousseau-planner' },
  { label: '✨ AI Drape Stylist', to: '/stylist' },
  { label: 'Master Weavers', to: '/artisans' },
  { label: 'Saree Care', to: '/saree-care' },
  { label: 'Kanchipuram', to: '/products?search=kanchipuram' },
  { label: 'Silk sarees', to: '/products?search=silk' },
  { label: 'Wedding edit', to: '/products?search=bridal' },
  { label: 'Track Order', to: '/track-order' },
];

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [openMegaMenu, setOpenMegaMenu] = useState(null);
  const [isScrolled, setIsScrolled] = useState(false);
  const [isVisualSearchOpen, setIsVisualSearchOpen] = useState(false);
  const [isVideoShoppingOpen, setIsVideoShoppingOpen] = useState(false);
  const [isSilkMarkOpen, setIsSilkMarkOpen] = useState(false);
  const [videoShoppingSaree, setVideoShoppingSaree] = useState(null);
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    const onScroll = () => setIsScrolled(window.scrollY > 20);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const handleOpenVideo = (e) => {
      setVideoShoppingSaree(e.detail || null);
      setIsVideoShoppingOpen(true);
    };
    const handleOpenSilk = () => setIsSilkMarkOpen(true);

    window.addEventListener('open-video-shopping', handleOpenVideo);
    window.addEventListener('open-silk-mark', handleOpenSilk);
    return () => {
      window.removeEventListener('open-video-shopping', handleOpenVideo);
      window.removeEventListener('open-silk-mark', handleOpenSilk);
    };
  }, []);

  const profileLabel = useMemo(() => {
    if (!isAuthenticated) return 'Sign in';
    return user?.firstName || 'Account';
  }, [isAuthenticated, user]);

  const toSearchLink = (value) => `/products?search=${encodeURIComponent(value)}`;
  const navTone = isScrolled
    ? 'border-[#DDD8CF] bg-white shadow-[0_8px_26px_rgba(23,33,31,0.08)]'
    : 'border-[#E8E2D9] bg-white';
  const mutedTone = 'text-[#4E5B56] hover:text-[#1E6A62]';

  return (
    <header className="sticky left-0 top-0 z-50 w-full">
      <AnnouncementBar
        onOpenVideoShopping={() => {
          setVideoShoppingSaree(null);
          setIsVideoShoppingOpen(true);
        }}
        onOpenSilkMark={() => setIsSilkMarkOpen(true)}
      />

      <nav className={`relative border-b transition-colors duration-300 ${navTone}`} onMouseLeave={() => setOpenMegaMenu(null)}>
        <div className="section-shell flex h-[76px] items-center justify-between gap-3 sm:h-[84px]">
          <div className="flex min-w-0 items-center gap-2 sm:gap-3">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              className="flex h-9 w-9 shrink-0 items-center justify-center border border-[#C9C1B5] hover:bg-[#F7F4EE] sm:h-10 sm:w-10"
              aria-label="Open menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <Link to="/" className="min-w-0" onClick={() => setMobileMenuOpen(false)}>
              <span className="block truncate font-serif text-[24px] font-medium leading-none text-[#17211F] sm:text-[31px]">SareeKart</span>
              <span className="mt-1 block truncate text-[8px] font-bold uppercase tracking-[0.18em] text-[#1E6A62] sm:text-[9px] sm:tracking-[0.24em]">
                the handloom edit
              </span>
            </Link>
          </div>

          <div className="hidden items-center gap-6 lg:flex">
            <Link to="/products" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone}`}>What's new</Link>
            <Link to="/heritage-weaves" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone}`}>Heritage Weaves</Link>
            <Link to="/stores" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone}`}>Boutiques</Link>
            <Link to="/trousseau-planner" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone}`}>Trousseau</Link>
            <Link to="/stylist" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone} text-[#B45309]`}>AI Stylist</Link>
            <Link to="/products?search=bridal" className={`text-xs font-bold uppercase tracking-[0.08em] transition ${mutedTone}`}>Wedding edit</Link>
          </div>

          <div className="flex items-center justify-end gap-0 text-[#17211F] sm:gap-1">
            <div className="hidden xl:block">
              <I18nLanguageSelector />
            </div>
            <button
              type="button"
              onClick={() => {
                setVideoShoppingSaree(null);
                setIsVideoShoppingOpen(true);
              }}
              className="hidden items-center gap-1.5 rounded-full border border-[#DDD8CF] bg-[#FAF8F5] px-3 py-1.5 text-xs font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition-all md:flex"
              aria-label="Book Video Shopping Appointment"
            >
              <Video className="h-3.5 w-3.5 text-[#F3C56A]" />
              <span>Video Shopping</span>
            </button>
            <button
              type="button"
              onClick={() => setIsVisualSearchOpen(true)}
              className="flex h-9 w-9 items-center justify-center transition hover:bg-[#F7F4EE] sm:h-10 sm:w-10 text-[#1E6A62]"
              title="Visual Saree Search (Gemini 2.5 Vision)"
              aria-label="Search by saree image with Gemini"
            >
              <Camera className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={() => navigate('/products')}
              className="flex h-9 w-9 items-center justify-center transition hover:bg-[#F7F4EE] sm:h-10 sm:w-10"
              aria-label="Search catalog"
            >
              <Search className="h-5 w-5" />
            </button>
            {isAuthenticated && (
              <Link
                to="/wallet"
                className="hidden sm:inline-flex items-center gap-1.5 rounded-full border border-[#DDD8CF] bg-[#FAF8F5] px-2.5 py-1 text-[11px] font-bold text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition-all mr-1"
                title="View Silk Patron Wallet & Rewards"
              >
                <Wallet className="h-3.5 w-3.5 text-[#C4A052]" />
                <span>Wallet</span>
              </Link>
            )}
            <Link
              to={isAuthenticated ? '/orders' : '/login'}
              title={profileLabel}
              className="flex h-9 w-9 items-center justify-center transition hover:bg-[#F7F4EE] sm:h-10 sm:w-10"
              aria-label={profileLabel}
            >
              <UserRound className="h-[18px] w-[18px]" />
            </Link>
            <Link to="/wishlist" className="flex h-9 w-9 items-center justify-center transition hover:bg-[#F7F4EE] sm:h-10 sm:w-10" aria-label="Saved Wishlist">
              <Heart className="h-[19px] w-[19px]" />
            </Link>
            <NotificationBell />
            <CartButton />
          </div>
        </div>

        <div className="hidden border-t border-[#E8E2D9] lg:block">
          <div className="section-shell flex items-center justify-center gap-8 xl:gap-10">
            {categoryMenus.map((menu, index) => (
              <div key={menu.label} className="relative py-4" onMouseEnter={() => setOpenMegaMenu(index)}>
                <button
                  type="button"
                  onClick={() => setOpenMegaMenu(openMegaMenu === index ? null : index)}
                  className={`inline-flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-[0.12em] transition ${openMegaMenu === index ? 'text-[#1E6A62]' : mutedTone}`}
                  aria-expanded={openMegaMenu === index}
                >
                  {menu.label}
                  <ChevronDown className={`h-3.5 w-3.5 transition ${openMegaMenu === index ? 'rotate-180' : ''}`} />
                </button>

                {openMegaMenu === index && (
                  <div className="absolute left-1/2 top-full z-50 w-[min(92vw,960px)] -translate-x-1/2 border border-[#DDD8CF] bg-white p-7 shadow-[0_20px_60px_rgba(23,33,31,0.14)]" onMouseEnter={() => setOpenMegaMenu(index)}>
                    <div className="grid gap-8 md:grid-cols-[1.1fr_2fr]">
                      <div className="border-r border-[#E8E2D9] pr-8">
                        <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-[#B84F49]">The SareeKart edit</p>
                        <h2 className="mt-3 max-w-xs font-serif text-3xl font-medium leading-tight text-[#17211F]">{menu.label}, chosen with a little more intention.</h2>
                        <p className="mt-3 max-w-xs text-sm leading-6 text-[#71817A]">{menu.intro}</p>
                        <Link to={toSearchLink(menu.label)} onClick={() => setOpenMegaMenu(null)} className="mt-6 inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.12em] text-[#1E6A62]">
                          Shop {menu.label}
                          <ArrowUpRight className="h-4 w-4" />
                        </Link>
                      </div>
                      <div className="grid content-start grid-cols-2 gap-x-8 gap-y-4 sm:grid-cols-3">
                        {menu.links.map((link) => (
                          <Link key={link} to={toSearchLink(link)} onClick={() => setOpenMegaMenu(null)} className="text-sm font-semibold text-[#4E5B56] transition hover:text-[#B84F49]">
                            {link}
                          </Link>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
            <Link to="/products" className={`py-4 text-[11px] font-bold uppercase tracking-[0.12em] transition ${mutedTone}`}>Blogs</Link>
          </div>
        </div>
      </nav>

      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[70] bg-[#17211F]/55 backdrop-blur-sm"
            role="dialog"
            aria-modal="true"
            aria-label="Mobile navigation"
            onClick={() => setMobileMenuOpen(false)}
          >
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'tween', duration: 0.25 }}
              className="flex h-full w-[min(88vw,390px)] flex-col bg-white p-6 text-[#17211F] shadow-2xl"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-[#DDD8CF] pb-5">
                <Link to="/" className="font-serif text-3xl font-medium">SareeKart</Link>
                <button type="button" onClick={() => setMobileMenuOpen(false)} className="flex h-10 w-10 items-center justify-center bg-[#F7F4EE]" aria-label="Close menu">
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="grid gap-1 overflow-y-auto py-6">
                {mobileLinks.map((link) => (
                  <Link key={link.label} to={link.to} onClick={() => setMobileMenuOpen(false)} className="border-b border-[#DDD8CF] py-4 font-serif text-2xl font-medium">
                    {link.label}
                  </Link>
                ))}
                <Link to="/products" onClick={() => setMobileMenuOpen(false)} className="border-b border-[#DDD8CF] py-4 font-serif text-2xl font-medium">All sarees</Link>
                <button
                  type="button"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    setVideoShoppingSaree(null);
                    setIsVideoShoppingOpen(true);
                  }}
                  className="flex items-center justify-between border-b border-[#DDD8CF] py-4 text-left font-serif text-2xl font-medium text-[#1E6A62]"
                >
                  <span>Video Shopping</span>
                  <Video className="h-5 w-5 text-[#F3C56A]" />
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    setIsSilkMarkOpen(true);
                  }}
                  className="flex items-center justify-between border-b border-[#DDD8CF] py-4 text-left font-serif text-2xl font-medium text-[#9B6A27]"
                >
                  <span>Silk Mark Guarantee</span>
                  <Award className="h-5 w-5 text-[#9B6A27]" />
                </button>
              </div>

              <div className="mt-auto grid gap-3 border-t border-[#DDD8CF] pt-5">
                <Link to="/products" onClick={() => setMobileMenuOpen(false)} className="inline-flex min-h-12 items-center justify-center bg-[#17211F] px-5 text-sm font-bold uppercase tracking-[0.12em] text-white">Shop the edit</Link>
                {isAuthenticated ? (
                  <button type="button" onClick={() => { setMobileMenuOpen(false); dispatch(logout()); }} className="inline-flex min-h-12 items-center justify-center gap-2 border border-[#C9C1B5] px-5 text-sm font-bold text-[#17211F]">
                    <LogOut className="h-4 w-4" /> Sign out
                  </button>
                ) : (
                  <Link to="/login" onClick={() => setMobileMenuOpen(false)} className="inline-flex min-h-12 items-center justify-center border border-[#C9C1B5] px-5 text-sm font-bold text-[#17211F]">Sign in</Link>
                )}
              </div>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>

      <VisualSearchModal
        isOpen={isVisualSearchOpen}
        onClose={() => setIsVisualSearchOpen(false)}
      />

      <VideoShoppingModal
        isOpen={isVideoShoppingOpen}
        onClose={() => setIsVideoShoppingOpen(false)}
        initialSaree={videoShoppingSaree}
      />

      <SilkMarkModal
        isOpen={isSilkMarkOpen}
        onClose={() => setIsSilkMarkOpen(false)}
      />
    </header>
  );
}
