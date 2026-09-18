import { useEffect } from 'react';
import { CANONICAL_DOMAIN, DEFAULT_OG_IMAGE, toAbsoluteImageUrl } from '../../utils/seoUtils';

export default function SEO({
  title = "SareeKart | India's Premium Luxury Handloom Saree Platform",
  description = "Explore certified handwoven Banarasi, Kanchipuram, Uppada, and Pochampally silk sarees directly from India's master artisans.",
  canonical = null,
  noindex = false,
  nofollow = false,
  ogType = "website",
  ogImage = DEFAULT_OG_IMAGE,
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
    if (noindex) {
      metaRobots.content = nofollow ? 'noindex, nofollow' : 'noindex, follow';
    } else {
      metaRobots.content = 'index, follow';
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
  }, [title, description, canonical, noindex, ogType, ogImage, schemaData]);

  return null; // SEO component does not render any visual UI elements
}
