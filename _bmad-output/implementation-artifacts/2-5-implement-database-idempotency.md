# Story 2.5: Implement Database-Enforced Idempotency

Status: blocked

**Blocked By:** Epic 3: Data Persistence Layer (requires database schema and repository)

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a system,
I want the database to enforce URL uniqueness,
so that concurrent requests for the same URL return identical short codes.

## Acceptance Criteria

1. **Service Layer Logic**
   - [ ] Method: `@Transactional public ShortenResponse shortenUrl(String originalUrl)`
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

## Tasks / Subtasks

**NOTE:** These tasks can only be completed after Epic 3 (Data Persistence Layer) is implemented.

- [ ] Task 1: Update database schema with UNIQUE constraint (AC: #3)
  - [ ] Subtask 1.1: Add Liquibase changeset for UNIQUE index
  - [ ] Subtask 1.2: Create expression index: `CREATE UNIQUE INDEX idx_url_normalized ON urls (LOWER(TRIM(original_url)))`
  - [ ] Subtask 1.3: Test constraint violation behavior
  - [ ] Subtask 1.4: Document index rationale in changelog
  
- [ ] Task 2: Add URL normalization utility (AC: #2)
  - [ ] Subtask 2.1: Create `normalizeUrl(String url)` method in UrlShortenerService
  - [ ] Subtask 2.2: Implement: `url.trim().toLowerCase()`
  - [ ] Subtask 2.3: Add unit tests for normalization edge cases
  - [ ] Subtask 2.4: Test empty string, whitespace-only, mixed case
  
- [ ] Task 3: Add repository custom query (AC: #5)
  - [ ] Subtask 3.1: Add method to UrlRepository: `Optional<UrlEntity> findByNormalizedUrl(String normalizedUrl)`
  - [ ] Subtask 3.2: Use `@Query` annotation with JPQL
  - [ ] Subtask 3.3: Query: `SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = :normalizedUrl`
  - [ ] Subtask 3.4: Test query returns correct entity
  
- [ ] Task 4: Implement try-insert-catch-select pattern (AC: #1, #4)
  - [ ] Subtask 4.1: Update UrlShortenerService.shortenUrl() method
  - [ ] Subtask 4.2: Add `@Transactional` annotation
  - [ ] Subtask 4.3: Normalize URL before processing
  - [ ] Subtask 4.4: Implement try block: generate code, create entity, save
  - [ ] Subtask 4.5: Implement catch block: catch DataIntegrityViolationException
  - [ ] Subtask 4.6: In catch: query for existing entity by normalized URL
  - [ ] Subtask 4.7: Return same response format for both paths
  
- [ ] Task 5: Write idempotency unit tests (AC: #1, #2, #4)
  - [ ] Subtask 5.1: Test same URL (exact match) returns same short code
  - [ ] Subtask 5.2: Test same URL (different case) returns same short code
  - [ ] Subtask 5.3: Test same URL (with whitespace) returns same short code
  - [ ] Subtask 5.4: Test different URLs get different short codes
  - [ ] Subtask 5.5: Mock repository to simulate constraint violation
  
- [ ] Task 6: Write concurrency integration tests (AC: #4)
  - [ ] Subtask 6.1: Create test with `@SpringBootTest` and `@Testcontainers`
  - [ ] Subtask 6.2: Start 10 threads requesting same URL concurrently
  - [ ] Subtask 6.3: Use CountDownLatch to coordinate simultaneous execution
  - [ ] Subtask 6.4: Collect all returned short codes
  - [ ] Subtask 6.5: Assert all short codes are identical
  - [ ] Subtask 6.6: Assert only one database row created
  
- [ ] Task 7: Add logging and metrics (AC: #1)
  - [ ] Subtask 7.1: Log when constraint violation occurs (idempotency hit)
  - [ ] Subtask 7.2: Log normalized URL for debugging
  - [ ] Subtask 7.3: Consider adding metric counter for idempotency hits
  - [ ] Subtask 7.4: Use INFO level for normal flow, WARN for unusual patterns

## Dev Notes

### 🎯 Implementation Strategy

This story implements **database-enforced idempotency** to ensure the same URL always returns the same short code, even under concurrent requests. The key pattern is:

**Try-Insert-Catch-Select Pattern:**
1. Try to insert new mapping (generate new short code)
2. If constraint violation (URL already exists), catch exception
3. Select existing mapping by normalized URL
4. Return existing short code

This pattern is **simpler than check-then-insert** because:
- No race condition (database constraint is atomic)
- No application-level locks needed
- Optimistic: assumes URL is new (common case)

**Normalization Rules:**
- Trim whitespace: " example.com " → "example.com"
- Lowercase: "Example.COM" → "example.com"
- Order matters: trim THEN lowercase

### 📋 Testing Checklist

- [ ] Unit tests validate normalization logic
- [ ] Unit tests validate try-catch flow
- [ ] Integration tests validate database constraint
- [ ] Concurrency tests validate idempotency under load
- [ ] Tests verify only one DB row per normalized URL

### ⚙️ Technical Decisions

1. **Normalization approach**: Database expression index vs. application-level
   - **Decision**: Database expression index `LOWER(TRIM(original_url))`
   - **Rationale**: Database enforces uniqueness atomically; application normalizes for query consistency

2. **Exception handling**: Try-catch vs. check-before-insert
   - **Decision**: Try-catch (optimistic approach)
   - **Rationale**: New URLs are the common case; checking first adds extra query

3. **Transaction scope**: Method-level vs. explicit transaction
   - **Decision**: `@Transactional` at service method level
   - **Rationale**: Ensures atomic try-insert-catch-select operation

4. **Normalization extent**: URL parsing vs. simple string operations
   - **Decision**: Simple trim + lowercase (no URL parsing)
   - **Rationale**: MVP simplicity; URL validation already done in controller

### 🔗 Dependencies

**Before this story:**
- Story 2.4: SnowflakeIdGenerator Component (generates short codes)
- **Epic 3: Data Persistence Layer** (BLOCKER - provides UrlEntity, UrlRepository, database schema)

**After this story:**
- Epic 2 complete
- Full idempotency guarantee in place

### 🚨 Implementation Notes

**IMPORTANT:** This story cannot be fully implemented until Epic 3 is complete because it requires:
- `UrlEntity` JPA entity
- `UrlRepository` Spring Data JPA repository
- Liquibase database schema with `urls` table
- PostgreSQL database running

**Recommended Approach:**
1. Implement Epic 3 first (database layer)
2. Return to this story for service layer integration
3. OR: Stub this story with TODO comments for now

## Dev Agent Record

<!-- This section will be populated by the dev agent during implementation -->

### Implementation Summary
<!-- Brief overview of what was implemented -->

### Tests Created
<!-- List of test classes and key test cases -->

### Decisions Made
<!-- Any technical decisions or deviations from the original plan -->

### Blocked Status
<!-- Document why story is blocked and what needs to happen to unblock -->

**Current Status:** Story design complete, implementation blocked pending Epic 3 completion.

**Unblocking Criteria:**
- [ ] UrlEntity class exists
- [ ] UrlRepository interface exists
- [ ] Database schema with urls table created
- [ ] Liquibase migrations configured

## File List

<!-- Updated after each task completion -->
### Files Created
- [ ] None yet (blocked)

### Files Modified (When Unblocked)
- [ ] src/main/java/com/example/urlshortener/service/UrlShortenerService.java (implement try-insert-catch-select)
- [ ] src/main/java/com/example/urlshortener/repository/UrlRepository.java (add custom query)
- [ ] src/main/resources/db/changelog/db.changelog-master.yaml (add UNIQUE index)
- [ ] src/test/java/com/example/urlshortener/service/UrlShortenerServiceIdempotencyTest.java (new test)

### Files Deleted
- [ ] src/main/java/com/example/urlshortener/service/UrlShortenerServiceStub.java (replace with real implementation)
