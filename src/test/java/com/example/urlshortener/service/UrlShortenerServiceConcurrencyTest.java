package com.example.urlshortener.service;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency integration tests for UrlShortenerServiceImpl.
 * 
 * <p>These tests verify database-enforced idempotency under concurrent load:
 * <ul>
 *   <li>Multiple threads requesting same URL simultaneously</li>
 *   <li>All threads receive identical short code</li>
 *   <li>Only one database row created</li>
 *   <li>No race conditions or duplicate mappings</li>
 * </ul>
 * 
 * <p>Uses Testcontainers to run tests against a real PostgreSQL database.
 */
@SpringBootTest
class UrlShortenerServiceConcurrencyTest extends AbstractIntegrationTest {
    
    @Autowired
    private UrlShortenerService urlShortenerService;
    
    @Autowired
    private UrlRepository urlRepository;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        urlRepository.deleteAll();
    }
    
    /**
     * Tests that 10 concurrent threads requesting the same URL all receive the same short code.
     * 
     * <p>Verifies:
     * <ul>
     *   <li>All threads complete successfully</li>
     *   <li>All threads receive identical short code</li>
     *   <li>Only one database row created</li>
     * </ul>
     */
    @Test
    void concurrentRequestsForSameUrl_returnsSameShortCode() throws InterruptedException, ExecutionException {
        // Given
        String url = "https://example.com";
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ShortenResponse>> futures = new ArrayList<>();
        
        // When - submit 10 concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            Future<ShortenResponse> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Execute shorten URL
                    ShortenResponse response = urlShortenerService.shortenUrl(url);
                    
                    return response;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete (max 10 seconds)
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        
        // Then - collect results
        List<String> shortCodes = new ArrayList<>();
        for (Future<ShortenResponse> future : futures) {
            ShortenResponse response = future.get();
            shortCodes.add(response.shortCode());
        }
        
        // Assert all threads received same short code
        Set<String> uniqueShortCodes = shortCodes.stream().collect(Collectors.toSet());
        assertThat(uniqueShortCodes).hasSize(1);
        
        String shortCode = uniqueShortCodes.iterator().next();
        assertThat(shortCode).isNotNull().isNotEmpty();
        
        // Assert only one database row created
        List<UrlEntity> allEntities = urlRepository.findAll();
        assertThat(allEntities).hasSize(1);
        assertThat(allEntities.get(0).getShortCode()).isEqualTo(shortCode);
        assertThat(allEntities.get(0).getOriginalUrl()).isEqualTo(url);
    }
    
    /**
     * Tests that concurrent requests with different case variations return the same short code.
     * 
     * <p>Verifies normalization works correctly under concurrency:
     * <ul>
     *   <li>"HTTPS://EXAMPLE.COM" and "https://example.com" treated as same URL</li>
     *   <li>All variations receive identical short code</li>
     *   <li>Only one database row created</li>
     * </ul>
     */
    @Test
    void concurrentRequestsWithDifferentCase_returnsSameShortCode() throws InterruptedException, ExecutionException {
        // Given - mix of case variations
        String[] urlVariations = {
            "https://example.com",
            "HTTPS://EXAMPLE.COM",
            "Https://Example.Com",
            "https://EXAMPLE.com",
            "HTTPS://example.COM"
        };
        
        int threadCount = urlVariations.length;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ShortenResponse>> futures = new ArrayList<>();
        
        // When - submit concurrent tasks with different case variations
        for (String urlVariation : urlVariations) {
            Future<ShortenResponse> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    ShortenResponse response = urlShortenerService.shortenUrl(urlVariation);
                    return response;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then - collect and verify results
        List<String> shortCodes = new ArrayList<>();
        for (Future<ShortenResponse> future : futures) {
            shortCodes.add(future.get().shortCode());
        }
        
        // Assert all variations received same short code
        Set<String> uniqueShortCodes = shortCodes.stream().collect(Collectors.toSet());
        assertThat(uniqueShortCodes).hasSize(1);
        
        // Assert only one database row created with normalized URL
        List<UrlEntity> allEntities = urlRepository.findAll();
        assertThat(allEntities).hasSize(1);
        assertThat(allEntities.get(0).getOriginalUrl()).isEqualTo("https://example.com");
    }
    
    /**
     * Tests that concurrent requests with whitespace variations return the same short code.
     * 
     * <p>Verifies trim normalization works correctly under concurrency.
     */
    @Test
    void concurrentRequestsWithWhitespace_returnsSameShortCode() throws InterruptedException, ExecutionException {
        // Given - URLs with various whitespace
        String[] urlVariations = {
            "https://example.com",
            "  https://example.com",
            "https://example.com  ",
            "  https://example.com  ",
            "   https://example.com   "
        };
        
        int threadCount = urlVariations.length;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ShortenResponse>> futures = new ArrayList<>();
        
        // When
        for (String urlVariation : urlVariations) {
            Future<ShortenResponse> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    return urlShortenerService.shortenUrl(urlVariation);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then
        List<String> shortCodes = new ArrayList<>();
        for (Future<ShortenResponse> future : futures) {
            shortCodes.add(future.get().shortCode());
        }
        
        Set<String> uniqueShortCodes = shortCodes.stream().collect(Collectors.toSet());
        assertThat(uniqueShortCodes).hasSize(1);
        
        List<UrlEntity> allEntities = urlRepository.findAll();
        assertThat(allEntities).hasSize(1);
        assertThat(allEntities.get(0).getOriginalUrl()).isEqualTo("https://example.com");
    }
    
    /**
     * Tests that concurrent requests for different URLs generate different short codes.
     * 
     * <p>Verifies that idempotency doesn't incorrectly conflate different URLs.
     */
    @Test
    void concurrentRequestsForDifferentUrls_generateDifferentShortCodes() throws InterruptedException, ExecutionException {
        // Given - 5 different URLs
        String[] urls = {
            "https://example1.com",
            "https://example2.com",
            "https://example3.com",
            "https://example4.com",
            "https://example5.com"
        };
        
        int threadCount = urls.length;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ShortenResponse>> futures = new ArrayList<>();
        
        // When
        for (String url : urls) {
            Future<ShortenResponse> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    return urlShortenerService.shortenUrl(url);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then
        List<String> shortCodes = new ArrayList<>();
        for (Future<ShortenResponse> future : futures) {
            shortCodes.add(future.get().shortCode());
        }
        
        // Assert all URLs got different short codes
        Set<String> uniqueShortCodes = shortCodes.stream().collect(Collectors.toSet());
        assertThat(uniqueShortCodes).hasSize(urls.length);
        
        // Assert 5 database rows created
        List<UrlEntity> allEntities = urlRepository.findAll();
        assertThat(allEntities).hasSize(urls.length);
    }
    
    /**
     * Stress test: 50 threads requesting same URL.
     * 
     * <p>Verifies system handles higher concurrency levels correctly.
     */
    @Test
    void highConcurrency_50Threads_returnsSameShortCode() throws InterruptedException, ExecutionException {
        // Given
        String url = "https://stress-test.com";
        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ShortenResponse>> futures = new ArrayList<>();
        
        // When
        for (int i = 0; i < threadCount; i++) {
            Future<ShortenResponse> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    return urlShortenerService.shortenUrl(url);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then
        List<String> shortCodes = new ArrayList<>();
        for (Future<ShortenResponse> future : futures) {
            shortCodes.add(future.get().shortCode());
        }
        
        Set<String> uniqueShortCodes = shortCodes.stream().collect(Collectors.toSet());
        assertThat(uniqueShortCodes).hasSize(1);
        
        List<UrlEntity> allEntities = urlRepository.findAll();
        assertThat(allEntities).hasSize(1);
        assertThat(allEntities.get(0).getOriginalUrl()).isEqualTo(url);
    }
}
