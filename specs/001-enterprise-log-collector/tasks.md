# Tasks: Enterprise Log Collection and Analysis System

**Input**: Design documents from `/specs/001-enterprise-log-collector/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Tests are included for all user stories as per constitution § III (Test-First Development - NON-NEGOTIABLE)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions (from plan.md)
```
src/main/java/com/logcollector/     # Main application source
src/main/resources/                  # Configuration files
tests/                               # Test directories
docker/                              # Docker configurations
k8s/                                 # Kubernetes manifests
scripts/                             # Utility scripts
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create Maven project structure with parent POM for dependency management
- [X] T002 Add Spring Boot 3.2+ dependencies to pom.xml (spring-boot-starter-web, spring-boot-starter-data-jpa, spring-kafka, spring-boot-starter-oauth2-resource-server, spring-boot-starter-actuator)
- [X] T003 [P] Add testing dependencies to pom.xml (JUnit 5, Spring Boot Test, Mockito, Testcontainers, JMH)
- [X] T004 [P] Add third-party dependencies to pom.xml (JSch 0.1.55, MinIO 8.5.7, Elasticsearch client, Micrometer Prometheus registry)
- [X] T005 [P] Create application.yml base configuration in src/main/resources/
- [X] T006 [P] Create application-dev.yml for development environment in src/main/resources/
- [X] T007 [P] Create application-prod.yml for production environment in src/main/resources/
- [X] T008 [P] Create logback-spring.xml for structured logging with correlation IDs in src/main/resources/
- [X] T009 [P] Configure JaCoCo plugin for 80% code coverage requirement in pom.xml
- [X] T010 [P] Create .gitignore for Java/Maven project

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T011 Create MySQL database schema DDL scripts in src/main/resources/db/migration/V001__initial_schema.sql
- [X] T012 [P] Create domain entities in src/main/java/com/logcollector/domain/model/ (ServerConfiguration, LogEntry, Transaction, TransactionFlowNode, AlertRule, AlertInstance, ArchiveJob, UserAccount)
- [X] T013 [P] Create Spring Data JPA repositories in src/main/java/com/logcollector/domain/repository/ for all entities
- [X] T014 [P] Configure HikariCP connection pooling in src/main/java/com/logcollector/config/database/DataSourceConfig.java
- [X] T015 [P] Configure MySQL table partitioning support in src/main/java/com/logcollector/config/database/PartitionConfig.java
- [X] T016 [P] Configure Redis connection and cache settings in src/main/java/com/logcollector/config/redis/RedisConfig.java
- [X] T017 [P] Configure Elasticsearch client and indexing in src/main/java/com/logcollector/config/elasticsearch/ElasticsearchConfig.java
- [X] T018 [P] Configure MinIO client for S3-compatible storage in src/main/java/com/logcollector/config/minio/MinIOConfig.java
- [X] T019 [P] Configure Kafka producer settings (acks=all, batch.size=32768, compression.type=lz4, linger.ms=50) in src/main/java/com/logcollector/config/kafka/KafkaProducerConfig.java
- [X] T020 [P] Configure Kafka consumer settings (enable.auto.commit=false, consumer groups) in src/main/java/com/logcollector/config/kafka/KafkaConsumerConfig.java
- [X] T021 [P] Generate Avro classes from kafka-schemas.avro (RawLogMessage, ProcessedLogMessage, AlertMessage, TransactionEventMessage, ArchiveRequestMessage)
- [X] T022 [P] Configure OAuth2 Resource Server with Keycloak in src/main/java/com/logcollector/config/security/SecurityConfig.java
- [X] T023 [P] Implement RBAC role-based method security (ADMIN, OPERATOR, VIEWER) in src/main/java/com/logcollector/config/security/RoleConfig.java
- [X] T024 [P] Configure Micrometer metrics registry for Prometheus in src/main/java/com/logcollector/config/metrics/MetricsConfig.java
- [X] T025 [P] Configure Spring Boot Actuator endpoints in application.yml (health, metrics)
- [X] T026 [P] Create encryption utility for AES-256 credential encryption in src/main/java/com/logcollector/config/security/EncryptionUtil.java
- [X] T027 [P] Create correlation ID filter for MDC logging in src/main/java/com/logcollector/config/logging/CorrelationIdFilter.java
- [X] T028 [P] Create global exception handler in src/main/java/com/logcollector/api/controller/GlobalExceptionHandler.java
- [X] T029 [P] Create base DTO classes in src/main/java/com/logcollector/api/dto/ (PagedResponse, ErrorResponse, SuccessResponse)
- [X] T030 Create docker-compose.yml for local development stack (MySQL, Kafka, Redis, Elasticsearch, MinIO, Keycloak, Prometheus, Grafana) in docker/
- [X] T031 Create setup scripts for Keycloak realm and client configuration in scripts/setup-keycloak.sh
- [X] T032 Create test data generator script for mock logs and transaction IDs in scripts/test-data-generator.sh

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Automated Multi-Server Log Collection (Priority: P1) 🎯 MVP

