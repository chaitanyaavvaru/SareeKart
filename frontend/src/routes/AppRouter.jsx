import { lazy, Suspense } from 'react';
import { Navigate, Routes, Route } from 'react-router-dom';
import MainLayout from '../components/layout/MainLayout';
import ProtectedRoute from '../components/common/ProtectedRoute';

const HomePage = lazy(() => import('../pages/Home/HomePage'));
const ProductsPage = lazy(() => import('../pages/Products/ProductsPage'));
const ProductDetailPage = lazy(() => import('../pages/ProductDetails/ProductDetailPage'));
const LoginPage = lazy(() => import('../pages/Login/LoginPage'));
const RegisterPage = lazy(() => import('../pages/Register/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('../pages/Auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('../pages/Auth/ResetPasswordPage'));
const CheckoutPage = lazy(() => import('../pages/Checkout/CheckoutPage'));
const MyOrders = lazy(() => import('../pages/MyOrders'));
const ArtisansPage = lazy(() => import('../pages/Artisans/ArtisansPage'));
const WishlistPage = lazy(() => import('../pages/Wishlist/WishlistPage'));
const TrackOrderPage = lazy(() => import('../pages/Orders/TrackOrderPage'));
const StoresPage = lazy(() => import('../pages/Stores/StoresPage'));
const HeritageWeavesPage = lazy(() => import('../pages/Heritage/HeritageWeavesPage'));
const SareeCarePage = lazy(() => import('../pages/Care/SareeCarePage'));
const TrousseauPlannerPage = lazy(() => import('../pages/Bridal/TrousseauPlannerPage'));
const StylistStudioPage = lazy(() => import('../pages/Stylist/StylistStudioPage'));
const CartPage = lazy(() => import('../pages/Cart'));

// Admin Pages
const AdminDashboard = lazy(() => import('../pages/Admin/AdminDashboard'));
const AdminStats = lazy(() => import('../pages/Admin/AdminStats'));
const ManageSarees = lazy(() => import('../pages/Admin/ManageSarees'));
const ManageCategories = lazy(() => import('../pages/Admin/ManageCategories'));
const ManageOrders = lazy(() => import('../pages/Admin/ManageOrders'));
const ManageUsers = lazy(() => import('../pages/Admin/ManageUsers'));
const ManageCoupons = lazy(() => import('../pages/Admin/ManageCoupons'));
const ManageInventory = lazy(() => import('../pages/Admin/ManageInventory'));
const ManageFinance = lazy(() => import('../pages/Admin/ManageFinance'));
const ManageCMS = lazy(() => import('../pages/Admin/ManageCMS'));
const SecurityDashboard = lazy(() => import('../pages/Admin/SecurityDashboard'));
const PerformanceDashboard = lazy(() => import('../pages/Admin/PerformanceDashboard'));
const QADashboard = lazy(() => import('../pages/Admin/QADashboard'));
const DevOpsDashboard = lazy(() => import('../pages/Admin/DevOpsDashboard'));
const OperationsVault = lazy(() => import('../pages/Admin/OperationsVault'));
const AiRecommendationDashboard = lazy(() => import('../pages/Admin/AiRecommendationDashboard'));
const AiDemandDashboard = lazy(() => import('../pages/Admin/AiDemandDashboard'));
const AiPricingDashboard = lazy(() => import('../pages/Admin/AiPricingDashboard'));
const AiVisualSearchDashboard = lazy(() => import('../pages/Admin/AiVisualSearchDashboard'));
const AiStylistDashboard = lazy(() => import('../pages/Admin/AiStylistDashboard'));
const ApprovalCenter = lazy(() => import('../pages/Admin/ApprovalCenter'));
const ExcelTransactionCenter = lazy(() => import('../pages/Admin/ExcelTransactionCenter'));
const AnalyticsDashboard = lazy(() => import('../pages/Admin/AnalyticsDashboard'));
const ManageReviews = lazy(() => import('../pages/Admin/ManageReviews'));
const InvoicesPage = lazy(() => import('../pages/Invoices'));
const ManageReturns = lazy(() => import('../pages/Admin/ManageReturns'));
const WalletPage = lazy(() => import('../pages/Wallet/WalletPage'));
const ManageWallets = lazy(() => import('../pages/Admin/ManageWallets'));
const ManageLogistics = lazy(() => import('../pages/Admin/ManageLogistics'));
const WhatsAppConsole = lazy(() => import('../pages/Admin/WhatsAppConsole'));


function RouteLoading() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center bg-[#F7F4EE] text-[#71817A]">
      <div className="text-center">
        <div className="mx-auto h-8 w-8 animate-spin border-2 border-[#DDD8CF] border-t-[#1E6A62]" />
        <p className="mt-4 text-xs font-bold uppercase tracking-[0.14em]">Loading workspace</p>
      </div>
    </div>
  );
}

