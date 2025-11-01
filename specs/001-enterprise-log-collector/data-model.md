# Data Model: Enterprise Log Collection and Analysis System

**Feature**: Enterprise Log Collection and Analysis System
**Branch**: `001-enterprise-log-collector`
**Date**: 2025-10-18
**Source**: Derived from Key Entities in [spec.md](./spec.md)

## Overview

This document defines the domain entities for the log collection system. All entities are JPA-annotated classes stored primarily in MySQL, with selected caching in Redis and indexing in Elasticsearch.

## Entity Relationship Diagram

```
┌─────────────────────┐         ┌─────────────────────┐
│ ServerConfiguration │         │     UserAccount     │
│                     │         │                     │
│ - id (PK)           │         │ - id (PK)           │
│ - hostname          │         │ - username          │
│ - port              │         │ - email             │
│ - username          │         │ - roles[]           │
│ - encryptedPassword │         │ - createdAt         │
│ - logFilePaths[]    │         └─────────────────────┘
│ - collectionInterval│
│ - batchSize         │
│ - status            │
│ - lastConnectedAt   │
└─────────────────────┘
         │
         │ 1:N (one server → many log entries)
         ▼
┌─────────────────────┐         ┌─────────────────────┐
│      LogEntry       │────────▶│    Transaction      │
│                     │    N:1  │                     │
│ - id (PK)           │         │ - transactionId (PK)│
│ - serverId (FK)     │         │ - status            │
│ - fileName          │         │ - startTime         │
│ - logPath           │         │ - endTime           │
│ - logType           │         │ - durationMs        │
│ - content (TEXT)    │         │ - sourceSystem      │
│ - originalTimestamp │         │ - targetSystem      │
│ - collectionTimestamp│        │ - errorMessage      │
│ - transactionId (FK)│         │ - retryCount        │
│ - parsed (boolean)  │         └──────┬──────────────┘
└─────────────────────┘                │
                                       │ 1:N (one transaction → many flow nodes)
                                       ▼
                              ┌─────────────────────┐
                              │TransactionFlowNode  │
                              │                     │
                              │ - id (PK)           │
                              │ - transactionId (FK)│
                              │ - systemName        │
                              │ - stepName          │
                              │ - timestamp         │
                              │ - durationMs        │
                              │ - status            │
                              │ - sequenceOrder     │
                              └─────────────────────┘

┌─────────────────────┐         ┌─────────────────────┐
│     AlertRule       │────────▶│   AlertInstance     │
│                     │    1:N  │                     │
│ - id (PK)           │         │ - id (PK)           │
│ - name              │         │ - ruleId (FK)       │
│ - alertType         │         │ - transactionId (FK)│
│ - condition         │         │ - severity          │
│ - threshold         │         │ - title             │
│ - timeWindowMinutes │         │ - message           │
│ - severity          │         │ - alertTime         │
│ - deliveryChannels[]│         │ - resolvedTime      │
│ - enabled           │         │ - resolved          │
│ - createdBy         │         │ - acknowledged      │
└─────────────────────┘         │ - acknowledgedBy    │
                                │ - notificationStatus│
                                └─────────────────────┘

┌─────────────────────┐
│     ArchiveJob      │
│                     │
│ - id (PK)           │
│ - executionTime     │
│ - startDate         │
│ - endDate           │
│ - recordCount       │
│ - transactionCount  │
│ - logEntryCount     │
│ - storageLocation   │
│ - compressionRatio  │
│ - status            │
│ - errorMessage      │
└─────────────────────┘
```

## Core Entities

### 1. ServerConfiguration

**Purpose**: Represents a remote Linux server from which logs are collected.

