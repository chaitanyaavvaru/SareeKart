package com.sareekart.service.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO returned after storing an uploaded file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {
    private String url;
    private String filename;
    private long size;
    private String contentType;
    private String category;
}
