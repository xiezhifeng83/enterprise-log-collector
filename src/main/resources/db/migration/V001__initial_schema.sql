-- Enterprise Log Collection System - Initial Schema
-- Version: 1.0.0
-- Description: Creates all tables for log collection, transaction analysis, alerting, and archival

-- ============================================================================
-- Server Configuration
-- ============================================================================
CREATE TABLE server_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL UNIQUE,
    port INT NOT NULL DEFAULT 22,
    username VARCHAR(100) NOT NULL,
    encrypted_password VARCHAR(512) NOT NULL,
    log_file_paths JSON NOT NULL,
    collection_interval INT NOT NULL DEFAULT 30000,
    batch_size INT NOT NULL DEFAULT 500,
    status ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') NOT NULL DEFAULT 'DISCONNECTED',
    last_connected_at TIMESTAMP NULL,
    last_collection_at TIMESTAMP NULL,
    last_error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_collection (status, last_collection_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- User Accounts
-- ============================================================================
CREATE TABLE user_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    roles JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Transactions
-- ============================================================================
CREATE TABLE transactions (
    transaction_id VARCHAR(100) PRIMARY KEY,
    status ENUM('SUCCESS', 'FAILED', 'TIMEOUT', 'IN_PROGRESS') NOT NULL DEFAULT 'IN_PROGRESS',
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    duration_ms BIGINT NULL,
    source_system VARCHAR(100) NULL,
    target_system VARCHAR(100) NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_time (status, start_time),
    INDEX idx_start_time (start_time DESC),
    INDEX idx_source_system (source_system, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Log Entries
-- Note: Partitioning removed for dev environment to avoid MySQL limitations
-- Can be added back in production with DATETIME type and proper configuration
-- ============================================================================
CREATE TABLE log_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    log_path VARCHAR(500) NOT NULL,
    log_type VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    original_timestamp TIMESTAMP NOT NULL,
    collection_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_id VARCHAR(100) NULL,
    parsed BOOLEAN NOT NULL DEFAULT FALSE,
    error_indicator BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_timestamp_server (original_timestamp, server_id),
    INDEX idx_transaction (transaction_id),
    INDEX idx_parsed (parsed, collection_timestamp),
    FOREIGN KEY (server_id) REFERENCES server_configurations(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Transaction Flow Nodes
-- ============================================================================
CREATE TABLE transaction_flow_nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    step_name VARCHAR(200) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    duration_ms BIGINT NULL,
    status ENUM('STARTED', 'COMPLETED', 'FAILED') NOT NULL,
    sequence_order INT NOT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_transaction_sequence (transaction_id, sequence_order),
    INDEX idx_transaction_timestamp (transaction_id, timestamp),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    UNIQUE KEY uk_transaction_sequence (transaction_id, sequence_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Alert Rules
-- ============================================================================
CREATE TABLE alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    alert_type VARCHAR(100) NOT NULL,
    alert_condition TEXT NOT NULL,
    threshold DECIMAL(10,4) NULL,
    time_window_minutes INT NOT NULL DEFAULT 5,
    severity ENUM('CRITICAL', 'WARNING', 'INFO') NOT NULL,
    delivery_channels JSON NOT NULL,
    email_recipients JSON NULL,
    webhook_url VARCHAR(500) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled_type (enabled, alert_type),
    FOREIGN KEY (created_by) REFERENCES user_accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Alert Instances
-- ============================================================================
CREATE TABLE alert_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    transaction_id VARCHAR(100) NULL,
    severity ENUM('CRITICAL', 'WARNING', 'INFO') NOT NULL,
    title VARCHAR(300) NOT NULL,
    message TEXT NOT NULL,
    alert_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_time TIMESTAMP NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT NULL,
    acknowledged_at TIMESTAMP NULL,
    notification_status JSON NOT NULL,
    INDEX idx_alert_time (alert_time DESC),
    INDEX idx_resolved (resolved, alert_time),
    INDEX idx_severity (severity, resolved),
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    FOREIGN KEY (acknowledged_by) REFERENCES user_accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Archive Jobs
-- ============================================================================
CREATE TABLE archive_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    record_count BIGINT NOT NULL DEFAULT 0,
    transaction_count BIGINT NOT NULL DEFAULT 0,
    log_entry_count BIGINT NOT NULL DEFAULT 0,
    storage_location VARCHAR(500) NOT NULL,
    compression_ratio DECIMAL(5,2) NULL,
    status ENUM('RUNNING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'RUNNING',
    error_message TEXT NULL,
    completion_time TIMESTAMP NULL,
    INDEX idx_execution_time (execution_time DESC),
    INDEX idx_status (status, execution_time),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Seed Data - Default Admin User
-- ============================================================================
INSERT INTO user_accounts (username, email, roles, enabled) VALUES
('admin', 'admin@logcollector.com', '["ADMIN"]', TRUE),
('operator', 'operator@logcollector.com', '["OPERATOR"]', TRUE),
('viewer', 'viewer@logcollector.com', '["VIEWER"]', TRUE);

-- ============================================================================
-- Seed Data - Sample Alert Rules
-- ============================================================================
INSERT INTO alert_rules (name, alert_type, alert_condition, threshold, time_window_minutes, severity, delivery_channels, email_recipients, created_by) VALUES
('High Error Rate', 'ERROR_RATE', 'errorRate > 0.1', 0.1, 5, 'CRITICAL', '["email", "webhook"]', '["ops@logcollector.com"]', 1),
('Transaction Timeout', 'TRANSACTION_TIMEOUT', 'avgDuration > 30000', 30000, 10, 'WARNING', '["email"]', '["support@logcollector.com"]', 1),
('System Down', 'SYSTEM_DOWN', 'serverStatus = ERROR', NULL, 1, 'CRITICAL', '["email", "webhook"]', '["oncall@logcollector.com"]', 1);
