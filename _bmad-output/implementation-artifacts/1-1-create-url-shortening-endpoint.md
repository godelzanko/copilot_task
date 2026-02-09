# Story 1.1: Create URL Shortening Endpoint

Status: completed

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API client,
I want to send a long URL to POST /api/shorten,
so that I receive a short URL I can share.

## Acceptance Criteria

1. **Endpoint Configuration**
   - [x] Endpoint accepts POST requests at `/api/shorten`
   - [x] Controller method annotated with `@PostMapping("/api/shorten")`
   - [x] Consumes `application/json` Content-Type
   - [x] Produces `application/json` Content-Type

2. **Request Handling**
   - [x] Request body deserialized into DTO with single `url` field
   - [x] DTO class: `ShortenRequest` with `@NotBlank String url` field
   - [x] Spring validation annotations applied (`@Valid` on controller parameter)
   - [x] Missing URL field returns HTTP 400 with error message

3. **URL Validation**
   - [x] Java `URL()` class used to validate URL format
   - [x] Invalid URL format returns HTTP 400 Bad Request
   - [x] Error response body: `{"error": "Invalid URL format", "message": "URL must be a valid HTTP or HTTPS URL"}`
   - [x] Only HTTP and HTTPS protocols accepted

4. **Success Response**
   - [x] HTTP 200 OK status on successful shortening
   - [x] Response body contains `shortCode` and `shortUrl` fields
   - [x] Response DTO: `ShortenResponse` class with two String fields
   - [x] `shortUrl` constructed using `ServletUriComponentsBuilder.fromCurrentContextPath()`
   - [x] Example response: `{"shortCode": "aB3xK9", "shortUrl": "http://localhost:8080/aB3xK9"}`

5. **Service Layer Integration**
   - [x] Controller delegates to `UrlShortenerService.shortenUrl(String url)` method
   - [x] Service method returns DTO with short code and full URL
   - [x] Controller performs no business logic (thin controller pattern)

6. **Error Handling**
   - [x] `@ControllerAdvice` class handles exceptions globally
   - [x] `MethodArgumentNotValidException` returns HTTP 400 with validation errors
   - [x] Generic exceptions return HTTP 500 with safe error message
   - [x] Stack traces not exposed in production error responses

## Tasks / Subtasks