**Goal**: Automatically collect logs from multiple Linux servers via SSH without manual intervention, enabling centralized log access in seconds instead of minutes

**Independent Test**: Configure one or more test servers, run collection process, verify logs appear in central repository (MySQL + Elasticsearch) with correct metadata (server name, file name, timestamp)

### Tests for User Story 1 (TDD - Constitution Required)

**NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T033 [P] [US1] Unit test for SSH connection manager in tests/unit/collector/SSHConnectionManagerTest.java
- [ ] T034 [P] [US1] Unit test for incremental log collection logic in tests/unit/collector/IncrementalCollectorTest.java
- [ ] T035 [P] [US1] Unit test for Kafka producer batching in tests/unit/collector/LogKafkaProducerTest.java
- [ ] T036 [P] [US1] Integration test for SSH log collection with mock SSH server (Testcontainers) in tests/integration/ssh/SSHLogCollectionIntegrationTest.java
- [ ] T037 [P] [US1] Integration test for Kafka message flow (producer → consumer) in tests/integration/kafka/RawLogKafkaFlowTest.java
- [ ] T038 [P] [US1] Integration test for server configuration CRUD in tests/integration/database/ServerConfigurationRepositoryTest.java
- [ ] T039 [P] [US1] Contract test for POST /v1/servers endpoint in tests/contract/api/ServerApiContractTest.java
- [ ] T040 [P] [US1] Contract test for RawLogMessage Kafka schema in tests/contract/messaging/RawLogMessageSchemaTest.java

### Implementation for User Story 1

- [ ] T041 [P] [US1] Create SSH connection manager in src/main/java/com/logcollector/collector/ssh/SSHConnectionManager.java (JSch sessions, connection pooling)
- [ ] T042 [P] [US1] Create SSH command executor in src/main/java/com/logcollector/collector/ssh/SSHCommandExecutor.java (tail -n +position command)
- [ ] T043 [P] [US1] Create incremental collection position tracker in src/main/java/com/logcollector/collector/ssh/CollectionPositionTracker.java (persist last position per server/file)
- [ ] T044 [US1] Implement log collection scheduler in src/main/java/com/logcollector/collector/scheduler/LogCollectionScheduler.java (Spring @Scheduled per server interval)
- [ ] T045 [US1] Implement collection service in src/main/java/com/logcollector/collector/service/LogCollectionService.java (orchestrate SSH → parse → Kafka flow)
- [ ] T046 [US1] Create Kafka producer for raw logs in src/main/java/com/logcollector/collector/producer/RawLogKafkaProducer.java (publish RawLogMessage to log-raw-topic)
- [ ] T047 [P] [US1] Create server configuration DTO classes in src/main/java/com/logcollector/api/dto/server/ (CreateServerRequest, UpdateServerRequest, ServerConfiguration)
- [ ] T048 [US1] Implement server configuration REST controller in src/main/java/com/logcollector/api/controller/ServerController.java (GET /servers, POST /servers, PUT /servers/{id}, DELETE /servers/{id})
- [ ] T049 [US1] Implement server configuration service in src/main/java/com/logcollector/collector/service/ServerConfigurationService.java (CRUD operations with credential encryption)
- [ ] T050 [US1] Add retry logic with exponential backoff for SSH failures in src/main/java/com/logcollector/collector/ssh/SSHRetryHandler.java
- [ ] T051 [US1] Add server status tracking and error recording in ServerConfigurationService (update status, lastConnectedAt, lastErrorMessage)
- [ ] T052 [US1] Add correlation ID generation for distributed tracing in LogCollectionService
- [ ] T053 [US1] Add custom metrics for collection rate (logs.collected.total, logs.collection.errors.total) in LogCollectionService
- [ ] T054 [US1] Add health check for SSH connectivity in src/main/java/com/logcollector/collector/health/SSHHealthIndicator.java
- [ ] T055 [US1] Create collection error handling and dead letter queue in src/main/java/com/logcollector/collector/error/CollectionErrorHandler.java

**Checkpoint**: At this point, User Story 1 should be fully functional - logs collected from configured servers and available in central database

---

## Phase 4: User Story 2 - Transaction Flow Analysis and Visualization (Priority: P2)

