# Story 2.4: Create SnowflakeIdGenerator Spring Component

Status: ready

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a service layer,
I want a Spring-managed SnowflakeIdGenerator bean,
so that I can inject it and generate short codes on demand.

## Acceptance Criteria

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

## Tasks / Subtasks

- [ ] Task 1: Create SnowflakeIdGenerator component (AC: #1, #3, #4)
  - [ ] Subtask 1.1: Create `SnowflakeIdGenerator` class in generator package
  - [ ] Subtask 1.2: Add `@Component` annotation
  - [ ] Subtask 1.3: Add instance ID field `private final long instanceId`
  - [ ] Subtask 1.4: Add default constructor setting instanceId = 0
  - [ ] Subtask 1.5: Add alternative constructor accepting instanceId parameter
  - [ ] Subtask 1.6: Integrate SnowflakeId logic (from Story 2.2) into this class
  - [ ] Subtask 1.7: Integrate Base62Encoder logic (from Story 2.3) into this class
  
- [ ] Task 2: Implement generateShortCode() method (AC: #2)
  - [ ] Subtask 2.1: Create `public String generateShortCode()` method
  - [ ] Subtask 2.2: Call internal nextId() method to generate Snowflake ID
  - [ ] Subtask 2.3: Encode ID using Base62 encoder
  - [ ] Subtask 2.4: Return encoded string
  - [ ] Subtask 2.5: Method should be thread-safe (synchronized internally)
  
- [ ] Task 3: Add logging (AC: #5)
  - [ ] Subtask 3.1: Add SLF4J Logger field
  - [ ] Subtask 3.2: Log warning when sequence overflow occurs
  - [ ] Subtask 3.3: Log error when clock moves backwards
  - [ ] Subtask 3.4: Include relevant context in log messages (timestamps, sequence values)
  - [ ] Subtask 3.5: Use appropriate log levels (WARN for overflow, ERROR for clock backwards)
  
- [ ] Task 4: Write unit tests (AC: #2)
  - [ ] Subtask 4.1: Test generateShortCode() returns valid Base62 string
  - [ ] Subtask 4.2: Test multiple calls produce unique codes
  - [ ] Subtask 4.3: Test short code length is typically 7 characters
  - [ ] Subtask 4.4: Test short code contains only Base62 characters
  - [ ] Subtask 4.5: Test 1000 consecutive calls produce 1000 unique codes
  
- [ ] Task 5: Write Spring integration tests (AC: #1, #4)
  - [ ] Subtask 5.1: Create test with `@SpringBootTest`
  - [ ] Subtask 5.2: Auto-wire SnowflakeIdGenerator bean
  - [ ] Subtask 5.3: Verify bean is singleton (same instance on multiple injections)
  - [ ] Subtask 5.4: Test generateShortCode() works in Spring context
  - [ ] Subtask 5.5: Test concurrent access from multiple test threads
  
- [ ] Task 6: Write multi-threaded tests (AC: #2)
  - [ ] Subtask 6.1: Test 100 threads calling generateShortCode() concurrently
  - [ ] Subtask 6.2: Use CountDownLatch for thread coordination
  - [ ] Subtask 6.3: Collect all generated codes in thread-safe collection
  - [ ] Subtask 6.4: Assert no duplicates
  - [ ] Subtask 6.5: Assert all codes are valid Base62 strings
  
- [ ] Task 7: Update service layer to use generator (AC: #2)
  - [ ] Subtask 7.1: Inject SnowflakeIdGenerator into UrlShortenerServiceStub
  - [ ] Subtask 7.2: Replace hardcoded short code with generator.generateShortCode()
  - [ ] Subtask 7.3: Update existing service tests to work with real generator
  - [ ] Subtask 7.4: Remove stub behavior (no longer needed)

## Dev Notes

### 🎯 Implementation Strategy

This story **integrates all previous Epic 2 work** into a Spring-managed component. The generator combines:
1. Snowflake ID generation (Story 2.1, 2.2)
2. Base62 encoding (Story 2.3)
3. Spring dependency injection
4. Production-ready logging

**Key Implementation Pattern:**
- Self-contained component (no external dependencies)
- Thread-safe via synchronized methods
- Logging for operational visibility
- Simple public API: `generateShortCode()`

**Component Architecture:**
```
SnowflakeIdGenerator (@Component)
├── nextId() [synchronized] → generates 64-bit ID
│   ├── Timestamp from System.currentTimeMillis()
│   ├── Instance ID (hardcoded 0 for MVP)
│   └── Sequence counter (thread-safe)
└── generateShortCode() → encodes ID to Base62
    └── Returns URL-safe short code
```

### 📋 Testing Checklist

- [ ] Unit tests validate ID generation
- [ ] Spring integration tests validate bean lifecycle
- [ ] Multi-threaded tests validate thread safety
- [ ] Service layer integration validates real usage
- [ ] Logging tests validate observability

### ⚙️ Technical Decisions

1. **Component vs. Configuration Bean**: Using `@Component` annotation
   - **Rationale**: Simple singleton with no constructor dependencies
   - **Alternative**: Could use `@Configuration` with `@Bean` for explicit control

2. **Instance ID externalization**: Hardcoded vs. application.properties
   - **Decision**: Hardcoded 0 for MVP
   - **Future**: Could externalize to `${snowflake.instance.id:0}` for multi-instance

3. **Logging framework**: SLF4J vs. direct Logback
   - **Decision**: SLF4J for abstraction
   - **Rationale**: Allows swapping log implementation without code changes

4. **Integration approach**: Embed logic vs. delegate to separate classes
   - **Decision**: Embed SnowflakeId and Base62Encoder logic directly
   - **Rationale**: Simpler single-class design; could extract later if needed

### 🔗 Dependencies

**Before this story:**
- Story 2.1: Snowflake ID Data Structure
- Story 2.2: Thread-Safe Sequence Counter
- Story 2.3: Base62 Encoding

**After this story:**
- Service layer can generate real short codes (no more hardcoded stubs)
- Story 2.5: Database Idempotency (requires Epic 3)

## Dev Agent Record

<!-- This section will be populated by the dev agent during implementation -->

### Implementation Summary
<!-- Brief overview of what was implemented -->

### Tests Created
<!-- List of test classes and key test cases -->

### Decisions Made
<!-- Any technical decisions or deviations from the original plan -->

## File List

<!-- Updated after each task completion -->
### Files Created
- [ ] src/main/java/com/example/urlshortener/generator/SnowflakeIdGenerator.java
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdGeneratorSpringTest.java
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdGeneratorConcurrencyTest.java

### Files Modified
- [ ] src/main/java/com/example/urlshortener/service/UrlShortenerServiceStub.java (inject and use generator)
- [ ] src/test/java/com/example/urlshortener/service/UrlShortenerServiceStubTest.java (update for real generator)

### Files Deleted
- [ ] None (SnowflakeId.java and Base62Encoder.java may be merged into SnowflakeIdGenerator.java)
