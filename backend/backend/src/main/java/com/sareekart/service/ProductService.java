package com.sareekart.service;

import com.sareekart.dto.product.ProductFilterRequest;
import com.sareekart.dto.product.ProductRequest;
import com.sareekart.dto.product.ProductResponse;
import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.entity.ProductImage;
import com.sareekart.entity.ProductVariant;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductImageRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(pageable)
            .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.searchProducts(query, pageable)
            .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> filterProducts(ProductFilterRequest filter) {
        Sort sort = filter.getSortDir().equalsIgnoreCase("desc")
            ? Sort.by(filter.getSortBy()).descending()
            : Sort.by(filter.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        return productRepository.filterProducts(
                filter.getCategoryId(),
                filter.getFabric(),
                filter.getOccasion(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                pageable
            ).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long categoryId, Long excludeId) {
        return productRepository.findRelatedProducts(categoryId, excludeId, PageRequest.of(0, 4))
            .stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        return productRepository.findByActiveTrueAndFeaturedTrue(PageRequest.of(0, limit))
            .stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllFabrics() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(Pageable.unpaged())
            .stream()
            .map(Product::getFabric)
            .distinct()
            .filter(f -> f != null && !f.isBlank())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllOccasions() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(Pageable.unpaged())
            .stream()
            .map(Product::getOccasion)
            .distinct()
            .filter(o -> o != null && !o.isBlank())
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with this SKU already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setCategory(category);
        product.setFabric(request.getFabric());
        product.setOccasion(request.getOccasion());
        product.setSku(request.getSku());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());
        product.setImages(new ArrayList<>());
        product.setVariants(new ArrayList<>());

        Product savedProduct = productRepository.save(product);

        // Save images
        if (request.getImages() != null) {
            final Product finalProduct = savedProduct;
            List<ProductImage> images = request.getImages().stream()
                .map(img -> {
                    ProductImage image = new ProductImage();
                    image.setProduct(finalProduct);
                    image.setUrl(img.getUrl());
                    image.setAltText(img.getAltText());
                    image.setIsPrimary(img.getIsPrimary() != null ? img.getIsPrimary() : false);
                    image.setSortOrder(img.getSortOrder() != null ? img.getSortOrder() : 0);
                    return image;
                })
                .collect(Collectors.toList());
            productImageRepository.saveAll(images);
            savedProduct.setImages(images);
        }

        // Save variants
        if (request.getVariants() != null) {
            final Product finalProduct2 = savedProduct;
            List<ProductVariant> variants = request.getVariants().stream()
                .map(v -> {
                    if (v.getSku() != null && productVariantRepository.existsBySku(v.getSku())) {
                        throw new BadRequestException("Variant with SKU " + v.getSku() + " already exists");
                    }
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(finalProduct2);
                    variant.setSize(v.getSize());
                    variant.setColor(v.getColor());
                    variant.setStockQuantity(v.getStockQuantity() != null ? v.getStockQuantity() : 0);
                    variant.setPriceAdjustment(v.getPriceAdjustment() != null ? v.getPriceAdjustment() : BigDecimal.ZERO);
                    variant.setSku(v.getSku());
                    variant.setActive(v.getActive() != null ? v.getActive() : true);
                    return variant;
                })
                .collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
            savedProduct.setVariants(variants);
        }

        log.info("Created product: {}", product.getName());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getSku() != null && !request.getSku().equals(product.getSku())
                && productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with this SKU already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        product.setName(request.getName());
        product.setSlug(request.getSlug() != null ? request.getSlug() : product.getSlug());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setBasePrice(request.getBasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setCategory(category);
        product.setFabric(request.getFabric());
        product.setOccasion(request.getOccasion());
        product.setSku(request.getSku());
        product.setActive(request.getActive());
        product.setFeatured(request.getFeatured());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());

        final Product finalProduct = product;

        // Update images
        if (request.getImages() != null) {
            productImageRepository.deleteByProductId(product.getId());
            List<ProductImage> images = request.getImages().stream()
                .map(img -> {
                    ProductImage image = new ProductImage();
                    image.setProduct(finalProduct);
                    image.setUrl(img.getUrl());
                    image.setAltText(img.getAltText());
                    image.setIsPrimary(img.getIsPrimary() != null ? img.getIsPrimary() : false);
                    image.setSortOrder(img.getSortOrder() != null ? img.getSortOrder() : 0);
                    return image;
                })
                .collect(Collectors.toList());
            productImageRepository.saveAll(images);
            product.setImages(images);
        }

        // Update variants
        if (request.getVariants() != null) {
            productVariantRepository.deleteByProductId(product.getId());
            List<ProductVariant> variants = request.getVariants().stream()
                .map(v -> {
                    if (v.getSku() != null && productVariantRepository.existsBySku(v.getSku())) {
                        throw new BadRequestException("Variant with SKU " + v.getSku() + " already exists");
                    }
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(finalProduct);
                    variant.setSize(v.getSize());
                    variant.setColor(v.getColor());
                    variant.setStockQuantity(v.getStockQuantity() != null ? v.getStockQuantity() : 0);
                    variant.setPriceAdjustment(v.getPriceAdjustment() != null ? v.getPriceAdjustment() : BigDecimal.ZERO);
                    variant.setSku(v.getSku());
                    variant.setActive(v.getActive() != null ? v.getActive() : true);
                    return variant;
                })
                .collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
            product.setVariants(variants);
        }

        product = productRepository.save(product);
        log.info("Updated product: {}", product.getName());
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Deactivated product: {}", product.getName());
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer stockQuantity) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getVariants().isEmpty()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setStockQuantity(stockQuantity);
            variant.setActive(true);
            productVariantRepository.save(variant);
            product.getVariants().add(variant);
        } else {
            ProductVariant variant = product.getVariants().get(0);
            variant.setStockQuantity(stockQuantity);
            productVariantRepository.save(variant);
        }

        product = productRepository.save(product);
        return ProductResponse.from(product);
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        String baseSlug = slug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}