**Goal**: Enable support engineers to trace complete transaction flows across servers in under 30 seconds, reducing troubleshooting time by 60%

**Independent Test**: Process logs containing transaction IDs, query for specific transaction, verify system returns complete flow showing all steps, sequence, duration, and status

### Tests for User Story 2 (TDD - Constitution Required)

- [ ] T056 [P] [US2] Unit test for log entry parser (regex transaction ID extraction) in tests/unit/processor/LogEntryParserTest.java
- [ ] T057 [P] [US2] Unit test for transaction analyzer (state transitions) in tests/unit/processor/TransactionAnalyzerTest.java
- [ ] T058 [P] [US2] Unit test for transaction flow graph builder in tests/unit/processor/TransactionFlowGraphBuilderTest.java
- [ ] T059 [P] [US2] Integration test for log processing pipeline (Kafka consumer → parse → DB persist → ES index) in tests/integration/processor/LogProcessingPipelineTest.java
- [ ] T060 [P] [US2] Integration test for transaction linking across multiple servers in tests/integration/processor/TransactionLinkingTest.java
- [ ] T061 [P] [US2] Integration test for Redis transaction caching in tests/integration/cache/TransactionCacheTest.java
- [ ] T062 [P] [US2] Contract test for GET /v1/transactions/{transactionId}/flow endpoint in tests/contract/api/TransactionFlowApiContractTest.java
- [ ] T063 [P] [US2] Contract test for ProcessedLogMessage Kafka schema in tests/contract/messaging/ProcessedLogMessageSchemaTest.java
- [ ] T064 [P] [US2] Contract test for TransactionEventMessage Kafka schema in tests/contract/messaging/TransactionEventMessageSchemaTest.java
- [ ] T065 [P] [US2] Performance test for log parsing throughput (JMH benchmark ≥1000 lines/sec) in tests/performance/parsing/LogParsingBenchmark.java

### Implementation for User Story 2

- [ ] T066 [P] [US2] Create log entry parser with configurable regex patterns in src/main/java/com/logcollector/processor/parser/LogEntryParser.java (extract transaction ID, timestamp, error indicators)
- [ ] T067 [P] [US2] Create timestamp parser with multiple format support in src/main/java/com/logcollector/processor/parser/TimestampParser.java
- [ ] T068 [P] [US2] Create error detector pattern matcher in src/main/java/com/logcollector/processor/parser/ErrorDetector.java
- [ ] T069 [US2] Implement Kafka consumer for raw logs in src/main/java/com/logcollector/processor/consumer/RawLogKafkaConsumer.java (consume from log-raw-topic)
- [ ] T070 [US2] Implement log processing service in src/main/java/com/logcollector/processor/service/LogProcessingService.java (parse → persist → index → publish)
- [ ] T071 [US2] Implement Elasticsearch indexer in src/main/java/com/logcollector/processor/elasticsearch/LogEntryIndexer.java (bulk indexing for full-text search)
- [ ] T072 [US2] Create Kafka producer for processed logs in src/main/java/com/logcollector/processor/producer/ProcessedLogKafkaProducer.java (publish ProcessedLogMessage to log-processed-topic)
- [ ] T073 [P] [US2] Implement transaction analyzer in src/main/java/com/logcollector/processor/analyzer/TransactionAnalyzer.java (link logs by transaction ID, determine status)
- [ ] T074 [P] [US2] Implement transaction state machine in src/main/java/com/logcollector/processor/analyzer/TransactionStateMachine.java (handle state transitions: IN_PROGRESS → SUCCESS/FAILED/TIMEOUT)
- [ ] T075 [US2] Implement transaction flow graph builder in src/main/java/com/logcollector/processor/analyzer/TransactionFlowGraphBuilder.java (create TransactionFlowNode entities)
- [ ] T076 [US2] Implement Redis transaction cache in src/main/java/com/logcollector/processor/cache/TransactionCacheService.java (1-hour TTL for active transactions)
- [ ] T077 [US2] Create Kafka producer for transaction events in src/main/java/com/logcollector/processor/producer/TransactionEventKafkaProducer.java (publish TransactionEventMessage to transaction-event-topic)
- [ ] T078 [P] [US2] Create transaction DTO classes in src/main/java/com/logcollector/api/dto/transaction/ (TransactionSummary, TransactionDetail, TransactionFlow, FlowNode, FlowEdge)
- [ ] T079 [US2] Implement transaction REST controller in src/main/java/com/logcollector/api/controller/TransactionController.java (GET /transactions, GET /transactions/{id}, GET /transactions/{id}/flow)
- [ ] T080 [US2] Implement transaction query service in src/main/java/com/logcollector/processor/service/TransactionQueryService.java (retrieve with caching, pagination, filtering)
- [ ] T081 [US2] Implement transaction flow visualization generator in src/main/java/com/logcollector/processor/service/TransactionFlowVisualizationService.java (generate Mermaid diagram syntax)
- [ ] T082 [US2] Add timeout detection scheduled task in src/main/java/com/logcollector/processor/scheduler/TransactionTimeoutDetector.java (mark transactions with no activity >30s as TIMEOUT)
- [ ] T083 [US2] Add custom metrics for transaction processing (transactions.analyzed.total, transaction.analysis.duration) in TransactionAnalyzer
- [ ] T084 [US2] Add exactly-once processing semantics (manual Kafka commit after DB transaction) in LogProcessingService
- [ ] T085 [US2] Handle Elasticsearch indexing failures gracefully (continue processing, alert on repeated failures) in LogEntryIndexer

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - logs collected AND transactions analyzed with flow visualization