**Table**: `server_configurations`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| hostname | VARCHAR(255) | NOT NULL, UNIQUE | Server hostname or IP address |
| port | INT | NOT NULL, DEFAULT 22 | SSH port number |
| username | VARCHAR(100) | NOT NULL | SSH username |
| encryptedPassword | VARCHAR(512) | NOT NULL | AES-256 encrypted SSH password |
| logFilePaths | JSON | NOT NULL | Array of log file paths to collect (e.g., ["/var/log/app.log", "/var/log/ledger.log"]) |
| collectionInterval | INT | NOT NULL, DEFAULT 30000 | Collection interval in milliseconds |
| batchSize | INT | NOT NULL, DEFAULT 500 | Number of log lines to collect per batch |
| status | ENUM | NOT NULL | CONNECTED, DISCONNECTED, ERROR |
| lastConnectedAt | TIMESTAMP | NULL | Last successful SSH connection timestamp |
| lastCollectionAt | TIMESTAMP | NULL | Last successful log collection timestamp |
| lastErrorMessage | TEXT | NULL | Most recent error message if status=ERROR |
| createdAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | Last modification timestamp |

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX (hostname)
- INDEX (status, lastCollectionAt) - for finding stale servers

**Validation Rules**:
- hostname must be valid IP or domain name
- port must be 1-65535
- collectionInterval >= 10000 (minimum 10 seconds)
- batchSize >= 100, <= 10000

**State Transitions**:
```
DISCONNECTED → CONNECTED (on successful SSH connection)
CONNECTED → ERROR (on SSH failure, network timeout)
ERROR → CONNECTED (on retry success)
```

---

### 2. LogEntry

**Purpose**: Individual log line collected from a server.

**Table**: `log_entries`

**Partitioning**: Monthly partitions on `originalTimestamp` using RANGE partitioning:
```sql
PARTITION BY RANGE (YEAR(originalTimestamp) * 100 + MONTH(originalTimestamp)) (
  PARTITION p202510 VALUES LESS THAN (202511),
  PARTITION p202511 VALUES LESS THAN (202512),
  ...
)
```

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| serverId | BIGINT | FK (server_configurations.id), NOT NULL | Source server reference |
| fileName | VARCHAR(255) | NOT NULL | Log file name (e.g., "64701.log") |
| logPath | VARCHAR(500) | NOT NULL | Full log file path |
| logType | VARCHAR(100) | NOT NULL | Log type (MAIN_APP, LEDGER, GATEWAY, etc.) |
| content | TEXT | NOT NULL | Raw log line content |
| originalTimestamp | TIMESTAMP | NOT NULL | Timestamp extracted from log line |
| collectionTimestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When this line was collected |
| transactionId | VARCHAR(100) | NULL, FK (transactions.transaction_id) | Extracted transaction ID (nullable - not all logs have transactions) |
| parsed | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether this log line has been parsed/processed |
| errorIndicator | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether log contains ERROR/EXCEPTION keywords |

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (serverId) REFERENCES server_configurations(id)
- FOREIGN KEY (transactionId) REFERENCES transactions(transaction_id)
- INDEX (originalTimestamp, serverId) - time-range queries per server
- INDEX (transactionId) - transaction flow lookup
- INDEX (parsed, collectionTimestamp) - processing backlog
- FULLTEXT INDEX (content) - full-text search

