package com.sareekart.controller;

import com.sareekart.entity.InventoryItem;
import com.sareekart.entity.Product;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoUploadControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private PhotoUploadController photoUploadController;

    private Product product;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Silk Saree")
                .price(new BigDecimal("5000.00"))
                .images(new ArrayList<>())
                .build();

        inventoryItem = InventoryItem.builder()
                .id(10L)
                .sku("SKU-SILK-01")
                .productId(1L)
                .productName("Silk Saree")
                .build();
    }

    @Test
    void uploadPhoto_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        ResponseEntity<Map<String, Object>> response = photoUploadController.uploadPhoto(file, 1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertTrue(((String) response.getBody().get("url")).startsWith("/uploads/saree-photos/"));
        // Atomicity check: upload must not prematurely mutate product in repository before save
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void uploadPhoto_emptyFile_badRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        ResponseEntity<Map<String, Object>> response = photoUploadController.uploadPhoto(file, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    void uploadPhoto_unsupportedType_badRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf-bytes".getBytes()
        );

        ResponseEntity<Map<String, Object>> response = photoUploadController.uploadPhoto(file, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(((String) response.getBody().get("error")).contains("Unsupported image format"));
    }

    @Test
    void getPhotosBySku_success() {
        when(inventoryItemRepository.findBySku("SKU-SILK-01")).thenReturn(Optional.of(inventoryItem));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ResponseEntity<Map<String, Object>> response = photoUploadController.getPhotosBySku("SKU-SILK-01");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("SKU-SILK-01", response.getBody().get("sku"));
    }
}
