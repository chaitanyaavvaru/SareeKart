import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';
import CartDrawer from '../cart/CartDrawer';
import AddedToCartModal from '../cart/AddedToCartModal';
import AiAssistantModal from '../common/AiAssistantModal';
import WhatsAppWidget from '../common/WhatsAppWidget';
import PwaManager from '../common/PwaManager';
import MobileBottomNav from './MobileBottomNav';

export default function MainLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-[#F7F4EE] text-[#17211F]">
      <Navbar />

      <main className="flex-grow pb-16 md:pb-0">
        <Outlet />
      </main>

      <CartDrawer />
      <AddedToCartModal />
      <AiAssistantModal />
      <WhatsAppWidget />
      <PwaManager />
      <MobileBottomNav />
      <Footer />
    </div>
  );
}
