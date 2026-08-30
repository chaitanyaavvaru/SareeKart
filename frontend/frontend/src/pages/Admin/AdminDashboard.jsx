import { useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { 
  LayoutDashboard, 
  ShoppingBag, 
  ClipboardList, 
  Users, 
  Globe, 
  ChevronLeft, 
  ChevronRight,
  LogOut,
  Bell,
  Tag,
  RefreshCw
} from 'lucide-react';
import { logout } from '../../redux/slices/authSlice';

const ADMIN_NAV = [
  { path: "/admin", icon: LayoutDashboard, label: "Dashboard" },
  { path: "/admin/products", icon: ShoppingBag, label: "Manage Sarees" },
  { path: "/admin/orders", icon: ClipboardList, label: "Manage Orders" },
  { path: "/admin/users", icon: Users, label: "Customers" },
  { path: "/admin/coupons", icon: Tag, label: "Coupons" },
  { path: "/admin/refunds", icon: RefreshCw, label: "Refunds" },
];

export default function AdminDashboard() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  
  const { user } = useSelector(state => state.auth);
  
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const activePath = location.pathname;

  return (
    <div className="flex min-h-screen bg-[#f7f5f2] font-sans antialiased text-text-primary">
      
      {/* ── SIDEBAR ── */}
      <aside 
        className={`bg-maroon-dark text-white min-h-screen flex flex-col transition-all duration-300 shrink-0 sticky top-0 h-screen overflow-y-auto ${
          sidebarCollapsed ? 'w-16' : 'w-64'
        }`}
      >
        {/* Sidebar Header */}
        <div className={`p-5 border-b border-white/10 flex items-center justify-between ${
          sidebarCollapsed ? 'justify-center' : ''
        }`}>
          {!sidebarCollapsed && (
            <div>
              <div className="text-xl font-bold text-gold font-serif tracking-widest">SareeKart</div>
              <div className="text-[10px] text-gold/60 uppercase tracking-widest font-semibold mt-0.5">Admin Portal</div>
            </div>
          )}
          
          <button 
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="p-1 rounded-lg hover:bg-white/10 text-gold transition-colors focus:outline-none cursor-pointer"
          >
            {sidebarCollapsed ? <ChevronRight className="w-5 h-5" /> : <ChevronLeft className="w-5 h-5" />}
          </button>
        </div>

        {/* Navigation links */}
        <nav className="flex-grow py-6 space-y-1">
          {ADMIN_NAV.map((item) => {
            const isActive = activePath === item.path || (item.path !== '/admin' && activePath.startsWith(item.path));
            const Icon = item.icon;
            
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`w-full flex items-center gap-4 py-3.5 transition-all relative ${
                  sidebarCollapsed ? 'justify-center px-0' : 'px-6'
                } ${
                  isActive 
                    ? 'bg-[#C9A227]/15 text-[#C9A227] font-bold border-l-3 border-[#C9A227] rounded-r-lg' 
                    : 'text-white/70 hover:text-white hover:bg-white/5 border-l-3 border-transparent'
                }`}
                title={sidebarCollapsed ? item.label : ""}
              >
                <Icon className={`w-5 h-5 shrink-0 ${isActive ? 'text-gold' : 'text-white/60'}`} />
                {!sidebarCollapsed && <span className="text-sm tracking-wide">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        {/* Sidebar Footer */}
        <div className="p-4 border-t border-white/10 space-y-3">
          <div className={`flex items-center gap-3 ${sidebarCollapsed ? 'justify-center' : ''}`}>
            <div className="w-9 h-9 rounded-full bg-gold/20 border border-gold/40 flex items-center justify-center font-bold text-gold shrink-0">
              {user?.firstName ? user.firstName[0].toUpperCase() : 'A'}
            </div>
            {!sidebarCollapsed && (
              <div className="min-w-0">
                <p className="text-xs font-bold text-gold truncate">{user?.firstName} {user?.lastName || 'Admin'}</p>
                <p className="text-[10px] text-white/55 truncate">{user?.email}</p>
              </div>
            )}
          </div>
          
          <button
            onClick={handleLogout}
            className={`w-full flex items-center gap-3 py-2 text-red-400 hover:text-red-300 hover:bg-red-950/20 rounded-lg transition-colors text-xs font-bold cursor-pointer ${
              sidebarCollapsed ? 'justify-center' : 'px-2'
            }`}
            title={sidebarCollapsed ? "Sign Out" : ""}
          >
            <LogOut className="w-4 h-4 shrink-0" />
            {!sidebarCollapsed && <span>Sign Out</span>}
          </button>
        </div>
      </aside>

      {/* ── MAIN CONTENT CONTAINER ── */}
      <main className="flex-grow flex flex-col min-w-0 min-h-screen">
        
        {/* Top Header – polished */}
        <header className="bg-white/70 backdrop-blur-xl supports-[backdrop-filter]:bg-white/60 border-b border-border sticky top-0 z-40 px-6 py-4 flex items-center justify-between shadow-[0_4px_24px_rgba(44,15,31,0.04)]">
          <div className="flex items-center gap-3">
            <div className="h-8 w-1 rounded-full bg-gradient-to-b from-gold to-gold-dark hidden sm:block" />
            <div>
              <span className="text-sm font-extrabold text-maroon uppercase tracking-[0.14em] font-serif">
                {ADMIN_NAV.find(n => n.path === activePath || (n.path !== '/admin' && activePath.startsWith(n.path)))?.label || "Admin Console"}
              </span>
              <p className="text-[11px] text-text-muted hidden sm:block">Manage inventory, orders, coupons & refunds</p>
            </div>
          </div>
          
          <div className="flex items-center gap-3">
            <button className="p-2 rounded-xl border border-border bg-white hover:bg-cream text-text-secondary transition-colors relative shadow-xs hover:shadow-sm group" aria-label="Notifications">
              <Bell className="w-4 h-4 group-hover:text-maroon transition-colors" />
              <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-accent-red rounded-full border-2 border-white animate-pulse"></span>
            </button>
            
            <Link
              to="/"
              className="inline-flex items-center gap-2 px-5 py-2 bg-gradient-to-br from-maroon to-maroon-dark hover:from-maroon-dark hover:to-maroon text-white rounded-xl text-xs font-bold shadow-md hover:shadow-lg hover:-translate-y-[1px] transition-all"
            >
              <Globe className="w-3.5 h-3.5" /> View Store
            </Link>
          </div>
        </header>

        {/* Content Outlet */}
        <div className="flex-grow p-6 sm:p-8 overflow-y-auto bg-[#FAF8F5]">
          <Outlet />
        </div>

      </main>

    </div>
  );
}
