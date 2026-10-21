package com.sareekart.service.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Explicit whitelist of allowed storage categories for file uploads.
 * Restricts directory placement and S3 object key prefixes to known safe domains.
 */
@Getter
@RequiredArgsConstructor
public enum StorageCategory {

    SAREE_PHOTOS("saree-photos", true),
    RETURN_PHOTOS("return-photos", false);

    private final String directory;
    private final boolean isPublic;

    /**
     * Resolves and validates a string category into an allowed enum constant.
     * Rejects directory traversal sequences and unknown categories.
     *
     * @param path The incoming URL or request category string
     * @return The validated StorageCategory
     * @throws IllegalArgumentException if the category is null, blank, or not in the allowlist
     */
    public static StorageCategory fromPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Storage category must not be empty");
        }
        String normalized = path.trim().toLowerCase();
        for (StorageCategory category : values()) {
            if (category.directory.equals(normalized)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unsupported or unauthorized storage category: " + path);
    }
}
