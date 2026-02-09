package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;

/**
 * Service interface for URL shortening operations.
 * 
 * Implementation will be replaced in Epic 2 (ID generation) and Epic 3 (persistence).
 */
public interface UrlShortenerService {
    
    /**
     * Shortens a given URL and returns the short code and full shortened URL.
     * 
     * @param url the long URL to shorten (must be valid HTTP/HTTPS)
     * @return ShortenResponse containing shortCode and shortUrl
     */
    ShortenResponse shortenUrl(String url);
    
    /**
     * Retrieves the original URL for a given short code.
     * 
     * @param shortCode the short code to look up
     * @return the original URL
     * @throws com.example.urlshortener.exception.ShortCodeNotFoundException if short code doesn't exist
     */
    String getOriginalUrl(String shortCode);
}
