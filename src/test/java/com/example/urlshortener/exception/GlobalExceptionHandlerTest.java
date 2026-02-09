package com.example.urlshortener.exception;

import com.example.urlshortener.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests all exception handler methods for correct HTTP status codes,
 * error response structure, and timestamp presence.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleValidationException_Returns400WithTimestamp() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "url", "URL must not be blank");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // Act
        Instant beforeCall = Instant.now();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex);
        Instant afterCall = Instant.now();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
        assertThat(response.getBody().message()).contains("URL must not be blank");
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(beforeCall, afterCall);
    }

    @Test
    void testHandleIllegalArgumentException_Returns400WithTimestamp() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid URL format");

        // Act
        Instant beforeCall = Instant.now();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);
        Instant afterCall = Instant.now();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Invalid Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid URL format");
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(beforeCall, afterCall);
    }

    @Test
    void testHandleShortCodeNotFoundException_Returns404WithTimestamp() {
        // Arrange
        ShortCodeNotFoundException ex = new ShortCodeNotFoundException("xyz123");

        // Act
        Instant beforeCall = Instant.now();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleShortCodeNotFoundException(ex);
        Instant afterCall = Instant.now();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("xyz123");
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(beforeCall, afterCall);
    }

    @Test
    void testHandleGenericException_Returns500WithTimestamp() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected error");

        // Act
        Instant beforeCall = Instant.now();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);
        Instant afterCall = Instant.now();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(beforeCall, afterCall);
    }

    @Test
    void testGenericException_DoesNotExposeStackTrace() {
        // Arrange
        Exception ex = new NullPointerException("Something was null");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("NullPointerException");
        assertThat(response.getBody().message()).doesNotContain("null");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void testValidationException_WithMultipleFieldErrors() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("object", "url", "URL must not be blank");
        FieldError fieldError2 = new FieldError("object", "customAlias", "Custom alias is too long");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("URL must not be blank");
        assertThat(response.getBody().message()).contains("Custom alias is too long");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void testTimestampIsWithinReasonableTimeWindow() {
        // Arrange
        Exception ex = new RuntimeException("Test");

        // Act
        Instant beforeCall = Instant.now();
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isCloseTo(beforeCall, within(5, java.time.temporal.ChronoUnit.SECONDS));
    }
}
