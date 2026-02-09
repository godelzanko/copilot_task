package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub implementation of UrlShortenerService for Story 1.1 and 1.2.
 * 
 * Returns hardcoded response and stores mappings in memory. Will be replaced with real implementation in:
 * - Epic 2: SnowflakeIdGenerator integration (Story 2.4)
 * - Epic 3: Database persistence (Story 3.3)
 */
@Service
public class UrlShortenerServiceStub implements UrlShortenerService {
    
    private final Map<String, String> shortCodeToUrl = new HashMap<>();
    
    @Override
    public ShortenResponse shortenUrl(String url) {
        // Stub implementation - returns hardcoded response
        // Real implementation will generate unique short codes and persist to database
        String shortCode = "STUB123";
        shortCodeToUrl.put(shortCode, url);
        return new ShortenResponse(shortCode, "http://localhost:8080/" + shortCode);
    }
    
    @Override
    public String getOriginalUrl(String shortCode) {
        String originalUrl = shortCodeToUrl.get(shortCode);
        if (originalUrl == null) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return originalUrl;
    }
}
