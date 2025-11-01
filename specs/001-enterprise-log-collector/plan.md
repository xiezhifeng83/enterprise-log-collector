# Implementation Plan: Enterprise Log Collection and Analysis System

**Branch**: `001-enterprise-log-collector` | **Date**: 2025-10-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-enterprise-log-collector/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Build an enterprise-grade distributed log collection system that automatically gathers logs from multiple Linux servers via SSH, processes them through Kafka message queues, analyzes transaction flows across distributed systems, provides intelligent alerting on anomalies, and manages multi-tier data lifecycle (hot/warm/cold storage). The system enables operations teams to troubleshoot distributed transactions in under 30 seconds, supports 1000+ log lines/second throughput, and maintains 99.9% uptime with automated failover.

Technical approach: Microservices architecture using Spring Boot with Kafka for async message processing, multi-tier storage (MySQL/Redis/Elasticsearch/MinIO), OAuth2 authentication via Keycloak, Prometheus/Grafana monitoring, and Kubernetes deployment with Docker containers.

## Technical Context

**Language/Version**: Java 17 (LTS)
**Primary Dependencies**: Spring Boot 3.2+, Spring Cloud, Apache Kafka 3.x+, JSch (SSH client), Micrometer (metrics)
**Storage**: MySQL 8.0 (primary relational), Redis (cache), Elasticsearch (full-text search), MinIO (S3-compatible archival)
**Testing**: JUnit 5, Spring Boot Test, Mockito, Testcontainers (integration tests), JMH (performance benchmarks)
**Target Platform**: Linux servers (production), Docker containers, Kubernetes orchestration
**Project Type**: Distributed microservices (backend services + RESTful APIs)
**Performance Goals**: 1000 log lines/second throughput, <500ms p95 query latency, <5 second transaction trace retrieval
**Constraints**: <30 second end-to-end transaction flow analysis, 99.9% collection uptime, <5% alert false positive rate
**Scale/Scope**: Multiple Linux servers (10-100), millions of log entries per day, 365-day retention with archival

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Enterprise-Grade Architecture - COMPLIANT

**Requirement**: Distributed microservices architecture with proper service boundaries, Kafka messaging, multi-tier storage (MySQL/Redis/Elasticsearch/MinIO)

**Implementation**:
- **4 Independent Services**: Log Collector Service, Log Processor Service, Alert Service, Archive Service
- **Kafka Message Bus**: Services communicate asynchronously via Kafka topics (log-raw-topic, log-processed-topic, alert-topic)
- **Multi-Tier Storage**:
  - Hot: MySQL (transactions, metadata) + Redis (active transaction cache) - <30 days
  - Warm: Elasticsearch (searchable logs) - 30-90 days
  - Cold: MinIO (compressed archives) - >90 days
- **Independent Deployment**: Each service has its own Docker image, Kubernetes deployment, and can scale independently

**Rationale**: Aligns with constitution's microservices mandate. Collection service scales independently from processing service to handle high-volume log ingestion.

### ✅ II. Observability & Monitoring (NON-NEGOTIABLE) - COMPLIANT

**Requirement**: Prometheus metrics at `/actuator/metrics`, structured logging with correlation IDs, health checks at `/actuator/health`

**Implementation**:
- **Metrics Exposure**: Spring Boot Actuator enabled on all services exposing:
  - Business: `logs.collected.total`, `logs.processed.total`, `transactions.analyzed.total`, `alerts.triggered.total`
  - System: `jvm.memory.used`, `hikari.connections.active`, `kafka.consumer.lag`
  - Performance: `log.processing.latency` (p50/p95/p99), `transaction.analysis.duration`
- **Structured Logging**: Logback with pattern: `[%d{ISO8601}] [%X{correlationId}] [%X{service}] [%-5level] [%logger{36}] - %msg%n`
- **Health Checks**: `/actuator/health` with liveness (app running) and readiness (dependencies available) probes
- **Distributed Tracing**: Correlation IDs propagated through Kafka message headers

**Rationale**: Critical for troubleshooting distributed system. Correlation IDs enable tracing log collection → processing → alert flow.

### ✅ III. Test-First Development (NON-NEGOTIABLE) - COMPLIANT

**Requirement**: TDD with Red-Green-Refactor, unit/integration/contract/performance tests, >80% coverage

