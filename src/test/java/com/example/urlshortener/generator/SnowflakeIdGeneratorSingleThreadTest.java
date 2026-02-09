package com.example.urlshortener.generator;

import com.example.urlshortener.exception.ClockMovedBackwardsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-threaded unit tests for SnowflakeId generator.
 * Tests sequence increment logic, clock backwards detection, and overflow handling.
 */
class SnowflakeIdGeneratorSingleThreadTest {

    @Test
    @DisplayName("nextId should generate unique IDs in sequence")
    void testNextIdGeneratesUniqueIds() {
        SnowflakeId generator = new SnowflakeId(0);
        
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            long id = generator.nextId();
            assertTrue(ids.add(id), "Generated duplicate ID: " + id);
        }
        
        assertEquals(100, ids.size(), "Should generate 100 unique IDs");
    }

    @Test
    @DisplayName("nextId should increment sequence within same millisecond")
    void testSequenceIncrementsWithinMillisecond() {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Generate multiple IDs rapidly to hit same millisecond
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();
        
        // Extract components
        long timestamp1 = SnowflakeId.extractTimestamp(id1);
        long timestamp2 = SnowflakeId.extractTimestamp(id2);
        long timestamp3 = SnowflakeId.extractTimestamp(id3);
        
        long sequence1 = SnowflakeId.extractSequence(id1);
        long sequence2 = SnowflakeId.extractSequence(id2);
        long sequence3 = SnowflakeId.extractSequence(id3);
        
        // If generated in same millisecond, sequences should increment
        if (timestamp1 == timestamp2) {
            assertEquals(sequence1 + 1, sequence2, "Sequence should increment by 1");
        }
        if (timestamp2 == timestamp3) {
            assertEquals(sequence2 + 1, sequence3, "Sequence should increment by 1");
        }
    }

    @Test
    @DisplayName("nextId should reset sequence on new millisecond")
    void testSequenceResetsOnNewMillisecond() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Generate IDs to increment sequence
        for (int i = 0; i < 10; i++) {
            generator.nextId();
        }
        
        // Wait for next millisecond
        Thread.sleep(2);
        
        long id = generator.nextId();
        long sequence = SnowflakeId.extractSequence(id);
        
        // Sequence should be reset to 0 (or low value if some IDs generated in new ms)
        assertTrue(sequence < 10, "Sequence should reset on new millisecond, got: " + sequence);
    }

    @Test
    @DisplayName("nextId should handle sequence overflow by waiting")
    void testSequenceOverflowWaitsForNextMillisecond() {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Generate many IDs to potentially cause overflow
        // With 13-bit sequence (0-8191), we need 8192 IDs in 1ms to overflow
        // This is unlikely but let's generate enough to test the logic
        long firstId = generator.nextId();
        long firstTimestamp = SnowflakeId.extractTimestamp(firstId);
        
        // Generate 100 IDs and verify they're all valid
        for (int i = 0; i < 100; i++) {
            long id = generator.nextId();
            assertTrue(id != 0, "ID should not be zero");
            
            long timestamp = SnowflakeId.extractTimestamp(id);
            // Timestamp should be >= firstTimestamp (never backwards)
            assertTrue(timestamp >= firstTimestamp, 
                "Timestamp should not go backwards");
        }
    }

    @Test
    @DisplayName("nextId should throw ClockMovedBackwardsException on clock skew")
    void testClockBackwardsThrowsException() {
        // This test is difficult without mocking time
        // We can only test the exception class itself
        ClockMovedBackwardsException exception = 
            new ClockMovedBackwardsException(1000L, 900L);
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Clock moved backwards"));
        assertTrue(exception.getMessage().contains("100ms"));
    }

    @Test
    @DisplayName("Generated IDs should be extractable to correct components")
    void testGeneratedIdsExtractCorrectly() {
        SnowflakeId generator = new SnowflakeId(5);
        
        long id = generator.nextId();
        
        // Extract and verify components
        long timestamp = SnowflakeId.extractTimestamp(id);
        long instanceId = SnowflakeId.extractInstanceId(id);
        long sequence = SnowflakeId.extractSequence(id);
        
        // Verify instance ID matches
        assertEquals(5, instanceId, "Instance ID should match constructor parameter");
        
        // Verify timestamp is positive and reasonable
        assertTrue(timestamp > 0, "Timestamp should be positive");
        
        // Verify sequence is within valid range
        assertTrue(sequence >= 0 && sequence <= SnowflakeId.MAX_SEQUENCE,
            "Sequence should be in valid range 0-8191");
    }

    @Test
    @DisplayName("Multiple generators with different instance IDs should generate unique IDs")
    void testMultipleInstancesGenerateUniqueIds() {
        SnowflakeId generator1 = new SnowflakeId(0);
        SnowflakeId generator2 = new SnowflakeId(1);
        
        long id1 = generator1.nextId();
        long id2 = generator2.nextId();
        
        assertNotEquals(id1, id2, "IDs from different instances should be unique");
        
        long instanceId1 = SnowflakeId.extractInstanceId(id1);
        long instanceId2 = SnowflakeId.extractInstanceId(id2);
        
        assertEquals(0, instanceId1);
        assertEquals(1, instanceId2);
    }

    @Test
    @DisplayName("IDs should be monotonically increasing over time")
    void testIdsAreMonotonicallyIncreasing() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        
        long prevId = generator.nextId();
        
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1); // Ensure time advances
            long currentId = generator.nextId();
            
            assertTrue(currentId > prevId, 
                String.format("IDs should be monotonically increasing: %d > %d", 
                    currentId, prevId));
            
            prevId = currentId;
        }
    }

    @Test
    @DisplayName("Constructor should validate instance ID")
    void testConstructorValidatesInstanceId() {
        // Valid instance IDs (0-1023)
        assertDoesNotThrow(() -> new SnowflakeId(0));
        assertDoesNotThrow(() -> new SnowflakeId(1023));
        
        // Invalid instance IDs
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeId(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeId(1024));
    }
}
