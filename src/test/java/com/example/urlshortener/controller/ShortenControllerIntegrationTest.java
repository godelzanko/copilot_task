package com.example.urlshortener.controller;

import com.example.urlshortener.AbstractIntegrationTest;
import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ShortenController using TestRestTemplate.
 * Tests complete request-response flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShortenControllerIntegrationTest extends AbstractIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void endToEndShortenFlow() {
        // Given
        ShortenRequest request = new ShortenRequest("https://example.com/very/long/path/to/resource");
        
        // When
        ResponseEntity<ShortenResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ShortenResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().shortCode()).isNotNull();
        assertThat(response.getBody().shortCode()).isNotEmpty();
        assertThat(response.getBody().shortUrl()).contains(response.getBody().shortCode());
    }
    
    @Test
    void invalidUrlReturnsErrorResponse() {
        // Given
        ShortenRequest request = new ShortenRequest("not-a-valid-url");
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Invalid Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid URL format");
    }
    
    @Test
    void blankUrlReturnsValidationError() {
        // Given
        ShortenRequest request = new ShortenRequest("");
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/shorten",
            request,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
    }
}
