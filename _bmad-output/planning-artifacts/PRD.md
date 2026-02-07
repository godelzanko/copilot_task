# Product Requirements Document (PRD)
## URL Shortener Service

---

**Document Version:** 1.0  
**Product Name:** URL Shortener Service  
**Document Owner:** Product Manager  
**Date Created:** 2026-02-06  
**Last Updated:** 2026-02-06  
**Status:** Approved for Development

---

## Executive Summary

This document outlines the product requirements for a URL Shortener Service - a minimalist, production-ready REST API that provides persistent URL shortening functionality. The service embraces a "HashMap-via-REST" philosophy: it is fundamentally a persistent key-value store exposed through a clean HTTP interface, built for educational purposes and production deployment.

**Core Value Proposition:** Transform long URLs into short, shareable links with guaranteed idempotency, deployed in a fully containerized, reproducible environment.

**Target Audience:** This is a learning project designed to demonstrate:
- Advanced ID generation algorithms (Snowflake)
- Database constraint-based idempotency patterns
- RESTful API design principles
- Production-ready containerized deployment
- Clean, maintainable code architecture

---

## 1. Product Vision & Strategy

### 1.1 Product Vision

Build a lean, educational URL shortener service that demonstrates production-grade engineering practices without unnecessary complexity. The product focuses exclusively on core URL shortening functionality, proving that a well-architected MVP can be both simple and powerful.

### 1.2 Success Metrics

**Technical Success Indicators:**
- Zero ID collisions across billions of generated URLs
- Deterministic URL-to-short-code mapping (same URL always returns same short code)
- Sub-100ms redirect response time (without caching layer)
- One-command deployment via Docker Compose
- 100% test coverage for core business logic

**Learning Goals:**
- Master Snowflake ID generation algorithm
- Understand database constraint-based concurrency control
- Practice RESTful API design patterns
- Gain experience with Docker multi-service orchestration
- Learn Liquibase database migration management

### 1.3 Product Principles

1. **Simplicity Over Features:** Ship the smallest thing that provides core value
2. **Database as Source of Truth:** Let PostgreSQL handle what it does best (constraints, transactions, indexes)
3. **Pragmatic Over Perfect:** Hardcode simplifying assumptions appropriate for MVP scope
4. **Production-Ready From Day One:** Full containerization, health checks, proper error handling
5. **Educational Value First:** Code clarity and architectural demonstrations prioritized over optimization

---

## 2. Target Users & Use Cases

### 2.1 Primary User Persona

**"The Learning Developer"**
- **Background:** Intermediate Java developer learning production system design
- **Goals:** Understand real-world engineering patterns, build portfolio projects
- **Pain Points:** Many tutorials skip production considerations like idempotency, proper database design, containerization
- **Success Criteria:** Can explain and demonstrate advanced concepts in technical interviews

### 2.2 Use Cases

#### Use Case 1: Shorten a URL
**Actor:** API Client  
**Goal:** Convert a long URL into a short, shareable link  
**Preconditions:** Valid HTTP/HTTPS URL provided  
**Flow:**
1. Client sends POST request to `/api/shorten` with `{"url": "https://example.com/very/long/path"}`
2. System validates URL format
3. System normalizes URL (lowercase, trim whitespace)
4. System checks if URL already exists in database
5. If exists: Return existing short code
6. If new: Generate Snowflake-based short code, store mapping
7. Return response: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`

**Postconditions:** URL mapping persisted, same URL always returns same short code

#### Use Case 2: Access Original URL via Short Code
**Actor:** End User (browser)  
**Goal:** Navigate to original URL using short link  
**Preconditions:** Valid short code exists in system  
**Flow:**
1. User navigates to `http://localhost:8080/aB3xK9`
2. System looks up short code in database
3. If found: Return HTTP 301 redirect with Location header set to original URL
4. If not found: Return HTTP 404 Not Found

**Postconditions:** User redirected to original destination, browser may cache redirect

#### Use Case 3: Handle Duplicate URL Requests (Idempotency)
**Actor:** API Client  
**Goal:** Ensure same URL always maps to same short code, even with concurrent requests  
**Preconditions:** Same URL submitted multiple times (potentially concurrently)  
**Flow:**
1. Multiple clients POST same normalized URL simultaneously
2. Database UNIQUE constraint ensures only one record created
3. Try-insert-catch-select pattern handles race conditions gracefully
4. All requests receive identical short code in response

**Postconditions:** Exactly one mapping exists, all clients get consistent result

---

## 3. Functional Requirements

### 3.1 Core Features (MVP - Must Have)

#### Feature 1: URL Shortening API Endpoint