---

## Phase 5: User Story 3 - Intelligent Alerting on Anomalies (Priority: P3)

**Goal**: Enable proactive response to issues through automated notifications when abnormal patterns occur (high error rates, timeouts, failures) with <5% false positive rate

**Independent Test**: Configure alert rules, trigger conditions (e.g., create errors exceeding threshold), verify alerts generated and delivered via configured channels within specified timeframes

### Tests for User Story 3 (TDD - Constitution Required)

- [ ] T086 [P] [US3] Unit test for alert rule evaluator (condition expression parsing) in tests/unit/alert/AlertRuleEvaluatorTest.java
- [ ] T087 [P] [US3] Unit test for error rate calculator in tests/unit/alert/ErrorRateCalculatorTest.java
- [ ] T088 [P] [US3] Unit test for alert throttling logic in tests/unit/alert/AlertThrottleManagerTest.java
- [ ] T089 [P] [US3] Integration test for alert triggering pipeline in tests/integration/alert/AlertTriggeringPipelineTest.java
- [ ] T090 [P] [US3] Integration test for email notification delivery in tests/integration/alert/EmailNotificationTest.java
- [ ] T091 [P] [US3] Integration test for webhook notification delivery in tests/integration/alert/WebhookNotificationTest.java
- [ ] T092 [P] [US3] Contract test for POST /v1/alerts/rules endpoint in tests/contract/api/AlertRuleApiContractTest.java
- [ ] T093 [P] [US3] Contract test for AlertMessage Kafka schema in tests/contract/messaging/AlertMessageSchemaTest.java

### Implementation for User Story 3

- [ ] T094 [P] [US3] Create alert rule evaluator in src/main/java/com/logcollector/alert/rules/AlertRuleEvaluator.java (evaluate condition expressions: ERROR_RATE, TRANSACTION_TIMEOUT, PATTERN_MATCH)
- [ ] T095 [P] [US3] Create error rate calculator in src/main/java/com/logcollector/alert/evaluator/ErrorRateCalculator.java (count errors in time window)
- [ ] T096 [P] [US3] Create timeout detector in src/main/java/com/logcollector/alert/evaluator/TimeoutDetector.java (detect avg duration > threshold)
- [ ] T097 [P] [US3] Create pattern matcher in src/main/java/com/logcollector/alert/evaluator/PatternMatcher.java (regex matching on log content)
- [ ] T098 [US3] Implement Kafka consumer for processed logs in src/main/java/com/logcollector/alert/consumer/ProcessedLogKafkaConsumer.java (consume from log-processed-topic for alert evaluation)
- [ ] T099 [US3] Implement alert service in src/main/java/com/logcollector/alert/service/AlertService.java (orchestrate rule evaluation → instance creation → notification)
- [ ] T100 [US3] Implement alert throttle manager in src/main/java/com/logcollector/alert/throttle/AlertThrottleManager.java (Redis-based throttling to prevent duplicate alerts)
- [ ] T101 [US3] Create Kafka producer for alerts in src/main/java/com/logcollector/alert/producer/AlertKafkaProducer.java (publish AlertMessage to alert-topic)
- [ ] T102 [P] [US3] Implement email notifier in src/main/java/com/logcollector/alert/notifier/EmailNotifier.java (JavaMail integration)
- [ ] T103 [P] [US3] Implement webhook notifier in src/main/java/com/logcollector/alert/notifier/WebhookNotifier.java (HTTP POST to webhook URL)
- [ ] T104 [US3] Implement notification dispatcher in src/main/java/com/logcollector/alert/notifier/NotificationDispatcher.java (consume from alert-topic, dispatch to channels)
- [ ] T105 [US3] Implement alert resolution detector in src/main/java/com/logcollector/alert/service/AlertResolutionDetector.java (detect condition cleared, send resolution notification)
- [ ] T106 [P] [US3] Create alert rule DTO classes in src/main/java/com/logcollector/api/dto/alert/ (CreateAlertRuleRequest, UpdateAlertRuleRequest, AlertRule, AlertInstance)
- [ ] T107 [US3] Implement alert rule REST controller in src/main/java/com/logcollector/api/controller/AlertRuleController.java (GET /alerts/rules, POST /alerts/rules, PUT /alerts/rules/{id}, DELETE /alerts/rules/{id})
- [ ] T108 [US3] Implement alert instance REST controller in src/main/java/com/logcollector/api/controller/AlertInstanceController.java (GET /alerts/active, POST /alerts/{id}/acknowledge)
- [ ] T109 [US3] Implement alert rule management service in src/main/java/com/logcollector/alert/service/AlertRuleManagementService.java (CRUD with validation)
- [ ] T110 [US3] Add validation for alert rule delivery channels (email requires emailRecipients, webhook requires webhookUrl) in CreateAlertRuleRequest
- [ ] T111 [US3] Add custom metrics for alerting (alerts.triggered.total, alert.evaluation.duration) in AlertService
- [ ] T112 [US3] Add scheduled task for auto-resolution of stale alerts (>24h without resolution) in src/main/java/com/logcollector/alert/scheduler/StaleAlertResolver.java

