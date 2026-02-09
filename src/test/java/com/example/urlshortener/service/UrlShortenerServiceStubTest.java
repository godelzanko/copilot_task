package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.generator.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UrlShortenerServiceStub with real SnowflakeIdGenerator.
 * 
 * Validates:
 * - Generator integration
 * - Unique short code generation
 * - URL mapping and retrieval
 * - Error handling
 */
class UrlShortenerServiceStubTest {
    
    private UrlShortenerServiceStub service;
    private SnowflakeIdGenerator generator;
    
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    
    @BeforeEach
    void setUp() {
        generator = new SnowflakeIdGenerator();
        service = new UrlShortenerServiceStub(generator);
    }
    
    /**
     * Test: Service uses real generator (no more hardcoded "STUB123")
     */
    @Test
    void testServiceUsesRealGenerator() {
        ShortenResponse response = service.shortenUrl("https://example.com");
        
        // Should NOT be the old hardcoded value
        assertNotEquals("STUB123", response.shortCode(),
                "Service should use real generator, not hardcoded 'STUB123'");
        
        // Should be valid Base62
        assertTrue(BASE62_PATTERN.matcher(response.shortCode()).matches(),
                "Short code should be valid Base62: " + response.shortCode());
    }
    
    /**
     * Test: Each URL gets a unique short code
     */
    @Test
    void testEachUrlGetsUniqueShortCode() {
        ShortenResponse response1 = service.shortenUrl("https://example.com");
        ShortenResponse response2 = service.shortenUrl("https://example.org");
        ShortenResponse response3 = service.shortenUrl("https://example.net");
        
        assertNotEquals(response1.shortCode(), response2.shortCode());
        assertNotEquals(response2.shortCode(), response3.shortCode());
        assertNotEquals(response1.shortCode(), response3.shortCode());
    }
    
    /**
     * Test: Same URL gets different short codes on multiple calls
     * (no deduplication in stub - each call generates new code)
     */
    @Test
    void testSameUrlGetsDifferentShortCodesOnMultipleCalls() {
        String url = "https://example.com";
        
        ShortenResponse response1 = service.shortenUrl(url);
        ShortenResponse response2 = service.shortenUrl(url);
        
        assertNotEquals(response1.shortCode(), response2.shortCode(),
                "Stub doesn't deduplicate - same URL should get different codes");
    }
    
    /**
     * Test: Short code can be used to retrieve original URL
     */
    @Test
    void testShortCodeRetrievesOriginalUrl() {
        String originalUrl = "https://example.com/path?query=value";
        
        ShortenResponse response = service.shortenUrl(originalUrl);
        String retrievedUrl = service.getOriginalUrl(response.shortCode());
        
        assertEquals(originalUrl, retrievedUrl,
                "Retrieved URL should match original");
    }
    
    /**
     * Test: Multiple URLs can be stored and retrieved correctly
     */
    @Test
    void testMultipleUrlsCanBeStoredAndRetrieved() {
        String url1 = "https://example.com/page1";
        String url2 = "https://example.org/page2";
        String url3 = "https://example.net/page3";
        
        ShortenResponse response1 = service.shortenUrl(url1);
        ShortenResponse response2 = service.shortenUrl(url2);
        ShortenResponse response3 = service.shortenUrl(url3);
        
        assertEquals(url1, service.getOriginalUrl(response1.shortCode()));
        assertEquals(url2, service.getOriginalUrl(response2.shortCode()));
        assertEquals(url3, service.getOriginalUrl(response3.shortCode()));
    }
    
    /**
     * Test: Unknown short code throws ShortCodeNotFoundException
     */
    @Test
    void testUnknownShortCodeThrowsException() {
        assertThrows(ShortCodeNotFoundException.class,
                () -> service.getOriginalUrl("nonexistent"),
                "Unknown short code should throw ShortCodeNotFoundException");
    }
    
    /**
     * Test: Response contains correct short URL format
     */
    @Test
    void testResponseContainsCorrectShortUrlFormat() {
        ShortenResponse response = service.shortenUrl("https://example.com");
        
        String expectedPrefix = "http://localhost:8080/";
        assertTrue(response.shortUrl().startsWith(expectedPrefix),
                "Short URL should start with " + expectedPrefix);
        
        String shortCodeFromUrl = response.shortUrl().substring(expectedPrefix.length());
        assertEquals(response.shortCode(), shortCodeFromUrl,
                "Short code in URL should match shortCode field");
    }
    
    /**
     * Test: 100 URLs generate 100 unique short codes
     */
    @Test
    void test100UrlsGenerate100UniqueShortCodes() {
        Set<String> shortCodes = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            ShortenResponse response = service.shortenUrl("https://example.com/page" + i);
            shortCodes.add(response.shortCode());
        }
        
        assertEquals(100, shortCodes.size(),
                "100 URLs should generate 100 unique short codes");
    }
    
    /**
     * Test: All generated short codes are valid Base62
     */
    @Test
    void testAllGeneratedShortCodesAreValidBase62() {
        for (int i = 0; i < 50; i++) {
            ShortenResponse response = service.shortenUrl("https://example.com/page" + i);
            assertTrue(BASE62_PATTERN.matcher(response.shortCode()).matches(),
                    "Short code should be valid Base62: " + response.shortCode());
        }
    }
    
    /**
     * Test: Service handles very long URLs
     */
    @Test
    void testServiceHandlesVeryLongUrls() {
        String longUrl = "https://example.com/" + "a".repeat(1000);
        
        ShortenResponse response = service.shortenUrl(longUrl);
        String retrieved = service.getOriginalUrl(response.shortCode());
        
        assertEquals(longUrl, retrieved,
                "Service should handle very long URLs");
    }
    
    /**
     * Test: Service handles URLs with special characters
     */
    @Test
    void testServiceHandlesUrlsWithSpecialCharacters() {
        String url = "https://example.com/path?query=value&param=123#anchor";
        
        ShortenResponse response = service.shortenUrl(url);
        String retrieved = service.getOriginalUrl(response.shortCode());
        
        assertEquals(url, retrieved,
                "Service should handle URLs with special characters");
    }
}
