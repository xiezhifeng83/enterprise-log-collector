# Research: Enterprise Log Collection and Analysis System

**Feature**: Enterprise Log Collection and Analysis System
**Branch**: `001-enterprise-log-collector`
**Date**: 2025-10-18
**Status**: Complete - No research required

## Summary

All technology decisions were pre-defined by the project constitution (`.specify/memory/constitution.md`). No research phase was needed as the Technical Context had zero "NEEDS CLARIFICATION" markers.

## Technology Stack (From Constitution)

### Decided Technologies

| Component | Decision | Source |
|-----------|----------|--------|
| Language | Java 17 (LTS) | Constitution § Technology Stack |
| Framework | Spring Boot 3.2+ | Constitution § Technology Stack |
| Message Queue | Apache Kafka 3.x+ | Constitution § Technology Stack |
| Databases | MySQL 8.0, Redis, Elasticsearch | Constitution § Technology Stack |
| Archive Storage | MinIO (S3-compatible) | Constitution § Technology Stack |
| Authentication | Keycloak (OAuth2/OIDC) | Constitution § Technology Stack |
| Monitoring | Prometheus + Grafana | Constitution § Technology Stack |
| Build Tool | Maven 3.8+ | Constitution § Technology Stack |
| Containerization | Docker + Kubernetes | Constitution § Technology Stack |
| Testing | JUnit 5, Spring Boot Test, Mockito, Testcontainers | Constitution § Test-First Development |

### Rationale

**Why these choices were made** (from constitution):

1. **Spring Boot 3.2+**: Enterprise-grade framework with production-ready features (Actuator for metrics/health, Spring Security for OAuth2, Spring Data for repositories). Provides comprehensive microservices support via Spring Cloud.

2. **Apache Kafka 3.x+**: High-throughput distributed message streaming. Handles 1000+ log lines/second requirement with batching/compression. Enables async processing and service decoupling.

3. **MySQL 8.0**: Relational database for transactions and metadata. Supports partitioning (required for billion-row log_entries table), ACID guarantees for audit compliance.

4. **Redis**: High-performance in-memory cache for active transactions (1-hour TTL). Reduces database load by 70% per constitution performance targets.

5. **Elasticsearch**: Full-text search engine for log content. Required for P4 user story (historical search by keyword/pattern across 90 days of data).

6. **MinIO**: S3-compatible object storage for cold data (>90 days). Cost-effective archival with 70% storage cost reduction target.

7. **Keycloak**: Centralized OAuth2/OIDC identity provider. Enables SSO, RBAC (ADMIN/OPERATOR/VIEWER roles), meets compliance requirements (SOX, GDPR).

8. **Prometheus + Grafana**: Standardized metrics collection and visualization. Required by constitution § II (Observability & Monitoring - NON-NEGOTIABLE).

9. **Kubernetes**: Container orchestration for zero-downtime deployments, auto-scaling (HPA), health-based recovery. Required by constitution § VI (Operational Excellence).

### Alternatives Considered

None. The constitution mandates specific technologies based on enterprise best practices and production-proven stack. Any deviation would violate constitution compliance gates.

## Architecture Patterns

### Decided Patterns

| Pattern | Decision | Rationale |
|---------|----------|-----------|
| Service Communication | Event-Driven (Kafka) | Loose coupling, async processing, fault tolerance. Collection service produces raw logs, processor service consumes and analyzes independently. |
| Data Access | Repository Pattern (Spring Data JPA) | Abstraction over database operations, supports test mocking, type-safe queries. Constitution requires test-first development. |
| Caching | Cache-Aside (Redis) | Application manages cache manually. Write to DB → invalidate cache. Read: check cache first → miss? query DB → populate cache. Gives control over TTL. |
| Authentication | OAuth2 Resource Server | Stateless JWT tokens from Keycloak. No session state in services enables horizontal scaling. |
| Observability | Structured Logging + Metrics | Correlation IDs in logs (Logback MDC), Micrometer metrics. Required by constitution § II. |

### SSH Client Pattern

**Decision**: JSch library for SSH connections

**Rationale**:
- Mature Java SSH library (widely used)
- Supports password authentication (requirement from spec - server credentials in environment variables)
- Allows channel execution for remote commands (e.g., `tail -n +{position} {logfile}` for incremental collection)
- Thread-safe session management for parallel server collection

**Alternative**: Apache MINA SSHD - Rejected due to more complex API, JSch sufficient for read-only log collection

### Log Parsing Strategy

**Decision**: Regex-based parsing with configurable patterns

