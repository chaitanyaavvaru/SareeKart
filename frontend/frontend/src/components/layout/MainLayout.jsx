import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';
import CartDrawer from '../cart/CartDrawer';
import AddedToCartModal from '../cart/AddedToCartModal';

export default function MainLayout() {
  return (
    <div className="flex flex-col min-h-screen bg-cream">
      {/* Navigation Header */}
      <Navbar />

      {/* Main page content area */}
      <main className="flex-grow">
        <Outlet />
      </main>

      {/* Overlay Shopping Cart Drawer */}
      <CartDrawer />

      {/* Cart Success Popup Modal */}
      <AddedToCartModal />

      {/* Footer info section */}
      <Footer />
    </div>
  );
}
