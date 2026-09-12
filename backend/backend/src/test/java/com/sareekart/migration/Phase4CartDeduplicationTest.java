package com.sareekart.migration;

import com.sareekart.config.CartConstants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the deterministic deduplication logic specified in V25__enhance_cart_and_wishlist.sql.
 * Deduplication Rule:
 * 1. For each duplicate group (cart_id, product_id), compute SUM(quantity).
 * 2. Cap aggregate quantity at LEAST(SUM(quantity), 10) (CartConstants.MAX_QUANTITY_PER_SKU).
 * 3. Update the primary record (MIN(id)) with the capped quantity.
 * 4. Delete all secondary duplicate records (id != MIN(id)).
 */
class Phase4CartDeduplicationTest {

    static class CartItemRecord {
        long id;
        long cartId;
        long productId;
        int quantity;

        CartItemRecord(long id, long cartId, long productId, int quantity) {
            this.id = id;
            this.cartId = cartId;
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    @Test
    void testDeterministicDeduplicationAlgorithm() {
        List<CartItemRecord> items = new ArrayList<>();
        // Duplicate group 1: Cart 10, Product 25 -> quantities 2, 3, 1 (total = 6 <= 10)
        items.add(new CartItemRecord(1L, 10L, 25L, 2));
        items.add(new CartItemRecord(2L, 10L, 25L, 3));
        items.add(new CartItemRecord(3L, 10L, 25L, 1));

        // Duplicate group 2: Cart 10, Product 30 -> quantities 7, 8 (total = 15 > 10, must cap at 10)
        items.add(new CartItemRecord(4L, 10L, 30L, 7));
        items.add(new CartItemRecord(5L, 10L, 30L, 8));

        // Unique item: Cart 10, Product 40 -> quantity 4
        items.add(new CartItemRecord(6L, 10L, 40L, 4));

        // Duplicate group in another cart: Cart 20, Product 25 -> quantities 1, 2
        items.add(new CartItemRecord(7L, 20L, 25L, 1));
        items.add(new CartItemRecord(8L, 20L, 25L, 2));

        // Execute deterministic deduplication logic mirroring V25 SQL
        Map<String, List<CartItemRecord>> groups = new HashMap<>();
        for (CartItemRecord item : items) {
            String key = item.cartId + ":" + item.productId;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<CartItemRecord> dedupedResult = new ArrayList<>();
        for (List<CartItemRecord> group : groups.values()) {
            if (group.size() > 1) {
                CartItemRecord primary = group.stream().min((a, b) -> Long.compare(a.id, b.id)).orElseThrow();
                int totalQuantity = group.stream().mapToInt(i -> i.quantity).sum();
                int mergedQuantity = Math.min(totalQuantity, CartConstants.MAX_QUANTITY_PER_SKU);
                primary.quantity = mergedQuantity;
                dedupedResult.add(primary);
            } else {
                dedupedResult.add(group.get(0));
            }
        }

        // Verify total surviving rows
        assertEquals(4, dedupedResult.size(), "Each unique (cart_id, product_id) must have exactly 1 row");

        // Verify group 1: Cart 10, Product 25 -> primary ID 1, quantity 6
        CartItemRecord g1 = dedupedResult.stream().filter(r -> r.cartId == 10L && r.productId == 25L).findFirst().orElseThrow();
        assertEquals(1L, g1.id);
        assertEquals(6, g1.quantity);

        // Verify group 2: Cart 10, Product 30 -> primary ID 4, capped quantity 10
        CartItemRecord g2 = dedupedResult.stream().filter(r -> r.cartId == 10L && r.productId == 30L).findFirst().orElseThrow();
        assertEquals(4L, g2.id);
        assertEquals(10, g2.quantity);

        // Verify unique item: Cart 10, Product 40 -> primary ID 6, quantity 4
        CartItemRecord unique = dedupedResult.stream().filter(r -> r.cartId == 10L && r.productId == 40L).findFirst().orElseThrow();
        assertEquals(6L, unique.id);
        assertEquals(4, unique.quantity);

        // Verify Cart 20, Product 25 -> primary ID 7, quantity 3
        CartItemRecord g3 = dedupedResult.stream().filter(r -> r.cartId == 20L && r.productId == 25L).findFirst().orElseThrow();
        assertEquals(7L, g3.id);
        assertEquals(3, g3.quantity);
    }
}