**Checkpoint**: All three priority user stories now functional - collection, analysis, AND alerting working independently

---

## Phase 6: User Story 4 - Historical Search and Analysis (Priority: P4)

**Goal**: Enable compliance auditors and support engineers to search historical logs by various criteria with results returned within 10 seconds

**Independent Test**: Archive historical data, execute searches with different criteria (time range, server, transaction ID, patterns), verify results accurate and returned within acceptable time

### Tests for User Story 4 (TDD - Constitution Required)

- [ ] T113 [P] [US4] Unit test for search query builder (Elasticsearch DSL) in tests/unit/search/SearchQueryBuilderTest.java
- [ ] T114 [P] [US4] Unit test for search result highlighter in tests/unit/search/SearchResultHighlighterTest.java
- [ ] T115 [P] [US4] Integration test for full-text search in tests/integration/search/FullTextSearchTest.java
- [ ] T116 [P] [US4] Integration test for multi-criteria filtering in tests/integration/search/MultiCriteriaFilterTest.java
- [ ] T117 [P] [US4] Integration test for archived data retrieval in tests/integration/search/ArchiveSearchTest.java
- [ ] T118 [P] [US4] Contract test for POST /v1/logs/search endpoint in tests/contract/api/LogSearchApiContractTest.java
- [ ] T119 [P] [US4] Performance test for search query response time (<10 seconds for 90 days) in tests/performance/search/SearchPerformanceBenchmark.java

### Implementation for User Story 4

- [ ] T120 [P] [US4] Create Elasticsearch query builder in src/main/java/com/logcollector/search/elasticsearch/SearchQueryBuilder.java (build complex queries with filters)
- [ ] T121 [P] [US4] Create search result mapper in src/main/java/com/logcollector/search/mapper/SearchResultMapper.java (map ES results to DTOs with highlighting)
- [ ] T122 [P] [US4] Create archive data retriever in src/main/java/com/logcollector/search/archive/ArchiveDataRetriever.java (fetch from MinIO when needed)
- [ ] T123 [US4] Implement log search service in src/main/java/com/logcollector/search/service/LogSearchService.java (search Elasticsearch + archive transparently)
- [ ] T124 [US4] Implement pagination handler in src/main/java/com/logcollector/search/service/SearchPaginationHandler.java (handle large result sets)
- [ ] T125 [P] [US4] Create log search DTO classes in src/main/java/com/logcollector/api/dto/search/ (LogSearchRequest, LogSearchResponse, LogEntry with highlight)
- [ ] T126 [US4] Implement log search REST controller in src/main/java/com/logcollector/api/controller/LogSearchController.java (POST /logs/search)
- [ ] T127 [US4] Add result export functionality (CSV, JSON formats) in src/main/java/com/logcollector/search/export/SearchResultExporter.java
- [ ] T128 [US4] Add custom metrics for search operations (search.queries.total, search.response.duration) in LogSearchService
- [ ] T129 [US4] Optimize Elasticsearch queries with proper field mapping and analyzers

