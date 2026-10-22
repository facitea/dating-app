USE dating_db;

CREATE TABLE IF NOT EXISTS `admin_review_histories` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `action_type` VARCHAR(30) NOT NULL,
  `detail` TEXT NOT NULL,
  `actor_label` VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_review_histories_user_id` (`user_id`),
  CONSTRAINT `fk_admin_review_histories_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;