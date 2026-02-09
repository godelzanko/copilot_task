# Story 2.2: Implement Thread-Safe Sequence Counter

Status: ready

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a system,
I want a thread-safe sequence counter,
so that I can generate multiple IDs per millisecond without collisions.

## Acceptance Criteria

1. **State Management**
   - [ ] Instance variable: `private long lastTimestamp = -1L`
   - [ ] Instance variable: `private long sequence = 0L`
   - [ ] Both variables protected by `synchronized` methods

2. **Sequence Increment Logic**
   - [ ] If `currentTimestamp == lastTimestamp`: increment sequence
   - [ ] If `currentTimestamp > lastTimestamp`: reset sequence to 0
   - [ ] If `currentTimestamp < lastTimestamp`: throw `ClockMovedBackwardsException` (clock skew)

3. **Sequence Overflow Handling**
   - [ ] When sequence reaches 8191 (max 13-bit value), wait for next millisecond
   - [ ] Busy-wait loop: `while (currentTimestamp == lastTimestamp) { currentTimestamp = System.currentTimeMillis() }`
   - [ ] After wait, reset sequence to 0
   - [ ] Overflow rare (requires 8192 IDs in 1ms)

4. **Clock Backwards Detection**
   - [ ] Custom exception: `ClockMovedBackwardsException extends RuntimeException`
   - [ ] Thrown when system clock moves backwards (NTP adjustment, daylight saving)
   - [ ] Exception message includes old and new timestamps

5. **Thread Safety**
   - [ ] `synchronized` keyword on ID generation method
   - [ ] Ensures only one thread modifies sequence at a time
   - [ ] Performance acceptable for MVP (lock contention unlikely)

## Tasks / Subtasks

