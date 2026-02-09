package com.example.urlshortener.generator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Snowflake ID Structure - 64-bit time-sortable, collision-free identifier.
 * 
 * <p>Bit Layout (64 bits total):
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Timestamp (41 bits)                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Timestamp| Instance ID (10) |      Sequence (13 bits)         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <p>Components:
 * <ul>
 *   <li><b>Timestamp (41 bits):</b> Milliseconds since custom epoch (2024-01-01T00:00:00Z).
 *       Supports ~69 years (2^41 milliseconds).</li>
 *   <li><b>Instance ID (10 bits):</b> Identifies the generator instance (0-1023).
 *       Hardcoded to 0 for MVP.</li>
 *   <li><b>Sequence (13 bits):</b> Per-millisecond counter (0-8191).
 *       Resets every millisecond.</li>
 * </ul>
 * 
 * <p>Thread Safety:
 * This class is thread-safe. ID generation is synchronized to ensure no duplicate IDs
 * are generated even under concurrent access.
 * 
 * <p>Usage Example:
 * <pre>{@code
 * SnowflakeId generator = new SnowflakeId(0); // instance ID 0 for MVP
 * long id1 = generator.nextId();
 * long id2 = generator.nextId();
 * 
 * // Extract components
 * long extractedTimestamp = SnowflakeId.extractTimestamp(id1);
 * long extractedInstanceId = SnowflakeId.extractInstanceId(id1);
 * long extractedSequence = SnowflakeId.extractSequence(id1);
 * }</pre>
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Snowflake_ID">Snowflake ID</a>
 */
public class SnowflakeId {

    // Bit allocation constants (AC #1)
    /** Number of bits allocated for timestamp component (41 bits) */
    public static final int TIMESTAMP_BITS = 41;
    
    /** Number of bits allocated for instance ID component (10 bits) */
    public static final int INSTANCE_ID_BITS = 10;
    
    /** Number of bits allocated for sequence component (13 bits per Story 2.2 AC#3) */
    public static final int SEQUENCE_BITS = 13;

    // Bit shift constants (AC #3 - explicit shifts)
    /** Left shift for timestamp component (23 bits for 13-bit sequence + 10-bit instance) */
    public static final int TIMESTAMP_SHIFT = 23;
    
    /** Left shift for instance ID component (13 bits for 13-bit sequence) */
    public static final int INSTANCE_ID_SHIFT = 13;

    // Maximum values for validation (AC #1)
    /** Maximum valid timestamp value: 2^41 - 1 */
    public static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;
    
    /** Maximum valid instance ID value: 2^10 - 1 = 1023 */
    public static final long MAX_INSTANCE_ID = (1L << INSTANCE_ID_BITS) - 1;
    
    /** Maximum valid sequence value: 2^13 - 1 = 8191 */
    public static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;  // 8191

    // Custom epoch constant (AC #2)
    /**
     * Custom epoch: 2024-01-01T00:00:00Z in milliseconds since Unix epoch.
     * All timestamps are calculated relative to this epoch.
     * Supports ~69 years from this date (2^41 milliseconds = ~69.7 years).
     */
    public static final long EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();

    // Bit masks for extraction (AC #4)
    /** Bit mask for extracting timestamp (41 bits) */
    private static final long TIMESTAMP_MASK = MAX_TIMESTAMP;
    
    /** Bit mask for extracting instance ID (10 bits) */
    private static final long INSTANCE_ID_MASK = MAX_INSTANCE_ID;
    
    /** Bit mask for extracting sequence (13 bits) */
    private static final long SEQUENCE_MASK = MAX_SEQUENCE;

    // State variables for thread-safe sequence management (Story 2.2 AC #1)
    /** Instance ID for this generator (0-1023) */
    private final long instanceId;
    
    /** Last timestamp when ID was generated, in milliseconds since custom epoch */
    private long lastTimestamp = -1L;
    
    /** Sequence counter for current millisecond (0-8191) */
    private long sequence = 0L;

    /**
     * Constructs a new SnowflakeId generator.
     * 
     * @param instanceId the instance identifier (0-1023) for this generator
     * @throws IllegalArgumentException if instanceId is invalid
     */
    public SnowflakeId(long instanceId) {
        validateInstanceId(instanceId);
        this.instanceId = instanceId;
    }

