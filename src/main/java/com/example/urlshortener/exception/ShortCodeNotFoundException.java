package com.example.urlshortener.exception;

/**
 * Exception thrown when a requested short code does not exist in the system.
 */
public class ShortCodeNotFoundException extends RuntimeException {
    
    public ShortCodeNotFoundException(String shortCode) {
        super("Short code not found: " + shortCode);
    }
}
