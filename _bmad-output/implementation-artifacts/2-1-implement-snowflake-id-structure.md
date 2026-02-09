# Story 2.1: Implement Snowflake ID Data Structure

Status: ready

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a developer,
I want a 64-bit Snowflake ID structure,
so that I can generate time-sortable, collision-free identifiers.

## Acceptance Criteria

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

## Tasks / Subtasks

- [ ] Task 1: Create SnowflakeId class structure (AC: #1, #2)
  - [ ] Subtask 1.1: Create `SnowflakeId` class in generator package
  - [ ] Subtask 1.2: Define bit allocation constants (TIMESTAMP_BITS=41, INSTANCE_ID_BITS=10, SEQUENCE_BITS=13)
  - [ ] Subtask 1.3: Define bit shift constants (TIMESTAMP_SHIFT=22, INSTANCE_ID_SHIFT=12)
  - [ ] Subtask 1.4: Define max value constants for each component
  - [ ] Subtask 1.5: Define EPOCH constant as milliseconds since 2024-01-01T00:00:00Z
  
- [ ] Task 2: Implement ID generation method (AC: #3)
  - [ ] Subtask 2.1: Create `generateId(long timestamp, long instanceId, long sequence)` method
  - [ ] Subtask 2.2: Implement bit shifting: `(timestamp << 22) | (instanceId << 12) | sequence`
  - [ ] Subtask 2.3: Add parameter validation before bit operations
  - [ ] Subtask 2.4: Return generated 64-bit long ID
  
- [ ] Task 3: Implement component extraction methods (AC: #4)
  - [ ] Subtask 3.1: Implement `extractTimestamp(long id)` using right-shift and masking
  - [ ] Subtask 3.2: Implement `extractInstanceId(long id)` using right-shift and masking
  - [ ] Subtask 3.3: Implement `extractSequence(long id)` using bit masking
  - [ ] Subtask 3.4: Use unsigned right-shift (>>>) to avoid sign extension
  
- [ ] Task 4: Add validation logic (AC: #5)
  - [ ] Subtask 4.1: Validate timestamp does not exceed 2^41 - 1
  - [ ] Subtask 4.2: Validate instanceId is in range 0-1023
  - [ ] Subtask 4.3: Validate sequence is in range 0-8191
  - [ ] Subtask 4.4: Throw `IllegalArgumentException` for invalid values
  
- [ ] Task 5: Write comprehensive unit tests (AC: All)
  - [ ] Subtask 5.1: Test ID generation with known values produces expected 64-bit result
  - [ ] Subtask 5.2: Test component extraction returns correct values
  - [ ] Subtask 5.3: Test bit overflow validation throws exceptions
  - [ ] Subtask 5.4: Test epoch calculation matches expected milliseconds
  - [ ] Subtask 5.5: Test maximum timestamp value (2^41 - 1) is valid
  - [ ] Subtask 5.6: Test round-trip: generate ID and extract components match inputs
  
- [ ] Task 6: Add JavaDoc documentation (AC: All)
  - [ ] Subtask 6.1: Add class-level JavaDoc with ASCII bit layout diagram
  - [ ] Subtask 6.2: Document each method with parameters and return values
  - [ ] Subtask 6.3: Add usage examples in JavaDoc
  - [ ] Subtask 6.4: Document bit allocation and epoch information

## Dev Notes

### 🎯 Implementation Strategy

This story implements the **core Snowflake ID bit structure** without thread-safety concerns. The focus is on:
1. Correct bit manipulation operations
2. Accurate component extraction
3. Proper validation

**Key Implementation Pattern:**
- Static utility methods for ID generation and extraction
- Constants for all magic numbers (bit sizes, shifts, masks)
- Clear separation between ID generation and validation

**Bit Layout Diagram:**
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Timestamp (41 bits)                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|Timestamp| Instance ID (10) |      Sequence (13 bits)         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 📋 Testing Checklist

- [ ] Unit tests cover all bit operations
- [ ] Edge cases tested (0 values, max values)
- [ ] Validation logic tested
- [ ] Round-trip tests (generate → extract → compare)

### ⚙️ Technical Decisions

1. **Static methods vs. instance methods**: Using static utility methods for now since there's no state
2. **Validation approach**: Fail-fast with IllegalArgumentException for invalid inputs
3. **Bit masks**: Pre-compute max values as constants for readability

### 🔗 Dependencies

**Before this story:**
- Epic 1 completed (Spring Boot project initialized)

**After this story:**
- Story 2.2: Thread-Safe Sequence Counter will use these bit operations
- Story 2.3: Base62 Encoding will encode these IDs

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
- [ ] src/main/java/com/example/urlshortener/generator/SnowflakeId.java
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdTest.java

### Files Modified
- [ ] None

### Files Deleted
- [ ] None
