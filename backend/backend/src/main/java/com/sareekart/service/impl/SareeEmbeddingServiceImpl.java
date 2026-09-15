package com.sareekart.service.impl;

import com.sareekart.entity.Product;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.SareeEmbeddingService;
import com.sareekart.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Phase 8: Saree Canonical Text & Dense Semantic Embedding Service Implementation.
 * 
 * Implements deterministic domain-aware 384-dimensional dense semantic feature projection
 * for sarees. Captures heritage weave, fabric texture, drape, color palette, and artisanal
 * craft motifs into normalized L2 unit vectors.
 */
@Service
@Slf4j
public class SareeEmbeddingServiceImpl implements SareeEmbeddingService {

    private final ProductRepository productRepository;
    private final VectorSearchService vectorSearchService;
    private final Driver neo4jDriver;

    // Domain Clusters for semantic dimension partitioning (384 dimensions total)
    private static final Map<String, Integer> CATEGORY_MAP = Map.ofEntries(
            Map.entry("kanchipuram", 0), Map.entry("banarasi", 4), Map.entry("chanderi", 8),
            Map.entry("paithani", 12), Map.entry("tussar", 16), Map.entry("chiffon", 20),
            Map.entry("georgette", 24), Map.entry("organza", 28), Map.entry("patola", 32),
            Map.entry("bandhani", 36), Map.entry("handloom", 40), Map.entry("silk mark", 44)
    );

    private static final Map<String, Integer> FABRIC_MAP = Map.ofEntries(
            Map.entry("pure silk", 48), Map.entry("silk", 52), Map.entry("cotton", 56),
            Map.entry("art silk", 60), Map.entry("katan silk", 64), Map.entry("tussar silk", 68),
            Map.entry("linen", 72), Map.entry("crepe", 76), Map.entry("velvet", 80),
            Map.entry("tissue", 84), Map.entry("satin", 88), Map.entry("muslin", 92)
    );

    private static final Map<String, Integer> OCCASION_MAP = Map.ofEntries(
            Map.entry("wedding", 96), Map.entry("bridal", 100), Map.entry("marriage", 104),
            Map.entry("party", 108), Map.entry("festive", 112), Map.entry("festival", 116),
            Map.entry("diwali", 120), Map.entry("reception", 124), Map.entry("puja", 128),
            Map.entry("pooja", 132), Map.entry("traditional", 136), Map.entry("casual", 140)
    );

    private static final Map<String, Integer> COLOR_MAP = Map.ofEntries(
            Map.entry("red", 144), Map.entry("crimson", 148), Map.entry("ruby", 152),
            Map.entry("maroon", 156), Map.entry("pink", 160), Map.entry("gold", 164),
            Map.entry("golden", 168), Map.entry("yellow", 172), Map.entry("green", 176),
            Map.entry("emerald", 180), Map.entry("blue", 184), Map.entry("purple", 188)
    );

    private static final Map<String, Integer> CRAFT_MAP = Map.ofEntries(
            Map.entry("zari", 192), Map.entry("brocade", 198), Map.entry("temple border", 204),
            Map.entry("border", 210), Map.entry("pallu", 216), Map.entry("buta", 222),
            Map.entry("motifs", 228), Map.entry("paisley", 234), Map.entry("handcrafted", 240),
            Map.entry("handwoven", 246), Map.entry("master weavers", 252), Map.entry("jacquard", 258),
            Map.entry("embroidery", 264), Map.entry("contrast border", 270), Map.entry("sheen", 276),
            Map.entry("drape", 282)
    );

    @Autowired
    public SareeEmbeddingServiceImpl(
            ProductRepository productRepository,
            VectorSearchService vectorSearchService,
            @Autowired(required = false) Driver neo4jDriver
    ) {
        this.productRepository = productRepository;
        this.vectorSearchService = vectorSearchService;
        this.neo4jDriver = neo4jDriver;
    }

    @Override
    public String buildCanonicalDescriptor(Product product) {
        if (product == null) {
            return "";
        }

        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Saree";
        String fabricName = product.getFabricEntity() != null ? product.getFabricEntity().getName()
                : (product.getFabric() != null ? product.getFabric() : "Silk");
        String colorName = product.getColorEntity() != null ? product.getColorEntity().getName()
                : (product.getColor() != null ? product.getColor() : "Multi");
        String colorFamily = product.getColorEntity() != null ? product.getColorEntity().getFamily() : "General";
        String hexCode = product.getColorEntity() != null ? product.getColorEntity().getHexCode() : "#000000";
        String occasionName = product.getOccasionEntity() != null ? product.getOccasionEntity().getName()
                : (product.getOccasion() != null ? product.getOccasion() : "Festive");
        String description = product.getDescription() != null ? product.getDescription() : product.getName();

        return String.format(
                "[HERITAGE WEAVE] %s, crafted in authentic %s. [COLOR & AESTHETICS] %s (%s palette), Hex %s. [OCCASION & DRAPE] Tailored for %s celebrations and weddings. [ARTISAN CRAFT] %s",
                categoryName, fabricName, colorName, colorFamily, hexCode, occasionName, description
        );
    }

