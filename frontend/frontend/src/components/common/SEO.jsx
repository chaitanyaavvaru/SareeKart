import { useEffect } from 'react';

export default function SEO({ 
  title = "SareeKart | India's Premium Luxury Saree Platform", 
  description = "Explore handwoven Banarasi, Kanchipuram, Uppada, and Pochampally silk sarees directly from India's master artisans.",
  ogType = "website",
  ogImage = "https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250",
  schemaData = null
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

    // 3. Open Graph Tags
    const ogTags = {
      'og:title': title,
      'og:description': description,
      'og:type': ogType,
      'og:image': ogImage,
      'og:url': window.location.href,
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

    // 4. Twitter Card Tags
    const twitterTags = {
      'twitter:card': 'summary_large_image',
      'twitter:title': title,
      'twitter:description': description,
      'twitter:image': ogImage,
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

    // 5. JSON-LD Schema
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
      "url": window.location.origin,
      "potentialAction": {
        "@type": "SearchAction",
        "target": `${window.location.origin}/products?search={search_term_string}`,
        "query-input": "required name=search_term_string"
      }
    };

    schemaScript.textContent = JSON.stringify(schemaData || defaultSchema);

    // Clean up function
    return () => {
      // Keep descriptions, just restore default values if needed
    };
  }, [title, description, ogType, ogImage, schemaData]);

  return null; // SEO component does not render any visual UI elements
}
