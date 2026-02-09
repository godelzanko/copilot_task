package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ShortenController using MockMvc.
 */
@WebMvcTest(ShortenController.class)
class ShortenControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UrlShortenerService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void validUrlReturns200OK() throws Exception {
        // Given
        String requestBody = "{\"url\": \"https://example.com/test\"}";
        ShortenResponse mockResponse = new ShortenResponse("aB3xK9", "http://localhost:8080/aB3xK9");
        
        when(service.shortenUrl(anyString())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.shortCode").value("aB3xK9"))
            .andExpect(jsonPath("$.shortUrl").exists());
    }
    
    @Test
    void missingUrlReturns400() throws Exception {
        // Given
        String requestBody = "{}";
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Error"))
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void blankUrlReturns400() throws Exception {
        // Given
        String requestBody = "{\"url\": \"\"}";
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Error"));
    }
    
    @Test
    void invalidUrlFormatReturns400() throws Exception {
        // Given
        String requestBody = "{\"url\": \"not-a-valid-url\"}";
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid URL format"))
            .andExpect(jsonPath("$.message").value("URL must be a valid HTTP or HTTPS URL"));
    }
    
    @Test
    void ftpProtocolReturns400() throws Exception {
        // Given
        String requestBody = "{\"url\": \"ftp://example.com/file\"}";
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid URL format"));
    }
    
    @Test
    void serviceMethodInvokedWithCorrectParameter() throws Exception {
        // Given
        String requestBody = "{\"url\": \"https://example.com/test\"}";
        ShortenResponse mockResponse = new ShortenResponse("testCode", "http://localhost:8080/testCode");
        
        when(service.shortenUrl("https://example.com/test")).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value("testCode"));
    }
}
