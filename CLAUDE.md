# log_collector_dev Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-18

## Active Technologies
- Java 17 (LTS) + Spring Boot 3.2+, Spring Cloud, Apache Kafka 3.x+, JSch (SSH client), Micrometer (metrics) (001-enterprise-log-collector)

## Project Structure
```
src/
tests/
```

## Commands
# Add commands for Java 17 (LTS)

## Code Style
Java 17 (LTS): Follow standard conventions

## Recent Changes
- 001-enterprise-log-collector: Added Java 17 (LTS) + Spring Boot 3.2+, Spring Cloud, Apache Kafka 3.x+, JSch (SSH client), Micrometer (metrics)
- **2025-10-24**: Added comprehensive log viewing system with Web UI and RESTful APIs

<!-- MANUAL ADDITIONS START -->

## Log Viewing System (Added 2025-10-24)

### Overview
A modern web-based log viewing and analysis system has been added to the project, providing intuitive interfaces for querying, analyzing, and tracking system logs.

### Features
- **Web UI**: Modern purple-gradient themed interface accessible at http://localhost:8080
- **Multi-dimensional Queries**: Filter by time range, server ID, log type, transaction ID, and error status
- **Transaction Tracking**: Trace and view all logs associated with specific transactions
- **Real-time Statistics**: View log counts, error rates, and parsing statistics
- **Pagination**: Efficient browsing of large log datasets

### API Endpoints

#### Log Query APIs (`/api/v1/logs`)
- `GET /api/v1/logs` - Query logs with filters (page, size, time range, serverId, logType, transactionId, errorIndicator)
- `GET /api/v1/logs/{id}` - Get single log entry by ID
- `GET /api/v1/logs/transaction/{transactionId}` - Get all logs for a transaction
- `GET /api/v1/logs/errors` - Get error logs with pagination
- `GET /api/v1/logs/statistics` - Get log statistics for a time range

#### Transaction Query APIs (`/api/v1/transactions`)
- `GET /api/v1/transactions` - Query transactions with filters
- `GET /api/v1/transactions/{transactionId}` - Get transaction details
- `GET /api/v1/transactions/{transactionId}/logs` - Get all logs for a transaction

### Code Structure

**Backend Components**:
```
src/main/java/com/logcollector/
├── api/
│   ├── controller/
│   │   ├── LogQueryController.java
│   │   └── TransactionController.java
│   ├── dto/
│   │   ├── LogEntryDTO.java
│   │   ├── TransactionDTO.java
│   │   └── LogStatisticsDTO.java
│   └── service/
│       ├── LogQueryService.java
│       └── TransactionQueryService.java
└── config/
    ├── security/SecurityConfig.java (dev profile with disabled auth)
    └── web/WebMvcConfig.java (static resource handler)
```

**Frontend Components**:
```
src/main/resources/static/
├── index.html     # Main UI with 3 tabs (logs, transactions, statistics)
├── styles.css     # Purple gradient theme styling
└── app.js         # Frontend logic (API calls, pagination, UI updates)
```

### Development Guidelines

**Testing the UI**:
1. Start the application: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
2. Open browser: http://localhost:8080
3. No authentication required in dev profile

**Security Configuration**:
- **Dev Profile**: Security disabled for easy testing
- **Production Profile**: OAuth2 JWT authentication required

**Database Configuration**:
- Uses MySQL with Hibernate auto-DDL (`ddl-auto: update`)
- Flyway disabled in dev profile
- Database auto-created if not exists: `createDatabaseIfNotExist=true`

**CORS Configuration**:
- Development: Allow all origins
- Production: Configure specific allowed origins

### Key Implementation Details

**Pagination**:
- Spring Data `Pageable` and `Page<T>` used for efficient data loading
- Frontend supports 10/20/50 items per page

**Date Handling**:
- Backend: `LocalDateTime` with `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`
- Frontend: ISO 8601 format for API calls, formatted display in UI

**Error Handling**:
- `GlobalExceptionHandler` (note: currently deleted, may need recreation)
- Proper HTTP status codes
- JSON error responses

**DTO Pattern**:
- Separate DTOs for API responses to decouple domain models from API contracts
- `@Builder` pattern for clean object construction
- Jackson annotations for JSON serialization

### Testing Recommendations

**Unit Tests**:
- Test service layer business logic
- Mock repository dependencies
- Test pagination logic

**Integration Tests**:
- Test REST endpoints with MockMvc
- Test database queries with @DataJpaTest
- Verify DTO serialization

**Frontend Tests**:
- Manual UI testing in browser
- Test API error handling
- Verify pagination and filtering

### Performance Considerations

- Database indexes on frequently queried columns (originalTimestamp, serverId, transactionId)
- Pagination to prevent loading large datasets
- Consider caching for statistics queries
- Frontend debouncing for search inputs

### Future Enhancements

- Add full-text search capability
- Export logs to CSV/Excel
- Real-time log streaming with WebSocket
- Advanced filtering with query builder
- Chart visualizations for statistics
- User preferences and saved filters

<!-- MANUAL ADDITIONS END -->
