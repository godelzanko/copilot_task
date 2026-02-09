package com.example.urlshortener.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency and thread-safety tests for SnowflakeIdGenerator.
 * 
 * Tests coverage:
 * - Multi-threaded concurrent access
 * - Thread coordination and synchronization
 * - Uniqueness under high concurrency
 * - No race conditions or duplicates
 */
class SnowflakeIdGeneratorConcurrencyTest {
    
    private SnowflakeIdGenerator generator;
    
    /** Pattern to validate Base62 characters */
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    
    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator();
    }
    
    /**
     * AC #2 Test: 100 threads calling generateShortCode() concurrently
     * 
     * This is the primary concurrency test validating:
     * - Thread-safe synchronized methods
     * - No duplicate IDs under concurrent load
     * - Correct sequence counter management
     */
    @Test
    void test100ThreadsCallingGenerateShortCodeConcurrently() 
            throws InterruptedException, ExecutionException {
        
        int threadCount = 100;
        int codesPerThread = 100;
        
        // Thread coordination
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Thread-safe collection for all generated codes
        Set<String> allCodes = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> futures = new ArrayList<>();
        
        // Submit tasks for all threads
        for (int i = 0; i < threadCount; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready (synchronized start)
                    startLatch.await();
                    
                    // Generate codes
                    Set<String> threadLocalCodes = new HashSet<>();
                    for (int j = 0; j < codesPerThread; j++) {
                        String code = generator.generateShortCode();
                        threadLocalCodes.add(code);
                        allCodes.add(code);
                    }
                    
                    // Verify thread-local uniqueness
                    if (threadLocalCodes.size() != codesPerThread) {
                        throw new AssertionError(
                            "Thread generated duplicates within its own batch");
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete (with timeout)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, 
                "All threads should complete within 30 seconds");
        
        // Check for exceptions in any thread
        for (Future<?> future : futures) {
            assertDoesNotThrow(() -> future.get(),
                    "No thread should throw exceptions during code generation");
        }
        
        executor.shutdown();
        
        // Assert no duplicates across all threads
        int expectedTotal = threadCount * codesPerThread;
        assertEquals(expectedTotal, allCodes.size(),
                String.format("Expected %d unique codes from %d threads × %d codes/thread, " +
                        "but got %d unique codes (duplicates detected!)",
                        expectedTotal, threadCount, codesPerThread, allCodes.size()));
    }
    
    /**
     * AC #2 Test: Validate all codes are valid Base62 strings
     */
    @Test
    void testAllCodesAreValidBase62Strings() throws InterruptedException {
        int threadCount = 50;
        int codesPerThread = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        Set<String> allCodes = Collections.synchronizedSet(new HashSet<>());
        List<String> invalidCodes = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < codesPerThread; j++) {
                        String code = generator.generateShortCode();
                        allCodes.add(code);
                        
                        // Validate immediately
                        if (!BASE62_PATTERN.matcher(code).matches()) {
                            invalidCodes.add(code);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(invalidCodes.isEmpty(),
                "All codes should be valid Base62. Invalid codes: " + invalidCodes);
        assertEquals(threadCount * codesPerThread, allCodes.size(),
                "All codes should be unique");
    }
    
    /**
     * Test: High-load stress test (1000 codes from 10 threads)
     */
    @Test
    void testHighLoadStressTest() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int codesPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        
        Set<String> allCodes = Collections.synchronizedSet(new HashSet<>());
        List<Future<Integer>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Future<Integer> future = executor.submit(() -> {
                startLatch.await();
                
                int count = 0;
                for (int j = 0; j < codesPerThread; j++) {
                    String code = generator.generateShortCode();
                    allCodes.add(code);
                    count++;
                }
                return count;
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        
        // Verify all threads completed successfully
        int totalGenerated = 0;
        for (Future<Integer> future : futures) {
            totalGenerated += future.get();
        }
        
        executor.shutdown();
        
        assertEquals(threadCount * codesPerThread, totalGenerated,
                "All threads should generate expected number of codes");
        assertEquals(threadCount * codesPerThread, allCodes.size(),
                "No duplicates should exist under high load");
    }
    
    /**
     * Test: Burst generation (many codes in quick succession)
     */
    @Test
    void testBurstGeneration() throws InterruptedException {
        int burstSize = 10000;
        Set<String> codes = Collections.synchronizedSet(new HashSet<>());
        
        // Single thread generating many codes rapidly
        for (int i = 0; i < burstSize; i++) {
            codes.add(generator.generateShortCode());
        }
        
        assertEquals(burstSize, codes.size(),
                "Burst generation should produce all unique codes");
    }
    
    /**
     * Test: Multiple generator instances don't interfere with each other
     */
    @Test
    void testMultipleGeneratorInstancesIndependence() 
            throws InterruptedException, ExecutionException {
        
        SnowflakeIdGenerator gen1 = new SnowflakeIdGenerator(0L);
        SnowflakeIdGenerator gen2 = new SnowflakeIdGenerator(1L);
        
        int threadCount = 20;
        int codesPerThread = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        
        Set<String> allCodes = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> futures = new ArrayList<>();
        
        // Half threads use gen1, half use gen2
        for (int i = 0; i < threadCount; i++) {
            final SnowflakeIdGenerator gen = (i % 2 == 0) ? gen1 : gen2;
            
            Future<?> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < codesPerThread; j++) {
                        allCodes.add(gen.generateShortCode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        
        for (Future<?> future : futures) {
            future.get();
        }
        
        executor.shutdown();
        
        // All codes should be unique across both generators
        // (different instance IDs ensure uniqueness)
        assertEquals(threadCount * codesPerThread, allCodes.size(),
                "Different instance IDs should produce unique codes");
    }
    
    /**
     * Test: Thread safety under exception conditions
     * 
     * This test verifies that thread safety is maintained even when
     * checking edge cases.
     */
    @Test
    void testThreadSafetyUnderNormalOperation() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        Set<String> codes = Collections.synchronizedSet(new HashSet<>());
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Each thread generates 100 codes
                    for (int j = 0; j < 100; j++) {
                        codes.add(generator.generateShortCode());
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(threadCount * 100, codes.size(),
                "Thread safety should be maintained");
    }
}
