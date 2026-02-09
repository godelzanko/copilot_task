---
stepsCompleted: ["step-01-validate-prerequisites", "epic-1-core-api-implementation", "story-3.4-completed"]
storiesCompleted: ["STORY-3.4"]
inputDocuments: 
  - "_bmad-output/planning-artifacts/PRD.md"
  - "_bmad-output/planning-artifacts/architecture.md"
---

# URL Shortener Service - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for the URL Shortener Service, decomposing the requirements from the PRD and Architecture documents into implementable stories.

### Epic Structure Rationale

**Note on Epic Organization:**  
This project uses a **layered architecture approach** to epic organization, with separate epics for API, ID Generation, Data Persistence, and Deployment layers. This structure prioritizes:

- **Educational clarity**: Each architectural layer is explicitly decomposed for learning purposes
- **Technical demonstration**: Shows understanding of system components and their interactions
- **Implementation independence**: Allows different developers to work on different layers

**Deviation from Standard Agile Best Practices:**  
Traditional agile methodology recommends user-value-focused epics (e.g., "As a user, I want to shorten and access URLs"). This project intentionally uses technical/infrastructure epics to demonstrate architectural thinking.

**Trade-offs Acknowledged:**
- ✅ **Benefit**: Clear separation of concerns, easier to understand system architecture
- ⚠️ **Trade-off**: Epics 2, 3, 4 don't directly deliver end-user value in isolation
- ⚠️ **Trade-off**: Epic 2 depends on Epic 3 (forward dependency violates epic independence)

**Recommendation for Production Teams:**  
For production agile teams, consider merging all technical work into user-facing epics (e.g., single epic "Shorten and Access URLs" containing all stories). This project optimizes for architectural demonstration over strict agile compliance.

## Requirements Inventory

### Functional Requirements

**FR-001: URL Shortening API Endpoint**
- Provide REST endpoint to create short URLs
- Endpoint: `POST /api/shorten`
- Request body: `{"url": "https://example.com/path"}`
- Response format: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`
- HTTP 200 OK on success, HTTP 400 Bad Request for invalid URLs
- Response includes both short code and full short URL for client convenience
- Use Java `URL()` class for validation
- Build full short URL using `ServletUriComponentsBuilder.fromCurrentContextPath()`

**FR-002: Redirect Endpoint**
- Redirect short URLs to original destinations
- Endpoint: `GET /{shortCode}`
- Returns HTTP 301 Moved Permanently on success
- Location header contains original URL
- HTTP 404 Not Found if short code doesn't exist
- No response body required (redirect only)
- Use `ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(uri).build()`
- Short code case-sensitive (Base62 encoding preserves case)

**FR-003: Snowflake-Based ID Generation**
- Generate collision-free short codes using Snowflake algorithm with Base62 encoding
- ID structure: 41-bit timestamp + 10-bit instance ID + 13-bit sequence counter
- Custom epoch: 2024-01-01T00:00:00Z
- Instance ID hardcoded to 0 (single-instance deployment)
- Base62 encoding: 0-9, a-z, A-Z (URL-safe, readable)
- Typical short code length: ~7 characters
- Thread-safe sequence counter (synchronized methods)
- Implement as Spring `@Component` singleton
- Base62 lookup table: `"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"`
- Sequence counter supports 8,192 IDs per millisecond per instance

**FR-004: Database-Enforced Idempotency**
- Guarantee same URL always maps to same short code using database constraints
- Normalized URL expression index: `LOWER(TRIM(original_url))`
- UNIQUE constraint prevents duplicate normalized URLs
- Try-insert-catch-select pattern handles concurrent requests gracefully
- No application-level locking required
- Case-insensitive and whitespace-insensitive URL matching
- Service layer tries INSERT, catches unique constraint violation, then SELECTs existing record
- Database atomically enforces business rule (one URL = one short code)

**FR-005: Minimal Database Schema**
- Three-column schema storing only essential data
- Table: `urls` with columns:
  - `short_code` VARCHAR(10) PRIMARY KEY (fast lookups for redirects)
  - `original_url` TEXT NOT NULL (supports very long URLs)
  - `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP (basic auditing)
- Unique normalized index on original URL: `CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))`
- No analytics columns, no expiration columns (out of scope for MVP)
- PostgreSQL TEXT type supports URLs up to 1GB (practically unlimited)

**FR-006: Docker-Based Deployment**
- Fully containerized deployment with orchestrated service startup
- Three-service architecture: postgres → liquibase → app
- Health check-driven dependency chain (sequential startup)
- PostgreSQL 16 Alpine (lightweight, production-grade)
- Liquibase runs migrations before app starts
- Multi-stage Dockerfile for optimized app image (~200MB)
- Single command deployment: `docker-compose up --build`
- Services:
  1. postgres: Database server with health checks (`pg_isready`)
  2. liquibase: One-shot migration runner (depends on postgres healthy, completes successfully)
  3. app: Spring Boot application (depends on liquibase completed, serves on port 3000)
- Maven build stage + JRE runtime stage (no build tools in production image)
- Volume mount for PostgreSQL data persistence
- Environment variables for Spring datasource configuration
- Bridge network for inter-service communication

### Non-Functional Requirements

**NFR-001: Redirect Latency**
- Sub-100ms average redirect response time
- Measurement: Database query time + HTTP redirect overhead
- 95th percentile under 100ms without caching layer
- PostgreSQL indexed lookup is sufficient; caching is premature optimization

**NFR-002: ID Generation Throughput**
- Generate 8,192 unique IDs per millisecond (per instance)
- Measurement: Snowflake 13-bit sequence counter capacity
- No sequence overflow under normal load
- Far exceeds realistic traffic for learning project

**NFR-003: URL Capacity**
- Support billions of URLs before timestamp exhaustion
- 41-bit timestamp with custom epoch 2024-01-01
- Service viable until ~2093 (69 years)
- Demonstrates understanding of Snowflake scalability properties

**NFR-004: Single-Instance Deployment**
- Hardcoded instance ID = 0 (no distributed coordination)
- Measurement: Deployment configuration
- Service runs on single Docker host
- Simplifies MVP; multi-instance can be added via configuration change

**NFR-005: Idempotency Guarantee**
- Same URL always returns same short code, even under concurrent requests
- Measurement: Integration tests with parallel POST requests
- 100% consistency across all concurrent requests
- Database UNIQUE constraint provides atomic enforcement

**NFR-006: Data Persistence**
- URL mappings survive service restarts
- Measurement: Docker volume persistence
- Data retained after `docker-compose down && docker-compose up`
- Demonstrates proper stateful service deployment

**NFR-007: Code Clarity**
- Code demonstrates educational concepts clearly
- Measurement: Code review, inline comments for complex logic
- New developer can understand architecture in <30 minutes
- Learning project prioritizes clarity over brevity

**NFR-008: Database Migration Management**
- All schema changes version-controlled via Liquibase
- Measurement: YAML changelog files in source control
- `docker-compose up` applies migrations automatically
- Demonstrates production-grade database change management

**NFR-009: One-Command Deployment**
- Service starts with single command: `docker-compose up --build`
- Measurement: Manual deployment test
- Fresh checkout to running service in <5 minutes
- Demonstrates proper containerized deployment practices

**NFR-010: Environment Independence**
- Service runs identically on any Docker-compatible host
- Measurement: Test on multiple operating systems (Linux, macOS, Windows with WSL2)
- No "works on my machine" issues
- Docker eliminates environment-specific configuration

### Additional Requirements

**Architecture-Specified Technical Requirements:**

- **Technology Stack:**
  - Java 21 (Modern LTS with virtual threads, pattern matching)
  - Spring Boot 3.2+ (Industry standard framework)
  - Maven 3.9+ (Build tool)
  - PostgreSQL 16 (ACID-compliant database)
  - Liquibase 4.25+ (Database migrations)
  - Docker 24.0+ (Containerization)
  - docker-compose 3.8+ (Multi-service orchestration)

- **Component Architecture:**
  - Controller Layer: Thin controllers for HTTP request/response handling
  - Service Layer: Business logic orchestration, URL normalization, idempotency enforcement
  - Generator Component: Thread-safe Snowflake ID generator with Base62 encoding
  - Repository Layer: Spring Data JPA for database CRUD operations

- **Snowflake ID Structure Details:**
  - 64-bit Long (Java long type)
  - 41 bits: Timestamp (milliseconds since custom epoch 2024-01-01)
  - 10 bits: Instance ID (hardcoded 0 for MVP)
  - 13 bits: Sequence counter (0-8191 per millisecond)
  - Base62 character set: `0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`
  - Typical output: 7 characters (e.g., `aB3xK9`)