export default function AppRouter() {
  return (
    <Suspense fallback={<RouteLoading />}>
      <Routes>
      {/* Shell Layout wrapping public routes */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="artisans" element={<ArtisansPage />} />
        <Route path="wishlist" element={<WishlistPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route path="track-order" element={<TrackOrderPage />} />
        <Route path="orders/:id/track" element={<TrackOrderPage />} />
        <Route path="stores" element={<StoresPage />} />
        <Route path="heritage-weaves" element={<HeritageWeavesPage />} />
        <Route path="saree-care" element={<SareeCarePage />} />
        <Route path="trousseau-planner" element={<TrousseauPlannerPage />} />
        <Route path="stylist" element={<StylistStudioPage />} />
        
        {/* Protected Customer Routes */}
        <Route 
          path="checkout" 
          element={
            <ProtectedRoute>
              <CheckoutPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="orders" 
          element={
            <ProtectedRoute>
              <MyOrders />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="wallet" 
          element={
            <ProtectedRoute>
              <WalletPage />
            </ProtectedRoute>
          } 
        />
      </Route>

      {/* Protected Admin Routes */}
      <Route 
        path="/admin" 
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminStats />} />
        <Route path="analytics" element={<AnalyticsDashboard />} />
        <Route path="products" element={<ManageSarees />} />
        <Route path="categories" element={<ManageCategories />} />
        <Route path="orders" element={<ManageOrders />} />
        <Route path="users" element={<ManageUsers />} />
        <Route path="coupons" element={<ManageCoupons />} />
        <Route path="inventory" element={<ManageInventory />} />
        <Route path="excel-transactions" element={<ExcelTransactionCenter />} />
        <Route path="finance" element={<ManageFinance />} />
        <Route path="cms" element={<ManageCMS />} />
        <Route path="approvals" element={<ApprovalCenter />} />
        <Route path="reviews" element={<ManageReviews />} />
        <Route path="invoices" element={<InvoicesPage />} />
        <Route path="returns" element={<ManageReturns />} />
        <Route path="wallets" element={<ManageWallets />} />
        <Route path="logistics" element={<ManageLogistics />} />
        <Route path="whatsapp" element={<WhatsAppConsole />} />

        <Route path="security" element={<SecurityDashboard />} />
        <Route path="performance" element={<PerformanceDashboard />} />
        <Route path="qa" element={<QADashboard />} />
        <Route path="devops" element={<DevOpsDashboard />} />
        <Route path="operations" element={<OperationsVault />} />
        <Route path="ai-recommendations" element={<AiRecommendationDashboard />} />
        <Route path="ai-demand" element={<AiDemandDashboard />} />
        <Route path="ai-pricing" element={<AiPricingDashboard />} />
        <Route path="ai-visual-search" element={<AiVisualSearchDashboard />} />
        <Route path="ai-stylist" element={<AiStylistDashboard />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />

      </Routes>
    </Suspense>
  );
}
