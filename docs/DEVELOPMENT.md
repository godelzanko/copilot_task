# Development Guide

This guide provides detailed implementation guidance for building the URL Shortener Service.

## Table of Contents

1. [Development Workflow](#development-workflow)
2. [Code Style Guidelines](#code-style-guidelines)
3. [Implementation Examples](#implementation-examples)
4. [Testing Strategy](#testing-strategy)
5. [Database Best Practices](#database-best-practices)
6. [Security Considerations](#security-considerations)

---

## Development Workflow

### Branch Strategy

```bash
# Create feature branch
git checkout -b feature/snowflake-id-generator

# Make changes, commit frequently
git add .
git commit -m "Implement Snowflake ID generator with Base62 encoding"

# Push to remote
git push origin feature/snowflake-id-generator

# Create pull request for review
```

### Local Development Loop

1. **Make code changes**
2. **Run tests:** `mvn test`
3. **Check code coverage:** `mvn jacoco:report`
4. **Run application:** `mvn spring-boot:run`
5. **Test manually:** Use curl or Postman
6. **Commit changes:** `git commit -am "Your message"`

---

## Code Style Guidelines

### Java Coding Standards

- **Use Java 21 features:** Records, pattern matching, enhanced switch
- **Follow naming conventions:**
  - Classes: `PascalCase` (e.g., `UrlShortenerService`)
  - Methods: `camelCase` (e.g., `shortenUrl()`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `BASE62_ALPHABET`)
  - Packages: `lowercase` (e.g., `com.example.urlshortener`)

### Documentation

- **Javadoc for public APIs:**
  ```java
  /**
   * Generates a collision-free short code using Snowflake algorithm.
   * 
   * @return Base62-encoded short code (typically 7 characters)
   * @throws IllegalStateException if sequence counter overflows
   */
  public String generateShortCode() { ... }
  ```

- **Comments for complex logic:**
  ```java
  // Try to insert new URL mapping
  // If UNIQUE constraint is violated (URL already exists), 
  // catch exception and retrieve existing mapping (idempotency)
  ```

### Spring Boot Best Practices

- **Use constructor injection:**
  ```java
  @Service
  public class UrlShortenerService {
      private final UrlRepository urlRepository;
      private final SnowflakeIdGenerator idGenerator;
      
      public UrlShortenerService(UrlRepository urlRepository, 
                                  SnowflakeIdGenerator idGenerator) {
          this.urlRepository = urlRepository;
          this.idGenerator = idGenerator;
      }
  }
  ```

- **Use Records for DTOs:**
  ```java
  public record ShortenRequest(
      @NotBlank(message = "URL cannot be blank") String url
  ) {}
  ```

---

## Implementation Examples

### Example 1: Snowflake ID Generator

**File:** `src/main/java/com/example/urlshortener/generator/SnowflakeIdGenerator.java`

```java
package com.example.urlshortener.generator;

import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class SnowflakeIdGenerator {
    
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    // Custom epoch: 2024-01-01T00:00:00Z
    private static final long CUSTOM_EPOCH = 1704067200000L;
    
    // Bit allocations
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 13;
    
    private static final int INSTANCE_ID = 0; // Hardcoded for MVP
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 8191
    
    private long lastTimestamp = -1L;
    private long sequence = 0L;
    
    /**
     * Generates a unique Snowflake ID and encodes it in Base62.
     * Thread-safe via synchronized method.
     */
    public synchronized String generate() {
        long currentTimestamp = timestamp();
        
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted, wait for next millisecond
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0;
        }
        
        lastTimestamp = currentTimestamp;
        
        // Construct 64-bit ID
        long id = ((currentTimestamp - CUSTOM_EPOCH) << (INSTANCE_BITS + SEQUENCE_BITS))
                | (INSTANCE_ID << SEQUENCE_BITS)
                | sequence;
        
        return toBase62(id);
    }
    
    private long timestamp() {
        return Instant.now().toEpochMilli();
    }
    
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }
    
    private String toBase62(long value) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62.charAt((int) (value % 62)));
            value /= 62;
        }
        return sb.reverse().toString();
    }
}
```

---

### Example 2: URL Entity

**File:** `src/main/java/com/example/urlshortener/entity/UrlEntity.java`

```java
package com.example.urlshortener.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "urls")
public class UrlEntity {
    
    @Id
    @Column(name = "short_code", length = 10)
    private String shortCode;
    
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Constructors
    public UrlEntity() {}
    
    public UrlEntity(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }
    
    // Getters and setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

---

### Example 3: Repository with Custom Query

**File:** `src/main/java/com/example/urlshortener/repository/UrlRepository.java`

```java
package com.example.urlshortener.repository;

import com.example.urlshortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, String> {
    
    /**
     * Finds URL by normalized original URL (case-insensitive, trimmed).
     * Used for idempotency - same URL always returns same short code.
     */
    @Query("SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = LOWER(TRIM(:url))")
    Optional<UrlEntity> findByNormalizedUrl(@Param("url") String url);
}
```

---

### Example 4: Service Layer with Idempotency

**File:** `src/main/java/com/example/urlshortener/service/UrlShortenerService.java`

```java
package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.generator.SnowflakeIdGenerator;
import com.example.urlshortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class UrlShortenerService {
    
    private final UrlRepository urlRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final String baseUrl;
    
    public UrlShortenerService(
            UrlRepository urlRepository,
            SnowflakeIdGenerator idGenerator,
            @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.idGenerator = idGenerator;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Creates a short URL for the given original URL.
     * Idempotent: same URL always returns the same short code.
     */
    @Transactional
    public ShortenResponse shortenUrl(String originalUrl) {
        validateUrl(originalUrl);
        String normalized = normalizeUrl(originalUrl);
        
        try {
            // Try to insert new mapping
            String shortCode = idGenerator.generate();
            UrlEntity entity = new UrlEntity(shortCode, normalized);
            urlRepository.save(entity);
            return buildResponse(entity);
        } catch (DataIntegrityViolationException e) {
            // URL already exists (UNIQUE constraint violation)
            // Retrieve existing mapping for idempotency
            UrlEntity existing = urlRepository.findByNormalizedUrl(normalized)
                .orElseThrow(() -> new IllegalStateException("Race condition detected"));
            return buildResponse(existing);
        }
    }
    
    /**
     * Retrieves the original URL for a given short code.
     */
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        return urlRepository.findById(shortCode)
            .map(UrlEntity::getOriginalUrl)
            .orElseThrow(() -> new UrlNotFoundException("Short code not found: " + shortCode));
    }
    
    private void validateUrl(String url) {
        try {
            new URL(url); // Java built-in URL validation
        } catch (MalformedURLException e) {
            throw new InvalidUrlException("Invalid URL format: " + url);
        }
    }
    
    private String normalizeUrl(String url) {
        return url.trim().toLowerCase();
    }
    
    private ShortenResponse buildResponse(UrlEntity entity) {
        String shortUrl = baseUrl + "/" + entity.getShortCode();
        return new ShortenResponse(entity.getShortCode(), shortUrl);
    }
}
```

---

### Example 5: REST Controllers

**File:** `src/main/java/com/example/urlshortener/controller/ShortenController.java`

```java
package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ShortenController {
    
    private final UrlShortenerService urlShortenerService;
    
    public ShortenController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }
    
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = urlShortenerService.shortenUrl(request.url());
        return ResponseEntity.ok(response);
    }
}
```

**File:** `src/main/java/com/example/urlshortener/controller/RedirectController.java`

```java
package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
public class RedirectController {
    
    private final UrlShortenerService urlShortenerService;
    
    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }
    
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);
        return ResponseEntity
            .status(HttpStatus.MOVED_PERMANENTLY)
            .location(URI.create(originalUrl))
            .build();
    }
}
```

---

## Testing Strategy

### Unit Tests Example

**File:** `src/test/java/com/example/urlshortener/generator/SnowflakeIdGeneratorTest.java`

```java
package com.example.urlshortener.generator;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdGeneratorTest {
    
    @Test
    void shouldGenerateUniqueIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        Set<String> ids = new HashSet<>();
        
        // Generate 10,000 IDs
        for (int i = 0; i < 10_000; i++) {
            String id = generator.generate();
            ids.add(id);
        }
        
        // All IDs should be unique
        assertThat(ids).hasSize(10_000);
    }
    
    @Test
    void shouldGenerateBase62EncodedIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        String id = generator.generate();
        
        // Base62 pattern: alphanumeric only
        assertThat(id).matches("^[0-9a-zA-Z]+$");
        
        // Typical length: ~7 characters
        assertThat(id.length()).isBetween(6, 10);
    }
}
```

---

### Integration Tests Example

**File:** `src/test/java/com/example/urlshortener/UrlShortenerIntegrationTest.java`

```java
package com.example.urlshortener;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UrlShortenerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldShortenUrlAndRedirect() {
        // 1. Create short URL
        ShortenRequest request = new ShortenRequest("https://www.example.com");
        ResponseEntity<ShortenResponse> createResponse = 
            restTemplate.postForEntity("/api/shorten", request, ShortenResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody().shortCode()).isNotBlank();
        
        // 2. Test redirect
        String shortCode = createResponse.getBody().shortCode();
        ResponseEntity<Void> redirectResponse = 
            restTemplate.getForEntity("/" + shortCode, Void.class);
        
        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
        assertThat(redirectResponse.getHeaders().getLocation().toString())
            .isEqualTo("https://www.example.com");
    }
    
    @Test
    void shouldReturnSameShortCodeForSameUrl() {
        ShortenRequest request = new ShortenRequest("https://www.example.com");
        
        // Create twice
        ResponseEntity<ShortenResponse> response1 = 
            restTemplate.postForEntity("/api/shorten", request, ShortenResponse.class);
        ResponseEntity<ShortenResponse> response2 = 
            restTemplate.postForEntity("/api/shorten", request, ShortenResponse.class);
        
        // Should return same short code (idempotency)
        assertThat(response1.getBody().shortCode())
            .isEqualTo(response2.getBody().shortCode());
    }
}
```

---

## Database Best Practices

### Migration Naming Convention

```
001-create-urls-table.yaml
002-create-normalized-url-index.yaml
003-add-table-comments.yaml
```

### Rollback Strategy

Always include rollback in Liquibase changesets:

```yaml
- changeSet:
    id: 002-create-normalized-url-index
    author: slavaz
    changes:
      - sql:
          sql: CREATE UNIQUE INDEX idx_original_url_normalized ON urls(LOWER(TRIM(original_url)))
    rollback:
      sql: DROP INDEX IF EXISTS idx_original_url_normalized
