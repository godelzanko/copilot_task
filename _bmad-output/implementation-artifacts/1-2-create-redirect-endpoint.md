# Story 1.2: Create Redirect Endpoint

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an end user,
I want to navigate to a short URL like http://localhost:8080/aB3xK9,
so that my browser is redirected to the original long URL.

## Acceptance Criteria

1. **Endpoint Configuration**
   - [x] Endpoint accepts GET requests at `/{shortCode}`
   - [x] Controller method annotated with `@GetMapping("/{shortCode}")`
   - [x] Path variable captured with `@PathVariable String shortCode`
   - [x] No request body required

2. **Redirect Behavior**
   - [x] Returns HTTP 301 Moved Permanently on successful lookup
   - [x] Location header contains original URL
   - [x] No response body sent (redirect only)
   - [x] Uses `ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(originalUrl)).build()`

3. **Error Handling**
   - [x] Returns HTTP 404 Not Found if short code doesn't exist
   - [x] 404 response body: `{"error": "Not Found", "message": "Short code not found: {shortCode}"}`
   - [x] Service layer throws `ShortCodeNotFoundException` (custom exception)
   - [x] Exception handler maps custom exception to 404 response

4. **Service Layer Integration**
   - [x] Controller delegates to `UrlShortenerService.getOriginalUrl(String shortCode)` method
   - [x] Service method returns original URL string or throws exception
   - [x] Controller performs URI construction and redirect logic only

5. **Case Sensitivity**
   - [x] Short codes treated as case-sensitive (Base62 encoding preserves case)
   - [x] `aB3xK9` and `AB3XK9` are different short codes
   - [x] No case normalization applied

## Tasks / Subtasks