**Requirement ID:** FR-001  
**Priority:** Critical  
**Description:** Provide REST endpoint to create short URLs

**Acceptance Criteria:**
- ✅ Endpoint: `POST /api/shorten`
- ✅ Request body: `{"url": "https://example.com/path"}`
- ✅ Response format: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`
- ✅ HTTP 200 OK on success
- ✅ HTTP 400 Bad Request for invalid URLs
- ✅ Response includes both short code and full short URL for client convenience

**Technical Notes:**
- Use Java `URL()` class for validation (throws `MalformedURLException` on invalid input)
- Build full short URL using `ServletUriComponentsBuilder.fromCurrentContextPath()`
- Service layer handles business logic, controller focuses on HTTP concerns

---

#### Feature 2: Redirect Endpoint

**Requirement ID:** FR-002  
**Priority:** Critical  
**Description:** Redirect short URLs to original destinations

**Acceptance Criteria:**
- ✅ Endpoint: `GET /{shortCode}`
- ✅ Returns HTTP 301 Moved Permanently on success
- ✅ Location header contains original URL
- ✅ HTTP 404 Not Found if short code doesn't exist
- ✅ No response body required (redirect only)

**Technical Notes:**
- Use `ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(uri).build()`
- 301 status enables browser caching (performance optimization)
- Short code case-sensitive (Base62 encoding preserves case)

---

#### Feature 3: Snowflake-Based ID Generation

**Requirement ID:** FR-003  
**Priority:** Critical  
**Description:** Generate collision-free short codes using Snowflake algorithm with Base62 encoding

**Acceptance Criteria:**
- ✅ ID structure: 41-bit timestamp + 10-bit instance ID + 13-bit sequence counter
- ✅ Custom epoch: 2024-01-01T00:00:00Z (space-efficient for new service)
- ✅ Instance ID hardcoded to 0 (single-instance deployment)
- ✅ Base62 encoding: 0-9, a-z, A-Z (URL-safe, readable)
- ✅ Typical short code length: ~7 characters
- ✅ Thread-safe sequence counter (synchronized methods)

**Technical Notes:**
- Implement as Spring `@Component` singleton
- Base62 lookup table: `"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"`
- Sequence counter supports 8,192 IDs per millisecond per instance
- Scale to billions of URLs before exhausting 41-bit timestamp space

---

#### Feature 4: Database-Enforced Idempotency

**Requirement ID:** FR-004  
**Priority:** Critical  
**Description:** Guarantee same URL always maps to same short code using database constraints

**Acceptance Criteria:**
- ✅ Normalized URL expression index: `LOWER(TRIM(original_url))`
- ✅ UNIQUE constraint prevents duplicate normalized URLs
- ✅ Try-insert-catch-select pattern handles concurrent requests gracefully
- ✅ No application-level locking required
- ✅ Case-insensitive and whitespace-insensitive URL matching

**Technical Notes:**
- Service layer tries INSERT, catches unique constraint violation, then SELECTs existing record
- Database atomically enforces business rule (one URL = one short code)
- Prevents subtle bugs from `"https://example.com"` vs `"https://example.com "` being treated as different

---

#### Feature 5: Minimal Database Schema

**Requirement ID:** FR-005  
**Priority:** Critical  
**Description:** Three-column schema storing only essential data

**Schema Definition:**
```sql
CREATE TABLE urls (
    short_code VARCHAR(10) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_original_url_normalized 
ON urls(LOWER(TRIM(original_url)));
```

**Acceptance Criteria:**
- ✅ Primary key on `short_code` (fast lookups for redirects)
- ✅ `original_url` stored as TEXT (supports very long URLs)
- ✅ `created_at` timestamp for basic auditing
- ✅ Unique normalized index on original URL (idempotency enforcement)
- ✅ No analytics columns, no expiration columns (out of scope for MVP)

**Technical Notes:**
- Schema is evolvable via Liquibase migrations (can add features later)
- No foreign keys, no complex relationships (pure key-value store)
- PostgreSQL TEXT type supports URLs up to 1GB (practically unlimited)

---

#### Feature 6: Docker-Based Deployment

**Requirement ID:** FR-006  
**Priority:** Critical  
**Description:** Fully containerized deployment with orchestrated service startup

**Acceptance Criteria:**
- ✅ Three-service architecture: postgres → liquibase → app
- ✅ Health check-driven dependency chain (sequential startup)
- ✅ PostgreSQL 16 Alpine (lightweight, production-grade)
- ✅ Liquibase runs migrations before app starts
- ✅ Multi-stage Dockerfile for optimized app image (~200MB)
- ✅ Single command deployment: `docker-compose up --build`

**docker-compose.yml Services:**
1. **postgres:** Database server with health checks (`pg_isready`)
2. **liquibase:** One-shot migration runner (depends on postgres healthy, completes successfully)
3. **app:** Spring Boot application (depends on liquibase completed, serves on port 3000)

**Technical Notes:**
- Maven build stage + JRE runtime stage (no build tools in production image)
- Volume mount for PostgreSQL data persistence
- Environment variables for Spring datasource configuration
- Bridge network for inter-service communication

---

### 3.2 Out of Scope for MVP (Explicitly Deferred to v2.0)

The following features are valuable but intentionally excluded from MVP to maintain focus and ship faster:

#### Performance Optimization
- ❌ **Redis/Caffeine Caching:** PostgreSQL is fast enough for learning project (YAGNI principle)
- ❌ **Browser Caching Headers:** Can add `Cache-Control` headers later if needed
- ❌ **Database Read Replicas:** Single-instance deployment sufficient for MVP

#### Analytics & Tracking
- ❌ **Visit Counting:** Adds complexity without core learning value
- ❌ **Click Analytics:** Geographic info, referrer tracking, user agents
- ❌ **Real-time Dashboards:** Monitoring and metrics

#### Advanced Features
- ❌ **Link Expiration/TTL:** Can add via Liquibase migration when needed
- ❌ **Custom Short Codes:** User-specified aliases (e.g., `/promo2024`)
- ❌ **Bulk URL Shortening:** Batch API endpoint
- ❌ **Link Editing:** Update destination URL for existing short code
- ❌ **Soft Deletion:** Archive links instead of permanent delete

#### Enterprise Features
- ❌ **Authentication/Authorization:** Public API for MVP
- ❌ **Rate Limiting:** Not needed for learning environment
- ❌ **Multi-tenancy:** Single-tenant service
- ❌ **API Versioning:** Simple API unlikely to break

**Rationale:** These features distract from core learning goals (Snowflake IDs, database constraints, REST APIs, Docker deployment). They can be added incrementally once MVP proves core concepts.

---

## 4. Non-Functional Requirements

### 4.1 Performance

**NFR-001: Redirect Latency**
- **Requirement:** Sub-100ms average redirect response time
- **Measurement:** Database query time + HTTP redirect overhead
- **Acceptance:** 95th percentile under 100ms without caching layer
- **Rationale:** PostgreSQL indexed lookup is sufficient; caching is premature optimization

**NFR-002: ID Generation Throughput**
- **Requirement:** Generate 8,192 unique IDs per millisecond (per instance)
- **Measurement:** Snowflake 13-bit sequence counter capacity
- **Acceptance:** No sequence overflow under normal load
- **Rationale:** Far exceeds realistic traffic for learning project

### 4.2 Scalability

**NFR-003: URL Capacity**
- **Requirement:** Support billions of URLs before timestamp exhaustion
- **Measurement:** 41-bit timestamp with custom epoch 2024-01-01
- **Acceptance:** Service viable until ~2093 (69 years)
- **Rationale:** Demonstrates understanding of Snowflake scalability properties

**NFR-004: Single-Instance Deployment**
- **Requirement:** Hardcoded instance ID = 0 (no distributed coordination)
- **Measurement:** Deployment configuration
- **Acceptance:** Service runs on single Docker host
- **Rationale:** Simplifies MVP; multi-instance can be added via configuration change

### 4.3 Reliability

**NFR-005: Idempotency Guarantee**
- **Requirement:** Same URL always returns same short code, even under concurrent requests
- **Measurement:** Integration tests with parallel POST requests
- **Acceptance:** 100% consistency across all concurrent requests
- **Rationale:** Database UNIQUE constraint provides atomic enforcement

**NFR-006: Data Persistence**
- **Requirement:** URL mappings survive service restarts
- **Measurement:** Docker volume persistence
- **Acceptance:** Data retained after `docker-compose down && docker-compose up`
- **Rationale:** Demonstrates proper stateful service deployment

### 4.4 Maintainability

**NFR-007: Code Clarity**
- **Requirement:** Code demonstrates educational concepts clearly
- **Measurement:** Code review, inline comments for complex logic
- **Acceptance:** New developer can understand architecture in <30 minutes
- **Rationale:** Learning project prioritizes clarity over brevity

**NFR-008: Database Migration Management**
- **Requirement:** All schema changes version-controlled via Liquibase
- **Measurement:** YAML changelog files in source control
- **Acceptance:** `docker-compose up` applies migrations automatically
- **Rationale:** Demonstrates production-grade database change management

### 4.5 Deployability

**NFR-009: One-Command Deployment**
- **Requirement:** Service starts with single command: `docker-compose up --build`
- **Measurement:** Manual deployment test
- **Acceptance:** Fresh checkout to running service in <5 minutes
- **Rationale:** Demonstrates proper containerized deployment practices

**NFR-010: Environment Independence**
- **Requirement:** Service runs identically on any Docker-compatible host
- **Measurement:** Test on multiple operating systems (Linux, macOS, Windows with WSL2)
- **Acceptance:** No "works on my machine" issues
- **Rationale:** Docker eliminates environment-specific configuration

---

## 5. Technical Architecture

### 5.1 Technology Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| **Language** | Java | 21 | Modern LTS, virtual threads, pattern matching |
| **Framework** | Spring Boot | 3.2+ | Industry standard, comprehensive ecosystem |
| **Build Tool** | Maven | 3.9+ | Reliable, well-documented, Java standard |
| **Database** | PostgreSQL | 16 | ACID compliance, excellent constraint support |
| **Migration** | Liquibase | 4.25+ | YAML-based, rollback capable, Spring integration |
| **Container** | Docker | 24.0+ | Production standard, reproducible environments |
| **Orchestration** | docker-compose | 3.8+ | Multi-service coordination, health checks |

### 5.2 System Components

```
┌─────────────────────────────────────────────────┐
│                   API Client                     │
└───────────────────┬─────────────────────────────┘
                    │ HTTP/REST
                    ▼
┌─────────────────────────────────────────────────┐
│          Spring Boot Application                 │
│  ┌───────────────────────────────────────────┐  │
│  │   Controllers (REST Endpoints)            │  │
│  │   - ShortenController (POST /api/shorten) │  │
│  │   - RedirectController (GET /{shortCode}) │  │
│  └───────────────────┬───────────────────────┘  │
│                      │                           │
│  ┌───────────────────▼───────────────────────┐  │
│  │   Service Layer (Business Logic)          │  │
│  │   - UrlShortenerService                   │  │
│  │   - Try-Insert-Catch-Select Pattern       │  │
│  └───────────────────┬───────────────────────┘  │
│                      │                           │
│  ┌───────────────────▼───────────────────────┐  │
│  │   Repository Layer (Data Access)          │  │
│  │   - UrlRepository (Spring Data JPA)       │  │
│  └───────────────────┬───────────────────────┘  │
│                      │                           │
│  ┌───────────────────▼───────────────────────┐  │
│  │   ID Generator (Snowflake Base62)         │  │
│  │   - Thread-safe singleton component       │  │
│  └───────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────┘
                    │ JDBC
                    ▼
┌─────────────────────────────────────────────────┐
│         PostgreSQL Database (Docker)             │
│  ┌───────────────────────────────────────────┐  │
│  │   urls table:                             │  │
│  │   - short_code (PK)                       │  │
│  │   - original_url                          │  │
│  │   - created_at                            │  │
│  │                                           │  │
│  │   Indexes:                                │  │
│  │   - PRIMARY KEY (short_code)              │  │
│  │   - UNIQUE (LOWER(TRIM(original_url)))    │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                    ▲
                    │ Migrations
┌───────────────────┴─────────────────────────────┐
│      Liquibase (Docker, one-shot startup)        │
│  - Applies db.changelog-master.yaml              │
│  - Runs before application starts                │
└─────────────────────────────────────────────────┘
```

### 5.3 Data Flow

#### Shorten URL Flow
```
1. POST /api/shorten {"url": "https://example.com"}
   ↓
2. Controller validates request
   ↓
3. Service layer:
   a. Validates URL format (new URL(originalUrl))
   b. Normalizes URL (toLowerCase(), trim())
   c. Generates Snowflake ID → Base62 encode
   d. Try INSERT INTO urls (short_code, original_url)
   e. If unique constraint violation → SELECT existing short_code
   ↓
4. Return {"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}
```

#### Redirect Flow
```
1. GET /aB3xK9
   ↓
2. Controller extracts shortCode path variable
   ↓
3. Repository: SELECT original_url FROM urls WHERE short_code = 'aB3xK9'
   ↓
4. If found: Return 301 redirect with Location: https://example.com
   If not found: Return 404 Not Found
```

### 5.4 Database Schema Details

**Table: urls**
```sql
CREATE TABLE urls (
    short_code VARCHAR(10) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Indexes:**
```sql
-- Primary key index (automatic, for fast redirects)
CREATE UNIQUE INDEX urls_pkey ON urls(short_code);

-- Idempotency enforcement index
CREATE UNIQUE INDEX idx_original_url_normalized 
ON urls(LOWER(TRIM(original_url)));
```

**Design Rationale:**
- `short_code` as VARCHAR(10): Snowflake Base62 generates ~7 chars, room for growth
- `original_url` as TEXT: Supports URLs up to practical browser limits (64KB+)
- `created_at`: Basic audit trail, useful for debugging, no performance impact
- Expression index: Normalizes URLs at query time for duplicate detection

### 5.5 Snowflake ID Structure

**64-bit Snowflake ID Composition:**
```
┌────────────────┬──────────────┬──────────────────┐
│   Timestamp    │  Instance ID │    Sequence      │
│    41 bits     │   10 bits    │    13 bits       │
└────────────────┴──────────────┴──────────────────┘
│                │              │                  │
│ Milliseconds   │ Hardcoded 0  │ 0-8191 counter   │
│ since custom   │ (single      │ per millisecond  │
│ epoch          │ instance)    │                  │
│ (2024-01-01)   │              │                  │
└────────────────┴──────────────┴──────────────────┘
        │                             │
        ▼                             ▼
   Time-ordered                   High throughput
   globally unique                per instance
```

**Base62 Encoding:**
- Alphabet: `0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`
- Result: URL-safe, case-sensitive, human-readable short codes
- Length: ~7 characters for typical IDs, grows slowly with time

**Example ID Generation:**
```
Timestamp: 1234567890123 ms (41 bits)
Instance:  0 (10 bits)
Sequence:  42 (13 bits)
         ↓
Combined:  5175672827914 (64-bit long)
         ↓
Base62:    "aB3xK9" (7 characters)
```

---

## 6. API Specification

### 6.1 Endpoint: POST /api/shorten

**Purpose:** Create a short URL for the provided long URL

**Request:**
```http
POST /api/shorten HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "url": "https://www.example.com/very/long/path/to/resource?param1=value1&param2=value2"
}
```

**Success Response (200 OK):**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "shortCode": "aB3xK9",
  "shortUrl": "http://localhost:8080/aB3xK9"
}
```

**Error Response (400 Bad Request):**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2024-02-06T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid URL format",
  "path": "/api/shorten"
}
```

**Validation Rules:**
- URL must be valid HTTP or HTTPS
- URL must pass `new URL(url)` constructor (Java standard validation)
- Empty or null URLs return 400 Bad Request
- Malformed URLs return 400 Bad Request

**Idempotency Guarantee:**
- Submitting the same URL multiple times returns the same `shortCode`
- Normalization rules: lowercase + trim whitespace
- `"HTTPS://EXAMPLE.COM"` and `"https://example.com"` produce same short code

---

### 6.2 Endpoint: GET /{shortCode}

**Purpose:** Redirect to the original URL associated with the short code

**Request:**
```http
GET /aB3xK9 HTTP/1.1
Host: localhost:8080
```

**Success Response (301 Moved Permanently):**
```http
HTTP/1.1 301 Moved Permanently
Location: https://www.example.com/very/long/path/to/resource?param1=value1&param2=value2
Content-Length: 0
```

**Error Response (404 Not Found):**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "timestamp": "2024-02-06T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Short code not found: invalidCode",
  "path": "/invalidCode"
}
```

**Behavior Notes:**
- 301 status enables browser caching (permanent redirect)
- Browsers may cache redirect locally, subsequent visits skip server
- Short codes are case-sensitive (`aB3xK9` ≠ `ab3xk9`)
- No response body required for successful redirects

---

## 7. Development Roadmap

### 7.1 Phase 1: Core Implementation (Weeks 1-2)

**Sprint 1.1: ID Generator & Database (Days 1-3)**
- ✅ Task 1.1.1: Create `SnowflakeIdGenerator` component
  - Implement 64-bit ID generation (timestamp + instance + sequence)
  - Add Base62 encoder with lookup table
  - Configure custom epoch (2024-01-01)
  - Unit tests: 10,000 IDs with zero collisions
  
- ✅ Task 1.1.2: Setup Liquibase database migrations
  - Create `db.changelog-master.yaml`
  - Changeset 001: Create `urls` table
  - Changeset 002: Add normalized unique index
  - Configure Spring datasource in `application.yml`

**Sprint 1.2: Service Layer (Days 4-6)**
- ✅ Task 1.2.1: Create `UrlShortenerService`
  - Implement try-insert-catch-select pattern
  - Add URL normalization (lowercase, trim)
  - URL validation with `new URL()`
  - Transaction management with `@Transactional`
  
- ✅ Task 1.2.2: Create `UrlRepository` (Spring Data JPA)
  - Define `Url` entity with JPA annotations
  - Custom query for normalized URL lookup
  - Repository interface extending `JpaRepository`

**Sprint 1.3: REST Controllers (Days 7-9)**
- ✅ Task 1.3.1: Create `ShortenController`
  - POST /api/shorten endpoint
  - Request validation and error handling
  - Build full short URL in response
  
- ✅ Task 1.3.2: Create `RedirectController`
  - GET /{shortCode} endpoint
  - 301 redirect implementation
  - 404 handling for invalid codes
  
- ✅ Task 1.3.3: Global exception handling
  - `@ControllerAdvice` for consistent error responses
  - Map exceptions to appropriate HTTP status codes

**Sprint 1.4: Testing (Days 10-12)**
- ✅ Task 1.4.1: Unit tests for all components
  - SnowflakeIdGenerator tests
  - Service layer tests with mock repository
  - Controller tests with MockMvc
  
- ✅ Task 1.4.2: Integration tests
  - Test idempotency with concurrent requests
  - End-to-end flow: shorten → redirect
  - Invalid input handling

---

### 7.2 Phase 2: Docker Deployment (Week 2)

**Sprint 2.1: Containerization (Days 13-14)**
- ✅ Task 2.1.1: Create multi-stage Dockerfile
  - Stage 1: Maven build (eclipse-temurin-21 + maven)
  - Stage 2: Runtime (eclipse-temurin-21-jre-alpine)
  - Optimize layer caching (dependencies first)
  
- ✅ Task 2.1.2: Create docker-compose.yml
  - PostgreSQL 16 service with health checks
  - Liquibase migration service
  - Spring Boot app service
  - Configure service dependencies and networks

**Sprint 2.2: Deployment Validation (Day 15)**
- ✅ Task 2.2.1: Test complete deployment flow
  - `docker-compose up --build` on fresh system
  - Verify service startup order (postgres → liquibase → app)
  - Smoke test API endpoints
  
- ✅ Task 2.2.2: Test data persistence
  - Create URLs, restart services
  - Verify data survives restart
  - Test volume mounting

---

### 7.3 Future Enhancements (v2.0 - Post-MVP)

**Potential Features for Next Iteration:**
- Add Redis caching layer for hot URLs
- Implement visit tracking and analytics
- Add link expiration/TTL management
- Custom short code aliases
- Rate limiting and authentication
- Admin dashboard for URL management
- Bulk shortening API
- Link editing capabilities

**Evaluation Criteria for v2.0:**
- Must not break existing API contracts
- Must add measurable user value
- Must be implementable via Liquibase migrations (no schema rewrites)

---

## 8. Testing Strategy

### 8.1 Unit Tests

**SnowflakeIdGenerator Tests:**
```java
@Test
void generateId_shouldProduceUniqueIds() {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 10000; i++) {
        String id = generator.generateId();
        assertTrue(ids.add(id), "Duplicate ID generated: " + id);
    }
}

