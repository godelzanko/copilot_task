package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.generator.SnowflakeIdGenerator;
import com.example.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlShortenerServiceImpl focusing on idempotency and normalization.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>URL normalization logic (trim + lowercase)</li>
 *   <li>Try-insert-catch-select pattern</li>
 *   <li>Idempotency behavior with constraint violations</li>
 *   <li>Edge cases (null, empty, whitespace)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceImplTest {
    
    @Mock
    private UrlRepository urlRepository;
    
    @Mock
    private SnowflakeIdGenerator generator;
    
    private UrlShortenerServiceImpl service;
    
    @BeforeEach
    void setUp() {
        service = new UrlShortenerServiceImpl(urlRepository, generator, "http://localhost:8080/");
    }
    
    // ========== URL Normalization Tests ==========
    
    @Test
    void normalizeUrl_trimsWhitespace() {
        // Given
        String urlWithWhitespace = "  https://example.com  ";
        
        // When
        String normalized = service.normalizeUrl(urlWithWhitespace);
        
        // Then
        assertThat(normalized).isEqualTo("https://example.com");
    }
    
    @Test
    void normalizeUrl_convertsToLowercase() {
        // Given
        String mixedCaseUrl = "HTTPS://EXAMPLE.COM";
        
        // When
        String normalized = service.normalizeUrl(mixedCaseUrl);
        
        // Then
        assertThat(normalized).isEqualTo("https://example.com");
    }
    
    @Test
    void normalizeUrl_trimsBeforeLowercase() {
        // Given
        String url = "  HTTPS://EXAMPLE.COM  ";
        
        // When
        String normalized = service.normalizeUrl(url);
        
        // Then
        assertThat(normalized).isEqualTo("https://example.com");
    }
    
    @Test
    void normalizeUrl_handlesEmptyString() {
        // When
        String normalized = service.normalizeUrl("");
        
        // Then
        assertThat(normalized).isEmpty();
    }
    
    @Test
    void normalizeUrl_handlesWhitespaceOnly() {
        // When
        String normalized = service.normalizeUrl("   ");
        
        // Then
        assertThat(normalized).isEmpty();
    }
    
    @Test
    void normalizeUrl_handlesNull() {
        // When
        String normalized = service.normalizeUrl(null);
        
        // Then
        assertThat(normalized).isNull();
    }
    
    // ========== Idempotency Tests ==========
    
    @Test
    void shortenUrl_createsNewMappingForNewUrl() {
        // Given
        String url = "https://example.com";
        String shortCode = "abc123";
        UrlEntity savedEntity = new UrlEntity(shortCode, "https://example.com", Instant.now());
        
        when(generator.generateShortCode()).thenReturn(shortCode);
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);
        
        // When
        ShortenResponse response = service.shortenUrl(url);
        
        // Then
        assertThat(response.shortCode()).isEqualTo(shortCode);
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + shortCode);
        verify(urlRepository).save(any(UrlEntity.class));
        verify(urlRepository, never()).findByNormalizedUrl(anyString());
    }
    
    @Test
    void shortenUrl_returnsExistingMappingOnConstraintViolation() {
        // Given
        String url = "https://example.com";
        String existingShortCode = "existing123";
        UrlEntity existingEntity = new UrlEntity(existingShortCode, "https://example.com", Instant.now());
        
        when(generator.generateShortCode()).thenReturn("newCode");
        when(urlRepository.save(any(UrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(urlRepository.findByNormalizedUrl("https://example.com"))
                .thenReturn(Optional.of(existingEntity));
        
        // When
        ShortenResponse response = service.shortenUrl(url);
        
        // Then
        assertThat(response.shortCode()).isEqualTo(existingShortCode);
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/" + existingShortCode);
        verify(urlRepository).save(any(UrlEntity.class));
        verify(urlRepository).findByNormalizedUrl("https://example.com");
    }
    
    @Test
    void shortenUrl_normalizesUrlBeforeSaving() {
        // Given
        String url = "  HTTPS://EXAMPLE.COM  ";
        String shortCode = "abc123";
        UrlEntity savedEntity = new UrlEntity(shortCode, "https://example.com", Instant.now());
        
        when(generator.generateShortCode()).thenReturn(shortCode);
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);
        
        // When
        service.shortenUrl(url);
        
        // Then
        verify(urlRepository).save(argThat(entity -> 
            entity.getOriginalUrl().equals("https://example.com")
        ));
    }
    
    @Test
    void shortenUrl_sameUrlDifferentCase_returnsExistingMapping() {
        // Given - first request saves lowercase
        String originalUrl = "https://example.com";
        String uppercaseUrl = "HTTPS://EXAMPLE.COM";
        String existingShortCode = "existing123";
        UrlEntity existingEntity = new UrlEntity(existingShortCode, "https://example.com", Instant.now());
        
        when(generator.generateShortCode()).thenReturn("newCode");
        when(urlRepository.save(any(UrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(urlRepository.findByNormalizedUrl("https://example.com"))
                .thenReturn(Optional.of(existingEntity));
        
        // When - second request with uppercase
        ShortenResponse response = service.shortenUrl(uppercaseUrl);
        
        // Then - returns existing short code
        assertThat(response.shortCode()).isEqualTo(existingShortCode);
        verify(urlRepository).findByNormalizedUrl("https://example.com");
    }
    
    @Test
    void shortenUrl_sameUrlWithWhitespace_returnsExistingMapping() {
        // Given
        String urlWithWhitespace = "  https://example.com  ";
        String existingShortCode = "existing123";
        UrlEntity existingEntity = new UrlEntity(existingShortCode, "https://example.com", Instant.now());
        
        when(generator.generateShortCode()).thenReturn("newCode");
        when(urlRepository.save(any(UrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(urlRepository.findByNormalizedUrl("https://example.com"))
                .thenReturn(Optional.of(existingEntity));
        
        // When
        ShortenResponse response = service.shortenUrl(urlWithWhitespace);
        
        // Then
        assertThat(response.shortCode()).isEqualTo(existingShortCode);
    }
    
    @Test
    void shortenUrl_differentUrls_generateDifferentShortCodes() {
        // Given
        String url1 = "https://example.com";
        String url2 = "https://different.com";
        String shortCode1 = "code1";
        String shortCode2 = "code2";
        
        UrlEntity entity1 = new UrlEntity(shortCode1, url1, Instant.now());
        UrlEntity entity2 = new UrlEntity(shortCode2, url2, Instant.now());
        
        when(generator.generateShortCode()).thenReturn(shortCode1, shortCode2);
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(entity1, entity2);
        
        // When
        ShortenResponse response1 = service.shortenUrl(url1);
        ShortenResponse response2 = service.shortenUrl(url2);
        
        // Then
        assertThat(response1.shortCode()).isNotEqualTo(response2.shortCode());
    }
    
    @Test
    void shortenUrl_constraintViolationButNoExisting_throwsIllegalStateException() {
        // Given - constraint violation but findByNormalizedUrl returns empty
        String url = "https://example.com";
        
        when(generator.generateShortCode()).thenReturn("newCode");
        when(urlRepository.save(any(UrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(urlRepository.findByNormalizedUrl("https://example.com"))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> service.shortenUrl(url))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Constraint violation occurred but no existing mapping found");
    }
    
    // ========== getOriginalUrl Tests ==========
    
    @Test
    void getOriginalUrl_returnsUrlForExistingShortCode() {
        // Given
        String shortCode = "abc123";
        String originalUrl = "https://example.com";
        UrlEntity entity = new UrlEntity(shortCode, originalUrl, Instant.now());
        
        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(entity));
        
        // When
        String result = service.getOriginalUrl(shortCode);
        
        // Then
        assertThat(result).isEqualTo(originalUrl);
    }
    
    @Test
    void getOriginalUrl_throwsExceptionForNonExistentShortCode() {
        // Given
        String shortCode = "nonexistent";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> service.getOriginalUrl(shortCode))
                .isInstanceOf(ShortCodeNotFoundException.class);
    }
}
