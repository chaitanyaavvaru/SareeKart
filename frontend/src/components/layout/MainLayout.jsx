import React, { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import Navbar from './Navbar';
import Footer from './Footer';
import CartDrawer from '../cart/CartDrawer';
import AddedToCartModal from '../cart/AddedToCartModal';
import AiAssistantModal from '../common/AiAssistantModal';
import WhatsAppWidget from '../common/WhatsAppWidget';
import PwaManager from '../common/PwaManager';
import MobileBottomNav from './MobileBottomNav';
import { fetchCart, mergeGuestCart } from '../../redux/slices/cartSlice';
import { getGuestCart } from '../../utils/guestCart';

export default function MainLayout() {
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state) => state.auth);

  useEffect(() => {
    if (isAuthenticated) {
      const guestCart = getGuestCart();
      if (guestCart.items && guestCart.items.length > 0) {
        dispatch(mergeGuestCart(guestCart.items));
      } else {
        dispatch(fetchCart());
      }
    }
  }, [dispatch, isAuthenticated]);

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