@Test
void generateId_shouldProduceBase62EncodedStrings() {
    String id = generator.generateId();
    assertTrue(id.matches("[0-9a-zA-Z]+"));
}
```

**UrlShortenerService Tests:**
```java
@Test
void shortenUrl_withDuplicateUrl_shouldReturnSameShortCode() {
    String url = "https://example.com";
    ShortenResponse first = service.shortenUrl(url);
    ShortenResponse second = service.shortenUrl(url);
    assertEquals(first.getShortCode(), second.getShortCode());
}

@Test
void shortenUrl_withInvalidUrl_shouldThrowBadRequestException() {
    assertThrows(BadRequestException.class, 
        () -> service.shortenUrl("not-a-valid-url"));
}
```

### 8.2 Integration Tests

**End-to-End Flow Test:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {
    
    @Test
    void fullFlow_shortenAndRedirect() throws Exception {
        // Shorten URL
        String response = mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://example.com\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        String shortCode = JsonPath.read(response, "$.shortCode");
        
        // Test redirect
        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isMovedPermanently())
            .andExpect(header().string("Location", "https://example.com"));
    }
}
```

**Concurrent Idempotency Test:**
```java
@Test
void shortenUrl_withConcurrentRequests_shouldReturnSameShortCode() throws Exception {
    String url = "https://example.com";
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    
    Set<String> shortCodes = ConcurrentHashMap.newKeySet();
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                String shortCode = service.shortenUrl(url).getShortCode();
                shortCodes.add(shortCode);
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    assertEquals(1, shortCodes.size(), "All requests should return same short code");
}
```