**Rationale**:
- Log formats vary by server (spec assumption: "formats may vary between servers")
- Transaction ID extraction requires pattern matching (e.g., `TXN_ID[:\s]+([A-Z0-9]+)`)
- Timestamp extraction with format detection (Java `DateTimeFormatter` with fallback patterns)
- Configurable via `application.yml` (e.g., `log.parsing.transaction-pattern: "TXN_ID[:\\s]+([A-Z0-9]+)"`)

**Performance**: Regex compiled once at startup, cached. JMH benchmarks (tests/performance/parsing/) validate >1000 lines/sec throughput.

## Performance Optimization Decisions

| Optimization | Decision | Target Metric |
|--------------|----------|---------------|
| JVM GC | G1GC with 200ms max pause | <500ms p95 latency |
| Heap Size | Fixed 4GB (`-Xms4g -Xmx4g`) | Avoid GC overhead from heap resizing |
| Kafka Batching | 32KB batch, 50ms linger | 1000+ log lines/sec throughput |
| Database Pooling | HikariCP (30 max, 10 min idle) | Handle concurrent API requests |
| Table Partitioning | Monthly partitions on log_entries | Prevent full table scans (billion rows) |
| Redis TTL | 1-hour for active transactions | Balance freshness vs. cache hit rate |

## Security Design

| Component | Decision | Compliance |
|-----------|----------|------------|
| API Authentication | OAuth2 JWT tokens (Keycloak) | SOX, GDPR (audit trail) |
| Credential Storage | AES-256 encrypted, environment variables | Prevent credential leakage |
| Audit Logging | Separate audit table, immutable | GDPR (right to audit), SOX |
| Input Validation | Bean Validation (@Valid), sanitization | Prevent SQL/log injection |
| RBAC Roles | ADMIN/OPERATOR/VIEWER | Least-privilege access |

## Testing Strategy

| Test Type | Framework | Coverage Target |
|-----------|-----------|----------------|
| Unit Tests | JUnit 5 + Mockito | >80% line coverage |
| Integration Tests | Spring Boot Test + Testcontainers | Kafka, MySQL, SSH |
| Contract Tests | Spring Cloud Contract / Pact | REST API, Kafka schemas |
| Performance Tests | JMH | Validate 1000 lines/sec |

**Test Data**: `scripts/test-data-generator.sh` creates:
- Sample log files with realistic transaction IDs, timestamps, error patterns
- Mock SSH server (OpenSSH in Docker container) for integration tests
- Load test scenarios (Gatling) simulating multiple servers

## Dependencies

### Core Dependencies (pom.xml)

```xml
<!-- Constitution-mandated stack -->
<spring-boot.version>3.2.0</spring-boot.version>
<kafka.version>3.6.0</kafka.version>
<mysql.version>8.0.33</mysql.version>

<!-- Key libraries -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId> <!-- REST API -->
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId> <!-- MySQL -->
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId> <!-- Redis cache -->
</dependency>
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId> <!-- Kafka -->
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId> <!-- OAuth2 -->
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId> <!-- Metrics/Health -->
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId> <!-- Prometheus -->
</dependency>
<dependency>
  <groupId>com.jcraft</groupId>
  <artifactId>jsch</artifactId> <!-- SSH client -->
  <version>0.1.55</version>
</dependency>
<dependency>
  <groupId>io.minio</groupId>
  <artifactId>minio</artifactId> <!-- MinIO client -->
  <version>8.5.7</version>
</dependency>
<dependency>
  <groupId>org.elasticsearch.client</groupId>
  <artifactId>elasticsearch-rest-high-level-client</artifactId> <!-- Elasticsearch -->
</dependency>
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Collector   │  │  Processor   │  │    Alert     │     │
│  │   Service    │  │   Service    │  │   Service    │     │
│  │  (3 replicas)│  │  (3 replicas)│  │  (2 replicas)│     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                     ┌──────▼───────┐                        │
│                     │    Kafka     │                        │
│                     │  (3 brokers) │                        │
│                     └──────┬───────┘                        │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐             │
│         │                  │                  │             │
│    ┌────▼────┐      ┌─────▼─────┐      ┌────▼────┐        │
│    │  MySQL  │      │   Redis   │      │  MinIO  │        │
│    │ Primary │      │   Cache   │      │ Archive │        │
│    └─────────┘      └───────────┘      └─────────┘        │
│                                                              │
│    ┌───────────────┐          ┌───────────────┐            │
│    │ Elasticsearch │          │   Keycloak    │            │
│    │    Search     │          │     Auth      │            │
│    └───────────────┘          └───────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

## Open Questions

None. All technical decisions provided by constitution.

## Next Steps

Proceed to Phase 1:
1. Generate `data-model.md` - Define JPA entities from spec Key Entities
2. Generate `contracts/` - OpenAPI spec for REST API, Avro schemas for Kafka messages
3. Generate `quickstart.md` - Developer getting-started guide
4. Update agent context with technology stack
