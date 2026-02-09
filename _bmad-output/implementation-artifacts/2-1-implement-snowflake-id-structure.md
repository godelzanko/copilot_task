# Story 2.1: Implement Snowflake ID Data Structure

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a developer,
I want a 64-bit Snowflake ID structure,
so that I can generate time-sortable, collision-free identifiers.

## Acceptance Criteria

1. **Bit Layout**
   - [x] 64-bit Java `long` type used for ID
   - [x] Bit allocation: 41 bits timestamp + 10 bits instance ID + 12 bits sequence (Note: AC had inconsistency - corrected to 12 bits based on explicit shifts in AC#3)
   - [x] Timestamp: milliseconds since custom epoch (2024-01-01T00:00:00Z)
   - [x] Instance ID: hardcoded 0 for MVP (supports 0-1023 range)
   - [x] Sequence: 0-4095 counter (resets every millisecond) - 12 bits per AC#3 shifts

2. **Custom Epoch**
   - [x] Constant: `EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()`
   - [x] Current timestamp calculated as `System.currentTimeMillis() - EPOCH`
   - [x] Supports 69 years from epoch (2^41 milliseconds)

3. **Bit Shifting Operations**
   - [x] Timestamp shifted left 22 bits (`timestamp << 22`)
   - [x] Instance ID shifted left 12 bits (`instanceId << 12`)
   - [x] Sequence occupies low 12 bits (no shift)
   - [x] Final ID: `(timestamp << 22) | (instanceId << 12) | sequence`

4. **Component Extraction**
   - [x] Helper method: `extractTimestamp(long id)` returns 41-bit timestamp
   - [x] Helper method: `extractInstanceId(long id)` returns 10-bit instance ID
   - [x] Helper method: `extractSequence(long id)` returns 12-bit sequence
   - [x] Extraction uses right-shift and bit masking

5. **Validation**
   - [x] Timestamp cannot exceed 41 bits (max value validation)
   - [x] Instance ID cannot exceed 10 bits (0-1023 range)
   - [x] Sequence cannot exceed 12 bits (0-4095 range) - corrected from 13 bits
   - [x] Overflow detection throws `IllegalArgumentException`

## Tasks / Subtasks

- [x] Task 1: Create SnowflakeId class structure (AC: #1, #2)
  - [x] Subtask 1.1: Create `SnowflakeId` class in generator package
  - [x] Subtask 1.2: Define bit allocation constants (TIMESTAMP_BITS=41, INSTANCE_ID_BITS=10, SEQUENCE_BITS=12)
  - [x] Subtask 1.3: Define bit shift constants (TIMESTAMP_SHIFT=22, INSTANCE_ID_SHIFT=12)
  - [x] Subtask 1.4: Define max value constants for each component
  - [x] Subtask 1.5: Define EPOCH constant as milliseconds since 2024-01-01T00:00:00Z
  
- [x] Task 2: Implement ID generation method (AC: #3)
  - [x] Subtask 2.1: Create `generateId(long timestamp, long instanceId, long sequence)` method
  - [x] Subtask 2.2: Implement bit shifting: `(timestamp << 22) | (instanceId << 12) | sequence`
  - [x] Subtask 2.3: Add parameter validation before bit operations
  - [x] Subtask 2.4: Return generated 64-bit long ID
  
- [x] Task 3: Implement component extraction methods (AC: #4)
  - [x] Subtask 3.1: Implement `extractTimestamp(long id)` using right-shift and masking
  - [x] Subtask 3.2: Implement `extractInstanceId(long id)` using right-shift and masking
  - [x] Subtask 3.3: Implement `extractSequence(long id)` using bit masking
  - [x] Subtask 3.4: Use unsigned right-shift (>>>) to avoid sign extension
  
- [x] Task 4: Add validation logic (AC: #5)
  - [x] Subtask 4.1: Validate timestamp does not exceed 2^41 - 1
  - [x] Subtask 4.2: Validate instanceId is in range 0-1023
  - [x] Subtask 4.3: Validate sequence is in range 0-4095 (corrected from 0-8191)
  - [x] Subtask 4.4: Throw `IllegalArgumentException` for invalid values
  
- [x] Task 5: Write comprehensive unit tests (AC: All)
  - [x] Subtask 5.1: Test ID generation with known values produces expected 64-bit result
  - [x] Subtask 5.2: Test component extraction returns correct values
  - [x] Subtask 5.3: Test bit overflow validation throws exceptions
  - [x] Subtask 5.4: Test epoch calculation matches expected milliseconds
  - [x] Subtask 5.5: Test maximum timestamp value (2^41 - 1) is valid
  - [x] Subtask 5.6: Test round-trip: generate ID and extract components match inputs
  
- [x] Task 6: Add JavaDoc documentation (AC: All)
  - [x] Subtask 6.1: Add class-level JavaDoc with ASCII bit layout diagram
  - [x] Subtask 6.2: Document each method with parameters and return values
  - [x] Subtask 6.3: Add usage examples in JavaDoc
  - [x] Subtask 6.4: Document bit allocation and epoch information

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

### Implementation Summary

Implemented complete Snowflake ID data structure with bit manipulation for time-sortable, collision-free 64-bit identifiers. All tasks completed following strict TDD (red-green-refactor) cycle.

**Key Implementation Details:**
- Static utility class with `generateId()` and extraction methods  
- Bit layout: 41-bit timestamp | 10-bit instance ID | 12-bit sequence
- Custom epoch: 2024-01-01T00:00:00Z
- Fail-fast validation with IllegalArgumentException
- Comprehensive JavaDoc with ASCII bit diagrams and usage examples

**AC Specification Issue Resolved:**
- AC #1 and #5 stated "13 bits sequence (0-8191)" but AC #3 specified explicit shifts that support only 12-bit sequence
- Resolved by prioritizing AC #3's explicit shift values (TIMESTAMP_SHIFT=22, INSTANCE_ID_SHIFT=12)
- Final implementation: 12-bit sequence (0-4095) matching bit shift requirements
- Tests updated to validate 12-bit sequence behavior

### Tests Created

**SnowflakeIdTest.java** - 17 comprehensive unit tests:
1. Constants validation (bit allocations, shifts, max values)
2. EPOCH constant verification  
3. ID generation with known values
4. ID generation with zeros
5. ID generation with maximum valid values
6. Timestamp extraction
7. Instance ID extraction
8. Sequence extraction  
9. Round-trip tests (generate → extract → verify)
10-12. Overflow validation (timestamp, instance ID, sequence)
13-15. Negative value validation
16. Epoch lifetime calculation (69 years)
17. Bit position verification

**Test Coverage:** All acceptance criteria validated
**Test Result:** 17/17 tests pass ✓ | Full suite: 46/46 tests pass ✓

### Decisions Made

1. **Specification Inconsistency Resolution**
   - AC #1 and #5: 13-bit sequence (0-8191)
   - AC #3: Shifts imply 12-bit sequence
   - **Decision:** Follow AC #3 explicit shifts → 12-bit sequence
   - **Rationale:** Explicit shift values (22, 12) are more authoritative than derived bit count

2. **Static Utility Class Pattern**
   - Used static methods instead of instance-based approach
   - **Rationale:** No state needed; pure bit manipulation functions
   - Private constructor prevents instantiation

3. **Validation Strategy**
   - Fail-fast with `IllegalArgumentException`
   - Separate validation methods for each component
   - **Rationale:** Clear error messages, easy to test edge cases

4. **Bit Mask Derivation**
   - Sequence mask derived from `INSTANCE_ID_SHIFT` instead of `SEQUENCE_BITS`
   - **Rationale:** Ensures consistency between shifts and extraction logic

## File List

### Files Created
- [x] src/main/java/com/example/urlshortener/generator/SnowflakeId.java
- [x] src/test/java/com/example/urlshortener/generator/SnowflakeIdTest.java

### Files Modified
- [x] None

### Files Deleted
- [x] None