### 8.3 Testing Checklist

**Pre-Deployment Validation:**
- ✅ All unit tests pass (100% coverage for business logic)
- ✅ Integration tests pass with real database (Testcontainers)
- ✅ Concurrent idempotency test passes
- ✅ Invalid input handling verified
- ✅ Docker build succeeds without errors
- ✅ docker-compose up starts services in correct order
- ✅ Manual smoke test: create URL, access short link in browser
- ✅ Data persists after docker-compose restart
- ✅ API returns proper error responses (400, 404)

---

## 9. Risk Assessment & Mitigation

### 9.1 Technical Risks

**Risk 1: Snowflake Sequence Overflow**
- **Likelihood:** Low (13-bit sequence = 8,192/ms capacity)
- **Impact:** High (service unavailable if counter exhausts)
- **Mitigation:** 
  - Monitor sequence resets per millisecond
  - Implement overflow exception with retry logic
  - Add metrics/logging for sequence usage
  - Consider expanding to multi-instance if needed

**Risk 2: Database Connection Pool Exhaustion**
- **Likelihood:** Medium (under high concurrent load)
- **Impact:** Medium (slow responses, timeouts)
- **Mitigation:**
  - Configure HikariCP connection pool size appropriately
  - Add connection pool metrics
  - Set reasonable connection timeout values
  - Implement circuit breaker pattern if needed

