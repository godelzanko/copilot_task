package com.example.urlshortener.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Spring-managed Snowflake ID generator with Base62 encoding for URL shortener.
 * 
 * <p>This component combines:
 * <ul>
 *   <li>Snowflake ID generation (64-bit time-sortable IDs)</li>
 *   <li>Base62 encoding (compact, URL-safe short codes)</li>
 *   <li>Spring dependency injection (singleton bean)</li>
 *   <li>Production-ready logging</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This component is thread-safe via synchronized methods.
 * Multiple threads can safely call {@link #generateShortCode()} concurrently.
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class UrlShortenerService {
 *     private final SnowflakeIdGenerator generator;
 *     
 *     public UrlShortenerService(SnowflakeIdGenerator generator) {
 *         this.generator = generator;
 *     }
 *     
 *     public String createShortCode() {
 *         return generator.generateShortCode();  // Returns e.g., "8M0kX"
 *     }
 * }
 * }</pre>
 * 
 * @see SnowflakeId
 * @see Base62Encoder
 */
@Component
public class SnowflakeIdGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);
    
    // Snowflake ID configuration constants
    /** Number of bits allocated for timestamp component (41 bits) */
    private static final int TIMESTAMP_BITS = 41;
    
    /** Number of bits allocated for instance ID component (10 bits) */
    private static final int INSTANCE_ID_BITS = 10;
    
    /** Number of bits allocated for sequence component (13 bits) */
    private static final int SEQUENCE_BITS = 13;
    
    /** Left shift for timestamp component (23 bits) */
    private static final int TIMESTAMP_SHIFT = INSTANCE_ID_BITS + SEQUENCE_BITS;
    
    /** Left shift for instance ID component (13 bits) */
    private static final int INSTANCE_ID_SHIFT = SEQUENCE_BITS;
    
    /** Maximum valid timestamp value: 2^41 - 1 */
    private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;
    
    /** Maximum valid sequence value: 2^13 - 1 = 8191 */
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    
    /**
     * Custom epoch: 2024-01-01T00:00:00Z in milliseconds since Unix epoch.
     * All timestamps are calculated relative to this epoch.
     */
    private static final long EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    
    // Base62 encoding configuration
    /**
     * Base62 alphabet: 0-9, a-z, A-Z (62 characters total).
     * Used for encoding Snowflake IDs to URL-safe short codes.
     */
    private static final String BASE62_ALPHABET = 
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /** Base value for Base62 encoding */
    private static final int BASE62 = 62;
    
    // Instance configuration
    /** Instance ID for this generator (hardcoded to 0 for MVP) */
    private final long instanceId;
    
    // State variables for thread-safe sequence management
    /** Last timestamp when ID was generated, in milliseconds since custom epoch */
    private long lastTimestamp = -1L;
    
    /** Sequence counter for current millisecond (0-8191) */
    private long sequence = 0L;
    
    /**
     * Default constructor for Spring dependency injection.
     * Creates a generator with instance ID = 0 (MVP configuration).
     */
    public SnowflakeIdGenerator() {
        this(0L);
    }
    
    /**
     * Constructor with custom instance ID for future multi-instance support.
     * 
     * @param instanceId the instance identifier (0-1023)
     * @throws IllegalArgumentException if instanceId is invalid
     */
    public SnowflakeIdGenerator(long instanceId) {
        if (instanceId < 0 || instanceId > ((1L << INSTANCE_ID_BITS) - 1)) {
            throw new IllegalArgumentException(
                String.format("Instance ID must be between 0 and %d, got: %d",
                    (1L << INSTANCE_ID_BITS) - 1, instanceId));
        }
        this.instanceId = instanceId;
        log.info("SnowflakeIdGenerator initialized with instanceId={}", instanceId);
    }
    
    /**
     * Generates a unique, URL-safe short code.
     * 
     * <p>This is the primary public API method. It combines:
     * <ol>
     *   <li>Generate 64-bit Snowflake ID ({@link #nextId()})</li>
     *   <li>Encode to Base62 string ({@link #encodeBase62(long)})</li>
     * </ol>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called
     * concurrently from multiple threads.
     * 
     * @return a Base62-encoded short code (typically 7 characters)
     * 
     * @example
     * <pre>
     * generator.generateShortCode() → "8M0kX"
     * generator.generateShortCode() → "8M0kY"
     * generator.generateShortCode() → "8M0kZ"
     * </pre>
     */
    public String generateShortCode() {
        long id = nextId();
        return encodeBase62(id);
    }
    
    /**
     * Generates the next unique 64-bit Snowflake ID.
     * 
     * <p>This method is thread-safe and handles:
     * <ul>
     *   <li>Sequence increment within same millisecond</li>
     *   <li>Sequence reset on new millisecond</li>
     *   <li>Sequence overflow with busy-wait until next millisecond</li>
     *   <li>Clock backwards detection with error logging</li>
     * </ul>
     * 
     * @return a unique 64-bit Snowflake ID
     * @throws com.example.urlshortener.exception.ClockMovedBackwardsException if system clock moves backwards
     */
    private synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - EPOCH;
        
        // Clock backwards detection
        if (timestamp < lastTimestamp) {
            log.error("Clock moved backwards! Refusing to generate ID. " +
                    "lastTimestamp={}, currentTimestamp={}, delta={}ms",
                    lastTimestamp, timestamp, lastTimestamp - timestamp);
            throw new com.example.urlshortener.exception.ClockMovedBackwardsException(
                    lastTimestamp, timestamp);
        }
        
        // Same millisecond - increment sequence
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            
            // Sequence overflow - wait for next millisecond
            if (sequence == 0) {
                log.warn("Sequence overflow at timestamp={}. Waiting for next millisecond. " +
                        "This indicates very high throughput (>8191 IDs/ms).",
                        timestamp);
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond - reset sequence
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // Generate ID using bit operations
        return (timestamp << TIMESTAMP_SHIFT) 
             | (instanceId << INSTANCE_ID_SHIFT) 
             | sequence;
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
     * Encodes a 64-bit ID into a Base62 string.
     * 
     * <p>Base62 encoding uses 62 characters (0-9, a-z, A-Z) to represent numbers,
     * providing compact, URL-safe strings. Typical Snowflake IDs encode to ~7 characters.
     * 
     * @param id the ID to encode (must be non-negative)
     * @return the Base62-encoded string
     * 
     * @example
     * <pre>
     * encodeBase62(0)     → "0"
     * encodeBase62(61)    → "z"
     * encodeBase62(62)    → "10"
     * </pre>
     */
    private String encodeBase62(long id) {
        if (id == 0) {
            return "0";
        }
        
        StringBuilder encoded = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE62);
            encoded.append(BASE62_ALPHABET.charAt(remainder));
            id = id / BASE62;
        }
        
        return encoded.reverse().toString();
    }
}
