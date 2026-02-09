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
        // Validate URL format
        validateUrl(request.url());
        
        // Delegate to service layer
        ShortenResponse response = urlShortenerService.shortenUrl(request.url());
        
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
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }
}
