package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of UrlShortenerService for Story 1.1.
 * 
 * Returns hardcoded response. Will be replaced with real implementation in:
 * - Epic 2: SnowflakeIdGenerator integration (Story 2.4)
 * - Epic 3: Database persistence (Story 3.3)
 */
@Service
public class UrlShortenerServiceStub implements UrlShortenerService {
    
    @Override
    public ShortenResponse shortenUrl(String url) {
        // Stub implementation - returns hardcoded response
        // Real implementation will generate unique short codes and persist to database
        return new ShortenResponse("STUB123", "http://localhost:8080/STUB123");
    }
}
