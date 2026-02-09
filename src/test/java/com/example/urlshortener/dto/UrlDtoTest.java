package com.example.urlshortener.dto;

import com.example.urlshortener.entity.UrlEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for UrlDto record and its mapper method.
 */
class UrlDtoTest {

    @Test
    void testToDto_ShouldCreateDtoWithCorrectShortUrl() {
        // Given
        UrlEntity entity = new UrlEntity("abc123", "https://example.com/long", Instant.now());
        String baseUrl = "http://localhost:8080";

        // When
        UrlDto dto = UrlDto.toDto(entity, baseUrl);

        // Then
        assertThat(dto.shortCode()).isEqualTo("abc123");
        assertThat(dto.shortUrl()).isEqualTo("http://localhost:8080/abc123");
    }

    @Test
    void testToDto_ShouldHandleBaseUrlWithTrailingSlash() {
        // Given
        UrlEntity entity = new UrlEntity("xyz789", "https://example.com", Instant.now());
        String baseUrl = "http://localhost:8080/"; // Trailing slash

        // When
        UrlDto dto = UrlDto.toDto(entity, baseUrl);

        // Then
        assertThat(dto.shortUrl()).isEqualTo("http://localhost:8080/xyz789");
        assertThat(dto.shortUrl()).doesNotContain("//xyz789"); // No double slash
    }

    @Test
    void testToDto_ShouldHandleBaseUrlWithoutTrailingSlash() {
        // Given
        UrlEntity entity = new UrlEntity("test", "https://example.com", Instant.now());
        String baseUrl = "http://localhost:8080"; // No trailing slash

        // When
        UrlDto dto = UrlDto.toDto(entity, baseUrl);

        // Then
        assertThat(dto.shortUrl()).isEqualTo("http://localhost:8080/test");
    }

    @Test
    void testToDto_WithNullEntity_ShouldThrowException() {
        // Given
        String baseUrl = "http://localhost:8080";

        // When/Then
        assertThatThrownBy(() -> UrlDto.toDto(null, baseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity cannot be null");
    }

    @Test
    void testToDto_WithNullBaseUrl_ShouldThrowException() {
        // Given
        UrlEntity entity = new UrlEntity("abc", "https://example.com", Instant.now());

        // When/Then
        assertThatThrownBy(() -> UrlDto.toDto(entity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base URL cannot be null or blank");
    }

    @Test
    void testToDto_WithBlankBaseUrl_ShouldThrowException() {
        // Given
        UrlEntity entity = new UrlEntity("abc", "https://example.com", Instant.now());

        // When/Then
        assertThatThrownBy(() -> UrlDto.toDto(entity, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base URL cannot be null or blank");
    }

    @Test
    void testRecordEquality() {
        // Given
        UrlDto dto1 = new UrlDto("abc123", "http://localhost:8080/abc123");
        UrlDto dto2 = new UrlDto("abc123", "http://localhost:8080/abc123");
        UrlDto dto3 = new UrlDto("xyz789", "http://localhost:8080/xyz789");

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void testRecordToString() {
        // Given
        UrlDto dto = new UrlDto("abc123", "http://localhost:8080/abc123");

        // Then
        assertThat(dto.toString()).contains("abc123");
        assertThat(dto.toString()).contains("http://localhost:8080/abc123");
    }
}
