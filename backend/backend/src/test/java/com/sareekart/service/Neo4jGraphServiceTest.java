package com.sareekart.service;

import com.sareekart.entity.*;
import com.sareekart.repository.*;
import com.sareekart.service.impl.Neo4jGraphServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Neo4jGraphServiceTest {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private Result result;

    @Mock
    private Record record;

    @Mock
    private Value value;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FabricRepository fabricRepository;

    @Mock
    private OccasionRepository occasionRepository;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerEventRepository customerEventRepository;

    @Mock
    private GraphSyncFailureRepository graphSyncFailureRepository;

    private Neo4jGraphServiceImpl graphService;

    @BeforeEach
    void setUp() {
        graphService = new Neo4jGraphServiceImpl(
                driver,
                productRepository,
                categoryRepository,
                fabricRepository,
                occasionRepository,
                colorRepository,
                orderRepository,
                orderItemRepository,
                customerEventRepository,
                graphSyncFailureRepository
        );
    }

    @Test
    @DisplayName("1. Schema Constraints: Initializes unique constraints for all 6 required node labels")
    void initializeSchemaConstraints_runsAllSixConstraints() {
        when(driver.session()).thenReturn(session);

        graphService.initializeSchemaConstraints();

        verify(session, times(6)).run(anyString());
        verify(session).run("CREATE CONSTRAINT user_id_unique IF NOT EXISTS FOR (u:User) REQUIRE u.userId IS UNIQUE;");
        verify(session).run("CREATE CONSTRAINT product_id_unique IF NOT EXISTS FOR (p:Product) REQUIRE p.productId IS UNIQUE;");
        verify(session).run("CREATE CONSTRAINT category_id_unique IF NOT EXISTS FOR (c:Category) REQUIRE c.categoryId IS UNIQUE;");
        verify(session).run("CREATE CONSTRAINT fabric_id_unique IF NOT EXISTS FOR (f:Fabric) REQUIRE f.fabricId IS UNIQUE;");
        verify(session).run("CREATE CONSTRAINT occasion_id_unique IF NOT EXISTS FOR (o:Occasion) REQUIRE o.occasionId IS UNIQUE;");
        verify(session).run("CREATE CONSTRAINT color_id_unique IF NOT EXISTS FOR (c:Color) REQUIRE c.colorId IS UNIQUE;");
    }

    @Test
    @DisplayName("2. Catalog Seeding: Correctly maps categories, fabrics, occasions, colors, and products")
    void seedCatalog_executesUnwindMergeQueries() {
        when(driver.session()).thenReturn(session);
        when(session.run(anyString(), any(Value.class))).thenReturn(result);

        Category cat = Category.builder().id(1L).name("Silk Sarees").slug("silk-sarees").build();
        Fabric fab = Fabric.builder().id(2L).name("Kanchipuram Silk").slug("kanchipuram").build();
        Occasion occ = Occasion.builder().id(3L).name("Wedding").slug("wedding").build();
        Color col = Color.builder().id(4L).name("Ruby Red").slug("ruby-red").family("Red").build();
        Product prod = Product.builder()
                .id(10L)
                .name("Bridal Silk Saree")
                .category(cat)
                .fabricEntity(fab)
                .occasionEntity(occ)
                .colorEntity(col)
                .active(true)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        when(fabricRepository.findAll()).thenReturn(List.of(fab));
        when(occasionRepository.findAll()).thenReturn(List.of(occ));
        when(colorRepository.findAll()).thenReturn(List.of(col));
        when(productRepository.findAll()).thenReturn(List.of(prod));

        graphService.seedCatalog();

        verify(session, atLeast(5)).run(anyString(), any(Value.class));
    }

    @Test
    @DisplayName("3. Product Deactivation: Sets active=false in Neo4j (soft deletion)")
    void deactivateProduct_executesSoftDeleteCypher() {
        when(driver.session()).thenReturn(session);
        when(session.run(anyString(), any(Value.class))).thenReturn(result);

        graphService.deactivateProduct(10L);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).run(queryCaptor.capture(), any(Value.class));
        assertTrue(queryCaptor.getValue().contains("SET p.active = false"));
    }

    @Test
    @DisplayName("4. Traversal Query: findFrequentlyBoughtTogether extracts candidate IDs")
    void findFrequentlyBoughtTogether_extractsCandidates() {
        when(driver.session()).thenReturn(session);
        when(session.run(anyString(), any(Value.class))).thenReturn(result);
        when(result.hasNext()).thenReturn(true, true, false);
        when(result.next()).thenReturn(record);
        when(record.get("candidateId")).thenReturn(value);
        when(value.asLong()).thenReturn(20L, 30L);

        List<Long> candidates = graphService.findFrequentlyBoughtTogether(10L, 4);

        assertEquals(2, candidates.size());
        assertEquals(20L, candidates.get(0));
        assertEquals(30L, candidates.get(1));
    }

    @Test
    @DisplayName("5. Behavioral Aggregation: recordProductView executes idempotent ON CREATE / ON MATCH")
    void recordProductView_executesAggregatedUpsert() {
        when(driver.session()).thenReturn(session);
        when(session.run(anyString(), any(Value.class))).thenReturn(result);

        graphService.recordProductView(1L, 10L, 5000L, "2026-09-15T12:00:00Z");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).run(queryCaptor.capture(), any(Value.class));
        String cypher = queryCaptor.getValue();
        assertTrue(cypher.contains("MERGE (u:User {userId: $userId})"));
        assertTrue(cypher.contains("MERGE (p:Product {productId: $productId})"));
        assertTrue(cypher.contains("MERGE (u)-[r:VIEWED]->(p)"));
        assertTrue(cypher.contains("ON CREATE SET"));
        assertTrue(cypher.contains("ON MATCH SET"));
    }
}
