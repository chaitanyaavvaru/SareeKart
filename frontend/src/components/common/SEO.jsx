import { useEffect } from 'react';
import {
  CANONICAL_DOMAIN,
  DEFAULT_OG_IMAGE,
  toAbsoluteImageUrl,
  isAutoPrivatePath,
} from '../../utils/seoUtils';

export default function SEO({
  title = "SareeKart | India's Premium Luxury Handloom Saree Platform",
  description = "Explore certified handwoven Banarasi, Kanchipuram, Uppada, and Pochampally silk sarees directly from India's master artisans.",
  canonical = null,
  noindex = false,
  nofollow = false,
  ogType = "website",
  ogImage = DEFAULT_OG_IMAGE,
  ogImageAlt = null,
  schemaData = null,
}) {
  useEffect(() => {
    // 1. Title
    document.title = title;

    // 2. Meta description
    let metaDesc = document.querySelector('meta[name="description"]');
    if (!metaDesc) {
      metaDesc = document.createElement('meta');
      metaDesc.name = 'description';
      document.head.appendChild(metaDesc);
    }
    metaDesc.content = description;

    // 3. Robots meta tag
    let metaRobots = document.querySelector('meta[name="robots"]');
    if (!metaRobots) {
      metaRobots = document.createElement('meta');
      metaRobots.name = 'robots';
      document.head.appendChild(metaRobots);
    }
    const currentPath = typeof window !== 'undefined' ? window.location.pathname : '';
    const shouldNoindex = noindex || isAutoPrivatePath(currentPath);
    if (shouldNoindex) {
      metaRobots.content = nofollow ? 'noindex, nofollow' : 'noindex, follow';
    } else {
      metaRobots.content = 'index, follow';
    }

    // 3b. Google Search Console Verification tag
    const gscToken = typeof import.meta !== 'undefined' && import.meta.env?.VITE_GSC_VERIFICATION;
    if (gscToken) {
      let metaGsc = document.querySelector('meta[name="google-site-verification"]');
      if (!metaGsc) {
        metaGsc = document.createElement('meta');
        metaGsc.name = 'google-site-verification';
        document.head.appendChild(metaGsc);
      }
      metaGsc.content = gscToken;
    }

    // 3c. Meta / Facebook Domain Verification tag
    const metaVerificationToken = typeof import.meta !== 'undefined' && import.meta.env?.VITE_META_DOMAIN_VERIFICATION;
    if (metaVerificationToken) {
      let metaFb = document.querySelector('meta[name="facebook-domain-verification"]');
      if (!metaFb) {
        metaFb = document.createElement('meta');
        metaFb.name = 'facebook-domain-verification';
        document.head.appendChild(metaFb);
      }
      metaFb.content = metaVerificationToken;
    }

    // 4. Canonical link tag
    const resolvedCanonical = canonical || `${CANONICAL_DOMAIN}${window.location.pathname}`;
    let linkCanonical = document.querySelector('link[rel="canonical"]');
    if (!linkCanonical) {
      linkCanonical = document.createElement('link');
      linkCanonical.setAttribute('rel', 'canonical');
      document.head.appendChild(linkCanonical);
    }
    linkCanonical.setAttribute('href', resolvedCanonical);

    // 5. Open Graph Tags
    const absoluteOgImage = toAbsoluteImageUrl(ogImage);
    const ogTags = {
      'og:title': title,
      'og:description': description,
      'og:type': ogType,
      'og:image': absoluteOgImage,
      'og:image:alt': ogImageAlt || `${title} - SareeKart Handloom`,
      'og:url': resolvedCanonical,
      'og:site_name': 'SareeKart',
    };

    Object.entries(ogTags).forEach(([property, content]) => {
      let tag = document.querySelector(`meta[property="${property}"]`);
      if (!tag) {
        tag = document.createElement('meta');
        tag.setAttribute('property', property);
        document.head.appendChild(tag);
      }
      tag.content = content;
    });

    // 6. Twitter Card Tags
    const twitterTags = {
      'twitter:card': 'summary_large_image',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': absoluteOgImage,
    };

    Object.entries(twitterTags).forEach(([name, content]) => {
      let tag = document.querySelector(`meta[name="${name}"]`);
      if (!tag) {
        tag = document.createElement('meta');
        tag.name = name;
        document.head.appendChild(tag);
      }
      tag.content = content;
    });

    // 7. JSON-LD Schema
    let schemaScript = document.getElementById('jsonld-schema');
    if (!schemaScript) {
      schemaScript = document.createElement('script');
      schemaScript.id = 'jsonld-schema';
      schemaScript.type = 'application/ld+json';
      document.head.appendChild(schemaScript);
    }

    const defaultSchema = {
      "@context": "https://schema.org",
      "@type": "WebSite",
      "name": "SareeKart",
      "url": CANONICAL_DOMAIN,
      "potentialAction": {
        "@type": "SearchAction",
        "target": `${CANONICAL_DOMAIN}/products?search={search_term_string}`,
        "query-input": "required name=search_term_string"
      }
    };

    if (Array.isArray(schemaData)) {
      schemaScript.textContent = JSON.stringify({
        "@context": "https://schema.org",
        "@graph": schemaData
      });
    } else {
      schemaScript.textContent = JSON.stringify(schemaData || defaultSchema);
    }
  }, [title, description, canonical, noindex, ogType, ogImage, ogImageAlt, schemaData]);

  return null; // SEO component does not render any visual UI elements
}