**Risk 3: URL Normalization Edge Cases**
- **Likelihood:** Medium (URLs are complex)
- **Impact:** Low (may create duplicate mappings)
- **Mitigation:**
  - Document normalization rules clearly
  - Add comprehensive test cases for URL variants
  - Consider more sophisticated normalization (remove trailing slashes, sort query params)
  - Accept some edge cases for MVP, address in v2.0

### 9.2 Operational Risks

**Risk 4: Docker Startup Race Conditions**
- **Likelihood:** Medium (health checks may not catch all cases)
- **Impact:** High (failed deployment, data corruption)
- **Mitigation:**
  - Comprehensive health check configuration
  - Liquibase depends on postgres healthy
  - App depends on liquibase completed successfully
  - Add startup retry logic in application

**Risk 5: Database Volume Data Loss**
- **Likelihood:** Low (Docker volumes are persistent)
- **Impact:** High (all URL mappings lost)
- **Mitigation:**
  - Document volume backup procedures
  - Use named volumes (explicit, not anonymous)
  - Consider volume backup strategy for production
  - Regularly test disaster recovery process

### 9.3 Learning Risks

**Risk 6: Over-Engineering for Learning Project**
- **Likelihood:** Medium (temptation to add features)
- **Impact:** Medium (delays completion, dilutes learning goals)
- **Mitigation:**
  - Strict MVP scope discipline
  - Defer all v2.0 features to post-MVP phase
  - Focus on core concepts: Snowflake, constraints, REST, Docker
  - Accept "good enough" over "perfect"

