CREATE TABLE IF NOT EXISTS `fraud_risk_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `risk_type` VARCHAR(50) NOT NULL,
  `score` INT NOT NULL,
  `detail` VARCHAR(255) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fraud_risk_logs_user_id` (`user_id`),
  CONSTRAINT `fk_fraud_risk_logs_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reporter_user_id` BIGINT UNSIGNED NOT NULL,
  `reported_user_id` BIGINT UNSIGNED NOT NULL,
  `reason_code` VARCHAR(50) NOT NULL,
  `detail` VARCHAR(500) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  `admin_note` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_reports_reported_status` (`reported_user_id`, `status`),
  KEY `idx_user_reports_status` (`status`),
  CONSTRAINT `fk_user_reports_reporter`
    FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_reports_reported`
    FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `fraud_risk_score` INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `fraud_review_status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
