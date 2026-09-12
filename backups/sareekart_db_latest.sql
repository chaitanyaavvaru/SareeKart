-- MySQL dump 10.13  Distrib 9.6.0, for macos15.7 (arm64)
--
-- Host: localhost    Database: sareekart_db
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '827aee8c-3410-11f1-abaa-ab99fe2ff025:1-138';

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`),
  CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (20,1,6,11),(22,2,1,10);
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK64t7ox312pqal3p7fg9o503c2` (`user_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
INSERT INTO `carts` VALUES (1,'2026-07-01 10:04:26.521455','2026-07-01 10:04:26.521455',2),(3,'2026-07-02 14:05:12.889269','2026-07-02 14:05:12.889269',1),(6,'2026-08-15 10:56:45.220189','2026-08-15 10:56:45.220189',3);
/*!40000 ALTER TABLE `carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'2026-07-01 09:31:11.500635','Premium silk sarees from renowned weavers',NULL,'Silk Sarees','2026-07-01 09:31:11.500635'),(2,'2026-07-01 09:31:11.504412','Comfortable and elegant cotton sarees',NULL,'Cotton Sarees','2026-07-01 09:31:11.504412'),(3,'2026-07-01 09:31:11.505135','Lightweight and flowing chiffon sarees',NULL,'Chiffon Sarees','2026-07-01 09:31:11.505135'),(4,'2026-07-01 09:31:11.505783','Graceful georgette sarees for every occasion',NULL,'Georgette Sarees','2026-07-01 09:31:11.505783'),(5,'2026-07-01 09:31:11.506415','Exquisite Banarasi sarees with intricate zari work',NULL,'Banarasi Sarees','2026-07-01 09:31:11.506415'),(6,'2026-07-01 09:31:11.506969','Traditional Kanchipuram silk sarees',NULL,'Kanchipuram Sarees','2026-07-01 09:31:11.506969'),(7,'2026-07-01 09:31:11.507513','Trendy designer sarees for modern women',NULL,'Designer Sarees','2026-07-01 09:31:11.507513'),(8,'2026-07-01 09:31:11.508099','Magnificent bridal sarees for your special day',NULL,'Bridal Sarees','2026-07-01 09:31:11.508099');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversations`
--

DROP TABLE IF EXISTS `conversations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversations` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversations`
--

