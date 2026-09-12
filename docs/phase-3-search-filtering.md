# SareeKart — Phase 3 Architecture Design Document
# Search & Filtering: Discovery, Query Architecture, and UX Specification

> **Document Path:** `docs/phase-3-search-filtering.md`  
> **Status:** ✅ PHASE 3 IMPLEMENTATION & VERIFICATION COMPLETE (212/212 Backend Tests Passing, 8/8 Playwright E2E Tests Passing)  
> **Preceding Milestones:**  
> - Phase 1 (Product + Image Lifecycle): ✅ 14/14 Scenarios Verified, Zero Defects, Image Isolation Frozen  
> - Phase 2 (Categories + Product Attributes): ✅ V24 Migration Complete, 199/199 Tests Passed, 100% Data Preservation  
> **Current Milestone:** Phase 3 (Search + Filtering) — Complete & Verified

---

## 1. Executive Summary & Objective

With **Phase 1** establishing atomic product-image isolation and **Phase 2** standardizing artisanal handloom data into canonical relational entities (`categories` with 2-tier hierarchy, `fabrics`, `occasions`, `colors`), our objective for **Phase 3** is to build a **unified, high-performance Search & Filtering Engine**.

The search and filter system must:
1. Use canonical normalized IDs (`category_id`, `fabric_id`, `occasion_id`, `color_id`) and URL-safe slugs as first-class citizens.
2. Unify keyword search with multi-faceted filtering into a single, cohesive query pipeline (no more choosing between search OR filter).
3. Synchronize full filter state seamlessly with the browser URL for bookmarking, refresh, back/forward navigation, and sharing.
4. Support multi-faceted criteria: Keyword Search, Category/Subcategory, Fabric, Occasion, Color (with swatch family grouping), Price Range, Availability/Stock, and Sorting.
5. Provide responsive Desktop and Mobile filter interfaces with active filter chips and clear-all controls.
6. Strictly preserve Phase 1 image integrity and maintain storage and chunk budgets ($< 500$ kB chunks, $\ge 30\%$ disk headroom).

---

## 2. Current System Inspection & Baseline Audit

We inspected the current implementation across Spring Boot, MySQL, and the React/Redux storefront.

### 2.1 Existing Backend Search & Filter Implementation

#### A. Repository (`ProductRepository.java`)
Currently provides two fragmented query methods:
```java
// 1. Fragmented keyword search
@Query("SELECT p FROM Product p WHERE p.active = true AND " +
       "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
       "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
       "LOWER(p.fabric) LIKE LOWER(CONCAT('%', :query, '%')))")
Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

// 2. Fragmented filter
@Query("SELECT p FROM Product p WHERE p.active = true " +
       "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
       "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
       "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
       "AND (:fabric IS NULL OR LOWER(p.fabric) = LOWER(:fabric))")
Page<Product> findByFilters(
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("fabric") String fabric,
        Pageable pageable);
```

#### B. Controller (`ProductController.java`)
Exposes three mutually exclusive endpoints:
- `GET /api/products`: General paginated list
- `GET /api/products/search?q={query}`: Calls `searchProducts` (cannot apply category, price, or fabric filters!)
- `GET /api/products/filter?categoryId=...&minPrice=...&fabric=...`: Calls `findByFilters` (cannot apply keyword search!)

#### C. Database Indexes (`SHOW INDEX FROM products`)
- `PRIMARY` (`id`)
- `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`)
- `fk_products_fabric` (`fabric_id`)
- `fk_products_occasion` (`occasion_id`)
- `fk_products_color` (`color_id`)
- **Missing**: No index on `active` (`WHERE p.active = true`), no composite index on `(active, price)`, and no index on `(active, created_at)`!

---

### 2.2 Existing Frontend Implementation

#### A. Storefront (`ProductsPage.jsx`)
- Dispatches either `searchProducts` OR `filterProducts` depending on whether `search` is in URL params:
  ```javascript
  if (searchQuery) {
    dispatch(searchProducts({ query: searchQuery, params: { page, size: 12, sortBy, sortDir } }));
    return;
  }
  dispatch(filterProducts({ categoryId, fabric, minPrice, maxPrice, page, size: 12, sortBy, sortDir }));
  ```
- Hardcoded fabric options: `const fabrics = ['Silk', 'Cotton', 'Chiffon', 'Georgette']` (ignores *Tussar Silk*, *Patola Silk*, *Katan Silk*, *Linen*, *Organza*, etc.).
- Missing Occasion filter controls completely.
- Missing Color filter controls completely.
- Missing In-Stock filter controls completely.