**Checkpoint**: Four user stories complete - full search capabilities added to collection, analysis, and alerting

---

## Phase 7: User Story 5 - Data Archival and Lifecycle Management (Priority: P5)

**Goal**: Automatic data lifecycle management to control storage costs while maintaining compliance (365-day retention)

**Independent Test**: Configure retention policies, allow time to pass or simulate date advancement, verify data moves through lifecycle stages (hot → warm → cold → purged) according to thresholds

### Tests for User Story 5 (TDD - Constitution Required)

- [ ] T130 [P] [US5] Unit test for retention policy evaluator in tests/unit/archive/RetentionPolicyEvaluatorTest.java
- [ ] T131 [P] [US5] Unit test for data compressor (GZIP compression ratio) in tests/unit/archive/DataCompressorTest.java
- [ ] T132 [P] [US5] Integration test for archival job execution in tests/integration/archive/ArchivalJobExecutionTest.java
- [ ] T133 [P] [US5] Integration test for MinIO upload and verification in tests/integration/archive/MinIOUploadTest.java
- [ ] T134 [P] [US5] Integration test for data deletion after successful archive in tests/integration/archive/PostArchiveDeletionTest.java
- [ ] T135 [P] [US5] Contract test for POST /v1/admin/archive endpoint in tests/contract/api/ArchiveApiContractTest.java
- [ ] T136 [P] [US5] Contract test for ArchiveRequestMessage Kafka schema in tests/contract/messaging/ArchiveRequestMessageSchemaTest.java

### Implementation for User Story 5

- [ ] T137 [P] [US5] Create retention policy evaluator in src/main/java/com/logcollector/archive/lifecycle/RetentionPolicyEvaluator.java (determine data tier by age)
- [ ] T138 [P] [US5] Create data exporter in src/main/java/com/logcollector/archive/export/DataExporter.java (export transactions and logs to JSON)
- [ ] T139 [P] [US5] Create data compressor in src/main/java/com/logcollector/archive/compressor/GzipCompressor.java (GZIP compression with ratio calculation)
- [ ] T140 [P] [US5] Create MinIO uploader in src/main/java/com/logcollector/archive/minio/MinIOUploader.java (upload with checksum verification)
- [ ] T141 [US5] Implement Kafka consumer for archive requests in src/main/java/com/logcollector/archive/consumer/ArchiveRequestKafkaConsumer.java (consume from archive-request-topic)
- [ ] T142 [US5] Implement archive service in src/main/java/com/logcollector/archive/service/ArchiveService.java (orchestrate export → compress → upload → verify → delete)
- [ ] T143 [US5] Implement archive scheduler in src/main/java/com/logcollector/archive/scheduler/ArchiveScheduler.java (daily scheduled archival of data >30 days)
- [ ] T144 [US5] Implement archive job manager in src/main/java/com/logcollector/archive/service/ArchiveJobManager.java (track job status, handle failures)
- [ ] T145 [US5] Implement data deletion service in src/main/java/com/logcollector/archive/service/DataDeletionService.java (transactional delete after successful archive)
- [ ] T146 [P] [US5] Create archive DTO classes in src/main/java/com/logcollector/api/dto/archive/ (TriggerArchiveRequest, ArchiveJobStatus, ArchiveFile)
- [ ] T147 [US5] Implement archive REST controller in src/main/java/com/logcollector/api/controller/ArchiveController.java (POST /admin/archive, GET /admin/archive/files)
- [ ] T148 [US5] Add audit trail logging for all archival and deletion operations in ArchiveService
- [ ] T149 [US5] Add data integrity verification (checksum comparison) in MinIOUploader
- [ ] T150 [US5] Add rollback mechanism for failed archival (keep source data intact) in ArchiveService
- [ ] T151 [US5] Add custom metrics for archival (archive.jobs.total, archive.data.bytes, archive.compression.ratio) in ArchiveService

**Checkpoint**: All five user stories complete - full lifecycle management from collection to archival

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and production readiness

