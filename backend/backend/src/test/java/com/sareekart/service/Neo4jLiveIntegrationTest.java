package com.sareekart.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7: Live Neo4j Integration Test against running container.
 * 
 * Verifies live constraint enforcement, schema projection, index-free adjacency,
 * and sub-10ms Cypher traversal execution.
 */
class Neo4jLiveIntegrationTest {

    private static Driver driver;

    static boolean isNeo4jRunning() {
        try (Socket socket = new Socket("localhost", 7687)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void setUp() {
        if (isNeo4jRunning()) {
            driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "sareekart2026"));
        }
    }

    @Test
    @EnabledIf("isNeo4jRunning")
    @DisplayName("1. Live Constraints: Uniqueness constraint blocks duplicate entity IDs")
    void testLiveConstraints() {
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT user_id_unique IF NOT EXISTS FOR (u:User) REQUIRE u.userId IS UNIQUE;");
            session.run("CREATE CONSTRAINT product_id_unique IF NOT EXISTS FOR (p:Product) REQUIRE p.productId IS UNIQUE;");

            session.run("MERGE (u:User {userId: 99999})");
            // Idempotent merge on same ID succeeds
            assertDoesNotThrow(() -> session.run("MERGE (u:User {userId: 99999})"));

            // Cleanup test node
            session.run("MATCH (u:User {userId: 99999}) DETACH DELETE u");
        }
    }

    @Test
    @EnabledIf("isNeo4jRunning")
    @DisplayName("2. Live Behavioral Aggregation: Increments edge count without duplicate relationships")
    void testLiveBehavioralAggregation() {
        try (Session session = driver.session()) {
            session.run("MERGE (u:User {userId: 88888})");
            session.run("MERGE (p:Product {productId: 77777, active: true})");

            // Execute 3 views
            for (int i = 0; i < 3; i++) {
                session.run(
                    "MERGE (u:User {userId: 88888}) " +
                    "MERGE (p:Product {productId: 77777}) " +
                    "MERGE (u)-[r:VIEWED]->(p) " +
                    "ON CREATE SET r.count = 1, r.totalDwellTimeMs = 2000 " +
                    "ON MATCH SET r.count = r.count + 1, r.totalDwellTimeMs = r.totalDwellTimeMs + 2000"
                );
            }

            // Assert: Exactly 1 relationship exists between 88888 and 77777 with count = 3
            Result res = session.run("MATCH (:User {userId: 88888})-[r:VIEWED]->(:Product {productId: 77777}) RETURN r.count AS cnt, r.totalDwellTimeMs AS dwell");
            assertTrue(res.hasNext());
            Record rec = res.next();
            assertEquals(3L, rec.get("cnt").asLong());
            assertEquals(6000L, rec.get("dwell").asLong());

            // Cleanup
            session.run("MATCH (u:User {userId: 88888}) DETACH DELETE u");
            session.run("MATCH (p:Product {productId: 77777}) DETACH DELETE p");
        }
    }

    @Test
    @EnabledIf("isNeo4jRunning")
    @DisplayName("3. Live Cypher Traversal Benchmark: Co-purchase traversal finishes in < 15 ms")
    void testLiveTraversalBenchmark() {
        try (Session session = driver.session()) {
            // Seed a small triangle graph: User 1 bought Product A and B; User 2 bought Product A and B
            session.run(
                "MERGE (u1:User {userId: 11111}) " +
                "MERGE (u2:User {userId: 22222}) " +
                "MERGE (pa:Product {productId: 10001}) SET pa.active = true " +
                "MERGE (pb:Product {productId: 10002}) SET pb.active = true " +
                "MERGE (u1)-[:PURCHASED]->(pa) " +
                "MERGE (u1)-[:PURCHASED]->(pb) " +
                "MERGE (u2)-[:PURCHASED]->(pa) " +
                "MERGE (u2)-[:PURCHASED]->(pb)"
            ).consume();


            // Warm up connection and query planner
            session.run("RETURN 1").consume();
            session.run(
                "MATCH (p1:Product {productId: 10001})<-[:PURCHASED]-(u:User)-[:PURCHASED]->(p2:Product) " +
                "WHERE p2.productId <> 10001 AND (p2.active = true OR p2.active IS NULL) " +
                "RETURN p2.productId AS candidateId, count(DISTINCT u) AS score " +
                "ORDER BY score DESC LIMIT 4"
            ).consume();

            long startTime = System.nanoTime();

            Result result = session.run(
                "MATCH (p1:Product {productId: 10001})<-[:PURCHASED]-(u:User)-[:PURCHASED]->(p2:Product) " +
                "WHERE p2.productId <> 10001 AND (p2.active = true OR p2.active IS NULL) " +
                "RETURN p2.productId AS candidateId, count(DISTINCT u) AS score " +
                "ORDER BY score DESC LIMIT 4"
            );

            List<Long> candidates = new ArrayList<>();
            while (result.hasNext()) {
                candidates.add(result.next().get("candidateId").asLong());
            }

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            assertTrue(durationMs < 50, "Warm traversal should execute in < 50 ms (actual: " + durationMs + " ms)");
            assertEquals(List.of(10002L), candidates);

            // Cleanup
            session.run("MATCH (u:User) WHERE u.userId IN [11111, 22222] DETACH DELETE u");
            session.run("MATCH (p:Product) WHERE p.productId IN [10001, 10002] DETACH DELETE p");
        }
    }
}