    /**
     * Generates the next unique 64-bit Snowflake ID.
     * 
     * <p>This method is thread-safe and handles:
     * <ul>
     *   <li>Sequence increment within same millisecond</li>
     *   <li>Sequence reset on new millisecond</li>
     *   <li>Sequence overflow with busy-wait until next millisecond</li>
     *   <li>Clock backwards detection with exception</li>
     * </ul>
     * 
     * @return a unique 64-bit Snowflake ID
     * @throws com.example.urlshortener.exception.ClockMovedBackwardsException if system clock moves backwards
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        
        // AC #4: Clock backwards detection
        if (timestamp < lastTimestamp) {
            throw new com.example.urlshortener.exception.ClockMovedBackwardsException(
                    lastTimestamp, timestamp);
        }
        
        // AC #2: Same millisecond - increment sequence
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            
            // AC #3: Sequence overflow - wait for next millisecond
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // AC #2: New millisecond - reset sequence
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // Generate ID using bit operations from Story 2.1
        return generateId(timestamp, instanceId, sequence);
    }

    /**
     * Busy-waits until the next millisecond.
     * 
     * @param lastTimestamp the last timestamp to wait past
     * @return the new timestamp
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() - EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }

    /**
     * Generates a 64-bit Snowflake ID from components.
     * 
     * <p>Performs bit shifting and OR operations to combine components:
     * <pre>
     * ID = (timestamp &lt;&lt; 23) | (instanceId &lt;&lt; 13) | sequence
     * </pre>
     * 
     * @param timestamp milliseconds since custom epoch (0 to 2^41-1)
     * @param instanceId generator instance identifier (0 to 1023)
     * @param sequence per-millisecond counter (0 to 8191)
     * @return 64-bit Snowflake ID as Java long
     * @throws IllegalArgumentException if any component exceeds its bit allocation
     *         or is negative
     */
    public static long generateId(long timestamp, long instanceId, long sequence) {
        // AC #5: Validation - fail fast with descriptive errors
        validateTimestamp(timestamp);
        validateInstanceId(instanceId);
        validateSequence(sequence);

        // Bit shifting operations for 13-bit sequence
        return (timestamp << TIMESTAMP_SHIFT) 
             | (instanceId << INSTANCE_ID_SHIFT) 
             | sequence;
    }

    /**
     * Extracts the timestamp component from a Snowflake ID.
     * 
     * <p>Uses unsigned right-shift and bit masking:
     * <pre>
     * timestamp = (id &gt;&gt;&gt; 23) &amp; TIMESTAMP_MASK
     * </pre>
     * 
     * @param id the 64-bit Snowflake ID
     * @return timestamp in milliseconds since custom epoch (41 bits)
     */
    public static long extractTimestamp(long id) {
        // AC #4: Component extraction using right-shift and masking
        return (id >>> TIMESTAMP_SHIFT) & TIMESTAMP_MASK;
    }

    /**
     * Extracts the instance ID component from a Snowflake ID.
     * 
     * <p>Uses unsigned right-shift and bit masking:
     * <pre>
     * instanceId = (id &gt;&gt;&gt; 13) &amp; INSTANCE_ID_MASK
     * </pre>
     * 
     * @param id the 64-bit Snowflake ID
     * @return instance identifier (10 bits, range 0-1023)
     */
    public static long extractInstanceId(long id) {
        // AC #4: Component extraction using right-shift and masking
        return (id >>> INSTANCE_ID_SHIFT) & INSTANCE_ID_MASK;
    }

    /**
     * Extracts the sequence component from a Snowflake ID.
     * 
     * <p>Uses bit masking (no shift needed - sequence is in low 13 bits):
     * <pre>
     * sequence = id &amp; SEQUENCE_MASK
     * </pre>
     * 
     * @param id the 64-bit Snowflake ID
     * @return sequence counter value (13 bits, range 0-8191)
     */
    public static long extractSequence(long id) {
        // AC #4: Component extraction using masking only (no shift)
        return id & SEQUENCE_MASK;
    }

    /**
     * Validates timestamp component.
     * 
     * @param timestamp the timestamp value to validate
     * @throws IllegalArgumentException if timestamp is negative or exceeds 41 bits
     */
    private static void validateTimestamp(long timestamp) {
        if (timestamp < 0) {
            throw new IllegalArgumentException(
                "Timestamp cannot be negative: " + timestamp);
        }
        if (timestamp > MAX_TIMESTAMP) {
            throw new IllegalArgumentException(
                String.format("Timestamp exceeds 41 bits: %d (max: %d)", 
                    timestamp, MAX_TIMESTAMP));
        }
    }

    /**
     * Validates instance ID component.
     * 
     * @param instanceId the instance ID value to validate
     * @throws IllegalArgumentException if instance ID is negative or exceeds 10 bits (1023)
     */
    private static void validateInstanceId(long instanceId) {
        if (instanceId < 0) {
            throw new IllegalArgumentException(
                "Instance ID cannot be negative: " + instanceId);
        }
        if (instanceId > MAX_INSTANCE_ID) {
            throw new IllegalArgumentException(
                String.format("Instance ID exceeds 10 bits (0-1023): %d", instanceId));
        }
    }

    /**
     * Validates sequence component.
     * 
     * @param sequence the sequence value to validate
     * @throws IllegalArgumentException if sequence is negative or exceeds 13 bits (8191)
     */
    private static void validateSequence(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException(
                "Sequence cannot be negative: " + sequence);
        }
        if (sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                String.format("Sequence exceeds 13 bits (0-8191): %d", sequence));
        }
    }
}
