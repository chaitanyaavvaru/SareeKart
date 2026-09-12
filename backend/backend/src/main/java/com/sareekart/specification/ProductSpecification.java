package com.sareekart.specification;

import com.sareekart.entity.Category;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Occasion;
import com.sareekart.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProductSpecification implements Specification<Product> {

    private final String q;
    private final Set<Long> categoryIds;
    private final Long fabricId;
    private final String fabricName;
    private final Long occasionId;
    private final String occasionName;
    private final Long colorId;
    private final String colorName;
    private final String colorFamily;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final Boolean inStock;

    public ProductSpecification(
            String q,
            Set<Long> categoryIds,
            Long fabricId,
            String fabricName,
            Long occasionId,
            String occasionName,
            Long colorId,
            String colorName,
            String colorFamily,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock) {
        this.q = q;
        this.categoryIds = categoryIds;
        this.fabricId = fabricId;
        this.fabricName = fabricName;
        this.occasionId = occasionId;
        this.occasionName = occasionName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.colorFamily = colorFamily;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.inStock = inStock;
    }

    @Override
    public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // 1. Only active products are visible on storefront/search
        predicates.add(cb.isTrue(root.get("active")));

        // 2. Category hierarchy (Safeguard 2: matches category OR direct children)
        if (categoryIds != null) {
            if (categoryIds.isEmpty()) {
                predicates.add(cb.disjunction()); // Category specified but doesn't exist
            } else {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }
        }

        // 3. Fabric filtering (canonical fabric entity or legacy string mirror)
        if (fabricId != null) {
            Predicate matchEntity = cb.equal(root.get("fabricEntity").get("id"), fabricId);
            if (fabricName != null && !fabricName.isBlank()) {
                Predicate matchLegacy = cb.equal(cb.lower(root.get("fabric")), fabricName.trim().toLowerCase());
                predicates.add(cb.or(matchEntity, matchLegacy));
            } else {
                predicates.add(matchEntity);
            }
        } else if (fabricName != null && !fabricName.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("fabric")), fabricName.trim().toLowerCase()));
        }

        // 4. Occasion filtering (canonical occasion entity or legacy string mirror)
        if (occasionId != null) {
            Predicate matchEntity = cb.equal(root.get("occasionEntity").get("id"), occasionId);
            if (occasionName != null && !occasionName.isBlank()) {
                Predicate matchLegacy = cb.equal(cb.lower(root.get("occasion")), occasionName.trim().toLowerCase());
                predicates.add(cb.or(matchEntity, matchLegacy));
            } else {
                predicates.add(matchEntity);
            }
        } else if (occasionName != null && !occasionName.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("occasion")), occasionName.trim().toLowerCase()));
        }

        // 5. Color filtering (canonical color entity or legacy string mirror)
        if (colorId != null) {
            Predicate matchEntity = cb.equal(root.get("colorEntity").get("id"), colorId);
            if (colorName != null && !colorName.isBlank()) {
                Predicate matchLegacy = cb.equal(cb.lower(root.get("color")), colorName.trim().toLowerCase());
                predicates.add(cb.or(matchEntity, matchLegacy));
            } else {
                predicates.add(matchEntity);
            }
        } else if (colorName != null && !colorName.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("color")), colorName.trim().toLowerCase()));
        }

        // 6. Color Family filtering (Safeguard 3: query canonical colors.family from database)
        if (colorFamily != null && !colorFamily.isBlank()) {
            Join<Product, Color> colorJoin = getOrCreateJoin(root, "colorEntity");
            predicates.add(cb.equal(cb.lower(colorJoin.get("family")), colorFamily.trim().toLowerCase()));
        }

        // 7. Price bounds
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        // 8. Stock availability
        if (Boolean.TRUE.equals(inStock)) {
            predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
        }

        // 9. Multi-token keyword search
        if (q != null && !q.trim().isEmpty()) {
            String[] tokens = q.trim().split("\\s+");
            Join<Product, Category> catJoin = getOrCreateJoin(root, "category");
            Join<Product, Fabric> fabricJoin = getOrCreateJoin(root, "fabricEntity");
            Join<Product, Occasion> occasionJoin = getOrCreateJoin(root, "occasionEntity");
            Join<Product, Color> colorJoin = getOrCreateJoin(root, "colorEntity");

            for (String token : tokens) {
                if (token.isEmpty()) continue;
                String pattern = "%" + token.toLowerCase() + "%";

                Predicate matchName = cb.like(cb.lower(root.get("name")), pattern);
                Predicate matchDesc = cb.like(cb.lower(root.get("description")), pattern);
                Predicate matchCategory = cb.like(cb.lower(catJoin.get("name")), pattern);
                Predicate matchFabricEntity = cb.like(cb.lower(fabricJoin.get("name")), pattern);
                Predicate matchFabricLegacy = cb.like(cb.lower(root.get("fabric")), pattern);
                Predicate matchOccasionEntity = cb.like(cb.lower(occasionJoin.get("name")), pattern);
                Predicate matchOccasionLegacy = cb.like(cb.lower(root.get("occasion")), pattern);
                Predicate matchColorEntity = cb.like(cb.lower(colorJoin.get("name")), pattern);
                Predicate matchColorFamily = cb.like(cb.lower(colorJoin.get("family")), pattern);
                Predicate matchColorLegacy = cb.like(cb.lower(root.get("color")), pattern);

                Predicate tokenMatch = cb.or(
                        matchName,
                        matchDesc,
                        matchCategory,
                        matchFabricEntity,
                        matchFabricLegacy,
                        matchOccasionEntity,
                        matchOccasionLegacy,
                        matchColorEntity,
                        matchColorFamily,
                        matchColorLegacy
                );
                predicates.add(tokenMatch);
            }
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    @SuppressWarnings("unchecked")
    private <T> Join<Product, T> getOrCreateJoin(Root<Product> root, String attributeName) {
        for (Join<Product, ?> join : root.getJoins()) {
            if (join.getAttribute().getName().equals(attributeName)) {
                return (Join<Product, T>) join;
            }
        }
        return root.join(attributeName, JoinType.LEFT);
    }
}
