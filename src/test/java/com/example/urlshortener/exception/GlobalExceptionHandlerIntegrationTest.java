package com.example.urlshortener.exception;

import com.example.urlshortener.dto.ErrorResponse;
import com.example.urlshortener.dto.ShortenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GlobalExceptionHandler.
 * Tests complete error handling flow end-to-end through real HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * AC #2, #3: Test validation error triggers 400 with proper error structure and timestamp.
     */
    @Test
    void testInvalidUrlValidation_Returns400WithTimestamp() {
        // Arrange - Create request with blank URL (triggers validation error)
        ShortenRequest request = new ShortenRequest("");

        // Act
        Instant beforeRequest = Instant.now();
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ErrorResponse.class
        );
        Instant afterRequest = Instant.now();

        // Assert - AC #2: Validation Error Handling
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
        assertThat(response.getBody().message()).contains("URL");
        
        // AC #2: Verify timestamp field is present and valid
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(
            beforeRequest.minus(Duration.ofSeconds(5)), 
            afterRequest.plus(Duration.ofSeconds(5))
        );
        
        // Verify consistent JSON structure
        assertThat(response.getBody().error()).isNotNull();
        assertThat(response.getBody().message()).isNotNull();
    }

    /**
     * AC #4: Test ShortCodeNotFoundException triggers 404 with proper error structure.
     */
    @Test
    void testNonExistentShortCode_Returns404WithTimestamp() {
        // Arrange
        String nonExistentCode = "NONEXIST999";

        // Act
        Instant beforeRequest = Instant.now();
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/{shortCode}",
            ErrorResponse.class,
            nonExistentCode
        );
        Instant afterRequest = Instant.now();

        // Assert - AC #4: Custom Exception Handling
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("Short code not found");
        assertThat(response.getBody().message()).contains(nonExistentCode);
        
        // AC #2: Verify timestamp field is present and valid
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(
            beforeRequest.minus(Duration.ofSeconds(5)), 
            afterRequest.plus(Duration.ofSeconds(5))
        );
    }

    /**
     * AC #2: Test timestamp field is present in all error responses.
     */
    @Test
    void testIllegalArgumentException_Returns400WithTimestamp() {
        // Arrange - Invalid URL format (not a URL at all)
        ShortenRequest request = new ShortenRequest("not-a-valid-url");

        // Act
        Instant beforeRequest = Instant.now();
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ErrorResponse.class
        );
        Instant afterRequest = Instant.now();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Invalid Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid URL format");
        
        // Verify timestamp is present and within reasonable range
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isBetween(
            beforeRequest.minus(Duration.ofSeconds(5)), 
            afterRequest.plus(Duration.ofSeconds(5))
        );
    }

    /**
     * AC #2, #6: Test that error responses can be parsed consistently by clients.
     * Verifies JSON structure consistency across all error types.
     */
    @Test
    void testErrorResponseStructureConsistency() throws Exception {
        // Arrange - Three different error scenarios
        ShortenRequest blankUrlRequest = new ShortenRequest("");
        ShortenRequest invalidUrlRequest = new ShortenRequest("invalid");
        String nonExistentCode = "XYZ999";

        // Act - Get all three error types
        ResponseEntity<ErrorResponse> validationError = restTemplate.postForEntity(
            "/api/shorten",
            blankUrlRequest,
            ErrorResponse.class
        );

        ResponseEntity<ErrorResponse> illegalArgError = restTemplate.postForEntity(
            "/api/shorten",
            invalidUrlRequest,
            ErrorResponse.class
        );

        ResponseEntity<ErrorResponse> notFoundError = restTemplate.getForEntity(
            "/{shortCode}",
            ErrorResponse.class,
            nonExistentCode
        );

        // Assert - All have consistent structure
        assertThat(validationError.getBody()).isNotNull();
        assertThat(validationError.getBody().error()).isNotNull();
        assertThat(validationError.getBody().message()).isNotNull();
        assertThat(validationError.getBody().timestamp()).isNotNull();

        assertThat(illegalArgError.getBody()).isNotNull();
        assertThat(illegalArgError.getBody().error()).isNotNull();
        assertThat(illegalArgError.getBody().message()).isNotNull();
        assertThat(illegalArgError.getBody().timestamp()).isNotNull();

        assertThat(notFoundError.getBody()).isNotNull();
        assertThat(notFoundError.getBody().error()).isNotNull();
        assertThat(notFoundError.getBody().message()).isNotNull();
        assertThat(notFoundError.getBody().timestamp()).isNotNull();

        // Verify all can be serialized/deserialized consistently
        String json1 = objectMapper.writeValueAsString(validationError.getBody());
        String json2 = objectMapper.writeValueAsString(illegalArgError.getBody());
        String json3 = objectMapper.writeValueAsString(notFoundError.getBody());

        assertThat(json1).contains("\"error\":", "\"message\":", "\"timestamp\":");
        assertThat(json2).contains("\"error\":", "\"message\":", "\"timestamp\":");
        assertThat(json3).contains("\"error\":", "\"message\":", "\"timestamp\":");
    }

    /**
     * AC #5: Verify no stack traces are exposed in error responses.
     */
    @Test
    void testNoStackTracesExposed() {
        // Arrange - Multiple error scenarios
        ShortenRequest request = new ShortenRequest("");

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ErrorResponse.class
        );

        // Assert - Error message should not contain stack trace keywords
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("Exception");
        assertThat(response.getBody().message()).doesNotContain("at java.");
        assertThat(response.getBody().message()).doesNotContain("at org.springframework");
        assertThat(response.getBody().message()).doesNotContain("Caused by:");
        
        // Error message should be human-readable, not technical
        assertThat(response.getBody().error()).isIn("Validation Error", "Invalid Request", "Not Found", "Internal Server Error");
    }

    /**
     * AC #2: Test timestamp is in ISO-8601 format when serialized to JSON.
     */
    @Test
    void testTimestampSerializationFormat() throws Exception {
        // Arrange
        ShortenRequest request = new ShortenRequest("");

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            String.class
        );

        // Assert - Check raw JSON response
        String jsonResponse = response.getBody();
        assertThat(jsonResponse).isNotNull();
        
        // Timestamp should be in ISO-8601 format (e.g., "2026-02-09T14:30:45.123Z")
        assertThat(jsonResponse).containsPattern("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z\"");
        
        // Verify it can be deserialized back
        ErrorResponse errorResponse = objectMapper.readValue(jsonResponse, ErrorResponse.class);
        assertThat(errorResponse.timestamp()).isNotNull();
        assertThat(errorResponse.timestamp()).isInstanceOf(Instant.class);
    }
}
