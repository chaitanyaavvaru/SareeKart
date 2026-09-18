-- MySQL dump 10.13  Distrib 9.6.0, for macos15.7 (arm64)
--
-- Host: 127.0.0.1    Database: sareekart_db
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

--
-- Table structure for table `ai_style_consultations`
--

DROP TABLE IF EXISTS `ai_style_consultations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_style_consultations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blouse_style` varchar(100) DEFAULT NULL,
  `chosen_look_title` varchar(255) DEFAULT NULL,
  `contrast_color` varchar(100) DEFAULT NULL,
  `converted_to_tailoring` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `fabric` varchar(100) DEFAULT NULL,
  `jewelry_recommendation` varchar(255) DEFAULT NULL,
  `occasion` varchar(100) DEFAULT NULL,
  `primary_color` varchar(100) DEFAULT NULL,
  `saree_name` varchar(255) NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_style_user` (`user_id`),
  KEY `idx_ai_style_product` (`product_id`),
  KEY `idx_ai_style_occasion` (`occasion`),
  KEY `idx_ai_style_converted` (`converted_to_tailoring`),
  KEY `idx_ai_style_created` (`created_at`),
  CONSTRAINT `FK5ok0uvgppcbc7d24sk3xolyr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKaxbl0svjt6lu2kr28v30bhqp2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_style_consultations`
--

LOCK TABLES `ai_style_consultations` WRITE;
/*!40000 ALTER TABLE `ai_style_consultations` DISABLE KEYS */;
INSERT INTO `ai_style_consultations` VALUES (1,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '','2026-09-12 10:04:37.807418','Pure Mulberry Silk','Temple Nakshi Antique Gold','Bridal / Wedding Festivities','Crimson Red','Kanchipuram Crimson Royal Bridal Silk Saree',NULL,NULL),(2,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-12 10:15:20.533541','Silk','Temple Nakshi Antique Gold','BRIDAL_WEDDING','Crimson Red','Chanderi Handloom Gold Saree',17,NULL),(3,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-12 10:15:25.559257','Chanderi Silk','Temple Nakshi Antique Gold','BRIDAL_WEDDING','Crimson Red','Chanderi Handloom Gold Saree',17,NULL),(4,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-12 10:16:42.586933','Chanderi Silk','Temple Nakshi Antique Gold','BRIDAL_WEDDING','Crimson Red','Chanderi Handloom Gold Saree',17,NULL),(5,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-12 10:16:51.561222','Chanderi Silk','Temple Nakshi Antique Gold','BRIDAL_WEDDING','Crimson Red','Chanderi Handloom Gold Saree',17,NULL),(6,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-16 09:29:05.154589','Silk','Temple Nakshi Antique Gold','Wedding','Red','Royal Banarasi Silk Saree',1,NULL),(7,'designer','Royal Heritage Grandeur','Temple Mustard Gold',_binary '\0','2026-09-16 09:29:05.154606','Patola Silk','Temple Nakshi Antique Gold','Wedding','pink','Patola Silk Double Ikat Saree',10,NULL),(8,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '','2026-09-16 09:29:05.154588','Kanchipuram Silk','Temple Nakshi Antique Gold','Temple','Red','Royal Banarasi Silk Saree',1,NULL),(9,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-16 09:29:06.028197','Silk','Temple Nakshi Antique Gold','Wedding','Red','Royal Banarasi Silk Saree',1,NULL),(10,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-17 14:47:39.407387','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(11,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-17 14:47:39.844506','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(12,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-17 14:54:13.541227','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(13,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:29:48.698654','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(14,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:35:45.563288','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(15,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.005694','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(16,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.005930','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(17,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.005698','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(18,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.010339','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(19,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.045906','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(20,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:38:27.445829','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(21,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.062939','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(22,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.062983','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(23,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.062939','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(24,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.062939','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(25,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.073123','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(26,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 09:39:17.560234','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(27,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:50.585715','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(28,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:50.590917','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(29,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:50.591117','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(30,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:50.590905','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(31,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:50.599846','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(32,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 10:17:51.105978','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(33,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:33.939235','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(34,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:33.939212','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(35,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:33.939165','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(36,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:33.939165','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(37,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:33.943086','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL),(38,'designer','Royal Heritage Grandeur','Peacock Emerald Green',_binary '\0','2026-09-18 11:04:34.416532','Silk','Temple Nakshi Antique Gold','Festive & Wedding Celebration','Red','Royal Banarasi Silk Saree',1,NULL);
/*!40000 ALTER TABLE `ai_style_consultations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_requests`
--

DROP TABLE IF EXISTS `approval_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_requests` (
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_requests`
--

