package com.sareekart.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageServiceImpl(tempDir.toString());
    }

    @Test
    void store_success() throws Exception {
        byte[] content = "saree image bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "kanchipuram.jpg", "image/jpeg", content
        );

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS);

        assertNotNull(stored);
        assertNotNull(stored.getFilename());
        assertTrue(stored.getUrl().startsWith("/uploads/saree-photos/"));
        assertEquals(content.length, stored.getSize());
        assertEquals("image/jpeg", stored.getContentType());
        assertEquals("saree-photos", stored.getCategory());

        Path filePath = tempDir.resolve("saree-photos").resolve(stored.getFilename());
        assertTrue(Files.exists(filePath));
        assertArrayEquals(content, Files.readAllBytes(filePath));
    }

    @Test
    void store_withCustomPrefix() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pallu.png", "image/png", "png data".getBytes()
        );

        StoredFile stored = storageService.store(file, StorageCategory.RETURN_PHOTOS, "order-99-");

        assertNotNull(stored);
        assertTrue(stored.getFilename().startsWith("order-99-"));
        assertTrue(stored.getUrl().startsWith("/uploads/return-photos/order-99-"));
        assertEquals("image/png", stored.getContentType());
    }

    @Test
    void store_emptyOrNullFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                storageService.store(emptyFile, StorageCategory.SAREE_PHOTOS)
        );
        assertThrows(IllegalArgumentException.class, () ->
                storageService.store(null, StorageCategory.SAREE_PHOTOS)
        );
    }

    @Test
    void store_pathTraversalInPrefix_sanitizesSafely() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image bytes".getBytes()
        );

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS, "../../../etc/");

        assertNotNull(stored);
        assertFalse(stored.getFilename().contains(".."));
        assertFalse(stored.getFilename().contains("/"));
        Path targetFile = tempDir.resolve("saree-photos").resolve(stored.getFilename());
        assertTrue(Files.exists(targetFile));
    }

    @Test
    void getMetadata_success() {
        byte[] content = "sample test data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.webp", "image/webp", content
        );

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS);
        MediaMetadata metadata = storageService.getMetadata(StorageCategory.SAREE_PHOTOS, stored.getFilename());

        assertNotNull(metadata);
        assertEquals("image/webp", metadata.getContentType());
        assertEquals(content.length, metadata.getContentLength());
        assertEquals("LOCAL_FS", metadata.getStorageClass());
        assertFalse(metadata.isArchived());
        assertNotNull(metadata.getETag());
    }

    @Test
    void getMetadata_notFound_throws404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storageService.getMetadata(StorageCategory.SAREE_PHOTOS, "non-existent.jpg")
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void openStream_success() throws Exception {
        byte[] expected = "stream verification payload".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "stream.jpg", "image/jpeg", expected
        );

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS);

        try (InputStream in = storageService.openStream(StorageCategory.SAREE_PHOTOS, stored.getFilename())) {
            byte[] actual = in.readAllBytes();
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void openStream_notFound_throws404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storageService.openStream(StorageCategory.RETURN_PHOTOS, "missing.png")
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void delete_existingFile_returnsTrue() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "del.jpg", "image/jpeg", "to-delete".getBytes()
        );

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS);
        Path filePath = tempDir.resolve("saree-photos").resolve(stored.getFilename());
        assertTrue(Files.exists(filePath));

        boolean deleted = storageService.delete(StorageCategory.SAREE_PHOTOS, stored.getFilename());
        assertTrue(deleted);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void delete_nonExistingFile_returnsFalse() {
        boolean deleted = storageService.delete(StorageCategory.SAREE_PHOTOS, "no-such-file.jpg");
        assertFalse(deleted);
    }

    @Test
    void delete_pathTraversalAttempt_throwsSecurityException() {
        assertThrows(SecurityException.class, () ->
                storageService.delete(StorageCategory.SAREE_PHOTOS, "../../../etc/passwd")
        );
    }
}
