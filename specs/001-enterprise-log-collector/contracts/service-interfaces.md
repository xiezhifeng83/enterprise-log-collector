# Service Interfaces Contract

**Feature**: Enterprise Log Collection and Analysis System
**Branch**: `001-enterprise-log-collector`
**Date**: 2025-10-18

## Overview

This document defines the contracts between microservices in the distributed log collection system. All inter-service communication occurs via Kafka message queues using the schemas defined in `kafka-schemas.avro`.

## Service Architecture

```
┌──────────────────┐     log-raw-topic      ┌──────────────────┐
│  Log Collector   │───────────────────────▶│  Log Processor   │
│    Service       │                         │    Service       │
└──────────────────┘                         └────────┬─────────┘
                                                       │
                                                       │ log-processed-topic
                                                       ▼
                          ┌─────────────────────────────────────┐
                          │                                      │
                   ┌──────▼──────────┐           ┌──────────────▼───────┐
                   │  Transaction    │           │     Alert            │
                   │   Analyzer      │           │    Service           │
                   └──────┬──────────┘           └──────────┬───────────┘
                          │                                  │
                          │ transaction-event-topic          │ alert-topic
                          │                                  │
                          ▼                                  ▼
              ┌─────────────────────┐          ┌─────────────────────┐
              │   Cache/Metrics     │          │   Notification      │
              │     Services        │          │     Service         │
              └─────────────────────┘          └─────────────────────┘

┌──────────────────┐    archive-request-topic   ┌──────────────────┐
│     Archive      │◀───────────────────────────│     Archive      │
│     Service      │                             │    Scheduler     │
└──────────────────┘                             └──────────────────┘
```

## Service 1: Log Collector Service

**Responsibility**: Collect logs from configured Linux servers via SSH and publish to Kafka

**Kafka Topics**:
- **Produces**: `log-raw-topic` (RawLogMessage)
- **Consumes**: None

**Message Flow**:
1. Scheduled task (every N seconds per server configuration)
2. SSH connection to target server
3. Execute: `tail -n +{last_position} {log_file_path}`
4. For each log line:
   - Parse timestamp from content
   - Create RawLogMessage with correlationId
   - Publish to `log-raw-topic`
5. Update server's last collection position

**Contract Guarantees**:
- ✅ **At-least-once delivery**: Kafka producer configured with `acks=all`
- ✅ **Ordering**: Messages from same server maintain temporal order (same partition key = serverId)
- ✅ **Idempotence**: Duplicate lines handled by processor (dedupe by content hash)
- ✅ **Correlation**: Every message includes unique correlationId for distributed tracing

**Error Handling**:
- SSH connection failure → Retry with exponential backoff (max 3 retries)
- Kafka publish failure → Log error, mark server status=ERROR, retry next cycle
- Log file not found → Mark server status=ERROR, send alert

---

## Service 2: Log Processor Service

**Responsibility**: Parse raw logs, extract transaction IDs, persist to database, publish processed logs

**Kafka Topics**:
- **Consumes**: `log-raw-topic` (RawLogMessage)
- **Produces**: `log-processed-topic` (ProcessedLogMessage)

**Message Flow**:
1. Consume RawLogMessage from `log-raw-topic`
2. Parse content using configured regex patterns:
   - Extract transaction ID: `Pattern.compile("TXN_ID[:\\s]+([A-Z0-9]+)")`
   - Extract timestamp if not parseable from line format
   - Detect error indicators: `ERROR|EXCEPTION|FAILED` (case-insensitive)
3. Persist to `log_entries` table in MySQL
4. Index to Elasticsearch for full-text search
5. Create ProcessedLogMessage and publish to `log-processed-topic`

**Contract Guarantees**:
- ✅ **Exactly-once processing**: Kafka consumer with `enable.auto.commit=false`, manual commit after DB persist
- ✅ **Durability**: Database transaction wraps (MySQL insert + ES index + Kafka commit)
- ✅ **Correlation preservation**: correlationId carried forward from RawLogMessage
- ✅ **Schema evolution**: Avro schema compatible changes (additive fields with defaults)

