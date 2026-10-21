package com.sareekart.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3StorageServiceImpl storageService;

    private static final String TEST_BUCKET = "sareekart-media-bucket-test";
    private static final String TEST_REGION = "ap-south-1";

    @BeforeEach
    void setUp() {
        storageService = new S3StorageServiceImpl(s3Client);
        ReflectionTestUtils.setField(storageService, "bucketName", TEST_BUCKET);
        ReflectionTestUtils.setField(storageService, "region", TEST_REGION);
    }

    @Test
    void store_success_withAes256Encryption() {
        byte[] content = "saree photo bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "banarasi.jpg", "image/jpeg", content
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        StoredFile stored = storageService.store(file, StorageCategory.SAREE_PHOTOS, "prod-100-");

        assertNotNull(stored);
        assertTrue(stored.getFilename().startsWith("prod-100-"));
        assertTrue(stored.getUrl().startsWith("/uploads/saree-photos/prod-100-"));
        assertEquals(content.length, stored.getSize());
        assertEquals("image/jpeg", stored.getContentType());
        assertEquals("saree-photos", stored.getCategory());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals(TEST_BUCKET, captured.bucket());
        assertEquals("saree-photos/" + stored.getFilename(), captured.key());
        assertEquals(ServerSideEncryption.AES256, captured.serverSideEncryption());
        assertEquals(content.length, captured.contentLength());
        assertEquals("image/jpeg", captured.contentType());
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
    void store_s3Exception_throws500() {
        MockMultipartFile file = new MockMultipartFile("file", "err.jpg", "image/jpeg", "data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 connection error").statusCode(500).build());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storageService.store(file, StorageCategory.SAREE_PHOTOS)
        );
        assertEquals(500, ex.getStatusCode().value());
    }

    @Test
    void delete_existingFile_returnsTrue() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        boolean deleted = storageService.delete(StorageCategory.SAREE_PHOTOS, "photo-123.jpg");
        assertTrue(deleted);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());
        assertEquals(TEST_BUCKET, captor.getValue().bucket());
        assertEquals("saree-photos/photo-123.jpg", captor.getValue().key());
    }

    @Test
    void delete_noSuchKey_returnsFalse() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").statusCode(404).build());

        boolean deleted = storageService.delete(StorageCategory.RETURN_PHOTOS, "missing.jpg");
        assertFalse(deleted);
    }

    @Test
    void delete_pathTraversalAttempt_throwsSecurityException() {
        assertThrows(SecurityException.class, () ->
                storageService.delete(StorageCategory.SAREE_PHOTOS, "../../secret.txt")
        );
    }

    @Test
    void getMetadata_standardStorage_success() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentType("image/png")
                .contentLength(2048L)
                .storageClass("STANDARD_IA")
                .eTag("\"etag-999\"")
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        MediaMetadata metadata = storageService.getMetadata(StorageCategory.SAREE_PHOTOS, "test.png");

        assertNotNull(metadata);
        assertEquals("image/png", metadata.getContentType());
        assertEquals(2048L, metadata.getContentLength());
        assertEquals("STANDARD_IA", metadata.getStorageClass());
        assertEquals("\"etag-999\"", metadata.getETag());
        assertFalse(metadata.isArchived());
    }

    @Test
    void getMetadata_glacierWithoutRestore_flagsArchived() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentType("image/jpeg")
                .contentLength(4096L)
                .storageClass("GLACIER")
                .restore(null)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        MediaMetadata metadata = storageService.getMetadata(StorageCategory.RETURN_PHOTOS, "archived.jpg");

        assertNotNull(metadata);
        assertEquals("GLACIER", metadata.getStorageClass());
        assertTrue(metadata.isArchived());
    }

    @Test
    void getMetadata_notFound_throws404() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).message("Not found").build());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storageService.getMetadata(StorageCategory.SAREE_PHOTOS, "non-existent.jpg")
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void openStream_success() throws Exception {
        byte[] expected = "s3 stream content payload".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse getResponse = GetObjectResponse.builder()
                .contentLength((long) expected.length)
                .contentType("image/jpeg")
                .build();

        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                getResponse,
                AbortableInputStream.create(new ByteArrayInputStream(expected))
        );

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

        try (InputStream in = storageService.openStream(StorageCategory.SAREE_PHOTOS, "sample.jpg")) {
            byte[] actual = in.readAllBytes();
            assertArrayEquals(expected, actual);
        }

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObject(captor.capture());
        assertEquals(TEST_BUCKET, captor.getValue().bucket());
        assertEquals("saree-photos/sample.jpg", captor.getValue().key());
    }

    @Test
    void openStream_notFound_throws404() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).message("Key not found").build());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storageService.openStream(StorageCategory.RETURN_PHOTOS, "missing.jpg")
        );
        assertEquals(404, ex.getStatusCode().value());
    }
}