    @Override
    public float[] generateEmbedding(String text) {
        float[] vector = new float[EMBEDDING_DIMENSION];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String lower = text.toLowerCase(Locale.ROOT);

        // 1. Activate Category Partition (dims 0..47)
        activateCluster(lower, CATEGORY_MAP, vector, 4, 1.8f);

        // 2. Activate Fabric Partition (dims 48..95)
        activateCluster(lower, FABRIC_MAP, vector, 4, 1.6f);

        // 3. Activate Occasion Partition (dims 96..143)
        activateCluster(lower, OCCASION_MAP, vector, 4, 1.4f);

        // 4. Activate Color Partition (dims 144..191)
        activateCluster(lower, COLOR_MAP, vector, 4, 1.5f);

        // 5. Activate Craft & Weave Partition (dims 192..287)
        activateCluster(lower, CRAFT_MAP, vector, 6, 1.3f);

        // 6. Project Contextual Lexical N-grams (dims 288..383)
        projectLexicalNgrams(lower, vector);

        // 7. L2 Normalization
        double normSq = 0.0;
        for (float v : vector) {
            normSq += v * v;
        }
        double norm = Math.sqrt(normSq);
        if (norm > 1e-9) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }

        return vector;
    }

    @Override
    @Transactional(readOnly = true)
    public void seedCatalogEmbeddings() {
        log.info("Starting SareeKart dense embedding generation for product catalog...");
        List<Product> products = productRepository.findAll();
        int count = 0;

        for (Product product : products) {
            try {
                String descriptor = buildCanonicalDescriptor(product);
                float[] embedding = generateEmbedding(descriptor);
                vectorSearchService.registerEmbedding(product.getId(), embedding);
                syncToNeo4j(product.getId(), embedding);
                count++;
            } catch (Exception e) {
                log.warn("Non-blocking error generating embedding for product ID {}: {}", product.getId(), e.getMessage());
            }
        }

        log.info("Successfully generated and indexed dense embeddings for {} catalog sarees.", count);
    }

    @Override
    @Transactional(readOnly = true)
    public void syncProductEmbedding(Long productId) {
        if (productId == null) return;
        productRepository.findById(productId).ifPresent(product -> {
            String descriptor = buildCanonicalDescriptor(product);
            float[] embedding = generateEmbedding(descriptor);
            vectorSearchService.registerEmbedding(productId, embedding);
            syncToNeo4j(productId, embedding);
        });
    }

    private void activateCluster(String text, Map<String, Integer> clusterMap, float[] vector, int span, float weight) {
        for (Map.Entry<String, Integer> entry : clusterMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                int baseDim = entry.getValue();
                for (int i = 0; i < span; i++) {
                    int dim = baseDim + i;
                    if (dim < vector.length) {
                        vector[dim] += weight * (1.0f - (0.15f * i));
                    }
                }
            }
        }
    }

    private void projectLexicalNgrams(String text, float[] vector) {
        String[] tokens = text.split("\\W+");
        for (String token : tokens) {
            if (token.length() < 3) continue;
            int hash = hashToken(token);
            int dim = 288 + (Math.abs(hash) % (384 - 288));
            vector[dim] += 0.8f;

            // Character trigrams
            for (int i = 0; i <= token.length() - 3; i++) {
                String trigram = token.substring(i, i + 3);
                int triHash = hashToken(trigram);
                int triDim = 288 + (Math.abs(triHash) % (384 - 288));
                vector[triDim] += 0.3f;
            }
        }
    }

    private int hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            return token.hashCode();
        }
    }

    private void syncToNeo4j(Long productId, float[] embedding) {
        if (neo4jDriver == null) return;
        try (Session session = neo4jDriver.session()) {
            List<Double> doubleList = new ArrayList<>(embedding.length);
            for (float f : embedding) {
                doubleList.add((double) f);
            }
            session.run(
                    "MATCH (p:Product {id: $productId}) SET p.embedding = $embedding",
                    Values.parameters("productId", productId, "embedding", doubleList)
            );
        } catch (Exception e) {
            log.debug("Non-blocking Neo4j embedding property sync skipped for product {}: {}", productId, e.getMessage());
        }
    }
}
