package com.example.urlshortener.dto;

/**
 * Response DTO for error scenarios.
 * 
 * @param error the error type or category
 * @param message detailed error message for the client
 */
public record ErrorResponse(
    String error,
    String message
) {}
