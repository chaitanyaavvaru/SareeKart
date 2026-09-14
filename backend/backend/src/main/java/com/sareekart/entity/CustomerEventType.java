package com.sareekart.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum CustomerEventType {
    PRODUCT_VIEW,
    SEARCH_QUERY,
    CATEGORY_VIEW,
    ADD_TO_CART,
    REMOVE_FROM_CART,
    ADD_TO_WISHLIST,
    REMOVE_FROM_WISHLIST,
    CHECKOUT_INITIATED,
    ORDER_COMPLETED,
    AI_STYLIST_ENGAGE,
    VISUAL_SEARCH_ENGAGE;

    private static final Set<String> NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public static boolean isValid(String eventType) {
        return eventType != null && NAMES.contains(eventType.trim().toUpperCase());
    }

    public static CustomerEventType fromString(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        return valueOf(eventType.trim().toUpperCase());
    }
}
