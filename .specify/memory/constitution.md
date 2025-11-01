<!--
  ============================================================================
  SYNC IMPACT REPORT
  ============================================================================
  Version Change: NONE → 1.0.0

  Modified Principles:
  - Created I. Enterprise-Grade Architecture
  - Created II. Observability & Monitoring (NON-NEGOTIABLE)
  - Created III. Test-First Development (NON-NEGOTIABLE)
  - Created IV. Security & Authentication
  - Created V. Performance & Scalability
  - Created VI. Operational Excellence

  Added Sections:
  - Technology Stack
  - Development Workflow
  - Governance

  Removed Sections: N/A (initial creation)

  Templates Requiring Updates:
  - ✅ plan-template.md - Constitution Check section compatible
  - ✅ spec-template.md - Requirements alignment compatible
  - ✅ tasks-template.md - Task categorization compatible

  Follow-up TODOs: None

  Ratification Date: 2025-10-18 (project initialization)
  Last Amendment: 2025-10-18 (initial version)
  ============================================================================
-->

# Enterprise Log Collector System Constitution

## Core Principles

### I. Enterprise-Grade Architecture

The system MUST follow distributed microservices architecture with proper service boundaries. Each
service (Log Collector, Log Processor, Alert Service, Archive Service) MUST be independently
deployable and scalable. Services communicate via Kafka message queues to ensure loose coupling
and fault tolerance.

**Multi-tier storage strategy is mandatory:**
- Hot data in MySQL/Redis for real-time queries (<30 days)
- Indexed data in Elasticsearch for search (<90 days)
- Cold data archived to MinIO for compliance (>90 days)

**Rationale**: Enterprise systems must handle failure gracefully and scale horizontally.
Microservices enable independent scaling of high-load components (collection vs. processing).
Multi-tier storage optimizes cost (MinIO) vs. performance (Redis) trade-offs while meeting
compliance requirements.

### II. Observability & Monitoring (NON-NEGOTIABLE)

Every service MUST expose Prometheus metrics at `/actuator/metrics` endpoint. All business
operations MUST emit structured logs with correlation IDs for distributed tracing. Health checks
MUST be implemented at `/actuator/health` with liveness and readiness probes.

**Required metrics categories:**
- Business metrics: logs processed, error rates, transaction success rates
- System metrics: JVM heap/GC, connection pool usage, Kafka consumer lag
- Performance metrics: processing latency (p50, p95, p99), throughput

**Structured logging format:**
```
[timestamp] [correlation-id] [service] [level] [logger] - message
```

**Rationale**: Without observability, troubleshooting distributed systems is impossible.
Correlation IDs enable tracing requests across service boundaries. Prometheus integration provides
standardized monitoring and alerting via Grafana. Health checks enable Kubernetes to automatically
recover failed instances.

### III. Test-First Development (NON-NEGOTIABLE)

TDD is strictly enforced: tests MUST be written → user approved → tests FAIL → then implement.
Red-Green-Refactor cycle is mandatory.

**Test pyramid requirements:**
- Unit tests: Service logic, parsers, validators (>80% coverage)
- Integration tests: Kafka message flow, database operations, SSH connections
- Contract tests: REST API endpoints, message schemas
- Performance tests: Throughput benchmarks, memory/CPU profiling

**Test data generation**: Mock SSH servers, sample log generators, and load testing scripts MUST
be provided in `scripts/` directory.

**Rationale**: Log collection systems handle critical audit data - bugs can lead to compliance
violations or data loss. Test-first ensures correctness before deployment. Performance tests catch
regressions in throughput/latency. Contract tests prevent breaking changes in message formats.

### IV. Security & Authentication

OAuth2/OIDC authentication via Keycloak is mandatory for all API endpoints. Role-based access
control (RBAC) MUST enforce least-privilege access. Sensitive data (SSH credentials, database
passwords) MUST be encrypted at rest using AES-256 and stored in environment variables or secrets
management (not in code).

**Audit logging requirements:**
- All data access MUST be logged with user identity, timestamp, operation
- Security events (failed auth, privilege escalation) MUST trigger alerts
- Audit logs MUST be immutable and retained for compliance period

**Input validation**: All external inputs (API parameters, SSH log content) MUST be validated and
sanitized to prevent injection attacks (SQL injection, log injection).

**Rationale**: Enterprise systems process sensitive business transactions. OAuth2 provides
centralized authentication/SSO. RBAC prevents unauthorized access to sensitive data. Audit logging
meets compliance requirements (GDPR, SOX, etc.). Encryption protects credentials if infrastructure
is compromised.

### V. Performance & Scalability