---

## 10. Success Criteria

### 10.1 MVP Completion Checklist

**Functional Completeness:**
- ✅ User can shorten any valid HTTP/HTTPS URL via POST /api/shorten
- ✅ User can access original URL by visiting short link (301 redirect)
- ✅ Same URL always returns same short code (idempotency)
- ✅ Invalid URLs return 400 Bad Request with clear error message
- ✅ Non-existent short codes return 404 Not Found

**Technical Excellence:**
- ✅ Snowflake ID generator produces collision-free codes
- ✅ Database UNIQUE constraint enforces idempotency atomically
- ✅ All code follows clean architecture principles (separation of concerns)
- ✅ Comprehensive test coverage (unit + integration)
- ✅ Liquibase migrations apply successfully on fresh database

**Deployment Readiness:**
- ✅ `docker-compose up --build` deploys complete stack
- ✅ Services start in correct order with health check coordination
- ✅ Data persists across service restarts
- ✅ No manual configuration required (fully automated setup)

**Documentation:**
- ✅ README.md with quick start guide
- ✅ API documentation with example requests/responses
- ✅ Architecture diagram showing component interactions
- ✅ Inline code comments explaining complex logic (Snowflake, idempotency)

### 10.2 Learning Objectives Achievement

**Core Concepts Demonstrated:**
- ✅ **Snowflake Algorithm:** Custom implementation with Base62 encoding
- ✅ **Database Constraints:** UNIQUE index for business rule enforcement
- ✅ **RESTful API Design:** Proper HTTP methods, status codes, resource modeling
- ✅ **Idempotency Patterns:** Try-insert-catch-select with database atomicity
- ✅ **Docker Orchestration:** Multi-service startup coordination
- ✅ **Database Migrations:** Version-controlled schema evolution with Liquibase

