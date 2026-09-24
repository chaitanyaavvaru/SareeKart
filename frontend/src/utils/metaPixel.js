/**
 * Lightweight, zero-dependency Meta (Facebook) Pixel integration for SareeKart.
 * Respects user privacy, redacts PII, and operates non-blockingly.
 */

class MetaPixel {
  constructor() {
    this.initialized = false;
    this.pixelId = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_META_PIXEL_ID) || null;
    this.trackedPurchases = new Set();

    if (typeof window !== 'undefined' && this.pixelId) {
      this.init(this.pixelId);
    }
  }

  /**
   * Initializes the Meta Pixel base code dynamically
   */
  init(pixelId) {
    if (this.initialized || typeof window === 'undefined' || !pixelId) return;

    !(function (f, b, e, v, n, t, s) {
      if (f.fbq) return;
      n = f.fbq = function () {
        n.callMethod ? n.callMethod.apply(n, arguments) : n.queue.push(arguments);
      };
      if (!f._fbq) f._fbq = n;
      n.push = n;
      n.loaded = !0;
      n.version = '2.0';
      n.queue = [];
      t = b.createElement(e);
      t.async = !0;
      t.src = v;
      s = b.getElementsByTagName(e)[0];
      s.parentNode.insertBefore(t, s);
    })(window, document, 'script', 'https://connect.facebook.net/en_US/fbevents.js');

    if (window.fbq) {
      window.fbq('init', pixelId);
      this.initialized = true;
    }
  }

  track(eventName, params = {}) {
    if (typeof window === 'undefined' || !window.fbq) return;
    try {
      window.fbq('track', eventName, params);
    } catch (e) {
      // Non-blocking degradation
      console.warn('[MetaPixel] Track failed:', e.message);
    }
  }

  trackCustom(eventName, params = {}) {
    if (typeof window === 'undefined' || !window.fbq) return;
    try {
      window.fbq('trackCustom', eventName, params);
    } catch (e) {
      console.warn('[MetaPixel] TrackCustom failed:', e.message);
    }
  }

  trackPageView() {
    this.track('PageView');
  }

  trackViewContent(product) {
    if (!product) return;
    this.track('ViewContent', {
      content_name: product.name || 'Luxury Saree',
      content_category: product.category?.name || product.fabric || 'Saree',
      content_ids: [product.id ? String(product.id) : ''],
      content_type: 'product',
      value: product.price || 0,
      currency: 'INR',
    });
  }

  trackSearch(query, params = {}) {
    if (!query) return;
    this.track('Search', {
      search_string: typeof query === 'string' ? query.trim() : String(query),
      content_category: params.content_category || params.category || 'All',
      ...params,
    });
  }

  trackAddToCart(item) {
    if (!item) return;
    this.track('AddToCart', {
      content_name: item.name || 'Luxury Saree',
      content_ids: [item.id ? String(item.id) : ''],
      content_type: 'product',
      value: item.price || 0,
      currency: 'INR',
    });
  }

  trackInitiateCheckout(cart) {
    if (!cart) return;
    const items = cart.items || [];
    const total = cart.totalAmount || cart.total || 0;
    this.track('InitiateCheckout', {
      num_items: items.length,
      value: total,
      currency: 'INR',
    });
  }

  trackPurchase(order) {
    if (!order) return;
    const orderId = String(order.id || order.orderId || '');
    if (orderId && this.trackedPurchases.has(orderId)) {
      return; // Prevent duplicate Purchase events on re-renders / page reloads
    }
    if (orderId) {
      this.trackedPurchases.add(orderId);
    }
    this.track('Purchase', {
      value: order.totalAmount || order.total || 0,
      currency: 'INR',
      content_type: 'product',
      order_id: orderId,
    });
  }
}

export const metaPixel = new MetaPixel();
export default metaPixel;
