package com.sareekart.service.impl;

import com.sareekart.entity.*;
import com.sareekart.repository.*;
import com.sareekart.service.GraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 7: Knowledge Graph Service Implementation using Neo4j Java Driver.
 * 
 * Non-Negotiable Guarantees:
 * 1. MySQL remains the sole authoritative source of truth.
 * 2. Neo4j is strictly a relationship and traversal projection layer.
 * 3. Absolute Failure Isolation: Neo4j outages never interrupt commerce.
 * 4. Circuit Breaker protection on all graph queries.
 * 5. Dead-Letter Queue persistence in graph_sync_failures.
 */
@Service
public class Neo4jGraphServiceImpl implements GraphService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphServiceImpl.class);

    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_RESET_TIMEOUT_MS = 30_000L;

    private final Driver driver;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FabricRepository fabricRepository;
    private final OccasionRepository occasionRepository;
    private final ColorRepository colorRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerEventRepository customerEventRepository;
    private final GraphSyncFailureRepository graphSyncFailureRepository;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private volatile long lastFailureTime = 0L;

    @Autowired
    public Neo4jGraphServiceImpl(
            @Autowired(required = false) Driver driver,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            FabricRepository fabricRepository,
            OccasionRepository occasionRepository,
            ColorRepository colorRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CustomerEventRepository customerEventRepository,
            GraphSyncFailureRepository graphSyncFailureRepository) {
        this.driver = driver;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.fabricRepository = fabricRepository;
        this.occasionRepository = occasionRepository;
        this.colorRepository = colorRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerEventRepository = customerEventRepository;
        this.graphSyncFailureRepository = graphSyncFailureRepository;
    }

    private boolean isCircuitPermitted() {
        if (driver == null) {
            return false;
        }
        if (circuitOpen.get()) {
            long elapsed = System.currentTimeMillis() - lastFailureTime;
            if (elapsed > CIRCUIT_BREAKER_RESET_TIMEOUT_MS) {
                log.info("Neo4j circuit breaker entered HALF-OPEN state, probing connection...");
                return true;
            }
            return false;
        }
        return true;
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
        if (circuitOpen.get()) {
            circuitOpen.set(false);
            log.info("Neo4j circuit breaker CLOSED: connectivity recovered.");
        }
    }

    private void recordFailure(Throwable t) {
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD && !circuitOpen.get()) {
            circuitOpen.set(true);
            log.warn("Neo4j circuit breaker OPENED after {} consecutive failures. Error: {}", failures, t.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        if (!isCircuitPermitted()) {
            return false;
        }
        try (Session session = driver.session()) {
            session.run("RETURN 1").consume();
            recordSuccess();
            return true;
        } catch (Exception e) {
            recordFailure(e);
            return false;
        }
    }

    @Override
    public void initializeSchemaConstraints() {
        if (!isCircuitPermitted()) {
            log.warn("Neo4j unavailable. Skipping constraint initialization.");
            return;
        }
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT user_id_unique IF NOT EXISTS FOR (u:User) REQUIRE u.userId IS UNIQUE;");
            session.run("CREATE CONSTRAINT product_id_unique IF NOT EXISTS FOR (p:Product) REQUIRE p.productId IS UNIQUE;");
            session.run("CREATE CONSTRAINT category_id_unique IF NOT EXISTS FOR (c:Category) REQUIRE c.categoryId IS UNIQUE;");
            session.run("CREATE CONSTRAINT fabric_id_unique IF NOT EXISTS FOR (f:Fabric) REQUIRE f.fabricId IS UNIQUE;");
            session.run("CREATE CONSTRAINT occasion_id_unique IF NOT EXISTS FOR (o:Occasion) REQUIRE o.occasionId IS UNIQUE;");
            session.run("CREATE CONSTRAINT color_id_unique IF NOT EXISTS FOR (c:Color) REQUIRE c.colorId IS UNIQUE;");
            recordSuccess();
            log.info("Neo4j Phase 7 unique constraints verified successfully.");
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Failed to initialize Neo4j constraints: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void seedCatalog() {
        if (!isCircuitPermitted()) {
            log.warn("Neo4j unavailable. Skipping catalog seeding.");
            return;
        }

        try (Session session = driver.session()) {
            // 1. Categories
            List<Category> categories = categoryRepository.findAll();
            List<Map<String, Object>> catMaps = new ArrayList<>();
            List<Map<String, Object>> hierarchyMaps = new ArrayList<>();
            for (Category c : categories) {
                Map<String, Object> map = new HashMap<>();
                map.put("categoryId", c.getId());
                map.put("name", c.getName());
                map.put("slug", c.getSlug() != null ? c.getSlug() : c.getName().toLowerCase().replace(" ", "-"));
                catMaps.add(map);

                if (c.getParent() != null) {
                    Map<String, Object> hMap = new HashMap<>();
                    hMap.put("childId", c.getId());
                    hMap.put("parentId", c.getParent().getId());
                    hierarchyMaps.add(hMap);
                }
            }

            session.run(
                "UNWIND $categories AS cat " +
                "MERGE (c:Category {categoryId: cat.categoryId}) " +
                "SET c.name = cat.name, c.slug = cat.slug",
                Values.parameters("categories", catMaps)
            ).consume();

            if (!hierarchyMaps.isEmpty()) {
                session.run(
                    "UNWIND $hierarchies AS h " +
                    "MATCH (child:Category {categoryId: h.childId}) " +
                    "MATCH (parent:Category {categoryId: h.parentId}) " +
                    "MERGE (child)-[:CHILD_OF]->(parent)",
                    Values.parameters("hierarchies", hierarchyMaps)
                ).consume();
            }

            // 2. Fabrics
            List<Fabric> fabrics = fabricRepository.findAll();
            List<Map<String, Object>> fabMaps = new ArrayList<>();
            for (Fabric f : fabrics) {
                Map<String, Object> map = new HashMap<>();
                map.put("fabricId", f.getId());
                map.put("name", f.getName());
                map.put("slug", f.getSlug());
                fabMaps.add(map);
            }
            if (!fabMaps.isEmpty()) {
                session.run(
                    "UNWIND $fabrics AS fab " +
                    "MERGE (f:Fabric {fabricId: fab.fabricId}) " +
                    "SET f.name = fab.name, f.slug = fab.slug",
                    Values.parameters("fabrics", fabMaps)
                ).consume();
            }

            // 3. Occasions
            List<Occasion> occasions = occasionRepository.findAll();
            List<Map<String, Object>> occMaps = new ArrayList<>();
            for (Occasion o : occasions) {
                Map<String, Object> map = new HashMap<>();
                map.put("occasionId", o.getId());
                map.put("name", o.getName());
                map.put("slug", o.getSlug());
                occMaps.add(map);
            }
            if (!occMaps.isEmpty()) {
                session.run(
                    "UNWIND $occasions AS occ " +
                    "MERGE (o:Occasion {occasionId: occ.occasionId}) " +
                    "SET o.name = occ.name, o.slug = occ.slug",
                    Values.parameters("occasions", occMaps)
                ).consume();
            }

            // 4. Colors
            List<Color> colors = colorRepository.findAll();
            List<Map<String, Object>> colMaps = new ArrayList<>();
            for (Color col : colors) {
                Map<String, Object> map = new HashMap<>();
                map.put("colorId", col.getId());
                map.put("name", col.getName());
                map.put("slug", col.getSlug());
                map.put("family", col.getFamily());
                colMaps.add(map);
            }
            if (!colMaps.isEmpty()) {
                session.run(
                    "UNWIND $colors AS col " +
                    "MERGE (c:Color {colorId: col.colorId}) " +
                    "SET c.name = col.name, c.slug = col.slug, c.family = col.family",
                    Values.parameters("colors", colMaps)
                ).consume();
            }

            // 5. Products & Structural Links
            List<Product> products = productRepository.findAll();
            List<Map<String, Object>> prodMaps = new ArrayList<>();
            for (Product p : products) {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", p.getId());
                map.put("active", p.getActive() != null ? p.getActive() : true);
                map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
                map.put("fabricId", p.getFabricEntity() != null ? p.getFabricEntity().getId() : null);
                map.put("occasionId", p.getOccasionEntity() != null ? p.getOccasionEntity().getId() : null);
                map.put("colorId", p.getColorEntity() != null ? p.getColorEntity().getId() : null);
                prodMaps.add(map);
            }

            if (!prodMaps.isEmpty()) {
                session.run(
                    "UNWIND $products AS prod " +
                    "MERGE (p:Product {productId: prod.productId}) " +
                    "SET p.active = prod.active " +
                    "WITH p, prod " +
                    "FOREACH (_ IN CASE WHEN prod.categoryId IS NOT NULL THEN [1] ELSE [] END | " +
                    "    MERGE (c:Category {categoryId: prod.categoryId}) " +
                    "    MERGE (p)-[:BELONGS_TO]->(c) " +
                    ") " +
                    "FOREACH (_ IN CASE WHEN prod.fabricId IS NOT NULL THEN [1] ELSE [] END | " +
                    "    MERGE (f:Fabric {fabricId: prod.fabricId}) " +
                    "    MERGE (p)-[:MADE_OF]->(f) " +
                    ") " +
                    "FOREACH (_ IN CASE WHEN prod.occasionId IS NOT NULL THEN [1] ELSE [] END | " +
                    "    MERGE (o:Occasion {occasionId: prod.occasionId}) " +
                    "    MERGE (p)-[:SUITABLE_FOR]->(o) " +
                    ") " +
                    "FOREACH (_ IN CASE WHEN prod.colorId IS NOT NULL THEN [1] ELSE [] END | " +
                    "    MERGE (col:Color {colorId: prod.colorId}) " +
                    "    MERGE (p)-[:HAS_COLOR]->(col) " +
                    ")",
                    Values.parameters("products", prodMaps)
                ).consume();
            }

            recordSuccess();
            log.info("Neo4j catalog seeding completed: {} products, {} categories, {} fabrics, {} occasions, {} colors.",
                    prodMaps.size(), catMaps.size(), fabMaps.size(), occMaps.size(), colMaps.size());
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Neo4j catalog seeding failed: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncProduct(Long productId) {
        if (!isCircuitPermitted() || productId == null) {
            return;
        }
        Optional<Product> prodOpt = productRepository.findById(productId);
        if (prodOpt.isEmpty()) {
            return;
        }
        Product p = prodOpt.get();

        Map<String, Object> map = new HashMap<>();
        map.put("productId", p.getId());
        map.put("active", p.getActive() != null ? p.getActive() : true);
        map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
        map.put("fabricId", p.getFabricEntity() != null ? p.getFabricEntity().getId() : null);
        map.put("occasionId", p.getOccasionEntity() != null ? p.getOccasionEntity().getId() : null);
        map.put("colorId", p.getColorEntity() != null ? p.getColorEntity().getId() : null);

        try (Session session = driver.session()) {
            session.run(
                "MERGE (p:Product {productId: $productId}) " +
                "SET p.active = $active " +
                "WITH p " +
                // Remove old structural relations to prevent stale links
                "OPTIONAL MATCH (p)-[rb:BELONGS_TO]->() DELETE rb " +
                "WITH p " +
                "OPTIONAL MATCH (p)-[rm:MADE_OF]->() DELETE rm " +
                "WITH p " +
                "OPTIONAL MATCH (p)-[rs:SUITABLE_FOR]->() DELETE rs " +
                "WITH p " +
                "OPTIONAL MATCH (p)-[rc:HAS_COLOR]->() DELETE rc " +
                "WITH p " +
                "FOREACH (_ IN CASE WHEN $categoryId IS NOT NULL THEN [1] ELSE [] END | " +
                "    MERGE (c:Category {categoryId: $categoryId}) " +
                "    MERGE (p)-[:BELONGS_TO]->(c) " +
                ") " +
                "FOREACH (_ IN CASE WHEN $fabricId IS NOT NULL THEN [1] ELSE [] END | " +
                "    MERGE (f:Fabric {fabricId: $fabricId}) " +
                "    MERGE (p)-[:MADE_OF]->(f) " +
                ") " +
                "FOREACH (_ IN CASE WHEN $occasionId IS NOT NULL THEN [1] ELSE [] END | " +
                "    MERGE (o:Occasion {occasionId: $occasionId}) " +
                "    MERGE (p)-[:SUITABLE_FOR]->(o) " +
                ") " +
                "FOREACH (_ IN CASE WHEN $colorId IS NOT NULL THEN [1] ELSE [] END | " +
                "    MERGE (col:Color {colorId: $colorId}) " +
                "    MERGE (p)-[:HAS_COLOR]->(col) " +
                ")",
                map
            ).consume();
            recordSuccess();
            log.debug("Synced product {} to Neo4j.", productId);
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "PRODUCT_SYNC", "productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void deactivateProduct(Long productId) {
        if (!isCircuitPermitted() || productId == null) {
            return;
        }
        try (Session session = driver.session()) {
            session.run(
                "MATCH (p:Product {productId: $productId}) SET p.active = false",
                Values.parameters("productId", productId)
            ).consume();
            recordSuccess();
            log.info("Marked product {} inactive in Neo4j (soft deletion).", productId);
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "PRODUCT_DEACTIVATE", "productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void deleteProduct(Long productId) {
        if (!isCircuitPermitted() || productId == null) {
            return;
        }
        try (Session session = driver.session()) {
            session.run(
                "MATCH (p:Product {productId: $productId}) DETACH DELETE p",
                Values.parameters("productId", productId)
            ).consume();
            recordSuccess();
            log.info("Permanently deleted product {} from Neo4j.", productId);
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "PRODUCT_DELETE", "productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void recordProductView(Long userId, Long productId, long dwellTimeMs, String timestamp) {
        if (!isCircuitPermitted() || userId == null || productId == null) {
            return;
        }
        String ts = timestamp != null ? timestamp : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {userId: $userId}) " +
                "MERGE (p:Product {productId: $productId}) " +
                "MERGE (u)-[r:VIEWED]->(p) " +
                "ON CREATE SET " +
                "    r.count = 1, " +
                "    r.totalDwellTimeMs = $dwellTimeMs, " +
                "    r.firstViewedAt = datetime($timestamp), " +
                "    r.lastViewedAt = datetime($timestamp) " +
                "ON MATCH SET " +
                "    r.count = r.count + 1, " +
                "    r.totalDwellTimeMs = r.totalDwellTimeMs + $dwellTimeMs, " +
                "    r.lastViewedAt = datetime($timestamp)",
                Values.parameters("userId", userId, "productId", productId, "dwellTimeMs", dwellTimeMs, "timestamp", ts)
            ).consume();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "PRODUCT_VIEW", "userId=" + userId + ",productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void recordAddToCart(Long userId, Long productId, String timestamp) {
        if (!isCircuitPermitted() || userId == null || productId == null) {
            return;
        }
        String ts = timestamp != null ? timestamp : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {userId: $userId}) " +
                "MERGE (p:Product {productId: $productId}) " +
                "MERGE (u)-[r:CARTED]->(p) " +
                "ON CREATE SET " +
                "    r.count = 1, " +
                "    r.firstAddedAt = datetime($timestamp), " +
                "    r.lastAddedAt = datetime($timestamp) " +
                "ON MATCH SET " +
                "    r.count = r.count + 1, " +
                "    r.lastAddedAt = datetime($timestamp)",
                Values.parameters("userId", userId, "productId", productId, "timestamp", ts)
            ).consume();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "ADD_TO_CART", "userId=" + userId + ",productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void recordWishlist(Long userId, Long productId, boolean active, String timestamp) {
        if (!isCircuitPermitted() || userId == null || productId == null) {
            return;
        }
        String ts = timestamp != null ? timestamp : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {userId: $userId}) " +
                "MERGE (p:Product {productId: $productId}) " +
                "MERGE (u)-[r:WISHLISTED]->(p) " +
                "ON CREATE SET " +
                "    r.active = $active, " +
                "    r.addedAt = datetime($timestamp), " +
                "    r.updatedAt = datetime($timestamp) " +
                "ON MATCH SET " +
                "    r.active = $active, " +
                "    r.updatedAt = datetime($timestamp)",
                Values.parameters("userId", userId, "productId", productId, "active", active, "timestamp", ts)
            ).consume();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "WISHLIST", "userId=" + userId + ",productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void recordPurchase(Long userId, Long productId, double amount, String timestamp) {
        if (!isCircuitPermitted() || userId == null || productId == null) {
            return;
        }
        String ts = timestamp != null ? timestamp : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {userId: $userId}) " +
                "MERGE (p:Product {productId: $productId}) " +
                "MERGE (u)-[r:PURCHASED]->(p) " +
                "ON CREATE SET " +
                "    r.orderCount = 1, " +
                "    r.totalSpent = $amount, " +
                "    r.firstPurchasedAt = datetime($timestamp), " +
                "    r.lastPurchasedAt = datetime($timestamp) " +
                "ON MATCH SET " +
                "    r.orderCount = r.orderCount + 1, " +
                "    r.totalSpent = r.totalSpent + $amount, " +
                "    r.lastPurchasedAt = datetime($timestamp)",
                Values.parameters("userId", userId, "productId", productId, "amount", amount, "timestamp", ts)
            ).consume();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "ORDER_COMPLETED", "userId=" + userId + ",productId=" + productId, e.getMessage());
        }
    }

    @Override
    public void recordCategoryAffinity(Long userId, Long categoryId, String timestamp) {
        if (!isCircuitPermitted() || userId == null || categoryId == null) {
            return;
        }
        String ts = timestamp != null ? timestamp : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {userId: $userId}) " +
                "MERGE (c:Category {categoryId: $categoryId}) " +
                "MERGE (u)-[r:AFFINITY_TO]->(c) " +
                "ON CREATE SET " +
                "    r.weight = 1.0, " +
                "    r.firstInteractedAt = datetime($timestamp), " +
                "    r.lastInteractedAt = datetime($timestamp) " +
                "ON MATCH SET " +
                "    r.weight = r.weight + 1.0, " +
                "    r.lastInteractedAt = datetime($timestamp)",
                Values.parameters("userId", userId, "categoryId", categoryId, "timestamp", ts)
            ).consume();
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            enqueueFailure(null, null, "CATEGORY_VIEW", "userId=" + userId + ",categoryId=" + categoryId, e.getMessage());
        }
    }

    @Override
    public List<Long> findFrequentlyBoughtTogether(Long productId, int limit) {
        if (!isCircuitPermitted() || productId == null) {
            return Collections.emptyList();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (p1:Product {productId: $productId})<-[:PURCHASED]-(u:User)-[:PURCHASED]->(p2:Product) " +
                "WHERE p2.productId <> $productId AND (p2.active = true OR p2.active IS NULL) " +
                "RETURN p2.productId AS candidateId, count(DISTINCT u) AS score " +
                "ORDER BY score DESC " +
                "LIMIT $limit",
                Values.parameters("productId", productId, "limit", cappedLimit)
            );
            List<Long> candidates = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                candidates.add(record.get("candidateId").asLong());
            }
            recordSuccess();
            return candidates;
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Error querying frequently bought together for product {}: {}", productId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> findCustomersAlsoViewed(Long productId, int limit) {
        if (!isCircuitPermitted() || productId == null) {
            return Collections.emptyList();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (p1:Product {productId: $productId})<-[:VIEWED]-(u:User)-[:VIEWED]->(p2:Product) " +
                "WHERE p2.productId <> $productId AND (p2.active = true OR p2.active IS NULL) " +
                "RETURN p2.productId AS candidateId, count(DISTINCT u) AS score " +
                "ORDER BY score DESC " +
                "LIMIT $limit",
                Values.parameters("productId", productId, "limit", cappedLimit)
            );
            List<Long> candidates = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                candidates.add(record.get("candidateId").asLong());
            }
            recordSuccess();
            return candidates;
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Error querying customers also viewed for product {}: {}", productId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> findPersonalizedRecommendations(Long userId, int limit) {
        if (!isCircuitPermitted() || userId == null) {
            return Collections.emptyList();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (u:User {userId: $userId})-[:PURCHASED|WISHLISTED|CARTED]->(interacted:Product) " +
                "MATCH (interacted)-[:MADE_OF]->(f:Fabric)<-[:MADE_OF]-(candidate:Product) " +
                "WHERE candidate.productId <> interacted.productId " +
                "  AND (candidate.active = true OR candidate.active IS NULL) " +
                "  AND NOT (u)-[:PURCHASED]->(candidate) " +
                "RETURN candidate.productId AS candidateId, count(DISTINCT f) AS score " +
                "ORDER BY score DESC " +
                "LIMIT $limit",
                Values.parameters("userId", userId, "limit", cappedLimit)
            );
            List<Long> candidates = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                candidates.add(record.get("candidateId").asLong());
            }
            recordSuccess();
            return candidates;
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Error querying personalized recommendations for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> findSimilarSarees(Long productId, int limit) {
        if (!isCircuitPermitted() || productId == null) {
            return Collections.emptyList();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (target:Product {productId: $productId}) " +
                "MATCH (target)-[:MADE_OF]->(f:Fabric)<-[:MADE_OF]-(candidate:Product) " +
                "MATCH (target)-[:SUITABLE_FOR]->(o:Occasion)<-[:SUITABLE_FOR]-(candidate) " +
                "WHERE candidate.productId <> $productId AND (candidate.active = true OR candidate.active IS NULL) " +
                "OPTIONAL MATCH (target)-[:HAS_COLOR]->(c1:Color) " +
                "WITH candidate, c1, c2, count(DISTINCT f) AS fCount, count(DISTINCT o) AS oCount " +
                "WITH candidate, fCount * 3 + oCount * 2 + (CASE WHEN c1.family IS NOT NULL AND c1.family = c2.family THEN 2 ELSE 0 END) AS similarityScore " +
                "RETURN candidate.productId AS candidateId, similarityScore " +
                "ORDER BY similarityScore DESC " +
                "LIMIT $limit",
                Values.parameters("productId", productId, "limit", cappedLimit)
            );
            List<Long> candidates = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                candidates.add(record.get("candidateId").asLong());
            }
            recordSuccess();
            return candidates;
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Error querying similar sarees for product {}: {}", productId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void seedHistoricalInteractions() {
        if (!isCircuitPermitted()) {
            log.warn("Neo4j unavailable. Skipping historical interactions seeding.");
            return;
        }

        try {
            // 1. Project past non-cancelled orders
            List<Order> orders = orderRepository.findAll();
            int orderCount = 0;
            for (Order o : orders) {
                if (o.getUser() == null || o.getStatus() == OrderStatus.CANCELLED) {
                    continue;
                }
                Long userId = o.getUser().getId();
                String timestamp = o.getCreatedAt() != null ?
                        DateTimeFormatter.ISO_INSTANT.format(o.getCreatedAt().atZone(java.time.ZoneId.of("UTC")).toInstant()) :
                        null;

                for (OrderItem item : o.getItems()) {
                    if (item.getProduct() != null) {
                        double itemTotal = item.getPrice() != null ? item.getPrice().doubleValue() * item.getQuantity() : 0.0;
                        recordPurchase(userId, item.getProduct().getId(), itemTotal, timestamp);
                        orderCount++;
                    }
                }
            }

            // 2. Project past customer telemetry events
            List<CustomerEvent> events = customerEventRepository.findAll();
            int eventCount = 0;
            for (CustomerEvent ev : events) {
                if (ev.getUser() == null || ev.getEntityId() == null) {
                    continue;
                }
                Long userId = ev.getUser().getId();
                String timestamp = ev.getCreatedAt() != null ?
                        DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt().atZone(java.time.ZoneId.of("UTC")).toInstant()) :
                        null;

                String type = ev.getEventType();
                if ("PRODUCT_VIEW".equalsIgnoreCase(type)) {
                    long dwellTime = 5000;
                    recordProductView(userId, ev.getEntityId(), dwellTime, timestamp);
                    eventCount++;
                } else if ("ADD_TO_CART".equalsIgnoreCase(type)) {
                    recordAddToCart(userId, ev.getEntityId(), timestamp);
                    eventCount++;
                } else if ("ADD_TO_WISHLIST".equalsIgnoreCase(type)) {
                    recordWishlist(userId, ev.getEntityId(), true, timestamp);
                    eventCount++;
                } else if ("CATEGORY_VIEW".equalsIgnoreCase(type)) {
                    recordCategoryAffinity(userId, ev.getEntityId(), timestamp);
                    eventCount++;
                }
            }

            log.info("Seeded historical graph interactions: {} order items, {} customer events.", orderCount, eventCount);
        } catch (Exception e) {
            log.warn("Error during historical interactions seeding: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getGraphStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        boolean available = isAvailable();
        stats.put("available", available);
        stats.put("circuitOpen", circuitOpen.get());
        stats.put("consecutiveFailures", consecutiveFailures.get());

        if (!available) {
            return stats;
        }

        try (Session session = driver.session()) {
            // Count nodes per label
            List<String> labels = List.of("User", "Product", "Category", "Fabric", "Occasion", "Color");
            Map<String, Long> nodeCounts = new LinkedHashMap<>();
            for (String label : labels) {
                Result r = session.run("MATCH (n:" + label + ") RETURN count(n) AS cnt");
                if (r.hasNext()) {
                    nodeCounts.put(label, r.next().get("cnt").asLong());
                }
            }
            stats.put("nodeCounts", nodeCounts);

            // Count relationships per type
            List<String> rels = List.of("BELONGS_TO", "MADE_OF", "SUITABLE_FOR", "HAS_COLOR", "CHILD_OF",
                    "VIEWED", "CARTED", "WISHLISTED", "PURCHASED", "AFFINITY_TO");
            Map<String, Long> relCounts = new LinkedHashMap<>();
            for (String rel : rels) {
                Result r = session.run("MATCH ()-[r:" + rel + "]->() RETURN count(r) AS cnt");
                if (r.hasNext()) {
                    relCounts.put(rel, r.next().get("cnt").asLong());
                }
            }
            stats.put("relationshipCounts", relCounts);
            recordSuccess();
        } catch (Exception e) {
            recordFailure(e);
            log.warn("Failed to retrieve Neo4j graph statistics: {}", e.getMessage());
        }

        return stats;
    }

    private void enqueueFailure(Long eventId, String clientEventId, String eventType, String payload, String errorMessage) {
        try {
            GraphSyncFailure failure = GraphSyncFailure.builder()
                    .eventId(eventId)
                    .clientEventId(clientEventId)
                    .eventType(eventType)
                    .payload(payload != null ? "{\"details\":\"" + payload.replace("\"", "\\\"") + "\"}" : null)
                    .errorMessage(errorMessage != null && errorMessage.length() > 2000 ? errorMessage.substring(0, 2000) : errorMessage)
                    .retryCount(0)
                    .build();
            graphSyncFailureRepository.save(failure);
        } catch (Exception ex) {
            log.error("Failed to enqueue graph sync failure into DLQ: {}", ex.getMessage());
        }
    }
}
