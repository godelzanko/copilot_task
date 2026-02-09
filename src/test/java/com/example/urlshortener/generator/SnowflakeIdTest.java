package com.example.urlshortener.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Unit tests for SnowflakeId class.
 * Tests bit layout, ID generation, component extraction, and validation.
 */
class SnowflakeIdTest {

    @Test
    @DisplayName("Constants should be properly defined according to spec")
    void testConstants() {
        // AC #1: Bit allocation (13 bits for sequence per Story 2.2)
        assertEquals(41, SnowflakeId.TIMESTAMP_BITS);
        assertEquals(10, SnowflakeId.INSTANCE_ID_BITS);
        assertEquals(13, SnowflakeId.SEQUENCE_BITS);  // 13 bits per Story 2.2
        
        // Bit shifts adjusted for 13-bit sequence
        assertEquals(23, SnowflakeId.TIMESTAMP_SHIFT);  // 13 + 10 = 23
        assertEquals(13, SnowflakeId.INSTANCE_ID_SHIFT); // 13 for sequence bits
        
        // Verify max values
        assertEquals((1L << 41) - 1, SnowflakeId.MAX_TIMESTAMP);
        assertEquals((1L << 10) - 1, SnowflakeId.MAX_INSTANCE_ID);
        assertEquals((1L << 13) - 1, SnowflakeId.MAX_SEQUENCE);  // 8191 per Story 2.2
    }

    @Test
    @DisplayName("EPOCH should be 2024-01-01T00:00:00Z in milliseconds")
    void testEpochConstant() {
        // AC #2: Custom Epoch
        long expectedEpoch = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        assertEquals(expectedEpoch, SnowflakeId.EPOCH);
    }

    @Test
    @DisplayName("generateId should create correct 64-bit ID from components")
    void testGenerateIdWithKnownValues() {
        // Bit shifting operations with 13-bit sequence
        long timestamp = 1000L;
        long instanceId = 5L;
        long sequence = 100L;
        
        long expectedId = (timestamp << 23) | (instanceId << 13) | sequence;
        long actualId = SnowflakeId.generateId(timestamp, instanceId, sequence);
        
        assertEquals(expectedId, actualId);
    }

    @Test
    @DisplayName("generateId should handle zero values")
    void testGenerateIdWithZeros() {
        long id = SnowflakeId.generateId(0L, 0L, 0L);
        assertEquals(0L, id);
    }

    @Test
    @DisplayName("generateId should handle maximum valid values")
    void testGenerateIdWithMaxValues() {
        // AC #5: Max value validation (13 bits = 8191 per Story 2.2)
        long maxTimestamp = (1L << 41) - 1;
        long maxInstanceId = (1L << 10) - 1; // 1023
        long maxSequence = (1L << 13) - 1;   // 8191 per Story 2.2
        
        long id = SnowflakeId.generateId(maxTimestamp, maxInstanceId, maxSequence);
        // ID can be negative when bit 63 is set (timestamp uses bits 23-63)
        assertTrue(id != 0, "ID should not be zero");
        
        // Verify extracted components match
        assertEquals(maxTimestamp, SnowflakeId.extractTimestamp(id));
        assertEquals(maxInstanceId, SnowflakeId.extractInstanceId(id));
        assertEquals(maxSequence, SnowflakeId.extractSequence(id));
    }

    @Test
    @DisplayName("extractTimestamp should return correct 41-bit timestamp")
    void testExtractTimestamp() {
        // AC #4: Component extraction
        long timestamp = 123456789L;
        long instanceId = 42L;
        long sequence = 500L;
        
        long id = SnowflakeId.generateId(timestamp, instanceId, sequence);
        assertEquals(timestamp, SnowflakeId.extractTimestamp(id));
    }

    @Test
    @DisplayName("extractInstanceId should return correct 10-bit instance ID")
    void testExtractInstanceId() {
        // AC #4: Component extraction
        long timestamp = 999999L;
        long instanceId = 1023L; // max instance ID
        long sequence = 8191L;    // max sequence (13 bits per Story 2.2)
        
        long id = SnowflakeId.generateId(timestamp, instanceId, sequence);
        assertEquals(instanceId, SnowflakeId.extractInstanceId(id));
    }

    @Test
    @DisplayName("extractSequence should return correct 13-bit sequence")
    void testExtractSequence() {
        // AC #4: Component extraction (13 bits per Story 2.2)
        long timestamp = 555555L;
        long instanceId = 100L;
        long sequence = 8191L; // max sequence (13 bits per Story 2.2)
        
        long id = SnowflakeId.generateId(timestamp, instanceId, sequence);
        assertEquals(sequence, SnowflakeId.extractSequence(id));
    }

