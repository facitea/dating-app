CREATE TABLE IF NOT EXISTS blocked_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identity_type VARCHAR(30) NOT NULL,
    identity_value VARCHAR(255) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    source_user_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_blocked_identities_source_user_id (source_user_id),
    UNIQUE KEY uk_blocked_identities_type_value_active (identity_type, identity_value, active)
);