**Implementation**:
- **Test Structure**:
  - `tests/unit/` - Service logic, parsers (LogEntryParser, TransactionAnalyzer), validators
  - `tests/integration/` - Kafka flow (producer→consumer), database ops, SSH connections (Testcontainers)
  - `tests/contract/` - REST API endpoint contracts (OpenAPI validation), Kafka message schemas
  - `tests/performance/` - JMH benchmarks for log parsing throughput, transaction graph building
- **Test Data**: `scripts/test-data-generator.sh` creates mock SSH servers, sample log files with transaction IDs
- **Coverage**: JaCoCo plugin configured with 80% line coverage minimum

**Rationale**: Log collection handles audit data - test-first prevents data loss bugs. Contract tests ensure Kafka schema changes don't break consumer contracts.

### ✅ IV. Security & Authentication - COMPLIANT

**Requirement**: OAuth2/Keycloak for APIs, RBAC, AES-256 encryption for credentials, audit logging

**Implementation**:
- **Authentication**: Spring Security with OAuth2 Resource Server, Keycloak integration
- **RBAC Roles**:
  - ADMIN: Configure servers, manage alert rules, view audit logs
  - OPERATOR: View transactions, acknowledge alerts, trigger manual archival
  - VIEWER: Read-only access to logs and transactions
- **Credential Encryption**: SSH passwords, database credentials encrypted with AES-256, stored in environment variables (Docker secrets in production)
- **Audit Logging**: All API calls logged to separate audit table with user, timestamp, action, IP address
- **Input Validation**: Bean Validation (@Valid) on API parameters, log content sanitized before storage to prevent log injection

**Rationale**: Enterprise systems process sensitive transaction data. Keycloak provides SSO integration. Audit logs meet compliance (SOX, GDPR).

### ✅ V. Performance & Scalability - COMPLIANT

**Requirement**: 1000 log lines/sec, <500ms p95 query latency, JVM tuning (G1GC, 4-8GB heap), HikariCP connection pooling, Kafka/database optimization

**Implementation**:
- **JVM Configuration**:
  - `-Xms4g -Xmx4g` (fixed heap to avoid resizing)
  - `-XX:+UseG1GC -XX:MaxGCPauseMillis=200` (low-latency GC)
  - `-XX:G1HeapRegionSize=8M` (optimized for 4GB heap)
- **Kafka Optimization**:
  - Producer: `batch.size=32768`, `compression.type=lz4`, `linger.ms=50`
  - Consumer: Parallel processing with 10-partition topics, consumer group for each service
- **Database Optimization**:
  - HikariCP: `maximum-pool-size=30`, `minimum-idle=10`
  - Indexes: `(transaction_id)`, `(timestamp, server_name)`, `(status, start_time)`
  - Partitioning: `log_entries` table partitioned by month (RANGE on YEAR(timestamp)*100+MONTH(timestamp))
- **Redis Caching**: Active transactions cached with 1-hour TTL, reduces database queries by 70%

**Rationale**: Constitution mandates 1000 lines/sec. Kafka batching achieves throughput, partitioning prevents full table scans on billion-row log tables.

### ✅ VI. Operational Excellence - COMPLIANT

**Requirement**: Docker/K8s deployment, automated backups (MySQL 30-day, Kafka replication ≥3), CI/CD pipeline, runbooks

**Implementation**:
- **Containerization**: Multi-stage Dockerfile (Maven build → JRE runtime), images pushed to private registry
- **Kubernetes**: 3 replicas per service, HorizontalPodAutoscaler (CPU >70%), resource limits (2 CPU, 4GB RAM per pod)
- **Backup Strategy**:
  - MySQL: Daily automated backup via CronJob, stored locally + S3, 30-day retention
  - Kafka: Replication factor 3, min.insync.replicas=2
  - MinIO: Cross-region replication for archive data
- **CI/CD**: Jenkins pipeline: Maven build → JUnit tests → SonarQube → Docker build → K8s deployment → smoke tests
- **Runbooks**: `docs/runbooks/` covers SSH connection failures, Kafka lag remediation, database deadlock recovery

**Rationale**: K8s enables zero-downtime rolling updates. Automated backups prevent data loss. Runbooks reduce MTTR when SSH servers become unreachable.

### Gate Decision: ✅ **PASSED** - All constitution principles satisfied

No complexity violations to justify. Architecture aligns with microservices mandate, observability requirements met, test-first enforced, security measures in place, performance targets achievable with specified configuration, operational excellence practices defined.

## Project Structure

### Documentation (this feature)

