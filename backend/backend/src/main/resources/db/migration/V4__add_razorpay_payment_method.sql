-- V4: Add RAZORPAY payment method used by the checkout flow's online payment option.

ALTER TABLE orders MODIFY COLUMN payment_method ENUM('UPI', 'CARD', 'NETBANKING', 'COD', 'WALLET', 'RAZORPAY') NOT NULL;
ALTER TABLE payments MODIFY COLUMN method ENUM('UPI', 'CARD', 'NETBANKING', 'COD', 'WALLET', 'RAZORPAY') NOT NULL;