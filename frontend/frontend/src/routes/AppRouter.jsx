import { Routes, Route } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import MainLayout from '../components/layout/MainLayout';
import HomePage from '../pages/Home/HomePage';
import ProductsPage from '../pages/Products/ProductsPage';
import ProductDetailPage from '../pages/ProductDetails/ProductDetailPage';
import LoginPage from '../pages/Login/LoginPage';
import RegisterPage from '../pages/Register/RegisterPage';
import ProtectedRoute from '../components/common/ProtectedRoute';
import CheckoutPage from '../pages/Checkout/CheckoutPage';
import SilkLoader from '../components/common/SilkLoader';

// Simple stub for my orders
import MyOrders from '../pages/MyOrders';
const WishlistPage = lazy(() => import('../pages/Wishlist/WishlistPage'));

// Admin Pages – lazy for bundle split (Phase 12 polish)
const AdminDashboard = lazy(() => import('../pages/Admin/AdminDashboard'));
const AdminStats = lazy(() => import('../pages/Admin/AdminStats'));
const ManageSarees = lazy(() => import('../pages/Admin/ManageSarees'));
const ManageOrders = lazy(() => import('../pages/Admin/ManageOrders'));
const ManageUsers = lazy(() => import('../pages/Admin/ManageUsers'));
const ManageCoupons = lazy(() => import('../pages/Admin/ManageCoupons'));
const ManageRefunds = lazy(() => import('../pages/Admin/ManageRefunds'));

// Import monolith if they want to access legacy views (like old admin dashboard mockup)
import App from '../App';

function AdminSuspense({ children }) {
  return <Suspense fallback={<div className="min-h-[50vh] flex items-center justify-center"><SilkLoader /></div>}>{children}</Suspense>;
}

export default function AppRouter() {
  return (
    <Routes>
      {/* Shell Layout wrapping public routes */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        
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
          path="wishlist" 
          element={
            <ProtectedRoute>
              <Suspense fallback={<div className="min-h-[40vh] flex items-center justify-center"><SilkLoader /></div>}>
                <WishlistPage />
              </Suspense>
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
        <Route index element={<AdminSuspense><AdminStats /></AdminSuspense>} />
        <Route path="products" element={<AdminSuspense><ManageSarees /></AdminSuspense>} />
        <Route path="orders" element={<AdminSuspense><ManageOrders /></AdminSuspense>} />
        <Route path="users" element={<AdminSuspense><ManageUsers /></AdminSuspense>} />
        <Route path="coupons" element={<AdminSuspense><ManageCoupons /></AdminSuspense>} />
        <Route path="refunds" element={<AdminSuspense><ManageRefunds /></AdminSuspense>} />
      </Route>

      {/* Legacy monolithic view accessible at /legacy if needed */}
      <Route path="/legacy" element={<App />} />
    </Routes>
  );
}
