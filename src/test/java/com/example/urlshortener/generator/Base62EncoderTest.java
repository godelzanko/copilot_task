package com.example.urlshortener.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Base62Encoder.
 * 
 * Tests cover:
 * - Encoding edge cases (0, small values, large values)
 * - Decoding edge cases
 * - Round-trip correctness
 * - Invalid input handling
 * - Performance characteristics
 */
@DisplayName("Base62Encoder Tests")
class Base62EncoderTest {
    
    private Base62Encoder encoder;
    
    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }
    
    @Nested
    @DisplayName("Encoding Tests")
    class EncodingTests {
        
        @Test
        @DisplayName("encode(0) should return '0'")
        void testEncodeZero() {
            assertEquals("0", encoder.encode(0));
        }
        
        @Test
        @DisplayName("encode(61) should return 'Z'")
        void testEncode61() {
            assertEquals("Z", encoder.encode(61));
        }
        
        @Test
        @DisplayName("encode(62) should return '10'")
        void testEncode62() {
            assertEquals("10", encoder.encode(62));
        }
        
        @ParameterizedTest(name = "encode({0}) should return ''{1}''")
        @CsvSource({
            "0, 0",
            "1, 1",
            "9, 9",
            "10, a",
            "35, z",
            "36, A",
            "61, Z",
            "62, 10",
            "123, 1Z",
            "3844, 100",
            "123456, w7e",
            "123456789, 8m0Kx",
            "9223372036854775807, aZl8N0y58M7"  // Long.MAX_VALUE
        })
        @DisplayName("encode() should correctly encode known values")
        void testEncodeKnownValues(long id, String expected) {
            assertEquals(expected, encoder.encode(id));
        }
        
        @Test
        @DisplayName("encode(Long.MAX_VALUE) should not throw exception")
        void testEncodeLargeValue() {
            assertDoesNotThrow(() -> encoder.encode(Long.MAX_VALUE));
        }
        
        @Test
        @DisplayName("encode(negative) should throw IllegalArgumentException")
        void testEncodeNegative() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encoder.encode(-1)
            );
            assertTrue(exception.getMessage().contains("non-negative"));
        }
        
        @Test
        @DisplayName("encoded strings should be compact (~7 chars for typical Snowflake IDs)")
        void testEncodedLength() {
            // Typical Snowflake ID: 41-bit timestamp + 10-bit instance + 13-bit sequence
            // Use a smaller timestamp to avoid overflow
            // Real Snowflake timestamp would be milliseconds since custom epoch (2024-01-01)
            long timestamp = 1_000_000_000L; // ~11 days of milliseconds
            long instanceId = 0; // 10 bits
            long sequence = 8191; // 13 bits max
            
            // Build a typical Snowflake ID
            long typicalSnowflakeId = (timestamp << 23) | (instanceId << 13) | sequence;
            
            String encoded = encoder.encode(typicalSnowflakeId);
            assertTrue(encoded.length() >= 7 && encoded.length() <= 11, 
                "Encoded length should be 7-11 chars for typical Snowflake ID, got: " + encoded.length() + " for ID: " + typicalSnowflakeId);
        }
        
        @Test
        @DisplayName("encoded strings should be URL-safe (no special characters)")
        void testEncodedIsUrlSafe() {
            long[] testValues = {0, 62, 123456, Long.MAX_VALUE / 2, Long.MAX_VALUE};
            
            for (long id : testValues) {
                String encoded = encoder.encode(id);
                // URL-safe means only alphanumeric characters
                assertTrue(encoded.matches("[0-9a-zA-Z]+"), 
                    "Encoded value should only contain alphanumeric characters: " + encoded);
            }
        }
        
        @Test
        @DisplayName("different IDs should produce different encodings")
        void testUniqueness() {
            Set<String> encodings = new HashSet<>();
            for (long i = 0; i < 10000; i++) {
                String encoded = encoder.encode(i);
                assertTrue(encodings.add(encoded), 
                    "Encoding collision detected for ID " + i + ": " + encoded);
            }
        }
    }
    
    @Nested
    @DisplayName("Decoding Tests")
    class DecodingTests {
        
        @Test
        @DisplayName("decode('0') should return 0")
        void testDecodeZero() {
            assertEquals(0, encoder.decode("0"));
        }
        
        @Test
        @DisplayName("decode('Z') should return 61")
        void testDecode61() {
            assertEquals(61, encoder.decode("Z"));
        }
        
        @Test
        @DisplayName("decode('10') should return 62")
        void testDecode62() {
            assertEquals(62, encoder.decode("10"));
        }
        
        @ParameterizedTest(name = "decode(''{1}'') should return {0}")
        @CsvSource({
            "0, 0",
            "1, 1",
            "9, 9",
            "10, a",
            "35, z",
            "36, A",
            "61, Z",
            "62, 10",
            "123, 1Z",
            "3844, 100",
            "123456, w7e",
            "123456789, 8m0Kx"
        })
        @DisplayName("decode() should correctly decode known values")
        void testDecodeKnownValues(long expected, String encoded) {
            assertEquals(expected, encoder.decode(encoded));
        }
        
        @Test
        @DisplayName("decode(null) should throw IllegalArgumentException")
        void testDecodeNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encoder.decode(null)
            );
            assertTrue(exception.getMessage().contains("null"));
        }
        
        @Test
        @DisplayName("decode('') should throw IllegalArgumentException")
        void testDecodeEmpty() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encoder.decode("")
            );
            assertTrue(exception.getMessage().contains("empty"));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+", "=", " ", "-", "_"})
        @DisplayName("decode(invalid character) should throw IllegalArgumentException")
        void testDecodeInvalidCharacter(String invalidChar) {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encoder.decode("abc" + invalidChar + "xyz")
            );
            assertTrue(exception.getMessage().contains("Invalid character"));
        }
    }
    
    @Nested
    @DisplayName("Round-Trip Tests")
    class RoundTripTests {
        
        @Test
        @DisplayName("decode(encode(x)) should equal x for all test values")
        void testEncodeDecodeRoundTrip() {
            long[] testValues = {
                0, 1, 61, 62, 63, 100, 1000, 10000, 
                123456789L, 
                Long.MAX_VALUE / 2,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE
            };
            
            for (long original : testValues) {
                String encoded = encoder.encode(original);
                long decoded = encoder.decode(encoded);
                assertEquals(original, decoded, 
                    String.format("Round-trip failed for %d: encode→'%s'→decode→%d", 
                        original, encoded, decoded));
            }
        }
        
        @Test
        @DisplayName("encode(decode(y)) should equal y for all test values")
        void testDecodeEncodeRoundTrip() {
            String[] testStrings = {
                "0", "1", "z", "Z", "10", "100", "abc", "XYZ", 
                "8m0Kx", "aZl8N0y58M7"
            };
            
            for (String original : testStrings) {
                long decoded = encoder.decode(original);
                String encoded = encoder.encode(decoded);
                assertEquals(original, encoded,
                    String.format("Round-trip failed for '%s': decode→%d→encode→'%s'",
                        original, decoded, encoded));
            }
        }
        
        @Test
        @DisplayName("property-based: encode/decode round-trip for random values")
        void testRandomRoundTrip() {
            Random random = new Random(42); // Fixed seed for reproducibility
            
            for (int i = 0; i < 1000; i++) {
                // Generate random positive long (excluding negatives)
                long original = Math.abs(random.nextLong());
                
                String encoded = encoder.encode(original);
                long decoded = encoder.decode(encoded);
                
                assertEquals(original, decoded,
                    String.format("Random round-trip failed for %d", original));
            }
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("encoding 10,000 IDs should complete quickly")
        void testEncodingPerformance() {
            long startTime = System.nanoTime();
            
            for (long i = 0; i < 10000; i++) {
                encoder.encode(i);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            // Should complete in well under 1 second (typically < 100ms)
            assertTrue(durationMs < 1000, 
                "Encoding 10,000 IDs took " + durationMs + "ms (expected < 1000ms)");
        }
        
        @Test
        @DisplayName("decoding 10,000 strings should complete quickly")
        void testDecodingPerformance() {
            // Pre-generate encoded strings
            String[] encodedValues = new String[10000];
            for (int i = 0; i < 10000; i++) {
                encodedValues[i] = encoder.encode(i);
            }
            
            long startTime = System.nanoTime();
            
            for (String encoded : encodedValues) {
                encoder.decode(encoded);
            }
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            // Should complete in well under 1 second
            assertTrue(durationMs < 1000,
                "Decoding 10,000 strings took " + durationMs + "ms (expected < 1000ms)");
        }
        
        @Test
        @DisplayName("typical Snowflake IDs should encode to ~7-11 characters")
        void testTypicalEncodedLength() {
            // Generate typical Snowflake IDs using reasonable timestamp values
            long baseTimestamp = 1_000_000_000L; // Start from a billion milliseconds
            int samplesWithin7to11Chars = 0;
            int totalSamples = 1000;
            
            for (int i = 0; i < totalSamples; i++) {
                // Simulate Snowflake ID: timestamp (41 bits) + instance (10 bits) + sequence (13 bits)
                long timestamp = baseTimestamp + (i * 1000); // Increment by 1 second
                long instanceId = 0; // 10 bits
                long sequence = i % 8192; // 13 bits
                
                // Construct Snowflake ID
                long snowflakeId = (timestamp << 23) | (instanceId << 13) | sequence;
                
                String encoded = encoder.encode(snowflakeId);
                
                if (encoded.length() >= 7 && encoded.length() <= 11) {
                    samplesWithin7to11Chars++;
                }
            }
            
            // At least 95% of typical Snowflake IDs should be 7-11 characters
            double percentage = (samplesWithin7to11Chars * 100.0) / totalSamples;
            assertTrue(percentage >= 95.0,
                String.format("Only %.1f%% of IDs were 7-11 chars (expected >= 95%%)", percentage));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("alphabet boundaries: first char (0)")
        void testFirstChar() {
            assertEquals("0", encoder.encode(0));
        }
        
        @Test
        @DisplayName("alphabet boundaries: last lowercase (z)")
        void testLastLowercase() {
            assertEquals("z", encoder.encode(35));
        }
        
        @Test
        @DisplayName("alphabet boundaries: first uppercase (A)")
        void testFirstUppercase() {
            assertEquals("A", encoder.encode(36));
        }
        
        @Test
        @DisplayName("alphabet boundaries: last char (Z)")
        void testLastChar() {
            assertEquals("Z", encoder.encode(61));
        }
        
        @Test
        @DisplayName("powers of 62 should encode predictably")
        void testPowersOf62() {
            assertEquals("10", encoder.encode(62));      // 62^1
            assertEquals("100", encoder.encode(3844));   // 62^2
            assertEquals("1000", encoder.encode(238328)); // 62^3
        }
        
        @Test
        @DisplayName("case sensitivity test")
        void testCaseSensitivity() {
            // 'a' (index 10) and 'A' (index 36) should produce different results
            assertNotEquals(encoder.encode(10), encoder.encode(36));
            assertEquals("a", encoder.encode(10));
            assertEquals("A", encoder.encode(36));
        }
    }
}
