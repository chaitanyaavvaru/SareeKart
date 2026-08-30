package com.sareekart.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction port — domain code must never depend on AWS SDK classes.
 *
 * Implementations:
 *  - LocalFileStorageService (dev/test, app.storage.provider=local)
 *  - S3FileStorageService    (prod,     app.storage.provider=s3)
 */
public interface FileStorageService {

    /**
     * Stores an image and returns its publicly accessible URL.
     * Implementations generate server-controlled object keys; the client
     * filename is only used (sanitised) to derive the file extension.
     *
     * @param productId owning product — used as the key folder
     * @param file      validated multipart image
     * @return publicly accessible URL for the stored image
     */
    String store(Long productId, MultipartFile file);

    /**
     * Deletes the object at the given storage URL.
     * Idempotent: missing objects do not throw.
     */
    void delete(String url);

    /**
     * Extracts the provider-specific storage key from a previously returned
     * URL, or null if the URL doesn't belong to this provider.
     */
    String extractKey(String url);
}