- **Database Migration Management:**
  - Tool: Liquibase 4.25+
  - Format: YAML changelog files
  - Location: `src/main/resources/db/changelog/`
  - Execution: Automated during Docker Compose startup
  - Changesets: 001-create-urls-table, 002-create-normalized-url-index

- **Docker Multi-Service Architecture:**
  - Service dependency graph: postgres (healthy) → liquibase (completed) → app (healthy)
  - PostgreSQL service: postgres:16-alpine image, health check via `pg_isready`
  - Liquibase service: liquibase/liquibase:4.25-alpine, one-shot execution
  - App service: Custom multi-stage Dockerfile, port 8080 internal → 3000 external

- **Multi-Stage Dockerfile Strategy:**
  - Stage 1: Build with maven:3.9-eclipse-temurin-21 (dependency caching, offline build)
  - Stage 2: Runtime with eclipse-temurin:21-jre-alpine (minimal ~200MB vs ~700MB with JDK)

- **Security Considerations:**
  - URL validation via Java `URL()` class (prevents malformed URLs)
  - Spring Data JPA with parameterized queries (SQL injection prevention)
  - No rate limiting for MVP (acceptable for learning environment)
  - Database credentials via environment variables (dev environment only)

- **Testing Requirements:**
  - Unit tests: 100% coverage for business logic
  - Integration tests: Concurrent idempotency, end-to-end flow
  - Testcontainers for real database testing
  - Manual smoke tests post-deployment

### FR Coverage Map

| Functional Requirement | Epic(s) | Story Count |
|------------------------|---------|-------------|
| FR-001: URL Shortening API Endpoint | Epic 1: Core API Implementation | 2 |
| FR-002: Redirect Endpoint | Epic 1: Core API Implementation | 1 |
| FR-003: Snowflake-Based ID Generation | Epic 2: ID Generation System | 3 |
| FR-004: Database-Enforced Idempotency | Epic 2: ID Generation System | 2 |
| FR-005: Minimal Database Schema | Epic 3: Data Persistence Layer | 2 |
| FR-006: Docker-Based Deployment | Epic 4: Deployment & Infrastructure | 4 |

| Non-Functional Requirement | Epic(s) | Story Count |
|---------------------------|---------|-------------|
| NFR-001: Redirect Latency | Epic 1, Epic 3 | Validated via performance tests |
| NFR-002: ID Generation Throughput | Epic 2 | Validated via load tests |
| NFR-003: URL Capacity | Epic 2 | Design validation |
| NFR-004: Single-Instance Deployment | Epic 4 | Configuration validation |
| NFR-005: Idempotency Guarantee | Epic 2, Epic 3 | Integration tests |
| NFR-006: Data Persistence | Epic 3, Epic 4 | Docker volume tests |
| NFR-007: Code Clarity | All Epics | Code review criteria |
| NFR-008: Database Migration Management | Epic 3 | Liquibase changelog tests |
| NFR-009: One-Command Deployment | Epic 4 | Deployment validation |
| NFR-010: Environment Independence | Epic 4 | Multi-platform testing |

## Epic List

### Epic 1: Core API Implementation
**Goal:** Deliver RESTful HTTP endpoints for URL shortening and redirection  
**Business Value:** Enables clients to create and use short URLs  
**Dependencies:** None (first epic to implement)  
**Story Count:** 4  
**Estimated Effort:** 10-16 hours

### Epic 2: ID Generation System
**Goal:** Implement collision-free, deterministic short code generation  
**Business Value:** Ensures URL uniqueness and idempotency guarantees  
**Dependencies:** Epic 3 (requires database for idempotency enforcement)  
**Story Count:** 5  
**Estimated Effort:** 13-21 hours

### Epic 3: Data Persistence Layer
**Goal:** Establish database schema, migrations, and repository layer  
**Business Value:** Persistent storage with constraint-based integrity  
**Dependencies:** None (can be implemented in parallel with Epic 1)  
**Story Count:** 4  
**Estimated Effort:** 10-16 hours

### Epic 4: Deployment & Infrastructure
**Goal:** Create fully containerized, production-ready deployment configuration  
**Business Value:** One-command deployment with proper service orchestration  
**Dependencies:** Epics 1-3 (requires working application to deploy)  
**Story Count:** 5  
**Estimated Effort:** 10-16 hours

---

# Detailed Epic Breakdown

---

## Epic 1: Core API Implementation

### Epic Overview
**ID:** EPIC-001  
**Title:** Core API Implementation  
**Description:** Implement the REST API layer with Spring Boot controllers for URL shortening and redirection endpoints. This epic focuses on HTTP request/response handling, input validation, and proper HTTP status code usage.

**Success Criteria:**
- POST /api/shorten endpoint accepts JSON requests and returns short URL responses
- GET /{shortCode} endpoint performs HTTP 301 redirects to original URLs
- Invalid requests return appropriate 4xx status codes with error messages
- All endpoints follow RESTful conventions and Spring Boot best practices
- API contract matches specification from Architecture document

**Dependencies:**
- Spring Boot 3.2+ project initialized with Web and JPA starters
- Service layer interfaces defined (implementation can be stubbed initially)

**Risk Assessment:**
- **Low Risk:** Well-defined requirements, standard Spring Boot patterns
- **Mitigation:** Use Spring Initializr for proper project structure, follow official Spring guides

---

### Story 1.0: Initialize Spring Boot Project

**ID:** STORY-1.0  
**Title:** Create Spring Boot project structure  
**Priority:** High (Must be first)  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a developer
I want to initialize a Spring Boot project with all required dependencies
So that I have a working foundation to implement URL shortener features
```

**Acceptance Criteria:**

1. **Project Initialization**
   - [ ] Generate Spring Boot 3.2+ project using Spring Initializr (https://start.spring.io)
   - [ ] Group ID: `com.urlshortener`
   - [ ] Artifact ID: `url-shortener-service`
   - [ ] Java version: 21
   - [ ] Packaging: Jar
   - [ ] Build tool: Maven

2. **Required Dependencies**
   - [ ] Spring Web (REST API support)
   - [ ] Spring Data JPA (Database access)
   - [ ] PostgreSQL Driver (Database connectivity)
   - [ ] Liquibase Migration (Schema management)
   - [ ] Spring Boot Validation (Input validation)
   - [ ] Lombok (Optional: Reduces boilerplate)

3. **Project Structure**
   - [ ] Standard Maven directory structure created
   - [ ] Package structure: `com.urlshortener.{controller, service, repository, model, dto, config}`
   - [ ] `application.yml` configured with placeholders for database connection
   - [ ] Main application class with `@SpringBootApplication` annotation

4. **Build Verification**
   - [ ] `mvn clean compile` completes successfully
   - [ ] `mvn test` runs (even with no tests yet)
   - [ ] Application starts with `mvn spring-boot:run`
   - [ ] Default Spring Boot banner displayed on startup

5. **Basic Configuration**
   - [ ] `application.yml` includes:
     ```yaml
     spring:
       datasource:
         url: jdbc:postgresql://localhost:5432/urlshortener
         username: postgres
         password: postgres
       jpa:
         hibernate:
           ddl-auto: none
       liquibase:
         enabled: true
         change-log: classpath:db/changelog/db.changelog-master.yaml
     server:
       port: 8080
     ```
   - [ ] Liquibase changelog directory created: `src/main/resources/db/changelog/`
   - [ ] Empty master changelog file created

6. **Version Control**
   - [ ] `.gitignore` includes `target/`, `.idea/`, `*.iml`, `.DS_Store`
   - [ ] Initial commit with message "Initial Spring Boot project setup"

**Technical Notes:**
- Use Spring Initializr web interface or `curl` command for project generation
- Verify Java 21 is installed: `java -version`
- Verify Maven is installed: `mvn -version`
- Spring Boot DevTools optional for auto-restart during development

**Testing Requirements:**
- Manual test: Application starts without errors
- Manual test: Maven build completes successfully
- Manual test: IDE imports project without errors (IntelliJ IDEA or Eclipse)

**Definition of Done:**
- [ ] Project compiles without errors
- [ ] Application starts and shows Spring Boot banner
- [ ] All required dependencies in `pom.xml`
- [ ] Standard package structure created
- [ ] Code committed to version control

---

### Story 1.1: Create URL Shortening Endpoint

**ID:** STORY-1.1  
**Title:** Implement POST /api/shorten endpoint  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As an API client
I want to send a long URL to POST /api/shorten
So that I receive a short URL I can share
```