- [ ] T152 [P] Create multi-stage Dockerfile in docker/Dockerfile (Maven build → JRE runtime)
- [ ] T153 [P] Create Kubernetes deployment manifests in k8s/deployments/ (collector-deployment.yaml, processor-deployment.yaml, alert-deployment.yaml, archive-deployment.yaml)
- [ ] T154 [P] Create Kubernetes service manifests in k8s/services/ (api-service.yaml)
- [ ] T155 [P] Create Kubernetes ConfigMaps in k8s/configmaps/ (application-config.yaml)
- [ ] T156 [P] Create Kubernetes Secrets in k8s/secrets/ (credentials-secret.yaml)
- [ ] T157 [P] Create HorizontalPodAutoscaler for services in k8s/autoscaling/ (CPU >70% trigger)
- [ ] T158 [P] Create Prometheus monitoring dashboard in dashboards/prometheus-dashboard.json
- [ ] T159 [P] Create Grafana visualization dashboard in dashboards/grafana-dashboard.json
- [ ] T160 [P] Create API documentation using Springdoc OpenAPI in src/main/java/com/logcollector/config/OpenApiConfig.java
- [ ] T161 [P] Add comprehensive JavaDoc comments to all public APIs
- [ ] T162 [P] Create runbook for SSH connection failures in docs/runbooks/ssh-connection-failures.md
- [ ] T163 [P] Create runbook for Kafka lag remediation in docs/runbooks/kafka-lag-remediation.md
- [ ] T164 [P] Create runbook for database deadlock recovery in docs/runbooks/database-deadlock-recovery.md
- [ ] T165 [P] Configure JVM parameters in application.yml (G1GC, 4GB heap: -Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200)
- [ ] T166 [P] Add input sanitization to prevent log injection attacks in LogEntryParser
- [ ] T167 [P] Add rate limiting to public API endpoints in SecurityConfig
- [ ] T168 [P] Add database query optimization (add missing indexes identified in testing)
- [ ] T169 [P] Add graceful shutdown handling for all services (drain Kafka messages, max 30s)
- [ ] T170 Run full integration test suite against docker-compose environment
- [ ] T171 Run performance benchmarks and validate against constitution targets (1000 lines/sec, <500ms p95 latency)
- [ ] T172 Run JaCoCo coverage report and ensure >80% coverage
- [ ] T173 Run SonarQube static analysis and fix critical/blocker issues
- [ ] T174 Validate quickstart.md setup steps work end-to-end
- [ ] T175 Create production deployment guide in docs/production-deployment.md
- [ ] T176 [P] Add monitoring alerts for critical metrics in Prometheus (high error rate, Kafka lag, low disk space)
- [ ] T177 [P] Configure log retention policy for application logs (7-day rotation)
- [ ] T178 Final code review and refactoring for maintainability

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4 → P5)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Consumes Kafka messages produced by US1 but can be tested independently
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Consumes messages from US2 but can be tested independently
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Uses Elasticsearch indexes from US2 but can be tested independently
- **User Story 5 (P5)**: Can start after Foundational (Phase 2) - Archives data from US1/US2 but can be tested independently

### Within Each User Story

**CRITICAL - TDD Flow**:
1. Tests MUST be written and FAIL before implementation
2. Models before services
3. Services before endpoints/controllers
4. Core implementation before integration
5. Story complete before moving to next priority

### Parallel Opportunities

**Setup Phase (Phase 1)**: Tasks T002-T010 can all run in parallel

**Foundational Phase (Phase 2)**:
- Database tasks: T011-T015 can run in parallel after T011
- Config tasks: T016-T027 can all run in parallel
- DTO/Exception tasks: T028-T029 can run in parallel

**User Story 1 Tests**: T033-T040 can all run in parallel (write all tests first)

**User Story 1 Implementation**:
- T041-T043 (SSH components) can run in parallel
- T047 (DTOs) can run in parallel with T041-T043

**User Story 2 Tests**: T056-T065 can all run in parallel

**User Story 2 Implementation**:
- T066-T068 (parsers) can run in parallel
- T073-T074 (analyzers) can run in parallel
- T078 (DTOs) can run in parallel with other tasks

**User Story 3 Tests**: T086-T093 can all run in parallel

**User Story 3 Implementation**:
- T094-T097 (evaluators) can run in parallel
- T102-T103 (notifiers) can run in parallel
- T106 (DTOs) can run in parallel with other tasks

**User Story 4 Tests**: T113-T119 can all run in parallel

**User Story 4 Implementation**:
- T120-T122 (search components) can run in parallel
- T125 (DTOs) can run in parallel with other tasks

**User Story 5 Tests**: T130-T136 can all run in parallel

**User Story 5 Implementation**:
- T137-T140 (archive components) can run in parallel
- T146 (DTOs) can run in parallel with other tasks

**Polish Phase (Phase 8)**: Tasks T152-T169 and T176-T177 can all run in parallel

**Multiple User Stories in Parallel**: Once Foundational is complete, different team members can work on US1, US2, US3, US4, US5 simultaneously

---

## Parallel Example: User Story 1