**Error Handling**:
- Parse failure → Log as parsed=false, still persist raw content
- Database constraint violation → Skip (duplicate), log warning
- Elasticsearch indexing failure → Continue (ES is best-effort), alert on repeated failures

---

## Service 3: Transaction Analyzer

**Responsibility**: Link log entries into transactions, build flow graphs, detect status changes

**Kafka Topics**:
- **Consumes**: `log-processed-topic` (ProcessedLogMessage)
- **Produces**: `transaction-event-topic` (TransactionEventMessage)

**Message Flow**:
1. Consume ProcessedLogMessage (filter: transactionId != null)
2. Lookup transaction in Redis cache (key: `transaction:{transactionId}`)
3. If cache miss → Query MySQL `transactions` table
4. Update transaction state:
   - **First log with TXN_ID** → Create transaction (status=IN_PROGRESS)
   - **Log with "SUCCESS|COMPLETED"** → Update status=SUCCESS, set endTime
   - **Log with "ERROR|FAILED"** → Update status=FAILED, capture errorMessage
   - **Timeout detection** → If no update for >30s → status=TIMEOUT
5. Create TransactionFlowNode for each distinct system in log path
6. Publish TransactionEventMessage if status changed
7. Update Redis cache with 1-hour TTL

**Contract Guarantees**:
- ✅ **State consistency**: Transaction status transitions are append-only (events never deleted)
- ✅ **Flow completeness**: Nodes created for all log entries, even if transaction fails
- ✅ **Event ordering**: transaction-event-topic partitioned by transactionId ensures order
- ✅ **Cache coherence**: Redis cache invalidated on status change

**State Machine**:
```
NULL → IN_PROGRESS (first log)
IN_PROGRESS → SUCCESS (completion log)
IN_PROGRESS → FAILED (error log)
IN_PROGRESS → TIMEOUT (30s no activity)
FAILED → IN_PROGRESS (retry detected)
```

---

## Service 4: Alert Service

**Responsibility**: Evaluate alert rules, trigger notifications on condition match

**Kafka Topics**:
- **Consumes**: `log-processed-topic` (ProcessedLogMessage)
- **Produces**: `alert-topic` (AlertMessage)

**Message Flow**:
1. Consume ProcessedLogMessage
2. For each enabled AlertRule:
   - Fetch time-window data (e.g., last 5 minutes of errors)
   - Evaluate condition expression:
     - **ERROR_RATE**: `(error_count / total_count) > threshold`
     - **TRANSACTION_TIMEOUT**: `avg_duration > threshold_ms`
     - **PATTERN_MATCH**: `content LIKE pattern`
3. If condition met AND not recently alerted (throttling):
   - Create AlertInstance in database
   - Publish AlertMessage to `alert-topic`
4. If condition cleared AND alert unresolved:
   - Update AlertInstance (resolved=true, resolvedTime=now)
   - Publish resolution message to `alert-topic`

**Contract Guarantees**:
- ✅ **No duplicate alerts**: Throttling window prevents alert storms (configurable per rule)
- ✅ **Guaranteed delivery**: AlertMessage delivery confirmed before marking notification_status
- ✅ **Resolution tracking**: All triggered alerts eventually get resolution event (auto-resolve after 24h max)

**Throttling Rules**:
- Same rule + same condition → Only alert once per time window
- Use Redis key: `alert:throttle:{ruleId}:{conditionHash}` with TTL = timeWindowMinutes

---

## Service 5: Archive Service

**Responsibility**: Move old data from MySQL to MinIO, compress, maintain audit trail

**Kafka Topics**:
- **Consumes**: `archive-request-topic` (ArchiveRequestMessage)
- **Produces**: None

