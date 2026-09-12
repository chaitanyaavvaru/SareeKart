import { Fragment, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  BadgeIndianRupee,
  BadgePercent,
  BarChart3,
  Bell,
  Boxes,
  CheckCheck,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Coins,
  FileSpreadsheet,
  FileText,
  FlaskConical,
  FolderTree,
  Gauge,
  Globe,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquare,
  Receipt,
  Rocket,
  RotateCcw,
  ScanSearch,
  ServerCog,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  TrendingUp,
  Truck,
  Users,
  WalletCards,
  WandSparkles,
  X,
} from 'lucide-react';
import { logout } from '../../redux/slices/authSlice';
import SEO from '../../components/common/SEO';

const ADMIN_NAV = [
  { path: '/admin', icon: LayoutDashboard, label: 'Dashboard', group: 'Store' },
  { path: '/admin/products', icon: ShoppingBag, label: 'Sarees', group: 'Store' },
  { path: '/admin/categories', icon: FolderTree, label: 'Categories', group: 'Store' },
  { path: '/admin/orders', icon: ClipboardList, label: 'Orders', group: 'Store' },
  { path: '/admin/users', icon: Users, label: 'Customers', group: 'Store' },
  { path: '/admin/coupons', icon: BadgePercent, label: 'Coupons', group: 'Commerce' },
  { path: '/admin/inventory', icon: Boxes, label: 'Inventory', group: 'Commerce' },
  { path: '/admin/returns', icon: RotateCcw, label: 'Returns & Exchanges', group: 'Commerce' },
  { path: '/admin/wallets', icon: Coins, label: 'Patron Wallets', group: 'Commerce' },
  { path: '/admin/logistics', icon: Truck, label: 'Logistics & SLAs', group: 'Commerce' },
  { path: '/admin/whatsapp', icon: MessageSquare, label: 'WhatsApp Dispatch', group: 'Commerce' },
  { path: '/admin/reviews', icon: MessageSquare, label: 'Reviews', group: 'Commerce' },
  { path: '/admin/analytics', icon: BarChart3, label: 'Analytics', group: 'Commerce' },
  { path: '/admin/excel-transactions', icon: FileSpreadsheet, label: 'Excel Engine', group: 'Commerce' },
  { path: '/admin/finance', icon: WalletCards, label: 'Finance', group: 'Commerce' },
  { path: '/admin/invoices', icon: Receipt, label: 'Invoices', group: 'Commerce' },
  { path: '/admin/cms', icon: FileText, label: 'CMS', group: 'Commerce' },
  { path: '/admin/approvals', icon: CheckCheck, label: 'Approvals', group: 'Operations' },
  { path: '/admin/security', icon: ShieldCheck, label: 'Security', group: 'Operations' },
  { path: '/admin/performance', icon: Gauge, label: 'Performance', group: 'Operations' },
  { path: '/admin/qa', icon: FlaskConical, label: 'QA', group: 'Operations' },
  { path: '/admin/devops', icon: ServerCog, label: 'DevOps', group: 'Operations' },
  { path: '/admin/operations', icon: Rocket, label: 'Go-Live', group: 'Operations' },
  { path: '/admin/ai-recommendations', icon: Sparkles, label: 'Recommendations', group: 'AI Studio' },
  { path: '/admin/ai-demand', icon: TrendingUp, label: 'Demand', group: 'AI Studio' },
  { path: '/admin/ai-pricing', icon: BadgeIndianRupee, label: 'Pricing', group: 'AI Studio' },
  { path: '/admin/ai-visual-search', icon: ScanSearch, label: 'Visual Search', group: 'AI Studio' },
  { path: '/admin/ai-stylist', icon: WandSparkles, label: 'Stylist', group: 'AI Studio' },
];

