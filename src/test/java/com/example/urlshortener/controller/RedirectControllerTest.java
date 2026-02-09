package com.example.urlshortener.controller;

import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RedirectController.
 * Tests redirect endpoint functionality with mocked service layer.
 */
@WebMvcTest(RedirectController.class)
class RedirectControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UrlShortenerService urlShortenerService;
    
    /**
     * AC #1, #2: Test existing short code returns 301 with correct Location header.
     */
    @Test
    void testRedirect_ExistingShortCode_Returns301WithLocationHeader() throws Exception {
        // Arrange
        String shortCode = "aB3xK9";
        String originalUrl = "https://example.com/very/long/url";
        when(urlShortenerService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        
        // Act & Assert
        mockMvc.perform(get("/{shortCode}", shortCode))
            .andExpect(status().isMovedPermanently())  // HTTP 301
            .andExpect(header().string("Location", originalUrl))
            .andExpect(content().string(""));  // No response body
        
        verify(urlShortenerService).getOriginalUrl(shortCode);
    }
    
    /**
     * AC #3: Test non-existent short code returns 404 Not Found.
     */
    @Test
    void testRedirect_NonExistentShortCode_Returns404() throws Exception {
        // Arrange
        String shortCode = "invalid";
        when(urlShortenerService.getOriginalUrl(shortCode))
            .thenThrow(new ShortCodeNotFoundException(shortCode));
        
        // Act & Assert
        mockMvc.perform(get("/{shortCode}", shortCode))
            .andExpect(status().isNotFound())  // HTTP 404
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Short code not found: " + shortCode));
        
        verify(urlShortenerService).getOriginalUrl(shortCode);
    }
    
    /**
     * AC #5: Test case-sensitive short code handling (aB3xK9 vs AB3XK9).
     */
    @Test
    void testRedirect_CaseSensitiveShortCodes_TreatedDifferently() throws Exception {
        // Arrange
        String lowerCaseCode = "aB3xK9";
        String upperCaseCode = "AB3XK9";
        String url1 = "https://example.com/url1";
        String url2 = "https://example.com/url2";
        
        when(urlShortenerService.getOriginalUrl(lowerCaseCode)).thenReturn(url1);
        when(urlShortenerService.getOriginalUrl(upperCaseCode)).thenReturn(url2);
        
        // Act & Assert - lowercase variant
        mockMvc.perform(get("/{shortCode}", lowerCaseCode))
            .andExpect(status().isMovedPermanently())
            .andExpect(header().string("Location", url1));
        
        // Act & Assert - uppercase variant
        mockMvc.perform(get("/{shortCode}", upperCaseCode))
            .andExpect(status().isMovedPermanently())
            .andExpect(header().string("Location", url2));
        
        // Verify both calls were made with different parameters
        verify(urlShortenerService).getOriginalUrl(lowerCaseCode);
        verify(urlShortenerService).getOriginalUrl(upperCaseCode);
    }
    
    /**
     * AC #4: Test service method invocation with correct parameter.
     */
    @Test
    void testRedirect_DelegatesToService_WithCorrectParameter() throws Exception {
        // Arrange
        String shortCode = "xyz123";
        String originalUrl = "https://test.com";
        when(urlShortenerService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        
        // Act
        mockMvc.perform(get("/{shortCode}", shortCode));
        
        // Assert - verify service was called with exact parameter
        verify(urlShortenerService).getOriginalUrl(shortCode);
    }
}
