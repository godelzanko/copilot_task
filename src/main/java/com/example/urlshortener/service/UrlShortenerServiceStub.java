package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.generator.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub implementation of UrlShortenerService for Story 1.1 and 1.2.
 * 
 * Now uses real SnowflakeIdGenerator (Story 2.4) instead of hardcoded codes.
 * Database persistence will be added in Epic 3 (Story 3.3).
 */
@Service
public class UrlShortenerServiceStub implements UrlShortenerService {
    
    private final Map<String, String> shortCodeToUrl = new HashMap<>();
    private final SnowflakeIdGenerator generator;
    
    public UrlShortenerServiceStub(SnowflakeIdGenerator generator) {
        this.generator = generator;
    }
    
    @Override
    public ShortenResponse shortenUrl(String url) {
        // Generate unique short code using Snowflake ID generator
        String shortCode = generator.generateShortCode();
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
