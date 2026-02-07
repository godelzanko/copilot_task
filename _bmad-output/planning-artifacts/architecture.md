# Architecture Documentation
## URL Shortener Service

**Version:** 1.0  
**Last Updated:** 2026-02-06  
**Status:** Approved for Implementation

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Principles](#architecture-principles)
3. [Component Architecture](#component-architecture)
4. [Data Architecture](#data-architecture)
5. [Deployment Architecture](#deployment-architecture)
6. [Technical Decision Records](#technical-decision-records)
7. [API Specification](#api-specification)
8. [Security Considerations](#security-considerations)
9. [Scalability & Performance](#scalability--performance)
10. [Future Architecture Evolution](#future-architecture-evolution)

---

## System Overview

### Core Philosophy: "HashMap-via-REST"

The URL Shortener Service is fundamentally a **persistent key-value store exposed through a RESTful HTTP interface**. This mental model drives all architectural decisions, keeping the implementation focused on essential functionality without over-engineering.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     API Clients                          │
│         (Web browsers, Mobile apps, Scripts)             │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Application                     │
│                    (Port 8080)                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  REST Controllers                                 │  │
│  │  • POST /api/shorten → Create short URL          │  │
│  │  • GET /{shortCode} → Redirect to original       │  │
│  └─────────────────┬─────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼─────────────────────────────────┐  │
│  │  Service Layer                                    │  │
│  │  • URL validation & normalization                │  │
│  │  • Idempotency handling                          │  │
│  │  • Business logic orchestration                  │  │
│  └─────────────────┬─────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼─────────────────────────────────┐  │
│  │  Snowflake ID Generator                          │  │
│  │  • 41-bit timestamp (custom epoch 2024-01-01)   │  │
│  │  • 10-bit instance ID (hardcoded 0)             │  │
│  │  • 13-bit sequence counter                       │  │
│  │  • Base62 encoding                               │  │
│  └──────────────────────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼─────────────────────────────────┐  │
│  │  Repository Layer (Spring Data JPA)              │  │
│  │  • UrlEntity CRUD operations                     │  │
│  │  • Custom query methods                          │  │
│  └─────────────────┬─────────────────────────────────┘  │
└────────────────────┼─────────────────────────────────────┘
                     │ JDBC
                     ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL 16 Database                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Table: urls                                      │  │
│  │  • short_code VARCHAR(10) PRIMARY KEY            │  │
│  │  • original_url TEXT NOT NULL                    │  │
│  │  • created_at TIMESTAMP                          │  │
│  │                                                   │  │
│  │  Index: idx_original_url_normalized              │  │
│  │  • UNIQUE(LOWER(TRIM(original_url)))            │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Architecture Principles

### 1. Simplicity Over Features
- **YAGNI (You Aren't Gonna Need It):** Only build what's essential for MVP
- **No Premature Optimization:** PostgreSQL is fast enough without caching layer
- **Clear Scope Boundaries:** Explicitly defer non-essential features to v2.0

### 2. Database as Source of Truth
- **Constraint-Based Integrity:** Let PostgreSQL enforce business rules (UNIQUE constraints)
- **Transactional Consistency:** Database handles concurrency, not application locks
- **Schema Evolution:** Liquibase migrations enable safe database changes

### 3. Production-Ready from Day One
- **Full Containerization:** Docker ensures environment reproducibility
- **Health Check Orchestration:** Services start in correct dependency order
- **Proper Error Handling:** Graceful failure modes, meaningful HTTP status codes

### 4. Educational Value First
- **Code Clarity:** Prioritize understandability over brevity
- **Pattern Demonstration:** Showcase industry-standard approaches
- **Documentation:** Architecture decisions explicitly documented

---

## Component Architecture

### Layer Responsibilities

#### 1. Controller Layer
**Package:** `com.example.urlshortener.controller`

**Responsibilities:**
- HTTP request/response handling
- Input validation (format, required fields)
- HTTP status code selection
- Response body construction

**Components:**
- **ShortenController:** Handles `POST /api/shorten`
  - Accepts JSON: `{"url": "https://example.com/path"}`
  - Returns JSON: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`
  - Status: 200 OK (success), 400 Bad Request (invalid URL)

- **RedirectController:** Handles `GET /{shortCode}`
  - Returns HTTP 301 redirect with Location header
  - Status: 301 Moved Permanently (found), 404 Not Found (missing)

**Design Pattern:** Thin controllers – delegate business logic to service layer

---

#### 2. Service Layer
**Package:** `com.example.urlshortener.service`

**Responsibilities:**
- Business logic orchestration
- URL normalization (lowercase, trim)
- Idempotency enforcement
- Transaction management

**Components:**
- **UrlShortenerService:**
  - `shortenUrl(String originalUrl)` → Returns UrlDto
  - `getOriginalUrl(String shortCode)` → Returns original URL or throws exception

**Key Patterns:**

**Try-Insert-Catch-Select (Idempotency):**
```java
@Transactional
public UrlDto shortenUrl(String originalUrl) {
    String normalized = normalizeUrl(originalUrl);
    
    try {
        // Attempt to insert new mapping
        String shortCode = snowflakeIdGenerator.generate();
        UrlEntity entity = new UrlEntity(shortCode, normalized);
        urlRepository.save(entity);
        return toDto(entity);
    } catch (DataIntegrityViolationException e) {
        // URL already exists, retrieve existing mapping
        UrlEntity existing = urlRepository.findByNormalizedUrl(normalized);
        return toDto(existing);
    }
}
```

**URL Normalization:**
```java
private String normalizeUrl(String url) {
    return url.trim().toLowerCase();
}
```

---

#### 3. Generator Component
**Package:** `com.example.urlshortener.generator`

**Responsibilities:**
- Collision-free ID generation
- Base62 encoding
- Thread-safe sequence management

**Components:**
- **SnowflakeIdGenerator:**
  - Spring `@Component` singleton
  - Thread-safe via `synchronized` methods
  - Custom epoch: 2024-01-01T00:00:00Z

**Snowflake ID Structure:**
```
┌──────────────────────────────────────────────────────────────┐
│ 64-bit Long (Java long type)                                 │
├──────────────────────┬─────────────┬─────────────────────────┤
│ 41 bits              │ 10 bits     │ 13 bits                 │
│ Timestamp            │ Instance ID │ Sequence                │
│ (milliseconds since  │ (0 for MVP) │ (0-8191 per ms)        │
│  custom epoch)       │             │                         │
└──────────────────────┴─────────────┴─────────────────────────┘
```

**Base62 Encoding:**
- Character set: `0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`
- Typical output: 7 characters (e.g., `aB3xK9`)
- URL-safe, case-sensitive, human-readable

**Capacity:**
- 8,192 unique IDs per millisecond
- 69 years of operation (2024-2093)
- Billions of URLs supported

---

#### 4. Repository Layer
**Package:** `com.example.urlshortener.repository`

**Responsibilities:**
- Database CRUD operations
- Custom query methods
- JPA entity mapping

**Components:**
- **UrlRepository:** Extends `JpaRepository<UrlEntity, String>`
  - `findByNormalizedUrl(String normalizedUrl)` → Custom query for idempotency

**Entity Model:**
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

---

## Data Architecture

### Database Schema

#### Table: urls
```sql
CREATE TABLE urls (
    short_code VARCHAR(10) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_original_url_normalized 
ON urls(LOWER(TRIM(original_url)));
```

**Column Specifications:**

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| `short_code` | VARCHAR(10) | PRIMARY KEY | Base62-encoded Snowflake ID, fast lookup |
| `original_url` | TEXT | NOT NULL | Original destination URL, unlimited length |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Audit trail, future analytics |

**Index Strategy:**

1. **Primary Key Index on `short_code`:**
   - Purpose: Fast redirect lookups (O(log n))
   - Usage: `SELECT original_url FROM urls WHERE short_code = ?`

2. **Unique Expression Index on Normalized URL:**
   - Purpose: Idempotency enforcement, prevent duplicate URLs
   - Expression: `LOWER(TRIM(original_url))`
   - Benefits:
     - Case-insensitive matching
     - Whitespace-insensitive matching
     - Atomic constraint enforcement

**Data Migration Management:**

- **Tool:** Liquibase 4.25+
- **Format:** YAML changelog files
- **Location:** `src/main/resources/db/changelog/`
- **Execution:** Automated during Docker Compose startup

**Changelog Structure:**
```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-urls-table
      author: developer
      changes:
        - createTable:
            tableName: urls
            columns:
              - column:
                  name: short_code
                  type: VARCHAR(10)
                  constraints:
                    primaryKey: true
              # ... other columns
  
  - changeSet:
      id: 002-create-normalized-url-index
      author: developer
      changes:
        - sql:
            sql: CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))
```

---

## Deployment Architecture

### Docker Multi-Service Architecture

#### Service Dependency Graph
```
postgres (service healthy)
    ↓
liquibase (service completed successfully)
    ↓
app (service healthy)
```

### Container Specifications

#### 1. PostgreSQL Service
```yaml
Image: postgres:16-alpine
Purpose: Persistent data storage
Health Check: pg_isready -U urlshortener -d urlshortener
Port: 5432 (internal), mapped to host 5432
Volume: postgres_data (persistent)
```

**Configuration:**
- Database: `urlshortener`
- User: `urlshortener`
- Password: `urlshortener_pass` (dev environment only)

---

#### 2. Liquibase Service
```yaml
Image: liquibase/liquibase:4.25-alpine
Purpose: Database schema migration
Dependency: postgres (condition: service_healthy)
Execution: One-shot (exits after migrations)
Volume Mount: ./src/main/resources/db/changelog
```

**Command:**
```bash
liquibase --changelog-file=changelog/db.changelog-master.yaml \
          --driver=org.postgresql.Driver \
          --url=jdbc:postgresql://postgres:5432/urlshortener \
          --username=urlshortener \
          --password=urlshortener_pass \
          update
```

---

#### 3. Application Service
```yaml
Image: Custom (multi-stage Dockerfile)
Purpose: Spring Boot REST API
Dependency: liquibase (condition: service_completed_successfully)
Port: 8080 (internal), mapped to host 3000
Network: url-shortener-network (bridge)
```

**Environment Variables:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/urlshortener
SPRING_DATASOURCE_USERNAME=urlshortener
SPRING_DATASOURCE_PASSWORD=urlshortener_pass
SERVER_PORT=8080
SPRING_LIQUIBASE_ENABLED=false  # Migrations handled by separate service
```

---

### Multi-Stage Dockerfile Strategy

#### Stage 1: Build (Maven + JDK 21)
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B  # Cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests
```

**Benefits:**
- Dependency caching speeds up rebuilds
- Offline mode ensures reproducible builds
- Clean package ensures fresh artifacts

---

#### Stage 2: Runtime (JRE 21 Alpine)
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Benefits:**
- Minimal image size (~200MB vs ~700MB with JDK)
- No build tools in production image (security)
- Alpine Linux base (lightweight, secure)

---

### Network Architecture

```
┌────────────────────────────────────────────────────────┐
│                     Docker Host                         │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │     url-shortener-network (bridge)               │  │
│  │                                                   │  │
│  │  ┌──────────┐   ┌────────────┐   ┌──────────┐  │  │
│  │  │ postgres │   │ liquibase  │   │   app    │  │  │
│  │  │  :5432   │◄──┤  (one-shot)│◄──┤  :8080   │  │  │
│  │  └──────────┘   └────────────┘   └──────────┘  │  │
│  │       │                                  │       │  │
│  └───────┼──────────────────────────────────┼───────┘  │
│          │                                  │          │
│    ┌─────▼──────┐                     ┌────▼─────┐    │
│    │  Volume:   │                     │ Host Port│    │
│    │ postgres_  │                     │  :3000   │    │
│    │   data     │                     └──────────┘    │
│    └────────────┘                                      │
└────────────────────────────────────────────────────────┘
                                │
                                ▼
                          External Clients
                    (http://localhost:3000)
```

---

## Technical Decision Records

### TDR-001: Snowflake ID Generation (DIY Implementation)

**Status:** Approved  
**Date:** 2026-02-06

**Context:**
Need collision-free, time-sortable unique identifiers for short codes.

**Options Considered:**
1. UUID v4 (random)
2. Database auto-increment
3. Third-party Snowflake library
4. DIY Snowflake implementation

**Decision:** DIY Snowflake with Base62 encoding

**Rationale:**
- **Educational Value:** Demonstrates understanding of distributed ID generation
- **Performance:** 8,192 IDs per millisecond sufficient for MVP
- **Simplicity:** Hardcoded instance ID = 0 avoids distributed coordination
- **Scalability:** Proven pattern used by Twitter, Discord, Instagram

**Consequences:**
- Must implement thread-safe sequence counter
- Base62 encoding requires custom implementation
- Single-instance limitation (acceptable for MVP)

---

### TDR-002: Database-Enforced Idempotency

**Status:** Approved  
**Date:** 2026-02-06

**Context:**
Same URL must always return same short code, even with concurrent requests.

**Options Considered:**
1. Application-level locking (synchronized blocks)
2. Distributed lock (Redis)
3. Database UNIQUE constraint + try-catch pattern
4. Pre-check before insert

**Decision:** UNIQUE constraint with try-insert-catch-select pattern

**Rationale:**
- **Atomicity:** Database enforces constraint transactionally
- **Simplicity:** No application-level locks or external dependencies
- **Performance:** Leverages PostgreSQL's MVCC concurrency model
- **Correctness:** Impossible to violate uniqueness constraint

**Consequences:**
- Must handle `DataIntegrityViolationException` gracefully
- Normalized URL index adds storage overhead (minimal)
- Edge case: Failed insert → successful select within same transaction

---

### TDR-003: HTTP 301 Permanent Redirect

**Status:** Approved  
**Date:** 2026-02-06

**Context:**
Choose redirect status code for short URL → original URL navigation.

**Options Considered:**
1. HTTP 302 Found (temporary redirect)
2. HTTP 301 Moved Permanently (permanent redirect)
3. HTTP 307 Temporary Redirect (preserve method)

**Decision:** HTTP 301 Moved Permanently

**Rationale:**
- **Browser Caching:** Clients may cache redirect, reducing server load
- **SEO-Friendly:** Search engines pass link authority to target
- **Simplicity:** No redirect state to manage
- **Standard Practice:** Used by bit.ly, TinyURL, other URL shorteners

**Consequences:**
- Once cached, browsers bypass server for subsequent requests
- Cannot easily change destination URL (feature, not bug for MVP)
- Analytics tracking harder (out of scope anyway)

---

### TDR-004: No Caching Layer (YAGNI)

**Status:** Approved  
**Date:** 2026-02-06

**Context:**
Optimize redirect performance with caching (Redis, Caffeine, etc.).

**Options Considered:**
1. Redis cache (distributed)
2. Caffeine cache (in-memory, local)
3. No caching (PostgreSQL only)

**Decision:** No caching for MVP

**Rationale:**
- **Performance Adequate:** PostgreSQL indexed lookup < 5ms
- **Complexity Reduction:** Fewer moving parts, simpler deployment
- **Learning Focus:** Cache invalidation distracts from core concepts
- **Premature Optimization:** No performance problem to solve yet

**Consequences:**
- All redirects hit database (acceptable for MVP)
- Can add caching later via configuration change
- Database becomes single point of contention (not bottleneck at MVP scale)

---

### TDR-005: Three-Service Docker Architecture

**Status:** Approved  
**Date:** 2026-02-06

**Context:**
Orchestrate database migrations before application startup.

**Options Considered:**
1. Migrations in app startup (Spring Liquibase integration)
2. Manual migration scripts
3. Separate Liquibase service with health check dependency chain

**Decision:** Separate Liquibase service with dependency chain

**Rationale:**
- **Separation of Concerns:** Migration failures don't crash app
- **Explicit Dependencies:** `postgres → liquibase → app` order guaranteed
- **Production Pattern:** Mirrors CI/CD pipeline migration stages
- **Health Check Visibility:** Each stage reports success/failure clearly

**Consequences:**
- More complex docker-compose.yml (educational benefit)
- Must disable Spring Liquibase integration (`SPRING_LIQUIBASE_ENABLED=false`)
- Liquibase container exits after migrations (expected behavior)

---

## API Specification

### Endpoint: Create Short URL

**HTTP Method:** `POST`  
**Path:** `/api/shorten`  
**Content-Type:** `application/json`

**Request Body:**
```json
{
  "url": "https://example.com/very/long/path?query=params"
}
```

**Success Response (200 OK):**
```json
{
  "shortCode": "aB3xK9",
  "shortUrl": "http://localhost:8080/aB3xK9"
}
```

**Error Responses:**

```json
// 400 Bad Request (Invalid URL)
{
  "error": "Invalid URL format",
  "message": "URL must be a valid HTTP or HTTPS URL"
}
```

**Business Rules:**
- URL must be valid HTTP/HTTPS (validated via Java `URL()` class)
- Normalization applied: `url.toLowerCase().trim()`
- Idempotent: Same normalized URL always returns same short code

**Example:**
```bash
curl -X POST http://localhost:3000/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/spring-projects/spring-boot"}'
```

---

### Endpoint: Redirect to Original URL

**HTTP Method:** `GET`  
**Path:** `/{shortCode}`

**Success Response (301 Moved Permanently):**
```
HTTP/1.1 301 Moved Permanently
Location: https://example.com/original/path
```

**Error Response (404 Not Found):**
```json
{
  "error": "Short code not found",
  "message": "The requested short code does not exist"
}
```

**Business Rules:**
- Short code is case-sensitive
- Browser may cache redirect (HTTP 301 behavior)
- No request body required

**Example:**
```bash
curl -I http://localhost:3000/aB3xK9
# Returns:
# HTTP/1.1 301 Moved Permanently
# Location: https://github.com/spring-projects/spring-boot
```

---

## Security Considerations

### URL Validation
- **Input:** Java `URL()` class validates protocol, host, path
- **Prevents:** Malformed URLs, unsupported protocols (file://, javascript:, etc.)
- **Limitation:** Does not prevent malicious destinations (phishing sites)

### SQL Injection
- **Mitigation:** Spring Data JPA with parameterized queries
- **Pattern:** All database access via JPA repository methods
- **Risk:** Minimal (no raw SQL in application code)

### Denial of Service
- **Current State:** No rate limiting (acceptable for MVP)
- **Future Consideration:** Spring Boot Actuator rate limiting for v2.0

### Data Exposure
- **Database Credentials:** Environment variables (dev environment only)
- **Production Recommendation:** Use Docker secrets, key management service
- **URL Privacy:** All URLs publicly accessible via short codes (design decision)

---

## Scalability & Performance

### Current Capacity (MVP)

| Metric | Capacity | Measurement |
|--------|----------|-------------|
| **Max URLs** | ~69 years of IDs | 41-bit timestamp with 2024 epoch |
| **IDs per Millisecond** | 8,192 | 13-bit sequence counter |
| **Redirect Latency** | <100ms p95 | PostgreSQL indexed lookup + HTTP overhead |
| **Concurrent Requests** | Hundreds | Limited by database connection pool |

### Bottlenecks (Identified, Not Addressed in MVP)

1. **Database Connections:** Default pool size ~10 connections
2. **Single Instance:** Hardcoded instance ID = 0 prevents horizontal scaling
3. **No Caching:** All redirects query database

### Scaling Strategy (Future)

**Vertical Scaling (First Step):**
- Increase database connection pool size
- Add database read replicas for redirect queries
- PostgreSQL query caching via `shared_buffers`

**Horizontal Scaling (Multi-Instance):**
- Configure unique instance IDs (0-1023) per app instance
- Load balancer in front of app instances
- Database remains single-writer (read replicas for redirects)

**Caching Layer (Performance Optimization):**
- Redis for hot short codes (top 20% of traffic)
- TTL-based cache invalidation
- Write-through pattern (update cache on create)

---

## Future Architecture Evolution

### Version 2.0 Enhancements

#### Analytics & Tracking
```
┌────────────────────────────────────┐
│  Add: visits table                 │
│  - short_code (FK to urls)        │
│  - visited_at TIMESTAMP            │
│  - user_agent TEXT                 │
│  - referer TEXT                    │
│  - ip_address INET                 │
└────────────────────────────────────┘
```

**Implementation:**
- Async event processing (Spring ApplicationEventPublisher)
- Separate write-optimized table (append-only)
- Aggregation queries for dashboards

---

#### Link Expiration
```
┌────────────────────────────────────┐
│  Add to urls table:                │
│  - expires_at TIMESTAMP (nullable) │
└────────────────────────────────────┘
```

**Implementation:**
- Liquibase migration adds column
- Scheduled job marks expired links
- Redirect endpoint checks expiration before returning 301

---

#### Custom Short Codes
```
┌────────────────────────────────────┐
│  Add: custom_aliases table         │
│  - alias VARCHAR(50) PRIMARY KEY   │
│  - short_code VARCHAR(10) FK       │
└────────────────────────────────────┘
```

**Implementation:**
- User-specified vanity URLs (e.g., `/promo2024`)
- Alias → short code → original URL (indirection)
- Unique constraint on alias

---

### Migration Path

**From MVP to v2.0:**
1. **Backward Compatible Schema Changes:** Add columns with `DEFAULT NULL`
2. **Feature Flags:** Toggle new features via `application.yml` properties
3. **API Versioning:** Introduce `/api/v2/shorten` for new features
4. **Zero-Downtime Migration:** Blue-green deployment with Liquibase rollback capability

---

## Appendix: Technology Versions

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.x | Application framework |
| Spring Data JPA | (included) | Database access |
| Spring Web | (included) | REST controllers |
| PostgreSQL Driver | 42.7.x | JDBC connectivity |
| Liquibase | 4.25.x | Database migrations |
| Java | 21 | Language runtime |
| Maven | 3.9.x | Build tool |
| Docker | 24.0+ | Containerization |
| PostgreSQL | 16 | Database server |

---

**End of Architecture Documentation**
