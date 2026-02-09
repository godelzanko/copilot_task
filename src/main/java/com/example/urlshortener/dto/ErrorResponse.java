package com.example.urlshortener.dto;

import java.time.Instant;

/**
 * Response DTO for error scenarios.
 * 
 * @param error the error type or category
 * @param message detailed error message for the client
 * @param timestamp when the error occurred
 */
public record ErrorResponse(
    String error,
    String message,
    Instant timestamp
) {}