**Interview-Ready Talking Points:**
- ✅ Can explain Snowflake ID structure and scalability properties
- ✅ Can discuss trade-offs: database constraints vs application logic
- ✅ Can justify 301 vs 302 redirect choice
- ✅ Can describe Docker multi-stage build optimization
- ✅ Can defend MVP scope decisions (what to exclude and why)

---

## 11. Appendix

### 11.1 Glossary

**Terms:**
- **Base62 Encoding:** Number system using digits 0-9, lowercase a-z, uppercase A-Z (62 symbols total), producing URL-safe strings
- **Idempotency:** Property where repeated identical requests produce the same result
- **Snowflake Algorithm:** Distributed ID generation scheme invented by Twitter, produces 64-bit time-ordered unique identifiers
- **Try-Insert-Catch-Select:** Pattern where code attempts INSERT, catches unique constraint violation, then SELECTs existing record
- **Expression Index:** Database index on computed value (e.g., `LOWER(column)`) rather than raw column value
- **Multi-Stage Docker Build:** Dockerfile with multiple FROM statements, enabling separate build and runtime environments
- **Health Check:** Container diagnostic that reports service readiness, used to coordinate dependent service startup

### 11.2 References

**Technical Documentation:**
- Spring Boot Reference: https://docs.spring.io/spring-boot/docs/current/reference/html/
- PostgreSQL Documentation: https://www.postgresql.org/docs/16/
- Liquibase YAML Format: https://docs.liquibase.com/concepts/changelogs/yaml-format.html
- Docker Compose Specification: https://docs.docker.com/compose/compose-file/
- Snowflake ID Original Paper: https://github.com/twitter-archive/snowflake

