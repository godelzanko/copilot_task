package com.example.urlshortener.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load tests for SnowflakeId generator.
 * Tests performance and correctness under high load.
 */
class SnowflakeIdGeneratorLoadTest {

    @Test
    @DisplayName("Should generate 10,000 unique IDs rapidly")
    void testGenerate10000UniqueIds() {
        SnowflakeId generator = new SnowflakeId(0);
        int count = 10000;
        
        Set<Long> ids = new HashSet<>();
        List<Long> idList = new ArrayList<>();
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < count; i++) {
            long id = generator.nextId();
            ids.add(id);
            idList.add(id);
        }
        
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double idsPerSecond = (count / durationMs) * 1000.0;
        
        // Verify no duplicates
        assertEquals(count, ids.size(), "All IDs should be unique");
        
        // Report performance
        System.out.printf("Generated %d IDs in %.2f ms (%.0f IDs/sec)%n", 
            count, durationMs, idsPerSecond);
        
        // Performance should be reasonable (at least 10,000 IDs/sec)
        assertTrue(idsPerSecond > 10000, 
            String.format("Performance too low: %.0f IDs/sec", idsPerSecond));
    }

    @Test
    @DisplayName("Generated IDs should be monotonically increasing")
    void testIdsAreMonotonicallyIncreasing() {
        SnowflakeId generator = new SnowflakeId(0);
        int count = 10000;
        
        long prevId = generator.nextId();
        
        for (int i = 1; i < count; i++) {
            long currentId = generator.nextId();
            
            // IDs should always increase (due to time or sequence increment)
            assertTrue(currentId > prevId, 
                String.format("ID not increasing at position %d: %d <= %d", 
                    i, currentId, prevId));
            
            prevId = currentId;
        }
    }

    @Test
    @DisplayName("IDs should be time-sorted within reasonable tolerance")
    void testIdsAreTimeSorted() {
        SnowflakeId generator = new SnowflakeId(0);
        int count = 1000;
        
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(generator.nextId());
        }
        
        // Extract timestamps and verify they're non-decreasing
        long prevTimestamp = SnowflakeId.extractTimestamp(ids.get(0));
        
        for (int i = 1; i < count; i++) {
            long currentTimestamp = SnowflakeId.extractTimestamp(ids.get(i));
            
            assertTrue(currentTimestamp >= prevTimestamp, 
                String.format("Timestamp decreased at position %d: %d < %d", 
                    i, currentTimestamp, prevTimestamp));
            
            prevTimestamp = currentTimestamp;
        }
    }

    @Test
    @DisplayName("All components should be within valid ranges under load")
    void testComponentsValidUnderLoad() {
        SnowflakeId generator = new SnowflakeId(42);
        int count = 10000;
        
        for (int i = 0; i < count; i++) {
            long id = generator.nextId();
            
            long timestamp = SnowflakeId.extractTimestamp(id);
            long instanceId = SnowflakeId.extractInstanceId(id);
            long sequence = SnowflakeId.extractSequence(id);
            
            // Verify all components are valid
            assertTrue(timestamp >= 0 && timestamp <= SnowflakeId.MAX_TIMESTAMP,
                "Timestamp out of range: " + timestamp);
            assertEquals(42, instanceId, "Instance ID should match");
            assertTrue(sequence >= 0 && sequence <= SnowflakeId.MAX_SEQUENCE,
                "Sequence out of range: " + sequence);
        }
    }

    @Test
    @DisplayName("Should handle rapid generation without errors")
    void testRapidGenerationNoErrors() {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Generate IDs as fast as possible
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50000; i++) {
                generator.nextId();
            }
        });
    }

    @Test
    @DisplayName("Sequence should reset correctly under load")
    void testSequenceResetsUnderLoad() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Generate many IDs
        List<Long> batch1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            batch1.add(generator.nextId());
        }
        
        // Wait for new millisecond
        Thread.sleep(2);
        
        // Generate more IDs
        List<Long> batch2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            batch2.add(generator.nextId());
        }
        
        // Check that sequences in batch2 started from 0 or low value
        long firstSeqBatch2 = SnowflakeId.extractSequence(batch2.get(0));
        assertTrue(firstSeqBatch2 < 50, 
            "Sequence should reset on new millisecond, got: " + firstSeqBatch2);
    }

    @Test
    @DisplayName("Measure peak throughput")
    void testPeakThroughput() {
        SnowflakeId generator = new SnowflakeId(0);
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            generator.nextId();
        }
        
        // Measure
        int count = 100000;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        double idsPerSecond = (count / durationMs) * 1000.0;
        
        System.out.printf("Peak throughput: %.0f IDs/sec (%.3f ms for %d IDs)%n",
            idsPerSecond, durationMs, count);
        
        // Should handle at least 50,000 IDs per second
        assertTrue(idsPerSecond > 50000,
            String.format("Throughput too low: %.0f IDs/sec", idsPerSecond));
    }
}
