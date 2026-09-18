-- ============================================================================
-- V31: Razorpay Order ID Index Optimization
-- Accelerated query execution for webhook callbacks and payment verification
-- ============================================================================

-- Fast O(1) order lookup by Razorpay order ID during async webhook fulfillment
CREATE INDEX idx_orders_razorpay_order_id ON orders (razorpay_order_id);