**Acceptance Criteria:**

1. **Endpoint Configuration**
   - [ ] Endpoint accepts POST requests at `/api/shorten`
   - [ ] Controller method annotated with `@PostMapping("/api/shorten")`
   - [ ] Consumes `application/json` Content-Type
   - [ ] Produces `application/json` Content-Type

2. **Request Handling**
   - [ ] Request body deserialized into DTO with single `url` field
   - [ ] DTO class: `ShortenRequest` with `@NotBlank String url` field
   - [ ] Spring validation annotations applied (`@Valid` on controller parameter)
   - [ ] Missing URL field returns HTTP 400 with error message

3. **URL Validation**
   - [ ] Java `URL()` class used to validate URL format
   - [ ] Invalid URL format returns HTTP 400 Bad Request
   - [ ] Error response body: `{"error": "Invalid URL format", "message": "URL must be a valid HTTP or HTTPS URL"}`
   - [ ] Only HTTP and HTTPS protocols accepted

4. **Success Response**
   - [ ] HTTP 200 OK status on successful shortening
   - [ ] Response body contains `shortCode` and `shortUrl` fields
   - [ ] Response DTO: `ShortenResponse` class with two String fields
   - [ ] `shortUrl` constructed using `ServletUriComponentsBuilder.fromCurrentContextPath()`
   - [ ] Example response: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`

5. **Service Layer Integration**
   - [ ] Controller delegates to `UrlShortenerService.shortenUrl(String url)` method
   - [ ] Service method returns DTO with short code and full URL
   - [ ] Controller performs no business logic (thin controller pattern)

6. **Error Handling**
   - [ ] `@ControllerAdvice` class handles exceptions globally
   - [ ] `MethodArgumentNotValidException` returns HTTP 400 with validation errors
   - [ ] Generic exceptions return HTTP 500 with safe error message
   - [ ] Stack traces not exposed in production error responses

**Technical Notes:**
- Use `@RestController` annotation for automatic JSON serialization
- DTO classes should be immutable records (Java 17+ feature)
- Validation errors should include field name and constraint violation message

**Testing Requirements:**
- Unit test: Valid URL returns 200 OK with expected response structure
- Unit test: Missing URL field returns 400 Bad Request
- Unit test: Invalid URL format returns 400 Bad Request
- Unit test: Service layer method invoked with correct parameter
- Integration test: End-to-end POST request with real Spring context

**Definition of Done:**
- [ ] Code implemented and passing all unit tests
- [ ] Integration test validates endpoint with TestRestTemplate
- [ ] Code reviewed for Spring Boot best practices
- [ ] JavaDoc comments added to controller methods
- [ ] Postman/curl example documented in README

---

### Story 1.2: Create Redirect Endpoint

**ID:** STORY-1.2  
**Title:** Implement GET /{shortCode} redirect endpoint  
**Priority:** High (Critical Path)  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As an end user
I want to navigate to a short URL like http://localhost:8080/aB3xK9
So that my browser is redirected to the original long URL
```

**Acceptance Criteria:**

1. **Endpoint Configuration**
   - [ ] Endpoint accepts GET requests at `/{shortCode}`
   - [ ] Controller method annotated with `@GetMapping("/{shortCode}")`
   - [ ] Path variable captured with `@PathVariable String shortCode`
   - [ ] No request body required

2. **Redirect Behavior**
   - [ ] Returns HTTP 301 Moved Permanently on successful lookup
   - [ ] Location header contains original URL
   - [ ] No response body sent (redirect only)
   - [ ] Uses `ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(originalUrl)).build()`

3. **Error Handling**
   - [ ] Returns HTTP 404 Not Found if short code doesn't exist
   - [ ] 404 response body: `{"error": "Short code not found", "message": "The requested short code does not exist"}`
   - [ ] Service layer throws `ShortCodeNotFoundException` (custom exception)
   - [ ] Exception handler maps custom exception to 404 response

4. **Service Layer Integration**
   - [ ] Controller delegates to `UrlShortenerService.getOriginalUrl(String shortCode)` method
   - [ ] Service method returns original URL string or throws exception
   - [ ] Controller performs URI construction and redirect logic only

5. **Case Sensitivity**
   - [ ] Short codes treated as case-sensitive (Base62 encoding preserves case)
   - [ ] `aB3xK9` and `AB3XK9` are different short codes
   - [ ] No case normalization applied

**Technical Notes:**
- Use `ResponseEntity<Void>` return type (no body for redirects)
- HTTP 301 allows browser caching (permanent redirect)
- Consider HTTP 302 for temporary redirects if needed in future

**Testing Requirements:**
- Unit test: Existing short code returns 301 redirect with correct Location header
- Unit test: Non-existent short code returns 404 Not Found
- Unit test: Case-sensitive short code handling
- Integration test: Browser-like redirect following with TestRestTemplate
- Integration test: 404 error response structure validation

**Definition of Done:**
- [ ] Code implemented and passing all unit tests
- [ ] Integration test validates redirect with real database lookup
- [ ] Browser manual test confirms redirect works end-to-end
- [ ] JavaDoc comments added to controller methods
- [ ] curl example showing redirect behavior documented

---

### Story 1.3: Implement Global Exception Handling

**ID:** STORY-1.3  
**Title:** Create centralized exception handler for API error responses  
**Priority:** Medium  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a developer
I want consistent error response formats across all API endpoints
So that clients can reliably parse and display error messages
```

**Acceptance Criteria:**

1. **Exception Handler Class**
   - [ ] Class annotated with `@ControllerAdvice` (global scope)
   - [ ] Class name: `GlobalExceptionHandler`
   - [ ] Package: `com.example.urlshortener.exception`

2. **Error Response DTO**
   - [ ] Immutable DTO class: `ErrorResponse` record with fields:
     - `String error` (error type/category)
     - `String message` (human-readable message)
     - `Instant timestamp` (when error occurred)
   - [ ] Consistent JSON structure across all error responses

3. **Validation Error Handling**
   - [ ] `@ExceptionHandler(MethodArgumentNotValidException.class)` method
   - [ ] Returns HTTP 400 Bad Request
   - [ ] Extracts field validation errors from `BindingResult`
   - [ ] Error message includes field name and violation message

4. **Custom Exception Handling**
   - [ ] `@ExceptionHandler(ShortCodeNotFoundException.class)` method
   - [ ] Returns HTTP 404 Not Found
   - [ ] Uses exception message in response body

5. **Generic Exception Handling**
   - [ ] `@ExceptionHandler(Exception.class)` method (catch-all)
   - [ ] Returns HTTP 500 Internal Server Error
   - [ ] Logs full stack trace (for debugging)
   - [ ] Returns safe generic message (no sensitive data exposed)

6. **HTTP Status Mapping**
   - [ ] Each handler method uses `@ResponseStatus` or `ResponseEntity` with correct status
   - [ ] Status codes match REST conventions (400 for client errors, 500 for server errors)

**Technical Notes:**
- Use SLF4J for logging (Spring Boot default)
- Never return stack traces in JSON responses (security risk)
- Consider using `@ResponseBody` with `@ExceptionHandler` for automatic JSON serialization

**Testing Requirements:**
- Unit test: Validation exception returns 400 with field errors
- Unit test: ShortCodeNotFoundException returns 404 with error message
- Unit test: Generic exception returns 500 with safe message
- Integration test: Invalid request triggers validation error response
- Integration test: Non-existent short code triggers 404 error response

**Definition of Done:**
- [ ] GlobalExceptionHandler class implemented
- [ ] All exception types handled with appropriate status codes
- [ ] Unit tests validate error response structure
- [ ] Integration tests confirm error handling in full Spring context
- [ ] Error response examples documented in API specification

---

## Epic 2: ID Generation System

### Epic Overview
**ID:** EPIC-002  
**Title:** ID Generation System  
**Description:** Implement the Snowflake ID generation algorithm with Base62 encoding to produce collision-free, URL-safe short codes. This epic includes thread-safe sequence management, custom epoch configuration, and database-enforced idempotency.

**Success Criteria:**
- SnowflakeIdGenerator produces unique 64-bit IDs with timestamp, instance, and sequence components
- Base62 encoding produces readable ~7-character short codes
- Generator supports 8,192 IDs per millisecond without collisions
- Database UNIQUE constraint prevents duplicate URLs
- Concurrent requests for same URL return same short code (idempotency)

**Dependencies:**
- Epic 3: Data Persistence Layer (requires database schema and repository)
- Spring Boot application context for component injection

**Risk Assessment:**
- **Medium Risk:** Complex thread-safe sequence counter, concurrency edge cases
- **Mitigation:** Comprehensive unit tests, integration tests with concurrent requests

---

### Story 2.1: Implement Snowflake ID Data Structure

**ID:** STORY-2.1  
**Title:** Create 64-bit Snowflake ID structure with bit-level operations  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a developer
I want a 64-bit Snowflake ID structure
So that I can generate time-sortable, collision-free identifiers
```