```
specs/001-enterprise-log-collector/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── api-openapi.yaml        # REST API contract
│   ├── kafka-schemas.avro      # Kafka message schemas
│   └── service-interfaces.md   # Inter-service contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```
src/main/java/com/logcollector/
├── collector/              # Log Collector Service
│   ├── ssh/               # SSH connection management
│   ├── scheduler/         # Collection scheduling
│   └── producer/          # Kafka producer
│
├── processor/             # Log Processor Service
│   ├── consumer/          # Kafka consumer
│   ├── parser/            # Log parsing (regex, patterns)
│   └── analyzer/          # Transaction analysis & linking
│
├── alert/                 # Alert Service
│   ├── rules/             # Alert rule engine
│   ├── evaluator/         # Condition evaluation
│   └── notifier/          # Email/webhook delivery
│
├── archive/               # Archive Service
│   ├── lifecycle/         # Data retention policies
│   ├── compressor/        # GZIP compression
│   └── minio/             # MinIO client
│
├── api/                   # Shared REST API controllers
│   ├── controller/        # @RestController classes
│   ├── dto/               # Request/response DTOs
│   └── security/          # OAuth2/RBAC configuration
│
├── domain/                # Shared domain models
│   ├── model/             # JPA entities (Transaction, LogEntry, Alert, etc.)
│   ├── repository/        # Spring Data repositories
│   └── validator/         # Bean validation
│
└── config/                # Shared configuration
    ├── kafka/             # Kafka producer/consumer config
    ├── database/          # DataSource, HikariCP, partitioning
    ├── redis/             # RedisTemplate, cache config
    ├── metrics/           # Micrometer/Prometheus config
    └── security/          # Keycloak OAuth2 resource server

src/main/resources/
├── application.yml        # Base configuration
├── application-dev.yml    # Development overrides
├── application-prod.yml   # Production configuration
└── logback-spring.xml     # Logging configuration

tests/
├── unit/
│   ├── collector/         # SSH client, scheduler tests
│   ├── processor/         # Parser, analyzer tests
│   ├── alert/             # Rule engine tests
│   └── archive/           # Lifecycle policy tests
│
├── integration/
│   ├── kafka/             # Producer→Consumer flow (Testcontainers)
│   ├── database/          # JPA operations (Testcontainers MySQL)
│   └── ssh/               # Mock SSH server interactions
│
├── contract/
│   ├── api/               # OpenAPI contract validation
│   └── messaging/         # Kafka schema validation
│
└── performance/
    ├── parsing/           # JMH log parsing benchmarks
    └── graph/             # Transaction graph build benchmarks

scripts/
├── test-data-generator.sh   # Create sample logs with transaction IDs
├── mock-ssh-server.sh       # Launch mock SSH server for testing
└── load-test.sh             # Load testing with Gatling/JMeter

docker/
├── docker-compose.yml         # Local development stack (MySQL, Kafka, Redis, Keycloak)
├── docker-compose-prod.yml   # Production-like stack
└── Dockerfile                # Multi-stage app build

k8s/
├── deployments/
│   ├── collector-deployment.yaml
│   ├── processor-deployment.yaml
│   ├── alert-deployment.yaml
│   └── archive-deployment.yaml
├── services/
│   └── api-service.yaml
├── configmaps/
│   └── application-config.yaml
└── secrets/
    └── credentials-secret.yaml
```

**Structure Decision**:

This is a **distributed microservices backend** project. We use a monorepo structure with separate packages for each service (collector, processor, alert, archive) but shared domain models and configuration. This allows:

1. **Independent Service Scaling**: Each service has its own main class and can be packaged into separate Docker images
2. **Shared Code Reuse**: Common domain models (Transaction, LogEntry), Kafka configuration, security setup are shared
3. **Consistent Testing**: All services use the same test infrastructure (Testcontainers, JUnit)
4. **Simplified Development**: Single Maven project with module structure reduces build complexity compared to separate repos

The structure follows Spring Boot best practices with clear separation between:
- **Service layers** (collector, processor, alert, archive) - business logic
- **API layer** - REST controllers and DTOs
- **Domain layer** - JPA entities and repositories
- **Config layer** - Spring configuration classes

Tests are organized by type (unit/integration/contract/performance) rather than by service, enabling parallel test execution and clear test pyramid visualization.

## Complexity Tracking

*No violations - Constitution Check passed. This section intentionally left empty.*