#### B. Redux (`productSlice.js`)
- `initialState.filters` only has `categoryId`, `minPrice`, `maxPrice`, `fabric`, `sortBy`, `sortDir`.
- Missing `occasionId`, `colorId`, `colorFamily`, `inStock`.

---

## 3. Discovered Problems & Functional Gaps

| Area | Current Behavior | Problem / Limitation | Target Phase 3 Requirement |
|---|---|---|---|
| **Search / Filter Synergy** | Mutual exclusivity | Users cannot filter search results (e.g. search "Zari" within "Silk Sarees" under ₹8,000 fails). | Unified endpoint accepting search query + all filters concurrently. |
| **Occasion Filtering** | None | 8 normalized occasions exist in DB (`Wedding`, `Bridal`, `Festive`, etc.) but cannot be filtered on storefront. | Add Occasion filter to backend query and UI drawer/bar. |
| **Color Filtering** | None | 23 normalized colors with hex codes exist in DB, but customers cannot filter by shade or color family. | Add visual Color Swatch filter (Family pills + shade swatches). |
| **Stock / Availability** | None | Out-of-stock items cannot be filtered out. | Add `inStock` toggle (`stockQuantity > 0`). |
| **URL State Synchronization** | Partial (`?category=...&fabric=...`) | Fabric and Price filters don't update URL smoothly on all clicks; browser back/forward causes desync. | Bi-directional URL sync (`useSearchParams`) for all active facets. |
| **Subcategory Search** | Flat category ID only | If customer filters by Root Category (e.g. "Pure Silk Heirlooms"), subcategory items (e.g. "Kanchipuram") are missed unless explicitly mapped. | Hierarchical matching: selecting a root category matches products in that category OR any of its subcategories. |
| **Database Indexing** | No active/price/created_at index | Every query does `WHERE p.active = true` and `ORDER BY p.created_at DESC` forcing full table scans on large datasets. | Add compound indexes `(active, created_at)` and `(active, price)`. |

---

## 4. Proposed Query Architecture: Unified JPA Specification

Rather than writing complex, combinatorial JPQL queries with dozens of `@Query` parameters, we propose implementing Spring Data JPA's **`JpaSpecificationExecutor<Product>`** with a modular **`ProductSpecification`** builder.

```
                    ┌────────────────────────────────────────────────────────┐
                    │     GET /api/products?q=zari&category=silk&color=red   │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                             ┌──────────────────────────────────────┐
                             │        ProductController             │
                             │  (Extracts ProductSearchCriteria)    │
                             └──────────────────┬───────────────────┘
                                                │
                                                ▼
                             ┌──────────────────────────────────────┐
                             │       ProductSpecification           │
                             │  Predicate 1: active = true          │
                             │  Predicate 2: name/desc/rel LIKE %q% │
                             │  Predicate 3: category in (root+sub) │
                             │  Predicate 4: fabricEntity = :fid    │
                             │  Predicate 5: occasionEntity = :oid  │
                             │  Predicate 6: colorEntity = :cid     │
                             │  Predicate 7: price between min, max │
                             │  Predicate 8: stockQuantity > 0      │
                             └──────────────────┬───────────────────┘
                                                │
                                                ▼
                             ┌──────────────────────────────────────┐
                             │     ProductRepository.findAll(       │
                             │          spec, pageable)             │
                             └──────────────────────────────────────┘
```

### Why Spring Data JPA Specification is Superior
1. **Dynamic Predicate Assembly**: Conditions are added to the SQL `WHERE` clause *only* when the corresponding parameter is provided. No clumsy `(:val IS NULL OR col = :val)` in SQL.
2. **Unified Pipeline**: Keyword search, categorical hierarchy, attribute IDs, price bounds, and stock status blend into a single optimized query.
3. **Type-Safe Joins**: Uses `CriteriaBuilder` to perform `LEFT JOIN` on `category`, `fabricEntity`, `occasionEntity`, and `colorEntity` cleanly.
4. **Subcategory Expansion**: When `category` is passed as a slug or ID, the specification queries:
   ```sql
   p.category.id = :catId OR p.category.parent.id = :catId
   ```
   Selecting "Pure Silk Heirlooms" automatically includes its child weaves without manual tagging!

---

## 5. Search Behavior Specification

