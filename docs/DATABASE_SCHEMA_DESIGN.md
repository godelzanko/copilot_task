# Database Schema Design Documentation

**Story:** 3.1 - Design and Create Database Schema  
**Date:** 2026-02-06  
**Status:** Implemented and Validated

## Overview

This document explains the design decisions for the URL Shortener database schema, including rationale for column types, indexing strategy, and normalization approach.

---

## Table: urls

```sql
CREATE TABLE urls (
    short_code VARCHAR(10) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Application-level normalization index (ChangeSet 004)
CREATE UNIQUE INDEX idx_original_url_normalized ON urls(original_url);
```

---

## Design Decisions

### 1. VARCHAR(10) for short_code

**Decision:** Use `VARCHAR(10)` instead of smaller types (VARCHAR(7), CHAR(7))

**Rationale:**
- **Base62 Encoding:** Snowflake IDs typically encode to ~7 characters
- **Safety Margin:** VARCHAR(10) provides buffer for:
  - Future ID space expansion
  - Potential encoding changes
  - Edge cases in timestamp-based generation
- **Performance:** VARCHAR length doesn't affect index size significantly in PostgreSQL
- **Flexibility:** No need to change schema if encoding strategy evolves

**Source:** TDR-001 (Architecture.md lines 465-491), Story 3.1 AC #4

---

### 2. TEXT vs VARCHAR for original_url

**Decision:** Use `TEXT` instead of `VARCHAR(n)` for original URLs

**Rationale:**
- **Unlimited Length:** TEXT supports URLs up to 1GB in PostgreSQL
- **No Arbitrary Limit:** VARCHAR(2000) would fail on very long URLs (query params, deep paths)
- **Storage Efficiency:** Both TEXT and VARCHAR use varlena storage - no performance difference
- **PostgreSQL Best Practice:** TEXT is preferred for variable-length strings with unknown max length

**Performance Notes:**
- Index on TEXT is efficient (indexes first N bytes)
- No storage overhead compared to VARCHAR
- Retrieval speed identical for typical URL lengths (<500 chars)

**Source:** Story 3.1 AC #4, PostgreSQL 16 documentation

---

### 3. TIMESTAMP vs TIMESTAMPTZ for created_at

**Decision:** Use `TIMESTAMP` (without timezone) assuming UTC

**Rationale:**
- **Simplicity:** Application controls timezone, stores UTC consistently
- **No Session Dependency:** TIMESTAMPTZ converts based on PostgreSQL session timezone setting
- **Predictable Behavior:** TIMESTAMP stores value as-is (no conversion surprises)
- **Performance:** Marginally faster (no timezone conversion on retrieval)

**Assumptions:**
- Application always writes UTC timestamps
- created_at is immutable (`updatable = false` in JPA entity)
- Future analytics convert to user timezone at presentation layer

**Alternative Considered:**
- TIMESTAMPTZ would be better if multiple systems write with different timezones
- Current MVP has single Spring Boot app writing UTC → TIMESTAMP sufficient

**Source:** Story 3.1 AC #4, Architecture.md lines 258-277

---

### 4. Application-Level Normalization Strategy (CRITICAL)

**Decision:** Changed from database expression index to application-level normalization

**Original Design (ChangeSet 002):**
```sql
CREATE UNIQUE INDEX idx_original_url_normalized 
ON urls(LOWER(TRIM(original_url)));
```

**Current Design (ChangeSet 004):**
```sql
CREATE UNIQUE INDEX idx_original_url_normalized 
ON urls(original_url);
```

**Rationale for Change:**
- **Application Control:** Service layer normalizes URLs before database operations
- **Flexibility:** Normalization logic can evolve without schema migration
- **Performance:** Avoid double normalization (application + database expression)
- **Testing:** Easier to unit test normalization in Java code

**Consequences:**
- **Service Layer Responsibility:** Must ALWAYS normalize URLs (lowercase + trim) before insert/query
- **JPA Repository:** Queries must use pre-normalized URLs
- **Risk:** If normalization skipped, duplicate URLs bypass unique constraint

**Implementation Requirements:**
```java
// Service layer MUST normalize before database operations
// Uses RFC 3986 normalization: lowercases scheme and host only, preserves path case
private String normalizeUrl(String url) {
    try {
        URI uri = new URI(url.trim());
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : null;
        String host = uri.getHost() != null ? uri.getHost().toLowerCase() : null;
        
        return new URI(
            scheme,
            uri.getUserInfo(),
            host,
            uri.getPort(),
            uri.getPath(),
            uri.getQuery(),
            uri.getFragment()
        ).toString();
    } catch (URISyntaxException e) {
        return url.trim();
    }
}
```

**Rollback Capability:**
- ChangeSet 004 includes rollback script
- Can restore expression-based index if application normalization proves problematic