**Elasticsearch Mapping**: Log entries are also indexed in Elasticsearch for full-text search:
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "serverId": { "type": "long" },
      "fileName": { "type": "keyword" },
      "logType": { "type": "keyword" },
      "content": { "type": "text", "analyzer": "standard" },
      "originalTimestamp": { "type": "date" },
      "transactionId": { "type": "keyword" },
      "errorIndicator": { "type": "boolean" }
    }
  }
}
```

**Lifecycle**: Logs older than 30 days are removed from MySQL (moved to MinIO archive), but remain in Elasticsearch for 90 days.

---

### 3. Transaction

**Purpose**: Business transaction spanning multiple log entries and servers.

**Table**: `transactions`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| transactionId | VARCHAR(100) | PK | Transaction identifier (e.g., "TXN20251018001") |
| status | ENUM | NOT NULL | SUCCESS, FAILED, TIMEOUT, IN_PROGRESS |
| startTime | TIMESTAMP | NOT NULL | First log entry timestamp for this transaction |
| endTime | TIMESTAMP | NULL | Last log entry timestamp (NULL if IN_PROGRESS) |
| durationMs | BIGINT | NULL | Duration in milliseconds (endTime - startTime) |
| sourceSystem | VARCHAR(100) | NULL | Originating system (e.g., "APP_SERVER") |
| targetSystem | VARCHAR(100) | NULL | Destination system (e.g., "GATEWAY") |
| errorMessage | TEXT | NULL | Error details if status=FAILED |
| retryCount | INT | NOT NULL, DEFAULT 0 | Number of retry attempts |
| createdAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | First appearance in system |
| updatedAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | Last modification |

**Indexes**:
- PRIMARY KEY (transactionId)
- INDEX (status, startTime) - status-filtered time-range queries
- INDEX (startTime DESC) - recent transactions
- INDEX (sourceSystem, startTime) - system-specific queries

**Redis Caching**: Active transactions (IN_PROGRESS or updated within 1 hour) are cached in Redis:
- Key: `transaction:{transactionId}`
- TTL: 3600 seconds (1 hour)
- Value: JSON serialized transaction object

**State Transitions**:
```
NULL → IN_PROGRESS (on first log entry with transaction ID)
IN_PROGRESS → SUCCESS (on successful completion log)
IN_PROGRESS → FAILED (on error log)
IN_PROGRESS → TIMEOUT (if no update for >30 seconds)
FAILED → IN_PROGRESS (on retry)
```

---

### 4. TransactionFlowNode

**Purpose**: Single step in a transaction's journey across systems.

**Table**: `transaction_flow_nodes`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| transactionId | VARCHAR(100) | FK (transactions.transaction_id), NOT NULL | Parent transaction |
| systemName | VARCHAR(100) | NOT NULL | System name (e.g., "APP_SERVER_MAIN_APP") |
| stepName | VARCHAR(200) | NOT NULL | Step description (e.g., "Payment Processing") |
| timestamp | TIMESTAMP | NOT NULL | When this step occurred |
| durationMs | BIGINT | NULL | Time spent in this step (if measurable) |
| status | ENUM | NOT NULL | STARTED, COMPLETED, FAILED |
| sequenceOrder | INT | NOT NULL | Order in flow (1, 2, 3...) |
| metadata | JSON | NULL | Additional step-specific data |
| createdAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (transactionId) REFERENCES transactions(transaction_id) ON DELETE CASCADE
- INDEX (transactionId, sequenceOrder) - ordered flow retrieval
- INDEX (transactionId, timestamp) - chronological flow

**Validation Rules**:
- sequenceOrder must be unique within a transaction
- status=COMPLETED requires durationMs to be set

---

### 5. AlertRule

**Purpose**: Configuration for an alert condition.

**Table**: `alert_rules`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| name | VARCHAR(200) | NOT NULL, UNIQUE | Rule name (e.g., "High Error Rate Alert") |
| alertType | VARCHAR(100) | NOT NULL | ERROR_RATE, TRANSACTION_TIMEOUT, SYSTEM_DOWN, PATTERN_MATCH |
| condition | TEXT | NOT NULL | Condition expression (e.g., "errorRate > 0.1") |
| threshold | DECIMAL(10,4) | NULL | Numeric threshold value |
| timeWindowMinutes | INT | NOT NULL, DEFAULT 5 | Time window for evaluation (minutes) |
| severity | ENUM | NOT NULL | CRITICAL, WARNING, INFO |
| deliveryChannels | JSON | NOT NULL | Array of channels (["email", "webhook"]) |
| emailRecipients | JSON | NULL | Array of email addresses |
| webhookUrl | VARCHAR(500) | NULL | Webhook endpoint URL |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether rule is active |
| createdBy | BIGINT | FK (user_accounts.id), NOT NULL | User who created rule |
| createdAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updatedAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | Last modification |

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX (name)
- INDEX (enabled, alertType) - active rules by type
- FOREIGN KEY (createdBy) REFERENCES user_accounts(id)

**Validation Rules**:
- deliveryChannels must contain at least one channel
- If "email" in deliveryChannels, emailRecipients required
- If "webhook" in deliveryChannels, webhookUrl required
- timeWindowMinutes must be 1-60

---

### 6. AlertInstance

**Purpose**: Specific occurrence of an alert being triggered.

**Table**: `alert_instances`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| ruleId | BIGINT | FK (alert_rules.id), NOT NULL | Triggering rule |
| transactionId | VARCHAR(100) | FK (transactions.transaction_id), NULL | Related transaction (if applicable) |
| severity | ENUM | NOT NULL | CRITICAL, WARNING, INFO (copied from rule) |
| title | VARCHAR(300) | NOT NULL | Alert title |
| message | TEXT | NOT NULL | Detailed alert message |
| alertTime | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When alert triggered |
| resolvedTime | TIMESTAMP | NULL | When condition cleared |
| resolved | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether alert has been resolved |
| acknowledged | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether operator acknowledged |
| acknowledgedBy | BIGINT | FK (user_accounts.id), NULL | User who acknowledged |
| acknowledgedAt | TIMESTAMP | NULL | Acknowledgment timestamp |
| notificationStatus | JSON | NOT NULL | Delivery status per channel ({"email": "sent", "webhook": "failed"}) |

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (ruleId) REFERENCES alert_rules(id)
- FOREIGN KEY (transactionId) REFERENCES transactions(transaction_id)
- FOREIGN KEY (acknowledgedBy) REFERENCES user_accounts(id)
- INDEX (alertTime DESC) - recent alerts
- INDEX (resolved, alertTime) - active alerts
- INDEX (severity, resolved) - critical unresolved alerts

---

### 7. ArchiveJob

**Purpose**: Record of a data archival operation.

**Table**: `archive_jobs`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| executionTime | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Job start time |
| startDate | DATE | NOT NULL | Beginning of archived date range |
| endDate | DATE | NOT NULL | End of archived date range |
| recordCount | BIGINT | NOT NULL | Total records archived |
| transactionCount | BIGINT | NOT NULL | Transactions archived |
| logEntryCount | BIGINT | NOT NULL | Log entries archived |
| storageLocation | VARCHAR(500) | NOT NULL | MinIO bucket/path (e.g., "s3://archive/2025-10/") |
| compressionRatio | DECIMAL(5,2) | NULL | Compression ratio achieved (e.g., 0.35 = 65% reduction) |
| status | ENUM | NOT NULL | RUNNING, COMPLETED, FAILED |
| errorMessage | TEXT | NULL | Error details if status=FAILED |
| completionTime | TIMESTAMP | NULL | Job end time |

**Indexes**:
- PRIMARY KEY (id)
- INDEX (executionTime DESC) - recent jobs
- INDEX (status, executionTime) - active jobs
- INDEX (startDate, endDate) - date range coverage

---

### 8. UserAccount

**Purpose**: System user for authentication and authorization.

**Table**: `user_accounts`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique identifier |
| username | VARCHAR(100) | NOT NULL, UNIQUE | Login username |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email address |
| roles | JSON | NOT NULL | Array of roles (["ADMIN", "OPERATOR"]) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Account active status |
| createdAt | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| lastLoginAt | TIMESTAMP | NULL | Last successful login |

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX (username)
- UNIQUE INDEX (email)
- INDEX (enabled) - active users

**Note**: Passwords are managed by Keycloak (OAuth2 provider), not stored in this table. This table is synchronized from Keycloak for audit logging and user attribution.

**Roles**:
- **ADMIN**: Full access (configure servers, manage alerts, view audits)
- **OPERATOR**: Operational access (view transactions, acknowledge alerts, trigger archival)
- **VIEWER**: Read-only access (view logs and transactions)

---

## Database Schema Creation

### MySQL DDL

```sql
-- Server configurations
CREATE TABLE server_configurations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  hostname VARCHAR(255) NOT NULL UNIQUE,
  port INT NOT NULL DEFAULT 22,
  username VARCHAR(100) NOT NULL,
  encrypted_password VARCHAR(512) NOT NULL,
  log_file_paths JSON NOT NULL,
  collection_interval INT NOT NULL DEFAULT 30000,
  batch_size INT NOT NULL DEFAULT 500,
  status ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') NOT NULL,
  last_connected_at TIMESTAMP NULL,
  last_collection_at TIMESTAMP NULL,
  last_error_message TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status_collection (status, last_collection_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Log entries (partitioned by month)
CREATE TABLE log_entries (
  id BIGINT AUTO_INCREMENT,
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
  PRIMARY KEY (id, original_timestamp),
  INDEX idx_timestamp_server (original_timestamp, server_id),
  INDEX idx_transaction (transaction_id),
  INDEX idx_parsed (parsed, collection_timestamp),
  FULLTEXT INDEX idx_content (content),
  FOREIGN KEY (server_id) REFERENCES server_configurations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(original_timestamp) * 100 + MONTH(original_timestamp)) (
  PARTITION p202510 VALUES LESS THAN (202511),
  PARTITION p202511 VALUES LESS THAN (202512),
  PARTITION p202512 VALUES LESS THAN (202601),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- Transactions
CREATE TABLE transactions (
  transaction_id VARCHAR(100) PRIMARY KEY,
  status ENUM('SUCCESS', 'FAILED', 'TIMEOUT', 'IN_PROGRESS') NOT NULL,
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

-- Transaction flow nodes
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
  FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User accounts
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

-- Alert rules
CREATE TABLE alert_rules (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL UNIQUE,
  alert_type VARCHAR(100) NOT NULL,
  condition TEXT NOT NULL,
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
  FOREIGN KEY (created_by) REFERENCES user_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alert instances
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
  FOREIGN KEY (rule_id) REFERENCES alert_rules(id),
  FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
  FOREIGN KEY (acknowledged_by) REFERENCES user_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Archive jobs
CREATE TABLE archive_jobs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  execution_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  record_count BIGINT NOT NULL,
  transaction_count BIGINT NOT NULL,
  log_entry_count BIGINT NOT NULL,
  storage_location VARCHAR(500) NOT NULL,
  compression_ratio DECIMAL(5,2) NULL,
  status ENUM('RUNNING', 'COMPLETED', 'FAILED') NOT NULL,
  error_message TEXT NULL,
  completion_time TIMESTAMP NULL,
  INDEX idx_execution_time (execution_time DESC),
  INDEX idx_status (status, execution_time),
  INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Data Lifecycle

### Hot Data (MySQL + Redis)
- **Transactions**: IN_PROGRESS + last 30 days
- **Log Entries**: Last 30 days
- **Redis Cache**: Active transactions (1-hour TTL)

### Warm Data (Elasticsearch)
- **Log Entries**: 30-90 days (searchable)

### Cold Data (MinIO)
- **Archived Data**: >90 days (compressed JSON/Parquet)
- **Archive Format**: `{year}/{month}/transactions_{date}.json.gz`

## Migration Strategy

1. **Initial Schema**: Run DDL scripts to create all tables
2. **Add Foreign Keys**: After table creation to avoid constraint errors
3. **Create Partitions**: Add monthly partitions for log_entries (automated via scheduled job)
4. **Seed Data**: Insert default admin user, sample alert rules

## Performance Considerations

- **Partitioning**: log_entries table grows to billions of rows - monthly partitions enable partition pruning
- **Caching**: Active transactions cached in Redis to avoid MySQL joins on transaction → log_entry lookups
- **Indexes**: Composite indexes on (status, timestamp) support common query patterns
- **Archival**: Data older than 30 days moved to MinIO, reducing primary storage by 70%