    @Test
    @DisplayName("Round-trip: generated ID should extract to same components")
    void testRoundTrip() {
        // AC #4: Test multiple round trips (13-bit sequence = 8191 max per Story 2.2)
        long[][] testCases = {
            {1L, 0L, 0L},
            {100L, 5L, 10L},
            {999999L, 512L, 8191L},  // Max 13-bit sequence per Story 2.2
            {(1L << 41) - 1, (1L << 10) - 1, (1L << 13) - 1}  // All max values
        };
        
        for (long[] testCase : testCases) {
            long timestamp = testCase[0];
            long instanceId = testCase[1];
            long sequence = testCase[2];
            
            long id = SnowflakeId.generateId(timestamp, instanceId, sequence);
            
            assertEquals(timestamp, SnowflakeId.extractTimestamp(id), 
                "Timestamp mismatch for case: " + timestamp);
            assertEquals(instanceId, SnowflakeId.extractInstanceId(id), 
                "InstanceId mismatch for case: " + instanceId);
            assertEquals(sequence, SnowflakeId.extractSequence(id), 
                "Sequence mismatch for case: " + sequence);
        }
    }

    @Test
    @DisplayName("generateId should throw exception when timestamp exceeds 41 bits")
    void testTimestampOverflow() {
        // AC #5: Validation
        long invalidTimestamp = (1L << 41); // One bit too many
        long validInstanceId = 0L;
        long validSequence = 0L;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(invalidTimestamp, validInstanceId, validSequence)
        );
        assertTrue(exception.getMessage().contains("Timestamp"));
    }

    @Test
    @DisplayName("generateId should throw exception when instanceId exceeds 10 bits")
    void testInstanceIdOverflow() {
        // AC #5: Validation (0-1023 range)
        long validTimestamp = 1000L;
        long invalidInstanceId = 1024L; // Max is 1023
        long validSequence = 0L;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(validTimestamp, invalidInstanceId, validSequence)
        );
        assertTrue(exception.getMessage().contains("Instance ID"));
    }

    @Test
    @DisplayName("generateId should throw exception when sequence exceeds 13 bits")
    void testSequenceOverflow() {
        // AC #5: Validation (0-8191 range for 13-bit sequence per Story 2.2)
        long validTimestamp = 1000L;
        long validInstanceId = 0L;
        long invalidSequence = 8192L; // Max is 8191 for 13-bit sequence
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(validTimestamp, validInstanceId, invalidSequence)
        );
        assertTrue(exception.getMessage().contains("Sequence"));
    }

    @Test
    @DisplayName("generateId should throw exception for negative timestamp")
    void testNegativeTimestamp() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(-1L, 0L, 0L)
        );
        assertTrue(exception.getMessage().contains("Timestamp"));
    }

    @Test
    @DisplayName("generateId should throw exception for negative instanceId")
    void testNegativeInstanceId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(1000L, -1L, 0L)
        );
        assertTrue(exception.getMessage().contains("Instance ID"));
    }

    @Test
    @DisplayName("generateId should throw exception for negative sequence")
    void testNegativeSequence() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SnowflakeId.generateId(1000L, 0L, -1L)
        );
        assertTrue(exception.getMessage().contains("Sequence"));
    }

    @Test
    @DisplayName("Epoch calculation should support 69 years from 2024")
    void testEpochLifetime() {
        // AC #2: Supports 69 years from epoch
        long maxTimestamp = (1L << 41) - 1;
        long millisecondsIn69Years = 69L * 365 * 24 * 60 * 60 * 1000;
        
        // Max timestamp should be approximately 69 years in milliseconds
        assertTrue(maxTimestamp >= millisecondsIn69Years,
            "Max timestamp should support at least 69 years");
    }

    @Test
    @DisplayName("ID generation should use correct bit positions")
    void testBitPositions() {
        // Verify bit positions for 13-bit sequence
        // timestamp at bit 23-63, instanceId at bit 13-22, sequence at bit 0-12
        long timestamp = 1L;
        long instanceId = 1L;
        long sequence = 1L;
        
        long id = SnowflakeId.generateId(timestamp, instanceId, sequence);
        
        // Check individual bits are in correct positions
        // Timestamp should be at position 23
        assertTrue((id & (1L << 23)) != 0, "Timestamp bit not at position 23");
        // InstanceId should be at position 13
        assertTrue((id & (1L << 13)) != 0, "InstanceId bit not at position 13");
        // Sequence should be at position 0
        assertTrue((id & 1L) != 0, "Sequence bit not at position 0");
    }
}
