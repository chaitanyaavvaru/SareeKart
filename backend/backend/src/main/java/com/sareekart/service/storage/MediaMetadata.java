package com.sareekart.service.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata descriptor for stored media files used in HTTP serving.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetadata {
    private String contentType;
    private long contentLength;
    private String storageClass;
    private String eTag;
    private boolean isArchived;
}