**Message Flow**:
1. Consume ArchiveRequestMessage
2. Query data older than `cutoffDays`:
   - SELECT from `transactions` WHERE `start_time` < DATE_SUB(NOW(), INTERVAL {cutoffDays} DAY)
   - SELECT from `log_entries` WHERE `original_timestamp` < cutoff
3. Export to JSON files:
   - Group by month: `transactions_YYYYMM.json`, `logs_YYYYMM.json`
4. Compress with GZIP (compression_ratio typically ~0.35)
5. Upload to MinIO bucket: `s3://archive/{year}/{month}/`
6. Verify upload integrity (checksum comparison)
7. Delete from MySQL (only after successful upload)
8. Update ArchiveJob record with status=COMPLETED

**Contract Guarantees**:
- ✅ **Data integrity**: No data deleted until MinIO upload verified
- ✅ **Atomic archival**: Transaction-wrapped DELETE (rollback on any failure)
- ✅ **Audit trail**: All archival operations logged to archive_jobs table
- ✅ **Idempotence**: Re-running archive for same date range is safe (skips already archived)

---

## Shared Contracts

### Correlation IDs

All services MUST:
- Propagate `correlationId` from consumed messages to produced messages
- Log correlationId in MDC (Mapped Diagnostic Context) for structured logging
- Include correlationId in error logs and exceptions

Format: UUID v4 (e.g., `550e8400-e29b-41d4-a716-446655440000`)

### Error Handling Strategy

| Error Type | Action | Retry | Alert |
|------------|--------|-------|-------|
| Transient (network timeout) | Retry with exponential backoff | Yes (max 3) | After 3 failures |
| Permanent (invalid data) | Dead letter queue | No | Immediate |
| Dependency unavailable (DB down) | Circuit breaker pattern | Yes (unlimited with backoff) | After 5 min |
| Kafka publish failure | Local buffer + retry | Yes (max 10) | After 10 failures |

### Monitoring Requirements

All services MUST expose:
- **Prometheus Metrics**:
  - `{service}_messages_consumed_total{topic}`
  - `{service}_messages_produced_total{topic}`
  - `{service}_processing_duration_seconds{topic, percentile}`
  - `{service}_errors_total{type}`

- **Health Checks** (`/actuator/health`):
  - Liveness: Service process running
  - Readiness: Dependencies available (Kafka, DB)

### Deployment Contract

Services MUST support:
- **Graceful shutdown**: Drain in-flight messages before terminating (max 30s)
- **Rolling updates**: Kafka consumer rebalancing without message loss
- **Horizontal scaling**: Multiple instances per service (consumer groups)
- **Resource limits**: Respect K8s resource requests/limits (2 CPU, 4GB RAM)

## Message Retention Policies

| Topic | Retention | Replication | Partitions |
|-------|-----------|-------------|------------|
| log-raw-topic | 7 days | 3 | 10 |
| log-processed-topic | 30 days | 3 | 5 |
| alert-topic | 3 days | 3 | 3 |
| transaction-event-topic | 90 days | 3 | 5 |
| archive-request-topic | 7 days | 3 | 1 |

## Schema Evolution Rules

When modifying Avro schemas:
1. ✅ **Allowed**: Add optional fields with defaults
2. ✅ **Allowed**: Add enum symbols (append only)
3. ❌ **Forbidden**: Remove fields
4. ❌ **Forbidden**: Change field types
5. ⚠️ **Requires migration**: Rename fields (publish with both old + new, deprecate old)

## Service Discovery

Services discover Kafka brokers via:
- **Development**: `spring.kafka.bootstrap-servers=localhost:9092`
- **Production**: Kubernetes Service DNS (`kafka-service.log-system.svc.cluster.local:9092`)

## Security

All Kafka communication MUST use:
- **TLS encryption** (in production)
- **SASL/SCRAM authentication** (username/password)
- **ACLs** (each service has READ/WRITE permissions only for its topics)
