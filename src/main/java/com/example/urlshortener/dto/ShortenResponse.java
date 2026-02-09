package com.example.urlshortener.dto;

/**
 * Response DTO for successful URL shortening.
 * 
 * @param shortCode the generated short code (e.g., "aB3xK9")
 * @param shortUrl the complete shortened URL (e.g., "http://localhost:8080/aB3xK9")
 */
public record ShortenResponse(
    String shortCode,
    String shortUrl
) {}