**Acceptance Criteria:**

1. **Bit Layout**
   - [ ] 64-bit Java `long` type used for ID
   - [ ] Bit allocation: 41 bits timestamp + 10 bits instance ID + 13 bits sequence
   - [ ] Timestamp: milliseconds since custom epoch (2024-01-01T00:00:00Z)
   - [ ] Instance ID: hardcoded 0 for MVP (supports 0-1023 range)
   - [ ] Sequence: 0-8191 counter (resets every millisecond)

2. **Custom Epoch**
   - [ ] Constant: `EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()`
   - [ ] Current timestamp calculated as `System.currentTimeMillis() - EPOCH`
   - [ ] Supports 69 years from epoch (2^41 milliseconds)

3. **Bit Shifting Operations**
   - [ ] Timestamp shifted left 22 bits (`timestamp << 22`)
   - [ ] Instance ID shifted left 12 bits (`instanceId << 12`)
   - [ ] Sequence occupies low 12 bits (no shift)
   - [ ] Final ID: `(timestamp << 22) | (instanceId << 12) | sequence`

4. **Component Extraction**
   - [ ] Helper method: `extractTimestamp(long id)` returns 41-bit timestamp
   - [ ] Helper method: `extractInstanceId(long id)` returns 10-bit instance ID
   - [ ] Helper method: `extractSequence(long id)` returns 13-bit sequence
   - [ ] Extraction uses right-shift and bit masking

5. **Validation**
   - [ ] Timestamp cannot exceed 41 bits (max value validation)
   - [ ] Instance ID cannot exceed 10 bits (0-1023 range)
   - [ ] Sequence cannot exceed 13 bits (0-8191 range)
   - [ ] Overflow detection throws `IllegalStateException`

**Technical Notes:**
- Use `>>>` (unsigned right shift) for extraction to avoid sign extension
- Consider extracting bit constants: `TIMESTAMP_BITS = 41`, `INSTANCE_ID_BITS = 10`, `SEQUENCE_BITS = 12`
- Document bit layout in JavaDoc with ASCII diagram

**Testing Requirements:**
- Unit test: ID generation with known timestamp/instance/sequence produces expected 64-bit value
- Unit test: Component extraction returns correct values
- Unit test: Bit overflow throws exception
- Unit test: Epoch calculation matches expected milliseconds since 2024-01-01
- Unit test: Maximum timestamp value (2^41 - 1) is valid

**Definition of Done:**
- [ ] Snowflake ID structure implemented with bit operations
- [ ] Unit tests validate bit layout and component extraction
- [ ] JavaDoc documents bit layout with examples
- [ ] Constants extracted for maintainability

---

### Story 2.2: Implement Thread-Safe Sequence Counter

**ID:** STORY-2.2  
**Title:** Create synchronized sequence counter with overflow handling  
**Priority:** High (Critical Path)  
**Estimated Effort:** 4-6 hours

**User Story:**
```
As a system
I want a thread-safe sequence counter
So that I can generate multiple IDs per millisecond without collisions
```

**Acceptance Criteria:**

1. **State Management**
   - [ ] Instance variable: `private long lastTimestamp = -1L`
   - [ ] Instance variable: `private long sequence = 0L`
   - [ ] Both variables protected by `synchronized` methods

2. **Sequence Increment Logic**
   - [ ] If `currentTimestamp == lastTimestamp`: increment sequence
   - [ ] If `currentTimestamp > lastTimestamp`: reset sequence to 0
   - [ ] If `currentTimestamp < lastTimestamp`: throw `ClockMovedBackwardsException` (clock skew)

3. **Sequence Overflow Handling**
   - [ ] When sequence reaches 8191 (max 13-bit value), wait for next millisecond
   - [ ] Busy-wait loop: `while (currentTimestamp == lastTimestamp) { currentTimestamp = System.currentTimeMillis() }`
   - [ ] After wait, reset sequence to 0
   - [ ] Overflow rare (requires 8192 IDs in 1ms)

4. **Clock Backwards Detection**
   - [ ] Custom exception: `ClockMovedBackwardsException extends RuntimeException`
   - [ ] Thrown when system clock moves backwards (NTP adjustment, daylight saving)
   - [ ] Exception message includes old and new timestamps

5. **Thread Safety**
   - [ ] `synchronized` keyword on ID generation method
   - [ ] Ensures only one thread modifies sequence at a time
   - [ ] Performance acceptable for MVP (lock contention unlikely)

**Technical Notes:**
- Alternative: Use `ReentrantLock` for explicit lock control
- Consider adding metric: sequence resets per second (overflow frequency)
- Busy-wait acceptable for rare overflows; sleep() would introduce unnecessary latency

**Testing Requirements:**
- Unit test: Single-threaded ID generation increments sequence correctly
- Unit test: Timestamp change resets sequence to 0
- Unit test: Clock backwards throws exception
- Unit test (multi-threaded): 100 threads generating IDs concurrently produce unique values
- Unit test (load): Generate 10,000 IDs rapidly, verify no duplicates

**Definition of Done:**
- [ ] Sequence counter implemented with synchronized methods
- [ ] Clock backwards detection working
- [ ] Multi-threaded tests pass without collisions
- [ ] JavaDoc explains sequence overflow handling

---

### Story 2.3: Implement Base62 Encoding

**ID:** STORY-2.3  
**Title:** Convert Snowflake IDs to Base62-encoded short codes  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a system
I want to encode 64-bit IDs into Base62 strings
So that short codes are URL-safe, readable, and compact
```

**Acceptance Criteria:**

1. **Character Set**
   - [x] Constant: `private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"`
   - [x] 62 characters total (0-9, a-z, A-Z)
   - [x] Case-sensitive encoding

2. **Encoding Algorithm**
   - [x] Method signature: `public String encode(long id)`
   - [x] Division-remainder algorithm:
     ```java
     while (id > 0) {
         result = ALPHABET.charAt(id % 62) + result;
         id = id / 62;
     }
     ```
   - [x] Handles `id = 0` as special case (returns "0")

3. **Output Format**
   - [x] Typical output length: 7 characters (for realistic IDs)
   - [x] No padding (variable length based on ID value)
   - [x] No special characters, spaces, or delimiters

4. **Decoding (Optional)**
   - [x] Method signature: `public long decode(String encoded)`
   - [x] Reverse algorithm: `result = result * 62 + ALPHABET.indexOf(char)`
   - [x] Used for validation tests (not required by API)

5. **Edge Cases**
   - [x] ID = 0 returns "0"
   - [x] ID = 61 returns "z"
   - [x] ID = 62 returns "10"
   - [x] Negative IDs throw `IllegalArgumentException`

**Technical Notes:**
- Base62 produces shorter codes than Base36 (with numbers + lowercase only)
- Case sensitivity requires careful handling in URLs (most browsers preserve case)
- Consider using `StringBuilder` for efficient string concatenation

**Testing Requirements:**
- Unit test: Known ID values produce expected Base62 strings
- Unit test: Encode-decode round trip returns original ID
- Unit test: ID = 0 edge case
- Unit test: Large ID (near 2^63) encodes without error
- Unit test: Negative ID throws exception

**Definition of Done:**
- [x] Base62 encoding implemented
- [x] Decode method implemented for testing
- [x] Unit tests validate encoding correctness
- [x] JavaDoc includes encoding examples
- [x] Overflow bug fixed in decode method

---

### Story 2.4: Create SnowflakeIdGenerator Spring Component