```

### Performance Tips

- **Use indexed columns in WHERE clauses**
- **Avoid SELECT * in production code**
- **Use connection pooling (HikariCP is default)**
- **Enable query logging during development:**
  ```yaml
  spring:
    jpa:
      show-sql: true
      properties:
        hibernate:
          format_sql: true
  ```

---

## Security Considerations

### Input Validation

```java
public record ShortenRequest(
    @NotBlank(message = "URL cannot be blank")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    String url
) {}
```

### SQL Injection Prevention

- ✅ **Always use JPA repository methods** (parameterized queries)
- ✅ **Use `@Query` with named parameters** (`:url`)
- ❌ **Never concatenate SQL strings**

### Production Configuration

**Environment Variables:**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

**Docker Secrets (Production):**
```yaml
secrets:
  db_password:
    external: true
services:
  app:
    secrets:
      - db_password
```

---

## Monitoring & Logging

### Structured Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UrlShortenerService {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    
    public ShortenResponse shortenUrl(String originalUrl) {
        logger.info("Shortening URL: {}", originalUrl);
        // ... implementation
        logger.info("Generated short code: {}", shortCode);
        return response;
    }
}
```

### Health Checks

Spring Boot Actuator provides health endpoints:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

Access: `http://localhost:8080/actuator/health`

---

**Happy coding! Follow these patterns to build a clean, maintainable URL Shortener service.** 🚀