The system MUST handle **1000 log lines/second** throughput with **<500ms p95 latency** for query
operations. JVM MUST be tuned for G1GC with heap sizing appropriate to workload (4-8GB for
production). Database connection pooling (HikariCP) MUST be configured for optimal concurrency.

**Kafka optimization:**
- Producer: Batching enabled (batch.size=32KB), compression (snappy/lz4)
- Consumer: Parallel consumption with consumer groups, offset management

**Database optimization:**
- Indexes on high-cardinality query columns (transaction_id, timestamp, server_name)
- Partitioning by month for log_entries table to manage growth
- Query timeout enforcement to prevent runaway queries

**Caching strategy**: Redis MUST cache frequently accessed data (active transactions, recent
alerts) with TTL-based expiration to reduce database load.

**Rationale**: Enterprise log volumes grow exponentially - systems must scale proactively. JVM
tuning prevents GC pauses that cause timeouts. Kafka batching/compression reduces network overhead.
Database partitioning prevents table scans on multi-billion row tables. Redis caching reduces
database hotspots.

### VI. Operational Excellence

**Deployment**: Docker containers with Kubernetes orchestration. Rolling updates with zero
downtime. Resource limits (CPU/memory) MUST be defined to prevent noisy neighbor issues.

**Backup & Recovery**:
- MySQL automated daily backups with 30-day retention (local + S3)
- Kafka topic replication factor ≥3 for fault tolerance
- MinIO archive data replicated across availability zones

**CI/CD Pipeline**: Jenkins/GitLab CI with automated stages:
1. Build → Test → SonarQube analysis → Docker build → Push to registry → Deploy to K8s
2. Automated rollback on health check failures
3. Smoke tests post-deployment

**Runbook documentation**: Operational guides MUST cover common failure scenarios (SSH connection
loss, Kafka lag, database deadlocks) with step-by-step remediation.

**Rationale**: Production systems fail - operations must be designed for failure recovery.
Automated backups prevent data loss. CI/CD ensures consistent deployments and fast rollback.
Runbooks reduce MTTR (mean time to recovery). K8s resource limits prevent cascade failures.

## Technology Stack

**Backend Framework**: Spring Boot 3.2+ with Spring Cloud for microservices
**Message Queue**: Apache Kafka (≥3.x) with Zookeeper/KRaft
**Databases**: MySQL 8.0 (primary), Redis (cache), Elasticsearch (search)
**Storage**: MinIO S3-compatible object storage
**Monitoring**: Prometheus + Grafana
**Authentication**: Keycloak (OAuth2/OIDC)
**Containerization**: Docker + Docker Compose (dev), Kubernetes (production)
**Build Tool**: Maven 3.8+
**Java Version**: JDK 17 (LTS)

**Rationale**: Spring Boot provides enterprise-grade framework with production-ready features
(Actuator, security). Kafka handles high-throughput message streaming. Elasticsearch enables
full-text search on log content. Keycloak provides centralized identity management. MinIO offers
cost-effective archival storage with S3 compatibility.

## Development Workflow

**Branching Strategy**: Feature branches from `main`, naming convention `###-feature-name`
**Code Reviews**: All PRs require approval + passing CI checks before merge
**Commit Messages**: Conventional commits format (feat:, fix:, docs:, refactor:)

**Development Environment**:
- Local: Docker Compose with all dependencies (MySQL, Kafka, Redis, Keycloak)
- IDE: IntelliJ IDEA or VS Code with Spring Boot extensions
- Debugging: Remote debug port (5005) enabled in development profile

**Code Standards**:
- Naming: PascalCase (classes), camelCase (methods), UPPER_SNAKE_CASE (constants)
- Javadoc required for all public APIs
- Lombok annotations for boilerplate reduction
- Spring dependency injection (constructor injection preferred)

**Rationale**: Standardized workflow reduces onboarding friction and merge conflicts. Docker
Compose provides consistent local environments. Code reviews catch bugs and share knowledge.
Conventional commits enable automated changelog generation.

## Governance

This constitution supersedes all other development practices and decisions. Amendments require:
1. Written proposal with rationale
2. Team review and approval
3. Migration plan for existing code (if breaking change)
4. Version bump according to semantic versioning

**Compliance verification**: All PRs MUST verify adherence to constitution principles. Reviewers
MUST challenge violations with reference to specific principle sections.

**Complexity justification**: Any violation of simplicity principles (adding new services,
frameworks, databases) MUST be documented with business justification and simpler alternatives
considered.

**Runtime guidance**: Developers should reference `LOG_COLLECTOR_DEV_GUIDE.md` for detailed
implementation guidance, architecture diagrams, and operational procedures.

**Version**: 1.0.0 | **Ratified**: 2025-10-18 | **Last Amended**: 2025-10-18