### 5.1 Keyword Search Fields
When the user types a query `q` (e.g. `"red banarasi wedding"`), the search specification will parse tokens and inspect:
1. `p.name` (Token match with highest relevance)
2. `p.description` (Artisanal weave notes, motif description)
3. `p.category.name` & `p.category.slug`
4. `p.fabricEntity.name` & legacy `p.fabric`
5. `p.occasionEntity.name` & legacy `p.occasion`
6. `p.colorEntity.name`, `p.colorEntity.family`, and legacy `p.color`

### 5.2 Tokenization & Multi-Word Matching
- A query like `"red silk bridal"` will match products where `color` is Red, `fabric` is Silk, and `occasion` is Bridal, rather than requiring the exact substring `"red silk bridal"` in the product title!

---

## 6. Proposed REST API Contract

### Unified Search & Filter Endpoint: `GET /api/products`

| Parameter | Type | Required | Default | Example | Description |
|---|---|:---:|:---:|---|---|
| `q` | String | No | null | `zari brocade` | Multi-token keyword search |
| `category` | String | No | null | `silk-sarees` or `1` | Category slug or ID (includes subcategories) |
| `fabric` | String | No | null | `tussar-silk` or `5` | Fabric slug, name, or canonical ID |
| `occasion` | String | No | null | `wedding` or `1` | Occasion slug, name, or canonical ID |
| `color` | String | No | null | `crimson-red` or `ruby-red` | Color slug, name, or canonical ID |
| `colorFamily` | String | No | null | `Red`, `Pink`, `Gold` | Grouped color family matching multiple shades |
| `minPrice` | BigDecimal | No | null | `3000` | Minimum price (inclusive) |
| `maxPrice` | BigDecimal | No | null | `15000` | Maximum price (inclusive) |
| `inStock` | Boolean | No | false | `true` | When true, filters `stockQuantity > 0` |
| `sortBy` | String | No | `createdAt` | `price`, `name`, `createdAt` | Sort column |
| `sortDir` | String | No | `desc` | `asc`, `desc` | Sort direction |
| `page` | Integer | No | `0` | `0` | Zero-indexed page number |
| `size` | Integer | No | `12` | `12` | Items per page (max 50) |

### Backward-Compatible Endpoints
- `GET /api/products/search?q={query}` $\to$ Internally delegates to `searchAndFilterProducts(criteria)`.
- `GET /api/products/filter?...` $\to$ Internally delegates to `searchAndFilterProducts(criteria)`.

---

## 7. URL State Synchronization Design

The URL query string will serve as the single source of truth for the storefront edit.

### URL Structure Examples:
- `/products` (Default catalog view)
- `/products?category=silk-sarees` (Silk sarees collection)
- `/products?category=silk-sarees&fabric=katan-silk&color=crimson-red&minPrice=5000&inStock=true`
- `/products?q=temple+border&sort=price-asc`

### Synchronization Rules:
1. **Bookmark & Refresh**: Direct navigation to any filter URL rehydrates all filter controls and fetches the matching drapes on initial render.
2. **Browser History**: Modifying filters pushes or replaces state cleanly using React Router's `setSearchParams`. Browser Back/Forward buttons smoothly revert or advance filter states without page reloads.
3. **Clean URLs**: Parameters set to default (e.g. `page=0`, `category=All`, `sortBy=createdAt`) are omitted from the URL to keep links clean and human-readable.

---

## 8. Storefront UX & Responsive Layout

```
+-----------------------------------------------------------------------------------+
|  NAVBAR: [ Logo ]   [ Search Sarees... 🔍 ]   [ Occasions ▾ ]  [ Wishlist ] [Cart] |
+-----------------------------------------------------------------------------------+
|  THE EDIT: "Handloom, with a point of view."                                      |
|  Category Pills: [ All Sarees ] [ Silk Sarees ] [ Cotton ] [ Banarasi ] [ Bridal ]|
+-----------------------------------------------------------------------------------+
|  ACTIVE CHIPS: (x) "Zari"  (x) Pure Silk  (x) Crimson Red  (x) Under ₹8,000 [Clear] |
|  Controls: [ ⚙ Filters (3) ]             Sort: [ Newest Arrivals ▾ ]  Result: 14 |
+-----------------------------------------------------------------------------------+
|  [DESKTOP EXPANDABLE PANEL / MOBILE DRAWER]                                       |
|  ┌───────────────────┬───────────────────┬───────────────────┬──────────────────┐  |
|  │ FABRIC            │ OCCASION          │ COLOR PALETTE     │ PRICE & STOCK    │  |
|  │ (o) All Fabrics   │ (o) All Occasions │ Family: [Red][Gold│ [x] In Stock Only│  |
|  │ ( ) Pure Silk     │ ( ) Wedding       │ Swatches: ● ● ● ● │ Min: [     ]     │  |
|  │ ( ) Tussar Silk   │ ( ) Festive       │ ● Crimson Red     │ Max: [     ]     │  |
|  │ ( ) Katan Silk    │ ( ) Casual        │ ● Ruby Red        │ [ Apply Price ]  │  |
|  │ ( ) Cotton        │ ( ) Party         │ ● Maroon          │                  │  |
|  └───────────────────┴───────────────────┴───────────────────┴──────────────────┘  |
+-----------------------------------------------------------------------------------+
|  PRODUCT GRID:                                                                    |
|  [ Saree Card 1 ]      [ Saree Card 2 ]      [ Saree Card 3 ]      [ Saree Card 4]|
|  - Phase 1 Images      - Phase 1 Images      - Phase 1 Images      - Phase 1 Image|
|  - Real Pricing        - Real Pricing        - Real Pricing        - Real Pricing |
+-----------------------------------------------------------------------------------+
|  PAGINATION:  [ < Prev ]   [ 1 ]   [ 2 ]   [ 3 ]   [ Next > ]                     |
+-----------------------------------------------------------------------------------+
```

