package com.sareekart.dto.projection;

import java.time.LocalDateTime;

/**
 * Lightweight JPA projection for category dynamic XML sitemap generation.
 */
public interface CategorySitemapProjection {
    String getSlug();
    String getName();
    LocalDateTime getUpdatedAt();
}