LOCK TABLES `approval_requests` WRITE;
/*!40000 ALTER TABLE `approval_requests` DISABLE KEYS */;
INSERT INTO `approval_requests` VALUES (1,'RESTOCK','2026-09-04 12:51:25.685560','INVENTORY_STOCK','On Hand: 25','Festive season restock','manager@sareekart.com',5,'10','Approved by Owner','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-04 12:51:36.354954',1),(2,'RESTOCK','2026-09-04 12:54:53.957595','INVENTORY_STOCK','On Hand: 35','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:51.257593',1),(3,'RESTOCK','2026-09-04 12:55:00.579334','INVENTORY_STOCK','On Hand: 35','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:38.637966',1),(4,'RESTOCK','2026-09-04 12:58:56.387348','INVENTORY_STOCK','On Hand: 35','PO restock from Varanasi Weaver Cooperative','admin@sareekart.com',1,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:36.407921',1),(5,'RESTOCK','2026-09-04 14:57:07.228720','INVENTORY_STOCK','On Hand: 35','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-04 14:57:08.969068',1),(6,'RESTOCK','2026-09-04 14:57:53.220711','INVENTORY_STOCK','On Hand: 60','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-04 14:57:54.472960',1),(7,'RESTOCK','2026-09-04 15:22:14.852868','INVENTORY_STOCK','On Hand: 85','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-04 15:22:15.990256',1),(8,'RESTOCK','2026-09-05 11:47:44.605341','INVENTORY_STOCK','On Hand: 110','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:34.920145',1),(9,'RESTOCK','2026-09-06 12:14:23.746732','INVENTORY_STOCK','On Hand: 110','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:33.466722',1),(10,'RESTOCK','2026-09-06 12:14:33.852397','INVENTORY_STOCK','On Hand: 110','PO restock from Varanasi Weaver Cooperative','manager@sareekart.com',5,'25','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-09 13:58:32.518674',1),(11,'RESTOCK','2026-09-11 15:24:13.977268','INVENTORY_STOCK','On Hand: 270','Manager routine adjustment','manager@sareekart.com',5,'5','Approved via Owner Console','owner@sareekart.com',4,'APPROVED','SK-ROYAL-BANARASI-SILK-SA-1','2026-09-12 10:35:14.308346',1);
/*!40000 ALTER TABLE `approval_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
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
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,'SUBMIT_REQUEST','2026-09-04 12:51:25.695460','Submitted request #1 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(2,'APPROVE_REQUEST','2026-09-04 12:51:36.353679','Approved request #1 (RESTOCK). Note: Approved by Owner','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(3,'SUBMIT_REQUEST','2026-09-04 12:54:54.011593','Submitted request #2 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(4,'SUBMIT_REQUEST','2026-09-04 12:55:00.585501','Submitted request #3 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(5,'SUBMIT_REQUEST','2026-09-04 12:58:56.388505','Submitted request #4 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'admin@sareekart.com','ADMIN',1),(6,'SUBMIT_REQUEST','2026-09-04 14:57:07.249614','Submitted request #5 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(7,'APPROVE_REQUEST','2026-09-04 14:57:08.967351','Approved request #5 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(8,'SUBMIT_REQUEST','2026-09-04 14:57:53.222324','Submitted request #6 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(9,'APPROVE_REQUEST','2026-09-04 14:57:54.472010','Approved request #6 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(10,'SUBMIT_REQUEST','2026-09-04 15:22:14.871272','Submitted request #7 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(11,'APPROVE_REQUEST','2026-09-04 15:22:15.988910','Approved request #7 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(12,'SUBMIT_REQUEST','2026-09-05 11:47:44.633455','Submitted request #8 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(13,'SUBMIT_REQUEST','2026-09-06 12:14:23.792418','Submitted request #9 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(14,'SUBMIT_REQUEST','2026-09-06 12:14:33.861653','Submitted request #10 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(15,'APPROVE_REQUEST','2026-09-09 13:58:32.490283','Approved request #10 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(16,'APPROVE_REQUEST','2026-09-09 13:58:33.464079','Approved request #9 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(17,'APPROVE_REQUEST','2026-09-09 13:58:34.917271','Approved request #8 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(18,'APPROVE_REQUEST','2026-09-09 13:58:36.404137','Approved request #4 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(19,'APPROVE_REQUEST','2026-09-09 13:58:38.635255','Approved request #3 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(20,'APPROVE_REQUEST','2026-09-09 13:58:51.254121','Approved request #2 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4),(21,'SUBMIT_REQUEST','2026-09-11 15:24:13.979759','Submitted request #11 (RESTOCK) awaiting owner approval','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'manager@sareekart.com','MANAGER',5),(22,'APPROVE_REQUEST','2026-09-12 10:35:14.299514','Approved request #11 (RESTOCK). Note: Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','INVENTORY_STOCK',NULL,'owner@sareekart.com','OWNER',4);
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

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
  UNIQUE KEY `uq_cart_items_cart_product` (`cart_id`,`product_id`),
  KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`),
  CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=315 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (20,1,6,11),(49,2,1,11),(146,1,65,23);
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
) ENGINE=InnoDB AUTO_INCREMENT=164 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
INSERT INTO `carts` VALUES (1,'2026-07-01 10:04:26.521455','2026-07-01 10:04:26.521455',2),(3,'2026-07-02 14:05:12.889269','2026-07-02 14:05:12.889269',1),(6,'2026-08-15 10:56:45.220189','2026-08-15 10:56:45.220189',3),(7,'2026-09-12 15:57:28.818334','2026-09-12 15:57:28.818334',4),(8,'2026-09-16 10:45:02.585619','2026-09-16 10:45:02.585619',5),(65,'2026-09-17 10:21:10.412722','2026-09-17 10:21:10.412722',9);
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
  `parent_id` bigint DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `active` bit(1) NOT NULL DEFAULT b'1',
  `name` varchar(255) NOT NULL,
  `slug` varchar(150) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`),
  UNIQUE KEY `slug` (`slug`),
  KEY `fk_categories_parent` (`parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'2026-07-01 09:31:11.500635','Premium silk sarees from renowned weavers',NULL,NULL,0,_binary '','Silk Sarees','silk-sarees','2026-07-01 09:31:11.500635'),(2,'2026-07-01 09:31:11.504412','Comfortable and elegant cotton sarees',NULL,NULL,0,_binary '','Cotton Sarees','cotton-sarees','2026-07-01 09:31:11.504412'),(3,'2026-07-01 09:31:11.505135','Lightweight and flowing chiffon sarees',NULL,NULL,0,_binary '','Chiffon Sarees','chiffon-sarees','2026-07-01 09:31:11.505135'),(4,'2026-07-01 09:31:11.505783','Graceful georgette sarees for every occasion',NULL,NULL,0,_binary '','Georgette Sarees','georgette-sarees','2026-07-01 09:31:11.505783'),(5,'2026-07-01 09:31:11.506415','Exquisite Banarasi sarees with intricate zari work',NULL,NULL,0,_binary '','Banarasi Sarees','banarasi-sarees','2026-07-01 09:31:11.506415'),(6,'2026-07-01 09:31:11.506969','Traditional Kanchipuram silk sarees',NULL,NULL,0,_binary '','Kanchipuram Sarees','kanchipuram-sarees','2026-07-01 09:31:11.506969'),(7,'2026-07-01 09:31:11.507513','Trendy designer sarees for modern women',NULL,NULL,0,_binary '','Designer Sarees','designer-sarees','2026-07-01 09:31:11.507513'),(8,'2026-07-01 09:31:11.508099','Magnificent bridal sarees for your special day',NULL,NULL,0,_binary '','Bridal Sarees','bridal-sarees','2026-09-12 11:27:48.280825');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `colors`
--

DROP TABLE IF EXISTS `colors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `colors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `slug` varchar(100) NOT NULL,
  `family` varchar(50) NOT NULL,
  `hex_code` varchar(10) NOT NULL,
  `display_order` int DEFAULT '0',
  `active` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `colors`
--

LOCK TABLES `colors` WRITE;
/*!40000 ALTER TABLE `colors` DISABLE KEYS */;
INSERT INTO `colors` VALUES (1,'Red','red','Red','#FF0000',1,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(2,'Maroon','maroon','Red','#800000',2,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(3,'Crimson','crimson','Red','#DC143C',3,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(4,'Crimson Red','crimson-red','Red','#990000',4,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(5,'Ruby Red','ruby-red','Red','#C70039',5,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(6,'Pink','pink','Pink','#FFC0CB',6,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(7,'baby pink','baby-pink','Pink','#F4C2C2',7,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(8,'Ruby Pink','ruby-pink','Pink','#E0115F',8,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(9,'Rose','rose','Pink','#FF007F',9,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(10,'White','white','White','#FFFFFF',10,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(11,'Ivory','ivory','White','#FFFFF0',11,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(12,'Green','green','Green','#008000',12,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(13,'Emerald Green','emerald-green','Green','#50C878',13,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(14,'Navy Blue','navy-blue','Blue','#000080',14,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(15,'Peacock Blue','peacock-blue','Blue','#005F73',15,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(16,'Yellow','yellow','Yellow','#FFFF00',16,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(17,'Gold','gold','Gold','#FFD700',17,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(18,'Royal Gold','royal-gold','Gold','#D4AF37',18,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(19,'Honey Gold','honey-gold','Gold','#E5B80B',19,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(20,'Beige','beige','Neutral','#F5F5DC',20,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(21,'Champagne Silver','champagne-silver','Metallic','#E5E4E2',21,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(22,'Terracotta Earth','terracotta-earth','Orange','#E2725B',22,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913'),(23,'Black','black','Black','#000000',23,_binary '','2026-09-12 16:49:49.505913','2026-09-12 16:49:49.505913');
/*!40000 ALTER TABLE `colors` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversations`
--

LOCK TABLES `conversations` WRITE;
/*!40000 ALTER TABLE `conversations` DISABLE KEYS */;
INSERT INTO `conversations` VALUES (1,'2026-09-16 09:52:46.252668','2026-09-16 09:52:45.000000','BOT_HANDLING',NULL,'2026-09-16 09:54:38.688790',NULL,1);
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
  `discount_amount` double DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `min_purchase_amount` double DEFAULT NULL,
  `times_used` int DEFAULT NULL,
  `usage_limit` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeplt0kkm9yf2of2lnx6c1oy9b` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupons`
--

LOCK TABLES `coupons` WRITE;
/*!40000 ALTER TABLE `coupons` DISABLE KEYS */;
INSERT INTO `coupons` VALUES (1,_binary '','WELCOME10',10,'2027-01-01 09:31:11.560041',NULL,NULL,NULL,NULL,NULL),(2,_binary '','WEDDING20',20,'2027-01-01 09:31:11.560060',NULL,NULL,NULL,NULL,NULL),(3,_binary '','WELCOME15',15,'2026-12-31 18:29:59.000000',0,_binary '\0',15000,0,500),(4,_binary '','ROYAL10',10,'2026-12-31 18:29:59.000000',0,_binary '\0',5000,0,500);
/*!40000 ALTER TABLE `coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_events`
--

DROP TABLE IF EXISTS `customer_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_event_id` varchar(64) NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `event_type` varchar(50) NOT NULL,
  `entity_type` varchar(50) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `metadata` json DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_customer_events_client_event_id` (`client_event_id`),
  KEY `idx_customer_events_session` (`session_id`,`created_at` DESC),
  KEY `idx_customer_events_user_type` (`user_id`,`event_type`,`created_at` DESC),
  KEY `idx_customer_events_type_created` (`event_type`,`created_at` DESC),
  KEY `idx_customer_events_entity` (`entity_type`,`entity_id`),
  KEY `idx_customer_events_created` (`created_at` DESC),
  CONSTRAINT `fk_customer_events_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_events`
--

LOCK TABLES `customer_events` WRITE;
/*!40000 ALTER TABLE `customer_events` DISABLE KEYS */;
INSERT INTO `customer_events` VALUES (1,'evt_test_123','sess_test_123',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"dwellTimeMs\": 5000}','0:0:0:0:0:0:0:1','curl/8.7.1','2026-09-14 14:23:26.427580'),(2,'evt_1c619700deac49a3a72fdcac5cc97cac','sess_a35648ae4d344747996b7f58758631e9',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:24:11.614472'),(3,'evt_a237d296653d4beaad97909f579c7140','sess_7dff2155d25548a989ef007d907c5b18',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:24:11.649412'),(4,'evt_b78cd96e528e46688a9a8f1af0a14a1f','sess_7dff2155d25548a989ef007d907c5b18',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:24:11.654330'),(5,'evt_ee39fc2af8b24ab88f6c1735c7db0531','sess_7dff2155d25548a989ef007d907c5b18',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:24:14.340691'),(6,'evt_64df670b8a9840c2aca2a83259b6c8ca','sess_82df258262e5487296ea2927b0dd2b01',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:29:50.498620'),(7,'evt_758daf8729a24361b42c662b7d57cbd6','sess_82df258262e5487296ea2927b0dd2b01',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:29:50.500744'),(8,'evt_bddec7e97395480d8207864511c362f2','sess_82df258262e5487296ea2927b0dd2b01',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:29:51.264027'),(9,'evt_92ac0aa5b6284975bd15f13b734b5a85','sess_d38c144e9af04e98a4523a1690b3b4bd',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:40:50.184069'),(10,'evt_553733ce2ec6448280de8cc1c635722f','sess_e255e5a8609c40318ce25a0e2160e495',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:40:51.688200'),(11,'evt_5c93f439bfdc42cd84ff1b0488ba788b','sess_e255e5a8609c40318ce25a0e2160e495',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:40:51.694414'),(12,'evt_917b33265195464faa9ad11f4b4dfb5e','sess_3345b4dc2291453c9012c5eafb7c77ef',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:41:33.016612'),(13,'evt_4b44f95da0374a699f76c9626ac1ff3c','sess_3345b4dc2291453c9012c5eafb7c77ef',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:41:33.021216'),(14,'evt_3e77caa2d7a74612aaa2feb9c96ea801','sess_3345b4dc2291453c9012c5eafb7c77ef',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:41:33.024497'),(15,'evt_7603f2f8a2af4fe19bc3e7e95133e380','sess_46a6216ed9c54bda8bb1f4719a377f04',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:41:34.513891'),(16,'evt_a2c42b01e24e4696b279c124ba35d97c','sess_ce2f92821f80494dbdec6dc1b66c5fd9',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:43:14.951672'),(17,'evt_127e3d7892ef4075afe16cbec1ea1046','sess_ce2f92821f80494dbdec6dc1b66c5fd9',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:43:14.956259'),(18,'evt_e66e6705f93245a7916c742bc0fb2218','sess_ce2f92821f80494dbdec6dc1b66c5fd9',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:43:14.960515'),(19,'evt_ba537aac966c42ce872a93d69ee01b9f','sess_f5c8ebc467a543388112181d578a01d8',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:43:16.474479'),(20,'evt_2e9a433c72e748679da227c81a89720c','sess_bf8d843a61d84f8e9e201261723fbb58',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:45:15.038370'),(21,'evt_76e9e714518242c5980227f493ce9cff','sess_bf8d843a61d84f8e9e201261723fbb58',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:45:15.042092'),(22,'evt_d27b7413af744c37a838dabbe9e94d92','sess_bf8d843a61d84f8e9e201261723fbb58',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:45:15.045230'),(23,'evt_e9e839236dd34f438e3559bbea0b712d','sess_d896fd22d4e642b6ac121670d9da9b3d',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:45:16.560455'),(24,'evt_34c09d6d72c141c98f1e386eba3cc5d4','sess_ed57eb53a96d498b8b27015ce948bf8f',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:47:47.765404'),(25,'evt_987caf04d7154013b364c373fe8e90b0','sess_ed57eb53a96d498b8b27015ce948bf8f',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:47:47.768447'),(26,'evt_fe33817232f348d1b6aa8ab330a79a94','sess_ed57eb53a96d498b8b27015ce948bf8f',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:47:47.771497'),(27,'evt_d1ebdc0f5ed74162a6163028bee55889','sess_b7ff58f502414d1d9808f92379b7b71a',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:47:49.276926'),(28,'evt_420c94d8391e402ead661da09c89dfda','sess_0c6a34c447154af4812e68a0457a3fde',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:49:10.160269'),(29,'evt_e277d13fb4f94d639bda650bf47fffaf','sess_0c6a34c447154af4812e68a0457a3fde',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:49:10.163881'),(30,'evt_e1b0468faf204227aad0a95df630e8ab','sess_0c6a34c447154af4812e68a0457a3fde',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:49:10.166389'),(31,'evt_07df273457ed40ba8ac9669e2b331a13','sess_9126900eeffc49b9872238247623f988',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-14 14:49:11.668190'),(32,'evt_cfaae051fe884081bdb1272d466752cd','sess_c749e738521c4e6f93f0cd19fdf4a677',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:09.298672'),(33,'evt_d3598164932349e8903c9cd33e9d744b','sess_c749e738521c4e6f93f0cd19fdf4a677',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:09.315367'),(34,'evt_1376bbe524934f90909c850c8d82b159','sess_c749e738521c4e6f93f0cd19fdf4a677',NULL,'ADD_TO_CART','PRODUCT',1,'{\"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Royal Banarasi Silk Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:09.316958'),(35,'evt_49e136fb2d3444f39b906a08fd3615c4','sess_31e202497dad4dc8b7d959898b51442b',NULL,'SEARCH_QUERY','SEARCH',NULL,'{\"query\": \"Banarasi\", \"fabric\": \"All\", \"category\": \"All\", \"occasion\": \"All\", \"resultCount\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:31.898326'),(36,'evt_0eba0acdc9ac4828b8b6ab17360902db','sess_0152299e48de47a3be0c052b8867fb20',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:32.424425'),(37,'evt_f37554f2f67146609e53333d28efd18c','sess_0152299e48de47a3be0c052b8867fb20',NULL,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:32.442632'),(38,'evt_a0381a31a52b425789d9c79e3c407bbd','sess_0152299e48de47a3be0c052b8867fb20',NULL,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:32.452057'),(39,'evt_449cd2936b23449aa3e96537d6598504','sess_624232494e57453ebe4975d6e7627b23',2,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:52.659574'),(40,'evt_5216768608194266bc4f155661c07273','sess_624232494e57453ebe4975d6e7627b23',2,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:52.666912'),(41,'evt_f8895c01a2714aab8777dfd7de1411a4','sess_624232494e57453ebe4975d6e7627b23',2,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:52.672048'),(42,'evt_54d81bd1fdfb434fa8051df8edd6cbec','sess_624232494e57453ebe4975d6e7627b23',2,'CHECKOUT_INITIATED','ORDER',NULL,'{\"subtotal\": 19497, \"cartValue\": 19497, \"itemCount\": 3}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:18:53.088891'),(43,'evt_6ba5189fd17a449baf3db626247f47ac','sess_8a656bf954f348c99a93c5f1252daf43',2,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:19:32.909449'),(44,'evt_9d7cff38de694948bbd11cc872a66a8f','sess_8a656bf954f348c99a93c5f1252daf43',2,'PRODUCT_VIEW','PRODUCT',2,'{\"name\": \"Kanchipuram Temple Border Saree\", \"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:19:32.911206'),(45,'evt_0f789f2e21a94a9b88f28809a4993e15','sess_8a656bf954f348c99a93c5f1252daf43',2,'ADD_TO_CART','PRODUCT',2,'{\"color\": \"Maroon\", \"price\": 6499, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Kanchipuram Temple Border Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:19:32.914600'),(46,'evt_efc49045474d41e297e9c5f1d3a95d7b','sess_1406379a874843f3a9cbb06c28d6de70',NULL,'PRODUCT_VIEW','PRODUCT',11,'{\"name\": \"Taranga Kanchi Silk Brocade Green Saree\", \"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.293616'),(47,'evt_71cbd024b44544dda7af371465b8332e','sess_1406379a874843f3a9cbb06c28d6de70',NULL,'PRODUCT_VIEW','PRODUCT',11,'{\"name\": \"Taranga Kanchi Silk Brocade Green Saree\", \"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.296201'),(48,'evt_02b1f27c3ca946a5b320229dd559fb9d','sess_1406379a874843f3a9cbb06c28d6de70',NULL,'ADD_TO_CART','PRODUCT',11,'{\"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Taranga Kanchi Silk Brocade Green Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.297753'),(49,'evt_0abc0a6f2245459d805313bbd1e35fa1','sess_2cbf208e31cd480d86f35851a6cf680f',NULL,'PRODUCT_VIEW','PRODUCT',11,'{\"name\": \"Taranga Kanchi Silk Brocade Green Saree\", \"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.607262'),(50,'evt_4e77882f3de74256b50e91bb02c49aa4','sess_2cbf208e31cd480d86f35851a6cf680f',NULL,'PRODUCT_VIEW','PRODUCT',11,'{\"name\": \"Taranga Kanchi Silk Brocade Green Saree\", \"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"category\": \"Kanchipuram Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.608558'),(51,'evt_32a5790dfc214afdb43d2a1423f2e7af','sess_2cbf208e31cd480d86f35851a6cf680f',NULL,'ADD_TO_CART','PRODUCT',11,'{\"color\": \"Green\", \"price\": 17650, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Taranga Kanchi Silk Brocade Green Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:20:06.609556'),(52,'evt_69c895d19d7a4132a9134ce1c27e4d27','sess_023b29bcc9f34835a6a84b67ee85e4d8',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:16.037460'),(53,'evt_733e87832d4d428d9eee954524c8a749','sess_023b29bcc9f34835a6a84b67ee85e4d8',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:16.040590'),(54,'evt_7d79c9dc886c43788b1118f150721b37','sess_023b29bcc9f34835a6a84b67ee85e4d8',NULL,'ADD_TO_CART','PRODUCT',1,'{\"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Royal Banarasi Silk Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:16.041475'),(55,'evt_13167fb5cfa2423aa22339c861e620ab','sess_987e75c0b92542cd8fd1aeb50eaa8eda',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15','2026-09-15 07:41:18.193341'),(56,'evt_440eaf17805f4f5f839ea1bcb4ab46a2','sess_987e75c0b92542cd8fd1aeb50eaa8eda',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15','2026-09-15 07:41:18.194787'),(57,'evt_d2349a94867d47ea80137d07f21fd2ef','sess_987e75c0b92542cd8fd1aeb50eaa8eda',NULL,'ADD_TO_CART','PRODUCT',1,'{\"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Royal Banarasi Silk Saree\"}','127.0.0.1','Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15','2026-09-15 07:41:18.196183'),(58,'evt_544109d177294009b0f29bec2d161683','sess_3b7898cebd604415951acd09ed2e986c',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:26.978725'),(59,'evt_96a125502dd546ca9ca22fa83059efc2','sess_3b7898cebd604415951acd09ed2e986c',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:26.980525'),(60,'evt_bc335fe098834f43b449c1230f17e7f6','sess_3b7898cebd604415951acd09ed2e986c',NULL,'ADD_TO_CART','PRODUCT',1,'{\"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Royal Banarasi Silk Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:26.983631'),(61,'evt_44af636c756944869266270ceb708edc','sess_3ca4140c87d842c88e4e96f90d75ad72',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:35.878632'),(62,'evt_894a6f93f01541e583406aa3a9446a22','sess_3ca4140c87d842c88e4e96f90d75ad72',NULL,'PRODUCT_VIEW','PRODUCT',1,'{\"name\": \"Royal Banarasi Silk Saree\", \"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"category\": \"Banarasi Sarees\", \"dwellTimeMs\": 0}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:35.880040'),(63,'evt_8823802e76bd497c93d8b9757edd0ce2','sess_3ca4140c87d842c88e4e96f90d75ad72',NULL,'ADD_TO_CART','PRODUCT',1,'{\"color\": \"Red\", \"price\": 4999, \"fabric\": \"Silk\", \"source\": \"product_detail\", \"quantity\": 1, \"productName\": \"Royal Banarasi Silk Saree\"}','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.55 Safari/537.36','2026-09-15 07:41:35.881637');
/*!40000 ALTER TABLE `customer_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fabrics`
--

DROP TABLE IF EXISTS `fabrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fabrics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `description` text,
  `care_instructions` varchar(255) DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `active` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fabrics`
--

LOCK TABLES `fabrics` WRITE;
/*!40000 ALTER TABLE `fabrics` DISABLE KEYS */;
INSERT INTO `fabrics` VALUES (1,'Silk','silk',NULL,'Professional petrol dry clean only. Wrap in breathable cotton or muslin.',1,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(2,'Cotton','cotton',NULL,'Gentle hand wash in cold water or mild dry clean. Starch if desired.',2,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(3,'Chiffon','chiffon',NULL,'Mild dry clean. Do not wring, hang dry in shade.',3,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(4,'Georgette','georgette',NULL,'Professional dry clean. Steam iron on reverse.',4,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(5,'Tussar Silk','tussar-silk',NULL,'Gentle dry clean. Store away from direct moisture.',5,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(6,'Patola Silk','patola-silk',NULL,'Handloom dry clean only. Heirloom preservation.',6,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(7,'Katan Silk','katan-silk',NULL,'Dry clean only. Refold quarterly to protect zari creases.',7,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(8,'Linen','linen',NULL,'Gentle hand wash or dry clean. Iron while slightly damp.',8,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(9,'Organza','organza',NULL,'Dry clean only. Hang on padded hangers to avoid crushing.',9,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991'),(10,'Chanderi Silk','chanderi-silk',NULL,'Dry clean recommended. Low iron setting.',10,_binary '','2026-09-12 16:49:49.493991','2026-09-12 16:49:49.493991');
/*!40000 ALTER TABLE `fabrics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `graph_sync_failures`
--

DROP TABLE IF EXISTS `graph_sync_failures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `graph_sync_failures` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint DEFAULT NULL,
  `client_event_id` varchar(64) DEFAULT NULL,
  `event_type` varchar(50) NOT NULL,
  `payload` json DEFAULT NULL,
  `error_message` text,
  `retry_count` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `resolved_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_graph_sync_unresolved` (`resolved_at`,`retry_count`),
  KEY `idx_graph_sync_created` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `graph_sync_failures`
--

LOCK TABLES `graph_sync_failures` WRITE;
/*!40000 ALTER TABLE `graph_sync_failures` DISABLE KEYS */;
/*!40000 ALTER TABLE `graph_sync_failures` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_items`
--

DROP TABLE IF EXISTS `inventory_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_items` (
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
  UNIQUE KEY `UKp0mih1lkha7t38jh46r3uu0eg` (`sku`),
  KEY `idx_inventory_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_items`
--

LOCK TABLES `inventory_items` WRITE;
/*!40000 ALTER TABLE `inventory_items` DISABLE KEYS */;
INSERT INTO `inventory_items` VALUES (1,273,'A2-R2-S1','Silk',275,1,'Royal Banarasi Silk Saree',2,'SK-ROYAL-BANARASI-SILK-SA-1','IN_STOCK',4999.00,'2026-09-16 10:26:58.907631','WH-01','WH-01 Bengaluru Central Fulfillment Hub'),(2,12,'A3-R3-S1','Silk',14,2,'Kanchipuram Temple Border Saree',2,'SK-KANCHIPURAM-TEMPLE-BOR-2','IN_STOCK',6499.00,'2026-09-04 12:48:50.019179','WH-01','Bengaluru Central Fulfillment Hub'),(3,48,'A4-R1-S1','Cotton',50,3,'Elegant Cotton Handloom Saree',2,'SK-ELEGANT-COTTON-HANDLOO-3','IN_STOCK',1299.00,'2026-09-04 12:48:50.020172','WH-01','Bengaluru Central Fulfillment Hub'),(4,28,'A5-R2-S1','Chiffon',30,4,'Designer Chiffon Party Saree',2,'SK-DESIGNER-CHIFFON-PARTY-4','IN_STOCK',2999.00,'2026-09-04 12:48:50.020884','WH-01','Bengaluru Central Fulfillment Hub'),(5,18,'A1-R3-S1','Georgette',20,5,'Pure Georgette Embroidered Saree',2,'SK-PURE-GEORGETTE-EMBROID-5','IN_STOCK',3499.00,'2026-09-04 12:48:50.021552','WH-01','Bengaluru Central Fulfillment Hub'),(6,33,'A2-R1-S1','Tussar Silk',35,6,'Tussar Silk Printed Saree',2,'SK-TUSSAR-SILK-PRINTED-SA-6','IN_STOCK',2499.00,'2026-09-04 12:48:50.022204','WH-01','Bengaluru Central Fulfillment Hub'),(7,48,'A3-R2-S1','Silk',50,7,'Banarasi Kora Organza Brocade Baby Pink Saree',2,'SK-BANARASI-KORA-ORGANZA--7','IN_STOCK',12999.00,'2026-09-04 12:48:50.022854','WH-01','Bengaluru Central Fulfillment Hub'),(8,16,'A4-R3-S1','Georgette',18,8,'Indo-Western Designer Saree',2,'SK-INDO-WESTERN-DESIGNER--8','IN_STOCK',5499.00,'2026-09-04 12:48:50.023472','WH-01','Bengaluru Central Fulfillment Hub'),(9,58,'A5-R1-S1','Cotton',60,9,'Gadwal Silk Butta Yellow Saree',2,'SK-GADWAL-SILK-BUTTA-YELL-9','IN_STOCK',899.00,'2026-09-04 12:48:50.024107','WH-01','Bengaluru Central Fulfillment Hub'),(10,46,'A1-R2-S1','Patola Silk',48,10,'Patola Silk Double Ikat Saree',2,'SK-PATOLA-SILK-DOUBLE-IKA-10','IN_STOCK',15999.00,'2026-09-04 12:48:50.024765','WH-01','Bengaluru Central Fulfillment Hub'),(11,7,'A2-R3-S1','Silk',9,11,'Taranga Kanchi Silk Brocade Green Saree',2,'SK-TARANGA-KANCHI-SILK-BR-11','IN_STOCK',17650.00,'2026-09-04 12:48:50.025466','WH-01','Bengaluru Central Fulfillment Hub'),(12,15,'A3-R1-S1','Silk Sarees',15,12,'Exclusive Offline Silk Saree',0,'SK-EXCLUSIVE-OFFLINE-SI-12','IN_STOCK',18500.00,'2026-09-11 15:04:46.204758','WH-01','Bengaluru Central Fulfillment Hub'),(13,8,'A4-R2-S1','Silk Sarees',8,13,'Manager Catalog Drape',0,'SK-MANAGER-CATALOG-DRAP-13','IN_STOCK',12000.00,'2026-09-11 15:04:46.208992','WH-01','Bengaluru Central Fulfillment Hub'),(14,5,'A5-R3-S1','Silk Sarees',5,14,'Royal Crimson Kanchipuram Silk Saree',0,'SK-ROYAL-CRIMSON-KANCHI-14','LOW_STOCK',28500.00,'2026-09-11 15:04:46.210732','WH-01','Bengaluru Central Fulfillment Hub'),(15,3,'A1-R1-S1','Silk Sarees',3,15,'Emerald Green Banarasi Silk Saree',0,'SK-EMERALD-GREEN-BANARA-15','LOW_STOCK',32000.00,'2026-09-11 15:04:46.212529','WH-01','Bengaluru Central Fulfillment Hub'),(16,2,'A2-R2-S1','Silk Sarees',2,16,'Peacock Blue Paithani Silk Saree',0,'SK-PEACOCK-BLUE-PAITHAN-16','LOW_STOCK',45000.00,'2026-09-11 15:04:46.214123','WH-01','Bengaluru Central Fulfillment Hub'),(17,7,'A3-R3-S1','Silk Sarees',7,17,'Chanderi Handloom Gold Saree',0,'SK-CHANDERI-HANDLOOM-GO-17','IN_STOCK',16500.00,'2026-09-11 15:05:55.443106','WH-01','Bengaluru Central Fulfillment Hub'),(18,0,'A4-R1-S1','Silk Sarees',5,18,'Test Empty Image Saree',0,'SK-TEST-EMPTY-IMAGE-SAR-18','OUT_OF_STOCK',2999.00,'2026-09-12 10:58:50.593952','WH-01','Bengaluru Central Fulfillment Hub'),(19,0,'A5-R2-S1','Silk Sarees',3,19,'Crimson Banarasi Saree',0,'SK-CRIMSON-BANARASI-SAR-19','OUT_OF_STOCK',12500.00,'2026-09-12 10:58:50.611430','WH-01','Bengaluru Central Fulfillment Hub'),(20,0,'A1-R3-S1','Silk Sarees',10,20,'Zero Image Artisan Drape',0,'SK-ZERO-IMAGE-ARTISAN-D-20','OUT_OF_STOCK',3499.00,'2026-09-12 11:05:26.672668','WH-01','Bengaluru Central Fulfillment Hub'),(21,0,'A2-R1-S1','Silk Sarees',5,21,'Broken Image Error Test',0,'SK-BROKEN-IMAGE-ERROR-T-21','OUT_OF_STOCK',4999.00,'2026-09-12 11:05:30.365479','WH-01','Bengaluru Central Fulfillment Hub'),(22,0,'A3-R2-S1','Silk Sarees',10,22,'Zero Image Artisan Drape',0,'SK-ZERO-IMAGE-ARTISAN-D-22','OUT_OF_STOCK',3499.00,'2026-09-12 11:06:41.335102','WH-01','Bengaluru Central Fulfillment Hub'),(23,5,'A4-R3-S1','Silk Sarees',5,23,'Broken Image Error Test',0,'SK-BROKEN-IMAGE-ERROR-T-23','LOW_STOCK',4999.00,'2026-09-12 11:06:41.350267','WH-01','Bengaluru Central Fulfillment Hub'),(24,0,'A5-R1-S1','Silk Sarees',10,24,'Zero Image Artisan Drape',0,'SK-ZERO-IMAGE-ARTISAN-D-24','OUT_OF_STOCK',3499.00,'2026-09-12 11:07:12.289110','WH-01','Bengaluru Central Fulfillment Hub'),(25,0,'A1-R2-S1','Silk Sarees',5,25,'Broken Image Error Test',0,'SK-BROKEN-IMAGE-ERROR-T-25','OUT_OF_STOCK',4999.00,'2026-09-12 11:07:15.994034','WH-01','Bengaluru Central Fulfillment Hub');
/*!40000 ALTER TABLE `inventory_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `message` text NOT NULL,
  `target_role` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'2026-09-06 15:19:09.336083',_binary '\0','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #1','STOCK_TRANSFER',NULL),(2,'2026-09-06 15:19:10.101666',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(3,'2026-09-06 15:19:48.256868',_binary '\0','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #2','STOCK_TRANSFER',NULL),(4,'2026-09-06 15:19:48.852700',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(5,'2026-09-06 15:20:21.656140',_binary '\0','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #3','STOCK_TRANSFER',NULL),(6,'2026-09-06 15:20:22.298316',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(7,'2026-09-06 15:21:02.640259',_binary '','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #4','STOCK_TRANSFER',NULL),(8,'2026-09-06 15:21:03.179851',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(9,'2026-09-06 15:21:13.155432',_binary '','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #5','STOCK_TRANSFER',NULL),(10,'2026-09-06 15:21:13.893528',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(11,'2026-09-06 15:21:46.856933',_binary '','/admin/approvals','manager@sareekart.com requested transfer of 5 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-01 Bengaluru Central Fulfillment Hub to WH-02 Mumbai West Distribution Hub.','OWNER','Stock Transfer Request #6','STOCK_TRANSFER',NULL),(12,'2026-09-06 15:21:47.717821',_binary '','/admin/reviews','Review submitted by Chaitanya Customer on product #1: \"The zari work has an authentic antique luster. Pure handloom perfection!\"','ADMIN','New Review Submitted (5★)','REVIEW_SUBMITTED',NULL),(13,'2026-09-11 15:23:58.375794',_binary '','/admin/approvals','manager@sareekart.com requested transfer of 2 units (SK-ROYAL-BANARASI-SILK-SA-1) from WH-03 Kanchipuram Heritage Reserve to WH-01 Bengaluru Central Fulfillment Hub.','OWNER','Stock Transfer Request #8','STOCK_TRANSFER',NULL);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `occasions`
--

DROP TABLE IF EXISTS `occasions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `occasions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `active` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `occasions`
--

LOCK TABLES `occasions` WRITE;
/*!40000 ALTER TABLE `occasions` DISABLE KEYS */;
INSERT INTO `occasions` VALUES (1,'Wedding','wedding','Grand ceremonies, muhurtham, and traditional marriage rituals',1,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(2,'Bridal','bridal','Sacred bridal trousseau and bride-specific ceremonies',2,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(3,'Festive','festive','Festive celebrations, Diwali, Pongal, and Dussehra celebrations',3,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(4,'Festival','festival','Traditional cultural and temple festivals',4,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(5,'Casual','casual','Comfortable outings, weekend wear, and informal gatherings',5,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(6,'Daily','daily','Daily office wear and breathable everyday comfort',6,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(7,'Party','party','Cocktails, sangeet celebrations, and evening parties',7,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575'),(8,'Formal','formal','Conferences, academic convocations, and executive ceremonies',8,_binary '','2026-09-12 16:49:49.499575','2026-09-12 16:49:49.499575');
/*!40000 ALTER TABLE `occasions` ENABLE KEYS */;
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
  `product_name` varchar(255) NOT NULL DEFAULT 'Saree Product',
  `product_image` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `chk_order_items_positive_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,4999.00,1,1,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(2,2999.00,1,1,4,'Designer Chiffon Party Saree','https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(3,6499.00,2,2,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(4,1299.00,1,3,3,'Elegant Cotton Handloom Saree','https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466'),(5,2999.00,2,4,4,'Designer Chiffon Party Saree','https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(6,12999.00,1,4,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(7,3499.00,1,5,5,'Pure Georgette Embroidered Saree','https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800'),(8,2499.00,2,6,6,'Tussar Silk Printed Saree','https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800'),(9,12999.00,1,7,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(10,15999.00,1,7,10,'Patola Silk Double Ikat Saree','https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800'),(11,5499.00,2,8,8,'Indo-Western Designer Saree','https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800'),(12,899.00,1,9,9,'Gadwal Silk Butta Yellow Saree','https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059'),(13,15999.00,2,10,10,'Patola Silk Double Ikat Saree','https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800'),(14,6499.00,1,10,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(15,17650.00,1,11,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(16,4999.00,2,12,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(17,6499.00,1,13,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(18,3499.00,1,13,5,'Pure Georgette Embroidered Saree','https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800'),(19,1299.00,2,14,3,'Elegant Cotton Handloom Saree','https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466'),(20,2999.00,1,15,4,'Designer Chiffon Party Saree','https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(21,3499.00,2,16,5,'Pure Georgette Embroidered Saree','https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800'),(22,5499.00,1,16,8,'Indo-Western Designer Saree','https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800'),(23,2499.00,1,17,6,'Tussar Silk Printed Saree','https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800'),(24,12999.00,2,18,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(25,5499.00,1,19,8,'Indo-Western Designer Saree','https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800'),(26,17650.00,1,19,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(27,899.00,2,20,9,'Gadwal Silk Butta Yellow Saree','https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059'),(28,15999.00,1,21,10,'Patola Silk Double Ikat Saree','https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800'),(29,17650.00,2,22,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(30,1299.00,1,22,3,'Elegant Cotton Handloom Saree','https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466'),(31,4999.00,1,23,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(32,6499.00,2,24,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(33,1299.00,1,25,3,'Elegant Cotton Handloom Saree','https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466'),(34,2499.00,1,25,6,'Tussar Silk Printed Saree','https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800'),(35,2999.00,2,26,4,'Designer Chiffon Party Saree','https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(36,3499.00,1,27,5,'Pure Georgette Embroidered Saree','https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800'),(37,2499.00,2,28,6,'Tussar Silk Printed Saree','https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800'),(38,899.00,1,28,9,'Gadwal Silk Butta Yellow Saree','https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059'),(39,12999.00,1,29,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(40,5499.00,2,30,8,'Indo-Western Designer Saree','https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800'),(41,899.00,1,31,9,'Gadwal Silk Butta Yellow Saree','https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059'),(42,4999.00,1,31,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(43,15999.00,2,32,10,'Patola Silk Double Ikat Saree','https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800'),(44,17650.00,1,33,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(45,4999.00,2,34,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(46,2999.00,1,34,4,'Designer Chiffon Party Saree','https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800'),(47,6499.00,1,35,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(48,4999.00,1,36,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(49,4999.00,1,37,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(50,12999.00,1,38,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(51,12999.00,1,39,7,'Banarasi Kora Organza Brocade Baby Pink Saree','https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013'),(52,6499.00,1,40,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(53,17650.00,9,40,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(54,4999.00,1,41,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(55,4999.00,1,42,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(56,6499.00,1,42,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(57,4999.00,1,43,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(58,6499.00,8,44,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(59,6499.00,4,45,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(60,4999.00,1,46,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(61,4999.00,1,47,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(62,17650.00,1,47,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(63,6499.00,6,48,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(64,17650.00,8,48,11,'Taranga Kanchi Silk Brocade Green Saree','https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282'),(65,4999.00,1,49,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(66,6499.00,10,49,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800'),(67,4999.00,1,50,1,'Royal Banarasi Silk Saree','https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070'),(68,6499.00,9,50,2,'Kanchipuram Temple Border Saree','https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800');
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
  `courier_partner` varchar(255) DEFAULT NULL,
  `current_location` varchar(255) DEFAULT NULL,
  `estimated_delivery_date` varchar(255) DEFAULT NULL,
  `tracking_number` varchar(255) DEFAULT NULL,
  `idempotency_key` varchar(64) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `wallet_credit_used` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_orders_user_idempotency` (`user_id`,`idempotency_key`),
  KEY `idx_orders_user_created` (`user_id`,`created_at` DESC),
  KEY `idx_orders_status_created` (`status`,`created_at` DESC),
  KEY `idx_orders_razorpay_order_id` (`razorpay_order_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=168 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'2026-09-04 15:43:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Bengaluru','Chaitanya Customer','9876543211','560001','Karnataka','Flat 100, Residency Road','DELIVERED',7198.20,'2026-09-04 15:43:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2,'2026-09-04 14:36:16.942847','COD','COMPLETED',NULL,NULL,'Mumbai','Priya Sharma','9876543220','400001','Maharashtra','Flat 101, Residency Road','DELIVERED',12998.00,'2026-09-04 14:36:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3,'2026-09-03 13:29:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Delhi','Ananya Verma','9876543221','110001','Delhi','Flat 102, Residency Road','DELIVERED',1449.00,'2026-09-03 13:29:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4,'2026-09-03 12:22:16.942847','COD','COMPLETED',NULL,NULL,'Hyderabad','Chaitanya Customer','9876543211','500001','Telangana','Flat 103, Residency Road','DELIVERED',18997.00,'2026-09-03 12:22:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(5,'2026-09-02 11:15:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Chennai','Priya Sharma','9876543220','600001','Tamil Nadu','Flat 104, Residency Road','DELIVERED',3299.10,'2026-09-02 11:15:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(6,'2026-09-01 10:08:16.942847','COD','COMPLETED',NULL,NULL,'Bengaluru','Ananya Verma','9876543221','560001','Karnataka','Flat 105, Residency Road','DELIVERED',5148.00,'2026-09-01 10:08:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(7,'2026-08-31 09:01:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Mumbai','Chaitanya Customer','9876543211','400001','Maharashtra','Flat 106, Residency Road','DELIVERED',28998.00,'2026-08-31 09:01:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(8,'2026-08-30 07:54:16.942847','COD','COMPLETED',NULL,NULL,'Delhi','Priya Sharma','9876543220','110001','Delhi','Flat 107, Residency Road','DELIVERED',8798.40,'2026-08-30 07:54:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(9,'2026-08-29 06:47:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Hyderabad','Ananya Verma','9876543221','500001','Telangana','Flat 108, Residency Road','DELIVERED',959.10,'2026-08-29 06:47:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(10,'2026-08-28 06:40:16.942847','COD','COMPLETED',NULL,NULL,'Chennai','Chaitanya Customer','9876543211','600001','Tamil Nadu','Flat 109, Residency Road','DELIVERED',38497.00,'2026-08-28 06:40:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(11,'2026-08-27 05:33:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Bengaluru','Priya Sharma','9876543220','560001','Karnataka','Flat 110, Residency Road','DELIVERED',17650.00,'2026-08-27 05:33:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(12,'2026-08-25 04:26:16.942847','COD','COMPLETED',NULL,NULL,'Mumbai','Ananya Verma','9876543221','400001','Maharashtra','Flat 111, Residency Road','DELIVERED',9998.00,'2026-08-25 04:26:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(13,'2026-08-23 15:19:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Delhi','Chaitanya Customer','9876543211','110001','Delhi','Flat 112, Residency Road','DELIVERED',8998.20,'2026-08-23 15:19:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(14,'2026-08-21 14:12:16.942847','COD','COMPLETED',NULL,NULL,'Hyderabad','Priya Sharma','9876543220','500001','Telangana','Flat 113, Residency Road','DELIVERED',2748.00,'2026-08-21 14:12:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(15,'2026-08-19 13:05:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Chennai','Ananya Verma','9876543221','600001','Tamil Nadu','Flat 114, Residency Road','DELIVERED',2549.20,'2026-08-19 13:05:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(16,'2026-08-17 11:58:16.942847','COD','REFUNDED/CANCELLED',NULL,NULL,'Bengaluru','Chaitanya Customer','9876543211','560001','Karnataka','Flat 115, Residency Road','CANCELLED',12497.00,'2026-08-17 11:58:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(17,'2026-08-15 10:51:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Mumbai','Priya Sharma','9876543220','400001','Maharashtra','Flat 116, Residency Road','DELIVERED',2399.10,'2026-08-15 10:51:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(18,'2026-08-13 09:44:16.942847','COD','COMPLETED',NULL,NULL,'Delhi','Ananya Verma','9876543221','110001','Delhi','Flat 117, Residency Road','DELIVERED',25998.00,'2026-08-13 09:44:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(19,'2026-08-11 09:37:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Hyderabad','Chaitanya Customer','9876543211','500001','Telangana','Flat 118, Residency Road','DELIVERED',23149.00,'2026-08-11 09:37:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(20,'2026-08-09 08:30:16.942847','COD','COMPLETED',NULL,NULL,'Chennai','Priya Sharma','9876543220','600001','Tamil Nadu','Flat 119, Residency Road','DELIVERED',1948.00,'2026-08-09 08:30:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(21,'2026-08-07 07:23:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Bengaluru','Ananya Verma','9876543221','560001','Karnataka','Flat 120, Residency Road','DELIVERED',14399.10,'2026-08-07 07:23:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(22,'2026-08-05 06:16:16.942847','COD','COMPLETED',NULL,NULL,'Mumbai','Chaitanya Customer','9876543211','400001','Maharashtra','Flat 121, Residency Road','DELIVERED',29279.20,'2026-08-05 06:16:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(23,'2026-08-02 05:09:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Delhi','Priya Sharma','9876543220','110001','Delhi','Flat 122, Residency Road','DELIVERED',5149.00,'2026-08-02 05:09:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(24,'2026-07-30 04:02:16.942847','COD','COMPLETED',NULL,NULL,'Hyderabad','Ananya Verma','9876543221','500001','Telangana','Flat 123, Residency Road','DELIVERED',12998.00,'2026-07-30 04:02:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(25,'2026-07-26 14:55:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Chennai','Chaitanya Customer','9876543211','600001','Tamil Nadu','Flat 124, Residency Road','DELIVERED',3568.20,'2026-07-26 14:55:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(26,'2026-07-21 13:48:16.942847','COD','COMPLETED',NULL,NULL,'Bengaluru','Priya Sharma','9876543220','560001','Karnataka','Flat 125, Residency Road','DELIVERED',5998.00,'2026-07-21 13:48:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(27,'2026-07-16 13:41:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Mumbai','Ananya Verma','9876543221','400001','Maharashtra','Flat 126, Residency Road','DELIVERED',3649.00,'2026-07-16 13:41:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(28,'2026-07-11 12:34:16.942847','COD','COMPLETED',NULL,NULL,'Delhi','Chaitanya Customer','9876543211','110001','Delhi','Flat 127, Residency Road','DELIVERED',5897.00,'2026-07-11 12:34:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(29,'2026-07-06 11:27:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Hyderabad','Priya Sharma','9876543220','500001','Telangana','Flat 128, Residency Road','DELIVERED',11699.10,'2026-07-06 11:27:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(30,'2026-07-01 10:20:16.942847','COD','COMPLETED',NULL,NULL,'Chennai','Ananya Verma','9876543221','600001','Tamil Nadu','Flat 129, Residency Road','DELIVERED',10998.00,'2026-07-01 10:20:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(31,'2026-06-26 09:13:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Bengaluru','Chaitanya Customer','9876543211','560001','Karnataka','Flat 130, Residency Road','DELIVERED',5898.00,'2026-06-26 09:13:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(32,'2026-06-21 08:06:16.942847','COD','COMPLETED',NULL,NULL,'Mumbai','Priya Sharma','9876543220','400001','Maharashtra','Flat 131, Residency Road','DELIVERED',31998.00,'2026-06-21 08:06:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(33,'2026-06-16 06:59:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Delhi','Ananya Verma','9876543221','110001','Delhi','Flat 132, Residency Road','DELIVERED',15885.00,'2026-06-16 06:59:16.942847',9,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(34,'2026-06-11 05:52:16.942847','COD','COMPLETED',NULL,NULL,'Hyderabad','Chaitanya Customer','9876543211','500001','Telangana','Flat 133, Residency Road','DELIVERED',12997.00,'2026-06-11 05:52:16.942847',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(35,'2026-06-08 04:45:16.942847','RAZORPAY','COMPLETED',NULL,NULL,'Chennai','Priya Sharma','9876543220','600001','Tamil Nadu','Flat 134, Residency Road','DELIVERED',6499.00,'2026-06-08 04:45:16.942847',8,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(36,'2026-09-06 10:33:13.402579','COD','CANCELLED',NULL,NULL,'Bengaluru','Priya Sharma','9876543210','560038','Karnataka','42 Heritage Loom Lane, Indiranagar','CANCELLED',5149.00,'2026-09-06 10:33:13.402579',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(37,'2026-09-06 10:33:56.658126','COD','CANCELLED',NULL,NULL,'Bengaluru','Priya Sharma','9876543210','560038','Karnataka','42 Heritage Loom Lane, Indiranagar','CANCELLED',5149.00,'2026-09-06 11:29:11.923819',2,'BlueDart Express','Kanchipuram Artisan Guild - Atelier Central Hub','3-4 Working Days','SK-BD-1028749',NULL,NULL,NULL),(38,'2026-09-06 10:37:52.834728','RAZORPAY','CANCELLED',NULL,NULL,'venkatagiri','Chaitanya Customer','8985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','CANCELLED',12999.00,'2026-09-06 10:37:52.834728',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(39,'2026-09-06 10:38:19.618694','COD','CANCELLED',NULL,NULL,'venkatagiri','Chaitanya Customer','8985975765','517501','andhrapradesh','16/642/4 arava brahmana veedi venkatagiri tirupati','CANCELLED',12999.00,'2026-09-06 11:42:10.035868',2,'BlueDart Express','Kanchipuram Artisan Guild - Atelier Central Hub','3-4 Working Days','SK-BD-1030303',NULL,NULL,NULL),(40,'2026-09-12 15:57:28.290031','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',165349.00,'2026-09-12 15:57:28.290031',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-9932054','e2e_idem_1789228648097',NULL,0.00),(41,'2026-09-12 15:58:16.206425','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',5149.00,'2026-09-12 15:58:16.206425',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-2244997','e2e_idem_1789228696137',NULL,0.00),(42,'2026-09-12 15:59:46.638992','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',11498.00,'2026-09-12 15:59:46.638992',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-1720526','e2e_idem_1789228786571',NULL,0.00),(43,'2026-09-12 16:02:54.486022','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',5149.00,'2026-09-12 16:02:54.486022',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-5697127','e2e_idem_1789228974432',NULL,0.00),(44,'2026-09-12 16:02:57.251377','COD','CANCELLED',NULL,NULL,'Chennai','Radha Krishna','9876543210','600001','Tamil Nadu','77 Silk Weavers Colony, Gandhi Road','CANCELLED',51992.00,'2026-09-12 16:02:57.251377',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-4436478','chk_xv1ti02y1_mtykqhrx',NULL,0.00),(45,'2026-09-12 16:03:11.926404','COD','CANCELLED',NULL,NULL,'Chennai','Radha Krishna','9876543210','600001','Tamil Nadu','77 Silk Weavers Colony, Gandhi Road','CANCELLED',25996.00,'2026-09-12 16:03:11.926404',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-3169126','chk_f5xonx95g_mtykqt1b',NULL,0.00),(46,'2026-09-12 16:03:12.413461','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',5149.00,'2026-09-12 16:03:12.413461',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-3643331','e2e_idem_1789228992362',NULL,0.00),(47,'2026-09-12 16:04:05.700194','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',22649.00,'2026-09-12 16:04:05.700194',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-3391401','e2e_idem_1789229045685',NULL,0.00),(48,'2026-09-12 16:04:06.911608','COD','CANCELLED',NULL,NULL,'Chennai','Radha Krishna','9876543210','600001','Tamil Nadu','77 Silk Weavers Colony, Gandhi Road','CANCELLED',180194.00,'2026-09-12 16:04:06.911608',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-1149233','chk_8x87vh85z_mtykrzjf',NULL,0.00),(49,'2026-09-15 07:18:50.768295','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',69989.00,'2026-09-15 07:18:50.768295',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-3650873','e2e_idem_1789456730737',NULL,0.00),(50,'2026-09-15 07:19:46.865205','COD','CANCELLED',NULL,NULL,'Chennai','Idempotency E2E Test','9876543210','600001','Tamil Nadu','100 Queen Street','CANCELLED',63490.00,'2026-09-15 07:19:46.865205',2,'Blue Dart Apex Air','Kanchipuram Reserve Atelier (WH-03)','Next Day Priority Delivery','SK-BD-9132434','e2e_idem_1789456786858',NULL,0.00);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
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
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES (1,'2026-09-04 14:55:54.257221','2026-09-04 15:25:54.253689','ab4d885e-8c5f-4013-ab50-c607a5a4283c',_binary '',2),(2,'2026-09-04 14:56:27.388274','2026-09-04 15:26:27.388164','c7239291-5974-4d79-a5ae-e2fc32e07946',_binary '',2),(3,'2026-09-04 14:56:59.448873','2026-09-04 15:26:59.448580','697aebca-55f6-4867-b9f2-0b506d9bdcdb',_binary '',2),(4,'2026-09-04 14:57:01.001036','2026-09-04 15:27:01.000949','28eed2f3-529d-4363-a370-d06ac248f708',_binary '',2),(5,'2026-09-04 14:57:08.687966','2026-09-04 15:27:08.687873','1a171064-bd11-44c4-a392-6862563f9561',_binary '',2),(6,'2026-09-04 14:57:10.259560','2026-09-04 15:27:10.259470','4255e701-3991-4f3a-b359-55332be3445a',_binary '',2),(7,'2026-09-04 14:57:55.904864','2026-09-04 15:27:55.904738','a286e0df-281a-4fcd-90af-fb15d84f3e44',_binary '',6),(8,'2026-09-04 15:22:16.304469','2026-09-04 15:52:16.304037','4e848723-257e-41f8-a497-1457b4facc83',_binary '',7),(9,'2026-09-05 11:47:45.417724','2026-09-05 12:17:45.416156','3480d676-6232-4890-8c4e-91aa105b498e',_binary '',10),(10,'2026-09-06 10:55:15.238078','2026-09-06 11:25:15.237587','05ad61e6-dcdf-483a-b0a7-afbcb1d6199c',_binary '',2),(11,'2026-09-06 10:57:04.077511','2026-09-06 11:27:04.070914','67f3db18-28bb-4d00-b5ed-37af0151a947',_binary '',2),(12,'2026-09-06 10:57:59.934959','2026-09-06 11:27:59.931792','96234b57-a433-45ca-ac24-0700a87e575e',_binary '',2),(13,'2026-09-06 10:58:00.689578','2026-09-06 11:28:00.689444','18e32eb1-fb39-4f9a-851e-3119c15d5c04',_binary '',2),(14,'2026-09-06 10:58:04.749885','2026-09-06 11:28:04.749611','0b6f87df-c1f8-4f47-9686-291427a7c5e0',_binary '',11),(15,'2026-09-06 10:58:05.402906','2026-09-06 11:28:05.402574','466bb4d8-1f95-4234-8f53-c7f246cff922',_binary '',2),(16,'2026-09-06 10:58:06.094307','2026-09-06 11:28:06.091667','a9e6d8c4-6c2e-4415-873b-d11704707985',_binary '',2),(17,'2026-09-06 10:58:16.096385','2026-09-06 11:28:16.096019','014015b8-8c95-41ca-b2a9-d346bf33b375',_binary '',12),(18,'2026-09-06 10:58:16.861743','2026-09-06 11:28:16.861606','8599c08e-c743-4d46-a7c9-597c601bedcf',_binary '',2),(19,'2026-09-06 10:58:17.581478','2026-09-06 11:28:17.581046','0c40dcde-144f-4a24-a7d7-185bdb7735da',_binary '',2),(20,'2026-09-06 10:58:23.293120','2026-09-06 11:28:23.292424','c45e8acf-05cd-4feb-bc33-af4c86f7f64b',_binary '',2),(21,'2026-09-06 10:58:23.741309','2026-09-06 11:28:23.741184','7ae0cca8-bf51-4a2c-af44-4f94759dc3b3',_binary '',2);
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pincode_overrides`
--

DROP TABLE IF EXISTS `pincode_overrides`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pincode_overrides` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(100) NOT NULL,
  `is_cod_available` bit(1) NOT NULL,
  `courier_partner` varchar(100) NOT NULL,
  `notes` text,
  `pincode` varchar(6) NOT NULL,
  `is_serviceable` bit(1) NOT NULL,
  `state` varchar(100) NOT NULL,
  `transit_days` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `zone` enum('METRO','REGIONAL','REMOTE','TIER_1','TIER_2') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pincode_overrides_pincode` (`pincode`),
  KEY `idx_pincode_overrides_pin` (`pincode`),
  KEY `idx_pincode_overrides_zone` (`zone`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pincode_overrides`
--

LOCK TABLES `pincode_overrides` WRITE;
/*!40000 ALTER TABLE `pincode_overrides` DISABLE KEYS */;
/*!40000 ALTER TABLE `pincode_overrides` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `product_id` bigint NOT NULL,
  `image_url` varchar(1000) DEFAULT NULL,
  `image_order` int NOT NULL,
  KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`),
  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_images`
--

LOCK TABLES `product_images` WRITE;
/*!40000 ALTER TABLE `product_images` DISABLE KEYS */;
INSERT INTO `product_images` VALUES (2,'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?w=800',0),(4,'https://images.unsplash.com/photo-1609357605129-26f69add5d6e?w=800',0),(5,'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?w=800',0),(6,'https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=800',0),(8,'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800',0),(3,'https://kankatala.com/cdn/shop/files/1216471915_3.jpg?v=1781619466',0),(9,'https://kankatala.com/cdn/shop/files/1216484173_2.jpg?v=1781672059',0),(7,'https://kankatala.com/cdn/shop/files/1216456877_4.jpg?v=1781606013',0),(10,'https://kankatala.com/cdn/shop/files/1216476212_1.jpg?v=1781431994oto-1617627143233-46b92015e905?w=800',0),(11,'https://kankatala.com/cdn/shop/files/1216488898_1.jpg?v=1781606282',0),(1,'https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070',0),(15,'https://images.unsplash.com/photo-1583391265517-35bbdad01209?auto=format&fit=crop&fm=webp&w=800&q=80',0),(16,'https://kankatala.com/cdn/shop/files/1216730670_1.webp?v=1786097422&width=1070',0),(17,'https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=800&q=80',0),(19,'https://kankatala.com/cdn/shop/files/1216484173_2.jpg',0),(19,'https://kankatala.com/cdn/shop/files/1216471915_3.jpg',1),(21,'https://invalid-non-existent-domain-404.com/broken-saree.jpg',0),(23,'https://invalid-non-existent-domain-404.com/broken-saree.jpg',0),(12,'https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=800&q=80',0),(13,'https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134',0),(14,'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80',0),(25,'https://invalid-non-existent-domain-404.com/broken-saree.jpg',0);
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
  `color_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `fabric` varchar(255) DEFAULT NULL,
  `fabric_id` bigint DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `occasion` varchar(255) DEFAULT NULL,
  `occasion_id` bigint DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock_quantity` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  KEY `fk_products_fabric` (`fabric_id`),
  KEY `fk_products_occasion` (`occasion_id`),
  KEY `fk_products_color` (`color_id`),
  KEY `idx_products_active_price` (`active`,`price`),
  KEY `idx_products_active_category` (`active`,`category_id`),
  CONSTRAINT `fk_products_color` FOREIGN KEY (`color_id`) REFERENCES `colors` (`id`),
  CONSTRAINT `fk_products_fabric` FOREIGN KEY (`fabric_id`) REFERENCES `fabrics` (`id`),
  CONSTRAINT `fk_products_occasion` FOREIGN KEY (`occasion_id`) REFERENCES `occasions` (`id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `chk_products_stock_non_negative` CHECK ((`stock_quantity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=3844 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,_binary '','Red',1,'2026-07-01 09:31:11.542572','Handwoven Banarasi silk saree with intricate gold zari work. Perfect for weddings and grand celebrations.','Silk',1,'Royal Banarasi Silk Saree','Wedding',1,4999.00,266,'2026-09-12 10:35:14.314689',5),(2,_binary '','Maroon',2,'2026-07-01 09:31:11.544713','Authentic Kanchipuram silk saree with traditional temple border design and rich pallu.','Silk',1,'Kanchipuram Temple Border Saree','Wedding',1,6499.00,0,'2026-07-01 10:05:58.229637',6),(3,_binary '','White',10,'2026-07-01 09:31:11.545442','Soft handloom cotton saree with beautiful jamdani weave. Ideal for daily wear and office.','Cotton',2,'Elegant Cotton Handloom Saree','Casual',5,1299.00,50,'2026-07-01 10:29:06.585548',2),(4,_binary '','Pink',6,'2026-07-01 09:31:11.546122','Stunning chiffon saree with sequin work and designer blouse piece. Perfect for parties.','Chiffon',3,'Designer Chiffon Party Saree','Party',7,2999.00,30,'2026-07-01 09:31:11.546122',3),(5,_binary '','Green',12,'2026-07-01 09:31:11.546882','Luxurious georgette saree with heavy embroidery work and stone detailing.','Georgette',4,'Pure Georgette Embroidered Saree','Festival',4,3499.00,20,'2026-07-01 09:31:11.546882',4),(6,_binary '','Beige',20,'2026-07-01 09:31:11.547740','Natural tussar silk saree with hand block prints. Eco-friendly and stylish.','Tussar Silk',5,'Tussar Silk Printed Saree','Casual',5,2499.00,35,'2026-07-01 09:31:11.547740',1),(7,_binary '','baby pink',7,'2026-07-01 09:31:11.548373','Embrace the grace of tradition with our Banarasi Kora Organza Brocade Bay Pink Saree, a masterpiece of heritage-rich craftsmanship The body is adorned with exquisite brocade that reflects the timeless sophistication of gold zari work, weaving a tapestry of elegance and charm\n\nThe saree features a self gold zari brocade border in a subtle bay pink hue, offering an understated yet luxurious finish The harmonious pallu showcases a self brocade pattern, enhancing the saree\'s allure with its intricate detailing\n\nCompleting this elegant ensemble is a self plain blouse, designed to complement the saree\'s opulence and create a cohesive look Perfect for weddings or special occasions, this saree is a testament to refined elegance and tradition','Silk',1,'Banarasi Kora Organza Brocade Baby Pink Saree','Wedding',1,12999.00,48,'2026-09-06 10:38:19.626376',8),(8,_binary '','Navy Blue',14,'2026-07-01 09:31:11.549106','Modern indo-western designer saree with contemporary drape style and pre-stitched pallu.','Georgette',4,'Indo-Western Designer Saree','Party',7,5499.00,18,'2026-07-01 09:31:11.549106',7),(9,_binary '','Yellow',16,'2026-07-01 09:31:11.549891','Embrace the essence of tradition with the Gadwal Silk Butta Yellow Saree, a masterpiece of heritage-rich elegance. The vibrant yellow body is adorned with exquisite silk buttas, each telling a story of age-old craftsmanship and sophistication\n\nThe saree\'s border is a harmonious blend of intricate zari work, enhancing its grandeur while the pallu features elaborate zari motifs that cascade beautifully, adding a touch of opulence and charm These elements work together to create a visual symphony of grace and luxury\n\nPaired with a coordinated blouse, this saree offers a seamless fusion of tradition and modernity, making it a perfect choice for weddings and grand celebrations. Experience the allure of silk and zari in every drape, elevating your ensemble to one of timeless beauty','Cotton',2,'Gadwal Silk Butta Yellow Saree','Festive',3,899.00,60,'2026-07-01 10:32:07.792705',2),(10,_binary '','pink',6,'2026-07-01 09:31:11.550603','Rare Patola silk saree with double ikat weave from Gujarat. A collector\'s piece.','Patola Silk',6,'Patola Silk Double Ikat Saree','Wedding',1,15999.00,50,'2026-09-04 12:59:30.504999',1),(11,_binary '','Green',12,'2026-07-01 10:24:15.441450','Embrace the beauty of tradition with the Taranga Kanchi Silk Brocade Green Saree, a true embodiment of heritage-rich elegance The lush green body is adorned with intricate brocade motifs that whisper tales of age-old craftsmanship, while the silk fabric drapes you in luxurious grace\n\nA striking contrast is offered by the gold zari brocade border in a rich purple hue, adding a touch of regal splendor The saree\'s pallu, crafted in a harmonious brocade, enhances the overall allure with its fine detailing\n\nCompleting the ensemble is a plain, coordinated blouse that perfectly balances the intricate designs of the saree Woven from the finest silk, this saree is a testament to elegance and timeless style, perfect for those cherished occasions','Silk',1,'Taranga Kanchi Silk Brocade Green Saree','Festive',3,17650.00,11,'2026-09-09 15:26:10.353305',6),(12,_binary '','Royal Gold',18,'2026-09-11 09:15:32.583202','Handcrafted tissue silk drape','Silk',1,'Exclusive Offline Silk Saree','Wedding',1,18500.00,15,'2026-09-11 09:15:32.583202',1),(13,_binary '','Ruby Pink',8,'2026-09-11 09:15:39.081295','Added by Store Manager','Silk',1,'Manager Catalog Drape','Festive',3,12000.00,8,'2026-09-11 09:15:39.081295',1),(14,_binary '','Crimson Red',4,'2026-09-11 14:51:18.994638','Pure zari handwoven silk saree with intricate temple borders','Silk',1,'Royal Crimson Kanchipuram Silk Saree','Wedding',1,28500.00,5,'2026-09-11 14:51:18.994638',1),(15,_binary '','Emerald Green',13,'2026-09-11 14:51:24.553574','Exquisite Banarasi silk saree with gold floral vines','Silk',1,'Emerald Green Banarasi Silk Saree','Wedding',1,32000.00,3,'2026-09-11 14:51:24.553574',1),(16,_binary '','Peacock Blue',15,'2026-09-11 14:51:29.248686','Rich Paithani weave with mor bangadi pallu motif','Silk',1,'Peacock Blue Paithani Silk Saree','Wedding',1,45000.00,2,'2026-09-11 14:51:29.248686',1),(17,_binary '','Honey Gold',19,'2026-09-11 15:05:55.428733','Featherlight pure Chanderi saree with zari butta motifs','Silk',1,'Chanderi Handloom Gold Saree','Festive',3,16500.00,7,'2026-09-11 15:05:55.428733',1),(18,_binary '\0','Gold',17,'2026-09-12 10:58:30.012501','Test saree with no images','Silk',1,'Test Empty Image Saree','Party',7,2999.00,5,'2026-09-12 10:58:50.593910',1),(19,_binary '\0','Crimson',3,'2026-09-12 10:58:35.696700','Pure silk banarasi','Katan Silk',7,'Crimson Banarasi Saree','Bridal',2,12500.00,3,'2026-09-12 10:58:50.611388',1),(20,_binary '\0','Ivory',11,'2026-09-12 11:05:25.802404','Testing clean zero-image state','Cotton',2,'Zero Image Artisan Drape','Daily',6,3499.00,10,'2026-09-12 11:05:26.672608',1),(21,_binary '\0','Rose',9,'2026-09-12 11:05:26.683516','Testing error placeholder','Silk',1,'Broken Image Error Test','Party',7,4999.00,5,'2026-09-12 11:05:30.365428',1),(22,_binary '\0','Ivory',11,'2026-09-12 11:06:40.463177','Testing clean zero-image state','Cotton',2,'Zero Image Artisan Drape','Daily',6,3499.00,10,'2026-09-12 11:06:41.335026',1),(23,_binary '','Rose',9,'2026-09-12 11:06:41.347595','Testing error placeholder','Silk',1,'Broken Image Error Test','Party',7,4999.00,5,'2026-09-12 11:06:41.347595',1),(24,_binary '\0','Ivory',11,'2026-09-12 11:07:11.407732','Testing clean zero-image state','Cotton',2,'Zero Image Artisan Drape','Daily',6,3499.00,10,'2026-09-12 11:07:12.289057',1),(25,_binary '\0','Rose',9,'2026-09-12 11:07:12.300238','Testing error placeholder','Silk',1,'Broken Image Error Test','Party',7,4999.00,5,'2026-09-12 11:07:15.993954',1);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `return_requests`
--

DROP TABLE IF EXISTS `return_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `comments` text,
  `created_at` datetime(6) DEFAULT NULL,
  `exchange_sku` varchar(100) DEFAULT NULL,
  `images` text,
  `reason` enum('COLOR_MISMATCH','FABRIC_FEEL','INCORRECT_ITEM','OTHER','SIZE_MISMATCH','ZARI_DEFECT') NOT NULL,
  `refund_amount` decimal(10,2) DEFAULT NULL,
  `refund_mode` enum('EXCHANGE_DRAPE','ORIGINAL_PAYMENT','STORE_CREDIT') DEFAULT NULL,
  `reverse_courier` varchar(100) DEFAULT NULL,
  `reverse_tracking_number` varchar(100) DEFAULT NULL,
  `status` enum('APPROVED','COMPLETED','PENDING','PICKUP_SCHEDULED','REJECTED') NOT NULL,
  `type` enum('EXCHANGE','RETURN') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_requests_order` (`order_id`),
  KEY `idx_return_requests_user` (`user_id`),
  KEY `idx_return_requests_order` (`order_id`),
  KEY `idx_return_requests_status` (`status`),
  KEY `idx_return_requests_created_at` (`created_at`),
  CONSTRAINT `FK6pd9hi2rbbct43io2pgcma1sh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbski88d6kewx0cbj5pk7nes01` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `return_requests`
--

LOCK TABLES `return_requests` WRITE;
/*!40000 ALTER TABLE `return_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `return_requests` ENABLE KEYS */;
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
  `status` varchar(255) DEFAULT NULL,
  `verified_buyer` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reviews_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:19:10.077361',1,5,'Chaitanya Customer','APPROVED',_binary ''),(2,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:19:48.843154',1,5,'Chaitanya Customer','APPROVED',_binary ''),(3,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:20:22.291546',1,5,'Chaitanya Customer','APPROVED',_binary ''),(4,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:21:03.172708',1,5,'Chaitanya Customer','APPROVED',_binary ''),(5,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:21:13.887482',1,5,'Chaitanya Customer','APPROVED',_binary ''),(6,'The zari work has an authentic antique luster. Pure handloom perfection!','2026-09-06 15:21:47.697235',1,5,'Chaitanya Customer','APPROVED',_binary '');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_transfers`
--

DROP TABLE IF EXISTS `stock_transfers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transfers` (
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_transfers`
--

LOCK TABLES `stock_transfers` WRITE;
/*!40000 ALTER TABLE `stock_transfers` DISABLE KEYS */;
INSERT INTO `stock_transfers` VALUES (1,'owner@sareekart.com',4,'2026-09-06 15:19:09.309684','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-09 15:16:04.338564'),(2,'owner@sareekart.com',4,'2026-09-06 15:19:48.251322','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-09 15:16:03.322896'),(3,'owner@sareekart.com',4,'2026-09-06 15:20:21.642611','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-09 15:16:02.408068'),(4,'owner@sareekart.com',4,'2026-09-06 15:21:02.637464','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-06 15:21:05.156735'),(5,'owner@sareekart.com',4,'2026-09-06 15:21:13.150338','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-06 15:21:15.598827'),(6,'owner@sareekart.com',4,'2026-09-06 15:21:46.806437','Royal Banarasi Silk Saree',5,'Diwali wedding demand transfer to Mumbai hub','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-02 Mumbai West Distribution Hub','2026-09-06 15:21:49.023534'),(7,'owner@sareekart.com',4,'2026-09-11 15:23:53.910986','Royal Banarasi Silk Saree',5,'Direct festive rebalance by Owner','owner@sareekart.com',4,'Directly executed by OWNER','SK-ROYAL-BANARASI-SILK-SA-1','WH-01 Bengaluru Central Fulfillment Hub','APPROVED','WH-03 Kanchipuram Heritage Reserve','2026-09-11 15:23:53.910986'),(8,'owner@sareekart.com',4,'2026-09-11 15:23:58.370632','Royal Banarasi Silk Saree',2,'Manager routine return transfer','manager@sareekart.com',5,'Approved via Owner Console','SK-ROYAL-BANARASI-SILK-SA-1','WH-03 Kanchipuram Heritage Reserve','APPROVED','WH-01 Bengaluru Central Fulfillment Hub','2026-09-16 10:26:58.907250');
/*!40000 ALTER TABLE `stock_transfers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trousseau_boards`
--

DROP TABLE IF EXISTS `trousseau_boards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trousseau_boards` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wedding_date` date DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `share_token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_public_voting` tinyint(1) NOT NULL DEFAULT '1',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `share_token` (`share_token`),
  KEY `idx_trousseau_user` (`user_id`),
  KEY `idx_trousseau_token` (`share_token`),
  CONSTRAINT `fk_trousseau_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1422 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trousseau_boards`
--

LOCK TABLES `trousseau_boards` WRITE;
/*!40000 ALTER TABLE `trousseau_boards` DISABLE KEYS */;
INSERT INTO `trousseau_boards` VALUES (627,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_J_bK5eZPyMYG0sJknxZdDhfR9K0hqvgW',1,'ACTIVE','2026-09-17 04:50:57','2026-09-17 04:50:57'),(628,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_plJUOMZgPGqEgYxZib7m4Kd2Pjws5T0p',1,'ACTIVE','2026-09-17 04:51:08','2026-09-17 04:51:08'),(629,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_5gttVexVhBK5GlEg2mBkFn-elYtqVqZY',1,'ACTIVE','2026-09-17 04:51:19','2026-09-17 04:51:19'),(630,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_9BSCQrOeFUFyQeN7IP3SHr3oKiea65xx',1,'ACTIVE','2026-09-17 04:51:32','2026-09-17 04:51:32'),(631,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_Zre_iN0ErPKoE1ufG82dZpemig3U9_Bn',1,'ACTIVE','2026-09-17 04:51:45','2026-09-17 04:51:45'),(632,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_fUYMD2zI7-R2i4dl48udwAZwfvW9qEyi',1,'ACTIVE','2026-09-17 04:52:26','2026-09-17 04:52:26'),(633,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_DHOcsXHAIXaT3rkDmKUVWlKrQjIUtQwr',1,'ACTIVE','2026-09-17 04:52:55','2026-09-17 04:52:55'),(740,9,'Kavya & Arjun Royal Wedding Trousseau','2026-11-20','South Indian handlooms with traditional temple zari','tkn_s0Pw-VckyNU_DESPd9lyqGcavQ3kngjy',1,'ACTIVE','2026-09-17 05:25:32','2026-09-17 05:25:32');
/*!40000 ALTER TABLE `trousseau_boards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trousseau_ceremonies`
--

DROP TABLE IF EXISTS `trousseau_ceremonies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trousseau_ceremonies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `ceremony_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `color_theme` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_budget` decimal(10,2) DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ceremony_board` (`board_id`),
  CONSTRAINT `fk_trousseau_ceremony_board` FOREIGN KEY (`board_id`) REFERENCES `trousseau_boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1776 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trousseau_ceremonies`
--

LOCK TABLES `trousseau_ceremonies` WRITE;
/*!40000 ALTER TABLE `trousseau_ceremonies` DISABLE KEYS */;
INSERT INTO `trousseau_ceremonies` VALUES (756,627,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:50:57'),(757,627,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:50:57'),(758,628,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:51:08'),(759,628,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:51:08'),(760,629,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:51:19'),(761,629,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:51:19'),(762,630,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:51:32'),(763,630,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:51:32'),(764,631,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:51:45'),(765,631,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:51:45'),(766,632,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:52:26'),(767,632,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:52:26'),(768,633,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 04:52:55'),(769,633,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 04:52:55'),(910,740,'MUHURTHAM','Auspicious Muhurtham','Crimson & Pure Gold Zari',75000.00,1,'2026-09-17 05:25:32'),(911,740,'ENGAGEMENT','Pastel Ring Ceremony','Pastel Blush Rose',45000.00,2,'2026-09-17 05:25:32');
/*!40000 ALTER TABLE `trousseau_ceremonies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trousseau_collaborators`
--

DROP TABLE IF EXISTS `trousseau_collaborators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trousseau_collaborators` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VOTER',
  `invite_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_collab_board` (`board_id`),
  CONSTRAINT `fk_trousseau_collab_board` FOREIGN KEY (`board_id`) REFERENCES `trousseau_boards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=378 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trousseau_collaborators`
--

LOCK TABLES `trousseau_collaborators` WRITE;
/*!40000 ALTER TABLE `trousseau_collaborators` DISABLE KEYS */;
/*!40000 ALTER TABLE `trousseau_collaborators` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trousseau_items`
--

DROP TABLE IF EXISTS `trousseau_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trousseau_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_id` bigint NOT NULL,
  `ceremony_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `added_by_user_id` bigint DEFAULT NULL,
  `is_ai_recommended` tinyint(1) NOT NULL DEFAULT '0',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SHORTLISTED',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_trousseau_item_added_by` (`added_by_user_id`),
  KEY `idx_item_ceremony` (`ceremony_id`),
  KEY `idx_item_product` (`product_id`),
  KEY `idx_item_board` (`board_id`),
  CONSTRAINT `fk_trousseau_item_added_by` FOREIGN KEY (`added_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_trousseau_item_board` FOREIGN KEY (`board_id`) REFERENCES `trousseau_boards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trousseau_item_ceremony` FOREIGN KEY (`ceremony_id`) REFERENCES `trousseau_ceremonies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trousseau_item_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1417 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trousseau_items`
--

LOCK TABLES `trousseau_items` WRITE;
/*!40000 ALTER TABLE `trousseau_items` DISABLE KEYS */;
INSERT INTO `trousseau_items` VALUES (573,627,756,23,9,0,'Pure Mulberry silk handwoven by master weaver','SHORTLISTED','2026-09-17 04:50:57','2026-09-17 04:50:57'),(574,628,758,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:51:09','2026-09-17 04:51:10'),(575,629,760,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:51:19','2026-09-17 04:51:21'),(576,630,762,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:51:32','2026-09-17 04:51:34'),(577,631,764,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:51:46','2026-09-17 04:51:47'),(578,632,766,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:52:26','2026-09-17 04:52:28'),(579,633,768,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 04:52:55','2026-09-17 04:52:57'),(696,740,910,23,9,0,'Pure Mulberry silk handwoven by master weaver','IN_CART','2026-09-17 05:25:33','2026-09-17 05:25:34');
/*!40000 ALTER TABLE `trousseau_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trousseau_votes`
--

DROP TABLE IF EXISTS `trousseau_votes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trousseau_votes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `voter_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `voter_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `reaction` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_trousseau_vote_user` (`user_id`),
  KEY `idx_vote_item` (`item_id`),
  CONSTRAINT `fk_trousseau_vote_item` FOREIGN KEY (`item_id`) REFERENCES `trousseau_items` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trousseau_vote_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=298 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trousseau_votes`
--

LOCK TABLES `trousseau_votes` WRITE;
/*!40000 ALTER TABLE `trousseau_votes` DISABLE KEYS */;
INSERT INTO `trousseau_votes` VALUES (136,574,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:51:09'),(137,574,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:51:10'),(138,575,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:51:20'),(139,575,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:51:20'),(140,576,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:51:33'),(141,576,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:51:33'),(142,577,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:51:46'),(143,577,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:51:47'),(144,578,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:52:27'),(145,578,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:52:27'),(146,579,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 04:52:56'),(147,579,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 04:52:56'),(168,696,'Amma (Radha)','+919876543210',NULL,'LOVE','The pure zari border is absolutely breathtaking!','2026-09-17 05:25:33'),(169,696,'Sunita Chachi','+919876543211',NULL,'LIKE','Elegant drape','2026-09-17 05:25:34');
/*!40000 ALTER TABLE `trousseau_votes` ENABLE KEYS */;
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
  `role` varchar(32) NOT NULL DEFAULT 'CUSTOMER',
  `updated_at` datetime(6) DEFAULT NULL,
  `recovery_key` varchar(255) DEFAULT NULL,
  `whatsapp_opt_in` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2687 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-07-01 09:31:11.481336','admin@sareekart.com','SareeKart','Admin','9876543210','$2a$10$tladQzHZFh.yWadD49Hwdu6R2/JlxLBjmBg9yq3o1K3xO9sAwUGju','ADMIN','2026-09-12 10:32:50.963723',NULL,_binary ''),(2,'2026-07-01 09:31:11.494397','customer@sareekart.com','Chaitanya','Customer','9876543211','$2a$10$zxFW0D25S60/Y8TBv6Fjbuv.cCiWH9iu4cC22JuuER.pMLSbscZoW','CUSTOMER','2026-09-06 10:58:23.836529',NULL,NULL),(3,'2026-08-15 10:56:22.222268','chaitanyaca183@gmail.com','chaitanya','avvaru','8985975765','$2a$10$z7ff.RY7unZ70Wgn7LgaeOXbuGAkaNkwdr8sUsNYkzv0DV2SnikTG','CUSTOMER','2026-08-15 10:56:22.222268',NULL,NULL),(4,'2026-09-04 12:48:49.911751','owner@sareekart.com','Super','Owner','9876543212','$2a$10$TEBOrWVb3QMWAmWf1/uRl.RZra/a1Qoq6rbCQQGAxM5Kv8XzlP3m2','OWNER','2026-09-04 12:48:49.911751',NULL,NULL),(5,'2026-09-04 12:48:49.996627','manager@sareekart.com','Store','Manager','9876543213','$2a$10$wTEQtWvLF7cTyH8P4AA08.UlWdWIFUsAYwSkJo2SrNJJ7mmJgrx0q','MANAGER','2026-09-04 12:48:49.996627',NULL,NULL),(6,'2026-09-04 14:57:53.972220','reset.tester.1788533873850@sareekart.com','Reset','Tester','9876543299','$2a$10$BXGFk24QGu5aEe8bgkqOhegam0Y0NJw6XhkoIFIrf6CSu7lKfmMzm','CUSTOMER','2026-09-04 14:57:57.040957',NULL,NULL),(7,'2026-09-04 15:22:15.745947','reset.tester.1788535335644@sareekart.com','Reset','Tester','9876543299','$2a$10$2CSdSW2j8SDQ5PFioXiqmOBbQdofwGZuu.vfT.p08QC8bxk5Yy9cW','CUSTOMER','2026-09-04 15:22:17.536194',NULL,NULL),(8,'2026-09-04 15:43:16.861673','priya@example.com','Priya','Sharma','9876543220','$2a$10$nWKIWX3dtPzFbx4HeaU6YeyoN2YXEeNgi7EPP35YSyW4kCACwQSf6','CUSTOMER','2026-09-04 15:43:16.861673',NULL,NULL),(9,'2026-09-04 15:43:16.941764','ananya@example.com','Ananya','Verma','9876543221','$2a$10$j55CoUt3a8NIKJhwcjW1IOdr/MVfCh4DlQblN3Ag7LmldC/Dg9acq','CUSTOMER','2026-09-04 15:43:16.941764',NULL,NULL),(10,'2026-09-05 11:47:44.868528','reset.tester.1788608864757@sareekart.com','Reset','Tester','9876543299','$2a$10$EkaT0wMqcva689tZfIJ9OOwv44FnHPFsqSZwgdkPE6bvKgXAaj5iW','CUSTOMER','2026-09-05 11:47:46.626798',NULL,NULL),(11,'2026-09-06 10:58:04.107628','reset.tester.1788692283920@sareekart.com','Reset','Tester','9876543299','$2a$10$fBgruESCK/CsN1T3PnPAXOIrNNXeZpj5iWDBmve1R3dXAXiLRb9Ge','CUSTOMER','2026-09-06 10:58:05.990222',NULL,NULL),(12,'2026-09-06 10:58:15.412980','reset.tester.1788692295240@sareekart.com','Reset','Tester','9876543299','$2a$10$fp5qvZKhunx68Akl4.uYf.PXaw6pXCGfYb8lti5BslBduu119iAoq','CUSTOMER','2026-09-06 10:58:17.324144',NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visual_search_queries`
--

DROP TABLE IF EXISTS `visual_search_queries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visual_search_queries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `confidence_score` decimal(5,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `execution_time_ms` int NOT NULL,
  `extracted_primary_color` varchar(50) NOT NULL,
  `extracted_secondary_color` varchar(50) DEFAULT NULL,
  `extracted_weave_type` varchar(100) DEFAULT NULL,
  `source` varchar(50) NOT NULL,
  `top_matched_product_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visual_search_queries`
--

LOCK TABLES `visual_search_queries` WRITE;
/*!40000 ALTER TABLE `visual_search_queries` DISABLE KEYS */;
INSERT INTO `visual_search_queries` VALUES (1,97.90,'2026-09-12 10:44:53.380976',14,'#B84F49','#D4AF37','Kanchipuram','CAMERA',14,NULL),(2,94.30,'2026-09-12 10:45:49.390820',164,'#1E1E1E','#D4AF37','Jamdani','SAMPLE_PREVIEW',5,NULL),(3,97.90,'2026-09-12 10:45:49.453834',235,'#B84F49','#D4AF37','Banarasi','SAMPLE_PREVIEW',1,NULL),(4,97.90,'2026-09-12 10:45:50.967454',29,'#B84F49','#D4AF37','Banarasi','SAMPLE_PREVIEW',1,NULL),(5,94.30,'2026-09-12 10:45:51.350429',11,'#1E1E1E','#D4AF37','Jamdani','SAMPLE_PREVIEW',5,NULL),(6,97.90,'2026-09-12 10:46:17.257937',5,'#B84F49','#D4AF37','Banarasi','SAMPLE_PREVIEW',1,NULL),(7,94.30,'2026-09-12 10:46:17.742090',5,'#1E1E1E','#D4AF37','Jamdani','SAMPLE_PREVIEW',5,NULL),(8,94.30,'2026-09-12 10:47:14.783792',10,'#1E1E1E','#D4AF37','Jamdani','SAMPLE_PREVIEW',5,NULL),(9,97.90,'2026-09-12 10:47:16.107240',5,'#B84F49','#D4AF37','Banarasi','SAMPLE_PREVIEW',1,NULL);
/*!40000 ALTER TABLE `visual_search_queries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_transactions`
--

DROP TABLE IF EXISTS `wallet_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `balance_after` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text NOT NULL,
  `points` int NOT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `type` enum('CREDIT_LOYALTY_EARNED','CREDIT_ORDER_CANCEL_REFUND','CREDIT_PROMO_BONUS','CREDIT_RETURN_REFUND','DEBIT_CHECKOUT_REDEMPTION') NOT NULL,
  `wallet_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wallet_tx_wallet` (`wallet_id`),
  KEY `idx_wallet_tx_type` (`type`),
  KEY `idx_wallet_tx_created` (`created_at`),
  CONSTRAINT `FK8seu7b87ifqi09ghhssusmb0x` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_transactions`
--

LOCK TABLES `wallet_transactions` WRITE;
/*!40000 ALTER TABLE `wallet_transactions` DISABLE KEYS */;
INSERT INTO `wallet_transactions` VALUES (1,100000.00,100000.00,'2026-09-12 10:41:35.095178','Promotional bonus: festival',0,4,'ADMIN_MANUAL','CREDIT_PROMO_BONUS',1);
/*!40000 ALTER TABLE `wallet_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallets`
--

DROP TABLE IF EXISTS `wallets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `balance` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `lifetime_spent` decimal(12,2) NOT NULL,
  `loyalty_points` int NOT NULL,
  `tier` enum('GOLD','ROYAL_PATRON','SILVER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wallets_user` (`user_id`),
  KEY `idx_wallets_user` (`user_id`),
  KEY `idx_wallets_tier` (`tier`),
  CONSTRAINT `FKc1foyisidw7wqqrkamafuwn4e` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallets`
--

LOCK TABLES `wallets` WRITE;
/*!40000 ALTER TABLE `wallets` DISABLE KEYS */;
INSERT INTO `wallets` VALUES (1,100000.00,'2026-09-12 10:16:51.644200',0.00,0,'SILVER','2026-09-12 10:41:35.102046',1),(2,0.00,'2026-09-12 10:34:17.137414',0.00,0,'SILVER','2026-09-12 10:34:17.137414',4),(3,0.00,'2026-09-12 15:57:30.405705',0.00,0,'SILVER','2026-09-12 15:57:30.405705',2);
/*!40000 ALTER TABLE `wallets` ENABLE KEYS */;
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
  `opted_in` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'WhatsApp messaging consent: 1=opted-in, 0=opted-out (STOP received)',
  `opt_in_updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmkn3wlni8surpx18remkyfr39` (`phone_number`),
  KEY `FKq9yy1jb9pitajvjmvce9ywbs1` (`user_id`),
  CONSTRAINT `FKq9yy1jb9pitajvjmvce9ywbs1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=453 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whatsapp_contacts`
--

LOCK TABLES `whatsapp_contacts` WRITE;
/*!40000 ALTER TABLE `whatsapp_contacts` DISABLE KEYS */;
INSERT INTO `whatsapp_contacts` VALUES (1,'2026-09-16 09:52:46.207712','Priya Sundaram','9876543210','2026-09-16 09:52:46.259632',1,1,NULL),(6,'2026-09-17 12:59:34.987098','Patron','9876540001','2026-09-17 12:59:34.987098',NULL,1,NULL),(64,'2026-09-17 14:47:39.062515','Patron','9876100001','2026-09-17 14:47:39.062515',NULL,1,NULL),(96,'2026-09-17 14:47:39.761095','Patron','9876543000','2026-09-17 14:47:39.761095',NULL,1,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whatsapp_messages`
--

LOCK TABLES `whatsapp_messages` WRITE;
/*!40000 ALTER TABLE `whatsapp_messages` DISABLE KEYS */;
INSERT INTO `whatsapp_messages` VALUES (1,'Namaste! Show me bridal silks under 40000','DELIVERED',NULL,'TEXT','CUSTOMER','2026-09-16 09:52:46.267503','wam_test_dedup_1789552365799',1);
/*!40000 ALTER TABLE `whatsapp_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `whatsapp_notification_logs`
--

DROP TABLE IF EXISTS `whatsapp_notification_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `whatsapp_notification_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `courier_partner` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `delivery_status` varchar(30) NOT NULL,
  `event_type` enum('DELIVERED','MANUAL_CONCIERGE','ORDER_CONFIRMED','OUT_FOR_DELIVERY','RETURN_PICKUP','SHIPPED') NOT NULL,
  `message_content` text NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `recipient_name` varchar(100) NOT NULL,
  `recipient_phone` varchar(30) NOT NULL,
  `return_request_id` bigint DEFAULT NULL,
  `simulated` bit(1) NOT NULL,
  `template_name` varchar(100) NOT NULL,
  `tracking_number` varchar(100) DEFAULT NULL,
  `tracking_url` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=169 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whatsapp_notification_logs`
--

LOCK TABLES `whatsapp_notification_logs` WRITE;
/*!40000 ALTER TABLE `whatsapp_notification_logs` DISABLE KEYS */;
INSERT INTO `whatsapp_notification_logs` VALUES (1,'Blue Dart Apex Air','2026-09-12 10:32:50.933472','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Sita Devi!*\n\nYour heirloom order *#1* has been confirmed at SareeKart.\nArtisan weavers are preparing your drape with the official *Silk Mark India* seal.\n\nTrack: https://sareekart.com/orders/1',1,'Sita Devi','+919876543210',NULL,_binary '\0','tpl_simulated_order_confirmed','BD-902148','https://www.bluedart.com/tracking?awb=BD-902148','2026-09-12 10:32:50.933472',2),(2,'Blue Dart Apex Air','2026-09-12 10:32:50.954794','DELIVERED','SHIPPED','📦 *Heirloom Saree Dispatched!*\n\n*Namaste Sita Devi*, order *#1* is en route via Blue Dart Apex Air.\nAWB: BD-902148\nTrack: https://www.bluedart.com/tracking?awb=BD-902148',1,'Sita Devi','+919876543210',NULL,_binary '\0','tpl_simulated_shipped','BD-902148','https://www.bluedart.com/tracking?awb=BD-902148','2026-09-12 10:32:50.954794',2),(3,NULL,'2026-09-12 15:57:28.651852','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#40* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Kanchipuram Temple Border Saree (x1)\n• Taranga Kanchi Silk Brocade Green Saree (x9)\n\n💰 *Total Amount:* ₹165,349.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/40',40,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/40','2026-09-12 15:57:28.651852',2),(4,NULL,'2026-09-12 15:58:16.221730','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#41* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n\n💰 *Total Amount:* ₹5,149.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/41',41,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/41','2026-09-12 15:58:16.221730',2),(5,NULL,'2026-09-12 15:59:46.847296','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#42* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n• Kanchipuram Temple Border Saree (x1)\n\n💰 *Total Amount:* ₹11,498.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/42',42,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/42','2026-09-12 15:59:46.847296',2),(6,NULL,'2026-09-12 16:02:54.646578','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#43* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n\n💰 *Total Amount:* ₹5,149.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/43',43,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/43','2026-09-12 16:02:54.646578',2),(7,NULL,'2026-09-12 16:02:57.257400','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#44* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Kanchipuram Temple Border Saree (x8)\n\n💰 *Total Amount:* ₹51,992.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/44',44,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/44','2026-09-12 16:02:57.257400',2),(8,NULL,'2026-09-12 16:03:11.941785','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#45* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Kanchipuram Temple Border Saree (x4)\n\n💰 *Total Amount:* ₹25,996.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/45',45,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/45','2026-09-12 16:03:11.941785',2),(9,NULL,'2026-09-12 16:03:12.422659','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#46* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n\n💰 *Total Amount:* ₹5,149.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/46',46,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/46','2026-09-12 16:03:12.422659',2),(10,NULL,'2026-09-12 16:04:05.706993','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#47* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n• Taranga Kanchi Silk Brocade Green Saree (x1)\n\n💰 *Total Amount:* ₹22,649.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/47',47,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/47','2026-09-12 16:04:05.706993',2),(11,NULL,'2026-09-12 16:04:06.918360','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#48* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Kanchipuram Temple Border Saree (x6)\n• Taranga Kanchi Silk Brocade Green Saree (x8)\n\n💰 *Total Amount:* ₹180,194.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/48',48,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/48','2026-09-12 16:04:06.918360',2),(12,NULL,'2026-09-15 07:18:50.874435','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#49* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n• Kanchipuram Temple Border Saree (x10)\n\n💰 *Total Amount:* ₹69,989.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/49',49,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/49','2026-09-15 07:18:50.874435',2),(13,NULL,'2026-09-15 07:19:46.868258','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste Chaitanya!*\n\nThank you for patronizing *SareeKart Handlooms*. Your bespoke order *#50* has been confirmed!\n\n✨ *Artisan Drape Summary:*\n• Royal Banarasi Silk Saree (x1)\n• Kanchipuram Temple Border Saree (x9)\n\n💰 *Total Amount:* ₹63,490.00\n📅 *Estimated Delivery:* Next Day Priority Delivery\n\nOur master weavers and curators are preparing your weave with the official *Silk Mark India* seal.\n\n🔗 *Track Order:* https://sareekart.com/orders/50',50,'Chaitanya Customer','9876543210',NULL,_binary '\0','tpl_order_confirmed_v1',NULL,'https://sareekart.com/orders/50','2026-09-15 07:19:46.868258',2),(14,'Blue Dart Apex Air','2026-09-16 10:45:42.958137','DELIVERED','ORDER_CONFIRMED','🙏 *Namaste geethesh!*\n\nYour heirloom order *#1001* has been confirmed at SareeKart.\nArtisan weavers are preparing your drape with the official *Silk Mark India* seal.\n\nTrack: https://sareekart.com/orders/1001',NULL,'geethesh','6301564830',NULL,_binary '\0','tpl_simulated_order_confirmed','BD-882194','https://www.bluedart.com/tracking?awb=BD-882194','2026-09-16 10:45:42.958137',5),(15,'Blue Dart Apex Air','2026-09-16 10:46:04.099928','DELIVERED','ORDER_CONFIRMED','your heirloom order *#1001* has been confirmed at SareeKart.\nArtisan weavers are preparing your drape with the official *Silk Mark India* seal.\n\nTrack: https://sareekart.com/orders/1001',NULL,'geethesh','6301564830',NULL,_binary '\0','tpl_simulated_order_confirmed','BD-882194','https://www.bluedart.com/tracking?awb=BD-882194','2026-09-16 10:46:04.099928',5);
/*!40000 ALTER TABLE `whatsapp_notification_logs` ENABLE KEYS */;
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
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi5s9pmqjw5sihfb0kkcakbk2y` (`user_id`,`product_id`),
  UNIQUE KEY `UKht6e6158srxsvjciahp1kjywf` (`user_id`,`product_id`),
  KEY `fk_wishlists_product` (`product_id`),
  KEY `idx_wishlists_user_created` (`user_id`,`created_at` DESC),
  CONSTRAINT `fk_wishlists_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wishlists_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlists`
--

LOCK TABLES `wishlists` WRITE;
/*!40000 ALTER TABLE `wishlists` DISABLE KEYS */;
/*!40000 ALTER TABLE `wishlists` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'sareekart_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-18 17:17:49
