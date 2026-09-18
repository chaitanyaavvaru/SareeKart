package com.sareekart.dr;

import com.sareekart.entity.Product;
import com.sareekart.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 13 · Stage 7: Disaster Recovery & Rollback Drill Integration Test.
 * 
 * Verifies that the restored disposable database (`sareekart_recovery_drill_db`)
 * passes strict Hibernate schema validation (ddl-auto: validate), that all core
 * business records (catalog, orders, inventory, trousseau, users) are intact,
 * and that application health and query endpoints function 100%.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=${DR_DATASOURCE_URL:jdbc:mysql://localhost:3306/sareekart_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true}",
    "spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}",
    "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root123}",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "neo4j.enabled=false"
})
class DisasterRecoveryDatabaseRestoreDrillTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private TrousseauBoardRepository trousseauBoardRepository;

    @Test
    @DisplayName("DR Drill 1: Schema validation succeeds with ddl-auto: validate")
    void testSchemaValidationSucceeds() {
        assertNotNull(productRepository, "Product repository must be loaded");
        assertNotNull(orderRepository, "Order repository must be loaded");
    }

    @Test
    @DisplayName("DR Drill 2: Catalog and inventory entities restored with exact counts")
    void testCatalogDataIntegrity() {
        long productCount = productRepository.count();
        assertEquals(25L, productCount, "Restored catalog must contain exactly 25 products");

        long categoryCount = categoryRepository.count();
        assertEquals(8L, categoryCount, "Restored categories must contain exactly 8 categories");

        long inventoryCount = inventoryItemRepository.count();
        assertEquals(25L, inventoryCount, "Restored inventory items must contain exactly 25 items");

        Product p1 = productRepository.findById(1L).orElse(null);
        assertNotNull(p1, "Product ID 1 must exist in restored database");
        assertTrue(p1.getName().contains("Banarasi"), "Product name must contain Banarasi");
        assertNotNull(p1.getPrice(), "Product price must not be null");
    }

    @Test
    @DisplayName("DR Drill 3: Customer and order records restored with exact counts")
    void testCustomerAndOrderIntegrity() {
        long userCount = userRepository.count();
        assertEquals(12L, userCount, "Restored users must contain exactly 12 users");

        long orderCount = orderRepository.count();
        assertEquals(50L, orderCount, "Restored orders must contain exactly 50 orders");
    }

    @Test
    @DisplayName("DR Drill 4: Trousseau collaborative state restored with exact counts")
    void testTrousseauStateIntegrity() {
        long boardCount = trousseauBoardRepository.count();
        assertEquals(8L, boardCount, "Restored trousseau boards must contain exactly 8 boards");
    }

    @Test
    @DisplayName("DR Drill 5: Application health and catalog endpoints respond against restored database")
    void testApplicationHealthAndCatalogEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
