package com.sareekart.config;

/**
 * Business constants for shopping cart and inventory limits in SareeKart.
 */
public final class CartConstants {

    private CartConstants() {
        // Prevent instantiation
    }

    /**
     * Maximum quantity of units per Saree SKU allowed per customer in a single cart.
     * Enforces artisan handloom stock protection and prevents bulk automated hoarding.
     */
    public static final int MAX_QUANTITY_PER_SKU = 10;

    /**
     * Expiration duration in days for guest cart sessions in client storage.
     */
    public static final int GUEST_CART_EXPIRATION_DAYS = 30;
}
