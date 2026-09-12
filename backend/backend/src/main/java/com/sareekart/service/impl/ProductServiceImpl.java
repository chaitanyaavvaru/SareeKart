package com.sareekart.service.impl;

import com.sareekart.dto.request.ProductRequest;
import com.sareekart.dto.request.ProductSearchCriteria;
import com.sareekart.dto.response.PagedResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Category;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Occasion;
import com.sareekart.entity.Product;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ColorRepository;
import com.sareekart.repository.FabricRepository;
import com.sareekart.repository.OccasionRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.ProductService;
import com.sareekart.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FabricRepository fabricRepository;
    private final OccasionRepository occasionRepository;
    private final ColorRepository colorRepository;
    private final ProductMapper productMapper;
    private final com.sareekart.repository.InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPagedResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                // PHASE 1 FROZEN: Product images staged list
                .images(request.getImages() != null ? new java.util.ArrayList<>(request.getImages()) : new java.util.ArrayList<>())
                .active(true)
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        // Canonical attributes resolution & dual-write mirroring
        resolveFabric(product, request.getFabricId(), request.getFabric());
        resolveOccasion(product, request.getOccasionId(), request.getOccasion());
        resolveColor(product, request.getColorId(), request.getColor());

        Product savedProduct = productRepository.save(product);
        createInventoryItemForProduct(savedProduct);
        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        // PHASE 1 FROZEN: Image collection modification
        if (request.getImages() != null) {
            // Clear in-place then re-add — avoids JPA orphan/constraint issues with @ElementCollection
            product.getImages().clear();
            product.getImages().addAll(request.getImages());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // Canonical attributes resolution & dual-write mirroring
        resolveFabric(product, request.getFabricId(), request.getFabric());
        resolveOccasion(product, request.getOccasionId(), request.getOccasion());
        resolveColor(product, request.getColorId(), request.getColor());

        Product updatedProduct = productRepository.save(product);

        // Synchronize changes to linked InventoryItem
        if (updatedProduct.getId() != null) {
            inventoryItemRepository.findByProductId(updatedProduct.getId()).ifPresent(item -> {
                item.setProductName(updatedProduct.getName());
                if (updatedProduct.getCategory() != null) {
                    item.setCategory(updatedProduct.getCategory().getName());
                } else if (updatedProduct.getFabric() != null) {
                    item.setCategory(updatedProduct.getFabric());
                }
                item.setUnitPrice(updatedProduct.getPrice());
                item.setOnHand(updatedProduct.getStockQuantity() != null ? updatedProduct.getStockQuantity() : 0);
                item.recalculateStatus();
                inventoryItemRepository.save(item);
            });
        }

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);

        inventoryItemRepository.findByProductId(id).ifPresent(item -> {
            item.setStatus("OUT_OF_STOCK");
            item.setAvailable(0);
            inventoryItemRepository.save(item);
        });
    }

    private void resolveFabric(Product product, Long fabricId, String legacyFabric) {
        if (fabricId != null) {
            Fabric fabric = fabricRepository.findById(fabricId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fabric", "id", fabricId));
            product.setFabricEntity(fabric);
            product.setFabric(fabric.getName()); // Canonical -> legacy mirror
        } else if (legacyFabric != null && !legacyFabric.trim().isEmpty()) {
            String trimmed = legacyFabric.trim();
            fabricRepository.findByNameIgnoreCase(trimmed).ifPresentOrElse(f -> {
                product.setFabricEntity(f);
                product.setFabric(f.getName());
            }, () -> {
                product.setFabricEntity(null);
                product.setFabric(trimmed);
            });
        } else {
            product.setFabricEntity(null);
            product.setFabric(null);
        }
    }

    private void resolveOccasion(Product product, Long occasionId, String legacyOccasion) {
        if (occasionId != null) {
            Occasion occasion = occasionRepository.findById(occasionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Occasion", "id", occasionId));
            product.setOccasionEntity(occasion);
            product.setOccasion(occasion.getName()); // Canonical -> legacy mirror
        } else if (legacyOccasion != null && !legacyOccasion.trim().isEmpty()) {
            String trimmed = legacyOccasion.trim();
            occasionRepository.findByNameIgnoreCase(trimmed).ifPresentOrElse(o -> {
                product.setOccasionEntity(o);
                product.setOccasion(o.getName());
            }, () -> {
                product.setOccasionEntity(null);
                product.setOccasion(trimmed);
            });
        } else {
            product.setOccasionEntity(null);
            product.setOccasion(null);
        }
    }

    private void resolveColor(Product product, Long colorId, String legacyColor) {
        if (colorId != null) {
            Color color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Color", "id", colorId));
            product.setColorEntity(color);
            product.setColor(color.getName()); // Canonical -> legacy mirror
        } else if (legacyColor != null && !legacyColor.trim().isEmpty()) {
            String trimmed = legacyColor.trim();
            colorRepository.findByNameIgnoreCase(trimmed).ifPresentOrElse(c -> {
                product.setColorEntity(c);
                product.setColor(c.getName());
            }, () -> {
                product.setColorEntity(null);
                product.setColor(trimmed);
            });
        } else {
            product.setColorEntity(null);
            product.setColor(null);
        }
    }

    private void createInventoryItemForProduct(Product product) {
        if (product.getId() == null || inventoryItemRepository.findByProductId(product.getId()).isPresent()) {
            return;
        }

        String sku = generateSku(product);
        if (inventoryItemRepository.findBySku(sku).isPresent()) {
            sku = sku + "-" + (System.currentTimeMillis() % 10000);
        }

        String catName = "Handloom Silk";
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            catName = product.getCategory().getName();
        } else if (product.getFabric() != null) {
            catName = product.getFabric();
        }

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        long pid = product.getId() != null ? product.getId() : 1L;

        com.sareekart.entity.InventoryItem item = com.sareekart.entity.InventoryItem.builder()
                .sku(sku)
                .productId(product.getId())
                .productName(product.getName())
                .category(catName)
                .warehouseCode("WH-01")
                .warehouseName("Bengaluru Central Fulfillment Hub")
                .binLocation("A" + (pid % 5 + 1) + "-R" + (pid % 3 + 1) + "-S1")
                .onHand(stock)
                .reserved(0)
                .available(stock)
                .unitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                .build();
        item.recalculateStatus();
        inventoryItemRepository.save(item);
    }

    private String generateSku(Product product) {
        String cleanName = product.getName().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        if (cleanName.startsWith("-")) cleanName = cleanName.substring(1);
        if (cleanName.endsWith("-")) cleanName = cleanName.substring(0, cleanName.length() - 1);
        if (cleanName.length() > 20) cleanName = cleanName.substring(0, 20);
        return "SK-" + cleanName + "-" + product.getId();
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncMissingInventoryItems() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if (p.getId() != null && inventoryItemRepository.findByProductId(p.getId()).isEmpty()) {
                createInventoryItemForProduct(p);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String query, int page, int size, String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.searchProducts(query, pageable);
        return toPagedResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> filterProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String fabric, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findByFilters(categoryId, minPrice, maxPrice, fabric, pageable);
        return toPagedResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchAndFilterProducts(ProductSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new ProductSearchCriteria();
        }

        Set<Long> categoryIds = resolveCategoryIds(criteria.getCategory());
        ResolvedCriteriaAttribute fabricAttr = resolveFabricCriteria(criteria.getFabric());
        ResolvedCriteriaAttribute occasionAttr = resolveOccasionCriteria(criteria.getOccasion());
        ResolvedCriteriaAttribute colorAttr = resolveColorCriteria(criteria.getColor());

        ProductSpecification spec = new ProductSpecification(
                criteria.getQ(),
                categoryIds,
                fabricAttr != null ? fabricAttr.id() : null,
                fabricAttr != null ? fabricAttr.name() : null,
                occasionAttr != null ? occasionAttr.id() : null,
                occasionAttr != null ? occasionAttr.name() : null,
                colorAttr != null ? colorAttr.id() : null,
                colorAttr != null ? colorAttr.name() : null,
                criteria.getColorFamily(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getInStock()
        );

        int page = criteria.getPage() != null && criteria.getPage() >= 0 ? criteria.getPage() : 0;
        int size = criteria.getSize() != null && criteria.getSize() > 0 ? criteria.getSize() : 12;
        Sort sort = buildSort(criteria.getSortBy(), criteria.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return toPagedResponse(productPage);
    }

    private Set<Long> resolveCategoryIds(String categoryParam) {
        if (categoryParam == null || categoryParam.trim().isEmpty()) {
            return null;
        }
        String trimmed = categoryParam.trim();
        Optional<Category> categoryOpt = Optional.empty();
        try {
            Long id = Long.parseLong(trimmed);
            categoryOpt = categoryRepository.findById(id);
        } catch (NumberFormatException ignored) {}

        if (categoryOpt.isEmpty()) {
            categoryOpt = categoryRepository.findBySlug(trimmed);
        }
        if (categoryOpt.isEmpty()) {
            categoryOpt = categoryRepository.findByName(trimmed);
        }

        if (categoryOpt.isEmpty()) {
            return Set.of(-1L);
        }

        Category cat = categoryOpt.get();
        Set<Long> ids = new HashSet<>();
        ids.add(cat.getId());
        // Safeguard 2: Match category OR direct children where parent_id = :catId
        List<Category> children = categoryRepository.findByParentIdOrderByDisplayOrderAsc(cat.getId());
        for (Category child : children) {
            ids.add(child.getId());
        }
        return ids;
    }

    private record ResolvedCriteriaAttribute(Long id, String name) {}

    private ResolvedCriteriaAttribute resolveFabricCriteria(String fabricParam) {
        if (fabricParam == null || fabricParam.trim().isEmpty()) return null;
        String trimmed = fabricParam.trim();
        try {
            Long id = Long.parseLong(trimmed);
            Optional<Fabric> f = fabricRepository.findById(id);
            if (f.isPresent()) return new ResolvedCriteriaAttribute(f.get().getId(), f.get().getName());
        } catch (NumberFormatException ignored) {}

        Optional<Fabric> f = fabricRepository.findBySlug(trimmed);
        if (f.isEmpty()) {
            f = fabricRepository.findByNameIgnoreCase(trimmed);
        }
        return f.map(fabric -> new ResolvedCriteriaAttribute(fabric.getId(), fabric.getName()))
                .orElseGet(() -> new ResolvedCriteriaAttribute(null, trimmed));
    }

    private ResolvedCriteriaAttribute resolveOccasionCriteria(String occasionParam) {
        if (occasionParam == null || occasionParam.trim().isEmpty()) return null;
        String trimmed = occasionParam.trim();
        try {
            Long id = Long.parseLong(trimmed);
            Optional<Occasion> o = occasionRepository.findById(id);
            if (o.isPresent()) return new ResolvedCriteriaAttribute(o.get().getId(), o.get().getName());
        } catch (NumberFormatException ignored) {}

        Optional<Occasion> o = occasionRepository.findBySlug(trimmed);
        if (o.isEmpty()) {
            o = occasionRepository.findByNameIgnoreCase(trimmed);
        }
        return o.map(occ -> new ResolvedCriteriaAttribute(occ.getId(), occ.getName()))
                .orElseGet(() -> new ResolvedCriteriaAttribute(null, trimmed));
    }

    private ResolvedCriteriaAttribute resolveColorCriteria(String colorParam) {
        if (colorParam == null || colorParam.trim().isEmpty()) return null;
        String trimmed = colorParam.trim();
        try {
            Long id = Long.parseLong(trimmed);
            Optional<Color> c = colorRepository.findById(id);
            if (c.isPresent()) return new ResolvedCriteriaAttribute(c.get().getId(), c.get().getName());
        } catch (NumberFormatException ignored) {}

        Optional<Color> c = colorRepository.findBySlug(trimmed);
        if (c.isEmpty()) {
            c = colorRepository.findByNameIgnoreCase(trimmed);
        }
        return c.map(col -> new ResolvedCriteriaAttribute(col.getId(), col.getName()))
                .orElseGet(() -> new ResolvedCriteriaAttribute(null, trimmed));
    }

    private PagedResponse<ProductResponse> toPagedResponse(Page<Product> productPage) {
        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .toList();
        return PagedResponse.of(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Set<String> allowedSorts = Set.of("createdAt", "price", "name");
        String safeSortBy = allowedSorts.contains(sortBy) ? sortBy : "createdAt";
        return "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortBy).descending()
                : Sort.by(safeSortBy).ascending();
    }
}