**ID:** STORY-2.4  
**Title:** Integrate Snowflake ID generation into Spring application context  
**Priority:** High (Critical Path)  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a service layer
I want a Spring-managed SnowflakeIdGenerator bean
So that I can inject it and generate short codes on demand
```

**Acceptance Criteria:**

1. **Component Configuration**
   - [ ] Class annotated with `@Component`
   - [ ] Package: `com.example.urlshortener.generator`
   - [ ] Class name: `SnowflakeIdGenerator`
   - [ ] Singleton scope (Spring default)

2. **Public API**
   - [ ] Method: `public String generateShortCode()`
   - [ ] Returns Base62-encoded short code
   - [ ] Combines Snowflake ID generation + Base62 encoding

3. **Internal Structure**
   - [ ] Private instance ID field: `private final long instanceId = 0L`
   - [ ] Constant custom epoch
   - [ ] Sequence counter state variables
   - [ ] Base62 alphabet constant

4. **Dependency Injection**
   - [ ] Constructor with no parameters (default instance ID = 0)
   - [ ] Alternative constructor: `public SnowflakeIdGenerator(long instanceId)` for future multi-instance support
   - [ ] No external dependencies (self-contained component)

5. **Logging**
   - [ ] SLF4J logger: `private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class)`
   - [ ] Log warning on sequence overflow (rare event)
   - [ ] Log error on clock backwards detection

**Technical Notes:**
- Consider extracting interface `IdGenerator` for future algorithm swaps
- Instance ID could be externalized to `application.properties` for multi-instance deployments
- Avoid constructor injection for stateful singleton (no circular dependency risk)

**Testing Requirements:**
- Unit test: Bean auto-wired in Spring context
- Unit test: `generateShortCode()` returns valid Base62 string
- Integration test: Service layer injects generator and creates short codes
- Integration test: Multiple generator method calls produce unique codes

**Definition of Done:**
- [ ] SnowflakeIdGenerator annotated as Spring component
- [ ] Public `generateShortCode()` method working
- [ ] Unit tests validate Spring integration
- [ ] Component successfully injected into service layer

---

### Story 2.5: Implement Database-Enforced Idempotency

**ID:** STORY-2.5  
**Title:** Ensure same URL always returns same short code via UNIQUE constraint  
**Priority:** High (Critical Path)  
**Estimated Effort:** 4-6 hours

**User Story:**
```
As a system
I want the database to enforce URL uniqueness
So that concurrent requests for the same URL return identical short codes
```

**Acceptance Criteria:**

1. **Service Layer Logic**
   - [ ] Method: `@Transactional public UrlDto shortenUrl(String originalUrl)`
   - [ ] Normalize URL: `String normalized = originalUrl.trim().toLowerCase()`
   - [ ] Try-insert-catch-select pattern:
     ```java
     try {
         String shortCode = generator.generateShortCode();
         UrlEntity entity = new UrlEntity(shortCode, normalized);
         urlRepository.save(entity);
         return toDto(entity);
     } catch (DataIntegrityViolationException e) {
         UrlEntity existing = urlRepository.findByNormalizedUrl(normalized);
         return toDto(existing);
     }
     ```

2. **URL Normalization**
   - [ ] Trim whitespace: `url.trim()`
   - [ ] Convert to lowercase: `url.toLowerCase()`
   - [ ] Order: trim first, then lowercase
   - [ ] Applied before database operations

3. **Database Constraint**
   - [ ] UNIQUE index on normalized expression: `LOWER(TRIM(original_url))`
   - [ ] Database enforces atomicity (no application-level locks)
   - [ ] Constraint violation triggers `DataIntegrityViolationException`

4. **Concurrency Handling**
   - [ ] Multiple threads requesting same URL: first wins, others catch exception
   - [ ] No race condition: database guarantees uniqueness
   - [ ] All concurrent requests eventually return same short code

5. **Repository Method**
   - [ ] Custom query: `Optional<UrlEntity> findByNormalizedUrl(String normalizedUrl)`
   - [ ] Query: `SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = :normalizedUrl`
   - [ ] Used in catch block to retrieve existing mapping

**Technical Notes:**
- `@Transactional` ensures atomic try-insert-catch-select operation
- Normalized URL index supports fast lookups (O(log n))
- Exception-driven flow acceptable for idempotency enforcement

**Testing Requirements:**
- Unit test: Same URL (different case/whitespace) returns same short code
- Integration test: 10 concurrent threads requesting same URL get identical short code
- Integration test: Different URLs get different short codes
- Integration test: Database constraint violation logged correctly

**Definition of Done:**
- [ ] Try-insert-catch-select pattern implemented in service layer
- [ ] URL normalization working correctly
- [ ] Repository custom query implemented
- [ ] Concurrent integration tests passing
- [ ] JavaDoc explains idempotency strategy

---

## Epic 3: Data Persistence Layer

### Epic Overview
**ID:** EPIC-003  
**Title:** Data Persistence Layer  
**Description:** Establish the database schema with PostgreSQL, implement Spring Data JPA repositories, and configure Liquibase for database migration management. This epic ensures persistent, constraint-enforced storage of URL mappings.

**Success Criteria:**
- PostgreSQL database schema created with urls table and normalized URL index
- Spring Data JPA repository provides CRUD operations for UrlEntity
- Liquibase changelog manages schema migrations in version control
- Database constraints enforce business rules (unique URLs, non-null fields)
- Data persists across application restarts (Docker volume)

**Dependencies:**
- PostgreSQL 16 running (can use Docker for local development)
- Spring Boot project configured with Spring Data JPA and PostgreSQL driver

**Risk Assessment:**
- **Low Risk:** Well-established patterns, Spring Data JPA documentation comprehensive
- **Mitigation:** Use Liquibase from start (avoid ad-hoc schema changes)

---

### Story 3.1: Design and Create Database Schema

**ID:** STORY-3.1  
**Title:** Define urls table schema with primary key and unique constraint  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a database
I want a minimal schema optimized for URL lookups
So that I can store and retrieve URL mappings efficiently
```

**Acceptance Criteria:**

1. **Table Definition**
   - [ ] Table name: `urls`
   - [ ] Columns:
     - `short_code VARCHAR(10) PRIMARY KEY` (Base62 short code, max 10 chars for safety)
     - `original_url TEXT NOT NULL` (supports unlimited URL length)
     - `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP` (audit trail)

2. **Primary Key Index**
   - [ ] Primary key on `short_code`
   - [ ] Automatically creates B-tree index for O(log n) lookups
   - [ ] Supports fast redirect queries: `SELECT original_url FROM urls WHERE short_code = ?`

3. **Unique Constraint on Normalized URL**
   - [ ] Index name: `idx_original_url_normalized`
   - [ ] Expression: `CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))`
   - [ ] Prevents duplicate URLs with different case/whitespace
   - [ ] Enforces idempotency at database level

4. **Column Specifications**
   - [ ] `short_code`: VARCHAR(10) chosen for flexibility (typical length ~7 chars)
   - [ ] `original_url`: TEXT type supports very long URLs (up to 1GB in PostgreSQL)
   - [ ] `created_at`: TIMESTAMP without timezone (UTC assumed)

5. **No Foreign Keys**
   - [ ] Single-table design (no relationships)
   - [ ] Simplifies schema, no cascade delete complexity

**Technical Notes:**
- PostgreSQL expression indexes allow constraints on computed values
- UNIQUE constraint throws error on duplicate, enabling try-catch pattern
- Consider adding `updated_at` column in future for expiration feature

**Testing Requirements:**
- Manual test: Table created successfully via psql
- Manual test: Insert duplicate normalized URL throws constraint violation
- Manual test: Primary key index used in query plan (EXPLAIN ANALYZE)

**Definition of Done:**
- [ ] SQL schema definition documented
- [ ] Schema tested in local PostgreSQL instance
- [ ] Index query plan validated
- [ ] Schema ready for Liquibase migration

---

### Story 3.2: Create Liquibase Migration Changelog

