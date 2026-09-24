-- ==============================================================================
-- SareeKart Migration V1: Baseline Foundational Schema
-- Consolidates the 20 foundational core e-commerce tables prior to V17
-- Deterministically extracted from verified canonical database schema
-- ==============================================================================

-- 1. users
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(32) NOT NULL DEFAULT 'CUSTOMER',
  `updated_at` datetime(6) DEFAULT NULL,
  `recovery_key` varchar(255) DEFAULT NULL,
  `whatsapp_opt_in` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. categories (Pre-V24: before slug, hierarchy, display_order, active)
CREATE TABLE IF NOT EXISTS `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. products (Pre-V24 & Pre-V26: before fabric_id/occasion_id/color_id & stock non-negative check)
CREATE TABLE IF NOT EXISTS `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `color` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `fabric` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `occasion` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock_quantity` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. product_images
CREATE TABLE IF NOT EXISTS `product_images` (
  `product_id` bigint NOT NULL,
  `image_url` varchar(1000) DEFAULT NULL,
  `image_order` int NOT NULL,
  KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`),
  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. orders (Pre-V26 & Pre-V31: before idempotency_key & razorpay_order_id index)
CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `payment_status` varchar(255) DEFAULT NULL,
  `razorpay_order_id` varchar(255) DEFAULT NULL,
  `razorpay_payment_id` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `pincode` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `street_address` varchar(255) DEFAULT NULL,
  `status` enum('CANCELLED','CONFIRMED','DELIVERED','PENDING','SHIPPED') NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `courier_partner` varchar(255) DEFAULT NULL,
  `current_location` varchar(255) DEFAULT NULL,
  `estimated_delivery_date` varchar(255) DEFAULT NULL,
  `tracking_number` varchar(255) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `wallet_credit_used` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. order_items (Pre-V26: before product_name snapshot, product_image, and positive quantity check)
CREATE TABLE IF NOT EXISTS `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. carts
CREATE TABLE IF NOT EXISTS `carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK64t7ox312pqal3p7fg9o503c2` (`user_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. cart_items (Pre-V25: before uq_cart_items_cart_product unique constraint)
CREATE TABLE IF NOT EXISTS `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. wishlists (Pre-V25: before created_at timestamp and CASCADE foreign keys)
CREATE TABLE IF NOT EXISTS `wishlists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi5s9pmqjw5sihfb0kkcakbk2y` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10. coupons
CREATE TABLE IF NOT EXISTS `coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(255) NOT NULL,
  `discount_percent` double NOT NULL,
  `expiry_date` datetime(6) DEFAULT NULL,
  `discount_amount` double DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `min_purchase_amount` double DEFAULT NULL,
  `times_used` int DEFAULT NULL,
  `usage_limit` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeplt0kkm9yf2of2lnx6c1oy9b` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11. reviews (Pre-V30: before idx_reviews_product index)
CREATE TABLE IF NOT EXISTS `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` text,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `verified_buyer` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcgy7qjc1r99dp117y9en6lxye` (`product_id`),
  CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 12. inventory_items (Pre-V30: before idx_inventory_product index)
CREATE TABLE IF NOT EXISTS `inventory_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available` int NOT NULL,
  `bin_location` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `on_hand` int NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_name` varchar(255) NOT NULL,
  `reserved` int NOT NULL,
  `sku` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `warehouse_code` varchar(255) NOT NULL,
  `warehouse_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp0mih1lkha7t38jh46r3uu0eg` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 13. notifications
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `message` text NOT NULL,
  `target_role` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0aymxivlo8q5vvwy` (`user_id`),
  CONSTRAINT `FK9y21adhxn0aymxivlo8q5vvwy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 14. whatsapp_contacts (Pre-V32: before opted_in & opt_in_updated_at)
CREATE TABLE IF NOT EXISTS `whatsapp_contacts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmkn3wlni8surpx18remkyfr39` (`phone_number`),
  KEY `FKq9yy1jb9pitajvjmvce9ywbs1` (`user_id`),
  CONSTRAINT `FKq9yy1jb9pitajvjmvce9ywbs1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 15. conversations (Pre-V32: initial status enum values)
