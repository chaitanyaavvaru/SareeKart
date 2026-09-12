import React, { useState, useEffect, useRef } from 'react';
import { Bell, Check, CheckCheck, Clock, Truck, Package, AlertTriangle, ExternalLink, X, Sparkles, MessageSquare } from 'lucide-react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../../api/axiosConfig';

export default function NotificationBell() {
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const popoverRef = useRef(null);

  const fetchNotifications = async () => {
    if (!isAuthenticated) {
      // Local fallback telemetry for guest visitors
      setNotifications([
        {
          id: 'demo-1',
          title: 'Royal Heritage Edit Live',
          message: 'Welcome to SareeKart. Explore authentic Kanchipuram and Banarasi handlooms.',
          type: 'ORDER_PLACED',
          isRead: false,
          linkUrl: '/products',
          createdAt: new Date().toISOString()
        }
      ]);
      setUnreadCount(1);
      return;
    }

    try {
      const res = await api.get('/notifications');
      if (res.data?.success && Array.isArray(res.data.data)) {
        const items = res.data.data;
        setNotifications(items);
        setUnreadCount(items.filter((n) => !n.isRead).length);
      }
    } catch (err) {
      console.warn('Could not fetch notifications telemetry', err?.message);
    }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 25000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  // Close popover when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAsRead = async (id, e) => {
    e.stopPropagation();
    if (!isAuthenticated) {
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
      return;
    }

    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Failed to mark notification read', err);
    }
  };

  const handleMarkAllRead = async () => {
    if (!isAuthenticated) {
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
      return;
    }

    try {
      await api.put('/notifications/read-all');
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all notifications read', err);
    }
  };

  const handleNotificationClick = (notif) => {
    setOpen(false);
    if (!notif.isRead && isAuthenticated) {
      api.put(`/notifications/${notif.id}/read`).catch(() => {});
      setNotifications((prev) => prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    }
    if (notif.linkUrl) {
      navigate(notif.linkUrl);
    }
  };

  const getIconForType = (type) => {
    switch (type) {
      case 'SHIPMENT_DISPATCHED':
        return <Truck className="w-4 h-4 text-emerald-700" />;
      case 'ORDER_DELIVERED':
        return <Package className="w-4 h-4 text-[#1E6A62]" />;
      case 'LOW_STOCK':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      case 'STOCK_TRANSFER':
        return <Truck className="w-4 h-4 text-purple-600" />;
      case 'REVIEW_SUBMITTED':
        return <MessageSquare className="w-4 h-4 text-blue-600" />;
      default:
        return <Sparkles className="w-4 h-4 text-[#C8A04D]" />;
    }
  };

  return (
    <div className="relative inline-block" ref={popoverRef}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="relative flex h-9 w-9 sm:h-10 sm:w-10 items-center justify-center transition hover:bg-[#F7F4EE] rounded-full text-[#17211F] cursor-pointer"
        aria-label="View notifications and dispatch telemetry"
        title="Notifications & Telemetry"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 flex h-4 min-w-4 px-1 items-center justify-center rounded-full bg-[#E85D4F] text-[9px] font-black text-white shadow-xs animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 mt-2 w-80 sm:w-96 rounded-3xl bg-white border border-[#DDD8CF] shadow-[0_20px_60px_rgba(23,33,31,0.16)] z-50 overflow-hidden text-left font-sans"
          >
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-[#F7F4EE] bg-[#FAF8F5]">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold font-serif text-[#17211F]">Notifications</h3>
                {unreadCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full bg-[#1E6A62] text-white text-[10px] font-bold">
                    {unreadCount} unread
                  </span>
                )}
              </div>
              <div className="flex items-center gap-2">
                {unreadCount > 0 && (
                  <button
                    onClick={handleMarkAllRead}
                    className="text-[11px] font-bold text-[#1E6A62] hover:underline flex items-center gap-1 cursor-pointer"
                    title="Mark all as read"
                  >
                    <CheckCheck className="w-3.5 h-3.5" /> Mark read
                  </button>
                )}
                <button
                  onClick={() => setOpen(false)}
                  className="p-1 rounded-full text-[#71817A] hover:bg-[#F7F4EE] cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* List */}
            <div className="max-h-[380px] overflow-y-auto divide-y divide-[#F7F4EE]">
              {notifications.length === 0 ? (
                <div className="p-8 text-center text-xs text-[#71817A] space-y-2">
                  <Bell className="w-8 h-8 text-[#DDD8CF] mx-auto" />
                  <p className="font-bold text-[#17211F]">All caught up</p>
                  <p>No new orders, tracking updates, or system alerts at this moment.</p>
                </div>
              ) : (
                notifications.map((notif) => (
                  <div
                    key={notif.id}
                    onClick={() => handleNotificationClick(notif)}
                    className={`p-4 transition-colors cursor-pointer flex gap-3 items-start ${
                      notif.isRead ? 'bg-white hover:bg-[#FAF8F5]' : 'bg-[#F7F4EE]/50 hover:bg-[#F7F4EE]'
                    }`}
                  >
                    <div className="w-8 h-8 rounded-full bg-white border border-[#DDD8CF] flex items-center justify-center shrink-0 shadow-xs mt-0.5">
                      {getIconForType(notif.type)}
                    </div>
                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-start justify-between gap-1">
                        <p className={`text-xs ${notif.isRead ? 'font-medium text-[#17211F]' : 'font-bold text-[#17211F]'}`}>
                          {notif.title}
                        </p>
                        {!notif.isRead && (
                          <button
                            onClick={(e) => handleMarkAsRead(notif.id, e)}
                            className="p-1 text-[#71817A] hover:text-[#1E6A62] rounded-full shrink-0"
                            title="Mark as read"
                          >
                            <Check className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                      <p className="text-[11px] text-[#71817A] leading-relaxed line-clamp-2">
                        {notif.message}
                      </p>
                      <div className="flex items-center justify-between pt-1 text-[10px] text-[#A2ADA8]">
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {notif.createdAt
                            ? new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                            : 'Just now'}
                        </span>
                        {notif.linkUrl && (
                          <span className="text-[#1E6A62] font-bold flex items-center gap-0.5 hover:underline">
                            View <ExternalLink className="w-2.5 h-2.5" />
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Footer */}
            <div className="p-3 bg-[#FAF8F5] border-t border-[#F7F4EE] text-center">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                SareeKart Dispatch & Operations Telemetry
              </span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
