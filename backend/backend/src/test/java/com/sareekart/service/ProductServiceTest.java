package com.sareekart.service;

import com.sareekart.dto.request.ProductRequest;
import com.sareekart.dto.response.PagedResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private com.sareekart.repository.InventoryItemRepository inventoryItemRepository;

    @Mock
    private com.sareekart.repository.FabricRepository fabricRepository;

    @Mock
    private com.sareekart.repository.OccasionRepository occasionRepository;

    @Mock
    private com.sareekart.repository.ColorRepository colorRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Category category;
    private ProductResponse productResponse;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Silk")
                .description("Pure Silk Sarees")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Kanchipuram Silk")
                .description("Heavy Temple Border Saree")
                .price(new BigDecimal("15000.00"))
                .stockQuantity(10)
                .fabric("Silk")
                .occasion("Wedding")
                .color("Red")
                .images(new ArrayList<>(List.of("image1.jpg")))
                .active(true)
                .category(category)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Kanchipuram Silk")
                .description("Heavy Temple Border Saree")
                .price(new BigDecimal("15000.00"))
                .stockQuantity(10)
                .fabric("Silk")
                .occasion("Wedding")
                .color("Red")
                .images(List.of("image1.jpg"))
                .active(true)
                .categoryId(1L)
                .categoryName("Silk")
                .build();

        productRequest = ProductRequest.builder()
                .name("Kanchipuram Silk")
                .description("Heavy Temple Border Saree")
                .price(new BigDecimal("15000.00"))
                .stockQuantity(10)
                .fabric("Silk")
                .occasion("Wedding")
                .color("Red")
                .images(List.of("image1.jpg"))
                .categoryId(1L)
                .build();
    }

    @Test
    void getAllProducts_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        PagedResponse<ProductResponse> response = productService.getAllProducts(0, 10, "id", "asc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Kanchipuram Silk", response.getContent().get(0).getName());
        verify(productRepository, times(1)).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProductResponse() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals("Kanchipuram Silk", response.getName());
        verify(productRepository, times(1)).findByIdAndActiveTrue(1L);
    }

    @Test
    void getProductById_WhenProductDoesNotExist_ShouldThrowResourceNotFoundException() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(1L));
        verify(productRepository, times(1)).findByIdAndActiveTrue(1L);
    }

    @Test
    void createProduct_WithCategory_ShouldReturnCreatedProductResponse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals("Kanchipuram Silk", response.getName());
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_WithInvalidCategory_ShouldThrowResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(productRequest));
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProductResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertNotNull(response);
        assertEquals("Kanchipuram Silk", response.getName());
        verify(productRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldSoftDeleteProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertFalse(product.getActive());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void searchProducts_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.searchProducts(eq("Kanchipuram"), any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        PagedResponse<ProductResponse> response = productService.searchProducts("Kanchipuram", 0, 10, "price", "asc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(productRepository, times(1)).searchProducts(eq("Kanchipuram"), any(Pageable.class));
    }

    @Test
    void filterProducts_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findByFilters(eq(1L), any(BigDecimal.class), any(BigDecimal.class), eq("Silk"), any(Pageable.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        PagedResponse<ProductResponse> response = productService.filterProducts(
                1L, BigDecimal.ZERO, new BigDecimal("20000.00"), "Silk", 0, 10, "price", "asc"
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(productRepository, times(1)).findByFilters(eq(1L), any(BigDecimal.class), any(BigDecimal.class), eq("Silk"), any(Pageable.class));
    }

    @Test
    void createProduct_ShouldSynchronizeToInventoryItem() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);
        when(inventoryItemRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        verify(inventoryItemRepository, times(1)).save(any(com.sareekart.entity.InventoryItem.class));
    }

    @Test
    void updateProduct_ShouldSynchronizeToInventoryItem() {
        com.sareekart.entity.InventoryItem existingItem = com.sareekart.entity.InventoryItem.builder()
                .sku("SK-KANCHIPURAM-SILK-1")
                .productId(1L)
                .productName("Old Name")
                .onHand(5)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);
        when(inventoryItemRepository.findByProductId(1L)).thenReturn(Optional.of(existingItem));

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertNotNull(response);
        assertEquals("Kanchipuram Silk", existingItem.getProductName());
        verify(inventoryItemRepository, times(1)).save(existingItem);
    }
}