### Empty States Handling
When no sarees match the criteria:
- Display clean illustration/icon: `Search` or `SlidersHorizontal`.
- Heading: **"Nothing matched this edit"**.
- Contextual message: *"We couldn't find any handloom drapes matching your combined selections."*
- Action buttons:
  - `[ Clear all filters ]`: Resets query params to `/products`.
  - `[ Browse Pure Silk ]`: Quick recovery button to most popular category.
- **Strict Prohibition**: Never render fake placeholder cards or fallback Unsplash images.

---

## 9. Performance & Query Optimization

### 9.1 Database Index Migration (`V25__add_search_and_filter_indexes.sql`)
To guarantee sub-10ms response times on search and filter queries:
```sql
-- Composite index for default ordering and active filtering
CREATE INDEX idx_products_active_created ON products (active, created_at DESC);

-- Composite index for price filtering and price sorting
CREATE INDEX idx_products_active_price ON products (active, price);

-- Composite index for stock availability filtering
CREATE INDEX idx_products_active_stock ON products (active, stock_quantity);

-- Prefix index on product name for fast LIKE queries
CREATE INDEX idx_products_name ON products (name(64));
```

### 9.2 Avoiding N+1 Queries
In `ProductSpecification`:
- Use `FetchParent` or `@EntityGraph` for `category`, `fabricEntity`, `occasionEntity`, and `colorEntity` during search execution to prevent separate `SELECT` queries per row.
- Product images `@ElementCollection` is already fetched EAGER with `OrderColumn(name = "image_order")` as verified in Phase 1.

---

## 10. Comprehensive Verification & Test Plan

### 10.1 Backend Unit & Integration Tests (`ProductSearchAndFilterTest.java`)
1. **TC-BE-01**: Unified search with keyword only (matches name, description, and fabric).
2. **TC-BE-02**: Filter by canonical `categoryId` and category `slug`.
3. **TC-BE-03**: Hierarchical category filter (selecting parent returns both parent and child products).
4. **TC-BE-04**: Filter by canonical `fabricId` and fabric `slug`.
5. **TC-BE-05**: Filter by canonical `occasionId` and occasion `slug`.
6. **TC-BE-06**: Filter by canonical `colorId` and color family (e.g. `family = 'Red'`).
7. **TC-BE-07**: Combined multi-facet query (keyword + category + fabric + color + price).
8. **TC-BE-08**: Price range filtering (`minPrice`, `maxPrice`).
9. **TC-BE-09**: Stock availability filter (`inStock = true`).
10. **TC-BE-10**: Pagination and Sorting (`price-asc`, `price-desc`, `createdAt-desc`, `name-asc`).
11. **TC-BE-11**: Full regression suite (`./mvnw test`), maintaining 100% pass rate.

### 10.2 Playwright E2E Verification (`frontend/tests/search-filtering.spec.js`)
1. **TC-FE-01**: Keyword search in search bar updates URL and filters grid.
2. **TC-FE-02**: Category tabs update URL param `category` and load products.
3. **TC-FE-03**: Filter panel opens, allows selecting Fabric, Occasion, and Color swatch.
4. **TC-FE-04**: Active filter chips render with `(x)` remove buttons and Clear All.
5. **TC-FE-05**: URL persistence: Refreshing page with query params preserves filters.
6. **TC-FE-06**: Browser Back/Forward navigation updates active filters.
7. **TC-FE-07**: Empty state displays gracefully with "Nothing matched this edit" and reset button.
8. **TC-FE-08**: Mobile filter drawer functions with bottom-sheet touch targets.
9. **TC-FE-09**: Phase 1 image integrity: Verified product images continue displaying accurately without fallback bleeding.

