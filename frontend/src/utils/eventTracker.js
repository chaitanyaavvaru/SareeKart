import api from '../api/axiosConfig';
import metaPixel from './metaPixel';

/**
 * Enterprise non-blocking customer behavior telemetry client for SareeKart.
 * Buffers events client-side, batches network requests, uses sendBeacon on navigation,
 * and maintains resilient failure isolation from core commerce transactions.
 */

const SESSION_STORAGE_KEY = 'sk_session_id';
const BATCH_FLUSH_INTERVAL_MS = 1500;
const MAX_BUFFER_SIZE = 10;
const SENSITIVE_KEY_PATTERN = /password|token|secret|creditcard|cvv|cardnumber|auth|pin/i;

class EventTracker {
  constructor() {
    this.queue = [];
    this.timer = null;
    this.sessionId = this.getOrCreateSessionId();
    this.initialized = false;

    if (typeof window !== 'undefined') {
      this.setupLifecycleListeners();
      this.initialized = true;
    }
  }

  getOrCreateSessionId() {
    if (typeof window === 'undefined') return 'sess_ssr_default';
    try {
      let id = sessionStorage.getItem(SESSION_STORAGE_KEY) || localStorage.getItem(SESSION_STORAGE_KEY);
      if (!id) {
        id = 'sess_' + (typeof crypto !== 'undefined' && crypto.randomUUID 
          ? crypto.randomUUID().replace(/-/g, '') 
          : Math.random().toString(36).substring(2, 15) + Date.now().toString(36));
        sessionStorage.setItem(SESSION_STORAGE_KEY, id);
        localStorage.setItem(SESSION_STORAGE_KEY, id);
      }
      return id;
    } catch (e) {
      return 'sess_fallback_' + Date.now();
    }
  }

  generateClientEventId() {
    return 'evt_' + (typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID().replace(/-/g, '')
      : Math.random().toString(36).substring(2, 15) + Date.now().toString(36));
  }