LOCK TABLES `conversations` WRITE;
/*!40000 ALTER TABLE `conversations` DISABLE KEYS */;
/*!40000 ALTER TABLE `conversations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(255) NOT NULL,
  `discount_percent` double NOT NULL,
  `expiry_date` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeplt0kkm9yf2of2lnx6c1oy9b` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupons`
--

LOCK TABLES `coupons` WRITE;
/*!40000 ALTER TABLE `coupons` DISABLE KEYS */;
INSERT INTO `coupons` VALUES (1,_binary '','WELCOME10',10,'2027-01-01 09:31:11.560041'),(2,_binary '','WEDDING20',20,'2027-01-01 09:31:11.560060');
/*!40000 ALTER TABLE `coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,6499.00,1,1,2),(2,12999.00,1,2,7),(3,17650.00,9,3,11),(4,17650.00,9,3,11),(5,15999.00,1,4,10),(6,15999.00,10,5,10),(7,15999.00,10,5,10),(8,15999.00,1,6,10),(9,15999.00,1,6,10),(10,17650.00,1,7,11);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
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
  PRIMARY KEY (`id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'2026-07-01 10:05:58.221776','RAZORPAY','COMPLETED',NULL,NULL,'venkatagiri','Chaitanya ','70758725245','524132','andhrapradesh','16/642/4 arava bramhana veedi venkatagiri tirupati ','DELIVERED',6499.00,'2026-07-01 13:59:55.548337',2),(2,'2026-07-01 10:20:19.014832','COD','COMPLETED',NULL,NULL,'venkatagiri','Chaitanya','8985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','DELIVERED',11699.10,'2026-07-01 14:00:05.221674',2),(3,'2026-07-02 13:40:32.204603','COD','COMPLETED',NULL,NULL,'venkatagiri','Chaitanya CA','08985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','DELIVERED',317700.00,'2026-07-02 14:15:38.613356',2),(4,'2026-07-02 14:05:20.984282','COD','PENDING',NULL,NULL,'venkatagiri','SareeKart Admin','08985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','CANCELLED',15999.00,'2026-07-02 14:05:52.907088',1),(5,'2026-07-02 14:11:16.102933','COD','COMPLETED',NULL,NULL,'venkatagiri','Chaitanya Customer','08985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','DELIVERED',319980.00,'2026-07-02 14:15:50.097146',2),(6,'2026-08-15 12:28:35.510675','RAZORPAY','PENDING',NULL,NULL,'proddatur','geethesh','9989098664','517501','andhra pradesh','4/203 gandhi road proddatur','PENDING',28798.20,'2026-08-15 12:28:35.510675',1),(7,'2026-08-15 14:36:12.187152','RAZORPAY','PENDING',NULL,NULL,'cuddapah','geethesh','9989098664','516360','andhrapradesh','proddutur','PENDING',15885.00,'2026-08-15 14:36:12.187152',3);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `product_id` bigint NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`),
  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_images`
--

LOCK TABLES `product_images` WRITE;
/*!40000 ALTER TABLE `product_images` DISABLE KEYS */;
INSERT INTO `product_images` VALUES (1,'https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=800'),(2,'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(4,'https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(5,'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800'),(6,'https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800'),(8,'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800'),(3,'https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466'),(9,'https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059'),(7,'https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(10,'https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800'),(11,'https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282');
/*!40000 ALTER TABLE `product_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,_binary '','Red','2026-07-01 09:31:11.542572','Handwoven Banarasi silk saree with intricate gold zari work. Perfect for weddings and grand celebrations.','Silk','Royal Banarasi Silk Saree','Wedding',4999.00,25,'2026-07-01 09:31:11.542572',5),(2,_binary '','Maroon','2026-07-01 09:31:11.544713','Authentic Kanchipuram silk saree with traditional temple border design and rich pallu.','Silk','Kanchipuram Temple Border Saree','Wedding',6499.00,14,'2026-07-01 10:05:58.229637',6),(3,_binary '','White','2026-07-01 09:31:11.545442','Soft handloom cotton saree with beautiful jamdani weave. Ideal for daily wear and office.','Cotton','Elegant Cotton Handloom Saree','Casual',1299.00,50,'2026-07-01 10:29:06.585548',2),(4,_binary '','Pink','2026-07-01 09:31:11.546122','Stunning chiffon saree with sequin work and designer blouse piece. Perfect for parties.','Chiffon','Designer Chiffon Party Saree','Party',2999.00,30,'2026-07-01 09:31:11.546122',3),(5,_binary '','Green','2026-07-01 09:31:11.546882','Luxurious georgette saree with heavy embroidery work and stone detailing.','Georgette','Pure Georgette Embroidered Saree','Festival',3499.00,20,'2026-07-01 09:31:11.546882',4),(6,_binary '','Beige','2026-07-01 09:31:11.547740','Natural tussar silk saree with hand block prints. Eco-friendly and stylish.','Tussar Silk','Tussar Silk Printed Saree','Casual',2499.00,35,'2026-07-01 09:31:11.547740',1),(7,_binary '','baby pink','2026-07-01 09:31:11.548373','Embrace the grace of tradition with our Banarasi Kora Organza Brocade Bay Pink Saree, a masterpiece of heritage-rich craftsmanship The body is adorned with exquisite brocade that reflects the timeless sophistication of gold zari work, weaving a tapestry of elegance and charm\n\nThe saree features a self gold zari brocade border in a subtle bay pink hue, offering an understated yet luxurious finish The harmonious pallu showcases a self brocade pattern, enhancing the saree\'s allure with its intricate detailing\n\nCompleting this elegant ensemble is a self plain blouse, designed to complement the saree\'s opulence and create a cohesive look Perfect for weddings or special occasions, this saree is a testament to refined elegance and tradition','Silk','Banarasi Kora Organza Brocade Baby Pink Saree','Wedding',12999.00,50,'2026-07-01 10:33:48.032691',8),(8,_binary '','Navy Blue','2026-07-01 09:31:11.549106','Modern indo-western designer saree with contemporary drape style and pre-stitched pallu.','Georgette','Indo-Western Designer Saree','Party',5499.00,18,'2026-07-01 09:31:11.549106',7),(9,_binary '','Yellow','2026-07-01 09:31:11.549891','Embrace the essence of tradition with the Gadwal Silk Butta Yellow Saree, a masterpiece of heritage-rich elegance. The vibrant yellow body is adorned with exquisite silk buttas, each telling a story of age-old craftsmanship and sophistication\n\nThe saree\'s border is a harmonious blend of intricate zari work, enhancing its grandeur while the pallu features elaborate zari motifs that cascade beautifully, adding a touch of opulence and charm These elements work together to create a visual symphony of grace and luxury\n\nPaired with a coordinated blouse, this saree offers a seamless fusion of tradition and modernity, making it a perfect choice for weddings and grand celebrations. Experience the allure of silk and zari in every drape, elevating your ensemble to one of timeless beauty','Cotton','Gadwal Silk Butta Yellow Saree','Festive',899.00,60,'2026-07-01 10:32:07.792705',2),(10,_binary '','pink','2026-07-01 09:31:11.550603','Rare Patola silk saree with double ikat weave from Gujarat. A collector\'s piece.','Patola Silk','Patola Silk Double Ikat Saree','Wedding',15999.00,48,'2026-08-15 12:28:35.595215',1),(11,_binary '','Green','2026-07-01 10:24:15.441450','Embrace the beauty of tradition with the Taranga Kanchi Silk Brocade Green Saree, a true embodiment of heritage-rich elegance The lush green body is adorned with intricate brocade motifs that whisper tales of age-old craftsmanship, while the silk fabric drapes you in luxurious grace\n\nA striking contrast is offered by the gold zari brocade border in a rich purple hue, adding a touch of regal splendor The saree\'s pallu, crafted in a harmonious brocade, enhances the overall allure with its fine detailing\n\nCompleting the ensemble is a plain, coordinated blouse that perfectly balances the intricate designs of the saree Woven from the finest silk, this saree is a testament to elegance and timeless style, perfect for those cherished occasions','Silk','Taranga Kanchi Silk Brocade Green Saree','Festive',17650.00,9,'2026-08-15 14:36:12.300624',6);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment` text,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `user_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTOMER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-07-01 09:31:11.481336','admin@sareekart.com','SareeKart','Admin','9876543210','$2a$10$tladQzHZFh.yWadD49Hwdu6R2/JlxLBjmBg9yq3o1K3xO9sAwUGju','ADMIN','2026-07-01 09:31:11.481336'),(2,'2026-07-01 09:31:11.494397','customer@sareekart.com','Chaitanya','Customer','9876543211','$2a$10$xodZ2.nNFUDzDL1eRYJ.HeT3NFNXhMLXmho22S.Z.lJC6P1dVY.YG','CUSTOMER','2026-07-01 09:31:11.494397'),(3,'2026-08-15 10:56:22.222268','chaitanyaca183@gmail.com','chaitanya','avvaru','8985975765','$2a$10$z7ff.RY7unZ70Wgn7LgaeOXbuGAkaNkwdr8sUsNYkzv0DV2SnikTG','CUSTOMER','2026-08-15 10:56:22.222268');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `whatsapp_contacts`
--

DROP TABLE IF EXISTS `whatsapp_contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `whatsapp_contacts` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whatsapp_contacts`
--

LOCK TABLES `whatsapp_contacts` WRITE;
/*!40000 ALTER TABLE `whatsapp_contacts` DISABLE KEYS */;
/*!40000 ALTER TABLE `whatsapp_contacts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `whatsapp_messages`
--

DROP TABLE IF EXISTS `whatsapp_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `whatsapp_messages` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whatsapp_messages`
--

LOCK TABLES `whatsapp_messages` WRITE;
/*!40000 ALTER TABLE `whatsapp_messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `whatsapp_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishlists`
--

DROP TABLE IF EXISTS `wishlists`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi5s9pmqjw5sihfb0kkcakbk2y` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlists`
--

LOCK TABLES `wishlists` WRITE;
/*!40000 ALTER TABLE `wishlists` DISABLE KEYS */;
/*!40000 ALTER TABLE `wishlists` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-03 20:53:51