**Source:** db.changelog-master.yaml ChangeSet 004, TDR-002 (Architecture.md lines 494-520)

---

### 5. Single-Table Design (No Foreign Keys)

**Decision:** Single `urls` table with no relationships

**Rationale:**
- **Simplicity:** URL shortener domain has one entity (URL mapping)
- **Performance:** No JOIN queries needed for core operations
- **Scalability:** Easier to shard/partition single table
- **No Cascade Complexity:** No delete propagation logic required

**Future Considerations:**
- Analytics data (clicks, referrers) would go in separate table
- User/tenant data would be separate table with foreign key to urls
- Current MVP scope doesn't require multi-table design

**Source:** Story 3.1 AC #5, Architecture.md single-table design

---

## Index Strategy

### Primary Key Index (Automatic)

```sql
-- Automatically created by PRIMARY KEY constraint
CREATE UNIQUE INDEX urls_pkey ON urls(short_code);
```

**Properties:**
- B-tree index (PostgreSQL default)
- O(log n) lookup time
- Supports fast redirect queries: `SELECT original_url FROM urls WHERE short_code = ?`
- Enforces uniqueness of short codes

**Performance:**
- ~12 disk reads for 1M rows
- ~20 disk reads for 1B rows
- Cache-friendly for hot short codes

**Source:** Story 3.1 AC #2, Architecture.md lines 280-282

---

### Unique Index on original_url

```sql
CREATE UNIQUE INDEX idx_original_url_normalized ON urls(original_url);
```

**Purpose:**
- Enforce idempotency (same URL → same short code)
- Prevent duplicate URLs in database
- Support try-insert-catch-select pattern (TDR-002)

**Usage Pattern:**
```java
try {
    // Attempt insert
    urlRepository.save(new UrlEntity(shortCode, normalizedUrl));
} catch (DataIntegrityViolationException e) {
    // Duplicate URL - fetch existing short code
    UrlEntity existing = urlRepository.findByOriginalUrl(normalizedUrl);
    return existing.getShortCode();
}
```

**Performance:**
- Index scan for idempotency check: O(log n)
- Unique constraint enforced atomically (no race conditions)
- Minimal storage overhead (~50 bytes per URL)

**Source:** Story 3.1 AC #3, TDR-002 (Architecture.md lines 494-520)

---

## Migration Management

### Liquibase Changesets

1. **001-create-urls-table:** Base table structure
2. **002-create-normalized-url-index:** Original expression-based index
3. **003-add-table-comments:** Documentation for schema introspection
4. **004-update-url-index-for-app-normalization:** **Critical change** to application normalization

**Execution:**
- Automated via Spring Boot Liquibase integration
- Runs on application startup (or Docker Compose startup)
- Changesets tracked in `databasechangelog` table (prevents re-execution)

**File:** `src/main/resources/db/changelog/db.changelog-master.yaml`

---

## Validation Results

### Schema Correctness
- ✅ Table name: `urls` (AC #1)
- ✅ Columns match specifications exactly (AC #1)
- ✅ Primary key on `short_code` (AC #2)
- ✅ Unique index on `original_url` (AC #3, modified in ChangeSet 004)
- ✅ Column types match PostgreSQL 16 best practices (AC #4)
- ✅ Single-table design (AC #5)

### Architecture Compliance
- ✅ Matches Architecture.md lines 258-268 (with ChangeSet 004 modification)
- ✅ Supports TDR-001 Snowflake ID strategy (lines 465-491)
- ✅ Supports TDR-002 database-enforced idempotency (lines 494-520)
- ✅ Compatible with Spring Data JPA 3.2.x mapping

---

## Next Steps (Story 3.3)

### JPA Entity Mapping

```java
@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_original_url_normalized", columnList = "original_url", unique = true)
})
public class UrlEntity {
    @Id
    @Column(name = "short_code", length = 10, nullable = false)
    private String shortCode;
    
    @Column(name = "original_url", columnDefinition = "TEXT", nullable = false)
    private String originalUrl;  // MUST be pre-normalized by service layer
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Timestamp createdAt;
    
    // Constructors, getters, setters
}
```

**Critical Notes:**
- `originalUrl` field stores already-normalized URLs (lowercase + trim)
- Service layer responsible for normalization before calling repository
- JPA entity does NOT normalize - receives pre-normalized values

---

## References

- [Source: _bmad-output/planning-artifacts/architecture.md#Data Architecture (lines 254-323)]
- [Source: _bmad-output/planning-artifacts/architecture.md#TDR-001 (lines 465-491)]
- [Source: _bmad-output/planning-artifacts/architecture.md#TDR-002 (lines 494-520)]
- [Source: src/main/resources/db/changelog/db.changelog-master.yaml]
- [Source: Story 3.1 Implementation Story]