export default function AdminDashboard() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const activePath = location.pathname;
  const activeItem = ADMIN_NAV.find((item) => item.path === activePath || (item.path !== '/admin' && activePath.startsWith(item.path))) || ADMIN_NAV[0];

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const closeMobileNav = () => setMobileNavOpen(false);

  return (
    <div className="flex min-h-dvh bg-[#F7F4EE] text-[#17211F]">
      <SEO
        title="Admin Dashboard | SareeKart"
        description="SareeKart admin workspace for catalog, orders, customer, inventory, and sales operations."
      />

      {mobileNavOpen && (
        <button
          type="button"
          aria-label="Close admin navigation"
          onClick={closeMobileNav}
          className="fixed inset-0 z-40 bg-[#17211F]/45 lg:hidden"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-[272px] -translate-x-full flex-col bg-[#17211F] text-white shadow-[12px_0_40px_rgba(23,33,31,0.15)] transition-[width,transform] duration-300 lg:sticky lg:top-0 lg:h-dvh lg:translate-x-0 lg:shadow-none ${sidebarCollapsed ? 'lg:w-[88px]' : 'lg:w-[272px]'} ${mobileNavOpen ? 'translate-x-0' : ''}`}
        aria-label="Admin navigation sidebar"
      >
        <div className={`flex min-h-[84px] items-center border-b border-white/10 px-5 ${sidebarCollapsed ? 'lg:justify-center' : 'justify-between'}`}>
          <Link to="/admin" onClick={closeMobileNav} className={`min-w-0 ${sidebarCollapsed ? 'lg:hidden' : ''}`}>
            <span className="block truncate font-serif text-2xl font-medium">SareeKart</span>
            <span className="mt-1 block truncate text-[9px] font-bold uppercase tracking-[0.22em] text-[#F3C56A]">Sales workspace</span>
          </Link>
          <button
            type="button"
            onClick={() => setSidebarCollapsed((value) => !value)}
            aria-label={sidebarCollapsed ? 'Expand sidebar navigation' : 'Collapse sidebar navigation'}
            className="hidden h-9 w-9 shrink-0 items-center justify-center border border-white/15 text-white/75 transition hover:border-[#F3C56A] hover:text-[#F3C56A] lg:flex"
          >
            {sidebarCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>
          <button type="button" onClick={closeMobileNav} aria-label="Close admin navigation" className="flex h-9 w-9 items-center justify-center text-white/75 hover:text-white lg:hidden">
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="min-h-0 flex-1 overflow-y-auto py-5" aria-label="Admin portal links">
          {ADMIN_NAV.map((item, index) => {
            const isActive = activePath === item.path || (item.path !== '/admin' && activePath.startsWith(item.path));
            const Icon = item.icon;

            return (
              <Fragment key={item.path}>
                {!sidebarCollapsed && (index === 0 || ADMIN_NAV[index - 1].group !== item.group) && (
                  <div className="px-6 pb-2 pt-5 text-[10px] font-black uppercase tracking-[0.2em] text-white/35">{item.group}</div>
                )}
                <Link
                  to={item.path}
                  onClick={closeMobileNav}
                  title={sidebarCollapsed ? item.label : undefined}
                  className={`relative flex min-h-11 items-center gap-3 border-l-2 px-5 text-sm font-semibold transition ${sidebarCollapsed ? 'lg:justify-center lg:px-0' : ''} ${isActive ? 'border-[#F3C56A] bg-white/10 text-white' : 'border-transparent text-white/62 hover:bg-white/6 hover:text-white'}`}
                >
                  <Icon className={`h-[18px] w-[18px] shrink-0 ${isActive ? 'text-[#F3C56A]' : 'text-white/55'}`} />
                  {!sidebarCollapsed && <span className="truncate">{item.label}</span>}
                </Link>
              </Fragment>
            );
          })}
        </nav>

        <div className={`border-t border-white/10 p-4 ${sidebarCollapsed ? 'lg:px-3' : ''}`}>
          <div className={`flex items-center gap-3 ${sidebarCollapsed ? 'lg:justify-center' : ''}`}>
            <div className="flex h-9 w-9 shrink-0 items-center justify-center bg-[#1E6A62] text-sm font-bold text-white">
              {user?.firstName ? user.firstName[0].toUpperCase() : 'A'}
            </div>
            {!sidebarCollapsed && (
              <div className="min-w-0">
                <p className="truncate text-xs font-bold text-white">{user?.firstName || 'Admin'} {user?.lastName || ''}</p>
                <p className="truncate text-[10px] text-white/55">{user?.email || 'Store operator'} &bull; <span className="font-bold text-[#F3C56A]">{user?.role || 'ADMIN'}</span></p>
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={handleLogout}
            title={sidebarCollapsed ? 'Sign out' : undefined}
            className={`mt-4 flex min-h-10 w-full items-center gap-3 border border-white/10 px-3 text-xs font-bold text-white/70 transition hover:border-[#B84F49] hover:bg-[#B84F49]/15 hover:text-white ${sidebarCollapsed ? 'lg:justify-center lg:px-0' : ''}`}
          >
            <LogOut className="h-4 w-4 shrink-0" />
            {!sidebarCollapsed && <span>Sign out</span>}
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <header className="sticky top-0 z-30 border-b border-[#DDD8CF] bg-white/95 px-4 py-4 shadow-[0_8px_24px_rgba(23,33,31,0.05)] backdrop-blur sm:px-6 lg:px-8">
          <div className="flex items-center justify-between gap-4">
            <div className="flex min-w-0 items-center gap-3">
              <button type="button" onClick={() => setMobileNavOpen(true)} aria-label="Open admin navigation" className="flex h-10 w-10 shrink-0 items-center justify-center border border-[#C9C1B5] text-[#17211F] hover:bg-[#F7F4EE] lg:hidden">
                <Menu className="h-5 w-5" />
              </button>
              <div className="min-w-0">
                <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-[#1E6A62]">SareeKart / {activeItem.group}</p>
                <h1 className="truncate font-serif text-2xl font-medium text-[#17211F] sm:text-3xl">{activeItem.label}</h1>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              <button type="button" aria-label="View notifications" className="relative flex h-10 w-10 items-center justify-center border border-[#DDD8CF] text-[#4E5B56] transition hover:border-[#1E6A62] hover:text-[#1E6A62]">
                <Bell className="h-4 w-4" />
                <span className="absolute right-2 top-2 h-1.5 w-1.5 bg-[#B84F49]" />
              </button>
              <Link to="/" className="inline-flex h-10 items-center gap-2 bg-[#17211F] px-3 text-xs font-bold uppercase tracking-[0.08em] text-white transition hover:bg-[#1E6A62] sm:px-4">
                <Globe className="h-4 w-4 text-[#F3C56A]" />
                <span className="hidden sm:inline">View store</span>
              </Link>
            </div>
          </div>
        </header>

        <div className="min-h-[calc(100dvh-97px)] bg-[#F7F4EE] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