**Design Patterns:**
- RESTful API Design Best Practices
- Database Constraint-Based Concurrency Control
- Try-Insert-Catch-Select Pattern for Idempotency
- Multi-Stage Docker Builds for Size Optimization

### 11.3 Decision Log

| Date | Decision | Rationale | Impact |
|------|----------|-----------|--------|
| 2024-02-06 | Use Snowflake over UUID | Time-ordered IDs, shorter Base62 encoding, demonstrates algorithm knowledge | Positive: Educational value, scalability story |
| 2024-02-06 | Hardcode instance ID = 0 | MVP simplicity, single-instance deployment sufficient | Neutral: Configurable in v2.0 if needed |
| 2024-02-06 | Database constraints over application locks | PostgreSQL handles atomicity better than app code | Positive: Simpler code, better concurrency |
| 2024-02-06 | 301 Permanent Redirect over 302 | Browser caching benefits, SEO-friendly | Positive: Performance, standard practice |
| 2024-02-06 | No caching layer for MVP | YAGNI principle, PostgreSQL fast enough | Positive: Faster delivery, focus on core |
| 2024-02-06 | No analytics/tracking | Out of scope for learning goals | Positive: Cleaner MVP, can add later |
| 2024-02-06 | Liquibase YAML over SQL | More readable, better diff visibility in Git | Positive: Maintainability |
| 2024-02-06 | Three-service Docker Compose | Clean separation, health check orchestration | Positive: Production-like setup |

### 11.4 Open Questions

**For MVP:**
- None (all MVP decisions finalized)

**For v2.0 Consideration:**
- Should we add custom short code aliases (e.g., `/promo2024`)?
- What analytics are actually valuable (visits? geography? referrers)?
- Should we support link editing (update destination URL)?
- Do we need API authentication for production use?
- Should we implement rate limiting per IP address?

### 11.5 Configuration Files Reference

**Complete docker-compose.yml:** See Appendix in brainstorming document  
**Complete Dockerfile:** See Appendix in brainstorming document  
**Liquibase Changelog Example:**
```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-urls-table
      author: urlshortener
      changes:
        - createTable:
            tableName: urls
            columns:
              - column:
                  name: short_code
                  type: VARCHAR(10)
                  constraints:
                    primaryKey: true
              - column:
                  name: original_url
                  type: TEXT
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP

  - changeSet:
      id: 002-create-normalized-url-index
      author: urlshortener
      changes:
        - sql:
            sql: CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))
```

---

## Document Approval

**Product Manager:** ✅ Approved  
**Tech Lead:** _Pending Implementation_  
**QA Lead:** _Pending Testing_  

**Next Steps:**
1. Begin Phase 1 implementation (Snowflake ID generator)
2. Setup CI/CD pipeline for automated testing
3. Schedule weekly progress reviews

---

**End of Product Requirements Document**

*This PRD represents the complete specification for the URL Shortener Service MVP. All stakeholders should refer to this document for scope, requirements, and success criteria. Questions or clarification requests should be directed to the Product Manager.*
