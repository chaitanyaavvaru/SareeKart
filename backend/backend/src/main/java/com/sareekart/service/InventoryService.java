package com.sareekart.service;

import com.sareekart.entity.Order;
import com.sareekart.entity.OrderItem;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Inventory movements tied to the payment lifecycle.
 *
 * Commit points (exactly once per order, enforced by callers' idempotency
 * guards and DB transactions):
 *  - COD: decremented at order creation.
 *  - Online (Razorpay): decremented when verification/webhook flips the
 *    payment to PAID — never on order creation.
 * Cancellation restocks iff stock was actually committed for that order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductVariantRepository productVariantRepository;

    /** Decrement stock for every line item; fails atomically on any shortfall. */
    public void commitForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;
            if (variantId == null) {
                // Products without variants are not inventory-tracked yet.
                continue;
            }
            int updated = productVariantRepository.decrementStock(variantId, item.getQuantity());
            if (updated == 0) {
                throw new BadRequestException(
                    "Insufficient stock for '" + item.getProductName() + "'. Order was not completed.");
            }
        }
        log.info("Inventory committed for order {}", order.getOrderNumber());
    }

    /** Restock every variant line of a cancelled/refunded order. */
    public void releaseForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;
            if (variantId == null) {
                continue;
            }
            productVariantRepository.incrementStock(variantId, item.getQuantity());
        }
        log.info("Inventory released for order {}", order.getOrderNumber());
    }

    /**
     * Whether stock has been committed for this order already. COD orders are
     * committed at creation; online orders only once paid/verified.
     */
    public boolean isCommitted(Order order) {
        if (order.getPaymentMethod() == com.sareekart.entity.enums.PaymentMethod.COD) {
            return true;
        }
        return order.getPaymentStatus() == com.sareekart.entity.enums.PaymentStatus.PAID
            || order.getPaymentStatus() == com.sareekart.entity.enums.PaymentStatus.PARTIALLY_REFUNDED;
    }
}