package com.sareekart.service;

/**
 * Service for generating dynamic search engine assets including
 * XML sitemaps, robots.txt, and canonical catalog URL discovery.
 */
public interface SeoService {

    /**
     * Generates a fully-compliant XML sitemap (sitemaps.org protocol 0.9)
     * containing only active, indexable public URLs.
     *
     * @return Raw XML string with valid XML declaration and urlset.
     */
    String generateSitemapXml();

    /**
     * Generates standard production robots.txt content with crawl rules,
     * private route disallows, and canonical sitemap reference.
     *
     * @return Plaintext robots.txt format.
     */
    String generateRobotsTxt();
}
