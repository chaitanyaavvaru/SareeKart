package com.sareekart.service;

/**
 * Service for generating product catalog feeds for Meta Commerce Manager and Instagram Shop.
 */
public interface MetaCatalogService {

    /**
     * Generates an RFC 4180-compliant CSV catalog feed projecting active products
     * with canonical URLs, absolute image URLs, INR prices, and standardized categories.
     *
     * @return RFC 4180 CSV string
     */
    String generateCatalogCsv();
}
