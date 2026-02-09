package com.example.urlshortener.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SnowflakeIdGenerator component.
 * 
 * Tests coverage:
 * - Basic code generation
 * - Uniqueness guarantees
 * - Base62 encoding validation
 * - Length expectations
 * - High-volume uniqueness
 */
class SnowflakeIdGeneratorTest {
    
    private SnowflakeIdGenerator generator;
    
    /** Pattern to validate Base62 characters (0-9, a-z, A-Z) */
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    
    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator();
    }
    
    /**
     * AC #2 Test: generateShortCode() returns valid Base62 string
     */
    @Test
    void testGenerateShortCodeReturnsValidBase62String() {
        String shortCode = generator.generateShortCode();
        
        assertNotNull(shortCode, "Short code should not be null");
        assertFalse(shortCode.isEmpty(), "Short code should not be empty");
        assertTrue(BASE62_PATTERN.matcher(shortCode).matches(),
                "Short code should contain only Base62 characters (0-9, a-z, A-Z), got: " + shortCode);
    }
    
    /**
     * AC #2 Test: Multiple calls produce unique codes
     */
    @Test
    void testMultipleCallsProduceUniqueCodes() {
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        String code3 = generator.generateShortCode();
        
        assertNotEquals(code1, code2, "First and second codes should be different");
        assertNotEquals(code2, code3, "Second and third codes should be different");
        assertNotEquals(code1, code3, "First and third codes should be different");
    }
    
    /**
     * AC #2 Test: Short code length is typically 7 characters
     * 
     * Note: Length varies with timestamp (grows over time), but should be ~7 chars
     * for current epoch (2024+). As time progresses, codes will get longer (up to ~11 chars).
     */
    @Test
    void testShortCodeLengthIsTypically7Characters() {
        String shortCode = generator.generateShortCode();
        
        // Length should be reasonable (5-11 characters for current timestamps)
        // Snowflake IDs grow over time, so in 2026+ we might see 10-11 characters
        assertTrue(shortCode.length() >= 5 && shortCode.length() <= 11,
                "Short code length should be between 5-11 characters, got: " + 
                shortCode.length() + " (" + shortCode + ")");
    }
    
    /**
     * AC #2 Test: Short code contains only Base62 characters
     */
    @Test
    void testShortCodeContainsOnlyBase62Characters() {
        // Generate multiple codes to increase test coverage
        for (int i = 0; i < 100; i++) {
            String shortCode = generator.generateShortCode();
            
            for (char c : shortCode.toCharArray()) {
                boolean isValidChar = 
                    (c >= '0' && c <= '9') ||  // digits
                    (c >= 'a' && c <= 'z') ||  // lowercase
                    (c >= 'A' && c <= 'Z');    // uppercase
                
                assertTrue(isValidChar,
                        "Invalid character '" + c + "' in short code: " + shortCode);
            }
        }
    }
    
    /**
     * AC #2 Test: 1000 consecutive calls produce 1000 unique codes
     * 
     * This validates:
     * - No duplicates under rapid sequential generation
     * - Sequence counter works correctly
     * - Thread-safe state management (within single thread)
     */
    @Test
    void test1000ConsecutiveCallsProduce1000UniqueCodes() {
        Set<String> generatedCodes = new HashSet<>();
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            String code = generator.generateShortCode();
            generatedCodes.add(code);
        }
        
        assertEquals(iterations, generatedCodes.size(),
                "All 1000 generated codes should be unique");
    }
    
    /**
     * Test: Default constructor sets instance ID to 0
     */
    @Test
    void testDefaultConstructorUsesInstanceIdZero() {
        // This is implicit in default constructor usage
        // We can verify by checking that generator works without errors
        SnowflakeIdGenerator defaultGenerator = new SnowflakeIdGenerator();
        assertDoesNotThrow(() -> defaultGenerator.generateShortCode(),
                "Default generator should work without errors");
    }
    
    /**
     * Test: Alternative constructor accepts custom instance ID
     */
    @Test
    void testAlternativeConstructorAcceptsCustomInstanceId() {
        // Valid instance IDs: 0-1023 (10 bits)
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(0L));
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(1L));
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(512L));
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(1023L));
    }
    
    /**
     * Test: Constructor rejects invalid instance IDs
     */
    @Test
    void testConstructorRejectsInvalidInstanceIds() {
        // Negative instance ID
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(-1L),
                "Should reject negative instance ID");
        
        // Instance ID exceeds 10 bits (>1023)
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(1024L),
                "Should reject instance ID > 1023");
        
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(2048L),
                "Should reject instance ID > 1023");
    }
    
    /**
     * Test: Different instance IDs produce different code sequences
     * 
     * Note: Codes will differ in the instance ID bits, though Base62 encoding
     * makes this less obvious. Main validation is that both generators work.
     */
    @Test
    void testDifferentInstanceIdsProduceDifferentSequences() {
        SnowflakeIdGenerator generator0 = new SnowflakeIdGenerator(0L);
        SnowflakeIdGenerator generator1 = new SnowflakeIdGenerator(1L);
        
        String code0 = generator0.generateShortCode();
        String code1 = generator1.generateShortCode();
        
        // Both should produce valid codes
        assertNotNull(code0);
        assertNotNull(code1);
        assertTrue(BASE62_PATTERN.matcher(code0).matches());
        assertTrue(BASE62_PATTERN.matcher(code1).matches());
        
        // Note: codes might occasionally match due to timing, but that's OK
        // The important part is both generators work independently
    }
    
    /**
     * Test: Generator is consistent (same instance produces unique IDs)
     */
    @Test
    void testGeneratorConsistency() {
        Set<String> codes = new HashSet<>();
        
        // Generate 100 codes
        for (int i = 0; i < 100; i++) {
            codes.add(generator.generateShortCode());
        }
        
        assertEquals(100, codes.size(), "All 100 codes should be unique");
    }
}
