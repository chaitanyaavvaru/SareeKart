package com.sareekart.dto.projection;

import java.time.LocalDateTime;

/**
 * Lightweight JPA projection for dynamic XML sitemap generation.
 * Prevents N+1 database queries and avoids loading complete Product entities into memory.
 */
public interface ProductSitemapProjection {
    Long getId();
    LocalDateTime getUpdatedAt();
    LocalDateTime getCreatedAt();
}