- [ ] Task 1: Create custom exception class (AC: #4)
  - [ ] Subtask 1.1: Create `ClockMovedBackwardsException` in exception package
  - [ ] Subtask 1.2: Extend `RuntimeException`
  - [ ] Subtask 1.3: Add constructor accepting old and new timestamp parameters
  - [ ] Subtask 1.4: Format exception message: "Clock moved backwards. Refusing to generate id for {duration}ms"
  
- [ ] Task 2: Add state variables to SnowflakeId class (AC: #1)
  - [ ] Subtask 2.1: Add instance variable `private long lastTimestamp = -1L`
  - [ ] Subtask 2.2: Add instance variable `private long sequence = 0L`
  - [ ] Subtask 2.3: Convert SnowflakeId from static utility to instance-based class
  - [ ] Subtask 2.4: Add instance ID field for future multi-instance support
  
- [ ] Task 3: Implement synchronized ID generation (AC: #2, #3, #5)
  - [ ] Subtask 3.1: Create `synchronized long nextId()` method
  - [ ] Subtask 3.2: Get current timestamp: `long timestamp = System.currentTimeMillis() - EPOCH`
  - [ ] Subtask 3.3: Implement clock backwards check and throw exception if detected
  - [ ] Subtask 3.4: Implement same-timestamp logic: increment sequence
  - [ ] Subtask 3.5: Implement sequence overflow handling with busy-wait
  - [ ] Subtask 3.6: Implement new-timestamp logic: reset sequence to 0
  - [ ] Subtask 3.7: Update lastTimestamp variable
  - [ ] Subtask 3.8: Generate and return ID using bit operations from Story 2.1
  
- [ ] Task 4: Add overflow handling (AC: #3)
  - [ ] Subtask 4.1: Check if sequence exceeds MAX_SEQUENCE (8191)
  - [ ] Subtask 4.2: Implement busy-wait loop until next millisecond
  - [ ] Subtask 4.3: Reset sequence to 0 after wait
  - [ ] Subtask 4.4: Add logging for overflow events (performance monitoring)
  
- [ ] Task 5: Write single-threaded unit tests (AC: #2, #3, #4)
  - [ ] Subtask 5.1: Test sequence increments correctly within same millisecond
  - [ ] Subtask 5.2: Test sequence resets to 0 on timestamp change
  - [ ] Subtask 5.3: Test clock backwards throws ClockMovedBackwardsException
  - [ ] Subtask 5.4: Test sequence overflow triggers wait (mock time progression)
  - [ ] Subtask 5.5: Test generated IDs are unique in sequence
  
- [ ] Task 6: Write multi-threaded tests (AC: #5)
  - [ ] Subtask 6.1: Test 100 threads generating IDs concurrently produce unique values
  - [ ] Subtask 6.2: Use CountDownLatch to coordinate thread start
  - [ ] Subtask 6.3: Collect all generated IDs in thread-safe collection
  - [ ] Subtask 6.4: Assert no duplicates using Set comparison
  - [ ] Subtask 6.5: Assert all IDs are positive and within valid range
  
- [ ] Task 7: Write load tests (AC: All)
  - [ ] Subtask 7.1: Generate 10,000 IDs rapidly in single thread
  - [ ] Subtask 7.2: Verify no duplicates
  - [ ] Subtask 7.3: Verify IDs are monotonically increasing (time-sorted)
  - [ ] Subtask 7.4: Measure generation performance (IDs per second)

## Dev Notes

### 🎯 Implementation Strategy

This story adds **thread-safety and state management** to the Snowflake ID structure from Story 2.1. Key challenges:
1. Thread-safe sequence counter without excessive locking
2. Handling sequence overflow gracefully
3. Detecting and handling clock backwards scenarios

**Key Implementation Pattern:**
- Synchronized methods to protect shared state
- Busy-wait for sequence overflow (acceptable for rare edge case)
- Fail-fast on clock backwards (don't generate invalid IDs)

**Sequence Management Flow:**
```
1. Get current timestamp
2. If timestamp < lastTimestamp → THROW ClockMovedBackwardsException
3. If timestamp == lastTimestamp → sequence++
   - If sequence > MAX_SEQUENCE → busy-wait for next millisecond, reset sequence
4. If timestamp > lastTimestamp → sequence = 0
5. Update lastTimestamp
6. Generate ID: (timestamp << 22) | (instanceId << 12) | sequence
```

### 📋 Testing Checklist

- [ ] Single-threaded tests validate sequence logic
- [ ] Multi-threaded tests validate thread safety
- [ ] Load tests validate performance under stress
- [ ] Clock backwards scenario tested
- [ ] Sequence overflow scenario tested

### ⚙️ Technical Decisions

1. **Synchronization approach**: Using `synchronized` keyword for simplicity (vs. ReentrantLock)
   - **Rationale**: Lock contention unlikely in MVP single-instance deployment
   - **Future**: Could optimize with lock-free algorithms if needed

2. **Overflow handling**: Busy-wait vs. throwing exception
   - **Decision**: Busy-wait (blocks briefly until next millisecond)
   - **Rationale**: Overflow is rare; throwing exception would fail requests unnecessarily

3. **Clock backwards handling**: Throw exception vs. wait
   - **Decision**: Throw exception immediately
   - **Rationale**: Clock moving backwards indicates system configuration issue; should not silently generate potentially invalid IDs

### 🔗 Dependencies

**Before this story:**
- Story 2.1: Snowflake ID Data Structure (provides bit operations)

**After this story:**
- Story 2.3: Base62 Encoding will encode these generated IDs
- Story 2.4: Spring Component will expose this as a bean

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
- [ ] src/main/java/com/example/urlshortener/exception/ClockMovedBackwardsException.java
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdGeneratorThreadSafetyTest.java
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdGeneratorLoadTest.java

### Files Modified
- [ ] src/main/java/com/example/urlshortener/generator/SnowflakeId.java (convert to instance-based, add state variables)
- [ ] src/test/java/com/example/urlshortener/generator/SnowflakeIdTest.java (update for instance-based API)

### Files Deleted
- [ ] None
