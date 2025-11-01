# Quickstart Guide: Enterprise Log Collection System

**Feature**: Enterprise Log Collection and Analysis System
**Branch**: `001-enterprise-log-collector`
**Date**: 2025-10-18

## Prerequisites

- **Java**: JDK 17 (LTS)
- **Maven**: 3.8+
- **Docker**: 20.10+ with Docker Compose
- **Git**: For version control
- **IDE**: IntelliJ IDEA or VS Code with Java extensions

## Quick Setup (5 Minutes)

###  1. Clone and Navigate

```bash
cd D:\GitHub\claude-demo\log_collector_dev
git checkout 001-enterprise-log-collector
```

### 2. Start Infrastructure

```bash
# Launch all dependencies (MySQL, Kafka, Redis, Keycloak, etc.)
docker-compose up -d

# Wait for services to be ready (~30 seconds)
docker-compose logs -f | grep "started"
```

**Services Started**:
- MySQL (port 3306)
- Redis (port 6379)
- Kafka + Zookeeper (ports 9092, 2181)
- Elasticsearch (port 9200)
- MinIO (ports 9000, 9001)
- Keycloak (port 8180)
- Prometheus (port 9090)
- Grafana (port 3000)

### 3. Initialize Database

```bash
# Run Flyway migrations
mvn flyway:migrate

# Seed test data (optional)
mvn flyway:migrate -Dflyway.locations=filesystem:db/migration,filesystem:db/seeds
```

### 4. Configure Keycloak

```bash
# Access Keycloak admin console
open http://localhost:8180

# Login: admin / admin
# Create realm: log-monitor
# Create client: log-collector-client (confidential)
# Create roles: ADMIN, OPERATOR, VIEWER
# Create test user: testuser / testpass with OPERATOR role
# Copy client secret to application-dev.yml
```

Or use automated setup:
```bash
./scripts/setup-keycloak.sh
```

### 5. Build Application

```bash
mvn clean package -DskipTests
```

### 6. Run Application

```bash
# Development mode with auto-reload
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run JAR
java -jar target/log-collector-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

###7. Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}

# Get OAuth2 token
TOKEN=$(curl -X POST http://localhost:8180/realms/log-monitor/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=log-collector-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=testuser" \
  -d "password=testpass" \
  -d "grant_type=password" | jq -r '.access_token')

# Test API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/v1/transactions

# Expected: {"content":[],"totalElements":0,...}
```

##  Development Workflow

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Performance tests
mvn test -P performance

# Coverage report
mvn test jacoco:report
open target/site/jacoco/index.html
```

### Adding a New Server for Log Collection

```bash
# Via API
curl -X POST http://localhost:8080/v1/servers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "192.168.1.100",
    "port": 22,
    "username": "loguser",
    "password": "logpass",
    "logFilePaths": ["/var/log/myapp.log", "/var/log/ledger.log"],
    "collectionInterval": 30000,
    "batchSize": 500
  }'
```

### Monitoring Logs

```bash
# Application logs
tail -f logs/application.log

# Filter by correlation ID
tail -f logs/application.log | grep "correlationId"

# Watch Kafka topics
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic log-raw-topic \
  --from-beginning
```

### Accessing Prometheus & Grafana

```bash
# Prometheus
open http://localhost:9090
# Query: logs_collected_total

