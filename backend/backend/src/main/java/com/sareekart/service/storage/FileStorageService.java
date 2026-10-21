package com.sareekart.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Unified storage abstraction for persisting, retrieving, streaming, and deleting
 * media assets across local development filesystems and production AWS S3.
 */
public interface FileStorageService {

    /**
     * Stores an uploaded multipart file under the specified category with an optional prefix.
     * Enforces content-type validation, size limits, and sanitizes filenames.
     *
     * @param file The multipart file uploaded by the client
     * @param category The storage category (e.g., SAREE_PHOTOS, RETURN_PHOTOS)
     * @param customPrefix Optional custom prefix for the filename (e.g. "product-12-")
     * @return StoredFile descriptor containing access URL and filename
     */
    StoredFile store(MultipartFile file, StorageCategory category, String customPrefix);

    /**
     * Stores an uploaded multipart file without a custom prefix.
     */
    default StoredFile store(MultipartFile file, StorageCategory category) {
        return store(file, category, null);
    }

    /**
     * Deletes a previously stored file.
     *
     * @param category The storage category
     * @param filename Sanitized filename
     * @return true if deleted, false if file did not exist
     */
    boolean delete(StorageCategory category, String filename);

    /**
     * Retrieves media metadata (content type, size, storage class) for HTTP serving.
     *
     * @param category The storage category
     * @param filename Sanitized filename
     * @return MediaMetadata descriptor
     */
    MediaMetadata getMetadata(StorageCategory category, String filename);

    /**
     * Opens a raw streaming InputStream for the object.
     * Callers MUST close the returned stream in a try-with-resources block
     * to guarantee underlying HTTP socket connection closure.
     *
     * @param category The storage category
     * @param filename Sanitized filename
     * @return InputStream to stream data from
     */
    InputStream openStream(StorageCategory category, String filename);
}