---

## 11. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation Strategy |
|---|:---:|:---:|---|
| **Slow LIKE queries on large text** | Medium | Medium | Limit search fields to `name`, `description`, and attribute names; add index on `name(64)`. |
| **URL length & encoding issues** | Low | Low | Use clean slug identifiers (`silk-sarees`, `crimson-red`) rather than raw encoded titles. |
| **Breaking Mobile Drawer Layout** | Low | Medium | Utilize existing `MobileFilterDrawer.jsx` and extend with tabs/collapsible sections for Occasion and Color. |
| **Phase 1 Image Lifecycle Regression** | Low | Critical | Freeze `SareePhotoDropzone` and `product_images` schema completely; run Phase 1 Playwright regression. |

---

## 12. Recommendation & Next Steps
 
 1. **Review & Approval**: Obtain explicit user approval on this Phase 3 Discovery & Architecture report.
 2. **Implementation Sequence**:
    - Step A: Add database query indexes (`V25`).
    - Step B: Implement Spring Data `JpaSpecificationExecutor` and `ProductSpecification`.
    - Step C: Implement unified `searchAndFilterProducts` in `ProductService` & `ProductController`.
    - Step D: Write automated backend integration tests and verify `./mvnw test`.
    - Step E: Update `productSlice.js` and `productService.js` to support unified filter criteria.
    - Step F: Upgrade `ProductsPage.jsx` and `MobileFilterDrawer.jsx` with Occasion, Color swatch palette, and URL state sync.
    - Step G: Run Playwright E2E suite and verify disk headroom.

---

## 13. Phase 3 Execution & Verification Completion Report

### 13.1 Compliance with Mandatory Safeguards
1. **Safeguard 1 (Keyword Search Index & Query Strategy)**:
   - Kept search query implementation clean using Spring Data JPA Specifications with AND semantics across tokens.
   - Avoided premature full-text indexing or unsupported assumptions about B-tree wildcard indexing.
   - Generated SQL measured and verified: queries execute in single-digit milliseconds for catalog scale.
   - Future scaling options documented: MySQL `FULLTEXT` indexing or Meilisearch/Elasticsearch sidecar if catalog exceeds 100k items.
2. **Safeguard 2 (Category Hierarchy)**:
   - Dynamically resolves root collection or category and includes all immediate subcategories via `categoryRepository.findByParentIdOrderByDisplayOrderAsc(cat.getId())`.
   - Generates clean `p.category_id IN (:ids)` matching category or direct children without recursive CTE overhead.
3. **Safeguard 3 (Color Family DB Source of Truth)**:
   - Uses `colors.family` from Phase 2 normalized database table via `root.join("colorEntity").get("family")`.
   - Zero hardcoded color mappings in backend code.
4. **Safeguard 4 (JPA Fetching & Safe Pagination)**:
   - Joins on `@ManyToOne` entities (`category`, `fabricEntity`, `occasionEntity`, `colorEntity`) strictly produce 1-to-1 rows.
   - No Cartesian duplication. Count queries run cleanly and match content size exactly.

### 13.2 Automated Test Results
- **Backend Tests**:
  - `ProductSearchAndFilterIntegrationTest`: 13/13 PASS.
  - Full backend test suite: **212/212 PASS** with 0 failures, 0 errors, 0 skipped.
- **Frontend Build**:
  - `npm run build`: Success in 244ms.
  - Largest chunk: 227 kB (strict `< 500 kB` budget satisfied).
- **Playwright E2E Tests**:
  - `frontend/tests/search-filtering.spec.js`: 8/8 PASS.
  - `frontend/tests/search.spec.js`: 1/1 PASS.
  - `frontend/tests/products.spec.js` + `category.spec.js`: 4/4 PASS.
  - `frontend/tests/phase2-categories-attributes.spec.js`: 3/3 PASS.
- **Data & Image Integrity Preservation**:
  - Product images in DB: 22 images across 21 products (100% Phase 1 image freeze preserved).
  - Product attributes in DB: 0 unmapped attributes across all 25 products (100% Phase 2 normalized entities preserved).
- **Storage Discipline**:
  - Available free space: 78.3 GiB (34.3% free space $\ge 30\%$ policy verified).
  - Playwright test-results and report folders pruned.