**ID:** STORY-3.2  
**Title:** Define database migrations in YAML changelog files  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a developer
I want database schema changes version-controlled in Liquibase
So that I can safely evolve the schema over time
```

**Acceptance Criteria:**

1. **Changelog File Structure**
   - [x] Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
   - [x] Includes individual changesets:
     - `001-create-urls-table.yaml`
     - `002-create-normalized-url-index.yaml`

2. **Changeset 001: Create urls Table**
   - [x] Changeset ID: `001-create-urls-table`
   - [x] Author: `developer`
   - [x] Changes:
     ```yaml
     changes:
       - createTable:
           tableName: urls
           columns:
             - column:
                 name: short_code
                 type: VARCHAR(10)
                 constraints:
                   primaryKey: true
                   nullable: false
             - column:
                 name: original_url
                 type: TEXT
                 constraints:
                   nullable: false
             - column:
                 name: created_at
                 type: TIMESTAMP
                 defaultValueComputed: CURRENT_TIMESTAMP
     ```

3. **Changeset 002: Create Normalized Index**
   - [x] Changeset ID: `002-create-normalized-url-index`
   - [x] Author: `developer`
   - [x] Changes:
     ```yaml
     changes:
       - sql:
           sql: CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))
     ```
   - [x] Uses raw SQL (Liquibase YAML doesn't support expression indexes)

4. **Rollback Configuration**
   - [x] Rollback for changeset 001:
     ```yaml
     rollback:
       - dropTable:
           tableName: urls
     ```
   - [x] Rollback for changeset 002:
     ```yaml
     rollback:
       - sql:
           sql: DROP INDEX IF EXISTS idx_original_url_normalized
     ```

5. **Master Changelog**
   - [x] Includes both changesets in order:
     ```yaml
     databaseChangeLog:
       - include:
           file: db/changelog/001-create-urls-table.yaml
       - include:
           file: db/changelog/002-create-normalized-url-index.yaml
     ```

**Technical Notes:**
- Liquibase tracks applied changesets in `DATABASECHANGELOG` table
- Changesets applied once (idempotent by default)
- YAML format preferred over XML for readability

**Testing Requirements:**
- Manual test: Run `liquibase update` against local PostgreSQL
- Manual test: Verify `DATABASECHANGELOG` table contains both changesets
- Manual test: Run `liquibase rollback` to version 0, verify table dropped
- Integration test: Spring Boot application applies migrations on startup

**Definition of Done:**
- [x] Liquibase changelog files created
- [x] Migrations tested manually with Liquibase CLI
- [x] Rollback commands tested
- [x] Changelog committed to version control

---

### Story 3.3: Implement JPA Entity and Repository

**ID:** STORY-3.3  
**Title:** Create UrlEntity and UrlRepository for database operations  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a service layer
I want a JPA repository for URL entities
So that I can perform database operations without writing SQL
```

**Acceptance Criteria:**

1. **Entity Class**
   - [ ] Class: `UrlEntity` in package `com.example.urlshortener.entity`
   - [ ] Annotations:
     ```java
     @Entity
     @Table(name = "urls")
     public class UrlEntity {
         @Id
         @Column(name = "short_code", length = 10)
         private String shortCode;
         
         @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
         private String originalUrl;
         
         @Column(name = "created_at", nullable = false, updatable = false)
         @CreationTimestamp
         private Instant createdAt;
     }
     ```
   - [ ] All-args constructor, no-args constructor, getters, setters

2. **Repository Interface**
   - [ ] Interface: `UrlRepository extends JpaRepository<UrlEntity, String>`
   - [ ] Package: `com.example.urlshortener.repository`
   - [ ] Spring Data JPA provides CRUD methods automatically

3. **Custom Query Method**
   - [ ] Method signature: `Optional<UrlEntity> findByOriginalUrlIgnoreCaseAndTrim(String originalUrl)`
   - [ ] Alternative: Use custom `@Query`:
     ```java
     @Query("SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = LOWER(TRIM(:url))")
     Optional<UrlEntity> findByNormalizedUrl(@Param("url") String url);
     ```
   - [ ] Used for idempotency lookup

4. **Data Transfer Object (DTO)**
   - [ ] Record: `UrlDto(String shortCode, String shortUrl)` in package `com.example.urlshortener.dto`
   - [ ] Immutable, used for API responses
   - [ ] Mapper method: `UrlDto toDto(UrlEntity entity, String baseUrl)`

5. **Spring Data JPA Configuration**
   - [ ] `application.properties` or `application.yml`:
     ```yaml
     spring:
       jpa:
         hibernate:
           ddl-auto: validate  # Validate schema matches entities (Liquibase handles migrations)
         show-sql: true  # Log SQL for development
     ```

**Technical Notes:**
- Use `@CreationTimestamp` for automatic created_at population
- Hibernate `ddl-auto: validate` prevents auto-schema generation (Liquibase controls schema)
- Consider Lombok `@Data` annotation to reduce boilerplate

**Testing Requirements:**
- Unit test (with Testcontainers): Save UrlEntity and retrieve by short code
- Unit test: Custom query method finds entity by normalized URL
- Unit test: Duplicate short code throws constraint violation
- Integration test: Repository auto-wired in Spring context

**Definition of Done:**
- [ ] UrlEntity class implemented with JPA annotations
- [ ] UrlRepository interface created
- [ ] Custom query method implemented
- [ ] Unit tests passing with Testcontainers PostgreSQL
- [ ] Spring Data JPA configuration validated

---

### Story 3.4: Configure Spring Data JPA and PostgreSQL Connection

**ID:** STORY-3.4  
**Title:** Set up Spring Boot application to connect to PostgreSQL database  
**Priority:** High (Critical Path)  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a Spring Boot application
I want to connect to a PostgreSQL database
So that I can persist and retrieve URL mappings
```

**Acceptance Criteria:**

1. **Maven Dependencies**
   - [ ] Add to `pom.xml`:
     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-data-jpa</artifactId>
     </dependency>
     <dependency>
         <groupId>org.postgresql</groupId>
         <artifactId>postgresql</artifactId>
         <scope>runtime</scope>
     </dependency>
     <dependency>
         <groupId>org.liquibase</groupId>
         <artifactId>liquibase-core</artifactId>
     </dependency>
     ```

2. **Application Configuration**
   - [ ] File: `src/main/resources/application.yml`
   - [ ] Configuration:
     ```yaml
     spring:
       datasource:
         url: jdbc:postgresql://localhost:5432/urlshortener
         username: urlshortener
         password: urlshortener_pass
         driver-class-name: org.postgresql.Driver
       jpa:
         hibernate:
           ddl-auto: validate
         show-sql: true
         properties:
           hibernate:
             format_sql: true
       liquibase:
         enabled: false  # Disabled when using separate Liquibase service
     ```

3. **Environment Variable Overrides**
   - [ ] Support environment variables for Docker deployment:
     - `SPRING_DATASOURCE_URL`
     - `SPRING_DATASOURCE_USERNAME`
     - `SPRING_DATASOURCE_PASSWORD`
   - [ ] Spring Boot auto-binds environment variables to properties

4. **Connection Pooling**
   - [ ] Use HikariCP (Spring Boot default)
   - [ ] Default pool size: 10 connections (sufficient for MVP)
   - [ ] No custom configuration required