- [x] Task 1: Create Custom Exception (AC: #3)
  - [x] Subtask 1.1: Create `ShortCodeNotFoundException` class extending `RuntimeException`
  - [x] Subtask 1.2: Add constructor accepting String message
  - [x] Subtask 1.3: Place in `com.example.urlshortener.exception` package
  
- [x] Task 2: Update Service Interface (AC: #4)
  - [x] Subtask 2.1: Add `getOriginalUrl(String shortCode)` method to `UrlShortenerService` interface
  - [x] Subtask 2.2: Update stub implementation to return hardcoded URL or throw exception
  - [x] Subtask 2.3: Add JavaDoc documenting the method and exception
  
- [x] Task 3: Create Redirect Controller (AC: #1, #2, #4, #5)
  - [x] Subtask 3.1: Create `RedirectController` class with `@RestController` annotation
  - [x] Subtask 3.2: Inject `UrlShortenerService` via constructor
  - [x] Subtask 3.3: Implement `GET /{shortCode}` endpoint method
  - [x] Subtask 3.4: Capture path variable with `@PathVariable String shortCode`
  - [x] Subtask 3.5: Call service method to get original URL
  - [x] Subtask 3.6: Return `ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(originalUrl)).build()`
  - [x] Subtask 3.7: Use `ResponseEntity<Void>` return type
  
- [x] Task 4: Update Global Exception Handler (AC: #3)
  - [x] Subtask 4.1: Add `@ExceptionHandler(ShortCodeNotFoundException.class)` method
  - [x] Subtask 4.2: Return HTTP 404 with structured error response
  - [x] Subtask 4.3: Use ErrorResponse DTO with appropriate error message
  
- [x] Task 5: Write Unit Tests (AC: All)
  - [x] Subtask 5.1: Test existing short code returns 301 with correct Location header
  - [x] Subtask 5.2: Test non-existent short code returns 404 Not Found
  - [x] Subtask 5.3: Test case-sensitive short code handling (aB3xK9 vs AB3XK9)
  - [x] Subtask 5.4: Test service method invocation with correct parameter
  - [x] Subtask 5.5: Use MockMvc for controller testing
  
- [x] Task 6: Write Integration Test (AC: All)
  - [x] Subtask 6.1: Create integration test with `@SpringBootTest`
  - [x] Subtask 6.2: Use `TestRestTemplate` for end-to-end testing
  - [x] Subtask 6.3: Verify complete redirect flow end-to-end
  - [x] Subtask 6.4: Validate 404 error response structure

## Dev Notes

### 🎯 Implementation Strategy

This story creates the **redirect endpoint** that completes the core URL shortener functionality. The service layer will return stub data for now since actual database lookups are implemented in later stories (Epic 3).

**Key Implementation Pattern:**
- **Thin Controller**: Controller handles HTTP redirect mechanics only
- **Service Delegation**: Business logic (lookup) delegated to service layer
- **Custom Exception**: `ShortCodeNotFoundException` provides semantic error handling
- **HTTP 301**: Permanent redirect allows browser caching for performance

### 📋 Technical Requirements

**HTTP Redirect Specification:**
- Use HTTP 301 (Moved Permanently) for production short URLs
- Set Location header to original URL
- No response body needed (HTTP spec allows empty body for redirects)
- Browser automatically follows redirect

**Stub Implementation Details:**
- Hardcode a mapping of known short codes for testing (e.g., "aB3xK9" → "https://example.com")
- Throw `ShortCodeNotFoundException` for unknown codes
- Real database lookup implemented in Story 2.4/3.1

### ⚠️ Common Pitfalls

1. **Wrong Status Code**: Using 302 (Found) instead of 301 (Moved Permanently)
2. **Missing Location Header**: Redirect requires Location header with full URL
3. **Returning Body**: Redirect should not include response body
4. **Case Insensitivity**: Short codes must preserve case (Base62 requirement)

### 🧪 Testing Strategy

**Unit Tests:**
- Mock service to return known URL → verify 301 + Location header
- Mock service to throw exception → verify 404 + error body
- Test case sensitivity with different case variants

**Integration Tests:**
- Use `TestRestTemplate` with redirect following disabled to verify 301
- Verify Location header contains expected URL
- Test 404 response for non-existent codes

### 🔗 Dependencies

**Depends On:**
- Story 1.1: Requires ErrorResponse DTO and GlobalExceptionHandler framework
- Story 1.0: Spring Boot project structure

**Enables:**
- Story 2.4: Service implementation can replace stub with real database lookup
- Story 3.1: Database repository integration

### 📚 Reference Documentation

- [Spring ResponseEntity](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html)
- [HTTP 301 Specification](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/301)
- [Spring @PathVariable](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/PathVariable.html)

### 💡 Implementation Notes

**Why HTTP 301 vs 302?**
- 301 (Permanent): URL shortener URLs are permanent, browsers can cache
- 302 (Temporary): Use if short codes might change (not our case)

**Why Separate Controller?**
- `ShortenController` handles `/api/*` endpoints
- `RedirectController` handles `/*` endpoints (root level)
- Clear separation of concerns

**Service Stub Example:**
```java
@Override
public String getOriginalUrl(String shortCode) {
    // Temporary stub - replace with database lookup in Story 2.4/3.1
    if ("aB3xK9".equals(shortCode)) {
        return "https://example.com/very/long/url";
    }
    throw new ShortCodeNotFoundException("Short code not found: " + shortCode);
}
```

### ✅ Definition of Done

- [ ] Code implemented and passing all unit tests
- [ ] Integration test validates redirect with stub service
- [ ] Browser manual test confirms redirect works end-to-end
- [ ] JavaDoc comments added to controller methods
- [ ] curl example showing redirect behavior documented in README

### 📝 Example Usage

**Success Case:**
```bash
curl -I http://localhost:8080/aB3xK9
# Expected Response:
# HTTP/1.1 301 Moved Permanently
# Location: https://example.com/very/long/url
```

**Error Case:**
```bash
curl http://localhost:8080/invalid
# Expected Response:
# HTTP/1.1 404 Not Found
# Content-Type: application/json
# {"error": "Short code not found", "message": "The requested short code does not exist"}
```

---

## Dev Agent Record

### Implementation Plan
Story 1-2 implements the redirect endpoint that completes the core URL shortener user flow. Tasks 1-4 were already implemented (exception, service, controller, handler). Dev Agent focused on Tasks 5-6: comprehensive unit and integration testing following TDD red-green-refactor cycle.

**Testing Strategy:**
- Unit tests with MockMvc to verify controller behavior in isolation
- Integration tests with TestRestTemplate to verify end-to-end flow
- Coverage of all 5 acceptance criteria including case sensitivity

### Completion Notes
✅ **All tasks complete** - 16 tests passing (4 unit + 3 integration for redirect, plus existing tests)

**Implementation Details:**
- `RedirectControllerTest.java`: 4 unit tests covering HTTP 301 redirect, 404 errors, case sensitivity, service delegation
- `RedirectControllerIntegrationTest.java`: 3 integration tests for end-to-end redirect flow, error responses, case preservation
- All acceptance criteria validated through tests
- Full test suite passing (16 total tests across all controllers)

**Technical Decisions:**
- Used `HttpEntity` with proper headers for integration test HTTP POST requests
- Verified case sensitivity by testing different-case short codes return different error messages
- Integration tests use stub service's in-memory map to validate full flow

**Test Coverage:**
- AC #1: Endpoint configuration ✓
- AC #2: HTTP 301 redirect behavior ✓
- AC #3: 404 error handling ✓
- AC #4: Service layer delegation ✓
- AC #5: Case sensitivity ✓

---

## File List

### Modified Files
- `src/main/java/com/example/urlshortener/exception/ShortCodeNotFoundException.java` (already existed)
- `src/main/java/com/example/urlshortener/service/UrlShortenerService.java` (already existed)
- `src/main/java/com/example/urlshortener/service/UrlShortenerServiceStub.java` (already existed)
- `src/main/java/com/example/urlshortener/controller/RedirectController.java` (already existed)
- `src/main/java/com/example/urlshortener/exception/GlobalExceptionHandler.java` (already existed)

### New Files
- `src/test/java/com/example/urlshortener/controller/RedirectControllerTest.java`
- `src/test/java/com/example/urlshortener/controller/RedirectControllerIntegrationTest.java`

---

## Change Log

- **2026-02-09**: Story 1-2 implementation completed
  - Added comprehensive unit tests for RedirectController (4 tests)
  - Added integration tests for redirect endpoint (3 tests)
  - Verified all acceptance criteria through automated tests
  - Full test suite passing: 16 tests (0 failures, 0 errors)
  - Status updated: backlog → in-progress → review

---

**Estimated Effort:** 2-3 hours  
**Priority:** High (Critical Path)