CREATE TABLE IF NOT EXISTS `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `last_message_at` datetime(6) DEFAULT NULL,
  `status` enum('BOT_HANDLING','CLOSED','OPEN') NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `assigned_admin_id` bigint DEFAULT NULL,
  `contact_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi1ogc8nahlfh6erho9wdbfx3r` (`assigned_admin_id`),
  KEY `FKp9vmo1aofi3k3m4afrx783rok` (`contact_id`),
  CONSTRAINT `FKi1ogc8nahlfh6erho9wdbfx3r` FOREIGN KEY (`assigned_admin_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKp9vmo1aofi3k3m4afrx783rok` FOREIGN KEY (`contact_id`) REFERENCES `whatsapp_contacts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 16. whatsapp_messages
CREATE TABLE IF NOT EXISTS `whatsapp_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `delivery_status` enum('DELIVERED','FAILED','READ','SENT') DEFAULT NULL,
  `media_url` varchar(1000) DEFAULT NULL,
  `message_type` enum('DOCUMENT','IMAGE','INTERACTIVE','LOCATION','TEXT') NOT NULL,
  `sender_type` enum('ADMIN','BOT','CUSTOMER') NOT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `wam_id` varchar(255) DEFAULT NULL,
  `conversation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtk1lnfa80cuxdpopq2vg7kqt7` (`wam_id`),
  KEY `FKq11b89frtcp95i28s4nsl3hcq` (`conversation_id`),
  CONSTRAINT `FKq11b89frtcp95i28s4nsl3hcq` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 17. stock_transfers
CREATE TABLE IF NOT EXISTS `stock_transfers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved_by_email` varchar(255) DEFAULT NULL,
  `approved_by_user_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `quantity` int NOT NULL,
  `reason` text,
  `requested_by_email` varchar(255) NOT NULL,
  `requested_by_user_id` bigint NOT NULL,
  `review_note` text,
  `sku` varchar(255) NOT NULL,
  `source_warehouse` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `target_warehouse` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 18. approval_requests
