package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for URL shortening endpoint.
 * 
 * @param url the long URL to be shortened (must not be blank)
 */
public record ShortenRequest(
    @NotBlank(message = "URL is required") 
    String url
) {}
