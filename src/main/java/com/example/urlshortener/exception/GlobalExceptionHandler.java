package com.example.urlshortener.exception;

import com.example.urlshortener.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers.
 * Provides consistent error responses across the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles validation errors from @Valid annotated request bodies.
     * 
     * @param ex the validation exception
     * @return 400 Bad Request with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Validation Error",
            errors,
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles illegal argument exceptions (e.g., invalid URL format).
     * 
     * @param ex the illegal argument exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Invalid Request",
            ex.getMessage(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles short code not found exceptions.
     * 
     * @param ex the short code not found exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShortCodeNotFoundException(ShortCodeNotFoundException ex) {
        log.info("Short code not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Not Found",
            ex.getMessage(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handles all other unexpected exceptions.
     * 
     * @param ex the exception
     * @return 500 Internal Server Error with safe error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        // Don't expose stack traces in production
        ErrorResponse errorResponse = new ErrorResponse(
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