CREATE TABLE IF NOT EXISTS `approval_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `entity_type` varchar(255) NOT NULL,
  `previous_value` text,
  `reason` text,
  `requested_by_email` varchar(255) NOT NULL,
  `requested_by_user_id` bigint NOT NULL,
  `requested_value` text NOT NULL,
  `review_note` text,
  `reviewed_by_email` varchar(255) DEFAULT NULL,
  `reviewed_by_user_id` bigint DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `target_entity_id` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 19. audit_logs
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `details` text,
  `entity_id` varchar(255) DEFAULT NULL,
  `entity_type` varchar(255) NOT NULL,
  `ip_address` varchar(255) DEFAULT NULL,
  `performed_by_email` varchar(255) DEFAULT NULL,
  `performed_by_role` varchar(255) DEFAULT NULL,
  `performed_by_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 20. password_reset_tokens
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `used` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71lqwbwtklmljk3qlsugr1mig` (`token`),
  KEY `FKk3ndxg5xp6v7wd4gjyusp15gq` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ==============================================================================
-- 21. BASELINE REFERENCE CATALOG DATA
-- Foundational reference categories and catalog handlooms required by V23/V24
-- Contains strictly public catalog items: zero customer data, zero credentials
-- ==============================================================================

INSERT INTO `categories` (`id`, `name`, `description`, `image_url`, `created_at`, `updated_at`) VALUES
(1, 'Silk Sarees', 'Premium silk sarees from renowned weavers', NULL, '2026-07-01 09:31:11.500635', '2026-07-01 09:31:11.500635'),
(2, 'Cotton Sarees', 'Comfortable and elegant cotton sarees', NULL, '2026-07-01 09:31:11.504412', '2026-07-01 09:31:11.504412'),
(3, 'Chiffon Sarees', 'Lightweight and flowing chiffon sarees', NULL, '2026-07-01 09:31:11.505135', '2026-07-01 09:31:11.505135'),
(4, 'Georgette Sarees', 'Graceful georgette sarees for every occasion', NULL, '2026-07-01 09:31:11.505783', '2026-07-01 09:31:11.505783'),
(5, 'Banarasi Sarees', 'Exquisite Banarasi sarees with intricate zari work', NULL, '2026-07-01 09:31:11.506415', '2026-07-01 09:31:11.506415'),
(6, 'Kanchipuram Sarees', 'Traditional Kanchipuram silk sarees', NULL, '2026-07-01 09:31:11.506969', '2026-07-01 09:31:11.506969'),
(7, 'Designer Sarees', 'Trendy designer sarees for modern women', NULL, '2026-07-01 09:31:11.507513', '2026-07-01 09:31:11.507513'),
(8, 'Bridal Sarees', 'Magnificent bridal sarees for your special day', NULL, '2026-07-01 09:31:11.508099', '2026-09-12 11:27:48.280825');

INSERT INTO `products` (`id`, `active`, `color`, `created_at`, `description`, `fabric`, `name`, `occasion`, `price`, `stock_quantity`, `updated_at`, `category_id`) VALUES
(1, _binary '', 'Red', '2026-07-01 09:31:11.542572', 'Handwoven Banarasi silk saree with intricate gold zari work. Perfect for weddings and grand celebrations.', 'Silk', 'Royal Banarasi Silk Saree', 'Wedding', 4999.00, 266, '2026-09-12 10:35:14.314689', 5),
(2, _binary '', 'Maroon', '2026-07-01 09:31:11.544713', 'Authentic Kanchipuram silk saree with traditional temple border design and rich pallu.', 'Silk', 'Kanchipuram Temple Border Saree', 'Wedding', 6499.00, 0, '2026-07-01 10:05:58.229637', 6),
(3, _binary '', 'White', '2026-07-01 09:31:11.545442', 'Soft handloom cotton saree with beautiful jamdani weave. Ideal for daily wear and office.', 'Cotton', 'Elegant Cotton Handloom Saree', 'Casual', 1299.00, 50, '2026-07-01 10:29:06.585548', 2),
(4, _binary '', 'Pink', '2026-07-01 09:31:11.546122', 'Stunning chiffon saree with sequin work and designer blouse piece. Perfect for parties.', 'Chiffon', 'Designer Chiffon Party Saree', 'Party', 2999.00, 30, '2026-07-01 09:31:11.546122', 3),
(5, _binary '', 'Green', '2026-07-01 09:31:11.546882', 'Luxurious georgette saree with heavy embroidery work and stone detailing.', 'Georgette', 'Pure Georgette Embroidered Saree', 'Festival', 3499.00, 20, '2026-07-01 09:31:11.546882', 4),
(6, _binary '', 'Beige', '2026-07-01 09:31:11.547740', 'Natural tussar silk saree with hand block prints. Eco-friendly and stylish.', 'Tussar Silk', 'Tussar Silk Printed Saree', 'Casual', 2499.00, 35, '2026-07-01 09:31:11.547740', 1),
(7, _binary '', 'baby pink', '2026-07-01 09:31:11.548373', 'Embrace the grace of tradition with our Banarasi Kora Organza Brocade Bay Pink Saree, a masterpiece of heritage-rich craftsmanship The body is adorned with exquisite brocade that reflects the timeless sophistication of gold zari work, weaving a tapestry of elegance and charm\n\nThe saree features a self gold zari brocade border in a subtle bay pink hue, offering an understated yet luxurious finish The harmonious pallu showcases a self brocade pattern, enhancing the saree\'s allure with its intricate detailing\n\nCompleting this elegant ensemble is a self plain blouse, designed to complement the saree\'s opulence and create a cohesive look Perfect for weddings or special occasions, this saree is a testament to refined elegance and tradition', 'Silk', 'Banarasi Kora Organza Brocade Baby Pink Saree', 'Wedding', 12999.00, 48, '2026-09-06 10:38:19.626376', 8),
(8, _binary '', 'Navy Blue', '2026-07-01 09:31:11.549106', 'Modern indo-western designer saree with contemporary drape style and pre-stitched pallu.', 'Georgette', 'Indo-Western Designer Saree', 'Party', 5499.00, 18, '2026-07-01 09:31:11.549106', 7),
(9, _binary '', 'Yellow', '2026-07-01 09:31:11.549891', 'Embrace the essence of tradition with the Gadwal Silk Butta Yellow Saree, a masterpiece of heritage-rich elegance. The vibrant yellow body is adorned with exquisite silk buttas, each telling a story of age-old craftsmanship and sophistication\n\nThe saree\'s border is a harmonious blend of intricate zari work, enhancing its grandeur while the pallu features elaborate zari motifs that cascade beautifully, adding a touch of opulence and charm These elements work together to create a visual symphony of grace and luxury\n\nPaired with a coordinated blouse, this saree offers a seamless fusion of tradition and modernity, making it a perfect choice for weddings and grand celebrations. Experience the allure of silk and zari in every drape, elevating your ensemble to one of timeless beauty', 'Cotton', 'Gadwal Silk Butta Yellow Saree', 'Festive', 899.00, 60, '2026-07-01 10:32:07.792705', 2),
(10, _binary '', 'pink', '2026-07-01 09:31:11.550603', 'Rare Patola silk saree with double ikat weave from Gujarat. A collector\'s piece.', 'Patola Silk', 'Patola Silk Double Ikat Saree', 'Wedding', 15999.00, 50, '2026-09-04 12:59:30.504999', 1),
(11, _binary '', 'Green', '2026-07-01 10:24:15.441450', 'Embrace the beauty of tradition with the Taranga Kanchi Silk Brocade Green Saree, a true embodiment of heritage-rich elegance The lush green body is adorned with intricate brocade motifs that whisper tales of age-old craftsmanship, while the silk fabric drapes you in luxurious grace\n\nA striking contrast is offered by the gold zari brocade border in a rich purple hue, adding a touch of regal splendor The saree\'s pallu, crafted in a harmonious brocade, enhances the overall allure with its fine detailing\n\nCompleting the ensemble is a plain, coordinated blouse that perfectly balances the intricate designs of the saree Woven from the finest silk, this saree is a testament to elegance and timeless style, perfect for those cherished occasions', 'Silk', 'Taranga Kanchi Silk Brocade Green Saree', 'Festive', 17650.00, 11, '2026-09-09 15:26:10.353305', 6),
(12, _binary '', 'Royal Gold', '2026-09-11 09:15:32.583202', 'Handcrafted tissue silk drape', 'Silk', 'Exclusive Offline Silk Saree', 'Wedding', 18500.00, 15, '2026-09-11 09:15:32.583202', 1),
(13, _binary '', 'Ruby Pink', '2026-09-11 09:15:39.081295', 'Added by Store Manager', 'Silk', 'Manager Catalog Drape', 'Festive', 12000.00, 8, '2026-09-11 09:15:39.081295', 1),
(14, _binary '', 'Crimson Red', '2026-09-11 14:51:18.994638', 'Pure zari handwoven silk saree with intricate temple borders', 'Silk', 'Royal Crimson Kanchipuram Silk Saree', 'Wedding', 28500.00, 5, '2026-09-11 14:51:18.994638', 1),
(15, _binary '', 'Emerald Green', '2026-09-11 14:51:24.553574', 'Exquisite Banarasi silk saree with gold floral vines', 'Silk', 'Emerald Green Banarasi Silk Saree', 'Wedding', 32000.00, 3, '2026-09-11 14:51:24.553574', 1),
(16, _binary '', 'Peacock Blue', '2026-09-11 14:51:29.248686', 'Rich Paithani weave with mor bangadi pallu motif', 'Silk', 'Peacock Blue Paithani Silk Saree', 'Wedding', 45000.00, 2, '2026-09-11 14:51:29.248686', 1),
(17, _binary '', 'Honey Gold', '2026-09-11 15:05:55.428733', 'Featherlight pure Chanderi saree with zari butta motifs', 'Silk', 'Chanderi Handloom Gold Saree', 'Festive', 16500.00, 7, '2026-09-11 15:05:55.428733', 1);
