package com.sareekart.storage;

import com.sareekart.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Local-storage unit tests: key generation, traversal prevention,
 * idempotent delete. Uses @TempDir for isolated filesystem.
 */
@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setProvider(FileStorageProperties.Provider.LOCAL);
        props.setUploadDir(tempDir.toString());
        props.setMaxFileSize(5 * 1024 * 1024);
        service = new LocalFileStorageService(props, new ImageValidator(props));
    }

    private MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("file", name, "image/jpeg",
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01});
    }

    @Test
    @DisplayName("stores file and returns /uploads/ URL")
    void storeAndReturnUrl() throws Exception {
        String url = service.store(1L, jpeg("test.jpg"));

        assertThat(url).startsWith("/uploads/products/1/");
        assertThat(url).endsWith(".jpg");
        String key = service.extractKey(url);
        assertThat(key).isNotNull();
        assertThat(key).doesNotContain("../");
    }

    @Test
    @DisplayName("duplicate uploads get unique keys (no collision)")
    void duplicateNoCollision() {
        String url1 = service.store(1L, jpeg("photo.jpg"));
        String url2 = service.store(1L, jpeg("photo.jpg"));
        assertThat(url1).isNotEqualTo(url2);
    }


    @Test
    @DisplayName("delete is idempotent: missing file doesn't throw")
    void deleteIdempotent() {
        service.delete("/uploads/products/999/nonexistent.png");
        // No exception expected
    }

    @Test
    @DisplayName("extractKey returns null for non-upload URLs")
    void extractKeyForeign() {
        assertThat(service.extractKey("https://s3.amazonaws.com/bucket/key")).isNull();
        assertThat(service.extractKey(null)).isNull();
    }
}