```bash
# Step 1: Launch all tests together (TDD - write failing tests first):
Task: T033 "Unit test for SSH connection manager"
Task: T034 "Unit test for incremental log collection logic"
Task: T035 "Unit test for Kafka producer batching"
Task: T036 "Integration test for SSH log collection"
Task: T037 "Integration test for Kafka message flow"
Task: T038 "Integration test for server configuration CRUD"
Task: T039 "Contract test for POST /v1/servers"
Task: T040 "Contract test for RawLogMessage Kafka schema"

# Step 2: Launch parallel implementation tasks:
Task: T041 "Create SSH connection manager"
Task: T042 "Create SSH command executor"
Task: T043 "Create incremental collection position tracker"
Task: T047 "Create server configuration DTO classes"

# Step 3: Sequential tasks (depend on Step 2):
Task: T044 "Implement log collection scheduler" (uses T041-T043)
Task: T045 "Implement collection service" (uses T044, T046)
Task: T046 "Create Kafka producer for raw logs"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (Tasks T001-T010)
2. Complete Phase 2: Foundational (Tasks T011-T032) - **CRITICAL BLOCKER**
3. Complete Phase 3: User Story 1 (Tasks T033-T055)
4. **STOP and VALIDATE**:
   - Run tests (should now PASS after TDD implementation)
   - Configure test server
   - Verify logs collected and visible in database
   - Check metrics in Grafana
5. Deploy/demo MVP if ready

**MVP Deliverable**: Automated log collection from multiple servers - immediately replaces manual SSH/file copying

### Incremental Delivery

1. **Foundation** (Phases 1-2) → Infrastructure ready
2. **MVP** (Phase 3 - US1) → Test independently → Deploy/Demo
   - **Value**: Eliminate manual log collection
3. **+ Transaction Analysis** (Phase 4 - US2) → Test independently → Deploy/Demo
   - **Value**: Troubleshoot distributed transactions in <30 seconds
4. **+ Alerting** (Phase 5 - US3) → Test independently → Deploy/Demo
   - **Value**: Proactive issue detection before customer impact
5. **+ Search** (Phase 6 - US4) → Test independently → Deploy/Demo
   - **Value**: Compliance audit support, historical analysis
6. **+ Archival** (Phase 7 - US5) → Test independently → Deploy/Demo
   - **Value**: 70% storage cost reduction
7. **Polish** (Phase 8) → Production hardening

Each increment delivers measurable value without breaking previous features.

### Parallel Team Strategy

With multiple developers:

1. **Together**: Complete Setup (Phase 1) + Foundational (Phase 2)
2. **Once Foundational complete**:
   - **Developer A**: User Story 1 (Log Collection)
   - **Developer B**: User Story 2 (Transaction Analysis)
   - **Developer C**: User Story 3 (Alerting)
   - **Developer D**: User Story 4 (Search)
   - **Developer E**: User Story 5 (Archival)
3. **Integration**: Stories integrate via Kafka - minimal coordination needed
4. **Testing**: Each developer validates their story independently
5. **Polish**: Team reconvenes for Phase 8

---

## Summary

- **Total Tasks**: 178
- **User Story 1 (P1 - MVP)**: 23 tasks (13 foundational + 8 tests + 15 implementation)
- **User Story 2 (P2)**: 30 tasks (10 tests + 20 implementation)
- **User Story 3 (P3)**: 27 tasks (8 tests + 19 implementation)
- **User Story 4 (P4)**: 17 tasks (7 tests + 10 implementation)
- **User Story 5 (P5)**: 22 tasks (7 tests + 15 implementation)
- **Setup**: 10 tasks
- **Foundational**: 22 tasks (blocks all user stories)
- **Polish**: 27 tasks

**Parallel Opportunities Identified**:
- ~45 tasks marked [P] across all phases
- 5 user stories can proceed in parallel after Foundational phase
- All tests within a story can be written in parallel (TDD first)
- All model/parser/evaluator tasks within stories are parallelizable

**MVP Scope**: Phase 1 (Setup) + Phase 2 (Foundational) + Phase 3 (User Story 1) = **45 tasks**

**Constitution Compliance**:
- ✅ Test-First Development: Tests written before implementation for all stories
- ✅ >80% Coverage: JaCoCo configured with 80% minimum
- ✅ Performance Targets: JMH benchmarks for 1000 lines/sec validation
- ✅ Security: OAuth2, RBAC, AES-256 encryption, audit logging
- ✅ Observability: Prometheus metrics, correlation IDs, health checks
- ✅ Operational Excellence: Docker/K8s, automated backups, runbooks

**Format Validation**: ✅ All tasks follow checklist format with checkbox, ID, [P] marker (where applicable), [Story] label (for user story tasks), and file paths