- [x] Task 1: Create DTO classes (AC: #2, #4)
  - [x] Subtask 1.1: Create `ShortenRequest` record in dto package
  - [x] Subtask 1.2: Add `@NotBlank` validation on url field
  - [x] Subtask 1.3: Create `ShortenResponse` record with shortCode and shortUrl fields
  - [x] Subtask 1.4: Create `ErrorResponse` record for error handling
  
- [x] Task 2: Create Service Interface and Stub Implementation (AC: #5)
  - [x] Subtask 2.1: Create `UrlShortenerService` interface in service package
  - [x] Subtask 2.2: Define `shortenUrl(String url)` method signature returning ShortenResponse
  - [x] Subtask 2.3: Create stub implementation class with `@Service` annotation
  - [x] Subtask 2.4: Stub returns hardcoded response for now (real implementation in Story 2.4)
  
- [x] Task 3: Create Controller (AC: #1, #2, #3, #4, #5)
  - [x] Subtask 3.1: Create `ShortenController` class with `@RestController` annotation
  - [x] Subtask 3.2: Add `@RequestMapping("/api")` at class level
  - [x] Subtask 3.3: Inject `UrlShortenerService` via constructor
  - [x] Subtask 3.4: Implement `POST /api/shorten` endpoint method
  - [x] Subtask 3.5: Use `@Valid` for request validation
  - [x] Subtask 3.6: Validate URL format using Java `URL()` class
  - [x] Subtask 3.7: Build full short URL using `ServletUriComponentsBuilder.fromCurrentContextPath()`
  - [x] Subtask 3.8: Return `ResponseEntity<ShortenResponse>` with HTTP 200
  
- [x] Task 4: Create Global Exception Handler (AC: #6)
  - [x] Subtask 4.1: Create `GlobalExceptionHandler` class with `@ControllerAdvice`
  - [x] Subtask 4.2: Add handler for `MethodArgumentNotValidException` → HTTP 400
  - [x] Subtask 4.3: Add handler for `MalformedURLException` → HTTP 400
  - [x] Subtask 4.4: Add generic exception handler → HTTP 500
  - [x] Subtask 4.5: Return structured error responses using ErrorResponse DTO
  
- [x] Task 5: Write Unit Tests (AC: All)
  - [x] Subtask 5.1: Test valid URL returns 200 OK with correct response structure
  - [x] Subtask 5.2: Test missing URL field returns 400 Bad Request
  - [x] Subtask 5.3: Test invalid URL format returns 400 Bad Request
  - [x] Subtask 5.4: Test service method invocation with correct parameter
  - [x] Subtask 5.5: Use MockMvc for controller testing
  
- [x] Task 6: Write Integration Test (AC: All)
  - [x] Subtask 6.1: Create integration test with `@SpringBootTest`
  - [x] Subtask 6.2: Use `TestRestTemplate` for end-to-end testing
  - [x] Subtask 6.3: Verify complete request-response flow

## Dev Notes

### 🎯 Implementation Strategy

This story creates the **API contract layer** for URL shortening. The service layer will return stub data for now since the actual business logic (ID generation, database persistence) is implemented in later stories.

**Key Implementation Pattern:**
- **Thin Controller**: No business logic, only HTTP concerns
- **Service Stub**: Hardcoded response until Epic 2 (ID generation) and Epic 3 (persistence) are complete
- **Validation First**: URL validation at controller layer before service invocation

### 🏗️ Architecture Compliance

**Package Structure (Already Exists from Story 1.0):**
```
com.example.urlshortener/
├── controller/       ← ShortenController goes here
├── service/          ← UrlShortenerService interface + stub implementation
├── dto/             ← ShortenRequest, ShortenResponse, ErrorResponse
├── config/          ← Future: GlobalExceptionHandler or separate exception package
└── UrlShortenerApplication.java
```

**API Endpoint Contract (From Architecture.md #Component-Architecture):**
```
POST /api/shorten
Content-Type: application/json

Request Body:
{
  "url": "https://example.com/very/long/path/to/resource"
}

Response (200 OK):
{
  "shortCode": "aB3xK9",
  "shortUrl": "http://localhost:8080/aB3xK9"
}

Error Response (400 Bad Request):
{
  "error": "Invalid URL format",
  "message": "URL must be a valid HTTP or HTTPS URL"
}
```

### 🔧 Technical Requirements

**Spring Boot Annotations:**
- `@RestController` - Automatic JSON serialization/deserialization
- `@RequestMapping("/api")` - Base path for all endpoints in controller
- `@PostMapping("/api/shorten")` - HTTP POST method mapping
- `@Valid` - Triggers Spring validation on request DTO
- `@ControllerAdvice` - Global exception handling

**Validation:**
- Use `jakarta.validation.constraints.@NotBlank` on DTO field
- Spring Boot Validation starter already included in pom.xml (from Story 1.0)
- URL validation using `new URL(url)` - throws MalformedURLException if invalid

**URL Construction:**
- `ServletUriComponentsBuilder.fromCurrentContextPath()` builds full URL
- Example: `http://localhost:8080` + `/{shortCode}` = `http://localhost:8080/aB3xK9`
- Handles different environments (localhost, production) automatically

**Java Records (Modern Java 17+ Feature):**
```java
// DTO as immutable record (preferred over traditional class)
public record ShortenRequest(@NotBlank String url) {}
public record ShortenResponse(String shortCode, String shortUrl) {}
public record ErrorResponse(String error, String message) {}
```

### 📦 Dependencies (Already Configured)

All required dependencies present in pom.xml from Story 1.0:
- ✅ `spring-boot-starter-web` - REST controllers, JSON serialization
- ✅ `spring-boot-starter-validation` - Bean validation (@Valid, @NotBlank)
- ✅ Jackson (transitive) - JSON processing

### 🔬 Previous Story Intelligence

**From Story 1.0 (Initialize Spring Boot Project):**

**Completed Infrastructure:**
- Spring Boot 3.2.2 application running successfully
- PostgreSQL database connected via Liquibase (3 changesets applied)
- Package structure already created: controller, service, dto, config, entity, repository
- Application starts on port 8080 (verified with `mvn spring-boot:run`)

**Environment Configuration:**
- Java 17 (downgraded from 21 due to environment constraint)
- Maven build working (`mvn clean compile` succeeds)
- Database: PostgreSQL via jdbc:postgresql://localhost:5432/urlshortener
- HikariCP connection pool configured

**Key Learnings:**
- Use `com.example.urlshortener` package (not `com.urlshortener`)
- Conventional commit messages: `feat:` prefix
- Configuration files exist: application.yml, db.changelog-master.yaml
- No Java implementation classes exist yet (only UrlShortenerApplication.java)

**Files to Build Upon:**
- `src/main/java/com/example/urlshortener/UrlShortenerApplication.java` - Main class
- `src/main/resources/application.yml` - Configuration with datasource, JPA, Liquibase
- `pom.xml` - All dependencies configured

### 🚨 Critical Developer Reminders

**MUST DO:**
1. **Use Java Records** for DTOs (ShortenRequest, ShortenResponse, ErrorResponse) - modern Java 17 feature
2. **Thin Controller Pattern** - Controller only handles HTTP, delegates to service
3. **Service Stub Implementation** - Return hardcoded response for now:
   ```java
   return new ShortenResponse("STUB123", "http://localhost:8080/STUB123");
   ```
4. **URL Validation** - Use `new URL(url)` to validate, catch MalformedURLException
5. **Global Exception Handler** - Create `@ControllerAdvice` class for consistent error responses

**WATCH OUT:**
- Don't implement actual ID generation yet (that's Story 2.4 - SnowflakeIdGenerator)
- Don't implement database persistence yet (that's Story 3.3 - JPA repository)
- Stub service should NOT throw exceptions - just return hardcoded response
- Test with `mvn test` before marking complete

**SUCCESS CRITERIA:**
- `mvn clean compile` succeeds
- `mvn test` passes all unit and integration tests
- Manual test with curl/Postman shows correct JSON response
- Error handling returns structured error messages (not stack traces)

### 🧪 Testing Requirements

**Unit Tests (Use MockMvc):**
```java
@WebMvcTest(ShortenController.class)
class ShortenControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UrlShortenerService service;
    
    @Test
    void validUrlReturns200OK() { /* ... */ }
    
    @Test
    void missingUrlReturns400() { /* ... */ }
    
    @Test
    void invalidUrlFormatReturns400() { /* ... */ }
}
```

**Integration Test (Use TestRestTemplate):**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShortenControllerIntegrationTest {
    @Autowired private TestRestTemplate restTemplate;
    
    @Test
    void endToEndShortenFlow() { /* ... */ }
}
```

**Manual Testing:**
```bash
# Valid request
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/test"}'

# Expected: {"shortCode":"STUB123","shortUrl":"http://localhost:8080/STUB123"}

# Invalid URL
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "not-a-valid-url"}'

# Expected: {"error":"Invalid URL format","message":"URL must be a valid HTTP or HTTPS URL"}
```

### 📚 References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-1.1-Create-URL-Shortening-Endpoint]
- [Source: _bmad-output/planning-artifacts/architecture.md#Controller-Layer]
- [Source: _bmad-output/planning-artifacts/architecture.md#Service-Layer]
- [Source: _bmad-output/implementation-artifacts/1-0-initialize-spring-boot-project.md#Dev-Notes]
- [Spring Boot Documentation: Building REST Services](https://spring.io/guides/tutorials/rest/)
- [Spring Framework: Validation](https://docs.spring.io/spring-framework/reference/core/validation.html)

### 🔗 Cross-Story Dependencies

**Depends On:**
- ✅ Story 1.0: Initialize Spring Boot Project (COMPLETED - in review)

**Blocks:**
- Story 1.2: Create Redirect Endpoint (needs service interface)
- Story 2.4: Create SnowflakeIdGenerator Spring Component (will replace stub)
- Story 3.3: Implement JPA Entity and Repository (will enable real persistence)

**Integration Points:**
- Epic 2 will replace stub service with real ID generation
- Epic 3 will add database persistence to service layer
- This story establishes API contract that won't change

### 💡 Implementation Tips

**Java 17 Records Pattern:**
```java
// Modern immutable DTO - no getters/setters needed
public record ShortenRequest(
    @NotBlank(message = "URL is required") 
    String url
) {}
```

**ServletUriComponentsBuilder Usage:**
```java
String shortUrl = ServletUriComponentsBuilder
    .fromCurrentContextPath()
    .path("/{shortCode}")
    .buildAndExpand(shortCode)
    .toUriString();
```

**URL Validation Pattern:**
```java
try {
    URL validatedUrl = new URL(url);
    String protocol = validatedUrl.getProtocol();
    if (!protocol.equals("http") && !protocol.equals("https")) {
        throw new IllegalArgumentException("Only HTTP and HTTPS protocols are supported");
    }
} catch (MalformedURLException e) {
    throw new IllegalArgumentException("Invalid URL format", e);
}
```

### 🎓 Learning Objectives

This story demonstrates:
- **Spring Boot REST API** development patterns
- **Thin Controller** architecture (separation of concerns)
- **Bean Validation** with `@Valid` and constraint annotations
- **Global Exception Handling** with `@ControllerAdvice`
- **Java Records** for immutable DTOs
- **Test-Driven Development** with MockMvc and TestRestTemplate

### 🔄 Next Story Preview

**Story 1.2: Create Redirect Endpoint**
- Will implement `GET /{shortCode}` endpoint
- HTTP 301 redirect to original URL
- Reuses `UrlShortenerService` interface created in this story
- Service stub will return hardcoded redirect URL

---

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.5

### Debug Log References

N/A - Implementation completed without issues

### Completion Notes List

**Implementation Summary:**
- ✅ Created 3 DTO records using Java 17 Records pattern (ShortenRequest, ShortenResponse, ErrorResponse)
- ✅ Implemented UrlShortenerService interface and UrlShortenerServiceStub with hardcoded response ("STUB123")
- ✅ Created ShortenController with thin controller pattern - delegates to service layer
- ✅ Implemented URL validation using Java URL() class with HTTP/HTTPS protocol enforcement
- ✅ Created GlobalExceptionHandler with @ControllerAdvice for consistent error responses
- ✅ Built full short URL using ServletUriComponentsBuilder.fromCurrentContextPath()
- ✅ Written 6 comprehensive unit tests using @WebMvcTest and MockMvc (all passing)
- ✅ Written 3 integration tests using @SpringBootTest and TestRestTemplate (all passing)
- ✅ Full test suite: 9/9 tests passing with no regressions

**Test Results:**
- Unit Tests: 6/6 passing (ShortenControllerTest)
- Integration Tests: 3/3 passing (ShortenControllerIntegrationTest)
- Total: 9/9 tests passing
- Build: SUCCESS
- Code Coverage: Generated via JaCoCo

**Technical Decisions:**
- Used Java 17 Records for immutable DTOs (modern Java feature)
- Implemented thin controller pattern - no business logic in controller
- Service stub returns hardcoded response as specified (will be replaced in Epic 2 & 3)
- Validation errors handled consistently via @ControllerAdvice
- Error responses never expose stack traces (production-safe)

**All Acceptance Criteria Satisfied:**
- AC #1: Endpoint configuration ✅
- AC #2: Request handling ✅
- AC #3: URL validation ✅
- AC #4: Success response ✅
- AC #5: Service layer integration ✅
- AC #6: Error handling ✅

### File List

**Files Created/Modified:**
- src/main/java/com/example/urlshortener/dto/ShortenRequest.java (NEW)
- src/main/java/com/example/urlshortener/dto/ShortenResponse.java (NEW)
- src/main/java/com/example/urlshortener/dto/ErrorResponse.java (NEW)
- src/main/java/com/example/urlshortener/service/UrlShortenerService.java (NEW - interface)
- src/main/java/com/example/urlshortener/service/UrlShortenerServiceStub.java (NEW - stub impl)
- src/main/java/com/example/urlshortener/controller/ShortenController.java (NEW)
- src/main/java/com/example/urlshortener/exception/GlobalExceptionHandler.java (NEW)
- src/test/java/com/example/urlshortener/controller/ShortenControllerTest.java (NEW)
- src/test/java/com/example/urlshortener/controller/ShortenControllerIntegrationTest.java (NEW)

## Change Log

- **2026-02-09**: Implemented URL shortening endpoint with full test coverage (9/9 tests passing). Created DTOs, service interface/stub, controller, global exception handler, and comprehensive unit/integration tests. All acceptance criteria satisfied.
