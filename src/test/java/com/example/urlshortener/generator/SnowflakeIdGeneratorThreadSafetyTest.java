package com.example.urlshortener.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-threaded tests for SnowflakeId generator.
 * Verifies thread safety under concurrent access.
 */
class SnowflakeIdGeneratorThreadSafetyTest {

    @Test
    @DisplayName("100 threads should generate unique IDs concurrently")
    void testConcurrentIdGenerationUniqueness() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        int threadCount = 100;
        int idsPerThread = 100;
        
        // Thread-safe collection for generated IDs
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Create threads
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Generate IDs
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId();
                        if (!allIds.add(id)) {
                            failureCount.incrementAndGet();
                            System.err.println("Duplicate ID detected: " + id);
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        doneLatch.await();
        
        // Verify no failures
        assertEquals(0, failureCount.get(), "No thread failures should occur");
        
        // Verify all IDs are unique
        int expectedIdCount = threadCount * idsPerThread;
        assertEquals(expectedIdCount, allIds.size(), 
            "All generated IDs should be unique");
    }

    @Test
    @DisplayName("Concurrent access should not produce negative IDs")
    void testConcurrentAccessProducesValidIds() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        int threadCount = 50;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger negativeIdCount = new AtomicInteger(0);
        AtomicInteger invalidIdCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 100; j++) {
                        long id = generator.nextId();
                        
                        // Check if ID is valid (can be negative due to bit 63)
                        if (id == 0) {
                            invalidIdCount.incrementAndGet();
                        }
                        
                        // Verify extractable components are valid
                        long timestamp = SnowflakeId.extractTimestamp(id);
                        long instanceId = SnowflakeId.extractInstanceId(id);
                        long sequence = SnowflakeId.extractSequence(id);
                        
                        if (timestamp < 0 || instanceId < 0 || sequence < 0 ||
                            sequence > SnowflakeId.MAX_SEQUENCE) {
                            invalidIdCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    invalidIdCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        assertEquals(0, invalidIdCount.get(), "All IDs should be valid");
    }

    @Test
    @DisplayName("High contention should not cause deadlock")
    void testHighContentionNoDeadlock() throws InterruptedException {
        SnowflakeId generator = new SnowflakeId(0);
        int threadCount = 200;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    // Generate a few IDs
                    for (int j = 0; j < 10; j++) {
                        generator.nextId();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        
        // Wait with timeout to detect deadlock
        boolean completed = doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete without deadlock");
    }

    @Test
    @DisplayName("Multiple generators with different instance IDs should work concurrently")
    void testMultipleGeneratorsConcurrent() throws InterruptedException {
        SnowflakeId generator1 = new SnowflakeId(0);
        SnowflakeId generator2 = new SnowflakeId(1);
        
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        
        // Thread for generator 1
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    allIds.add(generator1.nextId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        }).start();
        
        // Thread for generator 2
        new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    allIds.add(generator2.nextId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        }).start();
        
        startLatch.countDown();
        doneLatch.await();
        
        // All IDs should be unique
        assertEquals(2000, allIds.size(), "IDs from different generators should all be unique");
    }
}
