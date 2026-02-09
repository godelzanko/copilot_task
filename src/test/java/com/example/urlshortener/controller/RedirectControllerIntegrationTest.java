package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RedirectController.
 * Tests complete redirect flow end-to-end with real service layer (stub implementation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedirectControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    /**
     * AC #1, #2: Test complete redirect flow end-to-end.
     * Uses hardcoded short code that stub service recognizes.
     */
    @Test
    void testRedirect_ExistingShortCode_Returns301WithLocationHeader() {
        // Arrange - First, create a shortened URL to populate the stub's internal map
        String originalUrl = "https://example.com/very/long/url/for/testing";
        String requestBody = String.format("{\"url\": \"%s\"}", originalUrl);
        
        // Set up HTTP headers with Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        // Create short code via POST /api/shorten (stub returns "STUB123")
        restTemplate.postForEntity(
            "/api/shorten",
            request,
            String.class
        );
        
        // Act - Attempt redirect using the hardcoded stub short code
        String shortCode = "STUB123";  // Known stub short code
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(
            "/{shortCode}",
            Void.class,
            shortCode
        );
        
        // Assert
        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);  // HTTP 301
        assertThat(redirectResponse.getHeaders().getLocation()).isNotNull();
        assertThat(redirectResponse.getHeaders().getLocation().toString()).isEqualTo(originalUrl);
        assertThat(redirectResponse.getBody()).isNull();  // No response body for redirects
    }
    
    /**
     * AC #3: Test 404 error response structure for non-existent short code.
     */
    @Test
    void testRedirect_NonExistentShortCode_Returns404WithErrorResponse() {
        // Arrange
        String nonExistentShortCode = "NONEXIST";
        
        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
            "/{shortCode}",
            ErrorResponse.class,
            nonExistentShortCode
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);  // HTTP 404
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("Short code not found");
        assertThat(response.getBody().message()).contains(nonExistentShortCode);
    }
    
    /**
     * AC #5: Test case-sensitive short code handling in integration scenario.
     * Note: Current stub always returns "STUB123", so this test demonstrates the principle.
     */
    @Test
    void testRedirect_CaseSensitiveShortCodes_TreatedAsDifferent() {
        // This test demonstrates that the endpoint preserves case sensitivity
        // In real implementation (Epic 3), different case variants would map to different URLs
        
        // Arrange
        String lowerCode = "abc123";  // Non-existent in stub
        String upperCode = "ABC123";  // Non-existent in stub
        
        // Act - Both should fail with 404 (stub doesn't have these codes)
        ResponseEntity<ErrorResponse> lowerResponse = restTemplate.getForEntity(
            "/{shortCode}",
            ErrorResponse.class,
            lowerCode
        );
        
        ResponseEntity<ErrorResponse> upperResponse = restTemplate.getForEntity(
            "/{shortCode}",
            ErrorResponse.class,
            upperCode
        );
        
        // Assert - Both 404, but with different short codes in error message (proving case preservation)
        assertThat(lowerResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(lowerResponse.getBody().message()).contains(lowerCode);
        
        assertThat(upperResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(upperResponse.getBody().message()).contains(upperCode);
        
        // Verify case was NOT normalized (error messages contain original case)
        assertThat(lowerResponse.getBody().message()).isNotEqualTo(upperResponse.getBody().message());
    }
}
