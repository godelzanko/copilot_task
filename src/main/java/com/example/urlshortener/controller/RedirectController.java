package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST controller for URL redirection.
 * Handles GET /{shortCode} endpoint.
 */
@RestController
public class RedirectController {
    
    private final UrlShortenerService urlShortenerService;
    
    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }
    
    /**
     * Redirects a short code to its original URL using HTTP 301 Moved Permanently.
     * 
     * @param shortCode the short code to redirect
     * @return ResponseEntity with 301 status and Location header
     * @throws com.example.urlshortener.exception.ShortCodeNotFoundException if short code doesn't exist (handled as 404)
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);
        
        return ResponseEntity
            .status(HttpStatus.MOVED_PERMANENTLY)
            .location(URI.create(originalUrl))
            .build();
    }
}