  setupLifecycleListeners() {
    const flushOnUnload = () => {
      this.flushBeacon();
    };

    window.addEventListener('beforeunload', flushOnUnload);
    window.addEventListener('pagehide', flushOnUnload);
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.flushBeacon();
      }
    });
  }

  sanitizeMetadata(raw) {
    if (!raw || typeof raw !== 'object') return {};
    const sanitized = {};
    for (const [key, value] of Object.entries(raw)) {
      if (SENSITIVE_KEY_PATTERN.test(key)) {
        sanitized[key] = '[REDACTED]';
      } else if (value !== undefined && typeof value !== 'function') {
        sanitized[key] = value;
      }
    }
    return sanitized;
  }

  track(eventType, entityType = null, entityId = null, metadata = {}, immediate = false) {
    // Failure isolation kill-switch for automated resilience testing
    if (typeof window !== 'undefined' && window.__DISABLE_TELEMETRY__ === true) {
      return;
    }

    try {
      const eventPayload = {
        clientEventId: this.generateClientEventId(),
        sessionId: this.sessionId,
        eventType: String(eventType).trim().toUpperCase(),
        entityType: entityType ? String(entityType).trim().toUpperCase() : null,
        entityId: entityId ? Number(entityId) : null,
        metadata: this.sanitizeMetadata(metadata),
      };

      this.queue.push(eventPayload);

      if (immediate || this.queue.length >= MAX_BUFFER_SIZE) {
        this.flush();
      } else if (!this.timer) {
        this.timer = setTimeout(() => this.flush(), BATCH_FLUSH_INTERVAL_MS);
      }
    } catch (err) {
      // Telemetry error must never bubble up or interrupt customer interaction
      if (typeof console !== 'undefined' && console.warn) {
        console.warn('Silent telemetry capture error:', err.message);
      }
    }
  }

  async flush() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    if (this.queue.length === 0) return;

    const batchToSend = [...this.queue];
    this.queue = [];

    try {
      await api.post('/events/batch', { events: batchToSend });
    } catch (error) {
      // Non-blocking: log silently without throwing to caller
      if (typeof console !== 'undefined' && console.debug) {
        console.debug('Telemetry batch delivery skipped or failed:', error.message);
      }
    }
  }

  flushBeacon() {
    if (this.queue.length === 0) return;
    const batch = [...this.queue];
    this.queue = [];

    try {
      const payload = JSON.stringify({ events: batch });
      if (typeof navigator !== 'undefined' && navigator.sendBeacon) {
        const blob = new Blob([payload], { type: 'application/json' });
        navigator.sendBeacon('/api/events/batch', blob);
      }
    } catch (e) {
      if (typeof console !== 'undefined' && console.debug) {
        console.debug('Beacon batch dispatch failed:', e?.message);
      }
    }
  }

  // =========================================================================
  // Core Event Taxonomy API
  // =========================================================================

  trackProductView(product, dwellTimeMs = 0) {
    if (!product) return;
    this.track('PRODUCT_VIEW', 'PRODUCT', product.id, {
      name: product.name,
      category: product.categoryName || product.category || 'Handloom Saree',
      fabric: product.fabric || product.fabricName,
      color: product.color || product.colorName,
      price: product.price,
      dwellTimeMs: Number(dwellTimeMs) || 0,
    });
    metaPixel.trackViewContent(product);
    if (typeof window !== 'undefined' && typeof window.gtag === 'function') {
      window.gtag('event', 'view_item', {
        currency: 'INR',
        value: product.price || 0,
        items: [{
          item_id: String(product.id || ''),
          item_name: product.name || '',
          item_category: product.categoryName || product.category || 'Saree',
          price: product.price || 0,
        }],
      });
    }
  }

  trackSearch(query, resultCountOrFilters = 0, metadata = {}) {
    if (!query && typeof resultCountOrFilters !== 'object') return;
    let resultCount = 0;
    let filters = {};
    if (typeof resultCountOrFilters === 'number') {
      resultCount = resultCountOrFilters;
      filters = metadata;
    } else if (typeof resultCountOrFilters === 'object' && resultCountOrFilters !== null) {
      filters = resultCountOrFilters;
      resultCount = Number(metadata) || 0;
    }
    this.track('SEARCH_QUERY', 'SEARCH', null, {
      query: String(query || '').trim(),
      resultCount,
      ...this.sanitizeMetadata(filters),
    }, true);
  }

  trackCategoryView(categoryIdOrSlug, categoryName = '', metadata = {}) {
    this.track('CATEGORY_VIEW', 'CATEGORY', typeof categoryIdOrSlug === 'number' ? categoryIdOrSlug : null, {
      categorySlug: String(categoryIdOrSlug),
      categoryName: categoryName || String(categoryIdOrSlug),
      ...this.sanitizeMetadata(metadata),
    });
  }

  trackAddToCart(product, quantity = 1, source = 'product_detail') {
    if (!product) return;
    this.track('ADD_TO_CART', 'PRODUCT', product.id, {
      productName: product.name,
      price: product.price,
      quantity: Number(quantity) || 1,
      fabric: product.fabric || product.fabricName,
      color: product.color || product.colorName,
      source,
    }, true);
    metaPixel.trackAddToCart(product);
    if (typeof window !== 'undefined' && typeof window.gtag === 'function') {
      window.gtag('event', 'add_to_cart', {
        currency: 'INR',
        value: (product.price || 0) * (Number(quantity) || 1),
        items: [{
          item_id: String(product.id || ''),
          item_name: product.name || '',
          item_category: product.categoryName || product.category || 'Saree',
          price: product.price || 0,
          quantity: Number(quantity) || 1,
        }],
      });
    }
  }

  trackRemoveFromCart(product, quantity = 1) {
    if (!product) return;
    this.track('REMOVE_FROM_CART', 'PRODUCT', product.id, {
      productName: product.name,
      quantity: Number(quantity) || 1,
    });
  }

  trackWishlistAdd(product, source = 'storefront') {
    if (!product) return;
    this.track('ADD_TO_WISHLIST', 'PRODUCT', product.id, {
      productName: product.name,
      price: product.price,
      source,
    });
  }

  trackWishlistRemove(product) {
    if (!product) return;
    this.track('REMOVE_FROM_WISHLIST', 'PRODUCT', product.id, {
      productName: product.name,
    });
  }

  trackCheckoutInitiated(itemCountOrSummary = {}, cartValue = 0, metadata = {}) {
    let count;
    let value;
    if (typeof itemCountOrSummary === 'object' && itemCountOrSummary !== null) {
      count = itemCountOrSummary.itemCount || 0;
      value = itemCountOrSummary.cartValue || itemCountOrSummary.subtotal || 0;
      this.track('CHECKOUT_INITIATED', 'ORDER', null, {
        itemCount: count,
        cartValue: value,
        paymentMethod: itemCountOrSummary.paymentMethod || 'COD',
        ...itemCountOrSummary,
      }, true);
    } else {
      count = Number(itemCountOrSummary) || 0;
      value = Number(cartValue) || 0;
      this.track('CHECKOUT_INITIATED', 'ORDER', null, {
        itemCount: count,
        cartValue: value,
        ...this.sanitizeMetadata(metadata),
      }, true);
    }
    metaPixel.trackInitiateCheckout({ totalAmount: value, items: Array(count) });
    if (typeof window !== 'undefined' && typeof window.gtag === 'function') {
      window.gtag('event', 'begin_checkout', {
        currency: 'INR',
        value: value,
      });
    }
  }

  trackOrderCompleted(orderIdOrObj = {}, totalAmount = 0, metadata = {}) {
    let orderId;
    let total;
    if (typeof orderIdOrObj === 'object' && orderIdOrObj !== null) {
      orderId = orderIdOrObj.id;
      total = orderIdOrObj.totalAmount || 0;
      this.track('ORDER_COMPLETED', 'ORDER', orderId, {
        orderId: orderId,
        totalAmount: total,
        itemCount: orderIdOrObj.items?.length || 1,
        paymentMethod: orderIdOrObj.paymentMethod,
        ...orderIdOrObj,
      }, true);
      metaPixel.trackPurchase(orderIdOrObj);
    } else {
      orderId = Number(orderIdOrObj);
      total = Number(totalAmount) || 0;
      this.track('ORDER_COMPLETED', 'ORDER', orderId, {
        orderId: orderId,
        totalAmount: total,
        ...this.sanitizeMetadata(metadata),
      }, true);
      metaPixel.trackPurchase({ id: orderId, totalAmount: total });
    }
    if (typeof window !== 'undefined' && typeof window.gtag === 'function') {
      window.gtag('event', 'purchase', {
        transaction_id: String(orderId || ''),
        value: total,
        currency: 'INR',
      });
    }
  }

  trackAiStylistEngage(consultationData = {}) {
    this.track('AI_STYLIST_ENGAGE', 'CONSULTATION', consultationData.productId || null, {
      occasion: consultationData.occasion,
      primaryColor: consultationData.primaryColor,
      blouseStyle: consultationData.blouseStyle,
      sareeName: consultationData.sareeName,
    });
  }

  trackVisualSearchEngage(searchData = {}) {
    this.track('VISUAL_SEARCH_ENGAGE', 'IMAGE', searchData.matchedProductId || null, {
      extractedWeave: searchData.extractedWeave,
      extractedColor: searchData.extractedColor,
      confidence: searchData.confidence,
    });
  }

  // =========================================================================
  // Phase 13 Stage 5: Full-Funnel & Auxiliary Channels
  // =========================================================================

  trackLanding(metadata = {}) {
    let referrer = '';
    let path = '/';
    if (typeof window !== 'undefined') {
      referrer = document.referrer || '';
      path = window.location.pathname;
    }
    this.track('LANDING_PAGE_VIEW', 'PAGE', null, {
      path,
      referrer,
      ...this.sanitizeMetadata(metadata),
    });
    metaPixel.trackPageView();
    if (typeof window !== 'undefined' && typeof window.gtag === 'function') {
      window.gtag('event', 'page_view', {
        page_path: path,
      });
    }
  }

  trackPaymentAttempt(paymentMethod, amount = 0, metadata = {}) {
    this.track('PAYMENT_ATTEMPT', 'PAYMENT', null, {
      paymentMethod: String(paymentMethod || 'UNKNOWN'),
      amount: Number(amount) || 0,
      ...this.sanitizeMetadata(metadata),
    }, true);
  }

  trackRecommendationClick(productId, strategy = 'CURATED', metadata = {}) {
    if (!productId) return;
    this.track('RECOMMENDATION_CLICK', 'PRODUCT', Number(productId), {
      strategy: String(strategy),
      ...this.sanitizeMetadata(metadata),
    });
  }

  trackWhatsAppEngage(action = 'CHAT_OPEN', context = {}) {
    this.track('WHATSAPP_COMMERCE_ENGAGE', 'CHANNEL', null, {
      action: String(action),
      ...this.sanitizeMetadata(context),
    });
  }

  trackTrousseauEngage(boardId = null, action = 'VIEW_BOARD', metadata = {}) {
    this.track('TROUSSEAU_ENGAGE', 'TROUSSEAU_BOARD', boardId ? Number(boardId) : null, {
      action: String(action),
      ...this.sanitizeMetadata(metadata),
    });
  }

  trackShareLinkEngage(token = '', targetType = 'TROUSSEAU', channel = 'DIRECT') {
    this.track('SHARE_LINK_ENGAGE', 'SHARE', null, {
      shareToken: String(token || ''),
      targetType: String(targetType),
      channel: String(channel),
    });
  }

  async identifyUser() {
    try {
      await api.post('/events/identify', { sessionId: this.sessionId });
    } catch (e) {
      if (typeof console !== 'undefined' && console.debug) {
        console.debug('Silent identity resolution skip:', e.message);
      }
    }
  }

  resetSession() {
    try {
      if (typeof window !== 'undefined') {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        localStorage.removeItem(SESSION_STORAGE_KEY);
        this.sessionId = this.getOrCreateSessionId();
      }
    } catch (e) {
      if (typeof console !== 'undefined' && console.debug) {
        console.debug('Storage reset failed:', e?.message);
      }
    }
  }
}

const eventTracker = new EventTracker();
export default eventTracker;
