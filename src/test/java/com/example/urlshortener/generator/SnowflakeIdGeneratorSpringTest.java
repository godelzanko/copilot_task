package com.example.urlshortener.generator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring integration tests for SnowflakeIdGenerator.
 * 
 * Tests coverage:
 * - Spring bean lifecycle and dependency injection
 * - Singleton scope validation
 * - Component functionality in Spring context
 * - Concurrent access from multiple threads
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
class SnowflakeIdGeneratorSpringTest {
    
    @Autowired
    private SnowflakeIdGenerator generator;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /** Pattern to validate Base62 characters */
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    
    /**
     * AC #1 Test: Bean is auto-wired successfully
     */
    @Test
    void testBeanIsAutoWired() {
        assertNotNull(generator, "SnowflakeIdGenerator bean should be auto-wired");
    }
    
    /**
     * AC #4 Test: Bean is singleton (same instance on multiple injections)
     */
    @Test
    void testBeanIsSingleton() {
        SnowflakeIdGenerator bean1 = applicationContext.getBean(SnowflakeIdGenerator.class);
        SnowflakeIdGenerator bean2 = applicationContext.getBean(SnowflakeIdGenerator.class);
        
        assertSame(bean1, bean2, 
                "Spring should return the same singleton instance");
        assertSame(generator, bean1,
                "Auto-wired instance should be the same as context.getBean()");
    }
    
    /**
     * AC #2, #4 Test: generateShortCode() works in Spring context
     */
    @Test
    void testGenerateShortCodeWorksInSpringContext() {
        String shortCode = generator.generateShortCode();
        
        assertNotNull(shortCode, "Short code should not be null");
        assertFalse(shortCode.isEmpty(), "Short code should not be empty");
        assertTrue(BASE62_PATTERN.matcher(shortCode).matches(),
                "Short code should contain only Base62 characters");
    }
    
    /**
     * AC #2 Test: Multiple calls produce unique codes in Spring context
     */
    @Test
    void testMultipleCallsProduceUniqueCodesInSpringContext() {
        Set<String> codes = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            codes.add(generator.generateShortCode());
        }
        
        assertEquals(100, codes.size(), 
                "All 100 codes should be unique in Spring context");
    }
    
    /**
     * AC #2 Test: Concurrent access from multiple test threads
     * 
     * This test validates:
     * - Thread safety of the singleton bean
     * - Correct synchronization in Spring context
     * - No duplicates under concurrent load
     */
    @Test
    void testConcurrentAccessFromMultipleThreads() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int codesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Thread-safe collection for results
        Set<String> allCodes = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> futures = new ArrayList<>();
        
        // Submit tasks
        for (int i = 0; i < threadCount; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Generate codes
                    for (int j = 0; j < codesPerThread; j++) {
                        String code = generator.generateShortCode();
                        allCodes.add(code);
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
        
        // Wait for completion
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");
        
        // Check for exceptions
        for (Future<?> future : futures) {
            assertDoesNotThrow(() -> future.get(),
                    "No thread should throw exceptions");
        }
        
        executor.shutdown();
        
        // Validate results
        int expectedTotal = threadCount * codesPerThread;
        assertEquals(expectedTotal, allCodes.size(),
                "All " + expectedTotal + " codes should be unique (no duplicates in concurrent access)");
        
        // Validate all codes are valid Base62
        for (String code : allCodes) {
            assertTrue(BASE62_PATTERN.matcher(code).matches(),
                    "All codes should be valid Base62: " + code);
        }
    }
    
    /**
     * Test: Singleton bean maintains state across calls
     */
    @Test
    void testSingletonBeanMaintainsState() {
        // Generate some codes to advance the sequence
        for (int i = 0; i < 10; i++) {
            generator.generateShortCode();
        }
        
        // Get bean again from context - should be same instance with same state
        SnowflakeIdGenerator sameBean = applicationContext.getBean(SnowflakeIdGenerator.class);
        
        // Generate more codes - sequence should continue from previous state
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            codes.add(sameBean.generateShortCode());
        }
        
        assertEquals(10, codes.size(),
                "Bean should maintain state and continue generating unique codes");
    }
    
    /**
     * Test: Component annotation enables auto-discovery
     */
    @Test
    void testComponentAnnotationEnablesAutoDiscovery() {
        // Verify the bean was discovered via component scanning
        Map<String, SnowflakeIdGenerator> beans = 
                applicationContext.getBeansOfType(SnowflakeIdGenerator.class);
        
        assertEquals(1, beans.size(), 
                "Exactly one SnowflakeIdGenerator bean should exist");
        
        String beanName = beans.keySet().iterator().next();
        assertTrue(beanName.contains("snowflakeIdGenerator") || 
                   beanName.contains("SnowflakeIdGenerator"),
                "Bean name should reflect the class name");
    }
}
