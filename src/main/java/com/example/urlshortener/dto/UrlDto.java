package com.example.urlshortener.dto;

import com.example.urlshortener.entity.UrlEntity;

/**
 * Data Transfer Object for URL entity responses.
 * Immutable record containing the short code and full shortened URL.
 * 
 * @param shortCode the Base62-encoded Snowflake ID
 * @param shortUrl the complete shortened URL (base URL + short code)
 */
public record UrlDto(String shortCode, String shortUrl) {
    
    /**
     * Converts a UrlEntity to a UrlDto.
     * 
     * @param entity the URL entity to convert
     * @param baseUrl the base URL for constructing the full shortened URL
     * @return UrlDto with short code and complete URL
     */
    public static UrlDto toDto(UrlEntity entity, String baseUrl) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL cannot be null or blank");
        }
        
        // Ensure baseUrl doesn't end with slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String fullUrl = cleanBaseUrl + "/" + entity.getShortCode();
        
        return new UrlDto(entity.getShortCode(), fullUrl);
    }
}
