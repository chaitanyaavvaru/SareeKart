import {
  GUEST_CART_KEY,
  GUEST_WISHLIST_KEY,
  GUEST_CART_EXPIRATION_DAYS,
  MAX_QUANTITY_PER_SKU,
} from '../constants/cartConstants';

const EXPIRATION_MS = GUEST_CART_EXPIRATION_DAYS * 24 * 60 * 60 * 1000;

/**
 * Loads the guest cart from localStorage.
 * Enforces explicit expiration: if now > expiresAt, the cart is purged and an empty cart is returned.
 */
export function getGuestCart() {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY);
    if (!raw) {
      return { items: [], createdAt: null, updatedAt: null, expiresAt: null };
    }

    const data = JSON.parse(raw);

    // Support legacy flat array format if present
    if (Array.isArray(data)) {
      const now = Date.now();
      const envelope = {
        items: data,
        createdAt: now,
        updatedAt: now,
        expiresAt: now + EXPIRATION_MS,
      };
      saveGuestCart(envelope.items, envelope.createdAt);
      return envelope;
    }

    // Check expiration timestamp
    if (data.expiresAt && Date.now() > data.expiresAt) {
      localStorage.removeItem(GUEST_CART_KEY);
      return { items: [], createdAt: null, updatedAt: null, expiresAt: null };
    }

    return {
      items: Array.isArray(data.items) ? data.items : [],
      createdAt: data.createdAt || Date.now(),
      updatedAt: data.updatedAt || Date.now(),
      expiresAt: data.expiresAt || Date.now() + EXPIRATION_MS,
    };
  } catch (err) {
    console.warn('Failed to parse guest cart from localStorage:', err);
    localStorage.removeItem(GUEST_CART_KEY);
    return { items: [], createdAt: null, updatedAt: null, expiresAt: null };
  }
}

/**
 * Saves items to localStorage wrapped in an explicit expiration envelope.
 */
export function saveGuestCart(items, initialCreatedAt = null) {
  try {
    const now = Date.now();
    const existing = getGuestCart();
    const createdAt = initialCreatedAt || existing.createdAt || now;
    const expiresAt = now + EXPIRATION_MS;

    const envelope = {
      items: items || [],
      createdAt,
      updatedAt: now,
      expiresAt,
    };

    localStorage.setItem(GUEST_CART_KEY, JSON.stringify(envelope));
    return envelope;
  } catch (err) {
    console.error('Failed to save guest cart to localStorage:', err);
    return null;
  }
}

/**
 * Explicitly clears the guest cart from localStorage.
 */
export function clearGuestCart() {
  try {
    localStorage.removeItem(GUEST_CART_KEY);
  } catch (err) {
    console.error('Failed to clear guest cart:', err);
  }
}

/**
 * Loads the guest wishlist (array of product IDs or product objects).
 */
export function getGuestWishlist() {
  try {
    const raw = localStorage.getItem(GUEST_WISHLIST_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.warn('Failed to parse guest wishlist:', err);
    return [];
  }
}

/**
 * Saves guest wishlist to localStorage.
 */
export function saveGuestWishlist(items) {
  try {
    localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(items || []));
  } catch (err) {
    console.error('Failed to save guest wishlist:', err);
  }
}

/**
 * Clears guest wishlist from localStorage.
 */
export function clearGuestWishlist() {
  try {
    localStorage.removeItem(GUEST_WISHLIST_KEY);
  } catch (err) {
    console.error('Failed to clear guest wishlist:', err);
  }
}
