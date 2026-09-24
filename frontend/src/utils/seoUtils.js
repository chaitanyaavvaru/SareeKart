/**
 * Utility functions for production SEO canonical URLs, metadata normalization,
 * and schema.org structured data.
 */

export const CANONICAL_DOMAIN = (
  (typeof import.meta !== 'undefined' && import.meta.env?.VITE_CANONICAL_DOMAIN) ||
  'https://sareekart.com'
).replace(/\/+$/, '');

export const DEFAULT_OG_IMAGE = `${CANONICAL_DOMAIN}/favicon.svg`;

/**
 * Determines whether a pathname belongs to a private customer, authentication,
 * checkout, or administrative area that must strictly not be indexed by search crawlers.
 *
 * @param {string} pathname
 * @returns {boolean}
 */
export function isAutoPrivatePath(pathname) {
  if (!pathname) return false;
  const p = pathname.toLowerCase();
  return p.startsWith('/admin') ||
         p.startsWith('/checkout') ||
         p.startsWith('/cart') ||
         p.startsWith('/orders') ||
         p.startsWith('/account') ||
         p.startsWith('/profile') ||
         p.startsWith('/wallet') ||
         p.startsWith('/invoices') ||
         p.startsWith('/wishlist') ||
         p === '/trousseau' || p.startsWith('/trousseau/') ||
         p === '/login' || p === '/register' ||
         p === '/forgot-password' || p === '/reset-password';
}

/**
 * Generates an absolute, canonical HTTPS URL with consistent trailing-slash policy
 * and whitelisted query parameters.
 *
 * @param {string} pathname - e.g. '/products' or '/products/42'
 * @param {URLSearchParams|string|null} searchParams - current search parameters
 * @param {string[]} allowedParams - query parameters that are part of the canonical URL (e.g. ['category'])
 * @returns {string} Fully qualified canonical URL.
 */
export function getCanonicalUrl(pathname = '/', searchParams = null, allowedParams = []) {
  let cleanPath = pathname || '/';
  if (!cleanPath.startsWith('/')) {
    cleanPath = `/${cleanPath}`;
  }
  // Trim trailing slash for paths other than root
  if (cleanPath.length > 1 && cleanPath.endsWith('/')) {
    cleanPath = cleanPath.slice(0, -1);
  }

  let queryString = '';
  if (searchParams && allowedParams.length > 0) {
    const params = typeof searchParams === 'string'
      ? new URLSearchParams(searchParams)
      : new URLSearchParams(searchParams);

    const canonicalParams = new URLSearchParams();
    for (const key of allowedParams) {
      const val = params.get(key);
      if (val && val !== 'All' && val !== 'all' && val.trim() !== '') {
        canonicalParams.set(key, val.trim());
      }
    }
    const qs = canonicalParams.toString();
    if (qs) {
      queryString = `?${qs}`;
    }
  }

  return `${CANONICAL_DOMAIN}${cleanPath}${queryString}`;
}

/**
 * Truncates meta description to target length (155-160 chars) on word boundaries.
 */
export function truncateDescription(text, maxLength = 160) {
  if (!text) return '';
  const clean = text.replace(/\s+/g, ' ').trim();
  if (clean.length <= maxLength) return clean;
  const sub = clean.slice(0, maxLength);
  const lastSpace = sub.lastIndexOf(' ');
  return (lastSpace > 100 ? sub.slice(0, lastSpace) : sub) + '...';
}

/**
 * Normalizes an image path to an absolute HTTPS URL.
 */
export function toAbsoluteImageUrl(url) {
  if (!url) return DEFAULT_OG_IMAGE;
  const trimmed = String(url).trim();
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }
  if (trimmed.startsWith('/')) {
    return `${CANONICAL_DOMAIN}${trimmed}`;
  }
  return `${CANONICAL_DOMAIN}/${trimmed}`;
}
