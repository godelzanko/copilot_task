package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * REST controller for URL shortening operations.
 * Handles POST /api/shorten endpoint.
 */
@RestController
@RequestMapping("/api")
public class ShortenController {
    
    private final UrlShortenerService urlShortenerService;
    
    public ShortenController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }
    
    /**
     * Shortens a long URL and returns the short code and full shortened URL.
     * 
     * @param request the shorten request containing the URL to shorten
     * @return ResponseEntity with ShortenResponse (200 OK)
     * @throws IllegalArgumentException if URL format is invalid or protocol is not HTTP/HTTPS
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        // Trim and validate URL format
        String trimmedUrl = request.url().trim();
        validateUrl(trimmedUrl);
        
        // Normalize URL (lowercase scheme and host, preserve path case)
        String normalizedUrl = normalizeUrl(trimmedUrl);
        
        // Delegate to service layer
        ShortenResponse response = urlShortenerService.shortenUrl(normalizedUrl);
        
        // Build full short URL using current context path
        String shortUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/{shortCode}")
            .buildAndExpand(response.shortCode())
            .toUriString();
        
        // Return updated response with proper short URL
        ShortenResponse finalResponse = new ShortenResponse(response.shortCode(), shortUrl);
        
        return ResponseEntity.ok(finalResponse);
    }
    
    /**
     * Validates URL format and protocol.
     * 
     * @param urlString the URL to validate
     * @throws IllegalArgumentException if URL is invalid or protocol is not HTTP/HTTPS
     */
    private void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException("Only HTTP and HTTPS protocols are supported");
            }
            
            if (url.getUserInfo() != null) {
                throw new IllegalArgumentException("URLs with user credentials are not supported");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }
    
    /**
     * Normalizes URL according to RFC 3986.
     * Lowercases scheme and host (case-insensitive) while preserving path case (case-sensitive).
     * Strips fragment as it's client-side only per RFC 3986.
     * 
     * @param urlString the URL to normalize
     * @return normalized URL string
     */
    private String normalizeUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String normalizedScheme = url.getProtocol().toLowerCase();
            String normalizedHost = url.getHost().toLowerCase();
            int port = url.getPort();
            String file = url.getFile(); // includes path and query
            
            StringBuilder normalized = new StringBuilder();
            normalized.append(normalizedScheme).append("://").append(normalizedHost);
            
            if (port != -1 && port != url.getDefaultPort()) {
                normalized.append(":").append(port);
            }
            
            normalized.append(file);
            
            return normalized.toString();
        } catch (MalformedURLException e) {
            // Should not happen as validation is done first
            return urlString;
        }
    }
}
