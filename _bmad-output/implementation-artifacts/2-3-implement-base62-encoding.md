# Story 2.3: Implement Base62 Encoding

Status: ready

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a system,
I want to encode 64-bit IDs into Base62 strings,
so that short codes are URL-safe, readable, and compact.

## Acceptance Criteria

1. **Character Set**
   - [ ] Constant: `private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"`
   - [ ] 62 characters total (0-9, a-z, A-Z)
   - [ ] Case-sensitive encoding

2. **Encoding Algorithm**
   - [ ] Method signature: `public String encode(long id)`
   - [ ] Division-remainder algorithm:
     ```java
     while (id > 0) {
         result = ALPHABET.charAt(id % 62) + result;
         id = id / 62;
     }
     ```
   - [ ] Handles `id = 0` as special case (returns "0")

3. **Output Format**
   - [ ] Typical output length: 7 characters (for realistic IDs)
   - [ ] No padding (variable length based on ID value)
   - [ ] No special characters, spaces, or delimiters

4. **Decoding (Optional)**
   - [ ] Method signature: `public long decode(String encoded)`
   - [ ] Reverse algorithm: `result = result * 62 + ALPHABET.indexOf(char)`
   - [ ] Used for validation tests (not required by API)

5. **Edge Cases**
   - [ ] ID = 0 returns "0"
   - [ ] ID = 61 returns "z"
   - [ ] ID = 62 returns "10"
   - [ ] Negative IDs throw `IllegalArgumentException`

## Tasks / Subtasks

- [ ] Task 1: Create Base62Encoder class (AC: #1, #2)
  - [ ] Subtask 1.1: Create `Base62Encoder` class in generator package
  - [ ] Subtask 1.2: Define constant `BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"`
  - [ ] Subtask 1.3: Define constant `BASE = 62`
  - [ ] Subtask 1.4: Add class-level JavaDoc explaining Base62 encoding
  
- [ ] Task 2: Implement encode method (AC: #2, #3, #5)
  - [ ] Subtask 2.1: Create `public String encode(long id)` method
  - [ ] Subtask 2.2: Validate input: throw IllegalArgumentException if id < 0
  - [ ] Subtask 2.3: Handle special case: if id == 0, return "0"
  - [ ] Subtask 2.4: Implement division-remainder algorithm with StringBuilder
  - [ ] Subtask 2.5: Use StringBuilder for efficient string building
  - [ ] Subtask 2.6: Reverse StringBuilder at end or prepend during loop
  - [ ] Subtask 2.7: Return encoded string
  
- [ ] Task 3: Implement decode method (AC: #4)
  - [ ] Subtask 3.1: Create `public long decode(String encoded)` method
  - [ ] Subtask 3.2: Validate input: check for null/empty string
  - [ ] Subtask 3.3: Initialize result = 0
  - [ ] Subtask 3.4: Iterate through characters: result = result * 62 + indexOf(char)
  - [ ] Subtask 3.5: Handle invalid characters (throw IllegalArgumentException)
  - [ ] Subtask 3.6: Return decoded long value
  
- [ ] Task 4: Write encoding unit tests (AC: #2, #3, #5)
  - [ ] Subtask 4.1: Test id = 0 returns "0"
  - [ ] Subtask 4.2: Test id = 61 returns "z"
  - [ ] Subtask 4.3: Test id = 62 returns "10"
  - [ ] Subtask 4.4: Test known values produce expected Base62 strings
  - [ ] Subtask 4.5: Test large ID (near 2^63) encodes without error
  - [ ] Subtask 4.6: Test negative ID throws IllegalArgumentException
  
- [ ] Task 5: Write decoding unit tests (AC: #4)
  - [ ] Subtask 5.1: Test "0" decodes to 0
  - [ ] Subtask 5.2: Test "z" decodes to 61
  - [ ] Subtask 5.3: Test "10" decodes to 62
  - [ ] Subtask 5.4: Test null/empty string throws exception
  - [ ] Subtask 5.5: Test invalid character throws exception
  
- [ ] Task 6: Write round-trip tests (AC: #2, #4)
  - [ ] Subtask 6.1: Test encode(decode(x)) == x for various values
  - [ ] Subtask 6.2: Test decode(encode(y)) == y for various values
  - [ ] Subtask 6.3: Use property-based testing for random values
  
- [ ] Task 7: Write performance tests (AC: #3)
  - [ ] Subtask 7.1: Test encoding 10,000 IDs completes in reasonable time
  - [ ] Subtask 7.2: Measure average encoding time per ID
  - [ ] Subtask 7.3: Verify typical output length is 7 characters for realistic Snowflake IDs

## Dev Notes

### 🎯 Implementation Strategy

This story implements **Base62 encoding** to convert 64-bit Snowflake IDs into compact, URL-safe strings. The algorithm is:
1. Simple division-remainder conversion
2. Case-sensitive for maximum compactness
3. Variable-length output (no padding)

**Key Implementation Pattern:**
- Static utility methods (no state needed)
- StringBuilder for efficient string building
- Clear separation between encoding and decoding

**Algorithm Visualization:**
```
Encode 3844 (decimal):
3844 % 62 = 0  → '0'
  62 % 62 = 0  → '0'
   1 % 62 = 1  → '1'
Result: "100" (prepend order)

Decode "100":
result = 0
result = 0 * 62 + 1 = 1
result = 1 * 62 + 0 = 62
result = 62 * 62 + 0 = 3844
```

**Base62 vs Alternatives:**
- Base64: Uses special characters (+, /, =) → not URL-safe
- Base36: Only 0-9a-z → longer strings (8-9 chars vs 7)
- Base62: Perfect balance of compactness and URL-safety

### 📋 Testing Checklist

- [ ] Unit tests cover edge cases (0, small values, large values)
- [ ] Round-trip tests validate correctness
- [ ] Invalid input tests validate error handling
- [ ] Performance tests validate efficiency

### ⚙️ Technical Decisions

1. **String building approach**: StringBuilder vs string concatenation
   - **Decision**: Use StringBuilder
   - **Rationale**: Efficient for repeated character prepending

2. **Decode method necessity**: Required for API vs. testing only
   - **Decision**: Implement for testing, make public for future use
   - **Rationale**: Useful for debugging, validation, and potential future features

3. **Character ordering**: 0-9 first vs. a-z first
   - **Decision**: 0-9, then a-z, then A-Z (standard Base62)
   - **Rationale**: Consistent with common implementations, lexicographically sensible

### 🔗 Dependencies

**Before this story:**
- Story 2.2: Thread-Safe Sequence Counter (generates IDs to encode)

**After this story:**
- Story 2.4: Spring Component will use this encoder

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
- [ ] src/main/java/com/example/urlshortener/generator/Base62Encoder.java
- [ ] src/test/java/com/example/urlshortener/generator/Base62EncoderTest.java

### Files Modified
- [ ] None

### Files Deleted
- [ ] None
