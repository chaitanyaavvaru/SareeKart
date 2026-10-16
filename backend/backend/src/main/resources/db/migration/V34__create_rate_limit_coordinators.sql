-- ==============================================================================
-- SareeKart Migration V34: Create Rate Limit Coordinators Table (SEC-03)
-- Enables distributed, concurrency-safe rate limiting across multi-instance deployments
-- using database row locks on privacy-preserving coordinate keys.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS `rate_limit_coordinators` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `coordinate_key` varchar(191) NOT NULL,
  `failed_attempts` int NOT NULL DEFAULT 0,
  `first_failure_at` datetime(6) DEFAULT NULL,
  `last_failure_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rate_limit_coordinate_key` (`coordinate_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
