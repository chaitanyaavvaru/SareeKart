package com.sareekart.migration;

import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ColorRepository;
import com.sareekart.repository.FabricRepository;
import com.sareekart.repository.OccasionRepository;
import com.sareekart.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class Phase2DataIntegrityTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FabricRepository fabricRepository;

    @Autowired
    private OccasionRepository occasionRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Test
    @DisplayName("Safeguard 10: Verify product count, category count, and non-null foreign keys")
    void testPhase2DataIntegrityAndPreservation() {
        List<Product> products = productRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        assertEquals(25, products.size(), "Product count must remain exactly 25");
        assertEquals(8, categories.size(), "Category count must remain exactly 8");

        for (Product product : products) {
            // Category check
            assertNotNull(product.getCategory(), "Product " + product.getId() + " must have a category");

            // Fabric check
            assertNotNull(product.getFabricEntity(), "Product " + product.getId() + " must have canonical fabricEntity");
            assertNotNull(product.getFabric(), "Product " + product.getId() + " must preserve legacy fabric string");
            assertTrue(product.getFabricEntity().getName().equalsIgnoreCase(product.getFabric()),
                    "Legacy fabric string must match canonical fabric name for product " + product.getId());

            // Occasion check
            assertNotNull(product.getOccasionEntity(), "Product " + product.getId() + " must have canonical occasionEntity");
            assertNotNull(product.getOccasion(), "Product " + product.getId() + " must preserve legacy occasion string");
            assertTrue(product.getOccasionEntity().getName().equalsIgnoreCase(product.getOccasion()),
                    "Legacy occasion string must match canonical occasion name for product " + product.getId());

            // Color check
            assertNotNull(product.getColorEntity(), "Product " + product.getId() + " must have canonical colorEntity");
            assertNotNull(product.getColor(), "Product " + product.getId() + " must preserve legacy color string");
            assertTrue(product.getColorEntity().getName().equalsIgnoreCase(product.getColor()),
                    "Legacy color string must match canonical color name for product " + product.getId());
            assertNotNull(product.getColorEntity().getHexCode(), "Color must have hexCode");
            assertNotNull(product.getColorEntity().getFamily(), "Color must have family");
        }
    }
}