5. **Health Check**
   - [ ] Spring Boot Actuator dependency (optional):
     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-actuator</artifactId>
     </dependency>
     ```
   - [ ] Endpoint: `GET /actuator/health` shows database connection status

**Technical Notes:**
- Disable Liquibase in Spring Boot when using separate Liquibase container
- Use `${ENV_VAR:default}` syntax for environment variable fallback
- Consider externalizing credentials to Docker secrets in production

**Testing Requirements:**
- Manual test: Application starts and connects to local PostgreSQL
- Manual test: JPA repository methods execute queries successfully
- Integration test: Testcontainers spins up PostgreSQL and validates connection
- Integration test: Health check endpoint returns UP status

**Definition of Done:**
- [ ] PostgreSQL driver and Spring Data JPA dependencies added
- [ ] application.yml configured with database connection
- [ ] Application successfully connects to PostgreSQL on startup
- [ ] Health check validates database connectivity

---

## Epic 4: Deployment & Infrastructure

### Epic Overview
**ID:** EPIC-004  
**Title:** Deployment & Infrastructure  
**Description:** Create a fully containerized deployment solution using Docker and docker-compose. This epic includes multi-stage Dockerfile for the Spring Boot application, PostgreSQL database container, Liquibase migration runner, and orchestrated service startup with health checks.

**Success Criteria:**
- Single command `docker-compose up --build` deploys entire stack
- Services start in correct order: postgres → liquibase → app
- Health checks ensure readiness before dependent services start
- PostgreSQL data persists in Docker volume across restarts
- Application accessible on http://localhost:3000
- Environment variables configure database connection

**Dependencies:**
- Epics 1-3 complete (working application with database schema)
- Docker 24.0+ and docker-compose installed

**Risk Assessment:**
- **Low Risk:** Docker well-documented, standard patterns available
- **Mitigation:** Test on multiple platforms (Linux, macOS, Windows WSL2)

---

### Story 4.1: Create Multi-Stage Dockerfile for Spring Boot Application

**ID:** STORY-4.1  
**Title:** Build optimized Docker image with build and runtime stages  
**Priority:** High (Critical Path)  
**Estimated Effort:** 3-5 hours

**User Story:**
```
As a deployment engineer
I want a minimal Docker image for the Spring Boot application
So that deployments are fast and images are secure
```

**Acceptance Criteria:**

1. **Build Stage**
   - [ ] Base image: `FROM maven:3.9-eclipse-temurin-21 AS build`
   - [ ] Working directory: `WORKDIR /app`
   - [ ] Copy dependency file: `COPY pom.xml .`
   - [ ] Download dependencies: `RUN mvn dependency:go-offline -B`
   - [ ] Copy source code: `COPY src ./src`
   - [ ] Build JAR: `RUN mvn clean package -DskipTests`
   - [ ] Skip tests in Docker build (run separately in CI/CD)

2. **Runtime Stage**
   - [ ] Base image: `FROM eclipse-temurin:21-jre-alpine`
   - [ ] Working directory: `WORKDIR /app`
   - [ ] Copy JAR from build stage: `COPY --from=build /app/target/*.jar app.jar`
   - [ ] Expose port: `EXPOSE 8080`
   - [ ] Entrypoint: `ENTRYPOINT ["java", "-jar", "app.jar"]`

3. **Image Optimization**
   - [ ] Build stage not included in final image (multi-stage pattern)
   - [ ] Final image size: ~200MB (JRE + JAR only)
   - [ ] No Maven, no source code in final image
   - [ ] Alpine Linux base (lightweight, secure)

4. **Dependency Caching**
   - [ ] Separate COPY for `pom.xml` enables layer caching
   - [ ] Dependencies re-downloaded only when `pom.xml` changes
   - [ ] Speeds up iterative builds

5. **JVM Configuration (Optional)**
   - [ ] Consider adding JVM flags:
     ```dockerfile
     ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
     ```
   - [ ] Container-aware JVM flags optimize memory usage

**Technical Notes:**
- Alpine image smaller than Debian-based images (~100MB vs ~300MB)
- Maven offline mode (`-B`) ensures reproducible builds
- Consider adding non-root user for security (future enhancement)

**Testing Requirements:**
- Manual test: Build image with `docker build -t url-shortener .`
- Manual test: Run container with `docker run -p 8080:8080 url-shortener`
- Manual test: Verify application starts and responds to health check
- Manual test: Inspect image size with `docker images`

**Definition of Done:**
- [ ] Dockerfile created with multi-stage build
- [ ] Image builds successfully
- [ ] Container runs and application starts
- [ ] Image size optimized (<250MB)
- [ ] Dockerfile commented for clarity

---

### Story 4.2: Create docker-compose Configuration

**ID:** STORY-4.2  
**Title:** Define multi-service orchestration with postgres, liquibase, and app  
**Priority:** High (Critical Path)  
**Estimated Effort:** 4-6 hours

**User Story:**
```
As a developer
I want a single command to start the entire application stack
So that I can quickly test the full system locally
```

**Acceptance Criteria:**

1. **PostgreSQL Service**
   - [ ] Service name: `postgres`
   - [ ] Image: `postgres:16-alpine`
   - [ ] Environment variables:
     ```yaml
     environment:
       POSTGRES_DB: urlshortener
       POSTGRES_USER: urlshortener
       POSTGRES_PASSWORD: urlshortener_pass
     ```
   - [ ] Health check:
     ```yaml
     healthcheck:
       test: ["CMD-SHELL", "pg_isready -U urlshortener -d urlshortener"]
       interval: 10s
       timeout: 5s
       retries: 5
     ```
   - [ ] Volume: `postgres_data:/var/lib/postgresql/data`
   - [ ] Port: `5432:5432` (for local access)

2. **Liquibase Service**
   - [ ] Service name: `liquibase`
   - [ ] Image: `liquibase/liquibase:4.25-alpine`
   - [ ] Depends on: `postgres` (condition: `service_healthy`)
   - [ ] Command:
     ```yaml
     command:
       - --changelog-file=db/changelog/db.changelog-master.yaml
       - --driver=org.postgresql.Driver
       - --url=jdbc:postgresql://postgres:5432/urlshortener
       - --username=urlshortener
       - --password=urlshortener_pass
       - update
     ```
   - [ ] Volume mount: `./src/main/resources/db/changelog:/liquibase/changelog`
   - [ ] Restart policy: `on-failure` (exits after migrations complete)

3. **Application Service**
   - [ ] Service name: `app`
   - [ ] Build context: `.`
   - [ ] Dockerfile: `./Dockerfile`
   - [ ] Depends on: `liquibase` (condition: `service_completed_successfully`)
   - [ ] Environment variables:
     ```yaml
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urlshortener
       SPRING_DATASOURCE_USERNAME: urlshortener
       SPRING_DATASOURCE_PASSWORD: urlshortener_pass
       SERVER_PORT: 8080
       SPRING_LIQUIBASE_ENABLED: false
     ```
   - [ ] Port: `3000:8080` (external:internal)

4. **Network Configuration**
   - [ ] Custom bridge network: `url-shortener-network`
   - [ ] All services on same network for inter-service communication

5. **Volume Configuration**
   - [ ] Named volume: `postgres_data` for database persistence

**Technical Notes:**
- Dependency conditions: `service_healthy`, `service_completed_successfully` ensure correct startup order
- Liquibase exits after migrations (expected behavior)
- Port 3000 chosen for external access (avoids conflict with common dev servers on 8080)

**Testing Requirements:**
- Manual test: `docker-compose up --build` starts all services
- Manual test: Application accessible at http://localhost:3000
- Manual test: Database migrations applied (check DATABASECHANGELOG table)
- Manual test: `docker-compose down && docker-compose up` preserves data

**Definition of Done:**
- [ ] docker-compose.yml created
- [ ] All services start in correct order
- [ ] Health checks working
- [ ] Application responds to HTTP requests
- [ ] Data persists across restarts

---

### Story 4.3: Configure Service Health Checks and Readiness Probes

**ID:** STORY-4.3  
**Title:** Implement health checks for all services in Docker stack  
**Priority:** Medium  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a docker-compose orchestrator
I want health checks for each service
So that I can determine when services are ready for traffic
```

**Acceptance Criteria:**

1. **PostgreSQL Health Check**
   - [ ] Command: `pg_isready -U urlshortener -d urlshortener`
   - [ ] Interval: 10 seconds
   - [ ] Timeout: 5 seconds
   - [ ] Retries: 5 (marks unhealthy after 50 seconds)
   - [ ] Start period: 10 seconds (grace period before first check)

2. **Application Health Check**
   - [ ] Spring Boot Actuator endpoint: `/actuator/health`
   - [ ] Health check command:
     ```yaml
     healthcheck:
       test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
       interval: 30s
       timeout: 10s
       retries: 3
       start_period: 60s
     ```
   - [ ] Requires `wget` in Alpine image (install in Dockerfile):
     ```dockerfile
     RUN apk add --no-cache wget
     ```

3. **Health Check Status Visibility**
   - [ ] `docker-compose ps` shows health status
   - [ ] Unhealthy services restart automatically (Docker default behavior)

4. **Dependency Chain Validation**
   - [ ] postgres must be healthy before liquibase starts
   - [ ] liquibase must complete successfully before app starts
   - [ ] app must be healthy before accepting traffic

5. **Startup Logs**
   - [ ] Health check failures logged to console
   - [ ] Service readiness logged: "Listening on port 8080"

**Technical Notes:**
- `wget --spider` checks URL without downloading content
- `start_period` prevents false negatives during slow startup
- Consider using `curl` instead of `wget` if smaller image size needed

**Testing Requirements:**
- Manual test: `docker-compose ps` shows all services healthy
- Manual test: Stop postgres, verify app becomes unhealthy
- Manual test: Slow database startup delays app start correctly
- Manual test: Health check endpoint returns 200 OK

**Definition of Done:**
- [ ] Health checks configured for postgres and app
- [ ] Dependency chain enforced by health check conditions
- [ ] Health status visible in docker-compose ps
- [ ] Health check failures trigger restarts

---

### Story 4.4: Create README with Deployment Instructions

**ID:** STORY-4.4  
**Title:** Document one-command deployment process in README.md  
**Priority:** Medium  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a new developer
I want clear instructions to run the application locally
So that I can get started quickly without guessing
```

**Acceptance Criteria:**

1. **Prerequisites Section**
   - [ ] List required software:
     - Docker 24.0+ (installation link)
     - docker-compose 3.8+ (usually bundled with Docker)
   - [ ] Verify installation commands:
     ```bash
     docker --version
     docker-compose --version
     ```

2. **Quick Start Section**
   - [ ] One-command deployment:
     ```bash
     docker-compose up --build
     ```
   - [ ] Application URL: http://localhost:3000
   - [ ] Stop command: `Ctrl+C` or `docker-compose down`

3. **API Usage Examples**
   - [ ] Shorten URL example:
     ```bash
     curl -X POST http://localhost:3000/api/shorten \
       -H "Content-Type: application/json" \
       -d '{"url": "https://github.com/spring-projects/spring-boot"}'
     ```
   - [ ] Expected response:
     ```json
     {"shortCode": "aB3xK9", "shortUrl": "http://localhost:3000/aB3xK9"}
     ```
   - [ ] Redirect test:
     ```bash
     curl -I http://localhost:3000/aB3xK9
     ```

4. **Troubleshooting Section**
   - [ ] Port 3000 already in use: Stop conflicting service or change port in docker-compose.yml
   - [ ] Permission denied (Docker socket): Add user to docker group
   - [ ] Database migration errors: Check liquibase service logs

5. **Development Workflow**
   - [ ] Rebuild after code changes: `docker-compose up --build`
   - [ ] View logs: `docker-compose logs -f app`
   - [ ] Reset database: `docker-compose down -v` (deletes volumes)

**Technical Notes:**
- Consider adding Postman collection link for easier API testing
- Include link to Architecture documentation for deep dive
- Add badges: build status, license, etc.

**Testing Requirements:**
- Peer review: Another developer follows README and successfully deploys
- Manual test: Fresh checkout → working application in <5 minutes

**Definition of Done:**
- [ ] README.md created with all sections
- [ ] Instructions tested on clean environment
- [ ] curl examples validated
- [ ] Troubleshooting section covers common issues
- [ ] README committed to repository

---

### Story 4.5: Create Environment-Specific Configuration Profiles

**ID:** STORY-4.5  
**Title:** Support development and production configurations  
**Priority:** Low (Nice to Have)  
**Estimated Effort:** 2-3 hours

**User Story:**
```
As a deployment engineer
I want separate configurations for development and production
So that I can optimize settings for each environment
```

**Acceptance Criteria:**

1. **Spring Profiles**
   - [ ] Development profile: `application-dev.yml`
     ```yaml
     spring:
       jpa:
         show-sql: true
         properties:
           hibernate:
             format_sql: true
     logging:
       level:
         com.example.urlshortener: DEBUG
     ```
   - [ ] Production profile: `application-prod.yml`
     ```yaml
     spring:
       jpa:
         show-sql: false
     logging:
       level:
         com.example.urlshortener: INFO
     ```

2. **Profile Activation**
   - [ ] Default profile: `dev` (in application.yml)
     ```yaml
     spring:
       profiles:
         active: dev
     ```
   - [ ] Override via environment variable: `SPRING_PROFILES_ACTIVE=prod`

3. **Docker Compose Profiles**
   - [ ] Development: Default docker-compose.yml
   - [ ] Production: `docker-compose.prod.yml` with:
     - External database (not containerized PostgreSQL)
     - Health checks more aggressive
     - Resource limits (CPU, memory)

4. **Database Credential Security**
   - [ ] Development: Hardcoded in docker-compose.yml
   - [ ] Production: Use Docker secrets or environment files
   - [ ] `.env.example` file with placeholder values

5. **Logging Configuration**
   - [ ] Development: Console logging with DEBUG level
   - [ ] Production: File logging with INFO level, log rotation

**Technical Notes:**
- Avoid committing `.env` files with real credentials
- Consider using Spring Cloud Config for centralized configuration
- Production deployment may use Kubernetes instead of docker-compose

**Testing Requirements:**
- Manual test: `docker-compose up` uses dev profile
- Manual test: `SPRING_PROFILES_ACTIVE=prod docker-compose up` uses prod profile
- Manual test: Logging levels differ between profiles

**Definition of Done:**
- [ ] application-dev.yml and application-prod.yml created
- [ ] Profile activation working via environment variable
- [ ] .env.example file documented
- [ ] Profile-specific behavior validated

---

## Implementation Roadmap

### Phase 1: Foundation (Parallel Workstreams)
**Duration:** 1-2 weeks

**Workstream A: Data Layer**
1. Story 3.1: Design and Create Database Schema
2. Story 3.2: Create Liquibase Migration Changelog
3. Story 3.3: Implement JPA Entity and Repository
4. Story 3.4: Configure Spring Data JPA and PostgreSQL Connection

**Workstream B: ID Generation**
1. Story 2.1: Implement Snowflake ID Data Structure
2. Story 2.2: Implement Thread-Safe Sequence Counter
3. Story 2.3: Implement Base62 Encoding
4. Story 2.4: Create SnowflakeIdGenerator Spring Component

### Phase 2: Core Application
**Duration:** 1 week
1. Story 1.1: Create URL Shortening Endpoint
2. Story 1.2: Create Redirect Endpoint
3. Story 1.3: Implement Global Exception Handling
4. Story 2.5: Implement Database-Enforced Idempotency

### Phase 3: Deployment
**Duration:** 3-5 days
1. Story 4.1: Create Multi-Stage Dockerfile
2. Story 4.2: Create docker-compose Configuration
3. Story 4.3: Configure Service Health Checks
4. Story 4.4: Create README with Deployment Instructions

### Phase 4: Polish (Optional)
**Duration:** 1-2 days
1. Story 4.5: Create Environment-Specific Configuration Profiles

---

## Testing Strategy

### Unit Tests
**Coverage Target:** 100% for business logic (service layer, generator component)

**Tools:**
- JUnit 5
- Mockito for mocking dependencies
- AssertJ for fluent assertions

**Key Test Cases:**
- SnowflakeIdGenerator: Bit layout, thread safety, overflow handling
- Base62 encoding: Round-trip encoding/decoding
- Service layer: URL normalization, idempotency logic
- Controllers: Request validation, status code mapping

### Integration Tests
**Coverage Target:** Critical paths (shorten → redirect flow)

**Tools:**
- Spring Boot Test (`@SpringBootTest`)
- Testcontainers for PostgreSQL
- TestRestTemplate for HTTP requests

**Key Test Cases:**
- End-to-end: POST /api/shorten → GET /{shortCode} redirect
- Concurrent idempotency: 10 threads shorten same URL
- Database constraints: Duplicate short code, unique normalized URL
- Health checks: Database connectivity, actuator endpoint

### Performance Tests (Optional)
**Tools:**
- JMeter or Gatling

**Key Scenarios:**
- Load test: 1000 requests/second to /api/shorten
- Latency test: p95 redirect response time
- Concurrency test: 100 concurrent threads generating IDs

---

## Acceptance Criteria for MVP Release

### Functional Completeness
- [ ] POST /api/shorten accepts valid URLs and returns short codes
- [ ] GET /{shortCode} redirects to original URLs with HTTP 301
- [ ] Idempotency: Same URL always returns same short code
- [ ] Error handling: Invalid URLs return 400, missing short codes return 404

### Non-Functional Requirements
- [ ] Redirect latency: p95 < 100ms
- [ ] ID generation: No collisions across 10,000 generated IDs
- [ ] Data persistence: Survives docker-compose down/up cycle
- [ ] One-command deployment: docker-compose up --build succeeds

### Code Quality
- [ ] Unit test coverage: >90% for service layer and generator
- [ ] Integration tests: All critical paths covered
- [ ] Code review: Passes peer review for Spring Boot best practices
- [ ] Documentation: README, JavaDoc, inline comments complete

### Deployment Readiness
- [ ] Docker images build successfully
- [ ] Services start in correct order (postgres → liquibase → app)
- [ ] Health checks report healthy status
- [ ] Application accessible at http://localhost:3000

---

## Out of Scope (Future Enhancements)

### Version 2.0 Features
- **Analytics:** Track click counts, referrers, user agents
- **Link Expiration:** Time-based URL expiration
- **Custom Short Codes:** User-specified vanity URLs
- **Rate Limiting:** Prevent abuse with request throttling
- **Caching Layer:** Redis for hot short code lookup
- **Multi-Instance Deployment:** Horizontal scaling with unique instance IDs

### Deferred Technical Improvements
- **Authentication:** API key-based access control
- **HTTPS Support:** TLS termination with Let's Encrypt
- **Monitoring:** Prometheus metrics, Grafana dashboards
- **CI/CD Pipeline:** Automated testing and deployment
- **Database Replication:** Read replicas for redirect queries

---

**Document Status:** Complete - Ready for Implementation  
**Total Story Count:** 18 stories across 4 epics  
**Estimated Total Effort:** 43-59 hours  
**Recommended Team Size:** 1-2 developers  
**Target Completion:** 3-4 weeks (part-time) or 1-2 weeks (full-time)