# Grafana
open http://localhost:3000
# Login: admin / admin
# Import dashboard: dashboards/log-collector-dashboard.json
```

##  Project Structure

```
log_collector_dev/
├── src/main/java/com/logcollector/
│   ├── collector/          # Log collection from SSH
│   ├── processor/          # Log parsing and analysis
│   ├── alert/              # Alert rule engine
│   ├── archive/            # Data lifecycle management
│   ├── api/                # REST controllers
│   ├── domain/             # JPA entities
│   └── config/             # Spring configuration
│
├── src/main/resources/
│   ├── application.yml     # Base config
│   ├── application-dev.yml # Dev overrides
│   └── db/migration/       # Flyway SQL scripts
│
├── tests/
│   ├── unit/               # Unit tests
│   ├── integration/        # Integration tests
│   ├── contract/           # API contract tests
│   └── performance/        # JMH benchmarks
│
├── docker/
│   ├── docker-compose.yml  # Local dev stack
│   └── Dockerfile          # App container
│
├── k8s/                    # Kubernetes manifests
├── scripts/                # Utility scripts
└── specs/                  # Feature specifications
```

## Common Tasks

### Add a New Alert Rule

```bash
curl -X POST http://localhost:8080/v1/alerts/rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "High Error Rate",
    "alertType": "ERROR_RATE",
    "condition": "errorRate > 0.1",
    "threshold": 0.1,
    "timeWindowMinutes": 5,
    "severity": "CRITICAL",
    "deliveryChannels": ["email", "webhook"],
    "emailRecipients": ["ops@example.com"],
    "webhookUrl": "https://hooks.slack.com/services/..."
  }'
```

### Search Historical Logs

```bash
curl -X POST http://localhost:8080/v1/logs/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ERROR transaction failed",
    "startTime": "2025-10-18T00:00:00Z",
    "endTime": "2025-10-18T23:59:59Z",
    "servers": ["APP_SERVER"],
    "page": 0,
    "size": 50
  }'
```

### Trigger Manual Archival

```bash
curl -X POST http://localhost:8080/v1/admin/archive \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cutoffDays": 30,
    "compress": true
  }'
```

##  Troubleshooting

### Issue: Kafka Connection Refused

```bash
# Check Kafka is running
docker-compose ps kafka

# Verify Kafka logs
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Issue: SSH Connection Failed

```bash
# Test SSH manually
ssh loguser@192.168.1.100

# Check server status in database
docker exec -it mysql mysql -uroot -p
USE logdb;
SELECT * FROM server_configurations WHERE status = 'ERROR';

# View detailed error
SELECT id, hostname, last_error_message FROM server_configurations;
```

### Issue: High Memory Usage

```bash
# Check JVM heap
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Adjust heap size in docker-compose.yml or application launch:
java -Xms2g -Xmx4g -jar target/log-collector-1.0.0-SNAPSHOT.jar
```

##  Configuration Reference

### Key Application Properties

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logdb
    username: loguser
    password: logpass

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: log-processor-group

  redis:
    host: localhost
    port: 6379

log-collection:
  interval: 30000           # Collection interval (ms)
  batch-size: 500          # Lines per batch
  parallel: true           # Parallel server collection
  thread-pool-size: 10     # Collection threads
  timeout: 60000           # SSH timeout (ms)
  retry-times: 3           # Retry attempts
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/test/prod) | dev |
| `DATABASE_URL` | JDBC URL | jdbc:mysql://localhost:3306/logdb |
| `KAFKA_BROKERS` | Kafka bootstrap servers | localhost:9092 |
| `REDIS_HOST` | Redis hostname | localhost |
| `KEYCLOAK_URL` | Keycloak server URL | http://localhost:8180 |
| `MINIO_ENDPOINT` | MinIO endpoint | http://localhost:9000 |

##  Next Steps

1. **Add Test Servers**: Configure your Linux servers for log collection
2. **Import Dashboards**: Load Grafana dashboards from `dashboards/`
3. **Configure Alerts**: Set up alert rules for your use cases
4. **Review Logs**: Check `logs/application.log` for collection activity
5. **Scale Up**: Run multiple instances with `docker-compose up --scale log-collector=3`

## Resources

- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
- **Keycloak**: http://localhost:8180
- **MinIO Console**: http://localhost:9001

- **Specifications**: `specs/001-enterprise-log-collector/`
- **Constitution**: `.specify/memory/constitution.md`
- **Development Guide**: `LOG_COLLECTOR_DEV_GUIDE.md`

##  Support

- **Issues**: Create issue on GitHub repository
- **Documentation**: See `docs/` directory
- **Slack**: #log-collector-dev channel

---

**Happy Coding!** Start with User Story 1 (P1) - Automated Multi-Server Log Collection
